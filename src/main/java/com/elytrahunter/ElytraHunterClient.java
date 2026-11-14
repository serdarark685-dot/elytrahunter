package com.elytrahunter;

import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

public class ElytraHunterClient implements ClientModInitializer {
   private static final double SCAN_RANGE = 96.0;
   private static final double KEEP_RANGE = 144.0;
   private static final double ATTACK_REACH = 3.0;
   private static final double NO_BOOST_DIST = 5.0;
   private static final float MIN_COOLDOWN = 0.9F;
   private static final Set<String> FRIENDS = Set.of();
   private static final int KEY_MENU = 344;
   private static final int KEY_TOGGLE = 72;
   private static ElytraHunterClient instance;
   private State state = State.APPROACH;
   private AbstractClientPlayerEntity target;
   private Vec3d lastTargetPos;
   private int retreatTicks;
   private int tickCounter;
   private int rocketCooldown = 1000;
   private boolean jumpHeld;
   private boolean active;
   private boolean menuKeyWas;
   private boolean toggleKeyWas;
   Vec3d prevEye;
   Vec3d curEye;
   Vec3d prevTarget;
   Vec3d curTarget;
   float prevYaw;
   float curYaw;
   float prevPitch;
   float curPitch;
   long tickEndNanos = System.nanoTime();
   private AbstractClientPlayerEntity snapTarget;
   private boolean hasSnap;

   public static ElytraHunterClient instance() {
      return instance;
   }

   public State getState() {
      return this.state;
   }

   public AbstractClientPlayerEntity getTarget() {
      return this.target;
   }

   public boolean isActive() {
      return this.active;
   }

   public void onInitializeClient() {
      instance = this;
      HunterConfig.load();
      ClientTickEvents.START_CLIENT_TICK.register(this::tick);
      ClientTickEvents.END_CLIENT_TICK.register(this::endTick);
      HudElementRegistry.addLast(Identifier.of("elytrahunter", "hud"), (context, tickCounter) -> HunterHud.render(context, MinecraftClient.getInstance()));
   }

   private void pollKeys(MinecraftClient mc) {
      if (mc.getWindow() != null) {
         long window = mc.getWindow().getHandle();
         boolean menuDown = GLFW.glfwGetKey(window, 344) == 1;
         boolean toggleDown = GLFW.glfwGetKey(window, 72) == 1;
         if (mc.currentScreen == null && mc.player != null) {
            if (menuDown && !this.menuKeyWas) {
               mc.setScreen(new HunterScreen());
            }

            if (toggleDown && !this.toggleKeyWas) {
               HunterConfig.enabled = !HunterConfig.enabled;
               HunterConfig.save();
               mc.player.sendMessage(Text.literal("Elytra Hunter: " + (HunterConfig.enabled ? "AÇIK" : "KAPALI")), true);
            }
         }

         this.menuKeyWas = menuDown;
         this.toggleKeyWas = toggleDown;
      }
   }

   private void tick(MinecraftClient mc) {
      this.pollKeys(mc);
      ClientPlayerEntity player = mc.player;
      if (player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
         this.reset(mc);
         return;
      }

      // Tetik: elytra giyili + fisek (sol elde VEYA hotbar'da; GUI'den secilir)
      if (!HunterConfig.enabled
            || !player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
            || !this.hasRocket(player)) {
         this.reset(mc);
         return;
      }

      this.active = true;
      this.tickCounter++;
      if (this.rocketCooldown < 1000) {
         this.rocketCooldown++;
      }

      if (this.target == null || !this.valid(player, this.target)) {
         this.target = this.findTarget(mc, player);
         this.state = State.APPROACH;
         this.lastTargetPos = null;
         this.retreatTicks = 0;
      }
      if (this.target == null) {
         this.releaseJump(mc);
         return;
      }

      Vec3d targetPos = new Vec3d(
            this.target.getX(), this.target.getY() + this.target.getHeight() * 0.5, this.target.getZ());
      Vec3d targetVel = this.lastTargetPos == null ? Vec3d.ZERO : targetPos.subtract(this.lastTargetPos);
      this.lastTargetPos = targetPos;
      Vec3d eye = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
      double dist = eye.distanceTo(targetPos);
      double speed = Math.max(player.getVelocity().length(), 1.0);
      double lead = MathHelper.clamp(dist / speed, 0.0, 4.0);
      Vec3d aim = targetPos.add(targetVel.multiply(lead));

      // Havada degilsek: kalkis
      if (!player.isGliding()) {
         this.jumpHeld = true;
         mc.options.jumpKey.setPressed(this.tickCounter % 2 == 0);
         this.lookAt(player, eye, aim);
         return;
      }
      this.releaseJump(mc);
      this.equipSword(player);

      double retreatDist = HunterConfig.retreatDistance;

      if (this.state == State.APPROACH) {
         this.lookAt(player, eye, aim);

         if (dist > NO_BOOST_DIST) {
            this.useFirework(mc, player);
         }

         if (this.reachDistance(eye, this.target) <= ATTACK_REACH
               && player.getAttackCooldownProgress(0.0F) >= MIN_COOLDOWN) {
            mc.interactionManager.attackEntity(player, this.target);
            player.swingHand(Hand.MAIN_HAND);
            // Uzaklasma 0 ise geri cekilme yok: hedefin dibinde dolasmaya devam eder
            if (retreatDist >= 0.25) {
               this.state = State.RETREAT;
               this.retreatTicks = 0;
            }
         }
      } else {
         // RETREAT: sadece ayarlanan mesafeye kadar uzaklas (fisek harcamadan, mevcut hizla)
         this.retreatTicks++;
         Vec3d away = eye.subtract(targetPos);
         if (away.lengthSquared() < 1.0E-4) {
            away = new Vec3d(0.0, 1.0, 0.0);
         }
         Vec3d retreatPoint = eye.add(away.normalize().multiply(8.0)).add(0.0, 1.0, 0.0);
         this.lookAt(player, eye, retreatPoint);

         int maxTicks = (int) MathHelper.clamp(6.0 + retreatDist * 1.5, 6.0, 30.0);
         if (dist >= retreatDist || this.retreatTicks >= maxTicks || retreatDist < 0.25) {
            this.state = State.APPROACH;
         }
      }
   }

   private boolean hasRocket(ClientPlayerEntity player) {
      if (HunterConfig.hotbarFirework) {
         return this.findHotbarRocket(player) >= 0;
      }
      return player.getOffHandStack().isOf(Items.FIREWORK_ROCKET);
   }

   private int findHotbarRocket(ClientPlayerEntity player) {
      PlayerInventory inv = player.getInventory();
      for (int i = 0; i < 9; i++) {
         if (inv.getStack(i).isOf(Items.FIREWORK_ROCKET)) {
            return i;
         }
      }
      return -1;
   }

   private boolean valid(ClientPlayerEntity player, AbstractClientPlayerEntity t) {
      return t.isAlive() && !t.isRemoved() && !t.isSpectator() && player.distanceTo(t) <= 144.0;
   }

   private AbstractClientPlayerEntity findTarget(MinecraftClient mc, ClientPlayerEntity player) {
      AbstractClientPlayerEntity best = null;
      double bestDist = 96.0;

      for (AbstractClientPlayerEntity other : mc.world.getPlayers()) {
         if (other != player && other.isAlive() && !other.isSpectator() && !FRIENDS.contains(other.getGameProfile().name())) {
            double d = player.distanceTo(other);
            if (d < bestDist) {
               bestDist = d;
               best = other;
            }
         }
      }

      return best;
   }

   private double reachDistance(Vec3d from, AbstractClientPlayerEntity t) {
      Box box = t.getBoundingBox();
      double x = MathHelper.clamp(from.x, box.minX, box.maxX);
      double y = MathHelper.clamp(from.y, box.minY, box.maxY);
      double z = MathHelper.clamp(from.z, box.minZ, box.maxZ);
      return from.distanceTo(new Vec3d(x, y, z));
   }

   private void lookAt(ClientPlayerEntity player, Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, horizontal) * 180.0 / Math.PI));
      player.setYaw(yaw);
      player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
   }

   private void useFirework(MinecraftClient mc, ClientPlayerEntity player) {
      // iki fisek arasi minimum sure
      if (this.rocketCooldown < Math.max(1, HunterConfig.fireworkInterval)) {
         return;
      }
      // momentum hala yuksekse yeni fisek harcama
      if (HunterConfig.minBoostSpeed > 0 && player.getVelocity().length() * 20.0 > HunterConfig.minBoostSpeed) {
         return;
      }

      if (HunterConfig.hotbarFirework) {
         int slot = this.findHotbarRocket(player);
         if (slot < 0) {
            return;
         }
         PlayerInventory inv = player.getInventory();
         int prev = inv.getSelectedSlot();
         inv.setSelectedSlot(slot);
         mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
         inv.setSelectedSlot(prev); // kilica geri don
         this.rocketCooldown = 0;
      } else {
         if (!player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            return;
         }
         mc.interactionManager.interactItem(player, Hand.OFF_HAND);
         this.rocketCooldown = 0;
      }
   }

   private void equipSword(ClientPlayerEntity player) {
      if (!player.getMainHandStack().isIn(ItemTags.SWORDS)) {
         PlayerInventory inv = player.getInventory();

         for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isIn(ItemTags.SWORDS)) {
               inv.setSelectedSlot(i);
               return;
            }
         }
      }
   }

   private void releaseJump(MinecraftClient mc) {
      if (this.jumpHeld) {
         mc.options.jumpKey.setPressed(false);
         this.jumpHeld = false;
      }
   }

   private void reset(MinecraftClient mc) {
      this.releaseJump(mc);
      this.target = null;
      this.lastTargetPos = null;
      this.state = State.APPROACH;
      this.retreatTicks = 0;
      this.active = false;
   }

   private void endTick(MinecraftClient mc) {
      ClientPlayerEntity p = mc.player;
      if (p != null && mc.world != null) {
         Vec3d eye = new Vec3d(p.getX(), p.getEyeY(), p.getZ());
         this.prevEye = this.hasSnap && this.curEye != null ? this.curEye : eye;
         this.curEye = eye;
         this.prevYaw = this.hasSnap ? this.curYaw : p.getYaw();
         this.curYaw = p.getYaw();
         this.prevPitch = this.hasSnap ? this.curPitch : p.getPitch();
         this.curPitch = p.getPitch();
         this.hasSnap = true;
         if (this.target != null && this.active) {
            Vec3d c = new Vec3d(this.target.getX(), this.target.getY() + this.target.getHeight() * 0.5, this.target.getZ());
            this.prevTarget = this.curTarget != null && this.snapTarget == this.target ? this.curTarget : c;
            this.curTarget = c;
            this.snapTarget = this.target;
         } else {
            this.curTarget = null;
            this.snapTarget = null;
         }

         this.tickEndNanos = System.nanoTime();
      } else {
         this.hasSnap = false;
         this.curTarget = null;
         this.snapTarget = null;
      }
   }

   public static enum State {
      APPROACH,
      RETREAT;
   }
}

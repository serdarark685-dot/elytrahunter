package com.elytrahunter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

/**
 * Elytra Hunter
 *
 * Aktif olma sartlari: govdede ELYTRA + SOL ELDE (offhand) FISEK.
 * Saf client-tick mantigi: render/GL/Mixin yok -> VulkanMod (ve Sodium vb.) ile cakismaz.
 */
public class ElytraHunterClient implements ClientModInitializer {

    // ---------- AYARLAR ----------
    private static final double SCAN_RANGE        = 96.0;  // hedef tarama menzili (blok)
    private static final double ATTACK_REACH      = 3.0;   // vurus mesafesi (hitbox'a en yakin nokta)
    private static final double NO_BOOST_DIST     = 5.0;   // bu mesafeden yakinda fisek atma (ustunden gecmesin)
    private static final double RETREAT_DISTANCE  = 6.0;   // vurduktan sonra bu kadar uzaklas
    private static final int    RETREAT_MAX_TICKS = 12;    // uzaklasma zaman asimi
    private static final int    FIREWORK_INTERVAL = 2;     // kac tickte bir fisek (1 = her tick)
    private static final float  MIN_COOLDOWN      = 0.9f;  // kilic vurus hazirligi (0..1)
    private static final Set<String> FRIENDS      = Set.of(); // dokunulmayacak oyuncu adlari, ornek: Set.of("arkadas1")
    // ------------------------------

    private enum State { APPROACH, RETREAT }

    private State state = State.APPROACH;
    private AbstractClientPlayerEntity target;
    private Vec3d lastTargetPos;
    private int retreatTicks;
    private int tickCounter;
    private boolean jumpHeld;

    @Override
    public void onInitializeClient() {
        // START tick: donus ve paketler bu tick'in hareket paketine dahil olsun (1 tick gecikme yok)
        ClientTickEvents.START_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            reset(mc);
            return;
        }

        // Tetik: elytra giyili + sol elde fisek
        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
                || !p.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            reset(mc);
            return;
        }

        tickCounter++;

        if (target == null || !valid(p, target)) {
            target = findTarget(mc, p);
            state = State.APPROACH;
            lastTargetPos = null;
            retreatTicks = 0;
        }
        if (target == null) {
            releaseJump(mc);
            return;
        }

        // Hedef hizi (tick basina), yon tahmini icin
        Vec3d tPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
        Vec3d tVel = lastTargetPos == null ? Vec3d.ZERO : tPos.subtract(lastTargetPos);
        lastTargetPos = tPos;

        Vec3d eye = new Vec3d(p.getX(), p.getEyeY(), p.getZ());
        double dist = eye.distanceTo(tPos);

        // Tahmini nokta (hedefin gidecegi yere kilit)
        double mySpeed = Math.max(p.getVelocity().length(), 1.0);
        double leadTicks = MathHelper.clamp(dist / mySpeed, 0.0, 4.0);
        Vec3d predicted = tPos.add(tVel.multiply(leadTicks));

        // Havada degilsek: kalkis (ziplama tusunu ardisik tickte birak/bas -> havada elytra acilir)
        if (!p.isGliding()) {
            jumpHeld = true;
            mc.options.jumpKey.setPressed(tickCounter % 2 == 0);
            lookAt(p, eye, predicted);
            return;
        }
        releaseJump(mc);

        // Kilic sec (hotbar)
        equipSword(p);

        if (state == State.APPROACH) {
            lookAt(p, eye, predicted);

            if (dist > NO_BOOST_DIST) {
                useFirework(mc, p);
            }

            if (reachDistance(eye, target) <= ATTACK_REACH
                    && p.getAttackCooldownProgress(0.0f) >= MIN_COOLDOWN) {
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
                state = State.RETREAT;
                retreatTicks = 0;
            }
        } else { // RETREAT: azicik uzaklas, sonra tekrar dal
            retreatTicks++;
            Vec3d away = eye.subtract(tPos);
            if (away.lengthSquared() < 1.0E-4) away = new Vec3d(0, 1, 0);
            Vec3d awayPoint = eye.add(away.normalize().multiply(8.0)).add(0, 1.0, 0);
            lookAt(p, eye, awayPoint);
            useFirework(mc, p);

            boolean far = dist >= RETREAT_DISTANCE;
            boolean ready = p.getAttackCooldownProgress(0.0f) >= MIN_COOLDOWN;
            if ((far && ready) || retreatTicks >= RETREAT_MAX_TICKS) {
                state = State.APPROACH;
            }
        }
    }

    // ---------- yardimcilar ----------

    private boolean valid(ClientPlayerEntity p, AbstractClientPlayerEntity t) {
        return t.isAlive() && !t.isRemoved() && !t.isSpectator()
                && p.distanceTo(t) <= SCAN_RANGE * 1.5;
    }

    private AbstractClientPlayerEntity findTarget(MinecraftClient mc, ClientPlayerEntity p) {
        AbstractClientPlayerEntity best = null;
        double bestDist = SCAN_RANGE;
        for (AbstractClientPlayerEntity other : mc.world.getPlayers()) {
            if (other == p || !other.isAlive() || other.isSpectator()) continue;
            if (FRIENDS.contains(other.getGameProfile().name())) continue;
            double d = p.distanceTo(other);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    /** Gozden hedefin hitbox'ina en yakin noktaya mesafe. */
    private double reachDistance(Vec3d eye, AbstractClientPlayerEntity t) {
        Box b = t.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    /** Aninda, yumusatmasiz kilit. */
    private void lookAt(ClientPlayerEntity p, Vec3d eye, Vec3d point) {
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(dy, horiz) * 180.0 / Math.PI);
        p.setYaw(yaw);
        p.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    private void useFirework(MinecraftClient mc, ClientPlayerEntity p) {
        if (tickCounter % FIREWORK_INTERVAL != 0) return;
        if (!p.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) return;
        mc.interactionManager.interactItem(p, Hand.OFF_HAND);
    }

    private void equipSword(ClientPlayerEntity p) {
        if (p.getMainHandStack().isIn(ItemTags.SWORDS)) return;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isIn(ItemTags.SWORDS)) {
                inv.setSelectedSlot(i);
                return;
            }
        }
    }

    private void releaseJump(MinecraftClient mc) {
        if (jumpHeld) {
            mc.options.jumpKey.setPressed(false);
            jumpHeld = false;
        }
    }

    private void reset(MinecraftClient mc) {
        releaseJump(mc);
        target = null;
        lastTargetPos = null;
        state = State.APPROACH;
        retreatTicks = 0;
    }
}

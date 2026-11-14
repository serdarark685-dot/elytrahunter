package com.elytrahunter;

import java.util.Locale;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;

final class HunterHud {
   static final int NEON = -16725761;
   static final int NEON_SOFT = -6559489;
   static final int CORE = -1442817;
   static final int GLOW = 1073793279;
   static final int GLOW_FAINT = 536922367;
   static final int BG = -1341911010;
   static final int DIM = -10835508;
   static final int BAD = -43657;

   private HunterHud() {
   }

   static void render(DrawContext ctx, MinecraftClient mc) {
      if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
         ElytraHunterClient e = ElytraHunterClient.instance();
         if (e != null) {
            if (HunterConfig.showPanel) {
               drawPanel(ctx, mc, e);
            }

            if (HunterConfig.showMarker) {
               drawMarker(ctx, mc, e);
            }
         }
      }
   }

   // ---------- RGB yardimcilari ----------

   /** Siradaki rengin tonu zamanla surekli doner (4 sn'de bir tur). offset: 0..1 arasi faz farki. */
   static int rainbow(double offset) {
      double t = (System.currentTimeMillis() % 4000L) / 4000.0;
      double hue = (t + offset) % 1.0;
      if (hue < 0) hue += 1.0;
      return 0xFF000000 | hsvToRgb(hue, 0.85, 1.0);
   }

   static int withAlpha(int argb, int alpha) {
      return (alpha << 24) | (argb & 0x00FFFFFF);
   }

   private static int hsvToRgb(double h, double s, double v) {
      double hh = h * 6.0;
      int i = (int) Math.floor(hh) % 6;
      double f = hh - Math.floor(hh);
      double p = v * (1.0 - s);
      double q = v * (1.0 - s * f);
      double t = v * (1.0 - s * (1.0 - f));
      double r;
      double g;
      double b;
      switch (i) {
         case 0: r = v; g = t; b = p; break;
         case 1: r = q; g = v; b = p; break;
         case 2: r = p; g = v; b = t; break;
         case 3: r = p; g = q; b = v; break;
         case 4: r = t; g = p; b = v; break;
         default: r = v; g = p; b = q; break;
      }
      int ri = (int) Math.round(r * 255.0);
      int gi = (int) Math.round(g * 255.0);
      int bi = (int) Math.round(b * 255.0);
      return (ri << 16) | (gi << 8) | bi;
   }

   private static String retreatText() {
      return HunterConfig.retreatDistance < 0.25
            ? "Uzakla\u015Fma: YOK (dibinde)"
            : String.format(Locale.ROOT, "Uzakla\u015Fma: %.1f blok", HunterConfig.retreatDistance);
   }

   private static void drawPanel(DrawContext ctx, MinecraftClient mc, ElytraHunterClient e) {
      TextRenderer tr = mc.textRenderer;
      boolean on = HunterConfig.enabled;
      String title = "ELYTRA HUNTER";

      int iv = Math.max(1, HunterConfig.fireworkInterval);
      String speedPart = HunterConfig.minBoostSpeed > 0 ? " <" + HunterConfig.minBoostSpeed + " b/sn" : "";

      AbstractClientPlayerEntity t = e.getTarget();
      String[] lines = new String[] {
         "Durum: " + (on ? "A\u00C7IK" : "KAPALI"),
         "Mod: " + modText(e),
         "Fi\u015Fek: " + (HunterConfig.hotbarFirework ? "Hotbar" : "Sol el") + "  " + iv + "t" + speedPart,
         retreatText(),
         on && e.isActive() && t != null
               ? "Hedef: " + t.getName().getString() + "  " + (int) mc.player.distanceTo(t) + "m"
               : "Hedef: yok",
         "[R-Shift] men\u00FC  [H] a\u00E7/kapa"
      };

      int textW = tr.getWidth(title);
      for (String l : lines) {
         textW = Math.max(textW, tr.getWidth(l));
      }
      int w = textW + 14;
      int lineH = 11;
      int h = 5 + 15 + lineH * (lines.length - 1) + 8 + 4;
      int x = 0;
      int y = mc.getWindow().getScaledHeight() / 3;

      int c0 = rainbow(0.0);

      // parlama + arka plan
      ctx.fill(x, y - 2, x + w + 2, y + h + 2, withAlpha(c0, 0x20));
      ctx.fill(x, y - 1, x + w + 1, y + h + 1, withAlpha(c0, 0x40));
      ctx.fill(x, y, x + w, y + h, -1341911010);

      // RGB kenarlik: ust, sag ve alt kenar boyunca akan gokkusagi
      double perim = w * 2.0 + h;
      for (int i = 0; i < w; i++) {
         ctx.fill(x + i, y, x + i + 1, y + 1, rainbow(i / perim));
         ctx.fill(x + i, y + h - 1, x + i + 1, y + h, rainbow((w + h + (w - 1 - i)) / perim));
      }
      for (int j = 0; j < h; j++) {
         ctx.fill(x + w - 1, y + j, x + w, y + j + 1, rainbow((w + j) / perim));
      }

      // baslik alti cizgisi
      ctx.fill(x, y + 16, x + w - 1, y + 17, withAlpha(rainbow(0.5), 0x70));

      int tx = x + 7;
      int ty = y + 5;
      ctx.drawText(tr, title, tx, ty, rainbow(0.1), true);
      ty += 15;
      for (int k = 0; k < lines.length; k++) {
         int color;
         if (k == 0) {
            color = on ? -6559489 : -43657;
         } else if (k == lines.length - 1) {
            color = -10835508;
         } else {
            color = -6559489;
         }
         ctx.drawText(tr, lines[k], tx, ty, color, true);
         ty += lineH;
      }
   }

   private static String modText(ElytraHunterClient e) {
      if (!HunterConfig.enabled) {
         return "Kapalı";
      } else if (!e.isActive()) {
         return "Bekliyor";
      } else {
         return e.getState() == ElytraHunterClient.State.RETREAT ? "Geri çekiliyor" : "Saldırıyor";
      }
   }

   private static void drawMarker(DrawContext ctx, MinecraftClient mc, ElytraHunterClient e) {
      AbstractClientPlayerEntity t = e.getTarget();
      if (HunterConfig.enabled && e.isActive() && t != null) {
         if (e.curTarget != null && e.prevTarget != null && e.curEye != null && e.prevEye != null) {
            double a = MathHelper.clamp((System.nanoTime() - e.tickEndNanos) / 5.0E7, 0.0, 1.0);
            Vec3d eye = lerp(e.prevEye, e.curEye, a);
            Vec3d tp = lerp(e.prevTarget, e.curTarget, a);
            float yaw = e.prevYaw + MathHelper.wrapDegrees(e.curYaw - e.prevYaw) * (float)a;
            float pitch = e.prevPitch + (e.curPitch - e.prevPitch) * (float)a;
            double yr = Math.toRadians(yaw);
            double pr = Math.toRadians(pitch);
            double fx = -Math.sin(yr) * Math.cos(pr);
            double fy = -Math.sin(pr);
            double fz = Math.cos(yr) * Math.cos(pr);
            double rx = -Math.cos(yr);
            double rz = -Math.sin(yr);
            double ux = -rz * fy;
            double uy = rz * fx - rx * fz;
            double uz = rx * fy;
            double dx = tp.x - eye.x;
            double dy = tp.y - eye.y;
            double dz = tp.z - eye.z;
            double zc = dx * fx + dy * fy + dz * fz;
            if (!(zc < 0.5)) {
               double xc = dx * rx + dz * rz;
               double yc = dx * ux + dy * uy + dz * uz;
               int sw = mc.getWindow().getScaledWidth();
               int sh = mc.getWindow().getScaledHeight();
               double fov = ((Integer)mc.options.getFov().getValue()).intValue();
               double f = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
               double aspect = (double)sw / sh;
               double sx = sw / 2.0 + xc / zc * f / aspect * (sw / 2.0);
               double sy = sh / 2.0 - yc / zc * f * (sh / 2.0);
               double heightPx = t.getHeight() / zc * f * (sh / 2.0);
               double r = MathHelper.clamp(heightPx * 0.62, 11.0, 55.0);
               double ang = System.currentTimeMillis() % 2400L / 2400.0 * Math.PI * 2.0;
               double[] cx = new double[4];
               double[] cy = new double[4];

               for (int k = 0; k < 4; k++) {
                  double ak = ang + (Math.PI / 4) + k * Math.PI / 2.0;
                  cx[k] = sx + Math.cos(ak) * r;
                  cy[k] = sy + Math.sin(ak) * r;
               }

               for (int k = 0; k < 4; k++) {
                  int n = (k + 1) % 4;
                  line(ctx, cx[k], cy[k], cx[n], cy[n], 1073793279, 1);
               }

               for (int k = 0; k < 4; k++) {
                  int n = (k + 1) % 4;
                  line(ctx, cx[k], cy[k], cx[n], cy[n], -16725761, 0);
               }

               int px = (int)Math.round(sx);
               int py = (int)Math.round(sy);
               ctx.fill(px - 3, py - 3, px + 4, py + 4, 1073793279);
               ctx.fill(px - 1, py - 1, px + 2, py + 2, -1442817);
               String label = t.getName().getString() + "  " + (int)mc.player.distanceTo(t) + "m";
               int lw = mc.textRenderer.getWidth(label);
               ctx.drawText(mc.textRenderer, label, px - lw / 2, (int)(sy + r + 5.0), -6559489, true);
            }
         }
      }
   }

   private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
      return new Vec3d(
         a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t
      );
   }

   static void line(DrawContext ctx, double x1, double y1, double x2, double y2, int color, int half) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      int n = (int)Math.max(1.0, Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));

      for (int i = 0; i <= n; i++) {
         int x = (int)Math.round(x1 + dx * i / n);
         int y = (int)Math.round(y1 + dy * i / n);
         ctx.fill(x - half, y - half, x + half + 1, y + half + 1, color);
      }
   }

   static void frame(DrawContext ctx, int x1, int y1, int x2, int y2) {
      ctx.fill(x1 - 2, y1 - 2, x2 + 2, y1 + 1, 536922367);
      ctx.fill(x1 - 2, y2 - 1, x2 + 2, y2 + 2, 536922367);
      ctx.fill(x1 - 2, y1 - 2, x1 + 1, y2 + 2, 536922367);
      ctx.fill(x2 - 1, y1 - 2, x2 + 2, y2 + 2, 536922367);
      ctx.fill(x1 - 1, y1 - 1, x2 + 1, y1 + 1, 1073793279);
      ctx.fill(x1 - 1, y2 - 1, x2 + 1, y2 + 1, 1073793279);
      ctx.fill(x1 - 1, y1 - 1, x1 + 1, y2 + 1, 1073793279);
      ctx.fill(x2 - 1, y1 - 1, x2 + 1, y2 + 1, 1073793279);
      ctx.fill(x1, y1, x2, y1 + 1, -16725761);
      ctx.fill(x1, y2 - 1, x2, y2, -16725761);
      ctx.fill(x1, y1, x1 + 1, y2, -16725761);
      ctx.fill(x2 - 1, y1, x2, y2, -16725761);
   }
}

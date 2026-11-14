package com.elytrahunter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Blue-neon HUD: always-visible mini tab + Iron-Man style rotating square on the locked target. */
final class HunterHud {

    static final int NEON = 0xFF00C8FF;
    static final int NEON_SOFT = 0xFF9BE8FF;
    static final int CORE = 0xFFE9FBFF;
    static final int GLOW = 0x4000C8FF;
    static final int GLOW_FAINT = 0x2000C8FF;
    static final int BG = 0xB004101E;
    static final int DIM = 0xFF5AA9CC;
    static final int BAD = 0xFFFF5577;

    private HunterHud() {}

    static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        ElytraHunterClient e = ElytraHunterClient.instance();
        if (e == null) return;
        if (HunterConfig.showPanel) drawPanel(ctx, mc, e);
        if (HunterConfig.showMarker) drawMarker(ctx, mc, e);
    }

    // ------------------------------------------------------------------ mini tab
    private static void drawPanel(DrawContext ctx, MinecraftClient mc, ElytraHunterClient e) {
        TextRenderer tr = mc.textRenderer;

        boolean on = HunterConfig.enabled;
        String title = "ELYTRA HUNTER";
        String l1 = "Durum: " + (on ? "A\u00c7IK" : "KAPALI");
        String l2 = "Mod: " + modText(e);
        int iv = Math.max(1, HunterConfig.fireworkInterval);
        String l3 = String.format(Locale.ROOT, "Fi\u015fek: %d tick (%.1f/sn)", iv, 20.0 / iv);
        AbstractClientPlayerEntity t = e.getTarget();
        String l4 = (on && e.isActive() && t != null)
                ? "Hedef: " + t.getName().getString() + "  " + (int) mc.player.distanceTo(t) + "m"
                : "Hedef: yok";
        String hint = "[R-Shift] men\u00fc  [H] a\u00e7/kapa";

        int textW = Math.max(Math.max(tr.getWidth(title), tr.getWidth(l1)),
                Math.max(Math.max(tr.getWidth(l2), tr.getWidth(l3)),
                        Math.max(tr.getWidth(l4), tr.getWidth(hint))));
        int w = textW + 14;
        int lineH = 11;
        int h = 8 + lineH * 6 + 2;
        int x = 0;
        int y = mc.getWindow().getScaledHeight() / 3;

        // glow, body, border (flush with the left screen edge, like a tab)
        ctx.fill(x, y - 2, x + w + 2, y + h + 2, GLOW_FAINT);
        ctx.fill(x, y - 1, x + w + 1, y + h + 1, GLOW);
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 1, NEON);
        ctx.fill(x, y + h - 1, x + w, y + h, NEON);
        ctx.fill(x + w - 1, y, x + w, y + h, NEON);
        ctx.fill(x, y + 16, x + w - 1, y + 17, GLOW);

        int tx = x + 7;
        int ty = y + 5;
        ctx.drawText(tr, title, tx, ty, NEON, true);
        ty += 15;
        ctx.drawText(tr, l1, tx, ty, on ? NEON_SOFT : BAD, true);
        ty += lineH;
        ctx.drawText(tr, l2, tx, ty, NEON_SOFT, true);
        ty += lineH;
        ctx.drawText(tr, l3, tx, ty, NEON_SOFT, true);
        ty += lineH;
        ctx.drawText(tr, l4, tx, ty, NEON_SOFT, true);
        ty += lineH;
        ctx.drawText(tr, hint, tx, ty, DIM, true);
    }

    private static String modText(ElytraHunterClient e) {
        if (!HunterConfig.enabled) return "Kapal\u0131";
        if (!e.isActive()) return "Bekliyor";
        return e.getState() == ElytraHunterClient.State.RETREAT ? "Geri \u00e7ekiliyor" : "Sald\u0131r\u0131yor";
    }

    // ------------------------------------------------------------------ target marker
    private static void drawMarker(DrawContext ctx, MinecraftClient mc, ElytraHunterClient e) {
        AbstractClientPlayerEntity t = e.getTarget();
        if (!HunterConfig.enabled || !e.isActive() || t == null) return;
        if (e.curTarget == null || e.prevTarget == null || e.curEye == null || e.prevEye == null) return;

        // interpolate between the last two ticks (same idea as vanilla partial ticks)
        double a = MathHelper.clamp((System.nanoTime() - e.tickEndNanos) / 50_000_000.0, 0.0, 1.0);
        Vec3d eye = lerp(e.prevEye, e.curEye, a);
        Vec3d tp = lerp(e.prevTarget, e.curTarget, a);
        float yaw = e.prevYaw + MathHelper.wrapDegrees(e.curYaw - e.prevYaw) * (float) a;
        float pitch = e.prevPitch + (e.curPitch - e.prevPitch) * (float) a;

        // camera basis (Minecraft: yaw 0 = +Z, pitch > 0 = looking down)
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);
        double fx = -Math.sin(yr) * Math.cos(pr);
        double fy = -Math.sin(pr);
        double fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr);
        double rz = -Math.sin(yr);
        // up = right x forward (right.y == 0)
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        double dx = tp.x - eye.x;
        double dy = tp.y - eye.y;
        double dz = tp.z - eye.z;
        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.5) return; // behind the camera / too close to project
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double fov = mc.options.getFov().getValue();
        double f = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
        double aspect = (double) sw / (double) sh;

        double sx = sw / 2.0 + (xc / zc) * f / aspect * (sw / 2.0);
        double sy = sh / 2.0 - (yc / zc) * f * (sh / 2.0);

        // square size follows the target's on-screen height
        double heightPx = (t.getHeight() / zc) * f * (sh / 2.0);
        double r = MathHelper.clamp(heightPx * 0.62, 11.0, 55.0);

        double ang = (System.currentTimeMillis() % 2400L) / 2400.0 * Math.PI * 2.0;
        double[] cx = new double[4];
        double[] cy = new double[4];
        for (int k = 0; k < 4; k++) {
            double ak = ang + Math.PI / 4.0 + k * Math.PI / 2.0;
            cx[k] = sx + Math.cos(ak) * r;
            cy[k] = sy + Math.sin(ak) * r;
        }
        for (int k = 0; k < 4; k++) {
            int n = (k + 1) % 4;
            line(ctx, cx[k], cy[k], cx[n], cy[n], GLOW, 1);   // glow
        }
        for (int k = 0; k < 4; k++) {
            int n = (k + 1) % 4;
            line(ctx, cx[k], cy[k], cx[n], cy[n], NEON, 0);   // core
        }

        // center dot
        int px = (int) Math.round(sx);
        int py = (int) Math.round(sy);
        ctx.fill(px - 3, py - 3, px + 4, py + 4, GLOW);
        ctx.fill(px - 1, py - 1, px + 2, py + 2, CORE);

        // small label under the square
        String label = t.getName().getString() + "  " + (int) mc.player.distanceTo(t) + "m";
        int lw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, px - lw / 2, (int) (sy + r + 5), NEON_SOFT, true);
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return new Vec3d(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }

    /** Pixel line built from small fills; half = 0 -> 1px, half = 1 -> 3px wide. */
    static void line(DrawContext ctx, double x1, double y1, double x2, double y2, int color, int half) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        int n = (int) Math.max(1.0, Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        for (int i = 0; i <= n; i++) {
            int x = (int) Math.round(x1 + dx * i / n);
            int y = (int) Math.round(y1 + dy * i / n);
            ctx.fill(x - half, y - half, x + half + 1, y + half + 1, color);
        }
    }

    /** Hollow neon rectangle (glow + core). */
    static void frame(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1 - 2, y1 - 2, x2 + 2, y1 + 1, GLOW_FAINT);
        ctx.fill(x1 - 2, y2 - 1, x2 + 2, y2 + 2, GLOW_FAINT);
        ctx.fill(x1 - 2, y1 - 2, x1 + 1, y2 + 2, GLOW_FAINT);
        ctx.fill(x2 - 1, y1 - 2, x2 + 2, y2 + 2, GLOW_FAINT);
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y1 + 1, GLOW);
        ctx.fill(x1 - 1, y2 - 1, x2 + 1, y2 + 1, GLOW);
        ctx.fill(x1 - 1, y1 - 1, x1 + 1, y2 + 1, GLOW);
        ctx.fill(x2 - 1, y1 - 1, x2 + 1, y2 + 1, GLOW);
        ctx.fill(x1, y1, x2, y1 + 1, NEON);
        ctx.fill(x1, y2 - 1, x2, y2, NEON);
        ctx.fill(x1, y1, x1 + 1, y2, NEON);
        ctx.fill(x2 - 1, y1, x2, y2, NEON);
    }
}

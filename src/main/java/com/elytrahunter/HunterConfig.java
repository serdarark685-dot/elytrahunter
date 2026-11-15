package com.elytrahunter;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties-file config: .minecraft/config/elytrahunter.properties */
public final class HunterConfig {
    public static final int MIN_INTERVAL = 1;
    public static final int MAX_INTERVAL = 40;

    /** Master switch (H key / menu). */
    public static boolean enabled = true;
    /** Ticks between two firework uses (20 ticks = 1 second). Original behaviour = 2. */
    public static int fireworkInterval = 2;
    /** After a hit: turn back toward the target once this far away (blocks). */
    public static double retreatDistance = 8.0;
    public static final double MIN_RETREAT = 0.5;
    public static final double MAX_RETREAT = 30.0;
    /** Keep firing rockets while retreating (original behaviour = true). */
    public static boolean retreatFireworks = false;
    /** true = rockets come from the hotbar (select rocket, use, back to sword). false = offhand. */
    public static boolean hotbarMode = false;
    /** Slowly cycling rainbow GUI colours instead of fixed neon blue. */
    public static boolean rgb = true;
    /** Rotating square + dot over the locked target. */
    public static boolean showMarker = true;
    /** Always-visible mini tab on the left edge. */
    public static boolean showPanel = true;

    // ---- 1.2.0: elytra-target style options (all default to the old 1.1.x behaviour) ----
    /** Speed-limited turning instead of snapping straight onto the target. */
    public static boolean smoothRotation = false;
    public static final int MIN_ROT_SPEED = 5;
    public static final int MAX_ROT_SPEED = 180;
    /** Max yaw / pitch change per tick (degrees) while smoothing is on. */
    public static int yawSpeed = 54;
    public static int pitchSpeed = 42;
    /** 0 = no lead, 1 = hunter (distance based lead, original), 2 = fixed multiplier. */
    public static int predictMode = 1;
    public static final int PREDICT_MODES = 3;
    public static final double MIN_PREDICT = 0.5;
    public static final double MAX_PREDICT = 3.0;
    /** Lead in ticks of target movement for predictMode 2. */
    public static double predictMultiplier = 1.9;
    /** 0 = center of the body (original), 1 = eyes. */
    public static int aimPoint = 0;
    /** Nudge upwards when the next tick would hit a block. */
    public static boolean safeMode = false;
    public static final int MIN_FAR = 10;
    public static final int MAX_FAR = 100;
    /** Beyond this distance the "far" firework interval is used. */
    public static int farDistance = 50;
    public static int farInterval = 2;

    private HunterConfig() {}

    private static int clampInt(String raw, int fallback, int min, int max) {
        int v;
        try {
            v = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            v = fallback;
        }
        return Math.max(min, Math.min(max, v));
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("elytrahunter.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        showMarker = Boolean.parseBoolean(p.getProperty("showMarker", "true"));
        showPanel = Boolean.parseBoolean(p.getProperty("showPanel", "true"));
        retreatFireworks = Boolean.parseBoolean(p.getProperty("retreatFireworks", "false"));
        hotbarMode = Boolean.parseBoolean(p.getProperty("hotbarMode", "false"));
        rgb = Boolean.parseBoolean(p.getProperty("rgb", "true"));
        smoothRotation = Boolean.parseBoolean(p.getProperty("smoothRotation", "false"));
        safeMode = Boolean.parseBoolean(p.getProperty("safeMode", "false"));
        yawSpeed = clampInt(p.getProperty("yawSpeed", "54"), 54, MIN_ROT_SPEED, MAX_ROT_SPEED);
        pitchSpeed = clampInt(p.getProperty("pitchSpeed", "42"), 42, MIN_ROT_SPEED, MAX_ROT_SPEED);
        predictMode = clampInt(p.getProperty("predictMode", "1"), 1, 0, PREDICT_MODES - 1);
        aimPoint = clampInt(p.getProperty("aimPoint", "0"), 0, 0, 1);
        farDistance = clampInt(p.getProperty("farDistance", "50"), 50, MIN_FAR, MAX_FAR);
        farInterval = clampInt(p.getProperty("farInterval", "2"), 2, MIN_INTERVAL, MAX_INTERVAL);
        try {
            predictMultiplier = Double.parseDouble(p.getProperty("predictMultiplier", "1.9").trim());
        } catch (NumberFormatException e) {
            predictMultiplier = 1.9;
        }
        predictMultiplier = Math.max(MIN_PREDICT, Math.min(MAX_PREDICT, predictMultiplier));
        try {
            retreatDistance = Double.parseDouble(p.getProperty("retreatDistance", "8").trim());
        } catch (NumberFormatException e) {
            retreatDistance = 8.0;
        }
        retreatDistance = Math.max(MIN_RETREAT, Math.min(MAX_RETREAT, retreatDistance));
        try {
            fireworkInterval = Integer.parseInt(p.getProperty("fireworkInterval", "2").trim());
        } catch (NumberFormatException e) {
            fireworkInterval = 2;
        }
        fireworkInterval = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, fireworkInterval));
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("fireworkInterval", Integer.toString(fireworkInterval));
        p.setProperty("showMarker", Boolean.toString(showMarker));
        p.setProperty("showPanel", Boolean.toString(showPanel));
        p.setProperty("retreatDistance", Double.toString(retreatDistance));
        p.setProperty("retreatFireworks", Boolean.toString(retreatFireworks));
        p.setProperty("hotbarMode", Boolean.toString(hotbarMode));
        p.setProperty("rgb", Boolean.toString(rgb));
        p.setProperty("smoothRotation", Boolean.toString(smoothRotation));
        p.setProperty("safeMode", Boolean.toString(safeMode));
        p.setProperty("yawSpeed", Integer.toString(yawSpeed));
        p.setProperty("pitchSpeed", Integer.toString(pitchSpeed));
        p.setProperty("predictMode", Integer.toString(predictMode));
        p.setProperty("predictMultiplier", Double.toString(predictMultiplier));
        p.setProperty("aimPoint", Integer.toString(aimPoint));
        p.setProperty("farDistance", Integer.toString(farDistance));
        p.setProperty("farInterval", Integer.toString(farInterval));
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Elytra Hunter");
        } catch (IOException ignored) {
        }
    }
}

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

    private HunterConfig() {}

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
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Elytra Hunter");
        } catch (IOException ignored) {
        }
    }
}

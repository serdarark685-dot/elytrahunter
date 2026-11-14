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
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Elytra Hunter");
        } catch (IOException ignored) {
        }
    }
}

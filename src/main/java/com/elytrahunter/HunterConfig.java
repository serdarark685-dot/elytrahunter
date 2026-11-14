package com.elytrahunter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class HunterConfig {
   public static final int MIN_INTERVAL = 1;
   public static final int MAX_INTERVAL = 40;
   public static final int MAX_SPEED = 36;          // blok/sn (0 = hiz siniri kapali)
   public static final double MAX_RETREAT = 15.0;   // blok (0 = geri cekilme yok)

   public static boolean enabled = true;
   public static int fireworkInterval = 8;          // iki fisek arasi en az kac tick
   public static int minBoostSpeed = 22;            // hiz bunun altina dusmeden yeni fisek atma (blok/sn)
   public static double retreatDistance = 2.0;      // vurduktan sonra uzaklasma mesafesi (blok)
   public static boolean hotbarFirework = false;    // false = sol el, true = hotbar'daki fisek
   public static boolean showMarker = true;
   public static boolean showPanel = true;

   private HunterConfig() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("elytrahunter.properties");
   }

   public static void load() {
      Path f = file();
      if (!Files.exists(f)) {
         return;
      }
      Properties p = new Properties();
      try (Reader r = Files.newBufferedReader(f)) {
         p.load(r);
      } catch (IOException e) {
         return;
      }

      enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
      showMarker = Boolean.parseBoolean(p.getProperty("showMarker", "true"));
      showPanel = Boolean.parseBoolean(p.getProperty("showPanel", "true"));
      hotbarFirework = Boolean.parseBoolean(p.getProperty("hotbarFirework", "false"));

      try {
         fireworkInterval = Integer.parseInt(p.getProperty("fireworkInterval", "8").trim());
      } catch (NumberFormatException e) {
         fireworkInterval = 8;
      }
      try {
         minBoostSpeed = Integer.parseInt(p.getProperty("minBoostSpeed", "22").trim());
      } catch (NumberFormatException e) {
         minBoostSpeed = 22;
      }
      try {
         retreatDistance = Double.parseDouble(p.getProperty("retreatDistance", "2.0").trim());
      } catch (NumberFormatException e) {
         retreatDistance = 2.0;
      }

      fireworkInterval = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, fireworkInterval));
      minBoostSpeed = Math.max(0, Math.min(MAX_SPEED, minBoostSpeed));
      retreatDistance = Math.max(0.0, Math.min(MAX_RETREAT, retreatDistance));
   }

   public static void save() {
      Properties p = new Properties();
      p.setProperty("enabled", Boolean.toString(enabled));
      p.setProperty("fireworkInterval", Integer.toString(fireworkInterval));
      p.setProperty("minBoostSpeed", Integer.toString(minBoostSpeed));
      p.setProperty("retreatDistance", Double.toString(retreatDistance));
      p.setProperty("hotbarFirework", Boolean.toString(hotbarFirework));
      p.setProperty("showMarker", Boolean.toString(showMarker));
      p.setProperty("showPanel", Boolean.toString(showPanel));

      try (Writer w = Files.newBufferedWriter(file())) {
         p.store(w, "Elytra Hunter");
      } catch (IOException e) {
         // sessizce gec
      }
   }
}

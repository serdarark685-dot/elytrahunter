package com.elytrahunter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class HunterScreen extends Screen {
   private static final int ROWS = 8;
   private final List<ClickableWidget> items = new ArrayList<>();

   public HunterScreen() {
      super(Text.literal("Elytra Hunter"));
   }

   private static String onOff(boolean v) {
      return v ? "A\u00C7IK" : "KAPALI";
   }

   private static String sourceText() {
      return HunterConfig.hotbarFirework ? "HOTBAR" : "SOL EL";
   }

   private <T extends ClickableWidget> T add(T w) {
      this.items.add(w);
      return (T) this.addDrawableChild(w);
   }

   private int top() {
      return Math.max(30, this.height / 2 - 104);
   }

   @Override
   protected void init() {
      this.items.clear();
      int w = 200;
      int h = 20;
      int x = this.width / 2 - w / 2;
      int y = this.top();

      // 0) Acik / kapali
      this.add(ButtonWidget.builder(Text.literal("Durum: " + onOff(HunterConfig.enabled)), b -> {
         HunterConfig.enabled = !HunterConfig.enabled;
         b.setMessage(Text.literal("Durum: " + onOff(HunterConfig.enabled)));
      }).dimensions(x, y, w, h).build());

      // 1) Fisek kaynagi: sol el / hotbar
      this.add(ButtonWidget.builder(Text.literal("Fi\u015Fek kayna\u011F\u0131: " + sourceText()), b -> {
         HunterConfig.hotbarFirework = !HunterConfig.hotbarFirework;
         b.setMessage(Text.literal("Fi\u015Fek kayna\u011F\u0131: " + sourceText()));
      }).dimensions(x, y + 26, w, h).build());

      // 2) Fisek araligi (en az kac tick)
      this.add(new NumSlider(x, y + 52, w, h, HunterConfig.MIN_INTERVAL, HunterConfig.MAX_INTERVAL, 1, HunterConfig.fireworkInterval,
            v -> HunterConfig.fireworkInterval = (int) Math.round(v),
            v -> String.format(Locale.ROOT, "Fi\u015Fek aral\u0131\u011F\u0131: %d tick (%.1f/sn)", (int) Math.round(v), 20.0 / Math.max(1.0, v))));

      // 3) Hiz esigi (bu hizin altina dusunce yeni fisek)
      this.add(new NumSlider(x, y + 78, w, h, 0, HunterConfig.MAX_SPEED, 1, HunterConfig.minBoostSpeed,
            v -> HunterConfig.minBoostSpeed = (int) Math.round(v),
            v -> (int) Math.round(v) == 0 ? "Fi\u015Fek h\u0131z e\u015Fi\u011Fi: KAPALI"
                  : String.format(Locale.ROOT, "Fi\u015Fek h\u0131z e\u015Fi\u011Fi: %d blok/sn alt\u0131", (int) Math.round(v))));

      // 4) Uzaklasma mesafesi (0 = hic uzaklasma, dibinde dolas)
      this.add(new NumSlider(x, y + 104, w, h, 0.0, HunterConfig.MAX_RETREAT, 0.5, HunterConfig.retreatDistance,
            v -> HunterConfig.retreatDistance = v,
            v -> v < 0.25 ? "Uzakla\u015Fma: YOK (dibinde dola\u015F\u0131r)"
                  : String.format(Locale.ROOT, "Uzakla\u015Fma mesafesi: %.1f blok", v)));

      // 5) Hedef isareti
      this.add(ButtonWidget.builder(Text.literal("Hedef i\u015Fareti: " + onOff(HunterConfig.showMarker)), b -> {
         HunterConfig.showMarker = !HunterConfig.showMarker;
         b.setMessage(Text.literal("Hedef i\u015Fareti: " + onOff(HunterConfig.showMarker)));
      }).dimensions(x, y + 130, w, h).build());

      // 6) Mini sekme
      this.add(ButtonWidget.builder(Text.literal("Mini sekme: " + onOff(HunterConfig.showPanel)), b -> {
         HunterConfig.showPanel = !HunterConfig.showPanel;
         b.setMessage(Text.literal("Mini sekme: " + onOff(HunterConfig.showPanel)));
      }).dimensions(x, y + 156, w, h).build());

      // 7) Kapat
      this.add(ButtonWidget.builder(Text.literal("Kapat"), b -> this.close())
            .dimensions(x, y + 182, w, h).build());
   }

   @Override
   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      super.render(ctx, mouseX, mouseY, delta);
      int cx = this.width / 2;
      int y0 = this.top();

      for (ClickableWidget wdg : this.items) {
         HunterHud.frame(ctx, wdg.getX(), wdg.getY(), wdg.getX() + wdg.getWidth(), wdg.getY() + wdg.getHeight());
      }

      HunterHud.frame(ctx, cx - 112, y0 - 26, cx + 112, y0 + 26 * (ROWS - 1) + 20 + 8);
      String title = "ELYTRA HUNTER";
      int tw = this.textRenderer.getWidth(title);
      ctx.drawText(this.textRenderer, title, cx - tw / 2, y0 - 16, HunterHud.NEON, true);
   }

   @Override
   public void removed() {
      HunterConfig.save();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   /** Genel amacli kaydirma cubugu: min..max arasi, step adimli. */
   private static class NumSlider extends SliderWidget {
      private final double min;
      private final double max;
      private final double step;
      private final DoubleConsumer setter;
      private final DoubleFunction<String> label;

      NumSlider(int x, int y, int w, int h, double min, double max, double step, double current,
            DoubleConsumer setter, DoubleFunction<String> label) {
         super(x, y, w, h, Text.empty(), (current - min) / (max - min));
         this.min = min;
         this.max = max;
         this.step = step;
         this.setter = setter;
         this.label = label;
         this.updateMessage();
      }

      private double real() {
         double v = this.min + this.value * (this.max - this.min);
         v = Math.round(v / this.step) * this.step;
         return Math.max(this.min, Math.min(this.max, v));
      }

      @Override
      protected void updateMessage() {
         if (this.label == null) {
            return;
         }
         this.setMessage(Text.literal(this.label.apply(this.real())));
      }

      @Override
      protected void applyValue() {
         if (this.setter == null) {
            return;
         }
         this.setter.accept(this.real());
      }
   }
}

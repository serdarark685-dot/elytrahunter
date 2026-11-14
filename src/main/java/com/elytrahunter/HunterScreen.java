package com.elytrahunter;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Settings menu (open with Right Shift). Everything is saved when the screen closes. */
public class HunterScreen extends Screen {

    private final List<ClickableWidget> items = new ArrayList<>();
    private ButtonWidget toggleBtn;
    private ButtonWidget markerBtn;
    private ButtonWidget panelBtn;

    public HunterScreen() {
        super(Text.literal("Elytra Hunter"));
    }

    private static String onOff(boolean v) {
        return v ? "A\u00c7IK" : "KAPALI";
    }

    private <T extends ClickableWidget> T add(T w) {
        items.add(w);
        return addDrawableChild(w);
    }

    @Override
    protected void init() {
        items.clear();
        int w = 200;
        int h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 62;

        toggleBtn = add(ButtonWidget.builder(Text.literal("Durum: " + onOff(HunterConfig.enabled)), b -> {
            HunterConfig.enabled = !HunterConfig.enabled;
            b.setMessage(Text.literal("Durum: " + onOff(HunterConfig.enabled)));
        }).dimensions(x, y, w, h).build());

        add(new IntervalSlider(x, y + 26, w, h));

        markerBtn = add(ButtonWidget.builder(Text.literal("Hedef i\u015fareti: " + onOff(HunterConfig.showMarker)), b -> {
            HunterConfig.showMarker = !HunterConfig.showMarker;
            b.setMessage(Text.literal("Hedef i\u015fareti: " + onOff(HunterConfig.showMarker)));
        }).dimensions(x, y + 52, w, h).build());

        panelBtn = add(ButtonWidget.builder(Text.literal("Mini sekme: " + onOff(HunterConfig.showPanel)), b -> {
            HunterConfig.showPanel = !HunterConfig.showPanel;
            b.setMessage(Text.literal("Mini sekme: " + onOff(HunterConfig.showPanel)));
        }).dimensions(x, y + 78, w, h).build());

        add(ButtonWidget.builder(Text.literal("Kapat"), b -> this.close())
                .dimensions(x, y + 104, w, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // background + widgets

        int cx = this.width / 2;
        int y0 = this.height / 2 - 62;
        // neon outline around every widget
        for (ClickableWidget wdg : items) {
            HunterHud.frame(ctx, wdg.getX(), wdg.getY(), wdg.getX() + wdg.getWidth(), wdg.getY() + wdg.getHeight());
        }
        // neon window frame + title
        HunterHud.frame(ctx, cx - 112, y0 - 26, cx + 112, y0 + 134);
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

    /** 1..40 ticks between rockets. */
    private static class IntervalSlider extends SliderWidget {
        IntervalSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), toSlider(HunterConfig.fireworkInterval));
            updateMessage();
        }

        private static double toSlider(int ticks) {
            return (ticks - HunterConfig.MIN_INTERVAL) / (double) (HunterConfig.MAX_INTERVAL - HunterConfig.MIN_INTERVAL);
        }

        private static int fromSlider(double v) {
            return HunterConfig.MIN_INTERVAL
                    + (int) Math.round(v * (HunterConfig.MAX_INTERVAL - HunterConfig.MIN_INTERVAL));
        }

        @Override
        protected void updateMessage() {
            int ticks = fromSlider(this.value);
            setMessage(Text.literal(String.format(Locale.ROOT,
                    "Fi\u015fek aral\u0131\u011f\u0131: %d tick (%.1f/sn)", ticks, 20.0 / ticks)));
        }

        @Override
        protected void applyValue() {
            HunterConfig.fireworkInterval = fromSlider(this.value);
        }
    }
}

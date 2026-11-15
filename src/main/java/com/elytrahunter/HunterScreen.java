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
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Settings menu (open with Right Shift). Everything is saved when the screen closes. */
public class HunterScreen extends Screen {

    private final List<ClickableWidget> items = new ArrayList<>();

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

    /** Toggle button: label + state text, flips via the given action. */
    private ButtonWidget toggle(int x, int y, int w, int h, String label,
                                BooleanSupplier state, Runnable flip, String onText, String offText) {
        return add(ButtonWidget.builder(Text.literal(label + (state.getAsBoolean() ? onText : offText)), b -> {
            flip.run();
            b.setMessage(Text.literal(label + (state.getAsBoolean() ? onText : offText)));
        }).dimensions(x, y, w, h).build());
    }

    private int page = 0;
    private int frameTop;
    private int frameBottom;
    private int titleY;

    private static final String[] PREDICT_NAMES = {"KAPALI", "HUNTER", "\u00c7ARPAN"};
    private static final String[] AIM_NAMES = {"G\u00d6VDE", "G\u00d6Z"};

    /** Button that cycles through a fixed number of states. */
    private ButtonWidget cycle(int x, int y, int w, int h, String label, String[] names,
                               java.util.function.IntSupplier get, IntConsumer set) {
        return add(ButtonWidget.builder(Text.literal(label + names[get.getAsInt()]), b -> {
            set.accept((get.getAsInt() + 1) % names.length);
            b.setMessage(Text.literal(label + names[get.getAsInt()]));
        }).dimensions(x, y, w, h).build());
    }

    @Override
    protected void init() {
        items.clear();
        int w = 200;
        int h = 20;
        int s = 22;
        int x = this.width / 2 - w / 2;
        int rows = page == 0 ? 8 : 9;
        int y = this.height / 2 - (s * rows + h) / 2 + 8;
        titleY = y - 16;
        frameTop = y - 26;
        frameBottom = y + s * rows + h + 8;

        if (page == 0) {
            toggle(x, y, w, h, "Durum: ",
                    () -> HunterConfig.enabled, () -> HunterConfig.enabled = !HunterConfig.enabled,
                    "A\u00c7IK", "KAPALI");

            add(new IntSlider(x, y + s, w, h, HunterConfig.MIN_INTERVAL, HunterConfig.MAX_INTERVAL,
                    HunterConfig.fireworkInterval,
                    v -> String.format(Locale.ROOT, "Fi\u015fek aral\u0131\u011f\u0131: %d tick (%.1f/sn)", v, 20.0 / v),
                    v -> HunterConfig.fireworkInterval = v));

            // slider works in half-block steps: 1..60 -> 0.5..30.0 blocks
            add(new IntSlider(x, y + s * 2, w, h, 1, 60,
                    (int) Math.round(HunterConfig.retreatDistance * 2.0),
                    v -> String.format(Locale.ROOT, "Geri \u00e7ekilme mesafesi: %.1f blok", v / 2.0),
                    v -> HunterConfig.retreatDistance = v / 2.0));

            toggle(x, y + s * 3, w, h, "Geri \u00e7ekilirken fi\u015fek: ",
                    () -> HunterConfig.retreatFireworks, () -> HunterConfig.retreatFireworks = !HunterConfig.retreatFireworks,
                    "A\u00c7IK", "KAPALI");

            toggle(x, y + s * 4, w, h, "Fi\u015fek eli: ",
                    () -> HunterConfig.hotbarMode, () -> HunterConfig.hotbarMode = !HunterConfig.hotbarMode,
                    "HOTBAR", "SOL EL");

            toggle(x, y + s * 5, w, h, "RGB: ",
                    () -> HunterConfig.rgb, () -> HunterConfig.rgb = !HunterConfig.rgb,
                    "A\u00c7IK", "KAPALI");

            toggle(x, y + s * 6, w, h, "Hedef i\u015fareti: ",
                    () -> HunterConfig.showMarker, () -> HunterConfig.showMarker = !HunterConfig.showMarker,
                    "A\u00c7IK", "KAPALI");

            toggle(x, y + s * 7, w, h, "Mini sekme: ",
                    () -> HunterConfig.showPanel, () -> HunterConfig.showPanel = !HunterConfig.showPanel,
                    "A\u00c7IK", "KAPALI");
        } else {
            toggle(x, y, w, h, "Yumu\u015fak d\u00f6n\u00fc\u015f: ",
                    () -> HunterConfig.smoothRotation, () -> HunterConfig.smoothRotation = !HunterConfig.smoothRotation,
                    "A\u00c7IK", "KAPALI");

            add(new IntSlider(x, y + s, w, h, HunterConfig.MIN_ROT_SPEED, HunterConfig.MAX_ROT_SPEED,
                    HunterConfig.yawSpeed,
                    v -> "Yaw h\u0131z\u0131: " + v + " derece/tick",
                    v -> HunterConfig.yawSpeed = v));

            add(new IntSlider(x, y + s * 2, w, h, HunterConfig.MIN_ROT_SPEED, HunterConfig.MAX_ROT_SPEED,
                    HunterConfig.pitchSpeed,
                    v -> "Pitch h\u0131z\u0131: " + v + " derece/tick",
                    v -> HunterConfig.pitchSpeed = v));

            cycle(x, y + s * 3, w, h, "Hedef tahmini: ", PREDICT_NAMES,
                    () -> HunterConfig.predictMode, v -> HunterConfig.predictMode = v);

            // tenths: 5..30 -> 0.5..3.0
            add(new IntSlider(x, y + s * 4, w, h, 5, 30,
                    (int) Math.round(HunterConfig.predictMultiplier * 10.0),
                    v -> String.format(Locale.ROOT, "Tahmin \u00e7arpan\u0131: %.1f tick", v / 10.0),
                    v -> HunterConfig.predictMultiplier = v / 10.0));

            cycle(x, y + s * 5, w, h, "Ni\u015fan noktas\u0131: ", AIM_NAMES,
                    () -> HunterConfig.aimPoint, v -> HunterConfig.aimPoint = v);

            toggle(x, y + s * 6, w, h, "G\u00fcvenli mod: ",
                    () -> HunterConfig.safeMode, () -> HunterConfig.safeMode = !HunterConfig.safeMode,
                    "A\u00c7IK", "KAPALI");

            add(new IntSlider(x, y + s * 7, w, h, HunterConfig.MIN_FAR, HunterConfig.MAX_FAR,
                    HunterConfig.farDistance,
                    v -> "Uzak mesafe: " + v + " blok",
                    v -> HunterConfig.farDistance = v));

            add(new IntSlider(x, y + s * 8, w, h, HunterConfig.MIN_INTERVAL, HunterConfig.MAX_INTERVAL,
                    HunterConfig.farInterval,
                    v -> String.format(Locale.ROOT, "Uzak fi\u015fek aral\u0131\u011f\u0131: %d tick", v),
                    v -> HunterConfig.farInterval = v));
        }

        int by = y + s * rows;
        int half = (w - 4) / 2;
        add(ButtonWidget.builder(Text.literal(page == 0 ? "Sayfa 2 >" : "< Sayfa 1"), b -> {
            page = page == 0 ? 1 : 0;
            HunterConfig.save();
            this.clearAndInit();
        }).dimensions(x, by, half, h).build());
        add(ButtonWidget.builder(Text.literal("Kapat"), b -> this.close())
                .dimensions(x + half + 4, by, half, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // background + widgets
        HunterHud.updateColors();

        int cx = this.width / 2;
        // neon outline around every widget
        for (ClickableWidget wdg : items) {
            HunterHud.frame(ctx, wdg.getX(), wdg.getY(), wdg.getX() + wdg.getWidth(), wdg.getY() + wdg.getHeight());
        }
        // neon window frame + title
        HunterHud.frame(ctx, cx - 112, frameTop, cx + 112, frameBottom);
        String title = page == 0 ? "ELYTRA HUNTER" : "ELYTRA TARGET";
        int tw = this.textRenderer.getWidth(title);
        ctx.drawText(this.textRenderer, title, cx - tw / 2, titleY, HunterHud.NEON, true);
    }

    @Override
    public void removed() {
        HunterConfig.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Integer slider with a custom label. */
    private static class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final IntFunction<String> label;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int h, int min, int max, int current,
                  IntFunction<String> label, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (current - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        private int val() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label.apply(val())));
        }

        @Override
        protected void applyValue() {
            setter.accept(val());
        }
    }
}

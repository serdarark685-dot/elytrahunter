package com.elytrahunter;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Settings menu (open with Right Shift). Two dark panels: switches on the left,
 * sliders on the right. Click a header to collapse a panel. Saved when the screen closes.
 *
 * The vanilla widgets are only used for input (they are registered as selectable children and
 * never drawn); everything you see is painted in render().
 */
public class HunterScreen extends Screen {

    private static final int PW = 160;          // panel width
    private static final int HEADER_H = 24;
    private static final int GAP = 12;

    private static final int BG = 0xF2101728;
    private static final int OUTLINE = 0xFF1C2744;
    private static final int ACCENT = 0xFF2F6DF2;
    private static final int TEXT_ON = 0xFFFFFFFF;
    private static final int TEXT_OFF = 0xFFCDD3E4;
    private static final int TEXT_VALUE = 0xFF8E98B3;
    private static final int TEXT_VALUE_ON = 0xFFDCE6FF;

    private static final String[] PREDICT_NAMES = {"KAPALI", "HUNTER", "\u00c7ARPAN"};
    private static final String[] AIM_NAMES = {"G\u00d6VDE", "G\u00d6Z"};

    // Inter font bundled in assets/elytrahunter/font (ui.json = Medium, ui_bold.json = SemiBold)
    private static final Style FONT = Style.EMPTY
            .withFont(new StyleSpriteSource.Font(Identifier.of("elytrahunter", "ui")));
    private static final Style FONT_BOLD = Style.EMPTY
            .withFont(new StyleSpriteSource.Font(Identifier.of("elytrahunter", "ui_bold")));

    private static Text txt(String s) {
        return Text.literal(s).setStyle(FONT);
    }

    private static Text bold(String s) {
        return Text.literal(s).setStyle(FONT_BOLD);
    }

    private final List<Panel> panels = new ArrayList<>();
    private final boolean[] collapsed = new boolean[2];
    private int rowH = 16;
    private int step = 18;

    public HunterScreen() {
        super(Text.literal("Elytra Hunter"));
    }

    // ------------------------------------------------------------------ model
    private static final class Row {
        final ClickableWidget widget;
        final String name;
        final Supplier<String> value;   // right-hand text, may be null
        final BooleanSupplier on;       // highlighted row when true, null for non-switches
        final IntSlider slider;         // non-null for slider rows

        Row(ClickableWidget widget, String name, Supplier<String> value, BooleanSupplier on, IntSlider slider) {
            this.widget = widget;
            this.name = name;
            this.value = value;
            this.on = on;
            this.slider = slider;
        }
    }

    private static final class Panel {
        final String title;
        final int index;
        final List<Row> rows = new ArrayList<>();
        int x;
        int y;
        int fullH;

        Panel(String title, int index) {
            this.title = title;
            this.index = index;
        }
    }

    // ------------------------------------------------------------------ row builders
    private int rowX(Panel p) {
        return p.x + 5;
    }

    private int rowY(Panel p) {
        return p.y + HEADER_H + 6 + p.rows.size() * step;
    }

    private void switchRow(Panel p, String name, BooleanSupplier get, Runnable flip) {
        ButtonWidget bw = ButtonWidget.builder(Text.empty(), b -> flip.run())
                .dimensions(rowX(p), rowY(p), PW - 10, rowH).build();
        addSelectableChild(bw);
        p.rows.add(new Row(bw, name, null, get, null));
    }

    private void cycleRow(Panel p, String name, Supplier<String> valueText, Runnable next) {
        ButtonWidget bw = ButtonWidget.builder(Text.empty(), b -> next.run())
                .dimensions(rowX(p), rowY(p), PW - 10, rowH).build();
        addSelectableChild(bw);
        p.rows.add(new Row(bw, name, valueText, null, null));
    }

    private void sliderRow(Panel p, String name, int min, int max, int current,
                           IntFunction<String> valueText, IntConsumer setter) {
        IntSlider sl = new IntSlider(rowX(p), rowY(p), PW - 10, rowH, min, max, current, valueText, setter);
        addSelectableChild(sl);
        p.rows.add(new Row(sl, name, () -> valueText.apply(sl.intValue()), null, sl));
    }

    // ------------------------------------------------------------------ layout
    @Override
    protected void init() {
        panels.clear();

        int maxRows = 11;
        int avail = this.height - 20;
        rowH = Math.max(13, Math.min(18, (avail - HEADER_H - 8) / maxRows - 2));
        step = rowH + 2;
        int top = Math.max(6, (this.height - (HEADER_H + 8 + maxRows * step)) / 2);
        int px = Math.max(4, this.width / 2 - (PW * 2 + GAP) / 2);

        Panel a = new Panel("Elytra Hunter", 0);
        a.x = px;
        a.y = top;
        Panel b = new Panel("Ayarlar", 1);
        b.x = px + PW + GAP;
        b.y = top;

        // ---- left panel: switches
        switchRow(a, "Durum",
                () -> HunterConfig.enabled, () -> HunterConfig.enabled = !HunterConfig.enabled);
        switchRow(a, "Ar\u0131 modu",
                () -> HunterConfig.beeMode, () -> HunterConfig.beeMode = !HunterConfig.beeMode);
        switchRow(a, "Yumu\u015fak d\u00f6n\u00fc\u015f",
                () -> HunterConfig.smoothRotation, () -> HunterConfig.smoothRotation = !HunterConfig.smoothRotation);
        switchRow(a, "G\u00fcvenli mod",
                () -> HunterConfig.safeMode, () -> HunterConfig.safeMode = !HunterConfig.safeMode);
        switchRow(a, "Geri \u00e7ekilirken fi\u015fek",
                () -> HunterConfig.retreatFireworks, () -> HunterConfig.retreatFireworks = !HunterConfig.retreatFireworks);
        cycleRow(a, "Fi\u015fek eli",
                () -> HunterConfig.hotbarMode ? "HOTBAR" : "SOL EL",
                () -> HunterConfig.hotbarMode = !HunterConfig.hotbarMode);
        cycleRow(a, "Hedef tahmini",
                () -> PREDICT_NAMES[HunterConfig.predictMode],
                () -> HunterConfig.predictMode = (HunterConfig.predictMode + 1) % PREDICT_NAMES.length);
        cycleRow(a, "Ni\u015fan noktas\u0131",
                () -> AIM_NAMES[HunterConfig.aimPoint],
                () -> HunterConfig.aimPoint = (HunterConfig.aimPoint + 1) % AIM_NAMES.length);
        switchRow(a, "Hedef i\u015fareti",
                () -> HunterConfig.showMarker, () -> HunterConfig.showMarker = !HunterConfig.showMarker);
        switchRow(a, "Mini sekme",
                () -> HunterConfig.showPanel, () -> HunterConfig.showPanel = !HunterConfig.showPanel);
        switchRow(a, "RGB",
                () -> HunterConfig.rgb, () -> HunterConfig.rgb = !HunterConfig.rgb);

        // ---- right panel: sliders
        sliderRow(b, "Fi\u015fek aral\u0131\u011f\u0131", HunterConfig.MIN_INTERVAL, HunterConfig.MAX_INTERVAL,
                HunterConfig.fireworkInterval,
                v -> v + " tick",
                v -> HunterConfig.fireworkInterval = v);
        // half-block steps: 1..60 -> 0.5..30.0 blocks
        sliderRow(b, "Geri \u00e7ekilme", 1, 60,
                (int) Math.round(HunterConfig.retreatDistance * 2.0),
                v -> String.format(Locale.ROOT, "%.1f blok", v / 2.0),
                v -> HunterConfig.retreatDistance = v / 2.0);
        sliderRow(b, "Yaw h\u0131z\u0131", HunterConfig.MIN_ROT_SPEED, HunterConfig.MAX_ROT_SPEED,
                HunterConfig.yawSpeed,
                v -> v + "\u00b0/t",
                v -> HunterConfig.yawSpeed = v);
        sliderRow(b, "Pitch h\u0131z\u0131", HunterConfig.MIN_ROT_SPEED, HunterConfig.MAX_ROT_SPEED,
                HunterConfig.pitchSpeed,
                v -> v + "\u00b0/t",
                v -> HunterConfig.pitchSpeed = v);
        // tenths: 5..30 -> 0.5..3.0
        sliderRow(b, "Tahmin \u00e7arpan\u0131", 5, 30,
                (int) Math.round(HunterConfig.predictMultiplier * 10.0),
                v -> String.format(Locale.ROOT, "%.1fx", v / 10.0),
                v -> HunterConfig.predictMultiplier = v / 10.0);
        sliderRow(b, "Uzak mesafe", HunterConfig.MIN_FAR, HunterConfig.MAX_FAR,
                HunterConfig.farDistance,
                v -> v + " blok",
                v -> HunterConfig.farDistance = v);
        sliderRow(b, "Uzak aral\u0131k", HunterConfig.MIN_INTERVAL, HunterConfig.MAX_INTERVAL,
                HunterConfig.farInterval,
                v -> v + " tick",
                v -> HunterConfig.farInterval = v);

        a.fullH = HEADER_H + 6 + a.rows.size() * step + 2;
        b.fullH = HEADER_H + 6 + b.rows.size() * step + 2;
        panels.add(a);
        panels.add(b);

        // header buttons collapse / expand the panel
        for (Panel p : panels) {
            ButtonWidget hb = ButtonWidget.builder(Text.empty(), btn -> collapsed[p.index] = !collapsed[p.index])
                    .dimensions(p.x, p.y, PW, HEADER_H).build();
            addSelectableChild(hb);
        }
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // dimmed background only, widgets are not drawn
        HunterHud.updateColors();
        int accent = HunterConfig.rgb ? HunterHud.NEON : ACCENT;
        for (Panel p : panels) {
            drawPanel(ctx, p, mouseX, mouseY, accent);
        }
    }

    private void drawPanel(DrawContext ctx, Panel p, int mouseX, int mouseY, int accent) {
        boolean col = collapsed[p.index];
        int ph = col ? HEADER_H : p.fullH;

        roundRect(ctx, p.x - 1, p.y - 1, p.x + PW + 1, p.y + ph + 1, 7, OUTLINE);
        roundRect(ctx, p.x, p.y, p.x + PW, p.y + ph, 6, BG);
        if (!col) {
            waves(ctx, p, ph);
        }

        // header
        if (mouseX >= p.x && mouseX < p.x + PW && mouseY >= p.y && mouseY < p.y + HEADER_H) {
            roundRect(ctx, p.x + 2, p.y + 2, p.x + PW - 2, p.y + HEADER_H - 2, 5, 0x14FFFFFF);
        }
        drawIcon(ctx, p.x + 16, p.y + HEADER_H / 2, 0xFFE6EAF5);
        ctx.drawText(this.textRenderer, bold(p.title), p.x + 30, p.y + (HEADER_H - 7) / 2, TEXT_ON, false);
        drawChevron(ctx, p.x + PW - 14, p.y + HEADER_H / 2, !col, 0xFFB4BCD2);

        for (Row row : p.rows) {
            row.widget.visible = !col;
            if (col) continue;
            drawRow(ctx, row, mouseX, mouseY, accent);
        }
    }

    private void drawRow(DrawContext ctx, Row row, int mouseX, int mouseY, int accent) {
        ClickableWidget w = row.widget;
        int rx = w.getX();
        int ry = w.getY();
        int rw = w.getWidth();
        int rh = w.getHeight();
        boolean hover = mouseX >= rx && mouseX < rx + rw && mouseY >= ry && mouseY < ry + rh;
        boolean on = row.on != null && row.on.getAsBoolean();

        if (row.slider != null) {
            roundRect(ctx, rx, ry, rx + rw, ry + rh, 4, hover ? 0x26FFFFFF : 0x16FFFFFF);
            int fw = (int) Math.round(row.slider.fraction() * rw);
            if (fw > 0) {
                roundRect(ctx, rx, ry, rx + Math.max(fw, 8), ry + rh, 4, (accent & 0x00FFFFFF) | 0xB0000000);
            }
        } else if (on) {
            roundRect(ctx, rx, ry, rx + rw, ry + rh, 4, hover ? lighten(accent) : accent);
        } else if (hover) {
            roundRect(ctx, rx, ry, rx + rw, ry + rh, 4, 0x22FFFFFF);
        }

        boolean bright = on || row.slider != null;
        int ty = ry + (rh - 7) / 2;
        ctx.drawText(this.textRenderer, txt(row.name), rx + 7, ty, bright ? TEXT_ON : TEXT_OFF, false);
        if (row.value != null) {
            Text v = txt(row.value.get());
            int vw = this.textRenderer.getWidth(v);
            ctx.drawText(this.textRenderer, v, rx + rw - 6 - vw, ty, bright ? TEXT_VALUE_ON : TEXT_VALUE, false);
        }
    }

    /** Faint wavy lines behind the rows. */
    private void waves(DrawContext ctx, Panel p, int ph) {
        int x1 = p.x + 3;
        int x2 = p.x + PW - 3;
        int yTop = p.y + HEADER_H + 2;
        int yBot = p.y + ph - 3;
        for (int k = 0; k < 10; k++) {
            double base = yTop + 6 + k * 24;
            for (int x = x1; x < x2; x++) {
                int yy = (int) Math.round(base + 5.0 * Math.sin((x - p.x) * 0.055 + k * 1.1));
                if (yy > yTop && yy < yBot) {
                    ctx.fill(x, yy, x + 1, yy + 1, 0x0EFFFFFF);
                }
            }
        }
    }

    /** Crosshair-style icon: ring, centre dot, four ticks. */
    private static void drawIcon(DrawContext ctx, int cx, int cy, int color) {
        for (int i = 0; i < 20; i++) {
            double a = i / 20.0 * Math.PI * 2.0;
            int x = cx + (int) Math.round(Math.cos(a) * 5.0);
            int y = cy + (int) Math.round(Math.sin(a) * 5.0);
            ctx.fill(x, y, x + 1, y + 1, color);
        }
        ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, color);
        ctx.fill(cx - 8, cy, cx - 6, cy + 1, color);
        ctx.fill(cx + 6, cy, cx + 8, cy + 1, color);
        ctx.fill(cx, cy - 8, cx + 1, cy - 6, color);
        ctx.fill(cx, cy + 6, cx + 1, cy + 8, color);
    }

    /** Small chevron: down when expanded, right when collapsed. */
    private static void drawChevron(DrawContext ctx, int cx, int cy, boolean expanded, int color) {
        for (int i = 0; i < 5; i++) {
            if (expanded) {
                ctx.fill(cx - 4 + i, cy - 2 + i, cx - 3 + i, cy - 1 + i, color);
                ctx.fill(cx + 4 - i, cy - 2 + i, cx + 5 - i, cy - 1 + i, color);
            } else {
                ctx.fill(cx - 2 + i, cy - 4 + i, cx - 1 + i, cy - 3 + i, color);
                ctx.fill(cx - 2 + i, cy + 4 - i, cx - 1 + i, cy + 5 - i, color);
            }
        }
    }

    /** Filled rectangle with rounded corners. */
    private static void roundRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            ctx.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            ctx.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    private static int lighten(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        r += (255 - r) / 6;
        g += (255 - g) / 6;
        b += (255 - b) / 6;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ screen basics
    @Override
    public void removed() {
        HunterConfig.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Integer slider; the label function now only gives the value text (drawing is done above). */
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

        int intValue() {
            return min + (int) Math.round(this.value * (max - min));
        }

        double fraction() {
            return (intValue() - min) / (double) (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label.apply(intValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(intValue());
        }
    }
}

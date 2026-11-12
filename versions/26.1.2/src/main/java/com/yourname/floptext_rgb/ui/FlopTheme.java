package com.yourname.floptext_rgb.ui;

import com.yourname.floptext_rgb.math.GradientCalculator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * One shared look for every FlopText RGB screen: neutral charcoal panels, a single blue accent,
 * and one flat button style. Buttons stay ordinary vanilla widgets (so clicks, focus and narration
 * are unchanged); {@link #paintButtons} simply repaints them in this style right after the screen
 * has drawn its widgets.
 */
public final class FlopTheme {
    public static final int TEXT = 0xFFEDEFF2;
    public static final int TEXT_MUTED = 0xFFA9AFB8;
    public static final int TEXT_DISABLED = 0xFF6C727B;
    public static final int ACCENT = 0xFF7AA7FF;
    public static final int PANEL_EDGE = 0xFF454545;

    /** Backdrop used behind very dark gradients so they stay readable on the dark UI. */
    public static final int PREVIEW_CHIP = 0xFF868E98;

    public enum Kind { NORMAL, PRIMARY, DANGER }

    private FlopTheme() { }

    /** Picks a button role from its label: confirming actions are blue, destructive ones are red. */
    public static Kind kindFor(String label) {
        if (label == null) return Kind.NORMAL;
        String l = label.trim();
        if (l.equals("Del") || l.equals("X") || l.equals("Remove") || l.equals("Remove Color")
            || l.startsWith("Delete")) {
            return Kind.DANGER;
        }
        if (l.equals("Load") || l.equals("Apply") || l.equals("Apply Hex") || l.equals("Apply Color")
            || l.equals("Copy") || l.equals("Copy To Clipboard") || l.equals("Create Folder")
            || l.equals("Add Preset") || l.startsWith("Save")) {
            return Kind.PRIMARY;
        }
        return Kind.NORMAL;
    }

    /** Draws one flat button. {@code selected} is for toggles and active filter chips. */
    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, Component label,
                              Kind kind, boolean hovered, boolean active, boolean selected) {
        if (w <= 0 || h <= 0) return;
        int fill;
        int border;
        int text;
        if (!active) {
            fill = 0xFF1C1F24;
            border = 0xFF30353C;
            text = TEXT_DISABLED;
        } else if (selected) {
            fill = hovered ? 0xFF34476B : 0xFF2B3A55;
            border = ACCENT;
            text = TEXT;
        } else if (kind == Kind.PRIMARY) {
            fill = hovered ? 0xFF325FA3 : 0xFF284B80;
            border = hovered ? 0xFF9CC0FF : 0xFF4C7BC0;
            text = 0xFFFFFFFF;
        } else if (kind == Kind.DANGER) {
            fill = hovered ? 0xFF5A2D33 : 0xFF3E2428;
            border = hovered ? 0xFFC0636C : 0xFF7A3B43;
            text = 0xFFFFC9C9;
        } else {
            fill = hovered ? 0xFF363C46 : 0xFF2A2E35;
            border = hovered ? ACCENT : 0xFF4B525C;
            text = hovered ? 0xFFFFFFFF : TEXT;
        }
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (active && h >= 8) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x22FFFFFF);
            if (selected) {
                g.fill(x + 1, y + h - 3, x + w - 1, y + h - 2, ACCENT);
            } else {
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x33000000);
            }
        }
        g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, text);
    }

    /** Repaints every visible vanilla {@link Button} on the screen in the FlopText style. */
    public static void paintButtons(GuiGraphicsExtractor g, Font font, Screen screen, int mouseX, int mouseY) {
        paintButtons(g, font, screen, mouseX, mouseY, null);
    }

    /** Same as above; {@code selected} marks toggle buttons that are currently switched on. */
    public static void paintButtons(GuiGraphicsExtractor g, Font font, Screen screen, int mouseX, int mouseY,
                                    Predicate<Button> selected) {
        for (var child : screen.children()) {
            if (!(child instanceof Button b) || !b.visible) continue;
            int x = b.getX();
            int y = b.getY();
            int w = b.getWidth();
            int h = b.getHeight();
            boolean hovered = b.active && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            Component label = b.getMessage();
            button(g, font, x, y, w, h, label, kindFor(label.getString()), hovered, b.active,
                selected != null && selected.test(b));
        }
    }

    /** True when a gradient is dark enough that it needs a lighter backdrop to be readable. */
    public static boolean isDark(List<GradientCalculator.ColorStop> stops) {
        if (stops == null || stops.isEmpty()) return false;
        double total = 0;
        for (GradientCalculator.ColorStop stop : stops) {
            int rgb = stop.rgb();
            double r = (rgb >> 16) & 255;
            double gr = (rgb >> 8) & 255;
            double b = rgb & 255;
            total += (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
        }
        return total / stops.size() < 0.30;
    }

    /** A light, softly rounded strip drawn behind a preview line of very dark text. */
    public static void previewStrip(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        if (x2 - x1 < 4 || y2 - y1 < 4) return;
        g.fill(x1 + 1, y1, x2 - 1, y2, PREVIEW_CHIP);
        g.fill(x1, y1 + 1, x2, y2 - 1, PREVIEW_CHIP);
    }
}

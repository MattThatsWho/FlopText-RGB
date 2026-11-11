package com.yourname.floptext_rgb.library;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractWidget;

/** Shared styling and layout helpers for the recovered library screens. */
final class LibraryUi {
    private LibraryUi() { }
    static Button button(java.util.function.Consumer<Button> register, String title, int x, int y, int w, int h, Button.OnPress action) {
        Button b = Button.builder(Component.literal(title), action).bounds(x, y, w, h).build();
        register.accept(b);
        return b;
    }
    static void label(GuiGraphicsExtractor c, Font renderer, String text, int x, int y, int color) { c.text(renderer, Component.literal(text), x, y, color); }
    static void panel(GuiGraphicsExtractor c, int x, int y, int w, int h) {
        // Exact charcoal panel and neutral border colors from the 1.21.11 LibraryUi.
        c.fill(x, y, x + w, y + h, 0xC5101010);
        c.fill(x, y, x + w, y + 1, 0xFF454545); c.fill(x, y + h - 1, x + w, y + h, 0xFF454545);
        c.fill(x, y, x + 1, y + h, 0xFF454545); c.fill(x + w - 1, y, x + w, y + h, 0xFF454545);
    }
    static void divider(GuiGraphicsExtractor c, int x, int y, int w) { c.fill(x, y, x + w, y + 1, 0xFF454545); }
    static String safeFile(String value) { if (value == null) return "floptext-rgb"; return value.replaceAll("[^A-Za-z0-9._ -]", "_").trim(); }
    static boolean fullyVisible(int x, int y, int w, int h) { Screen s = Minecraft.getInstance().screen; return s != null && x >= 0 && y >= 0 && x + w <= s.width && y + h <= s.height; }
    /** Pixel-perfect compact scrollbar used by the original Library screen.
     * Arguments after the viewport are scroll offset and maximum scroll amount.
     */
    static void scrollBar(GuiGraphicsExtractor c, int x, int top, int bottom, int scroll, int scrollMax) {
        if (scrollMax <= 0 || bottom <= top) return;
        int track = bottom - top;
        int thumb = Math.max(18, (int) ((long) track * track / Math.max(1, track + scrollMax)));
        thumb = Math.min(track, thumb);
        int thumbY = top + (int) ((long) (track - thumb) * Math.max(0, scroll) / scrollMax);
        c.fill(x - 3, top, x, bottom, 0x66000000);
        c.fill(x - 3, thumbY, x, thumbY + thumb, 0xFF78AFFF);
    }
    static void hideAll(Screen screen) { for (var child : screen.children()) if (child instanceof AbstractWidget w) w.visible = false; }
    static void noOpBlur(Screen screen, GuiGraphicsExtractor context) { /* Avoid duplicate blur calls while a modal is being drawn. */ }
}

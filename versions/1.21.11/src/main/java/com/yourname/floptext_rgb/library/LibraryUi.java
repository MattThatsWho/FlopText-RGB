package com.yourname.floptext_rgb.library;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ClickableWidget;

/** Shared styling and layout helpers for the recovered library screens. */
final class LibraryUi {
    private LibraryUi() { }
    static ButtonWidget button(Screen screen, String title, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(title), action).dimensions(x, y, w, h).build();
        screen.addDrawableChild(b); return b;
    }
    static void label(DrawContext c, TextRenderer renderer, String text, int x, int y, int color) { c.drawTextWithShadow(renderer, text, x, y, color); }
    static void panel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xFF111B2B);
        c.fill(x, y, x + w, y + 1, 0xFF2B4264); c.fill(x, y + h - 1, x + w, y + h, 0xFF2B4264);
        c.fill(x, y, x + 1, y + h, 0xFF2B4264); c.fill(x + w - 1, y, x + w, y + h, 0xFF2B4264);
    }
    static void divider(DrawContext c, int x, int y, int w) { c.fill(x, y, x + w, y + 1, 0xFF2B4264); }
    static String safeFile(String value) { if (value == null) return "floptext-rgb"; return value.replaceAll("[^A-Za-z0-9._ -]", "_").trim(); }
    static boolean fullyVisible(int x, int y, int w, int h) { Screen s = MinecraftClient.getInstance().currentScreen; return s != null && x >= 0 && y >= 0 && x + w <= s.width && y + h <= s.height; }
    static void scrollBar(DrawContext c, int x, int top, int bottom, int content, int scroll) {
        if (content <= bottom - top || bottom <= top) return;
        c.fill(x, top, x + 3, bottom, 0xFF1D2B40);
        int track = bottom - top, thumb = Math.max(16, track * track / content);
        int maxScroll = content - track; int thumbY = top + (track - thumb) * Math.max(0, scroll) / Math.max(1, maxScroll);
        c.fill(x, thumbY, x + 3, thumbY + thumb, 0xFF5E8DC8);
    }
    static void hideAll(Screen screen) { for (var child : screen.children()) if (child instanceof ClickableWidget w) w.visible = false; }
    static void noOpBlur(Screen screen, DrawContext context) { /* Avoid duplicate blur calls while a modal is being drawn. */ }
}

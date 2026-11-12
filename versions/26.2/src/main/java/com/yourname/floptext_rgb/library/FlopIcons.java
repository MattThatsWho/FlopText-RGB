package com.yourname.floptext_rgb.library;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small pixel-pattern icons; silhouettes do not depend on font glyphs. */
public final class FlopIcons {
    // Original FlopIcons pixel maps recovered from the v1.7.0/1.21.11 JAR.
    static final String[] GEAR = {
        ".....###.....", "...#.###.#...", "..#########..", ".###########.",
        "..###...###..", "####.....####", "####.....####", "####.....####",
        "..###...###..", ".###########.", "..#########..", "...#.###.#...", ".....###....."
    };
    static final String[] BOOK = {
        "..#####.#####..", ".#....#.#....#.", "#.###.#.#.###.#", "#.....#.#.....#",
        "#.###.#.#.###.#", "#.....#.#.....#", "#.###.#.#.###.#", "#.....#.#.....#",
        ".######.######.", "###############"
    };
    static final String[] PERSON = {
        "....####....", "...######...", "...######...", "...######...", "....####....",
        ".....##.....", "..########..", ".##########.", "############", "############"
    };

    private FlopIcons() { }

    static void bitmap(GuiGraphicsExtractor context, String[] pixels, int x, int y, int scale) {
        bitmap(context, pixels, x, y, scale, 0xFFE9F2FF);
    }

    static void bitmap(GuiGraphicsExtractor context, String[] pixels, int x, int y, int scale, int color) {
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length(); col++) {
                if (pixels[row].charAt(col) == '#') {
                    context.fill(x + col * scale, y + row * scale,
                        x + (col + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }

    static void centered(GuiGraphicsExtractor context, String[] pixels, int x, int y,
                         int width, int height, int color) {
        int scale = Math.max(1, Math.min(width / Math.max(1, pixels[0].length()),
            height / Math.max(1, pixels.length)));
        bitmap(context, pixels,
            x + (width - pixels[0].length() * scale) / 2,
            y + (height - pixels.length * scale) / 2, scale, color);
    }

    static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }

    /** Draw the v1.7.0 gear in its dedicated top-left hitbox. */
    public static void drawMain(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        drawSettings(context, mouseX, mouseY);
    }

    public static void drawSettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int x = 18, y = 14;
        boolean hover = inside(mouseX, mouseY, x, y, 24, 24);
        // The original draws the gear directly on the editor's panel—there is no
        // extra button tile behind it. Only the pixel tint changes on hover.
        centered(context, GEAR, x, y, 24, 24, hover ? -96 : -1);
        if (hover) {
            panel(context, x + 30, y + 2, 68, 18);
            context.text(net.minecraft.client.Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.literal("Settings"), x + 35, y + 7, 0xFFFFFFFF);
        }
    }

    /** Draws the Library book over the sidebar's blank button widget. */
    public static void drawLibrary(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, 24, 24);
        centered(context, BOOK, x, y, 24, 24, hover ? -96 : -1);
        if (hover) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int labelWidth = font.width("Library") + 12;
            int labelX = Math.max(4, x + 24 - labelWidth);
            LibraryUi.panel(context, labelX, y + 27, labelWidth, 18);
            context.text(font, net.minecraft.network.chat.Component.literal("Library"), labelX + 6, y + 32, -1);
        }
    }

    private static void panel(GuiGraphicsExtractor c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xB90B0B0B);
        c.fill(x, y, x + w, y + 1, 0xFF3A3A3A);
        c.fill(x, y + h - 1, x + w, y + h, 0xFF3A3A3A);
        c.fill(x, y, x + 1, y + h, 0xFF3A3A3A);
        c.fill(x + w - 1, y, x + w, y + h, 0xFF3A3A3A);
    }
}

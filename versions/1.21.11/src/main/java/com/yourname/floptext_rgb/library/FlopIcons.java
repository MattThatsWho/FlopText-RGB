package com.yourname.floptext_rgb.library;

import net.minecraft.client.gui.DrawContext;

/** Small pixel-pattern icons; silhouettes do not depend on font glyphs. */
public final class FlopIcons {
    static final String[] GEAR = {"..###..", ".#####.", "##.#.##", "#######", "##.#.##", ".#####.", "..###.."};
    static final String[] BOOK = {"##...##", "###.###", "#######", "##.#.##", "##.#.##", "##.#.##", "#######"};
    static final String[] PERSON = {"..###..", ".#####.", ".#####.", "...#...", ".#####.", "#######", "##...##"};

    private FlopIcons() { }

    static void bitmap(DrawContext context, String[] pixels, int x, int y, int scale) {
        bitmap(context, pixels, x, y, scale, 0xFFE9F2FF);
    }

    static void bitmap(DrawContext context, String[] pixels, int x, int y, int scale, int color) {
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length(); col++) {
                if (pixels[row].charAt(col) == '#') {
                    context.fill(x + col * scale, y + row * scale,
                        x + (col + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }

    static void centered(DrawContext context, String[] pixels, int x, int y,
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

    public static void drawMain(DrawContext context, int mouseX, int mouseY) {
        var screen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
        if (screen == null || screen.width < 100) return;
        int x = screen.width - 25, y = 12;
        int color = inside(mouseX, mouseY, x - 2, y - 2, 18, 18)
            ? 0xFF75B8FF : 0xFFD7E6FF;
        centered(context, GEAR, x - 1, y - 1, 16, 16, color);
    }
}

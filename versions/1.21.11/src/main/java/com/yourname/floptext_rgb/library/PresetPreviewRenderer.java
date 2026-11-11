package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.math.GradientCalculator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/** Character-by-character gradient preview reconstruction. */
public final class PresetPreviewRenderer {
    private PresetPreviewRenderer() { }

    public static void draw(DrawContext context, TextRenderer renderer, String ignoredPrefix,
                            int x, int y, GradientPreset preset, int maxWidth) {
        String input = preset == null || preset.inputText == null ? "" : preset.inputText;
        if (input.isBlank()) input = "Preview";
        int[] chars = input.codePoints().toArray();
        if (chars.length == 0) return;
        List<GradientCalculator.ColorStop> stops = preset == null ? List.of()
            : GradientCalculator.sorted(preset.toStops());
        int cursorX = x;
        int limitX = x + Math.max(0, maxWidth);
        for (int i = 0; i < chars.length; i++) {
            String character = new String(Character.toChars(chars[i]));
            int color = stops.isEmpty() ? 0xFFAAAAAA
                : 0xFF000000 | GradientCalculator.colorAt(stops,
                    GradientCalculator.charPosition(i, chars.length));
            Text styled = Text.literal(character).styled(style -> style
                .withColor(color)
                .withBold(preset != null && preset.bold)
                .withItalic(preset != null && preset.italic)
                .withStrikethrough(preset != null && preset.strikethrough)
                .withUnderline(preset != null && preset.underline));
            int charWidth = renderer.getWidth(styled);
            if (cursorX + charWidth > limitX) {
                if (cursorX + renderer.getWidth("...") <= limitX) {
                    context.drawText(renderer, "...", cursorX, y, 0xFFAAAAAA, false);
                }
                return;
            }
            context.drawText(renderer, styled, cursorX, y, color, false);
            cursorX += charWidth;
        }
    }
}

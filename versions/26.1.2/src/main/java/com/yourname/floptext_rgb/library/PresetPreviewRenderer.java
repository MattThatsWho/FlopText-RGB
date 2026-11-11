package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.math.GradientCalculator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Character-by-character gradient preview reconstruction. */
public final class PresetPreviewRenderer {
    private PresetPreviewRenderer() { }

    public static void draw(GuiGraphicsExtractor context, Font renderer, String ignoredPrefix,
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
            int rgb = stops.isEmpty() ? 0xAAAAAA
                : GradientCalculator.colorAt(stops,
                    GradientCalculator.charPosition(i, chars.length));
            Component styled = Component.literal(character).withStyle(style -> style
                .withColor(rgb)
                .withBold(preset != null && preset.bold)
                .withItalic(preset != null && preset.italic)
                .withStrikethrough(preset != null && preset.strikethrough)
                .withUnderlined(preset != null && preset.underline));
            int charWidth = renderer.width(styled);
            if (cursorX + charWidth > limitX) {
                if (cursorX + renderer.width("...") <= limitX) {
                    context.text(renderer, Component.literal("..."), cursorX, y, 0xFFAAAAAA, false);
                }
                return;
            }
            // GuiGraphicsExtractor uses ARGB since 1.21.6; raw RGB has alpha=0 and is invisible.
            context.text(renderer, styled, cursorX, y, 0xFF000000 | (rgb & 0xFFFFFF), false);
            cursorX += charWidth;
        }
    }
}

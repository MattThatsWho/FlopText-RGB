package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;

/** Directly transfers a gradient between the editor and preset data. */
public final class PresetLoadSupport {
    private PresetLoadSupport() { }

    public static GradientPreset snapshot(Object owner, String name) {
        if (owner instanceof FlopTextRGBScreen editor) return editor.snapshotCurrentPreset(name);
        throw new IllegalArgumentException("Owner is not a FlopText RGB editor screen.");
    }

    public static void applyAndReturn(Object owner, GradientPreset preset) {
        if (!(owner instanceof FlopTextRGBScreen editor)) {
            throw new IllegalArgumentException("Owner is not a FlopText RGB editor screen.");
        }
        editor.applyPreset(preset);
        MinecraftClient.getInstance().setScreen(editor);
    }
}

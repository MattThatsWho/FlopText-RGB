package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Reconstructed click action used to enter the preset library from the editor. */
public final class OpenLibraryAction implements ButtonWidget.PressAction {
    private final FlopTextRGBScreen parent;
    public OpenLibraryAction(FlopTextRGBScreen parent) { this.parent = parent; }
    @Override public void onPress(ButtonWidget button) {
        PresetLibraryStore.reload();
        MinecraftClient.getInstance().setScreen(new PresetLibraryScreen(parent));
    }
}

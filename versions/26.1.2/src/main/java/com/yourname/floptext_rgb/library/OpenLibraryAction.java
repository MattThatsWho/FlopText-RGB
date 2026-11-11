package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

/** Reconstructed click action used to enter the preset library from the editor. */
public final class OpenLibraryAction implements Button.OnPress {
    private final FlopTextRGBScreen parent;
    public OpenLibraryAction(FlopTextRGBScreen parent) { this.parent = parent; }
    @Override public void onPress(Button button) {
        PresetLibraryStore.reload();
        Minecraft.getInstance().setScreen(new PresetLibraryScreen(parent));
    }
}

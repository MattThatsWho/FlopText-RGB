package com.yourname.floptext_rgb.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Minecraft 26.1.x adapter: GLFW input, screen API on Minecraft. Tested in-game. */
public final class Compat {
    /** Default key for the open-editor hotkey: GLFW V. */
    public static final int OPEN_KEY_DEFAULT = InputConstants.KEY_V;

    private Compat() {}

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static boolean isLeft(int button) {
        return button == 0;
    }

    public static KeyMapping key(String name, int keyCode, KeyMapping.Category category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, category);
    }

    public static RenderTarget mainRenderTarget() {
        return Minecraft.getInstance().getMainRenderTarget();
    }
}

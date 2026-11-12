package com.yourname.floptext_rgb.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Minecraft 26.2 adapter: input is still GLFW (left click = 0), but the current-screen API moved
 * from Minecraft to Minecraft.gui. Modeled on the QualityClip 26.2 adapter. Not yet verified in-game.
 */
public final class Compat {
    /** Default key for the open-editor hotkey: GLFW V. */
    public static final int OPEN_KEY_DEFAULT = InputConstants.KEY_V;

    private Compat() {}

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static boolean isLeft(int button) {
        return button == 0;
    }

    public static KeyMapping key(String name, int keyCode, KeyMapping.Category category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, category);
    }

    /**
     * Finds a no-argument method on Minecraft that returns a RenderTarget instead of hard-linking,
     * because the main framebuffer accessor changed after 26.1.2. Returns null if none is found.
     */
    public static RenderTarget mainRenderTarget() {
        try {
            Minecraft mc = Minecraft.getInstance();
            for (java.lang.reflect.Method m : Minecraft.class.getMethods()) {
                if (m.getParameterCount() == 0 && RenderTarget.class.isAssignableFrom(m.getReturnType())) {
                    return (RenderTarget) m.invoke(mc);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

package com.yourname.floptext_rgb.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Minecraft 26.3 adapter. Minecraft 26.3 moved the current-screen API onto {@code Minecraft.gui}
 * and switched input from GLFW to SDL (left mouse button is 1, key codes are SDL scancodes).
 * Modeled on the QualityClip 26.3 adapter. NOT yet verified in a live 26.3 client.
 */
public final class Compat {
    /** Default key for the open-editor hotkey: the SDL scancode for V on 26.3. */
    public static final int OPEN_KEY_DEFAULT = 25;

    private Compat() {}

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static boolean isLeft(int button) {
        return button == 1;
    }

    /**
     * KeyMapping's constructor changed with the SDL switch, so look it up instead of hard-linking.
     * Tries (String, int, Category), then (String, Type, int, Category). Returns null if neither exists.
     */
    public static KeyMapping key(String name, int keyCode, KeyMapping.Category category) {
        try {
            for (Constructor<?> c : KeyMapping.class.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 3 && p[0] == String.class && p[1] == int.class && p[2] == KeyMapping.Category.class) {
                    return (KeyMapping) c.newInstance(name, keyCode, category);
                }
            }
            for (Constructor<?> c : KeyMapping.class.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 4 && p[0] == String.class && p[2] == int.class && p[3] == KeyMapping.Category.class
                        && p[1].isEnum()) {
                    Object type = null;
                    for (Object k : p[1].getEnumConstants()) {
                        String n = k.toString();
                        if (n.equals("KEYBOARD") || n.equals("KEYSYM")) type = k;
                    }
                    if (type != null) return (KeyMapping) c.newInstance(name, type, keyCode, category);
                }
            }
        } catch (Throwable ignored) {
        }
        return null; // no hotkey on this build; Mod Menu still works
    }

    /**
     * The main framebuffer accessor was renamed/moved in 26.3. Find a no-argument method on Minecraft
     * that returns a RenderTarget instead of hard-linking. Returns null if none is found.
     */
    public static RenderTarget mainRenderTarget() {
        try {
            Minecraft mc = Minecraft.getInstance();
            for (Method m : Minecraft.class.getMethods()) {
                if (m.getParameterCount() == 0 && RenderTarget.class.isAssignableFrom(m.getReturnType())) {
                    return (RenderTarget) m.invoke(mc);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

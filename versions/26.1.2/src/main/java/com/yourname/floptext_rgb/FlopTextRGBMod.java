package com.yourname.floptext_rgb;

import com.yourname.floptext_rgb.compat.Compat;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Reconstructed from the v1.7.0 class file. The registration and keybind
 * behavior was recovered from bytecode; this is not the original source file.
 */
public final class FlopTextRGBMod implements ClientModInitializer {
    public static final String NAMESPACE = "floptext_rgb";
    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        FlopTextRGBConfig.register();
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(NAMESPACE, "main")
        );
        // Default key is V. The key code and constructor differ per Minecraft version, so it goes through Compat.
        KeyMapping key = Compat.key("key.floptext_rgb.open", Compat.OPEN_KEY_DEFAULT, category);
        if (key != null) {
            openGuiKey = KeyMappingHelper.registerKeyMapping(key);
        }
        ClientTickEvents.END_CLIENT_TICK.register(FlopTextRGBMod::onEndClientTick);
    }

    private static void onEndClientTick(Minecraft client) {
        if (openGuiKey == null) return;
        while (openGuiKey.consumeClick()) {
            if (Compat.screen() == null) {
                Compat.setScreen(new FlopTextRGBScreen());
            }
        }
    }
}

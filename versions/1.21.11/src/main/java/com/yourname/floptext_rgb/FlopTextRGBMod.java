    package com.yourname.floptext_rgb;

    import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
    import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
    import net.fabricmc.api.ClientModInitializer;
    import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
    import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
    import net.minecraft.client.MinecraftClient;
    import net.minecraft.client.option.KeyBinding;
    import net.minecraft.client.util.InputUtil;
    import net.minecraft.util.Identifier;
    import org.lwjgl.glfw.GLFW;

    /**
     * Reconstructed from the v1.7.0 class file. The registration and keybind
     * behavior was recovered from bytecode; this is not the original source file.
     */
    public final class FlopTextRGBMod implements ClientModInitializer {
        public static final String NAMESPACE = "floptext_rgb";
        private static KeyBinding openGuiKey;

        @Override
        public void onInitializeClient() {
            FlopTextRGBConfig.register();
            KeyBinding.Category category = KeyBinding.Category.create(
                Identifier.of(NAMESPACE, "main")
            );
            openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.floptext_rgb.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category
            ));
            ClientTickEvents.END_CLIENT_TICK.register(FlopTextRGBMod::onEndClientTick);
        }

        private static void onEndClientTick(MinecraftClient client) {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new FlopTextRGBScreen());
                }
            }
        }
    }

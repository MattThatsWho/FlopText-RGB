    package com.yourname.floptext_rgb.integration;

    import com.terraformersmc.modmenu.api.ConfigScreenFactory;
    import com.terraformersmc.modmenu.api.ModMenuApi;
    import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;

    /** Reconstructed Mod Menu entrypoint. */
    public final class ModMenuIntegration implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
            return parent -> new FlopTextRGBScreen();
        }
    }

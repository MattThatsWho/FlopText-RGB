package com.yourname.floptext_rgb.library;

/** Centralizes transfer actions across the reconstructed Library and folder screens. */
public final class PresetTransferHooks {
    private PresetTransferHooks() { }

    public static void installLibraryButtons(Object owner) {
        if (owner instanceof PresetLibraryScreen screen) screen.installTransferButtons();
    }

    public static void installFolderButtons(Object owner) {
        if (owner instanceof PresetFolderScreen screen) screen.installTransferButtons();
    }

    public static void importFolderFromLibrary(Object owner) {
        PresetFileBrowserScreen.openFolderImport(owner);
    }

    public static void exportFolderFromFolderScreen(Object owner) {
        if (owner instanceof PresetFolderScreen screen) screen.exportCurrentFolder();
    }
}

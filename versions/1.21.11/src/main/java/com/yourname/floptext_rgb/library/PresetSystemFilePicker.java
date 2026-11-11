package com.yourname.floptext_rgb.library;

/**
 * Reconstructed compatibility shim. The in-game file browser is always used in this recovery
 * workspace because the original environment reported platform dialog failures in headless Java.
 */
public final class PresetSystemFilePicker {
    private PresetSystemFilePicker() { }

    public static boolean openIfConfigured(Object owner, int mode, int index) {
        // The original project defaulted to the in-game browser after AWT/Swing
        // dialogs failed in a headless environment. Preserve that safe default.
        if (owner == null) return false;
        PresetFileBrowserScreen.openTransfer(owner, mode, index);
        return true;
    }
}

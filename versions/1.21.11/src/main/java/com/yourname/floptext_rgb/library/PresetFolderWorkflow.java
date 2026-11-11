package com.yourname.floptext_rgb.library;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

/** File-exchange helpers. In-game navigation is the default; OS opening is optional. */
public final class PresetFolderWorkflow {
    private static final Path EXCHANGE = Path.of("config", "floptext_rgb", "preset_files");
    private PresetFolderWorkflow() { }

    public static Path directory() {
        try { Files.createDirectories(EXCHANGE); } catch (Exception ignored) { }
        return EXCHANGE;
    }

    public static boolean useSystemPickerSetting(Path ignored) {
        // The recovered UI deliberately falls back to the in-game browser.
        return false;
    }

    public static boolean openPresetFolder() {
        try {
            Path directory = directory().toAbsolutePath();
            if (!Desktop.isDesktopSupported()) return false;
            Desktop.getDesktop().open(directory.toFile());
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    public static void addBrowserButtons(Object owner) {
        if (owner instanceof PresetFileBrowserScreen screen) screen.installConvenienceButtons();
    }

    public static void addLibraryButton(Object owner) {
        if (owner instanceof PresetLibraryScreen screen) screen.installTransferButtons();
    }
}

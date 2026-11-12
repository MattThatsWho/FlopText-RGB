package com.yourname.floptext_rgb.library;

import java.nio.file.Files;
import java.nio.file.Path;

/** File-exchange helpers. Native PC file picking is the default; the in-game browser is optional. */
public final class PresetFolderWorkflow {
    private static final Path EXCHANGE = Path.of("config", "floptext_rgb", "preset_files");
    private PresetFolderWorkflow() { }

    public static Path directory() {
        try { Files.createDirectories(EXCHANGE); } catch (Exception ignored) { }
        return EXCHANGE;
    }

    public static boolean useSystemPickerSetting(Path ignored) {
        return PresetFilePickerSettings.useSystemPicker();
    }

    public static boolean openPresetFolder() {
        try {
            Path path = directory().toAbsolutePath().normalize();
            if (!Files.isDirectory(path)) Files.createDirectories(path);
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            ProcessBuilder builder;
            if (os.contains("win")) {
                builder = new ProcessBuilder("explorer.exe", path.toString());
            } else if (os.contains("mac")) {
                builder = new ProcessBuilder("open", path.toString());
            } else {
                builder = new ProcessBuilder("xdg-open", path.toString());
            }
            builder.start();
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    public static void addBrowserButtons(Object owner) {
        if (owner instanceof PresetFileBrowserScreen screen) screen.installConvenienceButtons();
    }

    public static void addLibraryButton(Object owner) {
        if (owner instanceof PresetLibraryScreen screen && !screen.isModalOpen())
            screen.installOpenPresetFolderButton();
    }
}

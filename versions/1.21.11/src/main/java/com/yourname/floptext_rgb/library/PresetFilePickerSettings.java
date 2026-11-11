package com.yourname.floptext_rgb.library;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reconstructed persistence for the user's system-vs-in-game file picker choice. */
public final class PresetFilePickerSettings {
    private static final Path FILE = Path.of("config", "floptext_rgb", "file_picker.json");
    private PresetFilePickerSettings() { }

    public static synchronized boolean useSystemPicker() {
        if (!Files.isRegularFile(FILE)) return false;
        try {
            String setting = Files.readString(FILE, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
            return setting.contains("system");
        } catch (IOException ignored) {
            return false;
        }
    }

    public static synchronized boolean setSystemPicker(boolean enabled) {
        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(FILE, "{\"picker\":\"" + (enabled ? "system" : "ingame") + "\"}",
                StandardCharsets.UTF_8);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}

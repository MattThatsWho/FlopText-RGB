package com.yourname.floptext_rgb.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** JSON exchange for single presets, Favorites collections, and named folders. */
public final class PresetJsonTransfer {
    private static final String CONFIG = "floptext_rgb";
    private static final String PRESET = "preset";
    private static final String STORE = "favorites";
    private static final String FOLDER = "folder";
    private static final String LIBRARY_DATA = "library";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String lastStatus = "";
    private PresetJsonTransfer() { }

    public static String lastStatus() { return lastStatus; }

    public static boolean exportPresetJson(Object value, Path file) {
        try {
            GradientPreset preset = value instanceof GradientPreset p ? p : null;
            if (preset == null) throw new IllegalArgumentException("No valid preset was supplied.");
            JsonEnvelope envelope = new JsonEnvelope("floptext_rgb", 1, PRESET, preset);
            write(file, envelope);
            return status("Preset exported to " + file.getFileName() + ".");
        } catch (Exception e) { return fail("Could not export preset", e); }
    }

    public static boolean exportFolderJson(Object value, Path file) {
        try {
            if (!(value instanceof PresetLibraryStore.Folder folder)) throw new IllegalArgumentException("No valid folder was supplied.");
            folder.sanitize();
            write(file, new JsonEnvelope("floptext_rgb", 1, FOLDER, folder));
            return status("Folder exported to " + file.getFileName() + ".");
        } catch (Exception e) { return fail("Could not export folder", e); }
    }

    public static boolean exportFavoritesJson(Path file) {
        try {
            List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
            write(file, new JsonEnvelope("floptext_rgb", 1, STORE, favorites));
            return status("Favorites exported to " + file.getFileName() + ".");
        } catch (Exception e) { return fail("Could not export Favorites", e); }
    }

    public static boolean importFolderFile(Path file) {
        try {
            JsonObject root = readObject(file);
            String type = type(root);
            PresetLibraryStore.Folder folder;
            if (FOLDER.equals(type)) folder = data(root, PresetLibraryStore.Folder.class);
            else if (type == null && root.has("presets")) folder = GSON.fromJson(root, PresetLibraryStore.Folder.class);
            else throw new IllegalArgumentException("This JSON file is not a folder export.");
            if (folder == null) throw new IllegalArgumentException("The file contains no folder data.");
            folder.sanitize();
            String base = PresetLibraryStore.cleanName(folder.name);
            if (base.isBlank()) base = "Imported Folder";
            folder.name = uniqueFolderName(base);
            PresetLibraryStore.data().folders.add(folder);
            PresetLibraryStore.save();
            return status("Imported folder: " + folder.name + ".");
        } catch (Exception e) { return fail("Could not import folder", e); }
    }

    public static boolean importJsonToFavorites(Path file) {
        try {
            JsonObject root = readObject(file);
            String type = type(root);
            List<GradientPreset> imported = new ArrayList<>();
            if (PRESET.equals(type)) {
                GradientPreset p = data(root, GradientPreset.class);
                if (p != null) imported.add(p);
            } else if (STORE.equals(type) || (type == null && root.has("presets"))) {
                JsonElement payload = root.has("data") ? root.get("data") : root.get("presets");
                if (payload != null && payload.isJsonArray()) {
                    for (JsonElement e : payload.getAsJsonArray()) {
                        GradientPreset p = GSON.fromJson(e, GradientPreset.class); if (p != null) imported.add(p);
                    }
                }
            } else throw new IllegalArgumentException("This JSON file is not a preset or Favorites export.");
            addToFavorites(imported);
            FlopTextRGBConfig.save();
            return status("Imported " + imported.size() + " preset(s) into Favorites.");
        } catch (Exception e) { return fail("Could not import JSON", e); }
    }

    public static boolean importJsonToFolder(Path file, Object target) {
        try {
            if (!(target instanceof PresetLibraryStore.Folder folder)) throw new IllegalArgumentException("No folder selected.");
            JsonObject root = readObject(file);
            String type = type(root);
            List<GradientPreset> imported = new ArrayList<>();
            if (PRESET.equals(type)) {
                GradientPreset p = data(root, GradientPreset.class); if (p != null) imported.add(p);
            } else if (STORE.equals(type)) {
                JsonElement payload = root.get("data");
                if (payload != null && payload.isJsonArray()) for (JsonElement e : payload.getAsJsonArray()) {
                    GradientPreset p = GSON.fromJson(e, GradientPreset.class); if (p != null) imported.add(p);
                }
            } else if (FOLDER.equals(type)) {
                PresetLibraryStore.Folder importedFolder = data(root, PresetLibraryStore.Folder.class);
                if (importedFolder != null && importedFolder.presets != null) imported.addAll(importedFolder.presets);
            } else throw new IllegalArgumentException("Unsupported JSON data type.");
            addToFolder(folder, imported);
            PresetLibraryStore.save();
            return status("Imported " + imported.size() + " preset(s) into " + folder.name + ".");
        } catch (Exception e) { return fail("Could not import JSON", e); }
    }

    public static void importFolderDialog(Object owner) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.IMPORT_FOLDER_JSON, -1); }
    public static void exportFolderDialog(Object owner) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.EXPORT_FOLDER_JSON, -1); }
    public static void importJsonToFavoritesDialog(Object owner) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.IMPORT_FAVORITES_JSON, -1); }
    public static void exportFavoritesDialog(Object owner) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.EXPORT_FAVORITES_JSON, -1); }
    public static void importJsonToFolderDialog(Object owner) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.IMPORT_FOLDER_PRESETS_JSON, -1); }
    public static void exportFavoritePresetDialog(Object owner, int index) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.EXPORT_FAVORITE_PRESET_JSON, index); }
    public static void exportFolderPresetDialog(Object owner, int index) { PresetFileBrowserScreen.openTransfer(owner, PresetFileBrowserScreen.EXPORT_FOLDER_PRESET_JSON, index); }

    private static JsonObject readObject(Path file) throws IOException {
        JsonElement parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonElement.class);
        if (parsed == null || !parsed.isJsonObject()) throw new IllegalArgumentException("The file is not a JSON object.");
        return parsed.getAsJsonObject();
    }
    private static String type(JsonObject object) {
        if (object.has("type")) return object.get("type").getAsString();
        if (object.has("kind")) return object.get("kind").getAsString();
        return null;
    }
    private static <T> T data(JsonObject root, Class<T> type) {
        JsonElement payload = root.has("data") ? root.get("data") : root.get("payload");
        if (payload == null || payload.isJsonNull()) return null;
        return GSON.fromJson(payload, type);
    }
    private static void write(Path file, JsonEnvelope envelope) throws IOException {
        Path normalized = ensureJson(file);
        if (normalized.getParent() != null) Files.createDirectories(normalized.getParent());
        Files.writeString(normalized, GSON.toJson(envelope), StandardCharsets.UTF_8);
    }
    private static Path ensureJson(Path p) {
        String name = p.getFileName().toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".json") ? p : p.resolveSibling(name + ".json");
    }
    private static String uniqueFolderName(String raw) {
        String base = raw == null || raw.isBlank() ? "Imported Folder" : raw.trim();
        String candidate = base;
        int suffix = 2;
        while (folderNameExists(candidate)) candidate = base + " (" + suffix++ + ")";
        return candidate;
    }

    private static boolean folderNameExists(String candidate) {
        for (PresetLibraryStore.Folder folder : PresetLibraryStore.data().folders) {
            if (folder.name != null && folder.name.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }
    private static void addToFavorites(List<GradientPreset> values) {
        List<GradientPreset> target = FlopTextRGBConfig.get().presets;
        for (GradientPreset p : values) if (p != null) {
            p.sanitize(); p.name = uniquePresetName(target, p.name); target.add(p);
        }
    }
    private static void addToFolder(PresetLibraryStore.Folder folder, List<GradientPreset> values) {
        if (folder.presets == null) folder.presets = new ArrayList<>();
        for (GradientPreset p : values) if (p != null) {
            p.sanitize(); p.name = uniquePresetName(folder.presets, p.name); folder.presets.add(p);
        }
    }
    private static String uniquePresetName(List<GradientPreset> list, String raw) {
        String base = raw == null || raw.isBlank() ? "Imported Preset" : raw.trim();
        String candidate = base;
        int suffix = 2;
        while (presetNameExists(list, candidate)) candidate = base + " (" + suffix++ + ")";
        return candidate;
    }

    private static boolean presetNameExists(List<GradientPreset> presets, String candidate) {
        for (GradientPreset preset : presets) {
            if (preset != null && preset.name != null && preset.name.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }
    private static boolean status(String message) { lastStatus = message; return true; }
    private static boolean fail(String prefix, Exception e) { lastStatus = prefix + ": " + message(e); return false; }
    private static String message(Throwable e) { Throwable c = e.getCause() == null ? e : e.getCause(); return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage(); }

    private static final class JsonEnvelope {
        String namespace; int version; String type; Object data;
        JsonEnvelope(String namespace, int version, String type, Object data) { this.namespace = namespace; this.version = version; this.type = type; this.data = data; }
    }
}

package com.yourname.floptext_rgb.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Editable reconstruction of the local Favorites/folder library persistence layer. */
public final class PresetLibraryStore {
    private static final Path FILE = Path.of("config", "floptext_rgb", "preset_library.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LibraryData cached;
    private static String status = "";
    private PresetLibraryStore() { }

    public static synchronized LibraryData data() {
        if (cached == null) reload();
        return cached;
    }

    public static synchronized void reload() {
        LibraryData loaded = new LibraryData();
        if (Files.isRegularFile(FILE)) {
            try {
                LibraryData parsed = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), LibraryData.class);
                if (parsed != null) loaded = parsed;
            } catch (Throwable ex) {
                status = "Could not load library: " + safe(ex);
            }
        }
        loaded.sanitize();
        cached = loaded;
    }

    public static synchronized void save() {
        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(FILE, GSON.toJson(data()), StandardCharsets.UTF_8);
            status = "Library saved.";
        } catch (Throwable ex) {
            status = "Could not save library: " + safe(ex);
        }
    }

    public static String status() { return status; }

    public static synchronized boolean addFolder(String rawName) {
        String name = cleanName(rawName);
        if (name.isBlank()) { status = "Enter a folder name."; return false; }
        if (folderNameExists(name)) { status = "A folder with that name already exists."; return false; }
        data().folders.add(new Folder(name));
        save();
        return true;
    }

    public static synchronized void deleteFolder(int index) {
        if (index >= 0 && index < data().folders.size()) {
            data().folders.remove(index);
            save();
        }
    }

    public static String cleanName(String raw) {
        if (raw == null) return "";
        String name = raw.trim().replaceAll("[\\p{Cntrl}]", "");
        return name.length() <= 40 ? name : name.substring(0, 40).trim();
    }

    public static synchronized boolean importFolder(Path archive) {
        try (InputStream in = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().equals("folder.json")) continue;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                zip.transferTo(bytes);
                Folder folder = GSON.fromJson(bytes.toString(StandardCharsets.UTF_8), Folder.class);
                if (folder == null) { status = "That ZIP does not contain a valid FlopText RGB folder."; return false; }
                folder.sanitize();
                if (folder.name.isBlank()) folder.name = cleanName(removeExtension(archive.getFileName().toString()));
                if (folder.name.isBlank()) folder.name = "Imported Folder";
                String baseName = folder.name;
                int suffix = 2;
                while (folderNameExists(folder.name)) folder.name = baseName + " (" + suffix++ + ")";
                data().folders.add(folder);
                save();
                status = "Imported folder: " + folder.name;
                return true;
            }
            status = "That ZIP is missing folder.json.";
        } catch (Throwable ex) {
            status = "Could not import folder: " + safe(ex);
        }
        return false;
    }

    public static boolean exportFolder(Folder folder, Path output) {
        try {
            String name = output.getFileName().toString();
            Path target = name.toLowerCase(Locale.ROOT).endsWith(".zip") ? output : output.resolveSibling(name + ".zip");
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(target); ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("folder.json"));
                zip.write(GSON.toJson(folder).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            status = "Exported folder to " + target.getFileName();
            return true;
        } catch (Throwable ex) {
            status = "Could not export folder: " + safe(ex);
            return false;
        }
    }

    private static boolean folderNameExists(String name) {
        for (Folder folder : data().folders) if (folder.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static String removeExtension(String value) {
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }

    private static String safe(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    public static final class LibraryData {
        public List<Folder> folders = new ArrayList<>();
        public void sanitize() {
            if (folders == null) folders = new ArrayList<>();
            folders.removeIf(folder -> folder == null);
            for (Folder folder : folders) folder.sanitize();
        }
    }

    public static final class Folder {
        public String name = "";
        public List<GradientPreset> presets = new ArrayList<>();
        public Folder() { }
        public Folder(String name) { this.name = name; }
        public void sanitize() {
            if (name == null) name = "";
            name = name.trim();
            if (presets == null) presets = new ArrayList<>();
            presets.removeIf(preset -> preset == null);
            for (GradientPreset preset : presets) preset.sanitize();
        }
    }
}

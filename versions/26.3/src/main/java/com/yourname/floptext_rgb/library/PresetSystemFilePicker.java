package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Native operating-system file picker for preset import/export.
 * It is deliberately dispatched on AWT's event thread, with all Minecraft
 * screen/status mutations returned to the client thread.
 */
public final class PresetSystemFilePicker {
    private PresetSystemFilePicker() { }

    /**
     * @return false only when the in-game browser is selected; true means this
     * picker handled the request, including cancellation or an unavailable AWT
     * environment. In system mode we never silently switch to the in-game UI.
     */
    public static boolean openIfConfigured(Object owner, int mode, int index) {
        if (!PresetFilePickerSettings.useSystemPicker()) return false;
        try {
            // Windows uses an external WinForms dialog. This deliberately bypasses
            // java.awt.GraphicsEnvironment.isHeadless(), which was incorrectly
            // disabling the PC picker in the user's Prism/Java setup.
            if (isWindows()) {
                Thread pickerThread = new Thread(() -> showWindowsDialog(owner, mode, index),
                    "FlopText RGB PC File Picker");
                pickerThread.setDaemon(true);
                pickerThread.start();
            } else if (GraphicsEnvironment.isHeadless()) {
                postStatus(owner, "The native PC file picker is unavailable on this system. Select the in-game browser in Settings.");
            } else {
                EventQueue.invokeLater(() -> showDialog(owner, mode, index));
            }
        } catch (Throwable error) {
            postStatus(owner, "Error opening the PC file picker: " + message(error));
        }
        return true;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Windows-native file dialog independent from the AWT headless flag. */
    private static void showWindowsDialog(Object owner, int mode, int index) {
        Path script = null;
        Path errorLog = null;
        try {
            script = Files.createTempFile("floptext-rgb-picker-", ".ps1");
            errorLog = Files.createTempFile("floptext-rgb-picker-", ".err");
            Files.writeString(script, WINDOWS_PICKER_SCRIPT, java.nio.charset.StandardCharsets.UTF_8);
            String kind = isSave(mode) ? "Save" : "Open";
            String title = dialogTitle(mode);
            String filter = windowsFilter(mode);
            String name = defaultName(owner, mode, index);
            String initialDirectory = System.getProperty("user.home", ".");
            Process process = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass",
                "-File", script.toAbsolutePath().toString(),
                "-DialogMode", kind,
                "-DialogTitle", title,
                "-DialogFilter", filter,
                "-DefaultName", name,
                "-InitialDirectory", initialDirectory)
                .redirectError(errorLog.toFile())
                .start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            int exit = process.waitFor();
            if (exit != 0) {
                String detail = Files.readString(errorLog, java.nio.charset.StandardCharsets.UTF_8).trim();
                throw new java.io.IOException(detail.isBlank()
                    ? "Windows file dialog exited with code " + exit : detail);
            }
            if (output.isBlank()) return; // The user cancelled the dialog.
            // PowerShell emits exactly one selected path; spaces and non-ASCII text are retained.
            String selectedLine = output.lines().reduce((first, last) -> last).orElse("").trim();
            if (selectedLine.isBlank()) return;
            Path selected = Path.of(selectedLine).toAbsolutePath().normalize();
            if (isSave(mode)) selected = withExpectedExtension(selected, mode);
            Path resultPath = selected;
            Minecraft.getInstance().execute(() -> performTransfer(owner, mode, index, resultPath));
        } catch (Throwable error) {
            postStatus(owner, "PC file picker failed: " + message(error));
        } finally {
            try { if (script != null) Files.deleteIfExists(script); } catch (Throwable ignored) { }
            try { if (errorLog != null) Files.deleteIfExists(errorLog); } catch (Throwable ignored) { }
        }
    }

    private static String windowsFilter(int mode) {
        if (mode == PresetFileBrowserScreen.IMPORT_FOLDER_ANY)
            return "JSON and ZIP preset files (*.json;*.zip)|*.json;*.zip|JSON presets (*.json)|*.json|ZIP archives (*.zip)|*.zip|All files (*.*)|*.*";
        if (mode == PresetFileBrowserScreen.LEGACY_ZIP_IMPORT || mode == PresetFileBrowserScreen.LEGACY_ZIP_EXPORT)
            return "ZIP archives (*.zip)|*.zip|All files (*.*)|*.*";
        return "FlopText RGB presets (*.json)|*.json|All files (*.*)|*.*";
    }

    private static final String WINDOWS_PICKER_SCRIPT = """
        param(
            [string]$DialogMode,
            [string]$DialogTitle,
            [string]$DialogFilter,
            [string]$DefaultName,
            [string]$InitialDirectory
        )
        $ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
        Add-Type -AssemblyName System.Windows.Forms
        [System.Windows.Forms.Application]::EnableVisualStyles()
        if ($DialogMode -eq 'Save') {
            $dialog = New-Object System.Windows.Forms.SaveFileDialog
            $dialog.OverwritePrompt = $true
            $dialog.AddExtension = $true
        } else {
            $dialog = New-Object System.Windows.Forms.OpenFileDialog
            $dialog.CheckFileExists = $true
            $dialog.Multiselect = $false
        }
        $dialog.Title = $DialogTitle
        $dialog.Filter = $DialogFilter
        $dialog.RestoreDirectory = $true
        if (-not [string]::IsNullOrWhiteSpace($InitialDirectory) -and (Test-Path -LiteralPath $InitialDirectory)) {
            $dialog.InitialDirectory = $InitialDirectory
        }
        if (-not [string]::IsNullOrWhiteSpace($DefaultName)) {
            $dialog.FileName = $DefaultName
            $extension = [System.IO.Path]::GetExtension($DefaultName).TrimStart('.')
            if (-not [string]::IsNullOrWhiteSpace($extension)) { $dialog.DefaultExt = $extension }
        }
        try {
            if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
                [Console]::Out.WriteLine($dialog.FileName)
            }
        } finally {
            $dialog.Dispose()
        }
        """;

    private static void showDialog(Object owner, int mode, int index) {
        Frame frame = null;
        FileDialog dialog = null;
        try {
            boolean save = isSave(mode);
            frame = new Frame();
            frame.setUndecorated(true);
            dialog = new FileDialog(frame, dialogTitle(mode), save ? FileDialog.SAVE : FileDialog.LOAD);
            dialog.setDirectory(System.getProperty("user.home", "."));
            String defaultName = defaultName(owner, mode, index);
            if (!defaultName.isBlank()) dialog.setFile(defaultName);
            dialog.setFilenameFilter((directory, name) -> {
                if (directory != null && new File(directory, name).isDirectory()) return true;
                return allowedExtension(mode, name);
            });
            dialog.setVisible(true);

            String selectedName = dialog.getFile();
            String selectedDirectory = dialog.getDirectory();
            if (selectedName == null || selectedName.isBlank()) return; // user cancelled
            File selectedFile = selectedDirectory == null
                ? new File(selectedName) : new File(selectedDirectory, selectedName);
            Path selected = selectedFile.toPath().toAbsolutePath().normalize();
            if (save) selected = withExpectedExtension(selected, mode);
            final Path resultPath = selected;
            Minecraft.getInstance().execute(() -> performTransfer(owner, mode, index, resultPath));
        } catch (Throwable error) {
            postStatus(owner, "Error using the PC file picker: " + message(error));
        } finally {
            if (dialog != null) dialog.dispose();
            if (frame != null) frame.dispose();
        }
    }

    private static void performTransfer(Object owner, int mode, int index, Path selected) {
        try {
            if (selected == null || selected.getFileName() == null) {
                throw new IllegalArgumentException("No file was selected.");
            }
            if (!isSave(mode) && !allowedExtension(mode, selected.getFileName().toString())) {
                throw new IllegalArgumentException(mode == PresetFileBrowserScreen.IMPORT_FOLDER_ANY
                    ? "Choose a folder .json or legacy .zip file." : "Choose a file with the supported extension.");
            }
            boolean ok;
            String status;
            switch (mode) {
                case PresetFileBrowserScreen.IMPORT_FAVORITES_JSON -> {
                    requireInputFile(selected);
                    ok = PresetJsonTransfer.importJsonToFavorites(selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.EXPORT_FAVORITES_JSON -> {
                    ok = PresetJsonTransfer.exportFavoritesJson(selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.IMPORT_FOLDER_JSON -> {
                    requireInputFile(selected);
                    ok = PresetJsonTransfer.importFolderFile(selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.IMPORT_FOLDER_ANY -> {
                    requireInputFile(selected);
                    if (selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        ok = PresetLibraryStore.importFolder(selected);
                        status = PresetLibraryStore.status();
                    } else {
                        ok = PresetJsonTransfer.importFolderFile(selected);
                        status = PresetJsonTransfer.lastStatus();
                    }
                }
                case PresetFileBrowserScreen.EXPORT_FOLDER_JSON -> {
                    PresetLibraryStore.Folder folder = folderFor(owner);
                    ok = PresetJsonTransfer.exportFolderJson(folder, selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.EXPORT_FAVORITE_PRESET_JSON -> {
                    var favorites = FlopTextRGBConfig.get().presets;
                    if (index < 0 || index >= favorites.size()) throw new IllegalArgumentException("Favorite preset no longer exists.");
                    ok = PresetJsonTransfer.exportPresetJson(favorites.get(index), selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.EXPORT_FOLDER_PRESET_JSON -> {
                    PresetLibraryStore.Folder folder = folderFor(owner);
                    if (folder == null || folder.presets == null || index < 0 || index >= folder.presets.size())
                        throw new IllegalArgumentException("Folder preset no longer exists.");
                    ok = PresetJsonTransfer.exportPresetJson(folder.presets.get(index), selected);
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.IMPORT_FOLDER_PRESETS_JSON -> {
                    requireInputFile(selected);
                    ok = PresetJsonTransfer.importJsonToFolder(selected, folderFor(owner));
                    status = PresetJsonTransfer.lastStatus();
                }
                case PresetFileBrowserScreen.LEGACY_ZIP_IMPORT -> {
                    requireInputFile(selected);
                    ok = PresetLibraryStore.importFolder(selected);
                    status = PresetLibraryStore.status();
                }
                case PresetFileBrowserScreen.LEGACY_ZIP_EXPORT -> {
                    PresetLibraryStore.Folder folder = folderFor(owner);
                    if (folder == null) throw new IllegalArgumentException("Open a folder before exporting it as ZIP.");
                    ok = PresetLibraryStore.exportFolder(folder, selected);
                    status = PresetLibraryStore.status();
                }
                default -> throw new IllegalArgumentException("Unsupported import/export operation.");
            }
            if (status == null || status.isBlank()) status = ok ? "Import/export completed." : "Import/export failed.";
            postStatus(owner, status);
        } catch (Throwable error) {
            postStatus(owner, "Import/export failed: " + message(error));
        }
    }

    private static void requireInputFile(Path selected) {
        if (selected == null || !Files.isRegularFile(selected)) {
            throw new IllegalArgumentException("Select an existing file.");
        }
    }

    private static PresetLibraryStore.Folder folderFor(Object owner) {
        return owner instanceof PresetFolderScreen folderScreen ? folderScreen.getFolder() : null;
    }

    private static void postStatus(Object owner, String message) {
        try {
            Minecraft.getInstance().execute(() -> {
                if (owner instanceof PresetLibraryScreen screen) screen.setNotice(message);
                else if (owner instanceof PresetFolderScreen screen) screen.setNotice(message);
                else if (owner instanceof PresetFileBrowserScreen screen) screen.setNotice(message);
                else if (owner instanceof Screen screen) Minecraft.getInstance().setScreen(screen);
            });
        } catch (Throwable ignored) { }
    }

    private static boolean isSave(int mode) {
        return mode == PresetFileBrowserScreen.EXPORT_FAVORITES_JSON
            || mode == PresetFileBrowserScreen.EXPORT_FOLDER_JSON
            || mode == PresetFileBrowserScreen.EXPORT_FAVORITE_PRESET_JSON
            || mode == PresetFileBrowserScreen.EXPORT_FOLDER_PRESET_JSON
            || mode == PresetFileBrowserScreen.LEGACY_ZIP_EXPORT;
    }

    private static String dialogTitle(int mode) {
        if (mode == PresetFileBrowserScreen.LEGACY_ZIP_IMPORT) return "Import FlopText RGB Folder ZIP";
        if (mode == PresetFileBrowserScreen.LEGACY_ZIP_EXPORT) return "Export FlopText RGB Folder ZIP";
        return isSave(mode) ? "Export FlopText RGB Presets" : "Import FlopText RGB Presets";
    }

    private static String defaultName(Object owner, int mode, int index) {
        return switch (mode) {
            case PresetFileBrowserScreen.EXPORT_FAVORITES_JSON -> "favorites.json";
            case PresetFileBrowserScreen.EXPORT_FOLDER_JSON -> {
                PresetLibraryStore.Folder folder = folderFor(owner);
                yield safeName(folder == null ? "folder" : folder.name) + ".json";
            }
            case PresetFileBrowserScreen.EXPORT_FAVORITE_PRESET_JSON,
                 PresetFileBrowserScreen.EXPORT_FOLDER_PRESET_JSON -> "preset.json";
            case PresetFileBrowserScreen.LEGACY_ZIP_EXPORT -> {
                PresetLibraryStore.Folder folder = folderFor(owner);
                yield safeName(folder == null ? "folder" : folder.name) + ".zip";
            }
            default -> "";
        };
    }

    private static boolean allowedExtension(int mode, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (mode == PresetFileBrowserScreen.LEGACY_ZIP_IMPORT || mode == PresetFileBrowserScreen.LEGACY_ZIP_EXPORT)
            return name.endsWith(".zip");
        if (mode == PresetFileBrowserScreen.IMPORT_FOLDER_ANY) return name.endsWith(".json") || name.endsWith(".zip");
        return name.endsWith(".json");
    }

    private static Path withExpectedExtension(Path path, int mode) {
        String expected = (mode == PresetFileBrowserScreen.LEGACY_ZIP_EXPORT) ? ".zip" : ".json";
        String filename = path.getFileName().toString();
        if (filename.toLowerCase(Locale.ROOT).endsWith(expected)) return path;
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return path.resolveSibling(base + expected);
    }

    private static String safeName(String raw) {
        if (raw == null) return "folder";
        String clean = raw.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return clean.isBlank() || clean.equals(".") || clean.equals("..") ? "folder" : clean;
    }

    private static String message(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}

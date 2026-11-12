package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.ui.FlopTheme;

import com.yourname.floptext_rgb.compat.Compat;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Reconstructed to match the original v1.7.0/1.21.11 Library layout. */
public final class PresetLibraryScreen extends Screen {
    private final FlopTextRGBScreen mainScreen;
    private int scroll, scrollMax, viewportTop, viewportBottom;
    private boolean addFolderPopup;
    private int deleteFolderPopup = -1;
    private EditBox folderNameField;
    private String notice = "";
    private int noticeTicks;
    private int foldersStart, favoritesStart, columns = 2, cardWidth;
    private final int cardHeight = 62;
    private final int presetCardHeight = 90;

    public PresetLibraryScreen(FlopTextRGBScreen mainScreen) {
        super(Component.literal("Preset Library"));
        this.mainScreen = mainScreen;
        PresetLibraryStore.reload();
    }

    public FlopTextRGBScreen getMainScreen() { return mainScreen; }
    public int screenWidth() { return width; }
    public int screenHeight() { return height; }
    public boolean isModalOpen() { return addFolderPopup || deleteFolderPopup >= 0; }
    public void setNotice(String message) { notice = message == null ? "" : message; noticeTicks = 180; }

    @Override protected void init() {
        clearWidgets();
        viewportTop = 112;
        viewportBottom = Math.max(viewportTop + 50, height - 56);
        columns = (width <= 640 || height <= 360) ? 2 : 3;
        int availableWidth = Math.max(180, width - 40);
        cardWidth = Math.max(90, (availableWidth - 10 * (columns - 1)) / columns);

        List<PresetLibraryStore.Folder> folders = PresetLibraryStore.data().folders;
        int folderRows = Math.max(1, (folders.size() + columns - 1) / columns);
        int folderFiller = folders.isEmpty() ? 48 : folderRows * 70;
        foldersStart = 118;
        favoritesStart = foldersStart + folderFiller + 14 + 1 + 16 + 20;
        List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
        int favoriteRows = Math.max(1, (favorites.size() + columns - 1) / columns);
        int contentEnd = favoritesStart + (favorites.isEmpty() ? 42 : favoriteRows * 98);
        scrollMax = Math.max(0, contentEnd - viewportBottom + 8);
        scroll = Math.max(0, Math.min(scroll, scrollMax));

        // A modal blocks the ordinary Library toolbar and content controls.
        if (isModalOpen()) {
            buildModal();
            PresetTransferHooks.installLibraryButtons(this);
            PresetFolderWorkflow.addLibraryButton(this);
            return;
        }

        LibraryUi.button(this::addRenderableWidget, "+ Add Folder", Math.max(0, width - 126), 87, 106, 20,
            b -> { addFolderPopup = true; init(); });

        // Folders appear before Favorites in the original Library.
        for (int i = 0; i < folders.size(); i++) {
            final int folderIndex = i;
            int col = i % columns;
            int row = i / columns;
            int x = 20 + col * (cardWidth + 10);
            int top = foldersStart + row * 70 - scroll;
            if (!cardVisible(top, cardHeight)) continue;
            if (fullyVisible(top + 36, 18)) {
                LibraryUi.button(this::addRenderableWidget, "Open", x + 8, top + 36,
                    Math.max(42, cardWidth - 42), 18,
                    b -> Compat.setScreen(new PresetFolderScreen(this, folderIndex)));
            }
            if (fullyVisible(top + 5, 18)) {
                LibraryUi.button(this::addRenderableWidget, "X", x + cardWidth - 28, top + 5, 20, 18,
                    b -> { deleteFolderPopup = folderIndex; init(); });
            }
        }

        for (int i = 0; i < favorites.size(); i++) {
            final int index = i;
            int col = i % columns;
            int row = i / columns;
            int x = 20 + col * (cardWidth + 10);
            int top = favoritesStart + row * 98 - scroll;
            if (!cardVisible(top, presetCardHeight)) continue;
            int buttonWidth = (cardWidth - 24) / 2;
            if (fullyVisible(top + 66, 18)) {
                LibraryUi.button(this::addRenderableWidget, "Load", x + 8, top + 66, buttonWidth, 18,
                    b -> { mainScreen.applyPreset(favorites.get(index)); Compat.setScreen(mainScreen); });
                LibraryUi.button(this::addRenderableWidget, "Del", x + 16 + buttonWidth, top + 66,
                    buttonWidth, 18, b -> deleteFavorite(index));
            }
        }

        LibraryUi.button(this::addRenderableWidget, "Back", Math.max(0, width / 2 - 70), height - 43,
            140, 24, b -> goBack());
        PresetTransferHooks.installLibraryButtons(this);
        PresetFolderWorkflow.addLibraryButton(this);
        CommunityPresetScreen.installLibraryEntry(this);
    }

    private boolean fullyVisible(int y, int h) { return y >= viewportTop && y + h <= viewportBottom; }
    private boolean cardVisible(int y, int h) { return y + h >= viewportTop && y <= viewportBottom; }

    private void buildModal() {
        int centerX = width / 2;
        int centerY = height / 2;
        if (addFolderPopup) {
            folderNameField = new EditBox(font, centerX - 150, centerY - 5, 190, 20,
                Component.literal("Folder name"));
            folderNameField.setMaxLength(40);
            addRenderableWidget(folderNameField);
            LibraryUi.button(this::addRenderableWidget, "Create Folder", centerX + 46, centerY - 5,
                104, 20, b -> {
                    if (PresetLibraryStore.addFolder(folderNameField.getValue())) {
                        addFolderPopup = false;
                        setNotice("Folder created.");
                    } else setNotice(PresetLibraryStore.status());
                    init();
                });
            LibraryUi.button(this::addRenderableWidget, "Upload Folder", centerX - 110, centerY + 28,
                108, 20, b -> PresetTransferHooks.importFolderFromLibrary(this));
            LibraryUi.button(this::addRenderableWidget, "Cancel", centerX + 12, centerY + 28,
                88, 20, b -> { addFolderPopup = false; init(); });
        } else if (deleteFolderPopup >= 0) {
            LibraryUi.button(this::addRenderableWidget, "Cancel Deletion", centerX - 148, centerY + 26,
                136, 22, b -> { deleteFolderPopup = -1; init(); });
            LibraryUi.button(this::addRenderableWidget, "Continue, I know what I'm doing", centerX + 2,
                centerY + 26, 146, 22, b -> {
                    PresetLibraryStore.deleteFolder(deleteFolderPopup);
                    deleteFolderPopup = -1;
                    setNotice("Folder deleted.");
                    init();
                });
        }
    }

    private void deleteFavorite(int index) {
        List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
        if (index < 0 || index >= favorites.size()) return;
        favorites.remove(index);
        FlopTextRGBConfig.save();
        setNotice("Preset deleted.");
        init();
    }

    public void addTransferButton(String title, int x, int y, int w, int h, Runnable task) {
        LibraryUi.button(this::addRenderableWidget, title, x, y, w, h, b -> task.run());
    }

    /** Import/export toolbar and per-favorite JSON controls from the 1.21.11 UI. */
    public void installTransferButtons() {
        if (isModalOpen()) return;
        int importX = Math.max(12, width - 350);
        int exportX = Math.max(12, width - 238);
        addTransferButton("Import JSON", importX, 87, 104, 20,
            () -> PresetJsonTransfer.importJsonToFavoritesDialog(this));
        addTransferButton("Export Favs", exportX, 87, 104, 20,
            () -> PresetJsonTransfer.exportFavoritesDialog(this));
        if (!isModalOpen()) {
            List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
            for (int i = 0; i < favorites.size(); i++) {
                int index = i;
                int col = i % columns, row = i / columns;
                int x = 20 + col * (cardWidth + 10);
                int y = favoritesStart + row * 98 - scroll;
                if (cardVisible(y, presetCardHeight) && fullyVisible(y + 5, 16)) {
                    addTransferButton("JSON", x + cardWidth - 44, y + 5, 36, 16,
                        () -> PresetJsonTransfer.exportFavoritePresetDialog(this, index));
                }
            }
        }
    }

    /** The original Open Preset Folder control sits on the upper-right header row. */
    public void installOpenPresetFolderButton() {
        addTransferButton("Open Preset Folder", Math.max(16, width - 194), 16, 174, 22, () ->
            setNotice(PresetFolderWorkflow.openPresetFolder()
                ? "Opened preset folder. Use Refresh after adding files."
                : "Could not open preset folder."));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        LibraryUi.panel(context, 10, 10, width - 20, height - 20);
        context.text(font, Component.literal("Preset Library"), 20, 18, -1);
        context.text(font, Component.literal("Manage Your Presets Better"), 20, 42, 0xFFB8B8B8);
        LibraryUi.divider(context, 20, 68, width - 40);
        LibraryUi.panel(context, 12, 78, width - 24, height - 132);
        boolean favoritesAtTop = favoritesStart - scroll <= viewportTop + 20;
        context.text(font, Component.literal(favoritesAtTop ? "Favorites" : "Folders"), 20, 92, -1);

        context.enableScissor(16, viewportTop, width - 16, viewportBottom);
        List<PresetLibraryStore.Folder> folders = PresetLibraryStore.data().folders;
        if (folders.isEmpty()) {
            context.centeredText(font, Component.literal("No folders"), width / 2 - 28,
                foldersStart + 15 - scroll, 0xFFB0B0B0);
        } else {
            for (int i = 0; i < folders.size(); i++) {
                PresetLibraryStore.Folder folder = folders.get(i);
                int col = i % columns, row = i / columns;
                int x = 20 + col * (cardWidth + 10);
                int y = foldersStart + row * 70 - scroll;
                if (y + cardHeight < viewportTop || y > viewportBottom) continue;
                LibraryUi.panel(context, x, y, cardWidth, cardHeight);
                context.text(font, Component.literal(clip(folder.name, Math.max(12, cardWidth - 44))), x + 8, y + 7, -1);
                context.text(font, Component.literal(folder.presets.size() + " presets"), x + 8, y + 24, 0xFFB8B8B8);
            }
        }
        int separatorY = favoritesStart - 23 - scroll;
        LibraryUi.divider(context, 20, separatorY, width - 40);
        // The fixed section title above changes from Folders to Favorites as this
        // section scrolls into the viewport, avoiding a duplicate moving heading.
        List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
        if (favorites.isEmpty()) {
            context.centeredText(font, Component.literal("No favorite presets"), width / 2,
                favoritesStart + 15 - scroll, 0xFFB0B0B0);
        } else {
            for (int i = 0; i < favorites.size(); i++) {
                GradientPreset preset = favorites.get(i);
                int col = i % columns, row = i / columns;
                int x = 20 + col * (cardWidth + 10);
                int y = favoritesStart + row * 98 - scroll;
                if (y + presetCardHeight < viewportTop || y > viewportBottom) continue;
                LibraryUi.panel(context, x, y, cardWidth, presetCardHeight);
                context.text(font, Component.literal(clip(preset.name, Math.max(12, cardWidth - 56))), x + 8, y + 7, -1);
                PresetPreviewRenderer.draw(context, font, "", x + 8, y + 31, preset, cardWidth - 16);
            }
        }
        context.disableScissor();
        LibraryUi.scrollBar(context, width - 16, viewportTop, viewportBottom, scroll, scrollMax);

        if (!notice.isEmpty() && noticeTicks > 0) {
            context.centeredText(font, Component.literal(notice), width / 2, height - 27,
                notice.startsWith("Could") || notice.startsWith("Error") ? 0xFFFF7777 : 0xFF70E090);
            noticeTicks--;
        }
        if (isModalOpen()) {
            context.fill(0, 0, width, height, 0x99000000);
            int modalW = addFolderPopup ? 330 : 310;
            int modalH = addFolderPopup ? 116 : 64;
            int x = (width - modalW) / 2;
            int y = (height - modalH) / 2 - 8;
            LibraryUi.panel(context, x, y, modalW, modalH);
            context.centeredText(font, Component.literal(addFolderPopup ? "Create a folder" : "Delete Folder?"),
                width / 2, y + 9, -1);
            if (addFolderPopup) {
                context.centeredText(font, Component.literal("Create a folder or upload folder JSON/ZIP."),
                    width / 2, y + 28, 0xFFB8B8B8);
            } else if (deleteFolderPopup >= 0 && deleteFolderPopup < folders.size()) {
                context.centeredText(font, Component.literal(clip(folders.get(deleteFolderPopup).name, 34)),
                    width / 2, y + 28, 0xFFB8B8B8);
                context.centeredText(font, Component.literal("This cannot be undone."), width / 2, y + 40, 0xFFFF7777);
            }
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        FlopTheme.paintButtons(context, font, this, mouseX, mouseY);
        // Draw the pixel-person icon after the widget, so its silhouette isn't covered.
        if (!isModalOpen()) CommunityPresetScreen.drawLibraryEntry(this, context, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (CommunityPresetScreen.handleLibraryEntryClick(this, click)) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isModalOpen()) return true;
        if (verticalAmount != 0 && mouseY >= viewportTop && mouseY <= viewportBottom) {
            scroll = Math.max(0, Math.min(scrollMax, scroll - (int) Math.round(verticalAmount * 18.0)));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void onClose() { goBack(); }
    private void goBack() { Compat.setScreen(mainScreen); }
    static String clip(String s, int n) { if (s == null) return ""; return s.length() <= n ? s : s.substring(0, Math.max(0, n - 1)) + "…"; }
    static int firstColor(GradientPreset preset) { return preset == null || preset.toStops().isEmpty() ? 0xFFFFFF : preset.toStops().get(0).rgb(); }
}

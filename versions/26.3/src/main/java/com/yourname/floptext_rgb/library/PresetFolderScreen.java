package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Folder-specific preset browser, reconstructed to match the original 1.21.11 layout. */
public final class PresetFolderScreen extends Screen {
    private static final int CARD_HEIGHT = 90;
    private static final int ROW_STEP = 98;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB0B0B0;

    private final PresetLibraryScreen library;
    private final int folderIndex;
    private int scroll;
    private int scrollMax;
    private int viewportTop;
    private int viewportBottom;
    private int columns = 2;
    private int cardWidth;
    private boolean addPresetPopup;
    private int confirmDeleteIndex = -1;
    private EditBox presetNameField;
    private String notice = "";
    private int noticeTicks;

    public PresetFolderScreen(PresetLibraryScreen library, int folderIndex) {
        super(Component.literal("Preset Folder"));
        this.library = library;
        this.folderIndex = folderIndex;
    }

    private PresetLibraryStore.Folder folder() {
        List<PresetLibraryStore.Folder> folders = PresetLibraryStore.data().folders;
        return folderIndex >= 0 && folderIndex < folders.size() ? folders.get(folderIndex) : null;
    }

    public PresetLibraryStore.Folder getFolder() { return folder(); }

    public void setNotice(String message) {
        notice = message == null ? "" : message;
        noticeTicks = 120;
    }

    @Override
    protected void init() {
        clearWidgets();
        PresetLibraryStore.reload();

        // Geometry follows the original 1.21.11 screen: a 108px viewport start,
        // 90px cards, 98px row pitch, and two/three columns based on GUI size.
        viewportTop = 108;
        viewportBottom = Math.max(viewportTop + 30, height - 56);
        columns = (width <= 640 || height <= 360) ? 2 : 3;
        cardWidth = Math.max(90, (width - 40 - (columns - 1) * 10) / columns);

        PresetLibraryStore.Folder currentFolder = folder();
        int count = currentFolder == null ? 0 : currentFolder.presets.size();
        int rows = (count + columns - 1) / columns;
        scrollMax = Math.max(0, rows * ROW_STEP - (viewportBottom - viewportTop) + 8);
        scroll = Math.max(0, Math.min(scroll, scrollMax));

        // Import actions sit in the top metadata row. Keep the established
        // system-picker routing and legacy ZIP import without covering the title.
        int importWidth = Math.min(96, Math.max(68, (width - 280) / 2));
        int importY = width >= 480 ? 42 : 44;
        int importJsonX;
        int importZipX;
        if (width >= 480) {
            importJsonX = width - 20 - importWidth * 2 - 6;
            importZipX = width - 20 - importWidth;
        } else {
            importJsonX = 20;
            importZipX = importJsonX + importWidth + 6;
        }
        LibraryUi.button(this::addRenderableWidget, "Import JSON", importJsonX, importY, importWidth, 21,
            b -> PresetJsonTransfer.importJsonToFolderDialog(this));
        LibraryUi.button(this::addRenderableWidget, "Import ZIP", importZipX, importY, importWidth, 21,
            b -> PresetFileBrowserScreen.openLegacyImport(this));

        // These two actions occupy the right side of the original Presets header.
        LibraryUi.button(this::addRenderableWidget, "Save Current", width - 252, 80, 108, 20,
            b -> openAddPopup());
        LibraryUi.button(this::addRenderableWidget, "Export Folder", width - 132, 80, 112, 20,
            b -> exportCurrentFolder());

        if (currentFolder != null) {
            for (int i = 0; i < currentFolder.presets.size(); i++) {
                final int index = i;
                int col = i % columns;
                int row = i / columns;
                int x = 20 + col * (cardWidth + 10);
                int y = viewportTop + row * ROW_STEP - scroll;
                if (y + CARD_HEIGHT < viewportTop || y > viewportBottom) continue;

                int buttonWidth = (cardWidth - 24) / 2;
                int buttonY = y + 66;
                // Do not leave partially clipped/invisible buttons in the event list.
                if (buttonY >= viewportTop && buttonY + 18 <= viewportBottom) {
                    LibraryUi.button(this::addRenderableWidget, "Load", x + 8, buttonY,
                        buttonWidth, 18, b -> load(index));
                    LibraryUi.button(this::addRenderableWidget, "Del", x + 16 + buttonWidth, buttonY,
                        buttonWidth, 18, b -> delete(index));
                }
            }
        }

        if (addPresetPopup || confirmDeleteIndex >= 0) {
            LibraryUi.hideAll(this);
            if (addPresetPopup) buildAddPresetPopup();
            else buildDeleteConfirmation();
        } else {
            // Matches the original centered footer Back control.
            LibraryUi.button(this::addRenderableWidget, "Back", Math.max(0, width / 2 - 70),
                height - 43, 140, 24, b -> goBack());
        }
    }

    private void openAddPopup() {
        addPresetPopup = true;
        init();
    }

    private void buildDeleteConfirmation() {
        int w = Math.min(340, width - 24);
        int x = (width - w) / 2;
        int y = height / 2 - 44;
        LibraryUi.button(this::addRenderableWidget, "Delete Preset", x + 14, y + 42,
            Math.max(90, (w - 36) / 2), 22, b -> {
                PresetLibraryStore.Folder target = folder();
                int index = confirmDeleteIndex;
                confirmDeleteIndex = -1;
                if (target != null && index >= 0 && index < target.presets.size()) {
                    target.presets.remove(index);
                    PresetLibraryStore.save();
                    setNotice("Preset deleted.");
                }
                init();
            });
        LibraryUi.button(this::addRenderableWidget, "Cancel", x + w / 2 + 4, y + 42,
            Math.max(90, (w - 36) / 2), 22, b -> {
                confirmDeleteIndex = -1;
                init();
            });
    }

    private void buildAddPresetPopup() {
        int w = Math.min(350, width - 24);
        int x = (width - w) / 2;
        int y = height / 2 - 60;
        presetNameField = new EditBox(font, x + 14, y + 38, w - 28, 20,
            Component.literal("Preset name"));
        presetNameField.setMaxLength(64);
        presetNameField.setValue("My Gradient");
        addRenderableWidget(presetNameField);

        LibraryUi.button(this::addRenderableWidget, "Add Preset", x + 16, y + 70, 125, 22, b -> {
            PresetLibraryStore.Folder target = folder();
            if (target == null) {
                setNotice("Folder not found.");
                return;
            }
            String name = PresetLibraryStore.cleanName(presetNameField.getValue());
            if (name.isBlank()) {
                setNotice("Enter a preset name.");
                return;
            }
            if (hasPresetName(target, name)) {
                setNotice("A preset with that name already exists.");
                return;
            }
            FlopTextRGBScreen editor = library.getMainScreen();
            target.presets.add(editor.snapshotCurrentPreset(name));
            PresetLibraryStore.save();
            addPresetPopup = false;
            setNotice("Preset added to folder.");
            init();
        });
        LibraryUi.button(this::addRenderableWidget, "Cancel", x + w - 140, y + 116, 124, 22, b -> {
            addPresetPopup = false;
            init();
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        LibraryUi.panel(context, 10, 10, width - 20, height - 20);

        PresetLibraryStore.Folder currentFolder = folder();
        String folderName = currentFolder == null ? "Preset Folder" : currentFolder.name;
        int count = currentFolder == null ? 0 : currentFolder.presets.size();
        String countLabel = count + (count == 1 ? " preset" : " presets");
        LibraryUi.label(context, font, folderName, 20, 18, WHITE);
        LibraryUi.label(context, font, countLabel, 20, width >= 480 ? 42 : 34, MUTED);
        LibraryUi.divider(context, 20, 68, width - 40);

        // Main card container and heading mirror the original folder page.
        LibraryUi.panel(context, 12, 76, width - 24, Math.max(40, height - 130));
        LibraryUi.label(context, font, "Presets", 20, 86, WHITE);

        if (currentFolder == null || currentFolder.presets.isEmpty()) {
            LibraryUi.label(context, font, "This folder has no presets yet.",
                Math.max(20, width / 2 - 80), viewportTop + 24, MUTED);
        } else {
            context.enableScissor(16, viewportTop, width - 16, viewportBottom);
            for (int i = 0; i < currentFolder.presets.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = 20 + col * (cardWidth + 10);
                int y = viewportTop + row * ROW_STEP - scroll;
                if (y + CARD_HEIGHT < viewportTop || y > viewportBottom) continue;

                LibraryUi.panel(context, x, y, cardWidth, CARD_HEIGHT);
                GradientPreset preset = currentFolder.presets.get(i);
                String name = preset.name == null ? "Preset" : preset.name;
                LibraryUi.label(context, font,
                    PresetLibraryScreen.clip(name, Math.max(12, (cardWidth - 16) / 6)),
                    x + 8, y + 7, WHITE);
                PresetPreviewRenderer.draw(context, font, "", x + 8, y + 31,
                    preset, cardWidth - 16);
            }
            context.disableScissor();
            LibraryUi.scrollBar(context, width - 16, viewportTop, viewportBottom, scroll, scrollMax);
        }

        if (!notice.isEmpty() && noticeTicks > 0) {
            LibraryUi.label(context, font,
                PresetLibraryScreen.clip(notice, Math.max(10, width - 40)),
                20, height - 60, 0xFFFFB05A);
            noticeTicks--;
        }

        if (addPresetPopup || confirmDeleteIndex >= 0) {
            context.fill(0, 0, width, height, 0x99000000);
            int w = Math.min(350, width - 24);
            int x = (width - w) / 2;
            if (addPresetPopup) {
                int y = height / 2 - 77;
                LibraryUi.panel(context, x, y, w, 154);
                context.centeredText(font, Component.literal("Add Preset to Folder"), width / 2, y + 12, WHITE);
                context.centeredText(font,
                    Component.literal("Save the current editor gradient here."), width / 2, y + 25, MUTED);
                LibraryUi.label(context, font, "Preset name", x + 14, y + 39, MUTED);
                LibraryUi.label(context, font, "Live preview", x + 14, y + 62, MUTED);
                PresetPreviewRenderer.draw(context, font, "", x + 14, y + 76,
                    library.getMainScreen().snapshotCurrentPreset("Preview"), w - 28);
            } else {
                int y = height / 2 - 44;
                LibraryUi.panel(context, x, y, w, 92);
                context.centeredText(font, Component.literal("Delete this preset?"), width / 2, y + 12, WHITE);
                context.centeredText(font, Component.literal("This cannot be undone."), width / 2, y + 26, MUTED);
            }
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (addPresetPopup || confirmDeleteIndex >= 0) return true;
        if (mouseY >= viewportTop && mouseY <= viewportBottom && verticalAmount != 0) {
            scroll = Math.max(0, Math.min(scrollMax, scroll - (int) Math.signum(verticalAmount) * 24));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void load(int index) {
        PresetLibraryStore.Folder currentFolder = folder();
        if (currentFolder == null || index < 0 || index >= currentFolder.presets.size()) return;
        library.getMainScreen().applyPreset(currentFolder.presets.get(index));
        Minecraft.getInstance().setScreen(library.getMainScreen());
    }

    private void delete(int index) {
        PresetLibraryStore.Folder currentFolder = folder();
        if (currentFolder == null || index < 0 || index >= currentFolder.presets.size()) return;
        confirmDeleteIndex = index;
        init();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        if (addPresetPopup || confirmDeleteIndex >= 0) {
            super.mouseClicked(click, doubled);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private static boolean hasPresetName(PresetLibraryStore.Folder folder, String name) {
        for (GradientPreset preset : folder.presets) {
            if (preset != null && preset.name != null && preset.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public void exportCurrentFolder() {
        if (folder() == null) {
            setNotice("Folder not found.");
            return;
        }
        PresetJsonTransfer.exportFolderDialog(this);
    }

    public void addTransferButton(String title, int x, int y, int w, int h, Runnable action) {
        LibraryUi.button(this::addRenderableWidget, title, x, y, w, h, b -> action.run());
    }

    public void installTransferButtons() {
        // Extra actions are deliberately not injected here: the visible toolbar is
        // defined once in init() so coordinates, paint geometry, and hitboxes match.
    }

    private void goBack() { Minecraft.getInstance().setScreen(library); }
    @Override public void onClose() { goBack(); }
}

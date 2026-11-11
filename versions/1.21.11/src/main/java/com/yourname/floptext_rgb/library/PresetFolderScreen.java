package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Folder-specific preset browser with load, delete, and transfer actions. */
public final class PresetFolderScreen extends Screen {
    private static final int CARD_HEIGHT = 66;
    private static final int WHITE = 0xFFF2F5FF;
    private static final int MUTED = 0xFF9BA9C0;

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
    private TextFieldWidget presetNameField;
    private String notice = "";
    private int noticeTicks;

    public PresetFolderScreen(PresetLibraryScreen library, int folderIndex) {
        super(Text.literal("Preset Folder"));
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
        clearChildren();
        PresetLibraryStore.reload();

        int left = 14;
        int right = width - 14;
        int availableWidth = right - left;
        columns = width >= 800 ? 3 : width >= 520 ? 2 : 1;
        cardWidth = Math.max(145, (availableWidth - 10 * (columns - 1)) / columns);

        boolean compact = availableWidth < 410;
        viewportTop = 104;
        viewportBottom = height - 58;
        PresetLibraryStore.Folder currentFolder = folder();
        int count = currentFolder == null ? 0 : currentFolder.presets.size();
        int contentHeight = ((count + columns - 1) / columns) * CARD_HEIGHT;
        scrollMax = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        scroll = Math.max(0, Math.min(scroll, scrollMax));

        LibraryUi.button(this, "Back", left, 14, 70, 22, b -> goBack());
        LibraryUi.button(this, "Save Current Here", left + 78, 14, compact ? 112 : 132, 22,
            b -> openAddPopup());

        int exportX = left + (compact ? 198 : 218);
        int exportW = Math.max(82, right - exportX - (compact ? 0 : 80));
        LibraryUi.button(this, "Export Folder JSON", exportX, 14, exportW, 22,
            b -> exportCurrentFolder());
        if (!compact) {
            LibraryUi.button(this, "Export ZIP", right - 74, 14, 74, 22, b -> exportLegacyZip());
        }

        if (compact) {
            int gap = 5;
            int slot = Math.max(1, (availableWidth - gap * 2) / 3);
            LibraryUi.button(this, "Import JSON", left, 47, slot, 22,
                b -> PresetJsonTransfer.importJsonToFolderDialog(this));
            LibraryUi.button(this, "Import ZIP", left + slot + gap, 47, slot, 22,
                b -> PresetFileBrowserScreen.openLegacyImport(library));
            LibraryUi.button(this, "Export ZIP", left + (slot + gap) * 2, 47,
                availableWidth - (slot + gap) * 2, 22, b -> exportLegacyZip());
        } else {
            int secondRowWidth = Math.max(100, (availableWidth - 8) / 2);
            LibraryUi.button(this, "Import Presets JSON", left, 47, secondRowWidth, 22,
                b -> PresetJsonTransfer.importJsonToFolderDialog(this));
            LibraryUi.button(this, "Import Folder ZIP", left + secondRowWidth + 8, 47,
                secondRowWidth, 22, b -> PresetFileBrowserScreen.openLegacyImport(library));
        }

        if (currentFolder != null) {
            context.enableScissor(14, viewportTop, width - 14, viewportBottom);
            for (int i = 0; i < currentFolder.presets.size(); i++) {
                final int index = i;
                int col = i % columns;
                int row = i / columns;
                int x = left + col * (cardWidth + 10);
                int y = viewportTop + row * CARD_HEIGHT - scroll;
                if (y < viewportTop || y + CARD_HEIGHT - 4 > viewportBottom) continue;

                int innerWidth = Math.max(120, cardWidth - 10);
                int gap = 4;
                int buttonWidth = Math.max(32, (innerWidth - gap * 2) / 3);
                LibraryUi.button(this, "Load", x + 5, y + 40, buttonWidth, 20,
                    b -> load(index));
                LibraryUi.button(this, "Delete", x + 5 + buttonWidth + gap, y + 40,
                    buttonWidth, 20, b -> delete(index));
                LibraryUi.button(this, "Export", x + 5 + (buttonWidth + gap) * 2, y + 40,
                    buttonWidth, 20, b -> PresetJsonTransfer.exportFolderPresetDialog(this, index));
            }
        }

        if (addPresetPopup || confirmDeleteIndex >= 0) {
            LibraryUi.hideAll(this);
            if (addPresetPopup) buildAddPresetPopup();
            else buildDeleteConfirmation();
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
        LibraryUi.button(this, "Delete Preset", x + 14, y + 42, Math.max(90, (w - 36) / 2), 22, b -> {
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
        LibraryUi.button(this, "Cancel", x + w / 2 + 4, y + 42, Math.max(90, (w - 36) / 2), 22, b -> {
            confirmDeleteIndex = -1;
            init();
        });
    }

    private void buildAddPresetPopup() {
        int w = Math.min(350, width - 24);
        int x = (width - w) / 2;
        int y = height / 2 - 60;
        presetNameField = new TextFieldWidget(textRenderer, x + 14, y + 38, w - 28, 20,
            Text.literal("Preset name"));
        presetNameField.setMaxLength(64);
        presetNameField.setText("My Gradient");
        addDrawableChild(presetNameField);

        LibraryUi.button(this, "Add Preset", x + 16, y + 70, 125, 22, b -> {
            PresetLibraryStore.Folder target = folder();
            if (target == null) {
                setNotice("Folder not found.");
                return;
            }
            String name = PresetLibraryStore.cleanName(presetNameField.getText());
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
        LibraryUi.button(this, "Cancel", x + w - 140, y + 70, 124, 22, b -> {
            addPresetPopup = false;
            init();
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        LibraryUi.panel(context, 10, 8, width - 20, height - 16);
        PresetLibraryStore.Folder currentFolder = folder();
        String folderName = currentFolder == null ? "Missing Folder" : currentFolder.name;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(folderName), width / 2, 20, WHITE);
        context.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Named presets · JSON exchange · legacy ZIP support"), width / 2, 34, MUTED);

        if (currentFolder != null) {
            context.enableScissor(14, viewportTop, width - 14, viewportBottom);
            for (int i = 0; i < currentFolder.presets.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = 14 + col * (cardWidth + 10);
                int y = viewportTop + row * CARD_HEIGHT - scroll;
                if (y < viewportTop || y + CARD_HEIGHT - 4 > viewportBottom) continue;
                LibraryUi.panel(context, x, y, cardWidth, CARD_HEIGHT - 4);
                GradientPreset preset = currentFolder.presets.get(i);
                context.drawTextWithShadow(textRenderer,
                    Text.literal(PresetLibraryScreen.clip(preset.name, Math.max(12, cardWidth / 6))),
                    x + 8, y + 7, WHITE);
                PresetPreviewRenderer.draw(context, textRenderer, "", x + 8, y + 23,
                    preset, cardWidth - 16);
            }
            context.disableScissor();
        }

        if (!notice.isEmpty() && noticeTicks > 0) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), width / 2,
                height - 30, 0xFF70D6A5);
            noticeTicks--;
        }

        if (addPresetPopup || confirmDeleteIndex >= 0) {
            context.fill(0, 0, width, height, 0x99000000);
            int w = Math.min(350, width - 24);
            int x = (width - w) / 2;
            if (addPresetPopup) {
                int y = height / 2 - 60;
                LibraryUi.panel(context, x, y, w, 120);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("Add Preset to Folder"),
                    width / 2, y + 12, WHITE);
                context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Save the current editor gradient here."), width / 2, y + 25, MUTED);
            } else {
                int y = height / 2 - 44;
                LibraryUi.panel(context, x, y, w, 92);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("Delete this preset?"),
                    width / 2, y + 12, WHITE);
                context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("This cannot be undone."), width / 2, y + 26, MUTED);
            }
        }
        super.render(context, mouseX, mouseY, delta);
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
        MinecraftClient.getInstance().setScreen(library.getMainScreen());
    }

    private void delete(int index) {
        PresetLibraryStore.Folder currentFolder = folder();
        if (currentFolder == null || index < 0 || index >= currentFolder.presets.size()) return;
        confirmDeleteIndex = index;
        init();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
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

    private void exportLegacyZip() {
        if (folder() != null) PresetFileBrowserScreen.openLegacyExportFolder(folder().name);
    }

    public void addTransferButton(String title, int x, int y, int w, int h, Runnable action) {
        LibraryUi.button(this, title, x, y, w, h, b -> action.run());
    }

    public void installTransferButtons() {
        // Controls are installed directly in init() so repeated init calls do not duplicate them.
    }

    private void goBack() { MinecraftClient.getInstance().setScreen(library); }
    @Override public void close() { goBack(); }
}

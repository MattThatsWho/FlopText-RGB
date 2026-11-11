package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/** Reconstructed scrollable Favorites and named-folder library UI. */
public final class PresetLibraryScreen extends Screen {
    private final FlopTextRGBScreen mainScreen;
    private int scroll, scrollMax, viewportTop, viewportBottom;
    private boolean addFolderPopup;
    private int deleteFolderPopup = -1;
    private TextFieldWidget folderNameField;
    private String notice = "";
    private int noticeTicks;
    private int foldersStart, favoritesStart, columns = 2, cardWidth, cardHeight = 60, presetCardHeight = 58;

    public PresetLibraryScreen(FlopTextRGBScreen mainScreen) {
        super(Text.literal("Preset Library"));
        this.mainScreen = mainScreen;
        PresetLibraryStore.reload();
    }

    public FlopTextRGBScreen getMainScreen() { return mainScreen; }
    public void setNotice(String message) { notice = message == null ? "" : message; noticeTicks = 150; }

    @Override protected void init() {
        clearChildren();
        int left = 14, right = width - 14, w = right - left;
        columns = width >= 900 ? 3 : (width >= 560 ? 2 : 1);
        cardWidth = Math.max(150, (w - 12 * (columns - 1)) / columns);
        viewportTop = 112; viewportBottom = height - 66;
        int itemCount = FlopTextRGBConfig.get().presets.size() + PresetLibraryStore.data().folders.size() + 2;
        scrollMax = Math.max(0, 80 + itemCount * cardHeight - (viewportBottom - viewportTop));
        scroll = Math.max(0, Math.min(scroll, scrollMax));
        LibraryUi.button(this, "Back", left, 14, 76, 22, b -> goBack());
        LibraryUi.button(this, "Community", left + 84, 14, 110, 22,
            b -> MinecraftClient.getInstance().setScreen(new CommunityPresetScreen(this, mainScreen)));
        LibraryUi.button(this, "Settings", right - 156, 14, 74, 22, b -> PresetSettingsScreen.open(this));
        LibraryUi.button(this, "Open Preset Folder", right - 74, 14, 74, 22,
            b -> setNotice(PresetFolderWorkflow.openPresetFolder() ? "Opened preset exchange folder." : "Open config/floptext_rgb/preset_files manually."));
        int toolGap = 8;
        int toolWidth = Math.max(68, (w - toolGap * 2) / 3);
        LibraryUi.button(this, "Add Folder", left, 49, toolWidth, 22, b -> openAddFolder());
        LibraryUi.button(this, "Import Favorites", left + toolWidth + toolGap, 49, toolWidth, 22,
            b -> PresetJsonTransfer.importJsonToFavoritesDialog(this));
        LibraryUi.button(this, "Export Favorites", left + (toolWidth + toolGap) * 2, 49, toolWidth, 22,
            b -> PresetJsonTransfer.exportFavoritesDialog(this));
        LibraryUi.button(this, "Import Folder JSON", left, 77, toolWidth, 22,
            b -> PresetJsonTransfer.importFolderDialog(this));
        LibraryUi.button(this, "Import Folder ZIP", left + toolWidth + toolGap, 77, toolWidth, 22,
            b -> PresetFileBrowserScreen.openLegacyImport(this));
        LibraryUi.button(this, "Save Current", left + (toolWidth + toolGap) * 2, 77, toolWidth, 22,
            b -> PresetDestinationScreen.openFromMain(mainScreen));

        int y = viewportTop + 5 - scroll;
        favoritesStart = y;
        List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
        for (int i = 0; i < favorites.size(); i++) {
            final int index = i;
            int col = i % columns;
            int row = i / columns;
            int x = left + col * (cardWidth + 12);
            int top = y + row * cardHeight;
            addCardButtons(x, top, cardWidth, favorites.get(i), index);
        }
        y += ((favorites.size() + columns - 1) / columns) * cardHeight + 32;
        foldersStart = y;
        List<PresetLibraryStore.Folder> folders = PresetLibraryStore.data().folders;
        for (int i = 0; i < folders.size(); i++) {
            final int index = i;
            int col = i % columns, row = i / columns;
            int x = left + col * (cardWidth + 12);
            int top = y + row * cardHeight;
            if (top < viewportTop || top + cardHeight > viewportBottom) continue;
            int deleteWidth = Math.min(58, Math.max(48, cardWidth / 3));
            int openWidth = Math.max(50, cardWidth - deleteWidth - 18);
            LibraryUi.button(this, "Open: " + clip(folders.get(i).name, 18), x + 5, top + 5, openWidth, 22,
                b -> MinecraftClient.getInstance().setScreen(new PresetFolderScreen(this, index)));
            LibraryUi.button(this, "Delete", x + cardWidth - deleteWidth - 5, top + 5, deleteWidth, 22,
                b -> { deleteFolderPopup = index; init(); });
        }
        if (addFolderPopup) {
            LibraryUi.hideAll(this);
            buildModal();
        }
        if (deleteFolderPopup >= 0) {
            LibraryUi.hideAll(this);
            buildDeleteConfirm();
        }
    }

    private void addCardButtons(int x, int y, int w, GradientPreset preset, int index) {
        if (y < viewportTop || y + cardHeight > viewportBottom) return;
        int available = Math.max(30, w - 16);
        int gap = 5;
        int buttonW = Math.max(30, (available - gap * 2) / 3);
        LibraryUi.button(this, "Load", x + 4, y + 30, buttonW, 20, b -> {
            mainScreen.applyPreset(preset);
            MinecraftClient.getInstance().setScreen(mainScreen);
        });
        LibraryUi.button(this, "Delete", x + 4 + buttonW + gap, y + 30, buttonW, 20, b -> {
            List<GradientPreset> values = FlopTextRGBConfig.get().presets;
            if (index >= 0 && index < values.size()) { values.remove(index); FlopTextRGBConfig.save(); setNotice("Preset deleted."); init(); }
        });
        LibraryUi.button(this, "Export", x + 4 + (buttonW + gap) * 2, y + 30, buttonW, 20,
            b -> PresetJsonTransfer.exportFavoritePresetDialog(this, index));
    }

    private void openAddFolder() { addFolderPopup = true; init(); }
    private void buildModal() {
        int w = Math.min(360, width - 24), x = (width - w) / 2, y = height / 2 - 64;
        folderNameField = new TextFieldWidget(textRenderer, x + 16, y + 43, w - 32, 20, Text.literal("Folder name"));
        folderNameField.setMaxLength(40); addDrawableChild(folderNameField);
        LibraryUi.button(this, "Create", x + 30, y + 76, 100, 22, b -> {
            if (PresetLibraryStore.addFolder(folderNameField.getText())) { addFolderPopup = false; setNotice("Folder created."); init(); }
            else setNotice(PresetLibraryStore.status());
        });
        LibraryUi.button(this, "Cancel", x + w - 130, y + 76, 100, 22, b -> { addFolderPopup = false; init(); });
    }

    private void buildDeleteConfirm() {
        int w = Math.min(370, width - 24), x = (width - w) / 2, y = height / 2 - 50;
        LibraryUi.button(this, "Delete Folder", x + 15, y + 38, 145, 22, b -> {
            PresetLibraryStore.deleteFolder(deleteFolderPopup); deleteFolderPopup = -1; setNotice("Folder deleted."); init();
        });
        LibraryUi.button(this, "Cancel Deletion", x + w - 160, y + 38, 145, 22, b -> { deleteFolderPopup = -1; init(); });
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        LibraryUi.panel(context, 10, 8, width - 20, height - 16);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Preset Library"), width / 2, 20, 0xFFF2F5FF);
        context.drawTextWithShadow(textRenderer, Text.literal("Favorites"), 18, viewportTop - 15, 0xFF9BA9C0);
        int left = 14;
        context.enableScissor(left, viewportTop, width - 14, viewportBottom);
        List<GradientPreset> favorites = FlopTextRGBConfig.get().presets;
        for (int i = 0; i < favorites.size(); i++) {
            int col = i % columns, row = i / columns;
            int x = left + col * (cardWidth + 12);
            int top = favoritesStart + row * cardHeight;
            if (top < viewportTop || top + cardHeight > viewportBottom) continue;
            LibraryUi.panel(context, x, top, cardWidth, presetCardHeight);
            context.drawTextWithShadow(textRenderer, Text.literal(clip(favorites.get(i).name, 24)), x + 8, top + 7, 0xFFF2F5FF);
            PresetPreviewRenderer.draw(context, textRenderer, "", x + 8, top + 20, favorites.get(i), cardWidth - 16);
        }
        int fy = favoritesStart + ((favorites.size() + columns - 1) / columns) * cardHeight + 17;
        if (fy >= viewportTop && fy + 10 <= viewportBottom)
            context.drawTextWithShadow(textRenderer, Text.literal("Folders"), 18, fy, 0xFF9BA9C0);
        List<PresetLibraryStore.Folder> folders = PresetLibraryStore.data().folders;
        for (int i = 0; i < folders.size(); i++) {
            int col = i % columns, row = i / columns;
            int x = left + col * (cardWidth + 12), top = foldersStart + row * cardHeight;
            if (top < viewportTop || top + cardHeight > viewportBottom) continue;
            LibraryUi.panel(context, x, top, cardWidth, presetCardHeight);
            String name = folders.get(i).name;
            context.drawTextWithShadow(textRenderer, Text.literal(clip(name, 22)), x + 8, top + 30, 0xFFF2F5FF);
            context.drawText(textRenderer, Text.literal(folders.get(i).presets.size() + " presets"), x + 8, top + 42, 0xFF9BA9C0, false);
        }
        context.disableScissor();
        if (!notice.isEmpty() && noticeTicks > 0) { context.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), width / 2, height - 26, 0xFF70D6A5); noticeTicks--; }
        if (addFolderPopup || deleteFolderPopup >= 0) {
            context.fill(0, 0, width, height, 0x99000000);
            int w = Math.min(370, width - 24), x = (width - w) / 2, y = height / 2 - 65;
            LibraryUi.panel(context, x, y, w, 118);
            String title = addFolderPopup ? "Create a folder" : "Delete folder?";
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(title), width / 2, y + 13, 0xFFF2F5FF);
            if (deleteFolderPopup >= 0 && deleteFolderPopup < PresetLibraryStore.data().folders.size())
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(clip(PresetLibraryStore.data().folders.get(deleteFolderPopup).name, 32)), width / 2, y + 25, 0xFF9BA9C0);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (addFolderPopup || deleteFolderPopup >= 0) return true;
        if (mouseY >= viewportTop && mouseY <= viewportBottom) { scroll = Math.max(0, Math.min(scrollMax, scroll - (int)Math.signum(verticalAmount) * 30)); init(); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override public void close() { goBack(); }
    private void goBack() { MinecraftClient.getInstance().setScreen(mainScreen); }
    static String clip(String s, int n) { if (s == null) return ""; return s.length() <= n ? s : s.substring(0, Math.max(0,n-1)) + "…"; }
    static int firstColor(GradientPreset preset) { return preset == null || preset.toStops().isEmpty() ? 0xFFFFFF : preset.toStops().get(0).rgb(); }
    public void addTransferButton(String title,int x,int y,int w,int h,Runnable task) { LibraryUi.button(this,title,x,y,w,h,b -> task.run()); }
    public void installTransferButtons() { /* Controls are installed directly during init to avoid duplicate widgets. */ }
}

package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Settings for preset exchange workflows. In-game browser is the reliable default. */
public final class PresetSettingsScreen extends Screen {
    private final Object owner;
    private String status = "";
    private int statusTicks;

    public PresetSettingsScreen(Object owner) {
        super(Text.literal("FlopText RGB Settings"));
        this.owner = owner;
    }

    public static void open(Object owner) {
        MinecraftClient.getInstance().setScreen(new PresetSettingsScreen(owner));
    }

    public static void drawTopButton(DrawContext context, int mouseX, int mouseY) {
        int screenWidth = MinecraftClient.getInstance().currentScreen == null
            ? 0 : MinecraftClient.getInstance().currentScreen.width;
        int x = screenWidth - 25;
        int y = 12;
        int color = FlopIcons.inside(mouseX, mouseY, x - 2, y - 2, 18, 18)
            ? 0xFF75B8FF : 0xFFD7E6FF;
        FlopIcons.centered(context, FlopIcons.GEAR, x - 1, y - 1, 16, 16, color);
    }

    /** The main-screen gear uses exactly the same rectangle for drawing and hit-testing. */
    public static boolean handleSettingsClick(FlopTextRGBScreen parent, Click click) {
        if (parent == null || click.button() != 0) return false;
        int x = parent.width - 25;
        int y = 12;
        if (!FlopIcons.inside((int) click.x(), (int) click.y(), x - 2, y - 2, 18, 18)) return false;
        open(parent);
        return true;
    }

    @Override
    protected void init() {
        clearChildren();
        int w = Math.min(440, width - 24);
        int x = (width - w) / 2;
        LibraryUi.button(this, "Back", x, 18, 78, 22, b -> goBack());
        LibraryUi.button(this, "Use in-game file browser", x, 70, w, 24, b -> {
            PresetFilePickerSettings.setSystemPicker(false);
            notice("In-game file browser selected.");
        });
        LibraryUi.button(this, "Open preset exchange folder", x, 108, w, 24, b -> {
            notice(PresetFolderWorkflow.openPresetFolder()
                ? "Opened config/floptext_rgb/preset_files."
                : "Could not open automatically. Folder: config/floptext_rgb/preset_files");
        });
        LibraryUi.button(this, "Refresh library data", x, 141, w, 24, b -> {
            PresetLibraryStore.reload();
            notice("Library data refreshed.");
        });
        LibraryUi.button(this, "Open Community Presets", x, 174, w, 24, b -> {
            if (owner instanceof PresetLibraryScreen library) {
                MinecraftClient.getInstance().setScreen(new CommunityPresetScreen(library, library.getMainScreen()));
            } else {
                goBack();
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        LibraryUi.panel(context, width / 2 - 230, 8, 460, height - 16);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Settings"), width / 2, 37, 0xFFF2F5FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Preset exchange directory"), width / 2, 51, 0xFF9BA9C0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("config/floptext_rgb/preset_files/"), width / 2, 220, 0xFF9BA9C0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("This recovery uses the in-game file browser."), width / 2, 235, 0xFF9BA9C0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("System dialogs are not enabled here."), width / 2, 248, 0xFF9BA9C0);
        if (statusTicks > 0) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height - 32, 0xFF70D6A5);
            statusTicks--;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void notice(String message) { status = message; statusTicks = 180; }
    private void goBack() { MinecraftClient.getInstance().setScreen(owner instanceof Screen screen ? screen : null); }
    @Override public void close() { goBack(); }
}

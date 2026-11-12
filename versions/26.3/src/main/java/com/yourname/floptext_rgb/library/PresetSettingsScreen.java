package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.compat.Compat;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Original v1.7.0 settings panel: choose the Minecraft-style or optional system picker. */
public final class PresetSettingsScreen extends Screen {
    private final Object owner;
    private String status = "";
    private int statusTicks;

    public PresetSettingsScreen(Object owner) {
        super(Component.literal("FlopText RGB Settings"));
        this.owner = owner;
    }

    public static void open(Object owner) { Compat.setScreen(new PresetSettingsScreen(owner)); }

    public static void drawTopButton(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        FlopIcons.drawSettings(context, mouseX, mouseY);
    }

    public static boolean handleSettingsClick(FlopTextRGBScreen parent, MouseButtonEvent click) {
        if (parent == null || !Compat.isLeft(click.button())) return false;
        if (!FlopIcons.inside((int) click.x(), (int) click.y(), 18, 14, 24, 24)) return false;
        open(parent);
        return true;
    }

    @Override protected void init() { clearWidgets(); }

    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD0080B10);
        LibraryUi.panel(context, 8, 8, width - 16, height - 16);
        context.text(font, Component.literal("FlopText RGB Settings"), 22, 18, -1);
        context.text(font, Component.literal("Choose how preset files are opened and saved"),
            22, 39, 0xFFB7C3D2);
        LibraryUi.divider(context, 22, 57, width - 44);
        context.text(font, Component.literal("File picker"), 24, 78, 0xFF7DE3F3);

        int optionX = Math.max(24, (width - 340) / 2);
        int optionW = Math.min(340, width - 48);
        boolean system = PresetFilePickerSettings.useSystemPicker();
        drawOption(context, optionX, 112, optionW, 38,
            "System file picker (Recommended)", "Browse files and folders on your PC", system, mouseX, mouseY);
        drawOption(context, optionX, 165, optionW, 38,
            "Minecraft-style preset folder (Secondary)", "Use the in-game file browser instead", !system, mouseX, mouseY);
        context.text(font, Component.literal("Default: your PC's native file picker."),
            24, 223, 0xFFB7C3D2);
        context.text(font, Component.literal("Choose the in-game browser above to keep file navigation inside Minecraft."),
            24, 239, 0xFFB7C3D2);
        if (statusTicks > 0 && !status.isBlank()) {
            context.text(font, Component.literal(status), 24, height - 65,
                status.startsWith("Error") ? 0xFFFF7777 : 0xFF70E090);
            statusTicks--;
        }
        drawButton(context, Math.max(0, width / 2 - 56), height - 46, 112, 26, "Back", mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawOption(GuiGraphicsExtractor context, int x, int y, int w, int h,
                            String title, String subtitle, boolean selected, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        LibraryUi.panel(context, x, y, w, h);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1,
            selected ? 0xFF244D58 : hover ? 0xFF303C46 : 0xFF1B242C);
        context.text(font, Component.literal(title + (selected ? "  [Selected]" : "")), x + 12, y + 6,
            selected ? 0xFF7DE3F3 : -1);
        context.text(font, Component.literal(subtitle), x + 12, y + 21, 0xFFB7C3D2);
    }

    private void drawButton(GuiGraphicsExtractor context, int x, int y, int w, int h,
                            String title, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        LibraryUi.panel(context, x, y, w, h);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF354A55 : 0xFF202B34);
        int tx = x + (w - font.width(title)) / 2;
        context.text(font, Component.literal(title), tx, y + (h - 8) / 2, -1);
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!Compat.isLeft(click.button())) return super.mouseClicked(click, doubled);
        int x = (int) click.x(), y = (int) click.y();
        int optionX = Math.max(24, (width - 340) / 2);
        int optionW = Math.min(340, width - 48);
        if (inside(x, y, optionX, 112, optionW, 38)) {
            setPicker(true, "System file picker selected.");
            return true;
        }
        if (inside(x, y, optionX, 165, optionW, 38)) {
            setPicker(false, "Minecraft-style preset folder selected.");
            return true;
        }
        if (inside(x, y, Math.max(0, width / 2 - 56), height - 46, 112, 26)) {
            goBack();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void setPicker(boolean useSystem, String success) {
        if (PresetFilePickerSettings.setSystemPicker(useSystem)) {
            status = success;
            statusTicks = 160;
        } else {
            status = "Error: Could not save settings.";
            statusTicks = 240;
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void goBack() { Compat.setScreen(owner instanceof Screen screen ? screen : null); }
    @Override public void onClose() { goBack(); }
}

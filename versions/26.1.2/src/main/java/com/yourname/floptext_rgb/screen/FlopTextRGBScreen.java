package com.yourname.floptext_rgb.screen;

import com.yourname.floptext_rgb.ui.FlopTheme;

import com.yourname.floptext_rgb.compat.Compat;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.compat.StyleBooleanBridge;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.library.CommunityPresetScreen;
import com.yourname.floptext_rgb.library.PresetDestinationScreen;
import com.yourname.floptext_rgb.library.PresetLibraryScreen;
import com.yourname.floptext_rgb.library.PresetLibraryStore;
import com.yourname.floptext_rgb.library.PresetSettingsScreen;
import com.yourname.floptext_rgb.library.FlopIcons;
import com.yourname.floptext_rgb.math.GradientCalculator;
import com.yourname.floptext_rgb.math.GradientCalculator.ColorStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Editable reconstruction of the original v1.7.0 gradient editor.
 * It is based on the shipped bytecode and metadata; it is not claimed to be
 * the original authoring source file. Preserve the reference JAR for comparison.
 */
public final class FlopTextRGBScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int PANEL = 0xB90B0B0B;
    private static final int PANEL_EDGE = 0xFF3A3A3A;
    private static final int ACCENT = 0xFF7AA7FF;
    private static final int BAR_FRAME = 0xFF5C5C5C;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final float MIN_STOP_GAP = 0.005f;
    private static final String DEFAULT_TEMPLATE = "$1$2$3$4$5$6$f$c";
    private static final List<String> OUTPUT_FORMATS = List.of("Legacy Hex", "MiniMessage", "Vanilla / Spigot", "Custom");

    private final List<ColorStop> stops = new ArrayList<>();
    private int selectedStop = 0;
    private int draggedStop = -1;
    private ColorStop draggedStopRef;
    private boolean draggedEndpoint;
    private String inputText = "";
    private String hexText = "#4A90E2";
    private String presetName = "";
    private int presetPage = 0;
    private boolean bold, italic, strikethrough, underline;
    private String outputFormat = "Legacy Hex";
    private String customTemplate = DEFAULT_TEMPLATE;
    private boolean templatePopup;
    private EditBox inputField, outputField, hexField, presetNameField, templateField;
    private Button templateSaveButton, templateBackButton;
    // Modal HSV color selector. The existing screenshot-based picker remains available
    // from the separate Pick button; clicking the selected-color swatch opens this UI.
    private boolean colorPickerOpen;
    private boolean draggingColorSquare, draggingHueSlider, syncingPickerHex;
    private float pickerHue, pickerSaturation, pickerValue;
    private int pickerRgb = 0x4A90E2;
    private String pickerHexText = "#4A90E2";
    private String pickerError = "";
    private EditBox pickerHexField;
    private int pickerPanelX, pickerPanelY, pickerPanelWidth, pickerPanelHeight;
    private int pickerSquareX, pickerSquareY, pickerSquareWidth, pickerSquareHeight;
    private int pickerHueX, pickerHueY, pickerHueWidth, pickerHueHeight;
    private int pickerHexX, pickerHexY, pickerHexWidth;
    private int pickerApplyX, pickerCancelX, pickerButtonsY, pickerButtonWidth;
    private int contentLeft, contentRight, sidebarLeft, sidebarRight;
    private int previewTop, previewBottom, inputTop, inputBottom, outputTop, outputBottom;
    private int barLeft, barRight, barTop, barBottom, barScreenTop, barScreenBottom, bottomTop;
    private int mainScroll, sidebarScroll, mainScrollMax, sidebarScrollMax;
    private String notice = "";
    private int noticeTicks;

    public FlopTextRGBScreen() {
        super(Component.literal("FlopText RGB"));
        loadFromConfig();
    }

    private void loadFromConfig() {
        FlopTextRGBConfig config = FlopTextRGBConfig.get();
        bold = config.bold;
        italic = config.italic;
        strikethrough = config.strikethrough;
        underline = config.underline;
        outputFormat = config.outputFormat;
        customTemplate = config.customTemplate;
        loadDefaultStopsIfNeeded();
        syncHex();
        updateOutput();
    }

    private void saveUiConfig() {
        FlopTextRGBConfig config = FlopTextRGBConfig.get();
        config.bold = bold;
        config.italic = italic;
        config.strikethrough = strikethrough;
        config.underline = underline;
        config.outputFormat = outputFormat;
        config.customTemplate = customTemplate;
        FlopTextRGBConfig.save();
    }

    private void cycleFormat() {
        int index = OUTPUT_FORMATS.indexOf(outputFormat);
        if (index < 0) index = 0;
        outputFormat = OUTPUT_FORMATS.get((index + 1) % OUTPUT_FORMATS.size());
        updateOutput();
        saveUiConfig();
        init();
    }

    private void loadDefaultStopsIfNeeded() {
        if (stops.isEmpty()) {
            // Exact v1.7.0 defaults: blue at 0%, pink at 100%.
            stops.add(new ColorStop(0x4A90E2, 0f));
            stops.add(new ColorStop(0xFF4A9A, 1f));
        }
        sortStops();
        selectedStop = Math.max(0, Math.min(selectedStop, stops.size() - 1));
    }

    @Override
    protected void init() {
        clearWidgets();
        int margin = 12;
        int sideWidth = Math.max(175, Math.min(220, width / 4));
        contentLeft = margin;
        sidebarRight = width - margin;
        sidebarLeft = sidebarRight - sideWidth;
        contentRight = sidebarLeft - 10;

        previewTop = 52;
        previewBottom = previewTop + 54;
        inputTop = previewBottom + 10;
        inputBottom = inputTop + 126;
        outputTop = inputBottom + 10;
        // The output section remains full-height in logical GUI coordinates. Its overflow is
        // intentional: mainScrollMax clips it to the footer, matching the 1.21.11 layout.
        outputBottom = outputTop + 90;
        bottomTop = height - 44;

        mainScrollMax = Math.max(0, outputBottom - (bottomTop - 2));
        int cardHeight = Math.max(61, (bottomTop - 48 - 54 - 15) / 4);
        sidebarScrollMax = Math.max(0, 54 + 4 * cardHeight + 15 - (bottomTop - 48));
        mainScroll = clamp(mainScroll, 0, mainScrollMax);
        sidebarScroll = clamp(sidebarScroll, 0, sidebarScrollMax);

        // The original shifts section geometry once in init. Drawing and widgets
        // then share the same scrolled coordinates, avoiding the old double offset.
        previewTop -= mainScroll;
        previewBottom -= mainScroll;
        inputTop -= mainScroll;
        inputBottom -= mainScroll;
        outputTop -= mainScroll;
        outputBottom -= mainScroll;
        barTop = inputTop + 58;
        barBottom = barTop + 12;

        // When the modal is open, build only its own widgets so the editor's hidden
        // controls cannot receive mouse/keyboard input behind the dialog.
        if (colorPickerOpen) {
            buildColorPickerWidgets();
            return;
        }

        buildTextFields();
        buildButtons();
        buildSidebar();
        if (templatePopup) rebuildPopupFields();
        updateOutput();
        updateAllWidgetVisibility();
    }

    private void buildTextFields() {
        int usableW = Math.max(200, contentRight - contentLeft);
        int x = contentLeft + 6;
        int w = Math.max(80, usableW - 16);
        inputField = new EditBox(font, x, inputTop + 24, w, 18, Component.literal("Input text"));
        inputField.setMaxLength(256);
        inputField.setValue(inputText);
        inputField.setResponder(value -> { inputText = value; updateOutput(); });
        addRenderableWidget(inputField);

        outputField = new EditBox(font, x, outputTop + 24, w, 18, Component.literal("Output"));
        outputField.setMaxLength(32767);
        outputField.setEditable(false);
        outputField.setValue(compileOutput());
        addRenderableWidget(outputField);

        hexField = new EditBox(font, contentLeft + 42, inputBottom - 28, 76, 18, Component.literal("Hex color"));
        hexField.setMaxLength(7);
        hexField.setValue(hexText);
        hexField.setResponder(value -> hexText = value);
        addRenderableWidget(hexField);

        presetNameField = new EditBox(font, contentLeft + 6, outputBottom - 24,
            Math.max(90, usableW - 96), 18, Component.literal("Preset name"));
        presetNameField.setMaxLength(32);
        presetNameField.setValue(presetName);
        presetNameField.setResponder(value -> presetName = value);
        addRenderableWidget(presetNameField);
    }

    private void buildButtons() {
        int usableW = Math.max(200, contentRight - contentLeft);
        int stylesY = inputTop + 4;
        int styleW = 26;
        addButton("[B]", contentRight - 120, stylesY, styleW, 18,
            b -> { bold = !bold; updateOutput(); init(); });
        addButton("[I]", contentRight - 92, stylesY, styleW, 18,
            b -> { italic = !italic; updateOutput(); init(); });
        addButton("[S]", contentRight - 64, stylesY, styleW, 18,
            b -> { strikethrough = !strikethrough; updateOutput(); init(); });
        addButton("[U]", contentRight - 36, stylesY, styleW, 18,
            b -> { underline = !underline; updateOutput(); init(); });

        int controlY = inputBottom - 28;
        if (usableW < 340) {
            // High GUI scales make the logical window narrow. Keep all controls
            // inside the old panel rather than allowing the old fixed widths to overlap.
            addButton("Pick", contentLeft + 124, controlY, 43, 18, b -> openPicker());
            addButton("Apply", contentLeft + 171, controlY, 43, 18, b -> applyHex());
            addButton("Remove", contentLeft + 218, controlY, Math.max(42, contentRight - (contentLeft + 218) - 10), 18,
                b -> removeSelectedStop());
        } else {
            addButton("In-Game Hex Picker", contentLeft + 124, controlY, 112, 18, b -> openPicker());
            addButton("Apply Hex", contentLeft + 242, controlY, 70, 18, b -> applyHex());
            addButton("Remove Color", contentRight - 98, controlY, 88, 18, b -> removeSelectedStop());
        }

        int outputButtonY = outputTop + 4;
        String formatLabel = formatButtonLabel(usableW);
        if (usableW < 340) {
            // Keep the original cycling selector clickable. Use compact labels at high GUI scales.
            addButton(formatLabel, contentLeft + 48, outputButtonY, 72, 18, b -> cycleFormat());
            addButton("Copy", contentLeft + 124, outputButtonY, 70, 18, b -> copyOutput());
            Button customButton = addButton("Custom Output", contentLeft + 198, outputButtonY,
                Math.max(76, contentRight - (contentLeft + 198) - 10), 18, b -> openTemplatePopup());
            customButton.active = false; // Keep disabled even when Format is Custom; popup is not ready.
        } else {
            addButton(formatLabel, contentRight - 360, outputButtonY, 126, 18, b -> cycleFormat());
            addButton("Copy To Clipboard", contentRight - 228, outputButtonY, 112, 18, b -> copyOutput());
            Button customButton = addButton("Custom Output", contentRight - 110, outputButtonY, 100, 18, b -> openTemplatePopup());
            customButton.active = false; // Keep disabled even when Format is Custom; popup is not ready.
        }
        addButton("Save Preset", contentRight - 86, outputBottom - 24, 76, 18, b -> savePreset());
        addButton("Close FlopText RGB", width / 2 - 90, bottomTop + 15, 180, 20, b -> closeScreen());
    }

    private String formatButtonLabel(int usableWidth) {
        if (usableWidth >= 340) return "Format: " + outputFormat;
        // The narrow 4x-GUI layout has a 72px selector, so keep its text compact.
        return switch (outputFormat) {
            case "Legacy Hex" -> "Format: Leg";
            case "MiniMessage" -> "Format: Mini";
            case "Vanilla / Spigot" -> "Format: Van";
            case "Custom" -> "Format: Custom";
            default -> "Format: Leg";
        };
    }

    private void buildSidebar() {
        int cardHeight = Math.max(61, (bottomTop - 48 - 54 - 15) / 4);
        int buttonWidth = (sidebarRight - sidebarLeft - 26) / 2;
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        int start = presetPage * 4;
        int cardTop = 54 - sidebarScroll;
        for (int row = 0; row < 4; row++) {
            int index = start + row;
            if (index >= presets.size()) break;
            int y = cardTop + row * (cardHeight + 5) + cardHeight - 22;
            final int presetIndex = index;
            addButton("Load", sidebarLeft + 11, y, buttonWidth, 18, b -> loadPreset(presetIndex));
            addButton("Del", sidebarLeft + 15 + buttonWidth, y, buttonWidth, 18, b -> deletePreset(presetIndex));
        }
        addButton("", sidebarRight - 30, 14, 24, 24,
            b -> Compat.setScreen(new PresetLibraryScreen(this)));
        addButton("<", sidebarLeft + 6, bottomTop - 26, 22, 20, b -> changePage(-1));
        addButton(">", sidebarRight - 28, bottomTop - 26, 22, 20, b -> changePage(1));
    }

    private Button addButton(String label, int x, int y, int w, int h, Button.OnPress action) {
        Button button = Button.builder(Component.literal(label), action).bounds(x, y, Math.max(1, w), h).build();
        addRenderableWidget(button);
        return button;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (colorPickerOpen) {
            // Keep the editor visible behind the modal, but don't render or activate
            // its ordinary widgets while the picker has focus.
            drawEditorBehindColorPicker(context, mouseX, mouseY);
            context.fill(0, 0, width, height, 0xD9000000);
            drawColorPicker(context, mouseX, mouseY);
            super.extractRenderState(context, mouseX, mouseY, delta);
            FlopTheme.paintButtons(context, font, this, mouseX, mouseY);
            return;
        }
        drawPanel(context, contentLeft, 8, contentRight - contentLeft, bottomTop - 8);
        drawPanel(context, sidebarLeft, 8, sidebarRight - sidebarLeft, bottomTop - 8);
        drawPanel(context, contentLeft, bottomTop + 6, sidebarRight - contentLeft, height - bottomTop - 6);

        // Card backgrounds are painted before widgets, never over Load/Del controls.
        context.enableScissor(sidebarLeft + 2, 44, sidebarRight - 2, bottomTop - 48);
        drawPresetCardBackgrounds(context);
        context.disableScissor();
        super.extractRenderState(context, mouseX, mouseY, delta);
        FlopTheme.paintButtons(context, font, this, mouseX, mouseY, this::isStyleToggleOn);

        context.centeredText(font, Component.literal("FlopText RGB"), (contentLeft + contentRight) / 2, 16, WHITE);
        context.centeredText(font, Component.literal("Client-side RGB text gradient creator"), (contentLeft + contentRight) / 2, 34, GRAY);
        // The gear is a dedicated pixel-art control at the top-left, as in v1.7.0.
        PresetSettingsScreen.drawTopButton(context, mouseX, mouseY);
        FlopIcons.drawLibrary(context, sidebarRight - 30, 14, mouseX, mouseY);

        context.enableScissor(8, 44, sidebarLeft - 4, bottomTop - 8);
        drawLabel(context, "Preview", contentLeft + 8, previewTop + 7);
        drawInset(context, contentLeft + 8, previewTop + 22, contentRight - 8, previewBottom - 6, previewFill());
        drawPreview(context, contentLeft + 14, previewTop + 31);

        drawLabel(context, "Input", contentLeft + 8, inputTop + 7);
        drawGradientBar(context);
        drawStopHandles(context, mouseX, mouseY);
        drawColorControls(context);

        drawLabel(context, "Output", contentLeft + 8, outputTop + 7);
        drawLabel(context, "Create Preset", contentLeft + 8, outputBottom - 36);
        drawMainScrollBar(context);
        context.disableScissor();

        drawLabel(context, "Favorites", sidebarLeft + 8, 16);
        drawLabel(context, "Saved gradients", sidebarLeft + 8, 34);
        context.enableScissor(sidebarLeft + 2, 44, sidebarRight - 2, bottomTop - 48);
        drawPresetCardContents(context);
        context.disableScissor();
        drawSidebarScrollBar(context);
        context.centeredText(font, Component.literal((presetPage + 1) + "/" + Math.max(1, (presetCount() + 3) / 4)),
            (sidebarLeft + sidebarRight) / 2, bottomTop - 40, GRAY);
        if (!notice.isEmpty() && noticeTicks > 0) {
            context.centeredText(font, Component.literal(notice), width / 2, bottomTop + 8,
                notice.startsWith("Could") ? RED : GREEN);
            noticeTicks--;
        }
        if (templatePopup) drawTemplatePopup(context);
        else drawColorSwatchTooltip(context, mouseX, mouseY);
    }

    /** Small hover hint for the selected-stop swatch; drawn last so it stays above the editor. */
    private void drawColorSwatchTooltip(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int swatchX = contentLeft + 8;
        int swatchY = inputBottom - 28;
        if (mouseX < swatchX || mouseX >= swatchX + 28
                || mouseY < swatchY || mouseY >= swatchY + 18) return;

        Component label = Component.literal("Click to edit");
        int paddingX = 5;
        int boxWidth = font.width(label) + paddingX * 2;
        int boxHeight = 18;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + boxWidth > width - 4) x = mouseX - boxWidth - 8;
        if (y + boxHeight > height - 4) y = mouseY - boxHeight - 8;
        x = Math.max(4, Math.min(x, width - boxWidth - 4));
        y = Math.max(4, Math.min(y, height - boxHeight - 4));

        ctx.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, ACCENT);
        ctx.fill(x, y, x + boxWidth, y + boxHeight, 0xFF111111);
        ctx.text(font, label, x + paddingX, y + 5, WHITE);
    }

    private void drawMainScrollBar(GuiGraphicsExtractor context) {
        drawScrollBar(context, contentRight - 4, 46, bottomTop - 2, mainScroll, mainScrollMax);
    }

    private void drawSidebarScrollBar(GuiGraphicsExtractor context) {
        drawScrollBar(context, sidebarRight - 4, 46, bottomTop - 48, sidebarScroll, sidebarScrollMax);
    }

    /** Exact 1.21.11 two-pixel scroll indicator, using the same offset/max ratio. */
    private void drawScrollBar(GuiGraphicsExtractor context, int x, int top, int bottom, int scroll, int scrollMax) {
        if (scrollMax <= 0) return;
        int track = Math.max(1, bottom - top);
        context.fill(x, top, x + 2, bottom, 0x55000000);
        int thumb = Math.min(track, Math.max(18, (int) ((long) track * track / (track + (long) scrollMax))));
        int thumbY = top + (int) ((long) (track - thumb) * scroll / scrollMax);
        context.fill(x, thumbY, x + 2, thumbY + thumb, ACCENT);
    }

    private void updateAllWidgetVisibility() {
        for (var child : children()) {
            if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)) continue;
            if (templatePopup) {
                widget.visible = widget == templateField || widget == templateSaveButton || widget == templateBackButton;
                continue;
            }
            int x = widget.getX(), y = widget.getY(), w = widget.getWidth(), h = widget.getHeight();
            boolean fixedClose = y == bottomTop + 15 && x == width / 2 - 90 && w == 180;
            boolean fixedLibraryButton = y == 14 && x == sidebarRight - 30 && w == 24 && h == 24;
            boolean fixedSidebarNav = y == bottomTop - 26 && h == 20 && w == 22
                && (x == sidebarLeft + 6 || x == sidebarRight - 28);
            if (fixedClose || fixedSidebarNav || fixedLibraryButton) {
                widget.visible = y >= 0 && y + h <= height;
            } else if (x < sidebarLeft) {
                widget.visible = y >= 44 && y + h <= bottomTop - 8;
            } else {
                widget.visible = y >= 44 && y + h <= bottomTop - 48;
            }
        }
    }

    private void drawPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 1, PANEL_EDGE);
        ctx.fill(x, y + h - 1, x + w, y + h, PANEL_EDGE);
        ctx.fill(x, y, x + 1, y + h, PANEL_EDGE);
        ctx.fill(x + w - 1, y, x + w, y + h, PANEL_EDGE);
    }

    private void drawLabel(GuiGraphicsExtractor ctx, String text, int x, int y) {
        ctx.text(font, Component.literal(text), x, y, GRAY);
    }

    private void drawPreview(GuiGraphicsExtractor ctx, int x, int y) {
        int[] chars = inputText.codePoints().toArray();
        if (chars.length == 0) {
            ctx.text(font, Component.literal("Start typing to preview your gradient"), x, y, GRAY, false);
            return;
        }
        int px = x;
        int maxX = contentRight - 14;
        for (int i = 0; i < chars.length; i++) {
            String ch = new String(Character.toChars(chars[i]));
            int rgb = Character.isWhitespace(chars[i]) ? WHITE : GradientCalculator.colorAt(stops, GradientCalculator.charPosition(i, chars.length));
            // Draw each code point independently so color and style match the output settings.
            Component text = Component.literal(ch).withStyle(style -> StyleBooleanBridge.withUnderlined(
                StyleBooleanBridge.withStrikethrough(
                    StyleBooleanBridge.withItalic(
                        StyleBooleanBridge.withBold(style.withColor(rgb), bold), italic), strikethrough), underline));
            // GuiGraphicsExtractor uses ARGB since 1.21.6; keep the RGB gradient opaque.
            ctx.text(font, text, px, y, 0xFF000000 | (rgb & 0xFFFFFF), false);
            // Measure the styled component, not the plain glyph: bold glyph advances
            // are wider in Minecraft and otherwise accumulate visible spacing drift.
            px += Math.max(1, font.width(text));
            if (px > maxX) break;
        }
    }

    private void drawGradientBar(GuiGraphicsExtractor ctx) {
        // Line the bar up with the text fields above it, inset so the end knobs stay inside the panel.
        int fieldLeft = inputField != null ? inputField.getX() : contentLeft + 6;
        int fieldRight = inputField != null ? inputField.getX() + inputField.getWidth() : contentRight - 10;
        barLeft = fieldLeft + 8;
        barRight = fieldRight - 8;
        int y0 = barTop;
        barScreenTop = y0;
        barScreenBottom = y0 + 12;
        int barWidth = Math.max(1, barRight - barLeft);
        // Thin frame around the whole bar so it reads as one control.
        ctx.fill(barLeft - 1, y0 - 1, barRight + 1, y0 + 13, BAR_FRAME);
        for (int x = 0; x < barWidth; x++) {
            float p = barWidth <= 1 ? 0f : x / (float) (barWidth - 1);
            int color = 0xFF000000 | GradientCalculator.colorAt(stops, p);
            ctx.fill(barLeft + x, y0, barLeft + x + 1, y0 + 12, color);
        }
        // Soft top highlight and bottom shade give the bar a little depth
        // without hiding any of the gradient.
        ctx.fill(barLeft, y0, barRight, y0 + 1, 0x33FFFFFF);
        ctx.fill(barLeft, y0 + 11, barRight, y0 + 12, 0x33000000);
    }

    /** Stop markers: a thin stem through the bar and a color knob underneath, so the gradient stays visible. */
    private void drawStopHandles(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int hovered = draggedStopRef == null ? handleAt(mouseX, mouseY) : -1;
        // Unselected markers first, the selected one last so it is always on top.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < stops.size(); i++) {
                boolean selected = i == selectedStop;
                if ((pass == 1) != selected) continue;
                ColorStop stop = stops.get(i);
                drawStopHandle(ctx, positionToX(stop.position()), 0xFF000000 | stop.rgb(), selected, i == hovered);
            }
        }
        drawSelectedStopLabel(ctx);
    }

    private void drawStopHandle(GuiGraphicsExtractor ctx, int x, int rgb, boolean selected, boolean hovered) {
        int top = barScreenTop;
        int ring = selected ? ACCENT : (hovered ? 0xFFF2F2F2 : 0xFFC4C4C4);
        int half = selected ? 6 : 5;
        int size = half * 2 + 1;
        int knobTop = top + 15;
        // Stem: dark outline with a light core, running from above the bar to the knob.
        ctx.fill(x - 1, top - 3, x + 2, knobTop, 0xFF000000);
        ctx.fill(x, top - 2, x + 1, knobTop, ring);
        // Knob: dark outline, colored ring, then the stop's color.
        ctx.fill(x - half - 1, knobTop - 1, x + half + 2, knobTop + size + 1, 0xFF000000);
        ctx.fill(x - half, knobTop, x + half + 1, knobTop + size, ring);
        ctx.fill(x - half + 2, knobTop + 2, x + half - 1, knobTop + size - 2, rgb);
    }

    private void drawSelectedStopLabel(GuiGraphicsExtractor ctx) {
        if (selectedStop < 0 || selectedStop >= stops.size()) return;
        ColorStop stop = stops.get(selectedStop);
        String label = Math.round(stop.position() * 100f) + "%";
        int x = positionToX(stop.position());
        int w = font.width(label);
        int lx = x + 12;
        if (lx + w > barRight + 4) lx = x - 12 - w;
        ctx.text(font, Component.literal(label), lx, barScreenTop + 18, GRAY, false);
    }

    /** Black preview box normally; a lighter one when the typed text uses a very dark gradient. */
    private int previewFill() {
        return !inputText.isEmpty() && FlopTheme.isDark(stops) ? FlopTheme.PREVIEW_CHIP : 0xAA000000;
    }

    /** Which of the [B] [I] [S] [U] style buttons are currently switched on. */
    private boolean isStyleToggleOn(Button button) {
        String label = button.getMessage().getString();
        return (label.equals("[B]") && bold) || (label.equals("[I]") && italic)
            || (label.equals("[S]") && strikethrough) || (label.equals("[U]") && underline);
    }

    private void drawInset(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int fill) {
        ctx.fill(x1, y1, x2, y2, fill);
        ctx.fill(x1, y1, x2, y1 + 1, PANEL_EDGE);
        ctx.fill(x1, y2 - 1, x2, y2, PANEL_EDGE);
        ctx.fill(x1, y1, x1 + 1, y2, PANEL_EDGE);
        ctx.fill(x2 - 1, y1, x2, y2, PANEL_EDGE);
    }

    private void layoutColorPicker() {
        pickerPanelWidth = Math.max(210, Math.min(310, width - 20));
        // Keep the controls on-screen at high GUI scales where the logical height is small.
        pickerPanelHeight = Math.max(160, Math.min(222, height - 12));
        pickerPanelX = (width - pickerPanelWidth) / 2;
        pickerPanelY = Math.max(0, (height - pickerPanelHeight) / 2);

        pickerSquareX = pickerPanelX + 12;
        pickerSquareY = pickerPanelY + 34;
        pickerSquareWidth = pickerPanelWidth - 64;
        pickerSquareHeight = Math.max(48, Math.min(96, pickerPanelHeight - 126));
        pickerHueX = pickerPanelX + pickerPanelWidth - 38;
        pickerHueY = pickerSquareY;
        pickerHueWidth = 20;
        pickerHueHeight = pickerSquareHeight;

        pickerHexY = pickerSquareY + pickerSquareHeight + 10;
        pickerHexX = pickerPanelX + 78;
        pickerHexWidth = Math.max(72, pickerPanelWidth - 90);

        pickerButtonsY = pickerPanelY + pickerPanelHeight - 32;
        pickerButtonWidth = (pickerPanelWidth - 36) / 2;
        pickerApplyX = pickerPanelX + 12;
        pickerCancelX = pickerApplyX + pickerButtonWidth + 12;
    }

    private void buildColorPickerWidgets() {
        layoutColorPicker();
        pickerHexField = new EditBox(font, pickerHexX, pickerHexY, pickerHexWidth, 18,
            Component.literal("Hex color"));
        pickerHexField.setMaxLength(7);
        syncingPickerHex = true;
        pickerHexField.setValue(pickerHexText);
        syncingPickerHex = false;
        pickerHexField.setResponder(value -> {
            if (syncingPickerHex) return;
            pickerHexText = value;
            int parsed = GradientCalculator.parseHex(value);
            if (parsed >= 0) {
                pickerError = "";
                pickerRgb = parsed & 0xFFFFFF;
                rgbToPickerHsv(pickerRgb);
            } else if (value != null && !value.isBlank()) {
                pickerError = "Enter a valid hex color (3 or 6 digits).";
            } else {
                pickerError = "";
            }
        });
        addRenderableWidget(pickerHexField);
        addButton("Apply Color", pickerApplyX, pickerButtonsY, pickerButtonWidth, 20,
            b -> applyColorPicker());
        addButton("Cancel", pickerCancelX, pickerButtonsY, pickerButtonWidth, 20,
            b -> cancelColorPicker());
    }

    private void drawEditorBehindColorPicker(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        drawPanel(context, contentLeft, 8, contentRight - contentLeft, bottomTop - 8);
        drawPanel(context, sidebarLeft, 8, sidebarRight - sidebarLeft, bottomTop - 8);
        drawPanel(context, contentLeft, bottomTop + 6, sidebarRight - contentLeft, height - bottomTop - 6);
        context.enableScissor(sidebarLeft + 2, 44, sidebarRight - 2, bottomTop - 48);
        drawPresetCardBackgrounds(context);
        context.disableScissor();
        context.centeredText(font, Component.literal("FlopText RGB"), (contentLeft + contentRight) / 2, 16, WHITE);
        context.centeredText(font, Component.literal("Client-side RGB text gradient creator"), (contentLeft + contentRight) / 2, 34, GRAY);
        PresetSettingsScreen.drawTopButton(context, mouseX, mouseY);
        FlopIcons.drawLibrary(context, sidebarRight - 30, 14, mouseX, mouseY);
        context.enableScissor(8, 44, sidebarLeft - 4, bottomTop - 8);
        drawLabel(context, "Preview", contentLeft + 8, previewTop + 7);
        drawInset(context, contentLeft + 8, previewTop + 22, contentRight - 8, previewBottom - 6, previewFill());
        drawPreview(context, contentLeft + 14, previewTop + 31);
        drawLabel(context, "Input", contentLeft + 8, inputTop + 7);
        drawGradientBar(context);
        drawStopHandles(context, mouseX, mouseY);
        drawColorControls(context);
        drawLabel(context, "Output", contentLeft + 8, outputTop + 7);
        drawLabel(context, "Create Preset", contentLeft + 8, outputBottom - 36);
        drawMainScrollBar(context);
        context.disableScissor();
        drawLabel(context, "Favorites", sidebarLeft + 8, 16);
        drawLabel(context, "Saved gradients", sidebarLeft + 8, 34);
        context.enableScissor(sidebarLeft + 2, 44, sidebarRight - 2, bottomTop - 48);
        drawPresetCardContents(context);
        context.disableScissor();
        drawSidebarScrollBar(context);
        context.centeredText(font, Component.literal((presetPage + 1) + "/" + Math.max(1, (presetCount() + 3) / 4)),
            (sidebarLeft + sidebarRight) / 2, bottomTop - 40, GRAY);
    }

    private void drawColorPicker(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        layoutColorPicker();
        drawPanel(ctx, pickerPanelX, pickerPanelY, pickerPanelWidth, pickerPanelHeight);
        ctx.centeredText(font, Component.literal("Choose Gradient Color"), width / 2,
            pickerPanelY + 10, WHITE);
        ctx.text(font, Component.literal("Saturation / brightness"), pickerSquareX,
            pickerSquareY - 12, GRAY);
        ctx.text(font, Component.literal("Hue"), pickerHueX, pickerHueY - 12, GRAY);

        // Draw the saturation/value plane as small adjacent tiles. The 64x32-ish
        // grid keeps the picker responsive on low-end systems while remaining smooth.
        int columns = Math.max(1, (pickerSquareWidth + 2) / 3);
        int rows = Math.max(1, (pickerSquareHeight + 2) / 3);
        for (int row = 0; row < rows; row++) {
            int y1 = pickerSquareY + row * pickerSquareHeight / rows;
            int y2 = pickerSquareY + (row + 1) * pickerSquareHeight / rows;
            float value = 1f - row / (float) Math.max(1, rows - 1);
            for (int col = 0; col < columns; col++) {
                int x1 = pickerSquareX + col * pickerSquareWidth / columns;
                int x2 = pickerSquareX + (col + 1) * pickerSquareWidth / columns;
                float saturation = col / (float) Math.max(1, columns - 1);
                int color = hsvToRgb(pickerHue, saturation, value);
                ctx.fill(x1, y1, x2, y2, 0xFF000000 | color);
            }
        }
        // A simple crosshair with dark outline identifies the draggable selection point.
        int dotX = pickerSquareX + Math.round(pickerSaturation * (pickerSquareWidth - 1));
        int dotY = pickerSquareY + Math.round((1f - pickerValue) * (pickerSquareHeight - 1));
        ctx.fill(dotX - 5, dotY - 1, dotX + 6, dotY + 2, 0xFF111111);
        ctx.fill(dotX - 1, dotY - 5, dotX + 2, dotY + 6, 0xFF111111);
        ctx.fill(dotX - 4, dotY, dotX + 5, dotY + 1, WHITE);
        ctx.fill(dotX, dotY - 4, dotX + 1, dotY + 5, WHITE);
        ctx.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF222222);

        // Hue track uses one scanline per hue step; the marker follows the selected hue.
        for (int row = 0; row < pickerHueHeight; row++) {
            float hue = row / (float) Math.max(1, pickerHueHeight - 1);
            int color = hsvToRgb(hue, 1f, 1f);
            ctx.fill(pickerHueX, pickerHueY + row, pickerHueX + pickerHueWidth,
                pickerHueY + row + 1, 0xFF000000 | color);
        }
        int hueMarkerY = pickerHueY + Math.round(pickerHue * (pickerHueHeight - 1));
        ctx.fill(pickerHueX - 2, hueMarkerY - 2, pickerHueX + pickerHueWidth + 2,
            hueMarkerY + 3, 0xFF111111);
        ctx.fill(pickerHueX - 1, hueMarkerY - 1, pickerHueX + pickerHueWidth + 1,
            hueMarkerY + 2, WHITE);
        ctx.fill(pickerHueX, hueMarkerY, pickerHueX + pickerHueWidth, hueMarkerY + 1,
            0xFF222222);

        int swatchX = pickerPanelX + 12;
        int swatchY = pickerHexY - 1;
        ctx.fill(swatchX, swatchY, swatchX + 28, swatchY + 22, 0xFF000000);
        ctx.fill(swatchX + 1, swatchY + 1, swatchX + 27, swatchY + 21,
            0xFF000000 | pickerRgb);
        ctx.text(font, Component.literal("Hex"), pickerPanelX + 48, pickerHexY + 5, WHITE);
        if (!pickerError.isEmpty()) {
            ctx.text(font, Component.literal(pickerError), pickerPanelX + 12,
                pickerButtonsY - 12, RED);
        }
    }

    private void openColorPicker() {
        if (selectedStop < 0 || selectedStop >= stops.size()) return;
        pickerRgb = stops.get(selectedStop).rgb() & 0xFFFFFF;
        pickerHexText = GradientCalculator.toDisplayHex(pickerRgb);
        pickerError = "";
        rgbToPickerHsv(pickerRgb);
        colorPickerOpen = true;
        draggingColorSquare = false;
        draggingHueSlider = false;
        init();
    }

    private void cancelColorPicker() {
        colorPickerOpen = false;
        draggingColorSquare = false;
        draggingHueSlider = false;
        pickerHexField = null;
        pickerError = "";
        init();
    }

    private void applyColorPicker() {
        int parsed = GradientCalculator.parseHex(pickerHexText);
        if (parsed < 0) {
            pickerError = "Enter a valid hex color (3 or 6 digits).";
            return;
        }
        if (selectedStop >= 0 && selectedStop < stops.size()) {
            ColorStop old = stops.get(selectedStop);
            stops.set(selectedStop, new ColorStop(parsed & 0xFFFFFF, old.position()));
        }
        syncHex();
        updateOutput();
        saveUiConfig();
        colorPickerOpen = false;
        draggingColorSquare = false;
        draggingHueSlider = false;
        pickerHexField = null;
        pickerError = "";
        init();
        tell("Color stop updated.");
    }

    private void setPickerFromSquare(double mouseX, double mouseY) {
        pickerSaturation = clamp01((float) ((mouseX - pickerSquareX) / Math.max(1.0, pickerSquareWidth - 1)));
        pickerValue = 1f - clamp01((float) ((mouseY - pickerSquareY) / Math.max(1.0, pickerSquareHeight - 1)));
        updatePickerColorFromHsv();
    }

    private void setPickerHueFromY(double mouseY) {
        pickerHue = clamp01((float) ((mouseY - pickerHueY) / Math.max(1.0, pickerHueHeight - 1)));
        updatePickerColorFromHsv();
    }

    private void updatePickerColorFromHsv() {
        pickerRgb = hsvToRgb(pickerHue, pickerSaturation, pickerValue);
        pickerHexText = GradientCalculator.toDisplayHex(pickerRgb);
        pickerError = "";
        if (pickerHexField != null && !pickerHexText.equals(pickerHexField.getValue())) {
            syncingPickerHex = true;
            pickerHexField.setValue(pickerHexText);
            syncingPickerHex = false;
        }
    }

    private void rgbToPickerHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0f) hue = 0f;
        else if (max == r) hue = ((g - b) / delta) % 6f;
        else if (max == g) hue = ((b - r) / delta) + 2f;
        else hue = ((r - g) / delta) + 4f;
        pickerHue = (hue / 6f);
        if (pickerHue < 0f) pickerHue += 1f;
        pickerSaturation = max == 0f ? 0f : delta / max;
        pickerValue = max;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float s = clamp01(saturation), v = clamp01(value);
        int sector = (int) Math.floor(h);
        float fraction = h - sector;
        float p = v * (1f - s);
        float q = v * (1f - fraction * s);
        float t = v * (1f - (1f - fraction) * s);
        float r, g, b;
        switch (Math.floorMod(sector, 6)) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void drawColorControls(GuiGraphicsExtractor ctx) {
        int y = inputBottom - 28;
        int x = contentLeft + 8;
        int rgb = selectedStop >= 0 && selectedStop < stops.size() ? stops.get(selectedStop).rgb() : 0xFFFFFF;
        ctx.fill(x, y, x + 28, y + 18, 0xFF9A9A9A);
        ctx.fill(x + 1, y + 1, x + 27, y + 17, 0xFF000000 | rgb);
    }

    private void drawPresetCardBackgrounds(GuiGraphicsExtractor ctx) {
        int cardW = sidebarRight - sidebarLeft - 8;
        int cardHeight = Math.max(61, (bottomTop - 48 - 54 - 15) / 4);
        int start = presetPage * 4;
        for (int row = 0; row < 4; row++) {
            int index = start + row;
            if (index >= presetCount()) break;
            int top = 54 + row * (cardHeight + 5) - sidebarScroll;
            drawPanel(ctx, sidebarLeft + 4, top, cardW, cardHeight);
        }
    }

    private void drawPresetCardContents(GuiGraphicsExtractor ctx) {
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        int cardW = sidebarRight - sidebarLeft - 8;
        int cardHeight = Math.max(61, (bottomTop - 48 - 54 - 15) / 4);
        int start = presetPage * 4;
        for (int row = 0; row < 4; row++) {
            int index = start + row;
            if (index >= presets.size()) break;
            GradientPreset preset = presets.get(index);
            int top = 54 + row * (cardHeight + 5) - sidebarScroll;
            ctx.text(font, Component.literal(clipped(preset.name, 22)), sidebarLeft + 10, top + 6, WHITE, false);
            drawPresetPreview(ctx, preset, sidebarLeft + 10, top + 25, cardW - 12);
        }
        if (presets.isEmpty()) {
            ctx.centeredText(font, Component.literal("No favorites saved yet"),
                (sidebarLeft + sidebarRight) / 2, 70, GRAY);
        }
    }

    private void drawPresetPreview(GuiGraphicsExtractor ctx, GradientPreset preset, int x, int y, int maxW) {
        int[] chars = (preset.inputText == null ? "" : preset.inputText).codePoints().toArray();
        if (chars.length == 0) chars = "Preview".codePoints().toArray();
        int px = x;
        List<ColorStop> presetStops = preset.toStops();
        if (FlopTheme.isDark(presetStops)) FlopTheme.previewStrip(ctx, x - 4, y - 3, x + maxW + 4, y + 11);
        for (int i = 0; i < chars.length; i++) {
            String ch = new String(Character.toChars(chars[i]));
            int color = GradientCalculator.colorAt(presetStops, GradientCalculator.charPosition(i, chars.length));
            Component styled = Component.literal(ch).withStyle(style -> StyleBooleanBridge.withUnderlined(
                StyleBooleanBridge.withStrikethrough(
                    StyleBooleanBridge.withItalic(
                        StyleBooleanBridge.withBold(style.withColor(color), preset.bold), preset.italic),
                    preset.strikethrough), preset.underline));
            // Match the rendered style's advance; plain-glyph width misaligns bold text.
            int cw = Math.max(1, font.width(styled));
            if (px + cw > x + maxW) break;
            // Explicit render color is ARGB; the Style itself is configured with RGB.
            ctx.text(font, styled, px, y, 0xFF000000 | (color & 0xFFFFFF), false);
            px += cw;
        }
    }

    private void drawTemplatePopup(GuiGraphicsExtractor ctx) {
        // Match the original 1.21.11 Custom Output overlay, adapting the text
        // field width to the smaller logical GUI widths used at high GUI scales.
        ctx.fill(0, 0, width, height, 0x88000000);
        drawPanel(ctx, 12, 12, Math.max(1, width - 12), Math.max(1, height - 12));
        ctx.centeredText(font, Component.literal("Custom Output"), width / 2, 28, WHITE);
        ctx.centeredText(font, Component.literal("Build and save the custom output template here."),
            width / 2, 48, GRAY);
        ctx.centeredText(font, Component.literal("$1-$6 = hex digits    $f = active formatting    $c = character"),
            width / 2, 64, GRAY);
        ctx.centeredText(font, Component.literal("The checkbox controls whether Custom Output is active."),
            width / 2, 80, GRAY);
        drawLabel(ctx, "Template", 18, 110);
        int boxX = 12, boxY = 92, boxW = Math.max(1, width - 24), boxH = 18;
        boolean enabled = "Custom".equals(outputFormat);
        ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF171717);
        ctx.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF454545);
        int checkX = boxX + 5, checkY = boxY + 4;
        ctx.fill(checkX, checkY, checkX + 10, checkY + 10, enabled ? 0xFF7AA7FF : 0xFFB0B0B0);
        if (enabled) ctx.text(font, Component.literal("✓"), checkX + 1, checkY + 1, 0xFF101010);
        ctx.text(font, Component.literal("Enable Custom Output"), checkX + 16, boxY + 5, WHITE);
        ctx.centeredText(font, Component.literal("Save the template, then use Back to return to the main screen."),
            width / 2, 158, 0xFF777777);
        int previewY = Math.min(height - 48, 190);
        ctx.text(font, Component.literal("Live preview"), 18, previewY - 12, GRAY);
        drawPreview(ctx, Math.max(18, (width - Math.max(1, contentRight - contentLeft)) / 2), previewY);
    }

    private void openTemplatePopup() {
        templatePopup = true;
        init();
    }

    private void rebuildPopupFields() {
        int fieldW = Math.min(560, Math.max(120, width - 24));
        int fieldX = (width - fieldW) / 2;
        templateField = new EditBox(font, fieldX, 124, fieldW, 18, Component.literal("Custom template"));
        templateField.setMaxLength(256);
        templateField.setValue(customTemplate);
        templateField.setResponder(v -> customTemplate = v);
        updateTemplateFieldState();
        addRenderableWidget(templateField);
        templateSaveButton = addButton("Save Template", width / 2 - 54, 154, 108, 20, b -> {
            customTemplate = templateField == null ? DEFAULT_TEMPLATE : templateField.getValue();
            if (customTemplate == null || customTemplate.isBlank()) customTemplate = DEFAULT_TEMPLATE;
            saveUiConfig();
            closeTemplatePopup();
            updateOutput();
        });
        templateBackButton = addButton("Back", width / 2 - 90, height - 32, 180, 20, b -> closeTemplatePopup());
    }

    private void updateTemplateFieldState() {
        boolean enabled = "Custom".equals(outputFormat);
        if (templateField != null) {
            templateField.setEditable(enabled);
            templateField.active = enabled;
        }
    }

    private void closeTemplatePopup() {
        templatePopup = false;
        templateField = null;
        templateSaveButton = null;
        templateBackButton = null;
        init();
    }

    private void updateOutput() {
        if (outputField != null) outputField.setValue(compileOutput());
    }

    private String compileOutput() {
        if (inputText == null || inputText.isEmpty()) return "";
        int[] chars = inputText.replaceAll("(?! )\\s+", "").codePoints().toArray();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            int cp = chars[i];
            if (Character.isWhitespace(cp)) { out.append(' '); continue; }
            String hex = GradientCalculator.toDisplayHex(GradientCalculator.colorAt(stops, GradientCalculator.charPosition(i, chars.length))).substring(1);
            String ch = new String(Character.toChars(cp));
            switch (outputFormat) {
                case "MiniMessage" -> out.append("<#").append(hex).append('>').append(miniStylesOpen()).append(escapeMini(ch)).append(miniStylesClose()).append("</#").append(hex).append('>');
                case "Vanilla / Spigot" -> out.append("&x").append(hex.chars().mapToObj(c -> "&" + (char)c).reduce("", String::concat)).append(styleAmpersands()).append(ch);
                case "Custom" -> out.append(applyTemplate(customTemplate, hex, ch));
                default -> out.append("&#").append(hex).append(styleAmpersands()).append(ch);
            }
        }
        return out.toString();
    }

    private String miniStylesOpen() {
        StringBuilder s = new StringBuilder();
        if (bold) s.append("<bold>"); if (italic) s.append("<italic>");
        if (strikethrough) s.append("<strikethrough>"); if (underline) s.append("<underlined>");
        return s.toString();
    }
    private String miniStylesClose() {
        StringBuilder s = new StringBuilder();
        if (underline) s.append("</underlined>"); if (strikethrough) s.append("</strikethrough>");
        if (italic) s.append("</italic>"); if (bold) s.append("</bold>");
        return s.toString();
    }
    private String styleAmpersands() {
        StringBuilder s = new StringBuilder();
        if (bold) s.append("&l"); if (italic) s.append("&o");
        if (strikethrough) s.append("&m"); if (underline) s.append("&n");
        return s.toString();
    }
    private static String escapeMini(String s) { return s.replace("\\", "\\\\").replace("<", "\\<"); }
    private String applyTemplate(String template, String hex, String ch) {
        if (template == null || template.isEmpty()) template = DEFAULT_TEMPLATE;
        String result = template;
        for (int i = 0; i < 6; i++) result = result.replace("$" + (i + 1), String.valueOf(hex.charAt(i)));
        return result.replace("$f", styleAmpersands()).replace("$c", ch);
    }

    private void copyOutput() {
        // Use Minecraft's clipboard abstraction so 26.1.2 routes through its active platform manager.
        TextFieldHelper.setClipboardContents(Minecraft.getInstance(), compileOutput());
        tell("Output copied to clipboard.");
    }

    private void syncHex() {
        if (selectedStop >= 0 && selectedStop < stops.size()) hexText = GradientCalculator.toDisplayHex(stops.get(selectedStop).rgb());
        if (hexField != null) hexField.setValue(hexText);
    }

    private void applyHex() {
        int rgb = GradientCalculator.parseHex(hexText);
        if (rgb < 0) return;
        if (selectedStop >= 0 && selectedStop < stops.size()) {
            ColorStop old = stops.get(selectedStop);
            stops.set(selectedStop, new ColorStop(rgb, old.position()));
            updateOutput(); saveUiConfig(); tell("Color stop updated.");
        }
    }

    private void openPicker() {
        Compat.setScreen(new ColorPickerScreen(this, this::setSelectedColor));
    }

    public void setSelectedColor(int rgb) {
        if (selectedStop < 0 || selectedStop >= stops.size()) return;
        ColorStop old = stops.get(selectedStop);
        stops.set(selectedStop, new ColorStop(rgb & 0xFFFFFF, old.position()));
        syncHex(); updateOutput();
        Compat.setScreen(this);
    }

    private void removeSelectedStop() {
        // The original keeps the first and last stops locked and silently ignores
        // removal when only two stops remain.
        if (stops.size() <= 2 || selectedStop <= 0 || selectedStop >= stops.size() - 1) return;
        stops.remove(selectedStop);
        selectedStop = Math.min(selectedStop, stops.size() - 1);
        syncHex();
        updateOutput();
        init();
    }

    private void loadPreset(int index) {
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        if (index < 0 || index >= presets.size()) return;
        applyPreset(presets.get(index));
        tell("Preset loaded.");
        init();
    }

    public void applyPreset(GradientPreset preset) {
        if (preset == null) return;
        inputText = preset.inputText == null ? "" : preset.inputText;
        if (inputField != null) inputField.setValue(inputText);
        stops.clear();
        stops.addAll(preset.toStops());
        loadDefaultStopsIfNeeded();
        sortStops();
        selectedStop = 0;
        bold = preset.bold; italic = preset.italic; strikethrough = preset.strikethrough; underline = preset.underline;
        outputFormat = preset.outputFormat == null ? "Legacy Hex" : preset.outputFormat;
        customTemplate = preset.customTemplate == null ? DEFAULT_TEMPLATE : preset.customTemplate;
        syncHex();
        updateOutput();
        saveUiConfig();
    }

    public GradientPreset snapshotCurrentPreset(String name) {
        return new GradientPreset(name, inputText, stops, bold, italic, strikethrough, underline, outputFormat, customTemplate);
    }

    public String getPresetName() {
        return presetNameField != null ? presetNameField.getValue().trim() : (presetName == null ? "" : presetName.trim());
    }

    public void setPresetName(String value) {
        presetName = value == null ? "" : value;
        if (presetNameField != null) presetNameField.setValue(presetName);
    }

    public void refreshFromPresetLibrary() { updateOutput(); init(); }

    private void deletePreset(int index) {
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        if (index >= 0 && index < presets.size()) {
            presets.remove(index); FlopTextRGBConfig.save();
            presetPage = Math.min(presetPage, Math.max(0, (presets.size() - 1) / 4)); init(); tell("Preset deleted.");
        }
    }

    private void changePage(int delta) {
        int pages = Math.max(1, (presetCount() + 3) / 4);
        presetPage = Math.floorMod(presetPage + delta, pages);
        init();
    }

    private void selectStop(int index) {
        if (index < 0 || index >= stops.size()) return;
        selectedStop = index; syncHex();
    }

    private int handleAt(double x, double y) {
        for (int i = 0; i < stops.size(); i++) {
            int hx = positionToX(stops.get(i).position());
            if (Math.abs(x - hx) <= 7 && y >= barScreenTop - 4 && y <= barScreenTop + 30) return i;
        }
        return -1;
    }

    private void addStop(float position) {
        position = Math.max(MIN_STOP_GAP, Math.min(1f - MIN_STOP_GAP, position));
        int color = GradientCalculator.colorAt(stops, position);
        stops.add(new ColorStop(color, position));
        sortStops();
        for (int i = 0; i < stops.size(); i++) {
            if (Math.abs(stops.get(i).position() - position) < 0.0001f) selectedStop = i;
        }
        syncHex();
        updateOutput();
    }

    private int closestStop(float pos) {
        int closest = 0; float distance = Float.MAX_VALUE;
        for (int i = 0; i < stops.size(); i++) { float d = Math.abs(stops.get(i).position() - pos); if (d < distance) { distance = d; closest = i; } }
        return closest;
    }
    private int positionToX(float position) { return barLeft + Math.round(position * Math.max(1, barRight - barLeft)); }
    private float xToPosition(double x) { return Math.max(0f, Math.min(1f, (float)((x - barLeft) / Math.max(1.0, barRight - barLeft)))); }
    private void sortStops() { stops.sort(Comparator.comparingDouble(ColorStop::position)); selectedStop = Math.max(0, Math.min(selectedStop, stops.size() - 1)); }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        int button = click.button();
        if (colorPickerOpen) {
            if (Compat.isLeft(button) && mouseX >= pickerSquareX && mouseX < pickerSquareX + pickerSquareWidth
                    && mouseY >= pickerSquareY && mouseY < pickerSquareY + pickerSquareHeight) {
                draggingColorSquare = true;
                draggingHueSlider = false;
                setPickerFromSquare(mouseX, mouseY);
                return true;
            }
            if (Compat.isLeft(button) && mouseX >= pickerHueX && mouseX < pickerHueX + pickerHueWidth
                    && mouseY >= pickerHueY && mouseY < pickerHueY + pickerHueHeight) {
                draggingHueSlider = true;
                draggingColorSquare = false;
                setPickerHueFromY(mouseY);
                return true;
            }
            boolean withinPicker = mouseX >= pickerPanelX && mouseX < pickerPanelX + pickerPanelWidth
                && mouseY >= pickerPanelY && mouseY < pickerPanelY + pickerPanelHeight;
            if (withinPicker) super.mouseClicked(click, doubled);
            return true; // Modal input never leaks through to the editor below.
        }
        if (!templatePopup && PresetSettingsScreen.handleSettingsClick(this, click)) return true;
        if (!templatePopup && Compat.isLeft(button) && mouseX >= contentLeft + 8 && mouseX < contentLeft + 36
                && mouseY >= inputBottom - 28 && mouseY < inputBottom - 10) {
            openColorPicker();
            return true;
        }
        if (templatePopup) {
            // The v1.21.11 checkbox toggles Custom output on/off. Keep its hitbox
            // aligned with the drawn control rather than using a guessed widget box.
            int boxX = 12, boxY = 92, boxW = Math.max(1, width - 24), boxH = 18;
            if (Compat.isLeft(button) && mouseX >= boxX && mouseX < boxX + boxW
                    && mouseY >= boxY && mouseY < boxY + boxH) {
                outputFormat = "Custom".equals(outputFormat) ? "Legacy Hex" : "Custom";
                updateTemplateFieldState();
                saveUiConfig();
                updateOutput();
                return true;
            }
            boolean overPopup = mouseX >= 12 && mouseX < width && mouseY >= 12 && mouseY < height;
            if (!overPopup) return true;
        }
        int hit = handleAt(mouseX, mouseY);
        if (Compat.isLeft(button) && hit >= 0) {
            selectStop(hit);
            draggedStop = hit;
            draggedStopRef = stops.get(hit);
            draggedEndpoint = hit == 0 || hit == stops.size() - 1;
            return true;
        }
        if (Compat.isLeft(button) && mouseY >= barScreenTop && mouseY <= barScreenBottom && mouseX >= barLeft && mouseX <= barRight) {
            addStop(xToPosition(mouseX)); return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (colorPickerOpen && Compat.isLeft(click.button())) {
            if (draggingColorSquare) {
                setPickerFromSquare(mouseX, mouseY);
                return true;
            }
            if (draggingHueSlider) {
                setPickerHueFromY(mouseY);
                return true;
            }
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (draggedStopRef != null && Compat.isLeft(click.button())) {
            if (draggedEndpoint) return true;
            int currentIndex = indexOfIdentity(draggedStopRef);
            if (currentIndex < 0) {
                draggedStopRef = null;
                draggedStop = -1;
                return false;
            }
            float pos = Math.max(MIN_STOP_GAP, Math.min(1f - MIN_STOP_GAP, xToPosition(mouseX)));
            ColorStop moved = new ColorStop(draggedStopRef.rgb(), pos);
            stops.set(currentIndex, moved);
            stops.sort(Comparator.comparingDouble(ColorStop::position));
            draggedStopRef = moved;
            draggedStop = indexOfIdentity(moved);
            selectedStop = Math.max(0, draggedStop);
            syncHex();
            updateOutput();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private int indexOfIdentity(ColorStop target) {
        for (int i = 0; i < stops.size(); i++) if (stops.get(i) == target) return i;
        return -1;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingColorSquare = false;
        draggingHueSlider = false;
        draggedStop = -1;
        draggedStopRef = null;
        draggedEndpoint = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (templatePopup || colorPickerOpen) return true;
        if (mouseY >= 44 && mouseY < bottomTop) {
            if (mouseX >= contentLeft && mouseX < sidebarLeft) {
                if (mainScrollMax <= 0) return false;
                int next = clamp(mainScroll - (int) Math.round(verticalAmount * 18.0), 0, mainScrollMax);
                if (next == mainScroll) return true;
                mainScroll = next;
                init();
                return true;
            }
            if (mouseX >= sidebarLeft && mouseX <= sidebarRight) {
                if (sidebarScrollMax <= 0) return false;
                int next = clamp(sidebarScroll - (int) Math.round(verticalAmount * 18.0), 0, sidebarScrollMax);
                if (next == sidebarScroll) return true;
                sidebarScroll = next;
                init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (colorPickerOpen) {
            cancelColorPicker();
            return;
        }
        saveUiConfig();
        super.onClose();
    }
    public void closeScreen() { onClose(); }

    private void savePreset() {
        String name = presetNameField != null ? presetNameField.getValue().trim() : presetName.trim();
        if (name.isEmpty()) { tell("Enter a preset name first."); return; }
        presetName = name;
        PresetDestinationScreen.openFromMain(this);
    }

    private int presetCount() { return FlopTextRGBConfig.get().presets.size(); }
    private void tell(String message) {
        notice = message == null ? "" : message;
        noticeTicks = 80;
    }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    private static String clipped(String value, int limit) { if (value == null) return ""; return value.length() <= limit ? value : value.substring(0, Math.max(0, limit - 1)) + "…"; }
}

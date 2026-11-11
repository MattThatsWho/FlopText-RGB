package com.yourname.floptext_rgb.screen;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.library.CommunityPresetScreen;
import com.yourname.floptext_rgb.library.PresetDestinationScreen;
import com.yourname.floptext_rgb.library.PresetLibraryScreen;
import com.yourname.floptext_rgb.library.PresetLibraryStore;
import com.yourname.floptext_rgb.library.PresetSettingsScreen;
import com.yourname.floptext_rgb.library.FlopIcons;
import com.yourname.floptext_rgb.math.GradientCalculator;
import com.yourname.floptext_rgb.math.GradientCalculator.ColorStop;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Editable reconstruction of the original v1.7.0 gradient editor.
 * It is based on the shipped bytecode and metadata; it is not claimed to be
 * the original authoring source file. Preserve the reference JAR for comparison.
 */
public final class FlopTextRGBScreen extends Screen {
    private static final int WHITE = 0xFFF2F5FF;
    private static final int GRAY = 0xFF9BA9C0;
    private static final int PANEL = 0xFF111B2B;
    private static final int PANEL_EDGE = 0xFF2B4264;
    private static final int ACCENT = 0xFF438DFF;
    private static final int GREEN = 0xFF70D6A5;
    private static final int RED = 0xFFFF7272;
    private static final float MIN_STOP_GAP = 0.035f;
    private static final String DEFAULT_TEMPLATE = "$1$2$3$4$5$6$f$c";
    private static final List<String> OUTPUT_FORMATS = List.of("Legacy Hex", "MiniMessage", "Vanilla / Spigot", "Custom");

    private final List<ColorStop> stops = new ArrayList<>();
    private int selectedStop = 0;
    private int draggedStop = -1;
    private String inputText = "Example Text";
    private String hexText = "#FF3D9A";
    private String presetName = "";
    private int presetPage = 0;
    private boolean bold, italic, strikethrough, underline;
    private String outputFormat = "Legacy Hex";
    private String customTemplate = DEFAULT_TEMPLATE;
    private boolean templatePopup;
    private TextFieldWidget inputField, outputField, hexField, presetNameField, templateField;
    private int contentLeft, contentRight, sidebarLeft, sidebarRight;
    private int previewTop, previewBottom, inputTop, inputBottom, outputTop, outputBottom;
    private int barLeft, barRight, barTop, barBottom, barScreenTop, barScreenBottom, bottomTop;
    private int mainScroll, sidebarScroll, mainScrollMax, sidebarScrollMax;
    private String notice = "";
    private int noticeTicks;

    public FlopTextRGBScreen() {
        super(Text.literal("FlopText RGB"));
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

    private void loadDefaultStopsIfNeeded() {
        if (stops.isEmpty()) {
            stops.add(new ColorStop(0xFF3D9A, 0f));
            stops.add(new ColorStop(0xFFB547, 0.5f));
            stops.add(new ColorStop(0x4CD9FF, 1f));
        }
        sortStops();
        selectedStop = Math.max(0, Math.min(selectedStop, stops.size() - 1));
    }

    @Override
    protected void init() {
        clearChildren();
        int margin = 16;
        int usableW = Math.max(300, width - margin * 2);
        boolean wide = width >= 760;
        int sideW = wide ? Math.max(205, Math.min(265, usableW / 3)) : usableW;
        contentLeft = margin;
        contentRight = wide ? width - margin - sideW - 12 : width - margin;
        sidebarLeft = wide ? contentRight + 12 : margin;
        sidebarRight = width - margin;
        int contentW = Math.max(260, contentRight - contentLeft);
        previewTop = 100;
        previewBottom = 160;
        inputTop = 180;
        inputBottom = 235;
        outputTop = 255;
        outputBottom = 310;
        barTop = 350;
        barBottom = 388;
        bottomTop = Math.max(415, height - 70);
        mainScrollMax = Math.max(0, bottomTop + 120 - (height - 20));
        int visiblePresetCount = Math.min(4, Math.max(0, presetCount() - presetPage * 4));
        sidebarScrollMax = Math.max(0, 66 + visiblePresetCount * 66 - (height - 110));
        mainScroll = clamp(mainScroll, 0, mainScrollMax);
        sidebarScroll = clamp(sidebarScroll, 0, sidebarScrollMax);

        buildTextFields();
        buildButtons();
        buildSidebar();
    }

    private void buildTextFields() {
        int x = contentLeft + 10;
        int w = Math.max(120, contentRight - contentLeft - 20);
        inputField = new TextFieldWidget(textRenderer, x, inputTop + 22 - mainScroll, w, 20, Text.literal("Input"));
        inputField.setMaxLength(512);
        inputField.setText(inputText);
        inputField.setChangedListener(value -> { inputText = value; updateOutput(); });
        addDrawableChild(inputField);

        outputField = new TextFieldWidget(textRenderer, x, outputTop + 22 - mainScroll, w, 20, Text.literal("Generated output"));
        outputField.setMaxLength(32767);
        outputField.setEditable(false);
        outputField.setText(compileOutput());
        addDrawableChild(outputField);

        hexField = new TextFieldWidget(textRenderer, contentLeft + 10, barBottom + 38 - mainScroll, 96, 20, Text.literal("Hex color"));
        hexField.setMaxLength(7);
        hexField.setText(hexText);
        hexField.setChangedListener(value -> hexText = value);
        addDrawableChild(hexField);

        presetNameField = new TextFieldWidget(textRenderer, sidebarLeft + 8, height - 94, Math.max(100, sidebarRight - sidebarLeft - 16), 20, Text.literal("Preset name"));
        presetNameField.setMaxLength(64);
        presetNameField.setText(presetName);
        presetNameField.setChangedListener(value -> presetName = value);
        if (width >= 760) addDrawableChild(presetNameField);
    }

    private void buildButtons() {
        int x = contentLeft + 10;
        int w = Math.max(120, contentRight - contentLeft - 20);
        int stylesY = 72;
        int gap = 5;
        int sw = Math.max(42, (w - gap * 3 - 86) / 4);
        addButton("B" + (bold ? " ✓" : ""), x, stylesY, sw, 20, b -> { bold = !bold; updateOutput(); init(); });
        addButton("I" + (italic ? " ✓" : ""), x + sw + gap, stylesY, sw, 20, b -> { italic = !italic; updateOutput(); init(); });
        addButton("S" + (strikethrough ? " ✓" : ""), x + (sw + gap) * 2, stylesY, sw, 20, b -> { strikethrough = !strikethrough; updateOutput(); init(); });
        addButton("U" + (underline ? " ✓" : ""), x + (sw + gap) * 3, stylesY, sw, 20, b -> { underline = !underline; updateOutput(); init(); });
        addButton("Format", x + (sw + gap) * 4, stylesY, 82, 20, b -> cycleFormat());

        int half = Math.max(70, (w - 8) / 2);
        addButton("Apply Hex", x, barBottom + 38 - mainScroll, half, 20, b -> applyHex());
        addButton("Pick Screen Color", x + half + 8, barBottom + 38 - mainScroll, w - half - 8, 20, b -> openPicker());
        addButton("Add Stop", x, barBottom + 64 - mainScroll, half, 20, b -> addStop(0.5f));
        addButton("Remove Stop", x + half + 8, barBottom + 64 - mainScroll, w - half - 8, 20, b -> removeSelectedStop());
        addButton("Custom Output", x, barBottom + 90 - mainScroll, half, 20, b -> openTemplatePopup());
        addButton("Copy Output", x + half + 8, barBottom + 90 - mainScroll, w - half - 8, 20, b -> copyOutput());

        addButton("Library", contentLeft + 10, height - 34, 90, 22, b -> MinecraftClient.getInstance().setScreen(new PresetLibraryScreen(this)));
        addButton("Close", Math.max(contentLeft + 110, contentRight - 92), height - 34, 82, 22, b -> closeScreen());
    }

    private void buildSidebar() {
        if (width < 760) {
            addButton("Preset Library", sidebarLeft, 438, Math.max(100, sidebarRight - sidebarLeft), 20,
                b -> MinecraftClient.getInstance().setScreen(new PresetLibraryScreen(this)));
            return;
        }
        int x = sidebarLeft + 8;
        int w = Math.max(120, sidebarRight - sidebarLeft - 16);
        addButton("Save Preset", x, height - 66, w, 22, b -> savePreset());
        addButton("‹", x, height - 34, 30, 22, b -> changePage(-1));
        addButton("›", sidebarRight - 38, height - 34, 30, 22, b -> changePage(1));
        addButton("Open Library", x + 35, height - 34, Math.max(60, w - 70), 22,
            b -> MinecraftClient.getInstance().setScreen(new PresetLibraryScreen(this)));
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        int start = presetPage * 4;
        for (int i = start; i < Math.min(presets.size(), start + 4); i++) {
            final int index = i;
            int top = 66 + (i - start) * 66 - sidebarScroll;
            if (top < 58 || top + 60 > height - 108) continue;
            int buttonGap = 5;
            int loadWidth = Math.max(54, (w - buttonGap) * 2 / 3);
            int deleteWidth = Math.max(44, w - buttonGap - loadWidth);
            addButton("Load", x, top + 39, loadWidth, 18, b -> loadPreset(index));
            addButton("Delete", x + loadWidth + buttonGap, top + 39, deleteWidth, 18, b -> deletePreset(index));
        }
    }

    private ButtonWidget addButton(String label, int x, int y, int w, int h, ButtonWidget.PressAction action) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, Math.max(1, w), h).build();
        addDrawableChild(button);
        return button;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawPanel(context, contentLeft, 10, contentRight - contentLeft, height - 54);
        if (width >= 760) drawPanel(context, sidebarLeft, 10, sidebarRight - sidebarLeft, height - 54);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("FlopText RGB"), (contentLeft + contentRight) / 2, 18, WHITE);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Client-side RGB text gradient creator"), (contentLeft + contentRight) / 2, 34, GRAY);
        FlopIcons.drawMain(context, mouseX, mouseY);
        drawLabel(context, "Text formatting", contentLeft + 10, 58);
        drawPanel(context, contentLeft + 8, previewTop - mainScroll, contentRight - contentLeft - 16, previewBottom - previewTop);
        drawLabel(context, "Preview", contentLeft + 16, previewTop - mainScroll + 7);
        drawPreview(context, contentLeft + 18, previewTop - mainScroll + 29);
        drawPanel(context, contentLeft + 8, inputTop - mainScroll, contentRight - contentLeft - 16, inputBottom - inputTop);
        drawLabel(context, "Input", contentLeft + 16, inputTop - mainScroll + 7);
        drawPanel(context, contentLeft + 8, outputTop - mainScroll, contentRight - contentLeft - 16, outputBottom - outputTop);
        drawLabel(context, "Output", contentLeft + 16, outputTop - mainScroll + 7);
        drawPanel(context, contentLeft + 8, barTop - mainScroll, contentRight - contentLeft - 16, 35);
        drawLabel(context, "Gradient stops — click the bar to add a stop", contentLeft + 10, barTop - mainScroll - 14);
        drawGradientBar(context);
        drawStopHandles(context);
        if (width >= 760) {
            drawLabel(context, "Preset quick access", sidebarLeft + 10, 24);
            drawLabel(context, "Favorites", sidebarLeft + 10, 44);
            drawPresetCards(context);
            context.drawText(textRenderer, FlopTextRGBConfig.get().presets.isEmpty() ? "No presets saved" : "Page " + (presetPage + 1), sidebarLeft + 10, height - 50, GRAY, false);
        }
        if (!notice.isEmpty() && noticeTicks > 0) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), (contentLeft + contentRight) / 2, height - 52, notice.startsWith("Could") ? RED : GREEN);
            noticeTicks--;
        }
        if (templatePopup) drawTemplatePopup(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 1, PANEL_EDGE);
        ctx.fill(x, y + h - 1, x + w, y + h, PANEL_EDGE);
        ctx.fill(x, y, x + 1, y + h, PANEL_EDGE);
        ctx.fill(x + w - 1, y, x + w, y + h, PANEL_EDGE);
    }

    private void drawLabel(DrawContext ctx, String text, int x, int y) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(text), x, y, GRAY);
    }

    private void drawPreview(DrawContext ctx, int x, int y) {
        int[] chars = inputText.codePoints().toArray();
        if (chars.length == 0) {
            ctx.drawText(textRenderer, Text.literal("Type something to preview your gradient"), x, y, GRAY, false);
            return;
        }
        int px = x;
        int maxX = contentRight - 14;
        for (int i = 0; i < chars.length; i++) {
            String ch = new String(Character.toChars(chars[i]));
            int rgb = Character.isWhitespace(chars[i]) ? WHITE : GradientCalculator.colorAt(stops, GradientCalculator.charPosition(i, chars.length));
            // Draw each code point independently so color and style match the output settings.
            Text text = Text.literal(ch).styled(style -> style
                .withColor(rgb)
                .withBold(bold)
                .withItalic(italic)
                .withStrikethrough(strikethrough)
                .withUnderline(underline));
            ctx.drawText(textRenderer, text, px, y, rgb, false);
            px += Math.max(1, textRenderer.getWidth(ch));
            if (px > maxX) break;
        }
    }

    private void drawGradientBar(DrawContext ctx) {
        int x0 = contentLeft + 12;
        int x1 = contentRight - 12;
        barLeft = x0; barRight = x1;
        int y0 = barTop - mainScroll + 4;
        barScreenTop = y0;
        barScreenBottom = y0 + 27;
        int width = Math.max(1, x1 - x0);
        for (int x = 0; x < width; x++) {
            float p = width <= 1 ? 0f : x / (float) (width - 1);
            int color = 0xFF000000 | GradientCalculator.colorAt(stops, p);
            ctx.fill(x0 + x, y0, x0 + x + 1, y0 + 27, color);
        }
        ctx.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
        ctx.fill(x0, y0 + 26, x1, y0 + 27, 0xFF000000);
    }

    private void drawStopHandles(DrawContext ctx) {
        for (int i = 0; i < stops.size(); i++) {
            ColorStop stop = stops.get(i);
            int x = positionToX(stop.position());
            int y = barScreenBottom;
            int c = 0xFF000000 | stop.rgb();
            ctx.fill(x - 5, y - 1, x + 6, y + 7, i == selectedStop ? 0xFFFFFFFF : PANEL_EDGE);
            ctx.fill(x - 4, y, x + 5, y + 6, c);
            if (i == selectedStop) ctx.fill(x - 4, y + 5, x + 5, y + 6, 0xFFFFFFFF);
        }
    }

    private void drawPresetCards(DrawContext ctx) {
        List<GradientPreset> presets = FlopTextRGBConfig.get().presets;
        int x = sidebarLeft + 8;
        int w = sidebarRight - sidebarLeft - 16;
        int start = presetPage * 4;
        for (int i = start; i < Math.min(start + 4, presets.size()); i++) {
            GradientPreset p = presets.get(i);
            int top = 66 + (i - start) * 66 - sidebarScroll;
            if (top < 58 || top + 60 > height - 108) continue;
            drawPanel(ctx, x, top, w, 60);
            ctx.drawText(textRenderer, Text.literal(clipped(p.name, 22)), x + 6, top + 5, WHITE, false);
            drawPresetPreview(ctx, p, x + 6, top + 22, w - 12);
        }
    }

    private void drawPresetPreview(DrawContext ctx, GradientPreset preset, int x, int y, int maxW) {
        int[] chars = (preset.inputText == null ? "" : preset.inputText).codePoints().toArray();
        if (chars.length == 0) chars = "Preview".codePoints().toArray();
        int px = x;
        List<ColorStop> presetStops = preset.toStops();
        for (int i = 0; i < chars.length; i++) {
            String ch = new String(Character.toChars(chars[i]));
            int color = GradientCalculator.colorAt(presetStops, GradientCalculator.charPosition(i, chars.length));
            int cw = textRenderer.getWidth(ch);
            if (px + cw > x + maxW) break;
            Text styled = Text.literal(ch).styled(style -> style
                .withColor(color)
                .withBold(preset.bold)
                .withItalic(preset.italic)
                .withStrikethrough(preset.strikethrough)
                .withUnderline(preset.underline));
            ctx.drawText(textRenderer, styled, px, y, color, false);
            px += cw;
        }
    }

    private void drawTemplatePopup(DrawContext ctx) {
        int w = Math.min(360, width - 28), h = 125;
        int x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(0, 0, width, height, 0x99000000);
        drawPanel(ctx, x, y, w, h);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Custom Output Template"), width / 2, y + 10, WHITE);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Use $1–$6 for hex digits, $f for styles, $c for the character"), x + 10, y + 30, GRAY);
        if (templateField != null) templateField.setPosition(x + 10, y + 50);
    }

    private void openTemplatePopup() {
        templatePopup = true;
        templateField = new TextFieldWidget(textRenderer, width / 2 - 165, height / 2 - 10, 330, 20, Text.literal("Template"));
        templateField.setMaxLength(200);
        templateField.setText(customTemplate);
        templateField.setChangedListener(v -> customTemplate = v);
        addDrawableChild(templateField);
        addButton("Apply", width / 2 - 90, height / 2 + 30, 82, 20, b -> { customTemplate = templateField.getText(); saveUiConfig(); closeTemplatePopup(); });
        addButton("Cancel", width / 2 + 8, height / 2 + 30, 82, 20, b -> closeTemplatePopup());
    }

    private void closeTemplatePopup() {
        templatePopup = false;
        templateField = null;
        init();
    }

    private void updateOutput() {
        if (outputField != null) outputField.setText(compileOutput());
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
        MinecraftClient.getInstance().keyboard.setClipboard(compileOutput());
        tell("Output copied.");
    }

    private void syncHex() {
        if (selectedStop >= 0 && selectedStop < stops.size()) hexText = GradientCalculator.toDisplayHex(stops.get(selectedStop).rgb());
        if (hexField != null) hexField.setText(hexText);
    }

    private void applyHex() {
        int rgb = GradientCalculator.parseHex(hexText);
        if (rgb < 0) { tell("Could not apply: enter a valid hex color."); return; }
        if (selectedStop >= 0 && selectedStop < stops.size()) {
            ColorStop old = stops.get(selectedStop);
            stops.set(selectedStop, new ColorStop(rgb, old.position()));
            updateOutput(); saveUiConfig(); tell("Color stop updated.");
        }
    }

    private void openPicker() {
        MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, this::setSelectedColor));
    }

    public void setSelectedColor(int rgb) {
        if (selectedStop < 0 || selectedStop >= stops.size()) return;
        ColorStop old = stops.get(selectedStop);
        stops.set(selectedStop, new ColorStop(rgb & 0xFFFFFF, old.position()));
        syncHex(); updateOutput();
        MinecraftClient.getInstance().setScreen(this);
    }

    private void removeSelectedStop() {
        if (stops.size() <= 2) { tell("A gradient needs at least two color stops."); return; }
        if (selectedStop <= 0 || selectedStop >= stops.size() - 1) { tell("The first and last stops are locked."); return; }
        stops.remove(selectedStop); selectedStop = Math.max(0, selectedStop - 1); sortStops(); syncHex(); updateOutput(); init();
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
        if (inputField != null) inputField.setText(inputText);
        stops.clear(); stops.addAll(preset.toStops()); loadDefaultStopsIfNeeded();
        bold = preset.bold; italic = preset.italic; strikethrough = preset.strikethrough; underline = preset.underline;
        outputFormat = preset.outputFormat == null ? "Legacy Hex" : preset.outputFormat;
        customTemplate = preset.customTemplate == null ? DEFAULT_TEMPLATE : preset.customTemplate;
        updateOutput(); saveUiConfig();
    }

    public GradientPreset snapshotCurrentPreset(String name) {
        return new GradientPreset(name, inputText, stops, bold, italic, strikethrough, underline, outputFormat, customTemplate);
    }

    public String getPresetName() {
        return presetNameField != null ? presetNameField.getText().trim() : (presetName == null ? "" : presetName.trim());
    }

    public void setPresetName(String value) {
        presetName = value == null ? "" : value;
        if (presetNameField != null) presetNameField.setText(presetName);
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
            if (Math.abs(x - hx) <= 7 && y >= barScreenTop - 5 && y <= barScreenBottom + 10) return i;
        }
        return -1;
    }

    private void addStop(float position) {
        if (stops.size() >= 12) { tell("You can use up to 12 stops."); return; }
        position = Math.max(MIN_STOP_GAP, Math.min(1f - MIN_STOP_GAP, position));
        for (ColorStop stop : stops) if (Math.abs(stop.position() - position) < MIN_STOP_GAP) { tell("Color stops are too close together."); return; }
        int color = GradientCalculator.colorAt(stops, position);
        stops.add(new ColorStop(color, position)); sortStops();
        selectedStop = closestStop(position); syncHex(); updateOutput(); init();
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
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        int button = click.button();
        if (!templatePopup && PresetSettingsScreen.handleSettingsClick(this, click)) return true;
        if (templatePopup) {
            int popupW = Math.min(360, width - 28);
            int popupH = 125;
            int popupX = (width - popupW) / 2;
            int popupY = (height - popupH) / 2;
            if (mouseX < popupX || mouseX >= popupX + popupW
                    || mouseY < popupY || mouseY >= popupY + popupH) return true;
        }
        int hit = handleAt(mouseX, mouseY);
        if (button == 0 && hit >= 0) { selectStop(hit); draggedStop = hit; return true; }
        if (button == 0 && mouseY >= barScreenTop && mouseY <= barScreenBottom && mouseX >= barLeft && mouseX <= barRight) {
            addStop(xToPosition(mouseX)); return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        if (draggedStop >= 0 && draggedStop < stops.size() && click.button() == 0) {
            if (draggedStop == 0 || draggedStop == stops.size() - 1) return true;
            float min = stops.get(draggedStop - 1).position() + MIN_STOP_GAP;
            float max = stops.get(draggedStop + 1).position() - MIN_STOP_GAP;
            float pos = Math.max(min, Math.min(max, xToPosition(mouseX)));
            ColorStop old = stops.get(draggedStop);
            stops.set(draggedStop, new ColorStop(old.rgb(), pos));
            sortStops(); updateOutput();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggedStop = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (templatePopup) return true;
        if (width >= 760 && mouseX >= sidebarLeft) sidebarScroll = clamp(sidebarScroll - (int)Math.signum(verticalAmount) * 24, 0, sidebarScrollMax);
        else mainScroll = clamp(mainScroll - (int)Math.signum(verticalAmount) * 24, 0, mainScrollMax);
        init();
        return true;
    }

    @Override
    public void close() { saveUiConfig(); super.close(); }
    public void closeScreen() { close(); }

    private void savePreset() {
        String name = presetNameField != null ? presetNameField.getText().trim() : presetName.trim();
        if (name.isEmpty()) { tell("Enter a preset name first."); return; }
        presetName = name;
        PresetDestinationScreen.openFromMain(this);
    }

    private int presetCount() { return FlopTextRGBConfig.get().presets.size(); }
    private void tell(String message) { notice = message; noticeTicks = 120; }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    private static String clipped(String value, int limit) { if (value == null) return ""; return value.length() <= limit ? value : value.substring(0, Math.max(0, limit - 1)) + "…"; }
}

package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Searchable, filterable catalogue of bundled Community Presets. */
public final class CommunityPresetScreen extends Screen {
    private static final int WHITE = 0xFFF2F5FF, MUTED = 0xFF9BA9C0, ACCENT = 0xFF438DFF;
    private static final int PANEL = 0xFF111B2B, CARD = 0xFF162338, BTN_FILL = 0xFF253B58;
    private static final int CHIP_FILL = 0xFF1A2940, CHIP_SELECTED = 0xFF244D80;
    private final PresetLibraryScreen parent;
    private final FlopTextRGBScreen mainScreen;
    private TextFieldWidget searchField;
    private String selectedTag = "all";
    private int page;
    private int columns = 3, rows = 2, pageSize = 6, cardWidth, cardHeight = 104;
    private int contentTop = 150, contentBottom;
    private final List<CardHit> visibleCards = new ArrayList<>();
    private final List<TagHit> visibleTags = new ArrayList<>();
    private final List<TagHit> visibleCardTags = new ArrayList<>();

    public CommunityPresetScreen(PresetLibraryScreen parent, FlopTextRGBScreen mainScreen) {
        super(Text.literal("Community Presets")); this.parent = parent; this.mainScreen = mainScreen;
    }
    private int fieldWidth() { return Math.max(180, Math.min(450, width - 220)); }
    private int clearX() { return width - 94; }

    @Override protected void init() {
        String previousQuery = searchField == null ? "" : searchField.getText();
        clearChildren();
        LibraryUi.button(this::addDrawableChild, "Back", 14, 12, 78, 22, b -> closeToLibrary());
        int fw = fieldWidth();
        searchField = new TextFieldWidget(textRenderer, (width - fw) / 2, 48, fw, 22, Text.literal("Search Community Presets"));
        searchField.setMaxLength(100);
        searchField.setText(previousQuery);
        searchField.setChangedListener(v -> { page = 0; layout(); });
        addDrawableChild(searchField);
        LibraryUi.button(this::addDrawableChild, "Clear", clearX(), 48, 74, 22, b -> { searchField.setText(""); page = 0; });
        layout();
        LibraryUi.button(this::addDrawableChild, "‹ Previous", 14, height - 34, 105, 22, b -> { if (page > 0) page--; init(); });
        LibraryUi.button(this::addDrawableChild, "Next ›", width - 119, height - 34, 105, 22, b -> { if (page + 1 < pageCount()) page++; init(); });
        LibraryUi.button(this::addDrawableChild, "All Categories", width / 2 - 80, height - 34, 160, 22, b -> { selectedTag = "all"; page = 0; init(); });
    }

    private void layout() {
        visibleTags.clear(); visibleCards.clear(); visibleCardTags.clear();
        int maxW = width - 32;
        columns = width >= 1050 ? 4 : width >= 760 ? 3 : width >= 520 ? 2 : 1;
        rows = height >= 700 ? 2 : 1;
        pageSize = columns * rows;
        cardWidth = Math.max(145, (maxW - 12 * (columns - 1)) / columns);
        contentTop = 150; contentBottom = height - 66;
        List<String> tags = tags();
        int x = 16, y = 88;
        for (String tag : tags) {
            String title = tag.equals("all") ? "All" : title(tag);
            int w = Math.max(42, textRenderer.getWidth(title) + 18);
            if (x + w > width - 16) { x = 16; y += 25; }
            visibleTags.add(new TagHit(tag, x, y, w, 20)); x += w + 6;
        }
        contentTop = Math.max(132, y + 28);
        List<CommunityPresetRepository.CommunityPreset> list = filtered();
        int start = page * pageSize;
        for (int i = start; i < Math.min(list.size(), start + pageSize); i++) {
            int slot = i - start, col = slot % columns, row = slot / columns;
            int left = 16 + col * (cardWidth + 12), top = contentTop + row * (cardHeight + 12);
            visibleCards.add(new CardHit(list.get(i), left, top, cardWidth, cardHeight));
        }
    }

    private List<String> tags() {
        Set<String> result = new LinkedHashSet<>(); result.add("all");
        for (CommunityPresetRepository.CommunityPreset preset : CommunityPresetRepository.all()) {
            if (preset.category != null && !preset.category.isBlank()) result.add(CommunityPresetRepository.tag(preset.category));
            if (preset.tags != null) for (String tag : preset.tags) if (tag != null && !tag.isBlank()) result.add(CommunityPresetRepository.tag(tag));
        }
        return new ArrayList<>(result);
    }

    private List<CommunityPresetRepository.CommunityPreset> filtered() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<CommunityPresetRepository.CommunityPreset> result = new ArrayList<>();
        for (CommunityPresetRepository.CommunityPreset preset : CommunityPresetRepository.all()) {
            if (!selectedTag.equals("all") && (preset.tags == null || !preset.tags.contains(selectedTag))
                && (preset.category == null || !CommunityPresetRepository.tag(preset.category).equals(selectedTag))) continue;
            if (!query.isEmpty() && !preset.searchable().contains(query)
                && (preset.inputText == null || !preset.inputText.toLowerCase(Locale.ROOT).contains(query))) continue;
            result.add(preset);
        }
        return result;
    }
    private int pageCount() { return Math.max(1, (filtered().size() + pageSize - 1) / pageSize); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        LibraryUi.panel(context, 10, 8, width - 20, height - 16);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Community Presets"), width / 2, 18, WHITE);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Search and filter the bundled gradient catalogue"), width / 2, 31, MUTED);
        // Draw category chips and their hitboxes from the same geometry list.
        for (TagHit tag : visibleTags) drawButton(context, tag.x, tag.y, tag.w, tag.h, title(tag.tag),
            selectedTag.equals(tag.tag) ? CHIP_SELECTED : CHIP_FILL, selectedTag.equals(tag.tag) ? 0xFFFFFFFF : MUTED,
            mouseX, mouseY, selectedTag.equals(tag.tag));
        List<CommunityPresetRepository.CommunityPreset> all = filtered();
        if (all.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(CommunityPresetRepository.loadError().isEmpty() ? "No matching presets." : "Preset catalogue could not be loaded."), width / 2, contentTop + 30, MUTED);
        }
        for (CardHit hit : visibleCards) {
            if (hit.y + hit.h < contentTop || hit.y > contentBottom) continue;
            LibraryUi.panel(context, hit.x, hit.y, hit.w, hit.h);
            context.drawTextWithShadow(textRenderer, Text.literal(clip(hit.preset.name, Math.max(10, hit.w / 6))), hit.x + 9, hit.y + 8, WHITE);
            context.drawText(textRenderer, Text.literal(title(hit.preset.category == null ? "general" : hit.preset.category)), hit.x + 9, hit.y + 23, MUTED, false);
            var preview = hit.preset.toPreset();
            PresetPreviewRenderer.draw(context, textRenderer, "", hit.x + 9, hit.y + 42, preview, hit.w - 18);
            String tags = hit.preset.tags == null ? "" : String.join(" · ", hit.preset.tags.subList(0, Math.min(2, hit.preset.tags.size())));
            context.drawText(textRenderer, Text.literal(clip(tags, Math.max(12, hit.w / 7))), hit.x + 9, hit.y + 62, MUTED, false);
            drawButton(context, hit.x + 8, hit.y + hit.h - 30, hit.w - 16, 22, "Load into editor", BTN_FILL, WHITE, mouseX, mouseY, false);
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Page " + (page + 1) + " / " + pageCount() + "  •  " + all.size() + " presets"), width / 2, height - 42, MUTED);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext c, int x, int y, int w, int h, String label, int fill, int text, int mx, int my, boolean selected) {
        boolean hover = inside(mx, my, x, y, w, h);
        int base = hover ? lighten(fill, 22) : fill;
        c.fill(x, y, x + w, y + h, base);
        c.fill(x, y, x + w, y + 1, hover ? 0xFFFFFFFF : 0xFF6F8BAE);
        c.fill(x, y + h - 1, x + w, y + h, 0xFF0B111C);
        c.fill(x, y, x + 1, y + h, 0xFF496584);
        c.fill(x + w - 1, y, x + w, y + h, 0xFF0B111C);
        if (selected) c.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, ACCENT);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFFFF : text);
    }
    private static int lighten(int c, int amount) { int r=Math.min(255,((c>>16)&255)+amount),g=Math.min(255,((c>>8)&255)+amount),b=Math.min(255,(c&255)+amount);return (c&0xFF000000)|(r<<16)|(g<<8)|b; }
    private static String title(String tag) { if (tag == null || tag.equals("all")) return "All"; String s=tag.replace('_',' ').replace('-',' '); return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }
    private static String clip(String s,int max){if(s==null)return "";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private static boolean inside(double x,double y,int left,int top,int w,int h){return x>=left&&y>=top&&x<left+w&&y<top+h;}

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        if (click.button() == 0) {
            for (TagHit hit : visibleTags) if (inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) { selectedTag = hit.tag; page = 0; init(); return true; }
            for (CardHit hit : visibleCards) if (inside(mouseX, mouseY, hit.x + 8, hit.y + hit.h - 30, hit.w - 16, 22)) {
                mainScreen.applyPreset(hit.preset.toPreset()); MinecraftClient.getInstance().setScreen(mainScreen); return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){if(vertical!=0){page=Math.max(0,Math.min(pageCount()-1,page-(int)Math.signum(vertical)));init();return true;}return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);}
    @Override public void close() { closeToLibrary(); }
    private void closeToLibrary() { MinecraftClient.getInstance().setScreen(parent); }

    private record CardHit(CommunityPresetRepository.CommunityPreset preset, int x, int y, int w, int h) { }
    private record TagHit(String tag, int x, int y, int w, int h) { }
}

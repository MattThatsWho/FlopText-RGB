package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.compat.Compat;

import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reconstructed port of the v1.7.0 Community Presets page.
 * Keeps its fixed tag vocabulary, compact 72px cards, text/tag search,
 * custom beveled controls, and footer navigation rather than adding new filters.
 */
public final class CommunityPresetScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TITLE = 0xFFBEEBFA;
    private static final int MUTED = 0xFF9BA7B4;
    private static final int PANEL = 0xD51A2028;
    private static final int PANEL_EDGE = 0xFF34424E;
    private static final int INPUT = 0xFF14191F;
    private static final int BUTTON = 0xFF354552;
    private static final int BUTTON_EDGE = 0xFF34424E;
    private static final int SELECTED = 0xFF246783;
    private static final int TAG_TEXT = 0xFF53B6D8;
    private static final int CARD_HEIGHT = 72;

    private static final String[] TAGS = {
        "all", "items", "biomes", "mobs", "holidays", "magic", "gaming", "fantasy", "cozy",
        "ores", "food", "ocean", "space", "flags", "competitive", "classic", "favorites"
    };
    private static final String[] TAG_LABELS = {
        "#all", "#items", "#biomes", "#mobs", "#holidays", "#magic", "#gaming", "#fantasy", "#cozy",
        "#ores", "#food", "#ocean", "#space", "#flags", "#comp", "#classic", "#favs"
    };

    private final PresetLibraryScreen parent;
    private final FlopTextRGBScreen mainScreen;
    private EditBox searchField;
    private String selectedTag = "all";
    private int page;
    private int chipRows = 3;
    private int chipsPerRow = 6;
    private int columns = 2;
    private int rows = 1;
    private int pageSize = 2;
    private int cardWidth;
    private int contentTop = 152;
    private int contentBottom;
    private final List<CardHit> visibleCards = new ArrayList<>();
    private final List<TagHit> visibleTags = new ArrayList<>();
    private final List<TagHit> visibleCardTags = new ArrayList<>();

    public CommunityPresetScreen(PresetLibraryScreen parent, FlopTextRGBScreen mainScreen) {
        super(Component.literal("Community Presets"));
        this.parent = parent;
        this.mainScreen = mainScreen;
    }

    private int fieldWidth() { return Math.max(100, width - 18 - 72 - 24); }
    private int clearX() { return width - 18 - 72; }
    private int footerY() { return height - 30; }

    @Override
    protected void init() {
        String previousQuery = searchField == null ? "" : searchField.getValue();
        clearWidgets();
        visibleCards.clear();
        visibleTags.clear();
        visibleCardTags.clear();
        searchField = new EditBox(font, 18, 48, fieldWidth(), 20, Component.literal("Search names or #tags"));
        searchField.setMaxLength(120);
        searchField.setValue(previousQuery);
        searchField.setResponder(value -> { page = 0; layout(); });
        addRenderableWidget(searchField);
        layout();
    }

    private void layout() {
        visibleCards.clear();
        visibleTags.clear();
        visibleCardTags.clear();

        int layoutWidth = Math.max(300, width);
        int available = layoutWidth - 36;
        chipsPerRow = Math.max(3, (available + 4) / 62);
        chipRows = (int) Math.ceil(TAGS.length / (double) chipsPerRow);
        chipsPerRow = (int) Math.ceil(TAGS.length / (double) Math.max(1, chipRows));
        contentTop = 84 + chipRows * 22 + 2;
        contentBottom = Math.max(contentTop + 74, height - 45);

        columns = layoutWidth >= 850 ? 4 : layoutWidth >= 610 ? 3 : 2;
        cardWidth = Math.max(130, (layoutWidth - 36 - (columns - 1) * 8) / columns);
        rows = Math.max(1, (contentBottom - contentTop + 8) / (CARD_HEIGHT + 8));
        pageSize = Math.max(1, rows * columns);

        int tagIndex = 0;
        for (int row = 0; row < chipRows; row++) {
            int first = row * chipsPerRow;
            int count = Math.min(chipsPerRow, TAGS.length - first);
            if (count <= 0) break;
            int rowWidth = layoutWidth - 36;
            int chipWidth = (rowWidth - (count - 1) * 4) / count;
            int extraPixels = rowWidth - (count - 1) * 4 - chipWidth * count;
            int x = 18;
            int y = 84 + row * 22;
            for (int col = 0; col < count; col++) {
                int w = chipWidth + (col < extraPixels ? 1 : 0);
                visibleTags.add(new TagHit(TAGS[tagIndex], TAG_LABELS[tagIndex], x, y, w, 18));
                x += w + 4;
                tagIndex++;
            }
        }

        List<CommunityPresetRepository.CommunityPreset> matches = filtered();
        page = Math.max(0, Math.min(page, Math.max(0, (matches.size() - 1) / pageSize)));
        int start = page * pageSize;
        int end = Math.min(matches.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            int slot = i - start;
            int col = slot % columns;
            int row = slot / columns;
            int x = 18 + col * (cardWidth + 8);
            int y = contentTop + row * (CARD_HEIGHT + 8);
            CommunityPresetRepository.CommunityPreset preset = matches.get(i);
            visibleCards.add(new CardHit(preset, x, y, cardWidth, CARD_HEIGHT));

            String category = preset.category == null || preset.category.isBlank() ? "community" : preset.category;
            String categoryText = "#" + category;
            int tagX = x + 7 + font.width(categoryText) + 7;
            int tagY = y + 43;
            if (preset.tags != null) {
                for (String tag : preset.tags) {
                    if (tag == null || tag.isBlank() || tag.equals(category) || tag.equals("community")) continue;
                    String shownTag = "#" + tag;
                    int w = font.width(shownTag) + 4;
                    if (tagX + w >= x + cardWidth - 55) break;
                    visibleCardTags.add(new TagHit(tag, shownTag, tagX, tagY - 2, w, 13));
                    break; // The 1.21.11 UI showed at most one extra clickable tag per card.
                }
            }
        }
    }

    private List<CommunityPresetRepository.CommunityPreset> filtered() {
        List<CommunityPresetRepository.CommunityPreset> result = new ArrayList<>();
        String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        String[] terms = query.isEmpty() ? new String[0] : query.split("\\s+");
        for (CommunityPresetRepository.CommunityPreset preset : CommunityPresetRepository.all()) {
            if (!"all".equals(selectedTag) && (preset.tags == null || !preset.tags.contains(selectedTag))) continue;
            String searchable = preset.searchable();
            if (preset.inputText != null) searchable += " " + preset.inputText.toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String term : terms) {
                if (term.isBlank()) continue;
                if (term.startsWith("#")) {
                    String tag = queryTag(term.substring(1));
                    if (!"all".equals(tag) && (preset.tags == null || !preset.tags.contains(tag))) {
                        matches = false;
                        break;
                    }
                } else if (!searchable.contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) result.add(preset);
        }
        return result;
    }

    private static String queryTag(String raw) {
        String normalized = CommunityPresetRepository.tag(raw);
        if ("comp".equals(normalized)) return "competitive";
        if ("favs".equals(normalized)) return "favorites";
        return normalized;
    }

    private int pageCount() {
        return Math.max(1, (filtered().size() + pageSize - 1) / Math.max(1, pageSize));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        layout();
        // Preserve the original charcoal/blue-grey frame and the compact header/search row.
        panel(context, 12, 10, width - 24, 66, PANEL, PANEL_EDGE);
        centered(context, "COMMUNITY PRESETS", 20, TITLE);
        centered(context, filtered().size() + " presets", 35, MUTED);
        panel(context, 14, 44, width - 28, 29, 0xE51F2832, PANEL_EDGE);
        panel(context, 18, 48, fieldWidth(), 20, INPUT, PANEL_EDGE);
        super.extractRenderState(context, mouseX, mouseY, delta);

        drawButton(context, clearX(), 48, 72, 20, "Clear", BUTTON, 0xFFECEFF4, mouseX, mouseY, false);
        for (TagHit tag : visibleTags) {
            boolean selected = selectedTag.equals(tag.tag);
            drawButton(context, tag.x, tag.y, tag.w, tag.h, tag.label,
                selected ? SELECTED : 0xFF2A323D, selected ? 0xFFECEFF4 : MUTED, mouseX, mouseY, selected);
        }

        List<CommunityPresetRepository.CommunityPreset> matches = filtered();
        String loadError = CommunityPresetRepository.loadError();
        if (loadError != null && !loadError.isBlank()) {
            centered(context, "Community presets could not be loaded: " + loadError, contentTop + 18, 0xFFFF8B5A);
        } else if (matches.isEmpty()) {
            centered(context, "No presets match. Clear the search or choose #all.", contentTop + 18, MUTED);
        }
        for (CardHit card : visibleCards) {
            if (card.y + card.h < contentTop || card.y > contentBottom) continue;
            boolean hover = inside(mouseX, mouseY, card.x, card.y, card.w, card.h);
            panel(context, card.x, card.y, card.w, card.h,
                hover ? 0xE82A343D : PANEL, PANEL_EDGE);
            text(context, clip(card.preset.name, Math.max(10, card.w / 6)), card.x + 7, card.y + 6, WHITE);
            String previewText = card.preset.inputText == null || card.preset.inputText.isBlank()
                ? "Example Text" : card.preset.inputText;
            PresetPreviewRenderer.draw(context, font, previewText, card.x + 7, card.y + 24,
                card.preset.toPreset(), Math.max(36, card.w - 14));

            String category = card.preset.category == null || card.preset.category.isBlank() ? "community" : card.preset.category;
            String categoryText = "#" + category;
            int categoryColor = inside(mouseX, mouseY, card.x + 7, card.y + 43,
                font.width(categoryText) + 4, 13) ? 0xFFECEFF4 : TAG_TEXT;
            text(context, categoryText, card.x + 7, card.y + 43, categoryColor);
        }
        // Card tags are drawn and hit-tested from the same layout rectangles.
        for (TagHit tag : visibleCardTags) {
            boolean hover = inside(mouseX, mouseY, tag.x, tag.y, tag.w, tag.h);
            text(context, tag.label, tag.x, tag.y + 2, hover ? 0xFFECEFF4 : 0xFF53B6D8);
        }
        // Compact Load button lives inside each 72px card, as in the original page.
        for (CardHit card : visibleCards) {
            drawButton(context, card.x + card.w - 48, card.y + 51, 41, 16, "Load",
                0xFF246783, 0xFFECEFF4, mouseX, mouseY, false);
        }

        int footer = footerY();
        drawButton(context, 18, footer, 68, 20, "Back", BUTTON, 0xFFECEFF4, mouseX, mouseY, false);
        drawButton(context, 92, footer, 68, 20, "Previous", BUTTON, 0xFFECEFF4, mouseX, mouseY, false);
        drawButton(context, width - 86, footer, 68, 20, "Next", BUTTON, 0xFFECEFF4, mouseX, mouseY, false);
        String footerText = matches.isEmpty() ? "0 results"
            : "Page " + (page + 1) + "/" + pageCount() + "  " + matches.size() + " results";
        int footerX = Math.max(166, width - 256);
        context.text(font, Component.literal(footerText),
            footerX + Math.max(0, (80 - font.width(footerText)) / 2), footer + 6, MUTED, false);
    }

    private void panel(GuiGraphicsExtractor c, int x, int y, int w, int h, int fill, int edge) {
        if (w <= 0 || h <= 0) return;
        c.fill(x, y, x + w, y + h, fill);
        c.fill(x, y, x + w, y + 1, edge);
        c.fill(x, y + h - 1, x + w, y + h, edge);
        c.fill(x, y, x + 1, y + h, edge);
        c.fill(x + w - 1, y, x + w, y + h, edge);
    }

    private void centered(GuiGraphicsExtractor c, String value, int y, int color) {
        c.centeredText(font, Component.literal(value), width / 2, y, color);
    }

    private void text(GuiGraphicsExtractor c, String value, int x, int y, int color) {
        c.text(font, Component.literal(value), x, y, color, false);
    }

    private void drawButton(GuiGraphicsExtractor c, int x, int y, int w, int h, String label,
                            int fill, int text, int mx, int my, boolean selected) {
        boolean hover = inside(mx, my, x, y, w, h);
        LibraryUi.panel(c, x, y, w, h);
        int base = hover ? lighten(fill, 14) : fill;
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, lighten(base, 22));
        c.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, darken(base, 26));
        if (selected) c.fill(x + 1, y + h - 3, x + w - 1, y + h - 2, 0xFF53B6D8);
        c.centeredText(font, Component.literal(label), x + w / 2, y + (h - 8) / 2,
            hover ? WHITE : text);
    }

    private static int lighten(int color, int amount) {
        int r = Math.min(255, ((color >>> 16) & 255) + amount);
        int g = Math.min(255, ((color >>> 8) & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, int amount) {
        int r = Math.max(0, ((color >>> 16) & 255) - amount);
        int g = Math.max(0, ((color >>> 8) & 255) - amount);
        int b = Math.max(0, (color & 255) - amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Installs the original Library's Community entry using shared geometry for draw and clicks. */
    public static void installLibraryEntry(PresetLibraryScreen library) {
        if (library == null || library.isModalOpen()) return;
        int[] r = libraryEntryRect(library);
        if (r == null) return;
        String label = r[4] == 1 ? "" : "     Community";
        library.addTransferButton(label, r[0], r[1], r[2], r[3], () ->
            Compat.setScreen(new CommunityPresetScreen(library, library.getMainScreen())));
    }

    /** Draws the pixel-person glyph over the blank leading space of the button label. */
    public static void drawLibraryEntry(PresetLibraryScreen library, GuiGraphicsExtractor context,
                                        int mouseX, int mouseY) {
        if (library == null || library.isModalOpen()) return;
        int[] r = libraryEntryRect(library);
        if (r == null) return;
        if (r[4] == 1) {
            FlopIcons.centered(context, FlopIcons.PERSON, r[0], r[1], r[2], r[3], -1);
            return;
        }
        int textWidth = Minecraft.getInstance().font.width("     Community");
        int textLeft = r[0] + (r[2] - textWidth) / 2;
        int iconX = textLeft + 2;
        int iconY = r[1] + (r[3] - FlopIcons.PERSON.length) / 2;
        FlopIcons.bitmap(context, FlopIcons.PERSON, iconX, iconY, 1, -1);
    }

    /** Called before ordinary Screen widget dispatch, matching the original custom hit-test path. */
    public static boolean handleLibraryEntryClick(PresetLibraryScreen library, MouseButtonEvent click) {
        if (library == null || click == null || !Compat.isLeft(click.button()) || library.isModalOpen()) return false;
        int[] r = libraryEntryRect(library);
        if (r == null || !inside(click.x(), click.y(), r[0], r[1], r[2], r[3])) return false;
        Compat.setScreen(new CommunityPresetScreen(library, library.getMainScreen()));
        return true;
    }

    private static int[] libraryEntryRect(PresetLibraryScreen library) {
        int width = library.screenWidth();
        int height = library.screenHeight();
        int backX = Math.max(0, width / 2 - 70);
        int x = backX + 146;
        int y = height - 43;
        int available = width - 14 - x;
        if (available < 24) return null;
        int buttonWidth = Math.min(112, available);
        boolean iconOnly = buttonWidth < 84;
        if (iconOnly) buttonWidth = 24;
        return new int[]{x, y, buttonWidth, 24, iconOnly ? 1 : 0};
    }

    private static String clip(String value, int limit) {
        if (value == null) return "";
        return value.length() <= limit ? value : value.substring(0, Math.max(0, limit - 1)) + "…";
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && y >= top && x < left + w && y < top + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x(), mouseY = click.y();
        if (!Compat.isLeft(click.button())) return super.mouseClicked(click, doubled);

        // Let the actual edit box process clicks before handling our custom controls.
        if (inside(mouseX, mouseY, 18, 48, fieldWidth(), 20))
            return super.mouseClicked(click, doubled);
        if (inside(mouseX, mouseY, clearX(), 48, 72, 20)) {
            if (searchField != null) searchField.setValue("");
            selectedTag = "all";
            page = 0;
            layout();
            return true;
        }
        for (TagHit tag : visibleTags) {
            if (inside(mouseX, mouseY, tag.x, tag.y, tag.w, tag.h)) {
                selectedTag = tag.tag;
                page = 0;
                layout();
                return true;
            }
        }
        for (TagHit tag : visibleCardTags) {
            if (inside(mouseX, mouseY, tag.x, tag.y, tag.w, tag.h)) {
                selectedTag = "all";
                if (searchField != null) searchField.setValue("#" + tag.tag);
                page = 0;
                layout();
                return true;
            }
        }

        int footer = footerY();
        if (inside(mouseX, mouseY, 18, footer, 68, 20)) { closeToLibrary(); return true; }
        if (inside(mouseX, mouseY, 92, footer, 68, 20)) { page = Math.max(0, page - 1); layout(); return true; }
        if (inside(mouseX, mouseY, width - 86, footer, 68, 20)) { page = Math.min(pageCount() - 1, page + 1); layout(); return true; }

        for (CardHit card : visibleCards) {
            if (inside(mouseX, mouseY, card.x + card.w - 49, card.y + 50, 43, 19)) {
                PresetLoadSupport.applyAndReturn(mainScreen, card.preset.toPreset());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0 || mouseY < contentTop || mouseY >= contentBottom
                || mouseX < 12 || mouseX >= width - 12) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        page = Math.max(0, Math.min(pageCount() - 1, page - (int) Math.signum(verticalAmount)));
        layout();
        return true;
    }

    @Override public void onClose() { closeToLibrary(); }
    private void closeToLibrary() { Compat.setScreen(parent); }

    private record CardHit(CommunityPresetRepository.CommunityPreset preset, int x, int y, int w, int h) { }
    private record TagHit(String tag, String label, int x, int y, int w, int h) { }
}

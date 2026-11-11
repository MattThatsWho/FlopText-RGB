    package com.yourname.floptext_rgb.library;

    import com.google.gson.Gson;
    import com.yourname.floptext_rgb.config.FlopTextRGBConfig;

    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.nio.charset.StandardCharsets;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.LinkedHashSet;
    import java.util.List;
    import java.util.Locale;
    import java.util.Set;

    /** Reconstructed catalogue loader and data validation logic. */
    final class CommunityPresetRepository {
        private static final String RESOURCE = "/assets/floptext_rgb/community_presets.json";
        private static List<CommunityPreset> cache;
        private static String loadError = "";

        private CommunityPresetRepository() { }

        static synchronized List<CommunityPreset> all() {
            if (cache != null) return cache;
            try (InputStream stream = CommunityPresetRepository.class.getResourceAsStream(RESOURCE)) {
                if (stream == null) throw new IllegalStateException("Bundled community preset resource is missing");
                CommunityData data = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), CommunityData.class);
                if (data == null || data.presets == null) {
                    throw new IllegalStateException("Bundled community preset catalogue is empty");
                }
                List<CommunityPreset> valid = new ArrayList<>();
                for (CommunityPreset preset : data.presets) {
                    if (preset == null || preset.name == null || preset.colors == null || preset.positions == null) continue;
                    if (preset.colors.size() < 2 || preset.colors.size() != preset.positions.size()) continue;
                    boolean colorsValid = true;
                    for (String color : preset.colors) {
                        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) colorsValid = false;
                    }
                    if (!colorsValid) continue;
                    if (preset.inputText == null || preset.inputText.isBlank()) preset.inputText = "Example Text";
                    if (preset.outputFormat == null || preset.outputFormat.isBlank()) preset.outputFormat = "Legacy Hex";
                    if (preset.customTemplate == null || preset.customTemplate.isBlank()) preset.customTemplate = "$1$2$3$4$5$6$f$c";
                    if (preset.tags == null) preset.tags = new ArrayList<>();
                    Set<String> tags = new LinkedHashSet<>();
                    if (preset.category != null && !preset.category.isBlank()) tags.add(tag(preset.category));
                    for (String tag : preset.tags) if (tag != null && !tag.isBlank()) tags.add(tag(tag));
                    preset.tags = new ArrayList<>(tags);
                    valid.add(preset);
                }
                cache = Collections.unmodifiableList(valid);
                if (cache.isEmpty()) throw new IllegalStateException("No valid community presets were found");
                loadError = "";
                return cache;
            } catch (Throwable error) {
                loadError = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
                cache = Collections.emptyList();
                return cache;
            }
        }

        static String loadError() {
            all();
            return loadError;
        }

        static String tag(String value) {
            return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        }

        static final class CommunityPreset {
            String name;
            String inputText;
            List<String> colors = new ArrayList<>();
            List<Float> positions = new ArrayList<>();
            boolean bold;
            boolean italic;
            boolean strikethrough;
            boolean underline;
            String outputFormat;
            String customTemplate;
            String category;
            List<String> tags = new ArrayList<>();

            FlopTextRGBConfig.GradientPreset toPreset() {
                List<com.yourname.floptext_rgb.math.GradientCalculator.ColorStop> stops = new ArrayList<>();
                for (int i = 0; i < colors.size(); i++) {
                    try {
                        int rgb = Integer.parseInt(colors.get(i).substring(1), 16);
                        Float position = positions.get(i);
                        float p = position == null ? (float) i / Math.max(1, colors.size() - 1)
                            : Math.max(0.0f, Math.min(1.0f, position));
                        stops.add(new com.yourname.floptext_rgb.math.GradientCalculator.ColorStop(rgb, p));
                    } catch (RuntimeException ignored) { }
                }
                return new FlopTextRGBConfig.GradientPreset(name, inputText, stops,
                    bold, italic, strikethrough, underline, outputFormat, customTemplate);
            }

            String searchable() {
                StringBuilder builder = new StringBuilder(name == null ? "" : name)
                    .append(' ').append(category == null ? "" : category);
                for (String tag : tags) builder.append(' ').append(tag);
                return builder.toString().toLowerCase(Locale.ROOT);
            }
        }

        private static final class CommunityData {
            int version;
            String source;
            List<CommunityPreset> presets;
        }
    }

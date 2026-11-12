    package com.yourname.floptext_rgb.config;

    import com.google.gson.Gson;
    import com.google.gson.GsonBuilder;
    import com.yourname.floptext_rgb.math.GradientCalculator;
    import com.yourname.floptext_rgb.math.GradientCalculator.ColorStop;

    import java.io.IOException;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.ArrayList;
    import java.util.List;

    /** Reconstructed config model and persistence logic from v1.7.0 bytecode. */
    public class FlopTextRGBConfig {
        private static final Path FILE = Path.of("config", "rgblocks.json");
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static FlopTextRGBConfig INSTANCE;

        public List<GradientPreset> presets = new ArrayList<>();
        public boolean bold;
        public boolean italic;
        public boolean strikethrough;
        public boolean underline;
        public String outputFormat = "Legacy Hex";
        public String customTemplate = "$1$2$3$4$5$6$f$c";

        public FlopTextRGBConfig() { }

        public static synchronized void register() {
            if (INSTANCE == null) INSTANCE = load();
        }

        public static FlopTextRGBConfig get() {
            register();
            return INSTANCE;
        }

        public static synchronized void save() {
            register();
            try {
                Path parent = FILE.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(FILE, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // Preserved behavior: config persistence failures do not crash the client.
            }
        }

        private static FlopTextRGBConfig load() {
            if (!Files.isRegularFile(FILE)) return new FlopTextRGBConfig();
            try {
                FlopTextRGBConfig loaded = GSON.fromJson(
                    Files.readString(FILE, StandardCharsets.UTF_8),
                    FlopTextRGBConfig.class
                );
                if (loaded == null) return new FlopTextRGBConfig();
                loaded.sanitize();
                return loaded;
            } catch (Exception ignored) {
                return new FlopTextRGBConfig();
            }
        }

        private void sanitize() {
            if (presets == null) presets = new ArrayList<>();
            presets.removeIf(preset -> preset == null);
            for (GradientPreset preset : presets) preset.sanitize();
            if (outputFormat == null) outputFormat = "Legacy Hex";
            if (!outputFormat.equals("Legacy Hex")
                    && !outputFormat.equals("MiniMessage")
                    && !outputFormat.equals("Vanilla / Spigot")
                    && !outputFormat.equals("Custom")) {
                outputFormat = "Legacy Hex";
            }
            if (customTemplate == null || customTemplate.isEmpty()) {
                customTemplate = "$1$2$3$4$5$6$f$c";
            }
        }

        public static final class GradientPreset {
            public String name = "Unnamed Preset";
            public String inputText = "";
            public List<String> colors = new ArrayList<>(List.of("#FF0000", "#0000FF"));
            public List<Float> positions = new ArrayList<>(List.of(0.0f, 1.0f));
            public boolean bold;
            public boolean italic;
            public boolean strikethrough;
            public boolean underline;
            public String outputFormat = "Legacy Hex";
            public String customTemplate = "$1$2$3$4$5$6$f$c";

            public GradientPreset() { }

            public GradientPreset(String name, String inputText, List<ColorStop> stops,
                                  boolean bold, boolean italic, boolean strikethrough,
                                  boolean underline, String outputFormat, String customTemplate) {
                this.name = name;
                this.inputText = inputText;
                this.colors = new ArrayList<>();
                this.positions = new ArrayList<>();
                for (ColorStop stop : stops) {
                    this.colors.add(GradientCalculator.toDisplayHex(stop.rgb()));
                    this.positions.add(stop.position());
                }
                this.bold = bold;
                this.italic = italic;
                this.strikethrough = strikethrough;
                this.underline = underline;
                this.outputFormat = outputFormat;
                this.customTemplate = customTemplate;
                sanitize();
            }

            public List<ColorStop> toStops() {
                List<ColorStop> result = new ArrayList<>();
                int count = Math.min(colors == null ? 0 : colors.size(),
                                     positions == null ? 0 : positions.size());
                for (int i = 0; i < count; i++) {
                    int rgb = GradientCalculator.parseHex(colors.get(i));
                    Float position = positions.get(i);
                    if (rgb >= 0 && position != null && !position.isNaN()) {
                        result.add(new ColorStop(rgb,
                            Math.max(0.0f, Math.min(1.0f, position))));
                    }
                }
                return GradientCalculator.sorted(result);
            }

            public void sanitize() {
                if (name == null || name.isBlank()) name = "Unnamed Preset";
                if (inputText == null) inputText = "";
                if (colors == null) colors = new ArrayList<>();
                if (positions == null) positions = new ArrayList<>();
                if (outputFormat == null) outputFormat = "Legacy Hex";
                if (customTemplate == null || customTemplate.isEmpty()) {
                    customTemplate = "$1$2$3$4$5$6$f$c";
                }
            }
        }
    }

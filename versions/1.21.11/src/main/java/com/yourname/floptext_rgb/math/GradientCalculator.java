    package com.yourname.floptext_rgb.math;

    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;
    import java.util.Locale;

    /** Core RGB interpolation and MiniMessage generation reconstructed from v1.7.0. */
    public final class GradientCalculator {
        private GradientCalculator() { }

        public static float charPosition(int index, int length) {
            return length <= 1 ? 0.0f : (float) index / (float) (length - 1);
        }

        public static String generateMiniMessage(String input, List<ColorStop> stops) {
            if (input == null || input.isEmpty()) return "";
            int[] codePoints = input.replaceAll("\\s+", " ").codePoints().toArray();
            List<ColorStop> sorted = sorted(stops);
            StringBuilder out = new StringBuilder(codePoints.length * 12);
            for (int i = 0; i < codePoints.length; i++) {
                int codePoint = codePoints[i];
                if (Character.isWhitespace(codePoint)) {
                    out.append(' ');
                    continue;
                }
                out.append('<').append(toHex(colorAt(sorted, charPosition(i, codePoints.length)))).append('>');
                if (codePoint == '<' || codePoint == '\\') out.append('\\');
                out.appendCodePoint(codePoint);
            }
            return out.toString();
        }

        public static int colorAt(List<ColorStop> stops, float position) {
            if (stops == null || stops.isEmpty()) return 0xFFFFFF;
            ColorStop first = stops.get(0);
            ColorStop last = stops.get(stops.size() - 1);
            if (position <= first.position()) return first.rgb();
            if (position >= last.position()) return last.rgb();
            for (int i = 0; i < stops.size() - 1; i++) {
                ColorStop left = stops.get(i);
                ColorStop right = stops.get(i + 1);
                if (position >= left.position() && position <= right.position()) {
                    float span = right.position() - left.position();
                    float t = span <= 0.0f ? 0.0f : (position - left.position()) / span;
                    return lerpRgb(left.rgb(), right.rgb(), t);
                }
            }
            return last.rgb();
        }

        public static List<ColorStop> sorted(List<ColorStop> stops) {
            List<ColorStop> copy = new ArrayList<>(stops == null ? List.of() : stops);
            copy.sort(Comparator.comparingDouble(ColorStop::position));
            return copy;
        }

        public static int lerpRgb(int a, int b, float t) {
            int r = lerp((a >> 16) & 255, (b >> 16) & 255, t);
            int g = lerp((a >> 8) & 255, (b >> 8) & 255, t);
            int bl = lerp(a & 255, b & 255, t);
            return (r << 16) | (g << 8) | bl;
        }

        private static int lerp(int a, int b, float t) {
            int value = Math.round(a + (b - a) * t);
            return Math.max(0, Math.min(255, value));
        }

        public static String toHex(int rgb) {
            return String.format(Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
        }

        public static String toDisplayHex(int rgb) {
            return toHex(rgb).toUpperCase(Locale.ROOT);
        }

        public static int parseHex(String value) {
            if (value == null) return -1;
            String hex = value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.matches("[0-9a-fA-F]{3}")) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
            }
            if (!hex.matches("[0-9a-fA-F]{6}")) return -1;
            try {
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        public record ColorStop(int rgb, float position) { }
    }

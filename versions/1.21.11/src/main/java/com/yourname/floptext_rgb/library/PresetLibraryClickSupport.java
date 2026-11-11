package com.yourname.floptext_rgb.library;


/** Explicit bridge for a custom-drawn Library Community entry. */
public final class PresetLibraryClickSupport {
    private PresetLibraryClickSupport() { }
    public static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }
}

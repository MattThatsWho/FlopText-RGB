package com.yourname.floptext_rgb.compat;
import net.minecraft.network.chat.Style;
/** Bridges primitive booleans in the recovered bytecode to Minecraft 26.1's nullable Boolean style APIs. */
public final class StyleBooleanBridge {
    private StyleBooleanBridge() {}
    public static Style withBold(Style style, boolean value) { return style.withBold(Boolean.valueOf(value)); }
    public static Style withItalic(Style style, boolean value) { return style.withItalic(Boolean.valueOf(value)); }
    public static Style withStrikethrough(Style style, boolean value) { return style.withStrikethrough(Boolean.valueOf(value)); }
    public static Style withUnderlined(Style style, boolean value) { return style.withUnderlined(Boolean.valueOf(value)); }
}

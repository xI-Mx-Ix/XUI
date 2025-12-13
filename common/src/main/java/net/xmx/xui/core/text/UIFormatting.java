/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.text;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents formatting codes for text components, including colors and styles.
 * Similar to standard game chat formatting.
 *
 * @author xI-Mx-Ix
 */
public enum UIFormatting {

    BLACK('0', 0x000000),
    DARK_BLUE('1', 0x0000AA),
    DARK_GREEN('2', 0x00AA00),
    DARK_AQUA('3', 0x00AAAA),
    DARK_RED('4', 0xAA0000),
    DARK_PURPLE('5', 0xAA00AA),
    GOLD('6', 0xFFAA00),
    GRAY('7', 0xAAAAAA),
    DARK_GRAY('8', 0x555555),
    BLUE('9', 0x5555FF),
    GREEN('a', 0x55FF55),
    AQUA('b', 0x55FFFF),
    RED('c', 0xFF5555),
    LIGHT_PURPLE('d', 0xFF55FF),
    YELLOW('e', 0xFFFF55),
    WHITE('f', 0xFFFFFF),
    
    OBFUSCATED('k', true),
    BOLD('l', true),
    STRIKETHROUGH('m', true),
    UNDERLINE('n', true),
    ITALIC('o', true),
    
    RESET('r', false);

    public static final char PREFIX_CHAR = '§';
    private static final Map<Character, UIFormatting> BY_CHAR = new HashMap<>();
    private static final Pattern STRIP_PATTERN = Pattern.compile("(?i)" + PREFIX_CHAR + "[0-9A-FK-OR]");

    private final char code;
    private final boolean isStyle;
    private final Integer color;
    private final String controlString;

    static {
        for (UIFormatting formatting : values()) {
            BY_CHAR.put(formatting.code, formatting);
        }
    }

    // Constructor for Colors
    UIFormatting(char code, int color) {
        this(code, false, color);
    }

    // Constructor for Styles
    UIFormatting(char code, boolean isStyle) {
        this(code, isStyle, null);
    }

    UIFormatting(char code, boolean isStyle, Integer color) {
        this.code = code;
        this.isStyle = isStyle;
        this.color = color;
        this.controlString = new String(new char[]{PREFIX_CHAR, code});
    }

    /**
     * Gets the character code associated with this formatting.
     */
    public char getCode() {
        return code;
    }

    /**
     * Returns true if this instance represents a style (bold, italic, etc.),
     * false if it represents a color or reset.
     */
    public boolean isStyle() {
        return isStyle;
    }

    /**
     * Returns true if this instance represents a color.
     */
    public boolean isColor() {
        return !isStyle && this != RESET;
    }

    /**
     * Gets the RGB integer value of the color, or null if this is a style.
     */
    public Integer getColor() {
        return color;
    }

    @Override
    public String toString() {
        return controlString;
    }

    /**
     * Gets a formatting instance by its character code.
     *
     * @param code The character (e.g., 'a', 'l', '0').
     * @return The UIFormatting or null if invalid.
     */
    public static UIFormatting getByCode(char code) {
        return BY_CHAR.get(Character.toLowerCase(code));
    }

    /**
     * Removes all formatting codes (starting with §) from the given text.
     *
     * @param input The text to clean.
     * @return The text without formatting codes, or null if input is null.
     */
    public static String stripFormatting(String input) {
        if (input == null) return null;
        return STRIP_PATTERN.matcher(input).replaceAll("");
    }
}
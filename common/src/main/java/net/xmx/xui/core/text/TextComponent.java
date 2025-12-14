/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.text;

import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.font.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a text component with specific content, style, and font family.
 * <p>
 * This class mimics the structure of Minecraft's component system, supporting
 * a hierarchy of siblings to allow rendering multi-colored or multi-styled text
 * as a single logical unit.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class TextComponent {

    private String text;
    private Font font;

    // Styling properties (null means inherit from parent/default)
    private Integer color = null;
    private Boolean bold = null;
    private Boolean italic = null;
    private Boolean underline = null;
    private Boolean strikethrough = null;
    private Boolean obfuscated = null;

    // Hierarchy
    private final List<TextComponent> siblings = new ArrayList<>();

    /**
     * Constructs a new component with the Vanilla font by default.
     *
     * @param text The text content.
     */
    public TextComponent(String text) {
        this(text, DefaultFonts.getVanilla());
    }

    /**
     * Constructs a new component with a specific font.
     *
     * @param text The text content.
     * @param font The font family to use.
     */
    public TextComponent(String text, Font font) {
        this.text = text;
        this.font = font;
    }

    /**
     * Creates a component from a string literal using the Vanilla font.
     *
     * @param text The text.
     * @return The new component.
     */
    public static TextComponent literal(String text) {
        return new TextComponent(text);
    }

    /**
     * Creates an empty component.
     *
     * @return An empty component.
     */
    public static TextComponent empty() {
        return new TextComponent("");
    }

    /**
     * Appends a sibling component to this component.
     * The sibling will be rendered immediately after this component.
     *
     * @param sibling The component to append.
     * @return This component for chaining.
     */
    public TextComponent append(TextComponent sibling) {
        this.siblings.add(sibling);
        return this;
    }

    /**
     * Appends a string literal as a sibling.
     * The new sibling inherits the font family of this component.
     *
     * @param text The text to append.
     * @return This component for chaining.
     */
    public TextComponent append(String text) {
        this.siblings.add(new TextComponent(text, this.font));
        return this;
    }

    /**
     * Creates a shallow copy of this component, including style properties
     * but excluding siblings.
     *
     * @return A copy of this component.
     */
    public TextComponent copy() {
        TextComponent copy = new TextComponent(this.text, this.font);
        copy.color = this.color;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underline = this.underline;
        copy.strikethrough = this.strikethrough;
        copy.obfuscated = this.obfuscated;
        return copy;
    }

    // --- Getters & Setters ---

    public String getText() {
        return text;
    }

    public TextComponent setText(String text) {
        this.text = text;
        return this;
    }

    public Font getFont() {
        return font;
    }

    /**
     * Sets the font family for this component.
     * Note: This does not automatically propagate to existing siblings.
     *
     * @param font The font family.
     * @return This component.
     */
    public TextComponent setFont(Font font) {
        this.font = font;
        return this;
    }

    public List<TextComponent> getSiblings() {
        return siblings;
    }

    // --- Styling Fluent API ---

    public TextComponent setColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * Applies a standard TextFormatting style or color to this component.
     *
     * @param format The formatting to apply.
     * @return This component.
     */
    public TextComponent applyFormatting(TextFormatting format) {
        if (format == null) return this;

        if (format == TextFormatting.RESET) {
            this.color = null;
            this.bold = null;
            this.italic = null;
            this.underline = null;
            this.strikethrough = null;
            this.obfuscated = null;
            return this;
        }

        if (format.isColor()) {
            this.color = format.getColor();
            // In vanilla MC, setting a color usually resets styles, 
            // but we'll keep it additive here unless you want strictly vanilla behavior.
        } else if (format.isStyle()) {
            switch (format) {
                case BOLD -> this.bold = true;
                case ITALIC -> this.italic = true;
                case UNDERLINE -> this.underline = true;
                case STRIKETHROUGH -> this.strikethrough = true;
                case OBFUSCATED -> this.obfuscated = true;
            }
        }
        return this;
    }

    public TextComponent setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public TextComponent setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public TextComponent setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public TextComponent setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public TextComponent setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
        return this;
    }

    // --- Style Accessors (with fallback logic if needed) ---

    public Integer getColor() {
        return color;
    }
    
    /**
     * Returns true if bold is explicitly set to true.
     */
    public boolean isBold() {
        return Boolean.TRUE.equals(bold);
    }
    
    public boolean isItalic() {
        return Boolean.TRUE.equals(italic);
    }
    
    public boolean isUnderline() {
        return Boolean.TRUE.equals(underline);
    }
    
    public boolean isStrikethrough() {
        return Boolean.TRUE.equals(strikethrough);
    }
    
    public boolean isObfuscated() {
        return Boolean.TRUE.equals(obfuscated);
    }

    public static int getTextWidth(TextComponent text) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWidth(text)); // Assumed method signature in Font needs update if it used UIComponent
    }

    public static int getFontHeight() {
        return 9;
    }

    public static int getWordWrapHeight(TextComponent text, int maxWidth) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWordWrapHeight(text, maxWidth)); // Assumed method signature in Font
    }
}
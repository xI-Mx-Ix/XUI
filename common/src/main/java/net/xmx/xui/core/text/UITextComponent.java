/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.text;

import net.xmx.xui.core.font.UIFont;
import net.xmx.xui.core.font.UIDefaultFonts;

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
public class UITextComponent {

    private String text;
    private UIFont font;

    // Styling properties (null means inherit from parent/default)
    private Integer color = null;
    private Boolean bold = null;
    private Boolean italic = null;
    private Boolean underline = null;
    private Boolean strikethrough = null;
    private Boolean obfuscated = null;

    // Hierarchy
    private final List<UITextComponent> siblings = new ArrayList<>();

    /**
     * Constructs a new component with the Vanilla font by default.
     *
     * @param text The text content.
     */
    public UITextComponent(String text) {
        this(text, UIDefaultFonts.getVanilla());
    }

    /**
     * Constructs a new component with a specific font.
     *
     * @param text The text content.
     * @param font The font family to use.
     */
    public UITextComponent(String text, UIFont font) {
        this.text = text;
        this.font = font;
    }

    /**
     * Creates a component from a string literal using the Vanilla font.
     *
     * @param text The text.
     * @return The new component.
     */
    public static UITextComponent literal(String text) {
        return new UITextComponent(text);
    }

    /**
     * Creates an empty component.
     *
     * @return An empty component.
     */
    public static UITextComponent empty() {
        return new UITextComponent("");
    }

    /**
     * Appends a sibling component to this component.
     * The sibling will be rendered immediately after this component.
     *
     * @param sibling The component to append.
     * @return This component for chaining.
     */
    public UITextComponent append(UITextComponent sibling) {
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
    public UITextComponent append(String text) {
        this.siblings.add(new UITextComponent(text, this.font));
        return this;
    }

    /**
     * Creates a shallow copy of this component, including style properties
     * but excluding siblings.
     *
     * @return A copy of this component.
     */
    public UITextComponent copy() {
        UITextComponent copy = new UITextComponent(this.text, this.font);
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

    public UITextComponent setText(String text) {
        this.text = text;
        return this;
    }

    public UIFont getFont() {
        return font;
    }

    /**
     * Sets the font family for this component.
     * Note: This does not automatically propagate to existing siblings.
     *
     * @param font The font family.
     * @return This component.
     */
    public UITextComponent setFont(UIFont font) {
        this.font = font;
        return this;
    }

    public List<UITextComponent> getSiblings() {
        return siblings;
    }

    // --- Styling Fluent API ---

    public UITextComponent setColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * Applies a standard UIFormatting style or color to this component.
     *
     * @param format The formatting to apply.
     * @return This component.
     */
    public UITextComponent applyFormatting(UIFormatting format) {
        if (format == null) return this;

        if (format == UIFormatting.RESET) {
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

    public UITextComponent setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public UITextComponent setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public UITextComponent setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public UITextComponent setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public UITextComponent setObfuscated(boolean obfuscated) {
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

    public static int getTextWidth(UITextComponent text) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWidth(text)); // Assumed method signature in UIFont needs update if it used UIComponent
    }

    public static int getFontHeight() {
        return 9;
    }

    public static int getWordWrapHeight(UITextComponent text, int maxWidth) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWordWrapHeight(text, maxWidth)); // Assumed method signature in UIFont
    }
}
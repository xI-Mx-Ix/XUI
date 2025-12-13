/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.text;

import net.xmx.xui.core.font.UIFont;
import net.xmx.xui.core.font.UIStandardFonts;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a text component with specific content, style, and font family.
 * <p>
 * This class mimics the structure of Minecraft's {@code Component} system, supporting
 * a hierarchy of siblings to allow rendering multi-colored or multi-styled text
 * as a single logical unit.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIComponent {

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
    private final List<UIComponent> siblings = new ArrayList<>();

    /**
     * Constructs a new component with the Vanilla font by default.
     *
     * @param text The text content.
     */
    public UIComponent(String text) {
        this(text, UIStandardFonts.getVanilla());
    }

    /**
     * Constructs a new component with a specific font.
     *
     * @param text The text content.
     * @param font The font family to use.
     */
    public UIComponent(String text, UIFont font) {
        this.text = text;
        this.font = font;
    }

    /**
     * Creates a component from a string literal using the Vanilla font.
     *
     * @param text The text.
     * @return The new component.
     */
    public static UIComponent literal(String text) {
        return new UIComponent(text);
    }

    /**
     * Creates an empty component.
     *
     * @return An empty component.
     */
    public static UIComponent empty() {
        return new UIComponent("");
    }

    /**
     * Appends a sibling component to this component.
     * The sibling will be rendered immediately after this component.
     *
     * @param sibling The component to append.
     * @return This component for chaining.
     */
    public UIComponent append(UIComponent sibling) {
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
    public UIComponent append(String text) {
        this.siblings.add(new UIComponent(text, this.font));
        return this;
    }

    /**
     * Creates a shallow copy of this component, including style properties
     * but excluding siblings.
     *
     * @return A copy of this component.
     */
    public UIComponent copy() {
        UIComponent copy = new UIComponent(this.text, this.font);
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

    public UIComponent setText(String text) {
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
    public UIComponent setFont(UIFont font) {
        this.font = font;
        return this;
    }

    public List<UIComponent> getSiblings() {
        return siblings;
    }

    // --- Styling Fluent API ---

    public UIComponent setColor(int color) {
        this.color = color;
        return this;
    }

    public UIComponent setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public UIComponent setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public UIComponent setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public UIComponent setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public UIComponent setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
        return this;
    }

    // --- Style Accessors (with fallback logic if needed) ---

    public Integer getColor() { return color; }
    
    /**
     * Returns true if bold is explicitly set to true.
     */
    public boolean isBold() { return Boolean.TRUE.equals(bold); }
    
    public boolean isItalic() { return Boolean.TRUE.equals(italic); }
    
    public boolean isUnderline() { return Boolean.TRUE.equals(underline); }
    
    public boolean isStrikethrough() { return Boolean.TRUE.equals(strikethrough); }
    
    public boolean isObfuscated() { return Boolean.TRUE.equals(obfuscated); }
}
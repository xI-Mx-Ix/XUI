/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.text;

import net.xmx.xui.core.font.UIFont;

/**
 * Represents a text component with specific content, style, and font family.
 *
 * @author xI-Mx-Ix
 */
public class UIComponent {

    private String text;
    private UIFont font;

    // Styling Flags
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private boolean obfuscated;

    /**
     * Constructs a new component.
     *
     * @param text The text content.
     * @param font The font family to use for rendering.
     */
    public UIComponent(String text, UIFont font) {
        this.text = text;
        this.font = font;
    }

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

    public UIComponent setFont(UIFont font) {
        this.font = font;
        return this;
    }

    // --- Fluent Style Setters ---

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

    // --- Getters ---

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font;

import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.TextComponent;

/**
 * Abstract base class for font families.
 * <p>
 * Defines the contract for measuring and rendering text, allowing distinct implementations
 * for Minecraft's internal font (Vanilla) and custom TrueType fonts (Custom).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public abstract class Font {

    /**
     * Enumeration of supported font pipeline types.
     */
    public enum Type {
        VANILLA,
        CUSTOM
    }

    private final Type type;

    protected Font(Type type) {
        this.type = type;
    }

    /**
     * Gets the pipeline type of this font.
     *
     * @return The font type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the standard height of a single line of text in pixels.
     *
     * @return The line height.
     */
    public abstract float getLineHeight();

    /**
     * Calculates the width of the provided component tree in pixels.
     *
     * @param component The root component to measure.
     * @return The total width.
     */
    public abstract float getWidth(TextComponent component);

    /**
     * Calculates the height required to render the text with a maximum width constraint.
     *
     * @param component The text content.
     * @param maxWidth  The width limit in pixels.
     * @return The total vertical height in pixels.
     */
    public abstract float getWordWrapHeight(TextComponent component, float maxWidth);

    /**
     * Renders a single line of text.
     *
     * @param renderer  The renderer instance.
     * @param component The text to render.
     * @param x         Absolute X coordinate.
     * @param y         Absolute Y coordinate.
     * @param color     The text color.
     * @param shadow    Whether to draw a drop shadow.
     */
    public abstract void draw(UIRenderer renderer, TextComponent component, float x, float y, int color, boolean shadow);

    /**
     * Renders text wrapped within a specific width.
     *
     * @param renderer  The renderer instance.
     * @param component The text to render.
     * @param x         Absolute X coordinate.
     * @param y         Absolute Y coordinate.
     * @param maxWidth  The maximum width before wrapping.
     * @param color     The text color.
     * @param shadow    Whether to draw a drop shadow.
     */
    public abstract void drawWrapped(UIRenderer renderer, TextComponent component, float x, float y, float maxWidth, int color, boolean shadow);
}
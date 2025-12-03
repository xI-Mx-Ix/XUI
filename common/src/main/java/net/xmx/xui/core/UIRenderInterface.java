/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

import net.minecraft.network.chat.Component;

/**
 * Abstract interface for UI rendering operations.
 * This abstraction allows the core library to remain independent of specific game engine classes.
 * Updated to support Minecraft's Component system for rich text and multi-line rendering.
 *
 * @author xI-Mx-Ix
 */
public interface UIRenderInterface {

    /**
     * Draws a filled rectangle with uniform corners.
     */
    void drawRect(float x, float y, float width, float height, int color, float radius);

    /**
     * Draws a filled rectangle with individual corner radii.
     * Use this to create shapes like tabs or connected dropdowns.
     *
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color  The ARGB color value.
     * @param rTL    Top-Left radius.
     * @param rTR    Top-Right radius.
     * @param rBR    Bottom-Right radius.
     * @param rBL    Bottom-Left radius.
     */
    void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL);

    /**
     * Draws a hollow outline (border) around a rectangle, optionally with rounded corners.
     */
    void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness);

    /**
     * Draws a hollow outline with individual corner radii.
     */
    void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL);

    /**
     * Draws a text component on the screen.
     * Supports styling, colors, and translations defined within the Component.
     *
     * @param text   The Component to render.
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param color  The ARGB color value (may be overridden by Component styles).
     * @param shadow Whether to render a drop shadow behind the text.
     */
    void drawText(Component text, float x, float y, int color, boolean shadow);

    /**
     * Draws text that automatically wraps to the next line if it exceeds the specified width.
     *
     * @param text   The Component to render.
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param width  The maximum width allowed before wrapping.
     * @param color  The ARGB color value.
     * @param shadow Whether to render a drop shadow.
     */
    void drawWrappedText(Component text, float x, float y, float width, int color, boolean shadow);

    /**
     * Calculates the width of the given text component in pixels.
     *
     * @param text The text to measure.
     * @return The width in pixels.
     */
    int getTextWidth(Component text);

    /**
     * Returns the height of the standard font line.
     *
     * @return The font height in pixels.
     */
    int getFontHeight();

    /**
     * Calculates the total height required to render the text with the given maximum width constraint.
     *
     * @param text     The text to measure.
     * @param maxWidth The wrapping width.
     * @return The total height in pixels.
     */
    int getWordWrapHeight(Component text, int maxWidth);

    /**
     * Enables a scissor test to clip rendering to a specific screen region.
     *
     * @param x      The x-coordinate of the clipping area.
     * @param y      The y-coordinate of the clipping area.
     * @param width  The width of the clipping area.
     * @param height The height of the clipping area.
     */
    void enableScissor(int x, int y, int width, int height);

    /**
     * Disables the active scissor test, restoring full screen rendering.
     */
    void disableScissor();

    /**
     * Moves the rendering position on the Z-axis (depth).
     * Positive values move towards the viewer (on top), rendering over other elements.
     * Negative values move away (behind).
     *
     * @param z The offset to apply to the Z-coordinate.
     */
    void translateZ(float z);

    /**
     * Retrieves the current scissor rectangle active in the renderer.
     * <p>
     * This is essential for nested clipping regions (e.g., a scroll panel inside another scroll panel).
     * Effects can use this to calculate the intersection between the parent's clip rect and the
     * widget's desired clip rect.
     * </p>
     *
     * @return An integer array {@code [x, y, width, height]} representing the current scissor window,
     *         or {@code null} if no scissor test is currently enabled.
     */
    int[] getCurrentScissor();
}
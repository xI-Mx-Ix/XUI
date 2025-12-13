/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import net.xmx.xui.core.text.UITextComponent;

/**
 * Abstract interface for UI rendering operations.
 * This abstraction allows the core library to remain independent of specific game engine classes.
 * Updated to support the internal UITextComponent system for rich text and multi-line rendering
 * (supporting both Vanilla and Custom fonts).
 *
 * @author xI-Mx-Ix
 */
public interface UIRenderInterface {

    /**
     * Draws a filled rectangle with uniform corners.
     *
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color  The ARGB color value.
     * @param radius The radius for all four corners.
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
     *
     * @param x         The absolute x-coordinate.
     * @param y         The absolute y-coordinate.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param color     The ARGB color value.
     * @param radius    The radius for all four corners.
     * @param thickness The thickness of the border line.
     */
    void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness);

    /**
     * Draws a hollow outline with individual corner radii.
     *
     * @param x         The absolute x-coordinate.
     * @param y         The absolute y-coordinate.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param color     The ARGB color value.
     * @param thickness The thickness of the border line.
     * @param rTL       Top-Left radius.
     * @param rTR       Top-Right radius.
     * @param rBR       Bottom-Right radius.
     * @param rBL       Bottom-Left radius.
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
    void drawText(UITextComponent text, float x, float y, int color, boolean shadow);

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
    void drawWrappedText(UITextComponent text, float x, float y, float width, int color, boolean shadow);

    /**
     * Enables a scissor test to clip rendering to a specific screen region.
     * <p>
     * <b>Implementation Note:</b> This method accepts float coordinates to allow for
     * sub-pixel precision calculations (e.g., centering logic). The implementation
     * must snap these values to the nearest physical pixel grid to ensure that
     * clipping areas align perfectly with rendered geometry.
     * </p>
     *
     * @param x      The logical x-coordinate of the clipping area.
     * @param y      The logical y-coordinate of the clipping area.
     * @param width  The logical width of the clipping area.
     * @param height The logical height of the clipping area.
     */
    void enableScissor(float x, float y, float width, float height);

    /**
     * Disables the active scissor test, restoring full screen rendering.
     */
    void disableScissor();

    /**
     * Translates the rendering origin by the specified amount.
     * This affects all subsequent drawing operations until reversed.
     * Useful for scroll containers or moving groups of widgets.
     *
     * @param x The offset on the X-axis in logical pixels.
     * @param y The offset on the Y-axis in logical pixels.
     * @param z The offset on the Z-axis (depth).
     */
    void translate(float x, float y, float z);

    /**
     * Retrieves the current scissor rectangle active in the renderer.
     * <p>
     * This is essential for nested clipping regions (e.g., a scroll panel inside another scroll panel).
     * Effects can use this to calculate the intersection between the parent's clip rect and the
     * widget's desired clip rect.
     * </p>
     *
     * @return A float array {@code [x, y, width, height]} representing the current scissor window in logical pixels,
     *         or {@code null} if no scissor test is currently enabled.
     */
    float[] getCurrentScissor();
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

/**
 * Abstract interface for UI rendering operations.
 * This abstraction allows the core library to remain independent of specific game engine classes.
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
     * Draws a string on the screen.
     *
     * @param text   The text content to render.
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param color  The ARGB color value.
     * @param shadow Whether to render a drop shadow behind the text.
     */
    void drawString(String text, float x, float y, int color, boolean shadow);

    /**
     * Calculates the width of the given text in pixels.
     *
     * @param text The text to measure.
     * @return The width in pixels.
     */
    int getStringWidth(String text);

    /**
     * Returns the height of the standard font line.
     *
     * @return The font height in pixels.
     */
    int getFontHeight();

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
}
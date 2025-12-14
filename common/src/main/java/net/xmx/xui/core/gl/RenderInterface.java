/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import net.xmx.xui.core.text.TextComponent;

/**
 * Abstract interface for UI rendering operations.
 * This abstraction allows the core library to remain independent of specific game engine classes.
 * Updated to support the internal TextComponent system for rich text and multi-line rendering
 * (supporting both Vanilla and Custom fonts).
 *
 * @author xI-Mx-Ix
 */
public interface RenderInterface {

    /**
     * Initializes the rendering pipeline for a new frame.
     * <p>
     * Implementation note: The implementation class is responsible for acquiring
     * the necessary graphics context (e.g., creating a new {@code GuiGraphics} instance)
     * and setting up the transformation stacks.
     * </p>
     *
     * @param uiScale          The calculated logical UI scale factor.
     * @param clearDepthBuffer {@code true} to clear the depth buffer (GL_DEPTH_BUFFER_BIT)
     *                         before rendering starts.
     */
    void beginFrame(double uiScale, boolean clearDepthBuffer);

    /**
     * Finalizes the rendering frame.
     * <p>
     * This method is responsible for cleaning up the state set by {@link #beginFrame}.
     * This typically involves popping transformation matrices, flushing render buffers,
     * and disposing of any temporary graphics contexts created.
     * </p>
     */
    void endFrame();

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
    void drawText(TextComponent text, float x, float y, int color, boolean shadow);

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
    void drawWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow);

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

    // --- Matrix Transformations (New) ---

    /**
     * Pushes the current matrix onto the stack.
     */
    void pushMatrix();

    /**
     * Pops the current matrix from the stack.
     */
    void popMatrix();

    /**
     * Translates the coordinate system.
     *
     * @param x X offset.
     * @param y Y offset.
     * @param z Z offset.
     */
    void translate(float x, float y, float z);

    /**
     * Rotates the coordinate system around a specific 3D axis.
     *
     * @param degrees Angle in degrees.
     * @param x The X component of the axis vector.
     * @param y The Y component of the axis vector.
     * @param z The Z component of the axis vector.
     */
    void rotate(float degrees, float x, float y, float z);

    /**
     * Scales the coordinate system.
     *
     * @param x X scale.
     * @param y Y scale.
     * @param z Z scale.
     */
    void scale(float x, float y, float z);
}
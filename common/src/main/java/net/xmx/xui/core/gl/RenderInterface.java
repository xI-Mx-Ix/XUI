/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import net.xmx.xui.core.font.type.VanillaFont;
import net.xmx.xui.core.gl.renderer.GeometryRenderer;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.TextComponent;
import org.lwjgl.opengl.GL11;

/**
 * Abstract interface for UI rendering operations.
 * <p>
 * This interface defines the contract for rendering UI elements within the XUI framework.
 * It serves as the primary entry point for widgets to perform drawing commands.
 * </p>
 * <p>
 * <b>Architecture Note:</b><br>
 * To maintain a clean separation between the Core logic and the Platform implementation,
 * this interface provides <b>default implementations</b> for all generic rendering tasks
 * (Geometry, Scissor, Matrix transformations). These default methods delegate directly
 * to the singleton {@link UIRenderer} and its specific subsystems.
 * </p>
 * <p>
 * The concrete implementation (e.g., {@code RenderImpl}) is only required to implement
 * the platform-specific bridges, such as initializing the frame, handling text rendering via
 * game engine fonts, and finalizing the render buffers.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public interface RenderInterface {

    /**
     * Initializes the rendering pipeline for a new frame.
     * <p>
     * This method must be implemented by the platform-specific renderer. Its responsibilities include:
     * <ul>
     *     <li>Acquiring the platform's graphics context (e.g., Minecraft's {@code GuiGraphics}).</li>
     *     <li>Updating the {@link UIRenderer} with the current UI scale factor.</li>
     *     <li>Resetting the global transformation stack.</li>
     *     <li>Optionally clearing the OpenGL depth buffer for 3D UI elements.</li>
     * </ul>
     * </p>
     *
     * @param uiScale          The calculated logical UI scale factor for this frame.
     * @param clearDepthBuffer {@code true} if the depth buffer (GL_DEPTH_BUFFER_BIT) should be cleared
     *                         before rendering begins; {@code false} otherwise.
     */
    default void beginFrame(double uiScale, boolean clearDepthBuffer) {
        UIRenderer.getInstance().setCurrentUiScale(uiScale);
        UIRenderer.getInstance().getTransformStack().reset();

        if (clearDepthBuffer) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    /**
     * Finalizes the rendering frame.
     * <p>
     * This method must be implemented by the platform-specific renderer. It is responsible for:
     * <ul>
     *     <li>Flushing any pending batches in the platform's render buffers.</li>
     *     <li>Restoring the previous state of the platform's matrix stack.</li>
     *     <li>Cleaning up references to the graphics context to prevent memory leaks.</li>
     * </ul>
     * </p>
     */
    void endFrame();

    // --- Geometry Rendering (Direct Delegation to GeometryRenderer) ---

    /**
     * Draws a filled rectangle with uniform rounded corners.
     * <p>
     * This is a default method that delegates the rendering logic to the
     * {@link GeometryRenderer} accessed via the central {@link UIRenderer}.
     * It performs an immediate render pass, handling state capture and shader setup automatically.
     * </p>
     *
     * @param x      The absolute logical x-coordinate of the rectangle's top-left corner.
     * @param y      The absolute logical y-coordinate of the rectangle's top-left corner.
     * @param width  The width of the rectangle in logical pixels.
     * @param height The height of the rectangle in logical pixels.
     * @param color  The ARGB color value (e.g., 0xFF0000FF for opaque blue).
     * @param radius The radius to apply to all four corners of the rectangle.
     */
    default void drawRect(float x, float y, float width, float height, int color, float radius) {
        UIRenderer.getInstance().getGeometry().renderRect(x, y, width, height, color, radius);
    }

    /**
     * Draws a filled rectangle with individually configurable corner radii.
     * <p>
     * This is a default method that delegates the rendering logic to the
     * {@link GeometryRenderer} accessed via the central {@link UIRenderer}.
     * It allows for creating complex shapes like tabs or connected modal windows.
     * </p>
     *
     * @param x      The absolute logical x-coordinate.
     * @param y      The absolute logical y-coordinate.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color  The ARGB color value.
     * @param rTL    The radius of the Top-Left corner.
     * @param rTR    The radius of the Top-Right corner.
     * @param rBR    The radius of the Bottom-Right corner.
     * @param rBL    The radius of the Bottom-Left corner.
     */
    default void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().getGeometry().renderRect(x, y, width, height, color, rTL, rTR, rBR, rBL);
    }

    /**
     * Draws a hollow outline (border) with uniform rounded corners.
     * <p>
     * This is a default method that delegates the rendering logic to the
     * {@link GeometryRenderer} accessed via the central {@link UIRenderer}.
     * It ensures the outline is drawn entirely within the specified bounds (inner stroke).
     * </p>
     *
     * @param x         The absolute logical x-coordinate.
     * @param y         The absolute logical y-coordinate.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param color     The ARGB color value.
     * @param radius    The radius for all four corners.
     * @param thickness The thickness of the border line in logical pixels.
     */
    default void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        UIRenderer.getInstance().getGeometry().renderOutline(x, y, width, height, color, radius, thickness);
    }

    /**
     * Draws a hollow outline with individually configurable corner radii.
     * <p>
     * This is a default method that delegates the rendering logic to the
     * {@link GeometryRenderer} accessed via the central {@link UIRenderer}.
     * </p>
     *
     * @param x         The absolute logical x-coordinate.
     * @param y         The absolute logical y-coordinate.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param color     The ARGB color value.
     * @param thickness The thickness of the border line.
     * @param rTL       The radius of the Top-Left corner.
     * @param rTR       The radius of the Top-Right corner.
     * @param rBR       The radius of the Bottom-Right corner.
     * @param rBL       The radius of the Bottom-Left corner.
     */
    default void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().getGeometry().renderOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
    }

    // --- Scissor & Clipping (Direct Delegation to ScissorManager) ---

    /**
     * Enables a scissor test to clip rendering to a specific screen region.
     * <p>
     * This default method delegates to the {@link net.xmx.xui.core.gl.renderer.ScissorManager}
     * via {@link UIRenderer}. It handles the conversion of the provided logical coordinates
     * into physical OpenGL viewport coordinates and pushes the new state onto the scissor stack.
     * </p>
     *
     * @param x      The logical x-coordinate of the clipping area.
     * @param y      The logical y-coordinate of the clipping area.
     * @param width  The logical width of the clipping area.
     * @param height The logical height of the clipping area.
     */
    default void enableScissor(float x, float y, float width, float height) {
        UIRenderer.getInstance().getScissor().enableScissor(x, y, width, height);
    }

    /**
     * Disables the currently active scissor test.
     * <p>
     * This default method delegates to the {@link net.xmx.xui.core.gl.renderer.ScissorManager}
     * via {@link UIRenderer}. It pops the current scissor state from the stack and restores
     * the previous clipping region (if any) or disables the scissor test entirely.
     * </p>
     */
    default void disableScissor() {
        UIRenderer.getInstance().getScissor().disableScissor();
    }

    /**
     * Retrieves the current scissor rectangle active in the renderer.
     * <p>
     * This default method delegates to the {@link net.xmx.xui.core.gl.renderer.ScissorManager}
     * via {@link UIRenderer}. It returns the active clipping region transformed back into
     * logical coordinates, which is essential for calculating intersections in nested layouts.
     * </p>
     *
     * @return A float array {@code [x, y, width, height]} representing the current scissor window in logical pixels,
     *         or {@code null} if no scissor test is currently enabled.
     */
    default float[] getCurrentScissor() {
        return UIRenderer.getInstance().getScissor().getCurrentLogicalScissor();
    }

    // --- Matrix Transformations (Direct Delegation to UIRenderer) ---

    /**
     * Pushes the current transformation matrix onto the internal stack.
     * <p>
     * This default method delegates to the {@link UIRenderer}. It saves the current
     * Model-View and Normal matrices, allowing subsequent transformations to be isolated.
     * </p>
     */
    default void pushMatrix() {
        UIRenderer.getInstance().pushMatrix();
    }

    /**
     * Pops the top transformation matrix from the internal stack.
     * <p>
     * This default method delegates to the {@link UIRenderer}. It restores the
     * Model-View and Normal matrices to their state before the corresponding {@link #pushMatrix()} call.
     * </p>
     */
    default void popMatrix() {
        UIRenderer.getInstance().popMatrix();
    }

    /**
     * Translates the current coordinate system.
     * <p>
     * This default method delegates to the {@link UIRenderer}. The translation is applied
     * to the matrix currently at the top of the stack.
     * </p>
     *
     * @param x The offset along the X-axis.
     * @param y The offset along the Y-axis.
     * @param z The offset along the Z-axis.
     */
    default void translate(float x, float y, float z) {
        UIRenderer.getInstance().translate(x, y, z);
    }

    /**
     * Rotates the current coordinate system around a specific axis.
     * <p>
     * This default method delegates to the {@link UIRenderer}. The rotation is applied
     * to the matrix currently at the top of the stack.
     * </p>
     *
     * @param degrees The angle of rotation in degrees.
     * @param x       The X component of the rotation axis vector.
     * @param y       The Y component of the rotation axis vector.
     * @param z       The Z component of the rotation axis vector.
     */
    default void rotate(float degrees, float x, float y, float z) {
        UIRenderer.getInstance().rotate(degrees, x, y, z);
    }

    /**
     * Scales the current coordinate system.
     * <p>
     * This default method delegates to the {@link UIRenderer}. The scaling is applied
     * to the matrix currently at the top of the stack.
     * </p>
     *
     * @param x The scale factor for the X-axis.
     * @param y The scale factor for the Y-axis.
     * @param z The scale factor for the Z-axis.
     */
    default void scale(float x, float y, float z) {
        UIRenderer.getInstance().scale(x, y, z);
    }

    // --- Text Rendering ---

    /**
     * Draws a text component on the screen.
     * <p>
     * This method is abstract because text rendering often requires tight coupling with the
     * underlying platform's font system (e.g., Minecraft's FontRenderer). The implementation
     * class handles the bridge between the Core's {@link TextComponent} and the engine's font logic.
     * </p>
     *
     * @param text   The {@link TextComponent} containing the string and style data.
     * @param x      The absolute x-coordinate for the text origin.
     * @param y      The absolute y-coordinate for the text origin.
     * @param color  The ARGB color value (may be overridden by component styles).
     * @param shadow {@code true} to render a drop shadow behind the text; {@code false} for flat text.
     */
    void drawText(TextComponent text, float x, float y, int color, boolean shadow);

    /**
     * Draws text that automatically wraps to the next line if it exceeds a specified width.
     * <p>
     * This method is abstract to allow the platform implementation to utilize the game engine's
     * specific word-wrapping or string-splitting logic if available.
     * </p>
     *
     * @param text   The {@link TextComponent} to render.
     * @param x      The absolute x-coordinate.
     * @param y      The absolute y-coordinate.
     * @param width  The maximum width allowed in pixels before the text wraps to the next line.
     * @param color  The ARGB color value.
     * @param shadow {@code true} to render a drop shadow.
     */
    void drawWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow);

    /**
     * Returns the currently active GUI scale factor.
     * <p>
     * This value represents the platform's current scale configuration (e.g., Minecraft's GUI Scale).
     * It is used to determine pixel density and alignment.
     * </p>
     *
     * @return The current GUI scale factor.
     */
    double getGuiScale();

    // --- Vanilla Font Bridge (Platform Specific) ---

    /**
     * Draws text using the platform's native (vanilla) font renderer.
     * <p>
     * This method is called by {@link VanillaFont} to delegate
     * the drawing command back to the implementation, which has access to the game engine.
     * </p>
     */
    void drawVanillaText(TextComponent text, float x, float y, int color, boolean shadow);

    /**
     * Draws wrapped text using the platform's native (vanilla) font renderer.
     * Called by {@link VanillaFont}.
     */
    void drawVanillaWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow);

    /**
     * Retrieves the standard line height of the platform's native font.
     * Called by {@link VanillaFont}.
     *
     * @return The line height in pixels.
     */
    float getVanillaLineHeight();

    /**
     * Calculates the width of the text using the platform's native font renderer.
     * Called by {@link VanillaFont}.
     *
     * @param text The component to measure.
     * @return The width in pixels.
     */
    float getVanillaWidth(TextComponent text);

    /**
     * Calculates the height required for wrapped text using the platform's native font renderer.
     * Called by {@link VanillaFont}.
     *
     * @param text     The component to measure.
     * @param maxWidth The maximum width constraint.
     * @return The height in pixels.
     */
    float getVanillaWordWrapHeight(TextComponent text, float maxWidth);
}
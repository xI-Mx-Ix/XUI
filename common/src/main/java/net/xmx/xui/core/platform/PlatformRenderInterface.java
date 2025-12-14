/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.platform;

import net.xmx.xui.core.text.TextComponent;
import org.joml.Matrix4f;

/**
 * Defines the low-level platform operations required by the XUI Core.
 * <p>
 * This interface acts as the <b>Implementor</b> in the Bridge Pattern. It abstracts
 * the underlying game engine (e.g., Minecraft, Fabric, NeoForge) from the UI logic.
 * </p>
 * <p>
 * <b>Contract:</b>
 * Implementations of this interface must be "dumb". They should not contain logic regarding
 * UI state, visibility, or layout. They simply execute the requested platform-specific
 * operation (e.g., "Get the Graphics Object", "Draw this specific text string", "Clear GL Depth").
 * </p>
 *
 * @author xI-Mx-Ix
 */
public interface PlatformRenderInterface {

    /**
     * Acquires the platform's graphics context for the current frame and synchronizes the coordinate systems.
     * <p>
     * This method initializes the backing graphics object (e.g., GuiGraphics) and applies
     * a global scaling factor to align the game's native rendering scale with the
     * provided logical UI scale.
     * </p>
     *
     * @param uiScale The current logical scale factor of the XUI context.
     */
    void initiateRenderCycle(double uiScale);

    /**
     * Flushes buffers and releases the graphics context.
     * <p>
     * This must be called at the very end of the render pass.
     * It ensures that all batched operations (like text rendering) are sent to the GPU
     * before the module relinquishes control back to the game engine.
     * </p>
     */
    void finishRenderCycle();

    /**
     * Retrieves the current GUI scale factor from the game settings.
     * <p>
     * This is used by the Core to calculate the logical pixel density.
     * </p>
     *
     * @return The raw scale factor (e.g., 1.0, 2.0, 3.0, 4.0).
     */
    double getScaleFactor();

    // =================================================================================
    // Native Font Operations (Bridge to MC FontRenderer)
    // =================================================================================

    /**
     * Renders a text component using the platform's native font engine (e.g., Vanilla Font).
     *
     * @param text      The XUI component to render. Implementation must convert this to the native component format.
     * @param x         The absolute X coordinate.
     * @param y         The absolute Y coordinate.
     * @param color     The ARGB color value.
     * @param shadow    {@code true} to render the text with a drop shadow.
     * @param pose      The current Model-View matrix from the Core's {@link net.xmx.xui.core.gl.TransformStack}.
     *                  The implementation <b>must</b> apply this matrix to the game's internal matrix stack
     *                  before drawing to ensure the text respects XUI's transformations.
     */
    void renderNativeText(TextComponent text, float x, float y, int color, boolean shadow, Matrix4f pose);

    /**
     * Renders wrapped text using the platform's native font engine.
     *
     * @param text      The XUI component to render.
     * @param x         The absolute X coordinate.
     * @param y         The absolute Y coordinate.
     * @param width     The maximum width allowed in pixels before wrapping to a new line.
     * @param color     The ARGB color value.
     * @param shadow    {@code true} to render the text with a drop shadow.
     * @param pose      The current Model-View matrix from the Core's transform stack.
     */
    void renderNativeWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow, Matrix4f pose);

    /**
     * Retrieves the standard line height of the platform's native font.
     *
     * @return The line height in pixels (typically 9 for Minecraft).
     */
    float getNativeLineHeight();

    /**
     * Measures the width of a text component using the platform's native font renderer.
     *
     * @param text The component to measure.
     * @return The total width in pixels.
     */
    float getNativeStringWidth(TextComponent text);

    /**
     * Calculates the vertical space required to render the text if it were wrapped
     * to the specified width, using the platform's native logic.
     *
     * @param text     The component to measure.
     * @param maxWidth The maximum width constraint in pixels.
     * @return The total height in pixels.
     */
    float getNativeWordWrapHeight(TextComponent text, float maxWidth);
}
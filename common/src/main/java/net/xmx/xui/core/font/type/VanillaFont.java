/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font.type;

import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.platform.PlatformRenderInterface;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.TextComponent;

/**
 * Represents the platform's native font engine (e.g., Minecraft's "Vanilla" Font).
 * <p>
 * This class acts as a <b>Proxy</b> or <b>Adapter</b>. Since the Core module cannot directly
 * access game engine classes (like {@code FontRenderer}), this class delegates all measurement
 * and rendering requests to the {@link PlatformRenderInterface} via the
 * central {@link UIRenderer}.
 * </p>
 * <p>
 * <b>Usage:</b><br>
 * When a {@link TextComponent} is assigned a {@code VanillaFont}, the rendering pipeline
 * automatically switches from the internal MSDF shader to the game's native text rendering
 * system, preserving compatibility with resource packs or other mods that alter the default font.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class VanillaFont extends Font {

    /**
     * Constructs a new VanillaFont instance.
     * <p>
     * Sets the internal type to {@link Font.Type#VANILLA}, signaling the renderer
     * to use the platform backend instead of the internal shader pipeline.
     * </p>
     */
    public VanillaFont() {
        super(Type.VANILLA);
    }

    /**
     * Retrieves the standard vertical height of a single line of text.
     * <p>
     * This value is fetched directly from the platform backend to ensure alignment
     * with the game's native UI elements. For Minecraft, this is typically 9 pixels.
     * </p>
     *
     * @return The line height in logical pixels.
     */
    @Override
    public float getLineHeight() {
        return UIRenderer.getInstance().getPlatform().getNativeLineHeight();
    }

    /**
     * Calculates the total horizontal width of the provided component tree.
     * <p>
     * This method delegates to the backend to perform the measurement, ensuring that
     * any native formatting codes (e.g., bold styles affecting width) are accurately calculated.
     * </p>
     *
     * @param component The root text component to measure.
     * @return The total width in logical pixels.
     */
    @Override
    public float getWidth(TextComponent component) {
        return UIRenderer.getInstance().getPlatform().getNativeStringWidth(component);
    }

    /**
     * Calculates the vertical height required to render the text if it were wrapped
     * within a specific width constraint.
     * <p>
     * This relies on the platform's specific word-wrapping logic to determine line breaks.
     * </p>
     *
     * @param component The text content to measure.
     * @param maxWidth  The maximum allowed width in logical pixels.
     * @return The total vertical height in logical pixels.
     */
    @Override
    public float getWordWrapHeight(TextComponent component, float maxWidth) {
        return UIRenderer.getInstance().getPlatform().getNativeWordWrapHeight(component, maxWidth);
    }

    /**
     * Renders a single line of text using the platform's native engine.
     * <p>
     * <b>Synchronization Logic:</b><br>
     * Because the native game engine maintains its own matrix stack (e.g., {@code PoseStack}),
     * we must explicitly pass the current Model-View matrix from the XUI {@link UIRenderer}.
     * The backend implementation is responsible for applying this matrix to the game's stack
     * before drawing, ensuring the text follows all XUI transformations (rotation, scale, position).
     * </p>
     *
     * @param context   The central renderer context orchestrating the draw call.
     * @param component The text component containing content and style.
     * @param x         The absolute logical X coordinate.
     * @param y         The absolute logical Y coordinate.
     * @param color     The ARGB color value (may be overridden by component styles).
     * @param shadow    {@code true} to render a drop shadow; {@code false} for flat text.
     */
    @Override
    public void draw(UIRenderer context, TextComponent component, float x, float y, int color, boolean shadow) {
        context.getPlatform().renderNativeText(
                component,
                x,
                y,
                color,
                shadow,
                context.getTransformStack().getDirectModelMatrix()
        );
    }

    /**
     * Renders text that automatically wraps to new lines based on a width constraint.
     * <p>
     * Similar to {@link #draw}, this delegates to the backend and synchronizes the
     * transformation matrices to ensure proper placement within the UI hierarchy.
     * </p>
     *
     * @param context   The central renderer context.
     * @param component The text component to render.
     * @param x         The absolute logical X coordinate.
     * @param y         The absolute logical Y coordinate.
     * @param maxWidth  The maximum width allowed in pixels before wrapping.
     * @param color     The ARGB color value.
     * @param shadow    {@code true} to render a drop shadow.
     */
    @Override
    public void drawWrapped(UIRenderer context, TextComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        context.getPlatform().renderNativeWrappedText(
                component,
                x,
                y,
                maxWidth,
                color,
                shadow,
                context.getTransformStack().getDirectModelMatrix()
        );
    }
}
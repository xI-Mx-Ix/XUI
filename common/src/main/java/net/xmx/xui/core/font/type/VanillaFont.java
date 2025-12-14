/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font.type;

import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.gl.RenderProvider;
import net.xmx.xui.core.text.TextComponent;

/**
 * Represents the platform's native font (e.g., Minecraft Vanilla Font).
 * <p>
 * This class resides in the Core module and is therefore strictly decoupled from
 * any specific game engine classes. It functions as a proxy, delegating all
 * measurement and rendering calls back to the {@link RenderInterface}, which
 * is implemented by the platform-specific module.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class VanillaFont extends Font {

    /**
     * Constructs a new VanillaFont instance.
     * Sets the type to {@link Font.Type#VANILLA}.
     */
    public VanillaFont() {
        super(Type.VANILLA);
    }

    /**
     * Retrieves the line height from the platform implementation.
     *
     * @return The line height in logical pixels.
     */
    @Override
    public float getLineHeight() {
        // Delegate to the registered renderer implementation
        return RenderProvider.get().getVanillaLineHeight();
    }

    /**
     * Calculates the width of the component by asking the platform implementation.
     *
     * @param component The root component to measure.
     * @return The total width in logical pixels.
     */
    @Override
    public float getWidth(TextComponent component) {
        return RenderProvider.get().getVanillaWidth(component);
    }

    /**
     * Calculates the wrapped height of the component by asking the platform implementation.
     *
     * @param component The text content.
     * @param maxWidth  The width limit in logical pixels.
     * @return The total vertical height in logical pixels.
     */
    @Override
    public float getWordWrapHeight(TextComponent component, float maxWidth) {
        return RenderProvider.get().getVanillaWordWrapHeight(component, maxWidth);
    }

    /**
     * Delegates the draw call to the platform-specific {@link RenderInterface#drawVanillaText} method.
     *
     * @param context   The render interface context.
     * @param component The text component to render.
     * @param x         The absolute logical X coordinate.
     * @param y         The absolute logical Y coordinate.
     * @param color     The default ARGB text color.
     * @param shadow    {@code true} to draw a drop shadow.
     */
    @Override
    public void draw(RenderInterface context, TextComponent component, float x, float y, int color, boolean shadow) {
        context.drawVanillaText(component, x, y, color, shadow);
    }

    /**
     * Delegates the wrapped draw call to the platform-specific {@link RenderInterface#drawVanillaWrappedText} method.
     *
     * @param context   The render interface context.
     * @param component The text component to render.
     * @param x         The absolute logical X coordinate.
     * @param y         The absolute logical Y coordinate.
     * @param maxWidth  The maximum width in logical pixels.
     * @param color     The default ARGB text color.
     * @param shadow    {@code true} to draw a drop shadow.
     */
    @Override
    public void drawWrapped(RenderInterface context, TextComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        context.drawVanillaWrappedText(component, x, y, maxWidth, color, shadow);
    }
}
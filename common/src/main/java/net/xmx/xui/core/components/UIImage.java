/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.texture.TextureCache;
import net.xmx.xui.core.gl.texture.UITexture;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * A widget that displays an image loaded from the classpath resources.
 * <p>
 * Supports tinting via {@link ThemeProperties#TEXT_COLOR} (white = no tint)
 * and rounded corners via {@link ThemeProperties#BORDER_RADIUS}.
 * The image stretches to fill the widget's dimensions.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIImage extends UIWidget {

    /**
     * Optional tint color for the image. Default is White (No tint).
     * We reuse TEXT_COLOR for this purpose to keep styles consistent.
     */
    public static final StyleKey<Integer> TINT_COLOR = ThemeProperties.TEXT_COLOR;

    private UITexture texture;

    /**
     * Controls the texture filtering mode.
     * true = Nearest Neighbor (Pixel Perfect, sharp edges).
     * false = Linear Interpolation (Smooth, blurred edges).
     */
    private boolean pixelPerfect = true;

    /**
     * Constructs an empty image component.
     * Use {@link #setImage(String, String)} to assign a texture.
     */
    public UIImage() {
        // Default transparent background, white tint (original image colors)
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0x00000000)
                .set(TINT_COLOR, 0xFFFFFFFF)
                .set(ThemeProperties.BORDER_RADIUS, 0.0f);
    }

    /**
     * Sets the image resource to display.
     * The texture is cached automatically.
     *
     * @param namespace The mod namespace (e.g. "xui").
     * @param path      The path within assets (e.g. "textures/logo.png").
     * @return This instance for chaining.
     */
    public UIImage setImage(String namespace, String path) {
        this.texture = TextureCache.get(namespace, path);
        return this;
    }

    /**
     * Sets the image texture directly.
     *
     * @param texture The loaded texture instance.
     * @return This instance.
     */
    public UIImage setTexture(UITexture texture) {
        this.texture = texture;
        return this;
    }

    /**
     * Returns the currently set texture.
     *
     * @return The texture instance.
     */
    public UITexture getTexture() {
        return texture;
    }

    /**
     * Configures the texture filtering mode.
     *
     * @param pixelPerfect If true, uses Nearest-Neighbor (blocky/sharp).
     *                     If false, uses Linear (smooth/blurry).
     * @return This instance.
     */
    public UIImage setPixelPerfect(boolean pixelPerfect) {
        this.pixelPerfect = pixelPerfect;
        return this;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        if (texture == null) return;

        // Retrieve styles
        float radius = getFloat(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        int tint = getColor(TINT_COLOR, state, deltaTime);

        // Handle transparency of the widget (opacity) combined with tint alpha
        float opacity = style().getValue(state, ThemeProperties.OPACITY);
        if (opacity < 1.0f) {
            int alpha = (tint >> 24) & 0xFF;
            alpha = (int) (alpha * opacity);
            tint = (tint & 0x00FFFFFF) | (alpha << 24);
        }

        // Delegate to renderer with the specific filtering flag
        renderer.getImage().drawImage(
                texture,
                x, y, width, height,
                tint,
                radius,
                pixelPerfect,
                renderer.getCurrentUiScale(),
                renderer.getTransformStack().getDirectModelMatrix()
        );
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.gl.UIRenderer;

/**
 * Concrete implementation of the {@link UIRenderInterface} for Minecraft.
 * <p>
 * This class acts as a Singleton bridge. It initializes the Minecraft-specific
 * {@link GuiGraphics} for text rendering and delegates raw geometry operations
 * to the {@link UIRenderer}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderImpl implements UIRenderInterface {

    private static UIRenderImpl instance;

    private final GuiGraphics guiGraphics;

    /**
     * Private constructor for Singleton.
     * Initializes the internal {@link GuiGraphics} using the Minecraft instance and its render buffers.
     */
    private UIRenderImpl() {
        this.guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
    }

    /**
     * Retrieves the global instance of the render interface implementation.
     *
     * @return The UIRenderImpl singleton.
     */
    public static UIRenderImpl getInstance() {
        if (instance == null) {
            instance = new UIRenderImpl();
        }
        return instance;
    }

    // --- Geometry & Effects (Delegated to UIRenderer) ---

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        UIRenderer.getInstance().drawRect(x, y, width, height, color, radius, radius, radius, radius);
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawRect(x, y, width, height, color, rTL, rTR, rBR, rBL);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        UIRenderer.getInstance().drawOutline(x, y, width, height, color, thickness, radius, radius, radius, radius);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        UIRenderer.getInstance().enableScissor(x, y, width, height);
    }

    @Override
    public void disableScissor() {
        UIRenderer.getInstance().disableScissor();
    }

    @Override
    public int[] getCurrentScissor() {
        return UIRenderer.getInstance().getCurrentScissor();
    }

    // --- Text Rendering (Minecraft Specific) ---

    @Override
    public void drawText(Component text, float x, float y, int color, boolean shadow) {
        // Text rendering delegates to Minecraft's font engine via GuiGraphics
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 0.1f); // Ensure text is drawn on top of the geometry layer
        guiGraphics.drawString(Minecraft.getInstance().font, text, (int) x, (int) y, color, shadow);
        guiGraphics.pose().popPose();
    }

    @Override
    public void drawWrappedText(Component text, float x, float y, float width, int color, boolean shadow) {
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, text, (int) x, (int) y, (int) width, color);
    }

    @Override
    public int getTextWidth(Component text) {
        return Minecraft.getInstance().font.width(text);
    }

    @Override
    public int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public int getWordWrapHeight(Component text, int maxWidth) {
        return Minecraft.getInstance().font.wordWrapHeight(text, maxWidth);
    }

    @Override
    public void translateZ(float z) {
        guiGraphics.pose().translate(0, 0, z);
    }
}
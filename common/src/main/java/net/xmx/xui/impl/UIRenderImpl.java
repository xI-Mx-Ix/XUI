/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.xmx.xui.core.font.UIFont;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.UIComponent;

/**
 * Concrete implementation of the {@link UIRenderInterface}.
 * <p>
 * This class acts as the bridge between the high-level UI component system
 * and the low-level rendering logic (Minecraft or OpenGL). It handles
 * coordinate alignment (pixel snapping), global offsets, and delegates text
 * rendering to the appropriate {@link UIFont} implementation.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderImpl implements UIRenderInterface {

    private static UIRenderImpl instance;

    private GuiGraphics guiGraphics;
    private double currentScale = 1.0;
    private float globalOffsetX = 0.0f;
    private float globalOffsetY = 0.0f;

    private UIRenderImpl() {
    }

    /**
     * Retrieves the singleton instance.
     * @return The renderer implementation.
     */
    public static UIRenderImpl getInstance() {
        if (instance == null) {
            instance = new UIRenderImpl();
        }
        return instance;
    }

    // --- State Management ---

    public void setScale(double scale) {
        this.currentScale = scale;
    }

    public void setGuiGraphics(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
        this.globalOffsetX = 0;
        this.globalOffsetY = 0;
    }

    /**
     * Exposes the current GuiGraphics for Vanilla font rendering.
     * @return The active GuiGraphics instance.
     */
    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public double getCurrentScale() {
        return currentScale;
    }

    // --- Pixel Snapping ---

    private float alignX(float logicalX) {
        return logicalX + globalOffsetX;
    }

    private float alignY(float logicalY) {
        return logicalY + globalOffsetY;
    }

    private float alignDim(float logicalSize) {
        return logicalSize;
    }

    // --- Geometry Rendering (Delegation) ---

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        UIRenderer.getInstance().drawRect(alignX(x), alignY(y), alignDim(width), alignDim(height), color, radius, radius, radius, radius, currentScale);
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawRect(alignX(x), alignY(y), alignDim(width), alignDim(height), color, rTL, rTR, rBR, rBL, currentScale);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        UIRenderer.getInstance().drawOutline(alignX(x), alignY(y), alignDim(width), alignDim(height), color, thickness, radius, radius, radius, radius, currentScale);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawOutline(alignX(x), alignY(y), alignDim(width), alignDim(height), color, thickness, rTL, rTR, rBR, rBL, currentScale);
    }

    // --- Text Rendering (Delegation to Font Abstraction) ---

    @Override
    public void drawText(UIComponent text, float x, float y, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);

        text.getFont().draw(this, text, ax, ay, color, shadow);
    }

    @Override
    public void drawWrappedText(UIComponent text, float x, float y, float width, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);
        float aw = alignDim(width);

        text.getFont().drawWrapped(this, text, ax, ay, aw, color, shadow);
    }

    @Override
    public int getTextWidth(UIComponent text) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWidth(text));
    }

    @Override
    public int getFontHeight() {
        // Returns Vanilla height by default as a generic metric,
        // components needing specific font height should call text.getFont().getLineHeight()
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public int getWordWrapHeight(UIComponent text, int maxWidth) {
        if (text == null || text.getFont() == null) return 0;
        return (int) Math.ceil(text.getFont().getWordWrapHeight(text, maxWidth));
    }

    // --- Scissor & Transforms ---

    @Override
    public void enableScissor(float x, float y, float width, float height) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        int physX = (int) (alignX(x) * currentScale);
        int physY = (int) (alignY(y) * currentScale);
        int physW = (int) (alignDim(width) * currentScale);
        int physH = (int) (alignDim(height) * currentScale);
        UIRenderer.getInstance().getScissor().pushScissor(physX, physY, physW, physH, 1.0);
    }

    @Override
    public void disableScissor() {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        UIRenderer.getInstance().getScissor().popScissor(1.0);
    }

    @Override
    public void translate(float x, float y, float z) {
        this.globalOffsetX += x;
        this.globalOffsetY += y;
        if (z != 0 && guiGraphics != null) {
            guiGraphics.pose().translate(0, 0, z);
        }
    }

    @Override
    public float[] getCurrentScissor() {
        int[] phys = UIRenderer.getInstance().getScissor().getCurrentScissor();
        if (phys == null || currentScale == 0) return null;
        return new float[]{
                (float) (phys[0] / currentScale),
                (float) (phys[1] / currentScale),
                (float) (phys[2] / currentScale),
                (float) (phys[3] / currentScale)
        };
    }
}
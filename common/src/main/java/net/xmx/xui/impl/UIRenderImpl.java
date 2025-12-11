/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.gl.UIRenderer;

/**
 * Concrete implementation of the {@link UIRenderInterface} for Minecraft.
 * <p>
 * This class acts as a Singleton bridge between the library's rendering abstraction
 * and the specific Minecraft rendering systems. It delegates raw geometry operations
 * (rectangles, outlines) to the custom {@link UIRenderer} and handles text rendering
 * via an injected {@link GuiGraphics} instance.
 * </p>
 * <p>
 * <b>Pixel Snapping Implementation:</b><br>
 * Since the UI uses floating-point scaling factors, logical coordinates often result
 * in sub-pixel positions (e.g., 10.5px). This implementation forces coordinates to
 * snap to the nearest physical monitor pixel using the {@link #align(float)} method
 * before rendering. This prevents visual artifacts like blurry fonts or uneven border thicknesses.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderImpl implements UIRenderInterface {

    private static UIRenderImpl instance;

    /**
     * The active GuiGraphics instance used for rendering text.
     * <p>
     * This is injected by the {@code UIContext} at the start of every render frame.
     * It contains the necessary PoseStack transformations (scaling) to ensure text
     * aligns with the custom geometry projection.
     * </p>
     */
    private GuiGraphics guiGraphics;

    /**
     * The scale factor currently used for geometry projection.
     * Defaults to 1.0 but is overridden by {@link #setScale(double)}.
     */
    private double currentScale = 1.0;

    /**
     * Private constructor for Singleton pattern.
     */
    private UIRenderImpl() {
        // Prevent external instantiation
    }

    /**
     * Retrieves the global singleton instance of the render interface implementation.
     *
     * @return The UIRenderImpl singleton.
     */
    public static UIRenderImpl getInstance() {
        if (instance == null) {
            instance = new UIRenderImpl();
        }
        return instance;
    }

    /**
     * Updates the scale factor used for rendering calculations.
     * This is typically called by the {@code UIContext} before rendering a frame.
     *
     * @param scale The new scale factor (logical pixels to physical pixels).
     */
    public void setScale(double scale) {
        this.currentScale = scale;
    }

    /**
     * Sets the active {@link GuiGraphics} instance for the current render pass.
     * <p>
     * This is crucial for text rendering, as the standard Minecraft font renderer
     * relies on the {@code PoseStack} within GuiGraphics.
     * </p>
     *
     * @param guiGraphics The graphics instance for the current frame.
     */
    public void setGuiGraphics(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    /**
     * Aligns a logical coordinate to the nearest physical screen pixel to prevent sub-pixel artifacts.
     * <p>
     * The formula used is: {@code round(logical * scale) / scale}.
     * This converts the logical coordinate to physical pixels, snaps it to the grid,
     * and converts it back to logical space for the GPU.
     * </p>
     *
     * @param logicalValue The coordinate in the UI system.
     * @return The coordinate adjusted to start exactly on a monitor pixel.
     */
    private float align(float logicalValue) {
        if (currentScale == 0) return logicalValue;
        return (float) (Math.round(logicalValue * currentScale) / currentScale);
    }

    /**
     * Aligns a dimension (width/height) to match integer physical pixels.
     * <p>
     * This method ensures that a non-zero logical size never rounds down to zero physical pixels.
     * If a border is 1px logical, but the scale makes it 0.4px physical, this forces it to be
     * at least 1 physical pixel wide to remain visible.
     * </p>
     *
     * @param logicalSize The width or height in logical pixels.
     * @return The aligned size, guaranteed to be at least 1 physical pixel if logicalSize > 0.
     */
    private float alignDim(float logicalSize) {
        if (logicalSize <= 0) return 0;
        float aligned = align(logicalSize);

        // If the size is positive but rounds to 0 (very thin line on low scale), force 1 physical pixel.
        if (aligned == 0 && logicalSize > 0) {
            return (float) (1.0 / currentScale);
        }
        return aligned;
    }

    // --- Geometry & Effects (Delegated to UIRenderer) ---

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        // Snap position and dimensions to the pixel grid
        float ax = align(x);
        float ay = align(y);
        float aw = alignDim(width);
        float ah = alignDim(height);

        UIRenderer.getInstance().drawRect(ax, ay, aw, ah, color, radius, radius, radius, radius, currentScale);
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        float ax = align(x);
        float ay = align(y);
        float aw = alignDim(width);
        float ah = alignDim(height);

        UIRenderer.getInstance().drawRect(ax, ay, aw, ah, color, rTL, rTR, rBR, rBL, currentScale);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        float ax = align(x);
        float ay = align(y);
        float aw = alignDim(width);
        float ah = alignDim(height);

        // Calculate minimal thickness to prevent borders from disappearing
        float minThickness = (float) (1.0 / currentScale);
        float snappedThickness = Math.max(minThickness, align(thickness));

        UIRenderer.getInstance().drawOutline(ax, ay, aw, ah, color, snappedThickness, radius, radius, radius, radius, currentScale);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        float ax = align(x);
        float ay = align(y);
        float aw = alignDim(width);
        float ah = alignDim(height);

        float minThickness = (float) (1.0 / currentScale);
        float snappedThickness = Math.max(minThickness, align(thickness));

        UIRenderer.getInstance().drawOutline(ax, ay, aw, ah, color, snappedThickness, rTL, rTR, rBR, rBL, currentScale);
    }

    // --- Scissor Logic & Batch Flushing ---

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Note:</b> This method flushes the Minecraft render buffer to ensure
     * text is drawn before the clip rect changes. It calculates the exact physical pixel
     * coordinates to prevent rounding errors (e.g., a 10.5px logical position becoming 21px physical).
     * </p>
     * <p>
     * To ensure compatibility with the {@link UIRenderer} which expects integers, this method
     * pre-calculates the physical coordinates and passes a scale of 1.0. This bypasses
     * any potential loss of precision if we were to cast logical coordinates to integers directly.
     * </p>
     */
    @Override
    public void enableScissor(float x, float y, float width, float height) {
        // Flush any pending rendering commands (especially text) before changing state.
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // 1. Align logical coordinates to the pixel grid (e.g., 10.5 -> 10.5)
        float ax = align(x);
        float ay = align(y);
        float aw = alignDim(width);
        float ah = alignDim(height);

        // 2. Convert to physical pixels (e.g., 10.5 * 2.0 = 21)
        int physX = (int) (ax * currentScale);
        int physY = (int) (ay * currentScale);
        int physW = (int) (aw * currentScale);
        int physH = (int) (ah * currentScale);

        // 3. Delegate to renderer with scale 1.0, as we have already performed the scaling.
        // This ensures the scissor rect exactly matches the pixels drawn by drawRect/drawOutline.
        UIRenderer.getInstance().enableScissor(physX, physY, physW, physH, 1.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableScissor() {
        // Flush any pending rendering commands inside the scissor region.
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // Disable the GL Scissor test.
        // We pass 1.0 here because enableScissor used the 1.0 scale trick.
        UIRenderer.getInstance().disableScissor(1.0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This retrieves the raw physical scissor rectangle from the renderer and converts it back
     * to logical coordinates so that {@link net.xmx.xui.core.effect.UIScissorsEffect} can
     * compute intersections correctly.
     * </p>
     */
    @Override
    public float[] getCurrentScissor() {
        int[] physScissor = UIRenderer.getInstance().getCurrentScissor();
        if (physScissor == null) return null;

        if (currentScale == 0) return new float[4];

        // Convert physical pixels back to logical pixels for the UI system
        return new float[]{
                (float) (physScissor[0] / currentScale),
                (float) (physScissor[1] / currentScale),
                (float) (physScissor[2] / currentScale),
                (float) (physScissor[3] / currentScale)
        };
    }

    // --- Text Rendering (Minecraft Specific) ---

    @Override
    public void drawText(Component text, float x, float y, int color, boolean shadow) {
        if (guiGraphics == null) return;

        // CRITICAL: Snap text position to the pixel grid.
        float ax = align(x);
        float ay = align(y);

        guiGraphics.pose().pushPose();
        // Translate slightly on Z to render over geometry
        guiGraphics.pose().translate(0, 0, 0.1f);
        guiGraphics.drawString(Minecraft.getInstance().font, text, (int) ax, (int) ay, color, shadow);
        guiGraphics.pose().popPose();
    }

    @Override
    public void drawWrappedText(Component text, float x, float y, float width, int color, boolean shadow) {
        if (guiGraphics == null) return;

        float ax = align(x);
        float ay = align(y);
        int aw = (int) alignDim(width);

        guiGraphics.drawWordWrap(Minecraft.getInstance().font, text, (int) ax, (int) ay, aw, color);
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
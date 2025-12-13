/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.gl.UITransformStack;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.UITextComponent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Concrete implementation of the {@link UIRenderInterface}.
 * <p>
 * This class acts as the bridge between the high-level UI component system
 * and the low-level rendering logic. It utilizes {@link UITransformStack} to
 * manage matrix transformations independently from the game engine, ensuring
 * custom shaders receive the correct ModelView matrix.
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

    private final UITransformStack transformStack = new UITransformStack();

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

        // Ensure we start fresh each frame to avoid stack drift
        this.transformStack.reset();
    }

    /**
     * Exposes the current GuiGraphics for Vanilla font rendering.
     * @return The active GuiGraphics instance.
     */
    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    /**
     * Provides the current custom model-view matrix for shaders.
     * @return The current transformation matrix.
     */
    public Matrix4f getCurrentMatrix() {
        return transformStack.getDirectModelMatrix();
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
        // Pass the current custom matrix to the renderer
        UIRenderer.getInstance().drawRect(alignX(x), alignY(y), alignDim(width), alignDim(height), color, radius, radius, radius, radius, currentScale, transformStack.getDirectModelMatrix());
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawRect(alignX(x), alignY(y), alignDim(width), alignDim(height), color, rTL, rTR, rBR, rBL, currentScale, transformStack.getDirectModelMatrix());
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        UIRenderer.getInstance().drawOutline(alignX(x), alignY(y), alignDim(width), alignDim(height), color, thickness, radius, radius, radius, radius, currentScale, transformStack.getDirectModelMatrix());
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawOutline(alignX(x), alignY(y), alignDim(width), alignDim(height), color, thickness, rTL, rTR, rBR, rBL, currentScale, transformStack.getDirectModelMatrix());
    }

    // --- Text Rendering (Delegation to Font Abstraction) ---

    @Override
    public void drawText(UITextComponent text, float x, float y, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);

        text.getFont().draw(this, text, ax, ay, color, shadow);
    }

    @Override
    public void drawWrappedText(UITextComponent text, float x, float y, float width, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);
        float aw = alignDim(width);

        text.getFont().drawWrapped(this, text, ax, ay, aw, color, shadow);
    }

    // --- Scissor & Transforms ---

    @Override
    public void enableScissor(float x, float y, float width, float height) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // Standard scaling logic for scissor test
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

    // --- Matrix Implementation ---

    @Override
    public void pushMatrix() {
        // Update both Vanilla PoseStack (for Vanilla Fonts) and Custom Matrix (for Shaders)
        if (guiGraphics != null) guiGraphics.pose().pushPose();

        // Push state to our independent stack
        transformStack.push();
    }

    @Override
    public void popMatrix() {
        if (guiGraphics != null) guiGraphics.pose().popPose();

        // Pop state from our independent stack
        transformStack.pop();
    }

    @Override
    public void translate(float x, float y, float z) {
        // Vanilla
        if (guiGraphics != null) guiGraphics.pose().translate(x, y, z);

        // Custom
        transformStack.applyTranslation(x, y, z);
    }

    @Override
    public void rotate(float degrees, float x, float y, float z) {
        // Vanilla
        if (guiGraphics != null) {
            Vector3f axisVector = new Vector3f(x, y, z);
            if (axisVector.lengthSquared() > 0) axisVector.normalize();
            Axis axis = Axis.of(axisVector);
            guiGraphics.pose().mulPose(axis.rotationDegrees(degrees));
        }

        // Custom
        transformStack.applyRotation(degrees, x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        // Vanilla
        if (guiGraphics != null) guiGraphics.pose().scale(x, y, z);

        // Custom
        transformStack.applyScaling(x, y, z);
    }
}
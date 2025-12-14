/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.gl.TransformStack;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.TextComponent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Concrete implementation of the {@link RenderInterface}.
 * <p>
 * This class acts as the bridge between the high-level UI component system (Core)
 * and the low-level rendering logic provided by Minecraft and OpenGL.
 * </p>
 * <p>
 * <b>Key Responsibilities:</b>
 * <ul>
 *     <li>Instantiating {@link GuiGraphics} for each frame to render onto the current screen.</li>
 *     <li>Managing the {@link TransformStack} to handle custom 3D matrix transformations independently of the game engine.</li>
 *     <li>Applying global scaling logic to map logical UI pixels to physical screen pixels.</li>
 *     <li>Delegating geometric draw calls to {@link UIRenderer}.</li>
 *     <li>Delegating text draw calls to the font system.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class RenderImpl implements RenderInterface {

    private static RenderImpl instance;

    /**
     * The Minecraft Graphics object for the current frame.
     * Created in {@link #beginFrame(double, boolean)} and destroyed in {@link #endFrame()}.
     */
    private GuiGraphics guiGraphics;

    /**
     * The current logical scale factor calculated by the UIContext.
     * Used to calculate scissor regions and shader projections.
     */
    private double currentScale = 1.0;

    /**
     * Global X offset applied to all rendering operations (usually 0).
     */
    private float globalOffsetX = 0.0f;

    /**
     * Global Y offset applied to all rendering operations (usually 0).
     */
    private float globalOffsetY = 0.0f;

    /**
     * Independent transformation stack for custom shaders (XUI Core Shader).
     * This runs in parallel to Minecraft's PoseStack but offers more control for UI widgets.
     */
    private final TransformStack transformStack = new TransformStack();

    /**
     * Private constructor to enforce Singleton pattern via {@link #getInstance()}.
     */
    private RenderImpl() {
    }

    /**
     * Retrieves the singleton instance of the renderer.
     *
     * @return The renderer implementation.
     */
    public static RenderImpl getInstance() {
        if (instance == null) {
            instance = new RenderImpl();
        }
        return instance;
    }

    // --- Lifecycle Management ---

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs the following steps:
     * <ol>
     *     <li>Instantiates a new {@link GuiGraphics} object tied to the main Minecraft render buffers.</li>
     *     <li>Resets the internal {@link TransformStack} to identity.</li>
     *     <li>Optionally clears the OpenGL depth buffer if requested (for 3D UI scenes).</li>
     *     <li>Calculates the relative scale between XUI's logical scale and Minecraft's GUI scale.</li>
     *     <li>Pushes and scales the Vanilla {@code PoseStack} to ensure fonts render at the correct size.</li>
     * </ol>
     * </p>
     */
    @Override
    public void beginFrame(double uiScale, boolean clearDepthBuffer) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Instantiate GuiGraphics
        // We use the main buffer source from Minecraft to allow efficient batching of draw calls.
        this.guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        this.currentScale = uiScale;

        // Reset global offsets for the new frame
        this.globalOffsetX = 0;
        this.globalOffsetY = 0;

        // 2. Reset Transform Stack
        // Ensure we start fresh each frame to avoid matrix drift.
        this.transformStack.reset();

        // 3. Optional Depth Clear
        // Essential for complex screens with Z-layering, but must be skipped for HUDs/Overlays.
        if (clearDepthBuffer) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }

        // 4. Calculate Scaling Ratio
        // XUI uses its own logical pixel grid (e.g., based on Reference Height 360).
        // Minecraft uses its own GUI Scale (Auto, 1, 2, 3...).
        // We need to scale the Vanilla PoseStack so that standard Minecraft fonts/items render correctly inside XUI.
        double mcScale = mc.getWindow().getGuiScale();
        float textScaleAdjustment = (float) (this.currentScale / mcScale);

        // 5. Apply Scale to PoseStack
        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().scale(textScaleAdjustment, textScaleAdjustment, 1.0f);

        // 6. Sync internal stack
        // We assume the initial state is identity, but conceptually we are "pushing" into the frame.
        this.transformStack.push();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cleans up the frame by:
     * <ol>
     *     <li>Flushing the render buffers to ensure all geometry is drawn.</li>
     *     <li>Popping the Vanilla {@code PoseStack} to restore previous state.</li>
     *     <li>Popping the internal {@link TransformStack}.</li>
     *     <li>Nullifying the {@code guiGraphics} reference to prevent memory leaks or usage outside a frame.</li>
     * </ol>
     * </p>
     */
    @Override
    public void endFrame() {
        if (this.guiGraphics == null) return;

        // 1. Flush Render Buffers
        // This forces any batched text or geometry to be rendered immediately.
        // Crucial before changing global GL state or popping matrices.
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // 2. Pop Vanilla Stack
        this.guiGraphics.pose().popPose();

        // 3. Pop Internal Stack
        this.transformStack.pop();

        // 4. Cleanup
        this.guiGraphics = null;
    }

    // --- Accessors for Internal Components ---

    /**
     * Exposes the current {@link GuiGraphics} instance.
     * <p>
     * <b>Note:</b> This is primarily used by {@link net.xmx.xui.core.font.type.VanillaFont}
     * to access the standard Minecraft font renderer.
     * </p>
     *
     * @return The active GuiGraphics instance, or null if called outside a frame.
     */
    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    /**
     * Provides the current custom ModelView matrix for shaders.
     *
     * @return The 4x4 transformation matrix from the top of the stack.
     */
    public Matrix4f getCurrentMatrix() {
        return transformStack.getDirectModelMatrix();
    }

    /**
     * Returns the current logical scale factor.
     *
     * @return The scale factor.
     */
    public double getCurrentScale() {
        return currentScale;
    }

    // --- Helper Methods ---

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
        // Delegate to the High-Level Geometry API in UIRenderer.
        // We pass the current Custom Matrix so the shader knows where to draw.
        UIRenderer.getInstance().drawRect(
                alignX(x), alignY(y), alignDim(width), alignDim(height),
                color, radius, radius, radius, radius,
                currentScale, transformStack.getDirectModelMatrix()
        );
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawRect(
                alignX(x), alignY(y), alignDim(width), alignDim(height),
                color, rTL, rTR, rBR, rBL,
                currentScale, transformStack.getDirectModelMatrix()
        );
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        UIRenderer.getInstance().drawOutline(
                alignX(x), alignY(y), alignDim(width), alignDim(height),
                color, thickness, radius, radius, radius, radius,
                currentScale, transformStack.getDirectModelMatrix()
        );
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer.getInstance().drawOutline(
                alignX(x), alignY(y), alignDim(width), alignDim(height),
                color, thickness, rTL, rTR, rBR, rBL,
                currentScale, transformStack.getDirectModelMatrix()
        );
    }

    // --- Text Rendering ---

    @Override
    public void drawText(TextComponent text, float x, float y, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);

        // Delegate to the Font object.
        // The Font object will call back into RenderImpl (via 'this') if it needs access to GuiGraphics (VanillaFont)
        // or uses UIRenderer (CustomFont).
        text.getFont().draw(this, text, ax, ay, color, shadow);
    }

    @Override
    public void drawWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        float ax = alignX(x);
        float ay = alignY(y);
        float aw = alignDim(width);

        text.getFont().drawWrapped(this, text, ax, ay, aw, color, shadow);
    }

    // --- Scissor & Clipping ---

    @Override
    public void enableScissor(float x, float y, float width, float height) {
        // IMPORTANT: Flush the batch before changing GL state (Scissor).
        // Otherwise, geometry drawn *before* this call might be clipped if it hasn't been flushed yet.
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // Convert logical coordinates to physical coordinates for OpenGL
        int physX = (int) (alignX(x) * currentScale);
        int physY = (int) (alignY(y) * currentScale);
        int physW = (int) (alignDim(width) * currentScale);
        int physH = (int) (alignDim(height) * currentScale);

        // Delegate to ScissorManager to handle the stack
        UIRenderer.getInstance().getScissor().pushScissor(physX, physY, physW, physH, 1.0);
    }

    @Override
    public void disableScissor() {
        // Flush before restoring state
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // Restore previous scissor state from stack
        UIRenderer.getInstance().getScissor().popScissor(1.0);
    }

    @Override
    public float[] getCurrentScissor() {
        int[] phys = UIRenderer.getInstance().getScissor().getCurrentScissor();
        if (phys == null || currentScale == 0) return null;

        // Convert physical coordinates back to logical coordinates
        return new float[]{
                (float) (phys[0] / currentScale),
                (float) (phys[1] / currentScale),
                (float) (phys[2] / currentScale),
                (float) (phys[3] / currentScale)
        };
    }

    @Override
    public double getGuiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    // --- Matrix Implementation ---

    @Override
    public void pushMatrix() {
        // 1. Push Vanilla PoseStack (Affects Vanilla Fonts, Items)
        if (guiGraphics != null) guiGraphics.pose().pushPose();

        // 2. Push Custom TransformStack (Affects Custom Geometry/Fonts via Shaders)
        transformStack.push();
    }

    @Override
    public void popMatrix() {
        // 1. Pop Vanilla PoseStack
        if (guiGraphics != null) guiGraphics.pose().popPose();

        // 2. Pop Custom TransformStack
        transformStack.pop();
    }

    @Override
    public void translate(float x, float y, float z) {
        // Apply translation to both stacks to keep them synchronized
        if (guiGraphics != null) guiGraphics.pose().translate(x, y, z);
        transformStack.applyTranslation(x, y, z);
    }

    @Override
    public void rotate(float degrees, float x, float y, float z) {
        // Apply rotation to Vanilla PoseStack
        if (guiGraphics != null) {
            Vector3f axisVector = new Vector3f(x, y, z);
            if (axisVector.lengthSquared() > 0) axisVector.normalize();
            Axis axis = Axis.of(axisVector);
            guiGraphics.pose().mulPose(axis.rotationDegrees(degrees));
        }

        // Apply rotation to Custom TransformStack
        transformStack.applyRotation(degrees, x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        // Apply scaling to both stacks
        if (guiGraphics != null) guiGraphics.pose().scale(x, y, z);
        transformStack.applyScaling(x, y, z);
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.UIRenderInterface;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Concrete implementation of UIRenderInterface using direct OpenGL calls.
 * This implementation bypasses Minecraft's {@code RenderSystem} for geometry to ensure
 * a clean, programmable pipeline.
 *
 * <p>It utilizes a state tracking mechanism to save and restore Minecraft's Vertex Array
 * and Buffer bindings, preventing "Array object is not active" errors when control
 * is returned to the game engine.</p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderImpl implements UIRenderInterface {

    private final GuiGraphics guiGraphics;

    // Custom OpenGL pipeline components
    private final UIShader shader;
    private final UIMeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Stack to manage nested scissor operations
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Constructs the renderer.
     * Initializes the custom shader and mesh buffer.
     *
     * @param guiGraphics The Minecraft graphics accessor (used for text fallback).
     */
    public UIRenderImpl(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
        this.shader = UIShader.ofResource("xui", "core/ui_core");
        this.mesh = new UIMeshBuffer();
    }

    // --- Scissor Logic ---

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        scissorStack.push(new int[]{x, y, width, height});
        setRawScissor(x, y, width, height);
    }

    @Override
    public void disableScissor() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] prev = scissorStack.peek();
            if (prev != null) {
                setRawScissor(prev[0], prev[1], prev[2], prev[3]);
            }
        }
    }

    @Override
    public int[] getCurrentScissor() {
        return scissorStack.peek();
    }

    /**
     * Calculates and applies the scissor test using raw OpenGL calls.
     * Handles the conversion from logical GUI coordinates to physical window pixels,
     * accounting for the inverted Y-axis in OpenGL.
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     */
    private void setRawScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        long windowHandle = mc.getWindow().getWindow();

        // Retrieve physical framebuffer size directly from GLFW/LWJGL
        int[] wW = new int[1];
        int[] wH = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        double scale = mc.getWindow().getGuiScale();

        // Convert logical coordinates to physical pixels
        int sx = (int) (x * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        // Calculate Y. OpenGL origin is Bottom-Left. GUI origin is Top-Left.
        int sy = wH[0] - (int) ((y * scale) + sh);

        // Clamp values to valid ranges
        if (sx < 0) sx = 0;
        if (sy < 0) sy = 0;
        if (sw < 0) sw = 0;
        if (sh < 0) sh = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    // --- Render Logic ---

    /**
     * Configures the OpenGL state for custom UI rendering.
     * Sets up blending, disables depth testing, and uploads the projection matrix.
     */
    private void prepareRenderState() {
        // Orthographic projection setup
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        float width = viewport[2];
        float height = viewport[3];

        projectionMatrix.identity().ortho(0, width, height, 0, 1000, -1000);
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        projectionMatrix.scale(scale, scale, 1.0f);

        // Basic State
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
    }

    /**
     * Completes the rendering cycle for a geometry batch.
     * Unbinds the custom shader and restores the previous OpenGL state to ensure
     * Minecraft's renderer (e.g., for text) continues to function correctly.
     */
    private void finalizeRenderState() {
        shader.unbind();

        // Restore the critical VAO/VBO/EBO state that was active before we started
        GlStateTracker.restorePreviousState();

        // Re-enable states that Minecraft typically expects for GUI rendering (like text)
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        drawRect(x, y, width, height, color, radius, radius, radius, radius);
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        if (width <= 0 || height <= 0) return;

        // 1. Save current Minecraft State
        GlStateTracker.saveCurrentState();

        // 2. Setup Custom State
        prepareRenderState();

        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        // Ensure radii do not exceed dimensions
        float maxR = Math.min(width, height) / 2.0f;
        rTL = Math.min(rTL, maxR);
        rTR = Math.min(rTR, maxR);
        rBR = Math.min(rBR, maxR);
        rBL = Math.min(rBL, maxR);

        // Center Body
        addQuad(x + rTL, y, width - rTL - rTR, height, r, g, b, a);
        // Left Vertical
        addQuad(x, y + rTL, rTL, height - rTL - rBL, r, g, b, a);
        // Right Vertical
        addQuad(x + width - rTR, y + rTR, rTR, height - rTR - rBR, r, g, b, a);

        // Corners
        if (rTL > 0) addCorner(x + rTL, y + rTL, rTL, Math.PI, 1.5 * Math.PI, r, g, b, a);
        else addQuad(x, y, rTL, rTL, r, g, b, a);

        if (rTR > 0) addCorner(x + width - rTR, y + rTR, rTR, 1.5 * Math.PI, 2.0 * Math.PI, r, g, b, a);

        if (rBR > 0) addCorner(x + width - rBR, y + height - rBR, rBR, 0.0, 0.5 * Math.PI, r, g, b, a);

        if (rBL > 0) addCorner(x + rBL, y + height - rBL, rBL, 0.5 * Math.PI, Math.PI, r, g, b, a);

        // 3. Draw
        mesh.flush(GL11.GL_TRIANGLES);

        // 4. Restore Minecraft State
        finalizeRenderState();
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        drawOutline(x, y, width, height, color, thickness, radius, radius, radius, radius);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        if (thickness <= 0) return;

        // 1. Save current Minecraft State
        GlStateTracker.saveCurrentState();

        // 2. Setup Custom State
        prepareRenderState();

        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        // Draw 4 Lines
        addQuad(x + rTL, y, width - rTL - rTR, thickness, r, g, b, a);
        addQuad(x + rBL, y + height - thickness, width - rBL - rBR, thickness, r, g, b, a);
        addQuad(x, y + rTL, thickness, height - rTL - rBL, r, g, b, a);
        addQuad(x + width - thickness, y + rTR, thickness, height - rTR - rBR, r, g, b, a);

        // Draw Corners
        if (rTL > 0) addCornerRing(x + rTL, y + rTL, rTL, Math.max(0, rTL - thickness), Math.PI, 1.5 * Math.PI, r, g, b, a);
        else addQuad(x, y, thickness, thickness, r, g, b, a);

        if (rTR > 0) addCornerRing(x + width - rTR, y + rTR, rTR, Math.max(0, rTR - thickness), 1.5 * Math.PI, 2.0 * Math.PI, r, g, b, a);
        else addQuad(x + width - thickness, y, thickness, thickness, r, g, b, a);

        if (rBR > 0) addCornerRing(x + width - rBR, y + height - rBR, rBR, Math.max(0, rBR - thickness), 0.0, 0.5 * Math.PI, r, g, b, a);
        else addQuad(x + width - thickness, y + height - thickness, thickness, thickness, r, g, b, a);

        if (rBL > 0) addCornerRing(x + rBL, y + height - rBL, rBL, Math.max(0, rBL - thickness), 0.5 * Math.PI, Math.PI, r, g, b, a);
        else addQuad(x, y + height - thickness, thickness, thickness, r, g, b, a);

        // 3. Draw
        mesh.flush(GL11.GL_TRIANGLES);

        // 4. Restore Minecraft State
        finalizeRenderState();
    }

    /**
     * Adds a generic quad to the mesh buffer.
     */
    private void addQuad(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (w <= 0 || h <= 0) return;
        mesh.addVertex(x, y + h, 0, r, g, b, a);
        mesh.addVertex(x + w, y + h, 0, r, g, b, a);
        mesh.addVertex(x + w, y, 0, r, g, b, a);

        mesh.addVertex(x, y + h, 0, r, g, b, a);
        mesh.addVertex(x + w, y, 0, r, g, b, a);
        mesh.addVertex(x, y, 0, r, g, b, a);
    }

    /**
     * Adds a filled corner arc to the mesh buffer.
     */
    private void addCorner(float cx, float cy, float radius, double startAngle, double endAngle, float r, float g, float b, float a) {
        int segments = 8;
        double step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = startAngle + i * step;
            double a2 = startAngle + (i + 1) * step;

            float x1 = cx + (float) (Math.cos(a1) * radius);
            float y1 = cy + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius);
            float y2 = cy + (float) (Math.sin(a2) * radius);

            mesh.addVertex(cx, cy, 0, r, g, b, a);
            mesh.addVertex(x1, y1, 0, r, g, b, a);
            mesh.addVertex(x2, y2, 0, r, g, b, a);
        }
    }

    /**
     * Adds an outline corner ring to the mesh buffer.
     */
    private void addCornerRing(float cx, float cy, float rOut, float rIn, double startAngle, double endAngle, float r, float g, float b, float a) {
        int segments = 8;
        double step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = startAngle + i * step;
            double a2 = startAngle + (i + 1) * step;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            float ox1 = cx + cos1 * rOut;
            float oy1 = cy + sin1 * rOut;
            float ox2 = cx + cos2 * rOut;
            float oy2 = cy + sin2 * rOut;

            float ix1 = cx + cos1 * rIn;
            float iy1 = cy + sin1 * rIn;
            float ix2 = cx + cos2 * rIn;
            float iy2 = cy + sin2 * rIn;

            mesh.addVertex(ix1, iy1, 0, r, g, b, a);
            mesh.addVertex(ox1, oy1, 0, r, g, b, a);
            mesh.addVertex(ox2, oy2, 0, r, g, b, a);

            mesh.addVertex(ix1, iy1, 0, r, g, b, a);
            mesh.addVertex(ox2, oy2, 0, r, g, b, a);
            mesh.addVertex(ix2, iy2, 0, r, g, b, a);
        }
    }

    // --- Text Rendering Fallback ---

    @Override
    public void drawText(Component text, float x, float y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 0.1f);
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

    /**
     * A private utility class for safely managing and isolating critical OpenGL state.
     *
     * <p>This class provides a simple mechanism to save the current bindings for Vertex Array Objects (VAO),
     * Vertex Buffer Objects (VBO), and Element Buffer Objects (EBO) before custom rendering operations
     * and restore them afterward. This prevents state desynchronization with Minecraft's RenderSystem.</p>
     */
    private static class GlStateTracker {
        private static int previousVaoId = -1;
        private static int previousVboId = -1;
        private static int previousEboId = -1;

        /**
         * Queries and stores the currently bound VAO, VBO, and EBO.
         */
        public static void saveCurrentState() {
            previousVaoId = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            previousVboId = GL15.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            previousEboId = GL15.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        }

        /**
         * Restores the VAO, VBO, and EBO that were saved by the last call to {@link #saveCurrentState()}.
         */
        public static void restorePreviousState() {
            if (previousVaoId != -1) {
                GL30.glBindVertexArray(previousVaoId);
                previousVaoId = -1;
            }
            if (previousVboId != -1) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousVboId);
                previousVboId = -1;
            }
            if (previousEboId != -1) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousEboId);
                previousEboId = -1;
            }
        }
    }
}
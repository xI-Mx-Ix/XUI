/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The core rendering engine for XUI.
 * <p>
 * This class handles all raw OpenGL operations, including shader management,
 * mesh batching, coordinate systems, and scissor clipping. It is designed to be
 * the central point for geometry rendering, independent of Minecraft's high-level
 * {@code GuiGraphics} rendering calls.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    private final UIShader shader;
    private final UIMeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Stack to manage nested scissor operations
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Private constructor for Singleton.
     * Initializes the custom shader and mesh buffer.
     */
    private UIRenderer() {
        // Load shader from: /assets/xui/shaders/core/ui_core.[vsh/fsh]
        this.shader = UIShader.ofResource("xui", "core/ui_core");
        this.mesh = new UIMeshBuffer();
    }

    /**
     * Retrieves the global instance of the renderer.
     *
     * @return The UIRenderer instance.
     */
    public static UIRenderer getInstance() {
        if (instance == null) {
            instance = new UIRenderer();
        }
        return instance;
    }

    // --- Scissor Logic ---

    /**
     * Enables a scissor test to clip rendering to a specific screen region.
     * Pushes the new scissor rectangle onto the internal stack.
     *
     * @param x      The logical x-coordinate of the clipping area.
     * @param y      The logical y-coordinate of the clipping area.
     * @param width  The logical width of the clipping area.
     * @param height The logical height of the clipping area.
     */
    public void enableScissor(int x, int y, int width, int height) {
        scissorStack.push(new int[]{x, y, width, height});
        setRawScissor(x, y, width, height);
    }

    /**
     * Disables the active scissor test or restores the previous state from the stack.
     * If the stack is empty, scissoring is completely disabled in OpenGL.
     */
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

    /**
     * Retrieves the current scissor rectangle active in the renderer.
     *
     * @return An integer array {@code [x, y, width, height]} representing the current scissor window,
     *         or {@code null} if no scissor test is currently enabled.
     */
    public int[] getCurrentScissor() {
        return scissorStack.peek();
    }

    /**
     * Calculates and applies the scissor test using raw OpenGL calls.
     * Converts logical GUI coordinates to physical window pixels and adjusts for the OpenGL Y-axis.
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     */
    private void setRawScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        long windowHandle = mc.getWindow().getWindow();

        int[] wW = new int[1];
        int[] wH = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        double scale = mc.getWindow().getGuiScale();

        int sx = (int) (x * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        // OpenGL Origin is Bottom-Left, GUI Origin is Top-Left
        int sy = wH[0] - (int) ((y * scale) + sh);

        if (sx < 0) sx = 0;
        if (sy < 0) sy = 0;
        if (sw < 0) sw = 0;
        if (sh < 0) sh = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    // --- Drawing Logic ---

    /**
     * Draws a filled rectangle with independent corner radii.
     *
     * @param x      The x-coordinate.
     * @param y      The y-coordinate.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color  The ARGB color value.
     * @param rTL    Top-Left corner radius.
     * @param rTR    Top-Right corner radius.
     * @param rBR    Bottom-Right corner radius.
     * @param rBL    Bottom-Left corner radius.
     */
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        if (width <= 0 || height <= 0) return;

        GlStateTracker.saveCurrentState();
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

        mesh.flush(GL11.GL_TRIANGLES);
        finalizeRenderState();
    }

    /**
     * Draws a hollow outline with independent corner radii.
     *
     * @param x         The x-coordinate.
     * @param y         The y-coordinate.
     * @param width     The width of the outline.
     * @param height    The height of the outline.
     * @param color     The ARGB color value.
     * @param thickness The thickness of the line.
     * @param rTL       Top-Left corner radius.
     * @param rTR       Top-Right corner radius.
     * @param rBR       Bottom-Right corner radius.
     * @param rBL       Bottom-Left corner radius.
     */
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        if (thickness <= 0) return;

        GlStateTracker.saveCurrentState();
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

        mesh.flush(GL11.GL_TRIANGLES);
        finalizeRenderState();
    }

    // --- Internals ---

    /**
     * Configures the OpenGL state for custom UI rendering.
     * Sets up blending, disables depth testing, and uploads the projection matrix.
     */
    private void prepareRenderState() {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        float width = viewport[2];
        float height = viewport[3];

        projectionMatrix.identity().ortho(0, width, height, 0, 1000, -1000);
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        projectionMatrix.scale(scale, scale, 1.0f);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
    }

    /**
     * Restores critical OpenGL state to ensure compatibility with Minecraft's renderer.
     * Specifically restores the VAO/VBO bindings to their previous state.
     */
    private void finalizeRenderState() {
        shader.unbind();
        GlStateTracker.restorePreviousState();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Adds a generic quad to the mesh buffer.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param w The width.
     * @param h The height.
     * @param r Red component (0-1).
     * @param g Green component (0-1).
     * @param b Blue component (0-1).
     * @param a Alpha component (0-1).
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
     * Adds a filled corner arc (fan) to the mesh buffer.
     *
     * @param cx         The center x-coordinate.
     * @param cy         The center y-coordinate.
     * @param radius     The radius of the corner.
     * @param startAngle The starting angle in radians.
     * @param endAngle   The ending angle in radians.
     * @param r          Red component.
     * @param g          Green component.
     * @param b          Blue component.
     * @param a          Alpha component.
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
     * Adds an outline corner ring segment to the mesh buffer.
     *
     * @param cx         The center x-coordinate.
     * @param cy         The center y-coordinate.
     * @param rOut       The outer radius.
     * @param rIn        The inner radius.
     * @param startAngle The starting angle in radians.
     * @param endAngle   The ending angle in radians.
     * @param r          Red component.
     * @param g          Green component.
     * @param b          Blue component.
     * @param a          Alpha component.
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

    /**
     * A utility class for safely capturing and restoring critical OpenGL state.
     * This prevents state desynchronization with Minecraft's internal RenderSystem.
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
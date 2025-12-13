/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.shader.impl.UICoreShader;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Handles the generation and batching of geometric shapes (rectangles, rounded corners, outlines).
 * <p>
 * This renderer uses the generic {@link MeshBuffer} with the {@link VertexFormat#POS_COLOR} format.
 * It translates high-level drawing commands (like {@code drawRect}) into raw vertices
 * suitable for the {@link UICoreShader}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class GeometryRenderer {

    private final UICoreShader shader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructs a new geometry renderer.
     * Initializes the core shader and a mesh buffer optimized for position and color data.
     */
    public GeometryRenderer() {
        this.shader = new UICoreShader();
        // Initialize the generic mesh buffer with Position + Color format
        this.mesh = new MeshBuffer(VertexFormat.POS_COLOR);
    }

    /**
     * Prepares the OpenGL state for geometry rendering.
     * <p>
     * This method binds the shader, calculates the orthographic projection matrix based on
     * the current viewport and GUI scale, and uploads it to the GPU.
     * It also uploads the current ModelView matrix to handle custom widget transformations.
     * </p>
     *
     * @param guiScale        The current GUI scale factor (logical to physical pixels).
     * @param modelViewMatrix The current transformation matrix for widgets.
     */
    public void begin(double guiScale, Matrix4f modelViewMatrix) {
        // Retrieve current viewport dimensions to setup projection
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        // Setup Orthographic Projection: (0,0) at top-left
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, 1000, -1000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadModelView(modelViewMatrix);
    }

    /**
     * Finalizes the rendering pass.
     * <p>
     * Flushes any remaining vertices in the mesh buffer to the GPU and unbinds the shader.
     * </p>
     */
    public void end() {
        mesh.flush(GL11.GL_TRIANGLES);
        shader.unbind();
    }

    // --- Drawing Operations ---

    /**
     * Queues a filled rectangle with optional rounded corners.
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     * @param color  ARGB Color.
     * @param rTL    Top-Left radius.
     * @param rTR    Top-Right radius.
     * @param rBR    Bottom-Right radius.
     * @param rBL    Bottom-Left radius.
     */
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        if (width <= 0 || height <= 0) return;

        float[] rgba = unpackColor(color);
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        // Clamp radii to half the shortest side to prevent artifacts
        float maxR = Math.min(width, height) / 2.0f;
        rTL = Math.min(rTL, maxR);
        rTR = Math.min(rTR, maxR);
        rBR = Math.min(rBR, maxR);
        rBL = Math.min(rBL, maxR);

        // 1. Draw Center Body (Cross shape logic)
        addQuad(x + rTL, y, width - rTL - rTR, height, r, g, b, a);

        // 2. Draw Left and Right sides (between corners)
        addQuad(x, y + rTL, rTL, height - rTL - rBL, r, g, b, a);
        addQuad(x + width - rTR, y + rTR, rTR, height - rTR - rBR, r, g, b, a);

        // 3. Draw Corners (Arcs) or fill sharp corners
        if (rTL > 0) addCorner(x + rTL, y + rTL, rTL, Math.PI, 1.5 * Math.PI, r, g, b, a);
        else addQuad(x, y, rTL, rTL, r, g, b, a); // Fill gap if radius is 0

        if (rTR > 0) addCorner(x + width - rTR, y + rTR, rTR, 1.5 * Math.PI, 2.0 * Math.PI, r, g, b, a);

        if (rBR > 0) addCorner(x + width - rBR, y + height - rBR, rBR, 0.0, 0.5 * Math.PI, r, g, b, a);

        if (rBL > 0) addCorner(x + rBL, y + height - rBL, rBL, 0.5 * Math.PI, Math.PI, r, g, b, a);
    }

    /**
     * Queues a hollow outline with optional rounded corners.
     *
     * @param x         Logical X position.
     * @param y         Logical Y position.
     * @param width     Logical Width.
     * @param height    Logical Height.
     * @param color     ARGB Color.
     * @param thickness Line thickness.
     * @param rTL       Top-Left radius.
     * @param rTR       Top-Right radius.
     * @param rBR       Bottom-Right radius.
     * @param rBL       Bottom-Left radius.
     */
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        if (thickness <= 0) return;

        float[] rgba = unpackColor(color);
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        // Draw 4 Linear Edges
        addQuad(x + rTL, y, width - rTL - rTR, thickness, r, g, b, a); // Top
        addQuad(x + rBL, y + height - thickness, width - rBL - rBR, thickness, r, g, b, a); // Bottom
        addQuad(x, y + rTL, thickness, height - rTL - rBL, r, g, b, a); // Left
        addQuad(x + width - thickness, y + rTR, thickness, height - rTR - rBR, r, g, b, a); // Right

        // Draw 4 Corner Rings (or standard quads if sharp)
        drawCornerRingOrQuad(x + rTL, y + rTL, rTL, thickness, Math.PI, 1.5 * Math.PI, r, g, b, a, x, y);
        drawCornerRingOrQuad(x + width - rTR, y + rTR, rTR, thickness, 1.5 * Math.PI, 2.0 * Math.PI, r, g, b, a, x + width - thickness, y);
        drawCornerRingOrQuad(x + width - rBR, y + height - rBR, rBR, thickness, 0.0, 0.5 * Math.PI, r, g, b, a, x + width - thickness, y + height - thickness);
        drawCornerRingOrQuad(x + rBL, y + height - rBL, rBL, thickness, 0.5 * Math.PI, Math.PI, r, g, b, a, x, y + height - thickness);
    }

    // --- Helpers ---

    private void drawCornerRingOrQuad(float cx, float cy, float radius, float th, double start, double end, float r, float g, float b, float a, float qx, float qy) {
        if (radius > 0) {
            addCornerRing(cx, cy, radius, Math.max(0, radius - th), start, end, r, g, b, a);
        } else {
            addQuad(qx, qy, th, th, r, g, b, a);
        }
    }

    /**
     * Adds a rectangle (composed of 2 triangles) to the generic mesh buffer.
     */
    private void addQuad(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (w <= 0 || h <= 0) return;

        // Triangle 1
        mesh.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        mesh.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        mesh.pos(x + w, y, 0).color(r, g, b, a).endVertex();

        // Triangle 2
        mesh.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        mesh.pos(x + w, y, 0).color(r, g, b, a).endVertex();
        mesh.pos(x, y, 0).color(r, g, b, a).endVertex();
    }

    /**
     * Adds a filled arc (fan) to the mesh buffer.
     */
    private void addCorner(float cx, float cy, float radius, double startAngle, double endAngle, float r, float g, float b, float a) {
        int segments = 8; // LOD for corners
        double step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = startAngle + i * step;
            double a2 = startAngle + (i + 1) * step;

            // Triangle center -> arc point 1 -> arc point 2
            mesh.pos(cx, cy, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + (float)(Math.cos(a1) * radius), cy + (float)(Math.sin(a1) * radius), 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + (float)(Math.cos(a2) * radius), cy + (float)(Math.sin(a2) * radius), 0).color(r, g, b, a).endVertex();
        }
    }

    /**
     * Adds a thick arc (ring segment) to the mesh buffer.
     */
    private void addCornerRing(float cx, float cy, float rOut, float rIn, double start, double end, float r, float g, float b, float a) {
        int segments = 8;
        double step = (end - start) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = start + i * step;
            double a2 = start + (i + 1) * step;

            float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
            float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

            // Triangle 1 (Inner1 -> Outer1 -> Outer2)
            mesh.pos(cx + c1 * rIn, cy + s1 * rIn, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + c1 * rOut, cy + s1 * rOut, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + c2 * rOut, cy + s2 * rOut, 0).color(r, g, b, a).endVertex();

            // Triangle 2 (Inner1 -> Outer2 -> Inner2)
            mesh.pos(cx + c1 * rIn, cy + s1 * rIn, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + c2 * rOut, cy + s2 * rOut, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + c2 * rIn, cy + s2 * rIn, 0).color(r, g, b, a).endVertex();
        }
    }

    /**
     * Helper to extract RGBA floats (0.0 - 1.0) from an ARGB integer.
     */
    private float[] unpackColor(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                ((color >> 24) & 0xFF) / 255.0F
        };
    }
}
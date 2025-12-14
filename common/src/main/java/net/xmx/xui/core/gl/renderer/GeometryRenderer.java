/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.shader.impl.PositionColorShader;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Handles the generation, batching, and rendering of geometric shapes (rectangles, rounded corners, outlines).
 * <p>
 * This class serves as the low-level geometry engine for the UI. It translates high-level shape
 * descriptions into raw vertices that are uploaded to the GPU. It supports two modes of operation:
 * <ul>
 *     <li><b>Immediate Mode:</b> Using methods like {@link #renderRect} which handle the entire
 *     render lifecycle (state capture, shader bind, upload, draw, state restore) in one call.</li>
 *     <li><b>Batched Mode:</b> Using {@link #begin}, {@link #drawRect} (to queue vertices),
 *     and {@link #end} to minimize draw calls for complex scenes.</li>
 * </ul>
 * </p>
 * <p>
 * It utilizes the {@link PositionColorShader} for rendering and a {@link MeshBuffer} for vertex management.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class GeometryRenderer {

    private final PositionColorShader shader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructs a new GeometryRenderer.
     * <p>
     * Initializes the specific shader used for UI geometry and creates a reusable mesh buffer
     * configured with the {@link VertexFormat#POS_COLOR} format (Position 3D + Color 4D).
     * </p>
     */
    public GeometryRenderer() {
        this.shader = new PositionColorShader();
        this.mesh = new MeshBuffer(VertexFormat.POS_COLOR);
    }

    // --- Lifecycle (Batching) ---

    /**
     * Prepares the OpenGL state and shader for a new batch of geometry rendering.
     * <p>
     * This method must be called before queuing any vertices via the {@code draw*} methods.
     * It performs the following setup:
     * <ol>
     *     <li>Calculates an orthographic projection matrix based on the current viewport and GUI scale.</li>
     *     <li>Binds the UI core shader.</li>
     *     <li>Uploads the projection and model-view matrices to the shader uniforms.</li>
     * </ol>
     * </p>
     *
     * @param guiScale        The current logical-to-physical UI scale factor.
     * @param modelViewMatrix The current transformation matrix from the {@link net.xmx.xui.core.gl.TransformStack}.
     */
    public void begin(double guiScale, Matrix4f modelViewMatrix) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        // Setup Orthographic Projection:
        // (0,0) is at top-left, X extends right, Y extends down.
        // Z-range is large (-10000 to 10000) to support UI layering.
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadModelView(modelViewMatrix);
    }

    /**
     * Finalizes the current batch of geometry rendering.
     * <p>
     * This method flushes the accumulated vertices in the mesh buffer to the GPU using
     * the {@code GL_TRIANGLES} primitive mode and subsequently unbinds the shader.
     * It must be called after all {@code draw*} commands are issued.
     * </p>
     */
    public void end() {
        mesh.flush(GL11.GL_TRIANGLES);
        shader.unbind();
    }

    // --- Standalone Rendering (Immediate Mode) ---

    /**
     * Renders a filled rectangle with uniform rounded corners immediately.
     * <p>
     * This is a convenience method for "Immediate Mode" rendering. It automatically handles:
     * <ol>
     *     <li>Capturing the current OpenGL state via {@link GlState}.</li>
     *     <li>Setting up the UI-specific GL capabilities.</li>
     *     <li>Calling {@link #begin} using the global scale and transform stack from {@link UIRenderer}.</li>
     *     <li>Queuing the rectangle geometry.</li>
     *     <li>Calling {@link #end} to draw.</li>
     *     <li>Restoring the captured OpenGL state.</li>
     * </ol>
     * </p>
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     * @param color  ARGB Color.
     * @param radius Uniform corner radius.
     */
    public void renderRect(float x, float y, float width, float height, int color, float radius) {
        renderRect(x, y, width, height, color, radius, radius, radius, radius);
    }

    /**
     * Renders a filled rectangle with complex (individual) rounded corners immediately.
     * <p>
     * Like {@link #renderRect(float, float, float, float, int, float)}, this method manages the
     * full rendering lifecycle automatically. It delegates to {@link #drawRect} for the vertex generation
     * but handles all the GL state management internally.
     * </p>
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     * @param color  ARGB Color.
     * @param rTL    Radius of Top-Left corner.
     * @param rTR    Radius of Top-Right corner.
     * @param rBR    Radius of Bottom-Right corner.
     * @param rBL    Radius of Bottom-Left corner.
     */
    public void renderRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer renderer = UIRenderer.getInstance();
        GlState state = renderer.getStateManager();

        // 1. Save external state (e.g., Game Engine state)
        state.capture();
        state.setupForUI();

        // 2. Begin batch with current global context
        begin(renderer.getCurrentUiScale(), renderer.getTransformStack().getDirectModelMatrix());

        // 3. Queue vertices
        drawRect(x, y, width, height, color, rTL, rTR, rBR, rBL);

        // 4. Draw and unbind shader
        end();

        // 5. Restore external state
        state.restore();
    }

    /**
     * Renders a hollow outline with uniform rounded corners immediately.
     * <p>
     * This is a convenience method that manages the full render lifecycle (State capture -> Draw -> Restore).
     * </p>
     *
     * @param x         Logical X position.
     * @param y         Logical Y position.
     * @param width     Logical Width.
     * @param height    Logical Height.
     * @param color     ARGB Color.
     * @param radius    Uniform corner radius.
     * @param thickness Thickness of the outline in logical pixels.
     */
    public void renderOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        renderOutline(x, y, width, height, color, thickness, radius, radius, radius, radius);
    }

    /**
     * Renders a hollow outline with complex (individual) rounded corners immediately.
     * <p>
     * Like {@link #renderOutline(float, float, float, float, int, float, float)}, this method manages the
     * full rendering lifecycle automatically. It delegates to {@link #drawOutline} for the vertex generation.
     * </p>
     *
     * @param x         Logical X position.
     * @param y         Logical Y position.
     * @param width     Logical Width.
     * @param height    Logical Height.
     * @param color     ARGB Color.
     * @param thickness Thickness of the outline.
     * @param rTL       Radius of Top-Left corner.
     * @param rTR       Radius of Top-Right corner.
     * @param rBR       Radius of Bottom-Right corner.
     * @param rBL       Radius of Bottom-Left corner.
     */
    public void renderOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        UIRenderer renderer = UIRenderer.getInstance();
        GlState state = renderer.getStateManager();

        state.capture();
        state.setupForUI();

        begin(renderer.getCurrentUiScale(), renderer.getTransformStack().getDirectModelMatrix());
        drawOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
        end();

        state.restore();
    }

    // --- Vertex Generation (Internal/Batched) ---

    /**
     * Queues vertices for a filled rectangle with optional rounded corners into the active mesh buffer.
     * <p>
     * <b>Note:</b> This method does NOT invoke draw calls or bind shaders. It must be called
     * strictly between {@link #begin} and {@link #end}.
     * </p>
     *
     * @param x      Logical X position.
     * @param y      Logical Y position.
     * @param width  Logical Width.
     * @param height Logical Height.
     * @param color  ARGB Color.
     * @param rTL    Radius of Top-Left corner.
     * @param rTR    Radius of Top-Right corner.
     * @param rBR    Radius of Bottom-Right corner.
     * @param rBL    Radius of Bottom-Left corner.
     */
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        if (width <= 0 || height <= 0) return;

        float[] rgba = unpackColor(color);
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        // Clamp radii to prevent visual artifacts if radii sum > dimension
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
     * Queues vertices for a hollow outline into the active mesh buffer.
     * <p>
     * <b>Note:</b> This method does NOT invoke draw calls or bind shaders. It must be called
     * strictly between {@link #begin} and {@link #end}.
     * </p>
     *
     * @param x         Logical X position.
     * @param y         Logical Y position.
     * @param width     Logical Width.
     * @param height    Logical Height.
     * @param color     ARGB Color.
     * @param thickness Thickness of the outline.
     * @param rTL       Radius of Top-Left corner.
     * @param rTR       Radius of Top-Right corner.
     * @param rBR       Radius of Bottom-Right corner.
     * @param rBL       Radius of Bottom-Left corner.
     */
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        if (thickness <= 0) return;

        float[] rgba = unpackColor(color);
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        // Draw 4 Linear Edges using quads
        addQuad(x + rTL, y, width - rTL - rTR, thickness, r, g, b, a); // Top
        addQuad(x + rBL, y + height - thickness, width - rBL - rBR, thickness, r, g, b, a); // Bottom
        addQuad(x, y + rTL, thickness, height - rTL - rBL, r, g, b, a); // Left
        addQuad(x + width - thickness, y + rTR, thickness, height - rTR - rBR, r, g, b, a); // Right

        // Draw 4 Corner Rings (arcs with inner and outer radii) or standard quads if sharp
        drawCornerRingOrQuad(x + rTL, y + rTL, rTL, thickness, Math.PI, 1.5 * Math.PI, r, g, b, a, x, y);
        drawCornerRingOrQuad(x + width - rTR, y + rTR, rTR, thickness, 1.5 * Math.PI, 2.0 * Math.PI, r, g, b, a, x + width - thickness, y);
        drawCornerRingOrQuad(x + width - rBR, y + height - rBR, rBR, thickness, 0.0, 0.5 * Math.PI, r, g, b, a, x + width - thickness, y + height - thickness);
        drawCornerRingOrQuad(x + rBL, y + height - rBL, rBL, thickness, 0.5 * Math.PI, Math.PI, r, g, b, a, x, y + height - thickness);
    }

    // --- Private Helpers ---

    /**
     * Helper to draw either a rounded corner ring or a simple quad filler if the radius is zero.
     *
     * @param cx      Center X of the arc.
     * @param cy      Center Y of the arc.
     * @param radius  Outer radius.
     * @param th      Thickness of the line.
     * @param start   Start angle in radians.
     * @param end     End angle in radians.
     * @param r       Red component.
     * @param g       Green component.
     * @param b       Blue component.
     * @param a       Alpha component.
     * @param qx      Fallback Quad X position (for sharp corners).
     * @param qy      Fallback Quad Y position (for sharp corners).
     */
    private void drawCornerRingOrQuad(float cx, float cy, float radius, float th, double start, double end, float r, float g, float b, float a, float qx, float qy) {
        if (radius > 0) {
            // Draw a thick arc
            addCornerRing(cx, cy, radius, Math.max(0, radius - th), start, end, r, g, b, a);
        } else {
            // Draw a simple square to connect edges
            addQuad(qx, qy, th, th, r, g, b, a);
        }
    }

    /**
     * Adds a rectangle (composed of 2 triangles) to the generic mesh buffer.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param w Width.
     * @param h Height.
     * @param r Red component.
     * @param g Green component.
     * @param b Blue component.
     * @param a Alpha component.
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
     * Adds a solid filled arc (fan) to the mesh buffer.
     * Used for filled rounded corners.
     *
     * @param cx         Center X.
     * @param cy         Center Y.
     * @param radius     Radius of the arc.
     * @param startAngle Start angle in radians.
     * @param endAngle   End angle in radians.
     * @param r          Red component.
     * @param g          Green component.
     * @param b          Blue component.
     * @param a          Alpha component.
     */
    private void addCorner(float cx, float cy, float radius, double startAngle, double endAngle, float r, float g, float b, float a) {
        int segments = 8; // Level of Detail for smoothness
        double step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = startAngle + i * step;
            double a2 = startAngle + (i + 1) * step;

            // Form a triangle from Center -> Point 1 -> Point 2
            mesh.pos(cx, cy, 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + (float)(Math.cos(a1) * radius), cy + (float)(Math.sin(a1) * radius), 0).color(r, g, b, a).endVertex();
            mesh.pos(cx + (float)(Math.cos(a2) * radius), cy + (float)(Math.sin(a2) * radius), 0).color(r, g, b, a).endVertex();
        }
    }

    /**
     * Adds a thick arc (ring segment) to the mesh buffer.
     * Used for outline rounded corners.
     *
     * @param cx    Center X.
     * @param cy    Center Y.
     * @param rOut  Outer Radius.
     * @param rIn   Inner Radius.
     * @param start Start angle in radians.
     * @param end   End angle in radians.
     * @param r     Red component.
     * @param g     Green component.
     * @param b     Blue component.
     * @param a     Alpha component.
     */
    private void addCornerRing(float cx, float cy, float rOut, float rIn, double start, double end, float r, float g, float b, float a) {
        int segments = 8;
        double step = (end - start) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = start + i * step;
            double a2 = start + (i + 1) * step;

            float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
            float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

            // Create two triangles (a quad) between the inner and outer radius for this segment

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
     * Utility method to extract normalized RGBA float values from a packed ARGB integer.
     *
     * @param color The packed ARGB color integer (e.g., 0xFFFFFFFF).
     * @return A float array of size 4 containing [Red, Green, Blue, Alpha] in range 0.0 - 1.0.
     */
    private float[] unpackColor(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F, // Red
                ((color >> 8) & 0xFF) / 255.0F,  // Green
                (color & 0xFF) / 255.0F,         // Blue
                ((color >> 24) & 0xFF) / 255.0F  // Alpha
        };
    }
}
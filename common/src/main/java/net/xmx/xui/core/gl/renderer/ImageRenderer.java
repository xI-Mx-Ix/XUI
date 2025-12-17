/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.shader.impl.TexturedRectShader;
import net.xmx.xui.core.gl.texture.UITexture;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Handles the rendering of textured rectangles (Images).
 * Uses a specific shader that supports rounding via Fragment Shader math.
 *
 * @author xI-Mx-Ix
 */
public class ImageRenderer {

    private final TexturedRectShader shader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    public ImageRenderer() {
        this.shader = new TexturedRectShader();
        this.mesh = new MeshBuffer(VertexFormat.POS_COLOR_UV);
    }

    /**
     * Renders a texture with specified dimensions and rounded corners.
     * <p>
     * Note: This method breaks batching because it changes texture bindings and
     * uploads specific uniforms (size/radius) per draw call.
     * </p>
     * <p>
     * <b>State Management:</b> This method now explicitly captures and restores the OpenGL
     * state using {@link GlState}. This is critical to prevent {@code GL_INVALID_OPERATION}
     * errors (ID 1282) related to active Vertex Array Objects (VAO) when interoping with Minecraft.
     * </p>
     *
     * @param texture      The texture object to render.
     * @param x            The absolute X coordinate.
     * @param y            The absolute Y coordinate.
     * @param w            The width in pixels.
     * @param h            The height in pixels.
     * @param color        The tint color (ARGB).
     * @param radius       The corner radius in pixels.
     * @param pixelPerfect If true, uses nearest-neighbor filtering. If false, uses linear.
     * @param uiScale      The current global UI scale.
     * @param modelView    The current model-view matrix.
     */
    public void drawImage(UITexture texture, float x, float y, float w, float h, int color, float radius, boolean pixelPerfect, double uiScale, Matrix4f modelView) {
        if (texture == null || w <= 0 || h <= 0) return;

        UIRenderer renderer = UIRenderer.getInstance();
        GlState glState = renderer.getStateManager();

        // 1. Capture current OpenGL State (VAO, VBO, Blend, Depth, etc.)
        glState.capture();

        // 2. Setup safe UI State (Enables Blending, disables Culling, etc.)
        glState.setupForUI();

        try {
            // 3. Setup Projection
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
            projectionMatrix.scale((float) uiScale, (float) uiScale, 1.0f);

            // 4. Bind Shader and Upload Uniforms
            shader.bind();
            shader.uploadProjection(projectionMatrix);
            shader.uploadModelView(modelView);
            shader.uploadSize(w, h);
            shader.uploadRadius(radius);
            shader.uploadTextureUnit(0);

            // 5. Bind Texture & Configure Filtering
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            texture.bind();

            // Apply dynamic filtering based on widget preference
            int filter = pixelPerfect ? GL11.GL_NEAREST : GL11.GL_LINEAR;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

            // 6. Build Quad
            float[] rgba = unpackColor(color);
            float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

            // We use a single quad. The fragment shader handles the rounded clipping.
            // Vertices: Pos(x,y,z), Color(r,g,b,a), UV(u,v)

            // Top-Left
            mesh.pos(x, y, 0).color(r, g, b, a).uv(0, 0).endVertex();
            // Bottom-Left
            mesh.pos(x, y + h, 0).color(r, g, b, a).uv(0, 1).endVertex();
            // Bottom-Right
            mesh.pos(x + w, y + h, 0).color(r, g, b, a).uv(1, 1).endVertex();

            // Top-Left
            mesh.pos(x, y, 0).color(r, g, b, a).uv(0, 0).endVertex();
            // Bottom-Right
            mesh.pos(x + w, y + h, 0).color(r, g, b, a).uv(1, 1).endVertex();
            // Top-Right
            mesh.pos(x + w, y, 0).color(r, g, b, a).uv(1, 0).endVertex();

            // 7. Draw
            // The mesh buffer handles its own VAO binding, but because we are inside a captured state,
            // restoring afterwards ensures we don't leak the VAO binding to Minecraft.
            mesh.flush(GL11.GL_TRIANGLES);

        } finally {
            shader.unbind();
            // 8. Restore previous OpenGL State (Critical for preventing GL_INVALID_OPERATION)
            glState.restore();
        }
    }

    private float[] unpackColor(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                ((color >> 24) & 0xFF) / 255.0F
        };
    }
}
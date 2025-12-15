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
     *
     * @param texture   The texture object to render.
     * @param x         The absolute X coordinate.
     * @param y         The absolute Y coordinate.
     * @param w         The width in pixels.
     * @param h         The height in pixels.
     * @param color     The tint color (ARGB).
     * @param radius    The corner radius in pixels.
     * @param uiScale   The current global UI scale.
     * @param modelView The current model-view matrix.
     */
    public void drawImage(UITexture texture, float x, float y, float w, float h, int color, float radius, double uiScale, Matrix4f modelView) {
        if (texture == null || w <= 0 || h <= 0) return;

        // 1. Setup Projection
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
        projectionMatrix.scale((float) uiScale, (float) uiScale, 1.0f);

        // 2. Bind Shader and Upload Uniforms
        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadModelView(modelView);
        shader.uploadSize(w, h);
        shader.uploadRadius(radius);
        shader.uploadTextureUnit(0);

        // 3. Bind Texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        texture.bind();

        // 4. Build Quad
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

        // 5. Draw
        mesh.flush(GL11.GL_TRIANGLES);
        shader.unbind();
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
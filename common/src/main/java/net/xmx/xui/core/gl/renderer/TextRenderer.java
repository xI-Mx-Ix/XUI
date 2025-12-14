/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.font.data.MSDFData;
import net.xmx.xui.core.gl.shader.impl.MSDFShader;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Handles the rendering lifecycle for text elements using the MSDF shader.
 * Acts as the bridge between high-level draw calls and OpenGL draw commands.
 *
 * @author xI-Mx-Ix
 */
public class TextRenderer {

    private final MSDFShader shader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructs a new text renderer with an MSDF shader and a standard mesh buffer.
     */
    public TextRenderer() {
        this.shader = new MSDFShader();
        // Uses the generic Position + Color + UV vertex format
        this.mesh = new MeshBuffer(VertexFormat.POS_COLOR_UV);
    }

    /**
     * Provides access to the mesh buffer for pushing vertex data.
     *
     * @return The active mesh buffer.
     */
    public MeshBuffer getMesh() {
        return mesh;
    }

    /**
     * Initializes the rendering state for a batch of text.
     * Sets up the projection matrix and configures shader uniforms based on the font atlas
     * and the current model-view transformation.
     *
     * @param guiScale        The current GUI scale factor.
     * @param atlasInfo       The metadata of the font atlas currently being rendered.
     * @param modelViewMatrix The current transformation matrix.
     */
    public void begin(double guiScale, MSDFData.AtlasInfo atlasInfo, Matrix4f modelViewMatrix) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        // Setup Orthographic Projection (0,0 at top-left)
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadModelView(modelViewMatrix);
        shader.uploadTextureUnit(0);

        if (atlasInfo != null) {
            // Pass the distance range from the JSON to the shader
            shader.uploadPxRange(atlasInfo.distanceRange);
        }
    }

    /**
     * Binds the specific font texture and draws the buffered text mesh.
     * <p>
     * This method binds the provided texture ID to GL_TEXTURE0 and then
     * flushes the mesh buffer content as GL_TRIANGLES.
     * </p>
     *
     * @param textureId The OpenGL texture ID of the font atlas to use.
     */
    public void drawBatch(int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Execute draw call and clear buffer for next batch
        mesh.flush(GL11.GL_TRIANGLES);
    }

    /**
     * Finalizes the rendering pass and unbinds the shader.
     */
    public void end() {
        shader.unbind();
    }
}
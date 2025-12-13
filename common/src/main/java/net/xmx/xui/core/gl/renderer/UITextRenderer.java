/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.font.UIFont;
import net.xmx.xui.core.gl.shader.impl.UITextShader;
import net.xmx.xui.core.gl.vertex.UIMeshBuffer;
import net.xmx.xui.core.gl.vertex.UIVertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Handles the rendering pipeline for text elements.
 * <p>
 * This class manages the {@link UITextShader} and the generic {@link UIMeshBuffer}.
 * It is responsible for setting up the text shader state (projection, texture units)
 * and executing the draw calls for font batches.
 * </p>
 * <p>
 * Note: Actual glyph geometry generation is handled by the {@link UIFont} class,
 * which populates the mesh buffer exposed by {@link #getMesh()}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UITextRenderer {

    private final UITextShader shader;
    private final UIMeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructs a new text renderer.
     * Initializes the text shader and the mesh buffer optimized for Position + Color + UV.
     */
    public UITextRenderer() {
        this.shader = new UITextShader();
        this.mesh = new UIMeshBuffer(UIVertexFormat.POS_COLOR_UV);
    }

    /**
     * Provides access to the underlying mesh buffer.
     * Used by {@link UIFont} to append glyph vertices.
     *
     * @return The active text mesh buffer.
     */
    public UIMeshBuffer getMesh() {
        return mesh;
    }

    /**
     * Prepares the OpenGL state for text rendering.
     * <p>
     * Binds the text shader, calculates the orthographic projection, and ensures
     * the texture sampler uniform is set to unit 0.
     * </p>
     *
     * @param guiScale The current GUI scale factor.
     */
    public void begin(double guiScale) {
        // Retrieve viewport to calculate projection
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        // Setup Orthographic Projection
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, 1000, -1000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadTextureUnit(0); // Ensure shader samples from Texture Unit 0
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
     * Finalizes the text rendering pass.
     * Unbinds the shader.
     */
    public void end() {
        shader.unbind();
    }

    /**
     * Gets the projection matrix currently in use by the renderer.
     * <p>
     * This can be useful for advanced font effects that might need to know the
     * current transformation state.
     * </p>
     *
     * @return The active 4x4 projection matrix.
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
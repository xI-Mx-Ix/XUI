/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import net.xmx.xui.core.msdf.MSDFAtlas;
import net.xmx.xui.core.gl.shader.impl.MSDFShader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Handles the rendering lifecycle for generic MSDF-based elements.
 * <p>
 * This renderer acts as the bridge between high-level draw calls (Fonts, Icons) and
 * OpenGL draw commands. It manages the shader state, matrix uploads, and uniform configuration.
 * It is agnostic to the content type, accepting any object implementing {@link MSDFAtlas}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MSDFRenderer {

    private final MSDFShader shader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructs a new renderer with an MSDF shader and a standard mesh buffer.
     */
    public MSDFRenderer() {
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
     * Initializes the rendering state for a batch of MSDF elements.
     * <p>
     * Sets up the orthogonal projection matrix and configures shader uniforms based on the
     * provided atlas metadata.
     * </p>
     *
     * @param guiScale        The current GUI scale factor used for projection.
     * @param atlas           The MSDF atlas that will be rendered. This provides the texture
     *                        and the {@code distanceRange} (pxRange) metadata required by the shader.
     * @param modelViewMatrix The current model-view transformation matrix.
     */
    public void begin(double guiScale, MSDFAtlas atlas, Matrix4f modelViewMatrix) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        // Setup Orthographic Projection (0,0 at top-left)
        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        shader.bind();
        shader.uploadProjection(projectionMatrix);
        shader.uploadModelView(modelViewMatrix);
        shader.uploadTextureUnit(0);

        // Upload the pixel range from the atlas metadata to the shader.
        // This ensures edge sharpness is calculated correctly regardless of whether
        // we are rendering a small icon or large text.
        if (atlas != null && atlas.getMetadata() != null && atlas.getMetadata().atlas != null) {
            shader.uploadPxRange(atlas.getMetadata().atlas.distanceRange);
        }
    }

    /**
     * Binds the specified texture and draws the buffered mesh.
     * <p>
     * This method binds the provided texture ID to GL_TEXTURE0 and then
     * flushes the mesh buffer content as GL_TRIANGLES.
     * </p>
     *
     * @param textureId The OpenGL texture ID of the MSDF atlas to use.
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
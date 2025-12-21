/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.gl.vertex.VertexFormat;
import net.xmx.xui.core.sdf.SDFAtlas;
import net.xmx.xui.core.sdf.SDFType;
import net.xmx.xui.core.sdf.shader.MSDFShader;
import net.xmx.xui.core.sdf.shader.MTSDFShader;
import net.xmx.xui.core.sdf.shader.SDFShader;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Handles the rendering lifecycle for Unified SDF-based elements.
 * <p>
 * This renderer manages both {@link MSDFShader} (for text) and {@link MTSDFShader} (for icons/shapes).
 * It automatically selects the appropriate shader based on the {@link SDFAtlas} type passed to {@link #begin}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class SDFRenderer {

    private final MSDFShader msdfShader;
    private final MTSDFShader mtsdfShader;
    private final MeshBuffer mesh;
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Track which shader is currently bound
    private SDFShader activeShader;

    public SDFRenderer() {
        this.msdfShader = new MSDFShader();
        this.mtsdfShader = new MTSDFShader();
        this.mesh = new MeshBuffer(VertexFormat.POS_COLOR_UV);
    }

    public MeshBuffer getMesh() {
        return mesh;
    }

    /**
     * Initializes the rendering state for a batch of SDF elements.
     * <p>
     * Automatically binds the correct shader (MSDF vs MTSDF) based on the atlas type.
     * </p>
     *
     * @param guiScale        The current GUI scale.
     * @param atlas           The SDF atlas to render.
     * @param modelViewMatrix The model-view matrix.
     */
    public void begin(double guiScale, SDFAtlas atlas, Matrix4f modelViewMatrix) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        projectionMatrix.identity().ortho(0, viewport[2], viewport[3], 0, -10000, 10000);
        projectionMatrix.scale((float) guiScale, (float) guiScale, 1.0f);

        // 1. Determine and Bind Shader
        SDFType type = (atlas != null) ? atlas.getType() : SDFType.MSDF;
        
        if (type == SDFType.MTSDF) {
            this.activeShader = this.mtsdfShader;
        } else {
            this.activeShader = this.msdfShader;
        }

        activeShader.bind();
        activeShader.uploadProjection(projectionMatrix);
        activeShader.uploadModelView(modelViewMatrix);
        activeShader.uploadTextureUnit(0);

        // 2. Upload Common Metadata
        if (atlas != null && atlas.getMetadata() != null && atlas.getMetadata().atlas != null) {
            activeShader.uploadPxRange(atlas.getMetadata().atlas.distanceRange);
        }

        // 3. Reset Default MTSDF State (No Outline by default)
        if (activeShader instanceof MTSDFShader) {
            ((MTSDFShader) activeShader).uploadOutlineWidth(0.0f);
        }
    }

    /**
     * Binds the texture and draws the mesh.
     *
     * @param textureId The OpenGL texture ID.
     */
    public void drawBatch(int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        mesh.flush(GL11.GL_TRIANGLES);
    }

    /**
     * Sets the outline parameters for the current batch.
     * <p>
     * <b>Note:</b> This only has an effect if the active atlas is of type {@link SDFType#MTSDF}.
     * </p>
     *
     * @param width Width of the outline (0.0 to 1.0).
     * @param color Color of the outline.
     */
    public void setOutline(float width, Vector4f color) {
        if (activeShader instanceof MTSDFShader) {
            ((MTSDFShader) activeShader).uploadOutlineWidth(width);
            ((MTSDFShader) activeShader).uploadOutlineColor(color);
        }
    }

    public void end() {
        if (activeShader != null) {
            activeShader.unbind();
            activeShader = null;
        }
    }
}
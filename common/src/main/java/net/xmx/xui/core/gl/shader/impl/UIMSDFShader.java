/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.shader.impl;

import net.xmx.xui.core.gl.shader.UIShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Shader program wrapper for the MSDF text rendering pipeline.
 * Manages uniform uploads for projection matrices, model-view matrices and font rendering parameters.
 *
 * @author xI-Mx-Ix
 */
public class UIMSDFShader extends UIShaderProgram {

    private int locProjMat;
    private int locModelViewMat;
    private int locFontTexture;
    private int locPxRange;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Constructs the shader by loading the "ui_msdf" sources.
     */
    public UIMSDFShader() {
        super("xui", "core/ui_msdf");
    }

    @Override
    protected void registerAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "color");
        super.bindAttribute(2, "uv");
    }

    @Override
    protected void registerUniforms() {
        locProjMat = super.getUniformLocation("projMat");
        locModelViewMat = super.getUniformLocation("modelViewMat");
        locFontTexture = super.getUniformLocation("fontTexture");
        locPxRange = super.getUniformLocation("pxRange");
    }

    /**
     * Uploads the orthogonal projection matrix.
     *
     * @param matrix The 4x4 matrix.
     */
    public void uploadProjection(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locProjMat, false, matrixBuffer);
    }

    /**
     * Uploads the model-view transformation matrix to the GPU.
     *
     * @param matrix The 4x4 model-view matrix.
     */
    public void uploadModelView(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locModelViewMat, false, matrixBuffer);
    }

    /**
     * Sets the texture unit index for the font atlas.
     *
     * @param unit The texture unit (e.g., 0).
     */
    public void uploadTextureUnit(int unit) {
        GL20.glUniform1i(locFontTexture, unit);
    }

    /**
     * Uploads the distance range parameter derived from the font metadata.
     * This controls the sharpness/softness of the edge evaluation.
     *
     * @param range The signed distance range in pixels.
     */
    public void uploadPxRange(float range) {
        GL20.glUniform1f(locPxRange, range);
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.shader.impl;

import net.xmx.xui.core.gl.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Shader program wrapper for the generic MSDF rendering pipeline.
 * Manages uniform uploads for matrices and MSDF parameters (pxRange).
 * <p>
 * This shader is used for both Fonts and Icons.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MSDFShader extends ShaderProgram {

    private int locProjMat;
    private int locModelViewMat;
    private int locMsdfTexture;
    private int locPxRange;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Constructs the shader by loading the generic "msdf" sources.
     */
    public MSDFShader() {
        super("xui", "core/msdf");
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
        locMsdfTexture = super.getUniformLocation("msdfTexture");
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
     * Sets the texture unit index for the MSDF atlas.
     *
     * @param unit The texture unit (e.g., 0).
     */
    public void uploadTextureUnit(int unit) {
        GL20.glUniform1i(locMsdfTexture, unit);
    }

    /**
     * Uploads the distance range parameter derived from the atlas metadata.
     * This controls the sharpness/softness of the edge evaluation.
     *
     * @param range The signed distance range in pixels.
     */
    public void uploadPxRange(float range) {
        GL20.glUniform1f(locPxRange, range);
    }
}
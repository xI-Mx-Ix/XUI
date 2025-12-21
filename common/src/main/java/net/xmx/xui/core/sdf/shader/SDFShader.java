/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf.shader;

import net.xmx.xui.core.gl.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Abstract base shader for Signed Distance Field rendering.
 * Contains common uniforms for matrix transformation and texture binding.
 *
 * @author xI-Mx-Ix
 */
public abstract class SDFShader extends ShaderProgram {

    protected int locProjMat;
    protected int locModelViewMat;
    protected int locSdfTexture;
    protected int locPxRange;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public SDFShader(String namespace, String path) {
        super(namespace, path);
    }

    @Override
    protected void registerAttributes() {
        // Standard attributes for all SDF rendering
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "color");
        super.bindAttribute(2, "uv");
    }

    @Override
    protected void registerUniforms() {
        locProjMat = super.getUniformLocation("projMat");
        locModelViewMat = super.getUniformLocation("modelViewMat");
        locSdfTexture = super.getUniformLocation("sdfTexture");
        locPxRange = super.getUniformLocation("pxRange");
    }

    public void uploadProjection(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locProjMat, false, matrixBuffer);
    }

    public void uploadModelView(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locModelViewMat, false, matrixBuffer);
    }

    public void uploadTextureUnit(int unit) {
        GL20.glUniform1i(locSdfTexture, unit);
    }

    public void uploadPxRange(float range) {
        GL20.glUniform1f(locPxRange, range);
    }
}
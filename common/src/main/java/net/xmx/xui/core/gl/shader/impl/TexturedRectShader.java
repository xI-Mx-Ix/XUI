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
 * Shader for rendering textured rectangles with optional rounded corners.
 * <p>
 * Uses an SDF (Signed Distance Field) calculation in the fragment shader
 * to clip pixels that fall outside the specified corner radius.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class TexturedRectShader extends ShaderProgram {

    private int locProjMat;
    private int locModelViewMat;
    private int locTex;
    private int locSize;
    private int locRadius;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public TexturedRectShader() {
        super("xui", "core/textured_rect");
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
        locTex = super.getUniformLocation("tex");
        locSize = super.getUniformLocation("size");
        locRadius = super.getUniformLocation("radius");
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
        GL20.glUniform1i(locTex, unit);
    }

    /**
     * Uploads the dimensions of the rectangle in pixels.
     * Required for the Fragment Shader to calculate the corner radius correctly.
     *
     * @param width  The width in pixels.
     * @param height The height in pixels.
     */
    public void uploadSize(float width, float height) {
        GL20.glUniform2f(locSize, width, height);
    }

    /**
     * Uploads the corner radius in pixels.
     *
     * @param radius The radius (0 for sharp corners).
     */
    public void uploadRadius(float radius) {
        GL20.glUniform1f(locRadius, radius);
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.shader.impl;

import net.xmx.xui.core.gl.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * The standard shader for rendering UI geometry (Rectangles, Outlines).
 * <p>
 * Attributes: Position (0), Color (1).
 * Uniforms: Projection Matrix, ModelView Matrix.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class PositionColorShader extends ShaderProgram {

    private int locationProjectionMatrix;
    private int locationModelViewMatrix;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Loads the "position_color" shader from the "xui" namespace.
     */
    public PositionColorShader() {
        super("xui", "core/position_color");
    }

    @Override
    protected void registerAttributes() {
        // Defined in GLSL: "in vec3 position;"
        super.bindAttribute(0, "position");
        // Defined in GLSL: "in vec4 color;"
        super.bindAttribute(1, "color");
    }

    @Override
    protected void registerUniforms() {
        // Defined in GLSL: "uniform mat4 projMat;"
        locationProjectionMatrix = super.getUniformLocation("projMat");
        // Defined in GLSL: "uniform mat4 modelViewMat;"
        locationModelViewMatrix = super.getUniformLocation("modelViewMat");
    }

    /**
     * Uploads the orthogonal projection matrix to the GPU.
     *
     * @param matrix The 4x4 projection matrix.
     */
    public void uploadProjection(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locationProjectionMatrix, false, matrixBuffer);
    }

    /**
     * Uploads the model-view transformation matrix to the GPU.
     *
     * @param matrix The 4x4 model-view matrix.
     */
    public void uploadModelView(Matrix4f matrix) {
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(locationModelViewMatrix, false, matrixBuffer);
    }
}
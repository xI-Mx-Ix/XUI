/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.shader.impl;

import net.xmx.xui.core.gl.shader.UIShader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * The specialized shader for rendering text with texture support.
 * <p>
 * Attributes: Position (0), Color (1), UV (2).
 * Uniforms: Projection Matrix, Font Texture Sampler.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UITextShader extends UIShader {

    private int locationProjectionMatrix;
    private int locationFontTexture; // Although usually 0, good practice to hold it
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Loads the "ui_text" shader from the "xui" namespace.
     */
    public UITextShader() {
        super("xui", "core/ui_text");
    }

    @Override
    protected void registerAttributes() {
        // Defined in GLSL: "in vec3 position;"
        super.bindAttribute(0, "position");
        // Defined in GLSL: "in vec4 color;"
        super.bindAttribute(1, "color");
        // Defined in GLSL: "in vec2 uv;"
        super.bindAttribute(2, "uv");
    }

    @Override
    protected void registerUniforms() {
        // Defined in GLSL: "uniform mat4 projMat;"
        locationProjectionMatrix = super.getUniformLocation("projMat");
        // Defined in GLSL: "uniform sampler2D fontTexture;"
        locationFontTexture = super.getUniformLocation("fontTexture");
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
     * Sets the texture unit index for the font sampler.
     * Usually 0 by default, but explicitly setting it ensures correctness.
     *
     * @param textureUnit The texture unit index (e.g., 0 for GL_TEXTURE0).
     */
    public void uploadTextureUnit(int textureUnit) {
        GL20.glUniform1i(locationFontTexture, textureUnit);
    }
}
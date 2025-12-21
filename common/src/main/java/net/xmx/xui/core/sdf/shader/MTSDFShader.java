/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf.shader;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;

/**
 * Shader implementation for MTSDF (Multi-channel True Signed Distance Field).
 * <p>
 * Uses RGB for sharp shape reconstruction and Alpha for the true distance field.
 * Supports rendering outlines via additional uniforms.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MTSDFShader extends SDFShader {

    private int locOutlineWidth;
    private int locOutlineColor;

    public MTSDFShader() {
        super("xui", "core/mtsdf");
    }

    @Override
    protected void registerUniforms() {
        super.registerUniforms();
        locOutlineWidth = super.getUniformLocation("outlineWidth");
        locOutlineColor = super.getUniformLocation("outlineColor");
    }

    /**
     * Sets the width of the outline.
     *
     * @param width The width (0.0 to 1.0, relative to the distance range).
     *              0.0 disables the outline.
     */
    public void uploadOutlineWidth(float width) {
        GL20.glUniform1f(locOutlineWidth, width);
    }

    /**
     * Sets the color of the outline.
     *
     * @param color The RGBA color vector.
     */
    public void uploadOutlineColor(Vector4f color) {
        GL20.glUniform4f(locOutlineColor, color.x, color.y, color.z, color.w);
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf.shader;

/**
 * Shader implementation for standard MSDF (Multi-channel Signed Distance Field).
 * Uses RGB channels to reconstruct sharp corners.
 *
 * @author xI-Mx-Ix
 */
public class MSDFShader extends SDFShader {

    public MSDFShader() {
        super("xui", "core/msdf");
    }

    @Override
    protected void registerUniforms() {
        super.registerUniforms();
    }
}
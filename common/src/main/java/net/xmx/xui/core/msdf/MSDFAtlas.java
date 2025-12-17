/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.msdf;

/**
 * Defines the contract for any resource that provides a Multi-channel Signed Distance Field (MSDF) texture atlas.
 * <p>
 * This interface serves as the abstraction layer for MSDF assets.
 * By implementing this interface, assets can be passed to their respective renderers
 * without tight coupling to the specific generation parameters.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public interface MSDFAtlas {

    /**
     * Retrieves the OpenGL texture ID handle for the loaded atlas image.
     * <p>
     * The texture bound to this ID must have been loaded with Linear Filtering enabled
     * to ensure correct MSDF evaluation in the shader.
     * </p>
     *
     * @return The integer texture handle.
     */
    int getTextureId();

    /**
     * Retrieves the metadata associated with this atlas.
     * <p>
     * This object contains critical rendering parameters, such as the {@code distanceRange}
     * (pxRange), which the shader needs to calculate edge sharpness.
     * </p>
     *
     * @return The MSDF metadata object.
     */
    MSDFMetadata getMetadata();
}
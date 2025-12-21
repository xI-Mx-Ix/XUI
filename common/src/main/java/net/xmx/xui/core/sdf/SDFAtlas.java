/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf;

/**
 * Defines the contract for any resource that provides a Signed Distance Field (SDF) texture atlas.
 * <p>
 * This interface abstracts the differences between MSDF (Fonts) and MTSDF (Icons).
 * Implementations provide the texture handle and metadata required for rendering.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public interface SDFAtlas {

    /**
     * Retrieves the OpenGL texture ID handle for the loaded atlas image.
     *
     * @return The integer texture handle.
     */
    int getTextureId();

    /**
     * Retrieves the metadata associated with this atlas.
     *
     * @return The SDF metadata object.
     */
    SDFMetadata getMetadata();

    /**
     * Determines the type of SDF data stored in this atlas.
     * <p>
     * Used by the renderer to switch between MSDF and MTSDF shaders.
     * </p>
     *
     * @return The type enum (MSDF or MTSDF).
     */
    default SDFType getType() {
        if (getMetadata() != null && getMetadata().atlas != null) {
            return getMetadata().atlas.type;
        }
        return SDFType.MSDF; // Default fallback
    }
}
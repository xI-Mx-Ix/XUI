/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf;

import com.google.gson.annotations.SerializedName;

/**
 * Enumerates the supported Signed Distance Field types.
 *
 * @author xI-Mx-Ix
 */
public enum SDFType {
    /**
     * Multi-channel Signed Distance Field.
     * Uses 3 channels (RGB). Best for sharp text.
     */
    @SerializedName("msdf")
    MSDF,

    /**
     * Multi-channel True Signed Distance Field.
     * Uses 4 channels (RGBA).
     * RGB holds the sharp MSDF data, Alpha holds the true distance field.
     * Best for shapes, icons, and effects like outlines/glows.
     */
    @SerializedName("mtsdf")
    MTSDF
}
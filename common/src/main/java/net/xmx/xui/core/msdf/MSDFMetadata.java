/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.msdf;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the base metadata structure of an MSDF atlas JSON file.
 * <p>
 * This class mirrors the common structure output by the <code>msdf-atlas-gen</code> tool.
 * It is intended to be extended by specific implementations like {@link net.xmx.xui.core.font.data.FontMetadata}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MSDFMetadata {

    /**
     * General configuration information about the generated texture atlas.
     */
    @SerializedName("atlas")
    public AtlasInfo atlas;

    /**
     * Encapsulates configuration details regarding the texture atlas parameters.
     */
    public static class AtlasInfo {
        /**
         * The type of the atlas generation algorithm (always "msdf").
         */
        public String type;

        /**
         * The signed distance range in pixels.
         * <p>
         * This value is critical for the shader. It represents the width of the
         * gradient (falloff) in the distance field. Higher values allow for softer edges
         * and better downscaling, but require more texture space.
         * </p>
         */
        public float distanceRange;

        /**
         * The base size (in pixels) used during generation (e.g. the font size).
         */
        public float size;

        /**
         * The total width of the generated texture atlas in pixels.
         */
        public float width;

        /**
         * The total height of the generated texture atlas in pixels.
         */
        public float height;

        /**
         * The origin of the Y-axis (e.g., "bottom").
         * Helps determine if V-coordinates need flipping.
         */
        public String yOrigin;
    }

    /**
     * Represents a geometric bounding box defined by its edges.
     * Used for defining glyph bounds in em-space or texture-space.
     */
    public static class Bounds {
        /**
         * The left edge coordinate.
         */
        public float left;

        /**
         * The bottom edge coordinate.
         */
        public float bottom;

        /**
         * The right edge coordinate.
         */
        public float right;

        /**
         * The top edge coordinate.
         */
        public float top;
    }
}
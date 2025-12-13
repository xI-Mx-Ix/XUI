/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font.data;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Represents the data structure of the MSDF font metadata file (JSON).
 * This class mirrors the output format of the msdf-atlas-gen tool.
 *
 * @author xI-Mx-Ix
 */
public class MSDFData {

    /**
     * General information about the generated texture atlas.
     */
    @SerializedName("atlas")
    public AtlasInfo atlas;

    /**
     * Global font metrics used for layout calculations.
     */
    @SerializedName("metrics")
    public Metrics metrics;

    /**
     * A map allowing fast lookup of glyph data by their Unicode integer value.
     * Note: This field is populated manually after parsing the raw JSON array.
     */
    public transient Map<Integer, Glyph> glyphMap;

    /**
     * Contains configuration details regarding the texture atlas.
     */
    public static class AtlasInfo {
        /**
         * The type of the atlas, typically "msdf".
         */
        public String type;

        /**
         * The signed distance range in pixels.
         * This value is critical for the shader to correctly interpret the color gradients.
         */
        public float distanceRange;

        /**
         * The font size used during generation.
         */
        public float size;

        /**
         * The total width of the texture atlas in pixels.
         */
        public float width;

        /**
         * The total height of the texture atlas in pixels.
         */
        public float height;

        /**
         * The origin of the Y-axis (e.g., "bottom").
         */
        public String yOrigin;
    }

    /**
     * Defines global vertical metrics for the font family.
     * All values are normalized to the em size.
     */
    public static class Metrics {
        /**
         * The size of the em square.
         */
        public float emSize;

        /**
         * The distance between baselines of consecutive lines.
         */
        public float lineHeight;

        /**
         * The distance from the baseline to the top of the highest glyph.
         */
        public float ascender;

        /**
         * The distance from the baseline to the bottom of the lowest glyph.
         */
        public float descender;
    }

    /**
     * Represents a single character glyph within the font.
     */
    public static class Glyph {
        /**
         * The unicode codepoint of this character.
         */
        public int unicode;

        /**
         * The horizontal distance to move the cursor after drawing this glyph.
         * Value is normalized to the em size.
         */
        public float advance;

        /**
         * The geometric bounds of the glyph relative to the baseline.
         * Used to determine vertex positions.
         */
        public Bounds planeBounds;

        /**
         * The pixel coordinates of the glyph within the texture atlas.
         * Used to calculate UV coordinates.
         */
        public Bounds atlasBounds;
    }

    /**
     * Represents a rectangle defined by its edges.
     */
    public static class Bounds {
        public float left;
        public float bottom;
        public float right;
        public float top;
    }
}
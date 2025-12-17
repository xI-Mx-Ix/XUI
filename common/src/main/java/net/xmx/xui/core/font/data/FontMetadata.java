/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font.data;

import com.google.gson.annotations.SerializedName;
import net.xmx.xui.core.msdf.MSDFMetadata;

import java.util.Map;

/**
 * Specialized metadata for Fonts, extending the base MSDF structure
 * with typography metrics and glyph information.
 *
 * @author xI-Mx-Ix
 */
public class FontMetadata extends MSDFMetadata {

    /**
     * Global font metrics used for line layout calculations.
     */
    @SerializedName("metrics")
    public Metrics metrics;

    /**
     * A map allowing fast lookup of glyph data by their Unicode integer value.
     * <p>
     * <b>Note:</b> This field is transient because it is populated manually
     * after parsing the raw JSON array for performance reasons (O(1) lookup).
     * </p>
     */
    public transient Map<Integer, Glyph> glyphMap;

    /**
     * Holds font metric values describing vertical font layout.
     */
    public static class Metrics {

        /**
         * Size of the em square used as the base unit for the font.
         */
        public float emSize;

        /**
         * Distance between the baselines of consecutive lines.
         */
        public float lineHeight;

        /**
         * Distance from the baseline to the top of the highest glyph.
         */
        public float ascender;

        /**
         * Distance from the baseline to the bottom of the lowest glyph.
         * <p>
         * This value is typically negative.
         */
        public float descender;
    }

    /**
     * Represents a single character glyph within the font.
     */
    public static class Glyph {
        /** The unicode codepoint of this character. */
        public int unicode;
        
        /** 
         * The horizontal distance to move the cursor after drawing this glyph.
         * Normalized to em size.
         */
        public float advance;
        
        /**
         * The geometric bounds relative to the baseline.
         */
        public MSDFMetadata.Bounds planeBounds;
        
        /**
         * The pixel coordinates in the atlas.
         */
        public MSDFMetadata.Bounds atlasBounds;
    }
}
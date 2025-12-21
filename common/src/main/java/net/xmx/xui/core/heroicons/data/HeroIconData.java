/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.heroicons.data;

import com.google.gson.annotations.SerializedName;
import net.xmx.xui.core.sdf.SDFMetadata;

import java.util.Map;

/**
 * Represents the JSON metadata structure for a Heroicon atlas.
 *
 * @author xI-Mx-Ix
 */
public class HeroIconData extends SDFMetadata {

    /**
     * Map of icon names (filename without extension) to their coordinates in the atlas.
     */
    @SerializedName("icons")
    public Map<String, IconBounds> icons;

    /**
     * Represents the pixel bounds of a single icon within the atlas texture.
     */
    public static class IconBounds {
        public int x;
        public int y;
        public int width;
        public int height;
    }
}
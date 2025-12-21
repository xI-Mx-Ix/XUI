/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.heroicons.atlas;

import com.google.gson.Gson;
import net.xmx.xui.core.heroicons.IconType;
import net.xmx.xui.core.heroicons.data.HeroIconData;
import net.xmx.xui.core.sdf.SDFAtlas;
import net.xmx.xui.core.sdf.SDFMetadata;
import net.xmx.xui.core.sdf.SDFType;
import net.xmx.xui.core.sdf.texture.SDFTextureLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages the texture and metadata for a set of Heroicons (either Solid or Outline).
 *
 * @author xI-Mx-Ix
 */
public class HeroIconAtlas implements SDFAtlas {

    private final int textureId;
    private final HeroIconData metadata;

    /**
     * Loads the icon atlas from the classpath.
     *
     * @param namespace The resource namespace (e.g. "xui").
     * @param type      The icon type (SOLID or OUTLINE) to determine the filename.
     */
    public HeroIconAtlas(String namespace, IconType type) {
        String variant = type.name().toLowerCase(); // "solid" or "outline"
        String basePath = "/assets/" + namespace + "/heroicons/" + variant;

        // 1. Load JSON
        try (InputStream stream = getClass().getResourceAsStream(basePath + ".json")) {
            if (stream == null) {
                throw new RuntimeException("HeroIcon JSON not found: " + basePath + ".json");
            }
            this.metadata = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    HeroIconData.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load HeroIcon metadata: " + variant, e);
        }

        // 2. Load Texture (Force 4 channels for MTSDF)
        this.textureId = SDFTextureLoader.loadSDFTexture(basePath + ".png", SDFType.MTSDF);
    }

    /**
     * Retrieves the metadata for a specific icon by name.
     *
     * @param name The name of the icon (e.g., "home", "user").
     * @return The bounds data or null if not found.
     */
    public HeroIconData.IconBounds getIcon(String name) {
        return metadata.icons.get(name);
    }

    @Override
    public int getTextureId() {
        return textureId;
    }

    @Override
    public SDFMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public SDFType getType() {
        return SDFType.MTSDF;
    }
}
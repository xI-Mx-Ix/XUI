/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.xmx.xui.core.font.data.FontMetadata;
import net.xmx.xui.core.msdf.MSDFAtlas;
import net.xmx.xui.core.msdf.MSDFMetadata;
import net.xmx.xui.core.msdf.texture.MSDFTextureLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Manages the loading and storage of a single font variant (e.g., "Regular" or "Bold").
 * <p>
 * This class implements {@link MSDFAtlas}, making it compatible with the shared rendering pipeline.
 * It parses specific font metrics alongside the standard MSDF metadata.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class FontAtlas implements MSDFAtlas {

    /**
     * The parsed metadata containing metrics and glyph bounds.
     */
    private final FontMetadata fontData;

    /**
     * The OpenGL texture ID handle for the loaded PNG atlas.
     */
    private final int textureId;

    /**
     * Constructs a new font atlas by loading resources from the classpath.
     *
     * @param namespace The resource namespace (e.g., "xui").
     * @param path      The relative path and name of the font file (e.g., "fonts/jetbrains-mono").
     */
    public FontAtlas(String namespace, String path) {
        String basePath = "/assets/" + namespace + "/fonts/" + path;

        // 1. Load and Parse JSON Metadata
        try (InputStream jsonStream = getClass().getResourceAsStream(basePath + ".json")) {
            if (jsonStream == null) {
                throw new RuntimeException("Font JSON resource not found at path: " + basePath + ".json");
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);

            this.fontData = new FontMetadata();
            // Parse generic atlas info
            this.fontData.atlas = gson.fromJson(root.get("atlas"), MSDFMetadata.AtlasInfo.class);
            // Parse font specific metrics
            this.fontData.metrics = gson.fromJson(root.get("metrics"), FontMetadata.Metrics.class);

            // Manually populate the glyph map for O(1) access
            this.fontData.glyphMap = new HashMap<>();
            JsonArray glyphs = root.getAsJsonArray("glyphs");

            for (JsonElement el : glyphs) {
                FontMetadata.Glyph g = gson.fromJson(el, FontMetadata.Glyph.class);
                this.fontData.glyphMap.put(g.unicode, g);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse font JSON metadata for: " + path, e);
        }

        // 2. Load PNG Texture Atlas
        this.textureId = MSDFTextureLoader.loadMSDFTexture(basePath + ".png");
    }

    /**
     * Gets the OpenGL texture ID for this font's atlas.
     *
     * @return The integer texture handle.
     */
    @Override
    public int getTextureId() {
        return textureId;
    }

    /**
     * Gets the full metadata object associated with this font.
     *
     * @return The MSDF data containing metrics and atlas info.
     */
    @Override
    public MSDFMetadata getMetadata() {
        return fontData;
    }

    /**
     * Gets the specialized font metadata containing metrics and glyphs.
     *
     * @return The typed FontMetadata.
     */
    public FontMetadata getFontData() {
        return fontData;
    }

    /**
     * Convenience method to retrieve a glyph by its character code.
     *
     * @param unicode The unicode codepoint.
     * @return The Glyph data or null.
     */
    public FontMetadata.Glyph getGlyph(int unicode) {
        return fontData.glyphMap.get(unicode);
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.texture;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the lifecycle and caching of loaded {@link UITexture}s.
 * Ensures that the same resource path is not loaded into VRAM multiple times.
 *
 * @author xI-Mx-Ix
 */
public final class TextureCache {

    private static final Map<String, UITexture> CACHE = new HashMap<>();

    private TextureCache() {}

    /**
     * Retrieves a texture from the cache or loads it if missing.
     *
     * @param namespace The resource namespace.
     * @param path      The resource path.
     * @return The cached or new texture instance.
     */
    public static UITexture get(String namespace, String path) {
        String key = namespace + ":" + path;
        return CACHE.computeIfAbsent(key, k -> new UITexture(namespace, path));
    }
    
    /**
     * Clears all textures and frees GPU memory.
     * Should be called on shutdown.
     */
    public static void cleanup() {
        CACHE.values().forEach(UITexture::cleanup);
        CACHE.clear();
    }
}
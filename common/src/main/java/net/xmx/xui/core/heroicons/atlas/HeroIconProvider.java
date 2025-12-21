/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.heroicons.atlas;

import net.xmx.xui.core.heroicons.IconType;
import net.xmx.xui.init.XuiMainClass;

/**
 * Singleton registry for accessing loaded HeroIcon atlases.
 * <p>
 * Must be initialized via {@link #init()} after the OpenGL context is created.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public final class HeroIconProvider {

    private static HeroIconAtlas solidAtlas;
    private static HeroIconAtlas outlineAtlas;

    private HeroIconProvider() {}

    /**
     * Initializes the icon atlases. 
     * Called during the main rendering initialization phase.
     */
    public static void init() {
        if (solidAtlas != null) return;
        try {
            solidAtlas = new HeroIconAtlas(XuiMainClass.MODID, IconType.SOLID);
            outlineAtlas = new HeroIconAtlas(XuiMainClass.MODID, IconType.OUTLINE);
        } catch (Exception e) {
            XuiMainClass.LOGGER.error("Failed to initialize HeroIconProvider", e);
        }
    }

    /**
     * Retrieves the atlas corresponding to the requested type.
     *
     * @param type The desired icon variant.
     * @return The loaded atlas.
     * @throws IllegalStateException if not initialized.
     */
    public static HeroIconAtlas getAtlas(IconType type) {
        if (solidAtlas == null) throw new IllegalStateException("HeroIconProvider not initialized.");
        return type == IconType.SOLID ? solidAtlas : outlineAtlas;
    }
}
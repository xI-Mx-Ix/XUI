/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.xmx.xui.core.platform.PlatformRenderProvider;
import net.xmx.xui.impl.RenderImpl;
import net.xmx.xui.init.registry.ModRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xI-Mx-Ix
 */
public class XuiMainClass {
    public static final String MODID = "xui";
    public static final Logger LOGGER = LogManager.getLogger("XUI");

    /**
     * Called during the common initialization phase (Server and Client).
     */
    public static void onInit() {
        ModRegistries.register();
    }

    /**
     * Called during the client-specific initialization phase.
     */
    @Environment(EnvType.CLIENT)
    public static void onClientInit() {
        PlatformRenderProvider.register(RenderImpl.getInstance());
    }
}
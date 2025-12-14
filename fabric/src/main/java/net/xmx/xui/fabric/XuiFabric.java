/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.fabric;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.fabricmc.api.ModInitializer;
import net.xmx.xui.init.XuiMainClass;

/**
 * @author xI-Mx-Ix
 */
public final class XuiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        XuiMainClass.onInit();
        if (Platform.getEnvironment() == Env.CLIENT) {
            XuiMainClass.onClientInit();
        }
    }
}
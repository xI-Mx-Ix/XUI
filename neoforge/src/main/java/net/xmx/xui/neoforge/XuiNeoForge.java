package net.xmx.xui.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.neoforged.fml.common.Mod;
import net.xmx.xui.init.XuiMainClass;

@Mod(XuiMainClass.MODID)
public final class XuiNeoForge {
    public XuiNeoForge() {
        XuiMainClass.onInit();
        if (Platform.getEnvironment() == Env.CLIENT) {
            XuiMainClass.onClientInit();
        }
    }
}
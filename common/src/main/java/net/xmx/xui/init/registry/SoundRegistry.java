/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.init.registry;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * @author xI-Mx-Ix
 */
public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create("xui", Registries.SOUND_EVENT);

    public static void register() {
        REGISTRY.register();
    }
}
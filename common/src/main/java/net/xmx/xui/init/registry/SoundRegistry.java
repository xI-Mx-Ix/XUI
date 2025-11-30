/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.init.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * @author xI-Mx-Ix
 */
public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create("xui", Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> CLICK = REGISTRY.register("click", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("xui", "click")));

    public static final RegistrySupplier<SoundEvent> CLICK_ERR = REGISTRY.register("click_err", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("xui", "click_err")));

    public static final RegistrySupplier<SoundEvent> CLICK_2 = REGISTRY.register("click_2", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("xui", "click_2")));

    public static final RegistrySupplier<SoundEvent> BUBBLE_CLICK = REGISTRY.register("bubble_click", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("xui", "bubble_click")));

    public static void register() {
        REGISTRY.register();
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.mixin.impl;

import net.minecraft.client.renderer.GameRenderer;
import net.xmx.xui.init.XuiMainClass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for GameRenderer to inject custom shader initialization.
 * <p>
 * Ensures that XuiMainClass.initShaders() is called when a valid
 * OpenGL context is available.
 *
 * @author xI-Mx-Ix
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    /**
     * Injects into the GameRenderer constructor after it finishes.
     * <p>
     * This is a safe point to initialize custom shaders because the
     * OpenGL context is ready and the GameRenderer has been fully constructed.
     *
     * @param ci Callback info provided by Mixin
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onGameRendererInit(CallbackInfo ci) {
        // Initialize custom shaders with a valid OpenGL context
        XuiMainClass.initShaders();
    }
}
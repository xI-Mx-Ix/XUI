/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Registry holding the singleton instances of available fonts.
 * Uses lazy initialization to prevent OpenGL crashes during early startup.
 *
 * @author xI-Mx-Ix
 */
public final class UIStandardFonts {

    private static UICustomFont jetBrainsMono;
    private static UIVanillaFont vanilla;
    private static boolean initialized = false;

    /**
     * Internal method to load fonts.
     * Only called when needed and ensures OpenGL context is active.
     */
    private static void ensureInitialized() {
        if (initialized) return;

        // Safety check: Ensure we are on the Render Thread with an active context
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(UIStandardFonts::ensureInitialized);
            return;
        }

        try {
            // Initialize Vanilla Wrapper
            vanilla = new UIVanillaFont();

            // Initialize Custom Font
            // Ensure the paths exist in your resources!
            jetBrainsMono = new UICustomFont();
            jetBrainsMono.setRegular("assets/xui/fonts/JetBrainsMono-Regular.ttf")
                    .setBold("assets/xui/fonts/JetBrainsMono-Bold.ttf")
                    .setItalic("assets/xui/fonts/JetBrainsMono-Italic.ttf");

            initialized = true;
        } catch (Exception e) {
            System.err.println("[XUI] Failed to load standard fonts!");
            e.printStackTrace();
            // Fallback to avoid null pointers later
            if (vanilla == null) vanilla = new UIVanillaFont();
        }
    }

    /**
     * @return The JetBrains Mono Custom Font.
     */
    public static UICustomFont getJetBrainsMono() {
        ensureInitialized();
        if (jetBrainsMono == null) {
            // Fallback if loading failed, to prevent crash
            // Ideally return a dummy or vanilla wrapper here
            throw new IllegalStateException("Custom font failed to load. Check logs.");
        }
        return jetBrainsMono;
    }

    /**
     * @return The Vanilla Minecraft Font.
     */
    public static UIVanillaFont getVanilla() {
        ensureInitialized();
        return vanilla;
    }

    private UIStandardFonts() {}
}
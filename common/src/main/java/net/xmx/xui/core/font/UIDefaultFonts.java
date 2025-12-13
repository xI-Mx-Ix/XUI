/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.xmx.xui.core.font.type.UICustomFont;
import net.xmx.xui.core.font.type.UIVanillaFont;
import net.xmx.xui.init.XuiMainClass;

/**
 * Registry holding the singleton instances of available fonts.
 * Uses lazy initialization to prevent OpenGL crashes during early startup.
 *
 * @author xI-Mx-Ix
 */
public final class UIDefaultFonts {

    private static UICustomFont jetBrainsMono;
    private static UICustomFont roboto;
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
            RenderSystem.recordRenderCall(UIDefaultFonts::ensureInitialized);
            return;
        }

        try {
            // Initialize Vanilla Wrapper
            vanilla = new UIVanillaFont();

            // Initialize JetBrains Mono (MSDF)
            // The paths include the subdirectory "jetbrains-mono/" to match your assets structure.
            jetBrainsMono = new UICustomFont();
            jetBrainsMono.setRegular("jetbrains-mono/JetBrainsMono-Regular")
                    .setBold("jetbrains-mono/JetBrainsMono-Bold")
                    .setItalic("jetbrains-mono/JetBrainsMono-Italic");

            // Initialize Roboto (MSDF)
            // The paths include the subdirectory "roboto/" to match your assets structure.
            roboto = new UICustomFont();
            roboto.setRegular("roboto/Roboto-Regular")
                    .setBold("roboto/Roboto-Bold")
                    .setItalic("roboto/Roboto-Italic");

            initialized = true;
        } catch (Exception e) {
            XuiMainClass.LOGGER.error("Failed to load standard fonts!");
            e.printStackTrace();
            // Fallback to avoid null pointers later
            if (vanilla == null) vanilla = new UIVanillaFont();
        }
    }

    /**
     * Retrieves the JetBrains Mono Custom Font instance.
     *
     * @return The JetBrains Mono Custom Font.
     */
    public static UICustomFont getJetBrainsMono() {
        ensureInitialized();
        if (jetBrainsMono == null) {
            // Fallback if loading failed, to prevent crash
            throw new IllegalStateException("JetBrains Mono font failed to load. Check logs.");
        }
        return jetBrainsMono;
    }

    /**
     * Retrieves the Roboto Custom Font instance.
     *
     * @return The Roboto Custom Font.
     */
    public static UICustomFont getRoboto() {
        ensureInitialized();
        if (roboto == null) {
            // Fallback if loading failed, to prevent crash
            throw new IllegalStateException("Roboto font failed to load. Check logs.");
        }
        return roboto;
    }

    /**
     * Retrieves the Vanilla Minecraft Font instance.
     *
     * @return The Vanilla Minecraft Font.
     */
    public static UIVanillaFont getVanilla() {
        ensureInitialized();
        return vanilla;
    }

    private UIDefaultFonts() {}
}
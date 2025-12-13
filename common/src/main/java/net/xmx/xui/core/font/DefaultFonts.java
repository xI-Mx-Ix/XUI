/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.xmx.xui.core.font.type.CustomFont;
import net.xmx.xui.core.font.type.VanillaFont;
import net.xmx.xui.init.XuiMainClass;

/**
 * Registry holding the singleton instances of available fonts.
 * Uses lazy initialization to prevent OpenGL crashes during early startup.
 *
 * @author xI-Mx-Ix
 */
public final class DefaultFonts {

    private static CustomFont jetBrainsMono;
    private static CustomFont roboto;
    private static VanillaFont vanilla;
    private static boolean initialized = false;

    /**
     * Internal method to load fonts.
     * Only called when needed and ensures OpenGL context is active.
     */
    private static void ensureInitialized() {
        if (initialized) return;

        // Safety check: Ensure we are on the Render Thread with an active context
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(DefaultFonts::ensureInitialized);
            return;
        }

        try {
            // Initialize Vanilla Wrapper
            vanilla = new VanillaFont();

            // Initialize JetBrains Mono (MSDF)
            // The paths include the subdirectory "jetbrains-mono/".
            jetBrainsMono = new CustomFont();
            jetBrainsMono.setRegular(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Regular")
                    .setBold(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Bold")
                    .setItalic(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Italic");

            // Initialize Roboto (MSDF)
            // The paths include the subdirectory "roboto/".
            roboto = new CustomFont();
            roboto.setRegular(XuiMainClass.MODID, "roboto/Roboto-Regular")
                    .setBold(XuiMainClass.MODID, "roboto/Roboto-Bold")
                    .setItalic(XuiMainClass.MODID, "roboto/Roboto-Italic");

            initialized = true;
        } catch (Exception e) {
            XuiMainClass.LOGGER.error("Failed to load standard fonts!");
            e.printStackTrace();
            // Fallback to avoid null pointers later
            if (vanilla == null) vanilla = new VanillaFont();
        }
    }

    /**
     * Retrieves the JetBrains Mono Custom Font instance.
     *
     * @return The JetBrains Mono Custom Font.
     */
    public static CustomFont getJetBrainsMono() {
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
    public static CustomFont getRoboto() {
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
    public static VanillaFont getVanilla() {
        ensureInitialized();
        return vanilla;
    }

    private DefaultFonts() {}
}
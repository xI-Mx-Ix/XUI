/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font;

import net.xmx.xui.core.font.type.CustomFont;
import net.xmx.xui.core.font.type.VanillaFont;
import net.xmx.xui.init.XuiMainClass;

/**
 * Registry holding the singleton instances of available fonts.
 * <p>
 * <b>Initialization:</b> Requires {@link #init()} to be called explicitly
 * once the OpenGL context is available.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public final class DefaultFonts {

    private static CustomFont jetBrainsMono;
    private static CustomFont roboto;
    private static VanillaFont vanilla;

    /**
     * Initializes the standard fonts.
     * <p>
     * This method must be called exactly once during the application startup,
     * specifically after the Render System/OpenGL context is ready.
     * </p>
     */
    public static void init() {
        // Prevent double initialization
        if (vanilla != null) return;

        try {
            // Initialize Vanilla Wrapper
            vanilla = new VanillaFont();

            // Initialize JetBrains Mono (MSDF)
            jetBrainsMono = new CustomFont();
            jetBrainsMono.setRegular(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Regular")
                    .setBold(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Bold")
                    .setItalic(XuiMainClass.MODID, "jetbrains-mono/JetBrainsMono-Italic");

            // Initialize Roboto (MSDF)
            roboto = new CustomFont();
            roboto.setRegular(XuiMainClass.MODID, "roboto/Roboto-Regular")
                    .setBold(XuiMainClass.MODID, "roboto/Roboto-Bold")
                    .setItalic(XuiMainClass.MODID, "roboto/Roboto-Italic");

        } catch (Exception e) {
            XuiMainClass.LOGGER.error("Failed to load standard fonts!", e);
        }
    }

    /**
     * Retrieves the JetBrains Mono Custom Font instance.
     *
     * @return The JetBrains Mono Custom Font.
     * @throws IllegalStateException If {@link #init()} has not been called.
     */
    public static CustomFont getJetBrainsMono() {
        if (jetBrainsMono == null) {
            throw new IllegalStateException("DefaultFonts not initialized. Call DefaultFonts.init() first.");
        }
        return jetBrainsMono;
    }

    /**
     * Retrieves the Roboto Custom Font instance.
     *
     * @return The Roboto Custom Font.
     * @throws IllegalStateException If {@link #init()} has not been called.
     */
    public static CustomFont getRoboto() {
        if (roboto == null) {
            throw new IllegalStateException("DefaultFonts not initialized. Call DefaultFonts.init() first.");
        }
        return roboto;
    }

    /**
     * Retrieves the Vanilla Minecraft Font instance.
     *
     * @return The Vanilla Minecraft Font.
     * @throws IllegalStateException If {@link #init()} has not been called.
     */
    public static VanillaFont getVanilla() {
        if (vanilla == null) {
            throw new IllegalStateException("DefaultFonts not initialized. Call DefaultFonts.init() first.");
        }
        return vanilla;
    }

    private DefaultFonts() {}
}
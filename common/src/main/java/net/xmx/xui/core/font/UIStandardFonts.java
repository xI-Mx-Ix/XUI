/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

/**
 * A registry holding globally accessible instances of standard fonts.
 * Ensures commonly used fonts like JetBrains Mono are loaded only once.
 *
 * @author xI-Mx-Ix
 */
public final class UIStandardFonts {

    private static UIFont jetBrainsMono;

    /**
     * Initializes the standard fonts.
     * Should be called during the mod/application initialization phase.
     */
    public static void init() {
        // JetBrains Mono Configuration
        jetBrainsMono = new UIFont()
                .setDefault("assets/xui/fonts/JetBrainsMono-Regular.ttf")
                .setBold("assets/xui/fonts/JetBrainsMono-Bold.ttf")
                .setItalic("assets/xui/fonts/JetBrainsMono-Italic.ttf");
    }

    /**
     * Gets the JetBrains Mono font family.
     * @return The font instance.
     */
    public static UIFont getJetBrainsMono() {
        if (jetBrainsMono == null) {
            throw new IllegalStateException("UIStandardFonts not initialized! Call init() first.");
        }
        return jetBrainsMono;
    }

    private UIStandardFonts() {
        // Prevent instantiation
    }
}
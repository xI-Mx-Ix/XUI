/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.UIWidget;

/**
 * A Modern Button component.
 * Features:
 * - Animated background and border colors
 * - Animated scale (pop effect)
 * - Rounded corners
 * - Text centering with Component support
 * - Configurable borders
 *
 * @author xI-Mx-Ix
 */
public class UIButton extends UIWidget {

    private TextComponent label;

    /**
     * Constructs a button with default styles and an empty label.
     * Content should be set using {@link #setLabel(TextComponent)} or {@link #setLabel(String)}.
     */
    public UIButton() {
        this.label = TextComponent.empty();
        setupModernStyles();
    }

    /**
     * Configures the default visual properties for the button states.
     */
    private void setupModernStyles() {
        this.style()
                .setTransitionSpeed(12.0f) // Fast, smooth animation (higher = faster)

                // --- DEFAULT STATE (Glass Look) ---
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xAA202020) // Transparent Dark Grey
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFE0E0E0)       // Off-White
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, 8.0f)          // Nice rounding
                .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 0f)         // No border by default
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0x00000000)

                // --- HOVER STATE (Highlight) ---
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xDD404040)   // Lighter, less transparent
                .set(InteractionState.HOVER, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF)         // Pure White
                .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.05f)                   // Grow slightly
                .set(InteractionState.HOVER, ThemeProperties.BORDER_RADIUS, 10.0f)           // Rounder
                .set(InteractionState.HOVER, ThemeProperties.BORDER_THICKNESS, 0f)           // Keep border thickness same
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0x00000000)       // Keep border transparent

                // --- ACTIVE/CLICK STATE (Feedback) ---
                .set(InteractionState.ACTIVE, ThemeProperties.BACKGROUND_COLOR, 0xFF000000)  // Black
                .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.95f);                 // Shrink slightly
    }

    @Override
    protected void drawSelf(RenderInterface renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Calculate animated values using the Animation Manager
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int txtColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);

        float radius = getFloat(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float scale = getFloat(ThemeProperties.SCALE, state, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 2. Math for Scaling from Center (Zoom effect)
        float scaledW = width * scale;
        float scaledH = height * scale;
        float adjX = x - (scaledW - width) / 2.0f;
        float adjY = y - (scaledH - height) / 2.0f;

        // 4. Draw Main Button Body
        renderer.drawRect(adjX, adjY, scaledW, scaledH, bgColor, radius);

        // 5. Draw Border (if enabled)
        if (borderThick > 0 && (borderColor >>> 24) > 0) {
            renderer.drawOutline(adjX, adjY, scaledW, scaledH, borderColor, radius, borderThick);
        }

        // 6. Draw Text (Centered)
        // Uses TextComponent.getTextWidth() for Component width calculation
        int strWidth = TextComponent.getTextWidth(label);
        int strHeight = TextComponent.getFontHeight();

        float textX = x + (width - strWidth) / 2.0f;
        float textY = y + (height - strHeight) / 2.0f + 1;

        renderer.drawText(label, textX, textY, txtColor, true);
    }

    /**
     * Updates the button label.
     *
     * @param label The new label component.
     * @return This button instance.
     */
    public UIButton setLabel(TextComponent label) {
        this.label = label;
        return this;
    }

    /**
     * Updates the button label with a string literal.
     *
     * @param label The new label string.
     * @return This button instance.
     */
    public UIButton setLabel(String label) {
        this.label = TextComponent.literal(label);
        return this;
    }
}
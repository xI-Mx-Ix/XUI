/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.network.chat.Component;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

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

    private Component label;

    /**
     * Constructs a button with default styles and an empty label.
     * Content should be set using {@link #setLabel(Component)} or {@link #setLabel(String)}.
     */
    public UIButton() {
        this.label = Component.empty();
        setupModernStyles();
    }

    /**
     * Configures the default visual properties for the button states.
     */
    private void setupModernStyles() {
        this.style()
                .setTransitionSpeed(12.0f) // Fast, smooth animation (higher = faster)

                // --- DEFAULT STATE (Glass Look) ---
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0xAA202020) // Transparent Dark Grey
                .set(UIState.DEFAULT, Properties.TEXT_COLOR, 0xFFE0E0E0)       // Off-White
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)          // Nice rounding
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.DEFAULT, Properties.BORDER_THICKNESS, 0f)         // No border by default
                .set(UIState.DEFAULT, Properties.BORDER_COLOR, 0x00000000)

                // --- HOVER STATE (Highlight) ---
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xDD404040)   // Lighter, less transparent
                .set(UIState.HOVER, Properties.TEXT_COLOR, 0xFFFFFFFF)         // Pure White
                .set(UIState.HOVER, Properties.SCALE, 1.05f)                   // Grow slightly
                .set(UIState.HOVER, Properties.BORDER_RADIUS, 10.0f)           // Rounder
                .set(UIState.HOVER, Properties.BORDER_THICKNESS, 0f)           // Keep border thickness same
                .set(UIState.HOVER, Properties.BORDER_COLOR, 0x00000000)       // Keep border transparent

                // --- ACTIVE/CLICK STATE (Feedback) ---
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0xFF000000)  // Black
                .set(UIState.ACTIVE, Properties.SCALE, 0.95f);                 // Shrink slightly
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, UIState state) {
        // 1. Calculate animated values using the Animation Manager
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, deltaTime);
        int txtColor = getColor(Properties.TEXT_COLOR, state, deltaTime);
        int borderColor = getColor(Properties.BORDER_COLOR, state, deltaTime);

        float radius = getFloat(Properties.BORDER_RADIUS, state, deltaTime);
        float scale = getFloat(Properties.SCALE, state, deltaTime);
        float borderThick = getFloat(Properties.BORDER_THICKNESS, state, deltaTime);

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
        // Uses renderer.getTextWidth() for Component width calculation
        int strWidth = renderer.getTextWidth(label);
        int strHeight = renderer.getFontHeight();

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
    public UIButton setLabel(Component label) {
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
        this.label = Component.literal(label);
        return this;
    }
}
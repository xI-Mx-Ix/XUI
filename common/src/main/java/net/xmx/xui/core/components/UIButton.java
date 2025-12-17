/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.UIWidget;

/**
 * A Modern Button component.
 * Features:
 * - Smooth color transitions (Dark Grey -> Lighter Grey)
 * - Tactile click feedback (Micro-scale)
 * - Stable hover state (No shape shifting)
 * - Text centering with Scaled Component support
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
                .setTransitionSpeed(12.0f)

                // --- DEFAULT STATE (Clean Dark) ---
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xCC202020) // Dark Grey, slightly transparent
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFE0E0E0)       // Light Grey Text (not harsh white)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f)) // Fixed, subtle rounding
                .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0x00000000)

                // --- HOVER STATE (Subtle Highlight) ---
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xEE353535)   // Slightly lighter, more opaque
                .set(InteractionState.HOVER, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF)         // Pure White Text
                .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.0f)                    // Stay scale 1.0
                .set(InteractionState.HOVER, ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f)) // KEEP RADIUS SAME
                .set(InteractionState.HOVER, ThemeProperties.BORDER_THICKNESS, 0f)
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0x00000000)

                // --- ACTIVE/CLICK STATE (Tactile Feedback) ---
                .set(InteractionState.ACTIVE, ThemeProperties.BACKGROUND_COLOR, 0xFF151515)  // Almost Black
                .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.98f)                  // 2% shrink is enough for feedback
                .set(InteractionState.ACTIVE, ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f));
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Calculate animated values using the Animation Manager
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int txtColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);

        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float scale = getFloat(ThemeProperties.SCALE, state, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 2. Math for Scaling from Center (Zoom effect)
        float scaledW = width * scale;
        float scaledH = height * scale;

        // Calculate the adjusted top-left coordinate for the centered scaled box
        float adjX = x - (scaledW - width) / 2.0f;
        float adjY = y - (scaledH - height) / 2.0f;

        // 4. Draw Main Button Body
        renderer.getGeometry().renderRect(
                adjX, adjY, scaledW, scaledH, bgColor,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
        );

        // 5. Draw Border (if enabled)
        if (borderThick > 0 && (borderColor >>> 24) > 0) {
            renderer.getGeometry().renderOutline(
                    adjX, adjY, scaledW, scaledH, borderColor, borderThick,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
        }

        // 6. Draw Text (Centered relative to the SCALED box)
        int strWidth = TextComponent.getTextWidth(label);
        int strHeight = TextComponent.getFontHeight();

        // Important: Calculate text position based on adjX/adjY so text moves with the scaling
        float textX = adjX + (scaledW - strWidth) / 2.0f;
        float textY = adjY + (scaledH - strHeight) / 2.0f + 1; // +1 often helps with visual font centering

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

    /**
     * Returns the current button label.
     *
     * @return The label TextComponent.
     */
    public TextComponent getLabel() {
        return label;
    }
}
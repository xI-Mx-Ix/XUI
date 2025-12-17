/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * A linear Progress Bar component.
 * <p>
 * Displays the progress of an operation. Features:
 * - Smooth animation between values.
 * - Configurable range (min/max).
 * - Optional text overlay.
 * - Rounded corners support (using Scissor clipping for perfect fill).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIProgressBar extends UIWidget {

    // --- Style Keys ---

    /**
     * The color of the filled portion of the bar.
     */
    public static final StyleKey<Integer> FILL_COLOR = new StyleKey<>("progress_fill_color", 0xFF2196F3); // Material Blue

    /**
     * Internal key to animate the visual progress value (0.0 to 1.0) smoothly.
     */
    private static final StyleKey<Float> VISUAL_PROGRESS = new StyleKey<>("progress_visual_value", 0.0f);

    // --- Fields ---

    private float minValue = 0.0f;
    private float maxValue = 1.0f;
    private float currentValue = 0.0f;

    private boolean showText = true;
    private String customText = null; // If null, shows percentage

    /**
     * Constructs a default progress bar (0.0 to 1.0).
     */
    public UIProgressBar() {
        setupStyles();
    }

    private void setupStyles() {
        this.style()
                .setTransitionSpeed(5.0f) // Slower speed for smoother fill animation

                // Track (Background)
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF303030)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF505050)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))

                // Fill
                .set(InteractionState.DEFAULT, FILL_COLOR, 0xFF2196F3)

                // Text
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Sets the current value.
     * The bar will animate to this value.
     *
     * @param value The new value (clamped between min and max).
     * @return This instance.
     */
    public UIProgressBar setProgress(float value) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, value));
        return this;
    }

    /**
     * Sets the range of the progress bar.
     *
     * @param min Minimum value (0%).
     * @param max Maximum value (100%).
     * @return This instance.
     */
    public UIProgressBar setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        // Re-clamp current value
        this.currentValue = Math.max(minValue, Math.min(maxValue, currentValue));
        return this;
    }

    /**
     * Configures whether to show text on the bar.
     */
    public UIProgressBar setShowText(boolean show) {
        this.showText = show;
        return this;
    }

    /**
     * Sets a custom text to display instead of the percentage.
     * Set to null to revert to percentage.
     */
    public UIProgressBar setCustomText(String text) {
        this.customText = text;
        return this;
    }

    /**
     * Gets the normalized progress (0.0 to 1.0).
     */
    public float getNormalizedProgress() {
        if (maxValue == minValue) return 0.0f;
        return (currentValue - minValue) / (maxValue - minValue);
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Resolve Colors and Geometry
        int trackColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        int fillColor = style().getValue(state, FILL_COLOR);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);

        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 2. Animate Progress
        float targetNorm = getNormalizedProgress();
        float currentNorm = animManager.getAnimatedFloat(VISUAL_PROGRESS, targetNorm, style().getTransitionSpeed(), deltaTime);

        // 3. Draw Track (Background)
        renderer.getGeometry().renderRect(
                x, y, width, height, trackColor,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
        );

        // 4. Draw Fill with Scissor Clipping
        // Instead of shrinking the rect (which distorts corners), we draw the full-size rect
        // but clip it to the current percentage width.
        if (currentNorm > 0.001f) {
            float visibleWidth = width * currentNorm;

            // Enable Scissor: Clip to (x, y, visibleWidth, height)
            renderer.getScissor().enableScissor(x, y, visibleWidth, height);

            // Draw the FULL bar (so the right-side corners exist, but are currently clipped off)
            renderer.getGeometry().renderRect(
                    x, y, width, height, fillColor,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );

            // Disable Scissor
            renderer.getScissor().disableScissor();
        }

        // 5. Draw Border (on top of everything)
        if (borderThick > 0) {
            renderer.getGeometry().renderOutline(
                    x, y, width, height, borderColor, borderThick,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
        }

        // 6. Draw Text
        if (showText) {
            String textToDraw;
            if (customText != null) {
                textToDraw = customText;
            } else {
                int percent = (int) (targetNorm * 100);
                textToDraw = percent + "%";
            }

            TextComponent comp = TextComponent.literal(textToDraw);
            int textW = TextComponent.getTextWidth(comp);
            int textH = TextComponent.getFontHeight();

            float textX = x + (width - textW) / 2.0f;
            float textY = y + (height - textH) / 2.0f + 1;

            renderer.drawText(comp, textX, textY, textColor, true);
        }
    }
}
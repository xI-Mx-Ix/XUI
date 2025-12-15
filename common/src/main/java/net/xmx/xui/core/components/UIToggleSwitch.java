/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.function.Consumer;

/**
 * A modern Toggle Switch component (Pill shape).
 * <p>
 * Mimics mobile OS switches. Features:
 * - Smooth sliding animation for the thumb.
 * - Color interpolation for the track background.
 * - Configurable thumb color and track colors.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIToggleSwitch extends UIWidget {

    // --- Style Keys ---

    /**
     * The background color of the track when the switch is ON.
     */
    public static final StyleKey<Integer> TRACK_ON_COLOR = new StyleKey<>("switch_track_on", 0xFF4CAF50); // Green accent

    /**
     * The color of the sliding thumb (handle).
     */
    public static final StyleKey<Integer> THUMB_COLOR = new StyleKey<>("switch_thumb_color", 0xFFE0E0E0); // Off-White

    /**
     * Internal key to track the animation progress (0.0 = Left/Off, 1.0 = Right/On).
     */
    private static final StyleKey<Float> ANIM_PROGRESS = new StyleKey<>("switch_progress", 0.0f);

    // --- Fields ---

    private TextComponent label;
    private boolean isChecked = false;
    private Consumer<Boolean> onValueChange;

    /**
     * Constructs a toggle switch.
     */
    public UIToggleSwitch() {
        this.label = TextComponent.empty();
        setupStyles();
    }

    /**
     * Configures default styles to match the "Pill" look.
     */
    private void setupStyles() {
        this.style()
                .setTransitionSpeed(12.0f)

                // --- Track Styles (OFF State) ---
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF303030) // Dark Grey Track
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF505050)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, 999.0f)        // Max rounding (Pill shape)
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF)

                // --- Specific Switch Styles ---
                .set(InteractionState.DEFAULT, TRACK_ON_COLOR, 0xFF4CAF50)
                .set(InteractionState.DEFAULT, THUMB_COLOR, 0xFFBBBBBB) // Slightly grey when off

                // --- Hover State ---
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFF808080)
                .set(InteractionState.HOVER, THUMB_COLOR, 0xFFFFFFFF);  // Brighten thumb on hover
    }

    /**
     * Sets the label text (displayed to the right of the switch).
     */
    public UIToggleSwitch setLabel(String text) {
        this.label = TextComponent.literal(text);
        return this;
    }

    /**
     * Sets the label component.
     */
    public UIToggleSwitch setLabel(TextComponent component) {
        this.label = component;
        return this;
    }

    /**
     * Toggles the state programmatically.
     */
    public UIToggleSwitch setChecked(boolean checked) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            if (onValueChange != null) {
                onValueChange.accept(checked);
            }
        }
        return this;
    }

    public boolean isChecked() {
        return isChecked;
    }

    /**
     * Sets a callback for state changes.
     */
    public UIToggleSwitch setOnValueChange(Consumer<Boolean> callback) {
        this.onValueChange = callback;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            setChecked(!isChecked);
            if (onClick != null) onClick.accept(this);
            return true;
        }
        return false;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Calculate Animation Progress (0.0 -> 1.0)
        float targetProgress = isChecked ? 1.0f : 0.0f;
        float currentProgress = animManager.getAnimatedFloat(ANIM_PROGRESS, targetProgress, style().getTransitionSpeed(), deltaTime);

        // 2. Resolve Colors
        // Interpolate Track Background: OFF Color -> ON Color
        int trackOffColor = style().getValue(state, ThemeProperties.BACKGROUND_COLOR);
        int trackOnColor = style().getValue(state, TRACK_ON_COLOR);
        
        // Use AnimationManager to smoothly fade the background color property
        int currentTrackColor = animManager.getAnimatedColor(
                ThemeProperties.BACKGROUND_COLOR, 
                isChecked ? trackOnColor : trackOffColor, 
                style().getTransitionSpeed(), 
                deltaTime
        );

        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        int thumbColor = getColor(THUMB_COLOR, state, deltaTime);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);

        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 3. Geometry (Pill Shape)
        // Ideally, height should be around 20-24px, width around 40-48px.
        // We ensure the radius makes it perfectly round at the ends.
        float pillRadius = height / 2.0f;

        // 4. Draw Track
        renderer.getGeometry().renderRect(x, y, width, height, currentTrackColor, pillRadius);
        if (borderThick > 0) {
            renderer.getGeometry().renderOutline(x, y, width, height, borderColor, pillRadius, borderThick);
        }

        // 5. Draw Thumb (Handle)
        float padding = 3.0f; // Gap between thumb and track edge
        float thumbSize = height - (padding * 2);
        float maxTravel = width - thumbSize - (padding * 2);

        float thumbX = x + padding + (maxTravel * currentProgress);
        float thumbY = y + padding;
        float thumbRadius = thumbSize / 2.0f;

        // Render Thumb Circle
        renderer.getGeometry().renderRect(thumbX, thumbY, thumbSize, thumbSize, thumbColor, thumbRadius);

        // 6. Draw Label
        if (label != null) {
            float textX = x + width + 8; // 8px gap
            float textY = y + (height - TextComponent.getFontHeight()) / 2.0f + 1;
            renderer.drawText(label, textX, textY, textColor, true);
        }
    }
}
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

import java.util.function.Consumer;

/**
 * A horizontal slider component.
 * <p>
 * Allows selecting a numeric value from a range by dragging a thumb along a track.
 * Features:
 * - Configurable range (min/max) and step size.
 * - Distinct active (left) and inactive (right) track colors.
 * - Smooth hover animations for the thumb.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UISlider extends UIWidget {

    // --- Style Keys ---

    /** Color of the active portion of the track (left of thumb). */
    public static final StyleKey<Integer> TRACK_ACTIVE_COLOR = new StyleKey<>("slider_track_active", 0xFFFFFFFF);

    /** Color of the inactive portion of the track (right of thumb). */
    public static final StyleKey<Integer> TRACK_INACTIVE_COLOR = new StyleKey<>("slider_track_inactive", 0xFF404040);

    /** Color of the draggable thumb (handle). */
    public static final StyleKey<Integer> THUMB_COLOR = new StyleKey<>("slider_thumb_color", 0xFFFFFFFF);

    /** The diameter of the thumb in pixels. */
    public static final StyleKey<Float> THUMB_SIZE = new StyleKey<>("slider_thumb_size", 12.0f);

    /** The thickness (height) of the track line in pixels. */
    public static final StyleKey<Float> TRACK_THICKNESS = new StyleKey<>("slider_track_thickness", 2.0f);

    /** Scale multiplier for the thumb when hovered/dragged. */
    public static final StyleKey<Float> THUMB_HOVER_SCALE = new StyleKey<>("slider_thumb_hover_scale", 1.2f);

    // --- Fields ---

    private float minValue = 0.0f;
    private float maxValue = 1.0f;
    private float currentValue = 0.5f;
    private float step = 0.0f; // 0 = continuous

    private boolean isDragging = false;
    private Consumer<Float> onValueChange;

    /**
     * Constructs a default slider (0.0 to 1.0).
     */
    public UISlider() {
        setupStyles();
    }

    private void setupStyles() {
        this.style()
                .setTransitionSpeed(10.0f)
                // Default: White active track, dark grey inactive, white thumb
                .set(InteractionState.DEFAULT, TRACK_ACTIVE_COLOR, 0xFFFFFFFF)
                .set(InteractionState.DEFAULT, TRACK_INACTIVE_COLOR, 0xFF404040)
                .set(InteractionState.DEFAULT, THUMB_COLOR, 0xFFFFFFFF)
                .set(InteractionState.DEFAULT, THUMB_SIZE, 12.0f)
                .set(InteractionState.DEFAULT, TRACK_THICKNESS, 2.0f)
                .set(InteractionState.DEFAULT, THUMB_HOVER_SCALE, 1.0f)
                
                // Hover Effects
                .set(InteractionState.HOVER, THUMB_HOVER_SCALE, 1.2f)
                .set(InteractionState.ACTIVE, THUMB_HOVER_SCALE, 1.1f);
    }

    /**
     * Sets the value range.
     */
    public UISlider setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        setValue(currentValue); // Re-clamp
        return this;
    }

    /**
     * Sets the step size for snapping (e.g., 1.0 for integers).
     * Set to 0.0 for continuous movement.
     */
    public UISlider setStep(float step) {
        this.step = step;
        return this;
    }

    /**
     * Sets the current value programmatically.
     */
    public UISlider setValue(float value) {
        float clamped = Math.max(minValue, Math.min(maxValue, value));
        
        if (step > 0) {
            float steps = Math.round((clamped - minValue) / step);
            clamped = minValue + (steps * step);
        }

        if (this.currentValue != clamped) {
            this.currentValue = clamped;
            if (onValueChange != null) {
                onValueChange.accept(this.currentValue);
            }
        }
        return this;
    }

    public float getValue() {
        return currentValue;
    }

    public UISlider setOnValueChange(Consumer<Float> callback) {
        this.onValueChange = callback;
        return this;
    }

    private void updateValueFromMouse(double mouseX) {
        float relativeX = (float) mouseX - this.x;
        float percent = relativeX / this.width;
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        
        float newValue = minValue + (percent * (maxValue - minValue));
        setValue(newValue);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            isDragging = true;
            isFocused = true; // Retain focus for dragging outside bounds
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            updateValueFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    protected boolean shouldRetainFocus() {
        return true;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Resolve Styles
        // Force HOVER state if dragging to keep the thumb visually active
        InteractionState visualState = isDragging ? InteractionState.ACTIVE : state;

        int activeColor = style().getValue(visualState, TRACK_ACTIVE_COLOR);
        int inactiveColor = style().getValue(visualState, TRACK_INACTIVE_COLOR);
        int thumbColor = style().getValue(visualState, THUMB_COLOR);
        
        float thumbBaseSize = style().getValue(visualState, THUMB_SIZE);
        float trackThickness = style().getValue(visualState, TRACK_THICKNESS);
        float hoverScale = getFloat(THUMB_HOVER_SCALE, visualState, deltaTime);

        // 2. Geometry Calculation
        float centerY = y + height / 2.0f;
        float normalizedValue = (currentValue - minValue) / (maxValue - minValue);
        
        // Visual thumb position (center of the thumb)
        float thumbX = x + (width * normalizedValue);
        
        // 3. Draw Track
        // Inactive part (Full width background, essentially)
        renderer.getGeometry().renderRect(x, centerY - trackThickness / 2.0f, width, trackThickness, inactiveColor, trackThickness / 2.0f);
        
        // Active part (Left side to thumb)
        if (normalizedValue > 0.0f) {
            renderer.getGeometry().renderRect(x, centerY - trackThickness / 2.0f, width * normalizedValue, trackThickness, activeColor, trackThickness / 2.0f);
        }

        // 4. Draw Thumb
        float currentThumbSize = thumbBaseSize * hoverScale;
        float thumbRadius = currentThumbSize / 2.0f;
        
        // Render centered on the calculated value position
        renderer.getGeometry().renderRect(thumbX - thumbRadius, centerY - thumbRadius, currentThumbSize, currentThumbSize, thumbColor, thumbRadius);
    }
}
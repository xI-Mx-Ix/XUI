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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Radio Button component.
 * <p>
 * Used for allowing the user to select exactly one option from a set.
 * To function correctly, multiple {@link UIRadioButton} instances must be assigned
 * to the same {@link RadioGroup}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRadioButton extends UIWidget {

    /**
     * Helper class to manage the mutual exclusion of radio buttons.
     */
    public static class RadioGroup {
        private final List<UIRadioButton> buttons = new ArrayList<>();
        private UIRadioButton selectedButton;
        private Consumer<UIRadioButton> onSelectionChange;

        /**
         * Registers a button to this group.
         * Called automatically by {@link UIRadioButton#setGroup(RadioGroup)}.
         */
        void add(UIRadioButton btn) {
            buttons.add(btn);
        }

        /**
         * Selects the specified button and deselects all others in the group.
         */
        void select(UIRadioButton btn) {
            if (selectedButton == btn) return;

            // Deselect current
            if (selectedButton != null) {
                selectedButton.setInternalSelected(false);
            }

            // Select new
            selectedButton = btn;
            if (selectedButton != null) {
                selectedButton.setInternalSelected(true);
            }

            if (onSelectionChange != null) {
                onSelectionChange.accept(selectedButton);
            }
        }

        /**
         * Sets a callback listener for selection changes.
         */
        public void setOnSelectionChange(Consumer<UIRadioButton> callback) {
            this.onSelectionChange = callback;
        }

        /**
         * Clears the current selection.
         */
        public void clearSelection() {
            select(null);
        }
    }

    // --- Style Keys ---

    /**
     * Color of the inner dot when selected.
     */
    public static final StyleKey<Integer> DOT_COLOR = new StyleKey<>("radio_dot_color", 0xFFFFFFFF);

    /**
     * Background color of the outer circle when selected (active accent).
     */
    public static final StyleKey<Integer> ACCENT_COLOR = new StyleKey<>("radio_accent_color", 0xFF4CAF50);

    /**
     * The scaling factor of the inner dot (0.0 to 1.0).
     * Used internally for animation.
     */
    public static final StyleKey<Float> DOT_SCALE = new StyleKey<>("radio_dot_scale", 0.0f);

    // --- Fields ---

    private TextComponent label;
    private RadioGroup group;
    private boolean isSelected = false;
    private float circleSize = 16.0f; // Diameter of the radio circle

    /**
     * Constructs a radio button with no label.
     */
    public UIRadioButton() {
        this.label = TextComponent.empty();
        setupStyles();
    }

    private void setupStyles() {
        this.style()
                .setTransitionSpeed(15.0f) // Snappy animation

                // Default State (Unchecked)
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0x00000000) // Transparent inner
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF808080)     // Grey ring
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 2.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFAAAAAA)
                .set(InteractionState.DEFAULT, DOT_COLOR, 0xFFFFFFFF)
                .set(InteractionState.DEFAULT, ACCENT_COLOR, 0xFF4CAF50)

                // Hover State
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFFFFFFFF)
                .set(InteractionState.HOVER, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Assigns this button to a {@link RadioGroup}.
     * This is required for mutual exclusion to work.
     *
     * @param group The group to join.
     * @return This instance.
     */
    public UIRadioButton setGroup(RadioGroup group) {
        this.group = group;
        if (group != null) {
            group.add(this);
        }
        return this;
    }

    /**
     * Sets the label text.
     */
    public UIRadioButton setLabel(String text) {
        this.label = TextComponent.literal(text);
        return this;
    }

    /**
     * Sets the label component.
     */
    public UIRadioButton setLabel(TextComponent component) {
        this.label = component;
        return this;
    }

    /**
     * Sets the diameter of the radio circle.
     */
    public UIRadioButton setCircleSize(float size) {
        this.circleSize = size;
        return this;
    }

    /**
     * Checks if this button is currently selected.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Used by the RadioGroup to update state without triggering recursion.
     */
    protected void setInternalSelected(boolean selected) {
        this.isSelected = selected;
    }

    /**
     * Manually selects this button.
     * This will trigger the RadioGroup to update other buttons.
     */
    public void setSelected(boolean selected) {
        if (group != null) {
            if (selected) {
                group.select(this);
            } else if (this.isSelected) {
                // Radio buttons typically cannot be deselected by setting them to false directly,
                // another button must be selected or the group cleared.
                // However, we allow clearing logic here if needed.
                group.select(null);
            }
        } else {
            this.isSelected = selected;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            // Radio interaction: clicking selects this button.
            // Clicking an already selected button does nothing.
            if (!isSelected) {
                if (group != null) {
                    group.select(this);
                } else {
                    this.isSelected = true;
                }
            }
            if (onClick != null) onClick.accept(this);
            return true;
        }
        return false;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Resolve Colors
        int ringColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);
        int dotColor = style().getValue(state, DOT_COLOR);
        int accentColor = style().getValue(state, ACCENT_COLOR);

        // If selected, the ring often changes color to the accent color
        if (isSelected) {
            ringColor = accentColor;
        }

        // 2. Animate Dot Scale
        // We use the AnimationManager to interpolate the DOT_SCALE property.
        // Target is 1.0 if selected, 0.0 if not.
        float targetScale = isSelected ? 1.0f : 0.0f;
        float currentScale = animManager.getAnimatedFloat(DOT_SCALE, targetScale, style().getTransitionSpeed(), deltaTime);

        // 3. Geometry Calculation
        float centerY = y + height / 2.0f;
        float circleX = x + circleSize / 2.0f;
        float radius = circleSize / 2.0f;

        // 4. Draw Outer Ring
        // We use a transparent fill and a colored outline
        float thickness = style().getValue(state, ThemeProperties.BORDER_THICKNESS);
        renderer.getGeometry().renderOutline(x, centerY - radius, circleSize, circleSize, ringColor, radius, thickness);

        // 5. Draw Inner Dot
        if (currentScale > 0.01f) {
            float dotRadius = (radius - 4) * currentScale; // 4px padding between ring and dot
            if (dotRadius > 0) {
                // Draw dot centered in the ring
                renderer.getGeometry().renderRect(
                        circleX - dotRadius,
                        centerY - dotRadius,
                        dotRadius * 2,
                        dotRadius * 2,
                        dotColor,
                        dotRadius
                );
            }
        }

        // 6. Draw Label
        if (label != null) {
            float textX = x + circleSize + 6; // 6px gap
            float textY = y + (height - TextComponent.getFontHeight()) / 2.0f + 1;
            renderer.drawText(label, textX, textY, textColor, true);
        }
    }
}
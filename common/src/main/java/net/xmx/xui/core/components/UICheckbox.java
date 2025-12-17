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

import java.util.function.Consumer;

/**
 * A checkbox component.
 * <p>
 * Displays a toggleable box with a label. The visual style (sharp vs rounded corners)
 * is controlled via {@link ThemeProperties#BORDER_RADIUS}.
 * The inner check mark renders as a filled square that scales in/out.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UICheckbox extends UIWidget {

    // --- Style Keys ---

    /**
     * The color of the inner "check" mark (the filled square).
     */
    public static final StyleKey<Integer> CHECK_COLOR = new StyleKey<>("checkbox_check_color", 0xFF64FFDA); // Cyan/Teal default

    /**
     * The size of the checkbox square in pixels.
     */
    public static final StyleKey<Float> BOX_SIZE = new StyleKey<>("checkbox_size", 14.0f);

    /**
     * Internal key for animating the scale of the check mark (0.0 to 1.0).
     */
    private static final StyleKey<Float> CHECK_SCALE = new StyleKey<>("checkbox_scale", 0.0f);

    // --- Fields ---

    private TextComponent label;
    private boolean isChecked = false;
    private Consumer<Boolean> onValueChange;

    /**
     * Constructs a checkbox with an empty label.
     */
    public UICheckbox() {
        this.label = TextComponent.empty();
        setupStyles();
    }

    /**
     * Configures the default visual properties.
     */
    private void setupStyles() {
        this.style()
                .setTransitionSpeed(15.0f) // Snappy animation

                // Default State (Unchecked)
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0x00000000) // Transparent inner
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF808080)     // Grey border
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 2.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(2.0f)) // Slight rounding
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFAAAAAA)
                .set(InteractionState.DEFAULT, CHECK_COLOR, 0xFF64FFDA)
                .set(InteractionState.DEFAULT, BOX_SIZE, 14.0f)

                // Hover State
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFFFFFFFF)
                .set(InteractionState.HOVER, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Sets the label text.
     *
     * @param text The text string.
     * @return This instance.
     */
    public UICheckbox setLabel(String text) {
        this.label = TextComponent.literal(text);
        return this;
    }

    /**
     * Sets the label component.
     *
     * @param component The text component.
     * @return This instance.
     */
    public UICheckbox setLabel(TextComponent component) {
        this.label = component;
        return this;
    }

    /**
     * Sets the checked state.
     *
     * @param checked The new state.
     * @return This instance.
     */
    public UICheckbox setChecked(boolean checked) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            if (onValueChange != null) {
                onValueChange.accept(checked);
            }
        }
        return this;
    }

    /**
     * @return True if checked.
     */
    public boolean isChecked() {
        return isChecked;
    }

    /**
     * Sets the callback to be executed when the state changes.
     *
     * @param callback Consumer accepting the new boolean state.
     * @return This instance.
     */
    public UICheckbox setOnValueChange(Consumer<Boolean> callback) {
        this.onValueChange = callback;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            // Toggle state
            setChecked(!isChecked);
            // Trigger generic click listener
            if (onClick != null) onClick.accept(this);
            return true;
        }
        return false;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Resolve Styles
        float boxSize = style().getValue(state, BOX_SIZE);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);

        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);
        int checkColor = style().getValue(state, CHECK_COLOR);

        // If checked, we often want the border to match the check color (accent)
        if (isChecked) {
            borderColor = checkColor;
        }

        // 2. Calculate Geometry
        // Center the box vertically within the widget height
        float boxY = y + (height - boxSize) / 2.0f;
        float boxX = x;

        // 3. Draw Outer Box (Container)
        renderer.getGeometry().renderRect(
                boxX, boxY, boxSize, boxSize, bgColor,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
        );

        renderer.getGeometry().renderOutline(
                boxX, boxY, boxSize, boxSize, borderColor, borderThick,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
        );

        // 4. Draw Inner Check (Animated Filled Square)
        // Interpolate scale: 0.0 -> 1.0
        float targetScale = isChecked ? 1.0f : 0.0f;
        float currentScale = animManager.getAnimatedFloat(CHECK_SCALE, targetScale, style().getTransitionSpeed(), deltaTime);

        if (currentScale > 0.01f) {
            // Padding between outer border and inner check
            float padding = 3.0f;
            float maxInnerSize = boxSize - (padding * 2);

            // Calculate current animated size
            float innerSize = maxInnerSize * currentScale;
            float offset = (boxSize - innerSize) / 2.0f;

            // Determine inner radii (slightly smaller than outer radii to nest correctly)
            // Logic: innerRadius = max(0, outerRadius - padding) * scale
            // We apply this to all 4 corners.
            float s = currentScale;
            float innerTL = Math.max(0, radii.topLeft() - 1) * s;
            float innerTR = Math.max(0, radii.topRight() - 1) * s;
            float innerBR = Math.max(0, radii.bottomRight() - 1) * s;
            float innerBL = Math.max(0, radii.bottomLeft() - 1) * s;

            renderer.getGeometry().renderRect(
                    boxX + offset,
                    boxY + offset,
                    innerSize,
                    innerSize,
                    checkColor,
                    innerTL, innerTR, innerBR, innerBL
            );
        }

        // 5. Draw Label
        if (label != null) {
            float textX = boxX + boxSize + 6; // 6px gap
            float textY = y + (height - TextComponent.getFontHeight()) / 2.0f + 1;
            renderer.drawText(label, textX, textY, textColor, true);
        }
    }
}
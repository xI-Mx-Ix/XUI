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
 * A Button that maintains an On/Off state.
 * <p>
 * This widget can operate in two modes based on the {@link #setAllowUntoggle(boolean)} property:
 * 1. <b>Standard Toggle (Default):</b> Clicking toggles the state between ON and OFF.
 * 2. <b>Radio/Tab Mode:</b> Clicking turns the button ON, but clicking it again while it is ON does nothing.
 *    (Useful for tabs or radio groups where the active selection persists).
 * </p>
 * <p>
 * It utilizes the central {@link net.xmx.xui.core.anim.AnimationManager} to smoothly
 * transition colors between the standard state and the toggled state without local interpolation logic.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIToggleButton extends UIWidget {

    // --- Toggled State Style Keys ---

    /**
     * The background color when the button is in the ON state.
     */
    public static final StyleKey<Integer> TOGGLED_BACKGROUND_COLOR = new StyleKey<>("toggled_bg_color", 0xFF4CAF50);

    /**
     * The border color when the button is in the ON state.
     */
    public static final StyleKey<Integer> TOGGLED_BORDER_COLOR = new StyleKey<>("toggled_border_color", 0xFF66BB6A);

    /**
     * The text color when the button is in the ON state.
     */
    public static final StyleKey<Integer> TOGGLED_TEXT_COLOR = new StyleKey<>("toggled_text_color", 0xFFFFFFFF);

    // --- Logic Fields ---

    private TextComponent label;
    private boolean isToggled = false;

    /**
     * Controls whether the user can turn the button OFF by clicking it.
     * Default is true (standard toggle behavior).
     * Set to false for Tab/Radio behavior.
     */
    private boolean allowUntoggle = true;

    private Consumer<Boolean> onToggle;

    /**
     * Constructs a new Toggle Button with an empty label.
     */
    public UIToggleButton() {
        this.label = TextComponent.empty();
        setupToggleStyles();
    }

    /**
     * Configures the default visual properties.
     */
    private void setupToggleStyles() {
        this.style()
                .setTransitionSpeed(10.0f)

                // --- OFF State (Standard Button Look) ---
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f))
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFAAAAAA)
                .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)

                // --- Hover State ---
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFF353535)
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFF606060)
                .set(InteractionState.HOVER, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF)
                .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.02f)

                // --- Active (Click) State ---
                .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.98f)

                // --- ON State (Toggled Colors) ---
                // We define these in DEFAULT state, but they could also have HOVER overrides if desired.
                .set(InteractionState.DEFAULT, TOGGLED_BACKGROUND_COLOR, 0xFF2E7D32) // Greenish
                .set(InteractionState.DEFAULT, TOGGLED_BORDER_COLOR, 0xFF4CAF50)     // Lighter Green
                .set(InteractionState.DEFAULT, TOGGLED_TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Sets the display label.
     *
     * @param label The text component.
     * @return This instance.
     */
    public UIToggleButton setLabel(TextComponent label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the display label string.
     *
     * @param label The text string.
     * @return This instance.
     */
    public UIToggleButton setLabel(String label) {
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

    /**
     * Configures the toggle behavior.
     *
     * @param allowUntoggle If true (default), clicking an active button turns it OFF.
     *                      If false, clicking an active button does nothing (Radio/Tab behavior).
     * @return This instance.
     */
    public UIToggleButton setAllowUntoggle(boolean allowUntoggle) {
        this.allowUntoggle = allowUntoggle;
        return this;
    }

    /**
     * Sets the toggle state programmatically.
     *
     * @param toggled The new state.
     * @return This instance.
     */
    public UIToggleButton setToggled(boolean toggled) {
        if (this.isToggled != toggled) {
            this.isToggled = toggled;
            if (onToggle != null) {
                onToggle.accept(toggled);
            }
        }
        return this;
    }

    /**
     * @return True if the button is currently ON.
     */
    public boolean isToggled() {
        return isToggled;
    }

    /**
     * Sets the callback to be executed when the state changes.
     *
     * @param onToggle Consumer accepting the new boolean state.
     * @return This instance.
     */
    public UIToggleButton setOnToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            // Visual feedback via focus/active state
            isFocused = true;

            // Logic Handling
            if (isToggled) {
                // Button is ON. Can we turn it OFF?
                if (allowUntoggle) {
                    setToggled(false);
                }
            } else {
                // Button is OFF. Always turn ON.
                setToggled(true);
            }

            // Trigger generic click listener if present (e.g. sound effects)
            if (onClick != null) onClick.accept(this);
            return true;
        }

        return false;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Resolve Target Colors
        // Depending on the toggle state, we choose which StyleKeys act as the "Target".
        int targetBg;
        int targetBorder;
        int targetText;

        if (isToggled) {
            // If ON, we want to animate towards the Toggled colors
            targetBg = style().getValue(state, TOGGLED_BACKGROUND_COLOR);
            targetBorder = style().getValue(state, TOGGLED_BORDER_COLOR);
            targetText = style().getValue(state, TOGGLED_TEXT_COLOR);
        } else {
            // If OFF, we animate towards the standard ThemeProperties (which respect Hover/Active states)
            targetBg = style().getValue(state, ThemeProperties.BACKGROUND_COLOR);
            targetBorder = style().getValue(state, ThemeProperties.BORDER_COLOR);
            targetText = style().getValue(state, ThemeProperties.TEXT_COLOR);
        }

        // 2. Delegate Interpolation to AnimationManager
        // We use the AnimationManager to smoothly transition the displayed color to the Target.
        float speed = style().getTransitionSpeed();

        int finalBg = animManager.getAnimatedColor(ThemeProperties.BACKGROUND_COLOR, targetBg, speed, deltaTime);
        int finalBorder = animManager.getAnimatedColor(ThemeProperties.BORDER_COLOR, targetBorder, speed, deltaTime);
        int finalText = animManager.getAnimatedColor(ThemeProperties.TEXT_COLOR, targetText, speed, deltaTime);

        // 3. Resolve Geometry and Scale
        // These are handled normally via the standard animation manager logic for hover/active effects
        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float scale = getFloat(ThemeProperties.SCALE, state, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 4. Calculate Scale Transformation (Center Zoom)
        float scaledW = width * scale;
        float scaledH = height * scale;
        float adjX = x - (scaledW - width) / 2.0f;
        float adjY = y - (scaledH - height) / 2.0f;

        // 5. Draw Body
        renderer.getGeometry().renderRect(
                adjX, adjY, scaledW, scaledH, finalBg,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
        );

        // 6. Draw Border
        if (borderThick > 0) {
            renderer.getGeometry().renderOutline(
                    adjX, adjY, scaledW, scaledH, finalBorder, borderThick,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
        }

        // 7. Draw Text (Centered)
        if (label != null) {
            int strWidth = TextComponent.getTextWidth(label);
            int strHeight = TextComponent.getFontHeight();

            float textX = x + (width - strWidth) / 2.0f;
            float textY = y + (height - strHeight) / 2.0f + 1;

            renderer.drawText(label, textX, textY, finalText, true);
        }
    }
}
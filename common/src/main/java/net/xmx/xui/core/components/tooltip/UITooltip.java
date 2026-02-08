/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.tooltip;

import net.xmx.xui.core.style.InteractionState;

/**
 * A standard, non-interactive tooltip.
 * <p>
 * This tooltip is designed for purely visual information. It does <b>not</b> allow
 * interaction with its content (buttons inside it will not work).
 * </p>
 * <p>
 * Because it is non-interactive, it supports the {@link TooltipAnchor#MOUSE} anchor,
 * allowing it to follow the cursor.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UITooltip extends UIAbstractTooltip {

    /**
     * Constructs a new simple tooltip.
     * Defaults to {@link TooltipAnchor#MOUSE}.
     */
    public UITooltip() {
        super();
        this.anchor = TooltipAnchor.MOUSE;
    }

    /**
     * Updates the internal state machine.
     * <p>
     * Logic: The tooltip is visible ONLY if the mouse is strictly over the target.
     * Once the mouse leaves the target, the tooltip begins to fade out immediately.
     * </p>
     *
     * @param dt     Delta time in seconds.
     * @param mouseX Global mouse X.
     * @param mouseY Global mouse Y.
     */
    @Override
    protected void updateLogic(float dt, int mouseX, int mouseY) {
        boolean targetHovered = target.isHovered();

        float delay = style().getValue(InteractionState.DEFAULT, SHOW_DELAY);
        float fadeIn = style().getValue(InteractionState.DEFAULT, FADE_IN_TIME);
        float fadeOut = style().getValue(InteractionState.DEFAULT, FADE_OUT_TIME);

        switch (state) {
            case HIDDEN -> {
                if (targetHovered) {
                    state = VisibilityState.WAITING_FOR_DELAY;
                    stateTimer = 0.0f;
                }
            }
            case WAITING_FOR_DELAY -> {
                if (!targetHovered) {
                    state = VisibilityState.HIDDEN;
                } else {
                    stateTimer += dt;
                    if (stateTimer >= delay) {
                        this.layout();
                        this.calculatePosition(mouseX, mouseY);

                        state = VisibilityState.FADING_IN;
                        stateTimer = 0.0f;
                        if (onShow != null) onShow.accept(this);
                    }
                }
            }
            case FADING_IN -> {
                if (!targetHovered) {
                    state = VisibilityState.FADING_OUT;
                    stateTimer = (1.0f - currentOpacity) * fadeOut;
                } else {
                    stateTimer += dt;
                    currentOpacity = Math.min(1.0f, stateTimer / fadeIn);
                    if (currentOpacity >= 1.0f) {
                        state = VisibilityState.VISIBLE;
                        currentOpacity = 1.0f;
                    }
                }
            }
            case VISIBLE -> {
                if (!targetHovered) {
                    state = VisibilityState.FADING_OUT;
                    stateTimer = 0.0f;
                }
            }
            case FADING_OUT -> {
                if (targetHovered) {
                    state = VisibilityState.FADING_IN;
                    stateTimer = currentOpacity * fadeIn;
                } else {
                    stateTimer += dt;
                    currentOpacity = Math.max(0.0f, 1.0f - (stateTimer / fadeOut));
                    if (currentOpacity <= 0.0f) {
                        state = VisibilityState.HIDDEN;
                        currentOpacity = 0.0f;
                        if (onHide != null) onHide.accept(this);
                    }
                }
            }
        }
    }

    // =================================================================================
    // Input Blocking
    // =================================================================================

    /**
     * Prevents any interaction with children components.
     * Simple tooltips are "phantom" widgets regarding input.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }
}
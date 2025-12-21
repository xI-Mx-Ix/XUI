/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.tooltip;

import net.xmx.xui.core.style.InteractionState;

/**
 * An advanced tooltip that allows user interaction with its content.
 * <p>
 * Unlike {@link UITooltip}, this widget allows the user to move the mouse cursor
 * from the target widget onto the tooltip itself without it disappearing.
 * This is useful for tooltips containing buttons, scroll areas, or selectable text.
 * </p>
 * <p>
 * <b>Constraint:</b> This tooltip CANNOT use {@link TooltipAnchor#MOUSE}.
 * It must be anchored to a static side (e.g., RIGHT, BOTTOM) to ensure a stable
 * path for the mouse to travel.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIInteractiveTooltip extends UIAbstractTooltip {

    /**
     * Time in seconds the tooltip stays open after the mouse leaves the target/tooltip area.
     * Allows the user to traverse the gap between target and tooltip.
     */
    private static final float GRACE_PERIOD = 0.25f;

    private float graceTimer = 0.0f;
    private boolean isGraceActive = false;

    /**
     * Constructs a new interactive tooltip.
     * Defaults to {@link TooltipAnchor#RIGHT}.
     */
    public UIInteractiveTooltip() {
        super();
        // Default anchor is RIGHT to allow easy sliding
        this.anchor = TooltipAnchor.RIGHT;
    }

    /**
     * Overrides the setter to forbid Mouse anchoring.
     */
    @Override
    public UIAbstractTooltip setAnchor(TooltipAnchor anchor) {
        if (anchor == TooltipAnchor.MOUSE) {
            throw new IllegalArgumentException("UIInteractiveTooltip cannot use MOUSE anchor. Use a static anchor (TOP, BOTTOM, LEFT, RIGHT).");
        }
        return super.setAnchor(anchor);
    }

    /**
     * Logic:
     * 1. If mouse is over Target OR over Tooltip (self) -> Show / Keep Open.
     * 2. If mouse leaves both -> Start Grace Timer.
     * 3. If mouse returns within Grace Period -> Reset Timer, Keep Open.
     * 4. If Grace Timer expires -> Fade Out.
     */
    @Override
    protected void updateLogic(float dt, int mouseX, int mouseY) {
        boolean isHoveringTarget = target.isMouseOver(mouseX, mouseY);
        // We can check isMouseOver on self because calculatePosition updates our layout rect every frame
        boolean isHoveringSelf = this.isMouseOver(mouseX, mouseY);
        boolean isSafe = isHoveringTarget || isHoveringSelf;

        float delay = style().getValue(InteractionState.DEFAULT, SHOW_DELAY);
        float fadeIn = style().getValue(InteractionState.DEFAULT, FADE_IN_TIME);
        float fadeOut = style().getValue(InteractionState.DEFAULT, FADE_OUT_TIME);

        switch (state) {
            case HIDDEN -> {
                if (isHoveringTarget) {
                    state = VisibilityState.WAITING_FOR_DELAY;
                    stateTimer = 0.0f;
                    isGraceActive = false;
                }
            }
            case WAITING_FOR_DELAY -> {
                if (!isHoveringTarget) {
                    state = VisibilityState.HIDDEN;
                } else {
                    stateTimer += dt;
                    if (stateTimer >= delay) {
                        state = VisibilityState.FADING_IN;
                        stateTimer = 0.0f;
                        if (onShow != null) onShow.accept(this);
                    }
                }
            }
            case FADING_IN -> {
                if (!isSafe) {
                    // Immediate fade out if we leave during fade-in
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
                if (isSafe) {
                    // We are safe, reset grace logic
                    isGraceActive = false;
                    graceTimer = 0.0f;
                } else {
                    // We left the safe zone
                    if (!isGraceActive) {
                        isGraceActive = true;
                        graceTimer = 0.0f;
                    } else {
                        graceTimer += dt;
                        if (graceTimer >= GRACE_PERIOD) {
                            state = VisibilityState.FADING_OUT;
                            stateTimer = 0.0f;
                            isGraceActive = false;
                        }
                    }
                }
            }
            case FADING_OUT -> {
                if (isSafe) {
                    // User returned! Resume Fade In
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
}
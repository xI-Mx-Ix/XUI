/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.tooltip;

/**
 * Defines the positioning strategy for a tooltip relative to its target widget.
 *
 * @author xI-Mx-Ix
 */
public enum TooltipAnchor {
    /**
     * The tooltip follows the mouse cursor.
     * <p>
     * <b>Note:</b> This is generally incompatible with {@link UIInteractiveTooltip}
     * as the movement makes it difficult to interact with the tooltip content.
     * </p>
     */
    MOUSE,

    /**
     * Positions the tooltip centered above the target.
     */
    TOP,

    /**
     * Positions the tooltip centered below the target.
     */
    BOTTOM,

    /**
     * Positions the tooltip to the left of the target.
     */
    LEFT,

    /**
     * Positions the tooltip to the right of the target.
     */
    RIGHT,

    /**
     * Positions the tooltip exactly in the center of the target.
     */
    CENTER
}
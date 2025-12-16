/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.scroll;

/**
 * Defines the physical orientation of a scrollbar or a scrolling interaction axis.
 * <p>
 * This enum is used by {@link UIScrollBar} to determine how to render itself
 * and which axis of the {@link UIScrollComponent} it controls.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public enum ScrollOrientation {
    
    /** 
     * Represents a vertical orientation (Top to Bottom).
     * Used for scrolling the Y-axis.
     */
    VERTICAL,

    /** 
     * Represents a horizontal orientation (Left to Right).
     * Used for scrolling the X-axis.
     */
    HORIZONTAL
}
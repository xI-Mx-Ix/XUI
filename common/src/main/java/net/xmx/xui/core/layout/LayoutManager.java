/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.layout;

import net.xmx.xui.core.components.UIPanel;

/**
 * Defines a strategy for arranging child widgets within a parent container.
 * <p>
 * Implementations of this interface calculate and apply position (X, Y) and
 * dimension (Width, Height) constraints to the children of the provided panel.
 * This runs during the parent's layout pass, before the children calculate their own layout.
 * </p>
 *
 * @author xI-Mx-Ix
 */
@FunctionalInterface
public interface LayoutManager {

    /**
     * Applies layout logic to the children of the given panel.
     *
     * @param panel The parent container whose children need arranging.
     */
    void arrange(UIPanel panel);
}
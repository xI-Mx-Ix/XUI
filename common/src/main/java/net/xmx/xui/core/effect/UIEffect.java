/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.effect;

import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;

/**
 * Represents a visual effect applied to a widget and its children during rendering.
 * Effects can manipulate the OpenGL state (Scissors, Stencils, Blend modes) or
 * alter how the widget is presented.
 *
 * Effects follow a stack-based approach where {@link #apply} is called before the
 * widget renders, and {@link #revert} is called after the widget and its children
 * have finished rendering to restore the state.
 *
 * @author xI-Mx-Ix
 */
public interface UIEffect {

    /**
     * Applied before the widget draws itself.
     * Use this to enable GL states, set scissors, or start stencil masks.
     *
     * @param renderer The render interface.
     * @param widget   The widget being rendered.
     */
    void apply(UIRenderInterface renderer, UIWidget widget);

    /**
     * Applied after the widget and its children have finished rendering.
     * Used to restore state (pop scissors, disable stencil).
     *
     * @param renderer The render interface.
     * @param widget   The widget being rendered.
     */
    void revert(UIRenderInterface renderer, UIWidget widget);
}
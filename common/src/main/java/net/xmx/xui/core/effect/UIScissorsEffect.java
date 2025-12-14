/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.effect;

import net.xmx.xui.core.components.UIScrollPanel;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.ScissorManager;
import net.xmx.xui.core.gl.renderer.UIRenderer;

/**
 * An effect that limits the rendering area to the bounds of the widget.
 * <p>
 * Any content (including children) rendered outside the widget's logical bounds
 * will be clipped by the GPU.
 * </p>
 * <p>
 * <b>Implementation Note:</b><br>
 * This class has been simplified to delegate all calculation logic to the
 * {@link ScissorManager}. The manager is responsible
 * for handling:
 * <ul>
 *     <li>Coordinate translation (scrolling offsets from {@link UIScrollPanel}).</li>
 *     <li>Intersection with parent scissor regions (nested clipping).</li>
 *     <li>Physical pixel scaling.</li>
 * </ul>
 * This separation of concerns prevents synchronization bugs where the effect
 * clips based on logical position while the render view is visually shifted.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScissorsEffect implements UIEffect {

    /**
     * Enables the scissor test for the widget's current bounds.
     *
     * @param renderer The renderer instance.
     * @param widget   The widget requesting clipping.
     */
    @Override
    public void apply(UIRenderer renderer, UIWidget widget) {
        // We simply pass the logical bounds. The ScissorManager will automatically
        // add the current ModelView translation (scroll offset) to x/y.
        renderer.enableScissor(
                widget.getX(),
                widget.getY(),
                widget.getWidth(),
                widget.getHeight()
        );
    }

    /**
     * Disables the scissor test (pops the stack).
     *
     * @param renderer The renderer instance.
     * @param widget   The widget requesting clipping.
     */
    @Override
    public void revert(UIRenderer renderer, UIWidget widget) {
        renderer.disableScissor();
    }
}
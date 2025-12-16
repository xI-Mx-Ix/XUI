/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.effect;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.ScissorManager;
import net.xmx.xui.core.gl.renderer.UIRenderer;

/**
 * A visual effect that restricts rendering to the boundaries of the host widget.
 * <p>
 * When applied, any content (including child widgets) that extends outside the
 * widget's dimensions will be clipped by the GPU.
 * </p>
 * <p>
 * <b>Implementation Details:</b><br>
 * This class acts as a high-level bridge to the {@link ScissorManager}.
 * It does not perform coordinate math itself. Instead, it passes the widget's
 * logical bounds to the manager, which handles:
 * <ul>
 *     <li>Applying the current scroll/transform matrix.</li>
 *     <li>Intersecting with any existing parent scissors.</li>
 *     <li>Scaling to physical display pixels.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScissorsEffect implements UIEffect {

    /**
     * Activates the scissor test for the widget's area.
     *
     * @param renderer The active renderer instance.
     * @param widget   The widget defining the clipping bounds.
     */
    @Override
    public void apply(UIRenderer renderer, UIWidget widget) {
        // Delegate to ScissorManager. The manager handles scroll offsets automatically
        // via the renderer's transform stack.
        renderer.getScissor().enableScissor(
                widget.getX(),
                widget.getY(),
                widget.getWidth(),
                widget.getHeight()
        );
    }

    /**
     * Deactivates the scissor test, restoring the previous state.
     *
     * @param renderer The active renderer instance.
     * @param widget   The widget defining the clipping bounds.
     */
    @Override
    public void revert(UIRenderer renderer, UIWidget widget) {
        renderer.getScissor().disableScissor();
    }
}
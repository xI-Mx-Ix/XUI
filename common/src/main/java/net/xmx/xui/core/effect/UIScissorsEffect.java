/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.effect;

import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;

/**
 * An effect that limits the rendering area to the bounds of the widget.
 * Any content (including children) outside the widget's dimensions will be clipped.
 * <p>
 * This implementation handles nested scissor regions (e.g., a scroll panel inside another
 * clipped area) by calculating the intersection of the current scissor rectangle and
 * the widget's bounds.
 * </p>
 * <p>
 * It uses floating-point coordinates to support sub-pixel positioning (e.g., centered layouts),
 * ensuring that the clipping area matches the widget's visual border exactly.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScissorsEffect implements UIEffect {

    /**
     * Applies the scissor test.
     * Calculates the intersection between the widget's bounds and any existing parent scissor rect.
     *
     * @param renderer The render interface.
     * @param widget   The widget being rendered.
     */
    @Override
    public void apply(UIRenderInterface renderer, UIWidget widget) {
        // Use floats to preserve precision (e.g. 10.5 for centering)
        float targetX = widget.getX();
        float targetY = widget.getY();
        float targetW = widget.getWidth();
        float targetH = widget.getHeight();

        // Check if there is already an active scissor (from a parent)
        float[] currentScissor = renderer.getCurrentScissor();

        if (currentScissor != null) {
            // Calculate intersection of the parent scissor and this widget's bounds
            float pX = currentScissor[0];
            float pY = currentScissor[1];
            float pW = currentScissor[2];
            float pH = currentScissor[3];

            // The new top-left is the maximum of the two X/Y coordinates
            float newX = Math.max(targetX, pX);
            float newY = Math.max(targetY, pY);

            // Calculate bottom-right points to determine new width/height
            float newRight = Math.min(targetX + targetW, pX + pW);
            float newBottom = Math.min(targetY + targetH, pY + pH);

            // Ensure dimensions are non-negative
            float newW = Math.max(0, newRight - newX);
            float newH = Math.max(0, newBottom - newY);

            renderer.enableScissor(newX, newY, newW, newH);
        } else {
            // No parent scissor active, just set the widget's bounds
            renderer.enableScissor(targetX, targetY, targetW, targetH);
        }
    }

    /**
     * Reverts the scissor state.
     * This pops the current scissor rectangle from the stack in the renderer.
     *
     * @param renderer The render interface.
     * @param widget   The widget being rendered.
     */
    @Override
    public void revert(UIRenderInterface renderer, UIWidget widget) {
        // Disabling the scissor restores the previous state from the stack in the renderer
        renderer.disableScissor();
    }
}
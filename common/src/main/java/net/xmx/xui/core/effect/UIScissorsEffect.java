/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.effect;

import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;

/**
 * An effect that limits the rendering area to the bounds of the widget.
 * Any content (including children) outside the widget's dimensions will be clipped.
 *
 * This implementation automatically handles intersection with existing parent scissor
 * rectangles, ensuring that nested scroll panels or clipped areas behave correctly.
 *
 * @author xI-Mx-Ix
 */
public class UIScissorsEffect implements UIEffect {

    @Override
    public void apply(UIRenderInterface renderer, UIWidget widget) {
        int targetX = (int) widget.getX();
        int targetY = (int) widget.getY();
        int targetW = (int) widget.getWidth();
        int targetH = (int) widget.getHeight();

        // Check if there is already an active scissor (from a parent)
        int[] currentScissor = renderer.getCurrentScissor();

        if (currentScissor != null) {
            // Calculate intersection of the parent scissor and this widget's bounds
            int pX = currentScissor[0];
            int pY = currentScissor[1];
            int pW = currentScissor[2];
            int pH = currentScissor[3];

            // The new top-left is the maximum of the two X/Y coordinates
            int newX = Math.max(targetX, pX);
            int newY = Math.max(targetY, pY);

            // Calculate bottom-right points to determine new width/height
            int newRight = Math.min(targetX + targetW, pX + pW);
            int newBottom = Math.min(targetY + targetH, pY + pH);

            // Ensure dimensions are non-negative
            int newW = Math.max(0, newRight - newX);
            int newH = Math.max(0, newBottom - newY);

            renderer.enableScissor(newX, newY, newW, newH);
        } else {
            // No parent scissor active, just set the widget's bounds
            renderer.enableScissor(targetX, targetY, targetW, targetH);
        }
    }

    @Override
    public void revert(UIRenderInterface renderer, UIWidget widget) {
        // Disabling the scissor restores the previous state from the stack in the renderer
        renderer.disableScissor();
    }
}
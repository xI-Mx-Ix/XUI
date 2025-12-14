/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.style.InteractionState;

/**
 * A generic container component (Panel).
 * It supports a background color, rounded corners, and a configurable border.
 * It is capable of holding children widgets.
 *
 * @author xI-Mx-Ix
 */
public class UIPanel extends UIWidget {

    /**
     * Constructs a panel with default transparent black background.
     */
    public UIPanel() {
        // Set default styles for a panel
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xAA000000) // Default transparent black
                .set(ThemeProperties.BORDER_COLOR, 0xFFFFFFFF)     // Default white border
                .set(ThemeProperties.BORDER_THICKNESS, 0f)         // No border by default
                .set(ThemeProperties.BORDER_RADIUS, 0f);
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        float radius = getFloat(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float thickness = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // 1. Draw the Border (if thickness > 0)
        if (thickness > 0 && (borderColor >>> 24) > 0) {
            renderer.getGeometry().renderOutline(x, y, width, height, borderColor, radius, thickness);
        }

        // 2. Draw the Background
        // Note: The background is drawn inside the border if transparent,
        // or fully covering the area. Standard panels usually draw background over the full area
        // and border on top/inset. Our render implementation handles the border as an inset frame,
        // so we can draw the background safely.

        // To prevent the background from overlapping the border antialiasing or transparency weirdly,
        // we usually draw the background slightly smaller if the border is very thick, 
        // but for standard UI, drawing the full background rect underneath is standard.
        if ((bgColor >>> 24) > 0) {
            // Adjust background size slightly to fit inside the border if desired,
            // or just draw the full rect behind.
            // Here we draw the full rect.
            renderer.getGeometry().renderRect(x + thickness, y + thickness, width - (thickness * 2), height - (thickness * 2), bgColor, Math.max(0, radius - thickness));
        }
    }
}
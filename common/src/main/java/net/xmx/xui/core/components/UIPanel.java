/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;

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
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.ZERO);
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        float thickness = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // Retrieve animated corner radii
        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);

        // 1. Draw the Border (if thickness > 0)
        if (thickness > 0 && (borderColor >>> 24) > 0) {
            renderer.getGeometry().renderOutline(
                    x, y, width, height,
                    borderColor, thickness,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
        }

        // 2. Draw the Background
        if ((bgColor >>> 24) > 0) {
            // Draw background. To prevent overlap issues with thick semi-transparent borders,
            // one might indent by thickness. Here we draw standard full rect.
            // We calculate inner radii to ensure the background fits inside the border if border is present.
            // Formula: max(0, outerRadius - thickness)

            float innerTL = Math.max(0, radii.topLeft() - thickness);
            float innerTR = Math.max(0, radii.topRight() - thickness);
            float innerBR = Math.max(0, radii.bottomRight() - thickness);
            float innerBL = Math.max(0, radii.bottomLeft() - thickness);

            renderer.getGeometry().renderRect(
                    x + thickness,
                    y + thickness,
                    width - (thickness * 2),
                    height - (thickness * 2),
                    bgColor,
                    innerTL, innerTR, innerBR, innerBL
            );
        }
    }
}
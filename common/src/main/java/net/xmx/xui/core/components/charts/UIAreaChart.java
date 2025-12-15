/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * An Area Chart component.
 * Fills the area under the curve.
 *
 * @author xI-Mx-Ix
 */
public class UIAreaChart extends UILineChart {

    public static final StyleKey<Integer> AREA_COLOR = new StyleKey<>("chart_area_color", 0x402196F3);

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // Draw Background
        int bg = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        renderer.getGeometry().renderRect(x, y, width, height, bg, 0);
        
        drawAxes(renderer, style().getValue(state, AXIS_COLOR), style().getValue(state, GRID_COLOR));

        if (dataPoints.size() < 2) return;

        int areaColor = style().getValue(state, AREA_COLOR);
        float stepX = width / (dataPoints.size() - 1);

        // Render Area using vertical strips (Quads)
        // This is an approximation. Ideally we would use a Polygon, but UIRenderer uses Rects.
        // We draw thin columns to approximate the slope.
        int resolution = 2; // Pixel width per strip
        
        for (int i = 0; i < dataPoints.size() - 1; i++) {
            float v1 = normalize(dataPoints.get(i));
            float v2 = normalize(dataPoints.get(i + 1));
            
            float xStart = x + (i * stepX);
            float dist = stepX;
            
            // Interpolate between v1 and v2 across 'dist'
            for (float d = 0; d < dist; d += resolution) {
                float fraction = d / dist;
                float currentVal = v1 + (v2 - v1) * fraction;
                float currentH = height * currentVal;
                
                renderer.getGeometry().renderRect(
                    xStart + d, 
                    y + height - currentH, 
                    resolution, 
                    currentH, 
                    areaColor, 
                    0
                );
            }
        }

        // Draw the line on top
        super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);
    }
}
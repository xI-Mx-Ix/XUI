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
 * A Bar Chart component.
 *
 * @author xI-Mx-Ix
 */
public class UIBarChart extends AbstractChart {

    public static final StyleKey<Integer> BAR_COLOR = new StyleKey<>("chart_bar_color", 0xFF4CAF50);
    public static final StyleKey<Float> BAR_GAP = new StyleKey<>("chart_bar_gap", 2.0f);

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // Draw Background
        int bg = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        renderer.getGeometry().renderRect(x, y, width, height, bg, 0);

        drawAxes(renderer, style().getValue(state, AXIS_COLOR), style().getValue(state, GRID_COLOR));

        if (dataPoints.isEmpty()) return;

        int barColor = style().getValue(state, BAR_COLOR);
        float gap = style().getValue(state, BAR_GAP);
        
        float availableWidth = width - (gap * (dataPoints.size() + 1));
        float barWidth = availableWidth / dataPoints.size();

        for (int i = 0; i < dataPoints.size(); i++) {
            float value = dataPoints.get(i);
            float normalized = normalize(value); // 0.0 to 1.0

            float barHeight = height * normalized;
            float barX = x + gap + (i * (barWidth + gap));
            float barY = y + height - barHeight - 1; // -1 to sit on axis

            // Hover effect
            boolean hovered = mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barY && mouseY <= y + height;
            int color = hovered ? 0xFFFFFFFF : barColor;

            renderer.getGeometry().renderRect(barX, barY, barWidth, barHeight, color, 2.0f);
        }
    }
}
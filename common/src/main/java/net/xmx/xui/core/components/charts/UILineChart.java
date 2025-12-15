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
 * A Line Chart component.
 *
 * @author xI-Mx-Ix
 */
public class UILineChart extends AbstractChart {

    public static final StyleKey<Integer> LINE_COLOR = new StyleKey<>("chart_line_color", 0xFF2196F3);
    public static final StyleKey<Float> LINE_THICKNESS = new StyleKey<>("chart_line_thickness", 2.0f);

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int bg = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        renderer.getGeometry().renderRect(x, y, width, height, bg, 0);
        drawAxes(renderer, style().getValue(state, AXIS_COLOR), style().getValue(state, GRID_COLOR));

        if (dataPoints.size() < 2) return;

        int lineColor = style().getValue(state, LINE_COLOR);
        float thickness = style().getValue(state, LINE_THICKNESS);
        float stepX = width / (dataPoints.size() - 1);

        // Draw segments
        for (int i = 0; i < dataPoints.size() - 1; i++) {
            float v1 = dataPoints.get(i);
            float v2 = dataPoints.get(i + 1);

            float x1 = x + (i * stepX);
            float y1 = y + height - (height * normalize(v1));
            float x2 = x + ((i + 1) * stepX);
            float y2 = y + height - (height * normalize(v2));

            drawLine(renderer, x1, y1, x2, y2, thickness, lineColor);
            
            // Draw Point
            renderer.getGeometry().renderRect(x1 - 2, y1 - 2, 4, 4, lineColor, 2);
        }
        // Last point
        float lastVal = dataPoints.get(dataPoints.size() - 1);
        float lastX = x + width;
        float lastY = y + height - (height * normalize(lastVal));
        renderer.getGeometry().renderRect(lastX - 2, lastY - 2, 4, 4, lineColor, 2);
    }

    /**
     * Helper to draw a rotated line using a rectangle.
     */
    private void drawLine(UIRenderer renderer, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        renderer.pushMatrix();
        renderer.translate(x1, y1, 0);
        renderer.rotate(angle, 0, 0, 1);
        // Draw rect from (0, -thickness/2) to (length, thickness/2)
        renderer.getGeometry().renderRect(0, -thickness / 2.0f, length, thickness, color, 0);
        renderer.popMatrix();
    }
}
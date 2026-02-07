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
 * A Bar Chart component optimized for real-time history data.
 * <p>
 * This class uses a primitive ring buffer to avoid GC pressure.
 * It renders bars with a fixed width based on the chart's total capacity,
 * ensuring a consistent look from the first data point added.
 * </p>
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

        synchronized (dataLock) {
            if (bufferSize == 0) return;

            performAutoScaling();

            int barColor = style().getValue(state, BAR_COLOR);
            float gap = style().getValue(state, BAR_GAP);

            // Calculate bar width based on MAXIMUM CAPACITY to ensure consistent bar sizes
            // even when only a few data points are present in the buffer.
            int divisor = (capacity > 0) ? capacity : bufferSize;

            float availableWidth = width - (gap * (divisor + 1));
            float barWidth = availableWidth / divisor;

            // Fallback for high-density charts: Disable gaps if bars would be too thin
            if (barWidth < 1.0f) {
                gap = 0;
                barWidth = width / divisor;
            }

            for (int i = 0; i < bufferSize; i++) {
                float value = getValueAt(i);
                float normalized = normalize(value);

                float barHeight = height * normalized;
                float barX = x + gap + (i * (barWidth + gap));
                float barY = y + height - barHeight - 1; // Sit on the axis

                // Simple AABB hit testing for hover highlights
                boolean hovered = mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barY && mouseY <= y + height;
                int color = hovered ? 0xFFFFFFFF : barColor;

                // Render with slightly rounded corners if space permits
                renderer.getGeometry().renderRect(barX, barY, Math.max(barWidth, 0.5f), barHeight, color, barWidth > 4 ? 2.0f : 0);
            }
        }
    }
}
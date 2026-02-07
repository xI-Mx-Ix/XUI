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
 * A Line Chart component optimized for real-time signals.
 * <p>
 * Features efficient rendering using downsampling when the dataset size
 * exceeds the screen pixel width.
 * </p>
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

        // Draw Axes before locking (visual layering)
        drawAxes(renderer, style().getValue(state, AXIS_COLOR), style().getValue(state, GRID_COLOR));

        int lineColor = style().getValue(state, LINE_COLOR);
        float thickness = style().getValue(state, LINE_THICKNESS);

        synchronized (dataLock) {
            if (bufferSize < 2) return;

            performAutoScaling();

            // --- Adaptive Rendering Strategy ---

            // If we have more data points than pixels on the screen, drawing every line segment
            // creates aliasing and kills performance. We switch to "Min-Max Column" rendering.
            boolean highDensity = bufferSize > width;

            if (highDensity) {
                drawHighDensity(renderer, lineColor);
            } else {
                drawStandardLine(renderer, lineColor, thickness);
            }

            // Draw Indicator for the latest value
            float lastVal = getValueAt(bufferSize - 1);
            float lastX = x + width;
            float lastY = y + height - (height * normalize(lastVal));
            renderer.getGeometry().renderRect(lastX - 2, lastY - 2, 4, 4, lineColor, 2);
        }
    }

    /**
     * Standard rendering: Connects every data point with a line segment.
     * Used when zoomed in or with low data counts.
     */
    private void drawStandardLine(UIRenderer renderer, int color, float thickness) {
        float stepX = width / (bufferSize - 1);

        for (int i = 0; i < bufferSize - 1; i++) {
            float v1 = getValueAt(i);
            float v2 = getValueAt(i + 1);

            float x1 = x + (i * stepX);
            float y1 = y + height - (height * normalize(v1));
            float x2 = x + ((i + 1) * stepX);
            float y2 = y + height - (height * normalize(v2));

            drawLine(renderer, x1, y1, x2, y2, thickness, color);
        }
    }

    /**
     * High-Performance rendering: Aggregates data per pixel column.
     * Draws a vertical line from the Min to the Max value within that pixel's time slice.
     * This mimics the look of an oscilloscope or audio editor waveform.
     */
    private void drawHighDensity(UIRenderer renderer, int color) {
        // Iterate over screen pixels instead of data points
        float samplesPerPixel = (float) bufferSize / width;

        for (float px = 0; px < width; px += 1.0f) {
            // Determine range of data indices covering this pixel
            int idxStart = (int) (px * samplesPerPixel);
            int idxEnd = (int) ((px + 1) * samplesPerPixel);

            // Boundary checks
            if (idxStart >= bufferSize) break;
            if (idxEnd > bufferSize) idxEnd = bufferSize;
            if (idxEnd <= idxStart) idxEnd = idxStart + 1;

            // Find Min/Max in this chunk
            float localMin = Float.MAX_VALUE;
            float localMax = -Float.MAX_VALUE;

            for (int i = idxStart; i < idxEnd; i++) {
                float v = getValueAt(i);
                if (v < localMin) localMin = v;
                if (v > localMax) localMax = v;
            }

            // Normalize
            float normMin = normalize(localMin);
            float normMax = normalize(localMax);

            float yMin = y + height - (height * normMin);
            float yMax = y + height - (height * normMax);

            // Ensure we draw at least 1 pixel height
            if (Math.abs(yMax - yMin) < 1.0f) {
                yMax -= 0.5f;
                yMin += 0.5f;
            }

            // Draw vertical strip
            // x + px is the current screen column
            renderer.getGeometry().renderRect(x + px, yMax, 1.0f, yMin - yMax, color, 0);
        }
    }

    /**
     * Helper to draw a rotated line using a rectangle.
     */
    private void drawLine(UIRenderer renderer, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.001f) return;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        renderer.pushMatrix();
        renderer.translate(x1, y1, 0);
        renderer.rotate(angle, 0, 0, 1);
        // Draw rect from (0, -thickness/2) to (length, thickness/2)
        renderer.getGeometry().renderRect(0, -thickness / 2.0f, length, thickness, color, 0);
        renderer.popMatrix();
    }
}
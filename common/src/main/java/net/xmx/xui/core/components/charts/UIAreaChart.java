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
 * Fills the area under the curve using the optimized ring buffer.
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

        synchronized (dataLock) {
            if (bufferSize < 2) return;

            // Ensure scale is up to date
            performAutoScaling();

            int areaColor = style().getValue(state, AREA_COLOR);

            // Adaptive Rendering:
            // If high density, we draw 1px wide columns for every pixel on X axis.
            // If low density, we interpolate to draw smoother blocks.

            float stepX = width / (Math.max(1, bufferSize - 1));

            // If the step is smaller than 1 pixel, we clamp to 1 pixel stepping (Downsampling)
            // to avoid overdraw.
            boolean performDownsample = stepX < 1.0f;
            float renderStep = performDownsample ? 1.0f : stepX;
            int iterations = performDownsample ? (int)width : bufferSize - 1;

            for (int i = 0; i < iterations; i++) {
                float value;

                if (performDownsample) {
                    // Calculate mapping from pixel index 'i' to buffer index
                    float bufferIndexFloat = ((float)i / width) * bufferSize;
                    value = getValueAt((int)bufferIndexFloat);
                } else {
                    value = getValueAt(i);
                }

                float normH = normalize(value);
                float barH = height * normH;
                float xPos = x + (i * renderStep);

                // Draw vertical strip from axis up to value
                renderer.getGeometry().renderRect(
                        xPos,
                        y + height - barH,
                        renderStep, // width of strip
                        barH,
                        areaColor,
                        0
                );
            }
        }

        // Draw the line on top (delegates to UILineChart logic which handles locking itself)
        // Note: UILineChart.drawSelf does the locking again. Re-entrant locks in Java are fine.
        super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);
    }
}
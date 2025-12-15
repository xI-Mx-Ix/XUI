/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for axis-based charts (Line, Bar, Area).
 *
 * @author xI-Mx-Ix
 */
public abstract class AbstractChart extends UIWidget {

    public static final StyleKey<Integer> AXIS_COLOR = new StyleKey<>("chart_axis_color", 0xFF808080);
    public static final StyleKey<Integer> GRID_COLOR = new StyleKey<>("chart_grid_color", 0x20FFFFFF);

    protected final List<Float> dataPoints = new ArrayList<>();
    protected float minVal = 0;
    protected float maxVal = 100;
    protected boolean autoScale = true;

    public AbstractChart() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x10000000); // Slight background
    }

    public void setData(List<Float> data) {
        this.dataPoints.clear();
        this.dataPoints.addAll(data);
        if (autoScale && !data.isEmpty()) {
            minVal = Float.MAX_VALUE;
            maxVal = Float.MIN_VALUE;
            for (float f : data) {
                if (f < minVal) minVal = f;
                if (f > maxVal) maxVal = f;
            }
            // Add some padding
            if (minVal == maxVal) maxVal += 1;
            float range = maxVal - minVal;
            minVal -= range * 0.1f;
            maxVal += range * 0.1f;
        }
    }

    protected void drawAxes(UIRenderer renderer, int axisColor, int gridColor) {
        // Y-Axis
        renderer.getGeometry().renderRect(x, y, 2, height, axisColor, 0);
        // X-Axis
        renderer.getGeometry().renderRect(x, y + height - 2, width, 2, axisColor, 0);

        // Simple Grid lines
        int steps = 4;
        for (int i = 1; i < steps; i++) {
            float yPos = y + height - (height * ((float) i / steps));
            renderer.getGeometry().renderRect(x, yPos, width, 1, gridColor, 0);
        }
    }

    protected float normalize(float value) {
        return (value - minVal) / (maxVal - minVal);
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * Base class for axis-based charts (Line, Bar, Area).
 * <p>
 * This class has been optimized for high-frequency real-time data visualization.
 * It uses a primitive float ring buffer to avoid garbage collection overhead
 * and supports thread-safe data ingestion.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public abstract class AbstractChart extends UIWidget {

    public static final StyleKey<Integer> AXIS_COLOR = new StyleKey<>("chart_axis_color", 0xFF808080);
    public static final StyleKey<Integer> GRID_COLOR = new StyleKey<>("chart_grid_color", 0x20FFFFFF);

    /**
     * Lock object for thread-safe access to the data buffer.
     */
    protected final Object dataLock = new Object();

    /**
     * Primitive circular buffer to store chart values without object boxing.
     */
    protected float[] valueBuffer;

    /**
     * The write index for the next value.
     */
    protected int bufferHead = 0;

    /**
     * The number of valid elements currently in the buffer.
     */
    protected int bufferSize = 0;

    /**
     * The maximum number of data points to keep in history.
     */
    protected int capacity = 500;

    protected float minVal = 0;
    protected float maxVal = 100;
    protected boolean autoScale = true;

    public AbstractChart() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x10000000); // Slight background
        // Initialize buffer
        this.valueBuffer = new float[capacity];
    }

    /**
     * Sets the maximum history size of the chart.
     * This resets the current data.
     *
     * @param capacity The number of data points to hold (e.g., 1000).
     */
    public void setCapacity(int capacity) {
        synchronized (dataLock) {
            this.capacity = capacity;
            this.valueBuffer = new float[capacity];
            this.bufferHead = 0;
            this.bufferSize = 0;
        }
    }

    /**
     * Disables auto-scaling and sets a fixed Y-axis range.
     * Essential for FPS charts (0-240) or Percentages (0-100) to prevent jitter.
     *
     * @param min The bottom value of the chart (e.g., 0).
     * @param max The top value of the chart (e.g., 100).
     */
    public void setFixedRange(float min, float max) {
        synchronized (dataLock) {
            this.autoScale = false;
            this.minVal = min;
            this.maxVal = max;
        }
    }

    /**
     * Pushes a new value into the rolling chart.
     * This operation is O(1) and thread-safe.
     *
     * @param value The new data point.
     */
    public void pushValue(float value) {
        synchronized (dataLock) {
            valueBuffer[bufferHead] = value;
            bufferHead = (bufferHead + 1) % capacity;
            if (bufferSize < capacity) {
                bufferSize++;
            }
        }
    }

    /**
     * Bulk update for replacing the entire dataset efficiently.
     *
     * @param data The new primitive data array.
     */
    public void setValues(float[] data) {
        synchronized (dataLock) {
            if (data.length != capacity) {
                this.capacity = data.length;
                this.valueBuffer = new float[capacity];
            }
            System.arraycopy(data, 0, this.valueBuffer, 0, data.length);
            this.bufferHead = 0; // Reset head, though strictly not a ring buffer in this mode
            this.bufferSize = data.length;
        }
    }

    /**
     * Retrieves a value from the logical history index.
     * Index 0 is the oldest value, Index (size-1) is the newest.
     *
     * @param index The logical index (0 to size-1).
     * @return The value at that position.
     */
    protected float getValueAt(int index) {
        // Calculate physical index in the ring buffer
        // If buffer is full, head points to the oldest element (which will be overwritten next).
        // If buffer is not full, index 0 is at physical 0.
        int start = (bufferSize < capacity) ? 0 : bufferHead;
        int physicalIdx = (start + index) % capacity;
        return valueBuffer[physicalIdx];
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

    /**
     * Recalculates min/max values based on current buffer content if autoScale is enabled.
     * This should be called within the synchronized block in the render loop.
     */
    protected void performAutoScaling() {
        if (!autoScale || bufferSize == 0) return;

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        // Efficient array iteration
        for (int i = 0; i < bufferSize; i++) {
            float v = valueBuffer[i];
            if (v < min) min = v;
            if (v > max) max = v;
        }

        if (min == Float.MAX_VALUE) {
            min = 0;
            max = 100;
        }

        // Add padding to prevent lines touching the absolute edge
        if (min == max) {
            max += 1f;
            min -= 1f;
        }

        float range = max - min;
        // Smooth target interpolation could be added here for visual polish
        this.minVal = min - (range * 0.05f);
        this.maxVal = max + (range * 0.05f);
    }

    protected float normalize(float value) {
        // Prevent division by zero if range is 0
        float range = maxVal - minVal;
        if (range == 0) return 0.5f;
        return (value - minVal) / range;
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A modern Pie and Donut Chart component.
 * <p>
 * This widget renders data values as proportional circular sectors.
 * It utilizes the hardware-accelerated {@link net.xmx.xui.core.gl.renderer.GeometryRenderer}
 * to draw smooth, anti-aliased arcs.
 * </p>
 * <p>
 * <b>Features:</b>
 * <ul>
 *     <li><b>Donut Mode:</b> Configurable inner hole radius via {@link #HOLE_RADIUS}.</li>
 *     <li><b>Interactive:</b> Slices expand/explode outwards when hovered.</li>
 *     <li><b>Dynamic Hitbox:</b> The interaction hitbox follows the animated position of the slice
 *     and accounts for parent widget transformations (e.g. animating panels).</li>
 *     <li><b>Animations:</b> Smooth transitions for hover effects using exponential decay.</li>
 *     <li><b>Center Info:</b> Displays the value and label of the hovered slice in the center (Donut mode only).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Performance Optimization (v3):</b>
 * Uses a "Snapshot" rendering model. A shallow copy of the list is created at the start of the frame.
 * This allows the Update-Thread to modify the structure without crashing the Render-Thread.
 * Hit-testing is performed in local transformed space to align with UI animations.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIPieChart extends UIWidget {

    // =================================================================================
    // Style Keys
    // =================================================================================

    /**
     * Determines the size of the inner hole as a percentage of the total radius.
     * <p>
     * Range: 0.0 (Solid Pie) to 1.0 (Invisible Ring).
     * Default: 0.6f (Donut style).
     * </p>
     */
    public static final StyleKey<Float> HOLE_RADIUS = new StyleKey<>("pie_hole_radius", 0.6f);

    /**
     * The starting angle of the first slice in degrees.
     * <p>
     * Default: -90.0f (Top/12 o'clock position).
     * 0 degrees is usually 3 o'clock (Right).
     * </p>
     */
    public static final StyleKey<Float> START_ANGLE = new StyleKey<>("pie_start_angle", -90.0f);

    /**
     * The maximum distance in pixels a slice moves outward when hovered.
     * Default: 6.0 pixels.
     */
    public static final StyleKey<Float> HOVER_EXPLODE = new StyleKey<>("pie_hover_explode", 6.0f);

    // =================================================================================
    // Data Structures
    // =================================================================================

    /**
     * Lock object for thread-safe access to the slices list.
     */
    private final Object dataLock = new Object();

    /**
     * Represents a single data entry within the chart.
     * <p>
     * <b>Note:</b> Fields are not final to allow real-time updates without object reallocation.
     * </p>
     */
    public static class Slice {
        /**
         * Display label for the slice (e.g., "Java").
         */
        String label;

        /**
         * Numerical value of the slice (e.g., 45.0).
         */
        float value;

        /**
         * ARGB rendering color.
         */
        int color;

        /**
         * Internal animation state for the explosion effect.
         * 0.0 = Resting position.
         * 1.0 = Fully exploded/expanded.
         */
        float animProgress = 0.0f;

        /**
         * Creates a new data slice.
         *
         * @param label The name of the entry.
         * @param value The numerical magnitude.
         * @param color The visual color (ARGB).
         */
        public Slice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    /**
     * The master list of data slices (Model).
     */
    private final List<Slice> slices = new ArrayList<>();

    /**
     * Constructs a default PieChart.
     * <p>
     * Initializes default transparency and text colors.
     * Sets a faster transition speed (10.0f) to ensure snappy hover feedback.
     * </p>
     */
    public UIPieChart() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.style().set(ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);

        // Important: Set a faster transition speed by default.
        // 10.0f roughly equals a 200-300ms transition using exponential decay.
        // Standard 0.2f or 1.0f would feel too sluggish for UI interactions.
        this.style().setTransitionSpeed(10.0f);
    }

    /**
     * Adds a data slice to the chart.
     * <p>
     * For high-frequency updates, prefer using {@link #updateValue(int, float)}
     * instead of clearing and re-adding slices.
     * </p>
     *
     * @param label The name of the entry.
     * @param value The numerical value.
     * @param color The render color.
     * @return This instance for chaining.
     */
    public UIPieChart addSlice(String label, float value, int color) {
        synchronized (dataLock) {
            slices.add(new Slice(label, value, color));
        }
        return this;
    }

    /**
     * Updates the value of an existing slice efficiently.
     * <p>
     * This method is thread-safe and does not allocate new memory, making it ideal
     * for calling hundreds of times per second.
     * </p>
     *
     * @param index    The index of the slice to update.
     * @param newValue The new numerical value.
     */
    public void updateValue(int index, float newValue) {
        synchronized (dataLock) {
            if (index >= 0 && index < slices.size()) {
                slices.get(index).value = newValue;
            }
        }
    }

    /**
     * Clears all data from the chart.
     */
    public void clear() {
        synchronized (dataLock) {
            slices.clear();
        }
    }

    // =================================================================================
    // Rendering Logic
    // =================================================================================

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {

        // --- 1. Create Snapshot (Critical Section) ---
        List<Slice> renderSlices;
        synchronized (dataLock) {
            if (slices.isEmpty()) return;
            // Shallow copy to preserve animProgress while allowing structural changes in other threads.
            renderSlices = new ArrayList<>(slices);
        }

        // --- 2. Calculate Total ---
        float renderTotal = 0;
        for (Slice s : renderSlices) renderTotal += s.value;
        if (renderTotal <= 0) return;

        // --- 3. Resolve Geometry & Styles ---
        float size = Math.min(width, height);
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        float radiusOuter = size / 2.0f;

        float holePercent = style().getValue(state, HOLE_RADIUS);
        float explodeOffset = style().getValue(state, HOVER_EXPLODE);
        float startAngle = style().getValue(state, START_ANGLE);
        float transitionSpeed = style().getTransitionSpeed();
        float radiusInner = radiusOuter * holePercent;
        int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);

        // ---------------------------------------------------------
        // 4. Transform Mouse to Local Transformed Space
        // ---------------------------------------------------------
        // We calculate the mouse position relative to the widget's current coordinate system.
        // This accounts for parent animations (like cards moving up).

        Matrix4f model = new Matrix4f();
        UIWidget current = this;
        // Walk up the hierarchy to build the full model matrix
        while (current != null) {
            float cX = current.getX() + current.getWidth() / 2.0f;
            float cY = current.getY() + current.getHeight() / 2.0f;

            // Apply current widget's style transforms (same logic as UIWidget.render)
            float tX = current.style().getValue(InteractionState.DEFAULT, ThemeProperties.TRANSLATE_X);
            float tY = current.style().getValue(InteractionState.DEFAULT, ThemeProperties.TRANSLATE_Y);
            float rZ = current.style().getValue(InteractionState.DEFAULT, ThemeProperties.ROTATION_Z);
            float sX = current.style().getValue(InteractionState.DEFAULT, ThemeProperties.SCALE_X);
            float sY = current.style().getValue(InteractionState.DEFAULT, ThemeProperties.SCALE_Y);

            // Pre-multiply parent transforms
            Matrix4f step = new Matrix4f()
                    .translate(cX + tX, cY + tY, 0)
                    .rotate((float) Math.toRadians(rZ), 0, 0, 1)
                    .scale(sX, sY, 1)
                    .translate(-cX, -cY, 0);

            model.mulLocal(step);
            current = current.getParent();
        }

        Vector4f localMouse = new Vector4f((float) mouseX, (float) mouseY, 0, 1).mul(model.invert());
        float lmX = localMouse.x;
        float lmY = localMouse.y;

        // Polar coordinates based on transformed local mouse
        float vdx = lmX - cx;
        float vdy = lmY - cy;
        float dist = (float) Math.sqrt(vdx * vdx + vdy * vdy);
        float mouseAngle = (float) Math.toDegrees(Math.atan2(vdy, vdx));

        Slice hoveredSlice = null;

        // --- 5. Render Loop & Hit Testing ---
        float currentAngle = startAngle;
        for (Slice slice : renderSlices) {
            float percent = slice.value / renderTotal;
            float sweepAngle = percent * 360f;
            float endAngle = currentAngle + sweepAngle;

            // --- Hit Testing (Hysteresis Logic) ---
            boolean isHovered = false;

            float normMouse = mouseAngle - currentAngle;
            while (normMouse < 0) normMouse += 360;
            while (normMouse >= 360) normMouse -= 360;

            if (normMouse >= 0 && normMouse <= sweepAngle) {
                // Outer radius grows with animation progress to follow the slice visually.
                // Inner radius stays stable at radiusInner to prevent flickering.
                float currentMaxRadius = radiusOuter + (slice.animProgress * explodeOffset);
                if (dist >= radiusInner && dist <= currentMaxRadius) {
                    isHovered = true;
                    hoveredSlice = slice;
                }
            }

            // --- Animation Logic ---
            float targetState = isHovered ? 1.0f : 0.0f;
            float lerp = 1.0f - (float) Math.exp(-transitionSpeed * deltaTime);
            slice.animProgress += (targetState - slice.animProgress) * lerp;

            // --- Draw Geometry ---
            float drawCx = cx;
            float drawCy = cy;

            if (slice.animProgress > 0.001f) {
                float midAngleRad = (float) Math.toRadians(currentAngle + sweepAngle / 2.0f);
                float drawOffset = explodeOffset * slice.animProgress;
                drawCx += (float) Math.cos(midAngleRad) * drawOffset;
                drawCy += (float) Math.sin(midAngleRad) * drawOffset;
            }

            if (radiusInner > 0.01f) {
                renderer.getGeometry().renderDonutSlice(drawCx, drawCy, radiusOuter, radiusInner, currentAngle, endAngle, slice.color);
            } else {
                renderer.getGeometry().renderPieSlice(drawCx, drawCy, radiusOuter, currentAngle, endAngle, slice.color);
            }

            currentAngle += sweepAngle;
        }

        // --- 6. Render Center Info ---
        if (radiusInner > 10) {
            String centerText = (hoveredSlice != null) ? String.valueOf((int) hoveredSlice.value) : String.valueOf((int) renderTotal);
            String subText = (hoveredSlice != null) ? hoveredSlice.label : "Total";

            TextComponent mainT = TextComponent.literal(centerText);
            float mainW = TextComponent.getTextWidth(mainT);
            float fontH = TextComponent.getFontHeight();
            renderer.drawText(mainT, cx - mainW / 2.0f, cy - fontH, textColor, true);

            TextComponent subT = TextComponent.literal(subText);
            float subW = TextComponent.getTextWidth(subT);
            renderer.drawText(subT, cx - subW / 2.0f, cy + 2, 0xFFAAAAAA, true);
        }
    }
}
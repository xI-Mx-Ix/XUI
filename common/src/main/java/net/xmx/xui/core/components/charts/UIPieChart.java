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
 *     <li><b>Animations:</b> Smooth transitions for hover effects using exponential decay.</li>
 *     <li><b>Center Info:</b> Displays the value and label of the hovered slice in the center (Donut mode only).</li>
 * </ul>
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
     * Represents a single data entry within the chart.
     */
    public static class Slice {
        /** Display label for the slice (e.g., "Java"). */
        final String label;

        /** Numerical value of the slice (e.g., 45.0). */
        final float value;

        /** ARGB rendering color. */
        final int color;

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

    /** The list of data slices to render. */
    private final List<Slice> slices = new ArrayList<>();

    /** The sum of all slice values, used to calculate percentages. */
    private float totalValue = 0;

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
     *
     * @param label The name of the entry.
     * @param value The numerical value.
     * @param color The render color.
     * @return This instance for chaining.
     */
    public UIPieChart addSlice(String label, float value, int color) {
        slices.add(new Slice(label, value, color));
        totalValue += value;
        return this;
    }

    /**
     * Clears all data from the chart.
     */
    public void clear() {
        slices.clear();
        totalValue = 0;
    }

    // =================================================================================
    // Rendering Logic
    // =================================================================================

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // Early exit if there is no data to render
        if (slices.isEmpty() || totalValue <= 0) return;

        // ---------------------------------------------------------
        // 1. Resolve Geometry & Styles
        // ---------------------------------------------------------

        // The chart fits within the smallest dimension (width or height) to remain circular
        float size = Math.min(width, height);
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        float radiusOuter = size / 2.0f;

        // Fetch animated style properties via the manager logic
        float holePercent = style().getValue(state, HOLE_RADIUS);
        float explodeOffset = style().getValue(state, HOVER_EXPLODE);
        float currentAngle = style().getValue(state, START_ANGLE);

        // We use the configured speed for the hover animation
        float transitionSpeed = style().getTransitionSpeed();

        float radiusInner = radiusOuter * holePercent;
        int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);

        // ---------------------------------------------------------
        // 2. Calculate Hover Logic (Polar Coordinates)
        // ---------------------------------------------------------

        // Vector from center to mouse
        float dx = mouseX - cx;
        float dy = mouseY - cy;

        // Distance from center (Hypotenuse)
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Angle in radians (-PI to +PI)
        double angleRad = Math.atan2(dy, dx);
        // Convert to degrees
        float mouseAngle = (float) Math.toDegrees(angleRad);

        // Check if mouse is strictly within the ring (between inner and outer radius)
        boolean isMouseInRing = dist >= radiusInner && dist <= radiusOuter;

        Slice hoveredSlice = null;

        // ---------------------------------------------------------
        // 3. Render Loop (Iterate Slices)
        // ---------------------------------------------------------

        for (Slice slice : slices) {
            // Calculate the angular size of this slice
            float percent = slice.value / totalValue;
            float sweepAngle = percent * 360f;
            float endAngle = currentAngle + sweepAngle;

            // --- Hit Testing ---
            boolean isHovered = false;
            if (isMouseInRing) {
                // Normalize mouse angle relative to the start of this slice
                // We normalize everything to 0..360 range to handle wrap-around cases
                float normMouse = (mouseAngle - currentAngle);
                while (normMouse < 0) normMouse += 360;
                while (normMouse >= 360) normMouse -= 360;

                // If the normalized mouse angle falls within the sweep of this slice, it's a hit
                if (normMouse >= 0 && normMouse <= sweepAngle) {
                    isHovered = true;
                    hoveredSlice = slice;
                }
            }

            // --- Animation Logic (Smooth Explosion) ---

            // Determine target state: 1.0 if hovered, 0.0 otherwise
            float targetState = isHovered ? 1.0f : 0.0f;

            // Exponential decay interpolation formula: current + (target - current) * factor
            // Factor is derived from time step and speed: 1 - e^(-speed * dt)
            float lerp = 1.0f - (float) Math.exp(-transitionSpeed * deltaTime);

            // Update the slice's individual progress
            slice.animProgress += (targetState - slice.animProgress) * lerp;

            // --- Draw Geometry ---

            // Base center position
            float drawCx = cx;
            float drawCy = cy;

            // Apply explosion offset if animation is active
            if (slice.animProgress > 0.001f) {
                // Calculate the middle angle of the slice to determine direction
                float midAngleRad = (float) Math.toRadians(currentAngle + sweepAngle / 2.0f);

                // Calculate pixel offset based on animation progress
                float offset = explodeOffset * slice.animProgress;

                // Move draw center outwards along the angle vector
                drawCx += Math.cos(midAngleRad) * offset;
                drawCy += Math.sin(midAngleRad) * offset;
            }

            // Dispatch render command to the Geometry Renderer
            if (radiusInner > 0.01f) {
                renderer.getGeometry().renderDonutSlice(drawCx, drawCy, radiusOuter, radiusInner, currentAngle, endAngle, slice.color);
            } else {
                renderer.getGeometry().renderPieSlice(drawCx, drawCy, radiusOuter, currentAngle, endAngle, slice.color);
            }

            // Advance the angle cursor for the next slice
            currentAngle += sweepAngle;
        }

        // ---------------------------------------------------------
        // 4. Render Center Info (Donut Mode Only)
        // ---------------------------------------------------------

        // Only draw text if there is enough space in the middle
        if (radiusInner > 10) {
            String centerText;
            String subText = null;

            if (hoveredSlice != null) {
                // Show specific slice data if hovered
                centerText = String.valueOf((int) hoveredSlice.value);
                subText = hoveredSlice.label;
            } else {
                // Show total summary if idle
                centerText = String.valueOf((int) totalValue);
                subText = "Total";
            }

            // Main Value Text (Centered)
            TextComponent mainT = TextComponent.literal(centerText);
            float mainW = TextComponent.getTextWidth(mainT);
            float fontH = TextComponent.getFontHeight();

            renderer.drawText(mainT, cx - mainW / 2.0f, cy - fontH, textColor, true);

            // Sub Label Text (Below main text)
            if (subText != null) {
                TextComponent subT = TextComponent.literal(subText);
                float subW = TextComponent.getTextWidth(subT);
                // Draw slightly below center
                renderer.drawText(subT, cx - subW / 2.0f, cy + 2, 0xFFAAAAAA, true);
            }
        }
    }
}
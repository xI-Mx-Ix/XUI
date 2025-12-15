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
 * A modern Pie/Donut Chart component.
 * <p>
 * Uses the hardware-accelerated geometry renderer to draw smooth arc segments.
 * Supports "Donut" mode via the {@link #HOLE_RADIUS} property and interactive hover effects.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIPieChart extends UIWidget {

    // --- Style Keys ---

    /**
     * Determines the size of the inner hole as a percentage of the total radius (0.0 to 1.0).
     * Set to 0.0 for a solid Pie Chart, or e.g. 0.6 for a Donut Chart.
     */
    public static final StyleKey<Float> HOLE_RADIUS = new StyleKey<>("pie_hole_radius", 0.6f);

    /**
     * The starting angle in degrees. Default is -90 (Top).
     */
    public static final StyleKey<Float> START_ANGLE = new StyleKey<>("pie_start_angle", -90.0f);

    /**
     * How many pixels a slice moves outward when hovered.
     */
    public static final StyleKey<Float> HOVER_EXPLODE = new StyleKey<>("pie_hover_explode", 6.0f);

    // --- Data ---

    public static class Slice {
        final String label;
        final float value;
        final int color;

        public Slice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private float totalValue = 0;

    /**
     * Constructs a default PieChart (Donut style by default style keys).
     */
    public UIPieChart() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.style().set(ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Adds a data slice to the chart.
     *
     * @param label The name of the entry.
     * @param value The numerical value.
     * @param color The render color.
     * @return This instance.
     */
    public UIPieChart addSlice(String label, float value, int color) {
        slices.add(new Slice(label, value, color));
        totalValue += value;
        return this;
    }

    /**
     * Clears all data.
     */
    public void clear() {
        slices.clear();
        totalValue = 0;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        if (slices.isEmpty() || totalValue <= 0) return;

        // 1. Resolve Geometry & Styles
        float size = Math.min(width, height);
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        float radiusOuter = size / 2.0f;
        
        float holePercent = style().getValue(state, HOLE_RADIUS);
        float explodeOffset = style().getValue(state, HOVER_EXPLODE);
        float currentAngle = style().getValue(state, START_ANGLE);
        
        float radiusInner = radiusOuter * holePercent;
        int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);

        // 2. Calculate Hover Logic
        // Convert mouse position to polar coordinates relative to center
        float dx = mouseX - cx;
        float dy = mouseY - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Calculate angle in degrees (0..360)
        double angleRad = Math.atan2(dy, dx);
        float mouseAngle = (float) Math.toDegrees(angleRad);
        
        // Normalize mouseAngle to match currentAngle logic (usually starting at -90)
        // This simple check works if we assume the chart fills 360 degrees.
        // For robustness, we check if the mouse is inside the ring first.
        boolean isMouseInRing = dist >= radiusInner && dist <= radiusOuter;
        
        Slice hoveredSlice = null;

        // 3. Render Slices
        for (Slice slice : slices) {
            float percent = slice.value / totalValue;
            float sweepAngle = percent * 360f;
            float endAngle = currentAngle + sweepAngle;

            // Check if hovered
            boolean isHovered = false;
            if (isMouseInRing) {
                // Normalize angles for comparison
                float normMouse = (mouseAngle - currentAngle);
                while (normMouse < 0) normMouse += 360;
                while (normMouse >= 360) normMouse -= 360;

                // If the normalized mouse angle is within the sweep
                // Note: accurate hit testing for wrapped angles can be complex, 
                // simplified check:
                if (normMouse >= 0 && normMouse <= sweepAngle) {
                    isHovered = true;
                    hoveredSlice = slice;
                }
            }

            // Calculate Explode Offset (move out from center)
            float drawCx = cx;
            float drawCy = cy;
            
            if (isHovered) {
                float midAngleRad = (float) Math.toRadians(currentAngle + sweepAngle / 2.0f);
                drawCx += Math.cos(midAngleRad) * explodeOffset;
                drawCy += Math.sin(midAngleRad) * explodeOffset;
            }

            // Draw using the efficient geometry method
            if (radiusInner > 0.01f) {
                renderer.getGeometry().renderDonutSlice(drawCx, drawCy, radiusOuter, radiusInner, currentAngle, endAngle, slice.color);
            } else {
                renderer.getGeometry().renderPieSlice(drawCx, drawCy, radiusOuter, currentAngle, endAngle, slice.color);
            }

            currentAngle += sweepAngle;
        }

        // 4. Render Center Info (if Donut)
        if (radiusInner > 10) {
            String centerText;
            String subText = null;

            if (hoveredSlice != null) {
                centerText = String.valueOf((int) hoveredSlice.value);
                subText = hoveredSlice.label;
            } else {
                centerText = String.valueOf((int) totalValue);
                subText = "Total";
            }

            TextComponent mainT = TextComponent.literal(centerText);
            float mainW = TextComponent.getTextWidth(mainT);
            float fontH = TextComponent.getFontHeight();

            renderer.drawText(mainT, cx - mainW / 2.0f, cy - fontH, textColor, true);

            if (subText != null) {
                TextComponent subT = TextComponent.literal(subText);
                float subW = TextComponent.getTextWidth(subT);
                // Draw slightly below
                renderer.drawText(subT, cx - subW / 2.0f, cy + 2, 0xFFAAAAAA, true);
            }
        }
    }
}
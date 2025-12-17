/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;

/**
 * A Heatmap chart showing values as color intensity in a 2D grid.
 *
 * @author xI-Mx-Ix
 */
public class UIHeatmap extends UIWidget {

    private float[][] values; // 0.0 to 1.0

    /**
     * Animation state for hover outlines: 0.0 (Invisible) -> 1.0 (Fully Visible).
     * Stores the current alpha value for the highlight border of each cell.
     */
    private float[][] hoverAlphas;

    private int rows = 5;
    private int cols = 5;
    private int baseColor = 0xFFFF0000; // Red

    public UIHeatmap setGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.values = new float[rows][cols];
        // Initialize animation state array with the same dimensions
        this.hoverAlphas = new float[rows][cols];
        return this;
    }

    public void setValue(int row, int col, float val) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            values[row][col] = Math.max(0f, Math.min(1f, val));
        }
    }

    public UIHeatmap setBaseColor(int color) {
        this.baseColor = color;
        return this;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        if (values == null) return;

        // Ensure state array exists and matches dimensions (safety check against dynamic resizing)
        if (hoverAlphas == null || hoverAlphas.length != rows || (rows > 0 && hoverAlphas[0].length != cols)) {
            hoverAlphas = new float[rows][cols];
        }

        float cellW = width / cols;
        float cellH = height / rows;
        float gap = 1.0f;

        // Get animation speed from style
        float transitionSpeed = style().getTransitionSpeed();
        // Calculate common interpolation factor for this frame based on delta time
        float lerpFactor = 1.0f - (float) Math.exp(-transitionSpeed * deltaTime);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float val = values[r][c];

                // Calculate Alpha based on value for the cell body
                int alpha = (int) (255 * val);
                int color = (baseColor & 0x00FFFFFF) | (alpha << 24);

                // Or darker/lighter variant
                // int color = interpolateColor(0xFF000000, baseColor, val);

                float cx = x + (c * cellW);
                float cy = y + (r * cellH);

                // Draw the main cell rectangle
                renderer.getGeometry().renderRect(cx, cy, cellW - gap, cellH - gap, color, 2.0f);

                // --- ANIMATION LOGIC for Hover Outline ---

                // 1. Check if specific cell is hovered
                boolean isHovered = mouseX >= cx && mouseX < cx + cellW &&
                        mouseY >= cy && mouseY < cy + cellH;

                // 2. Determine target alpha (1.0 if hovered, 0.0 if not)
                float targetAlpha = isHovered ? 1.0f : 0.0f;

                // 3. Interpolate current alpha towards target using exponential decay
                hoverAlphas[r][c] += (targetAlpha - hoverAlphas[r][c]) * lerpFactor;

                // 4. Render Outline if partially visible (alpha > ~1%)
                if (hoverAlphas[r][c] > 0.01f) {
                    // Convert float alpha (0-1) to byte (0-255)
                    int outlineAlpha = (int)(hoverAlphas[r][c] * 255);
                    // Create white color with calculated alpha
                    int outlineColor = (0xFFFFFF) | (outlineAlpha << 24);

                    renderer.getGeometry().renderOutline(
                            cx, cy,
                            cellW - gap, cellH - gap,
                            outlineColor,
                            2.0f, 1.0f
                    );
                }
            }
        }
    }
}
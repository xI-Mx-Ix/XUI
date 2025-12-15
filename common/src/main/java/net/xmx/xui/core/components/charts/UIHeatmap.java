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
    private int rows = 5;
    private int cols = 5;
    private int baseColor = 0xFFFF0000; // Red

    public UIHeatmap setGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.values = new float[rows][cols];
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

        float cellW = width / cols;
        float cellH = height / rows;
        float gap = 1.0f;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float val = values[r][c];
                
                // Calculate Alpha based on value
                int alpha = (int) (255 * val);
                int color = (baseColor & 0x00FFFFFF) | (alpha << 24);
                
                // Or darker/lighter variant
                // int color = interpolateColor(0xFF000000, baseColor, val);

                float cx = x + (c * cellW);
                float cy = y + (r * cellH);

                renderer.getGeometry().renderRect(cx, cy, cellW - gap, cellH - gap, color, 2.0f);
                
                // Hover tooltip logic could go here
                if (mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH) {
                    renderer.getGeometry().renderOutline(cx, cy, cellW - gap, cellH - gap, 0xFFFFFFFF, 2.0f, 1.0f);
                }
            }
        }
    }
}
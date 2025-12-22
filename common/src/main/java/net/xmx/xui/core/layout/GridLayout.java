/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.layout;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;

import java.util.List;

/**
 * A layout strategy that arranges children in a strict grid with a fixed number of columns.
 * <p>
 * The width of each item is automatically calculated to fill the available parent width,
 * accounting for the defined gaps. The height of items is fixed per row.
 * </p>
 * <p>
 * If the number of children exceeds {@code columns * rows}, new rows are added automatically.
 * The parent panel's height can optionally be adjusted to fit the content.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class GridLayout implements LayoutManager {

    private int columns = 2;
    private float gapX = 5.0f;
    private float gapY = 5.0f;
    private float rowHeight = 40.0f;
    private boolean resizeParentHeight = true;

    /**
     * Sets the number of columns in the grid.
     *
     * @param columns The number of columns (must be at least 1).
     * @return This instance for chaining.
     */
    public GridLayout setColumns(int columns) {
        this.columns = Math.max(1, columns);
        return this;
    }

    /**
     * Sets the spacing between items both horizontally and vertically.
     *
     * @param gap The gap in pixels.
     * @return This instance for chaining.
     */
    public GridLayout setGap(float gap) {
        this.gapX = gap;
        this.gapY = gap;
        return this;
    }

    /**
     * Sets separate horizontal and vertical gaps.
     *
     * @param gapX Horizontal spacing in pixels.
     * @param gapY Vertical spacing in pixels.
     * @return This instance for chaining.
     */
    public GridLayout setGap(float gapX, float gapY) {
        this.gapX = gapX;
        this.gapY = gapY;
        return this;
    }

    /**
     * Sets the fixed height for every row in the grid.
     *
     * @param height The height in pixels.
     * @return This instance for chaining.
     */
    public GridLayout setRowHeight(float height) {
        this.rowHeight = height;
        return this;
    }

    /**
     * Configures whether the parent panel should resize its height to fit the grid content.
     *
     * @param resize True to auto-resize parent height.
     * @return This instance for chaining.
     */
    public GridLayout setResizeParentHeight(boolean resize) {
        this.resizeParentHeight = resize;
        return this;
    }

    @Override
    public void arrange(UIPanel panel) {
        List<UIWidget> children = panel.getChildren();
        if (children.isEmpty()) return;

        float parentWidth = panel.getWidth();
        
        // Calculate the width available for a single item:
        // Formula: (TotalWidth - (Gaps between columns)) / ColumnCount
        float totalGapWidth = gapX * (columns - 1);
        float itemWidth = (parentWidth - totalGapWidth) / columns;

        // Prevent negative widths if parent is too small
        if (itemWidth < 0) itemWidth = 0;

        for (int i = 0; i < children.size(); i++) {
            UIWidget child = children.get(i);

            // Calculate grid coordinates
            int col = i % columns;
            int row = i / columns;

            // Calculate absolute position relative to parent content area
            float xPos = col * (itemWidth + gapX);
            float yPos = row * (rowHeight + gapY);

            // Enforce constraints on the child
            child.setX(Layout.pixel(xPos));
            child.setY(Layout.pixel(yPos));
            child.setWidth(Layout.pixel(itemWidth));
            child.setHeight(Layout.pixel(rowHeight));
        }

        // Adjust the height of the parent container to fit all rows
        if (resizeParentHeight) {
            int totalRows = (int) Math.ceil((double) children.size() / columns);
            float totalHeight = totalRows * (rowHeight + gapY) - gapY; // Subtract last gap
            
            // Only update if significantly different to avoid rounding jitter
            if (totalHeight > 0 && Math.abs(panel.getHeight() - totalHeight) > 0.5f) {
                panel.setHeight(Layout.pixel(totalHeight));
            }
        }
    }
}
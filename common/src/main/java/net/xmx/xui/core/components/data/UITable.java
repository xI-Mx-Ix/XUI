/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.ThemeProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * A data table component.
 * Organizes content into rows and columns.
 * Requires a {@link UITableHeader} to define column weights/widths.
 *
 * @author xI-Mx-Ix
 */
public class UITable extends UIPanel {

    private UITableHeader header;
    private final List<UITableRow> rows = new ArrayList<>();
    private float rowHeight = 24.0f;

    public UITable() {
        // Default table styling
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF1E1E1E);
        this.style().set(ThemeProperties.BORDER_COLOR, 0xFF404040);
        this.style().set(ThemeProperties.BORDER_THICKNESS, 1.0f);
    }

    /**
     * Sets the header row for the table.
     * The header defines the number of columns and their relative sizes.
     *
     * @param header The header component.
     * @return This table instance.
     */
    public UITable setHeader(UITableHeader header) {
        this.header = header;
        this.add(header);
        return this;
    }

    /**
     * Adds a data row to the table.
     *
     * @param row The row component.
     * @return This table instance.
     */
    public UITable addRow(UITableRow row) {
        this.rows.add(row);
        this.add(row);
        return this;
    }

    /**
     * Sets the fixed height for each row in the table.
     *
     * @param height The height in pixels.
     * @return This table instance.
     */
    public UITable setRowHeight(float height) {
        this.rowHeight = height;
        return this;
    }

    /**
     * Calculates the layout of the table.
     * Ensures the header and all rows match the width of the table before
     * calculating column distribution.
     */
    @Override
    public void layout() {
        // Calculate own dimensions first to determine available width
        super.layout();

        float tableWidth = this.getWidth();
        float currentY = 0;

        // 1. Position and Size Header
        if (header != null) {
            header.setY(Layout.pixel(0));
            // Explicitly set width to match table so percentages calculate correctly
            header.setWidth(Layout.pixel(tableWidth));
            header.setHeight(Layout.pixel(rowHeight));
            header.layout();
            currentY += header.getHeight();
        }

        // 2. Position and Size Rows
        for (UITableRow row : rows) {
            row.setY(Layout.pixel(currentY));
            // Explicitly set width to match table so percentages calculate correctly
            row.setWidth(Layout.pixel(tableWidth));
            row.setHeight(Layout.pixel(rowHeight));

            // Pass column configuration from header to row to align cells
            if (header != null) {
                row.syncColumns(header.getColumnWeights(), tableWidth);
            }

            row.layout();
            currentY += row.getHeight();
        }

        // Adjust total height of the table to fit all rows
        this.height = currentY;
    }
}
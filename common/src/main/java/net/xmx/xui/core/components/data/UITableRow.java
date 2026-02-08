/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic row in a {@link UITable}.
 * Contains {@link UITableCell}s.
 *
 * @author xI-Mx-Ix
 */
public class UITableRow extends UIPanel {

    private final List<UITableCell> cells = new ArrayList<>();

    public UITableRow() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000); // Transparent by default
        this.style()
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0x1FFFFFFF); // Hover effect
    }

    /**
     * Adds a cell to this row.
     */
    public UITableRow addCell(UITableCell cell) {
        this.cells.add(cell);
        this.add(cell);
        return this;
    }

    /**
     * Called by the parent table to align cells with the header.
     */
    public void syncColumns(List<Float> weights, float totalTableWidth) {
        float totalWeight = 0;
        for (float w : weights) totalWeight += w;

        float currentX = 0;
        int limit = Math.min(cells.size(), weights.size());

        for (int i = 0; i < limit; i++) {
            UITableCell cell = cells.get(i);
            float weight = weights.get(i);
            float cellWidth = (totalWeight > 0) ? (weight / totalWeight) * totalTableWidth : 0;

            cell.setX(Layout.pixel(currentX));
            cell.setWidth(Layout.pixel(cellWidth));
            cell.setHeight(Layout.relative(1.0f));

            currentX += cellWidth;
        }
    }
}
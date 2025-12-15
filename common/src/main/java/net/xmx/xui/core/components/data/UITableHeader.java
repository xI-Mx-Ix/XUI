/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.style.InteractionState;

import java.util.ArrayList;
import java.util.List;

/**
 * The header row of a {@link UITable}.
 * Defines column names and relative widths (weights).
 *
 * @author xI-Mx-Ix
 */
public class UITableHeader extends UIPanel {

    private final List<Float> columnWeights = new ArrayList<>();
    private final List<UITableCell> headerCells = new ArrayList<>();

    public UITableHeader() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF303030); // Darker header
        this.style().set(ThemeProperties.BORDER_THICKNESS, 0.0f);
    }

    /**
     * Adds a column definition.
     *
     * @param title  The column title.
     * @param weight The relative width weight (e.g., 1.0 for equal width, 2.0 for double).
     */
    public UITableHeader addColumn(String title, float weight) {
        UITableCell cell = new UITableCell(TextComponent.literal(title));
        // Bold text for header
        cell.style().set(ThemeProperties.TEXT_COLOR, 0xFFEEEEEE);
        
        this.columnWeights.add(weight);
        this.headerCells.add(cell);
        this.add(cell);
        return this;
    }

    public List<Float> getColumnWeights() {
        return columnWeights;
    }

    @Override
    public void layout() {
        super.layout(); // Resolve own dimensions first

        float totalWeight = 0;
        for (float w : columnWeights) totalWeight += w;

        float availableWidth = this.getWidth();
        float currentX = 0;

        for (int i = 0; i < headerCells.size(); i++) {
            UITableCell cell = headerCells.get(i);
            float weight = columnWeights.get(i);
            
            float cellWidth = (weight / totalWeight) * availableWidth;
            
            cell.setX(Layout.pixel(currentX));
            cell.setWidth(Layout.pixel(cellWidth));
            cell.setHeight(Layout.relative(1.0f));
            
            cell.layout();
            currentX += cellWidth;
        }
    }
}
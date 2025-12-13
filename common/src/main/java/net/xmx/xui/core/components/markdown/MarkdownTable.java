/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.text.UIComponent;
import net.xmx.xui.impl.UIRenderImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Table in Markdown.
 * Supports dynamic column sizing based on content width.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownTable extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a table from a list of raw string lines.
     *
     * @param lines        The raw lines of the table (including header and separator).
     * @param contentWidth The maximum available width.
     */
    public MarkdownTable(List<String> lines, float contentWidth) {
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));

        if (lines.size() < 2) {
            this.renderHeight = 0;
            return;
        }

        // Parse rows into cells
        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            // Split by pipe |, but ignore leading/trailing pipes if present
            String[] cells = line.trim().split("\\|");
            // Filter out empty strings caused by leading/trailing pipes
            List<String> rowCells = Arrays.stream(cells)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            rows.add(rowCells);
        }

        // Determine number of columns
        int columns = rows.get(0).size();

        // Calculate maximum width required for each column
        float[] colWidths = new float[columns];
        // Padding inside each cell
        float cellPadding = 10.0f;

        for (List<String> row : rows) {
            // Skip the separator row (usually contains ---)
            if (isSeparatorRow(row)) continue;

            for (int c = 0; c < columns && c < row.size(); c++) {
                String text = row.get(c);
                int textWidth = UIRenderImpl.getInstance().getTextWidth(MarkdownUtils.parseInline(text));
                if (textWidth + cellPadding > colWidths[c]) {
                    colWidths[c] = textWidth + cellPadding;
                }
            }
        }

        // Normalize widths to fit contentWidth if they overflow
        float totalReqWidth = 0;
        for (float w : colWidths) totalReqWidth += w;

        if (totalReqWidth > contentWidth) {
            float scale = contentWidth / totalReqWidth;
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i] *= scale;
            }
        }

        // Build UI Components
        float currentY = 0;
        float rowHeight = UIRenderImpl.getInstance().getFontHeight() + 8; // Text + padding

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);

            // Skip drawing the separator row (e.g. ---|---)
            if (isSeparatorRow(row)) {
                // Add a small divider line instead
                UIPanel separator = new UIPanel();
                separator.setX(Constraints.pixel(0))
                        .setY(Constraints.pixel(currentY))
                        .setWidth(Constraints.pixel(totalReqWidth))
                        .setHeight(Constraints.pixel(1));
                separator.style().set(Properties.BACKGROUND_COLOR, 0xFF606060);
                this.add(separator);
                currentY += 4;
                continue;
            }

            boolean isHeader = (r == 0);
            float currentX = 0;

            // Background for this row
            UIPanel rowBg = new UIPanel();
            rowBg.setX(Constraints.pixel(0))
                    .setY(Constraints.pixel(currentY))
                    .setWidth(Constraints.pixel(totalReqWidth))
                    .setHeight(Constraints.pixel(rowHeight));
            
            // Header is darker, alternating rows are slightly different
            int bgColor = isHeader ? 0xFF303030 : (r % 2 == 0 ? 0x00000000 : 0xFF181818);
            rowBg.style().set(Properties.BACKGROUND_COLOR, bgColor);
            this.add(rowBg);

            for (int c = 0; c < columns && c < row.size(); c++) {
                String cellText = row.get(c);
                UIComponent content = MarkdownUtils.parseInline(cellText);
                
                if (isHeader) {
                    // Header text is Bold and Yellow
                    content = content.copy().setBold(true).setColor(0xFFFFFF55); 
                }

                UIText cellWidget = new UIText();
                cellWidget.setText(content);
                // Center text vertically in row
                cellWidget.setX(Constraints.pixel(currentX + 5)); // +5 padding
                cellWidget.setY(Constraints.pixel(currentY + 4)); 
                
                this.add(cellWidget);
                currentX += colWidths[c];
            }

            currentY += rowHeight;
        }

        this.renderHeight = currentY + 10;
        this.setHeight(Constraints.pixel(renderHeight));
    }

    private boolean isSeparatorRow(List<String> row) {
        if (row.isEmpty()) return false;
        // Simple check: if first cell contains dashes
        String first = row.get(0);
        return first.contains("---");
    }

    /**
     * Returns the pre-calculated height of this component.
     * @return The total vertical space this component occupies.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
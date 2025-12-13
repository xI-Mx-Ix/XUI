/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

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
     * @param font         The font to use for table content.
     */
    public MarkdownTable(List<String> lines, float contentWidth, Font font) {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));

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
                TextComponent comp = MarkdownUtils.parseInline(text);
                
                // IMPORTANT: Apply the font before measuring, as width depends on the font!
                MarkdownUtils.applyFontRecursive(comp, font);
                
                int textWidth = TextComponent.getTextWidth(comp);
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
        // Calculate row height based on font size
        float rowHeight = font.getLineHeight() + 8; // Text + padding

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);

            // Skip drawing the separator row (e.g. ---|---)
            if (isSeparatorRow(row)) {
                // Add a small divider line instead
                UIPanel separator = new UIPanel();
                separator.setX(Layout.pixel(0))
                        .setY(Layout.pixel(currentY))
                        .setWidth(Layout.pixel(totalReqWidth))
                        .setHeight(Layout.pixel(1));
                separator.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF606060);
                this.add(separator);
                currentY += 4;
                continue;
            }

            boolean isHeader = (r == 0);
            float currentX = 0;

            // Background for this row
            UIPanel rowBg = new UIPanel();
            rowBg.setX(Layout.pixel(0))
                    .setY(Layout.pixel(currentY))
                    .setWidth(Layout.pixel(totalReqWidth))
                    .setHeight(Layout.pixel(rowHeight));
            
            // Header is darker, alternating rows are slightly different
            int bgColor = isHeader ? 0xFF303030 : (r % 2 == 0 ? 0x00000000 : 0xFF181818);
            rowBg.style().set(ThemeProperties.BACKGROUND_COLOR, bgColor);
            this.add(rowBg);

            for (int c = 0; c < columns && c < row.size(); c++) {
                String cellText = row.get(c);
                TextComponent content = MarkdownUtils.parseInline(cellText);
                MarkdownUtils.applyFontRecursive(content, font);
                
                if (isHeader) {
                    // Header text is Bold and Yellow
                    content = content.copy().setBold(true).setColor(0xFFFFFF55); 
                }

                UIText cellWidget = new UIText();
                cellWidget.setText(content);
                // Center text vertically in row
                cellWidget.setX(Layout.pixel(currentX + 5)); // +5 padding
                cellWidget.setY(Layout.pixel(currentY + 4));
                
                this.add(cellWidget);
                currentX += colWidths[c];
            }

            currentY += rowHeight;
        }

        this.renderHeight = currentY + 10;
        this.setHeight(Layout.pixel(renderHeight));
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
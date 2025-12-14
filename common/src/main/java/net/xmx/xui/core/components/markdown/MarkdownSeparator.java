/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * Represents a Horizontal Separator in Markdown (---).
 * Renders a thin line with vertical spacing.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownSeparator extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a separator line.
     *
     * @param contentWidth The width of the separator.
     */
    public MarkdownSeparator(float contentWidth) {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));
        
        // Layout calculation logic from original:
        // currentLayoutY += 5 (top margin)
        // 2px thick line
        // currentLayoutY += 10 (bottom margin)
        
        float topMargin = 5;
        float lineThickness = 2;
        float bottomMargin = 10;

        UIPanel line = new UIPanel();
        line.setX(Layout.pixel(0))
                .setY(Layout.pixel(topMargin))
                .setWidth(Layout.pixel(contentWidth))
                .setHeight(Layout.pixel(lineThickness));

        line.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF404040);

        this.add(line);
        
        // Set total height for the container
        this.renderHeight = topMargin + lineThickness + bottomMargin;
        this.setHeight(Layout.pixel(renderHeight));
    }

    /**
     * Returns the pre-calculated height of this component.
     * @return The total vertical space this component occupies.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
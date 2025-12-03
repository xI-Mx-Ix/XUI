/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.Properties;

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
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));
        
        // Layout calculation logic from original:
        // currentLayoutY += 5 (top margin)
        // 2px thick line
        // currentLayoutY += 10 (bottom margin)
        
        float topMargin = 5;
        float lineThickness = 2;
        float bottomMargin = 10;

        UIPanel line = new UIPanel();
        line.setX(Constraints.pixel(0))
                .setY(Constraints.pixel(topMargin))
                .setWidth(Constraints.pixel(contentWidth))
                .setHeight(Constraints.pixel(lineThickness));

        line.style().set(Properties.BACKGROUND_COLOR, 0xFF404040);

        this.add(line);
        
        // Set total height for the container
        this.renderHeight = topMargin + lineThickness + bottomMargin;
        this.setHeight(Constraints.pixel(renderHeight)); 
    }

    public float getRenderHeight() {
        return renderHeight;
    }
}
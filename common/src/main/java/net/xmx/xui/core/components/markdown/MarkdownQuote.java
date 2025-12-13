/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.text.UIComponent;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Represents a Blockquote in Markdown (>).
 * Renders text with a vertical accent bar and background.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownQuote extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a quote component.
     *
     * @param text         The text inside the quote.
     * @param contentWidth The available width.
     */
    public MarkdownQuote(UIComponent text, float contentWidth) {
        float padding = 4;
        float barWidth = 2;
        float quoteX = 10;
        int fontHeight = UIRenderImpl.getInstance().getFontHeight();

        // Create the text widget first to measure it.
        // We style it Gray and Italic to represent a quote.
        UIWrappedText textWidget = MarkdownUtils.createWrappingText(
                text.copy().setColor(0xFFAAAAAA).setItalic(true),
                quoteX + padding,
                contentWidth
        );

        // We shift the widget UP by fontHeight to make the visible text start exactly at 'padding'.
        // This compensates for the empty line inserted by creatingWrappingText.
        textWidget.setY(Constraints.pixel(padding - fontHeight));
        textWidget.layout();

        // Height Correction: The widget height includes the empty line.
        // Actual visual height = widget.height - fontHeight.
        // Total panel height = visual_text_height + padding*2.
        float panelHeight = (textWidget.getHeight() - fontHeight) + (padding * 2);

        this.setWidth(Constraints.pixel(contentWidth));
        this.setHeight(Constraints.pixel(panelHeight));

        // Visuals: Dark background with a vertical accent bar
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0x40000000)
                .set(Properties.BORDER_THICKNESS, 0f);

        // The vertical bar on the left
        UIPanel bar = new UIPanel();
        bar.setX(Constraints.pixel(0))
                .setY(Constraints.pixel(0))
                .setWidth(Constraints.pixel(barWidth))
                .setHeight(Constraints.relative(1.0f));
        bar.style().set(Properties.BACKGROUND_COLOR, 0xFFAAAAAA);

        this.add(bar);
        this.add(textWidget);
        
        // Original logic added 5px spacing after the panel
        this.renderHeight = panelHeight + 5;
    }

    /**
     * Returns the pre-calculated height of this component.
     * @return The total vertical space this component occupies.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
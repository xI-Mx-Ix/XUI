/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

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
     * @param font         The font to use for the quote text.
     */
    public MarkdownQuote(TextComponent text, float contentWidth, Font font) {
        float padding = 4;
        float barWidth = 2;
        float quoteX = 10;
        
        // Use dynamic font height
        float fontHeight = font.getLineHeight();

        // Create the text widget first to measure it.
        // We style it Gray and Italic to represent a quote.
        TextComponent styled = text.copy().setColor(0xFFAAAAAA).setItalic(true);
        
        // Apply the font
        MarkdownUtils.applyFontRecursive(styled, font);

        UIWrappedText textWidget = MarkdownUtils.createWrappingText(
                styled,
                quoteX + padding,
                contentWidth
        );

        // We shift the widget UP by fontHeight to make the visible text start exactly at 'padding'.
        // This compensates for the empty line inserted by creatingWrappingText.
        textWidget.setY(Layout.pixel(padding - fontHeight + 1));
        textWidget.layout();

        // Height Correction: The widget height includes the empty line.
        // Actual visual height = widget.height - fontHeight.
        // Total panel height = visual_text_height + padding*2.
        float panelHeight = (textWidget.getHeight() - fontHeight) + (padding * 2);

        this.setWidth(Layout.pixel(contentWidth));
        this.setHeight(Layout.pixel(panelHeight));

        // Visuals: Dark background with a vertical accent bar
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0x40000000)
                .set(ThemeProperties.BORDER_THICKNESS, 0f);

        // The vertical bar on the left
        UIPanel bar = new UIPanel();
        bar.setX(Layout.pixel(0))
                .setY(Layout.pixel(0))
                .setWidth(Layout.pixel(barWidth))
                .setHeight(Layout.relative(1.0f));
        bar.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFFAAAAAA);

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
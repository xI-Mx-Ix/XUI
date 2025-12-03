/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;

/**
 * Represents a Header element in Markdown (#, ##, ###).
 * Uses standard text wrapping with specific styling.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownHeader extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a header component.
     *
     * @param text         The raw text of the header.
     * @param color        The color associated with the header level.
     * @param contentWidth The available width for wrapping.
     */
    public MarkdownHeader(Component text, ChatFormatting color, float contentWidth) {
        // Transparent background
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));

        // Use color and style to distinguish headers
        MutableComponent styled = text.copy().withStyle(ChatFormatting.BOLD, color);

        UIText widget = MarkdownUtils.createWrappingText(styled, 0, contentWidth);
        
        // Add extra padding above headers (logic from original code: currentLayoutY += 5)
        float topPadding = 5;
        widget.setY(Constraints.pixel(topPadding));

        this.add(widget);

        // We use slightly less padding here because the "empty line" trick in createWrappingText
        // already adds ~9px of height at the top of the widget.
        // Logic: topPadding + widgetHeight + 2 (bottom margin)
        this.renderHeight = topPadding + widget.getHeight() + 2;
        this.setHeight(Constraints.pixel(renderHeight));
    }

    /**
     * Returns the pre-calculated height of this component including padding.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
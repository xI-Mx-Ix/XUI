/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A basic text label component.
 * It automatically resizes itself to fit the text content, or wraps text if a width is constrained.
 *
 * @author xI-Mx-Ix
 */
public class UIText extends UIWidget {

    private Component text;
    private boolean centered = false;
    private boolean shadow = true;
    private boolean wrapText = false;

    public UIText(String text) {
        this(Component.literal(text));
    }

    public UIText(Component text) {
        this.text = text;
        this.style().set(Properties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Calculates the layout dimensions.
     * Overridden to handle both auto-sizing (single line) and height-calculation for wrapped text.
     */
    @Override
    public void layout() {
        // Calculate initial dimensions based on parent constraints
        super.layout();

        if (!isVisible) return;

        if (wrapText) {
            // If wrapping is enabled, the width is determined by the constraint (e.g. fixed or relative to parent).
            // We use that calculated width to determine the necessary height.
            int wrappedHeight = Minecraft.getInstance().font.wordWrapHeight(text, (int) this.width);

            // Enforce the calculated height
            this.heightConstraint = Constraints.pixel(wrappedHeight);

            // Recalculate to apply the new height
            super.layout();
        } else {
            // Standard single-line behavior: strict pixel constraints matching the text size
            int strWidth = Minecraft.getInstance().font.width(text);
            int strHeight = Minecraft.getInstance().font.lineHeight;

            this.widthConstraint = Constraints.pixel(strWidth);
            this.heightConstraint = Constraints.pixel(strHeight);

            super.layout();
        }
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int color = getColor(Properties.TEXT_COLOR, state, partialTicks);

        float drawX = x;
        float drawY = y;

        if (wrapText) {
            renderer.drawWrappedText(text, drawX, drawY, width, color, shadow);
        } else {
            // Adjust position if internal centering flag is set (relative to self bounding box)
            if (centered) {
                drawX = x + (width - renderer.getTextWidth(text)) / 2.0f;
                drawY = y + (height - renderer.getFontHeight()) / 2.0f;
            }
            renderer.drawText(text, drawX, drawY, color, shadow);
        }
    }

    /**
     * Sets whether the text should wrap to the next line when exceeding width.
     * Note: If set to true, you must provide a width constraint to the widget.
     *
     * @param wrapText true to enable wrapping.
     * @return This widget.
     */
    public UIText setWrapping(boolean wrapText) {
        this.wrapText = wrapText;
        return this;
    }

    public UIText setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public UIText setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public UIText setText(String text) {
        return setText(Component.literal(text));
    }

    public UIText setText(Component text) {
        this.text = text;
        return this;
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A basic text label component.
 * It automatically resizes itself to fit the text content.
 *
 * @author xI-Mx-Ix
 */
public class UIText extends UIWidget {

    private String text;
    private boolean centered = false;
    private boolean shadow = true;

    public UIText(String text) {
        this.text = text;
        this.style().set(Properties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Calculates the layout dimensions.
     * Overridden to automatically set the width and height constraints based on the
     * font renderer's measurement of the current string.
     */
    @Override
    public void layout() {
        // Measure the text using the Minecraft Font instance
        int strWidth = Minecraft.getInstance().font.width(text);
        int strHeight = Minecraft.getInstance().font.lineHeight;

        // Apply strict pixel constraints matching the text size
        this.widthConstraint = Constraints.pixel(strWidth);
        this.heightConstraint = Constraints.pixel(strHeight);

        // Proceed with standard layout calculation
        super.layout();
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int color = getColor(Properties.TEXT_COLOR, state, partialTicks);

        float drawX = x;
        float drawY = y;

        // Adjust position if internal centering flag is set (relative to self bounding box)
        if (centered) {
            drawX = x + (width - renderer.getStringWidth(text)) / 2.0f;
            drawY = y + (height - renderer.getFontHeight()) / 2.0f;
        }

        renderer.drawString(text, drawX, drawY, color, shadow);
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
        this.text = text;
        return this;
    }
}
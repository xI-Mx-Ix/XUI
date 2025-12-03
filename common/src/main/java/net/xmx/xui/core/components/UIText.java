/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A standard text component representing a single line of text.
 * It supports adding multiple text segments (horizontal concatenation) via {@link #addText(Component)},
 * but does not support automatic line wrapping.
 * The widget automatically resizes its width and height to fit the content.
 *
 * @author xI-Mx-Ix
 */
public class UIText extends UIWidget {

    // We use a MutableComponent to accumulate appended text segments into a single logic line.
    private MutableComponent content;
    private boolean centered = false;
    private boolean shadow = true;

    /**
     * Constructs a text widget with no initial content.
     * The default text color is set to white.
     */
    public UIText() {
        this.content = Component.empty();
        this.style().set(Properties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Appends a text component to the current line.
     * This effectively concatenates the new text horizontally.
     *
     * @param text The component to append.
     * @return This widget instance for chaining.
     */
    public UIText addText(Component text) {
        this.content.append(text);
        return this;
    }

    /**
     * Clears the current content and sets a new single line of text.
     *
     * @param text The string literal to set.
     * @return This widget instance for chaining.
     */
    public UIText setText(String text) {
        return setText(Component.literal(text));
    }

    /**
     * Clears the current content and sets a new single line of text.
     *
     * @param text The component to set.
     * @return This widget instance for chaining.
     */
    public UIText setText(Component text) {
        // Since we need a mutable accumulator, we copy the input
        this.content = text.copy();
        return this;
    }

    /**
     * Returns the accumulated text content.
     *
     * @return The text component.
     */
    public Component getText() {
        return this.content;
    }

    /**
     * Configures whether the text should be rendered with a drop shadow.
     *
     * @param shadow True to enable shadow, false to disable.
     * @return This widget instance for chaining.
     */
    public UIText setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
     * Configures whether the text should be horizontally centered.
     * This has a visual effect only if the widget's width is externally constrained
     * to be larger than the text content.
     *
     * @param centered True to center the text.
     * @return This widget instance for chaining.
     */
    public UIText setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    /**
     * Calculates the layout dimensions based on the single line of content.
     * Overrides width and height constraints to strictly fit the text size.
     */
    @Override
    public void layout() {
        if (!isVisible) return;

        // Calculate required dimensions for the single line
        int textWidth = Minecraft.getInstance().font.width(this.content);
        int textHeight = Minecraft.getInstance().font.lineHeight;

        // Apply dimensions to constraints
        this.widthConstraint = Constraints.pixel(textWidth);
        this.heightConstraint = Constraints.pixel(textHeight);

        // Perform standard layout calculation
        super.layout();
    }

    /**
     * Renders the single text line.
     *
     * @param renderer     The render interface.
     * @param mouseX       The current mouse X coordinate.
     * @param mouseY       The current mouse Y coordinate.
     * @param partialTicks The partial tick time for interpolation.
     * @param state        The current UI state.
     */
    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int color = getColor(Properties.TEXT_COLOR, state, partialTicks);

        float drawX = this.x;
        float drawY = this.y;

        // Calculate centering offset if enabled
        if (centered) {
            drawX = this.x + (this.width - renderer.getTextWidth(this.content)) / 2.0f;
        }

        renderer.drawText(this.content, drawX, drawY, color, shadow);
    }
}
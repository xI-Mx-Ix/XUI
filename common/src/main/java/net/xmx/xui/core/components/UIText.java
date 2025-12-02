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

import java.util.ArrayList;
import java.util.List;

/**
 * A flexible text component supporting multiple lines.
 * Each line can be independently configured to wrap or remain on a single line.
 * Automatically calculates height based on content.
 *
 * @author xI-Mx-Ix
 */
public class UIText extends UIWidget {

    /**
     * Internal container for a text line.
     */
    private static class TextLine {
        final Component content;
        final boolean wrap;

        TextLine(Component content, boolean wrap) {
            this.content = content;
            this.wrap = wrap;
        }
    }

    private final List<TextLine> lines = new ArrayList<>();
    private boolean centered = false;
    private boolean shadow = true;

    /**
     * Constructs a text widget with no initial content.
     * Use {@link #setText(Component)} or {@link #addText(Component)} to populate.
     */
    public UIText() {
        this.style().set(Properties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Adds a new line of text with wrapping disabled by default.
     *
     * @param text The component to add.
     * @return This widget instance.
     */
    public UIText addText(Component text) {
        return addText(text, false);
    }

    /**
     * Adds a new line of text with specific wrapping configuration.
     *
     * @param text The component to add.
     * @param wrap If true, this line will wrap based on the widget's width.
     *             If false, it extends as far as needed (potentially overflowing).
     * @return This widget instance.
     */
    public UIText addText(Component text, boolean wrap) {
        this.lines.add(new TextLine(text, wrap));
        return this;
    }

    /**
     * Clears all existing text and sets a single new line (no wrap).
     *
     * @param text The new text.
     * @return This widget instance.
     */
    public UIText setText(String text) {
        return setText(Component.literal(text));
    }

    /**
     * Clears all existing text and sets a single new line (no wrap).
     *
     * @param text The new component.
     * @return This widget instance.
     */
    public UIText setText(Component text) {
        this.lines.clear();
        this.addText(text, false);
        return this;
    }

    /**
     * Calculates the layout dimensions.
     * Iterates through all added lines to determine the total required height.
     * If no lines have wrapping enabled, it also autosizes the width.
     */
    @Override
    public void layout() {
        if (!isVisible) return;

        // 1. Run standard layout to resolve the available width (from constraints like relative(1.0))
        super.layout();

        int totalHeight = 0;
        int maxLineWidth = 0;
        boolean anyLineWraps = false;

        // 2. Calculate dimensions based on lines
        for (TextLine line : lines) {
            if (line.wrap) {
                // For wrapping lines, height depends on the current resolved width
                totalHeight += Minecraft.getInstance().font.wordWrapHeight(line.content, (int) this.width);
                anyLineWraps = true;
            } else {
                // For non-wrapping lines, standard height and width calculation
                totalHeight += Minecraft.getInstance().font.lineHeight;
                int lineWidth = Minecraft.getInstance().font.width(line.content);
                if (lineWidth > maxLineWidth) {
                    maxLineWidth = lineWidth;
                }
            }
        }

        // 3. Update constraints based on content
        this.heightConstraint = Constraints.pixel(totalHeight);

        // If no lines require wrapping, we shrink the width to fit the widest line.
        // If wrapping is involved, we respect the width set by the parent/user constraints.
        if (!anyLineWraps) {
            this.widthConstraint = Constraints.pixel(maxLineWidth);
        }

        // 4. Re-apply to update x/y/width/height fields with new constraints
        super.layout();
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int color = getColor(Properties.TEXT_COLOR, state, partialTicks);

        float currentY = this.y;

        for (TextLine line : lines) {
            float lineDrawX = this.x;
            int lineHeight;

            if (line.wrap) {
                // Render wrapped block
                renderer.drawWrappedText(line.content, lineDrawX, currentY, this.width, color, shadow);
                lineHeight = renderer.getWordWrapHeight(line.content, (int) this.width);
            } else {
                // Render single line
                if (centered) {
                    // Center this specific line within the widget width
                    lineDrawX = this.x + (this.width - renderer.getTextWidth(line.content)) / 2.0f;
                }
                renderer.drawText(line.content, lineDrawX, currentY, color, shadow);
                lineHeight = renderer.getFontHeight();
            }

            // Advance Y position for the next line
            currentY += lineHeight;
        }
    }

    public UIText setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public UIText setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }
}
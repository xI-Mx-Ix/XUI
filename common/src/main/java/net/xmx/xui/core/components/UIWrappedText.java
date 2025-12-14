/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * A flexible text component supporting multiple vertical lines.
 * Each line can be independently configured to wrap automatically or remain on a single line.
 * The widget automatically calculates its total height based on the content of all lines.
 * <p>
 * Supports custom fonts, which are applied to all text lines contained within this widget.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIWrappedText extends UIWidget {

    /**
     * Internal container for a text line configuration.
     */
    private static class TextLine {
        final TextComponent content;
        final boolean wrap;

        /**
         * Creates a new text line definition.
         *
         * @param content The text component for this line.
         * @param wrap    True if this line should wrap within the widget's width;
         *                False to force it to render as a single line (potentially overflowing).
         */
        TextLine(TextComponent content, boolean wrap) {
            this.content = content;
            this.wrap = wrap;
        }
    }

    private final List<TextLine> lines = new ArrayList<>();
    private boolean centered = false;
    private boolean shadow = true;

    // Optional custom font applied to all lines in this widget.
    private Font customFont = null;

    /**
     * Constructs a wrapped text widget with no initial content.
     * The default text color is set to white.
     */
    public UIWrappedText() {
        this.style().set(ThemeProperties.TEXT_COLOR, 0xFFFFFFFF);
    }

    /**
     * Sets a custom font for this widget.
     * This font overrides the font of individual text components added to this widget.
     *
     * @param font The font to use, or null to use the component's default font.
     * @return This widget instance for chaining.
     */
    public UIWrappedText setFont(Font font) {
        this.customFont = font;
        return this;
    }

    /**
     * Returns the currently configured custom font.
     *
     * @return The custom font, or null if none is set.
     */
    public Font getFont() {
        return this.customFont;
    }

    /**
     * Adds a new line of text with wrapping disabled by default.
     * This acts like appending a new paragraph that does not break automatically.
     *
     * @param text The component to add as a new line.
     * @return This widget instance for chaining.
     */
    public UIWrappedText addText(TextComponent text) {
        return addText(text, false);
    }

    /**
     * Adds a new line of text with specific wrapping configuration.
     *
     * @param text The component to add as a new line.
     * @param wrap If true, this line will wrap based on the widget's available width.
     *             If false, it extends as far as needed (potentially overflowing).
     * @return This widget instance for chaining.
     */
    public UIWrappedText addText(TextComponent text, boolean wrap) {
        this.lines.add(new TextLine(text, wrap));
        return this;
    }

    /**
     * Clears all existing text lines and sets a single new line (no wrap).
     *
     * @param text The new text string.
     * @return This widget instance for chaining.
     */
    public UIWrappedText setText(String text) {
        return setText(TextComponent.literal(text));
    }

    /**
     * Clears all existing text lines and sets a single new line (no wrap).
     *
     * @param text The new text component.
     * @return This widget instance for chaining.
     */
    public UIWrappedText setText(TextComponent text) {
        this.lines.clear();
        this.addText(text, false);
        return this;
    }

    /**
     * Configures whether single (non-wrapped) lines should be centered.
     * Wrapped lines typically adhere to standard text flow alignment.
     *
     * @param centered True to center non-wrapped lines.
     * @return This widget instance for chaining.
     */
    public UIWrappedText setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    /**
     * Configures whether the text should be rendered with a drop shadow.
     *
     * @param shadow True to enable shadow, false to disable.
     * @return This widget instance for chaining.
     */
    public UIWrappedText setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
     * Applies the widget's custom font to the text component if one is configured.
     * This ensures correct size calculations and rendering for the component.
     *
     * @param component The component to update.
     */
    private void applyFont(TextComponent component) {
        if (this.customFont != null) {
            component.setFont(this.customFont);
        }
    }

    /**
     * Calculates the layout dimensions.
     * Iterates through all added lines to determine the total required height.
     * If no lines have wrapping enabled, it also autosizes the width to fit the widest line.
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
            // Ensure the component has the correct font before calculating dimensions
            applyFont(line.content);

            if (line.wrap) {
                // For wrapping lines, height depends on the current resolved width
                totalHeight += TextComponent.getWordWrapHeight(line.content, (int) this.width);
                anyLineWraps = true;
            } else {
                // For non-wrapping lines, standard height and width calculation
                totalHeight += TextComponent.getFontHeight();
                int lineWidth = TextComponent.getTextWidth(line.content);
                if (lineWidth > maxLineWidth) {
                    maxLineWidth = lineWidth;
                }
            }
        }

        // 3. Update constraints based on content
        this.heightConstraint = Layout.pixel(totalHeight);

        // If no lines require wrapping, we shrink the width to fit the widest line.
        // If wrapping is involved, we respect the width set by the parent/user constraints.
        if (!anyLineWraps) {
            this.widthConstraint = Layout.pixel(maxLineWidth);
        }

        // 4. Re-apply to update x/y/width/height fields with new constraints
        super.layout();
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int color = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);

        float currentY = this.y;

        for (TextLine line : lines) {
            // Ensure the component uses the correct font for rendering
            applyFont(line.content);

            float lineDrawX = this.x;
            int lineHeight;

            if (line.wrap) {
                // Render wrapped block
                renderer.drawWrappedText(line.content, lineDrawX, currentY, this.width, color, shadow);
                lineHeight = TextComponent.getWordWrapHeight(line.content, (int) this.width);
            } else {
                // Render single line
                if (centered) {
                    // Center this specific line within the widget width
                    lineDrawX = this.x + (this.width - TextComponent.getTextWidth(line.content)) / 2.0f;
                }
                renderer.drawText(line.content, lineDrawX, currentY, color, shadow);
                lineHeight = TextComponent.getFontHeight();
            }

            // Advance Y position for the next line
            currentY += lineHeight;
        }
    }
}
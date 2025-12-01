/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.style.Properties;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget that parses basic Markdown syntax and renders it using native UI components.
 * It acts as a vertical layout container for the generated text and code block widgets.
 *
 * Supported features:
 * - Headers (#, ##, ###)
 * - Code Blocks (```)
 * - Blockquotes (>)
 * - Lists (- or *)
 * - Inline styles (**bold**, *italic*, `code`)
 *
 * @author xI-Mx-Ix
 */
public class UIMarkdown extends UIPanel {

    private String rawMarkdown = "";
    private float currentLayoutY = 0;
    private final float contentWidth;

    /**
     * Constructs a Markdown viewer.
     *
     * @param contentWidth The fixed width available for text wrapping.
     *                     Important for correct height calculation inside ScrollPanels.
     */
    public UIMarkdown(float contentWidth) {
        this.contentWidth = contentWidth;
        // Transparent background by default
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));
    }

    /**
     * Sets the markdown content and rebuilds the UI structure.
     *
     * @param markdown The raw markdown string.
     * @return This widget.
     */
    public UIMarkdown setMarkdown(String markdown) {
        this.rawMarkdown = markdown;
        rebuild();
        return this;
    }

    /**
     * Clears current children and parses the markdown to create new widgets.
     */
    private void rebuild() {
        this.children.clear();
        this.currentLayoutY = 0;

        String[] lines = rawMarkdown.split("\n");
        List<String> codeBlockBuffer = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // --- Code Block Handling ---
            if (trimmed.startsWith("```")) {
                if (codeBlockBuffer == null) {
                    // Start code block
                    codeBlockBuffer = new ArrayList<>();
                } else {
                    // End code block
                    addCodeBlock(codeBlockBuffer);
                    codeBlockBuffer = null;
                }
                continue;
            }

            if (codeBlockBuffer != null) {
                codeBlockBuffer.add(line); // Add raw line (keep indentation)
                continue;
            }

            // --- Standard Markdown parsing ---
            if (trimmed.isEmpty()) {
                currentLayoutY += 10; // Paragraph spacing
            } else if (trimmed.startsWith("# ")) {
                addHeader(parseInline(trimmed.substring(2)), 1.5f, ChatFormatting.GOLD);
            } else if (trimmed.startsWith("## ")) {
                addHeader(parseInline(trimmed.substring(3)), 1.2f, ChatFormatting.YELLOW);
            } else if (trimmed.startsWith("### ")) {
                addHeader(parseInline(trimmed.substring(4)), 1.0f, ChatFormatting.AQUA);
            } else if (trimmed.startsWith("> ")) {
                addQuote(parseInline(trimmed.substring(2)));
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                addListItem(parseInline(trimmed.substring(2)));
            } else {
                addParagraph(parseInline(line));
            }
        }

        // Finalize height constraint based on total content
        this.setHeight(Constraints.pixel(currentLayoutY));
    }

    // --- Widget Generators ---

    /**
     * Helper to create a text widget that wraps correctly.
     * Because the current UIText constructor defaults to non-wrapping for the first line
     * and we cannot modify UIText, we create a widget with an empty first line
     * and add the actual content as a second line with wrapping enabled.
     *
     * @param content     The text component to display.
     * @param widthOffset The X offset (padding) to subtract from available width.
     * @return A configured UIText widget.
     */
    private UIText createWrappingText(Component content, float widthOffset) {
        // 1. Create with empty string (Lines: [Empty, NoWrap])
        // This acts as a small top padding (line height ~9px)
        UIText widget = new UIText(Component.empty());

        // 2. Add actual content with wrapping enabled (Lines: [Empty, NoWrap], [Content, Wrap])
        widget.addText(content, true);

        widget.setX(Constraints.pixel(widthOffset));
        widget.setY(Constraints.pixel(currentLayoutY));
        widget.setWidth(Constraints.pixel(contentWidth - widthOffset));

        // Force layout calculation immediately so we can read the height
        widget.layout();
        return widget;
    }

    private void addHeader(Component text, float scaleIgnoredForNow, ChatFormatting color) {
        // Since standard fonts don't support arbitrary scaling easily without matrix manipulation,
        // we use color and style to distinguish headers for now.
        MutableComponent styled = text.copy().withStyle(ChatFormatting.BOLD, color);

        // Add extra padding above headers
        currentLayoutY += 5;

        UIText widget = createWrappingText(styled, 0);

        this.add(widget);
        // We use slightly less padding here because the "empty line" trick in createWrappingText
        // already adds ~9px of height at the top of the widget.
        currentLayoutY += widget.getHeight() + 2;
    }

    private void addParagraph(Component text) {
        UIText widget = createWrappingText(text, 0);

        this.add(widget);
        currentLayoutY += widget.getHeight();
    }

    private void addQuote(Component text) {
        // Container for quote
        float padding = 4;
        float barWidth = 2;
        float quoteX = 10;
        int fontHeight = net.minecraft.client.Minecraft.getInstance().font.lineHeight;

        // Create the text widget first to measure it
        UIText textWidget = createWrappingText(text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), quoteX + padding);

        // FIX: The textWidget has an empty first line (~9px) that pushes text down.
        // We shift the widget UP by fontHeight to make the visible text start exactly at 'padding'.
        // NOTE: Standard UI clipping usually doesn't clip negative relative coordinates unless scissor is strict.
        textWidget.setY(Constraints.pixel(padding - fontHeight));
        textWidget.layout();

        // Height Correction: The widget height includes the empty line.
        // Actual visual height = widget.height - fontHeight.
        // Total panel height = visual_text_height + padding*2.
        float panelHeight = (textWidget.getHeight() - fontHeight) + (padding * 2);

        UIPanel quotePanel = new UIPanel();
        quotePanel.setX(Constraints.pixel(0));
        quotePanel.setY(Constraints.pixel(currentLayoutY));
        quotePanel.setWidth(Constraints.pixel(contentWidth));
        quotePanel.setHeight(Constraints.pixel(panelHeight));

        // Visuals: Dark background with a vertical accent bar
        quotePanel.style()
                .set(Properties.BACKGROUND_COLOR, 0x40000000)
                .set(Properties.BORDER_THICKNESS, 0f);

        // The vertical bar
        UIPanel bar = new UIPanel();
        bar.setX(Constraints.pixel(0))
                .setY(Constraints.pixel(0))
                .setWidth(Constraints.pixel(barWidth))
                .setHeight(Constraints.relative(1.0f));
        bar.style().set(Properties.BACKGROUND_COLOR, 0xFFAAAAAA);

        quotePanel.add(bar);
        quotePanel.add(textWidget);

        this.add(quotePanel);
        currentLayoutY += panelHeight + 5;
    }

    private void addListItem(Component text) {
        float bulletWidth = 15;

        // Bullet point doesn't need wrapping, simple creation
        UIText bullet = new UIText("•");
        bullet.setX(Constraints.pixel(5));

        // Determine Y offset.
        // createWrappingText adds an empty line (~9px) at the start.
        // We align the bullet to the second line of the content widget (where text actually starts).
        float fontHeight = net.minecraft.client.Minecraft.getInstance().font.lineHeight;
        bullet.setY(Constraints.pixel(currentLayoutY + fontHeight));

        UIText content = createWrappingText(text, bulletWidth);

        this.add(bullet);
        this.add(content);

        currentLayoutY += content.getHeight();
    }

    private void addCodeBlock(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append("\n");

        String code = sb.toString();
        int fontHeight = net.minecraft.client.Minecraft.getInstance().font.lineHeight;

        // Monospace-ish look (using standard font but grey)
        Component text = Component.literal(code).withStyle(ChatFormatting.GRAY);

        // Code blocks usually don't wrap, they scroll or clip.
        // For this implementation, we will allow wrapping via our helper.
        UIText content = createWrappingText(text, 0);

        float padding = 6;
        // Height Correction: Subtract empty line height
        float totalHeight = (content.getHeight() - fontHeight) + (padding * 2);

        UIPanel codePanel = new UIPanel();
        codePanel.setX(Constraints.pixel(0));
        codePanel.setY(Constraints.pixel(currentLayoutY));
        codePanel.setWidth(Constraints.pixel(contentWidth));
        codePanel.setHeight(Constraints.pixel(totalHeight));

        codePanel.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF151515) // Very dark
                .set(Properties.BORDER_RADIUS, 4.0f)
                .set(Properties.BORDER_COLOR, 0xFF303030)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // Adjust content position inside panel with vertical offset fix
        content.setX(Constraints.pixel(padding));
        content.setY(Constraints.pixel(padding - fontHeight));
        content.setWidth(Constraints.pixel(contentWidth - (padding * 2)));

        codePanel.add(content);
        this.add(codePanel);

        currentLayoutY += totalHeight + 10;
    }

    /**
     * Primitive regex parser for inline markdown styles.
     * **bold** -> Bold
     * *italic* -> Italic
     * `code` -> Gray/Monospace
     */
    private Component parseInline(String text) {
        // Note: Java Regex doesn't support recursive parsing easily.
        // We do a simple pass replacing markers with legacy formatting codes for simplicity,
        // then convert to Component.

        // Escape existing section signs to prevent injection
        String safe = text.replace("§", "");

        // Replace Bold **...**
        safe = safe.replaceAll("\\*\\*(.*?)\\*\\*", "§l$1§r");

        // Replace Italic *...*
        safe = safe.replaceAll("\\*(.*?)\\*", "§o$1§r");

        // Replace Code `...` (Yellow/Gray)
        safe = safe.replaceAll("`(.*?)`", "§7$1§r");

        return Component.literal(safe);
    }
}
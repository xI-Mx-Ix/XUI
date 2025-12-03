/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.minecraft.ChatFormatting;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.Properties;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget that parses comprehensive Markdown syntax and renders it using native UI components.
 * Updated to use native Checkbox rendering for Task Lists.
 *
 * @author xI-Mx-Ix
 */
public class UIMarkdown extends UIPanel {

    private String rawMarkdown = "";
    private float currentLayoutY = 0;
    private float contentWidth = 200.0f;

    /**
     * Constructs a Markdown viewer with default settings.
     * Use {@link #setContentWidth(float)} to define wrap width and {@link #setMarkdown(String)} to set content.
     */
    public UIMarkdown() {
        // Transparent background by default
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));
    }

    /**
     * Sets the fixed width available for text wrapping.
     * Important for correct height calculation inside ScrollPanels.
     *
     * @param width The content width in pixels.
     * @return This widget.
     */
    public UIMarkdown setContentWidth(float width) {
        this.contentWidth = width;
        this.setWidth(Constraints.pixel(width));
        if (!rawMarkdown.isEmpty()) {
            rebuild(); // Rebuild layout if content exists
        }
        return this;
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
        List<String> tableBuffer = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // --- 1. Code Block Handling ---
            if (trimmed.startsWith("```")) {
                if (codeBlockBuffer == null) {
                    if (tableBuffer != null) { flushTable(tableBuffer); tableBuffer = null; }
                    codeBlockBuffer = new ArrayList<>();
                } else {
                    addCodeBlock(codeBlockBuffer);
                    codeBlockBuffer = null;
                }
                continue;
            }

            if (codeBlockBuffer != null) {
                codeBlockBuffer.add(line);
                continue;
            }

            // --- 2. Table Handling ---
            if (trimmed.startsWith("|")) {
                if (tableBuffer == null) tableBuffer = new ArrayList<>();
                tableBuffer.add(line);
                continue;
            } else if (tableBuffer != null) {
                flushTable(tableBuffer);
                tableBuffer = null;
            }

            // --- 3. Standard Parsing ---
            if (trimmed.isEmpty()) {
                currentLayoutY += 8;
                continue;
            }

            if (trimmed.equals("---") || trimmed.equals("***")) {
                addSeparator();
            } else if (trimmed.startsWith("# ")) {
                addHeader(trimmed.substring(2), ChatFormatting.GOLD);
            } else if (trimmed.startsWith("## ")) {
                addHeader(trimmed.substring(3), ChatFormatting.YELLOW);
            } else if (trimmed.startsWith("### ")) {
                addHeader(trimmed.substring(4), ChatFormatting.AQUA);
            } else if (trimmed.startsWith("> ")) {
                addQuote(trimmed.substring(2));
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                // Check for Task List Syntax
                if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ")) {
                    boolean checked = trimmed.startsWith("- [x] ");
                    // Substring: "- [x] " is 6 chars long
                    addTaskListItem(trimmed.substring(6), checked);
                } else {
                    addListItem(trimmed.substring(2));
                }
            } else {
                addFlowParagraph(line);
            }
        }

        if (tableBuffer != null) flushTable(tableBuffer);
        if (codeBlockBuffer != null) addCodeBlock(codeBlockBuffer);

        this.setHeight(Constraints.pixel(currentLayoutY));
    }

    // --- Component Generators ---

    private void flushTable(List<String> lines) {
        MarkdownTable table = new MarkdownTable(lines, contentWidth);
        table.setY(Constraints.pixel(currentLayoutY));
        this.add(table);

        currentLayoutY += table.getRenderHeight();
    }

    private void addSeparator() {
        MarkdownSeparator separator = new MarkdownSeparator(contentWidth);
        separator.setY(Constraints.pixel(currentLayoutY));
        this.add(separator);

        currentLayoutY += separator.getRenderHeight();
    }

    private void addHeader(String rawText, ChatFormatting color) {
        MarkdownHeader header = new MarkdownHeader(MarkdownUtils.parseInline(rawText), color, contentWidth);
        header.setY(Constraints.pixel(currentLayoutY));
        this.add(header);

        currentLayoutY += header.getRenderHeight();
    }

    private void addQuote(String rawText) {
        MarkdownQuote quote = new MarkdownQuote(MarkdownUtils.parseInline(rawText), contentWidth);
        quote.setY(Constraints.pixel(currentLayoutY));
        this.add(quote);

        currentLayoutY += quote.getRenderHeight();
    }

    private void addListItem(String rawText) {
        MarkdownListItem item = new MarkdownListItem(rawText, contentWidth);
        item.setY(Constraints.pixel(currentLayoutY));
        this.add(item);

        currentLayoutY += item.getRenderHeight();
    }

    /**
     * Adds a Task List item using the new visual renderer.
     */
    private void addTaskListItem(String rawText, boolean checked) {
        MarkdownTaskListItem item = new MarkdownTaskListItem(rawText, checked, contentWidth);
        item.setY(Constraints.pixel(currentLayoutY));
        this.add(item);

        currentLayoutY += item.getRenderHeight();
    }

    private void addCodeBlock(List<String> lines) {
        MarkdownCodeBlock block = new MarkdownCodeBlock(lines, contentWidth);
        block.setY(Constraints.pixel(currentLayoutY));
        this.add(block);

        currentLayoutY += block.getRenderHeight();
    }

    private void addFlowParagraph(String line) {
        MarkdownParagraph paragraph = new MarkdownParagraph(line, contentWidth);
        paragraph.setY(Constraints.pixel(currentLayoutY));
        this.add(paragraph);

        currentLayoutY += paragraph.getRenderHeight();
    }
}
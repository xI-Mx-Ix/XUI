/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.style.ThemeProperties;

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

    // --- Font Configurations ---
    // Default to Vanilla to ensure compatibility if nothing is set
    private Font regularFont = DefaultFonts.getVanilla();
    private Font headerFont = DefaultFonts.getVanilla();
    private Font codeFont = DefaultFonts.getVanilla();

    /**
     * Constructs a Markdown viewer with default settings.
     * Use {@link #setContentWidth(float)} to define wrap width and {@link #setMarkdown(String)} to set content.
     */
    public UIMarkdown() {
        // Transparent background by default
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));
    }

    /**
     * Sets the font family for ALL markdown elements (Text, Headers, Code, Tables).
     * This is a convenience method that overrides any previously set specific fonts.
     *
     * @param font The font to use for everything.
     * @return This widget for chaining.
     */
    public UIMarkdown setFont(Font font) {
        this.regularFont = font;
        this.headerFont = font;
        this.codeFont = font;
        // If content already exists, we must rebuild the widgets to apply the new font
        if (!rawMarkdown.isEmpty()) rebuild();
        return this;
    }

    /**
     * Sets the font used for regular text elements.
     * Includes paragraphs, lists, quotes, and table content.
     *
     * @param font The font family.
     * @return This widget for chaining.
     */
    public UIMarkdown setRegularFont(Font font) {
        this.regularFont = font;
        if (!rawMarkdown.isEmpty()) rebuild();
        return this;
    }

    /**
     * Sets the font used specifically for header elements (#, ##, ###).
     *
     * @param font The font family.
     * @return This widget for chaining.
     */
    public UIMarkdown setHeaderFont(Font font) {
        this.headerFont = font;
        if (!rawMarkdown.isEmpty()) rebuild();
        return this;
    }

    /**
     * Sets the font used specifically for code blocks (```).
     *
     * @param font The font family.
     * @return This widget for chaining.
     */
    public UIMarkdown setCodeFont(Font font) {
        this.codeFont = font;
        if (!rawMarkdown.isEmpty()) rebuild();
        return this;
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
        this.setWidth(Layout.pixel(width));
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
                addHeader(trimmed.substring(2), 0xFFFFAA00); // GOLD
            } else if (trimmed.startsWith("## ")) {
                addHeader(trimmed.substring(3), 0xFFFFFF55); // YELLOW
            } else if (trimmed.startsWith("### ")) {
                addHeader(trimmed.substring(4), 0xFF55FFFF); // AQUA
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

        this.setHeight(Layout.pixel(currentLayoutY));
    }

    // --- Component Generators ---

    private void flushTable(List<String> lines) {
        // Pass regular font to table
        MarkdownTable table = new MarkdownTable(lines, contentWidth, regularFont);
        table.setY(Layout.pixel(currentLayoutY));
        this.add(table);

        currentLayoutY += table.getRenderHeight();
    }

    private void addSeparator() {
        MarkdownSeparator separator = new MarkdownSeparator(contentWidth);
        separator.setY(Layout.pixel(currentLayoutY));
        this.add(separator);

        currentLayoutY += separator.getRenderHeight();
    }

    private void addHeader(String rawText, int color) {
        // Pass header font
        MarkdownHeader header = new MarkdownHeader(MarkdownUtils.parseInline(rawText), color, contentWidth, headerFont);
        header.setY(Layout.pixel(currentLayoutY));
        this.add(header);

        currentLayoutY += header.getRenderHeight();
    }

    private void addQuote(String rawText) {
        // Pass regular font
        MarkdownQuote quote = new MarkdownQuote(MarkdownUtils.parseInline(rawText), contentWidth, regularFont);
        quote.setY(Layout.pixel(currentLayoutY));
        this.add(quote);

        currentLayoutY += quote.getRenderHeight();
    }

    private void addListItem(String rawText) {
        // Pass regular font
        MarkdownListItem item = new MarkdownListItem(rawText, contentWidth, regularFont);
        item.setY(Layout.pixel(currentLayoutY));
        this.add(item);

        currentLayoutY += item.getRenderHeight();
    }

    /**
     * Adds a Task List item using the new visual renderer.
     */
    private void addTaskListItem(String rawText, boolean checked) {
        // Pass regular font
        MarkdownTaskListItem item = new MarkdownTaskListItem(rawText, checked, contentWidth, regularFont);
        item.setY(Layout.pixel(currentLayoutY));
        this.add(item);

        currentLayoutY += item.getRenderHeight();
    }

    private void addCodeBlock(List<String> lines) {
        // Pass code font
        MarkdownCodeBlock block = new MarkdownCodeBlock(lines, contentWidth, codeFont);
        block.setY(Layout.pixel(currentLayoutY));
        this.add(block);

        currentLayoutY += block.getRenderHeight();
    }

    private void addFlowParagraph(String line) {
        // Pass regular font
        MarkdownParagraph paragraph = new MarkdownParagraph(line, contentWidth, regularFont);
        paragraph.setY(Layout.pixel(currentLayoutY));
        this.add(paragraph);

        currentLayoutY += paragraph.getRenderHeight();
    }
}
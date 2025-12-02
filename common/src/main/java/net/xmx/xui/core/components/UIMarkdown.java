/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.util.URLUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A widget that parses basic Markdown syntax and renders it using native UI components.
 * It acts as a vertical layout container for the generated text and code block widgets.
 *
 * Supported features:
 * - Headers (#, ##, ###)
 * - Code Blocks (```) with Syntax Highlighting
 * - Blockquotes (>)
 * - Lists (- or *)
 * - Inline styles (**bold**, *italic*, `code`)
 * - Horizontal Separators (---)
 * - Clickable Links ([Text](URL))
 *
 * @author xI-Mx-Ix
 */
public class UIMarkdown extends UIPanel {

    private String rawMarkdown = "";
    private float currentLayoutY = 0;
    private float contentWidth = 200.0f; // Default width

    // Pattern for detecting Markdown links: [Label](Url)
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");

    // Pattern for detecting tokens in code blocks for syntax highlighting
    // Captures: Strings, Comments, Keywords, Numbers
    private static final Pattern CODE_TOKEN_PATTERN = Pattern.compile(
            "(\".*?\"|'.*?'|//.*|/\\*[\\s\\S]*?\\*/|\\b(public|private|protected|class|interface|enum|extends|implements|void|int|float|double|boolean|char|String|return|if|else|for|while|do|switch|case|break|continue|new|null|true|false|this|super|static|final|abstract|synchronized|volatile|transient|import|package)\\b|\\b\\d+\\b)"
    );

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
                currentLayoutY += 8; // Paragraph spacing
            } else if (trimmed.equals("---") || trimmed.equals("***")) {
                addSeparator();
            } else if (trimmed.startsWith("# ")) {
                addHeader(parseInline(trimmed.substring(2)), ChatFormatting.GOLD);
            } else if (trimmed.startsWith("## ")) {
                addHeader(parseInline(trimmed.substring(3)), ChatFormatting.YELLOW);
            } else if (trimmed.startsWith("### ")) {
                addHeader(parseInline(trimmed.substring(4)), ChatFormatting.AQUA);
            } else if (trimmed.startsWith("> ")) {
                addQuote(parseInline(trimmed.substring(2)));
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                addListItem(trimmed.substring(2));
            } else {
                // Use flow layout to handle inline links correctly
                addFlowParagraph(line);
            }
        }

        // Finalize height constraint based on total content
        this.setHeight(Constraints.pixel(currentLayoutY));
    }

    // --- Widget Generators ---

    /**
     * Adds a horizontal separator line.
     */
    private void addSeparator() {
        currentLayoutY += 5;

        UIPanel separator = new UIPanel();
        separator.setX(Constraints.pixel(0))
                .setY(Constraints.pixel(currentLayoutY))
                .setWidth(Constraints.pixel(contentWidth))
                .setHeight(Constraints.pixel(2)); // 2px thick line

        separator.style().set(Properties.BACKGROUND_COLOR, 0xFF404040);

        this.add(separator);
        currentLayoutY += 10; // Height of separator + margin
    }

    /**
     * Helper to create a text widget that wraps correctly using standard UIText logic.
     * Used for headers and quotes where links are not prioritized.
     */
    private UIText createWrappingText(Component content, float widthOffset) {
        // 1. Create with empty string (Lines: [Empty, NoWrap])
        // This acts as a small top padding (line height ~9px)
        UIText widget = new UIText();
        widget.addText(Component.empty());

        // 2. Add actual content with wrapping enabled (Lines: [Empty, NoWrap], [Content, Wrap])
        widget.addText(content, true);

        widget.setX(Constraints.pixel(widthOffset));
        widget.setY(Constraints.pixel(currentLayoutY));
        widget.setWidth(Constraints.pixel(contentWidth - widthOffset));

        // Force layout calculation immediately so we can read the height
        widget.layout();
        return widget;
    }

    /**
     * Adds a paragraph using a "Flow Layout".
     * Splits the line by links and words, positioning each segment manually
     * to allow individual segments (links) to be clickable.
     *
     * @param rawText The full paragraph text.
     */
    private void addFlowParagraph(String rawText) {
        Matcher matcher = LINK_PATTERN.matcher(rawText);
        int lastIndex = 0;

        // Current X position relative to the container for this paragraph flow
        float cursorX = 0;
        float fontHeight = Minecraft.getInstance().font.lineHeight;

        while (matcher.find()) {
            // 1. Text before the link
            String preText = rawText.substring(lastIndex, matcher.start());
            if (!preText.isEmpty()) {
                cursorX = addFlowWords(preText, cursorX, fontHeight, false, null);
            }

            // 2. The Link itself
            String label = matcher.group(1);
            String url = matcher.group(2);
            cursorX = addFlowWords(label, cursorX, fontHeight, true, url);

            lastIndex = matcher.end();
        }

        // 3. Text after the last link
        String remaining = rawText.substring(lastIndex);
        if (!remaining.isEmpty()) {
            addFlowWords(remaining, cursorX, fontHeight, false, null);
        }

        // After the flow is done, move the global layout Y down by one line height
        // (plus any wrapping that happened inside addFlowWords)
        currentLayoutY += fontHeight;
    }

    /**
     * Helper to add words to the UI.
     * Splits text by spaces and adds widgets word-by-word (or chunk-by-chunk),
     * wrapping to the next line if the content width is exceeded.
     *
     * @param text     The text content.
     * @param startX   The current X offset in the line.
     * @param lineHeight The height of a line.
     * @param isLink   Whether this segment is a clickable link.
     * @param url      The URL target if isLink is true.
     * @return The new X position after adding the text.
     */
    private float addFlowWords(String text, float startX, float lineHeight, boolean isLink, String url) {
        // We split by spaces to perform simple word wrapping
        String[] words = text.split("(?<=\\s)|(?=\\s)"); // Keep delimiters (spaces)

        float currentX = startX;

        for (String word : words) {
            // Skip empty tokens
            if (word.isEmpty()) continue;

            // Parse styles (bold/italic/code)
            Component wordComp = parseInline(word);
            if (isLink) {
                // Style links as Blue and Underlined
                wordComp = wordComp.copy().withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE);
            }

            int wordWidth = Minecraft.getInstance().font.width(wordComp);

            // Check if we need to wrap
            if (currentX + wordWidth > contentWidth) {
                currentX = 0; // Reset X
                currentLayoutY += lineHeight; // Move Y down
            }

            // Create the widget for this word segment
            addSegmentWidget(wordComp, currentX, currentLayoutY, wordWidth, lineHeight, isLink, url);
            currentX += wordWidth;
        }

        return currentX;
    }

    /**
     * Creates and adds a small text widget for a specific segment (word or link).
     */
    private void addSegmentWidget(Component content, float x, float y, int w, float h, boolean isLink, String url) {
        UIText widget = new UIText();
        widget.setText(content);

        widget.setX(Constraints.pixel(x));
        widget.setY(Constraints.pixel(y));
        widget.setWidth(Constraints.pixel(w));
        // We set height slightly larger to ensure hitboxes work nicely
        widget.setHeight(Constraints.pixel(h));

        if (isLink && url != null) {
            // Hover: Change cursor to Hand
            widget.setOnMouseEnter(wgt -> {
                long window = Minecraft.getInstance().getWindow().getWindow();
                // Create a standard hand cursor
                long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                GLFW.glfwSetCursor(window, cursor);
            });

            // Exit: Change cursor back to Arrow
            widget.setOnMouseExit(wgt -> {
                long window = Minecraft.getInstance().getWindow().getWindow();
                long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
                GLFW.glfwSetCursor(window, cursor);
            });

            // Click: Open URL
            widget.setOnClick(wgt -> {
                try {
                    URLUtil.openURL(url);
                } catch (Exception e) {
                    System.err.println("Failed to open Markdown URL: " + url);
                    e.printStackTrace();
                }
            });
        }

        this.add(widget);
    }

    private void addHeader(Component text, ChatFormatting color) {
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

    private void addQuote(Component text) {
        // Container for quote
        float padding = 4;
        float barWidth = 2;
        float quoteX = 10;
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        // Create the text widget first to measure it
        UIText textWidget = createWrappingText(text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), quoteX + padding);

        // We shift the widget UP by fontHeight to make the visible text start exactly at 'padding'.
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

    private void addListItem(String rawText) {
        float bulletWidth = 15;

        // Bullet point doesn't need wrapping, simple creation
        UIText bullet = new UIText();
        bullet.setText("•");
        bullet.setX(Constraints.pixel(5));

        // Determine Y offset.
        // createWrappingText adds an empty line (~9px) at the start.
        // We align the bullet to the second line of the content widget (where text actually starts).
        float fontHeight = Minecraft.getInstance().font.lineHeight;
        bullet.setY(Constraints.pixel(currentLayoutY + fontHeight));
        this.add(bullet);

        // For list items, we currently use simple wrapping.
        // Advanced flow layout for list items with links requires handling indentation in addFlowParagraph,
        // which is omitted here for brevity, but basic parsing is applied.
        UIText content = createWrappingText(parseInline(rawText), bulletWidth);

        this.add(content);
        currentLayoutY += content.getHeight();
    }

    private void addCodeBlock(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append("\n");

        String code = sb.toString();
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        // Apply syntax highlighting
        Component coloredCode = highlightCode(code);

        // Code blocks usually don't wrap, they scroll or clip.
        // For this implementation, we will allow wrapping via our helper.
        UIText content = createWrappingText(coloredCode, 0);

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
     * Highlights code syntax using basic regex patterns.
     *
     * @param code The raw code string.
     * @return A Component with colors applied to keywords, strings, etc.
     */
    private Component highlightCode(String code) {
        MutableComponent result = Component.empty();
        Matcher matcher = CODE_TOKEN_PATTERN.matcher(code);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append un-matched segment (plain text / symbols) as gray
            String plain = code.substring(lastEnd, matcher.start());
            if (!plain.isEmpty()) {
                result.append(Component.literal(plain).withStyle(ChatFormatting.GRAY));
            }

            String token = matcher.group();
            ChatFormatting color;

            // Determine color based on token type
            if (token.startsWith("\"") || token.startsWith("'")) {
                color = ChatFormatting.GREEN; // Strings
            } else if (token.startsWith("//") || token.startsWith("/*")) {
                color = ChatFormatting.DARK_GRAY; // Comments
            } else if (Character.isDigit(token.charAt(0))) {
                color = ChatFormatting.BLUE; // Numbers
            } else {
                color = ChatFormatting.GOLD; // Keywords (public, void, etc.)
            }

            result.append(Component.literal(token).withStyle(color));
            lastEnd = matcher.end();
        }

        // Append any remaining text after the last match
        String tail = code.substring(lastEnd);
        if (!tail.isEmpty()) {
            result.append(Component.literal(tail).withStyle(ChatFormatting.GRAY));
        }

        return result;
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
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.util.URLUtil;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;

/**
 * Represents a Paragraph in Markdown.
 * Uses a "Flow Layout" to handle standard text mixed with clickable links.
 * Splits text into segments and positions them manually.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownParagraph extends UIPanel {

    private float currentLayoutY = 0;
    private final float contentWidth;
    private final float renderHeight;
    private final Font font;

    /**
     * Constructs a paragraph component.
     *
     * @param rawText      The full raw text of the paragraph.
     * @param contentWidth The available width.
     * @param font         The font to use for text rendering.
     */
    public MarkdownParagraph(String rawText, float contentWidth, Font font) {
        this.contentWidth = contentWidth;
        this.font = font;
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));

        buildFlow(rawText);

        // After flow is built, set height.
        // Use the font's line height to determine the final height correctly
        float lineHeight = font.getLineHeight();
        this.renderHeight = currentLayoutY + lineHeight;
        this.setHeight(Layout.pixel(renderHeight));
    }

    /**
     * Parses the text and creates widgets for links and words.
     */
    private void buildFlow(String rawText) {
        Matcher matcher = MarkdownUtils.LINK_PATTERN.matcher(rawText);
        int lastIndex = 0;

        // Current X position relative to the container for this paragraph flow
        float cursorX = 0;
        // Use dynamic line height from the font
        float lineHeight = font.getLineHeight();

        while (matcher.find()) {
            // 1. Text before the link
            String preText = rawText.substring(lastIndex, matcher.start());
            if (!preText.isEmpty()) {
                cursorX = addFlowWords(preText, cursorX, lineHeight, false, null);
            }

            // 2. The Link itself
            String label = matcher.group(1);
            String url = matcher.group(2);
            cursorX = addFlowWords(label, cursorX, lineHeight, true, url);

            lastIndex = matcher.end();
        }

        // 3. Text after the last link
        String remaining = rawText.substring(lastIndex);
        if (!remaining.isEmpty()) {
            addFlowWords(remaining, cursorX, lineHeight, false, null);
        }
    }

    /**
     * Helper to add words to the UI.
     * Splits text by spaces and adds widgets word-by-word, wrapping if needed.
     *
     * @return The new X position after adding the text.
     */
    private float addFlowWords(String text, float startX, float lineHeight, boolean isLink, String url) {
        // We split by spaces to perform simple word wrapping
        String[] words = text.split("(?<=\\s)|(?=\\s)"); // Keep delimiters (spaces)

        float currentX = startX;

        for (String word : words) {
            // Skip empty tokens
            if (word.isEmpty()) continue;

            // Parse styles (bold/italic/code) using MarkdownUtils
            TextComponent wordComp = MarkdownUtils.parseInline(word);
            
            // IMPORTANT: Apply the font to the parsed component tree
            MarkdownUtils.applyFontRecursive(wordComp, this.font);
            
            if (isLink) {
                // Style links as Blue (0xFF5555FF) and Underlined
                wordComp = wordComp.copy().setColor(0xFF5555FF).setUnderline(true);
            }

            // Calculate width using the component's font (which we just set)
            int wordWidth = TextComponent.getTextWidth(wordComp);

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
    private void addSegmentWidget(TextComponent content, float x, float y, int w, float h, boolean isLink, String url) {
        UIText widget = new UIText();
        widget.setText(content);

        widget.setX(Layout.pixel(x));
        widget.setY(Layout.pixel(y));
        widget.setWidth(Layout.pixel(w));
        // We set height slightly larger to ensure hitboxes work nicely
        widget.setHeight(Layout.pixel(h));

        if (isLink && url != null) {
            // Hover: Change cursor to Hand
            widget.setOnMouseEnter(wgt -> {
                long window = GLFW.glfwGetCurrentContext();
                // Create a standard hand cursor via GLFW
                long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                GLFW.glfwSetCursor(window, cursor);
            });

            // Exit: Change cursor back to Arrow
            widget.setOnMouseExit(wgt -> {
                long window = GLFW.glfwGetCurrentContext();
                long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
                GLFW.glfwSetCursor(window, cursor);
            });

            // Click: Open URL using utility class
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

    /**
     * Returns the total calculated height of this paragraph.
     * @return The height in pixels.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
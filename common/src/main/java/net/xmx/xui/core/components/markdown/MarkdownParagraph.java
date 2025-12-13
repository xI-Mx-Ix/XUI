/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.text.UITextComponent;
import net.xmx.xui.util.URLUtil;
import net.minecraft.client.Minecraft;
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

    /**
     * Constructs a paragraph component.
     *
     * @param rawText      The full raw text of the paragraph.
     * @param contentWidth The available width.
     */
    public MarkdownParagraph(String rawText, float contentWidth) {
        this.contentWidth = contentWidth;
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));

        buildFlow(rawText);

        // After flow is built, set height.
        this.renderHeight = currentLayoutY + UITextComponent.getFontHeight();
        this.setHeight(Constraints.pixel(renderHeight));
    }

    /**
     * Parses the text and creates widgets for links and words.
     */
    private void buildFlow(String rawText) {
        Matcher matcher = MarkdownUtils.LINK_PATTERN.matcher(rawText);
        int lastIndex = 0;

        // Current X position relative to the container for this paragraph flow
        float cursorX = 0;
        float fontHeight = UITextComponent.getFontHeight();

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
            UITextComponent wordComp = MarkdownUtils.parseInline(word);
            if (isLink) {
                // Style links as Blue (0xFF5555FF) and Underlined
                wordComp = wordComp.copy().setColor(0xFF5555FF).setUnderline(true);
            }

            int wordWidth = UITextComponent.getTextWidth(wordComp);

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
    private void addSegmentWidget(UITextComponent content, float x, float y, int w, float h, boolean isLink, String url) {
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
                // Create a standard hand cursor via GLFW
                long cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                GLFW.glfwSetCursor(window, cursor);
            });

            // Exit: Change cursor back to Arrow
            widget.setOnMouseExit(wgt -> {
                long window = Minecraft.getInstance().getWindow().getWindow();
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
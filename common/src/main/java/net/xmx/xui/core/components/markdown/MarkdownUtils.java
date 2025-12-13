/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.text.TextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing Markdown syntax and helper methods for text generation.
 * Contains logic for Regex patterns, inline style parsing, and syntax highlighting.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownUtils {

    // Pattern for detecting Markdown links: [Label](Url)
    public static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");

    // Pattern for detecting tokens in code blocks for syntax highlighting.
    // Updates:
    // 1. Strings ("..." or '...')
    // 2. Comments (//... or /*...*/)
    // 3. Keywords (public, private, etc.)
    // 4. Numbers (Integers, Decimals, Floats/Doubles/Longs like 1.5f, 10L, 3.14)
    public static final Pattern CODE_TOKEN_PATTERN = Pattern.compile(
            "(\".*?\"|'.*?'|//.*|/\\*[\\s\\S]*?\\*/|\\b(public|private|protected|class|interface|enum|extends|implements|void|int|float|double|boolean|char|String|return|if|else|for|while|do|switch|case|break|continue|new|null|true|false|this|super|static|final|abstract|synchronized|volatile|transient|import|package)\\b|\\b\\d+(\\.\\d+)?[fFdDL]?\\b)"
    );

    /**
     * Recursively applies a specific font to a component and all its siblings/children.
     * <p>
     * This is necessary because Markdown parsing often creates a tree of components
     * (e.g., a bold word inside a sentence), and the font needs to be propagated
     * to every part of that tree for consistent rendering and measurement.
     * </p>
     *
     * @param root The root text component to modify.
     * @param font The font to apply.
     */
    public static void applyFontRecursive(TextComponent root, Font font) {
        if (root == null || font == null) return;

        // Apply to the current node
        root.setFont(font);

        // Recursively apply to all siblings
        for (TextComponent sibling : root.getSiblings()) {
            applyFontRecursive(sibling, font);
        }
    }

    /**
     * Helper to create a text widget that wraps correctly using standard UIText logic.
     * Used for headers, quotes, and code blocks where links are not prioritized.
     *
     * @param content      The text component to render.
     * @param widthOffset  The x-offset to subtract from the total content width.
     * @param contentWidth The total available width.
     * @return A configured UIWrappedText widget with layout calculated.
     */
    public static UIWrappedText createWrappingText(TextComponent content, float widthOffset, float contentWidth) {
        UIWrappedText widget = new UIWrappedText();
        // Add empty line for padding logic
        widget.addText(TextComponent.empty());
        // Add actual content with wrapping enabled
        widget.addText(content, true);

        widget.setX(Layout.pixel(widthOffset));
        widget.setY(Layout.pixel(0));
        widget.setWidth(Layout.pixel(contentWidth - widthOffset));

        widget.layout();
        return widget;
    }

    /**
     * Parses inline markdown styles including Bold, Italic, Strikethrough, and Code.
     *
     * Supported syntax:
     * - **Bold** -> Bold
     * - *Italic* -> Italic
     * - ~~Strike~~ -> Strikethrough
     * - `Code` -> Gray/Monospace style
     *
     * @param text The raw text.
     * @return A component with formatting codes applied.
     */
    public static TextComponent parseInline(String text) {
        // Escape existing section signs to prevent injection
        String safe = text.replace("§", "");

        // Replace Bold **...**
        safe = safe.replaceAll("\\*\\*(.*?)\\*\\*", "§l$1§r");

        // Replace Italic *...*
        safe = safe.replaceAll("\\*(.*?)\\*", "§o$1§r");

        // Replace Strikethrough ~~...~~
        safe = safe.replaceAll("~~(.*?)~~", "§m$1§r");

        // Replace Code `...` (Yellow/Gray)
        safe = safe.replaceAll("`(.*?)`", "§7$1§r");

        return TextComponent.literal(safe);
    }

    /**
     * Highlights code syntax using regex patterns.
     *
     * @param code The raw code string.
     * @return A Component with colors applied to keywords, strings, etc.
     */
    public static TextComponent highlightCode(String code) {
        TextComponent result = TextComponent.empty();
        Matcher matcher = CODE_TOKEN_PATTERN.matcher(code);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append un-matched segment (plain text / symbols) as gray
            String plain = code.substring(lastEnd, matcher.start());
            if (!plain.isEmpty()) {
                result.append(TextComponent.literal(plain).setColor(0xFFAAAAAA)); // Gray
            }

            String token = matcher.group();
            int color;

            if (token.startsWith("\"") || token.startsWith("'")) {
                color = 0xFF55FF55; // Green (Strings)
            } else if (token.startsWith("//") || token.startsWith("/*")) {
                color = 0xFF555555; // Dark Gray (Comments)
            } else if (Character.isDigit(token.charAt(0))) {
                color = 0xFF5555FF; // Blue (Numbers)
            } else {
                color = 0xFFFFAA00; // Gold (Keywords)
            }

            result.append(TextComponent.literal(token).setColor(color));
            lastEnd = matcher.end();
        }

        String tail = code.substring(lastEnd);
        if (!tail.isEmpty()) {
            result.append(TextComponent.literal(tail).setColor(0xFFAAAAAA)); // Gray
        }

        return result;
    }
}
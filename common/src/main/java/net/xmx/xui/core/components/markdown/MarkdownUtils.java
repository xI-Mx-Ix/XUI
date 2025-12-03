/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing Markdown syntax and helper methods for text generation.
 * Contains shared logic for regex patterns and text wrapping initialization.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownUtils {

    // Pattern for detecting Markdown links: [Label](Url)
    public static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");

    // Pattern for detecting tokens in code blocks for syntax highlighting
    public static final Pattern CODE_TOKEN_PATTERN = Pattern.compile(
            "(\".*?\"|'.*?'|//.*|/\\*[\\s\\S]*?\\*/|\\b(public|private|protected|class|interface|enum|extends|implements|void|int|float|double|boolean|char|String|return|if|else|for|while|do|switch|case|break|continue|new|null|true|false|this|super|static|final|abstract|synchronized|volatile|transient|import|package)\\b|\\b\\d+\\b)"
    );

    /**
     * Helper to create a text widget that wraps correctly using standard UIText logic.
     * Used for headers, quotes, and code blocks where links are not prioritized.
     *
     * @param content The text component to render.
     * @param widthOffset The x-offset to subtract from the total content width.
     * @param contentWidth The total available width.
     * @return A configured UIText widget with layout calculated.
     */
    public static UIText createWrappingText(Component content, float widthOffset, float contentWidth) {
        // 1. Create with empty string (Lines: [Empty, NoWrap])
        // This acts as a small top padding (line height ~9px)
        UIText widget = new UIText();
        widget.addText(Component.empty());

        // 2. Add actual content with wrapping enabled (Lines: [Empty, NoWrap], [Content, Wrap])
        widget.addText(content, true);

        widget.setX(Constraints.pixel(widthOffset));
        widget.setY(Constraints.pixel(0)); // Y is relative to the container
        widget.setWidth(Constraints.pixel(contentWidth - widthOffset));

        // Force layout calculation immediately so we can read the height
        widget.layout();
        return widget;
    }

    /**
     * Primitive regex parser for inline markdown styles.
     * **bold** -> Bold
     * *italic* -> Italic
     * `code` -> Gray/Monospace
     *
     * @param text The raw text.
     * @return A component with formatting codes applied.
     */
    public static Component parseInline(String text) {
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

    /**
     * Highlights code syntax using basic regex patterns.
     *
     * @param code The raw code string.
     * @return A Component with colors applied to keywords, strings, etc.
     */
    public static Component highlightCode(String code) {
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
}
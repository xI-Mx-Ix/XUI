/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font.layout;

import net.xmx.xui.core.font.FontAtlas;
import net.xmx.xui.core.font.data.MSDFData;
import net.xmx.xui.core.font.type.CustomFont;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the calculation of text dimensions and layout.
 * Responsible for measuring strings and computing word-wrapping line breaks
 * based on the metrics provided by {@link CustomFont}.
 *
 * @author xI-Mx-Ix
 */
public class TextLayoutEngine {

    private final CustomFont fontSystem;
    private final float fontSize;

    /**
     * @param fontSystem The font system to resolve fonts from (Regular/Bold/Italic).
     * @param fontSize   The logical size of the font (em size).
     */
    public TextLayoutEngine(CustomFont fontSystem, float fontSize) {
        this.fontSystem = fontSystem;
        this.fontSize = fontSize;
    }

    /**
     * Calculates the width of a component tree recursively.
     *
     * @param component The root component.
     * @return The total width in pixels.
     */
    public float computeWidth(TextComponent component) {
        float width = getSingleComponentWidth(component);
        for (TextComponent sibling : component.getSiblings()) {
            width += computeWidth(sibling);
        }
        return width;
    }

    /**
     * Measures a single component's text string.
     * Calculates the width of every character in the sequence.
     */
    private float getSingleComponentWidth(TextComponent component) {
        String text = component.getText();
        if (text == null || text.isEmpty()) return 0;

        FontAtlas font = fontSystem.resolveFont(component);
        if (font == null) return 0;

        float width = 0;
        char[] chars = text.toCharArray();

        for (char c : chars) {
            MSDFData.Glyph glyph = font.getGlyph(c);

            if (glyph != null) {
                width += glyph.advance * fontSize;
            } else {
                // Fallback for missing characters using the space width or a default constant
                MSDFData.Glyph space = font.getGlyph(' ');
                if (space != null) {
                    width += space.advance * fontSize;
                } else {
                    width += 0.5f * fontSize;
                }
            }
        }
        return width;
    }

    /**
     * Splits a component tree into lines that fit within a maximum width.
     *
     * @param root     The root component.
     * @param maxWidth The maximum width in pixels.
     * @return A list of {@link TextLine} objects containing the layout.
     */
    public List<TextLine> computeWrappedLayout(TextComponent root, float maxWidth) {
        List<TextLine> lines = new ArrayList<>();
        TextLine currentLine = new TextLine();
        float currentLineWidth = 0;

        // Flatten the tree for easier iteration
        List<TextComponent> flatList = new ArrayList<>();
        flatten(root, flatList);

        for (TextComponent comp : flatList) {
            String text = comp.getText();
            if (text == null || text.isEmpty()) continue;

            FontAtlas font = fontSystem.resolveFont(comp);
            if (font == null) continue;

            // Split by boundaries while keeping the structure manageable
            String[] words = text.split("((?<=\\s)|(?=\\s))");

            for (String word : words) {
                // Handle explicit newlines
                if (word.equals("\n")) {
                    lines.add(currentLine);
                    currentLine = new TextLine();
                    currentLineWidth = 0;
                    continue;
                }

                // Measure the word
                float wordWidth = 0;
                char[] wChars = word.toCharArray();
                for (char c : wChars) {
                    MSDFData.Glyph g = font.getGlyph(c);
                    if (g != null) {
                        wordWidth += g.advance * fontSize;
                    } else {
                        MSDFData.Glyph space = font.getGlyph(' ');
                        if (space != null) {
                            wordWidth += space.advance * fontSize;
                        } else {
                            wordWidth += 0.5f * fontSize;
                        }
                    }
                }

                // Check fit
                if (currentLineWidth + wordWidth <= maxWidth) {
                    currentLine.add(comp, word, wordWidth);
                    currentLineWidth += wordWidth;
                } else {
                    // Wrap to new line
                    lines.add(currentLine);
                    currentLine = new TextLine();
                    currentLine.add(comp, word, wordWidth);
                    currentLineWidth = wordWidth;
                }
            }
        }

        // Add the final line if it has content
        if (!currentLine.getSegments().isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private void flatten(TextComponent comp, List<TextComponent> list) {
        list.add(comp);
        for (TextComponent s : comp.getSiblings()) flatten(s, list);
    }
}
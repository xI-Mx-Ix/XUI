/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.vertex.UIMeshBuffer;
import net.xmx.xui.core.text.UIComponent;
import net.xmx.xui.impl.UIRenderImpl;
import org.lwjgl.stb.STBTTPackedchar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of {@link UIFont} utilizing a custom TrueType renderer (STB).
 * Supports high-resolution text, multi-line wrapping, and standard style codes.
 * <p>
 * To ensure high render quality at small sizes (like the Vanilla 9px standard),
 * this class loads the font texture at a much higher resolution (High-DPI)
 * and scales the geometry down during the render pass.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UICustomFont extends UIFont {

    private LoadedFont regular;
    private LoadedFont bold;
    private LoadedFont italic;

    /**
     * The internal texture resolution size.
     * Loading at 48px ensures crisp edges even when scaled down.
     */
    private static final float TEXTURE_FONT_SIZE = 48.0f;

    /**
     * The logical height we want the font to appear on screen.
     * Matches Minecraft's Vanilla font height.
     */
    private static final float TARGET_VISUAL_SIZE = 9.0f;

    public UICustomFont() {
        super(Type.CUSTOM);
    }

    // --- Configuration ---

    public UICustomFont setRegular(String path) {
        this.regular = new LoadedFont(path, TEXTURE_FONT_SIZE);
        return this;
    }

    public UICustomFont setBold(String path) {
        this.bold = new LoadedFont(path, TEXTURE_FONT_SIZE);
        return this;
    }

    public UICustomFont setItalic(String path) {
        this.italic = new LoadedFont(path, TEXTURE_FONT_SIZE);
        return this;
    }

    // --- Metrics ---

    @Override
    public float getLineHeight() {
        // Always return 9.0f (Target Size), regardless of texture size
        return TARGET_VISUAL_SIZE;
    }

    @Override
    public float getWidth(UIComponent component) {
        float width = getSingleComponentWidth(component);
        for (UIComponent sibling : component.getSiblings()) {
            width += getWidth(sibling);
        }
        return width;
    }

    private float getSingleComponentWidth(UIComponent component) {
        LoadedFont font = resolveFontData(component);
        if (font == null || component.getText() == null) return 0;

        // Calculate scale factor (e.g., 9 / 48 = 0.1875)
        float scale = TARGET_VISUAL_SIZE / font.getFontSize();

        // Measure raw width from texture and scale down
        return font.getStringWidth(component.getText()) * scale;
    }

    @Override
    public float getWordWrapHeight(UIComponent component, float maxWidth) {
        List<LineLayout> layout = computeWrappedLayout(component, maxWidth);
        return layout.size() * getLineHeight();
    }

    // --- Rendering ---

    @Override
    public void draw(UIRenderImpl context, UIComponent component, float x, float y, int color, boolean shadow) {
        UIRenderer.getInstance().getText().begin(context.getCurrentScale());
        drawComponentRecursive(context, component, x, y, x, color);
        UIRenderer.getInstance().getText().end();
    }

    private float drawComponentRecursive(UIRenderImpl context, UIComponent comp, float currentX, float currentY, float startX, int defaultColor) {
        float newX = drawSingleString(context, comp, currentX, currentY, startX, defaultColor);
        for (UIComponent sibling : comp.getSiblings()) {
            newX = drawComponentRecursive(context, sibling, newX, currentY, startX, defaultColor);
        }
        return newX;
    }

    @Override
    public void drawWrapped(UIRenderImpl context, UIComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        List<LineLayout> lines = computeWrappedLayout(component, maxWidth);
        UIRenderer.getInstance().getText().begin(context.getCurrentScale());

        float currentY = y;
        float lineHeight = getLineHeight();

        for (LineLayout line : lines) {
            float currentX = x;
            for (LineSegment segment : line.segments) {
                String originalText = segment.component.getText();
                segment.component.setText(segment.text);
                currentX = drawSingleString(context, segment.component, currentX, currentY, x, color);
                segment.component.setText(originalText);
            }
            currentY += lineHeight;
        }

        UIRenderer.getInstance().getText().end();
    }

    // --- Core Draw Logic ---

    private float drawSingleString(UIRenderImpl context, UIComponent comp, float x, float y, float startX, int defaultColor) {
        String text = comp.getText();
        if (text == null || text.isEmpty()) return x;

        LoadedFont fontData = resolveFontData(comp);
        if (fontData == null) return x;

        // Calculate scaling factor to map High-Res Texture -> 9px Visual
        float scale = TARGET_VISUAL_SIZE / fontData.getFontSize();

        int color = (comp.getColor() != null) ? comp.getColor() : defaultColor;
        if (comp.isObfuscated()) {
            text = obfuscateText(text);
        }

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        net.xmx.xui.core.gl.renderer.UITextRenderer textRenderer = UIRenderer.getInstance().getText();
        textRenderer.drawBatch(fontData.getTextureId());

        UIMeshBuffer mesh = textRenderer.getMesh();
        STBTTPackedchar.Buffer charData = fontData.getCharData();

        float cursorX = x;
        // Scale the ascent (baseline offset)
        float ascent = fontData.getAscent() * scale;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                cursorX = startX;
                continue;
            }

            if (c < 32 || c >= 32 + 255) continue;
            STBTTPackedchar info = charData.get(c - 32);

            float drawY = y + ascent;

            // Apply scale to all horizontal and vertical offsets
            // Note: info.x0(), y0(), etc. are texture coordinates, they are NOT scaled.
            // info.xoff(), yoff(), xadvance() are pixel units, they MUST be scaled.

            float x0 = cursorX + (info.xoff() * scale);
            float y0 = drawY + (info.yoff() * scale);
            float x1 = x0 + ((info.xoff2() - info.xoff()) * scale);
            float y1 = y0 + ((info.yoff2() - info.yoff()) * scale);

            // UV coordinates remain 0.0 to 1.0 (Texture Space)
            float u0 = info.x0() / 1024.0f;
            float v0 = info.y0() / 1024.0f;
            float u1 = info.x1() / 1024.0f;
            float v1 = info.y1() / 1024.0f;

            mesh.pos(x0, y0, 0).color(r, g, b, a).uv(u0, v0).endVertex();
            mesh.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex();
            mesh.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex();

            mesh.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex();
            mesh.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex();
            mesh.pos(x1, y1, 0).color(r, g, b, a).uv(u1, v1).endVertex();

            // Advance cursor by scaled width
            cursorX += (info.xadvance() * scale);
        }

        textRenderer.drawBatch(fontData.getTextureId());

        // Decorations (Underline / Strikethrough)
        if (comp.isUnderline() || comp.isStrikethrough()) {
            float width = cursorX - x;
            if (width > 0) {
                // Adjust thickness and offset for the small font size
                if (comp.isUnderline()) {
                    context.drawRect(x, y + ascent + 1.0f, width, 0.5f, color, 0);
                }
                if (comp.isStrikethrough()) {
                    context.drawRect(x, y + (ascent / 2.0f) + 0.5f, width, 0.5f, color, 0);
                }
            }
        }

        return cursorX;
    }

    // --- Word Wrap Logic ---

    private static class LineSegment {
        UIComponent component;
        String text;
        LineSegment(UIComponent c, String t) { this.component = c; this.text = t; }
    }

    private static class LineLayout {
        List<LineSegment> segments = new ArrayList<>();
    }

    private List<LineLayout> computeWrappedLayout(UIComponent root, float maxWidth) {
        List<LineLayout> lines = new ArrayList<>();
        LineLayout currentLine = new LineLayout();
        float currentLineWidth = 0;

        List<UIComponent> flatList = new ArrayList<>();
        flatten(root, flatList);

        for (UIComponent comp : flatList) {
            String text = comp.getText();
            if (text == null || text.isEmpty()) continue;

            LoadedFont font = resolveFontData(comp);
            if (font == null) continue;

            // Calculate scale once for this component
            float scale = TARGET_VISUAL_SIZE / font.getFontSize();

            String[] words = text.split("((?<=\\s)|(?=\\s))");

            for (String word : words) {
                if (word.equals("\n")) {
                    lines.add(currentLine);
                    currentLine = new LineLayout();
                    currentLineWidth = 0;
                    continue;
                }

                // Get raw width and apply scale
                float wordWidth = font.getStringWidth(word) * scale;

                if (currentLineWidth + wordWidth <= maxWidth) {
                    currentLine.segments.add(new LineSegment(comp, word));
                    currentLineWidth += wordWidth;
                } else {
                    lines.add(currentLine);
                    currentLine = new LineLayout();
                    currentLine.segments.add(new LineSegment(comp, word));
                    currentLineWidth = wordWidth;
                }
            }
        }

        if (!currentLine.segments.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private void flatten(UIComponent comp, List<UIComponent> list) {
        list.add(comp);
        for (UIComponent s : comp.getSiblings()) {
            flatten(s, list);
        }
    }

    private LoadedFont resolveFontData(UIComponent comp) {
        if (comp.isBold()) return bold != null ? bold : regular;
        if (comp.isItalic()) return italic != null ? italic : regular;
        return regular;
    }

    private String obfuscateText(String input) {
        StringBuilder sb = new StringBuilder();
        long seed = System.currentTimeMillis() / 30;
        Random localRand = new Random(seed);
        for (char c : input.toCharArray()) {
            if (c <= 32) sb.append(c);
            else sb.append((char) (33 + localRand.nextInt(90)));
        }
        return sb.toString();
    }
}
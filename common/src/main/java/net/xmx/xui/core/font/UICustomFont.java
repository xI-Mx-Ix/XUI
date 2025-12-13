/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import net.xmx.xui.core.font.data.MSDFData;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.vertex.UIMeshBuffer;
import net.xmx.xui.core.text.UIComponent;
import net.xmx.xui.impl.UIRenderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of the {@link UIFont} interface utilizing MSDF (Multi-channel Signed Distance Field) rendering.
 * <p>
 * This class handles the geometry generation for text by mapping Unicode characters
 * to their corresponding bounds defined in the imported JSON metadata.
 * It supports high-quality scaling without pixelation artifacts typical of raster fonts.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UICustomFont extends UIFont {

    private UILoadedFont regular;
    private UILoadedFont bold;
    private UILoadedFont italic;

    /**
     * The logical visual size of the font in pixels.
     * This defines the size of 1 em unit on the screen.
     */
    private static final float FONT_SIZE = 9.0f;

    public UICustomFont() {
        super(Type.CUSTOM);
    }

    /**
     * Sets the font used for standard text.
     *
     * @param name The asset name of the font.
     * @return This instance for chaining.
     */
    public UICustomFont setRegular(String name) {
        this.regular = new UILoadedFont(name);
        return this;
    }

    /**
     * Sets the font used when the Bold style is active.
     *
     * @param name The asset name of the font.
     * @return This instance for chaining.
     */
    public UICustomFont setBold(String name) {
        this.bold = new UILoadedFont(name);
        return this;
    }

    /**
     * Sets the font used when the Italic style is active.
     *
     * @param name The asset name of the font.
     * @return This instance for chaining.
     */
    public UICustomFont setItalic(String name) {
        this.italic = new UILoadedFont(name);
        return this;
    }

    @Override
    public float getLineHeight() {
        if (regular == null) return FONT_SIZE;
        // Calculate the visual line height based on the font's metrics and the target font size
        return regular.getData().metrics.lineHeight * FONT_SIZE;
    }

    @Override
    public float getWidth(UIComponent component) {
        float width = getSingleComponentWidth(component);
        for (UIComponent sibling : component.getSiblings()) {
            width += getWidth(sibling);
        }
        return width;
    }

    /**
     * Calculates the width of a single text component segment.
     */
    private float getSingleComponentWidth(UIComponent component) {
        String text = component.getText();
        if (text == null || text.isEmpty()) return 0;

        UILoadedFont font = resolveFont(component);
        if (font == null) return 0;

        float width = 0;
        for (char c : text.toCharArray()) {
            MSDFData.Glyph glyph = font.getGlyph(c);
            if (glyph != null) {
                width += glyph.advance * FONT_SIZE;
            }
        }
        return width;
    }

    @Override
    public float getWordWrapHeight(UIComponent component, float maxWidth) {
        List<LineLayout> layout = computeWrappedLayout(component, maxWidth);
        return layout.size() * getLineHeight();
    }

    // --- Rendering ---

    @Override
    public void draw(UIRenderImpl context, UIComponent component, float x, float y, int color, boolean shadow) {
        UIRenderer renderer = UIRenderer.getInstance();

        // Capture the current OpenGL state (including the active VAO)
        // and configure the state required for UI rendering.
        renderer.getStateManager().capture();
        renderer.getStateManager().setupForUI();

        // Begin the text rendering pass, providing the atlas info for shader configuration
        renderer.getText().begin(context.getCurrentScale(), regular.getData().atlas);

        try {
            drawComponentRecursive(context, component, x, y, x, color);
        } finally {
            renderer.getText().end();
            // Restore the previous OpenGL state to ensure compatibility with the game engine.
            renderer.getStateManager().restore();
        }
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
        if (regular == null) return;

        List<LineLayout> lines = computeWrappedLayout(component, maxWidth);
        UIRenderer renderer = UIRenderer.getInstance();

        renderer.getStateManager().capture();
        renderer.getStateManager().setupForUI();
        renderer.getText().begin(context.getCurrentScale(), regular.getData().atlas);

        try {
            float currentY = y;
            float lineHeight = getLineHeight();

            for (LineLayout line : lines) {
                float currentX = x;
                for (LineSegment segment : line.segments) {
                    // Temporarily swap text content to render individual wrapped segments
                    String originalText = segment.component.getText();
                    segment.component.setText(segment.text);

                    currentX = drawSingleString(context, segment.component, currentX, currentY, x, color);

                    segment.component.setText(originalText);
                }
                currentY += lineHeight;
            }
        } finally {
            renderer.getText().end();
            renderer.getStateManager().restore();
        }
    }

    /**
     * Renders the text string of a single component using geometry calculated from the MSDF data.
     *
     * @param context      The render implementation.
     * @param comp         The component containing text and style.
     * @param x            The starting X coordinate.
     * @param y            The starting Y coordinate.
     * @param startX       The X coordinate to return to on newline.
     * @param defaultColor The inherited color if the component has none.
     * @return The X coordinate after rendering the text.
     */
    private float drawSingleString(UIRenderImpl context, UIComponent comp, float x, float y, float startX, int defaultColor) {
        String text = comp.getText();
        if (text == null || text.isEmpty()) return x;

        UILoadedFont font = resolveFont(comp);
        if (font == null) return x;

        // Color handling
        int color = (comp.getColor() != null) ? comp.getColor() : defaultColor;
        if (comp.isObfuscated()) {
            text = obfuscateText(text);
        }

        // Unpack ARGB color to normalized floats
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Render previous batch if texture changes (though usually handled by outer loop)
        UIRenderer.getInstance().getText().drawBatch(font.getTextureId());
        UIMeshBuffer mesh = UIRenderer.getInstance().getText().getMesh();

        float cursorX = x;

        // Calculate baseline:
        // XUI coordinates are Top-Left based.
        // Font metrics (ascender) tell us how far down the baseline is from the top of the line.
        float baselineOffset = font.getData().metrics.ascender * FONT_SIZE;
        float cursorY = y + baselineOffset;

        float atlasW = font.getData().atlas.width;
        float atlasH = font.getData().atlas.height;

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                cursorX = startX;
                continue;
            }

            MSDFData.Glyph glyph = font.getGlyph(c);
            if (glyph == null) continue;

            // Only generate geometry if the glyph has visual data (e.g., space characters have no bounds)
            if (glyph.planeBounds != null && glyph.atlasBounds != null) {

                // 1. Calculate Screen Positions (Vertex Coordinates)
                // Plane bounds are normalized (EM space), so we multiply by FONT_SIZE.
                // We subtract from cursorY because in OpenGL Y-Down, "Up" in font metrics means lower Y value.
                float pl = glyph.planeBounds.left;
                float pb = glyph.planeBounds.bottom;
                float pr = glyph.planeBounds.right;
                float pt = glyph.planeBounds.top;

                float x0 = cursorX + (pl * FONT_SIZE);
                float x1 = cursorX + (pr * FONT_SIZE);

                float y0 = cursorY - (pt * FONT_SIZE); // Top edge of glyph
                float y1 = cursorY - (pb * FONT_SIZE); // Bottom edge of glyph

                // 2. Calculate Texture Coordinates (UVs)
                // Atlas bounds are in raw pixels. We normalize by atlas dimensions.
                // Note: msdf-atlas-gen JSON assumes 0 at bottom for Y, but standard image load is 0 at top.
                // We flip the V coordinate: 1.0 - (y / height).
                float u0 = glyph.atlasBounds.left / atlasW;
                float u1 = glyph.atlasBounds.right / atlasW;
                float v0 = 1.0f - (glyph.atlasBounds.top / atlasH);
                float v1 = 1.0f - (glyph.atlasBounds.bottom / atlasH);

                // 3. Push Vertices (Two Triangles -> Quad)
                // Triangle 1
                mesh.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex(); // Bottom-Left
                mesh.pos(x1, y1, 0).color(r, g, b, a).uv(u1, v1).endVertex(); // Bottom-Right
                mesh.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex(); // Top-Right

                // Triangle 2
                mesh.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex(); // Top-Right
                mesh.pos(x0, y0, 0).color(r, g, b, a).uv(u0, v0).endVertex(); // Top-Left
                mesh.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex(); // Bottom-Left
            }

            // Advance cursor for next character
            cursorX += glyph.advance * FONT_SIZE;
        }

        // Flush this batch to ensure ordering
        UIRenderer.getInstance().getText().drawBatch(font.getTextureId());

        return cursorX;
    }

    /**
     * Resolves the correct font variant based on the component's style.
     *
     * @param comp The component to query.
     * @return The loaded font instance (regular, bold, or italic).
     */
    private UILoadedFont resolveFont(UIComponent comp) {
        if (comp.isBold()) return bold != null ? bold : regular;
        if (comp.isItalic()) return italic != null ? italic : regular;
        return regular;
    }

    // --- Helper classes for Word Wrapping logic ---

    private static class LineSegment {
        UIComponent component;
        String text;
        LineSegment(UIComponent c, String t) { this.component = c; this.text = t; }
    }

    private static class LineLayout {
        List<LineSegment> segments = new ArrayList<>();
    }

    /**
     * Splits the text into lines that fit within the given maximum width.
     *
     * @param root     The root component.
     * @param maxWidth The maximum width in pixels.
     * @return A list of LineLayout objects representing the wrapped text.
     */
    private List<LineLayout> computeWrappedLayout(UIComponent root, float maxWidth) {
        List<LineLayout> lines = new ArrayList<>();
        LineLayout currentLine = new LineLayout();
        float currentLineWidth = 0;
        List<UIComponent> flatList = new ArrayList<>();
        flatten(root, flatList);

        for (UIComponent comp : flatList) {
            String text = comp.getText();
            if (text == null || text.isEmpty()) continue;

            UILoadedFont font = resolveFont(comp);
            if (font == null) continue;

            // Split by whitespace boundaries while keeping delimiters
            String[] words = text.split("((?<=\\s)|(?=\\s))");

            for (String word : words) {
                if (word.equals("\n")) {
                    lines.add(currentLine);
                    currentLine = new LineLayout();
                    currentLineWidth = 0;
                    continue;
                }

                float wordWidth = 0;
                for(char c : word.toCharArray()) {
                    MSDFData.Glyph g = font.getGlyph(c);
                    if(g != null) wordWidth += g.advance * FONT_SIZE;
                }

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
        if (!currentLine.segments.isEmpty()) lines.add(currentLine);
        return lines;
    }

    private void flatten(UIComponent comp, List<UIComponent> list) {
        list.add(comp);
        for (UIComponent s : comp.getSiblings()) flatten(s, list);
    }

    /**
     * Generates obfuscated text for the "magic" formatting code.
     */
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
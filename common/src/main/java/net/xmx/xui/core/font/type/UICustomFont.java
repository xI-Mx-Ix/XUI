/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font.type;

import net.xmx.xui.core.font.layout.TextLayoutEngine;
import net.xmx.xui.core.font.layout.TextLine;
import net.xmx.xui.core.font.UIFont;
import net.xmx.xui.core.font.UIFontAtlas;
import net.xmx.xui.core.font.data.MSDFData;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.vertex.UIMeshBuffer;
import net.xmx.xui.core.text.UITextComponent;
import net.xmx.xui.impl.UIRenderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of the {@link UIFont} interface utilizing MSDF (Multi-channel Signed Distance Field) rendering.
 * <p>
 * This class orchestrates the rendering of high-quality scalable text.
 * It utilizes a two-pass rendering strategy:
 * <ol>
 *     <li><b>Text Pass:</b> Renders glyphs using the MSDF shader and atlas.</li>
 *     <li><b>Geometry Pass:</b> Renders text decorations (Underline, Strikethrough) using the generic geometry shader.</li>
 * </ol>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UICustomFont extends UIFont {

    private UIFontAtlas regular;
    private UIFontAtlas bold;
    private UIFontAtlas italic;

    /**
     * The logical visual size of the font in pixels.
     */
    public static final float FONT_SIZE = 9.0f;

    /**
     * Empirically determined vertical correction to align MSDF fonts with
     * the visual center of Vanilla font/standard UI elements.
     * Positive shifts text UP (subtracts from Y).
     */
    private static final float VERTICAL_SHIFT_CORRECTION = 1.5f;

    private final TextLayoutEngine layoutEngine;
    
    /**
     * Temporary list to hold decoration requests (lines) during the text render pass.
     * Flushed immediately after text rendering to ensure correct Z-ordering relative to the batch.
     */
    private final List<Decoration> pendingDecorations = new ArrayList<>();

    public UICustomFont() {
        super(Type.CUSTOM);
        this.layoutEngine = new TextLayoutEngine(this, FONT_SIZE);
    }

    /**
     * Sets the font used for standard text.
     *
     * @param namespace The resource namespace (e.g., "mymod").
     * @param path      The relative path to the font asset (e.g., "font/MyFont-Regular").
     * @return This instance for chaining.
     */
    public UICustomFont setRegular(String namespace, String path) {
        this.regular = new UIFontAtlas(namespace, path);
        return this;
    }

    /**
     * Sets the font used when the Bold style is active.
     *
     * @param namespace The resource namespace (e.g., "mymod").
     * @param path      The relative path to the font asset (e.g., "font/MyFont-Bold").
     * @return This instance for chaining.
     */
    public UICustomFont setBold(String namespace, String path) {
        this.bold = new UIFontAtlas(namespace, path);
        return this;
    }

    /**
     * Sets the font used when the Italic style is active.
     *
     * @param namespace The resource namespace (e.g., "mymod").
     * @param path      The relative path to the font asset (e.g., "font/MyFont-Italic").
     * @return This instance for chaining.
     */
    public UICustomFont setItalic(String namespace, String path) {
        this.italic = new UIFontAtlas(namespace, path);
        return this;
    }

    @Override
    public float getLineHeight() {
        if (regular == null) return FONT_SIZE;
        return regular.getData().metrics.lineHeight * FONT_SIZE;
    }

    @Override
    public float getWidth(UITextComponent component) {
        return layoutEngine.computeWidth(component);
    }

    @Override
    public float getWordWrapHeight(UITextComponent component, float maxWidth) {
        List<TextLine> layout = layoutEngine.computeWrappedLayout(component, maxWidth);
        return layout.size() * getLineHeight();
    }

    // --- Rendering Orchestration ---

    @Override
    public void draw(UIRenderImpl context, UITextComponent component, float x, float y, int color, boolean shadow) {
        renderTextBatch(context, x, y, () -> {
            drawComponentRecursive(context, component, x, y, x, color);
        });
    }

    @Override
    public void drawWrapped(UIRenderImpl context, UITextComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        List<TextLine> lines = layoutEngine.computeWrappedLayout(component, maxWidth);
        
        renderTextBatch(context, x, y, () -> {
            float currentY = y;
            float lineHeight = getLineHeight();

            for (TextLine line : lines) {
                float currentX = x;
                for (TextLine.Segment segment : line.getSegments()) {
                    // Temporarily swap text content to render individual wrapped segments
                    String originalText = segment.component().getText();
                    segment.component().setText(segment.text());

                    currentX = drawSingleString(context, segment.component(), currentX, currentY, x, color);

                    segment.component().setText(originalText);
                }
                currentY += lineHeight;
            }
        });
    }

    /**
     * Wraps the rendering operations in the necessary OpenGL state management.
     * Handles the Two-Pass strategy: Text First, then Decorations.
     */
    private void renderTextBatch(UIRenderImpl context, float x, float y, Runnable renderAction) {
        if (regular == null) return;

        UIRenderer renderer = UIRenderer.getInstance();
        
        // 1. Capture State
        renderer.getStateManager().capture();
        renderer.getStateManager().setupForUI();

        try {
            // 2. Pass 1: Render Text Glyphs (MSDF Shader)
            renderer.getText().begin(context.getCurrentScale(), regular.getData().atlas);
            
            // Execute the recursive drawing logic. 
            // This populates the mesh AND fills pendingDecorations.
            renderAction.run();
            
            renderer.getText().end();

            // 3. Pass 2: Render Decorations (Geometry Shader)
            if (!pendingDecorations.isEmpty()) {
                renderer.getGeometry().begin(context.getCurrentScale());
                
                for (Decoration deco : pendingDecorations) {
                    renderer.getGeometry().drawRect(
                            deco.x, deco.y, deco.w, deco.h, 
                            deco.color, 0, 0, 0, 0 // No corner radius for lines
                    );
                }
                
                renderer.getGeometry().end();
                pendingDecorations.clear();
            }

        } finally {
            // 4. Restore State
            renderer.getStateManager().restore();
        }
    }

    private float drawComponentRecursive(UIRenderImpl context, UITextComponent comp, float currentX, float currentY, float startX, int defaultColor) {
        float newX = drawSingleString(context, comp, currentX, currentY, startX, defaultColor);
        for (UITextComponent sibling : comp.getSiblings()) {
            newX = drawComponentRecursive(context, sibling, newX, currentY, startX, defaultColor);
        }
        return newX;
    }

    /**
     * Renders the text string and queues any necessary decorations.
     *
     * @return The X coordinate after rendering the text.
     */
    private float drawSingleString(UIRenderImpl context, UITextComponent comp, float x, float y, float startX, int defaultColor) {
        String text = comp.getText();
        if (text == null || text.isEmpty()) return x;

        UIFontAtlas font = resolveFont(comp);
        if (font == null) return x;

        // Color handling
        int color = (comp.getColor() != null) ? comp.getColor() : defaultColor;
        if (comp.isObfuscated()) {
            text = obfuscateText(text);
        }

        // Unpack ARGB color
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        UIRenderer.getInstance().getText().drawBatch(font.getTextureId());
        UIMeshBuffer mesh = UIRenderer.getInstance().getText().getMesh();

        float cursorX = x;

        // Calculate baseline:
        // XUI coordinates are Top-Left based.
        // Font metrics (ascender) tell us how far down the baseline is from the top of the line.
        float baselineOffset = font.getData().metrics.ascender * FONT_SIZE;
        float cursorY = y + baselineOffset - VERTICAL_SHIFT_CORRECTION;

        float atlasW = font.getData().atlas.width;
        float atlasH = font.getData().atlas.height;

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                cursorX = startX;
                continue;
            }

            MSDFData.Glyph glyph = font.getGlyph(c);
            if (glyph == null) continue;

            // Geometry Generation for Glyph
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
                // We flip the V coordinate: 1.0f - (y / height).
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

            cursorX += glyph.advance * FONT_SIZE;
        }

        // --- Decoration Handling ---
        // We calculate the geometry for lines now, but add them to a list
        // to be rendered in the second pass.

        float textWidth = cursorX - x;
        float thickness = Math.max(0.65f, FONT_SIZE / 12.0f); // Scale line thickness with font size

        // Underline logic: Draw slightly below the baseline
        if (comp.isUnderline()) {
            float lineY = cursorY + 0.35f; // Offset 0.35px below baseline
            pendingDecorations.add(new Decoration(x, lineY, textWidth, thickness, color));
        }

        // Strikethrough logic: Draw in the middle of the "x-height"
        // We estimate x-height as half the ascender height
        if (comp.isStrikethrough()) {
            float midOffset = (font.getData().metrics.ascender * FONT_SIZE) * 0.4f;
            float lineY = cursorY - midOffset;
            pendingDecorations.add(new Decoration(x, lineY, textWidth, thickness, color));
        }

        UIRenderer.getInstance().getText().drawBatch(font.getTextureId());
        return cursorX;
    }

    /**
     * Resolves the correct font variant based on the component's style.
     *
     * @param comp The component to query.
     * @return The loaded font instance (regular, bold, or italic).
     */
    public UIFontAtlas resolveFont(UITextComponent comp) {
        if (comp.isBold()) return bold != null ? bold : regular;
        if (comp.isItalic()) return italic != null ? italic : regular;
        return regular;
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

    /**
     * DTO for storing line rendering requests.
     */
    private record Decoration(float x, float y, float w, float h, int color) {}
}
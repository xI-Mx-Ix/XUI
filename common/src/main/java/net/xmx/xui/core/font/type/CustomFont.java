/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font.type;

import net.xmx.xui.core.font.FontAtlas;
import net.xmx.xui.core.font.data.FontMetadata;
import net.xmx.xui.core.font.layout.TextLayoutEngine;
import net.xmx.xui.core.font.layout.TextLine;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of the {@link Font} interface utilizing MSDF (Multi-channel Signed Distance Field) rendering.
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
public class CustomFont extends Font {

    private FontAtlas regular;
    private FontAtlas bold;
    private FontAtlas italic;

    /**
     * The logical visual size of the font in pixels.
     */
    public static final float FONT_SIZE = 9.0f;

    private final TextLayoutEngine layoutEngine;

    /**
     * Temporary list to hold decoration requests (lines) during the text render pass.
     * Flushed immediately after text rendering to ensure correct Z-ordering relative to the batch.
     */
    private final List<Decoration> pendingDecorations = new ArrayList<>();

    public CustomFont() {
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
    public CustomFont setRegular(String namespace, String path) {
        this.regular = new FontAtlas(namespace, path);
        return this;
    }

    /**
     * Sets the font used when the Bold style is active.
     *
     * @param namespace The resource namespace (e.g., "mymod").
     * @param path      The relative path to the font asset (e.g., "font/MyFont-Bold").
     * @return This instance for chaining.
     */
    public CustomFont setBold(String namespace, String path) {
        this.bold = new FontAtlas(namespace, path);
        return this;
    }

    /**
     * Sets the font used when the Italic style is active.
     *
     * @param namespace The resource namespace (e.g., "mymod").
     * @param path      The relative path to the font asset (e.g., "font/MyFont-Italic").
     * @return This instance for chaining.
     */
    public CustomFont setItalic(String namespace, String path) {
        this.italic = new FontAtlas(namespace, path);
        return this;
    }

    @Override
    public float getLineHeight() {
        // Enforce strict Vanilla metrics (9px height) to ensure unified scaling
        // across all widgets (Buttons, EditBoxes, etc.) regardless of the active font.
        return 9.0f;
    }

    @Override
    public float getWidth(TextComponent component) {
        return layoutEngine.computeWidth(component);
    }

    @Override
    public float getWordWrapHeight(TextComponent component, float maxWidth) {
        List<TextLine> layout = layoutEngine.computeWrappedLayout(component, maxWidth);
        return layout.size() * getLineHeight();
    }

    // --- Rendering Orchestration ---

    @Override
    public void draw(UIRenderer renderer, TextComponent component, float x, float y, int color, boolean shadow) {
        renderTextBatch(() -> drawComponentRecursive(component, x, y, x, color));
    }

    @Override
    public void drawWrapped(UIRenderer renderer, TextComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        List<TextLine> lines = layoutEngine.computeWrappedLayout(component, maxWidth);

        renderTextBatch(() -> {
            float currentY = y;
            float lineHeight = getLineHeight();

            for (TextLine line : lines) {
                float currentX = x;
                for (TextLine.Segment segment : line.getSegments()) {
                    // Temporarily swap text content to render individual wrapped segments
                    String originalText = segment.component().getText();
                    segment.component().setText(segment.text());

                    currentX = drawSingleString(segment.component(), currentX, currentY, x, color);

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
    private void renderTextBatch(Runnable renderAction) {
        if (regular == null) return;

        UIRenderer renderer = UIRenderer.getInstance();

        // 1. Capture State
        renderer.getStateManager().capture();
        renderer.getStateManager().setupForUI();

        try {
            // 2. Pass 1: Render Text Glyphs (MSDF Shader)
            renderer.getMsdf().begin(UIRenderer.getInstance().getCurrentUiScale(),
                    regular, UIRenderer.getInstance().getTransformStack().getDirectModelMatrix());

            // Execute the recursive drawing logic.
            // This populates the mesh AND fills pendingDecorations.
            renderAction.run();

            renderer.getMsdf().end();

            // 3. Pass 2: Render Decorations (Geometry Shader)
            if (!pendingDecorations.isEmpty()) {
                renderer.getGeometry().begin(UIRenderer.getInstance().getCurrentUiScale(),
                        UIRenderer.getInstance().getTransformStack().getDirectModelMatrix());

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

    private float drawComponentRecursive(TextComponent comp, float currentX, float currentY, float startX, int defaultColor) {
        float newX = drawSingleString(comp, currentX, currentY, startX, defaultColor);
        for (TextComponent sibling : comp.getSiblings()) {
            newX = drawComponentRecursive(sibling, newX, currentY, startX, defaultColor);
        }
        return newX;
    }

    /**
     * Renders the text string and queues any necessary decorations.
     * Updated to remove legacy formatting codes. Styles are now strictly derived from the TextComponent.
     *
     * @param comp         The component containing text and style.
     * @param x            The absolute X start position.
     * @param y            The absolute Y start position.
     * @param startX       The X position to return to on a newline.
     * @param defaultColor The fallback color if the component has none.
     * @return The X coordinate after rendering the text.
     */
    private float drawSingleString(TextComponent comp, float x, float y, float startX, int defaultColor) {
        String text = comp.getText();
        if (text == null || text.isEmpty()) return x;

        // Resolve the active font based on component state once at the start.
        // Since we removed legacy codes, the font variant (Bold/Italic) is constant for this string.
        FontAtlas currentFont = resolveFont(comp);
        if (currentFont == null) return x;

        // --- 1. Setup Base State ---
        int color = (comp.getColor() != null) ? comp.getColor() : defaultColor;

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // Track active styles directly from the component
        boolean isUnderlined = comp.isUnderline();
        boolean isStrikethrough = comp.isStrikethrough();
        boolean isObfuscated = comp.isObfuscated();

        // --- 2. Prepare Rendering ---
        // Bind the texture for the active font style
        UIRenderer.getInstance().getMsdf().drawBatch(currentFont.getTextureId());
        MeshBuffer mesh = UIRenderer.getInstance().getMsdf().getMesh();

        float cursorX = x;

        // Instead of calculating the baseline from the font's specific ascender (which varies per font),
        // we enforce a strict 7.0f offset from the top (y). This matches Minecraft's Vanilla behavior
        // (9px height, ~7px baseline) ensuring that custom fonts align perfectly with vanilla fonts.
        float cursorY = y + 7.0f;

        char[] chars = text.toCharArray();

        // Setup the random generator for obfuscation exactly as requested to match the visual style.
        // The seed changes every 30ms, creating the specific static noise effect.
        long seed = System.currentTimeMillis() / 30;
        Random obfuscationRandom = new Random(seed);

        // --- 3. Render Loop ---
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '\n') {
                cursorX = startX;
                continue;
            }

            // Apply obfuscation logic if active using the specific helper method
            if (isObfuscated && c > 32) {
                c = getObfuscatedChar(obfuscationRandom);
            }

            // --- Glyph Resolution & Fallback Strategy ---
            FontMetadata.Glyph glyph = currentFont.getGlyph(c);

            // Fallback to Regular if missing in current style (Bold/Italic).
            // This is still necessary even without legacy codes, because a Bold font might
            // miss a symbol that the Regular font has.
            if (glyph == null && currentFont != this.regular && this.regular != null) {
                FontMetadata.Glyph fallback = this.regular.getGlyph(c);
                if (fallback != null) {
                    // Flush current batch (e.g. Bold)
                    UIRenderer.getInstance().getMsdf().drawBatch(currentFont.getTextureId());

                    // Render single char using Regular texture
                    renderGlyph(mesh, fallback, cursorX, cursorY, this.regular, r, g, b, a);

                    // Flush Regular batch immediately
                    UIRenderer.getInstance().getMsdf().drawBatch(this.regular.getTextureId());

                    // Switch back binding to current font (e.g. Bold) for next chars
                    UIRenderer.getInstance().getMsdf().drawBatch(currentFont.getTextureId());

                    // Advance cursor
                    cursorX += fallback.advance * FONT_SIZE;
                    continue; // Skip standard rendering
                }
            }

            // Fallback for completely missing characters
            if (glyph == null) {
                // Determine a safe advance width (e.g. from space or '?' char)
                // This prevents text collapsing when characters are missing
                FontMetadata.Glyph space = currentFont.getGlyph(' ');
                float missingAdvance = (space != null) ? space.advance : 0.5f;
                cursorX += missingAdvance * FONT_SIZE;
                continue;
            }

            // --- Render Standard Glyph ---
            renderGlyph(mesh, glyph, cursorX, cursorY, currentFont, r, g, b, a);
            cursorX += glyph.advance * FONT_SIZE;
        }

        // --- 4. Render Decorations (Geometry Pass) ---
        int finalColor = ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
        float textWidth = cursorX - x;
        float thickness = Math.max(0.65f, FONT_SIZE / 12.0f);

        if (isUnderlined) {
            float lineY = cursorY + 0.35f;
            pendingDecorations.add(new Decoration(x, lineY, textWidth, thickness, finalColor));
        }

        if (isStrikethrough) {
            float midOffset = (currentFont.getFontData().metrics.ascender * FONT_SIZE) * 0.4f;
            float lineY = cursorY - midOffset;
            pendingDecorations.add(new Decoration(x, lineY, textWidth, thickness, finalColor));
        }

        // Ensure the texture binding is correct before returning control
        UIRenderer.getInstance().getMsdf().drawBatch(currentFont.getTextureId());
        return cursorX;
    }

    /**
     * Generates a single random character using the provided Random instance.
     * This preserves the specific visual noise logic requested (replacing the linear atlas scroll).
     *
     * @param random The pre-seeded random instance to ensure consistent noise across the string.
     * @return A random printable character.
     */
    private char getObfuscatedChar(Random random) {
        return (char) (33 + random.nextInt(90));
    }

    /**
     * Helper method to calculate vertex positions and push them to the mesh buffer.
     * Handles the normalization of atlas coordinates and scaling of glyph bounds.
     *
     * @param mesh    The mesh buffer to write to.
     * @param glyph   The glyph data containing bounds and advance.
     * @param cursorX The current drawing X position.
     * @param cursorY The calculated baseline Y position.
     * @param font    The font atlas containing texture dimensions.
     * @param r       Red color component (0-1).
     * @param g       Green color component (0-1).
     * @param b       Blue color component (0-1).
     * @param a       Alpha color component (0-1).
     */
    private void renderGlyph(MeshBuffer mesh, FontMetadata.Glyph glyph, float cursorX, float cursorY, FontAtlas font, float r, float g, float b, float a) {
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
            float atlasW = font.getFontData().atlas.width;
            float atlasH = font.getFontData().atlas.height;

            float u0 = glyph.atlasBounds.left / atlasW;
            float u1 = glyph.atlasBounds.right / atlasW;
            // Flip V coordinate because texture origin is typically Top-Left in load, but Bottom-Left in generation
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
    }

    /**
     * Resolves the correct font variant based on the component's style.
     *
     * @param comp The component to query.
     * @return The loaded font instance (regular, bold, or italic).
     */
    public FontAtlas resolveFont(TextComponent comp) {
        if (comp.isBold()) return bold != null ? bold : regular;
        if (comp.isItalic()) return italic != null ? italic : regular;
        return regular;
    }

    /**
     * DTO for storing line rendering requests.
     */
    private record Decoration(float x, float y, float w, float h, int color) {}
}
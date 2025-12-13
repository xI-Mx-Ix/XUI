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
import net.xmx.xui.core.text.UIFormatting;
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
     * Updated to support legacy '§' formatting codes, including dynamic font atlas switching.
     *
     * @param context      The render implementation context.
     * @param comp         The component containing text and style.
     * @param x            The absolute X start position.
     * @param y            The absolute Y start position.
     * @param startX       The X position to return to on a newline.
     * @param defaultColor The fallback color if the component has none.
     * @return The X coordinate after rendering the text.
     */
    private float drawSingleString(UIRenderImpl context, UITextComponent comp, float x, float y, float startX, int defaultColor) {
        String text = comp.getText();
        if (text == null || text.isEmpty()) return x;

        // Resolve the initial font based on component state
        UIFontAtlas initialFont = resolveFont(comp);
        if (initialFont == null) return x;

        // Track the currently active font atlas (may change mid-string via §l or §o)
        UIFontAtlas currentFont = initialFont;

        // --- 1. Setup Base State ---
        int color = (comp.getColor() != null) ? comp.getColor() : defaultColor;

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // Track active styles
        boolean isBold = comp.isBold();
        boolean isItalic = comp.isItalic();
        boolean isUnderlined = comp.isUnderline();
        boolean isStrikethrough = comp.isStrikethrough();
        boolean isObfuscated = comp.isObfuscated();

        // --- 2. Prepare Rendering ---
        // Bind the initial texture
        UIRenderer.getInstance().getText().drawBatch(currentFont.getTextureId());
        UIMeshBuffer mesh = UIRenderer.getInstance().getText().getMesh();

        float cursorX = x;

        // Calculate baseline using purely the font metrics (Ascender * FontSize)
        // No manual offset applied.
        float baselineOffset = currentFont.getData().metrics.ascender * FONT_SIZE;
        float cursorY = y + baselineOffset;

        char[] chars = text.toCharArray();

        // --- 3. Render Loop ---
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            // Handle Formatting Codes (§)
            if (c == '§' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                UIFormatting fmt = UIFormatting.getByCode(code);

                if (fmt != null) {
                    boolean fontChanged = false;

                    if (fmt == UIFormatting.RESET) {
                        // Reset all states to component defaults
                        int resetColor = (comp.getColor() != null) ? comp.getColor() : defaultColor;
                        a = ((resetColor >> 24) & 0xFF) / 255.0f;
                        r = ((resetColor >> 16) & 0xFF) / 255.0f;
                        g = ((resetColor >> 8) & 0xFF) / 255.0f;
                        b = (resetColor & 0xFF) / 255.0f;

                        isUnderlined = comp.isUnderline();
                        isStrikethrough = comp.isStrikethrough();
                        isObfuscated = comp.isObfuscated();
                        isBold = comp.isBold();
                        isItalic = comp.isItalic();
                        fontChanged = true;

                    } else if (fmt.isColor()) {
                        // Apply Color and reset styles (Vanilla behavior)
                        int fmtColor = fmt.getColor();
                        r = ((fmtColor >> 16) & 0xFF) / 255.0f;
                        g = ((fmtColor >> 8) & 0xFF) / 255.0f;
                        b = (fmtColor & 0xFF) / 255.0f;

                        isUnderlined = false;
                        isStrikethrough = false;
                        isObfuscated = false;
                        isBold = false;
                        isItalic = false;
                        fontChanged = true;

                    } else if (fmt.isStyle()) {
                        switch (fmt) {
                            case UNDERLINE -> isUnderlined = true;
                            case STRIKETHROUGH -> isStrikethrough = true;
                            case OBFUSCATED -> isObfuscated = true;
                            case BOLD -> {
                                isBold = true;
                                fontChanged = true;
                            }
                            case ITALIC -> {
                                isItalic = true;
                                fontChanged = true;
                            }
                        }
                    }

                    // If style changes affect the font atlas, we must switch textures
                    if (fontChanged) {
                        UIFontAtlas targetFont;
                        if (isBold && this.bold != null) targetFont = this.bold;
                        else if (isItalic && this.italic != null) targetFont = this.italic;
                        else targetFont = (this.regular != null) ? this.regular : initialFont;

                        // Only flush if the font actually differs
                        if (targetFont != currentFont) {
                            // Flush current geometry with the OLD texture
                            UIRenderer.getInstance().getText().drawBatch(currentFont.getTextureId());
                            // Update current font reference for subsequent characters
                            currentFont = targetFont;

                            // Recalculate baseline offset as fonts might have slight metric differences.
                            baselineOffset = currentFont.getData().metrics.ascender * FONT_SIZE;
                            cursorY = y + baselineOffset;
                        }
                    }
                }
                i++; // Skip code character
                continue;
            }

            if (c == '\n') {
                cursorX = startX;
                continue;
            }

            if (isObfuscated) {
                if (c > 32) c = (char) (33 + (System.currentTimeMillis() / 50 + i) % 90);
            }

            // --- Glyph Resolution & Fallback Strategy ---
            // 1. Try to find the glyph in the currently active font (e.g., Bold)
            MSDFData.Glyph glyph = currentFont.getGlyph(c);

            // 2. Fallback: If missing in style (e.g., Bold), try Regular font
            if (glyph == null && currentFont != this.regular && this.regular != null) {
                MSDFData.Glyph fallback = this.regular.getGlyph(c);
                if (fallback != null) {
                    // Flush current batch (Bold)
                    UIRenderer.getInstance().getText().drawBatch(currentFont.getTextureId());

                    // Render single char using Regular texture
                    renderGlyph(mesh, fallback, cursorX, cursorY, this.regular, r, g, b, a);

                    // Flush Regular batch immediately
                    UIRenderer.getInstance().getText().drawBatch(this.regular.getTextureId());

                    // Advance cursor
                    cursorX += fallback.advance * FONT_SIZE;
                    continue; // Skip standard rendering
                }
            }

            // 3. Fallback: If still null (missing in Regular too), treat as missing character
            if (glyph == null) {
                // Determine a safe advance width (e.g. from space or '?' char)
                // This prevents text collapsing when characters are missing
                MSDFData.Glyph space = currentFont.getGlyph(' ');
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
            float midOffset = (currentFont.getData().metrics.ascender * FONT_SIZE) * 0.4f;
            float lineY = cursorY - midOffset;
            pendingDecorations.add(new Decoration(x, lineY, textWidth, thickness, finalColor));
        }

        // Final flush of whatever texture is currently active
        UIRenderer.getInstance().getText().drawBatch(currentFont.getTextureId());
        return cursorX;
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
    private void renderGlyph(UIMeshBuffer mesh, MSDFData.Glyph glyph, float cursorX, float cursorY, UIFontAtlas font, float r, float g, float b, float a) {
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
            float atlasW = font.getData().atlas.width;
            float atlasH = font.getData().atlas.height;

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
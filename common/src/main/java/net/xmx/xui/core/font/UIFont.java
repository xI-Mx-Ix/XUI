/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.gl.shader.UIShader;
import net.xmx.xui.core.gl.vertex.UIMeshBuffer;
import net.xmx.xui.core.gl.vertex.UIVertexFormat;
import net.xmx.xui.core.text.UIComponent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.stb.STBTTPackedchar;

import java.util.Random;

/**
 * The high-level font manager that handles different styles (Default, Bold, Italic)
 * and rendering logic including Obfuscation, Underline, and Strikethrough.
 * <p>
 * This class delegates the actual glyph generation to {@link LoadedFont}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIFont {

    private LoadedFont regular;
    private LoadedFont bold;
    private LoadedFont italic;
    
    // Using the generic UIMeshBuffer with the Position + Color + UV format
    private static final UIMeshBuffer meshBuffer = new UIMeshBuffer(UIVertexFormat.POS_COLOR_UV);
    private static final UIShader shader = new net.xmx.xui.core.gl.shader.impl.UITextShader();
    private static final Random random = new Random();

    /**
     * Loads the default font variation.
     *
     * @param path Classpath to the .ttf file.
     * @return This instance.
     */
    public UIFont setDefault(String path) {
        this.regular = new LoadedFont(path, 32.0f); // Load at 32px for high quality
        return this;
    }

    /**
     * Loads the bold font variation.
     *
     * @param path Classpath to the .ttf file.
     * @return This instance.
     */
    public UIFont setBold(String path) {
        this.bold = new LoadedFont(path, 32.0f);
        return this;
    }

    /**
     * Loads the italic font variation.
     *
     * @param path Classpath to the .ttf file.
     * @return This instance.
     */
    public UIFont setItalic(String path) {
        this.italic = new LoadedFont(path, 32.0f);
        return this;
    }

    /**
     * Gets the width of the text considering the component's style (e.g. Bold might be wider).
     *
     * @param component The UI component containing text and style.
     * @return The width in pixels.
     */
    public float getWidth(UIComponent component) {
        LoadedFont font = resolveFont(component);
        return font != null ? font.getStringWidth(component.getText()) : 0;
    }

    /**
     * Gets the line height of the font.
     *
     * @return The height in pixels.
     */
    public float getHeight() {
        return regular != null ? regular.getFontSize() : 0;
    }

    /**
     * Renders the text component using the appropriate style and procedural effects.
     *
     * @param renderer     The main render interface (used for drawing lines/rects).
     * @param component    The component holding text and style flags.
     * @param x            Screen X position.
     * @param y            Screen Y position.
     * @param color        The text color (ARGB).
     * @param projMat      The current projection matrix from the renderer.
     */
    public void draw(UIRenderInterface renderer, UIComponent component, float x, float y, int color, Matrix4f projMat) {
        LoadedFont font = resolveFont(component);
        if (font == null) return;

        String textToDraw = component.getText();
        
        // Handle Obfuscated text (Magic text)
        if (component.isObfuscated()) {
            textToDraw = scrambleText(textToDraw, font);
        }

        // Prepare Shader and Texture
        shader.bind();
        if (shader instanceof net.xmx.xui.core.gl.shader.impl.UITextShader) {
            ((net.xmx.xui.core.gl.shader.impl.UITextShader) shader).uploadProjection(projMat);
            ((net.xmx.xui.core.gl.shader.impl.UITextShader) shader).uploadTextureUnit(0);
        }
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, font.getTextureId());

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        float currentX = x;
        float currentY = y + font.getAscent(); 

        STBTTPackedchar.Buffer charData = font.getCharData();
        int atlasW = 1024; // Must match LoadedFont constant
        int atlasH = 1024;

        // --- Render Glyphs ---
        for (int i = 0; i < textToDraw.length(); i++) {
            char c = textToDraw.charAt(i);
            if (c < 32 || c >= 32 + 255) continue;

            STBTTPackedchar info = charData.get(c - 32);

            // Calculate Vertex positions
            float x0 = currentX + info.xoff();
            float y0 = currentY + info.yoff();
            float x1 = x0 + (info.xoff2() - info.xoff());
            float y1 = y0 + (info.yoff2() - info.yoff());

            // Calculate UV coordinates
            float u0 = info.x0() / (float) atlasW;
            float v0 = info.y0() / (float) atlasH;
            float u1 = info.x1() / (float) atlasW;
            float v1 = info.y1() / (float) atlasH;

            // Add Quad to Mesh using the fluent API (Triangle 1)
            meshBuffer.pos(x0, y0, 0).color(r, g, b, a).uv(u0, v0).endVertex(); // TL
            meshBuffer.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex(); // BL
            meshBuffer.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex(); // TR

            // Triangle 2
            meshBuffer.pos(x1, y0, 0).color(r, g, b, a).uv(u1, v0).endVertex(); // TR
            meshBuffer.pos(x0, y1, 0).color(r, g, b, a).uv(u0, v1).endVertex(); // BL
            meshBuffer.pos(x1, y1, 0).color(r, g, b, a).uv(u1, v1).endVertex(); // BR

            currentX += info.xadvance();
        }

        // Flush text geometry
        meshBuffer.flush(GL11.GL_TRIANGLES);
        shader.unbind();

        // --- Render Procedural Effects (Underline / Strikethrough) ---
        // We use the standard UIRenderInterface for simple rectangles
        float width = font.getStringWidth(textToDraw);

        if (component.isUnderline()) {
            // Draw line slightly below the baseline
            float lineY = y + font.getAscent() + 2; 
            renderer.drawRect(x, lineY, width, 1.0f, color, 0, 0, 0, 0);
        }

        if (component.isStrikethrough()) {
            // Draw line in the middle of the ascent
            float lineY = y + (font.getAscent() / 2.0f);
            renderer.drawRect(x, lineY, width, 1.0f, color, 0, 0, 0, 0);
        }
    }

    /**
     * Selects the correct internal font based on the component style.
     * Fallback logic ensures a font is always returned if default exists.
     */
    private LoadedFont resolveFont(UIComponent comp) {
        if (comp.isBold() && comp.isItalic()) {
            // Fallback to Bold if BoldItalic is not loaded
            if (bold != null) return bold;
        }
        
        if (comp.isBold()) {
            return bold != null ? bold : regular;
        }
        
        if (comp.isItalic()) {
            return italic != null ? italic : regular;
        }

        return regular;
    }

    /**
     * Randomizes characters for the obfuscated effect.
     */
    private String scrambleText(String input, LoadedFont font) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') continue;
            // Pick a random char within visible ASCII range
            chars[i] = (char) (33 + random.nextInt(90)); 
        }
        return new String(chars);
    }
}
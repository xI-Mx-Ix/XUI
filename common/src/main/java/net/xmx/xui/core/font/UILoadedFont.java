/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Internal class representing a single loaded TTF font variant (e.g., just Bold).
 * Handles the generation of the glyph texture atlas using STB TrueType.
 *
 * @author xI-Mx-Ix
 */
public class UILoadedFont {

    private final int textureId;
    private final STBTTPackedchar.Buffer charData;
    private final float fontSize;
    private final float ascent;
    private final float descent;
    private final float lineGap;

    // Configuration for the texture atlas
    private static final int BITMAP_W = 1024;
    private static final int BITMAP_H = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 255; // Extended ASCII / Latin-1

    /**
     * Loads a TTF file from the classpath and generates an OpenGL texture.
     *
     * @param resourcePath The path to the .ttf file.
     * @param size         The desired point size.
     */
    public UILoadedFont(String resourcePath, float size) {
        this.fontSize = size;

        // 1. Read the TTF file into a direct ByteBuffer
        ByteBuffer fontBuffer;
        try {
            fontBuffer = loadResourceToBuffer(resourcePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font resource: " + resourcePath, e);
        }

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new RuntimeException("Failed to initialize STB TrueType for: " + resourcePath);
        }

        // 2. Retrieve vertical metrics (Ascent, Descent, LineGap)
        int[] pAscent = new int[1];
        int[] pDescent = new int[1];
        int[] pLineGap = new int[1];
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap);

        // Convert raw font units to pixels based on the requested size
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, size);
        this.ascent = pAscent[0] * scale;
        this.descent = pDescent[0] * scale;
        this.lineGap = pLineGap[0] * scale;

        // 3. Generate the Texture Atlas
        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
        charData = STBTTPackedchar.create(CHAR_COUNT);

        try (STBTTPackContext pc = STBTTPackContext.malloc()) {
            STBTruetype.stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, MemoryUtil.NULL);

            // Use oversampling for sharper text on edges (2x horizontal, 2x vertical)
            STBTruetype.stbtt_PackSetOversampling(pc, 2, 2);

            STBTruetype.stbtt_PackFontRange(pc, fontBuffer, 0, size, FIRST_CHAR, charData);

            STBTruetype.stbtt_PackEnd(pc);
        }

        // 4. Upload to OpenGL (Modern Core Profile)
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1); // Ensure tight packing for single-byte pixels
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, BITMAP_W, BITMAP_H, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap);

        // Texture Parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        // We tell OpenGL to map the RED channel (where our data is) to the ALPHA channel when sampled.
        // This ensures the shader code "texture(tex, uv).a" continues to work correctly.
        // R, G, B become 1.0 (White), A becomes the Red value (Opacity).
        int[] swizzle = {GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_RED};
        GL11.glTexParameteriv(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_RGBA, swizzle);
    }

    public int getTextureId() {
        return textureId;
    }

    public STBTTPackedchar.Buffer getCharData() {
        return charData;
    }

    public float getAscent() {
        return ascent;
    }

    public float getDescent() {
        return descent;
    }

    public float getFontSize() {
        return fontSize;
    }

    /**
     * Calculates the width of a string using the packed character data.
     *
     * @param text The text to measure.
     * @return The width in pixels.
     */
    public float getStringWidth(String text) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) continue;
            STBTTPackedchar info = charData.get(c - FIRST_CHAR);
            width += info.xadvance();
        }
        return width;
    }

    /**
     * Utility to load a file from the classpath into a Direct ByteBuffer.
     */
    private ByteBuffer loadResourceToBuffer(String path) throws IOException {
        InputStream stream = UILoadedFont.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IOException("Resource not found: " + path);

        try (ReadableByteChannel rbc = Channels.newChannel(stream)) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(8192);
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
            buffer.flip();
            return buffer;
        }
    }
}
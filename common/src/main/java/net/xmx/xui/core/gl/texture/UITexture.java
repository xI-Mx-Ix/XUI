/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.texture;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Represents a single loaded OpenGL texture from a resource path.
 *
 * @author xI-Mx-Ix
 */
public class UITexture {

    private final int textureId;
    private final int width;
    private final int height;

    /**
     * Loads a texture from the classpath.
     *
     * @param namespace The mod namespace (e.g. "xui").
     * @param path      The path inside /assets/namespace/ (e.g. "textures/icon.png").
     */
    public UITexture(String namespace, String path) {
        String fullPath = "/assets/" + namespace + "/" + path;

        try (InputStream imgStream = getClass().getResourceAsStream(fullPath)) {
            if (imgStream == null) {
                throw new RuntimeException("Texture resource not found: " + fullPath);
            }

            byte[] bytes = imgStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer c = BufferUtils.createIntBuffer(1);

            // Load as RGBA (4 channels)
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, c, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture via STB: " + STBImage.stbi_failure_reason());
            }

            this.width = w.get(0);
            this.height = h.get(0);

            // Generate Texture
            this.textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Parameters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            // Upload
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);

            STBImage.stbi_image_free(image);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create texture: " + fullPath, e);
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }
    
    public void cleanup() {
        GL11.glDeleteTextures(textureId);
    }
}
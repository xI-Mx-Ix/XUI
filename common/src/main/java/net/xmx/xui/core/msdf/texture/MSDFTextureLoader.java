/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.msdf.texture;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Centralized utility for loading Signed Distance Field (SDF) textures.
 * <p>
 * Handles the OpenGL texture parameters required for SDF rendering, such as
 * linear filtering and edge clamping. Currently supports standard MSDF textures (RGB).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MSDFTextureLoader {

    /**
     * Loads a standard MSDF texture (RGB).
     * <p>
     * Forces the image to be decoded as 3 channels (RGB). Standard MSDF is mainly used for fonts,
     * providing sharp edge reconstruction without alpha-channel correction.
     * </p>
     *
     * @param path The absolute classpath to the resource.
     * @return The OpenGL texture ID handle.
     * @throws RuntimeException If loading fails.
     */
    public static int loadMSDFTexture(String path) {
        // Load as 3 channels (RGB) and upload as GL_RGB
        return loadTextureInternal(path, 3, GL11.GL_RGB);
    }

    /**
     * Internal generic loading logic used by MSDF loader.
     * <p>
     * Reads the input stream, decodes the image using STB, and uploads it to OpenGL with
     * the correct texture parameters for SDF rendering.
     * </p>
     *
     * @param path            The resource path.
     * @param desiredChannels The number of channels to force decode (3 for RGB).
     * @param glFormat        The OpenGL format constant (GL_RGB).
     * @return The generated OpenGL texture ID.
     * @throws RuntimeException If loading or decoding fails.
     */
    private static int loadTextureInternal(String path, int desiredChannels, int glFormat) {
        try (InputStream imgStream = MSDFTextureLoader.class.getResourceAsStream(path)) {
            if (imgStream == null) {
                throw new RuntimeException("SDF texture resource not found: " + path);
            }

            // Read input stream into a direct ByteBuffer
            byte[] bytes = imgStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer c = BufferUtils.createIntBuffer(1);

            // Decode the image with STB, enforcing the desired channel count
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, c, desiredChannels);
            if (image == null) {
                throw new RuntimeException("STB failed to load image (" + path + "): " + STBImage.stbi_failure_reason());
            }

            // Generate OpenGL texture
            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Set filtering and wrapping parameters for SDF
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            // Upload image data to GPU
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, glFormat, w.get(0), h.get(0), 0, glFormat, GL11.GL_UNSIGNED_BYTE, image);

            // Free native memory
            STBImage.stbi_image_free(image);

            return textureId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }
}
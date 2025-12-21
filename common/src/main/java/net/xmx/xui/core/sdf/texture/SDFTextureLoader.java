/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.sdf.texture;

import net.xmx.xui.core.sdf.SDFType;
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
 * linear filtering and edge clamping. Supports both MSDF (RGB) and MTSDF (RGBA).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class SDFTextureLoader {

    /**
     * Loads an SDF texture based on the specific SDF Type provided in the metadata.
     * <p>
     * Automatically resolves the required channel count:
     * <ul>
     *     <li>{@link SDFType#MSDF} -> 3 Channels (RGB)</li>
     *     <li>{@link SDFType#MTSDF} -> 4 Channels (RGBA)</li>
     * </ul>
     * </p>
     *
     * @param path The absolute classpath to the resource.
     * @param type The SDF type definition (MSDF or MTSDF).
     * @return The OpenGL texture ID handle.
     */
    public static int loadSDFTexture(String path, SDFType type) {
        // MTSDF benötigt 4 Kanäle (RGBA), MSDF nur 3 (RGB)
        int channels = (type == SDFType.MTSDF) ? 4 : 3;
        return loadSDFTexture(path, channels);
    }

    /**
     * Loads an SDF texture with the specified channel configuration.
     *
     * @param path     The absolute classpath to the resource.
     * @param channels The number of channels to load (3 for MSDF, 4 for MTSDF).
     * @return The OpenGL texture ID handle.
     */
    public static int loadSDFTexture(String path, int channels) {
        int format = (channels == 4) ? GL11.GL_RGBA : GL11.GL_RGB;
        return loadTextureInternal(path, channels, format);
    }

    /**
     * Internal generic loading logic.
     *
     * @param path            The resource path.
     * @param desiredChannels The number of channels to force decode.
     * @param glFormat        The OpenGL format constant.
     * @return The generated OpenGL texture ID.
     */
    private static int loadTextureInternal(String path, int desiredChannels, int glFormat) {
        try (InputStream imgStream = SDFTextureLoader.class.getResourceAsStream(path)) {
            if (imgStream == null) {
                throw new RuntimeException("SDF texture resource not found: " + path);
            }

            byte[] bytes = imgStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer c = BufferUtils.createIntBuffer(1);

            // Decode the image with STB
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, c, desiredChannels);
            if (image == null) {
                throw new RuntimeException("STB failed to load image (" + path + "): " + STBImage.stbi_failure_reason());
            }

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Set filtering and wrapping parameters for SDF
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            // Upload image data to GPU
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, glFormat, w.get(0), h.get(0), 0, glFormat, GL11.GL_UNSIGNED_BYTE, image);

            STBImage.stbi_image_free(image);

            return textureId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }
}
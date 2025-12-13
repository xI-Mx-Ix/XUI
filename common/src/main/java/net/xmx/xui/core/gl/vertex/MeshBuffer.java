/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.vertex;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * A generic dynamic mesh buffer that adapts to any {@link VertexFormat}.
 * <p>
 * This class replaces the specialized geometry and text buffers.
 * Renderers interact with the raw {@link #put(float)} method to push data.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class MeshBuffer {

    private static final int MAX_VERTICES = 16384;

    private final VertexFormat format;
    private final int vaoId;
    private final int vboId;
    private final FloatBuffer buffer;

    private int vertexCount = 0;

    /**
     * Creates a new mesh buffer with the specified vertex format.
     *
     * @param format The format definition (e.g., {@link VertexFormat#POS_COLOR}).
     */
    public MeshBuffer(VertexFormat format) {
        this.format = format;
        
        // Calculate total buffer size
        this.buffer = BufferUtils.createFloatBuffer(MAX_VERTICES * format.getStrideFloats());

        // Generate OpenGL Objects
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // Allocate GPU memory (Dynamic Draw)
        long sizeBytes = (long) MAX_VERTICES * format.getStrideFloats() * 4;
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, sizeBytes, GL15.GL_DYNAMIC_DRAW);

        // Apply attributes logic from the format
        format.enableAttributes();

        GL30.glBindVertexArray(0);
    }

    /**
     * Pushes a single float value into the buffer.
     * Used by renderers to build vertices component by component.
     *
     * @param f The float value.
     * @return This buffer for chaining.
     */
    public MeshBuffer put(float f) {
        // Safety check: prevent buffer overflow
        if (buffer.position() < buffer.capacity()) {
            buffer.put(f);
        }
        return this;
    }

    /**
     * Marks the completion of a single vertex.
     * Must be called after putting all components for one vertex.
     */
    public void endVertex() {
        vertexCount++;
        // Optional: Auto-flush if full logic could go here
    }

    /**
     * Helper to add a position (x, y, z).
     */
    public MeshBuffer pos(float x, float y, float z) {
        buffer.put(x).put(y).put(z);
        return this;
    }

    /**
     * Helper to add a color (r, g, b, a).
     */
    public MeshBuffer color(float r, float g, float b, float a) {
        buffer.put(r).put(g).put(b).put(a);
        return this;
    }

    /**
     * Helper to add texture coordinates (u, v).
     */
    public MeshBuffer uv(float u, float v) {
        buffer.put(u).put(v);
        return this;
    }

    /**
     * Uploads the buffer to the GPU and draws the geometry.
     *
     * @param drawMode The OpenGL primitive type (e.g., GL_TRIANGLES).
     */
    public void flush(int drawMode) {
        if (vertexCount == 0) return;

        buffer.flip();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // Upload only the active part of the buffer
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

        // Draw
        GL11.glDrawArrays(drawMode, 0, vertexCount);

        GL30.glBindVertexArray(0);

        // Reset
        buffer.clear();
        vertexCount = 0;
    }

    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoId);
        GL15.glDeleteBuffers(vboId);
    }
}
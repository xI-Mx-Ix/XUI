/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * Handles batching and rendering of vertices using OpenGL Core Profile features (VAO/VBO).
 * This replaces the legacy immediate mode or Minecraft's Tesselator for custom UI rendering.
 *
 * <p>Vertices are stored as [x, y, z, r, g, b, a].</p>
 *
 * @author xI-Mx-Ix
 */
public class UIMeshBuffer {

    private static final int MAX_VERTICES = 8192;
    private static final int FLOAT_SIZE = 4;
    // x, y, z (3) + r, g, b, a (4)
    private static final int VERTEX_SIZE = 7;

    private final int vaoId;
    private final int vboId;
    private final FloatBuffer buffer;

    private int vertexCount = 0;

    /**
     * Initializes the VAO and VBO for rendering.
     */
    public UIMeshBuffer() {
        buffer = BufferUtils.createFloatBuffer(MAX_VERTICES * VERTEX_SIZE);

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // Allocate buffer memory (Dynamic Draw as UI changes every frame)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_VERTICES * VERTEX_SIZE * FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);

        // Attribute 0: Position (3 floats)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERTEX_SIZE * FLOAT_SIZE, 0);
        GL20.glEnableVertexAttribArray(0);

        // Attribute 1: Color (4 floats)
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, VERTEX_SIZE * FLOAT_SIZE, 3 * FLOAT_SIZE);
        GL20.glEnableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
    }

    /**
     * Adds a single vertex to the buffer.
     * Automatically flushes the buffer if it becomes full.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param r Red component (0.0-1.0).
     * @param g Green component (0.0-1.0).
     * @param b Blue component (0.0-1.0).
     * @param a Alpha component (0.0-1.0).
     */
    public void addVertex(float x, float y, float z, float r, float g, float b, float a) {
        if (vertexCount >= MAX_VERTICES) {
            flush(GL11.GL_TRIANGLES);
        }

        buffer.put(x).put(y).put(z);
        buffer.put(r).put(g).put(b).put(a);
        vertexCount++;
    }

    /**
     * Uploads the current buffer to the GPU and executes the draw call.
     * Resets the buffer after drawing.
     *
     * @param drawMode The OpenGL primitive type (e.g., GL_TRIANGLES).
     */
    public void flush(int drawMode) {
        if (vertexCount == 0) return;

        buffer.flip();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        // Upload only the used part of the buffer
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

        // Draw
        GL11.glDrawArrays(drawMode, 0, vertexCount);

        GL30.glBindVertexArray(0);

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
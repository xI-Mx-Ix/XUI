/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.vertex;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.List;

/**
 * Defines the structure of vertices in a mesh buffer.
 * Calculates strides and offsets automatically and handles VAO attribute enabling.
 *
 * @author xI-Mx-Ix
 */
public class VertexFormat {

    // --- Standard Formats ---

    /**
     * Format: Position (3) + Color (4)
     */
    public static final VertexFormat POS_COLOR = new VertexFormat(
            new VertexAttribute(0, 3),
            new VertexAttribute(1, 4)
    );

    /**
     *  Format: Position (3) + Color (4) + UV (2)
     */
    public static final VertexFormat POS_COLOR_UV = new VertexFormat(
            new VertexAttribute(0, 3),
            new VertexAttribute(1, 4),
            new VertexAttribute(2, 2)
    );

    // --- Implementation ---

    private final List<VertexAttribute> attributes;
    private final int strideBytes;
    private final int strideFloats;

    /**
     * Constructs a format from a list of attributes.
     *
     * @param attributes The attributes in order.
     */
    public VertexFormat(VertexAttribute... attributes) {
        this.attributes = List.of(attributes);

        int totalFloats = 0;
        for (VertexAttribute attr : this.attributes) {
            totalFloats += attr.count();
        }
        this.strideFloats = totalFloats;
        this.strideBytes = totalFloats * 4; // 4 bytes per float
    }

    /**
     * Applies the format to the currently bound VAO.
     * Calculates the pointer offsets for each attribute.
     */
    public void enableAttributes() {
        long offset = 0;
        for (VertexAttribute attr : attributes) {
            GL20.glVertexAttribPointer(
                    attr.index(),
                    attr.count(),
                    GL11.GL_FLOAT,
                    false,
                    strideBytes,
                    offset
            );
            GL20.glEnableVertexAttribArray(attr.index());

            // Move offset forward by the size of this attribute in bytes
            offset += (long) attr.count() * 4;
        }
    }

    /**
     * Gets the total size of one vertex in number of floats.
     * @return float count per vertex.
     */
    public int getStrideFloats() {
        return strideFloats;
    }
}
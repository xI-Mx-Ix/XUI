/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.vertex;

/**
 * Represents a single attribute within a vertex format (e.g., Position, Color, UV).
 *
 * @author xI-Mx-Ix
 */
public record VertexAttribute(int index, int count) {
    /**
     * Creates a new vertex attribute definition.
     *
     * @param index The shader attribute location (layout location = X).
     * @param count The number of components (e.g., 3 for vec3, 4 for vec4).
     */
    public VertexAttribute {
        if (count < 1 || count > 4) {
            throw new IllegalArgumentException("Attribute count must be between 1 and 4");
        }
    }
}
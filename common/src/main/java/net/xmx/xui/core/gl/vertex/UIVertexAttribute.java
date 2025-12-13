/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.vertex;

/**
 * Represents a single attribute within a vertex format (e.g., Position, Color, UV).
 *
 * @author xI-Mx-Ix
 */
public record UIVertexAttribute(int index, int count) {
    /**
     * Creates a new vertex attribute definition.
     *
     * @param index The shader attribute location (layout location = X).
     * @param count The number of components (e.g., 3 for vec3, 4 for vec4).
     */
    public UIVertexAttribute {
        if (count < 1 || count > 4) {
            throw new IllegalArgumentException("Attribute count must be between 1 and 4");
        }
    }
}
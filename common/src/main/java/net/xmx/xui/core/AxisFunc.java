/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

/**
 * Functional interface for calculating a position or dimension dynamically.
 *
 * @author xI-Mx-Ix
 */
@FunctionalInterface
public interface AxisFunc {

    /**
     * Calculates the pixel value based on the parent and self context.
     *
     * @param parentPos  The absolute position of the parent (X or Y).
     * @param parentSize The size of the parent (Width or Height).
     * @param selfSize   The size of the widget itself (Width or Height).
     * @return The calculated absolute position or dimension in pixels.
     */
    float calculate(float parentPos, float parentSize, float selfSize);
}
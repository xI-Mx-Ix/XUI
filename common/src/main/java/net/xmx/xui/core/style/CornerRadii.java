/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.style;

/**
 * Immutable value object representing the radii for the four corners of a rectangle.
 * Used for precise control over rounded corners.
 *
 * @param topLeft     Radius of the top-left corner in pixels.
 * @param topRight    Radius of the top-right corner in pixels.
 * @param bottomRight Radius of the bottom-right corner in pixels.
 * @param bottomLeft  Radius of the bottom-left corner in pixels.
 *
 * @author xI-Mx-Ix
 */
public record CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {

    /**
     * Constant for zero radius on all corners (Sharp).
     */
    public static final CornerRadii ZERO = new CornerRadii(0, 0, 0, 0);

    /**
     * Creates a uniform radius for all corners.
     *
     * @param radius The radius in pixels.
     * @return A new CornerRadii instance.
     */
    public static CornerRadii all(float radius) {
        return new CornerRadii(radius, radius, radius, radius);
    }

    /**
     * Creates a CornerRadii with explicit values.
     */
    public static CornerRadii of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return new CornerRadii(topLeft, topRight, bottomRight, bottomLeft);
    }

    /**
     * Linearly interpolates between this set of radii and another.
     * Used by the animation system.
     *
     * @param target The target radii.
     * @param t      The interpolation factor (0.0 to 1.0).
     * @return A new interpolated CornerRadii instance.
     */
    public CornerRadii lerp(CornerRadii target, float t) {
        return new CornerRadii(
                this.topLeft + (target.topLeft - this.topLeft) * t,
                this.topRight + (target.topRight - this.topRight) * t,
                this.bottomRight + (target.bottomRight - this.bottomRight) * t,
                this.bottomLeft + (target.bottomLeft - this.bottomLeft) * t
        );
    }
}
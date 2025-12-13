/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

/**
 * Functional interface for defining easing curves used in animations.
 * <p>
 * Easing functions specify the rate of change of a parameter over time.
 * Real objects don't just start and stop instantly, and they almost never move at a constant speed.
 * This interface provides standard mathematical functions to interpolate progress non-linearly.
 * </p>
 *
 * @author xI-Mx-Ix
 */
@FunctionalInterface
public interface UIEasing {

    /**
     * Applies the easing function to a linear progress value.
     *
     * @param t The linear progress between 0.0 and 1.0.
     * @return The interpolated progress value. Note that some easings (like Elastic or Back)
     *         may return values less than 0.0 or greater than 1.0 to simulate overshooting.
     */
    float apply(float t);

    /**
     * Linear interpolation (no easing).
     * The animation moves at a constant speed.
     */
    UIEasing LINEAR = t -> t;

    /**
     * Easing equation for a quadratic (t^2) ease-in.
     * Accelerates from zero velocity.
     */
    UIEasing EASE_IN_QUAD = t -> t * t;

    /**
     * Easing equation for a quadratic (t^2) ease-out.
     * Decelerates to zero velocity.
     */
    UIEasing EASE_OUT_QUAD = t -> t * (2 - t);

    /**
     * Easing equation for a quadratic (t^2) ease-in/out.
     * Accelerates until halfway, then decelerates.
     */
    UIEasing EASE_IN_OUT_QUAD = t -> t < .5 ? 2 * t * t : -1 + (4 - 2 * t) * t;

    /**
     * Easing equation for a cubic (t^3) ease-in.
     * Starts slower and accelerates faster than Quad.
     */
    UIEasing EASE_IN_CUBIC = t -> t * t * t;

    /**
     * Easing equation for a cubic (t^3) ease-out.
     * Decelerates faster than Quad.
     */
    UIEasing EASE_OUT_CUBIC = t -> (--t) * t * t + 1;

    /**
     * Easing equation for a back ease-in.
     * Pulls back slightly before accelerating (like an arrow in a bow).
     */
    UIEasing EASE_IN_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    };

    /**
     * Easing equation for a back ease-out.
     * Overshoots the target slightly before settling.
     */
    UIEasing EASE_OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        // Cast to float required as Math.pow returns double
        return (float) (1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2));
    };

    /**
     * Easing equation for a bounce ease-out.
     * Bounces at the end of the transition (like a ball dropping).
     */
    UIEasing EASE_OUT_BOUNCE = t -> {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5 / d1) * t + 0.75f;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25 / d1) * t + 0.9375f;
        } else {
            return n1 * (t -= 2.625 / d1) * t + 0.984375f;
        }
    };
}
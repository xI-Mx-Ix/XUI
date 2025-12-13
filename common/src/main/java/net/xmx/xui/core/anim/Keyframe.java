/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

/**
 * Represents a single point in time within an animation timeline.
 * <p>
 * A keyframe defines what value a specific property should have at a specific time,
 * and which easing function should be used to reach this value from the previous keyframe.
 * </p>
 *
 * @param <T> The type of the value being animated (e.g., Float for rotation, Integer for color).
 * @param time  The absolute time in seconds (relative to the start of the animation) when this keyframe occurs.
 * @param value The target value of the property at this time.
 * @param easing The easing function to use when interpolating <b>towards</b> this keyframe.
 *
 * @author xI-Mx-Ix
 */
public record Keyframe<T>(float time, T value, UIEasing easing) implements Comparable<Keyframe<T>> {

    /**
     * Compares this keyframe with another based on time.
     * Used to sort keyframes chronologically in the timeline.
     *
     * @param other The other keyframe to compare against.
     * @return A negative integer, zero, or a positive integer as this keyframe's time
     *         is less than, equal to, or greater than the specified keyframe's time.
     */
    @Override
    public int compareTo(Keyframe<T> other) {
        return Float.compare(this.time, other.time);
    }
}
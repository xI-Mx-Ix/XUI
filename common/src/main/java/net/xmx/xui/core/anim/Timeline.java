/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

import net.xmx.xui.core.style.UIProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a sequence of keyframes for a single UI Property.
 * <p>
 * The Timeline is responsible for:
 * 1. Storing keyframes in chronological order.
 * 2. Determining which two keyframes surround the current animation time.
 * 3. Calculating the interpolated value between those keyframes based on the easing function.
 * </p>
 *
 * @param <T> The type of value being animated (Float, Integer, etc.).
 * @author xI-Mx-Ix
 */
public class Timeline<T> {

    /**
     * The property that this timeline modifies (e.g., Properties.ROTATION_X).
     */
    private final UIProperty<T> property;

    /**
     * The sorted list of keyframes.
     */
    private final List<Keyframe<T>> keyframes = new ArrayList<>();

    /**
     * Constructs a new Timeline for a specific property.
     *
     * @param property The property to animate.
     */
    public Timeline(UIProperty<T> property) {
        this.property = property;
    }

    /**
     * Adds a new keyframe to the timeline.
     * The list is automatically re-sorted to ensure time consistency.
     *
     * @param time   The time in seconds.
     * @param value  The target value.
     * @param easing The easing curve to reach this value.
     */
    public void addKeyframe(float time, T value, UIEasing easing) {
        keyframes.add(new Keyframe<>(time, value, easing));
        Collections.sort(keyframes);
    }

    /**
     * Gets the property associated with this timeline.
     *
     * @return The UIProperty key.
     */
    public UIProperty<T> getProperty() {
        return property;
    }

    /**
     * Gets the time of the very last keyframe.
     * Used to determine the total duration of the animation sequence.
     *
     * @return The max time in seconds, or 0 if empty.
     */
    public float getLastKeyframeTime() {
        if (keyframes.isEmpty()) return 0f;
        return keyframes.get(keyframes.size() - 1).time();
    }

    /**
     * Calculates the interpolated value of the property at a specific point in time.
     *
     * @param time The elapsed time of the animation in seconds.
     * @return The interpolated value (Color int or Float).
     */
    @SuppressWarnings("unchecked")
    public T getValueAt(float time) {
        if (keyframes.isEmpty()) return property.getDefault();

        // Case 1: Time is before the first keyframe. Return first value.
        if (time <= keyframes.get(0).time()) {
            return keyframes.get(0).value();
        }

        // Case 2: Time is after the last keyframe. Return last value.
        if (time >= keyframes.get(keyframes.size() - 1).time()) {
            return keyframes.get(keyframes.size() - 1).value();
        }

        // Case 3: Time is between two keyframes. Interpolate.
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe<T> startFrame = keyframes.get(i);
            Keyframe<T> endFrame = keyframes.get(i + 1);

            if (time >= startFrame.time() && time < endFrame.time()) {
                float duration = endFrame.time() - startFrame.time();
                // Avoid division by zero
                if (duration <= 0) return endFrame.value();

                // Calculate linear progress (0.0 to 1.0)
                float linearProgress = (time - startFrame.time()) / duration;

                // Apply the easing function defined in the TARGET (end) keyframe
                float easedProgress = endFrame.easing().apply(linearProgress);

                return interpolate(startFrame.value(), endFrame.value(), easedProgress);
            }
        }

        // Fallback (should not be reached due to Case 2)
        return keyframes.get(keyframes.size() - 1).value();
    }

    /**
     * Helper method to interpolate between two generic values.
     * Supports Floats (Linear Algebra) and Integers (Color Channels).
     *
     * @param start The start value.
     * @param end   The end value.
     * @param t     The interpolation factor (0.0 to 1.0).
     * @return The interpolated result.
     */
    @SuppressWarnings("unchecked")
    private T interpolate(T start, T end, float t) {
        if (start instanceof Float && end instanceof Float) {
            float s = (Float) start;
            float e = (Float) end;
            return (T) Float.valueOf(s + (e - s) * t);
        }
        if (start instanceof Integer && end instanceof Integer) {
            // Standard ARGB Color Interpolation
            int c1 = (Integer) start;
            int c2 = (Integer) end;
            
            int a1 = (c1 >> 24) & 0xFF;
            int r1 = (c1 >> 16) & 0xFF;
            int g1 = (c1 >> 8) & 0xFF;
            int b1 = c1 & 0xFF;

            int a2 = (c2 >> 24) & 0xFF;
            int r2 = (c2 >> 16) & 0xFF;
            int g2 = (c2 >> 8) & 0xFF;
            int b2 = c2 & 0xFF;

            int a = (int) (a1 + (a2 - a1) * t);
            int r = (int) (r1 + (r2 - r1) * t);
            int g = (int) (g1 + (g2 - g1) * t);
            int b = (int) (b1 + (b2 - b1) * t);

            // Clamp values to valid byte range (0-255) to prevent overflow/underflow artifacts
            a = Math.max(0, Math.min(255, a));
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            return (T) Integer.valueOf((a << 24) | (r << 16) | (g << 8) | b);
        }
        
        // Fallback for non-interpolatable types (e.g. Boolean, Enums) -> Snap to End
        return t >= 0.5f ? end : start;
    }
}
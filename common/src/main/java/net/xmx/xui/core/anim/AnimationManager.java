package net.xmx.xui.core.anim;

import net.xmx.xui.core.style.UIProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages value interpolation for UI properties.
 * It stores the current "live" animated values and moves them towards target values.
 */
public class AnimationManager {

    private final Map<UIProperty<?>, Object> currentValues = new HashMap<>();

    /**
     * Gets or updates the animated float value.
     *
     * @param prop   The property key.
     * @param target The target value from the stylesheet.
     * @param speed  The interpolation speed (higher = faster, typical range 5-20).
     * @param dt     Delta time in seconds.
     * @return The interpolated value for the current frame.
     */
    public float getAnimatedFloat(UIProperty<Float> prop, float target, float speed, float dt) {
        float current = (float) currentValues.getOrDefault(prop, target);

        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            current = target;
        } else {
            // Exponential decay interpolation: lerp factor = 1 - e^(-speed * dt)
            // This ensures smooth, framerate-independent animation
            float lerpFactor = 1.0f - (float) Math.exp(-speed * dt);
            current += diff * lerpFactor;
        }

        currentValues.put(prop, current);
        return current;
    }

    /**
     * Gets or updates the animated color (Integer) value.
     *
     * @param prop   The property key.
     * @param target The target ARGB color.
     * @param speed  The interpolation speed (higher = faster, typical range 5-20).
     * @param dt     Delta time in seconds.
     * @return The interpolated color for the current frame.
     */
    public int getAnimatedColor(UIProperty<Integer> prop, int target, float speed, float dt) {
        int current = (int) currentValues.getOrDefault(prop, target);

        if (current == target) return current;

        // Exponential decay interpolation
        float lerpFactor = 1.0f - (float) Math.exp(-speed * dt);
        // Clamp to prevent overshoot
        if (lerpFactor > 1.0f) lerpFactor = 1.0f;

        int next = interpolateColor(current, target, lerpFactor);

        currentValues.put(prop, next);
        return next;
    }

    private int interpolateColor(int c1, int c2, float factor) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
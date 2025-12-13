/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

import net.xmx.xui.core.style.StyleKey;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages value interpolation for UI properties.
 * It stores the current "live" animated values and moves them towards target values.
 *
 * @author xI-Mx-Ix
 */
public class AnimationManager {

    private final Map<StyleKey<?>, Object> currentValues = new HashMap<>();

    // Thread-safe list to hold active complex animations (New)
    private final List<AnimationInstance> activeAnimations = new CopyOnWriteArrayList<>();

    /**
     * Registers a new animation instance to be updated by this manager.
     *
     * @param animation The animation to start.
     */
    public void startAnimation(AnimationInstance animation) {
        activeAnimations.add(animation);
    }

    /**
     * Updates all active animations. Should be called once per frame.
     *
     * @param dt Delta time in seconds.
     */
    public void update(float dt) {
        Iterator<AnimationInstance> it = activeAnimations.iterator();
        while (it.hasNext()) {
            AnimationInstance anim = it.next();
            boolean finished = anim.update(dt);
            if (finished) {
                activeAnimations.remove(anim);
            }
        }
    }

    /**
     * Gets or updates the animated float value.
     *
     * @param prop   The property key.
     * @param target The target value from the stylesheet.
     * @param speed  The interpolation speed (higher = faster, typical range 5-20).
     * @param dt     Delta time in seconds.
     * @return The interpolated value for the current frame.
     */
    public float getAnimatedFloat(StyleKey<Float> prop, float target, float speed, float dt) {
        // Sanity Check: Clamp dt to prevent instant jumps if the game lags or partialTicks was passed by mistake.
        // Max 0.1s (10 FPS) per frame calculation.
        float safeDt = Math.min(dt, 0.1f);

        float current = (float) currentValues.getOrDefault(prop, target);

        float diff = target - current;
        // If difference is negligible, snap to target to save CPU
        if (Math.abs(diff) < 0.001f) {
            current = target;
        } else {
            // Exponential decay interpolation
            // Formula: current + (target - current) * (1 - e^(-speed * dt))
            float lerpFactor = 1.0f - (float) Math.exp(-speed * safeDt);

            // Safety: Ensure lerpFactor is between 0 and 1
            lerpFactor = Math.max(0.0f, Math.min(1.0f, lerpFactor));

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
     * @param speed  The interpolation speed.
     * @param dt     Delta time in seconds.
     * @return The interpolated color for the current frame.
     */
    public int getAnimatedColor(StyleKey<Integer> prop, int target, float speed, float dt) {
        // Sanity Check: Clamp dt
        float safeDt = Math.min(dt, 0.1f);

        int current = (int) currentValues.getOrDefault(prop, target);

        if (current == target) return current;

        // Exponential decay interpolation
        float lerpFactor = 1.0f - (float) Math.exp(-speed * safeDt);

        // Safety clamp
        if (lerpFactor >= 1.0f) {
            currentValues.put(prop, target);
            return target;
        }

        int next = interpolateColor(current, target, lerpFactor);

        // If the color is extremely close to target (due to integer rounding), snap to target
        if (isColorClose(next, target)) {
            next = target;
        }

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

    private boolean isColorClose(int c1, int c2) {
        int rDiff = Math.abs(((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF));
        int gDiff = Math.abs(((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF));
        int bDiff = Math.abs((c1 & 0xFF) - (c2 & 0xFF));
        int aDiff = Math.abs(((c1 >> 24) & 0xFF) - ((c2 >> 24) & 0xFF));
        return rDiff < 2 && gDiff < 2 && bDiff < 2 && aDiff < 2;
    }
}
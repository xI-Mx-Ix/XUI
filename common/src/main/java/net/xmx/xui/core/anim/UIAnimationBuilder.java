/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.UIProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fluent Builder for creating complex 3D Keyframe Animations.
 * <p>
 * Unlike simple interpolation, this builder constructs full {@link Timeline}s for each property,
 * allowing precise control over multiple steps, easing changes, and synchronization.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIAnimationBuilder {

    private final UIWidget widget;
    
    /**
     * Maps a property (e.g., Scale X) to its specific timeline of keyframes.
     */
    private final Map<UIProperty<?>, Timeline<?>> timelines = new HashMap<>();
    
    /**
     * List of scheduled callbacks to trigger at specific times.
     */
    private final List<AnimationCallback> callbacks = new ArrayList<>();
    
    private boolean loop = false;
    private Runnable onComplete;

    /**
     * Constructs a new builder for the target widget.
     *
     * @param widget The widget to animate.
     */
    public UIAnimationBuilder(UIWidget widget) {
        this.widget = widget;
    }

    /**
     * Defines a specific keyframe for a property.
     *
     * @param time     The time in seconds from the start of the animation.
     * @param property The property to animate (e.g., Properties.ROTATION_Z).
     * @param value    The target value at this time.
     * @param easing   The easing curve used to approach this value from the previous keyframe.
     * @param <T>      The value type.
     * @return This builder instance.
     */
    public <T> UIAnimationBuilder keyframe(float time, UIProperty<T> property, T value, UIEasing easing) {
        // Retrieve or create the timeline for this property
        @SuppressWarnings("unchecked")
        Timeline<T> timeline = (Timeline<T>) timelines.computeIfAbsent(property, Timeline::new);
        
        // Add the keyframe
        timeline.addKeyframe(time, value, easing);
        return this;
    }

    /**
     * Convenience: Sets the starting value (Time 0.0s) for a property.
     * Use this if you want the animation to start from a specific value, rather than
     * the widget's current value.
     */
    public <T> UIAnimationBuilder setStart(UIProperty<T> property, T value) {
        return keyframe(0.0f, property, value, UIEasing.LINEAR);
    }

    /**
     * Convenience: Adds a keyframe with Linear easing.
     */
    public <T> UIAnimationBuilder keyframe(float time, UIProperty<T> property, T value) {
        return keyframe(time, property, value, UIEasing.LINEAR);
    }

    /**
     * Schedules a one-time action to run when the animation passes a specific timestamp.
     *
     * @param time   The time in seconds.
     * @param action The code to execute.
     * @return This builder instance.
     */
    public UIAnimationBuilder atTime(float time, Runnable action) {
        callbacks.add(new AnimationCallback(time, action));
        return this;
    }

    /**
     * Sets whether the entire animation sequence should loop indefinitely.
     *
     * @param loop True to loop.
     * @return This builder instance.
     */
    public UIAnimationBuilder loop(boolean loop) {
        this.loop = loop;
        return this;
    }

    /**
     * Sets a callback to run when the entire animation (longest timeline) finishes.
     * Note: If {@code loop} is true, this is never called.
     *
     * @param action The code to execute.
     * @return This builder instance.
     */
    public UIAnimationBuilder onComplete(Runnable action) {
        this.onComplete = action;
        return this;
    }

    /**
     * Compiles the timelines and starts the animation on the widget.
     */
    public void start() {
        // Create the runtime instance
        UIAnimationInstance instance = new UIAnimationInstance(
                widget, timelines, callbacks, loop, onComplete
        );
        
        // Register it with the widget's manager
        widget.getAnimationManager().startAnimation(instance);
    }
    
    /**
     * Internal record to store callback requests.
     */
    public record AnimationCallback(float time, Runnable action) {}
}
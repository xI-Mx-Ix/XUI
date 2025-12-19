/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.anim;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fluent Builder for creating complex 3D Keyframe Animations.
 * <p>
 * <b>Smart CSS-like Behavior:</b>
 * If you do not define a keyframe at time 0.0s for a property, this builder will
 * automatically snapshot the widget's <i>current</i> visual value (including active animations)
 * and use it as the starting point. This ensures smooth transitions without "popping",
 * even if the widget is interrupted mid-move.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class AnimationBuilder {

    private final UIWidget widget;
    
    /**
     * Maps a property (e.g., Scale X) to its specific timeline of keyframes.
     */
    private final Map<StyleKey<?>, Timeline<?>> timelines = new HashMap<>();
    
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
    public AnimationBuilder(UIWidget widget) {
        this.widget = widget;
    }

    /**
     * Defines a specific keyframe for a property.
     *
     * @param time     The time in seconds from the start of the animation.
     * @param property The property to animate (e.g., ThemeProperties.ROTATION_Z).
     * @param value    The target value at this time.
     * @param easing   The easing curve used to approach this value from the previous keyframe.
     * @param <T>      The value type.
     * @return This builder instance.
     */
    public <T> AnimationBuilder keyframe(float time, StyleKey<T> property, T value, Easing easing) {
        // Retrieve or create the timeline for this property
        @SuppressWarnings("unchecked")
        Timeline<T> timeline = (Timeline<T>) timelines.computeIfAbsent(property, Timeline::new);
        
        // Add the keyframe
        timeline.addKeyframe(time, value, easing);
        return this;
    }

    /**
     * Convenience: Sets the starting value (Time 0.0s) for a property.
     * Use this if you want to force the animation to start from a specific value,
     * overriding the automatic "current value" snapshot.
     *
     * @param property The property key.
     * @param value    The explicit start value.
     * @param <T>      The value type.
     * @return This builder instance.
     */
    public <T> AnimationBuilder setStart(StyleKey<T> property, T value) {
        return keyframe(0.0f, property, value, Easing.LINEAR);
    }

    /**
     * Convenience: Adds a keyframe with Linear easing.
     *
     * @param time     Time in seconds.
     * @param property The property key.
     * @param value    The target value.
     * @param <T>      The value type.
     * @return This builder instance.
     */
    public <T> AnimationBuilder keyframe(float time, StyleKey<T> property, T value) {
        return keyframe(time, property, value, Easing.LINEAR);
    }

    /**
     * Schedules a one-time action to run when the animation passes a specific timestamp.
     *
     * @param time   The time in seconds.
     * @param action The code to execute.
     * @return This builder instance.
     */
    public AnimationBuilder atTime(float time, Runnable action) {
        callbacks.add(new AnimationCallback(time, action));
        return this;
    }

    /**
     * Sets whether the entire animation sequence should loop indefinitely.
     *
     * @param loop True to loop.
     * @return This builder instance.
     */
    public AnimationBuilder loop(boolean loop) {
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
    public AnimationBuilder onComplete(Runnable action) {
        this.onComplete = action;
        return this;
    }

    /**
     * Compiles the timelines and starts the animation on the widget.
     * <p>
     * <b>Auto-Snapshot Logic:</b>
     * Checks each property being animated. If no keyframe exists at 0.0s,
     * it fetches the widget's current style value and inserts it as the start frame.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public void start() {
        // Pre-process timelines to ensure they have a start value
        for (Map.Entry<StyleKey<?>, Timeline<?>> entry : timelines.entrySet()) {
            StyleKey key = entry.getKey();
            Timeline timeline = entry.getValue();

            // Check if there is a keyframe at 0.0f
            boolean hasStart = timeline.hasKeyframeAt(0.0f);

            if (!hasStart) {
                // Snapshot the CURRENT value from the widget.
                // This gets the value as it looks right now (even if mid-animation).
                Object currentValue = widget.style().getValue(InteractionState.DEFAULT, (StyleKey<Object>) key);
                
                // Add it as the 0.0s keyframe
                // We cast to raw Timeline to bypass generic capture issues since we know the types match via the Map
                ((Timeline<Object>) timeline).addKeyframe(0.0f, currentValue, Easing.LINEAR);
            }
        }

        // Create the runtime instance
        AnimationInstance instance = new AnimationInstance(
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
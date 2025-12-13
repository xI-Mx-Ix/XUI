/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.anim;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.UIProperty;
import net.xmx.xui.core.style.UIState;

import java.util.List;
import java.util.Map;

/**
 * Represents an active, running animation sequence on a specific widget.
 * Created by {@link UIAnimationBuilder} and updated every frame by the Widget's {@link AnimationManager}.
 * <p>
 * It handles the progression of time, updates property values from multiple {@link Timeline}s,
 * triggers time-based callbacks, and manages looping/completion logic.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIAnimationInstance {

    private final UIWidget widget;
    private final Map<UIProperty<?>, Timeline<?>> timelines;
    private final List<UIAnimationBuilder.AnimationCallback> callbacks;
    private final boolean loop;
    private final Runnable onComplete;

    private float elapsedTime = 0.0f;
    private float maxDuration = 0.0f;

    /**
     * Constructs a new animation instance.
     *
     * @param widget     The target widget.
     * @param timelines  The map of property timelines.
     * @param callbacks  The list of time-based events.
     * @param loop       Whether to loop the animation.
     * @param onComplete Callback for completion.
     */
    public UIAnimationInstance(UIWidget widget, 
                               Map<UIProperty<?>, Timeline<?>> timelines, 
                               List<UIAnimationBuilder.AnimationCallback> callbacks,
                               boolean loop, Runnable onComplete) {
        this.widget = widget;
        this.timelines = timelines;
        this.callbacks = callbacks;
        this.loop = loop;
        this.onComplete = onComplete;

        // Calculate the total duration of the animation.
        // It is defined by the latest keyframe across all timelines.
        for (Timeline<?> t : timelines.values()) {
            float tEnd = t.getLastKeyframeTime();
            if (tEnd > maxDuration) {
                maxDuration = tEnd;
            }
        }
        
        // Ensure non-zero duration to prevent instant finish logic issues if empty
        if (maxDuration <= 0.001f) maxDuration = 0.001f;
    }

    /**
     * Updates the animation state.
     *
     * @param dt Time elapsed since the last frame in seconds.
     * @return {@code true} if the animation has finished and should be removed from the manager; {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean update(float dt) {
        elapsedTime += dt;

        // 1. Process Timelines
        // Iterate over each property being animated and update the widget's style
        for (Map.Entry<UIProperty<?>, Timeline<?>> entry : timelines.entrySet()) {
            UIProperty property = entry.getKey();
            Timeline timeline = entry.getValue();
            
            // Get the interpolated value for the current time
            Object value = timeline.getValueAt(elapsedTime);
            
            // Apply value to the widget's DEFAULT state style.
            // Using raw set with casting because we know the type matches from builder construction.
            ((UIProperty<Object>)property).getName(); // No-op, just ensuring type safety check logic if needed
            widget.style().set(UIState.DEFAULT, (UIProperty<Object>) property, value);
        }

        // 2. Process Time-based Callbacks
        for (UIAnimationBuilder.AnimationCallback cb : callbacks) {
            // Check if we crossed the callback's time threshold in this specific frame
            // Logic: (previousTime < threshold) AND (currentTime >= threshold)
            float prevTime = elapsedTime - dt;
            if (prevTime < cb.time() && elapsedTime >= cb.time()) {
                cb.action().run();
            }
        }

        // 3. Check for Completion or Looping
        if (elapsedTime >= maxDuration) {
            if (loop) {
                // Reset time to loop. We subtract maxDuration to maintain precise timing cadence
                // instead of setting to 0, which prevents drifting over many loops.
                elapsedTime %= maxDuration;
            } else {
                // Animation Finished
                // Ensure final state is set exactly to the end values
                for (Map.Entry<UIProperty<?>, Timeline<?>> entry : timelines.entrySet()) {
                    Timeline timeline = entry.getValue();
                    Object finalValue = timeline.getValueAt(maxDuration);
                    widget.style().set(UIState.DEFAULT, (UIProperty<Object>) entry.getKey(), finalValue);
                }
                
                if (onComplete != null) {
                    onComplete.run();
                }
                return true; // Signal removal
            }
        }

        return false; // Continue running
    }
}
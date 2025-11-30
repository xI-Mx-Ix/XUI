/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.style;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the styling rules for a widget across different states.
 * Acts as a local CSS definition for a component.
 *
 * @author xI-Mx-Ix
 */
public class StyleSheet {

    private final Map<UIState, Map<UIProperty<?>, Object>> stateMap = new HashMap<>();
    private float transitionSpeed = 0.2f;

    public StyleSheet() {
        // Initialize maps for all states
        for (UIState state : UIState.values()) {
            stateMap.put(state, new HashMap<>());
        }
    }

    /**
     * Sets a property value for a specific state.
     *
     * @param state    The state (e.g., HOVER).
     * @param property The property key.
     * @param value    The value to set.
     * @return The stylesheet instance for chaining.
     */
    public <T> StyleSheet set(UIState state, UIProperty<T> property, T value) {
        stateMap.get(state).put(property, value);
        return this;
    }

    /**
     * Sets a property value for the DEFAULT state.
     *
     * @param property The property key.
     * @param value    The value to set.
     * @return The stylesheet instance for chaining.
     */
    public <T> StyleSheet set(UIProperty<T> property, T value) {
        return set(UIState.DEFAULT, property, value);
    }

    /**
     * Retrieves the value for a property based on the current state.
     * Falls back to DEFAULT if the specific state is not defined.
     *
     * @param currentState The current state of the widget.
     * @param property     The property to retrieve.
     * @return The determined value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(UIState currentState, UIProperty<T> property) {
        Map<UIProperty<?>, Object> props = stateMap.get(currentState);

        // 1. Check specific state
        if (props.containsKey(property)) {
            return (T) props.get(property);
        }

        // 2. Fallback to DEFAULT (if not currently default)
        if (currentState != UIState.DEFAULT) {
            Map<UIProperty<?>, Object> defaults = stateMap.get(UIState.DEFAULT);
            if (defaults.containsKey(property)) {
                return (T) defaults.get(property);
            }
        }

        // 3. Return hardcoded default
        return property.getDefault();
    }

    public float getTransitionSpeed() {
        return transitionSpeed;
    }

    public StyleSheet setTransitionSpeed(float speed) {
        this.transitionSpeed = speed;
        return this;
    }
}
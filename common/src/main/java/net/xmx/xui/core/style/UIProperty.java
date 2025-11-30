/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.style;

/**
 * Represents a type-safe key for a style property.
 *
 * @param <T> The type of the value (e.g., Integer for color, Float for dimensions).
 *
 * @author xI-Mx-Ix
 */
public class UIProperty<T> {
    private final String name;
    private final T defaultValue;

    public UIProperty(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getDefault() {
        return defaultValue;
    }
}
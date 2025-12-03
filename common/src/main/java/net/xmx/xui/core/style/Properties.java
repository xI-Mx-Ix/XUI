/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.style;

/**
 * Static registry of common UI properties used by standard components.
 * These properties are shared across multiple widget types to ensure a consistent look and feel (e.g., Theme).
 * Widget-specific properties (like Scrollbar width or Tooltip delay) should be defined within their respective widget classes.
 *
 * @author xI-Mx-Ix
 */
public class Properties {
    // --- Global Colors ---
    public static final UIProperty<Integer> BACKGROUND_COLOR = new UIProperty<>("bg_color", 0xFF444444);
    public static final UIProperty<Integer> TEXT_COLOR = new UIProperty<>("text_color", 0xFFFFFFFF);
    public static final UIProperty<Integer> BORDER_COLOR = new UIProperty<>("border_color", 0xFF000000);

    // --- Interaction Colors ---
    public static final UIProperty<Integer> HOVER_COLOR = new UIProperty<>("hover_color", 0x80FFFFFF);

    // --- Global Dimensions & Effects ---
    public static final UIProperty<Float> BORDER_RADIUS = new UIProperty<>("border_radius", 0f);
    public static final UIProperty<Float> BORDER_THICKNESS = new UIProperty<>("border_thickness", 0f);
    public static final UIProperty<Float> SCALE = new UIProperty<>("scale", 1.0f);
    public static final UIProperty<Float> OPACITY = new UIProperty<>("opacity", 1.0f);
}
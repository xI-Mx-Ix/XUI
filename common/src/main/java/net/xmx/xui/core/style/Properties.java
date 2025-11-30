/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.style;

/**
 * Static registry of common UI properties used by standard components.
 *
 * @author xI-Mx-Ix
 */
public class Properties {
    // Colors
    public static final UIProperty<Integer> BACKGROUND_COLOR = new UIProperty<>("bg_color", 0xFF444444);
    public static final UIProperty<Integer> TEXT_COLOR = new UIProperty<>("text_color", 0xFFFFFFFF);
    public static final UIProperty<Integer> BORDER_COLOR = new UIProperty<>("border_color", 0xFF000000);

    // Dimensions & Effects
    public static final UIProperty<Float> BORDER_RADIUS = new UIProperty<>("border_radius", 0f);
    public static final UIProperty<Float> BORDER_THICKNESS = new UIProperty<>("border_thickness", 0f);
    public static final UIProperty<Float> SCALE = new UIProperty<>("scale", 1.0f);
    public static final UIProperty<Float> OPACITY = new UIProperty<>("opacity", 1.0f);
}
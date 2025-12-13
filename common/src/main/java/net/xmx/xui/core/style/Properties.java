/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.style;

/**
 * Static registry of common UI properties used by standard components.
 * Updated to include Transform properties.
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

    // --- 3D Transformations ---
    // These properties control the Matrix transformations applied before rendering.

    /**
     * Rotation around the X-axis (Pitch) in degrees.
     */
    public static final UIProperty<Float> ROTATION_X = new UIProperty<>("rotation_x", 0.0f);

    /**
     * Rotation around the Y-axis (Yaw) in degrees.
     */
    public static final UIProperty<Float> ROTATION_Y = new UIProperty<>("rotation_y", 0.0f);

    /**
     * Rotation around the Z-axis (Roll) in degrees. This corresponds to standard 2D rotation.
     */
    public static final UIProperty<Float> ROTATION_Z = new UIProperty<>("rotation_z", 0.0f);

    /**
     * Scale factor on the X-axis. 1.0 is default size.
     */
    public static final UIProperty<Float> SCALE_X = new UIProperty<>("scale_x", 1.0f);

    /**
     * Scale factor on the Y-axis. 1.0 is default size.
     */
    public static final UIProperty<Float> SCALE_Y = new UIProperty<>("scale_y", 1.0f);

    /**
     * Scale factor on the Z-axis (Depth). 1.0 is default.
     * Affects 3D perspective if perspective projection is enabled (rare in 2D UI but supported).
     */
    public static final UIProperty<Float> SCALE_Z = new UIProperty<>("scale_z", 1.0f);

    /**
     * Translation offset on the X-axis in pixels.
     */
    public static final UIProperty<Float> TRANSLATE_X = new UIProperty<>("translate_x", 0.0f);

    /**
     * Translation offset on the Y-axis in pixels.
     */
    public static final UIProperty<Float> TRANSLATE_Y = new UIProperty<>("translate_y", 0.0f);

    /**
     * Translation offset on the Z-axis (Depth/Layering) in pixels.
     * Positive values move towards the camera (on top), negative away.
     */
    public static final UIProperty<Float> TRANSLATE_Z = new UIProperty<>("translate_z", 0.0f);
}
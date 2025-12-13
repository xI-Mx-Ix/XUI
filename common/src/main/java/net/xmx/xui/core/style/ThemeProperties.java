/*
 * This file is part of XUI.
 * Licensed under the MIT License.
 */
package net.xmx.xui.core.style;

/**
 * Static registry of theme-related {@link StyleKey}s used by standard UI components.
 * <p>
 * This class defines global colors, dimensions, visual effects, and transformation
 * properties that can be applied consistently across the UI.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public final class ThemeProperties {

    // -------------------------------------------------------------------------
    // Global Colors
    // -------------------------------------------------------------------------

    /**
     * Background color of a component.
     */
    public static final StyleKey<Integer> BACKGROUND_COLOR =
            new StyleKey<>("bg_color", 0xFF444444);

    /**
     * Primary text color.
     */
    public static final StyleKey<Integer> TEXT_COLOR =
            new StyleKey<>("text_color", 0xFFFFFFFF);

    /**
     * Border color of a component.
     */
    public static final StyleKey<Integer> BORDER_COLOR =
            new StyleKey<>("border_color", 0xFF000000);

    // -------------------------------------------------------------------------
    // Interaction Colors
    // -------------------------------------------------------------------------

    /**
     * Overlay color applied when a component is hovered.
     */
    public static final StyleKey<Integer> HOVER_COLOR =
            new StyleKey<>("hover_color", 0x80FFFFFF);

    // -------------------------------------------------------------------------
    // Global Dimensions & Effects
    // -------------------------------------------------------------------------

    /**
     * Radius of rounded corners in pixels.
     */
    public static final StyleKey<Float> BORDER_RADIUS =
            new StyleKey<>("border_radius", 0f);

    /**
     * Thickness of the component border in pixels.
     */
    public static final StyleKey<Float> BORDER_THICKNESS =
            new StyleKey<>("border_thickness", 0f);

    /**
     * Uniform scale factor applied to the component.
     * A value of {@code 1.0} represents the default size.
     */
    public static final StyleKey<Float> SCALE =
            new StyleKey<>("scale", 1.0f);

    /**
     * Opacity of the component.
     * A value of {@code 1.0} is fully opaque, {@code 0.0} is fully transparent.
     */
    public static final StyleKey<Float> OPACITY =
            new StyleKey<>("opacity", 1.0f);

    // -------------------------------------------------------------------------
    // 3D Transformations
    // -------------------------------------------------------------------------

    /**
     * Rotation around the X-axis (pitch) in degrees.
     */
    public static final StyleKey<Float> ROTATION_X =
            new StyleKey<>("rotation_x", 0.0f);

    /**
     * Rotation around the Y-axis (yaw) in degrees.
     */
    public static final StyleKey<Float> ROTATION_Y =
            new StyleKey<>("rotation_y", 0.0f);

    /**
     * Rotation around the Z-axis (roll) in degrees.
     * This corresponds to the standard 2D rotation.
     */
    public static final StyleKey<Float> ROTATION_Z =
            new StyleKey<>("rotation_z", 0.0f);

    /**
     * Scale factor along the X-axis.
     * A value of {@code 1.0} represents the default size.
     */
    public static final StyleKey<Float> SCALE_X =
            new StyleKey<>("scale_x", 1.0f);

    /**
     * Scale factor along the Y-axis.
     * A value of {@code 1.0} represents the default size.
     */
    public static final StyleKey<Float> SCALE_Y =
            new StyleKey<>("scale_y", 1.0f);

    /**
     * Scale factor along the Z-axis (depth).
     * <p>
     * Primarily affects 3D perspective when enabled.
     * </p>
     */
    public static final StyleKey<Float> SCALE_Z =
            new StyleKey<>("scale_z", 1.0f);

    /**
     * Translation offset along the X-axis in pixels.
     */
    public static final StyleKey<Float> TRANSLATE_X =
            new StyleKey<>("translate_x", 0.0f);

    /**
     * Translation offset along the Y-axis in pixels.
     */
    public static final StyleKey<Float> TRANSLATE_Y =
            new StyleKey<>("translate_y", 0.0f);

    /**
     * Translation offset along the Z-axis (depth/layering) in pixels.
     * <p>
     * Positive values move the component closer to the camera,
     * negative values move it further away.
     * </p>
     */
    public static final StyleKey<Float> TRANSLATE_Z =
            new StyleKey<>("translate_z", 0.0f);

    private ThemeProperties() {
        /* utility class */
    }
}
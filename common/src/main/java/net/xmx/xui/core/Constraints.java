/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

/**
 * Factory class containing standard layout constraints.
 * Defines how a widget is positioned or sized relative to its parent.
 *
 * @author xI-Mx-Ix
 */
public final class Constraints {

    private Constraints() {
        // Prevent instantiation
    }

    // =================================================================================
    // Basic Alignment
    // =================================================================================

    /**
     * Aligns the widget to the start (Left/Top) of the parent.
     */
    public static UIConstraint alignStart() {
        return (parentPos, parentSize, selfSize) -> parentPos;
    }

    /**
     * Aligns the widget to the end (Right/Bottom) of the parent.
     */
    public static UIConstraint alignEnd() {
        return (parentPos, parentSize, selfSize) -> parentPos + parentSize - selfSize;
    }

    /**
     * Centers the widget within the parent.
     */
    public static UIConstraint center() {
        return centerOffset(0f);
    }

    /**
     * Centers the widget but adds a pixel offset.
     * @param offset Pixel shift (positive = right/down, negative = left/up).
     */
    public static UIConstraint centerOffset(float offset) {
        return (parentPos, parentSize, selfSize) ->
                parentPos + (parentSize - selfSize) / 2.0f + offset;
    }

    // =================================================================================
    // Fixed & Pixel Based (Padding/Anchors)
    // =================================================================================

    /**
     * Sets a fixed pixel size or offset.
     */
    public static UIConstraint pixel(float value) {
        return (parentPos, parentSize, selfSize) -> parentPos + value;
    }

    /**
     * Adds padding from the start edge (Alias for pixel).
     */
    public static UIConstraint paddingStart(float padding) {
        return pixel(padding);
    }

    /**
     * Adds padding from the end edge.
     * Places the widget relative to the end of the parent minus the widget size.
     */
    public static UIConstraint paddingEnd(float padding) {
        return (parentPos, parentSize, selfSize) -> parentPos + parentSize - selfSize - padding;
    }

    /**
     * LEGACY SUPPORT: Alias for paddingEnd to fix compilation errors in existing screens.
     */
    public static UIConstraint anchorEnd(float offset) {
        return paddingEnd(offset);
    }

    // =================================================================================
    // Relative (Percentage)
    // =================================================================================

    /**
     * Calculates size or position as a percentage of the parent.
     */
    public static UIConstraint relative(float percent) {
        return (parentPos, parentSize, selfSize) -> parentPos + (parentSize * percent);
    }

    /**
     * Like relative(), but clamps the percentage between 0.0 and 1.0.
     */
    public static UIConstraint relativeClamped(float percent) {
        return (parentPos, parentSize, selfSize) -> {
            float p = Math.max(0.0f, Math.min(1.0f, percent));
            return parentPos + (parentSize * p);
        };
    }

    /**
     * Fills the remaining space.
     */
    public static UIConstraint fill() {
        return (parentPos, parentSize, selfSize) -> parentSize;
    }

    /**
     * Calculates position based on weighted distribution.
     */
    public static UIConstraint weighted(float weight, float totalWeight) {
        return (parentPos, parentSize, selfSize) -> {
            if (totalWeight == 0) return parentPos;
            return parentPos + (parentSize * (weight / totalWeight));
        };
    }

    // =================================================================================
    // Sibling & Logic
    // =================================================================================

    /**
     * Positions this widget relative to a sibling widget.
     */
    public static UIConstraint sibling(UIWidget sibling, float offset, boolean vertical) {
        return (parentPos, parentSize, selfSize) -> {
            if (vertical) {
                return sibling.getY() + sibling.getHeight() + offset;
            } else {
                return sibling.getX() + sibling.getWidth() + offset;
            }
        };
    }

    /**
     * Positions relative to a sibling + offset by percentage of parent's size.
     */
    public static UIConstraint afterPercent(UIWidget sibling, float percent, boolean vertical) {
        return (parentPos, parentSize, selfSize) -> {
            float basePos = vertical
                    ? sibling.getY() + sibling.getHeight()
                    : sibling.getX() + sibling.getWidth();
            return basePos + (parentSize * percent);
        };
    }

    /**
     * Aligns the baseline (Y-axis) of this widget to another widget + offset.
     */
    public static UIConstraint baseline(UIWidget sibling, float offset) {
        return (parentPos, parentSize, selfSize) -> sibling.getY() + offset;
    }

    /**
     * Clamps the result of another constraint between min and max PIXELS relative to parent start.
     */
    public static UIConstraint clamp(UIConstraint base, float min, float max) {
        return (parentPos, parentSize, selfSize) -> {
            float calculated = base.calculate(parentPos, parentSize, selfSize);
            float absMin = parentPos + min;
            float absMax = parentPos + max;
            return Math.max(absMin, Math.min(absMax, calculated));
        };
    }

    /**
     * Mirrors the constraint across the parent axis.
     */
    public static UIConstraint mirror(UIConstraint base) {
        return (parentPos, parentSize, selfSize) -> {
            float originalPos = base.calculate(parentPos, parentSize, selfSize);
            float distFromStart = originalPos - parentPos;
            return parentPos + parentSize - distFromStart - selfSize;
        };
    }

    /**
     * Intended to be used with size constraints. Acts like alignStart.
     */
    public static UIConstraint stretch() {
        return (parentPos, parentSize, selfSize) -> parentPos;
    }
}
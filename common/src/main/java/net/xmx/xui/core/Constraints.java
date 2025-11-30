package net.xmx.xui.core;

/**
 * Factory class containing standard layout constraints.
 */
public final class Constraints {

    private Constraints() {
        // Prevent instantiation
    }

    /**
     * Creates a constraint representing a fixed pixel offset relative to the parent's start.
     *
     * @param pixels The offset in pixels.
     * @return The constraint.
     */
    public static UIConstraint pixel(float pixels) {
        return (parentPos, parentSize, selfSize) -> parentPos + pixels;
    }

    /**
     * Creates a constraint that centers the widget within the parent.
     *
     * @return The constraint.
     */
    public static UIConstraint center() {
        return (parentPos, parentSize, selfSize) -> parentPos + (parentSize / 2.0f) - (selfSize / 2.0f);
    }

    /**
     * Creates a constraint that anchors the widget to the end of the parent (Right or Bottom).
     *
     * @param offset The offset from the end edge (margin).
     * @return The constraint.
     */
    public static UIConstraint anchorEnd(float offset) {
        return (parentPos, parentSize, selfSize) -> parentPos + parentSize - selfSize - offset;
    }

    /**
     * Creates a constraint based on a percentage of the parent's size.
     *
     * @param percent The percentage (0.0 to 1.0).
     * @return The constraint.
     */
    public static UIConstraint relative(float percent) {
        return (parentPos, parentSize, selfSize) -> parentPos + (parentSize * percent);
    }

    /**
     * Creates a constraint relative to a sibling widget.
     *
     * @param sibling  The sibling widget to anchor to.
     * @param offset   The pixel margin from the sibling.
     * @param vertical True to position below the sibling (Y-axis), False to position to the right (X-axis).
     * @return The constraint.
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
}
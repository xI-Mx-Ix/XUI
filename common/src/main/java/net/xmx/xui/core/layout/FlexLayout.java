/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.layout;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;

import java.util.List;

/**
 * A flow-based layout that arranges children sequentially along a primary axis.
 * <p>
 * If the content exceeds the parent's dimensions along the primary axis,
 * the layout wraps the content to the next line (or column). This mimics CSS Flexbox behavior
 * with {@code flex-wrap: wrap}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class FlexLayout implements LayoutManager {

    /**
     * Defines the flow direction of the layout.
     */
    public enum Direction {
        /**
         * Items flow from left to right, wrapping to the next row.
         */
        HORIZONTAL,

        /**
         * Items flow from top to bottom, wrapping to the next column.
         */
        VERTICAL
    }

    private Direction direction = Direction.HORIZONTAL;
    private float gap = 5.0f;
    private boolean resizeParent = true;

    /**
     * Sets the primary flow direction.
     *
     * @param direction HORIZONTAL or VERTICAL.
     * @return This instance for chaining.
     */
    public FlexLayout setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the gap between items.
     *
     * @param gap The spacing in pixels.
     * @return This instance for chaining.
     */
    public FlexLayout setGap(float gap) {
        this.gap = gap;
        return this;
    }

    /**
     * Configures whether the parent should resize to fit the content.
     *
     * @param resize True to auto-resize.
     * @return This instance for chaining.
     */
    public FlexLayout setResizeParent(boolean resize) {
        this.resizeParent = resize;
        return this;
    }

    @Override
    public void arrange(UIPanel panel) {
        List<UIWidget> children = panel.getChildren();
        if (children.isEmpty()) return;

        if (direction == Direction.HORIZONTAL) {
            layoutHorizontal(panel, children);
        } else {
            layoutVertical(panel, children);
        }
    }

    private void layoutHorizontal(UIPanel panel, List<UIWidget> children) {
        float parentWidth = panel.getWidth();
        float currentX = 0;
        float currentY = 0;
        float maxRowHeight = 0;

        for (UIWidget child : children) {
            // We use the child's currently requested width/height.
            // Note: If child uses relative%, this calculation might need a pre-pass,
            // but for FlexLayout we assume children have intrinsic or pixel sizes.
            // We force a calculation of the child's size constraints based on current parent size (0, W, 0).
            float childW = child.getWidthConstraint().calculate(0, parentWidth, 0);
            float childH = child.getHeightConstraint().calculate(0, panel.getHeight(), 0);

            // Check for wrapping condition
            if (currentX + childW > parentWidth && currentX > 0) {
                // Wrap to next line
                currentX = 0;
                currentY += maxRowHeight + gap;
                maxRowHeight = 0;
            }

            // Apply calculated position
            child.setX(Layout.pixel(currentX));
            child.setY(Layout.pixel(currentY));
            // We apply the resolved size back to ensure consistency
            child.setWidth(Layout.pixel(childW));
            child.setHeight(Layout.pixel(childH));

            // Advance cursor
            currentX += childW + gap;
            maxRowHeight = Math.max(maxRowHeight, childH);
        }

        // Adjust parent height to fit all rows
        if (resizeParent) {
            float totalHeight = currentY + maxRowHeight;
            if (Math.abs(panel.getHeight() - totalHeight) > 0.5f) {
                panel.setHeight(Layout.pixel(totalHeight));
            }
        }
    }

    private void layoutVertical(UIPanel panel, List<UIWidget> children) {
        float parentHeight = panel.getHeight();
        float currentX = 0;
        float currentY = 0;
        float maxColWidth = 0;

        for (UIWidget child : children) {
            float childW = child.getWidthConstraint().calculate(0, panel.getWidth(), 0);
            float childH = child.getHeightConstraint().calculate(0, parentHeight, 0);

            // Check for wrapping condition
            if (currentY + childH > parentHeight && currentY > 0) {
                // Wrap to next column
                currentY = 0;
                currentX += maxColWidth + gap;
                maxColWidth = 0;
            }

            child.setX(Layout.pixel(currentX));
            child.setY(Layout.pixel(currentY));
            child.setWidth(Layout.pixel(childW));
            child.setHeight(Layout.pixel(childH));

            currentY += childH + gap;
            maxColWidth = Math.max(maxColWidth, childW);
        }

        // Adjust parent width to fit all columns
        if (resizeParent) {
            float totalWidth = currentX + maxColWidth;
            if (Math.abs(panel.getWidth() - totalWidth) > 0.5f) {
                panel.setWidth(Layout.pixel(totalWidth));
            }
        }
    }
}
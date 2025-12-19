/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.scroll;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;

/**
 * A visual control element that allows the user to manipulate a {@link UIScrollComponent}.
 * <p>
 * This widget renders a track and a thumb (handle). The thumb's size and position
 * are automatically calculated based on the state of the target ScrollComponent.
 * </p>
 * <p>
 * <b>Usage:</b>
 * Instantiate this widget, pass the ScrollComponent and the desired orientation
 * in the constructor, and add it to your layout (usually alongside the ScrollComponent).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScrollBar extends UIWidget {

    // --- Styling Keys ---

    /**
     * The color of the draggable thumb.
     */
    public static final StyleKey<Integer> THUMB_COLOR = new StyleKey<>("sb_thumb_color", 0x80FFFFFF);

    /**
     *  The color of the thumb when hovered or dragged.
     */
    public static final StyleKey<Integer> THUMB_HOVER_COLOR = new StyleKey<>("sb_thumb_hover_color", 0xB0FFFFFF);

    /**
     * The color of the background track.
     */
    public static final StyleKey<Integer> TRACK_COLOR = new StyleKey<>("sb_track_color", 0x20000000);

    /**
     *  The radius (corner roundness) for both track and thumb.
     */
    public static final StyleKey<Float> ROUNDING = new StyleKey<>("sb_rounding", 4.0f);

    /**
     *  The padding between the thumb and the edges of the track.
     */
    public static final StyleKey<Float> PADDING = new StyleKey<>("sb_padding", 2.0f);

    // --- Core Logic ---

    private final UIScrollComponent target;
    private final ScrollOrientation orientation;

    // --- Interaction State ---

    /**
     * true if the user is currently holding down the mouse button on the thumb.
     */
    private boolean isDragging = false;

    /**
     * The mouse coordinate (X or Y) when the drag started.
     */
    private float dragStartMouse = 0.0f;

    /**
     * The scroll offset of the target when the drag started.
     */
    private float dragStartScroll = 0.0f;

    /**
     * Minimum size in pixels for the thumb to ensure it remains clickable.
     */
    private static final float MIN_THUMB_SIZE = 20.0f;

    /**
     * Constructs a new Scroll Bar.
     *
     * @param target      The {@link UIScrollComponent} this bar controls.
     * @param orientation The axis this bar controls (Vertical/Horizontal).
     */
    public UIScrollBar(UIScrollComponent target, ScrollOrientation orientation) {
        this.target = target;
        this.orientation = orientation;
    }

    /**
     * Checks if scrolling is currently possible on the target component for the configured axis.
     *
     * @return true if the content is larger than the viewport.
     */
    public boolean canScroll() {
        if (target == null) return false;

        if (orientation == ScrollOrientation.VERTICAL) {
            return target.getMaxScrollY() > 0;
        } else {
            return target.getMaxScrollX() > 0;
        }
    }

    // =================================================================================
    // Helper: Geometry & Hit Detection
    // =================================================================================

    /**
     * Calculates whether the specified mouse coordinates are strictly within the bounds
     * of the draggable thumb.
     * <p>
     * This replicates the geometry logic used in rendering to ensure the click detection
     * matches the visual representation.
     * </p>
     *
     * @param mouseX The absolute X coordinate of the mouse.
     * @param mouseY The absolute Y coordinate of the mouse.
     * @return true if the mouse is over the thumb, false otherwise.
     */
    private boolean isMouseOverThumb(double mouseX, double mouseY) {
        if (!isVisible || !canScroll()) return false;

        // Use the raw style value for logic calculations to avoid animation inconsistencies
        float padding = style().getValue(InteractionState.DEFAULT, PADDING);

        float thumbX = this.x + padding;
        float thumbY = this.y + padding;
        float thumbW, thumbH;

        if (orientation == ScrollOrientation.VERTICAL) {
            float trackInnerHeight = this.height - (padding * 2);
            float viewRatio = target.getHeight() / target.getContentHeight();
            thumbH = Math.max(MIN_THUMB_SIZE, trackInnerHeight * viewRatio);
            thumbW = this.width - (padding * 2);

            float maxTravel = trackInnerHeight - thumbH;
            float scrollProgress = target.getScrollY() / target.getMaxScrollY();

            if (Float.isNaN(scrollProgress) || Float.isInfinite(scrollProgress)) scrollProgress = 0;

            thumbY += maxTravel * scrollProgress;
        } else {
            float trackInnerWidth = this.width - (padding * 2);
            float viewRatio = target.getWidth() / target.getContentWidth();
            thumbW = Math.max(MIN_THUMB_SIZE, trackInnerWidth * viewRatio);
            thumbH = this.height - (padding * 2);

            float maxTravel = trackInnerWidth - thumbW;
            float scrollProgress = target.getScrollX() / target.getMaxScrollX();

            if (Float.isNaN(scrollProgress) || Float.isInfinite(scrollProgress)) scrollProgress = 0;

            thumbX += maxTravel * scrollProgress;
        }

        // Check AABB collision
        return mouseX >= thumbX && mouseX <= thumbX + thumbW &&
                mouseY >= thumbY && mouseY <= thumbY + thumbH;
    }

    // =================================================================================
    // Rendering
    // =================================================================================

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
        // 1. Draw the Background Track
        // We use the standard animation fetch for track color and rounding
        int trackColor = getColor(TRACK_COLOR, state, deltaTime);
        float rounding = getFloat(ROUNDING, state, deltaTime);

        renderer.getGeometry().renderRect(this.x, this.y, this.width, this.height, trackColor, rounding);

        if (!canScroll()) {
            return;
        }

        // 2. Determine Target Thumb Color
        // The hover state is active if the user is dragging OR if the mouse is specifically over the thumb.
        boolean isHoveringThumb = isDragging || isMouseOverThumb(mouseX, mouseY);

        int targetThumbColor;
        if (isHoveringThumb) {
            targetThumbColor = style().getValue(InteractionState.HOVER, THUMB_HOVER_COLOR);
        } else {
            targetThumbColor = style().getValue(InteractionState.DEFAULT, THUMB_COLOR);
        }

        // 3. Animate Thumb Color
        // We use THUMB_COLOR as the "Identity Key" for the animation manager to store the intermediate value.
        // The manager will interpolate from the last frame's color towards 'targetThumbColor'.
        int effectiveColor = animManager.getAnimatedColor(
                THUMB_COLOR,
                targetThumbColor,
                style().getTransitionSpeed(),
                deltaTime
        );

        float padding = getFloat(PADDING, state, deltaTime);

        // 4. Calculate Thumb Geometry (Visual)
        // This math corresponds to isMouseOverThumb but uses the animated padding
        float thumbX = this.x + padding;
        float thumbY = this.y + padding;
        float thumbW, thumbH;

        if (orientation == ScrollOrientation.VERTICAL) {
            float trackInnerHeight = this.height - (padding * 2);
            float viewRatio = target.getHeight() / target.getContentHeight();
            thumbH = Math.max(MIN_THUMB_SIZE, trackInnerHeight * viewRatio);
            thumbW = this.width - (padding * 2);

            float maxTravel = trackInnerHeight - thumbH;
            float scrollProgress = target.getScrollY() / target.getMaxScrollY();
            if (Float.isNaN(scrollProgress)) scrollProgress = 0;

            thumbY += maxTravel * scrollProgress;
        } else {
            float trackInnerWidth = this.width - (padding * 2);
            float viewRatio = target.getWidth() / target.getContentWidth();
            thumbW = Math.max(MIN_THUMB_SIZE, trackInnerWidth * viewRatio);
            thumbH = this.height - (padding * 2);

            float maxTravel = trackInnerWidth - thumbW;
            float scrollProgress = target.getScrollX() / target.getMaxScrollX();
            if (Float.isNaN(scrollProgress)) scrollProgress = 0;

            thumbX += maxTravel * scrollProgress;
        }

        // 5. Draw the Thumb with the animated color
        renderer.getGeometry().renderRect(thumbX, thumbY, thumbW, thumbH, effectiveColor, rounding);
    }

    // =================================================================================
    // Interaction
    // =================================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Standard checks: Visible, Scrollable, Left Click
        if (!isVisible || !canScroll() || button != 0) {
            return false;
        }

        // Verify that the mouse click is strictly on the thumb (the handle)
        // and not just on the empty track.
        if (!isMouseOverThumb(mouseX, mouseY)) {
            return false;
        }

        // Initiate Dragging
        isDragging = true;

        if (orientation == ScrollOrientation.VERTICAL) {
            dragStartMouse = (float) mouseY;
            dragStartScroll = target.getScrollY();
        } else {
            dragStartMouse = (float) mouseX;
            dragStartScroll = target.getScrollX();
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Only handle drag if we initiated it and content is scrollable
        if (!isVisible || !isDragging || !canScroll()) return false;

        float padding = style().getValue(InteractionState.DEFAULT, PADDING);

        if (orientation == ScrollOrientation.VERTICAL) {
            // --- Vertical Drag Calculation ---
            float trackHeight = this.height - (padding * 2);

            // Recalculate thumb height to determine available travel pixels
            float viewRatio = target.getHeight() / target.getContentHeight();
            float thumbHeight = Math.max(MIN_THUMB_SIZE, trackHeight * viewRatio);
            float maxTravel = trackHeight - thumbHeight;

            if (maxTravel > 0) {
                // How far has the mouse moved since click?
                float deltaMouse = (float) mouseY - dragStartMouse;

                // Convert mouse movement percentage to scroll percentage
                float ratio = deltaMouse / maxTravel;

                // Apply to start scroll position
                float newScroll = dragStartScroll + (ratio * target.getMaxScrollY());
                target.setScrollY(newScroll);
            }

        } else {
            // --- Horizontal Drag Calculation ---
            float trackWidth = this.width - (padding * 2);

            // Recalculate thumb width
            float viewRatio = target.getWidth() / target.getContentWidth();
            float thumbWidth = Math.max(MIN_THUMB_SIZE, trackWidth * viewRatio);
            float maxTravel = trackWidth - thumbWidth;

            if (maxTravel > 0) {
                // How far has the mouse moved since click?
                float deltaMouse = (float) mouseX - dragStartMouse;

                // Convert mouse movement percentage to scroll percentage
                float ratio = deltaMouse / maxTravel;

                // Apply to start scroll position
                float newScroll = dragStartScroll + (ratio * target.getMaxScrollX());
                target.setScrollX(newScroll);
            }
        }

        return true;
    }

    /**
     * Ignores scroll wheel events on the scrollbar itself.
     * Usually, users expect the content to scroll even if hovering the bar,
     * so we return false to let the event propagate up (or to the parent).
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }
}
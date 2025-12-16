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
    // Rendering
    // =================================================================================

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
        // Fetch Styles
        int trackColor = style().getValue(state, TRACK_COLOR);
        float rounding = style().getValue(state, ROUNDING);
        
        // 1. Draw the Background Track
        renderer.getGeometry().renderRect(this.x, this.y, this.width, this.height, trackColor, rounding);

        // If content fits perfectly, we don't draw the thumb, but we still drew the track for layout consistency.
        // Alternatively, you could return early here if you want it to be invisible.
        if (!canScroll()) {
            return;
        }

        // Fetch remaining styles
        int thumbColor = style().getValue(state, THUMB_COLOR);
        int thumbHoverColor = style().getValue(state, THUMB_HOVER_COLOR);
        float padding = style().getValue(state, PADDING);

        // 2. Calculate Thumb Geometry
        float thumbX = this.x + padding;
        float thumbY = this.y + padding;
        float thumbW, thumbH;

        if (orientation == ScrollOrientation.VERTICAL) {
            // --- Vertical Math ---
            float trackInnerHeight = this.height - (padding * 2);
            
            // Ratio of Viewport Height to Content Height
            float viewRatio = target.getHeight() / target.getContentHeight();
            
            // Calculate Thumb Height (proportional, but clamped to min size)
            thumbH = Math.max(MIN_THUMB_SIZE, trackInnerHeight * viewRatio);
            thumbW = this.width - (padding * 2);

            // Calculate Thumb Position based on Scroll Progress
            float maxTravel = trackInnerHeight - thumbH;
            float scrollProgress = target.getScrollY() / target.getMaxScrollY();
            
            // Safety check for NaN
            if (Float.isNaN(scrollProgress)) scrollProgress = 0;
            
            thumbY += maxTravel * scrollProgress;

        } else {
            // --- Horizontal Math ---
            float trackInnerWidth = this.width - (padding * 2);

            // Ratio of Viewport Width to Content Width
            float viewRatio = target.getWidth() / target.getContentWidth();

            // Calculate Thumb Width
            thumbW = Math.max(MIN_THUMB_SIZE, trackInnerWidth * viewRatio);
            thumbH = this.height - (padding * 2);

            // Calculate Thumb Position
            float maxTravel = trackInnerWidth - thumbW;
            float scrollProgress = target.getScrollX() / target.getMaxScrollX();

            // Safety check for NaN
            if (Float.isNaN(scrollProgress)) scrollProgress = 0;

            thumbX += maxTravel * scrollProgress;
        }

        // 3. Determine Color (Hover/Drag Highlight)
        int effectiveColor = (isDragging || isHovered) ? thumbHoverColor : thumbColor;

        // 4. Draw the Thumb
        renderer.getGeometry().renderRect(thumbX, thumbY, thumbW, thumbH, effectiveColor, rounding);
    }

    // =================================================================================
    // Interaction
    // =================================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Standard checks: Visible, Scrollable, Inside Bounds, Left Click
        if (!isVisible || !canScroll() || !isMouseOver(mouseX, mouseY) || button != 0) {
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

        // Note: We could implement "Jump to position" logic here if the click 
        // lands on the track but not on the thumb.
        
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
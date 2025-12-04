/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.effect.UIEffect;
import net.xmx.xui.core.effect.UIScissorsEffect;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIProperty;
import net.xmx.xui.core.style.UIState;

import java.util.HashMap;
import java.util.Map;

/**
 * A scrollable panel container with a permanent scrollbar.
 * <p>
 * Features:
 * <ul>
 *   <li>Vertical scrolling via mouse wheel and draggable scrollbar.</li>
 *   <li>Persistent scrollbar that appears when content overflows.</li>
 *   <li>Smooth scroll interpolation.</li>
 *   <li>Automatic scissor clipping to prevent content from rendering outside the panel.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScrollPanel extends UIWidget {

    // --- Scrollbar Styling Properties ---

    /** The width of the scrollbar in pixels. */
    public static final UIProperty<Float> SCROLLBAR_WIDTH = new UIProperty<>("scrollbar_width", 8.0f);

    /** The padding between the scrollbar and the right edge of the panel. */
    public static final UIProperty<Float> SCROLLBAR_PADDING = new UIProperty<>("scrollbar_padding", 2.0f);

    /** The border radius (roundness) of the scrollbar thumb and track. */
    public static final UIProperty<Float> SCROLLBAR_RADIUS = new UIProperty<>("scrollbar_radius", 4.0f);

    /** The color of the draggable scrollbar thumb. */
    public static final UIProperty<Integer> SCROLLBAR_COLOR = new UIProperty<>("scrollbar_color", 0x80FFFFFF);

    /** The color of the static scrollbar background track. */
    public static final UIProperty<Integer> SCROLLBAR_TRACK_COLOR = new UIProperty<>("scrollbar_track_color", 0x20FFFFFF);

    // --- State Variables ---

    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;
    private float contentHeight = 0.0f;

    // Visibility and Animation
    private float scrollbarOpacity = 0.0f;
    private float targetScrollbarOpacity = 1.0f;
    private static final float SCROLLBAR_FADE_SPEED = 5.0f;

    // Drag Interaction
    private boolean isDraggingScrollbar = false;
    private float dragStartY = 0.0f;
    private float dragStartScroll = 0.0f;

    private boolean isScrollbarHovered = false;

    /**
     * Cache for the original Y positions of children.
     * Required because we modify the children's Y coordinate dynamically during render
     * to simulate scrolling, and we need a stable reference point.
     */
    private final Map<UIWidget, Float> childBaseYPositions = new HashMap<>();

    /**
     * Constructs a new Scroll Panel.
     * Automatically applies a {@link UIScissorsEffect} to clip overflowing content.
     */
    public UIScrollPanel() {
        this.addEffect(new UIScissorsEffect());
        setupDefaultStyles();
    }

    /**
     * Configures the initial default values for style properties.
     */
    private void setupDefaultStyles() {
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0x00000000) // Transparent background
                .set(SCROLLBAR_WIDTH, 8.0f)
                .set(SCROLLBAR_PADDING, 2.0f)
                .set(SCROLLBAR_RADIUS, 4.0f)
                .set(SCROLLBAR_COLOR, 0x80FFFFFF)
                .set(SCROLLBAR_TRACK_COLOR, 0x20FFFFFF);
    }

    /**
     * Recalculates the layout dimensions.
     * <p>
     * This method captures the "base" Y positions of all children before any scrolling
     * offset is applied. This ensures the scroll math remains accurate even after multiple frames.
     * </p>
     */
    @Override
    public void layout() {
        super.layout();

        // Store base Y positions before scrolling logic applies
        for (UIWidget child : children) {
            childBaseYPositions.put(child, child.getY());
        }

        calculateContentHeight();
        clampScrollOffset();
    }

    /**
     * Calculates the total height of all children to determine the scrollable area.
     * The content height is the bottom-most coordinate of the lowest child relative to the panel.
     */
    private void calculateContentHeight() {
        contentHeight = 0;
        for (UIWidget child : children) {
            Float baseY = childBaseYPositions.get(child);
            if (baseY == null) baseY = child.getY();

            float childBottom = baseY - this.getY() + child.getHeight();
            if (childBottom > contentHeight) {
                contentHeight = childBottom;
            }
        }
    }

    /**
     * Checks if the content is taller than the panel height, requiring scrolling.
     *
     * @return true if scrolling is active.
     */
    private boolean isScrollable() {
        return contentHeight > this.getHeight();
    }

    /**
     * Calculates the maximum valid scroll offset (Content Height - Panel Height).
     *
     * @return The max scroll offset in pixels.
     */
    private float getMaxScroll() {
        return Math.max(0, contentHeight - this.getHeight());
    }

    /**
     * Ensures the scroll offset stays within the valid range [0, MaxScroll].
     */
    private void clampScrollOffset() {
        float maxScroll = getMaxScroll();
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    /**
     * Handles mouse wheel scrolling events.
     * <p>
     * If the mouse is hovering over the panel and the content is scrollable,
     * this updates the scroll target and consumes the event.
     * </p>
     *
     * @param mouseX      The absolute X coordinate.
     * @param mouseY      The absolute Y coordinate.
     * @param scrollDelta The scroll amount.
     * @return true if scrolling occurred.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isVisible || !isMouseOver(mouseX, mouseY)) return false;

        if (isScrollable()) {
            float scrollSpeed = 20.0f;
            targetScrollOffset -= (float) scrollDelta * scrollSpeed;
            clampScrollOffset();
            showScrollbar();
            return true;
        }
        return false;
    }

    /**
     * Handles mouse click events.
     * <p>
     * Detects clicks on the scrollbar track to initiate dragging.
     * </p>
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button index.
     * @return true if handled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // Check if clicking on scrollbar
        if (button == 0 && isScrollable() && isPointOnScrollbar(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            dragStartY = (float) mouseY;
            dragStartScroll = scrollOffset;
            showScrollbar();
            // We return true to capture focus, ensuring we receive drag events
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles mouse release events.
     * Stops any active scrollbar dragging.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Handles mouse drag events to move the scrollbar thumb.
     * <p>
     * Converts the vertical mouse delta into a scroll offset percentage.
     * </p>
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button held down.
     * @param dragX  The horizontal delta.
     * @param dragY  The vertical delta.
     * @return true if handled.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && isScrollable()) {
            // We use the absolute position difference from the start of the drag
            // instead of accumulating deltas to avoid drift.
            float deltaY = (float) mouseY - dragStartY;

            float scrollbarTrackHeight = getScrollbarTrackHeight();
            float scrollbarThumbHeight = getScrollbarThumbHeight();
            float maxScrollbarTravel = scrollbarTrackHeight - scrollbarThumbHeight;

            if (maxScrollbarTravel > 0) {
                float scrollRatio = deltaY / maxScrollbarTravel;
                targetScrollOffset = dragStartScroll + (scrollRatio * getMaxScroll());
                clampScrollOffset();
            }
            showScrollbar();
            return true;
        }

        // Pass drag event to children if we aren't dragging the scrollbar
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Triggers the scrollbar visibility and resets the fade-out timer.
     */
    private void showScrollbar() {
        targetScrollbarOpacity = 1.0f;
    }

    /**
     * Updates the hover state of the scrollbar specifically.
     */
    private void updateScrollbarHoverState(int mouseX, int mouseY) {
        isScrollbarHovered = isScrollable() && isPointOnScrollbar(mouseX, mouseY);
        if (isScrollbarHovered || isDraggingScrollbar) {
            showScrollbar();
        }
    }

    /**
     * Checks if a point is geometrically within the scrollbar's bounds.
     */
    private boolean isPointOnScrollbar(double mouseX, double mouseY) {
        if (!isScrollable()) return false;

        float scrollbarWidth = style().getValue(UIState.DEFAULT, SCROLLBAR_WIDTH);
        float scrollbarPadding = style().getValue(UIState.DEFAULT, SCROLLBAR_PADDING);

        float scrollbarX = this.getX() + this.getWidth() - scrollbarWidth - scrollbarPadding;

        // Check against the thumb + track area (entire vertical strip)
        float trackY = this.getY() + scrollbarPadding;
        float trackHeight = getScrollbarTrackHeight();

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= trackY && mouseY <= trackY + trackHeight;
    }

    /**
     * Returns the height of the scrollbar track (the full available space).
     */
    private float getScrollbarTrackHeight() {
        float scrollbarPadding = style().getValue(UIState.DEFAULT, SCROLLBAR_PADDING);
        return this.getHeight() - (scrollbarPadding * 2);
    }

    /**
     * Returns the height of the scrollbar thumb (proportional to visible content ratio).
     */
    private float getScrollbarThumbHeight() {
        if (!isScrollable()) return 0;
        float trackHeight = getScrollbarTrackHeight();
        float visibleRatio = this.getHeight() / contentHeight;
        return Math.max(20, trackHeight * visibleRatio); // Minimum 20px
    }

    /**
     * Returns the Y position of the scrollbar thumb relative to the panel.
     */
    private float getScrollbarThumbPosition() {
        if (!isScrollable()) return 0;

        float scrollbarPadding = style().getValue(UIState.DEFAULT, SCROLLBAR_PADDING);
        float trackHeight = getScrollbarTrackHeight();
        float thumbHeight = getScrollbarThumbHeight();
        float maxScrollbarTravel = trackHeight - thumbHeight;
        float scrollRatio = scrollOffset / getMaxScroll();

        return scrollbarPadding + (maxScrollbarTravel * scrollRatio);
    }

    @Override
    public void render(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible) return;

        updateHoverState(mouseX, mouseY);

        // Keep scrollbar visible if interacting
        if (isDraggingScrollbar || isScrollbarHovered) {
            targetScrollbarOpacity = 1.0f;
        }

        // Determine current style state for the panel background
        UIState state = UIState.DEFAULT;
        if (isFocused) {
            state = UIState.ACTIVE;
        } else if (isHovered) {
            state = UIState.HOVER;
        }

        // Apply visual effects manually (e.g., Scissors)
        // We override render to control child positioning, so we must handle effects here
        for (UIEffect effect : effects) {
            effect.apply(renderer, this);
        }

        // Draw the panel itself (background)
        drawSelf(renderer, mouseX, mouseY, partialTicks, state);

        // Apply scroll offset to children before rendering them
        applyScrollOffsetToChildren();

        // Render children
        for (UIWidget child : children) {
            child.render(renderer, mouseX, mouseY, partialTicks);
        }

        // Revert visual effects
        for (int i = effects.size() - 1; i >= 0; i--) {
            effects.get(i).revert(renderer, this);
        }
    }

    /**
     * Applies the current scroll offset to children by modifying their Y positions dynamically.
     * Does not permanently alter constraints, just the calculated Y for this frame.
     */
    private void applyScrollOffsetToChildren() {
        for (UIWidget child : children) {
            Float baseY = childBaseYPositions.get(child);
            if (baseY != null) {
                final float scrolledY = baseY - scrollOffset;
                // Temporarily override the Y calculation for the next frame
                child.setY((parentPos, parentSize, selfSize) -> scrolledY);
                child.layout();
            }
        }
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        // Update hover logic
        updateScrollbarHoverState(mouseX, mouseY);

        // Animate scrollbar opacity
        float opacityDiff = targetScrollbarOpacity - scrollbarOpacity;
        if (Math.abs(opacityDiff) > 0.001f) {
            float lerpFactor = 1.0f - (float) Math.exp(-SCROLLBAR_FADE_SPEED * partialTicks);
            scrollbarOpacity += opacityDiff * lerpFactor;
        } else {
            scrollbarOpacity = targetScrollbarOpacity;
        }

        // Animate scroll offset (Smooth scrolling)
        float scrollDiff = targetScrollOffset - scrollOffset;
        if (Math.abs(scrollDiff) > 0.1f) {
            float lerpFactor = 1.0f - (float) Math.exp(-15.0f * partialTicks);
            scrollOffset += scrollDiff * lerpFactor;
        } else {
            scrollOffset = targetScrollOffset;
        }

        // Draw Panel Background
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);
        if ((bgColor >>> 24) > 0) {
            float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
            renderer.drawRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), bgColor, radius);
        }

        // Draw Scrollbar (Track + Thumb)
        if (isScrollable() && scrollbarOpacity > 0.01f) {
            drawScrollbar(renderer, state, partialTicks);
        }
    }

    /**
     * Renders the scrollbar visuals.
     */
    private void drawScrollbar(UIRenderInterface renderer, UIState state, float partialTicks) {
        float scrollbarWidth = style().getValue(state, SCROLLBAR_WIDTH);
        float scrollbarPadding = style().getValue(state, SCROLLBAR_PADDING);
        float scrollbarRadius = style().getValue(state, SCROLLBAR_RADIUS);
        int scrollbarColor = style().getValue(state, SCROLLBAR_COLOR);
        int trackColor = style().getValue(state, SCROLLBAR_TRACK_COLOR);

        // Apply calculated opacity
        int alpha = (int) (scrollbarOpacity * 255);
        scrollbarColor = (scrollbarColor & 0x00FFFFFF) | (alpha << 24);
        trackColor = (trackColor & 0x00FFFFFF) | ((int) (alpha * 0.3f) << 24);

        float scrollbarX = this.getX() + this.getWidth() - scrollbarWidth - scrollbarPadding;
        float scrollbarY = this.getY() + scrollbarPadding;
        float trackHeight = getScrollbarTrackHeight();

        // Draw Track
        renderer.drawRect(scrollbarX, scrollbarY, scrollbarWidth, trackHeight, trackColor, scrollbarRadius);

        // Draw Thumb
        float thumbY = this.getY() + getScrollbarThumbPosition();
        float thumbHeight = getScrollbarThumbHeight();

        // Highlight Thumb on Hover/Drag
        int thumbColor = isScrollbarHovered || isDraggingScrollbar ?
                brightenColor(scrollbarColor, 1.3f) : scrollbarColor;

        renderer.drawRect(scrollbarX, thumbY, scrollbarWidth, thumbHeight, thumbColor, scrollbarRadius);
    }

    /**
     * Utility to brighten a color for hover effects.
     */
    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Helper: Sets the scroll offset directly.
     * Useful for programmatic scrolling (e.g., "Scroll to Top").
     */
    public UIScrollPanel setScrollOffset(float offset) {
        this.targetScrollOffset = offset;
        clampScrollOffset();
        showScrollbar();
        return this;
    }

    /**
     * Helper: Scrolls to ensure a specific child is visible.
     */
    public UIScrollPanel scrollToChild(UIWidget child) {
        Float baseY = childBaseYPositions.get(child);
        if (baseY == null) return this;

        float childY = baseY - this.getY();
        float childBottom = childY + child.getHeight();

        if (childY < scrollOffset) {
            // Child is above visible area
            setScrollOffset(childY);
        } else if (childBottom > scrollOffset + this.getHeight()) {
            // Child is below visible area
            setScrollOffset(childBottom - this.getHeight());
        }
        return this;
    }
}
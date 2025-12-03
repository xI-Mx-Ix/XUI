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
 * Features:
 * - Vertical scrolling with mouse wheel
 * - Persistent scrollbar (always visible when content overflows)
 * - Draggable scrollbar track
 * - Customizable scrollbar appearance
 * - Smooth scroll animations
 * - Scissor clipping for content overflow
 *
 * @author xI-Mx-Ix
 */
public class UIScrollPanel extends UIWidget {

    // Scrollbar configuration
    public static final UIProperty<Float> SCROLLBAR_WIDTH = new UIProperty<>("scrollbar_width", 8.0f);
    public static final UIProperty<Float> SCROLLBAR_PADDING = new UIProperty<>("scrollbar_padding", 2.0f);
    public static final UIProperty<Float> SCROLLBAR_RADIUS = new UIProperty<>("scrollbar_radius", 4.0f);
    public static final UIProperty<Integer> SCROLLBAR_COLOR = new UIProperty<>("scrollbar_color", 0x80FFFFFF);
    public static final UIProperty<Integer> SCROLLBAR_TRACK_COLOR = new UIProperty<>("scrollbar_track_color", 0x20FFFFFF);

    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;
    private float contentHeight = 0.0f;

    private float scrollbarOpacity = 0.0f;
    private float targetScrollbarOpacity = 1.0f; // Default to visible
    private long lastScrollTime = 0;
    private static final float SCROLLBAR_FADE_SPEED = 5.0f;

    private boolean isDraggingScrollbar = false;
    private float dragStartY = 0.0f;
    private float dragStartScroll = 0.0f;

    private boolean isScrollbarHovered = false;

    // Store original Y positions of children
    private final Map<UIWidget, Float> childBaseYPositions = new HashMap<>();

    /**
     * Constructs a scroll panel.
     * Initializes the default styles and adds the scissor effect to handle content clipping.
     */
    public UIScrollPanel() {
        // Automatically add the scissor effect to clip scrolling content
        this.addEffect(new UIScissorsEffect());
        setupDefaultStyles();
    }

    private void setupDefaultStyles() {
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0x00000000) // Transparent by default
                .set(SCROLLBAR_WIDTH, 8.0f)
                .set(SCROLLBAR_PADDING, 2.0f)
                .set(SCROLLBAR_RADIUS, 4.0f)
                .set(SCROLLBAR_COLOR, 0x80FFFFFF)
                .set(SCROLLBAR_TRACK_COLOR, 0x20FFFFFF);
    }

    @Override
    public void layout() {
        super.layout();

        // Store base Y positions before scrolling
        for (UIWidget child : children) {
            childBaseYPositions.put(child, child.getY());
        }

        calculateContentHeight();
        clampScrollOffset();
    }

    /**
     * Calculates the total height of all children to determine scrollable area.
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
     * Returns true if content can be scrolled (content exceeds visible area).
     */
    private boolean isScrollable() {
        return contentHeight > this.getHeight();
    }

    /**
     * Returns the maximum scroll offset.
     */
    private float getMaxScroll() {
        return Math.max(0, contentHeight - this.getHeight());
    }

    /**
     * Clamps the scroll offset to valid bounds.
     */
    private void clampScrollOffset() {
        float maxScroll = getMaxScroll();
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    /**
     * Handles mouse wheel scrolling.
     */
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // Check if clicking on scrollbar
        if (button == 0 && isScrollable() && isPointOnScrollbar(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            dragStartY = (float) mouseY;
            dragStartScroll = scrollOffset;
            showScrollbar();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Handles scrollbar dragging.
     */
    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && isScrollable()) {
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
        }
    }

    /**
     * Triggers the scrollbar visibility.
     */
    private void showScrollbar() {
        lastScrollTime = System.currentTimeMillis();
        targetScrollbarOpacity = 1.0f;
    }

    /**
     * Updates scrollbar hover state.
     */
    private void updateScrollbarHoverState(int mouseX, int mouseY) {
        isScrollbarHovered = isScrollable() && isPointOnScrollbar(mouseX, mouseY);
        if (isScrollbarHovered || isDraggingScrollbar) {
            showScrollbar();
        }
    }

    /**
     * Checks if a point is on the scrollbar.
     */
    private boolean isPointOnScrollbar(double mouseX, double mouseY) {
        if (!isScrollable()) return false;

        float scrollbarWidth = style().getValue(UIState.DEFAULT, SCROLLBAR_WIDTH);
        float scrollbarPadding = style().getValue(UIState.DEFAULT, SCROLLBAR_PADDING);

        float scrollbarX = this.getX() + this.getWidth() - scrollbarWidth - scrollbarPadding;
        float scrollbarY = this.getY() + getScrollbarThumbPosition();
        float scrollbarHeight = getScrollbarThumbHeight();

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
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
     * Returns the Y position of the scrollbar thumb.
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

        // Maintain full visibility
        targetScrollbarOpacity = 1.0f;

        // Determine current style state
        UIState state = UIState.DEFAULT;
        if (isFocused) {
            state = UIState.ACTIVE;
        } else if (isHovered) {
            state = UIState.HOVER;
        }

        // Apply visual effects manually
        // We do this here because we override the main render method to handle scroll offsets for children
        for (UIEffect effect : effects) {
            effect.apply(renderer, this);
        }

        // Update animations and draw the panel background
        drawSelf(renderer, mouseX, mouseY, partialTicks, state);

        // Apply scroll offset to children before rendering
        applyScrollOffsetToChildren();

        // Render children
        for (UIWidget child : children) {
            child.render(renderer, mouseX, mouseY, partialTicks);
        }

        // Revert visual effects (in reverse order)
        for (int i = effects.size() - 1; i >= 0; i--) {
            effects.get(i).revert(renderer, this);
        }
    }

    /**
     * Applies the current scroll offset to children by modifying their Y positions.
     */
    private void applyScrollOffsetToChildren() {
        for (UIWidget child : children) {
            Float baseY = childBaseYPositions.get(child);
            if (baseY != null) {
                // Calculate new Y position with scroll offset applied
                final float scrolledY = baseY - scrollOffset;
                child.setY((parentPos, parentSize, selfSize) -> scrolledY);
                child.layout(); // Recalculate only this child's layout
            }
        }
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        // Update hover state
        updateScrollbarHoverState(mouseX, mouseY);

        // Maintain full visibility
        targetScrollbarOpacity = 1.0f;

        // Animate scrollbar opacity (interpolates to target)
        float opacityDiff = targetScrollbarOpacity - scrollbarOpacity;
        if (Math.abs(opacityDiff) > 0.001f) {
            float lerpFactor = 1.0f - (float) Math.exp(-SCROLLBAR_FADE_SPEED * partialTicks);
            scrollbarOpacity += opacityDiff * lerpFactor;
        } else {
            scrollbarOpacity = targetScrollbarOpacity;
        }

        // Animate scroll offset
        float scrollDiff = targetScrollOffset - scrollOffset;
        if (Math.abs(scrollDiff) > 0.1f) {
            float lerpFactor = 1.0f - (float) Math.exp(-15.0f * partialTicks);
            scrollOffset += scrollDiff * lerpFactor;
        } else {
            scrollOffset = targetScrollOffset;
        }

        // Draw background
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);
        if ((bgColor >>> 24) > 0) {
            float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
            renderer.drawRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), bgColor, radius);
        }

        // Draw scrollbar if scrollable and visible
        if (isScrollable() && scrollbarOpacity > 0.01f) {
            drawScrollbar(renderer, state, partialTicks);
        }
    }

    /**
     * Renders the scrollbar track and thumb.
     */
    private void drawScrollbar(UIRenderInterface renderer, UIState state, float partialTicks) {
        float scrollbarWidth = style().getValue(state, SCROLLBAR_WIDTH);
        float scrollbarPadding = style().getValue(state, SCROLLBAR_PADDING);
        float scrollbarRadius = style().getValue(state, SCROLLBAR_RADIUS);
        int scrollbarColor = style().getValue(state, SCROLLBAR_COLOR);
        int trackColor = style().getValue(state, SCROLLBAR_TRACK_COLOR);

        // Apply opacity
        int alpha = (int) (scrollbarOpacity * 255);
        scrollbarColor = (scrollbarColor & 0x00FFFFFF) | (alpha << 24);
        trackColor = (trackColor & 0x00FFFFFF) | ((int) (alpha * 0.3f) << 24);

        float scrollbarX = this.getX() + this.getWidth() - scrollbarWidth - scrollbarPadding;
        float scrollbarY = this.getY() + scrollbarPadding;
        float trackHeight = getScrollbarTrackHeight();

        // Draw track
        renderer.drawRect(scrollbarX, scrollbarY, scrollbarWidth, trackHeight, trackColor, scrollbarRadius);

        // Draw thumb
        float thumbY = this.getY() + getScrollbarThumbPosition();
        float thumbHeight = getScrollbarThumbHeight();

        // Highlight on hover
        int thumbColor = isScrollbarHovered || isDraggingScrollbar ?
                brightenColor(scrollbarColor, 1.3f) : scrollbarColor;

        renderer.drawRect(scrollbarX, thumbY, scrollbarWidth, thumbHeight, thumbColor, scrollbarRadius);
    }

    /**
     * Brightens a color by a given factor.
     */
    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Sets the scroll position programmatically.
     */
    public UIScrollPanel setScrollOffset(float offset) {
        this.targetScrollOffset = offset;
        clampScrollOffset();
        showScrollbar();
        return this;
    }

    /**
     * Scrolls to make a specific child widget visible.
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
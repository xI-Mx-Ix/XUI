/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.scroll;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.effect.UIEffect;
import net.xmx.xui.core.effect.UIScissorsEffect;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * A container widget that acts as a viewport for overflowing content.
 * <p>
 * The {@code UIScrollComponent} clips its children to its own bounds and allows
 * the view to be shifted horizontally and vertically. It handles the logical
 * aspect of scrolling (events, math, layout) but does not render visual scrollbars.
 * </p>
 * <p>
 * <b>Features:</b>
 * <ul>
 *     <li><b>Bi-directional Scrolling:</b> Supports simultaneous X and Y scrolling.</li>
 *     <li><b>Smooth Interpolation:</b> animating scroll positions for a fluid feel.</li>
 *     <li><b>Input Mapping:</b> Mouse Wheel scrolls Y, Shift + Mouse Wheel scrolls X.</li>
 *     <li><b>Event Propagation:</b> Correctly transforms mouse coordinates for children.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIScrollComponent extends UIWidget {

    // --- Layout Cache ---

    /**
     * Cache for the base X positions of children relative to the container.
     * We need this to recalculate content size without the current scrollOffset affecting the math.
     */
    private final Map<UIWidget, Float> childBaseXPositions = new HashMap<>();

    /**
     * Cache for the base Y positions of children relative to the container.
     */
    private final Map<UIWidget, Float> childBaseYPositions = new HashMap<>();

    // --- Scrolling State ---

    /**
     * The current visual horizontal scroll offset.
     */
    private float scrollX = 0.0f;

    /**
     *  The target horizontal scroll offset (used for animation).
     */
    private float targetScrollX = 0.0f;

    /**
     *  The maximum allowable horizontal scroll (Content Width - Viewport Width).
     */
    private float maxScrollX = 0.0f;

    /**
     * The current visual vertical scroll offset.
     */
    private float scrollY = 0.0f;

    /**
     * The target vertical scroll offset (used for animation).
     */
    private float targetScrollY = 0.0f;

    /**
     *  The maximum allowable vertical scroll (Content Height - Viewport Height).
     */
    private float maxScrollY = 0.0f;

    // --- Content Dimensions ---

    /**
     * The total calculated width of all children combined.
     */
    private float contentWidth = 0.0f;

    /** T
     * he total calculated height of all children combined.
     */
    private float contentHeight = 0.0f;

    // --- Configuration ---

    /**
     *  The speed at which the scroll target changes per mouse wheel tick.
     */
    private static final float SCROLL_SPEED = 25.0f;

    /**
     * The smoothing factor for the interpolation (Higher = Faster/Snappier).
     */
    private static final float SMOOTHING_FACTOR = 15.0f;

    /**
     * Internal flag to indicate that mouse coordinates are currently being
     * transformed for child event processing.
     */
    private boolean handlingChildEvent = false;

    /**
     * Constructs a new Scroll Component.
     * <p>
     * Automatically attaches a {@link UIScissorsEffect} to ensure content
     * outside the viewport is not rendered.
     * </p>
     */
    public UIScrollComponent() {
        // Essential: Clip content to the bounds of this component.
        this.addEffect(new UIScissorsEffect());
        
        // Default style: Transparent background so only content is visible.
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
    }

    // =================================================================================
    // Layout & Dimensions
    // =================================================================================

    /**
     * Recalculates the layout dimensions and content size.
     * <p>
     * This method iterates through all children to find the total area they occupy.
     * It updates {@code maxScrollX} and {@code maxScrollY} accordingly.
     * </p>
     */
    @Override
    public void layout() {
        // 1. Perform standard layout to determine this component's size and child positions.
        super.layout();

        // 2. Cache the "Base" positions of children.
        // We do this because children.getX() might be modified by animations or other logic,
        // and we want the layout-relative position.
        childBaseXPositions.clear();
        childBaseYPositions.clear();
        for (UIWidget child : children) {
            childBaseXPositions.put(child, child.getX());
            childBaseYPositions.put(child, child.getY());
        }

        // 3. Calculate how large the content actually is.
        calculateContentDimensions();

        // 4. Update the scroll limits based on the new content vs viewport size.
        updateScrollLimits();

        // 5. Ensure the current scroll position is still valid (e.g., if window resized).
        clampScrollOffsets();
    }

    /**
     * Determines the total width and height of the content.
     * The content size is defined by the bottom-right corner of the furthest child.
     */
    private void calculateContentDimensions() {
        contentWidth = 0.0f;
        contentHeight = 0.0f;

        for (UIWidget child : children) {
            Float baseX = childBaseXPositions.get(child);
            Float baseY = childBaseYPositions.get(child);

            // Fallback safety check
            if (baseX == null) baseX = child.getX();
            if (baseY == null) baseY = child.getY();

            // Calculate the edge of this child relative to the scroll component's origin (0,0)
            float relativeRight = (baseX - this.getX()) + child.getWidth();
            float relativeBottom = (baseY - this.getY()) + child.getHeight();

            if (relativeRight > contentWidth) {
                contentWidth = relativeRight;
            }
            if (relativeBottom > contentHeight) {
                contentHeight = relativeBottom;
            }
        }

        // Add a small padding (buffer) to the end so content isn't flush against the edge.
        if (contentWidth > 0) contentWidth += 5.0f;
        if (contentHeight > 0) contentHeight += 5.0f;
    }

    /**
     * Updates the maximum scrollable values.
     * Formula: Max Scroll = Content Size - Viewport Size.
     */
    private void updateScrollLimits() {
        maxScrollX = Math.max(0.0f, contentWidth - this.getWidth());
        maxScrollY = Math.max(0.0f, contentHeight - this.getHeight());
    }

    /**
     * Clamps the scroll offsets to ensure they stay within valid bounds [0, maxScroll].
     */
    private void clampScrollOffsets() {
        targetScrollX = Math.max(0.0f, Math.min(targetScrollX, maxScrollX));
        scrollX = Math.max(0.0f, Math.min(scrollX, maxScrollX));

        targetScrollY = Math.max(0.0f, Math.min(targetScrollY, maxScrollY));
        scrollY = Math.max(0.0f, Math.min(scrollY, maxScrollY));
    }

    // =================================================================================
    // Rendering & Animation
    // =================================================================================

    /**
     * Renders the component and its children.
     * <p>
     * This method handles the visual translation of the rendering context to simulate
     * the camera moving over the content.
     * </p>
     */
    @Override
    public void render(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime) {
        if (!isVisible) return;

        // Update hover state for the container itself
        updateHoverState(mouseX, mouseY);

        // 1. Animate the scroll values (Smooth Scrolling)
        animateScroll(deltaTime);

        // 2. Prepare Interaction State
        InteractionState state = InteractionState.DEFAULT;
        if (isFocused) {
            state = InteractionState.ACTIVE;
        } else if (isHovered) {
            state = InteractionState.HOVER;
        }

        // 3. Apply Effects (e.g., Scissors)
        for (UIEffect effect : effects) {
            effect.apply(renderer, this);
        }

        // 4. Draw the background of the scroll panel
        drawSelf(renderer, mouseX, mouseY, partialTick, deltaTime, state);

        // 5. Render Children
        // We translate the renderer backwards by the scroll amount.
        // This moves the "world" up and left, creating the illusion that we are looking down and right.
        renderer.translate(-scrollX, -scrollY, 0);

        // Transform mouse coordinates to "World Space" for children
        int scrolledMouseX = (int) (mouseX + scrollX);
        int scrolledMouseY = (int) (mouseY + scrollY);

        handlingChildEvent = true;
        try {
            for (UIWidget child : children) {
                // Potential optimization: Check if child is within viewport bounds before rendering.
                child.render(renderer, scrolledMouseX, scrolledMouseY, partialTick, deltaTime);
            }
        } finally {
            handlingChildEvent = false;
        }

        // Revert Translation
        renderer.translate(scrollX, scrollY, 0);

        // Revert Effects
        for (int i = effects.size() - 1; i >= 0; i--) {
            effects.get(i).revert(renderer, this);
        }
    }

    /**
     * Interpolates the current scroll positions towards their targets using an exponential decay function.
     *
     * @param deltaTime Time elapsed since last frame.
     */
    private void animateScroll(float deltaTime) {
        // Horizontal Interpolation
        float diffX = targetScrollX - scrollX;
        if (Math.abs(diffX) > 0.1f) {
            scrollX += diffX * (1.0f - (float) Math.exp(-SMOOTHING_FACTOR * deltaTime));
        } else {
            scrollX = targetScrollX;
        }

        // Vertical Interpolation
        float diffY = targetScrollY - scrollY;
        if (Math.abs(diffY) > 0.1f) {
            scrollY += diffY * (1.0f - (float) Math.exp(-SMOOTHING_FACTOR * deltaTime));
        } else {
            scrollY = targetScrollY;
        }
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        
        // Only render if there is a visible color
        if ((bgColor >>> 24) > 0) {
            float radius = getFloat(ThemeProperties.BORDER_RADIUS, state, deltaTime);
            renderer.getGeometry().renderRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), bgColor, radius);
        }
    }

    // =================================================================================
    // Input Handling
    // =================================================================================

    /**
     * Checks if the mouse is over the widget.
     * <p>
     * When {@code handlingChildEvent} is true (during child processing), this method
     * accepts coordinates that have been offset by scrollX/Y and converts them back
     * to screen space to check against the physical scissor bounds.
     * </p>
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (handlingChildEvent) {
            // Convert "World" coordinate back to "Screen" coordinate
            return super.isMouseOver(mouseX - scrollX, mouseY - scrollY);
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    /**
     * Handles mouse wheel scrolling.
     * <p>
     * Logic:
     * <ul>
     *   <li>Default: Scroll Vertically (Y).</li>
     *   <li>Shift Key Held: Scroll Horizontally (X).</li>
     * </ul>
     * </p>
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        // If not visible or mouse not inside the panel, ignore
        if (!isVisible || !isMouseOver(mouseX, mouseY)) return false;

        // Check Input State for Shift Key
        long windowHandle = GLFW.glfwGetCurrentContext();
        boolean isShiftDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        boolean handled = false;

        // 1. Try Horizontal Scrolling (Shift pressed OR only horizontal available)
        if (isShiftDown && maxScrollX > 0) {
            targetScrollX -= (float) (scrollDelta * SCROLL_SPEED);
            clampScrollOffsets();
            handled = true;
        } 
        // 2. Try Vertical Scrolling (Default)
        else if (!isShiftDown && maxScrollY > 0) {
            targetScrollY -= (float) (scrollDelta * SCROLL_SPEED);
            clampScrollOffsets();
            handled = true;
        }

        // Even if we scrolled, we technically consumed the event.
        if (handled) return true;

        // Fallback: Pass to children (though unlikely needed for standard scroll panels)
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    /**
     * Propagates mouse clicks to children.
     * Transforms the mouse coordinates into the scrolled coordinate space.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;
        
        // Use standard clipping check (Physical bounds)
        if (isClippedByParent(mouseX, mouseY)) return false;

        // 1. Obstruction Check
        if (isGlobalObstructed(mouseX, mouseY)) return false;

        // 2. Calculate coordinates in the scrolled world
        double scrolledMouseX = mouseX + scrollX;
        double scrolledMouseY = mouseY + scrollY;

        // 3. Pass to Children
        handlingChildEvent = true;
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                UIWidget child = children.get(i);
                if (child.mouseClicked(scrolledMouseX, scrolledMouseY, button)) {
                    // Unfocus siblings
                    for (UIWidget sibling : children) {
                        if (sibling != child) sibling.unfocus();
                    }
                    return true;
                }
            }
        } finally {
            handlingChildEvent = false;
        }

        // 4. Handle self-focus
        if (isHovered && button == 0) {
            isFocused = true;
            for (UIWidget child : children) child.unfocus();
            if (onClick != null) onClick.accept(this);
            return true;
        }

        isFocused = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        double scrolledMouseX = mouseX + scrollX;
        double scrolledMouseY = mouseY + scrollY;

        handlingChildEvent = true;
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                if (children.get(i).mouseReleased(scrolledMouseX, scrolledMouseY, button)) {
                    return true;
                }
            }
        } finally {
            handlingChildEvent = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isVisible) return false;

        double scrolledMouseX = mouseX + scrollX;
        double scrolledMouseY = mouseY + scrollY;

        handlingChildEvent = true;
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                if (children.get(i).mouseDragged(scrolledMouseX, scrolledMouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        } finally {
            handlingChildEvent = false;
        }

        return isFocused;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isVisible) return;

        // Update own hover state (Physical coords)
        updateHoverState((int) mouseX, (int) mouseY);

        double scrolledMouseX = mouseX + scrollX;
        double scrolledMouseY = mouseY + scrollY;

        // Update children hover state (Logical coords)
        handlingChildEvent = true;
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                children.get(i).mouseMoved(scrolledMouseX, scrolledMouseY);
            }
        } finally {
            handlingChildEvent = false;
        }
    }

    // =================================================================================
    // Accessors & Mutators
    // =================================================================================

    /**
     * Gets the current visual horizontal scroll offset.
     * @return The x offset in pixels.
     */
    public float getScrollX() {
        return scrollX;
    }

    /**
     * Sets the horizontal scroll target programmatically.
     * This will animate towards the target.
     *
     * @param targetX The target X position in pixels.
     */
    public void setScrollX(float targetX) {
        this.targetScrollX = targetX;
        this.clampScrollOffsets();
    }

    /**
     * Gets the current visual vertical scroll offset.
     * @return The y offset in pixels.
     */
    public float getScrollY() {
        return scrollY;
    }

    /**
     * Sets the vertical scroll target programmatically.
     * This will animate towards the target.
     *
     * @param targetY The target Y position in pixels.
     */
    public void setScrollY(float targetY) {
        this.targetScrollY = targetY;
        this.clampScrollOffsets();
    }

    /**
     * Gets the maximum possible horizontal scroll value.
     * (ContentWidth - ViewportWidth).
     * @return The max scroll X.
     */
    public float getMaxScrollX() {
        return maxScrollX;
    }

    /**
     * Gets the maximum possible vertical scroll value.
     * (ContentHeight - ViewportHeight).
     * @return The max scroll Y.
     */
    public float getMaxScrollY() {
        return maxScrollY;
    }

    /**
     * Gets the total calculated width of the content inside this component.
     * @return Content width in pixels.
     */
    public float getContentWidth() {
        return contentWidth;
    }

    /**
     * Gets the total calculated height of the content inside this component.
     * @return Content height in pixels.
     */
    public float getContentHeight() {
        return contentHeight;
    }
}
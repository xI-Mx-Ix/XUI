/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

import net.xmx.xui.core.anim.AnimationManager;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.StyleSheet;
import net.xmx.xui.core.style.UIProperty;
import net.xmx.xui.core.style.UIState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The base class for all UI components.
 * Handles layout resolution, event propagation, styling, animations, and hierarchy.
 *
 * @author xI-Mx-Ix
 */
public abstract class UIWidget {

    // Geometry
    protected float x, y, width, height;

    // Layout Constraints
    protected UIConstraint xConstraint = Constraints.pixel(0);
    protected UIConstraint yConstraint = Constraints.pixel(0);
    protected UIConstraint widthConstraint = Constraints.pixel(100);
    protected UIConstraint heightConstraint = Constraints.pixel(20);

    // Hierarchy
    protected UIWidget parent;
    protected final List<UIWidget> children = new ArrayList<>();

    // State Flags
    protected boolean isHovered;
    protected boolean isFocused;
    protected boolean isVisible = true;
    protected boolean useScissor = false;

    // Styling & Animation
    protected final StyleSheet styleSheet = new StyleSheet();
    protected final AnimationManager animManager = new AnimationManager();

    // Event Callbacks
    protected Consumer<UIWidget> onMouseEnter;
    protected Consumer<UIWidget> onMouseExit;
    protected Consumer<UIWidget> onClick;

    /**
     * Calculates the layout of this widget and recursively its children.
     * Should be called before the render loop if dimensions change.
     */
    public void layout() {
        if (!isVisible) return;

        float pX = (parent != null) ? parent.x : 0;
        float pY = (parent != null) ? parent.y : 0;
        float pW = (parent != null) ? parent.width : 0;
        float pH = (parent != null) ? parent.height : 0;

        // Calculate size first, as position calculation might depend on self size
        this.width = widthConstraint.calculate(0, pW, 0);
        this.height = heightConstraint.calculate(0, pH, 0);

        this.x = xConstraint.calculate(pX, pW, width);
        this.y = yConstraint.calculate(pY, pH, height);

        for (UIWidget child : children) {
            child.layout();
        }
    }

    /**
     * The main rendering method.
     * Handles state determination, scissor testing, animation updates, and child rendering.
     *
     * @param renderer     The render interface.
     * @param mouseX       Current mouse X position.
     * @param mouseY       Current mouse Y position.
     * @param partialTicks Time delta for animation smoothing.
     */
    public void render(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible) return;

        updateHoverState(mouseX, mouseY);

        // Determine current style state
        UIState state = UIState.DEFAULT;
        if (isFocused) {
            state = UIState.ACTIVE;
        } else if (isHovered) {
            state = UIState.HOVER;
        }

        if (useScissor) {
            renderer.enableScissor((int) x, (int) y, (int) width, (int) height);
        }

        drawSelf(renderer, mouseX, mouseY, partialTicks, state);

        for (UIWidget child : children) {
            child.render(renderer, mouseX, mouseY, partialTicks);
        }

        if (useScissor) {
            renderer.disableScissor();
        }
    }

    /**
     * Subclasses implement this to draw their specific content.
     *
     * @param renderer     The render interface.
     * @param mouseX       Mouse X.
     * @param mouseY       Mouse Y.
     * @param partialTicks Time delta.
     * @param state        The current interactive state (Default, Hover, Active).
     */
    protected abstract void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state);

    /**
     * Helper to retrieve an animated color value based on the current state.
     */
    protected int getColor(UIProperty<Integer> prop, UIState currentState, float dt) {
        int target = styleSheet.getValue(currentState, prop);
        return animManager.getAnimatedColor(prop, target, styleSheet.getTransitionSpeed(), dt);
    }

    /**
     * Helper to retrieve an animated float value based on the current state.
     */
    protected float getFloat(UIProperty<Float> prop, UIState currentState, float dt) {
        float target = styleSheet.getValue(currentState, prop);
        return animManager.getAnimatedFloat(prop, target, styleSheet.getTransitionSpeed(), dt);
    }

    /**
     * Checks if this widget is currently visually clipped by any of its ancestors.
     * This occurs if a parent has enabled scissor testing and the mouse cursor
     * is outside that parent's bounds.
     *
     * @param mouseX The current mouse X coordinate.
     * @param mouseY The current mouse Y coordinate.
     * @return true if the interaction should be blocked due to clipping.
     */
    protected boolean isClippedByParent(double mouseX, double mouseY) {
        UIWidget current = this.parent;
        while (current != null) {
            // If a parent uses scissor (like a ScrollPanel), checks if the mouse
            // is effectively "outside" the visible area of that parent.
            if (current.useScissor && !current.isMouseOver(mouseX, mouseY)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * Updates the internal hover state based on mouse position and visibility checks.
     */
    protected void updateHoverState(int mouseX, int mouseY) {
        // The widget is only considered hovered if the mouse is over it AND
        // it is not visually hidden by a parent's scissor clip.
        boolean nowHovered = isMouseOver(mouseX, mouseY) && !isClippedByParent(mouseX, mouseY);

        if (nowHovered && !isHovered) {
            if (onMouseEnter != null) onMouseEnter.accept(this);
        } else if (!nowHovered && isHovered) {
            if (onMouseExit != null) onMouseExit.accept(this);
        }
        isHovered = nowHovered;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // Do not process clicks if the widget is clipped by a parent container
        if (isClippedByParent(mouseX, mouseY)) return false;

        // Propagate to children first (top-most visible first)
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (isHovered) {
            isFocused = true;
            if (onClick != null) {
                onClick.accept(this);
            }
            return true;
        } else {
            isFocused = false;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // Do not process release events if the cursor is in a clipped region
        if (isClippedByParent(mouseX, mouseY)) {
            isFocused = false;
            return false;
        }

        // Release the active state when mouse is released
        isFocused = false;

        // Propagate to children
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void add(UIWidget child) {
        child.parent = this;
        children.add(child);
    }

    // --- Accessors ---

    public StyleSheet style() {
        return this.styleSheet;
    }

    public UIWidget setX(UIConstraint c) {
        this.xConstraint = c;
        return this;
    }

    public UIWidget setY(UIConstraint c) {
        this.yConstraint = c;
        return this;
    }

    public UIWidget setWidth(UIConstraint c) {
        this.widthConstraint = c;
        return this;
    }

    public UIWidget setHeight(UIConstraint c) {
        this.heightConstraint = c;
        return this;
    }

    public UIWidget setOnMouseEnter(Consumer<UIWidget> action) {
        this.onMouseEnter = action;
        return this;
    }

    public UIWidget setOnMouseExit(Consumer<UIWidget> action) {
        this.onMouseExit = action;
        return this;
    }

    public UIWidget setOnClick(Consumer<UIWidget> action) {
        this.onClick = action;
        return this;
    }

    public UIWidget setScissor(boolean enabled) {
        this.useScissor = enabled;
        return this;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public UIWidget getParent() {
        return this.parent;
    }

    public List<UIWidget> getChildren() {
        return this.children;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public UIWidget setVisible(boolean visible) {
        this.isVisible = visible;
        return this;
    }

    protected void setXDirect(float x) {
        this.x = x;
    }

    protected void setYDirect(float y) {
        this.y = y;
    }

    protected void setWidthDirect(float width) {
        this.width = width;
    }

    protected void setHeightDirect(float height) {
        this.height = height;
    }

    public UIConstraint getXConstraint() {
        return this.xConstraint;
    }

    public UIConstraint getYConstraint() {
        return this.yConstraint;
    }

    public UIConstraint getWidthConstraint() {
        return this.widthConstraint;
    }

    public UIConstraint getHeightConstraint() {
        return this.heightConstraint;
    }
}
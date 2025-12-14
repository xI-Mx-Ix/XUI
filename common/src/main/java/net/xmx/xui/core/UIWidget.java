/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

import net.xmx.xui.core.anim.AnimationManager;
import net.xmx.xui.core.anim.AnimationBuilder;
import net.xmx.xui.core.effect.UIEffect;
import net.xmx.xui.core.effect.UIScissorsEffect;
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.style.StyleSheet;
import net.xmx.xui.core.style.StyleKey;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The base class for all UI components.
 * Handles layout resolution, event propagation, styling, animations, hierarchy, and visual effects.
 *
 * @author xI-Mx-Ix
 */
public abstract class UIWidget {

    /**
     * Interface for widgets that can obstruct interactions with underlying widgets.
     * Examples: Open Dropdowns, Modal Dialogs, Tooltips.
     */
    public interface WidgetObstructor {
        /**
         * Checks if the given screen coordinates are covered/blocked by this widget's overlay.
         *
         * @param mouseX The absolute X coordinate of the mouse.
         * @param mouseY The absolute Y coordinate of the mouse.
         * @return true if the area is obstructed.
         */
        boolean isObstructing(double mouseX, double mouseY);
    }

    /**
     * A thread-safe list of active obstructors in the UI system.
     * Widgets registers themselves here when they open overlays.
     */
    private static final List<WidgetObstructor> globalObstructors = new CopyOnWriteArrayList<>();

    /**
     * Registers a widget as an active obstructor.
     *
     * @param obstructor The widget acting as an overlay.
     */
    public static void addObstructor(WidgetObstructor obstructor) {
        if (!globalObstructors.contains(obstructor)) {
            globalObstructors.add(obstructor);
        }
    }

    /**
     * Unregisters a widget from the obstruction list.
     *
     * @param obstructor The widget to remove.
     */
    public static void removeObstructor(WidgetObstructor obstructor) {
        globalObstructors.remove(obstructor);
    }

    // Geometry
    protected float x, y, width, height;

    // Layout Layout
    protected AxisFunc xConstraint = Layout.pixel(0);
    protected AxisFunc yConstraint = Layout.pixel(0);
    protected AxisFunc widthConstraint = Layout.pixel(100);
    protected AxisFunc heightConstraint = Layout.pixel(20);

    // Hierarchy
    protected UIWidget parent;
    protected final List<UIWidget> children = new ArrayList<>();

    // State Flags
    protected boolean isHovered;
    protected boolean isFocused;
    protected boolean isVisible = true;

    // Styling, Animation & Effects
    protected final StyleSheet styleSheet = new StyleSheet();
    protected final AnimationManager animManager = new AnimationManager();
    protected final List<UIEffect> effects = new ArrayList<>();

    // Event Callbacks
    protected Consumer<UIWidget> onMouseEnter;
    protected Consumer<UIWidget> onMouseExit;
    protected Consumer<UIWidget> onClick;

    /**
     * Adds a visual effect to this widget.
     * Effects are applied in the order they are added.
     *
     * @param effect The effect to add (e.g., Stencil, Scissor).
     * @return This widget instance.
     */
    public UIWidget addEffect(UIEffect effect) {
        this.effects.add(effect);
        return this;
    }

    /**
     * Removes a visual effect from this widget.
     *
     * @param effect The effect instance to remove.
     * @return This widget instance.
     */
    public UIWidget removeEffect(UIEffect effect) {
        this.effects.remove(effect);
        return this;
    }

    /**
     * Creates a new animation builder for this widget.
     * Allows fluent configuration of keyframe animations.
     *
     * @return A new AnimationBuilder instance.
     */
    public AnimationBuilder animate() {
        return new AnimationBuilder(this);
    }

    /**
     * Retrieves the animation manager for this widget.
     *
     * @return The internal AnimationManager instance.
     */
    public AnimationManager getAnimationManager() {
        return animManager;
    }

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
     * Handles state determination, effect application, animation updates, 3D TRANSFORMS, and child rendering.
     *
     * @param renderer     The render interface.
     * @param mouseX       Current mouse X position.
     * @param mouseY       Current mouse Y position.
     * @param partialTick  The normalized progress between two game ticks.
     * @param deltaTime    The time elapsed since the last frame in seconds.
     */
    public void render(RenderInterface renderer, int mouseX, int mouseY, float partialTick, float deltaTime) {
        if (!isVisible) return;

        // 1. Update Animation State
        // This advances timelines and updates style properties
        animManager.update(deltaTime);

        // 2. Retrieve 3D Transformation Values from Style
        // We generally use the DEFAULT state as the source for structural transforms.
        float rotX = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.ROTATION_X);
        float rotY = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.ROTATION_Y);
        float rotZ = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.ROTATION_Z);

        float scaleX = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.SCALE_X);
        float scaleY = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.SCALE_Y);
        float scaleZ = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.SCALE_Z);

        float transX = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.TRANSLATE_X);
        float transY = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.TRANSLATE_Y);
        float transZ = styleSheet.getValue(InteractionState.DEFAULT, ThemeProperties.TRANSLATE_Z);

        // 3. Update Hitbox Logic (Inverse Matrix Calculation)
        // Checks if the mouse is hovering the widget considering its 3D position/rotation.
        updateHoverState(mouseX, mouseY, rotX, rotY, rotZ, scaleX, scaleY, scaleZ, transX, transY, transZ);

        InteractionState state = InteractionState.DEFAULT;
        if (isFocused) {
            state = InteractionState.ACTIVE;
        } else if (isHovered) {
            state = InteractionState.HOVER;
        }

        // 4. Apply 3D Transforms to the Renderer
        renderer.pushMatrix();

        // Calculate Pivot Point (Center of the widget)
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float centerZ = 0.0f; // Standard UI plane is at Z=0 locally

        // Transform Sequence:
        // 1. Move origin to Pivot point (Center)
        // 2. Apply Translation (Offset)
        // 3. Apply Rotations (Euler)
        // 4. Apply Scale
        // 5. Move origin back to top-left (invert Pivot)

        renderer.translate(centerX + transX, centerY + transY, centerZ + transZ);

        // Apply Rotations (X, then Y, then Z - Standard Euler order)
        if (rotX != 0) renderer.rotate(rotX, 1, 0, 0); // Pitch
        if (rotY != 0) renderer.rotate(rotY, 0, 1, 0); // Yaw
        if (rotZ != 0) renderer.rotate(rotZ, 0, 0, 1); // Roll (2D)

        renderer.scale(scaleX, scaleY, scaleZ);

        // Move back from pivot
        renderer.translate(-centerX, -centerY, -centerZ);

        // 5. Apply Effects & Render Content
        for (UIEffect effect : effects) {
            effect.apply(renderer, this);
        }

        drawSelf(renderer, mouseX, mouseY, partialTick, deltaTime, state);

        for (UIWidget child : children) {
            child.render(renderer, mouseX, mouseY, partialTick, deltaTime);
        }

        // Revert effects
        for (int i = effects.size() - 1; i >= 0; i--) {
            effects.get(i).revert(renderer, this);
        }

        // Restore Matrix
        renderer.popMatrix();
    }

    /**
     * Performs a 3D Unproject/Inverse-Transform to check if the mouse cursor (in screen space)
     * intersects with the widget's local bounds, accounting for 3D rotations and scaling.
     *
     * @param mouseX Global mouse X.
     * @param mouseY Global mouse Y.
     * @param rX     Rotation X (deg).
     * @param rY     Rotation Y (deg).
     * @param rZ     Rotation Z (deg).
     * @param sX     Scale X.
     * @param sY     Scale Y.
     * @param sZ     Scale Z.
     * @param tX     Translate X.
     * @param tY     Translate Y.
     * @param tZ     Translate Z.
     */
    protected void updateHoverState(int mouseX, int mouseY,
                                    float rX, float rY, float rZ,
                                    float sX, float sY, float sZ,
                                    float tX, float tY, float tZ) {

        // 1. Pivot Calculation
        float cX = x + width / 2.0f;
        float cY = y + height / 2.0f;

        // 2. Construct Model Matrix
        // This must EXACTLY match the transformation sequence in render()
        Matrix4f model = new Matrix4f()
                .translate(cX + tX, cY + tY, tZ)
                .rotate((float) Math.toRadians(rX), 1, 0, 0)
                .rotate((float) Math.toRadians(rY), 0, 1, 0)
                .rotate((float) Math.toRadians(rZ), 0, 0, 1)
                .scale(sX, sY, sZ)
                .translate(-cX, -cY, 0);

        // 3. Invert Matrix (Global -> Local Space)
        // If the matrix is singular (e.g. Scale=0), invert() might fail, so we catch issues implicitly or check determinant
        Matrix4f inv = new Matrix4f(model).invert();

        // 4. Transform Mouse Point
        // We assume the mouse is on the screen plane (Z=0).
        Vector4f vec = new Vector4f((float) mouseX, (float) mouseY, 0.0f, 1.0f);
        vec.mul(inv);

        // 5. Hit Test in Local AABB
        // Since we transformed the mouse INTO the widget's coordinate system,
        // we can just check against the widget's original un-rotated bounds (x, y, width, height).
        boolean hit = vec.x >= x && vec.x <= x + width &&
                vec.y >= y && vec.y <= y + height;

        // 6. Apply standard blocking logic (Clipping, Obstructors)
        if (hit && !isClippedByParent(mouseX, mouseY) && !isGlobalObstructed(mouseX, mouseY)) {
            if (!isHovered) {
                isHovered = true;
                if (onMouseEnter != null) onMouseEnter.accept(this);
            }
        } else {
            if (isHovered) {
                isHovered = false;
                if (onMouseExit != null) onMouseExit.accept(this);
            }
        }
    }

    /**
     * Subclasses implement this to draw their specific content.
     *
     * @param renderer     The render interface.
     * @param mouseX       Mouse X.
     * @param mouseY       Mouse Y.
     * @param partialTick  The normalized progress between game ticks (for interpolation).
     * @param deltaTime    The time elapsed since last frame in seconds (for animations).
     * @param state        The current interactive state (Default, Hover, Active).
     */
    protected abstract void drawSelf(RenderInterface renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state);

    /**
     * Helper to retrieve an animated color value based on the current state.
     * Uses deltaTime to progress the animation.
     */
    protected int getColor(StyleKey<Integer> prop, InteractionState currentState, float dt) {
        int target = styleSheet.getValue(currentState, prop);
        return animManager.getAnimatedColor(prop, target, styleSheet.getTransitionSpeed(), dt);
    }

    /**
     * Helper to retrieve an animated float value based on the current state.
     * Uses deltaTime to progress the animation.
     */
    protected float getFloat(StyleKey<Float> prop, InteractionState currentState, float dt) {
        float target = styleSheet.getValue(currentState, prop);
        return animManager.getAnimatedFloat(prop, target, styleSheet.getTransitionSpeed(), dt);
    }

    /**
     * Checks if this widget is currently visually clipped by any of its ancestors.
     * This occurs if a parent has an active {@link UIScissorsEffect} and the mouse cursor
     * is outside that parent's bounds.
     *
     * @param mouseX The current mouse X coordinate.
     * @param mouseY The current mouse Y coordinate.
     * @return true if the interaction should be blocked due to clipping.
     */
    protected boolean isClippedByParent(double mouseX, double mouseY) {
        UIWidget current = this.parent;
        while (current != null) {
            // Iterate over the parent's effects to see if clipping is enabled
            for (UIEffect effect : current.effects) {
                if (effect instanceof UIScissorsEffect) {
                    // If the parent is clipping content, and the mouse is outside the parent,
                    // then this child is clipped/hidden at the mouse position.
                    if (!current.isMouseOver(mouseX, mouseY)) {
                        return true;
                    }
                }
            }
            current = current.parent;
        }
        return false;
    }

    /**
     * Checks if the given coordinates are obstructed by any active global overlay.
     *
     * @param mouseX The current mouse X.
     * @param mouseY The current mouse Y.
     * @return true if an active obstructor (like an open dropdown) is covering this point.
     */
    protected boolean isGlobalObstructed(double mouseX, double mouseY) {
        for (WidgetObstructor obstructor : globalObstructors) {
            // A widget does not obstruct itself
            if (obstructor != this && obstructor.isObstructing(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the internal hover state based on mouse position and visibility checks.
     */
    protected void updateHoverState(int mouseX, int mouseY) {
        // If an overlay (dropdown) blocks this area, we are not hovered,
        // unless we ARE the dropdown (handled in the check above via reference equality).
        if (isGlobalObstructed(mouseX, mouseY)) {
            if (isHovered) {
                isHovered = false;
                if (onMouseExit != null) onMouseExit.accept(this);
            }
            return;
        }

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

    /**
     * Determines whether this widget should keep its focus after the mouse button is released.
     * <p>
     * Override this method to return {@code true} for widgets that require continuous input
     * (like text fields). The default is {@code false}, which ensures that momentary widgets
     * (like buttons) release their active state immediately after the click action ends.
     * </p>
     *
     * @return true if focus should be retained, false to release it.
     */
    protected boolean shouldRetainFocus() {
        return false;
    }

    /**
     * Recursively removes focus from this widget and all its children.
     */
    public void unfocus() {
        this.isFocused = false;
        for (UIWidget child : children) {
            child.unfocus();
        }
    }

    // =================================================================================
    // Interaction & Input Handling
    // =================================================================================

    /**
     * Called when a mouse button is pressed.
     * <p>
     * This method propagates the event to children in reverse order (Z-order).
     * If no child consumes the event, the widget checks if it should handle the interaction itself.
     * Interaction is strictly limited to the Left Mouse Button (ID 0).
     * </p>
     *
     * @param mouseX The absolute X coordinate of the mouse.
     * @param mouseY The absolute Y coordinate of the mouse.
     * @param button The mouse button index. 0 represents the Left Mouse Button.
     * @return {@code true} if the event was consumed by this widget or a child; {@code false} otherwise.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If the widget is hidden, it cannot receive input.
        if (!isVisible) return false;

        // If the widget is visually clipped by a parent, ignore the input.
        if (isClippedByParent(mouseX, mouseY)) return false;

        // Propagate the event to children.
        // We iterate in reverse order so the last drawn child (topmost) gets the event first.
        for (int i = children.size() - 1; i >= 0; i--) {
            UIWidget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button)) {
                // If a child consumed the click, we must unfocus all its siblings
                // to ensure only one widget is focused at a time.
                for (UIWidget sibling : children) {
                    if (sibling != child) sibling.unfocus();
                }
                return true;
            }
        }

        // Check if an overlay (like an open dropdown) is blocking this area.
        if (isGlobalObstructed(mouseX, mouseY)) return false;

        // If no child handled it, check if we handled it ourselves.
        // We explicitly check for 'button == 0' to ensure only Left Click triggers the interaction.
        if (isHovered && button == 0) {
            isFocused = true;
            // If the container itself is clicked, remove focus from its children.
            for (UIWidget child : children) child.unfocus();

            // Trigger the OnClick callback if defined.
            if (onClick != null) onClick.accept(this);
            return true;
        } else {
            // Click occurred outside this widget or with a non-primary button.
            isFocused = false;
        }

        return false;
    }

    /**
     * Called when a mouse button is released.
     * <p>
     * Propagates to children first. If the widget was focused (e.g., a button being held),
     * it checks {@link #shouldRetainFocus()} to see if it should keep the active state.
     * </p>
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button released.
     * @return {@code true} if the event was handled.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;
        if (isClippedByParent(mouseX, mouseY)) return false;

        // Propagate to children first
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Logic for releasing focus.
        // Standard widgets (buttons) lose focus on release.
        // Complex widgets (text boxes) might retain focus to continue typing.
        if (isFocused && !shouldRetainFocus()) {
            isFocused = false;
            return true;
        }

        return false;
    }

    /**
     * Called when the mouse is moved while a button is held down.
     * <p>
     * This is used for dragging logic (sliders, scrollbars, text selection).
     * If a widget is focused, it typically consumes drag events even if the mouse
     * moves outside its bounds (e.g., dragging a slider thumb).
     * </p>
     *
     * @param mouseX The current absolute X coordinate.
     * @param mouseY The current absolute Y coordinate.
     * @param button The button currently held down.
     * @param dragX  The change in X since the last update.
     * @param dragY  The change in Y since the last update.
     * @return {@code true} if the event was handled.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isVisible) return false;

        // Propagate to children first
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }

        // If this widget is focused, it captures the drag event.
        return isFocused;
    }

    /**
     * Called when the mouse wheel is scrolled.
     *
     * @param mouseX      The X coordinate of the mouse.
     * @param mouseY      The Y coordinate of the mouse.
     * @param scrollDelta The amount scrolled (positive = up, negative = down).
     * @return {@code true} if the scroll event was consumed.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isVisible) return false;
        if (isClippedByParent(mouseX, mouseY)) return false;

        // Obstruction check: prevent scrolling widgets underneath a modal/dropdown.
        if (isGlobalObstructed(mouseX, mouseY)) return false;

        // Propagate to children first
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseScrolled(mouseX, mouseY, scrollDelta)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Called whenever the mouse moves, regardless of button state.
     * <p>
     * This is primarily used to update the "Hover" state of widgets or to track
     * the mouse position for tooltips and highlights.
     * </p>
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     */
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isVisible) return;

        // Force an update of the internal hover state based on the new position.
        // This ensures visual states change immediately, not just on render.
        updateHoverState((int) mouseX, (int) mouseY);

        // Propagate to all children so they can update their hover states too.
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * Called when a keyboard key is pressed.
     *
     * @param keyCode   The GLFW key code (e.g., GLFW.GLFW_KEY_A).
     * @param scanCode  The physical scan code of the key.
     * @param modifiers The bitmask of modifier keys pressed (Shift, Ctrl, Alt).
     * @return {@code true} if the key press was handled.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isVisible) return false;

        // Propagate to children
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a keyboard key is released.
     *
     * @param keyCode   The GLFW key code.
     * @param scanCode  The physical scan code.
     * @param modifiers The bitmask of modifier keys.
     * @return {@code true} if the event was handled.
     */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!isVisible) return false;

        // Propagate to children
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a Unicode character is typed.
     * <p>
     * This is distinct from {@link #keyPressed} as it handles actual text input,
     * respecting keyboard layouts and operating system modifiers.
     * </p>
     *
     * @param codePoint The Unicode character code.
     * @param modifiers The bitmask of modifier keys.
     * @return {@code true} if the character was consumed (e.g., by a text box).
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isVisible) return false;

        // Propagate to children
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).charTyped(codePoint, modifiers)) {
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

    public UIWidget setX(AxisFunc c) {
        this.xConstraint = c;
        return this;
    }

    public UIWidget setY(AxisFunc c) {
        this.yConstraint = c;
        return this;
    }

    public UIWidget setWidth(AxisFunc c) {
        this.widthConstraint = c;
        return this;
    }

    public UIWidget setHeight(AxisFunc c) {
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

    /**
     * Gets the physical window width using GLFW.
     *
     * @return The window width in pixels
     */
    public int getScreenWidth() {
        long window = GLFW.glfwGetCurrentContext();
        int[] width = new int[1];
        GLFW.glfwGetWindowSize(window, width, new int[1]);
        return width[0];
    }

    /**
     * Gets the physical window height using GLFW.
     *
     * @return The window height in pixels
     */
    public int getScreenHeight() {
        long window = GLFW.glfwGetCurrentContext();
        int[] height = new int[1];
        GLFW.glfwGetWindowSize(window, new int[1], height);
        return height[0];
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

    public AxisFunc getXConstraint() {
        return this.xConstraint;
    }

    public AxisFunc getYConstraint() {
        return this.yConstraint;
    }

    public AxisFunc getWidthConstraint() {
        return this.widthConstraint;
    }

    public AxisFunc getHeightConstraint() {
        return this.heightConstraint;
    }
}
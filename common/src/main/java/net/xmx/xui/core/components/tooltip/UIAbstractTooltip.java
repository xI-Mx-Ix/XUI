/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.tooltip;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

import java.util.function.Consumer;

/**
 * The base class for all tooltip implementations.
 * <p>
 * This class acts as a container (inheriting from {@link UIPanel}) that manages
 * visibility states, animations, autosizing based on content, and screen boundary clamping.
 * </p>
 * <p>
 * It implements {@link net.xmx.xui.core.UIWidget.WidgetObstructor} to ensure that
 * when the tooltip is visible, it blocks mouse interactions with widgets positioned underneath it.
 * </p>
 * <p>
 * Content added to this tooltip is placed inside an internal content panel, ensuring
 * that padding is respected automatically.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public abstract class UIAbstractTooltip extends UIPanel implements UIWidget.WidgetObstructor {

    // --- Style Keys ---

    /**
     * Time in seconds the mouse must hover before the tooltip appears.
     */
    public static final StyleKey<Float> SHOW_DELAY = new StyleKey<>("tooltip_show_delay", 0.5f);

    /**
     * Duration of the fade-in animation in seconds.
     */
    public static final StyleKey<Float> FADE_IN_TIME = new StyleKey<>("tooltip_fade_in_time", 0.2f);

    /**
     * Duration of the fade-out animation in seconds.
     */
    public static final StyleKey<Float> FADE_OUT_TIME = new StyleKey<>("tooltip_fade_out_time", 0.2f);

    /**
     * Offset distance from the target or mouse in pixels.
     */
    public static final StyleKey<Float> OFFSET = new StyleKey<>("tooltip_offset", 10.0f);

    /**
     * Padding around the content inside the tooltip.
     */
    public static final StyleKey<Float> PADDING = new StyleKey<>("tooltip_padding", 5.0f);

    // --- State Management ---

    /**
     * Internal state enum for the visibility state machine.
     */
    protected enum VisibilityState {
        HIDDEN,
        WAITING_FOR_DELAY,
        FADING_IN,
        VISIBLE,
        FADING_OUT
    }

    protected VisibilityState state = VisibilityState.HIDDEN;
    protected UIWidget target;
    protected TooltipAnchor anchor = TooltipAnchor.TOP;

    /**
     * Internal container for the actual content widgets.
     * This panel allows precise control over padding and content layout
     * separate from the background frame.
     */
    protected final UIPanel contentPanel = new UIPanel();

    // Timers
    protected float stateTimer = 0.0f;
    protected float currentOpacity = 0.0f;

    // Events
    protected Consumer<UIAbstractTooltip> onShow;
    protected Consumer<UIAbstractTooltip> onHide;

    /**
     * Constructs a base tooltip with default dark styling.
     * Initializes the internal content panel.
     */
    public UIAbstractTooltip() {
        // Default Styling for the tooltip frame
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xF0101010)
                .set(ThemeProperties.BORDER_COLOR, 0x505000FF)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f));

        // Configure internal content panel to be transparent
        this.contentPanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // Add the content panel as a direct child of the tooltip frame
        super.add(this.contentPanel);

        // Tooltips are logically visible but visually hidden by opacity initially
        this.setVisible(true);
    }

    /**
     * Sets the target widget that triggers this tooltip.
     *
     * @param target The widget to observe.
     * @return This instance for chaining.
     */
    public UIAbstractTooltip setTarget(UIWidget target) {
        this.target = target;
        return this;
    }

    /**
     * Defines where the tooltip should appear relative to the target or mouse.
     *
     * @param anchor The anchor strategy.
     * @return This instance for chaining.
     */
    public UIAbstractTooltip setAnchor(TooltipAnchor anchor) {
        this.anchor = anchor;
        return this;
    }

    /**
     * Callback fired when the tooltip becomes fully visible.
     *
     * @param onShow The consumer to execute.
     * @return This instance for chaining.
     */
    public UIAbstractTooltip setOnShow(Consumer<UIAbstractTooltip> onShow) {
        this.onShow = onShow;
        return this;
    }

    /**
     * Callback fired when the tooltip becomes completely hidden.
     *
     * @param onHide The consumer to execute.
     * @return This instance for chaining.
     */
    public UIAbstractTooltip setOnHide(Consumer<UIAbstractTooltip> onHide) {
        this.onHide = onHide;
        return this;
    }

    /**
     * Adds a child widget to the internal content panel.
     *
     * @param child The widget to add.
     */
    @Override
    public void add(UIWidget child) {
        this.contentPanel.add(child);
    }

    /**
     * Removes a child widget from the internal content panel.
     *
     * @param child The widget to remove.
     */
    public void remove(UIWidget child) {
        this.contentPanel.getChildren().remove(child);
    }

    /**
     * Recalculates the size of the tooltip based on the content panel's children.
     * <p>
     * This method positions the content panel according to the padding, allows
     * the content panel to calculate its layout, and then resizes the tooltip frame
     * to fit the content plus padding.
     * </p>
     */
    @Override
    public void layout() {
        float padding = style().getValue(InteractionState.DEFAULT, PADDING);

        // 1. Position the content panel based on padding
        // Using setters ensures the layout dirty flag is set for the content panel
        contentPanel.setX(Layout.pixel(padding));
        contentPanel.setY(Layout.pixel(padding));

        // 2. Execute standard layout to let the content panel position its children
        super.layout();

        float contentMaxX = 0;
        float contentMaxY = 0;

        // 3. Calculate the bounding box of the content panel's children
        // We calculate positions relative to the content panel
        for (UIWidget child : contentPanel.getChildren()) {
            // Calculate child's bottom-right edge relative to the content panel's origin
            float childRight = (child.getX() - contentPanel.getX()) + child.getWidth();
            float childBottom = (child.getY() - contentPanel.getY()) + child.getHeight();

            if (childRight > contentMaxX) contentMaxX = childRight;
            if (childBottom > contentMaxY) contentMaxY = childBottom;
        }

        // 4. Calculate total required size (Content Size + Padding * 2)
        // Ensure a minimum size of 20px to prevent visual artifacts if empty
        float newWidth = Math.max(20, contentMaxX + (padding * 2));
        float newHeight = Math.max(20, contentMaxY + (padding * 2));

        // Update the tooltip frame dimensions
        this.setWidth(Layout.pixel(newWidth));
        this.setHeight(Layout.pixel(newHeight));

        // Update the content panel dimensions to fill the area minus padding
        // This is important if children rely on relative sizing
        contentPanel.setWidth(Layout.pixel(contentMaxX));
        contentPanel.setHeight(Layout.pixel(contentMaxY));

        // 5. Force an immediate layout update to apply the new dimensions
        super.layout();
    }

    /**
     * Renders the tooltip on top of other widgets.
     * <p>
     * Applies dynamic positioning updates, Z-translation, and opacity management.
     * It also maintains the widget obstruction state based on visibility.
     * </p>
     */
    @Override
    public void render(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime) {
        if (target == null) return;

        // 1. Update State Logic (implemented by subclasses)
        updateLogic(deltaTime, mouseX, mouseY);

        // 2. Update Obstruction Registration
        updateObstructorState();

        // 3. Check if anything is visible
        if (currentOpacity <= 0.001f) return;

        // 4. Calculate Screen Position based on Anchor
        // This will update x/y constraints and force a layout update for this frame.
        calculatePosition(mouseX, mouseY);

        // 5. Push Z-Index to overlay everything
        renderer.pushMatrix();
        renderer.translate(0, 0, 500.0f);

        // 6. Apply Opacity
        float originalOpacity = style().getValue(InteractionState.DEFAULT, ThemeProperties.OPACITY);
        style().set(InteractionState.DEFAULT, ThemeProperties.OPACITY, currentOpacity);

        // 7. Render Self (Background) and Children (via ContentPanel)
        super.render(renderer, mouseX, mouseY, partialTick, deltaTime);

        // 8. Restore State
        style().set(InteractionState.DEFAULT, ThemeProperties.OPACITY, originalOpacity);
        renderer.popMatrix();
    }

    /**
     * Determines if this widget is obstructing interaction at the given coordinates.
     *
     * @param mouseX The absolute X coordinate of the mouse.
     * @param mouseY The absolute Y coordinate of the mouse.
     * @return true if the tooltip is visible, opaque enough, and covers the coordinates.
     */
    @Override
    public boolean isObstructing(double mouseX, double mouseY) {
        return this.isVisible()
                && this.currentOpacity > 0.1f
                && this.isMouseOver(mouseX, mouseY);
    }

    /**
     * Updates the global obstructor registration based on the current state.
     */
    private void updateObstructorState() {
        if (state != VisibilityState.HIDDEN) {
            UIWidget.addObstructor(this);
        } else {
            UIWidget.removeObstructor(this);
        }
    }

    /**
     * Updates the internal state machine (Timers, Fading).
     *
     * @param dt     Delta time in seconds.
     * @param mouseX Global mouse X.
     * @param mouseY Global mouse Y.
     */
    protected abstract void updateLogic(float dt, int mouseX, int mouseY);

    /**
     * Calculates the absolute screen position of the tooltip.
     * Handles boundary clamping to keep the tooltip on screen.
     *
     * @param mouseX Current mouse X.
     * @param mouseY Current mouse Y.
     */
    protected void calculatePosition(int mouseX, int mouseY) {
        float x;
        float y;

        if (anchor == TooltipAnchor.MOUSE) {
            float offset = style().getValue(InteractionState.DEFAULT, OFFSET);
            x = mouseX + offset;
            y = mouseY + offset;
        } else {
            x = calculateFixedX();
            y = calculateFixedY();
        }

        // Clamp to screen bounds
        int sw = getScreenWidth();
        int sh = getScreenHeight();

        // Check Right Edge
        if (x + width > sw) {
            if (anchor == TooltipAnchor.MOUSE) {
                x = mouseX - width - 10;
            } else if (anchor == TooltipAnchor.RIGHT) {
                // Smart Flip: If anchored RIGHT but no space, flip to LEFT of target
                x = target.getX() - width - 10;
            } else {
                x = sw - width - 5;
            }
        }

        // Check Bottom Edge
        if (y + height > sh) {
            if (anchor == TooltipAnchor.MOUSE) {
                y = mouseY - height;
            } else if (anchor == TooltipAnchor.BOTTOM) {
                // Smart Flip: If anchored BOTTOM but no space, flip to TOP of target
                y = target.getY() - height - 10;
            } else {
                y = sh - height - 5;
            }
        }

        // Check Left/Top Edges
        if (x < 5) x = 5;
        if (y < 5) y = 5;

        // Apply new position using setters to mark layout as dirty
        this.setX(Layout.pixel(x));
        this.setY(Layout.pixel(y));

        // Force an immediate layout update so the new X/Y are reflected in rendering
        super.layout();
    }

    /**
     * Calculates X position for fixed anchors.
     */
    private float calculateFixedX() {
        float offset = style().getValue(InteractionState.DEFAULT, OFFSET);
        float tx = target.getX();
        float tw = target.getWidth();

        return switch (anchor) {
            case TOP, BOTTOM, CENTER -> tx + (tw - width) / 2.0f;
            case LEFT -> tx - width - offset;
            case RIGHT -> tx + tw + offset;
            default -> 0;
        };
    }

    /**
     * Calculates Y position for fixed anchors.
     */
    private float calculateFixedY() {
        float offset = style().getValue(InteractionState.DEFAULT, OFFSET);
        float ty = target.getY();
        float th = target.getHeight();

        return switch (anchor) {
            case TOP -> ty - height - offset;
            case BOTTOM -> ty + th + offset;
            case LEFT, RIGHT, CENTER -> ty + (th - height) / 2.0f;
            default -> 0;
        };
    }

    /**
     * Helper to modify alpha channel of a color integer.
     *
     * @param color The ARGB color.
     * @param alpha The alpha multiplier (0.0 to 1.0).
     * @return The new ARGB color.
     */
    protected int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color & 0xFF);
        a = (int) (a * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Overridden to manually apply the calculated opacity to the background drawing.
     */
    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
        // Intercept colors to apply opacity
        int bg = style().getValue(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR);
        int border = style().getValue(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR);

        style().set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, applyAlpha(bg, currentOpacity));
        style().set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, applyAlpha(border, currentOpacity));

        super.drawSelf(renderer, mouseX, mouseY, partialTick, deltaTime, state);

        // Restore original colors
        style().set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, bg);
        style().set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, border);
    }
}
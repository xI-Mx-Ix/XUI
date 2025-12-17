/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.anim.Easing;
import net.xmx.xui.core.components.scroll.ScrollOrientation;
import net.xmx.xui.core.components.scroll.UIScrollBar;
import net.xmx.xui.core.components.scroll.UIScrollComponent;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * An Accordion widget that expands vertically to reveal a scrollable content area.
 * <p>
 * <b>Layout Behavior:</b>
 * This widget implements "Inline Expansion". When opened, it increases its own height,
 * pushing sibling widgets downwards.
 * </p>
 * <p>
 * <b>Visuals:</b>
 * <ul>
 *     <li><b>Header:</b> A clickable bar with a title and rotating arrow.</li>
 *     <li><b>Body:</b> A container styled like a {@link UIPanel} that holds the content.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Interaction:</b>
 * Single-click to open. Clicking outside the widget will automatically close it (unfocus).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIAccordion extends UIPanel {

    // --- Style Keys ---

    /**
     * Animation state for expansion (0.0 = Closed, 1.0 = Fully Open).
     */
    private static final StyleKey<Float> EXPANSION_PROGRESS = new StyleKey<>("accordion_expansion", 0.0f);

    /**
     * The rotation angle of the chevron arrow in degrees.
     * 0 = Right (Closed), 90 = Down (Open).
     */
    private static final StyleKey<Float> ARROW_ROTATION = new StyleKey<>("accordion_arrow_rot", 0.0f);

    /**
     * Color of the header background.
     */
    public static final StyleKey<Integer> HEADER_COLOR = new StyleKey<>("accordion_header_color", 0xFF252525);

    /**
     * Color of the chevron arrow.
     */
    public static final StyleKey<Integer> ARROW_COLOR = new StyleKey<>("accordion_arrow_color", 0xFFAAAAAA);

    /**
     * The vertical spacing (gap) between the header and the expanded body in pixels.
     */
    public static final StyleKey<Float> BODY_GAP = new StyleKey<>("accordion_body_gap", 4.0f);

    /**
     * Background color of the expanded body area.
     */
    public static final StyleKey<Integer> BODY_BACKGROUND_COLOR = new StyleKey<>("accordion_body_bg", 0xFF181818);

    /**
     * Border color of the expanded body area.
     */
    public static final StyleKey<Integer> BODY_BORDER_COLOR = new StyleKey<>("accordion_body_border", 0xFF404040);

    /**
     * Border radius of the expanded body area.
     */
    public static final StyleKey<Float> BODY_BORDER_RADIUS = new StyleKey<>("accordion_body_radius", 4.0f);

    // --- Child Components ---

    private final UIPanel header;
    private final UIText headerTitle;

    /**
     * The container for the accordion content.
     * This is an anonymous subclass of UIScrollComponent to enable custom visual styling (Panel look).
     */
    private final UIScrollComponent body;
    private final UIScrollBar scrollBar;

    // --- State & Config ---

    private boolean isOpen = false;
    private float headerHeight = 26.0f;
    private float maxBodyHeight = 150.0f; // The height of the viewport when fully open

    /**
     * Constructs a new Accordion with an empty title.
     * Use {@link #setTitle(String)} to set the label.
     */
    public UIAccordion() {
        // The main container is transparent; visuals are handled by children (header/body)
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // 1. Initialize Header
        this.header = new UIPanel();
        // We handle clicks in the Accordion's mouseClicked method to ensure immediate response
        // and correct focus handling, so we do not set a click listener on the header directly.

        this.headerTitle = new UIText().setText(""); // Empty default
        setupHeader();

        // 2. Initialize Body (The Scroll Viewport)
        // We use an anonymous class to override drawSelf. This allows the body to render
        // a background and border exactly like a UIPanel or Dropdown overlay.
        this.body = new UIScrollComponent() {
            @Override
            protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
                // Retrieve styling properties from the parent Accordion to ensure consistent styling.
                // We use Default state for the body to ensure stability.
                int bgColor = UIAccordion.this.getColor(BODY_BACKGROUND_COLOR, InteractionState.DEFAULT, deltaTime);
                int borderColor = UIAccordion.this.getColor(BODY_BORDER_COLOR, InteractionState.DEFAULT, deltaTime);
                float radius = UIAccordion.this.getFloat(BODY_BORDER_RADIUS, InteractionState.DEFAULT, deltaTime);

                // Thickness is currently generic, but could be added as a key if needed.
                // We fetch it from the body's local style or fallback.
                float thickness = getFloat(ThemeProperties.BORDER_THICKNESS, InteractionState.DEFAULT, deltaTime);

                // Draw Border (Outline)
                if (thickness > 0 && (borderColor >>> 24) > 0) {
                    renderer.getGeometry().renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), borderColor, radius, thickness);
                }

                // Draw Background
                // We draw the rect slightly inset (or just standard) depending on preference.
                // Here we match standard UIPanel rendering.
                if ((bgColor >>> 24) > 0) {
                    renderer.getGeometry().renderRect(
                            this.getX() + thickness,
                            this.getY() + thickness,
                            this.getWidth() - (thickness * 2),
                            this.getHeight() - (thickness * 2),
                            bgColor,
                            Math.max(0, radius - thickness)
                    );
                }
            }
        };

        // 3. Initialize Scrollbar (Vertical)
        this.scrollBar = new UIScrollBar(this.body, ScrollOrientation.VERTICAL);
        // Style the scrollbar to be unobtrusive
        this.scrollBar.style()
                .set(UIScrollBar.TRACK_COLOR, 0x00000000)
                .set(UIScrollBar.THUMB_COLOR, 0x40FFFFFF)
                .set(UIScrollBar.ROUNDING, 2.0f)
                .set(UIScrollBar.PADDING, 2.0f);

        // Add components to the hierarchy
        // Order matters for Z-Index: Body first, then Header on top.
        this.add(body);
        this.add(scrollBar);
        this.add(header);

        setupStyles();
    }

    /**
     * Configures the header panel layout.
     */
    private void setupHeader() {
        header.setX(Layout.pixel(0));
        header.setY(Layout.pixel(0));
        header.setWidth(Layout.relative(1.0f)); // Header fills entire width
        header.setHeight(Layout.pixel(headerHeight));

        headerTitle.setX(Layout.pixel(24)); // Offset to make room for the arrow
        headerTitle.setY(Layout.center());  // Vertically centered
        header.add(headerTitle);
    }

    /**
     * Applies default styles to the widget and its children.
     */
    private void setupStyles() {
        this.style()
                .setTransitionSpeed(12.0f) // Fast snappy animation

                // Header colors
                .set(InteractionState.DEFAULT, HEADER_COLOR, 0xFF252525)
                .set(InteractionState.HOVER, HEADER_COLOR, 0xFF353535)

                // Arrow colors
                .set(InteractionState.DEFAULT, ARROW_COLOR, 0xFF888888)
                .set(InteractionState.HOVER, ARROW_COLOR, 0xFFFFFFFF)

                // Body Styles
                .set(InteractionState.DEFAULT, BODY_BACKGROUND_COLOR, 0xFF181818)
                .set(InteractionState.DEFAULT, BODY_BORDER_COLOR, 0xFF404040)
                .set(InteractionState.DEFAULT, BODY_BORDER_RADIUS, 4.0f);

        // --- Header Panel Styling ---
        header.style()
                .set(ThemeProperties.BORDER_RADIUS, 4.0f)
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // --- Body Panel Styling (Local properties used for Thickness) ---
        body.style()
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);
    }

    /**
     * Sets the title text displayed on the header.
     *
     * @param title The text string.
     * @return This instance.
     */
    public UIAccordion setTitle(String title) {
        this.headerTitle.setText(title);
        return this;
    }

    /**
     * Sets the title component displayed on the header.
     *
     * @param title The text component.
     * @return This instance.
     */
    public UIAccordion setTitle(TextComponent title) {
        this.headerTitle.setText(title);
        return this;
    }

    /**
     * Adds a content widget to the scrollable body of the accordion.
     * <p>
     * The widget will be added to the internal {@link UIScrollComponent}.
     * Use this method instead of {@code this.add()} for content.
     * </p>
     *
     * @param widget The widget to add.
     * @return This accordion instance.
     */
    public UIAccordion addContent(UIWidget widget) {
        body.add(widget);
        return this;
    }

    /**
     * Toggles the open/closed state of the accordion.
     * Triggers animations for both expansion and arrow rotation.
     */
    public void toggle() {
        setOpen(!isOpen);
    }

    /**
     * Explicitly sets the open state of the accordion.
     *
     * @param open True to expand, false to collapse.
     */
    public void setOpen(boolean open) {
        if (this.isOpen == open) return;
        this.isOpen = open;

        // Animate Expansion Progress (0.0 <-> 1.0)
        float targetProgress = isOpen ? 1.0f : 0.0f;
        this.animate()
                .keyframe(0.3f, EXPANSION_PROGRESS, targetProgress, Easing.EASE_IN_OUT_CUBIC)
                .start();

        // Animate Arrow Rotation (0 <-> 90)
        float targetRot = isOpen ? 90.0f : 0.0f;
        this.animate()
                .keyframe(0.3f, ARROW_ROTATION, targetRot, Easing.EASE_IN_OUT_CUBIC)
                .start();

        // If opened manually, ensure we grab focus
        if (isOpen) {
            this.isFocused = true;
        }
    }

    /**
     * Sets the maximum height of the content area when fully expanded.
     * If content exceeds this height, the scrollbar becomes active.
     *
     * @param height Pixels.
     * @return This instance.
     */
    public UIAccordion setMaxBodyHeight(float height) {
        this.maxBodyHeight = height;
        return this;
    }

    // =================================================================================
    // Interaction
    // =================================================================================

    /**
     * Handles mouse clicks.
     * Overridden to provide robust toggle logic (Single click) and proper focus management.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // 1. Propagate to children first (Content interactions)
        // If the accordion is open, we must allow clicking on buttons/sliders inside it.
        if (isOpen) {
            // Check scrollbar first (it floats on top)
            if (scrollBar.mouseClicked(mouseX, mouseY, button)) return true;

            // Check body content
            if (body.mouseClicked(mouseX, mouseY, button)) {
                // If content was clicked, we keep focus on the accordion tree
                this.isFocused = true;
                return true;
            }
        }

        // 2. Check header click (Toggle Logic)
        if (header.isMouseOver(mouseX, mouseY)) {
            if (button == 0) { // Left Click
                toggle();

                // Force focus on this accordion.
                // This ensures that clicking subsequently outside will trigger 'unfocus()'.
                this.isFocused = true;

                // Unfocus siblings to maintain one active element
                if (parent != null) {
                    for (UIWidget sibling : parent.getChildren()) {
                        if (sibling != this) sibling.unfocus();
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Called when this widget loses focus (e.g., user clicked somewhere else).
     * Overridden to automatically close the accordion.
     */
    @Override
    public void unfocus() {
        super.unfocus();

        // Auto-close behavior similar to a dropdown
        if (isOpen) {
            setOpen(false);
        }
    }

    // =================================================================================
    // Layout & Rendering
    // =================================================================================

    @Override
    public void layout() {
        // 1. Calculate animation progress
        float progress = style().getValue(InteractionState.DEFAULT, EXPANSION_PROGRESS);

        // 2. Retrieve the configured gap from styles
        float maxGap = style().getValue(InteractionState.DEFAULT, BODY_GAP);

        // Interpolate gap and body height based on progress so they vanish when closed
        float currentGap = maxGap * progress;
        float currentBodyHeight = maxBodyHeight * progress;

        // 3. Layout Header
        header.setHeight(Layout.pixel(headerHeight));
        header.layout();

        // 4. Layout Body
        // Position is offset by headerHeight + the dynamic gap
        body.setY(Layout.pixel(headerHeight + currentGap));
        body.setX(Layout.pixel(0));
        body.setHeight(Layout.pixel(currentBodyHeight));

        // 5. Configure Scrollbar visibility and layout logic
        float scrollBarWidth = 6.0f;

        if (currentBodyHeight > 1.0f) {
            scrollBar.setVisible(true);
            // Scrollbar Y must also account for the gap
            scrollBar.setY(Layout.pixel(headerHeight + currentGap + 2));
            scrollBar.setX(Layout.anchorEnd(2));
            scrollBar.setWidth(Layout.pixel(scrollBarWidth));
            scrollBar.setHeight(Layout.pixel(currentBodyHeight - 4));

            body.setWidth(Layout.relative(1.0f));
        } else {
            scrollBar.setVisible(false);
            body.setWidth(Layout.relative(1.0f));
        }

        // 6. Update Total Widget Height
        // The container height now includes the gap
        float totalHeight = headerHeight + currentGap + currentBodyHeight;
        this.height = totalHeight;
        this.heightConstraint = Layout.pixel(totalHeight);

        // Propagate layout to all children
        super.layout();
    }

    @Override
    public void render(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime) {
        // 1. Sync Header Style
        InteractionState state = header.isMouseOver(mouseX, mouseY) ? InteractionState.HOVER : InteractionState.DEFAULT;
        int headerBg = getColor(HEADER_COLOR, state, deltaTime);
        header.style().set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, headerBg);

        // 2. Trigger Real-time Layout Update during Animation
        float progress = style().getValue(InteractionState.DEFAULT, EXPANSION_PROGRESS);
        float target = isOpen ? 1.0f : 0.0f;

        if (Math.abs(progress - target) > 0.001f) {
            this.layout();
            if (this.parent != null) {
                // Notify parent to re-stack its children based on our new height
                this.parent.layout();
            }
        }

        // 3. Render children (Header, Body, Scrollbar)
        // super.render calls layout() on children and handles clipping
        super.render(renderer, mouseX, mouseY, partialTick, deltaTime);

        // 4. Draw Custom Arrow on top of everything
        drawArrow(renderer, deltaTime);
    }

    /**
     * Renders the rotating chevron arrow on top of the header.
     * Uses the GeometryRenderer's smooth vector arrow.
     */
    private void drawArrow(UIRenderer renderer, float deltaTime) {
        // Calculate current rotation and color
        float rotation = animManager.getAnimatedFloat(ARROW_ROTATION, isOpen ? 90f : 0f, style().getTransitionSpeed(), deltaTime);
        int color = getColor(ARROW_COLOR, header.isHovered() ? InteractionState.HOVER : InteractionState.DEFAULT, deltaTime);

        // Calculate center position for the arrow (12px from left edge)
        float arrowX = this.x + 12;
        float arrowY = this.y + (headerHeight / 2.0f);
        float arrowSize = 6.0f;

        renderer.pushMatrix();

        // Pivot rotation around the center of the arrow
        renderer.translate(arrowX, arrowY, 0);
        renderer.rotate(rotation, 0, 0, 1);

        // Render smooth vector arrow centered at (0,0) relative to matrix
        renderer.getGeometry().renderArrow(0, 0, arrowSize, color);

        renderer.popMatrix();
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.UIWidget;

import java.util.List;

/**
 * A specialized widget that renders a floating tooltip associated with a target widget.
 * Features:
 * - Automatic visibility management (hover delays, fading).
 * - Smart positioning to stay within screen bounds.
 * - Dynamic content resizing.
 * - Fully styled via UIProperties (timing, offsets, padding).
 *
 * Usage: Create the tooltip, set the target via {@link #setTarget(UIWidget)},
 * set content via {@link #setContent(TextComponent)}, then add the tooltip
 * to the root container (not the target itself) to ensure proper Z-layering.
 *
 * @author xI-Mx-Ix
 */
public class UITooltip extends UIPanel {

    // --- Tooltip Specific ThemeProperties ---

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
     *  Horizontal offset from the mouse cursor in pixels.
     */
    public static final StyleKey<Float> OFFSET_X = new StyleKey<>("tooltip_offset_x", 12.0f);

    /**
     * Vertical offset from the mouse cursor in pixels.
     */
    public static final StyleKey<Float> OFFSET_Y = new StyleKey<>("tooltip_offset_y", -12.0f);

    /**
     *  Padding around the text content inside the tooltip frame.
     */
    public static final StyleKey<Float> PADDING = new StyleKey<>("tooltip_padding", 5.0f);

    private UIWidget target;
    private final UIWrappedText contentText;

    // State Management
    private enum TooltipState {
        HIDDEN,
        WAITING_FOR_DELAY,
        FADING_IN,
        VISIBLE,
        FADING_OUT
    }

    private TooltipState state = TooltipState.HIDDEN;
    private float stateTimer = 0.0f;
    private float currentOpacity = 0.0f;

    /**
     * Constructs an empty tooltip.
     * You must call {@link #setTarget(UIWidget)} and {@link #setContent(TextComponent)}
     * before it will function.
     */
    public UITooltip() {
        this.contentText = new UIWrappedText();

        // Default Tooltip Styling
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xF0101010) // Dark semi-transparent
                .set(ThemeProperties.BORDER_COLOR, 0x505000FF)     // Purple-ish border
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))
                // Default specific properties
                .set(SHOW_DELAY, 0.5f)
                .set(FADE_IN_TIME, 0.2f)
                .set(FADE_OUT_TIME, 0.2f)
                .set(OFFSET_X, 12.0f)
                .set(OFFSET_Y, -12.0f)
                .set(PADDING, 5.0f);

        // Setup internal text widget
        // Positions are updated in layout() based on PADDING property
        this.add(this.contentText);

        // Tooltips are invisible by default (managed by opacity), but the widget remains "active" to update logic
        this.setVisible(true);
    }

    /**
     * Sets the widget that triggers this tooltip.
     *
     * @param target The target widget.
     * @return This tooltip instance.
     */
    public UITooltip setTarget(UIWidget target) {
        this.target = target;
        return this;
    }

    /**
     * Sets the content of the tooltip (supports wrapping).
     *
     * @param text The component text.
     * @return This tooltip instance.
     */
    public UITooltip setContent(TextComponent text) {
        this.contentText.setText(text);
        return this;
    }

    /**
     * Sets the content of the tooltip from a string literal.
     *
     * @param text The string text.
     * @return This tooltip instance.
     */
    public UITooltip setContent(String text) {
        this.contentText.setText(text);
        return this;
    }

    /**
     * Sets the content of the tooltip with multiple lines.
     *
     * @param lines List of components.
     * @return This tooltip instance.
     */
    public UITooltip setLines(List<TextComponent> lines) {
        this.contentText.setText(TextComponent.empty()); // Clear initial
        for (TextComponent line : lines) {
            this.contentText.addText(line, false);
        }
        return this;
    }

    /**
     * Configures the hover delay via the style system.
     * @param seconds Time in seconds.
     */
    public UITooltip setDelay(float seconds) {
        this.style().set(InteractionState.DEFAULT, SHOW_DELAY, seconds);
        return this;
    }

    /**
     * Configures the fade animation durations via the style system.
     * @param fadeIn  Fade in duration in seconds.
     * @param fadeOut Fade out duration in seconds.
     */
    public UITooltip setFadeTimes(float fadeIn, float fadeOut) {
        this.style().set(InteractionState.DEFAULT, FADE_IN_TIME, fadeIn);
        this.style().set(InteractionState.DEFAULT, FADE_OUT_TIME, fadeOut);
        return this;
    }

    /**
     * Configures the mouse cursor offset via the style system.
     */
    public UITooltip setOffset(float x, float y) {
        this.style().set(InteractionState.DEFAULT, OFFSET_X, x);
        this.style().set(InteractionState.DEFAULT, OFFSET_Y, y);
        return this;
    }

    @Override
    public void layout() {
        // Retrieve dynamic padding from style
        float pad = style().getValue(InteractionState.DEFAULT, PADDING);

        // Update internal text position
        this.contentText.setX(Layout.pixel(pad));
        this.contentText.setY(Layout.pixel(pad));

        // 1. Let the text calculate its required size first
        this.contentText.layout();

        // 2. Resize this panel to fit the text plus padding
        float requiredWidth = this.contentText.getWidth() + (pad * 2);
        float requiredHeight = this.contentText.getHeight() + (pad * 2);

        this.setWidth(Layout.pixel(requiredWidth));
        this.setHeight(Layout.pixel(requiredHeight));

        // 3. Update super layout (positions children)
        super.layout();
    }

    /**
     * Overrides the main render method to handle tooltip logic (positioning, opacity, Z-ordering).
     * This avoids recursion issues by wrapping the call to super.render().
     */
    @Override
    public void render(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime) {
        // If no target is set, do not process logic
        if (target == null) return;

        // Update Logic using logic coordinates passed by UIContext
        updateState(deltaTime, mouseX, mouseY);

        // Early exit if completely invisible
        if (currentOpacity <= 0.001f) {
            return;
        }

        // Dynamic Positioning (Smart Placement)
        calculateSmartPosition(mouseX, mouseY);

        // Apply Opacity to Style for rendering
        float previousOpacity = style().getValue(InteractionState.DEFAULT, ThemeProperties.OPACITY);
        style().set(InteractionState.DEFAULT, ThemeProperties.OPACITY, currentOpacity);

        // Render Background (UIPanel logic)
        // We must manually apply opacity to colors because UIPanel pulls raw colors
        int baseBg = style().getValue(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR);
        int baseBorder = style().getValue(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR);
        int baseText = contentText.style().getValue(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR);

        // Temporarily inject alpha-modded colors into the style for drawSelf
        style().set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, applyAlpha(baseBg, currentOpacity));
        style().set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, applyAlpha(baseBorder, currentOpacity));
        contentText.style().set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, applyAlpha(baseText, currentOpacity));

        // Render Self (Background via drawSelf) and Children (Text)
        // We push Z translation to ensure tooltip is on top of everything
        renderer.translate(0, 0, 500.0f);

        // IMPORTANT: Call super.render, NOT super.drawSelf here.
        // super.render calls this.drawSelf (for background) AND renders children (text).
        super.render(renderer, mouseX, mouseY, partialTicks, deltaTime);

        renderer.translate(0, 0, -500.0f);

        // Restore original style values
        style().set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, baseBg);
        style().set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, baseBorder);
        style().set(InteractionState.DEFAULT, ThemeProperties.OPACITY, previousOpacity);
        contentText.style().set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, baseText);
    }

    /**
     * Draws the background and border of the tooltip.
     * Delegates to UIPanel implementation to avoid code duplication and recursion.
     */
    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);
    }

    private void updateState(float dt, int mouseX, int mouseY) {
        // Check if target is currently hovered using logical coordinates
        boolean targetHovered = target.isVisible() && target.isMouseOver(mouseX, mouseY);

        // Retrieve timing from style
        float delay = style().getValue(InteractionState.DEFAULT, SHOW_DELAY);
        float fadeInTime = style().getValue(InteractionState.DEFAULT, FADE_IN_TIME);
        float fadeOutTime = style().getValue(InteractionState.DEFAULT, FADE_OUT_TIME);

        switch (state) {
            case HIDDEN:
                if (targetHovered) {
                    state = TooltipState.WAITING_FOR_DELAY;
                    stateTimer = 0.0f;
                }
                break;

            case WAITING_FOR_DELAY:
                if (!targetHovered) {
                    state = TooltipState.HIDDEN;
                } else {
                    stateTimer += dt;
                    if (stateTimer >= delay) {
                        state = TooltipState.FADING_IN;
                        stateTimer = 0.0f;
                    }
                }
                break;

            case FADING_IN:
                if (!targetHovered) {
                    state = TooltipState.FADING_OUT;
                    // Invert timer for smooth transition
                    float progress = currentOpacity; // 0 to 1
                    stateTimer = (1.0f - progress) * fadeOutTime;
                } else {
                    stateTimer += dt;
                    currentOpacity = Math.min(1.0f, stateTimer / fadeInTime);
                    if (currentOpacity >= 1.0f) {
                        state = TooltipState.VISIBLE;
                        currentOpacity = 1.0f;
                    }
                }
                break;

            case VISIBLE:
                if (!targetHovered) {
                    state = TooltipState.FADING_OUT;
                    stateTimer = 0.0f;
                }
                break;

            case FADING_OUT:
                if (targetHovered) {
                    // Resume fading in from current alpha
                    state = TooltipState.FADING_IN;
                    float progress = currentOpacity;
                    stateTimer = progress * fadeInTime;
                } else {
                    stateTimer += dt;
                    float progress = stateTimer / fadeOutTime;
                    currentOpacity = Math.max(0.0f, 1.0f - progress);
                    if (currentOpacity <= 0.0f) {
                        state = TooltipState.HIDDEN;
                        currentOpacity = 0.0f;
                    }
                }
                break;
        }
    }

    /**
     * Calculates the position of the tooltip based on mouse coordinates.
     */
    private void calculateSmartPosition(int mouseX, int mouseY) {
        // Retrieve offsets from style
        float offX = style().getValue(InteractionState.DEFAULT, OFFSET_X);
        float offY = style().getValue(InteractionState.DEFAULT, OFFSET_Y);

        // Start with offset position
        float newX = mouseX + offX;
        float newY = mouseY + offY;

        // Check Right Edge
        if (newX + width > getScreenWidth() - 5) {
            newX = mouseX - width - offX; // Flip to left
        }
        // Check Bottom Edge
        if (newY + height > getScreenHeight() - 5) {
            newY = mouseY - height; // Flip upwards
        }

        // Clamp Left/Top
        if (newX < 5) newX = 5;
        if (newY < 5) newY = 5;

        // Update constraints directly for the rendering pass
        this.xConstraint = Layout.pixel(newX);
        this.yConstraint = Layout.pixel(newY);

        // Update layout immediately for self and children
        // Do NOT loop over children here; layout() propagates automatically.
        super.layout();
    }

    private int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color & 0xFF);

        a = (int) (a * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
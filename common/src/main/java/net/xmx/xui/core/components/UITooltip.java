/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.gl.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIProperty;
import net.xmx.xui.core.style.UIState;

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
 * set content via {@link #setContent(Component)}, then add the tooltip
 * to the root container (not the target itself) to ensure proper Z-layering.
 *
 * @author xI-Mx-Ix
 */
public class UITooltip extends UIPanel {

    // --- Tooltip Specific Properties ---

    /** Time in seconds the mouse must hover before the tooltip appears. */
    public static final UIProperty<Float> SHOW_DELAY = new UIProperty<>("tooltip_show_delay", 0.5f);

    /** Duration of the fade-in animation in seconds. */
    public static final UIProperty<Float> FADE_IN_TIME = new UIProperty<>("tooltip_fade_in_time", 0.2f);

    /** Duration of the fade-out animation in seconds. */
    public static final UIProperty<Float> FADE_OUT_TIME = new UIProperty<>("tooltip_fade_out_time", 0.2f);

    /** Horizontal offset from the mouse cursor in pixels. */
    public static final UIProperty<Float> OFFSET_X = new UIProperty<>("tooltip_offset_x", 12.0f);

    /** Vertical offset from the mouse cursor in pixels. */
    public static final UIProperty<Float> OFFSET_Y = new UIProperty<>("tooltip_offset_y", -12.0f);

    /** Padding around the text content inside the tooltip frame. */
    public static final UIProperty<Float> PADDING = new UIProperty<>("tooltip_padding", 5.0f);

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
     * You must call {@link #setTarget(UIWidget)} and {@link #setContent(Component)}
     * before it will function.
     */
    public UITooltip() {
        this.contentText = new UIWrappedText();

        // Default Tooltip Styling
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0xF0101010) // Dark semi-transparent
                .set(Properties.BORDER_COLOR, 0x505000FF)     // Purple-ish border
                .set(Properties.BORDER_THICKNESS, 1.0f)
                .set(Properties.BORDER_RADIUS, 4.0f)
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
    public UITooltip setContent(Component text) {
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
    public UITooltip setLines(List<Component> lines) {
        this.contentText.setText(Component.empty()); // Clear initial
        for (Component line : lines) {
            this.contentText.addText(line, false);
        }
        return this;
    }

    /**
     * Configures the hover delay via the style system.
     * @param seconds Time in seconds.
     */
    public UITooltip setDelay(float seconds) {
        this.style().set(UIState.DEFAULT, SHOW_DELAY, seconds);
        return this;
    }

    /**
     * Configures the fade animation durations via the style system.
     * @param fadeIn  Fade in duration in seconds.
     * @param fadeOut Fade out duration in seconds.
     */
    public UITooltip setFadeTimes(float fadeIn, float fadeOut) {
        this.style().set(UIState.DEFAULT, FADE_IN_TIME, fadeIn);
        this.style().set(UIState.DEFAULT, FADE_OUT_TIME, fadeOut);
        return this;
    }

    /**
     * Configures the mouse cursor offset via the style system.
     */
    public UITooltip setOffset(float x, float y) {
        this.style().set(UIState.DEFAULT, OFFSET_X, x);
        this.style().set(UIState.DEFAULT, OFFSET_Y, y);
        return this;
    }

    @Override
    public void layout() {
        // Retrieve dynamic padding from style
        float pad = style().getValue(UIState.DEFAULT, PADDING);

        // Update internal text position
        this.contentText.setX(Constraints.pixel(pad));
        this.contentText.setY(Constraints.pixel(pad));

        // 1. Let the text calculate its required size first
        this.contentText.layout();

        // 2. Resize this panel to fit the text plus padding
        float requiredWidth = this.contentText.getWidth() + (pad * 2);
        float requiredHeight = this.contentText.getHeight() + (pad * 2);

        this.setWidth(Constraints.pixel(requiredWidth));
        this.setHeight(Constraints.pixel(requiredHeight));

        // 3. Update super layout (positions children)
        super.layout();
    }

    @Override
    public void render(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks) {
        // If no target is set, do not process logic
        if (target == null) return;

        // Update Logic using logic coordinates passed by UIContext
        updateState(partialTicks, mouseX, mouseY);

        // Early exit if completely invisible
        if (currentOpacity <= 0.001f) {
            return;
        }

        // Dynamic Positioning (Smart Placement)
        calculateSmartPosition(mouseX, mouseY);

        // Apply Opacity to Style for rendering
        float previousOpacity = style().getValue(UIState.DEFAULT, Properties.OPACITY);
        style().set(UIState.DEFAULT, Properties.OPACITY, currentOpacity);

        // Render Background (UIPanel logic)
        // We must manually apply opacity to colors because UIPanel pulls raw colors
        int baseBg = style().getValue(UIState.DEFAULT, Properties.BACKGROUND_COLOR);
        int baseBorder = style().getValue(UIState.DEFAULT, Properties.BORDER_COLOR);
        int baseText = contentText.style().getValue(UIState.DEFAULT, Properties.TEXT_COLOR);

        // Temporarily inject alpha-modded colors into the style for drawSelf
        style().set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, applyAlpha(baseBg, currentOpacity));
        style().set(UIState.DEFAULT, Properties.BORDER_COLOR, applyAlpha(baseBorder, currentOpacity));
        contentText.style().set(UIState.DEFAULT, Properties.TEXT_COLOR, applyAlpha(baseText, currentOpacity));

        // Render Self and Children
        // We push Z translation to ensure tooltip is on top
        renderer.translate(0, 0, 500.0f);
        super.render(renderer, mouseX, mouseY, partialTicks);
        renderer.translate(0, 0, -500.0f);

        // Restore original style values
        style().set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, baseBg);
        style().set(UIState.DEFAULT, Properties.BORDER_COLOR, baseBorder);
        style().set(UIState.DEFAULT, Properties.OPACITY, previousOpacity);
        contentText.style().set(UIState.DEFAULT, Properties.TEXT_COLOR, baseText);
    }

    private void updateState(float dt, int mouseX, int mouseY) {
        // Check if target is currently hovered using logical coordinates
        boolean targetHovered = target.isVisible() && target.isMouseOver(mouseX, mouseY);

        // Retrieve timing from style
        float delay = style().getValue(UIState.DEFAULT, SHOW_DELAY);
        float fadeInTime = style().getValue(UIState.DEFAULT, FADE_IN_TIME);
        float fadeOutTime = style().getValue(UIState.DEFAULT, FADE_OUT_TIME);

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
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Retrieve offsets from style
        float offX = style().getValue(UIState.DEFAULT, OFFSET_X);
        float offY = style().getValue(UIState.DEFAULT, OFFSET_Y);

        // Start with offset position
        float newX = mouseX + offX;
        float newY = mouseY + offY;

        // Check Right Edge
        if (newX + width > screenWidth - 5) {
            newX = mouseX - width - offX; // Flip to left
        }
        // Check Bottom Edge
        if (newY + height > screenHeight - 5) {
            newY = mouseY - height; // Flip upwards
        }

        // Clamp Left/Top
        if (newX < 5) newX = 5;
        if (newY < 5) newY = 5;

        // Update constraints directly for the rendering pass
        this.x = newX;
        this.y = newY;

        // Update children positions relative to new X/Y
        for (UIWidget child : children) {
            this.xConstraint = Constraints.pixel(newX);
            this.yConstraint = Constraints.pixel(newY);
            super.layout();
        }
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
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

import java.util.List;

/**
 * A specialized widget that renders a floating tooltip associated with a target widget.
 * Features:
 * - Automatic visibility management (hover delays, fading).
 * - Smart positioning to stay within screen bounds.
 * - Dynamic content resizing.
 * - Configurable timing and offsets.
 *
 * Usage: Create the tooltip, set the target via {@link #setTarget(UIWidget)},
 * set content via {@link #setContent(Component)}, then add the tooltip
 * to the root container (not the target itself) to ensure proper Z-layering.
 *
 * @author xI-Mx-Ix
 */
public class UITooltip extends UIPanel {

    private UIWidget target;
    private final UIWrappedText contentText;

    // Timing Configuration (in seconds for consistency with XUI dt)
    private float delay = 0.5f;
    private float fadeInTime = 0.2f;
    private float fadeOutTime = 0.2f;

    // Positioning Configuration
    private float offsetX = 12.0f;
    private float offsetY = -12.0f;
    private float padding = 5.0f;

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

        // Setup internal text widget
        this.contentText.setX(Constraints.pixel(padding));
        this.contentText.setY(Constraints.pixel(padding));
        this.add(this.contentText);

        // Default Tooltip Styling
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0xF0101010) // Dark semi-transparent
                .set(Properties.BORDER_COLOR, 0x505000FF)     // Purple-ish border
                .set(Properties.BORDER_THICKNESS, 1.0f)
                .set(Properties.BORDER_RADIUS, 4.0f);

        // Tooltips are invisible by default and do not block input
        this.setVisible(true); // Logic handles opacity, widget remains "active" to update
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
     * Sets the hover delay before the tooltip appears.
     * @param seconds Time in seconds.
     */
    public UITooltip setDelay(float seconds) {
        this.delay = seconds;
        return this;
    }

    /**
     * Sets the fade animation durations.
     * @param fadeIn  Fade in duration in seconds.
     * @param fadeOut Fade out duration in seconds.
     */
    public UITooltip setFadeTimes(float fadeIn, float fadeOut) {
        this.fadeInTime = fadeIn;
        this.fadeOutTime = fadeOut;
        return this;
    }

    /**
     * Sets the mouse cursor offset.
     */
    public UITooltip setOffset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    @Override
    public void layout() {
        // 1. Let the text calculate its required size first
        this.contentText.layout();

        // 2. Resize this panel to fit the text plus padding
        float requiredWidth = this.contentText.getWidth() + (padding * 2);
        float requiredHeight = this.contentText.getHeight() + (padding * 2);

        this.setWidth(Constraints.pixel(requiredWidth));
        this.setHeight(Constraints.pixel(requiredHeight));

        // 3. Update super layout (positions children)
        super.layout();
    }

    @Override
    public void render(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks) {
        // If no target is set, do not process logic
        if (target == null) return;

        // Update Logic
        updateState(partialTicks);

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
        renderer.translateZ(500.0f);
        super.render(renderer, mouseX, mouseY, partialTicks);
        renderer.translateZ(-500.0f);

        // Restore original style values
        style().set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, baseBg);
        style().set(UIState.DEFAULT, Properties.BORDER_COLOR, baseBorder);
        style().set(UIState.DEFAULT, Properties.OPACITY, previousOpacity);
        contentText.style().set(UIState.DEFAULT, Properties.TEXT_COLOR, baseText);
    }

    private void updateState(float dt) {
        // Check if target is currently hovered
        boolean targetHovered = target.isVisible() && isMouseOverTarget();

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
     * Checks if the mouse is currently within the target widget's bounds.
     */
    private boolean isMouseOverTarget() {
        double mx = Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth();
        double my = Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight();

        return target.isMouseOver(mx, my);
    }

    /**
     * Calculates the position of the tooltip based on mouse coordinates.
     */
    private void calculateSmartPosition(int mouseX, int mouseY) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Start with offset position
        float newX = mouseX + offsetX;
        float newY = mouseY + offsetY;

        // Check Right Edge
        if (newX + width > screenWidth - 5) {
            newX = mouseX - width - offsetX; // Flip to left
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
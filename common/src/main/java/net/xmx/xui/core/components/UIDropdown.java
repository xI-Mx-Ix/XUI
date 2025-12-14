/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Modern Dropdown Widget.
 * <p>
 * This component features a header that behaves like a button and a floating
 * list overlay that animates open/close. The overlay is rendered visually distinct
 * from the header (floating card style) with a configurable gap.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIDropdown extends UIWidget implements UIWidget.WidgetObstructor {

    /**
     * Defines the expansion direction behavior.
     */
    public enum Direction {
        /**
         * Automatically determines the best direction based on available screen space.
         * <p>
         * Logic:
         * 1. Try opening downwards.
         * 2. If space is insufficient, try opening upwards.
         * 3. If neither fits, open towards the side with the most available space.
         * </p>
         */
        AUTO,

        /**
         * Always opens downwards, regardless of available space.
         */
        DOWN,

        /**
         * Always opens upwards, regardless of available space.
         */
        UP
    }

    /**
     * Color of the chevron/arrow indicator.
     */
    public static final StyleKey<Integer> ARROW_COLOR = new StyleKey<>("dropdown_arrow_color", 0xFFAAAAAA);

    // List Logic
    private final List<TextComponent> options = new ArrayList<>();
    private int selectedIndex = -1;
    private Consumer<Integer> onSelected;

    // State & Animation
    private boolean isOpen = false;
    private boolean active = true;

    /**
     * Current animation progress for the opening effect.
     * 0.0f = fully closed, 1.0f = fully open.
     */
    private float openProgress = 0.0f;

    // Layout Configuration
    private Direction preferredDirection = Direction.AUTO;
    private boolean openUpward = false;
    private final int optionHeight = 24;
    private final float dropdownGap = 4.0f;

    // Geometry Cache (Calculated in drawSelf for obstruction checks)
    private float overlayX, overlayY, overlayWidth, overlayHeight;

    /**
     * Constructs an empty dropdown with modern styling.
     */
    public UIDropdown() {
        setupModernStyles();
    }

    /**
     * Configures the visual theme properties.
     */
    private void setupModernStyles() {
        this.style()
                .setTransitionSpeed(10.0f)

                // Header - Default
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, 6.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFE0E0E0)
                .set(InteractionState.DEFAULT, ARROW_COLOR, 0xFF909090)

                // Hover overlay for items (lighter white)
                .set(InteractionState.DEFAULT, ThemeProperties.HOVER_COLOR, 0x1AFFFFFF)

                // Header - Hover
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFF303030)
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFF606060)
                .set(InteractionState.HOVER, ARROW_COLOR, 0xFFFFFFFF)

                // Header - Active/Open
                .set(InteractionState.ACTIVE, ThemeProperties.BACKGROUND_COLOR, 0xFF181818);
    }

    /**
     * Adds an option to the list.
     *
     * @param option The text component.
     * @return This instance.
     */
    public UIDropdown addOption(TextComponent option) {
        this.options.add(option);
        if (this.selectedIndex == -1) selectedIndex = 0;
        return this;
    }

    /**
     * Replaces all options.
     *
     * @param options The new list of options.
     * @return This instance.
     */
    public UIDropdown setOptions(List<TextComponent> options) {
        this.options.clear();
        this.options.addAll(options);
        this.selectedIndex = options.isEmpty() ? -1 : 0;
        return this;
    }

    /**
     * Sets the direction behavior for the dropdown.
     *
     * @param direction The expansion direction (AUTO, UP, or DOWN).
     * @return This instance for chaining.
     */
    public UIDropdown setDirection(Direction direction) {
        this.preferredDirection = direction;
        return this;
    }

    @Override
    protected void drawSelf(RenderInterface renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Update Animation Logic (Simple Lerp)
        float targetProgress = isOpen ? 1.0f : 0.0f;
        // Smoothly interpolate current progress towards target
        this.openProgress += (targetProgress - this.openProgress) * deltaTime * 15.0f;

        // Clamp to avoid float precision issues
        if (Math.abs(targetProgress - this.openProgress) < 0.001f) {
            this.openProgress = targetProgress;
        }

        // 2. Resolve Styles
        // Force hover state if open to give visual feedback on the header
        InteractionState headerState = isOpen ? InteractionState.HOVER : state;

        int headerBg = getColor(ThemeProperties.BACKGROUND_COLOR, headerState, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, headerState, deltaTime);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, headerState, deltaTime);
        int arrowColor = getColor(ARROW_COLOR, headerState, deltaTime);
        float radius = getFloat(ThemeProperties.BORDER_RADIUS, headerState, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, headerState, deltaTime);

        // 3. Draw Header (The Button)
        renderer.drawRect(x, y, width, height, headerBg, radius);
        if (borderThick > 0) {
            renderer.drawOutline(x, y, width, height, borderColor, radius, borderThick);
        }

        // Draw Selected Text
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            float textY = y + (height - TextComponent.getFontHeight()) / 2.0f + 1;
            // Limit text width to avoid overlapping arrow
            renderer.drawText(options.get(selectedIndex), x + 8, textY, textColor, false);
        }

        // Draw Chevron
        drawArrow(renderer, x + width - 14, y + height / 2.0f, arrowColor, isOpen);

        // 4. Draw Floating List Overlay (if visible)
        if (openProgress > 0.01f) {
            updateOverlayGeometry();

            renderer.translate(0, 0, 50);

            // We use the same border/radius style for consistency, but a solid background
            int listBg = 0xFF181818; // Very dark grey, opaque
            renderOverlay(renderer, mouseX, mouseY, listBg, borderColor, textColor, radius, borderThick);

            renderer.translate(0, 0, -50);
        }
    }

    /**
     * Calculates the direction in which the dropdown opens.
     * This is executed once at the start of the opening animation to ensure stability.
     */
    private void calculateDirection() {
        // Logic: Fixed Up
        if (preferredDirection == Direction.UP) {
            this.openUpward = true;
            return;
        }

        // Logic: Fixed Down
        if (preferredDirection == Direction.DOWN) {
            this.openUpward = false;
            return;
        }

        // Logic: Auto - Calculate available space
        int totalListHeight = options.size() * optionHeight;
        int screenHeight = getScreenHeight();

        float spaceBelow = screenHeight - (this.y + this.height + dropdownGap);
        float spaceAbove = this.y - dropdownGap;

        // 1. Default: Open downwards if sufficient space exists
        if (spaceBelow >= totalListHeight) {
            this.openUpward = false;
        }
        // 2. Fallback: Open upwards if down fails but up fits
        else if (spaceAbove >= totalListHeight) {
            this.openUpward = true;
        }
        // 3. Last Resort: Choose the side with more space
        else {
            this.openUpward = (spaceAbove > spaceBelow);
        }
    }

    private void updateOverlayGeometry() {
        int totalListHeight = options.size() * optionHeight;

        this.overlayWidth = this.width;
        // The actual rendered height depends on animation progress
        this.overlayHeight = totalListHeight * openProgress;
        this.overlayX = this.x;

        if (openUpward) {
            // Grows upwards from the bottom
            this.overlayY = (this.y - dropdownGap) - this.overlayHeight;
        } else {
            // Grows downwards from the top
            this.overlayY = this.y + this.height + dropdownGap;
        }
    }

    private void renderOverlay(RenderInterface renderer, int mouseX, int mouseY,
                               int bgColor, int borderColor, int textColor, float radius, float borderThick) {

        float totalListHeight = options.size() * optionHeight;

        // --- Radius Clamping Logic ---
        // Prevent graphical artifacts when the box height is smaller than 2x radius.
        float effectiveRadius = Math.min(radius, overlayHeight / 2.0f);

        // --- 1. Clipping (Scissors) ---
        // We define a window that matches the current animated box size.
        renderer.enableScissor(overlayX, overlayY, overlayWidth, overlayHeight);

        // --- 2. Draw Background ---
        renderer.drawRect(overlayX, overlayY, overlayWidth, overlayHeight, bgColor, effectiveRadius);

        // --- 3. Draw Options ---
        int hoverColor = style().getValue(InteractionState.DEFAULT, ThemeProperties.HOVER_COLOR);

        // If opening upward, we align content to the bottom of the overlay rect
        float startY = openUpward ? (overlayY + overlayHeight - totalListHeight) : overlayY;

        for (int i = 0; i < options.size(); i++) {
            float optY = startY + (i * optionHeight);

            // Check hover relative to the visual list and scissor bounds
            boolean isHovered = (mouseX >= overlayX && mouseX <= overlayX + overlayWidth &&
                    mouseY >= optY && mouseY < optY + optionHeight &&
                    mouseY >= overlayY && mouseY <= overlayY + overlayHeight);

            if (isHovered) {
                renderer.drawRect(overlayX + 2, optY, overlayWidth - 4, optionHeight, hoverColor, effectiveRadius / 2);
            }

            float textY = optY + (optionHeight - TextComponent.getFontHeight()) / 2.0f + 1;
            renderer.drawText(options.get(i), overlayX + 8, textY, textColor, false);
        }

        // --- 4. Draw Border ---
        if (borderThick > 0) {
            renderer.drawOutline(overlayX, overlayY, overlayWidth, overlayHeight, borderColor, effectiveRadius, borderThick);
        }

        // Disable clipping
        renderer.disableScissor();
    }

    private void drawArrow(RenderInterface renderer, float ax, float ay, int color, boolean pointUp) {
        float vDir = pointUp ? -1 : 1;

        // Draw chevron using small rect steps (pixel art style)
        // Center point
        renderer.drawRect(ax, ay + (2 * vDir), 1.5f, 1, color, 0);
        // Middle steps
        renderer.drawRect(ax - 1, ay + (1 * vDir), 1, 1, color, 0);
        renderer.drawRect(ax + 1.5f, ay + (1 * vDir), 1, 1, color, 0);
        // Outer tips
        renderer.drawRect(ax - 2, ay, 1, 1, color, 0);
        renderer.drawRect(ax + 2.5f, ay, 1, 1, color, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || button != 0) return false;

        // 1. Handle clicks in the overlay (only if fully or partially open)
        if (isOpen && openProgress > 0.1f) {
            // Check if mouse is within the VISIBLE part of the overlay
            if (mouseX >= overlayX && mouseX <= overlayX + overlayWidth &&
                    mouseY >= overlayY && mouseY <= overlayY + overlayHeight) {

                float totalListHeight = options.size() * optionHeight;
                float startY = openUpward ? (overlayY + overlayHeight - totalListHeight) : overlayY;

                float relativeY = (float) mouseY - startY;
                int index = (int) (relativeY / optionHeight);

                if (index >= 0 && index < options.size()) {
                    setSelectedIndex(index);
                    if (onSelected != null) onSelected.accept(index);
                    closeDropdown();
                    return true;
                }
            }

            // Close if clicked outside
            if (!isMouseOver(mouseX, mouseY)) {
                closeDropdown();
                return true;
            }
        }

        // 2. Handle Header Click
        if (isMouseOver(mouseX, mouseY)) {
            if (active) {
                toggleDropdown();
                return true;
            }
        }

        return false;
    }

    private void toggleDropdown() {
        if (isOpen) closeDropdown();
        else openDropdown();
    }

    private void openDropdown() {
        if (!isOpen) {
            // IMPORTANT: Calculate direction ONLY when opening.
            // This prevents the direction from flipping during the closing animation
            // when space calculations might differ due to shrinking dimensions.
            calculateDirection();

            this.isOpen = true;
            UIWidget.addObstructor(this);
        }
    }

    private void closeDropdown() {
        if (isOpen) {
            this.isOpen = false;
            UIWidget.removeObstructor(this);
        }
    }

    @Override
    public boolean isObstructing(double mouseX, double mouseY) {
        if (!isOpen) return false;
        return mouseX >= overlayX && mouseX <= overlayX + overlayWidth &&
                mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
    }

    // --- Getters / Setters ---

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
    }

    public UIDropdown setOnSelected(Consumer<Integer> onSelected) {
        this.onSelected = onSelected;
        return this;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
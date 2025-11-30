/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components;

import net.minecraft.client.Minecraft;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A Dropdown Widget (ComboBox) that displays a list of selectable options.
 * The header reacts to hover events by brightening, while the opened list maintains
 * a static background color until individual options are hovered.
 *
 * @author xI-Mx-Ix
 */
public class UIDropdown extends UIWidget implements UIWidget.WidgetObstructor {

    private final List<String> options = new ArrayList<>();

    // Tracks the animation progress (0.0f to 1.0f) for each option index
    private final Map<Integer, Float> optionAnimators = new HashMap<>();

    private int selectedIndex = -1;
    private boolean isOpen = false;
    private Consumer<Integer> onSelected;

    private boolean openUpward = false;
    private final int optionHeight = 20;

    private boolean active = true;

    // Cache for calculated overlay position to check for obstructions
    private float overlayY, overlayHeight;

    public UIDropdown(List<String> options) {
        this.options.addAll(options);
        if (!this.options.isEmpty()) {
            this.selectedIndex = 0;
        }
        setupDefaultStyles();
    }

    private void setupDefaultStyles() {
        this.style()
                .setTransitionSpeed(15.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0xFF202020)
                .set(UIState.DEFAULT, Properties.BORDER_COLOR, 0xFFFFFFFF)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 4.0f)
                .set(UIState.DEFAULT, Properties.BORDER_THICKNESS, 1.0f)
                .set(UIState.DEFAULT, Properties.TEXT_COLOR, 0xFFE0E0E0)
                .set(UIState.DEFAULT, Properties.ARROW_COLOR, 0xFFAAAAAA)

                // This color is drawn over the option to make it "lighter".
                // 0x40 is alpha (approx 25% opacity). White overlay = lighter background.
                .set(UIState.DEFAULT, Properties.HOVER_COLOR, 0x40FFFFFF)

                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xFF303030)
                .set(UIState.HOVER, Properties.ARROW_COLOR, 0xFFFFFFFF)

                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0xFF101010);
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        updateOverlayGeometry();

        // Calculate the background color for the header based on the current interaction state (e.g. Hover)
        int headerBgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);

        // Calculate the background color for the list overlay.
        // This always uses the DEFAULT state to ensure the list remains dark even when the header is hovered.
        int listBgColor = getColor(Properties.BACKGROUND_COLOR, UIState.DEFAULT, partialTicks);

        int borderColor = getColor(Properties.BORDER_COLOR, state, partialTicks);
        int textColor = getColor(Properties.TEXT_COLOR, state, partialTicks);
        int arrowColor = getColor(Properties.ARROW_COLOR, state, partialTicks);
        float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
        float borderThick = getFloat(Properties.BORDER_THICKNESS, state, partialTicks);

        float rTL = radius, rTR = radius, rBR = radius, rBL = radius;

        if (isOpen) {
            if (openUpward) {
                // List above: Flatten Top corners of the header
                rTL = 0;
                rTR = 0;
            } else {
                // List below: Flatten Bottom corners of the header
                rBR = 0;
                rBL = 0;
            }
        }

        // Draw the Header using the dynamic headerBgColor
        renderer.drawRect(x, y, width, height, headerBgColor, rTL, rTR, rBR, rBL);
        if (borderThick > 0) {
            renderer.drawOutline(x, y, width, height, borderColor, borderThick, rTL, rTR, rBR, rBL);
        }

        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            float textY = y + (height - renderer.getFontHeight()) / 2.0f;
            renderer.drawString(options.get(selectedIndex), x + 5, textY, textColor, false);
        }

        drawArrow(renderer, x + width - 12, y + (height - 5) / 2.0f, arrowColor, isOpen);

        if (isOpen) {
            // Render the options overlay on top of everything
            renderer.translateZ(300.0f);
            // Pass the static listBgColor for the overlay background
            renderDropdownOverlay(renderer, mouseX, mouseY, partialTicks, listBgColor, borderColor, textColor, radius);
            renderer.translateZ(-300.0f);
        }
    }

    private void updateOverlayGeometry() {
        int totalHeight = options.size() * optionHeight;
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float spaceBelow = screenHeight - (this.y + this.height);

        this.openUpward = (this.y > spaceBelow) && (totalHeight > spaceBelow);
        this.overlayHeight = totalHeight;

        if (openUpward) {
            this.overlayY = this.y - totalHeight + 1;
        } else {
            this.overlayY = this.y + this.height - 1;
        }
    }

    /**
     * Renders the expanded list of options.
     *
     * @param renderer     The rendering interface.
     * @param mouseX       Absolute mouse X.
     * @param mouseY       Absolute mouse Y.
     * @param partialTicks Animation time delta.
     * @param bgColor      The background color for the list (usually UIState.DEFAULT).
     * @param borderColor  The border color.
     * @param textColor    The text color.
     * @param radius       The border radius to apply to the outer corners.
     */
    private void renderDropdownOverlay(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks,
                                       int bgColor, int borderColor, int textColor, float radius) {

        float rTL = radius, rTR = radius, rBR = radius, rBL = radius;

        if (openUpward) {
            rBR = 0;
            rBL = 0;
        } else {
            rTL = 0;
            rTR = 0;
        }

        // Ensure background is solid (opaque) for the list so things behind don't bleed through
        int solidBg = bgColor | 0xFF000000;

        renderer.drawRect(x, overlayY, width, overlayHeight, solidBg, rTL, rTR, rBR, rBL);
        renderer.drawOutline(x, overlayY, width, overlayHeight, borderColor, 1.0f, rTL, rTR, rBR, rBL);

        // Retrieve the base hover overlay color (usually semi-transparent white)
        int baseHoverColor = style().getValue(UIState.DEFAULT, Properties.HOVER_COLOR);

        for (int i = 0; i < options.size(); i++) {
            float optY = overlayY + (i * optionHeight);

            boolean isOptionHovered = (mouseX >= x && mouseX <= x + width &&
                    mouseY >= optY && mouseY < optY + optionHeight);

            // Draw the highlight immediately if the option is hovered, without interpolation
            if (isOptionHovered) {
                renderer.drawRect(x, optY, width, optionHeight, baseHoverColor, 0);
            }

            String text = options.get(i);
            float textY = optY + (optionHeight - renderer.getFontHeight()) / 2.0f;
            renderer.drawString(text, x + 5, textY, textColor, false);
        }
    }

    private void closeDropdown() {
        if (this.isOpen) {
            this.isOpen = false;
            UIWidget.removeObstructor(this);
        }
    }

    private void drawArrow(UIRenderInterface renderer, float ax, float ay, int color, boolean pointUp) {
        if (pointUp) {
            for (int i = 0; i < 5; i++) {
                renderer.drawRect(ax - i, ay + 4 - i, (i * 2) + 1, 1, color, 0);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                renderer.drawRect(ax - i, ay + i, (i * 2) + 1, 1, color, 0);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        if (isOpen) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= overlayY && mouseY <= overlayY + overlayHeight) {
                int clickedIndex = (int) ((mouseY - overlayY) / optionHeight);

                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    setSelectedIndex(clickedIndex);
                    if (onSelected != null) {
                        onSelected.accept(clickedIndex);
                    }
                    closeDropdown();
                    return true;
                }
            }

            if (!isMouseOver(mouseX, mouseY)) {
                closeDropdown();
                return true;
            }
        }

        if (isMouseOver(mouseX, mouseY)) {
            if (active) {
                if (isOpen) closeDropdown();
                else openDropdown();
                return true;
            }
        }

        return false;
    }

    private void openDropdown() {
        if (!this.isOpen) {
            this.isOpen = true;
            UIWidget.addObstructor(this);
        }
    }

    @Override
    public boolean isObstructing(double mouseX, double mouseY) {
        if (!isOpen) return false;
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
    }

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
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;

import java.util.function.Consumer;

/**
 * A vertical list component that manages a collection of child widgets (items).
 * Features:
 * - Single item selection.
 * - Alternating row colors (zebra striping).
 * - Automatic vertical layout.
 *
 * @author xI-Mx-Ix
 */
public class UIListView extends UIPanel {

    /** Color for the selected item background. */
    public static final StyleKey<Integer> SELECTION_COLOR = new StyleKey<>("list_selection_color", 0xFF404040);
    
    /** Color for alternating rows (even index). Set to 0 for no striping. */
    public static final StyleKey<Integer> ALT_ROW_COLOR = new StyleKey<>("list_alt_row_color", 0x10FFFFFF);

    private UIWidget selectedItem;
    private Consumer<UIWidget> onSelectionChange;
    private float itemGap = 2.0f;

    public UIListView() {
        // Transparent default background
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
    }

    /**
     * Adds an item to the list.
     * The item's width will be automatically constrained to the list width.
     *
     * @param item The widget to add.
     * @return This list instance.
     */
    public UIListView addItem(UIWidget item) {
        this.add(item);
        // Ensure item fills the width of the list minus padding
        item.setWidth(Layout.relative(1.0f));
        return this;
    }

    /**
     * Sets the gap between items in pixels.
     */
    public UIListView setItemGap(float gap) {
        this.itemGap = gap;
        return this;
    }

    public UIWidget getSelectedItem() {
        return selectedItem;
    }

    public void setOnSelectionChange(Consumer<UIWidget> callback) {
        this.onSelectionChange = callback;
    }

    /**
     * Selects a specific item.
     */
    public void select(UIWidget item) {
        if (this.selectedItem != item) {
            this.selectedItem = item;
            if (onSelectionChange != null) {
                onSelectionChange.accept(item);
            }
        }
    }

    @Override
    public void layout() {
        super.layout();

        float currentY = 0;
        for (UIWidget child : children) {
            // Manually position children in a vertical stack
            child.setY(Layout.pixel(currentY));
            child.layout(); // Recalculate child with new constraints
            currentY += child.getHeight() + itemGap;
        }

        // Adjust the height of the list to fit content (if needed by parent scroll panel)
        this.height = currentY;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);

        int selectionColor = style().getValue(state, SELECTION_COLOR);
        int altColor = style().getValue(state, ALT_ROW_COLOR);

        // Render Selection and Striping backgrounds behind items
        for (int i = 0; i < children.size(); i++) {
            UIWidget child = children.get(i);
            
            if (child == selectedItem) {
                renderer.getGeometry().renderRect(child.getX(), child.getY(), child.getWidth(), child.getHeight(), selectionColor, 2.0f);
            } else if (i % 2 == 0 && (altColor >>> 24) > 0) {
                renderer.getGeometry().renderRect(child.getX(), child.getY(), child.getWidth(), child.getHeight(), altColor, 0);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle selection logic before passing event to children
        if (isVisible && isMouseOver(mouseX, mouseY) && button == 0) {
            for (UIWidget child : children) {
                if (child.isMouseOver(mouseX, mouseY)) {
                    select(child);
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
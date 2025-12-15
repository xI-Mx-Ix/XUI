/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * A TreeView component for hierarchical data.
 * Manages expanding/collapsing nodes and indentation.
 *
 * @author xI-Mx-Ix
 */
public class UITreeView extends UIPanel {

    /**
     * Represents a single node in the tree.
     * This internal class handles the "Widget" aspect of a node.
     */
    public class UITreeNode extends UIPanel {
        private final TextComponent label;
        private final List<UITreeNode> treeChildren = new ArrayList<>();
        private boolean expanded = false;
        private int depth = 0;
        private final float indentPerLevel = 16.0f;

        public UITreeNode(TextComponent label) {
            this.label = label;
            this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
            this.setHeight(Layout.pixel(20)); // Fixed node height
        }

        /**
         * Adds a child node to this node.
         * The child is added to the logical structure list. It is NOT added to
         * {@code this.children} because the {@link UITreeView} manages the
         * flat rendering list in its layout method.
         *
         * @param text The label text for the new node.
         * @return The created node.
         */
        public UITreeNode addChild(String text) {
            UITreeNode node = new UITreeNode(TextComponent.literal(text));
            node.depth = this.depth + 1;
            this.treeChildren.add(node);
            return node;
        }

        /**
         * Sets the expansion state of the node.
         * Triggers a layout update on the parent tree to refresh the visible list.
         *
         * @param expanded True to show children, false to hide.
         */
        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            UITreeView.this.layout();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isVisible && isMouseOver(mouseX, mouseY) && button == 0) {
                // Toggle expansion if there are logical sub-nodes
                if (!treeChildren.isEmpty()) {
                    setExpanded(!expanded);
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
            super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);

            int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);
            float currentX = 4 + (depth * indentPerLevel);
            float centerY = this.y + (this.height / 2.0f);

            // Draw Chevron (Arrow) if logical children exist
            if (!treeChildren.isEmpty()) {
                // Calculate absolute X position relative to the widget
                drawChevron(renderer, this.x + currentX, centerY, textColor, expanded);
                currentX += 12; // Space for arrow
            } else {
                currentX += 4;
            }

            // Draw Label
            renderer.drawText(label, this.x + currentX, centerY - 4, textColor, false);
        }

        private void drawChevron(UIRenderer renderer, float x, float y, int color, boolean open) {
            // Simple pixel-art style arrow
            if (open) {
                // Down arrow
                renderer.getGeometry().renderRect(x - 2, y - 1, 5, 1, color, 0);
                renderer.getGeometry().renderRect(x - 1, y, 3, 1, color, 0);
                renderer.getGeometry().renderRect(x, y + 1, 1, 1, color, 0);
            } else {
                // Right arrow
                renderer.getGeometry().renderRect(x - 1, y - 2, 1, 5, color, 0);
                renderer.getGeometry().renderRect(x, y - 1, 1, 3, color, 0);
                renderer.getGeometry().renderRect(x + 1, y, 1, 1, color, 0);
            }
        }

        public List<UITreeNode> getTreeChildren() {
            return treeChildren;
        }

        public boolean isExpanded() {
            return expanded;
        }
    }

    private final List<UITreeNode> rootNodes = new ArrayList<>();

    public UITreeView() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
    }

    public UITreeNode addRoot(String text) {
        UITreeNode node = new UITreeNode(TextComponent.literal(text));
        this.rootNodes.add(node);
        return node;
    }

    @Override
    public void layout() {
        // Flatten the visible tree for layout
        this.children.clear();
        float currentY = 0;

        for (UITreeNode root : rootNodes) {
            currentY = layoutNodeRecursive(root, currentY);
        }

        this.height = currentY;
        super.layout();
    }

    /**
     * Recursively adds visible nodes to the layout list.
     */
    private float layoutNodeRecursive(UITreeNode node, float currentY) {
        // Add current node
        this.add(node);
        node.setY(Layout.pixel(currentY));
        node.setWidth(Layout.relative(1.0f));
        // Node layout handled by its own class, but width constraint needs update from parent first
        node.layout();

        float nextY = currentY + node.getHeight();

        if (node.isExpanded()) {
            for (UITreeNode child : node.getTreeChildren()) {
                nextY = layoutNodeRecursive(child, nextY);
            }
        }
        return nextY;
    }
}
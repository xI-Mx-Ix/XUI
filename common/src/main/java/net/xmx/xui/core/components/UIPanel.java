/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.layout.LayoutManager;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * A generic container component (Panel).
 * <p>
 * It supports background color, borders, rounded corners, and a pluggable
 * {@link LayoutManager} strategy to automatically arrange its children.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIPanel extends UIWidget {

    /**
     * The strategy used to arrange children within this panel.
     * If null, children are positioned using their own absolute constraints.
     */
    private LayoutManager layoutManager;

    /**
     * Constructs a panel with default transparent black background.
     */
    public UIPanel() {
        this.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xAA000000)
                .set(ThemeProperties.BORDER_COLOR, 0xFFFFFFFF)
                .set(ThemeProperties.BORDER_THICKNESS, 0f)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.ZERO);
    }

    /**
     * Sets the layout manager for this panel.
     *
     * @param layoutManager The layout strategy to use, or null to disable.
     * @return This panel instance for chaining.
     */
    public UIPanel setLayout(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        this.markLayoutDirty(); 
        return this;
    }

    /**
     * Retrieves the current layout manager.
     *
     * @return The active LayoutManager or null.
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    /**
     * Calculates the layout of this panel and its children.
     * <p>
     * This implementation overrides {@link UIWidget#layout()} to inject the {@link LayoutManager} logic.
     * The process follows strict ordering to ensure the manager has correct context:
     * <ol>
     *     <li><b>Resolve Self:</b> Calculates this panel's own dimensions based on the parent's state.</li>
     *     <li><b>Run Manager:</b> If a layout manager exists, it calculates and sets the constraints for all children.</li>
     *     <li><b>Propagate:</b> Calls {@code super.layout()} to recursively process children based on the (potentially modified) constraints.</li>
     * </ol>
     * </p>
     */
    @Override
    public void layout() {
        // Optimization: Only run if marked dirty
        if (!isLayoutDirty) return;

        // 1. Resolve Self Dimensions manually first.
        // We must do this before running the LayoutManager, because the manager relies on 
        // this.width and this.height to calculate child positions (e.g. for grid columns).
        float pX = (parent != null) ? parent.getX() : 0;
        float pY = (parent != null) ? parent.getY() : 0;
        float pW = (parent != null) ? parent.getWidth() : 0;
        float pH = (parent != null) ? parent.getHeight() : 0;

        // Calculate and update protected fields inherited from UIWidget
        this.width = widthConstraint.calculate(0, pW, 0);
        this.height = heightConstraint.calculate(0, pH, 0);
        this.x = xConstraint.calculate(pX, pW, width);
        this.y = yConstraint.calculate(pY, pH, height);

        // 2. Run Layout Manager.
        // The manager reads 'this.width/height' and updates the x/y/w/h Constraints of the children.
        if (layoutManager != null) {
            layoutManager.arrange(this);
        }

        // 3. Delegate to super.layout().
        // This method will essentially re-calculate 'this' dimensions (result stays same),
        // but crucially, it iterates over all 'children', calls child.layout(), and resets the 'isLayoutDirty' flag.
        // Since the LayoutManager just updated the children's constraints, super.layout() will
        // now bake those constraints into the children's x/y/width/height fields.
        super.layout();
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        float thickness = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // Retrieve animated corner radii
        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);

        // 1. Draw the Border (if thickness > 0)
        if (thickness > 0 && (borderColor >>> 24) > 0) {
            renderer.getGeometry().renderOutline(
                    x, y, width, height,
                    borderColor, thickness,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
        }

        // 2. Draw the Background
        if ((bgColor >>> 24) > 0) {
            // Draw background inside the border
            float innerTL = Math.max(0, radii.topLeft() - thickness);
            float innerTR = Math.max(0, radii.topRight() - thickness);
            float innerBR = Math.max(0, radii.bottomRight() - thickness);
            float innerBL = Math.max(0, radii.bottomLeft() - thickness);

            renderer.getGeometry().renderRect(
                    x + thickness,
                    y + thickness,
                    width - (thickness * 2),
                    height - (thickness * 2),
                    bgColor,
                    innerTL, innerTR, innerBR, innerBL
            );
        }
    }
}
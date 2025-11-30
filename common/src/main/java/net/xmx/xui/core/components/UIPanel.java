package net.xmx.xui.core.components;

import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A generic container component (Panel).
 * It supports a background color, rounded corners, and a configurable border.
 * It is capable of holding children widgets.
 */
public class UIPanel extends UIWidget {

    public UIPanel() {
        // Set default styles for a panel
        this.style()
                .set(Properties.BACKGROUND_COLOR, 0xAA000000) // Default transparent black
                .set(Properties.BORDER_COLOR, 0xFFFFFFFF)     // Default white border
                .set(Properties.BORDER_THICKNESS, 0f)         // No border by default
                .set(Properties.BORDER_RADIUS, 0f);
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);
        int borderColor = getColor(Properties.BORDER_COLOR, state, partialTicks);
        float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
        float thickness = getFloat(Properties.BORDER_THICKNESS, state, partialTicks);

        // 1. Draw the Border (if thickness > 0)
        if (thickness > 0 && (borderColor >>> 24) > 0) {
            renderer.drawOutline(x, y, width, height, borderColor, radius, thickness);
        }

        // 2. Draw the Background
        // Note: The background is drawn inside the border if transparent,
        // or fully covering the area. Standard panels usually draw background over the full area
        // and border on top/inset. Our render implementation handles the border as an inset frame,
        // so we can draw the background safely.
        
        // To prevent the background from overlapping the border antialiasing or transparency weirdly,
        // we usually draw the background slightly smaller if the border is very thick, 
        // but for standard UI, drawing the full background rect underneath is standard.
        if ((bgColor >>> 24) > 0) {
            // Adjust background size slightly to fit inside the border if desired,
            // or just draw the full rect behind.
            // Here we draw the full rect.
            renderer.drawRect(x + thickness, y + thickness, width - (thickness * 2), height - (thickness * 2), bgColor, Math.max(0, radius - thickness));
        }
    }
}
package net.xmx.xui.core.components;

import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A Modern Button component.
 * Features:
 * - Animated background color
 * - Animated scale (pop effect)
 * - Rounded corners
 * - Text centering
 */
public class UIButton extends UIWidget {

    private String label;

    public UIButton(String label) {
        this.label = label;
        setupModernStyles();
    }

    private void setupModernStyles() {
        this.style()
                .setTransitionSpeed(0.25f) // Smooth speed

                // --- DEFAULT STATE (Glass Look) ---
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0xAA202020) // Transparent Dark Grey
                .set(UIState.DEFAULT, Properties.TEXT_COLOR, 0xFFE0E0E0)       // Off-White
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)          // Nice rounding
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)

                // --- HOVER STATE (Highlight) ---
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xDD404040)   // Lighter, less transparent
                .set(UIState.HOVER, Properties.TEXT_COLOR, 0xFFFFFFFF)         // Pure White
                .set(UIState.HOVER, Properties.SCALE, 1.05f)                   // Grow slightly
                .set(UIState.HOVER, Properties.BORDER_RADIUS, 10.0f)           // Rounder

                // --- ACTIVE/CLICK STATE (Feedback) ---
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0xFF000000)  // Black
                .set(UIState.ACTIVE, Properties.SCALE, 0.95f);                 // Shrink slightly
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        // 1. Calculate animated values using the Animation Manager
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);
        int txtColor = getColor(Properties.TEXT_COLOR, state, partialTicks);
        float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
        float scale = getFloat(Properties.SCALE, state, partialTicks);

        // 2. Math for Scaling from Center (Zoom effect)
        float scaledW = width * scale;
        float scaledH = height * scale;
        float adjX = x - (scaledW - width) / 2.0f;
        float adjY = y - (scaledH - height) / 2.0f;

        // 3. Draw Shadow (Fake depth)
        // Draw a black, transparent rect slightly offset
        renderer.drawRect(adjX + 2, adjY + 2, scaledW, scaledH, 0x40000000, radius);

        // 4. Draw Main Button Body
        renderer.drawRect(adjX, adjY, scaledW, scaledH, bgColor, radius);

        // 5. Draw Text (Centered)
        int strWidth = renderer.getStringWidth(label);
        int strHeight = renderer.getFontHeight();

        // Center text mathematically
        float textX = x + (width - strWidth) / 2.0f;
        float textY = y + (height - strHeight) / 2.0f + 1;

        renderer.drawString(label, textX, textY, txtColor, true);
    }

    public UIButton setLabel(String label) {
        this.label = label;
        return this;
    }
}
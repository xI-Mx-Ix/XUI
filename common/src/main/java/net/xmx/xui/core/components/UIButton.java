package net.xmx.xui.core.components;

import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A standard button component utilizing the theme system.
 */
public class UIButton extends UIWidget {
    
    private String label;

    public UIButton(String label) {
        this.label = label;
        setupDefaultStyles();
    }

    private void setupDefaultStyles() {
        this.style()
            .setTransitionSpeed(0.2f)
            
            // Default State
            .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0xFF3C3C3C)
            .set(UIState.DEFAULT, Properties.TEXT_COLOR, 0xFFE0E0E0)
            .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 4.0f)
            .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
            
            // Hover State
            .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xFF505050)
            .set(UIState.HOVER, Properties.TEXT_COLOR, 0xFFFFFFFF)
            .set(UIState.HOVER, Properties.SCALE, 1.02f)
            .set(UIState.HOVER, Properties.BORDER_RADIUS, 6.0f)

            // Active (Pressed) State
            .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0xFF282828)
            .set(UIState.ACTIVE, Properties.SCALE, 0.98f);
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        // Fetch current animated values
        int bgColor = getColor(Properties.BACKGROUND_COLOR, state, partialTicks);
        int txtColor = getColor(Properties.TEXT_COLOR, state, partialTicks);
        float radius = getFloat(Properties.BORDER_RADIUS, state, partialTicks);
        float scale = getFloat(Properties.SCALE, state, partialTicks);

        // Calculate scaled dimensions (centered scaling)
        float scaledW = width * scale;
        float scaledH = height * scale;
        float adjX = x - (scaledW - width) / 2.0f;
        float adjY = y - (scaledH - height) / 2.0f;

        // Draw background
        renderer.drawRect(adjX, adjY, scaledW, scaledH, bgColor, radius);

        // Draw Label Centered
        int strWidth = renderer.getStringWidth(label);
        int strHeight = renderer.getFontHeight();
        
        // Center text within original bounds (ignoring scale for text to keep it crisp, or scale it too if preferred)
        float textX = x + (width - strWidth) / 2.0f;
        float textY = y + (height - strHeight) / 2.0f + 1; 

        renderer.drawString(label, textX, textY, txtColor, true);
    }

    public UIButton setLabel(String label) {
        this.label = label;
        return this;
    }
}
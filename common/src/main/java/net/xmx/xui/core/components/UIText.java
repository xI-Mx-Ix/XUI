package net.xmx.xui.core.components;

import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * A basic text label component.
 */
public class UIText extends UIWidget {
    
    private String text;
    private boolean centered = false;
    private boolean shadow = true;

    public UIText(String text) {
        this.text = text;
        this.style().set(Properties.TEXT_COLOR, 0xFFFFFFFF);
    }

    @Override
    protected void drawSelf(UIRenderInterface renderer, int mouseX, int mouseY, float partialTicks, UIState state) {
        int color = getColor(Properties.TEXT_COLOR, state, partialTicks);
        
        float drawX = x;
        float drawY = y;
        
        if (centered) {
            drawX = x + (width - renderer.getStringWidth(text)) / 2.0f;
            drawY = y + (height - renderer.getFontHeight()) / 2.0f;
        }

        renderer.drawString(text, drawX, drawY, color, shadow);
    }

    public UIText setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public UIText setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public UIText setText(String text) {
        this.text = text;
        return this;
    }
}
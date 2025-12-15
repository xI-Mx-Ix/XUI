/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.charts;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * A Timeline component showing events on a horizontal line.
 *
 * @author xI-Mx-Ix
 */
public class UITimeline extends UIWidget {

    public static class Event {
        String label;
        float progress; // 0.0 to 1.0
        int color;

        public Event(String label, float progress, int color) {
            this.label = label;
            this.progress = progress;
            this.color = color;
        }
    }

    private final List<Event> events = new ArrayList<>();

    public UITimeline addEvent(String label, float progress, int color) {
        events.add(new Event(label, progress, color));
        return this;
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int lineColor = 0xFF808080;
        int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);

        float centerY = y + height / 2.0f;

        // Draw Main Line
        renderer.getGeometry().renderRect(x, centerY - 1, width, 2, lineColor, 0);

        // Draw Events
        for (Event e : events) {
            float ex = x + (width * e.progress);
            
            // Dot
            float dotSize = 10;
            boolean hovered = Math.abs(mouseX - ex) < dotSize && Math.abs(mouseY - centerY) < dotSize;
            float currentSize = hovered ? 14 : 10;
            
            renderer.getGeometry().renderRect(ex - currentSize/2, centerY - currentSize/2, currentSize, currentSize, e.color, currentSize/2);

            // Label (alternate Top/Bottom to avoid overlap)
            TextComponent t = TextComponent.literal(e.label);
            float tw = TextComponent.getTextWidth(t);
            float th = TextComponent.getFontHeight();
            
            float ty = hovered ? (centerY - 20) : (centerY + 15);
            
            renderer.drawText(t, ex - tw / 2.0f, ty, textColor, true);
        }
    }
}
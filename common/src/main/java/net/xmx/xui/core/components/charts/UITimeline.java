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
 * <p>
 * Updated to support thread-safe addition of events.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UITimeline extends UIWidget {

    private final Object dataLock = new Object();

    /**
     * Animation state: 0.0 (Empty) -> 1.0 (Full Display).
     */
    private static final StyleKey<Float> ANIM_PROGRESS = new StyleKey<>("timeline_progress", 0.0f);

    public static class Event {
        String label;
        float progress; // 0.0 to 1.0 relative to timeline width
        int color;

        /**
         * Internal animation state for hover effect.
         * 0.0 = Idle, 1.0 = Hovered.
         */
        float hoverAnimState = 0.0f;

        public Event(String label, float progress, int color) {
            this.label = label;
            this.progress = progress;
            this.color = color;
        }
    }

    private final List<Event> events = new ArrayList<>();

    public UITimeline() {
        // Set slower transition for a smooth entry effect
        this.style().setTransitionSpeed(2.0f);
    }

    /**
     * Adds an event in a thread-safe manner.
     */
    public UITimeline addEvent(String label, float progress, int color) {
        synchronized (dataLock) {
            events.add(new Event(label, progress, color));
        }
        return this;
    }

    /**
     * Replaces the current events with a new list.
     */
    public void setEvents(List<Event> newEvents) {
        synchronized (dataLock) {
            events.clear();
            events.addAll(newEvents);
        }
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        int lineColor = 0xFF808080;
        int textColor = style().getValue(state, ThemeProperties.TEXT_COLOR);

        // Animate global entry progress from current to 1.0
        float currentAnim = animManager.getAnimatedFloat(ANIM_PROGRESS, 1.0f, style().getTransitionSpeed(), deltaTime);

        float centerY = y + height / 2.0f;
        float hoverSpeed = 10.0f;
        float lerpFactor = 1.0f - (float) Math.exp(-hoverSpeed * deltaTime);

        // 1. Draw Main Line
        float lineWidth = width * currentAnim;
        if (lineWidth > 0) {
            renderer.getGeometry().renderRect(x, centerY - 1, lineWidth, 2, lineColor, 0);
        }

        // 2. Draw Events
        synchronized (dataLock) {
            for (Event e : events) {
                // Only draw event if the line has reached its position
                if (currentAnim < e.progress) continue;

                float ex = x + (width * e.progress);

                // Pop-in animation for dots: Scale up as the line passes them
                float entryScale = Math.min(1.0f, (currentAnim - e.progress) * 10.0f);

                // --- Interaction Logic ---
                float hitBoxSize = 14.0f;
                boolean hovered = Math.abs(mouseX - ex) < hitBoxSize && Math.abs(mouseY - centerY) < hitBoxSize;

                // Interpolate hover state (0.0 -> 1.0)
                float targetHover = hovered ? 1.0f : 0.0f;
                e.hoverAnimState += (targetHover - e.hoverAnimState) * lerpFactor;

                // Calculate dynamic size based on entry scale AND hover state
                float currentDotSize = (10.0f + (4.0f * e.hoverAnimState)) * entryScale;

                if (currentDotSize > 0) {
                    // Draw the dot
                    renderer.getGeometry().renderRect(
                            ex - currentDotSize / 2,
                            centerY - currentDotSize / 2,
                            currentDotSize,
                            currentDotSize,
                            e.color,
                            currentDotSize / 2
                    );

                    // Draw Label only when dot is largely visible
                    if (entryScale >= 0.8f) {
                        TextComponent t = TextComponent.literal(e.label);
                        float tw = TextComponent.getTextWidth(t);

                        float yIdle = centerY + 15;
                        float yHover = centerY - 20;
                        float currentTextY = yIdle + ((yHover - yIdle) * e.hoverAnimState);

                        int alpha = (int) (255 * entryScale);
                        int fadeColor = (textColor & 0x00FFFFFF) | (alpha << 24);

                        renderer.drawText(t, ex - tw / 2.0f, currentTextY, fadeColor, true);
                    }
                }
            }
        }
    }
}
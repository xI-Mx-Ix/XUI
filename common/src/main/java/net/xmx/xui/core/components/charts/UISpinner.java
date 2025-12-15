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

/**
 * A rotating loading spinner.
 * Displays a circle of dots that rotates continuously.
 *
 * @author xI-Mx-Ix
 */
public class UISpinner extends UIWidget {

    public static final StyleKey<Integer> SPINNER_COLOR = new StyleKey<>("spinner_color", 0xFF2196F3);
    public static final StyleKey<Float> SPEED = new StyleKey<>("spinner_speed", 200.0f); // Degrees per second

    private float currentRotation = 0.0f;

    public UISpinner() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // Update Rotation
        float speed = style().getValue(state, SPEED);
        currentRotation += speed * deltaTime;
        currentRotation %= 360.0f;

        int color = style().getValue(state, SPINNER_COLOR);
        float size = Math.min(width, height);
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float radius = size / 2.0f - 4;
        float dotSize = size / 6.0f;

        renderer.pushMatrix();
        
        // Pivot around center
        renderer.translate(centerX, centerY, 0);
        renderer.rotate(currentRotation, 0, 0, 1);
        renderer.translate(-centerX, -centerY, 0);

        // Draw 8 dots in a circle
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            float dx = (float) (centerX + Math.cos(angle) * radius);
            float dy = (float) (centerY + Math.sin(angle) * radius);

            // Fade alpha based on index for "trail" effect
            int alpha = (int) (255 * ((i + 1) / 8.0f));
            int dotColor = (color & 0x00FFFFFF) | (alpha << 24);

            renderer.getGeometry().renderRect(dx - dotSize / 2, dy - dotSize / 2, dotSize, dotSize, dotColor, dotSize / 2);
        }

        renderer.popMatrix();
    }
}
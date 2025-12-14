/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.anim.Easing;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * Example screen demonstrating continuous Looping Animations with Movement and Rotation.
 * <p>
 * This showcases:
 * 1. Defining a "Start Keyframe" (Time 0.0) to ensure smooth transitions.
 * 2. Combining multiple properties (Rotation Z, Translation X) in one timeline.
 * 3. Looping the animation indefinitely.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUIMoveRotateScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUIMoveRotateScreen() {
        super(Component.literal("Movement & Rotation Demo"));
    }

    @Override
    protected void init() {
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();
        uiContext.updateLayout(wW, wH);

        if (!uiContext.isInitialized()) {
            buildUI();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212);

        // Create the Button that will move
        UIButton animatedBtn = new UIButton();
        animatedBtn.setLabel(TextComponent.literal("Catch Me!").setFont(DefaultFonts.getRoboto()));
        
        // Position it in the absolute center of the screen initially
        animatedBtn.setX(Layout.center())
                   .setY(Layout.center())
                   .setWidth(Layout.pixel(120))
                   .setHeight(Layout.pixel(40));

        // Interaction logic (Rotated Hitbox test)
        animatedBtn.setOnClick(w -> System.out.println("You clicked the moving target!"));

        // === Animation Configuration ===
        // We define a 4-second loop cycle.
        // 1. Rotation: spins 360 degrees linearly over 4 seconds.
        // 2. Movement: Moves Right -> Center -> Left -> Center (Ping-Pong).
        
        animatedBtn.animate()
                .loop(true) // Enable infinite looping

                // --- 1. Rotation (Z-Axis / Roll) ---
                // Start at 0 degrees at 0.0s
                .setStart(ThemeProperties.ROTATION_Z, 0.0f)
                // Rotate to 360 degrees at 4.0s using Linear easing (constant speed)
                .keyframe(4.0f, ThemeProperties.ROTATION_Z, 360.0f, Easing.LINEAR)

                // --- 2. Movement (Translation X) ---
                // Start at 0 offset (Center)
                .setStart(ThemeProperties.TRANSLATE_X, 0.0f)
                
                // At 1.0s: Move Right (+100px)
                .keyframe(1.0f, ThemeProperties.TRANSLATE_X, 100.0f, Easing.EASE_IN_OUT_QUAD)
                
                // At 2.0s: Move back to Center (0px)
                .keyframe(2.0f, ThemeProperties.TRANSLATE_X, 0.0f, Easing.EASE_IN_OUT_QUAD)
                
                // At 3.0s: Move Left (-100px)
                .keyframe(3.0f, ThemeProperties.TRANSLATE_X, -100.0f, Easing.EASE_IN_OUT_QUAD)
                
                // At 4.0s: Move back to Center (0px) to complete the loop smoothly
                .keyframe(4.0f, ThemeProperties.TRANSLATE_X, 0.0f, Easing.EASE_IN_OUT_QUAD)

                .start();

        root.add(animatedBtn);
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        uiContext.render(mouseX, mouseY, partialTick);
        
        guiGraphics.drawString(font, "The hitbox rotates and moves with the widget.", 10, 10, 0xFFFFFF);
    }

    // --- Input Delegation ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (uiContext.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (uiContext.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
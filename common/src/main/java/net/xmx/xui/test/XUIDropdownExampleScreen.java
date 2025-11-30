/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIDropdown;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.impl.UIRenderImpl;

import java.util.Arrays;

/**
 * Demonstrates Dropdown Z-Ordering.
 * The Dropdown is in a top panel but renders visually OVER the bottom panel
 * and blocks clicks to elements behind it.
 *
 * @author xI-Mx-Ix
 */
public class XUIDropdownExampleScreen extends Screen {

    private UIWidget root;
    private UIRenderImpl renderer;
    private long lastFrameTime = 0;

    public XUIDropdownExampleScreen() {
        super(Component.literal("Dropdown Z-Test"));
    }

    @Override
    protected void init() {
        // 1. Root
        root = new UIPanel();
        root.setWidth(Constraints.pixel(this.width))
            .setHeight(Constraints.pixel(this.height));
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF121212);

        // 2. Bottom Container (Acts as the "Background" or "Obstructed" layer)
        // We place this before the top panel in the add list so it renders first,
        // but even if order was swapped, Z-translation handles the overlap.
        UIPanel bottomPanel = new UIPanel();
        bottomPanel.setX(Constraints.center())
                   .setY(Constraints.pixel(100))
                   .setWidth(Constraints.pixel(300))
                   .setHeight(Constraints.pixel(200));
        bottomPanel.style().set(Properties.BACKGROUND_COLOR, 0xFF2A2A2A); // Dark Grey

        // A Button inside the bottom panel that should be covered by the dropdown overlay
        UIButton obstructionButton = new UIButton("I am behind the dropdown!");
        obstructionButton.setX(Constraints.center())
                         .setY(Constraints.pixel(20))
                         .setWidth(Constraints.pixel(200))
                         .setHeight(Constraints.pixel(20));
        
        // If this prints, the overlap logic failed.
        obstructionButton.setOnClick(w -> System.out.println("FAIL: You clicked the background button!"));
        
        bottomPanel.add(obstructionButton);

        // 3. Top Container (Holds the Dropdown)
        UIPanel topPanel = new UIPanel();
        topPanel.setX(Constraints.center())
                .setY(Constraints.pixel(50)) // 50px from top
                .setWidth(Constraints.pixel(300))
                .setHeight(Constraints.pixel(60)); // Short height, so dropdown hangs out
        topPanel.style().set(Properties.BACKGROUND_COLOR, 0xFF353535); // Slightly lighter
        
        // Label
        UIText label = new UIText("Select Mode:");
        label.setX(Constraints.pixel(10)).setY(Constraints.center());
        
        // THE DROPDOWN
        UIDropdown dropdown = new UIDropdown(Arrays.asList("Survival Mode", "Creative Mode", "Spectator Mode", "Adventure Mode"));
        dropdown.setX(Constraints.anchorEnd(10)) // Align right
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(150))
                .setHeight(Constraints.pixel(20));
        
        dropdown.setOnSelected(idx -> System.out.println("Selected Index: " + idx));

        topPanel.add(label);
        topPanel.add(dropdown);

        // Add panels to root
        root.add(bottomPanel);
        root.add(topPanel); 

        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (renderer == null) renderer = new UIRenderImpl(guiGraphics);

        root.render(renderer, mouseX, mouseY, deltaTime);
        
        // Debug Text
        guiGraphics.drawString(font, "Dropdown should cover the button below.", 10, 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (root.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIDropdown;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.Arrays;

/**
 * Demonstrates Dropdown Z-Ordering using {@link UIContext}.
 * The Dropdown is in a top panel but renders visually OVER the bottom panel
 * and blocks clicks to elements behind it.
 *
 * @author xI-Mx-Ix
 */
public class XUIDropdownExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUIDropdownExampleScreen() {
        super(Component.literal("Dropdown Z-Test"));
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

        // 2. Bottom Container (Acts as the "Background" or "Obstructed" layer)
        // We place this before the top panel in the add list so it renders first.
        UIPanel bottomPanel = new UIPanel();
        bottomPanel.setX(Layout.center())
                .setY(Layout.pixel(100))
                .setWidth(Layout.pixel(300))
                .setHeight(Layout.pixel(200));
        bottomPanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF2A2A2A); // Dark Grey

        // A Button inside the bottom panel that should be covered by the dropdown overlay
        UIButton obstructionButton = new UIButton();
        obstructionButton.setLabel("I am behind the dropdown!");
        obstructionButton.setX(Layout.center())
                .setY(Layout.pixel(20))
                .setWidth(Layout.pixel(200))
                .setHeight(Layout.pixel(20));

        // If this prints, the overlap logic failed.
        obstructionButton.setOnClick(w -> System.out.println("FAIL: You clicked the background button!"));

        bottomPanel.add(obstructionButton);

        // 3. Top Container (Holds the Dropdown)
        UIPanel topPanel = new UIPanel();
        topPanel.setX(Layout.center())
                .setY(Layout.pixel(50)) // 50px from top
                .setWidth(Layout.pixel(300))
                .setHeight(Layout.pixel(60)); // Short height, so dropdown hangs out
        topPanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF353535); // Slightly lighter

        // Label
        UIText label = new UIText();
        label.setText("Select Mode:");
        label.setX(Layout.pixel(10)).setY(Layout.center());

        UIDropdown dropdown = new UIDropdown();
        dropdown.setOptions(Arrays.asList(
                TextComponent.literal("Survival Mode"),
                TextComponent.literal("Creative Mode"),
                TextComponent.literal("Spectator Mode"),
                TextComponent.literal("Adventure Mode")
        ));

        dropdown.setX(Layout.anchorEnd(10)) // Align right
                .setY(Layout.center())
                .setWidth(Layout.pixel(150))
                .setHeight(Layout.pixel(20));

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
        // Delegate rendering to the context
        uiContext.render(mouseX, mouseY, partialTick);

        // Debug Text drawn directly to screen (bypassing UIContext)
        guiGraphics.drawString(font, "Dropdown should cover the button below.", 10, 10, 0xFFFFFFFF);
    }

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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (uiContext.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (uiContext.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
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
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIDropdown;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.Arrays;

/**
 * Modernized example screen demonstrating the {@link UIDropdown} component.
 * <p>
 * This layout places a dropdown inside a central container. It demonstrates
 * how the dropdown's overlay correctly floats above other siblings (like the
 * "Apply" button below it) without affecting the layout flow.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUIDropdownExampleScreen extends Screen {

    // The context holds the widget tree and manages scaling
    private final UIContext uiContext = new UIContext();

    public XUIDropdownExampleScreen() {
        super(Component.literal("Dropdown Demo"));
    }

    @Override
    protected void init() {
        // 1. Update layout metrics
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();
        uiContext.updateLayout(wW, wH);

        // 2. Build UI only if not already present (preserves state on resize)
        if (!uiContext.isInitialized()) {
            buildUI();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212); // Dark background

        // --- Main Center Panel ---
        UIPanel container = new UIPanel();
        container.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(360))
                .setHeight(Layout.pixel(220));

        container.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(8.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // --- Header Title ---
        UIText title = new UIText();
        title.setText(TextComponent.literal("Game Settings").setBold(true));
        title.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(15));

        // --- Field Label ---
        UIText lblMode = new UIText();
        lblMode.setText("Select Game Mode:");
        lblMode.setX(Layout.pixel(30))
                .setY(Layout.pixel(55));
        lblMode.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        // --- The Dropdown Widget ---
        UIDropdown dropdown = new UIDropdown();
        dropdown.setOptions(Arrays.asList(
                TextComponent.literal("Survival Mode").setColor(0xFF55FF55), // Green
                TextComponent.literal("Creative Mode").setColor(0xFF55FFFF), // Aqua
                TextComponent.literal("Spectator Mode").setColor(0xFFAAAAAA), // Gray
                TextComponent.literal("Adventure Mode").setColor(0xFFFFAA00), // Gold
                TextComponent.literal("Hardcore Mode").setColor(0xFFFF5555)   // Red
        ));

        dropdown.setX(Layout.pixel(30))
                .setY(Layout.sibling(lblMode, 6, true)) // Placed right below label
                .setWidth(Layout.pixel(300))
                .setHeight(Layout.pixel(26)); // Modern height

        // Update console on selection
        dropdown.setOnSelected(idx ->
                System.out.println("User selected option index: " + idx)
        );

        // --- Obstructed Element (Button) ---
        // This button is placed strictly below the dropdown.
        // When the dropdown opens, it should cover this button visually,
        // and clicks on the dropdown overlay should NOT trigger this button.
        UIButton applyBtn = new UIButton();
        applyBtn.setLabel("Apply Changes");
        applyBtn.setX(Layout.center())
                .setY(Layout.sibling(dropdown, 25, true)) // 25px gap
                .setWidth(Layout.pixel(160))
                .setHeight(Layout.pixel(32));

        applyBtn.setOnClick(w -> {
            System.out.println("Settings Applied! (Mode Index: " + dropdown.getSelectedIndex() + ")");
            this.onClose();
        });

        // --- Status Text ---
        UIText statusText = new UIText();
        statusText.setText("Status: Waiting for input...");
        statusText.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.anchorEnd(15));
        statusText.style().set(ThemeProperties.TEXT_COLOR, 0xFF666666);

        // Add children to container
        container.add(title);
        container.add(lblMode);
        container.add(dropdown);
        container.add(applyBtn);
        container.add(statusText);

        // Add container to root
        root.add(container);
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // Delegate rendering to the context
        uiContext.render(mouseX, mouseY, partialTick);
    }

    // --- Input Forwarding via UIContext ---

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
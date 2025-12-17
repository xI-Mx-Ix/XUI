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
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UITooltip;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Example screen demonstrating the UITooltip system via {@link UIContext}.
 * Shows various tooltip configurations (simple, multi-line, rich text).
 *
 * @author xI-Mx-Ix
 */
public class XUITooltipExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUITooltipExampleScreen() {
        super(Component.literal("XUI Tooltip Demo"));
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
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF181818);

        // 2. Create a container for buttons
        UIPanel contentPanel = new UIPanel();
        contentPanel.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(400))
                .setHeight(Layout.pixel(300));
        contentPanel.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(10.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF444444)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        root.add(contentPanel);

        // Title
        UIText title = new UIText();
        title.setText("Tooltip Showcase");
        title.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(20))
                .setWidth(Layout.relative(1.0f));
        contentPanel.add(title);

        // List to hold tooltips so we can add them to root *after* buttons
        // This ensures they draw on top of everything.
        List<UITooltip> tooltips = new ArrayList<>();

        // --- Example 1: Simple Tooltip ---
        UIButton btnSimple = new UIButton();
        btnSimple.setLabel("Simple Tooltip");
        btnSimple.setX(Layout.center())
                .setY(Layout.pixel(60))
                .setWidth(Layout.pixel(200))
                .setHeight(Layout.pixel(30));

        UITooltip tipSimple = new UITooltip();
        tipSimple.setTarget(btnSimple);
        tipSimple.setContent("Just a basic string tooltip.");
        tooltips.add(tipSimple);
        contentPanel.add(btnSimple);

        // --- Example 2: Fast Tooltip (No Delay) ---
        UIButton btnInstant = new UIButton();
        btnInstant.setLabel("Instant Fade");
        btnInstant.setX(Layout.center())
                .setY(Layout.pixel(110))
                .setWidth(Layout.pixel(200))
                .setHeight(Layout.pixel(30));

        UITooltip tipInstant = new UITooltip();
        tipInstant.setTarget(btnInstant)
                .setContent("I appear immediately!")
                .setDelay(0.0f)
                .setFadeTimes(0.1f, 0.1f); // Fast fade

        // Custom styling for this tooltip
        tipInstant.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF003300) // Green tint
                .set(ThemeProperties.BORDER_COLOR, 0xFF00FF00);

        tooltips.add(tipInstant);
        contentPanel.add(btnInstant);

        // --- Example 3: Rich Text / Multi-line ---
        UIButton btnRich = new UIButton();
        btnRich.setLabel("Rich Content");
        btnRich.setX(Layout.center())
                .setY(Layout.pixel(160))
                .setWidth(Layout.pixel(200))
                .setHeight(Layout.pixel(30));

        // Use TextComponent list instead of Minecraft Component list
        List<TextComponent> richLines = new ArrayList<>();

        // Header: Gold + Bold
        richLines.add(TextComponent.literal("Header Info").setColor(0xFFFFAA00).setBold(true));

        // Rarity: Light Purple
        richLines.add(TextComponent.literal("Rarity: Legendary").setColor(0xFFFF55FF));

        // Spacer
        richLines.add(TextComponent.empty());

        // Stats Header: Gray
        richLines.add(TextComponent.literal("Stats:").setColor(0xFFAAAAAA));

        // Stats Values: Red and Blue
        richLines.add(TextComponent.literal(" +50 Strength").setColor(0xFFFF5555));
        richLines.add(TextComponent.literal(" +10 Speed").setColor(0xFF5555FF));

        UITooltip tipRich = new UITooltip();
        tipRich.setTarget(btnRich)
                .setLines(richLines)
                .setDelay(0.3f);

        tooltips.add(tipRich);
        contentPanel.add(btnRich);

        // --- Example 4: Corner Test (Smart Positioning) ---
        UIButton btnCorner = new UIButton();
        btnCorner.setLabel("Corner Test");
        btnCorner.setX(Layout.anchorEnd(10))
                .setY(Layout.anchorEnd(10)) // Bottom right of panel
                .setWidth(Layout.pixel(100))
                .setHeight(Layout.pixel(30));

        // Positioned inside the panel
        contentPanel.add(btnCorner);

        UITooltip tipCorner = new UITooltip();
        tipCorner.setTarget(btnCorner)
                .setContent("I should flip to the left/up so I don't go off screen!");
        tooltips.add(tipCorner);

        // 3. Add Tooltips to Root LAST
        // This is crucial for Z-Ordering. The tooltip needs to be above the panel and buttons.
        for (UITooltip tip : tooltips) {
            root.add(tip);
        }

        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // Delegate rendering to the context
        uiContext.render(mouseX, mouseY, partialTick);
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
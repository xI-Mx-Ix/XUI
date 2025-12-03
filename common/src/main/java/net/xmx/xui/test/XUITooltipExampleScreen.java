/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UITooltip;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.impl.UIRenderImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Example screen demonstrating the UITooltip system.
 * Shows various tooltip configurations (simple, multi-line, rich text).
 *
 * @author xI-Mx-Ix
 */
public class XUITooltipExampleScreen extends Screen {

    private UIWidget root;
    private long lastFrameTime = 0;

    public XUITooltipExampleScreen() {
        super(Component.literal("XUI Tooltip Demo"));
    }

    @Override
    protected void init() {
        // 1. Setup Root
        root = new UIPanel();
        root.setWidth(Constraints.pixel(this.width))
                .setHeight(Constraints.pixel(this.height));
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF181818);

        // 2. Create a container for buttons
        UIPanel contentPanel = new UIPanel();
        contentPanel.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(400))
                .setHeight(Constraints.pixel(300));
        contentPanel.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF252525)
                .set(Properties.BORDER_RADIUS, 10.0f)
                .set(Properties.BORDER_COLOR, 0xFF444444)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        root.add(contentPanel);

        // Title
        UIText title = new UIText();
        title.setText("Tooltip Showcase");
        title.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(20))
                .setWidth(Constraints.relative(1.0f));
        contentPanel.add(title);

        // List to hold tooltips so we can add them to root *after* buttons
        // This ensures they draw on top of everything.
        List<UITooltip> tooltips = new ArrayList<>();

        // --- Example 1: Simple Tooltip ---
        UIButton btnSimple = new UIButton();
        btnSimple.setLabel("Simple Tooltip");
        btnSimple.setX(Constraints.center())
                .setY(Constraints.pixel(60))
                .setWidth(Constraints.pixel(200))
                .setHeight(Constraints.pixel(30));

        UITooltip tipSimple = new UITooltip();
        tipSimple.setTarget(btnSimple);
        tipSimple.setContent("Just a basic string tooltip.");
        tooltips.add(tipSimple);
        contentPanel.add(btnSimple);

        // --- Example 2: Fast Tooltip (No Delay) ---
        UIButton btnInstant = new UIButton();
        btnInstant.setLabel("Instant Fade");
        btnInstant.setX(Constraints.center())
                .setY(Constraints.pixel(110))
                .setWidth(Constraints.pixel(200))
                .setHeight(Constraints.pixel(30));

        UITooltip tipInstant = new UITooltip();
        tipInstant.setTarget(btnInstant)
                .setContent("I appear immediately!")
                .setDelay(0.0f)
                .setFadeTimes(0.1f, 0.1f); // Fast fade

        // Custom styling for this tooltip
        tipInstant.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF003300) // Green tint
                .set(Properties.BORDER_COLOR, 0xFF00FF00);

        tooltips.add(tipInstant);
        contentPanel.add(btnInstant);

        // --- Example 3: Rich Text / Multi-line ---
        UIButton btnRich = new UIButton();
        btnRich.setLabel("Rich Content");
        btnRich.setX(Constraints.center())
                .setY(Constraints.pixel(160))
                .setWidth(Constraints.pixel(200))
                .setHeight(Constraints.pixel(30));

        List<Component> richLines = new ArrayList<>();
        richLines.add(Component.literal("Header Info").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        richLines.add(Component.literal("Rarity: Legendary").withStyle(ChatFormatting.LIGHT_PURPLE));
        richLines.add(Component.empty());
        richLines.add(Component.literal("Stats:").withStyle(ChatFormatting.GRAY));
        richLines.add(Component.literal(" +50 Strength").withStyle(ChatFormatting.RED));
        richLines.add(Component.literal(" +10 Speed").withStyle(ChatFormatting.BLUE));

        UITooltip tipRich = new UITooltip();
        tipRich.setTarget(btnRich)
                .setLines(richLines)
                .setDelay(0.3f);

        tooltips.add(tipRich);
        contentPanel.add(btnRich);

        // --- Example 4: Corner Test (Smart Positioning) ---
        UIButton btnCorner = new UIButton();
        btnCorner.setLabel("Corner Test");
        btnCorner.setX(Constraints.anchorEnd(10))
                .setY(Constraints.anchorEnd(10)) // Bottom right of panel
                .setWidth(Constraints.pixel(100))
                .setHeight(Constraints.pixel(30));

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

        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        root.render(UIRenderImpl.getInstance(), mouseX, mouseY, deltaTime);
    }
}
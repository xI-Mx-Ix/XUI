/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.style.Properties;

/**
 * Example screen demonstrating the multi-line and wrapping capabilities of UIText via {@link UIContext}.
 * Shows how to mix single lines, styled components, and wrapped blocks within one widget.
 *
 * @author xI-Mx-Ix
 */
public class XUITextExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();
    private long lastFrameTime = 0;

    public XUITextExampleScreen() {
        super(Component.literal("Text Layout Demo"));
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
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF101010);

        // 2. Main Paper/Card Panel
        UIPanel paperPanel = new UIPanel();
        paperPanel.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(400))
                .setHeight(Constraints.pixel(350));

        paperPanel.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF252525)
                .set(Properties.BORDER_RADIUS, 8.0f)
                .set(Properties.BORDER_COLOR, 0xFF404040)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // --- WIDGET 1: Simple Header (Single Line, Centered) ---
        UIText header = new UIText();
        header.setText(Component.literal("Update Changelog").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        header.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(20));

        // --- WIDGET 2: Multi-line List ---
        // UIWrappedText supports adding multiple vertical lines.
        UIWrappedText featureList = new UIWrappedText();

        // Add the title line
        featureList.setText(Component.literal("New Features:").withStyle(ChatFormatting.UNDERLINE));

        // Add subsequent lines. Passing 'false' for wrapping means they act as hard line breaks.
        featureList.addText(Component.literal("• Added multi-line text support").withStyle(ChatFormatting.GREEN), false)
                .addText(Component.literal("• Integrated Minecraft Components").withStyle(ChatFormatting.AQUA), false)
                .addText(Component.literal("• Dynamic height calculation").withStyle(ChatFormatting.YELLOW), false);

        featureList.setX(Constraints.pixel(20))
                .setY(Constraints.sibling(header, 30, true)); // Position below header

        // --- WIDGET 3: Mixed Wrapping (Introduction + Long Text) ---
        UIWrappedText description = new UIWrappedText();
        description.setText(Component.literal("Technical Details:").withStyle(ChatFormatting.RED));

        // Add a long paragraph with wrapping enabled (true)
        String longLorem = "The UIWrappedText component now supports an internal list of lines. " +
                "You can mix standard lines with auto-wrapping lines in the same widget. " +
                "The height of the widget is recalculated automatically based on the font renderer.";

        description.addText(Component.literal(longLorem).withStyle(ChatFormatting.GRAY), true);

        // Add another line without wrapping (might overflow if too long, but useful for signatures etc)
        description.addText(Component.literal("End of report.").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY), false);

        description.setX(Constraints.pixel(20))
                .setY(Constraints.sibling(featureList, 20, true))
                .setWidth(Constraints.pixel(360)); // Restrict width to force wrapping inside the panel

        // --- Close Button ---
        UIButton closeBtn = new UIButton();
        closeBtn.setLabel("Close");
        closeBtn.setX(Constraints.center())
                .setY(Constraints.anchorEnd(20))
                .setWidth(Constraints.pixel(100))
                .setHeight(Constraints.pixel(20));
        closeBtn.setOnClick(w -> this.onClose());

        // Build Tree
        paperPanel.add(header);
        paperPanel.add(featureList);
        paperPanel.add(description);
        paperPanel.add(closeBtn);
        root.add(paperPanel);

        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        uiContext.render(mouseX, mouseY, deltaTime);
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
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
import net.xmx.xui.core.components.UIEditBox;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Example screen demonstrating the {@link UIEditBox} component.
 * Includes examples of single-line input, multi-line text areas, text selection,
 * clipboard interaction, and scrolling.
 *
 * @author xI-Mx-Ix
 */
public class XUIEditBoxExampleScreen extends Screen {

    private UIWidget root;
    private long lastFrameTime = 0;

    public XUIEditBoxExampleScreen() {
        super(Component.literal("EditBox Demo"));
    }

    @Override
    protected void init() {
        // 1. Root container (Full Screen)
        root = new UIPanel();
        root.setWidth(Constraints.pixel(this.width))
                .setHeight(Constraints.pixel(this.height));
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF121212); // Dark background

        // 2. Main Center Panel
        UIPanel container = new UIPanel();
        container.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(400))
                .setHeight(Constraints.pixel(320));

        container.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF252525)
                .set(Properties.BORDER_RADIUS, 8.0f)
                .set(Properties.BORDER_COLOR, 0xFF404040)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // --- Header ---
        UIText title = new UIText();
        title.setText(Component.literal("Registration Form").withStyle(ChatFormatting.BOLD));
        title.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(15));
        
        // --- Field 1: Username (Single Line) ---
        
        // Label
        UIText lblUser = new UIText();
        lblUser.setText("Username:");
        lblUser.setX(Constraints.pixel(30))
               .setY(Constraints.pixel(50));
        lblUser.style().set(Properties.TEXT_COLOR, 0xFFAAAAAA);

        // Input Box
        UIEditBox userBox = new UIEditBox();
        userBox.setMultiline(false)
                .setMaxLength(16);
        userBox.setHint("Enter your username");
        
        userBox.setX(Constraints.pixel(30))
                .setY(Constraints.sibling(lblUser, 5, true)) // 5px below label
                .setWidth(Constraints.pixel(340))
                .setHeight(Constraints.pixel(22));
        
        userBox.setText("PlayerOne"); // Default value

        // --- Field 2: Biography (Multi Line) ---

        // Label
        UIText lblBio = new UIText();
        lblBio.setText("Biography (Multi-line):");
        lblBio.setX(Constraints.pixel(30))
              .setY(Constraints.sibling(userBox, 15, true));
        lblBio.style().set(Properties.TEXT_COLOR, 0xFFAAAAAA);

        // Input Area
        UIEditBox bioBox = new UIEditBox();
        bioBox.setMultiline(true)
              .setMaxLength(512);

        bioBox.setX(Constraints.pixel(30))
              .setY(Constraints.sibling(lblBio, 5, true))
              .setWidth(Constraints.pixel(340))
              .setHeight(Constraints.pixel(120));
        bioBox.setHint("Enter your biography");

        // Set some long initial text to demonstrate scrolling
        bioBox.setText("I am a Minecraft player.\n" +
                       "I enjoy building redstone contraptions and exploring new biomes.\n" +
                       "This text box supports scrolling if you type too much!\n" +
                       "Try selecting this text with your mouse.");

        // --- Submit Button ---
        UIButton submitBtn = new UIButton();
        submitBtn.setLabel("Print to Console");
        submitBtn.setX(Constraints.center())
                 .setY(Constraints.anchorEnd(20))
                 .setWidth(Constraints.pixel(150))
                 .setHeight(Constraints.pixel(30));
        
        submitBtn.setOnClick(widget -> {
            System.out.println("--- Form Data ---");
            System.out.println("User: " + userBox.getText());
            System.out.println("Bio:  " + bioBox.getText());
            this.onClose();
        });

        // Build Hierarchy
        container.add(title);
        container.add(lblUser);
        container.add(userBox);
        container.add(lblBio);
        container.add(bioBox);
        container.add(submitBtn);

        root.add(container);
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Calculate Delta Time for smooth animations (cursor blinking)
        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        root.render(UIRenderImpl.getInstance(), mouseX, mouseY, deltaTime);
    }

    // --- Input Forwarding ---
    // It is critical to forward all input events to the root widget
    // so the EditBox can receive keystrokes and focus events.

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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (root.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (root.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (root.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }
}
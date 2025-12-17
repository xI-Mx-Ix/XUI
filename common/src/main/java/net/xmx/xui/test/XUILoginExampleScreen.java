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
import net.xmx.xui.core.components.input.UITextInputBox;
import net.xmx.xui.core.components.input.UIPasswordInputBox;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * Example screen demonstrating a multi-page login flow.
 * <p>
 * Page 1: Email and Password (using {@link UIPasswordInputBox}).
 * Page 2: Username and Biography (using {@link UITextInputBox}).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUILoginExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    // Input references to retrieve data
    private UITextInputBox emailBox;
    private UIPasswordInputBox passwordBox;
    private UITextInputBox usernameBox;
    private UITextInputBox bioBox;

    // Container references for page switching
    private UIPanel pageOnePanel;
    private UIPanel pageTwoPanel;

    public XUILoginExampleScreen() {
        super(Component.literal("Login Demo"));
    }

    @Override
    protected void init() {
        // Update context layout dimensions based on new window size
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();
        uiContext.updateLayout(wW, wH);

        // Build UI only if not initialized (preserves text input during resize)
        if (!uiContext.isInitialized()) {
            buildUI();
        } else {
            // If already initialized (window resize), we must force a layout recalculation
            // to ensure centered elements stay centered.
            uiContext.getRoot().layout();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212);

        // Main Window Container
        UIPanel window = new UIPanel();
        window.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(380))
                .setHeight(Layout.pixel(340));

        window.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(8.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        root.add(window);

        // Initialize pages
        buildPageOne(window);
        buildPageTwo(window);

        // Start with Page 1 visible, Page 2 hidden
        pageOnePanel.setVisible(true);
        pageTwoPanel.setVisible(false);

        root.layout();
    }

    /**
     * Constructs the Email and Password page.
     */
    private void buildPageOne(UIPanel parent) {
        pageOnePanel = new UIPanel();
        pageOnePanel.setX(Layout.pixel(0)).setY(Layout.pixel(0))
                .setWidth(Layout.relative(1.0f)).setHeight(Layout.relative(1.0f));

        // Transparent background for inner panel
        pageOnePanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // Header
        UIText title = new UIText();
        title.setText(TextComponent.literal("Login").setBold(true));
        title.setCentered(true).setX(Layout.center()).setY(Layout.pixel(20));

        // --- Email Field ---
        UIText lblEmail = new UIText().setText("Email Address:");
        lblEmail.setX(Layout.pixel(30)).setY(Layout.pixel(60));
        lblEmail.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        emailBox = new UITextInputBox();
        emailBox.setHint("example@xui.net");
        emailBox.setFont(DefaultFonts.getRoboto());
        emailBox.setX(Layout.pixel(30))
                .setY(Layout.sibling(lblEmail, 5, true))
                .setWidth(Layout.pixel(320))
                .setHeight(Layout.pixel(22));

        // --- Password Field ---
        UIText lblPass = new UIText().setText("Password:");
        lblPass.setX(Layout.pixel(30)).setY(Layout.sibling(emailBox, 15, true));
        lblPass.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        passwordBox = new UIPasswordInputBox();

        passwordBox.setHint("Enter your password");
        passwordBox.setFont(DefaultFonts.getRoboto());

        passwordBox.setX(Layout.pixel(30))
                .setY(Layout.sibling(lblPass, 5, true))
                .setWidth(Layout.pixel(320))
                .setHeight(Layout.pixel(22));

        // --- Next Button ---
        UIButton nextBtn = new UIButton();
        nextBtn.setLabel("Next Step >");
        nextBtn.setX(Layout.center())
                .setY(Layout.anchorEnd(30))
                .setWidth(Layout.pixel(140))
                .setHeight(Layout.pixel(28));

        nextBtn.setOnClick(w -> {
            // Switch to Page 2
            pageOnePanel.setVisible(false);
            pageTwoPanel.setVisible(true);
            uiContext.getRoot().layout();
        });

        pageOnePanel.add(title);
        pageOnePanel.add(lblEmail);
        pageOnePanel.add(emailBox);
        pageOnePanel.add(lblPass);
        pageOnePanel.add(passwordBox);
        pageOnePanel.add(nextBtn);

        parent.add(pageOnePanel);
    }

    /**
     * Constructs the Profile (Username/Bio) page.
     */
    private void buildPageTwo(UIPanel parent) {
        pageTwoPanel = new UIPanel();
        pageTwoPanel.setX(Layout.pixel(0)).setY(Layout.pixel(0))
                .setWidth(Layout.relative(1.0f)).setHeight(Layout.relative(1.0f));

        pageTwoPanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // Header
        UIText title = new UIText();
        title.setText(TextComponent.literal("Create Profile").setBold(true));
        title.setCentered(true).setX(Layout.center()).setY(Layout.pixel(20));

        // --- Username Field ---
        UIText lblUser = new UIText().setText("Username:");
        lblUser.setX(Layout.pixel(30)).setY(Layout.pixel(60));
        lblUser.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        usernameBox = new UITextInputBox();
        usernameBox.setMaxLength(16);
        usernameBox.setHint("PlayerOne");
        usernameBox.setX(Layout.pixel(30))
                .setY(Layout.sibling(lblUser, 5, true))
                .setWidth(Layout.pixel(320))
                .setHeight(Layout.pixel(22));

        // --- Bio Field (Multiline) ---
        UIText lblBio = new UIText().setText("Biography:");
        lblBio.setX(Layout.pixel(30)).setY(Layout.sibling(usernameBox, 15, true));
        lblBio.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        bioBox = new UITextInputBox();
        bioBox.setMultiline(true);
        bioBox.setFont(DefaultFonts.getMerriweather()); // Use a fancy font for bio
        bioBox.setHint("Tell us about yourself...");
        bioBox.setX(Layout.pixel(30))
                .setY(Layout.sibling(lblBio, 5, true))
                .setWidth(Layout.pixel(320))
                .setHeight(Layout.pixel(100));

        // --- Back Button ---
        UIButton backBtn = new UIButton();
        backBtn.setLabel("< Back");
        backBtn.setX(Layout.pixel(30))
                .setY(Layout.anchorEnd(30))
                .setWidth(Layout.pixel(100))
                .setHeight(Layout.pixel(28));

        backBtn.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF404040); // Grey button

        backBtn.setOnClick(w -> {
            // Switch back to Page 1
            pageTwoPanel.setVisible(false);
            pageOnePanel.setVisible(true);
            uiContext.getRoot().layout();
        });

        // --- Submit Button ---
        UIButton submitBtn = new UIButton();
        submitBtn.setLabel("Register");
        submitBtn.setX(Layout.anchorEnd(30)) // Right aligned
                .setY(Layout.anchorEnd(30))
                .setWidth(Layout.pixel(140))
                .setHeight(Layout.pixel(28));

        submitBtn.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF2E7D32); // Green button

        submitBtn.setOnClick(w -> {
            System.out.println("=== REGISTRATION DATA ===");
            System.out.println("Email: " + emailBox.getText());
            System.out.println("Pass:  " + passwordBox.getText());
            System.out.println("User:  " + usernameBox.getText());
            System.out.println("Bio:   " + bioBox.getText().replace("\n", " [NL] "));
            this.onClose();
        });

        pageTwoPanel.add(title);
        pageTwoPanel.add(lblUser);
        pageTwoPanel.add(usernameBox);
        pageTwoPanel.add(lblBio);
        pageTwoPanel.add(bioBox);
        pageTwoPanel.add(backBtn);
        pageTwoPanel.add(submitBtn);

        parent.add(pageTwoPanel);
    }

    // --- Standard Screen Wrappers ---

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (uiContext.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (uiContext.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }
}
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
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.input.UIPasswordInputBox;
import net.xmx.xui.core.components.input.UITextInputBox;
import net.xmx.xui.core.effect.UIScissorsEffect;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * Robust Login Screen Implementation.
 * <p>
 * <b>Update:</b>
 * Removed explicit {@code layout()} calls. The system now uses a dirty-flag approach,
 * automatically recalculating layouts in the render loop whenever visibility or properties change.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUILoginExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    private UITextInputBox emailBox;
    private UIPasswordInputBox passwordBox;
    private UITextInputBox usernameBox;
    private UITextInputBox bioBox;

    private UIPanel pageOnePanel;
    private UIPanel pageTwoPanel;

    private static final float WIN_WIDTH = 380.0f;
    private static final float ANIM_SPEED = 0.4f;

    public XUILoginExampleScreen() {
        super(Component.literal("Login"));
    }

    @Override
    protected void init() {
        // Update context layout dimensions based on new window size
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();

        uiContext.updateLayout(wW, wH);

        if (!uiContext.isInitialized()) {
            buildUI();
        } else {
            // Trigger root layout to refresh any relative constraints after screen resize
            uiContext.getRoot().layout();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212);

        UIPanel window = new UIPanel();
        window.setX(Layout.center());
        window.setY(Layout.center());
        window.setWidth(Layout.pixel(WIN_WIDTH));
        window.setHeight(Layout.pixel(340));

        window.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(8.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        window.addEffect(new UIScissorsEffect());

        root.add(window);

        createPageOne(window);
        createPageTwo(window);

        showPageOneInstant();

        // Initial layout is still good practice to ensure init state is clean,
        // though strictly handled by render loop now.
        root.layout();
    }

    private void createPageOne(UIPanel parent) {
        pageOnePanel = new UIPanel();
        pageOnePanel.setX(Layout.pixel(0));
        pageOnePanel.setY(Layout.pixel(0));
        pageOnePanel.setWidth(Layout.relative(1.0f));
        pageOnePanel.setHeight(Layout.relative(1.0f));
        pageOnePanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        UIText title = new UIText();
        title.setText(TextComponent.literal("Login").setBold(true));
        title.setCentered(true);
        title.setX(Layout.center());
        title.setY(Layout.pixel(20));

        UIText lblEmail = new UIText();
        lblEmail.setText("Email:");
        lblEmail.setX(Layout.pixel(30));
        lblEmail.setY(Layout.pixel(60));
        lblEmail.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        emailBox = new UITextInputBox();
        emailBox.setHint("user@example.com");
        emailBox.setFont(DefaultFonts.getRoboto());
        emailBox.setX(Layout.pixel(30));
        emailBox.setY(Layout.sibling(lblEmail, 5, true));
        emailBox.setWidth(Layout.pixel(320));
        emailBox.setHeight(Layout.pixel(22));

        UIText lblPass = new UIText();
        lblPass.setText("Password:");
        lblPass.setX(Layout.pixel(30));
        lblPass.setY(Layout.sibling(emailBox, 15, true));
        lblPass.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        passwordBox = new UIPasswordInputBox();
        passwordBox.setHint("••••••");
        passwordBox.setX(Layout.pixel(30));
        passwordBox.setY(Layout.sibling(lblPass, 5, true));
        passwordBox.setWidth(Layout.pixel(320));
        passwordBox.setHeight(Layout.pixel(22));

        UIButton btnNext = new UIButton();
        btnNext.setLabel("Next >");
        btnNext.setX(Layout.center());
        btnNext.setY(Layout.anchorEnd(30));
        btnNext.setWidth(Layout.pixel(120));
        btnNext.setHeight(Layout.pixel(28));

        btnNext.setOnClick(w -> animateToPageTwo());

        pageOnePanel.add(title);
        pageOnePanel.add(lblEmail);
        pageOnePanel.add(emailBox);
        pageOnePanel.add(lblPass);
        pageOnePanel.add(passwordBox);
        pageOnePanel.add(btnNext);

        parent.add(pageOnePanel);
    }

    private void createPageTwo(UIPanel parent) {
        pageTwoPanel = new UIPanel();
        pageTwoPanel.setX(Layout.pixel(0));
        pageTwoPanel.setY(Layout.pixel(0));
        pageTwoPanel.setWidth(Layout.relative(1.0f));
        pageTwoPanel.setHeight(Layout.relative(1.0f));
        pageTwoPanel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        UIText title = new UIText();
        title.setText(TextComponent.literal("Profile").setBold(true));
        title.setCentered(true);
        title.setX(Layout.center());
        title.setY(Layout.pixel(20));

        UIText lblUser = new UIText();
        lblUser.setText("Username:");
        lblUser.setX(Layout.pixel(30));
        lblUser.setY(Layout.pixel(60));
        lblUser.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        usernameBox = new UITextInputBox();
        usernameBox.setMaxLength(16);
        usernameBox.setHint("Username");
        usernameBox.setX(Layout.pixel(30));
        usernameBox.setY(Layout.sibling(lblUser, 5, true));
        usernameBox.setWidth(Layout.pixel(320));
        usernameBox.setHeight(Layout.pixel(22));

        UIText lblBio = new UIText();
        lblBio.setText("Bio:");
        lblBio.setX(Layout.pixel(30));
        lblBio.setY(Layout.sibling(usernameBox, 15, true));
        lblBio.style().set(ThemeProperties.TEXT_COLOR, 0xFFAAAAAA);

        bioBox = new UITextInputBox();
        bioBox.setMultiline(true);
        bioBox.setHint("About you...");
        bioBox.setX(Layout.pixel(30));
        bioBox.setY(Layout.sibling(lblBio, 5, true));
        bioBox.setWidth(Layout.pixel(320));
        bioBox.setHeight(Layout.pixel(100));

        UIButton btnBack = new UIButton();
        btnBack.setLabel("< Back");
        btnBack.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF404040);
        btnBack.setX(Layout.pixel(30));
        btnBack.setY(Layout.anchorEnd(30));
        btnBack.setWidth(Layout.pixel(100));
        btnBack.setHeight(Layout.pixel(28));

        btnBack.setOnClick(w -> animateToPageOne());

        UIButton btnFinish = new UIButton();
        btnFinish.setLabel("Finish");
        btnFinish.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF2E7D32);
        btnFinish.setX(Layout.anchorEnd(30));
        btnFinish.setY(Layout.anchorEnd(30));
        btnFinish.setWidth(Layout.pixel(120));
        btnFinish.setHeight(Layout.pixel(28));

        btnFinish.setOnClick(w -> {
            System.out.println("Login: " + emailBox.getText());
            this.onClose();
        });

        pageTwoPanel.add(title);
        pageTwoPanel.add(lblUser);
        pageTwoPanel.add(usernameBox);
        pageTwoPanel.add(lblBio);
        pageTwoPanel.add(bioBox);
        pageTwoPanel.add(btnBack);
        pageTwoPanel.add(btnFinish);

        parent.add(pageTwoPanel);
    }

    private void showPageOneInstant() {
        pageOnePanel.setVisible(true);
        pageOnePanel.style().set(ThemeProperties.TRANSLATE_X, 0.0f);

        pageTwoPanel.setVisible(false);
        pageTwoPanel.style().set(ThemeProperties.TRANSLATE_X, WIN_WIDTH);
    }

    private void animateToPageTwo() {
        // 1. Make visible
        // Since setVisible() internally calls markLayoutDirty(),
        // the layout will be automatically recalculated in the next UIContext.render() pass.
        pageTwoPanel.setVisible(true);

        // 2. Animate
        pageOnePanel.animate()
                .keyframe(ANIM_SPEED, ThemeProperties.TRANSLATE_X, -WIN_WIDTH, Easing.EASE_IN_OUT_CUBIC)
                .onComplete(() -> pageOnePanel.setVisible(false))
                .start();

        pageTwoPanel.animate()
                .setStart(ThemeProperties.TRANSLATE_X, WIN_WIDTH)
                .keyframe(ANIM_SPEED, ThemeProperties.TRANSLATE_X, 0.0f, Easing.EASE_IN_OUT_CUBIC)
                .start();
    }

    private void animateToPageOne() {
        // 1. Make visible
        // Auto-layout is triggered via the dirty flag.
        pageOnePanel.setVisible(true);

        // 2. Animate
        pageTwoPanel.animate()
                .keyframe(ANIM_SPEED, ThemeProperties.TRANSLATE_X, WIN_WIDTH, Easing.EASE_IN_OUT_CUBIC)
                .onComplete(() -> pageTwoPanel.setVisible(false))
                .start();

        pageOnePanel.animate()
                .setStart(ThemeProperties.TRANSLATE_X, -WIN_WIDTH)
                .keyframe(ANIM_SPEED, ThemeProperties.TRANSLATE_X, 0.0f, Easing.EASE_IN_OUT_CUBIC)
                .start();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        uiContext.render(mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return uiContext.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return uiContext.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return uiContext.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return uiContext.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return uiContext.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }
}
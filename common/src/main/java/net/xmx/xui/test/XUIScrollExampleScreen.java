/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIScrollPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;

/**
 * Example screen demonstrating the UIScrollPanel component via {@link UIContext}.
 * Features a scrollable list of items with a permanent, thin scrollbar.
 *
 * @author xI-Mx-Ix
 */
public class XUIScrollExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();
    private long lastFrameTime = 0;

    // Unified Color Palette
    private static final int COLOR_BACKGROUND = 0xE0121212;      // Dark background
    private static final int COLOR_PANEL = 0xF01A1A1A;           // Panel background
    private static final int COLOR_ACCENT = 0xFF6B5ACD;          // Slate blue accent
    private static final int COLOR_ACCENT_HOVER = 0xFF8A7FDB;    // Lighter accent
    private static final int COLOR_ACCENT_ACTIVE = 0xFF5647B8;   // Darker accent
    private static final int COLOR_BORDER = 0x40FFFFFF;          // Subtle white border
    private static final int COLOR_TEXT_PRIMARY = 0xFFE8E8E8;    // Primary text
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0B0;  // Secondary text
    private static final int COLOR_DANGER = 0xFFE74C3C;          // Red for exit
    private static final int COLOR_DANGER_HOVER = 0xFFF25C4C;    // Lighter red
    private static final int COLOR_SCROLLBAR = 0xAA6B5ACD;       // Scrollbar color
    private static final int COLOR_SCROLLBAR_TRACK = 0x30FFFFFF; // Scrollbar track

    public XUIScrollExampleScreen() {
        super(Component.literal("Scroll Panel Demo"));
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
        root.style().set(Properties.BACKGROUND_COLOR, COLOR_BACKGROUND);

        // --- Main Content Panel (Centered) ---
        UIPanel mainPanel = new UIPanel();
        mainPanel.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(600))
                .setHeight(Constraints.pixel(500));

        // Style the main container with rounded corners and a border
        mainPanel.style()
                .set(Properties.BACKGROUND_COLOR, COLOR_PANEL)
                .set(Properties.BORDER_RADIUS, 16.0f)
                .set(Properties.BORDER_THICKNESS, 1.0f)
                .set(Properties.BORDER_COLOR, COLOR_BORDER);

        // --- Title ---
        UIText title = new UIText();
        title.setText("Scrollable Item List");
        title.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(30))
                .setWidth(Constraints.relative(1.0f));
        title.style().set(Properties.TEXT_COLOR, COLOR_TEXT_PRIMARY);

        // --- Subtitle ---
        UIText subtitle = new UIText();
        subtitle.setText("Try scrolling with mouse wheel or drag the scrollbar");
        subtitle.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(55))
                .setWidth(Constraints.relative(1.0f));
        subtitle.style().set(Properties.TEXT_COLOR, COLOR_TEXT_SECONDARY);

        // --- Scroll Panel Configuration ---
        UIScrollPanel scrollPanel = new UIScrollPanel();
        scrollPanel.setX(Constraints.pixel(30))
                .setY(Constraints.pixel(90))
                .setWidth(Constraints.pixel(540))
                .setHeight(Constraints.pixel(330));

        // Configure scrollbar aesthetics
        scrollPanel.style()
                .set(Properties.BACKGROUND_COLOR, 0x40000000)
                .set(Properties.BORDER_RADIUS, 8.0f)
                .set(Properties.BORDER_THICKNESS, 1.0f)
                .set(Properties.BORDER_COLOR, 0x20FFFFFF)
                .set(UIScrollPanel.SCROLLBAR_WIDTH, 4.0f)
                .set(UIScrollPanel.SCROLLBAR_PADDING, 4.0f)
                .set(UIScrollPanel.SCROLLBAR_RADIUS, 2.0f)
                .set(UIScrollPanel.SCROLLBAR_COLOR, COLOR_SCROLLBAR)
                .set(UIScrollPanel.SCROLLBAR_TRACK_COLOR, COLOR_SCROLLBAR_TRACK);

        // --- Add Items to Scroll Panel ---
        float itemSpacing = 10;
        float currentY = 10;

        for (int i = 1; i <= 25; i++) {
            UIButton itemButton = new UIButton();
            itemButton.setLabel("Item #" + i);
            itemButton.setX(Constraints.pixel(10))
                    .setY(Constraints.pixel(currentY))
                    .setWidth(Constraints.pixel(500))
                    .setHeight(Constraints.pixel(45));

            // Define colors for alternating pattern
            int bgColor = (i % 2 == 0) ? 0x80303030 : 0x80404040;
            int hoverColor = (i % 2 == 0) ? 0xAA404040 : 0xAA505050;

            // Apply style with subtle active state
            itemButton.style()
                    .setTransitionSpeed(20.0f)
                    .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, bgColor)
                    .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 6.0f)
                    .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                    .set(UIState.HOVER, Properties.BACKGROUND_COLOR, hoverColor)
                    .set(UIState.HOVER, Properties.SCALE, 1.02f)
                    .set(UIState.HOVER, Properties.BORDER_THICKNESS, 1.0f)
                    .set(UIState.HOVER, Properties.BORDER_COLOR, COLOR_ACCENT)
                    .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, hoverColor) // Maintains gray color on click
                    .set(UIState.ACTIVE, Properties.SCALE, 0.98f);                // Provides feedback via scaling

            final int itemNumber = i;
            itemButton.setOnClick(w -> System.out.println("Clicked Item #" + itemNumber));

            scrollPanel.add(itemButton);
            currentY += 45 + itemSpacing;
        }

        // --- Bottom Button Bar ---
        float buttonWidth = 150;
        float buttonHeight = 40;
        float buttonY = 440;

        // Scroll to Top Button
        UIButton btnTop = new UIButton();
        btnTop.setLabel("Scroll to Top");
        btnTop.setX(Constraints.pixel(50))
                .setY(Constraints.pixel(buttonY))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btnTop.style()
                .setTransitionSpeed(20.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, COLOR_ACCENT)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, COLOR_ACCENT_HOVER)
                .set(UIState.HOVER, Properties.SCALE, 1.05f)
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, COLOR_ACCENT_ACTIVE)
                .set(UIState.ACTIVE, Properties.SCALE, 0.95f);

        btnTop.setOnClick(w -> scrollPanel.setScrollOffset(0));

        // Scroll to Bottom Button
        UIButton btnBottom = new UIButton();
        btnBottom.setLabel("Scroll to Bottom");
        btnBottom.setX(Constraints.center())
                .setY(Constraints.pixel(buttonY))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btnBottom.style()
                .setTransitionSpeed(20.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, COLOR_ACCENT)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, COLOR_ACCENT_HOVER)
                .set(UIState.HOVER, Properties.SCALE, 1.05f)
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, COLOR_ACCENT_ACTIVE)
                .set(UIState.ACTIVE, Properties.SCALE, 0.95f);

        btnBottom.setOnClick(w -> scrollPanel.setScrollOffset(9999));

        // Exit Button
        UIButton btnClose = new UIButton();
        btnClose.setLabel("Close");
        btnClose.setX(Constraints.anchorEnd(50))
                .setY(Constraints.pixel(buttonY))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btnClose.style()
                .setTransitionSpeed(20.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, COLOR_DANGER)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, COLOR_DANGER_HOVER)
                .set(UIState.HOVER, Properties.SCALE, 1.05f)
                .set(UIState.ACTIVE, Properties.SCALE, 0.95f);

        btnClose.setOnClick(w -> this.onClose());

        // Construct the UI tree
        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(scrollPanel);
        mainPanel.add(btnTop);
        mainPanel.add(btnBottom);
        mainPanel.add(btnClose);
        root.add(mainPanel);

        // Perform initial layout calculation
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
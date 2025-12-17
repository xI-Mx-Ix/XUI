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
import net.xmx.xui.core.components.scroll.ScrollOrientation;
import net.xmx.xui.core.components.scroll.UIScrollBar;
import net.xmx.xui.core.components.scroll.UIScrollComponent;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * Example screen demonstrating the UIScrollPanel component via {@link UIContext}.
 * Features a scrollable list of items with a permanent, thin scrollbar.
 *
 * @author xI-Mx-Ix
 */
public class XUIScrollExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    // Unified Color Palette
    private static final int COLOR_BACKGROUND = 0xE0121212;      // Dark global background
    private static final int COLOR_PANEL = 0xF01A1A1A;           // Main panel background
    private static final int COLOR_ACCENT = 0xFF6B5ACD;          // Slate blue accent
    private static final int COLOR_ACCENT_HOVER = 0xFF8A7FDB;    // Lighter accent
    private static final int COLOR_ACCENT_ACTIVE = 0xFF5647B8;   // Darker accent
    private static final int COLOR_BORDER = 0x40FFFFFF;          // Subtle white border
    private static final int COLOR_TEXT_PRIMARY = 0xFFE8E8E8;    // Primary text
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0B0;  // Secondary text
    private static final int COLOR_DANGER = 0xFFE74C3C;          // Red for exit
    private static final int COLOR_DANGER_HOVER = 0xFFF25C4C;    // Lighter red
    private static final int COLOR_SCROLLBAR = 0xAA6B5ACD;       // Scrollbar thumb color
    private static final int COLOR_SCROLLBAR_TRACK = 0x10FFFFFF; // Scrollbar track color

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

    /**
     * Constructs the widget hierarchy.
     * This separates the UI creation from the rendering loop.
     */
    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, COLOR_BACKGROUND);

        // =========================================================================
        // 1. Main Container Panel
        // =========================================================================
        UIPanel mainPanel = new UIPanel();
        mainPanel.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(600))
                .setHeight(Layout.pixel(500));

        mainPanel.style()
                .set(ThemeProperties.BACKGROUND_COLOR, COLOR_PANEL)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(16.0f))
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(ThemeProperties.BORDER_COLOR, COLOR_BORDER);

        // =========================================================================
        // 2. Header Text
        // =========================================================================
        UIText title = new UIText();
        title.setText("Scrollable Item List");
        title.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(30))
                .setWidth(Layout.relative(1.0f));
        title.style().set(ThemeProperties.TEXT_COLOR, COLOR_TEXT_PRIMARY);

        UIText subtitle = new UIText();
        subtitle.setText("Demonstrating UIScrollComponent & UIScrollBar");
        subtitle.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(55))
                .setWidth(Layout.relative(1.0f));
        subtitle.style().set(ThemeProperties.TEXT_COLOR, COLOR_TEXT_SECONDARY);

        // =========================================================================
        // 3. The Scroll System
        // =========================================================================

        // A. The Viewport (Logic)
        // This component clips content and handles the actual scrolling math.
        UIScrollComponent scrollComponent = new UIScrollComponent();
        scrollComponent.setX(Layout.pixel(30))
                .setY(Layout.pixel(90))
                .setWidth(Layout.pixel(530)) // Slightly narrower to leave room for the bar
                .setHeight(Layout.pixel(330));

        scrollComponent.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0x40000000) // Darker inner background
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(8.0f));

        // B. The Scrollbar (Visual)
        // This component is placed *next* to the scroll component, not inside it.
        UIScrollBar scrollBar = new UIScrollBar(scrollComponent, ScrollOrientation.VERTICAL);
        scrollBar.setX(Layout.pixel(570)) // 30 (x) + 530 (width) + 10 (padding)
                .setY(Layout.pixel(90))
                .setWidth(Layout.pixel(8))
                .setHeight(Layout.pixel(330));

        scrollBar.style()
                .set(UIScrollBar.TRACK_COLOR, COLOR_SCROLLBAR_TRACK)
                .set(UIScrollBar.THUMB_COLOR, COLOR_SCROLLBAR)
                .set(UIScrollBar.THUMB_HOVER_COLOR, COLOR_ACCENT_HOVER)
                .set(UIScrollBar.ROUNDING, 4.0f)
                .set(UIScrollBar.PADDING, 1.5f);

        // =========================================================================
        // 4. Scroll Content Generation
        // =========================================================================
        float itemHeight = 45;
        float itemSpacing = 10;
        float currentY = 10;

        for (int i = 1; i <= 25; i++) {
            UIButton itemButton = new UIButton();
            itemButton.setLabel("List Item #" + i);
            itemButton.setX(Layout.pixel(10))
                    .setY(Layout.pixel(currentY))
                    .setWidth(Layout.pixel(490)) // Fits inside scrollComponent (530 - padding)
                    .setHeight(Layout.pixel(itemHeight));

            // Create an alternating color pattern
            int bgColor = (i % 2 == 0) ? 0x80303030 : 0x80404040;
            int hoverColor = (i % 2 == 0) ? 0xAA404040 : 0xAA505050;

            itemButton.style()
                    .setTransitionSpeed(15.0f)
                    .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, bgColor)
                    .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f))
                    .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)
                    .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, hoverColor)
                    .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.01f)
                    .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, COLOR_ACCENT)
                    .set(InteractionState.HOVER, ThemeProperties.BORDER_THICKNESS, 1.0f)
                    .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.98f);

            final int index = i;
            itemButton.setOnClick(w -> System.out.println("Selected Item " + index));

            scrollComponent.add(itemButton);
            currentY += itemHeight + itemSpacing;
        }

        // =========================================================================
        // 5. Control Buttons
        // =========================================================================
        float btnY = 440;
        float btnW = 150;
        float btnH = 40;

        // Button: Scroll to Top
        UIButton btnTop = createStyledButton("Scroll to Top", btnW, btnH, COLOR_ACCENT);
        btnTop.setX(Layout.pixel(50)).setY(Layout.pixel(btnY));

        // Logic: Reset scroll Y to 0
        btnTop.setOnClick(w -> scrollComponent.setScrollY(0));

        // Button: Scroll to Bottom
        UIButton btnBottom = createStyledButton("Scroll to Bottom", btnW, btnH, COLOR_ACCENT);
        btnBottom.setX(Layout.center()).setY(Layout.pixel(btnY));

        // Logic: Set scroll Y to the maximum calculated scroll value
        btnBottom.setOnClick(w -> scrollComponent.setScrollY(scrollComponent.getMaxScrollY()));

        // Button: Close
        UIButton btnClose = createStyledButton("Close", btnW, btnH, COLOR_DANGER);
        btnClose.setX(Layout.anchorEnd(50)).setY(Layout.pixel(btnY));

        // Logic: Close screen
        btnClose.setOnClick(w -> this.onClose());

        // =========================================================================
        // 6. Hierarchy Assembly
        // =========================================================================
        mainPanel.add(title);
        mainPanel.add(subtitle);

        mainPanel.add(scrollComponent); // Add logic component
        mainPanel.add(scrollBar);       // Add visual scrollbar component

        mainPanel.add(btnTop);
        mainPanel.add(btnBottom);
        mainPanel.add(btnClose);

        root.add(mainPanel);

        // Trigger initial layout calculation to verify sizes and positions
        root.layout();
    }

    /**
     * Helper to create consistent buttons.
     */
    private UIButton createStyledButton(String label, float width, float height, int baseColor) {
        UIButton btn = new UIButton();
        btn.setLabel(label);
        btn.setWidth(Layout.pixel(width));
        btn.setHeight(Layout.pixel(height));

        btn.style()
                .setTransitionSpeed(20.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, baseColor)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(8.0f))
                .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)
                .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.05f)
                .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.95f);

        return btn;
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
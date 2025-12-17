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
 * An example screen demonstrating bidirectional scrolling (Horizontal & Vertical).
 * <p>
 * This creates a large grid of items that exceeds the viewport dimensions in both axes.
 * It utilizes two {@link UIScrollBar} instances linked to a single {@link UIScrollComponent}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUIBidirectionalScrollExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    // --- Styling Constants ---
    private static final int COLOR_BG_DIM = 0xD0000000;
    private static final int COLOR_PANEL_BG = 0xFF212121;
    private static final int COLOR_VIEWPORT_BG = 0xFF121212;
    private static final int COLOR_BORDER = 0x40FFFFFF;
    private static final int COLOR_ACCENT = 0xFF00ADB5;
    private static final int COLOR_SCROLLBAR_THUMB = 0x80EEEEEE;
    private static final int COLOR_SCROLLBAR_TRACK = 0x20000000;

    // --- Layout Constants ---
    private static final float VIEWPORT_WIDTH = 500.0f;
    private static final float VIEWPORT_HEIGHT = 350.0f;
    private static final float SCROLLBAR_SIZE = 10.0f; // Width of V-Bar, Height of H-Bar

    public XUIBidirectionalScrollExampleScreen() {
        super(Component.literal("2D Scroll Demo"));
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
     * Constructs the UI hierarchy.
     */
    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, COLOR_BG_DIM);

        // 1. Main Frame (The window holding the viewport and controls)
        UIPanel mainFrame = new UIPanel();
        mainFrame.setX(Layout.center())
                .setY(Layout.center())
                // Width = Viewport + Scrollbar + Padding + Margins
                .setWidth(Layout.pixel(VIEWPORT_WIDTH + SCROLLBAR_SIZE + 40))
                .setHeight(Layout.pixel(VIEWPORT_HEIGHT + SCROLLBAR_SIZE + 100));

        mainFrame.style()
                .set(ThemeProperties.BACKGROUND_COLOR, COLOR_PANEL_BG)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(12.0f))
                .set(ThemeProperties.BORDER_COLOR, COLOR_BORDER)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // 2. Title
        UIText title = new UIText();
        title.setText("Bidirectional Scrolling");
        title.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(20));
        title.style().set(ThemeProperties.TEXT_COLOR, 0xFFEEEEEE);

        // 3. The Viewport (Scroll Component)
        // This clips the massive grid content.
        UIScrollComponent viewport = new UIScrollComponent();
        viewport.setX(Layout.pixel(20))
                .setY(Layout.pixel(50))
                .setWidth(Layout.pixel(VIEWPORT_WIDTH))
                .setHeight(Layout.pixel(VIEWPORT_HEIGHT));

        viewport.style()
                .set(ThemeProperties.BACKGROUND_COLOR, COLOR_VIEWPORT_BG)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))
                // We add a border to the viewport itself to define the "window"
                .set(ThemeProperties.BORDER_COLOR, 0x30FFFFFF)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // 4. Generate Content (The "Grid")
        // We create a 20x20 grid of buttons.
        // Total Content Size will be approx: 20 * (80+5) = 1700px width/height.
        generateGridContent(viewport, 20, 20);

        // 5. Scrollbars
        
        // A. Vertical Scrollbar (Right of Viewport)
        UIScrollBar vBar = new UIScrollBar(viewport, ScrollOrientation.VERTICAL);
        vBar.setX(Layout.pixel(20 + VIEWPORT_WIDTH)) // Exactly right of viewport
            .setY(Layout.pixel(50))                  // Aligned with top of viewport
            .setWidth(Layout.pixel(SCROLLBAR_SIZE))
            .setHeight(Layout.pixel(VIEWPORT_HEIGHT));

        // B. Horizontal Scrollbar (Bottom of Viewport)
        UIScrollBar hBar = new UIScrollBar(viewport, ScrollOrientation.HORIZONTAL);
        hBar.setX(Layout.pixel(20))                  // Aligned with left of viewport
            .setY(Layout.pixel(50 + VIEWPORT_HEIGHT))// Exactly below viewport
            .setWidth(Layout.pixel(VIEWPORT_WIDTH))
            .setHeight(Layout.pixel(SCROLLBAR_SIZE));

        // Style the bars
        applyScrollbarStyle(vBar);
        applyScrollbarStyle(hBar);

        // C. Corner Filler
        // Visual polish: A small square where the two scrollbars meet to complete the border.
        UIPanel cornerFiller = new UIPanel();
        cornerFiller.setX(Layout.pixel(20 + VIEWPORT_WIDTH))
                    .setY(Layout.pixel(50 + VIEWPORT_HEIGHT))
                    .setWidth(Layout.pixel(SCROLLBAR_SIZE))
                    .setHeight(Layout.pixel(SCROLLBAR_SIZE));
        cornerFiller.style().set(ThemeProperties.BACKGROUND_COLOR, COLOR_PANEL_BG); // Match frame

        // 6. Navigation Buttons
        float btnY = 50 + VIEWPORT_HEIGHT + SCROLLBAR_SIZE + 15;
        
        UIButton btnReset = createButton("Reset (0,0)", 120);
        btnReset.setX(Layout.pixel(20)).setY(Layout.pixel(btnY));
        btnReset.setOnClick(w -> {
            viewport.setScrollX(0);
            viewport.setScrollY(0);
        });

        UIButton btnCenter = createButton("Center Map", 120);
        btnCenter.setX(Layout.pixel(150)).setY(Layout.pixel(btnY));
        btnCenter.setOnClick(w -> {
            // Calculate center position: (ContentSize / 2) - (ViewportSize / 2)
            float cx = (viewport.getContentWidth() / 2) - (viewport.getWidth() / 2);
            float cy = (viewport.getContentHeight() / 2) - (viewport.getHeight() / 2);
            viewport.setScrollX(cx);
            viewport.setScrollY(cy);
        });

        UIButton btnClose = createButton("Close", 100);
        btnClose.setX(Layout.anchorEnd(20)).setY(Layout.pixel(btnY));
        btnClose.style()
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFFD32F2F)
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFFF44336);
        btnClose.setOnClick(w -> this.onClose());

        // Assembly
        mainFrame.add(title);
        mainFrame.add(viewport);
        mainFrame.add(vBar);
        mainFrame.add(hBar);
        mainFrame.add(cornerFiller);
        mainFrame.add(btnReset);
        mainFrame.add(btnCenter);
        mainFrame.add(btnClose);

        root.add(mainFrame);
        root.layout();
    }

    /**
     * Generates a grid of buttons to act as scrollable content.
     */
    private void generateGridContent(UIScrollComponent container, int rows, int cols) {
        float cellW = 80;
        float cellH = 40;
        float gap = 5;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                UIButton cell = new UIButton();
                cell.setLabel(col + "," + row);
                cell.setX(Layout.pixel(col * (cellW + gap)));
                cell.setY(Layout.pixel(row * (cellH + gap)));
                cell.setWidth(Layout.pixel(cellW));
                cell.setHeight(Layout.pixel(cellH));

                // Checkerboard pattern styling
                boolean dark = (row + col) % 2 == 0;

                // Define base colors
                int baseColor = dark ? 0xFF393E46 : 0xFF222831;

                // Define hover colors (slightly lighter for visual feedback)
                int hoverColor = dark ? 0xFF4B515C : 0xFF323944;

                cell.style()
                        .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))
                        // Remove all Border properties
                        .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, baseColor)
                        // Change background on hover instead of showing a border
                        .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, hoverColor)
                        // Add subtle click feedback
                        .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.96f);

                container.add(cell);
            }
        }
    }

    /**
     * Applies a consistent visual style to a scrollbar.
     */
    private void applyScrollbarStyle(UIScrollBar bar) {
        bar.style()
                .set(UIScrollBar.TRACK_COLOR, COLOR_SCROLLBAR_TRACK)
                .set(UIScrollBar.THUMB_COLOR, COLOR_SCROLLBAR_THUMB)
                .set(UIScrollBar.THUMB_HOVER_COLOR, COLOR_ACCENT) // Highlight on hover
                .set(UIScrollBar.ROUNDING, SCROLLBAR_SIZE / 2.0f) // Fully rounded ends
                .set(UIScrollBar.PADDING, 2.0f);
    }

    private UIButton createButton(String label, float width) {
        UIButton btn = new UIButton();
        btn.setLabel(label);
        btn.setWidth(Layout.pixel(width));
        btn.setHeight(Layout.pixel(25));
        
        btn.style()
                .setTransitionSpeed(15.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF424242)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFF616161)
                .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.05f)
                .set(InteractionState.ACTIVE, ThemeProperties.SCALE, 0.95f);
        return btn;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        uiContext.render(mouseX, mouseY, partialTick);
    }

    // --- Input Forwarding ---

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
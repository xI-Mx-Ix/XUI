package net.xmx.xui.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Example screen demonstrating the Modern UI framework usage.
 * Features a clean, centered design with cohesive color scheme and subtle animations.
 */
public class XUIExampleScreen extends Screen {

    private UIWidget root;
    private UIRenderImpl renderer;
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

    public XUIExampleScreen() {
        super(Component.literal("Modern UI"));
    }

    @Override
    protected void init() {
        // Root container
        root = new UIPanel();
        root.setWidth(Constraints.pixel(this.width))
                .setHeight(Constraints.pixel(this.height));
        root.style().set(Properties.BACKGROUND_COLOR, COLOR_BACKGROUND);

        // --- Main Content Panel (Centered, Large) ---
        UIPanel mainPanel = new UIPanel();
        mainPanel.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(500))
                .setHeight(Constraints.pixel(400));

        mainPanel.style()
                .set(Properties.BACKGROUND_COLOR, COLOR_PANEL)
                .set(Properties.BORDER_RADIUS, 16.0f)
                .set(Properties.BORDER_THICKNESS, 1.0f)
                .set(Properties.BORDER_COLOR, COLOR_BORDER);

        // --- Title ---
        UIText title = new UIText("Dashboard");
        title.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(40))
                .setWidth(Constraints.relative(1.0f));
        title.style().set(Properties.TEXT_COLOR, COLOR_TEXT_PRIMARY);

        // --- Subtitle ---
        UIText subtitle = new UIText("Modern UI Framework");
        subtitle.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(65))
                .setWidth(Constraints.relative(1.0f));
        subtitle.style().set(Properties.TEXT_COLOR, COLOR_TEXT_SECONDARY);

        // --- Button Container (Centered) ---
        float buttonWidth = 200;
        float buttonHeight = 40;
        float buttonSpacing = 15;
        float startY = 130;

        // --- Primary Action Button ---
        UIButton btn1 = new UIButton("Start Session");
        btn1.setX(Constraints.center())
                .setY(Constraints.pixel(startY))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btn1.style()
                .setTransitionSpeed(10.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, COLOR_ACCENT)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, COLOR_ACCENT_HOVER)
                .set(UIState.HOVER, Properties.SCALE, 1.02f)
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, COLOR_ACCENT_ACTIVE)
                .set(UIState.ACTIVE, Properties.SCALE, 0.98f);

        // --- Secondary Button ---
        UIButton btn2 = new UIButton("Settings");
        btn2.setX(Constraints.center())
                .setY(Constraints.pixel(startY + buttonHeight + buttonSpacing))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btn2.style()
                .setTransitionSpeed(10.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0x00000000)
                .set(UIState.DEFAULT, Properties.BORDER_THICKNESS, 1.5f)
                .set(UIState.DEFAULT, Properties.BORDER_COLOR, COLOR_ACCENT)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0x20FFFFFF)
                .set(UIState.HOVER, Properties.BORDER_COLOR, COLOR_ACCENT_HOVER)
                .set(UIState.HOVER, Properties.BORDER_THICKNESS, 1.5f)
                .set(UIState.HOVER, Properties.SCALE, 1.02f)
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0x10FFFFFF)
                .set(UIState.ACTIVE, Properties.SCALE, 0.98f);

        // --- Tertiary Button ---
        UIButton btn3 = new UIButton("View Stats");
        btn3.setX(Constraints.center())
                .setY(Constraints.pixel(startY + (buttonHeight + buttonSpacing) * 2))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btn3.style()
                .setTransitionSpeed(10.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0x00000000)
                .set(UIState.DEFAULT, Properties.BORDER_THICKNESS, 1.5f)
                .set(UIState.DEFAULT, Properties.BORDER_COLOR, COLOR_ACCENT)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0x20FFFFFF)
                .set(UIState.HOVER, Properties.BORDER_COLOR, COLOR_ACCENT_HOVER)
                .set(UIState.HOVER, Properties.BORDER_THICKNESS, 1.5f)
                .set(UIState.HOVER, Properties.SCALE, 1.02f)
                .set(UIState.ACTIVE, Properties.BACKGROUND_COLOR, 0x10FFFFFF)
                .set(UIState.ACTIVE, Properties.SCALE, 0.98f);

        // --- Exit Button (Bottom, Danger Style) ---
        UIButton btnClose = new UIButton("Exit");
        btnClose.setX(Constraints.center())
                .setY(Constraints.anchorEnd(30))
                .setWidth(Constraints.pixel(buttonWidth))
                .setHeight(Constraints.pixel(buttonHeight));

        btnClose.style()
                .setTransitionSpeed(10.0f)
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, COLOR_DANGER)
                .set(UIState.DEFAULT, Properties.BORDER_RADIUS, 8.0f)
                .set(UIState.DEFAULT, Properties.SCALE, 1.0f)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, COLOR_DANGER_HOVER)
                .set(UIState.HOVER, Properties.SCALE, 1.02f)
                .set(UIState.ACTIVE, Properties.SCALE, 0.98f);

        btnClose.setOnClick(w -> this.onClose());

        // Build the component tree
        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(btn1);
        mainPanel.add(btn2);
        mainPanel.add(btn3);
        mainPanel.add(btnClose);
        root.add(mainPanel);

        // Calculate initial positions
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (renderer == null) renderer = new UIRenderImpl(guiGraphics);

        root.render(renderer, mouseX, mouseY, deltaTime);
    }

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
}
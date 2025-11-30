package net.xmx.xui.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIRenderInterface;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.style.UIState;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Example screen demonstrating the Modern UI framework usage.
 * It constructs a sidebar layout with animated buttons and glassmorphism effects.
 */
public class XUIExampleScreen extends Screen {

    private UIWidget root;
    private UIRenderImpl renderer;
    private long lastFrameTime = 0;

    public XUIExampleScreen() {
        super(Component.literal("Modern UI"));
    }

    @Override
    protected void init() {
        // Initialize the root container filling the entire screen
        root = new UIContainer();
        root.setWidth(Constraints.pixel(this.width))
                .setHeight(Constraints.pixel(this.height));

        // Set the root background to fully transparent to show the game world behind the UI
        root.style().set(Properties.BACKGROUND_COLOR, 0x00000000);

        // --- Glassmorphism Sidebar ---
        UIWidget sidebar = new UIContainer();
        sidebar.setX(Constraints.pixel(20))
                .setY(Constraints.pixel(20))
                .setWidth(Constraints.pixel(160))
                .setHeight(Constraints.relative(0.9f));

        sidebar.style()
                .set(Properties.BACKGROUND_COLOR, 0xAA000000) // Semi-transparent black
                .set(Properties.BORDER_RADIUS, 12.0f);        // Rounded corners

        // --- Title ---
        UIText title = new UIText("MODERN UI");
        title.setCentered(true)
                .setWidth(Constraints.relative(1.0f))
                .setY(Constraints.pixel(25));

        // --- Dashboard Button (Purple Brand Color) ---
        UIButton btn1 = new UIButton("Dashboard");
        btn1.setX(Constraints.center())
                .setY(Constraints.sibling(title, 30, true))
                .setWidth(Constraints.relative(0.85f))
                .setHeight(Constraints.pixel(35));

        btn1.style()
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0xFF5D3FD3)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xFF7F63F4);

        // --- Settings Button ---
        UIButton btn2 = new UIButton("Settings");
        btn2.setX(Constraints.center())
                .setY(Constraints.sibling(btn1, 15, true))
                .setWidth(Constraints.relative(0.85f))
                .setHeight(Constraints.pixel(35));

        // --- Exit Button (Red) ---
        UIButton btnClose = new UIButton("Exit");
        btnClose.setX(Constraints.center())
                .setY(Constraints.anchorEnd(20))
                .setWidth(Constraints.relative(0.85f))
                .setHeight(Constraints.pixel(35));

        btnClose.style()
                .set(UIState.DEFAULT, Properties.BACKGROUND_COLOR, 0x88FF0000)
                .set(UIState.HOVER, Properties.BACKGROUND_COLOR, 0xFFFF0000);

        btnClose.setOnClick(w -> this.onClose());

        // Build the component tree
        sidebar.add(title);
        sidebar.add(btn1);
        sidebar.add(btn2);
        sidebar.add(btnClose);
        root.add(sidebar);

        // Calculate initial positions
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Renders the standard dark transparent background for the screen
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Calculate real delta time in seconds for smooth physics-based animations
        long now = System.currentTimeMillis();
        float deltaTime = (lastFrameTime == 0) ? 0.016f : (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (renderer == null) renderer = new UIRenderImpl(guiGraphics);

        // Pass deltaTime instead of partialTick to the UI render loop
        root.render(renderer, mouseX, mouseY, deltaTime);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * A simple container widget that draws a background rectangle.
     */
    private static class UIContainer extends UIWidget {
        @Override
        protected void drawSelf(UIRenderInterface renderer, int mx, int my, float dt, UIState state) {
            int color = getColor(Properties.BACKGROUND_COLOR, state, dt);
            float r = getFloat(Properties.BORDER_RADIUS, state, dt);

            // Only draw if not fully transparent
            if ((color >>> 24) > 0) {
                renderer.drawRect(x, y, width, height, color, r);
            }
        }
    }
}
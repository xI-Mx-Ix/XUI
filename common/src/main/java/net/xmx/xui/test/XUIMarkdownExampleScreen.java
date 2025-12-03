/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIScrollPanel;
import net.xmx.xui.core.components.markdown.UIMarkdown;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Example screen demonstrating the expanded UIMarkdown widget features.
 *
 * @author xI-Mx-Ix
 */
public class XUIMarkdownExampleScreen extends Screen {

    private UIWidget root;
    private UIRenderImpl renderer;
    private UIScrollPanel scrollPanel;
    private long lastFrameTime = 0;

    // A sample markdown string without images
    private static final String SAMPLE_MARKDOWN =
            "# XUI Framework\n" +
                    "Welcome to the **XUI** markdown demo.\n" +
                    "\n" +
                    "## Rich Formatting\n" +
                    "We now support:\n" +
                    "- **Bold** and *Italic*\n" +
                    "- ~~Strikethrough text~~\n" +
                    "- `Inline Code` formatting\n" +
                    "\n" +
                    "## Task Lists\n" +
                    "Track your progress easily:\n" +
                    "- [x] Implement Basic Markdown\n" +
                    "- [x] Add Tables support\n" +
                    "- [ ] Release version 1.0\n" +
                    "\n" +
                    "## Data Tables\n" +
                    "Tables render with dynamic column sizing:\n" +
                    "\n" +
                    "| ID | Item Name | Status |\n" +
                    "|---|---|---|\n" +
                    "| 1 | Diamond Sword | **Enchanted** |\n" +
                    "| 2 | Iron Pickaxe | Damaged |\n" +
                    "| 3 | Golden Apple | `Rare` |\n" +
                    "\n" +
                    "## Code Blocks\n" +
                    "Syntax highlighting for code:\n" +
                    "```\n" +
                    "// Java Entity Logic\n" +
                    "if (player.isSprinting()) {\n" +
                    "    speed *= 1.5f;\n" +
                    "    spawnParticles();\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "> \"The update adds significant flexibility to document rendering.\"\n" +
                    "\n" +
                    "Visit [GitHub](https://github.com) for more info.";

    public XUIMarkdownExampleScreen() {
        super(Component.literal("Markdown Demo"));
    }

    @Override
    protected void init() {
        // Root
        root = new UIPanel();
        root.setWidth(Constraints.pixel(this.width))
                .setHeight(Constraints.pixel(this.height));
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF121212);

        // Container Panel
        UIPanel container = new UIPanel();
        container.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(500))
                .setHeight(Constraints.pixel(400));

        container.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(Properties.BORDER_RADIUS, 8.0f)
                .set(Properties.BORDER_COLOR, 0xFF404040)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // Scroll Panel
        scrollPanel = new UIScrollPanel();
        scrollPanel.setX(Constraints.pixel(20))
                .setY(Constraints.pixel(20))
                .setWidth(Constraints.pixel(460))
                .setHeight(Constraints.pixel(360));

        UIMarkdown markdown = new UIMarkdown();
        markdown.setContentWidth(440);
        markdown.setMarkdown(SAMPLE_MARKDOWN);

        // Add to hierarchy
        scrollPanel.add(markdown);
        container.add(scrollPanel);
        root.add(container);

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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollPanel != null && scrollPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollPanel != null) {
            scrollPanel.mouseDragged(mouseX, mouseY, button);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
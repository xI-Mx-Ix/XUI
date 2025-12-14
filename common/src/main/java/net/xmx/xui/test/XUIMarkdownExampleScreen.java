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
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIScrollPanel;
import net.xmx.xui.core.components.markdown.UIMarkdown;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * Example screen demonstrating the expanded UIMarkdown widget features using {@link UIContext}.
 *
 * @author xI-Mx-Ix
 */
public class XUIMarkdownExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

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
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();
        uiContext.updateLayout(wW, wH);

        if (!uiContext.isInitialized()) {
            buildUI();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212);

        // Container Panel
        UIPanel container = new UIPanel();
        container.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(500))
                .setHeight(Layout.pixel(400));

        container.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(ThemeProperties.BORDER_RADIUS, 8.0f)
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // Scroll Panel
        UIScrollPanel scrollPanel = new UIScrollPanel();
        scrollPanel.setX(Layout.pixel(20))
                .setY(Layout.pixel(20))
                .setWidth(Layout.pixel(460))
                .setHeight(Layout.pixel(360));

        UIMarkdown markdown = new UIMarkdown();
        markdown.setContentWidth(440);
        markdown.setMarkdown(SAMPLE_MARKDOWN);
        markdown.setFont(DefaultFonts.getVanilla());

        // Add to hierarchy
        scrollPanel.add(markdown);
        container.add(scrollPanel);
        root.add(container);

        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // Delegate rendering to the context
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
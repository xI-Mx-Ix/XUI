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
import net.xmx.xui.core.components.UIMarkdown;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIScrollPanel;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Example screen demonstrating the UIMarkdown widget inside a ScrollPanel.
 *
 * @author xI-Mx-Ix
 */
public class XUIMarkdownExampleScreen extends Screen {

    private UIWidget root;
    private UIRenderImpl renderer;
    private UIScrollPanel scrollPanel;
    private long lastFrameTime = 0;

    // A sample markdown string with various features
    private static final String SAMPLE_MARKDOWN = 
        "# XUI Framework\n" +
        "Welcome to the **XUI** markdown demo.\n" +
        "\n" +
        "## Features\n" +
        "This widget supports various *styles*:\n" +
        "- Headers (H1, H2, H3)\n" +
        "- Unordered lists like this one\n" +
        "- **Bold text** and *Italic text*\n" +
        "- Inline `code snippets` for technical stuff\n" +
        "\n" +
        "## Blockquotes\n" +
        "> \"UI design is not just about how it looks, but how it works.\"\n" +
        "> - Anonymous Developer\n" +
        "\n" +
        "## Code Blocks\n" +
        "You can also display multiline code:\n" +
        "```\n" +
        "public void onInit() {\n" +
        "    System.out.println(\"Hello World\");\n" +
        "    renderUI();\n" +
        "}\n" +
        "```\n" +
        "\n" +
        "## Integration\n" +
        "This widget automatically calculates its height, making it perfect " +
        "for **UIScrollPanel**. As you add more text, the scrollbar adjusts automatically.\n" +
        "\n" +
        "### Conclusion\n" +
        "Enjoy building UIs with XUI!";

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
                   .setHeight(Constraints.pixel(360)); // Leave some margin
        
        // Markdown Widget
        // NOTE: We pass the width of the ScrollPanel (minus padding) to the Markdown widget
        // so it knows exactly where to wrap text.
        UIMarkdown markdown = new UIMarkdown(440); // 460 - padding
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
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
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.UIDefaultFonts;
import net.xmx.xui.core.style.Properties;
import net.xmx.xui.core.text.UIComponent;

/**
 * Example screen demonstrating the advanced text rendering capabilities of XUI.
 * <p>
 * This screen showcases:
 * <ul>
 *     <li>Mixing different font families (Vanilla, Roboto, JetBrains Mono) in a single context.</li>
 *     <li>Rich text styling (Bold, Italic, Underline, Strikethrough, Obfuscated).</li>
 *     <li>Multi-line wrapping with mixed styles.</li>
 *     <li>Proper z-ordering and layout constraints.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUITextExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUITextExampleScreen() {
        super(Component.literal("XUI Font Engine Demo"));
    }

    @Override
    protected void init() {
        // Update logical layout based on physical window size using integer scaling
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
        root.style().set(Properties.BACKGROUND_COLOR, 0xFF101010);

        // Main Container Panel
        UIPanel paperPanel = new UIPanel();
        paperPanel.setX(Constraints.center())
                .setY(Constraints.center())
                .setWidth(Constraints.pixel(420))
                .setHeight(Constraints.pixel(380));

        paperPanel.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(Properties.BORDER_RADIUS, 8.0f)
                .set(Properties.BORDER_COLOR, 0xFF333333)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // --- WIDGET 1: Header (Roboto Bold) ---
        UIText header = new UIText();
        header.setText(UIComponent.literal("Typography Showcase")
                .setColor(0xFF4FC3F7) // Light Blue
                .setBold(true)
                .setFont(UIDefaultFonts.getRoboto()));

        header.setCentered(true)
                .setX(Constraints.center())
                .setY(Constraints.pixel(15));

        // --- WIDGET 2: Font Family Comparison ---
        UIWrappedText fontCompare = new UIWrappedText();

        // Title
        fontCompare.setText(UIComponent.literal("1. Font Families")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(UIDefaultFonts.getRoboto()));

        // Vanilla Line
        fontCompare.addText(UIComponent.literal("Vanilla: The quick brown fox jumps.")
                .setColor(0xFFFFFFFF)
                .setFont(UIDefaultFonts.getVanilla()), false);

        // Roboto Line
        fontCompare.addText(UIComponent.literal("Roboto: The quick brown fox jumps.")
                .setColor(0xFFDDDDDD)
                .setFont(UIDefaultFonts.getRoboto()), false);

        // JetBrains Mono Line
        fontCompare.addText(UIComponent.literal("JB Mono: The quick brown fox jumps.")
                .setColor(0xFFB9FBC0) // Light Green
                .setFont(UIDefaultFonts.getJetBrainsMono()), false);

        fontCompare.setX(Constraints.pixel(20))
                .setY(Constraints.sibling(header, 25, true))
                .setWidth(Constraints.pixel(380));

        // --- WIDGET 3: Rich Styles & Obfuscation ---
        UIWrappedText styleShowcase = new UIWrappedText();

        styleShowcase.setText(UIComponent.literal("2. Rich Styles & Magic")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(UIDefaultFonts.getRoboto()));

        // Mixed styles in one line using Roboto
        UIComponent mixedLine = UIComponent.literal("We support ")
                .setFont(UIDefaultFonts.getRoboto())
                .append(UIComponent.literal("Bold, ").setBold(true).setColor(0xFFE57373))
                .append(UIComponent.literal("Italic, ").setItalic(true).setColor(0xFFFFF176))
                .append(UIComponent.literal("Underline ").setUnderline(true).setColor(0xFF64B5F6))
                .append(UIComponent.literal("& ").setColor(0xFF888888))
                .append(UIComponent.literal("Strikethrough").setStrikethrough(true).setColor(0xFFA1887F));

        styleShowcase.addText(mixedLine, false);

        // Obfuscation test (Matrix effect)
        UIComponent magicLine = UIComponent.literal("Secret Data: ")
                .setFont(UIDefaultFonts.getJetBrainsMono())
                .setColor(0xFF888888)
                .append(UIComponent.literal("kjd8s7d8s7d").setObfuscated(true).setColor(0xFFFF5555));

        styleShowcase.addText(magicLine, false);

        styleShowcase.setX(Constraints.pixel(20))
                .setY(Constraints.sibling(fontCompare, 20, true))
                .setWidth(Constraints.pixel(380));

        // --- WIDGET 4: Word Wrapping Paragraph ---
        UIWrappedText paragraph = new UIWrappedText();

        paragraph.setText(UIComponent.literal("3. Word Wrapping (Roboto)")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(UIDefaultFonts.getRoboto()));

        String lore = "XUI uses MSDF rendering to ensure text remains crisp at any scale. " +
                "This paragraph demonstrates automatic line wrapping within the container bounds. " +
                "It handles spaces, punctuation, and mixed font styles seamlessly.";

        UIComponent bodyText = UIComponent.literal(lore)
                .setColor(0xFFE0E0E0)
                .setFont(UIDefaultFonts.getRoboto());

        // Append a highlighed Mono section at the end
        bodyText.append(UIComponent.literal(" [System Status: OK]")
                .setFont(UIDefaultFonts.getJetBrainsMono())
                .setColor(0xFF00E676));

        paragraph.addText(bodyText, true); // true = enable wrapping

        paragraph.setX(Constraints.pixel(20))
                .setY(Constraints.sibling(styleShowcase, 20, true))
                .setWidth(Constraints.pixel(380));

        // --- Close Button ---
        UIButton closeBtn = new UIButton();
        closeBtn.setLabel(UIComponent.literal("Close").setFont(UIDefaultFonts.getRoboto()));
        closeBtn.setX(Constraints.center())
                .setY(Constraints.anchorEnd(20))
                .setWidth(Constraints.pixel(100))
                .setHeight(Constraints.pixel(20));
        closeBtn.setOnClick(w -> this.onClose());

        // Add to hierarchy
        paperPanel.add(header);
        paperPanel.add(fontCompare);
        paperPanel.add(styleShowcase);
        paperPanel.add(paragraph);
        paperPanel.add(closeBtn);

        root.add(paperPanel);
        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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
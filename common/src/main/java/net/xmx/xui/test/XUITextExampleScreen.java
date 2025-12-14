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
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

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
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF101010);

        // Main Container Panel
        UIPanel paperPanel = new UIPanel();
        paperPanel.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(420))
                .setHeight(Layout.pixel(380));

        paperPanel.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(ThemeProperties.BORDER_RADIUS, 8.0f)
                .set(ThemeProperties.BORDER_COLOR, 0xFF333333)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // --- WIDGET 1: Header (Roboto Bold) ---
        UIText header = new UIText();
        header.setText(TextComponent.literal("Typography Showcase")
                .setColor(0xFF4FC3F7) // Light Blue
                .setBold(true)
                .setFont(DefaultFonts.getRoboto()));

        header.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(15));

        // --- WIDGET 2: Font Family Comparison ---
        UIWrappedText fontCompare = new UIWrappedText();

        // Title
        fontCompare.setText(TextComponent.literal("1. Font Families")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(DefaultFonts.getRoboto()));

        // Vanilla Line
        fontCompare.addText(TextComponent.literal("Vanilla: The quick brown fox jumps.")
                .setColor(0xFFFFFFFF)
                .setFont(DefaultFonts.getVanilla()), false);

        // Roboto Line
        fontCompare.addText(TextComponent.literal("Roboto: The quick brown fox jumps.")
                .setColor(0xFFDDDDDD)
                .setFont(DefaultFonts.getRoboto()), false);

        // JetBrains Mono Line
        fontCompare.addText(TextComponent.literal("JB Mono: The quick brown fox jumps.")
                .setColor(0xFFB9FBC0) // Light Green
                .setFont(DefaultFonts.getJetBrainsMono()), false);

        fontCompare.setX(Layout.pixel(20))
                .setY(Layout.sibling(header, 25, true))
                .setWidth(Layout.pixel(380));

        // --- WIDGET 3: Rich Styles & Obfuscation ---
        UIWrappedText styleShowcase = new UIWrappedText();

        styleShowcase.setText(TextComponent.literal("2. Rich Styles & Magic")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(DefaultFonts.getRoboto()));

        // Mixed styles in one line using Roboto
        TextComponent mixedLine = TextComponent.literal("We support ")
                .setFont(DefaultFonts.getRoboto())
                .append(TextComponent.literal("Bold, ").setBold(true).setColor(0xFFE57373))
                .append(TextComponent.literal("Italic, ").setItalic(true).setColor(0xFFFFF176))
                .append(TextComponent.literal("Underline ").setUnderline(true).setColor(0xFF64B5F6))
                .append(TextComponent.literal("& ").setColor(0xFF888888))
                .append(TextComponent.literal("Strikethrough").setStrikethrough(true).setColor(0xFFA1887F));

        styleShowcase.addText(mixedLine, false);

        // Obfuscation test (Matrix effect)
        TextComponent magicLine = TextComponent.literal("Secret Data: ")
                .setFont(DefaultFonts.getJetBrainsMono())
                .setColor(0xFF888888)
                .append(TextComponent.literal("kjd8s7d8s7d").setObfuscated(true).setColor(0xFFFF5555));

        styleShowcase.addText(magicLine, false);

        styleShowcase.setX(Layout.pixel(20))
                .setY(Layout.sibling(fontCompare, 20, true))
                .setWidth(Layout.pixel(380));

        // --- WIDGET 4: Word Wrapping Paragraph ---
        UIWrappedText paragraph = new UIWrappedText();

        paragraph.setText(TextComponent.literal("3. Word Wrapping (Roboto)")
                .setColor(0xFFAAAAAA)
                .setUnderline(true)
                .setFont(DefaultFonts.getRoboto()));

        String lore = "XUI uses MSDF rendering to ensure text remains crisp at any scale. " +
                "This paragraph demonstrates automatic line wrapping within the container bounds. " +
                "It handles spaces, punctuation, and mixed font styles seamlessly.";

        TextComponent bodyText = TextComponent.literal(lore)
                .setColor(0xFFE0E0E0)
                .setFont(DefaultFonts.getRoboto());

        // Append a highlighed Mono section at the end
        bodyText.append(TextComponent.literal(" [System Status: OK]")
                .setFont(DefaultFonts.getJetBrainsMono())
                .setColor(0xFF00E676));

        paragraph.addText(bodyText, true); // true = enable wrapping

        paragraph.setX(Layout.pixel(20))
                .setY(Layout.sibling(styleShowcase, 20, true))
                .setWidth(Layout.pixel(380));

        // --- Close Button ---
        UIButton closeBtn = new UIButton();
        closeBtn.setLabel(TextComponent.literal("Close").setFont(DefaultFonts.getRoboto()));
        closeBtn.setX(Layout.center())
                .setY(Layout.anchorEnd(20))
                .setWidth(Layout.pixel(100))
                .setHeight(Layout.pixel(20));
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
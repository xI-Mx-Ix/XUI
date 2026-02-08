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
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.components.tooltip.TooltipAnchor;
import net.xmx.xui.core.components.tooltip.UIAbstractTooltip;
import net.xmx.xui.core.components.tooltip.UIInteractiveTooltip;
import net.xmx.xui.core.components.tooltip.UITooltip;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Example screen demonstrating the split Tooltip system.
 * <p>
 * Showcases:
 * <ul>
 *     <li>{@link UITooltip}: Simple, mouse-following, non-interactive.</li>
 *     <li>{@link UIInteractiveTooltip}: Static anchor, supports buttons/input inside.</li>
 *     <li>Smart positioning (Screen clamping).</li>
 *     <li>Custom content layouts inside tooltips.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUITooltipExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUITooltipExampleScreen() {
        super(Component.literal("XUI Advanced Tooltip Demo"));
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
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF181818);

        // 1. Container Panel for the buttons
        UIPanel contentPanel = new UIPanel();
        contentPanel.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(450))
                .setHeight(Layout.pixel(320));

        contentPanel.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF252525)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(10.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF444444)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        root.add(contentPanel);

        // Title
        UIText title = new UIText();
        title.setText("Tooltip System v2");
        title.setCentered(true)
                .setX(Layout.center())
                .setY(Layout.pixel(15));
        contentPanel.add(title);

        // List to hold tooltips so we can add them to root *after* everything else
        // (Ensures Z-Order is correct)
        List<UIAbstractTooltip> tooltipList = new ArrayList<>();

        // =================================================================
        // CASE 1: Simple Visual Tooltip (Follows Mouse)
        // =================================================================
        UIButton btnSimple = new UIButton();
        btnSimple.setLabel("Visual (Follow)");
        btnSimple.setX(Layout.pixel(30))
                .setY(Layout.pixel(60))
                .setWidth(Layout.pixel(180))
                .setHeight(Layout.pixel(30));

        UITooltip tipSimple = new UITooltip();
        tipSimple.setTarget(btnSimple);
        tipSimple.setAnchor(TooltipAnchor.MOUSE); // Follows cursor

        // Add standard text content
        UIWrappedText textSimple = new UIWrappedText();
        textSimple.setText("I am a simple visual tooltip. I follow the mouse cursor.");
        textSimple.setWidth(Layout.pixel(150)); // Hint width for wrapping
        tipSimple.add(textSimple);

        tooltipList.add(tipSimple);
        contentPanel.add(btnSimple);

        // =================================================================
        // CASE 2: Simple Static Tooltip (Top Anchor)
        // =================================================================
        UIButton btnStatic = new UIButton();
        btnStatic.setLabel("Visual (Fixed Top)");
        btnStatic.setX(Layout.pixel(240))
                .setY(Layout.pixel(60))
                .setWidth(Layout.pixel(180))
                .setHeight(Layout.pixel(30));

        UITooltip tipStatic = new UITooltip();
        tipStatic.setTarget(btnStatic);
        tipStatic.setAnchor(TooltipAnchor.TOP); // Fixed position

        UIWrappedText textStatic = new UIWrappedText();
        textStatic.addText(TextComponent.literal("I stay centered above the button."));
        textStatic.addText(TextComponent.literal("Even if you move the mouse."));
        tipStatic.add(textStatic);

        tooltipList.add(tipStatic);
        contentPanel.add(btnStatic);

        // =================================================================
        // CASE 3: Interactive Tooltip (With Buttons inside)
        // =================================================================
        UIButton btnInteractive = new UIButton();
        btnInteractive.setLabel("Interactive (Right)");
        btnInteractive.setX(Layout.pixel(30))
                .setY(Layout.pixel(130))
                .setWidth(Layout.pixel(180))
                .setHeight(Layout.pixel(30));

        UIInteractiveTooltip tipInteractive = new UIInteractiveTooltip();
        tipInteractive.setTarget(btnInteractive);
        tipInteractive.setAnchor(TooltipAnchor.RIGHT); // Must be static for interaction path

        // Build complex content inside the tooltip
        UIPanel containerInt = new UIPanel();
        containerInt.setWidth(Layout.pixel(160));
        containerInt.setHeight(Layout.pixel(80)); // Initial guess, autosize will fix
        containerInt.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000); // Transparent container

        UIText lblInt = new UIText();
        lblInt.setText("Actions:");
        lblInt.setX(Layout.pixel(0)).setY(Layout.pixel(0));

        UIButton btnAction = new UIButton();
        btnAction.setLabel("Click Me!");
        btnAction.setX(Layout.pixel(0))
                .setY(Layout.sibling(lblInt, 10, true))
                .setWidth(Layout.pixel(100))
                .setHeight(Layout.pixel(20));

        btnAction.setOnClick(w -> {
            btnInteractive.setLabel("Clicked inside!");
        });

        containerInt.add(lblInt);
        containerInt.add(btnAction);

        tipInteractive.add(containerInt);
        tooltipList.add(tipInteractive);
        contentPanel.add(btnInteractive);

        // =================================================================
        // CASE 4: Rich Content (Item Tooltip Style)
        // =================================================================
        UIButton btnRich = new UIButton();
        btnRich.setLabel("Rich Content (Bottom)");
        btnRich.setX(Layout.pixel(240))
                .setY(Layout.pixel(130))
                .setWidth(Layout.pixel(180))
                .setHeight(Layout.pixel(30));

        // Use Interactive type to allow copying text (hypothetically) or just stability
        UIInteractiveTooltip tipRich = new UIInteractiveTooltip();
        tipRich.setTarget(btnRich);
        tipRich.setAnchor(TooltipAnchor.BOTTOM);

        UIWrappedText richText = new UIWrappedText();
        richText.setWidth(Layout.pixel(200));

        // Header
        richText.addText(TextComponent.literal("Excalibur").setColor(0xFFFFD700).setBold(true), false);
        // Stats
        richText.addText(TextComponent.literal("Damage: +50").setColor(0xFF55FF55), false);
        richText.addText(TextComponent.literal("Speed: +10").setColor(0xFF55FFFF), false);
        // Lore
        richText.addText(TextComponent.literal("A legendary blade forged in the fires of XUI rendering engine.")
                .setColor(0xFFAAAAAA).setItalic(true), true);

        tipRich.add(richText);
        tooltipList.add(tipRich);
        contentPanel.add(btnRich);

        // =================================================================
        // CASE 5: Boundary Test (Clamping)
        // =================================================================
        UIButton btnEdge = new UIButton();
        btnEdge.setLabel("Screen Edge Test");
        btnEdge.setX(Layout.anchorEnd(20))
                .setY(Layout.anchorEnd(20)) // Bottom right of container
                .setWidth(Layout.pixel(120))
                .setHeight(Layout.pixel(30));

        contentPanel.add(btnEdge);

        UIInteractiveTooltip tipEdge = new UIInteractiveTooltip();
        tipEdge.setTarget(btnEdge);
        tipEdge.setAnchor(TooltipAnchor.RIGHT); // Should force flip to LEFT because no space on right

        UIWrappedText edgeText = new UIWrappedText();
        edgeText.addText(TextComponent.literal("I was anchored RIGHT, but I should appear LEFT to stay on screen."));
        tipEdge.add(edgeText);

        tooltipList.add(tipEdge);

        // =================================================================
        // FINAL: Register Tooltips to Root
        // =================================================================
        // We add them to the root panel last. This ensures they render
        // on top of the contentPanel and buttons.
        for (UIWidget tip : tooltipList) {
            root.add(tip);
        }

        root.layout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        uiContext.render(mouseX, mouseY, partialTick);
    }

    // --- Input Delegation ---

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
}
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
import net.xmx.xui.core.anim.Easing;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.scroll.ScrollOrientation;
import net.xmx.xui.core.components.scroll.UIScrollBar;
import net.xmx.xui.core.components.scroll.UIScrollComponent;
import net.xmx.xui.core.heroicons.HeroIcon;
import net.xmx.xui.core.heroicons.IconType;
import net.xmx.xui.core.heroicons.component.UIHeroIcon;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;

/**
 * Enhanced example screen demonstrating Heroicons with scrolling and animations.
 * <p>
 * Features:
 * <ul>
 *     <li><b>Scrollable Grid:</b> Automatically iterates over all {@link HeroIcon}.</li>
 *     <li><b>Animations:</b> Demonstrates continuous Rotation, Scaling, and Translation.</li>
 *     <li><b>Type Safety:</b> Uses the Enum registry instead of raw strings.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUIHeroIconExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    public XUIHeroIconExampleScreen() {
        super(Component.literal("HeroIcons Advanced Demo"));
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

        // --- Main Window Frame ---
        UIPanel frame = new UIPanel();
        frame.setX(Layout.center())
                .setY(Layout.center())
                .setWidth(Layout.pixel(600))
                .setHeight(Layout.pixel(450));

        frame.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(12.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF404040)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        root.add(frame);

        // --- Title ---
        // We can now dynamically show how many icons are registered in the Enum
        UIText title = new UIText();
        title.setText("HeroIcons Gallery (" + HeroIcon.values().length + " Icons)");
        title.setCentered(true).setX(Layout.center()).setY(Layout.pixel(15));
        title.style().set(ThemeProperties.TEXT_COLOR, 0xFFE0E0E0);
        frame.add(title);

        // =================================================================
        // SECTION 1: ANIMATED ICONS (Top Row)
        // =================================================================

        UIText lblAnim = new UIText();
        lblAnim.setText("Live Animations:");
        lblAnim.setX(Layout.pixel(20)).setY(Layout.pixel(45));
        lblAnim.style().set(ThemeProperties.TEXT_COLOR, 0xFF888888);
        frame.add(lblAnim);

        float animY = 65;
        float animGap = 60;
        float startX = 40;

        // 1. Spinning Cog (Rotation)
        createAnimatedIcon(frame, HeroIcon.COG_8_TOOTH, startX, animY, 0xFF9CA3AF, icon -> {
            icon.animate()
                    .loop(true)
                    .setStart(ThemeProperties.ROTATION_Z, 0.0f)
                    .keyframe(4.0f, ThemeProperties.ROTATION_Z, 360.0f, Easing.LINEAR)
                    .start();
        });

        // 2. Pulsing Heart (Scale)
        createAnimatedIcon(frame, HeroIcon.HEART, startX + animGap, animY, 0xFFEF4444, icon -> {
            icon.animate()
                    .loop(true)
                    .setStart(ThemeProperties.SCALE, 1.0f)
                    .keyframe(0.4f, ThemeProperties.SCALE, 1.2f, Easing.EASE_OUT_QUAD)
                    .keyframe(0.8f, ThemeProperties.SCALE, 1.0f, Easing.EASE_IN_QUAD)
                    .keyframe(1.2f, ThemeProperties.SCALE, 1.0f, Easing.LINEAR) // Pause
                    .start();
        });

        // 3. Bouncing Arrow (Translation Y)
        createAnimatedIcon(frame, HeroIcon.ARROW_DOWN, startX + animGap * 2, animY, 0xFF60A5FA, icon -> {
            icon.animate()
                    .loop(true)
                    .setStart(ThemeProperties.TRANSLATE_Y, 0.0f)
                    .keyframe(0.5f, ThemeProperties.TRANSLATE_Y, 10.0f, Easing.EASE_OUT_BOUNCE)
                    .keyframe(1.0f, ThemeProperties.TRANSLATE_Y, 0.0f, Easing.EASE_IN_SINE)
                    .start();
        });

        // 4. Shaking Bell (Rotation Ping-Pong)
        createAnimatedIcon(frame, HeroIcon.BELL, startX + animGap * 3, animY, 0xFFF59E0B, icon -> {
            icon.animate()
                    .loop(true)
                    .setStart(ThemeProperties.ROTATION_Z, 0.0f)
                    .keyframe(0.1f, ThemeProperties.ROTATION_Z, -15.0f, Easing.LINEAR)
                    .keyframe(0.3f, ThemeProperties.ROTATION_Z, 15.0f, Easing.LINEAR)
                    .keyframe(0.4f, ThemeProperties.ROTATION_Z, 0.0f, Easing.LINEAR)
                    .keyframe(2.0f, ThemeProperties.ROTATION_Z, 0.0f, Easing.LINEAR) // Long pause
                    .start();
        });

        // 5. Upload Icon (Pulse Color)
        createAnimatedIcon(frame, HeroIcon.ARROW_UP_TRAY, startX + animGap * 4, animY, 0xFF10B981, icon -> {
            icon.animate().loop(true)
                    .setStart(ThemeProperties.SCALE, 1.0f)
                    .keyframe(1.0f, ThemeProperties.SCALE, 1.1f, Easing.EASE_IN_OUT_SINE)
                    .keyframe(2.0f, ThemeProperties.SCALE, 1.0f, Easing.EASE_IN_OUT_SINE)
                    .start();
        });

        // =================================================================
        // SECTION 2: SCROLLABLE GRID
        // =================================================================

        UIText lblGrid = new UIText();
        lblGrid.setText("All Available Icons:");
        lblGrid.setX(Layout.pixel(20)).setY(Layout.pixel(110));
        lblGrid.style().set(ThemeProperties.TEXT_COLOR, 0xFF888888);
        frame.add(lblGrid);

        // 1. Setup Scroll Viewport
        UIScrollComponent viewport = new UIScrollComponent();
        viewport.setX(Layout.pixel(20))
                .setY(Layout.pixel(130))
                .setWidth(Layout.pixel(540))
                .setHeight(Layout.pixel(260));

        viewport.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(6.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF333333)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        // 2. Setup Scrollbar
        UIScrollBar scrollBar = new UIScrollBar(viewport, ScrollOrientation.VERTICAL);
        scrollBar.setX(Layout.pixel(565)) // Right of viewport
                .setY(Layout.pixel(130))
                .setWidth(Layout.pixel(8))
                .setHeight(Layout.pixel(260));

        scrollBar.style()
                .set(UIScrollBar.TRACK_COLOR, 0x00000000)
                .set(UIScrollBar.THUMB_COLOR, 0x40FFFFFF)
                .set(UIScrollBar.ROUNDING, 4.0f);

        // 3. Populate Grid using Enum Values
        float iconSize = 32;
        float padding = 15;
        int cols = 9; // Number of icons per row

        HeroIcon[] allIcons = HeroIcon.values();

        for (int i = 0; i < allIcons.length; i++) {
            HeroIcon iconEnum = allIcons[i];

            // Calculate grid position
            int col = i % cols;
            int row = i / cols;
            float x = padding + col * (iconSize + padding);
            float y = padding + row * (iconSize + padding);

            // Create Icon Widget using type-safe Setter
            UIHeroIcon icon = new UIHeroIcon();
            icon.setIcon(IconType.OUTLINE, iconEnum);

            icon.setX(Layout.pixel(x));
            icon.setY(Layout.pixel(y));
            icon.setWidth(Layout.pixel(iconSize));
            icon.setHeight(Layout.pixel(iconSize));

            // Interactive Style: Blue on hover, Scale up slightly
            icon.style()
                    .setTransitionSpeed(15.0f)
                    .set(InteractionState.DEFAULT, UIHeroIcon.ICON_COLOR, 0xFF9CA3AF) // Gray
                    .set(InteractionState.DEFAULT, ThemeProperties.SCALE, 1.0f)
                    .set(InteractionState.HOVER, UIHeroIcon.ICON_COLOR, 0xFF60A5FA)   // Blue
                    .set(InteractionState.HOVER, ThemeProperties.SCALE, 1.15f);

            // Toggle Outline/Solid on click
            icon.setOnClick(w -> {
                IconType next = (icon.getIconType() == IconType.SOLID) ? IconType.OUTLINE : IconType.SOLID;
                icon.setIconType(next);
                System.out.println("Clicked: " + iconEnum.getName());
            });

            viewport.add(icon);
        }

        frame.add(viewport);
        frame.add(scrollBar);

        // --- Footer Button ---
        UIButton closeBtn = new UIButton();
        closeBtn.setLabel("Close");
        closeBtn.setX(Layout.center()).setY(Layout.anchorEnd(15));
        closeBtn.setWidth(Layout.pixel(100)).setHeight(Layout.pixel(24));
        closeBtn.setOnClick(w -> this.onClose());
        frame.add(closeBtn);

        root.layout();
    }

    /**
     * Helper to create and setup a standalone animated icon presentation.
     * Uses type-safe StandardIcons enum.
     */
    private void createAnimatedIcon(UIPanel parent, HeroIcon iconEnum, float x, float y, int color, java.util.function.Consumer<UIHeroIcon> animSetup) {
        UIHeroIcon icon = new UIHeroIcon();
        icon.setIcon(IconType.SOLID, iconEnum);
        icon.setX(Layout.pixel(x)).setY(Layout.pixel(y));

        // Ensure strictly square dimensions for center rotation
        icon.setWidth(Layout.pixel(32)).setHeight(Layout.pixel(32));
        icon.style().set(UIHeroIcon.ICON_COLOR, color);

        // Apply custom animation logic
        animSetup.accept(icon);

        parent.add(icon);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (uiContext.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
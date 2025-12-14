/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.xmx.xui.core.platform.PlatformRenderInterface;
import net.xmx.xui.core.text.TextComponent;
import org.joml.Matrix4f;

/**
 * Concrete implementation of the {@link PlatformRenderInterface} for the Minecraft environment.
 * <p>
 * This class serves as the raw adapter to Minecraft's rendering systems ({@code GuiGraphics},
 * {@code FontRenderer}, {@code PoseStack}). It is strictly an execution layer and does
 * <b>not</b> contain any business logic or state management for the UI.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class RenderImpl implements PlatformRenderInterface {

    private static RenderImpl instance;

    /**
     * The Minecraft graphics context helper.
     * Valid only between {@link #initiateRenderCycle(double)} and {@link #finishRenderCycle()}.
     */
    private GuiGraphics guiGraphics;

    /**
     * Private constructor to enforce Singleton pattern via {@link #getInstance()}.
     */
    private RenderImpl() {
        // Singleton
    }

    /**
     * Retrieves the singleton instance of the implementation.
     *
     * @return The active renderer implementation.
     */
    public static RenderImpl getInstance() {
        if (instance == null) {
            instance = new RenderImpl();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Initializes the {@link GuiGraphics} instance and adjusts the internal {@code PoseStack}
     * to match the logical UI scale. This ensures that vanilla text rendering commands
     * sent from the UI (in logical pixels) land at the correct screen position and size.
     * </p>
     */
    @Override
    public void initiateRenderCycle(double uiScale) {
        Minecraft mc = Minecraft.getInstance();
        // Create a new graphics context using Minecraft's global buffer source.
        this.guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());

        // Calculate the ratio between the custom XUI logical scale and Minecraft's internal GUI scale.
        double mcScale = mc.getWindow().getGuiScale();
        float textScaleAdjustment = (float) (uiScale / mcScale);

        // Apply this scale globally to the vanilla PoseStack.
        // This bridges the coordinate systems so that (10, 10) in XUI aligns with visual (10, 10) in MC.
        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().scale(textScaleAdjustment, textScaleAdjustment, 1.0f);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Flushes the render buffers and restores the state of the vanilla {@code PoseStack}.
     * It is crucial to pop the stack here to undo the scaling applied in {@link #initiateRenderCycle}.
     * </p>
     */
    @Override
    public void finishRenderCycle() {
        if (this.guiGraphics != null) {
            // Force Minecraft to render all buffered batches immediately.
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

            // Restore the vanilla PoseStack to its clean state (removing our global scale).
            this.guiGraphics.pose().popPose();

            this.guiGraphics = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getScaleFactor() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    // =================================================================================
    // Native Font Implementation
    // =================================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderNativeText(TextComponent text, float x, float y, int color, boolean shadow, Matrix4f pose) {
        if (this.guiGraphics == null) return;

        // Convert Core component to Minecraft component
        Component mcComp = toMinecraftComponent(text);

        // Synchronize Matrix Stack:
        // 1. Push new state on MC stack
        this.guiGraphics.pose().pushPose();
        // 2. Apply the Core's transformation matrix
        this.guiGraphics.pose().mulPose(pose);
        // 3. Apply Z-offset to prevent z-fighting with panel backgrounds
        this.guiGraphics.pose().translate(0.0f, 0.0f, 0.05f);

        // 4. Delegate to MC FontRenderer
        this.guiGraphics.drawString(Minecraft.getInstance().font, mcComp, (int) x, (int) y, color, shadow);

        // 5. Restore stack
        this.guiGraphics.pose().popPose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderNativeWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow, Matrix4f pose) {
        if (this.guiGraphics == null) return;

        Component mcComp = toMinecraftComponent(text);

        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().mulPose(pose);
        this.guiGraphics.pose().translate(0.0f, 0.0f, 0.05f);

        this.guiGraphics.drawWordWrap(Minecraft.getInstance().font, mcComp, (int) x, (int) y, (int) width, color);

        this.guiGraphics.pose().popPose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getNativeLineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getNativeStringWidth(TextComponent text) {
        return Minecraft.getInstance().font.width(toMinecraftComponent(text));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getNativeWordWrapHeight(TextComponent text, float maxWidth) {
        return Minecraft.getInstance().font.wordWrapHeight(toMinecraftComponent(text), (int) maxWidth);
    }

    /**
     * Internal Helper: Converts XUI TextComponents to Minecraft Components.
     * This handles style mapping (Bold, Italic, etc.) and hierarchy.
     *
     * @param uiComp The Core text component.
     * @return The Minecraft text component with applied styles.
     */
    private Component toMinecraftComponent(TextComponent uiComp) {
        if (uiComp == null) return Component.empty();

        // 1. Create Base Literal
        MutableComponent mcComp =
                Component.literal(uiComp.getText() == null ? "" : uiComp.getText());

        // 2. Map Styles
        Style style = Style.EMPTY
                .withBold(uiComp.isBold())
                .withItalic(uiComp.isItalic())
                .withUnderlined(uiComp.isUnderline())
                .withStrikethrough(uiComp.isStrikethrough())
                .withObfuscated(uiComp.isObfuscated());

        // 3. Map Color
        if (uiComp.getColor() != null) {
            style = style.withColor(TextColor.fromRgb(uiComp.getColor()));
        }

        mcComp.setStyle(style);

        // 4. Recursively Append Siblings
        for (TextComponent sibling : uiComp.getSiblings()) {
            mcComp.append(toMinecraftComponent(sibling));
        }

        return mcComp;
    }
}
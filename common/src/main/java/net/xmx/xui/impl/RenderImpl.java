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
import net.xmx.xui.core.gl.RenderInterface;
import net.xmx.xui.core.gl.TransformStack;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.text.TextComponent;
import org.joml.Matrix4f;

/**
 * The concrete implementation of the {@link RenderInterface} for the Minecraft platform.
 * <p>
 * <b>Architecture Note:</b><br>
 * This class acts strictly as a <b>bridge</b> (Adapter Pattern) between the high-level XUI Core
 * and the low-level Minecraft rendering engine. It deliberately contains <b>NO</b> rendering logic
 * for geometry, shapes, or state management. All such logic is handled by the {@link UIRenderer}
 * in the Core module.
 * </p>
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *     <li>Acquiring and releasing the {@link GuiGraphics} instance for each frame.</li>
 *     <li>Synchronizing the Core's {@link UIRenderer} state with the new frame.</li>
 *     <li>Delegating text rendering calls to the appropriate font systems, while ensuring
 *         OpenGL matrices are synchronized between XUI and Minecraft.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class RenderImpl implements RenderInterface {

    private static RenderImpl instance;

    /**
     * The Minecraft Graphics object for the current frame.
     * <p>
     * This object is instantiated in {@link #beginFrame} and destroyed in {@link #endFrame}.
     * It is required by vanilla font renderers to draw text.
     * </p>
     */
    private GuiGraphics guiGraphics;

    /**
     * Private constructor to enforce Singleton pattern via {@link #getInstance()}.
     */
    private RenderImpl() {
    }

    /**
     * Retrieves the singleton instance of the renderer implementation.
     *
     * @return The active renderer implementation.
     */
    public static RenderImpl getInstance() {
        if (instance == null) {
            instance = new RenderImpl();
        }
        return instance;
    }

    // --- Lifecycle Management ---

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs the following platform-specific setup:
     * <ol>
     *     <li>Instantiates a new {@link GuiGraphics} object tied to Minecraft's buffer source.</li>
     *     <li>Injects the provided {@code uiScale} into the {@link UIRenderer} so core logic works correctly.</li>
     *     <li>Resets the {@link UIRenderer}'s transform stack to identity.</li>
     *     <li>Clears the depth buffer if requested (via direct OpenGL calls).</li>
     *     <li>Calculates the ratio between XUI's Logical Scale and Minecraft's GUI Scale, applying this
     *         scaling to the vanilla {@code PoseStack}. This ensures that standard Minecraft items or text
     *         rendered inside XUI appear at the correct size.</li>
     * </ol>
     * </p>
     */
    @Override
    public void beginFrame(double uiScale, boolean clearDepthBuffer) {
        RenderInterface.super.beginFrame(uiScale, clearDepthBuffer);

        Minecraft mc = Minecraft.getInstance();

        // We use the main buffer source from Minecraft to allow efficient batching of draw calls.
        this.guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());

        // Adjusts the vanilla PoseStack so that vanilla items/text match the XUI scale.
        double mcScale = mc.getWindow().getGuiScale();
        float textScaleAdjustment = (float) (uiScale / mcScale);

        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().scale(textScaleAdjustment, textScaleAdjustment, 1.0f);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs the following cleanup:
     * <ol>
     *     <li>Flushes Minecraft's render buffers to ensure all geometry (especially text) is drawn.</li>
     *     <li>Pops the Vanilla {@code PoseStack} to restore the game's previous state.</li>
     *     <li>Releases the reference to {@code guiGraphics} to prevent memory leaks or misuse.</li>
     * </ol>
     * </p>
     */
    @Override
    public void endFrame() {
        if (this.guiGraphics == null) return;

        // 1. Flush Render Buffers
        // This forces any batched text or geometry to be rendered immediately.
        // Crucial before changing global GL state or popping matrices.
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

        // 2. Pop Vanilla Stack
        this.guiGraphics.pose().popPose();

        // 3. Cleanup
        this.guiGraphics = null;
    }

    // --- Text Rendering (Platform Bridge) ---

    /**
     * {@inheritDoc}
     * <p>
     * <b>Synchronization Logic:</b><br>
     * The XUI Core maintains its own {@link TransformStack} in {@link UIRenderer}.
     * However, Minecraft's text rendering uses its own internal stack in {@link GuiGraphics}.
     * <br>
     * To ensure text follows the UI's rotations, scales, and translations, this method:
     * <ol>
     *     <li>Retrieves the current Model-View matrix from {@link UIRenderer}.</li>
     *     <li>Pushes a new state onto the Minecraft {@code PoseStack}.</li>
     *     <li>Multiplies the Minecraft stack by the XUI matrix.</li>
     *     <li>Delegates the draw call to the {@link TextComponent}'s font (which calls back to Vanilla/Custom logic).</li>
     *     <li>Pops the Minecraft {@code PoseStack}.</li>
     * </ol>
     * </p>
     */
    @Override
    public void drawText(TextComponent text, float x, float y, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        // 1. Get the current transformation state from Core
        Matrix4f uiMatrix = UIRenderer.getInstance().getTransformStack().getDirectModelMatrix();

        // 2. Synchronize: Apply Core transformations to Vanilla stack
        guiGraphics.pose().pushPose();
        guiGraphics.pose().mulPose(uiMatrix);

        // 3. Delegate to the Font object
        // The font object will either use UIRenderer (CustomFont) or RenderImpl.getGuiGraphics (VanillaFont)
        text.getFont().draw(this, text, x, y, color, shadow);

        // 4. Restore Vanilla stack
        guiGraphics.pose().popPose();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Applies the same matrix synchronization logic as {@link #drawText} to ensure
     * wrapped text blocks respect global UI transformations.
     * </p>
     */
    @Override
    public void drawWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow) {
        if (guiGraphics == null || text == null || text.getFont() == null) return;

        // 1. Get the current transformation state from Core
        Matrix4f uiMatrix = UIRenderer.getInstance().getTransformStack().getDirectModelMatrix();

        // 2. Synchronize: Apply Core transformations to Vanilla stack
        guiGraphics.pose().pushPose();
        guiGraphics.pose().mulPose(uiMatrix);

        // 3. Delegate to the Font object
        text.getFont().drawWrapped(this, text, x, y, width, color, shadow);

        // 4. Restore Vanilla stack
        guiGraphics.pose().popPose();
    }

    /**
     * {@inheritDoc}
     *
     * @return The raw GUI scale factor from the Minecraft window settings.
     */
    @Override
    public double getGuiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    // --- Vanilla Font Bridge (Platform Specific Implementation) ---

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the global line height from the Minecraft font engine.
     * This is used by the Core's {@code VanillaFont} to determine vertical layout metrics.
     * </p>
     *
     * @return The height of a standard text line in physical pixels (usually 9).
     */
    @Override
    public float getVanillaLineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Calculates the width of a {@link TextComponent} using Minecraft's native font renderer.
     * <p>
     * <b>Implementation Note:</b><br>
     * This method converts the XUI component tree into a Minecraft {@link net.minecraft.network.chat.Component}
     * tree internally before measuring, ensuring that all styles (bold, italic) are accounted for correctly.
     * </p>
     *
     * @param component The text component to measure.
     * @return The width in logical pixels.
     */
    @Override
    public float getVanillaWidth(TextComponent component) {
        if (component == null) return 0;
        return Minecraft.getInstance().font.width(toMinecraftComponent(component));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Calculates the vertical space required to render text wrapped to a specific width
     * using the Minecraft font engine.
     * </p>
     *
     * @param component The text component to measure.
     * @param maxWidth  The maximum width allowed before wrapping.
     * @return The total height in logical pixels.
     */
    @Override
    public float getVanillaWordWrapHeight(TextComponent component, float maxWidth) {
        if (component == null) return 0;
        return Minecraft.getInstance().font.wordWrapHeight(toMinecraftComponent(component), (int) maxWidth);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs the actual draw call for the {@code VanillaFont} using the active {@link GuiGraphics}.
     * </p>
     * <p>
     * <b>Matrix State:</b><br>
     * This method assumes that the caller (typically {@link #drawText(TextComponent, float, float, int, boolean)})
     * has already synchronized the Core's {@code TransformStack} onto the Vanilla {@code PoseStack}.
     * Therefore, it only applies a local Z-offset to prevent Z-fighting against background elements.
     * </p>
     *
     * @param text   The component to render.
     * @param x      The absolute X coordinate.
     * @param y      The absolute Y coordinate.
     * @param color  The ARGB color.
     * @param shadow Whether to render the shadow.
     */
    @Override
    public void drawVanillaText(TextComponent text, float x, float y, int color, boolean shadow) {
        // If we are not inside a frame (no GuiGraphics), we cannot render using Vanilla logic.
        if (this.guiGraphics == null) return;

        Component mcComp = toMinecraftComponent(text);

        // Apply a tiny Z-offset (0.05f) to ensure text renders on top of flat geometry (rects).
        // Without this, text drawn at the exact same Z-level as a button background might flicker.
        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().translate(0.0f, 0.0f, 0.05f);

        this.guiGraphics.drawString(Minecraft.getInstance().font, mcComp, (int) x, (int) y, color, shadow);

        this.guiGraphics.pose().popPose();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs the wrapped draw call for the {@code VanillaFont}.
     * Delegates to {@link GuiGraphics#drawWordWrap}.
     * </p>
     *
     * @param text   The component to render.
     * @param x      The absolute X coordinate.
     * @param y      The absolute Y coordinate.
     * @param width  The wrapping width.
     * @param color  The ARGB color.
     * @param shadow Ignored by Vanilla wrapped renderer (standard MC behavior).
     */
    @Override
    public void drawVanillaWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow) {
        if (this.guiGraphics == null) return;

        Component mcComp = toMinecraftComponent(text);

        // Z-offset for safety
        this.guiGraphics.pose().pushPose();
        this.guiGraphics.pose().translate(0.0f, 0.0f, 0.05f);

        this.guiGraphics.drawWordWrap(Minecraft.getInstance().font, mcComp, (int) x, (int) y, (int) width, color);

        this.guiGraphics.pose().popPose();
    }

    // --- Private Helpers ---

    /**
     * Converts an XUI {@link TextComponent} into a Minecraft native {@link Component}.
     * <p>
     * This mapping ensures that styles defined in the Core module (Bold, Italic, Color, etc.)
     * are correctly interpreted by the Minecraft font renderer.
     * </p>
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
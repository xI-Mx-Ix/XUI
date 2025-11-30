package net.xmx.xui.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.xmx.xui.core.UIRenderInterface;
import org.joml.Matrix4f;

/**
 * The rendering implementation for Minecraft 1.21.1.
 * Uses GuiGraphics and BufferBuilder (Modern OpenGL) instead of immediate mode.
 */
public class UIRenderImpl implements UIRenderInterface {

    private final GuiGraphics guiGraphics;
    private final Font font;

    /**
     * Constructs the renderer wrapper.
     *
     * @param guiGraphics The generic graphics context passed by the Minecraft Screen.
     */
    public UIRenderImpl(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        if (radius <= 0) {
            // Standard rectangle
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        } else {
            // Custom rounded rectangle using geometric construction
            drawRoundedRectInternal(x, y, width, height, radius, color);
        }
    }

    @Override
    public void drawString(String text, float x, float y, int color, boolean shadow) {
        guiGraphics.drawString(font, text, (int) x, (int) y, color, shadow);
    }

    @Override
    public int getStringWidth(String text) {
        return font.width(text);
    }

    @Override
    public int getFontHeight() {
        return font.lineHeight;
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        guiGraphics.enableScissor(x, y, x + width, y + height);
    }

    @Override
    public void disableScissor() {
        guiGraphics.disableScissor();
    }

    /**
     * Internal method to draw a rounded rectangle using Tesselator and TRIANGLE_FAN.
     */
    private void drawRoundedRectInternal(float x, float y, float w, float h, float r, int color) {
        // Clamp radius
        float minDim = Math.min(w, h);
        if (r > minDim / 2) r = minDim / 2;

        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red   = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Center of the shape
        buffer.addVertex(matrix, x + w / 2, y + h / 2, 0).setColor(red, green, blue, alpha);

        // Top-Right
        for (int i = 0; i <= 90; i += 15) {
            float rad = (float) Math.toRadians(i - 90);
            buffer.addVertex(matrix, x + w - r + (float) Math.cos(rad) * r, y + r + (float) Math.sin(rad) * r, 0)
                  .setColor(red, green, blue, alpha);
        }

        // Bottom-Right
        for (int i = 0; i <= 90; i += 15) {
            float rad = (float) Math.toRadians(i);
            buffer.addVertex(matrix, x + w - r + (float) Math.cos(rad) * r, y + h - r + (float) Math.sin(rad) * r, 0)
                  .setColor(red, green, blue, alpha);
        }

        // Bottom-Left
        for (int i = 0; i <= 90; i += 15) {
            float rad = (float) Math.toRadians(i + 90);
            buffer.addVertex(matrix, x + r + (float) Math.cos(rad) * r, y + h - r + (float) Math.sin(rad) * r, 0)
                  .setColor(red, green, blue, alpha);
        }

        // Top-Left
        for (int i = 0; i <= 90; i += 15) {
            float rad = (float) Math.toRadians(i + 180);
            buffer.addVertex(matrix, x + r + (float) Math.cos(rad) * r, y + r + (float) Math.sin(rad) * r, 0)
                  .setColor(red, green, blue, alpha);
        }

        // Close Loop (Back to Top-Right start)
        float rad = (float) Math.toRadians(-90);
        buffer.addVertex(matrix, x + w - r + (float) Math.cos(rad) * r, y + r + (float) Math.sin(rad) * r, 0)
              .setColor(red, green, blue, alpha);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
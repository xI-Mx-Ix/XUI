/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.xmx.xui.core.UIRenderInterface;
import org.joml.Matrix4f;

/**
 * Concrete implementation of the rendering interface.
 * This class handles the low-level rendering calls using the game's shader system,
 * Tesselator, and direct vertex buffer manipulation to draw UI elements.
 *
 * @author xI-Mx-Ix
 */
public class UIRenderImpl implements UIRenderInterface {

    private final GuiGraphics guiGraphics;
    private final Font font;

    public UIRenderImpl(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float radius) {
        // Clamp radius to half the shortest dimension to ensure geometric validity
        float minDim = Math.min(width, height);
        if (radius > minDim / 2) radius = minDim / 2;

        if (radius <= 0) {
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        } else {
            drawRoundedRectInternal(x, y, width, height, radius, color);
        }
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        if (thickness <= 0) return;

        // Clamp radius
        float minDim = Math.min(width, height);
        if (radius > minDim / 2) radius = minDim / 2;

        drawRoundedOutlineInternal(x, y, width, height, radius, thickness, color);
    }

    @Override
    public void drawString(String text, float x, float y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 0.01f);
        guiGraphics.drawString(font, text, (int) x, (int) y, color, shadow);
        guiGraphics.pose().popPose();
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

    // --- Internal Rendering Helpers ---

    private void setupRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private void drawRoundedRectInternal(float x, float y, float width, float height, float radius, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red   = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        setupRenderState();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float x1 = x + radius;
        float y1 = y + radius;
        float x2 = x + width - radius;
        float y2 = y + height - radius;

        // Center
        addQuad(buffer, matrix, x1, y1, x2, y2, red, green, blue, alpha);
        // Edges
        addQuad(buffer, matrix, x1, y, x2, y1, red, green, blue, alpha); // Top
        addQuad(buffer, matrix, x1, y2, x2, y + height, red, green, blue, alpha); // Bottom
        addQuad(buffer, matrix, x, y1, x1, y2, red, green, blue, alpha); // Left
        addQuad(buffer, matrix, x2, y1, x + width, y2, red, green, blue, alpha); // Right

        // Corners
        int segments = 16;
        addCorner(buffer, matrix, x1, y1, radius, Math.PI, Math.PI * 1.5, segments, red, green, blue, alpha);
        addCorner(buffer, matrix, x2, y1, radius, Math.PI * 1.5, Math.PI * 2.0, segments, red, green, blue, alpha);
        addCorner(buffer, matrix, x2, y2, radius, 0, Math.PI * 0.5, segments, red, green, blue, alpha);
        addCorner(buffer, matrix, x1, y2, radius, Math.PI * 0.5, Math.PI, segments, red, green, blue, alpha);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }

    /**
     * Renders a hollow rounded rectangle (border).
     * It constructs a "ring" mesh by defining an outer path and an inner path.
     */
    private void drawRoundedOutlineInternal(float x, float y, float width, float height, float radius, float thickness, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red   = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        setupRenderState();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Define outer boundary coords
        float oLeft = x;
        float oTop = y;
        float oRight = x + width;
        float oBottom = y + height;

        // Define inner boundary coords
        float iLeft = x + thickness;
        float iTop = y + thickness;
        float iRight = x + width - thickness;
        float iBottom = y + height - thickness;

        // Ensure inner bounds don't invert
        if (iLeft > iRight || iTop > iBottom) return;

        // Radii
        float rOut = radius;
        float rIn = Math.max(0, radius - thickness);

        // Center coordinates for the 4 corners
        float cx1 = oLeft + rOut;
        float cy1 = oTop + rOut; // Top-Left center
        float cx2 = oRight - rOut;
        float cy2 = oTop + rOut; // Top-Right center
        float cx3 = oRight - rOut;
        float cy3 = oBottom - rOut; // Bottom-Right center
        float cx4 = oLeft + rOut;
        float cy4 = oBottom - rOut; // Bottom-Left center

        // 1. Draw Linear Edges (Rectangles connecting the corners)
        // Top Edge
        addQuad(buffer, matrix, cx1, oTop, cx2, iTop, red, green, blue, alpha);
        // Bottom Edge
        addQuad(buffer, matrix, cx4, iBottom, cx3, oBottom, red, green, blue, alpha);
        // Left Edge
        addQuad(buffer, matrix, oLeft, cy1, iLeft, cy4, red, green, blue, alpha);
        // Right Edge
        addQuad(buffer, matrix, iRight, cy2, oRight, cy3, red, green, blue, alpha);

        // 2. Draw Rounded Corners (Ring segments)
        int segments = 16;
        addCornerRing(buffer, matrix, cx1, cy1, rOut, rIn, Math.PI, Math.PI * 1.5, segments, red, green, blue, alpha); // TL
        addCornerRing(buffer, matrix, cx2, cy2, rOut, rIn, Math.PI * 1.5, Math.PI * 2.0, segments, red, green, blue, alpha); // TR
        addCornerRing(buffer, matrix, cx3, cy3, rOut, rIn, 0, Math.PI * 0.5, segments, red, green, blue, alpha); // BR
        addCornerRing(buffer, matrix, cx4, cy4, rOut, rIn, Math.PI * 0.5, Math.PI, segments, red, green, blue, alpha); // BL

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }

    private void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, a);
    }

    private void addCorner(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius, double startAngle, double endAngle, int segments, float r, float g, float b, float a) {
        double angleStep = (endAngle - startAngle) / segments;
        double prevX = cx + Math.cos(startAngle) * radius;
        double prevY = cy + Math.sin(startAngle) * radius;

        for (int i = 1; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;

            buffer.addVertex(matrix, cx, cy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) prevX, (float) prevY, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) px, (float) py, 0).setColor(r, g, b, a);
            prevX = px;
            prevY = py;
        }
    }

    /**
     * Tesselates a partial ring (thick arc) for rounded borders.
     */
    private void addCornerRing(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float rOut, float rIn, double startAngle, double endAngle, int segments, float r, float g, float b, float a) {
        double angleStep = (endAngle - startAngle) / segments;

        double prevCos = Math.cos(startAngle);
        double prevSin = Math.sin(startAngle);

        for (int i = 1; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // Previous vertices
            float pOx = cx + (float) (prevCos * rOut);
            float pOy = cy + (float) (prevSin * rOut);
            float pIx = cx + (float) (prevCos * rIn);
            float pIy = cy + (float) (prevSin * rIn);

            // Current vertices
            float cOx = cx + (float) (cos * rOut);
            float cOy = cy + (float) (sin * rOut);
            float cIx = cx + (float) (cos * rIn);
            float cIy = cy + (float) (sin * rIn);

            // Add two triangles to form the quad for this segment
            // 1. Outer-Prev -> Inner-Prev -> Outer-Curr
            buffer.addVertex(matrix, pOx, pOy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, pIx, pIy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, cOx, cOy, 0).setColor(r, g, b, a);

            // 2. Inner-Prev -> Inner-Curr -> Outer-Curr
            buffer.addVertex(matrix, pIx, pIy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, cIx, cIy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, cOx, cOy, 0).setColor(r, g, b, a);

            prevCos = cos;
            prevSin = sin;
        }
    }
}
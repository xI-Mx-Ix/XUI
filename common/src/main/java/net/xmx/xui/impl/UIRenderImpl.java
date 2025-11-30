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
        drawRect(x, y, width, height, color, radius, radius, radius, radius);
    }

    @Override
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        if (rTL <= 0 && rTR <= 0 && rBR <= 0 && rBL <= 0) {
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
            return;
        }
        drawComplexRoundedRect(x, y, width, height, color, rTL, rTR, rBR, rBL);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float radius, float thickness) {
        drawOutline(x, y, width, height, color, thickness, radius, radius, radius, radius);
    }

    @Override
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL) {
        if (thickness <= 0) return;
        drawComplexRoundedOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
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

    @Override
    public void translateZ(float z) {
        guiGraphics.pose().translate(0, 0, z);
    }

    // --- Internal Rendering Helpers ---

    private void setupRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    /**
     * Draws a rectangle where each corner can have a different radius.
     */
    private void drawComplexRoundedRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red   = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        setupRenderState();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Inner safe rectangle (minus largest radius)
        float maxR = Math.max(Math.max(rTL, rTR), Math.max(rBR, rBL));
        // Clamp radii to 0..minDimension/2
        float minDim = Math.min(width, height);
        if (maxR > minDim/2) maxR = minDim/2;
        // Note: For perfect clamping we should clamp each radius individually against its neighbors,
        // but for UI usage this simple max clamp is usually sufficient.

        float x1 = x + maxR;
        float y1 = y + maxR;
        float x2 = x + width - maxR;
        float y2 = y + height - maxR;

        // 1. Center Body
        addQuad(buffer, matrix, x1, y1, x2, y2, red, green, blue, alpha);

        // 2. Edges
        addQuad(buffer, matrix, x + rTL, y, x + width - rTR, y + maxR, red, green, blue, alpha); // Top
        addQuad(buffer, matrix, x + rBL, y + height - maxR, x + width - rBR, y + height, red, green, blue, alpha); // Bottom
        addQuad(buffer, matrix, x, y + rTL, x + maxR, y + height - rBL, red, green, blue, alpha); // Left
        addQuad(buffer, matrix, x + width - maxR, y + rTR, x + width, y + height - rBR, red, green, blue, alpha); // Right

        // 3. Corners
        // Top-Left
        if (rTL > 0) addCorner(buffer, matrix, x + rTL, y + rTL, rTL, Math.PI, Math.PI * 1.5, 8, red, green, blue, alpha);
        else addQuad(buffer, matrix, x, y, x+maxR, y+maxR, red, green, blue, alpha); // Fill if sharp

        // Top-Right
        if (rTR > 0) addCorner(buffer, matrix, x + width - rTR, y + rTR, rTR, Math.PI * 1.5, Math.PI * 2.0, 8, red, green, blue, alpha);
        else addQuad(buffer, matrix, x+width-maxR, y, x+width, y+maxR, red, green, blue, alpha);

        // Bottom-Right
        if (rBR > 0) addCorner(buffer, matrix, x + width - rBR, y + height - rBR, rBR, 0, Math.PI * 0.5, 8, red, green, blue, alpha);
        else addQuad(buffer, matrix, x+width-maxR, y+height-maxR, x+width, y+height, red, green, blue, alpha);

        // Bottom-Left
        if (rBL > 0) addCorner(buffer, matrix, x + rBL, y + height - rBL, rBL, Math.PI * 0.5, Math.PI, 8, red, green, blue, alpha);
        else addQuad(buffer, matrix, x, y+height-maxR, x+maxR, y+height, red, green, blue, alpha);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }

    private void drawComplexRoundedOutline(float x, float y, float width, float height, int color, float thick, float rTL, float rTR, float rBR, float rBL) {
        // Simplified approach: Draw 4 lines for edges and arcs for corners
        // For a pixel-perfect outline with varying radii, signed distance fields are better,
        // but here we simply draw the 8 primitive parts.

        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float r   = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        setupRenderState();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Top Line
        addQuad(buffer, matrix, x + rTL, y, x + width - rTR, y + thick, r, g, b, alpha);
        // Bottom Line
        addQuad(buffer, matrix, x + rBL, y + height - thick, x + width - rBR, y + height, r, g, b, alpha);
        // Left Line
        addQuad(buffer, matrix, x, y + rTL, x + thick, y + height - rBL, r, g, b, alpha);
        // Right Line
        addQuad(buffer, matrix, x + width - thick, y + rTR, x + width, y + height - rBR, r, g, b, alpha);

        // Corners (Ring Segments)
        if(rTL > 0) addCornerRing(buffer, matrix, x+rTL, y+rTL, rTL, Math.max(0, rTL-thick), Math.PI, Math.PI*1.5, 8, r, g, b, alpha);
        else addQuad(buffer, matrix, x, y, x+thick, y+thick, r, g, b, alpha); // Square corner fill

        if(rTR > 0) addCornerRing(buffer, matrix, x+width-rTR, y+rTR, rTR, Math.max(0, rTR-thick), Math.PI*1.5, Math.PI*2.0, 8, r, g, b, alpha);
        else addQuad(buffer, matrix, x+width-thick, y, x+width, y+thick, r, g, b, alpha);

        if(rBR > 0) addCornerRing(buffer, matrix, x+width-rBR, y+height-rBR, rBR, Math.max(0, rBR-thick), 0, Math.PI*0.5, 8, r, g, b, alpha);
        else addQuad(buffer, matrix, x+width-thick, y+height-thick, x+width, y+height, r, g, b, alpha);

        if(rBL > 0) addCornerRing(buffer, matrix, x+rBL, y+height-rBL, rBL, Math.max(0, rBL-thick), Math.PI*0.5, Math.PI, 8, r, g, b, alpha);
        else addQuad(buffer, matrix, x, y+height-thick, x+thick, y+height, r, g, b, alpha);

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
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
            // Use the standard fill method for non-rounded rectangles
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        } else {
            // Use custom tesselation for rounded corners
            drawRoundedRectInternal(x, y, width, height, radius, color);
        }
    }

    @Override
    public void drawString(String text, float x, float y, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        // Translate Z-axis slightly to prevent z-fighting with the background
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

    /**
     * Renders a rounded rectangle by manually building the mesh.
     * The shape is constructed from five internal rectangles and four corner fans
     * to ensure correct geometry without overlapping fragments.
     */
    private void drawRoundedRectInternal(float x, float y, float width, float height, float radius, int color) {
        // Extract RGBA components from the packed integer
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red   = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue  = (color & 0xFF) / 255.0F;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        // 1. Render State Configuration
        // Reset the shader color to avoid tinting from previous operations
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Enable alpha blending for transparency support
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // Disable culling to ensure back-facing triangles (if any) are still drawn
        RenderSystem.disableCull();

        // Bind the PositionColor shader. This shader ignores texture coordinates,
        // allowing us to draw solid colored shapes without binding a white texture.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 2. Mesh Construction
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Define the inner boundaries where corners begin
        float x1 = x + radius;
        float y1 = y + radius;
        float x2 = x + width - radius;
        float y2 = y + height - radius;

        // Center Rectangle
        addQuad(buffer, matrix, x1, y1, x2, y2, red, green, blue, alpha);

        // Top Edge Rectangle
        addQuad(buffer, matrix, x1, y, x2, y1, red, green, blue, alpha);

        // Bottom Edge Rectangle
        addQuad(buffer, matrix, x1, y2, x2, y + height, red, green, blue, alpha);

        // Left Edge Rectangle
        addQuad(buffer, matrix, x, y1, x1, y2, red, green, blue, alpha);

        // Right Edge Rectangle
        addQuad(buffer, matrix, x2, y1, x + width, y2, red, green, blue, alpha);

        // Render Corners
        int segments = 16; // Number of triangles per corner fan for smoothness

        // Top Left Corner
        addCorner(buffer, matrix, x1, y1, radius, Math.PI, Math.PI * 1.5, segments, red, green, blue, alpha);

        // Top Right Corner
        addCorner(buffer, matrix, x2, y1, radius, Math.PI * 1.5, Math.PI * 2.0, segments, red, green, blue, alpha);

        // Bottom Right Corner
        addCorner(buffer, matrix, x2, y2, radius, 0, Math.PI * 0.5, segments, red, green, blue, alpha);

        // Bottom Left Corner
        addCorner(buffer, matrix, x1, y2, radius, Math.PI * 0.5, Math.PI, segments, red, green, blue, alpha);

        // 3. Upload and Draw
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // 4. State Restoration
        RenderSystem.enableCull();
    }

    /**
     * Helper method to add a quadrilateral (composed of two triangles) to the buffer.
     */
    private void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        // First Triangle
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);

        // Second Triangle
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, a);
    }

    /**
     * Helper method to add a triangle fan representing a rounded corner.
     */
    private void addCorner(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius, double startAngle, double endAngle, int segments, float r, float g, float b, float a) {
        double angleStep = (endAngle - startAngle) / segments;

        double prevX = cx + Math.cos(startAngle) * radius;
        double prevY = cy + Math.sin(startAngle) * radius;

        for (int i = 1; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;

            // Add triangle: Center -> Previous Point -> Current Point
            buffer.addVertex(matrix, cx, cy, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) prevX, (float) prevY, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) px, (float) py, 0).setColor(r, g, b, a);

            prevX = px;
            prevY = py;
        }
    }
}
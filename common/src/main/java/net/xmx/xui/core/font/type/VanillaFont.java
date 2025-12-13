/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.font.type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.impl.RenderImpl;

/**
 * Implementation of {@link Font} that delegates to the internal Minecraft font renderer.
 * Ensures consistent look and feel with the base game and resource pack support.
 *
 * @author xI-Mx-Ix
 */
public class VanillaFont extends Font {

    public VanillaFont() {
        super(Type.VANILLA);
    }

    @Override
    public float getLineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public float getWidth(TextComponent component) {
        return Minecraft.getInstance().font.width(toMinecraftComponent(component));
    }

    @Override
    public float getWordWrapHeight(TextComponent component, float maxWidth) {
        return Minecraft.getInstance().font.wordWrapHeight(toMinecraftComponent(component), (int) maxWidth);
    }

    @Override
    public void draw(RenderImpl context, TextComponent component, float x, float y, int color, boolean shadow) {
        GuiGraphics graphics = context.getGuiGraphics();
        if (graphics == null) return;

        Component mcComp = toMinecraftComponent(component);
        
        graphics.pose().pushPose();
        // Slight Z-offset to ensure text renders above background planes
        graphics.pose().translate(0, 0, 0.1f);
        graphics.drawString(Minecraft.getInstance().font, mcComp, (int) x, (int) y, color, shadow);
        graphics.pose().popPose();
    }

    @Override
    public void drawWrapped(RenderImpl context, TextComponent component, float x, float y, float maxWidth, int color, boolean shadow) {
        GuiGraphics graphics = context.getGuiGraphics();
        if (graphics == null) return;

        Component mcComp = toMinecraftComponent(component);
        // Note: GuiGraphics.drawWordWrap typically ignores the shadow parameter and doesn't use it.
        graphics.drawWordWrap(Minecraft.getInstance().font, mcComp, (int) x, (int) y, (int) maxWidth, color);
    }

    /**
     * Helper to convert XUI Component tree to Minecraft Component tree.
     */
    private Component toMinecraftComponent(TextComponent uiComp) {
        MutableComponent mcComp = Component.literal(uiComp.getText());
        Style style = Style.EMPTY
                .withBold(uiComp.isBold())
                .withItalic(uiComp.isItalic())
                .withUnderlined(uiComp.isUnderline())
                .withStrikethrough(uiComp.isStrikethrough())
                .withObfuscated(uiComp.isObfuscated());

        if (uiComp.getColor() != null) {
            style = style.withColor(TextColor.fromRgb(uiComp.getColor()));
        }

        mcComp.setStyle(style);

        for (TextComponent sibling : uiComp.getSiblings()) {
            mcComp.append(toMinecraftComponent(sibling));
        }

        return mcComp;
    }
}
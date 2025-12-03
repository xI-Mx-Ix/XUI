/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.minecraft.client.Minecraft;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.style.Properties;

/**
 * Represents a List Item in Markdown (- or *).
 * Renders a bullet point followed by wrapped text.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownListItem extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a list item component.
     *
     * @param rawText      The raw text of the item (without the bullet marker).
     * @param contentWidth The available width.
     */
    public MarkdownListItem(String rawText, float contentWidth) {
        // Transparent background
        this.style().set(Properties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Constraints.pixel(contentWidth));

        float bulletWidth = 15;

        // Bullet point doesn't need wrapping, simple creation
        UIText bullet = new UIText();
        bullet.setText("•");
        bullet.setX(Constraints.pixel(5));

        // Determine Y offset.
        // We align the bullet to the second line of the content widget (where text actually starts).
        float fontHeight = Minecraft.getInstance().font.lineHeight;
        bullet.setY(Constraints.pixel(fontHeight));
        
        this.add(bullet);

        // Content
        UIText content = MarkdownUtils.createWrappingText(
                MarkdownUtils.parseInline(rawText),
                bulletWidth,
                contentWidth
        );

        this.add(content);
        
        this.renderHeight = content.getHeight();
        this.setHeight(Constraints.pixel(renderHeight));
    }

    public float getRenderHeight() {
        return renderHeight;
    }
}
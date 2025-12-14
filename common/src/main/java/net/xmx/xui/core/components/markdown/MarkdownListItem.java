/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

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
     * @param font         The font to use.
     */
    public MarkdownListItem(String rawText, float contentWidth, Font font) {
        // Transparent background
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));

        float bulletWidth = 15;

        // Bullet point needs the same font to match size/style
        UIText bullet = new UIText();
        bullet.setText(TextComponent.literal("•").setFont(font));
        bullet.setX(Layout.pixel(5));

        float fontHeight = font.getLineHeight();
        bullet.setY(Layout.pixel(fontHeight));
        
        this.add(bullet);

        // Content
        TextComponent parsed = MarkdownUtils.parseInline(rawText);
        MarkdownUtils.applyFontRecursive(parsed, font);

        UIWrappedText content = MarkdownUtils.createWrappingText(
                parsed,
                bulletWidth,
                contentWidth
        );

        this.add(content);
        
        this.renderHeight = content.getHeight();
        this.setHeight(Layout.pixel(renderHeight));
    }

    /**
     * Returns the pre-calculated height of this component.
     *
     * @return The total vertical space this component occupies.
     */
    public float getRenderHeight() {
        return renderHeight;
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.components.markdown;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Constraints;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.style.Properties;

import java.util.List;

/**
 * Represents a Code Block in Markdown (```).
 * Renders text with syntax highlighting inside a dark box.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownCodeBlock extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a code block component.
     *
     * @param lines        The lines of code.
     * @param contentWidth The available width.
     */
    public MarkdownCodeBlock(List<String> lines, float contentWidth) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append("\n");

        String code = sb.toString();
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        // Apply syntax highlighting
        Component coloredCode = MarkdownUtils.highlightCode(code);

        // Create wrapping text
        UIWrappedText content = MarkdownUtils.createWrappingText(coloredCode, 0, contentWidth);

        float padding = 6;
        // Height Correction: Subtract empty line height
        float totalHeight = (content.getHeight() - fontHeight) + (padding * 2);

        this.setWidth(Constraints.pixel(contentWidth));
        this.setHeight(Constraints.pixel(totalHeight));

        this.style()
                .set(Properties.BACKGROUND_COLOR, 0xFF151515) // Very dark
                .set(Properties.BORDER_RADIUS, 4.0f)
                .set(Properties.BORDER_COLOR, 0xFF303030)
                .set(Properties.BORDER_THICKNESS, 1.0f);

        // Adjust content position inside panel with vertical offset fix
        content.setX(Constraints.pixel(padding));
        content.setY(Constraints.pixel(padding - fontHeight));
        content.setWidth(Constraints.pixel(contentWidth - (padding * 2)));

        this.add(content);

        // Original logic added 10px spacing after the panel
        this.renderHeight = totalHeight + 10;
    }

    public float getRenderHeight() {
        return renderHeight;
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.markdown;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIWrappedText;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.text.TextComponent;

/**
 * Represents a Task List Item in Markdown (- [ ] or - [x]).
 * Renders a visual checkbox widget followed by wrapped text.
 *
 * Updated with a vertical offset correction to align the box perfectly with text.
 *
 * @author xI-Mx-Ix
 */
public class MarkdownTaskListItem extends UIPanel {

    private final float renderHeight;

    /**
     * Constructs a task list item.
     *
     * @param rawText      The text content of the item.
     * @param isChecked    Whether the box should be checked.
     * @param contentWidth The available width.
     * @param font         The font to use.
     */
    public MarkdownTaskListItem(String rawText, boolean isChecked, float contentWidth, Font font) {
        // Transparent background for the container
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.setWidth(Layout.pixel(contentWidth));

        float fontHeight = font.getLineHeight();
        float boxSize = 10.0f;

        // Calculate Y offset:
        // 1. Start at 'fontHeight' (skipping the empty line from createWrappingText).
        // 2. Center vertically relative to line height: (fontHeight - boxSize) / 2.
        // 3. Apply manual correction (-1.0f) based on offset regression analysis.
        float centeredY = fontHeight + (fontHeight - boxSize) / 2.0f - 1.0f;

        // --- 1. The Checkbox Widget ---
        CheckboxWidget checkbox = new CheckboxWidget(isChecked);
        checkbox.setX(Layout.pixel(2)) // Small left margin
                .setY(Layout.pixel(centeredY))
                .setWidth(Layout.pixel(boxSize))
                .setHeight(Layout.pixel(boxSize));

        this.add(checkbox);

        // --- 2. The Text Content ---
        // Indent text so it doesn't overlap the box (Box + Margin)
        float textIndent = 18.0f;

        TextComponent parsed = MarkdownUtils.parseInline(rawText);
        MarkdownUtils.applyFontRecursive(parsed, font);

        UIWrappedText content = MarkdownUtils.createWrappingText(
                parsed,
                textIndent,
                contentWidth
        );

        this.add(content);

        // Calculate total height based on content
        // Ensure minimum height covers the checkbox position
        this.renderHeight = Math.max(content.getHeight(), centeredY + boxSize + 2);
        this.setHeight(Layout.pixel(renderHeight));
    }

    /**
     * Returns the pre-calculated height of this component.
     * @return The total vertical space this component occupies.
     */
    public float getRenderHeight() {
        return renderHeight;
    }

    /**
     * Internal widget to draw the visual box.
     */
    private static class CheckboxWidget extends UIWidget {
        private final boolean checked;

        public CheckboxWidget(boolean checked) {
            this.checked = checked;
            // Style: Dark Gray Background, Light Gray Border
            this.style()
                    .set(ThemeProperties.BACKGROUND_COLOR, 0xFF202020)
                    .set(ThemeProperties.BORDER_COLOR, 0xFF606060)
                    .set(ThemeProperties.BORDER_THICKNESS, 1.0f)
                    .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(2.0f))

                    // States for animation (Hover effect)
                    .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFF303030)
                    .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFF808080);
        }

        @Override
        protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
            int bg = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
            int border = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
            CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);

            // Draw Box
            renderer.getGeometry().renderRect(
                    x, y, width, height, bg,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );
            renderer.getGeometry().renderOutline(
                    x, y, width, height, border, 1.0f,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()
            );

            // Draw Checkmark if checked
            if (checked) {
                // Draw a small Green center to represent the check
                float checkPad = 2.0f;
                renderer.getGeometry().renderRect(x + checkPad, y + checkPad, width - (checkPad * 2), height - (checkPad * 2), 0xFF55FF55, 1.0f);
            }
        }
    }
}
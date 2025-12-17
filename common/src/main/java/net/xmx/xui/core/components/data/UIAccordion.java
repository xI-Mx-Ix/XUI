/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * An Accordion component.
 * Contains multiple sections, each with a clickable header that toggles the visibility of its content.
 *
 * @author xI-Mx-Ix
 */
public class UIAccordion extends UIPanel {

    private final List<AccordionSection> sections = new ArrayList<>();

    public UIAccordion() {
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
    }

    /**
     * Adds a new section to the accordion.
     *
     * @param title   The header title.
     * @param content The widget to show/hide.
     */
    public UIAccordion addSection(String title, UIWidget content) {
        AccordionSection section = new AccordionSection(title, content, this::layout);
        this.sections.add(section);
        this.add(section);
        return this;
    }

    /**
     * Updates the layout by stacking all sections vertically.
     * Updates the total height of this accordion to fit all expanded sections.
     */
    @Override
    public void layout() {
        // Reset height before calculation
        float currentY = 0;
        float myWidth = this.getWidth(); // Use currently resolved width

        for (AccordionSection section : sections) {
            // Position section
            section.setY(Layout.pixel(currentY));
            // Ensure section fills the accordion width
            section.setWidth(Layout.pixel(myWidth));

            // Force section to calculate its own height (Header + Content)
            section.layout();

            currentY += section.getHeight();
        }

        // Update total height for scrolling/parent layout
        this.setHeight(Layout.pixel(currentY));

        // Propagate layout to standard children mechanism
        super.layout();
    }

    /**
     * Internal class representing a header-content pair.
     */
    private static class AccordionSection extends UIPanel {
        private final UIButton header;
        private final UIWidget content;
        private final Runnable onToggle;
        private boolean expanded = false;

        public AccordionSection(String title, UIWidget content, Runnable onToggle) {
            this.content = content;
            this.onToggle = onToggle;

            this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

            // Create Header Button
            this.header = new UIButton();
            this.header.setLabel(TextComponent.literal(title));
            this.header.setHeight(Layout.pixel(24)); // Fixed header height
            // Header width is handled in layout()

            // Toggle expansion on click
            this.header.setOnClick(btn -> toggle());

            this.add(header);
            this.add(content);

            // Initial state: Content Hidden
            content.setVisible(false);
        }

        public void toggle() {
            this.expanded = !expanded;
            this.content.setVisible(expanded);

            // Notify parent to recalculate layout positions
            if (onToggle != null) {
                onToggle.run();
            }
        }

        @Override
        public void layout() {
            float myWidth = this.getWidth();

            // 1. Layout Header
            header.setX(Layout.pixel(0));
            header.setY(Layout.pixel(0));
            header.setWidth(Layout.pixel(myWidth));
            header.layout();

            float totalHeight = header.getHeight();

            // 2. Layout Content (if expanded)
            if (expanded) {
                content.setX(Layout.pixel(0));
                content.setY(Layout.pixel(totalHeight));
                content.setWidth(Layout.pixel(myWidth)); // Content fills width

                content.layout();
                totalHeight += content.getHeight();
            }

            // 3. Set own height
            this.setHeight(Layout.pixel(totalHeight));

            super.layout();
        }
    }
}
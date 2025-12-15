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
        AccordionSection section = new AccordionSection(title, content);
        this.sections.add(section);
        this.add(section);
        return this;
    }

    @Override
    public void layout() {
        super.layout();
        float currentY = 0;
        
        for (AccordionSection section : sections) {
            section.setY(Layout.pixel(currentY));
            section.setWidth(Layout.relative(1.0f));
            section.layout();
            currentY += section.getHeight();
        }
        
        this.height = currentY;
    }

    /**
     * Internal class representing a header-content pair.
     */
    private class AccordionSection extends UIPanel {
        private final UIButton header;
        private final UIWidget content;
        private boolean expanded = false;

        public AccordionSection(String title, UIWidget content) {
            this.content = content;
            this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

            // Create Header Button
            this.header = new UIButton();
            this.header.setLabel(TextComponent.literal(title));
            this.header.setHeight(Layout.pixel(24));
            this.header.setWidth(Layout.relative(1.0f));
            // Toggle expansion on click
            this.header.setOnClick(btn -> toggle());

            this.add(header);
            this.add(content);
            
            // Initial state
            content.setVisible(false);
        }

        public void toggle() {
            this.expanded = !expanded;
            this.content.setVisible(expanded);
            // Trigger layout recalculation of the main accordion
            UIAccordion.this.layout();
        }

        @Override
        public void layout() {
            // Position Header
            header.setY(Layout.pixel(0));
            header.layout();

            float myHeight = header.getHeight();

            // Position Content if expanded
            if (expanded) {
                content.setY(Layout.pixel(myHeight));
                content.setWidth(Layout.relative(1.0f));
                content.layout();
                myHeight += content.getHeight();
            }

            this.height = myHeight;
        }
    }
}
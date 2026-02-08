/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.data;

import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.text.TextComponent;

/**
 * A cell within a {@link UITable}.
 * Can contain simple text or any {@link UIWidget}.
 *
 * @author xI-Mx-Ix
 */
public class UITableCell extends UIPanel {

    private final UIWidget content;

    /**
     * Constructs a text cell.
     */
    public UITableCell(TextComponent text) {
        UIText textWidget = new UIText().setText(text);
        // Center text vertically
        textWidget.setY(Layout.center());
        textWidget.setX(Layout.pixel(4)); // Left padding

        this.content = textWidget;
        this.add(content);
    }

    /**
     * Constructs a widget cell (e.g., button, checkbox).
     */
    public UITableCell(UIWidget content) {
        this.content = content;
        // Basic padding
        content.setX(Layout.pixel(2));
        content.setY(Layout.pixel(2));
        content.setWidth(Layout.relative(1.0f).minus(4));
        content.setHeight(Layout.relative(1.0f).minus(4));

        this.add(content);
    }
}
/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xmx.xui.core.Layout;
import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.components.UIButton;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.core.components.UIText;
import net.xmx.xui.core.components.data.*;
import net.xmx.xui.core.components.scroll.ScrollOrientation;
import net.xmx.xui.core.components.scroll.UIScrollBar;
import net.xmx.xui.core.components.scroll.UIScrollComponent;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;

/**
 * An example screen demonstrating the three major data components:
 * {@link UITreeView}, {@link UITable}, and {@link UIListView}.
 * <p>
 * The layout mimics a dashboard with:
 * <ul>
 *     <li><b>Left:</b> A TreeView for hierarchical navigation.</li>
 *     <li><b>Center:</b> A Table for structured data display.</li>
 *     <li><b>Right:</b> A ListView for a vertical log/feed.</li>
 * </ul>
 * Each section is wrapped in a {@link UIScrollComponent} to handle large datasets.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class XUIDataComponentsExampleScreen extends Screen {

    private final UIContext uiContext = new UIContext();

    // --- Layout Constants ---
    private static final float SCREEN_PADDING = 20.0f;
    private static final float HEADER_HEIGHT = 40.0f;
    private static final float COL_GAP = 10.0f;

    public XUIDataComponentsExampleScreen() {
        super(Component.literal("Data Components Demo"));
    }

    @Override
    protected void init() {
        int wW = Minecraft.getInstance().getWindow().getWidth();
        int wH = Minecraft.getInstance().getWindow().getHeight();
        uiContext.updateLayout(wW, wH);

        if (!uiContext.isInitialized()) {
            buildUI();
        }
    }

    private void buildUI() {
        UIPanel root = uiContext.getRoot();
        root.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF121212);

        // 1. Screen Title
        UIText screenTitle = new UIText();
        screenTitle.setText(TextComponent.literal("Data Dashboard").setBold(true));
        screenTitle.setX(Layout.pixel(SCREEN_PADDING));
        screenTitle.setY(Layout.pixel(10));
        screenTitle.style().set(ThemeProperties.TEXT_COLOR, 0xFFE0E0E0);
        root.add(screenTitle);

        // 2. Calculate column widths
        // We want a layout like: | 20% Tree | 50% Table | 30% List |
        float availableWidth = uiContext.getLogicalWidth() - (SCREEN_PADDING * 2) - (COL_GAP * 2);
        float availableHeight = 300.0f; // Fixed height for demo panels

        float wTree = availableWidth * 0.20f;
        float wTable = availableWidth * 0.55f; // Slightly larger for table
        float wList = availableWidth * 0.25f;

        float startY = HEADER_HEIGHT;

        // --- Build Sections ---
        createTreeSection(root, SCREEN_PADDING, startY, wTree, availableHeight);
        createTableSection(root, SCREEN_PADDING + wTree + COL_GAP, startY, wTable, availableHeight);
        createListSection(root, SCREEN_PADDING + wTree + COL_GAP + wTable + COL_GAP, startY, wList, availableHeight);

        // --- Close Button ---
        UIButton closeBtn = new UIButton();
        closeBtn.setLabel("Close Demo");
        closeBtn.setX(Layout.center());
        closeBtn.setY(Layout.anchorEnd(20));
        closeBtn.setWidth(Layout.pixel(120));
        closeBtn.setHeight(Layout.pixel(30));
        closeBtn.setOnClick(w -> this.onClose());
        closeBtn.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFFB71C1C)
                .set(InteractionState.HOVER, ThemeProperties.BACKGROUND_COLOR, 0xFFD32F2F);

        root.add(closeBtn);

        // Initial layout pass
        root.layout();
    }

    /**
     * Creates the File Browser style TreeView.
     */
    private void createTreeSection(UIPanel parent, float x, float y, float w, float h) {
        UIPanel container = createSectionContainer(x, y, w, h, "Project Explorer");

        // 1. Scroll Wrapper
        UIScrollComponent scroll = new UIScrollComponent();
        scroll.setX(Layout.pixel(0));
        scroll.setY(Layout.pixel(24)); // Below title
        scroll.setWidth(Layout.relative(1.0f));
        scroll.setHeight(Layout.relative(1.0f).minus(24));
        scroll.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // 2. TreeView Component
        UITreeView tree = new UITreeView();

        // Root 1: Source
        var src = tree.addRoot("src");
        src.setExpanded(true);

        var main = src.addChild("main");
        main.setExpanded(true);
        main.addChild("java");
        main.addChild("resources");

        var test = src.addChild("test");
        test.addChild("java");

        // Root 2: Assets
        var assets = tree.addRoot("assets");
        assets.addChild("textures");
        assets.addChild("models");
        assets.addChild("sounds");

        // Root 3: Config
        var config = tree.addRoot("config");
        config.addChild("client.toml");
        config.addChild("server.toml");

        scroll.add(tree);
        container.add(scroll);

        // 3. Scrollbar
        addVerticalScrollbar(container, scroll);

        parent.add(container);
    }

    /**
     * Creates the Data Table section.
     */
    private void createTableSection(UIPanel parent, float x, float y, float w, float h) {
        UIPanel container = createSectionContainer(x, y, w, h, "User Database");

        // 1. Scroll Wrapper
        UIScrollComponent scroll = new UIScrollComponent();
        scroll.setX(Layout.pixel(0));
        scroll.setY(Layout.pixel(24));
        scroll.setWidth(Layout.relative(1.0f));
        scroll.setHeight(Layout.relative(1.0f).minus(24));
        scroll.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // 2. Table Component
        UITable table = new UITable();
        table.setRowHeight(28.0f);

        // A. Header definition
        UITableHeader header = new UITableHeader();
        header.addColumn("ID", 0.5f);
        header.addColumn("Username", 2.0f);
        header.addColumn("Role", 1.5f);
        header.addColumn("Status", 1.0f);
        header.addColumn("Action", 1.0f); // Button column

        table.setHeader(header);

        // B. Data Population (Mock Data)
        for (int i = 1; i <= 20; i++) {
            UITableRow row = new UITableRow();

            // ID
            row.addCell(new UITableCell(TextComponent.literal("#" + String.format("%03d", i))));

            // Name
            row.addCell(new UITableCell(TextComponent.literal("Player_" + i).setColor(0xFF90CAF9)));

            // Role
            String role = (i % 5 == 0) ? "Admin" : (i % 3 == 0) ? "Mod" : "User";
            int roleColor = role.equals("Admin") ? 0xFFFF5555 : role.equals("Mod") ? 0xFFFFAA00 : 0xFFAAAAAA;
            row.addCell(new UITableCell(TextComponent.literal(role).setColor(roleColor)));

            // Status
            boolean online = Math.random() > 0.5;
            row.addCell(new UITableCell(TextComponent.literal(online ? "Online" : "Offline")
                    .setColor(online ? 0xFF55FF55 : 0xFF555555)));

            // Action Button Cell
            UIButton btnEdit = new UIButton();
            btnEdit.setLabel("Edit");
            btnEdit.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF424242);
            int finalI = i;
            btnEdit.setOnClick(widget -> System.out.println("Editing User " + finalI));

            row.addCell(new UITableCell(btnEdit));

            table.addRow(row);
        }

        scroll.add(table);
        container.add(scroll);

        // 3. Scrollbar
        addVerticalScrollbar(container, scroll);

        parent.add(container);
    }

    /**
     * Creates the Activity Log list section.
     */
    private void createListSection(UIPanel parent, float x, float y, float w, float h) {
        UIPanel container = createSectionContainer(x, y, w, h, "Activity Log");

        // 1. Scroll Wrapper
        UIScrollComponent scroll = new UIScrollComponent();
        scroll.setX(Layout.pixel(0));
        scroll.setY(Layout.pixel(24));
        scroll.setWidth(Layout.relative(1.0f));
        scroll.setHeight(Layout.relative(1.0f).minus(24));
        scroll.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);

        // 2. ListView Component
        UIListView list = new UIListView();
        list.setItemGap(1.0f);

        // Styling: Enable zebra striping and selection highlight
        list.style()
            .set(UIListView.ALT_ROW_COLOR, 0x08FFFFFF) // Very subtle stripe
            .set(UIListView.SELECTION_COLOR, 0xFF2C3E50);

        list.setOnSelectionChange(item -> System.out.println("Selected log entry."));

        // Populate with mock log entries
        for (int i = 0; i < 30; i++) {
            list.addItem(createLogEntry("Server", "Player joined the game. [ID: " + i + "]", i));
        }

        scroll.add(list);
        container.add(scroll);

        // 3. Scrollbar
        addVerticalScrollbar(container, scroll);

        parent.add(container);
    }

    /**
     * Helper to create a styled log entry widget for the ListView.
     */
    private UIWidget createLogEntry(String source, String message, int index) {
        UIPanel panel = new UIPanel();
        panel.setHeight(Layout.pixel(30));
        panel.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000); // Transparent, relies on List striping

        UIText timestamp = new UIText();
        timestamp.setText("12:0" + (index % 10));
        timestamp.setX(Layout.pixel(4));
        timestamp.setY(Layout.center());
        timestamp.style().set(ThemeProperties.TEXT_COLOR, 0xFF666666);

        UIText content = new UIText();
        content.setText(message);
        content.setX(Layout.pixel(40));
        content.setY(Layout.center());
        content.style().set(ThemeProperties.TEXT_COLOR, 0xFFCCCCCC);

        panel.add(timestamp);
        panel.add(content);
        return panel;
    }

    /**
     * Helper to create a consistent styled container with a title bar.
     */
    private UIPanel createSectionContainer(float x, float y, float w, float h, String title) {
        UIPanel panel = new UIPanel();
        panel.setX(Layout.pixel(x));
        panel.setY(Layout.pixel(y));
        panel.setWidth(Layout.pixel(w));
        panel.setHeight(Layout.pixel(h));

        panel.style()
                .set(ThemeProperties.BACKGROUND_COLOR, 0xFF1E1E1E)
                .set(ThemeProperties.BORDER_RADIUS, CornerRadii.all(4.0f))
                .set(ThemeProperties.BORDER_COLOR, 0xFF333333)
                .set(ThemeProperties.BORDER_THICKNESS, 1.0f);

        UIText lblTitle = new UIText();
        lblTitle.setText(title);
        lblTitle.setX(Layout.pixel(8));
        lblTitle.setY(Layout.pixel(6));
        lblTitle.style().set(ThemeProperties.TEXT_COLOR, 0xFF00ADB5); // Accent color

        // Separator line
        UIPanel line = new UIPanel();
        line.setX(Layout.pixel(0));
        line.setY(Layout.pixel(23));
        line.setWidth(Layout.relative(1.0f));
        line.setHeight(Layout.pixel(1));
        line.style().set(ThemeProperties.BACKGROUND_COLOR, 0xFF333333);

        panel.add(lblTitle);
        panel.add(line);
        return panel;
    }

    /**
     * Attaches a vertical scrollbar to a scroll container.
     */
    private void addVerticalScrollbar(UIPanel parent, UIScrollComponent scroll) {
        UIScrollBar bar = new UIScrollBar(scroll, ScrollOrientation.VERTICAL);
        // Position on the right edge
        bar.setX(Layout.anchorEnd(0));
        bar.setY(Layout.pixel(24)); // Offset by header
        bar.setWidth(Layout.pixel(6));
        bar.setHeight(Layout.relative(1.0f).minus(24));

        bar.style()
                .set(UIScrollBar.TRACK_COLOR, 0x00000000)
                .set(UIScrollBar.THUMB_COLOR, 0x40FFFFFF)
                .set(UIScrollBar.THUMB_HOVER_COLOR, 0x80FFFFFF)
                .set(UIScrollBar.ROUNDING, 3.0f);

        parent.add(bar);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        uiContext.render(mouseX, mouseY, partialTick);
    }

    // --- Input Forwarding ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (uiContext.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (uiContext.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (uiContext.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (uiContext.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
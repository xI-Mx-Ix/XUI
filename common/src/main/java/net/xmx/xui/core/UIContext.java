/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.xmx.xui.core.components.UIPanel;
import net.xmx.xui.impl.UIRenderImpl;

/**
 * Manages the rendering context, scaling logic, and input transformation for a UI tree.
 * <p>
 * This class acts as the central hub for the custom UI system, decoupling it from
 * Minecraft's internal GUI scale settings. It calculates a custom {@code scaleFactor}
 * based on the physical window height to ensure that the UI maintains a consistent
 * size and aspect ratio across different resolutions.
 * </p>
 * <p>
 * It serves as a container for the {@link #root} widget and handles the delegation of
 * all input events (mouse clicks, dragging, scrolling, typing) to that root,
 * transforming the coordinates from Minecraft's GUI space to the custom logical space.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIContext {

    /**
     * The root widget of the component tree.
     * All other widgets in this context are children (or descendants) of this panel.
     */
    private final UIPanel root;

    /**
     * The current scale factor calculated by {@link #updateLayout(int, int)}.
     * <p>
     * This represents the ratio between physical window pixels and logical UI pixels.
     * </p>
     * <p>
     * <b>Pixel Perfect Mode:</b>
     * This value is strictly kept as an integer (1.0, 2.0, 3.0, etc.).
     * This ensures that fonts and geometric primitives align perfectly with the physical
     * pixel grid, avoiding artifacts where lines appear to have different widths or
     * text looks distorted/blurred.
     * </p>
     */
    private double scaleFactor = 1.0;

    /**
     * The reference height for the UI design in logical pixels.
     * <p>
     * The UI scale is calculated so that the logical height of the screen is roughly
     * this value. Since we enforce integer scaling for visual clarity, the actual logical
     * height might be larger than this reference value on some resolutions.
     * </p>
     * <p>
     * Value is set to <b>360.0</b>. This mimics Minecraft's "Auto" GUI scale logic.
     * </p>
     */
    private static final double REFERENCE_HEIGHT = 360.0;

    /**
     * Constructs a new UI Context with an initialized, empty root panel.
     * The root panel is configured to position itself at (0,0) by default.
     */
    public UIContext() {
        this.root = new UIPanel();
        // The root panel serves as the canvas and starts at the top-left corner.
        // Its width and height are updated dynamically in updateLayout().
        this.root.setX(Constraints.pixel(0));
        this.root.setY(Constraints.pixel(0));
    }

    /**
     * Updates the scale factor and logical dimensions based on the current physical window size.
     * <p>
     * This method implements an "Integer Scaling" strategy.
     * The scale factor is derived from the window height relative to {@link #REFERENCE_HEIGHT},
     * but it is floored to the nearest integer.
     * </p>
     * <p>
     * Example: If the ideal scale is 2.7, this will force it to 2.0.
     * While this might make the UI appear slightly smaller than the reference design on some
     * monitors, it guarantees that 1 logical pixel always maps to exactly N physical pixels,
     * resolving all "shimmering" or inconsistent width issues.
     * </p>
     *
     * @param windowWidth  The physical width of the window in pixels.
     * @param windowHeight The physical height of the window in pixels.
     */
    public void updateLayout(int windowWidth, int windowHeight) {
        // Calculate the raw scale ratio.
        double rawScale = (double) windowHeight / REFERENCE_HEIGHT;

        // Force Integer Scaling:
        // Round down to the nearest integer. This prevents fractional scaling artifacts.
        // e.g., 2.7 -> 2.0.
        this.scaleFactor = Math.floor(rawScale);

        // Ensure the scale is at least 1.0 to prevent division by zero or invisible UI.
        if (this.scaleFactor < 1.0) {
            this.scaleFactor = 1.0;
        }

        // Calculate the new logical dimensions by dividing physical pixels by the integer scale factor.
        float logicalWidth = (float) (windowWidth / scaleFactor);
        float logicalHeight = (float) (windowHeight / scaleFactor);

        // Apply these new dimensions to the root container.
        // This ensures the root panel always fills the "virtual" screen.
        root.setWidth(Constraints.pixel(logicalWidth));
        root.setHeight(Constraints.pixel(logicalHeight));

        // Trigger a full layout recalculation for the entire widget tree
        // so that children can adjust their positions relative to the new root size.
        root.layout();
    }

    /**
     * Renders the UI tree managed by this context.
     * <p>
     * This method orchestrates the rendering process by:
     * <ol>
     *   <li>Injecting the custom {@code scaleFactor} into the {@link UIRenderImpl}.</li>
     *   <li>Transforming the raw Minecraft mouse coordinates into logical coordinates.</li>
     *   <li>Creating a new {@link GuiGraphics} instance with a scaled {@code PoseStack}
     *       to ensure text rendering matches the custom coordinate system.</li>
     * </ol>
     * </p>
     *
     * @param mouseX       The mouse X coordinate provided by Minecraft (already scaled by MC's internal scale).
     * @param mouseY       The mouse Y coordinate provided by Minecraft (already scaled by MC's internal scale).
     * @param partialTicks The partial tick time used for smooth animations.
     */
    public void render(int mouseX, int mouseY, float partialTicks) {
        // 1. Set the custom scale in the render implementation.
        // This allows the UIRenderer (which handles shaders/VBOs) to project geometry correctly
        // and perform pixel snapping.
        UIRenderImpl.getInstance().setScale(this.scaleFactor);

        // 2. Transform input coordinates from Minecraft's GUI space to our custom logical space.
        // This is necessary because hit-testing (hover effects) happens during the render pass
        // or relies on the mouse position passed here.
        double logicalMouseX = transformMouseX(mouseX);
        double logicalMouseY = transformMouseY(mouseY);

        // 3. Setup text rendering context.
        Minecraft mc = Minecraft.getInstance();

        // We create a fresh GuiGraphics instance to manage the PoseStack for this specific rendering pass.
        // We reuse the main buffer source from Minecraft to batch render calls efficiently.
        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());

        // Calculate the relative scale adjustment.
        double mcScale = mc.getWindow().getGuiScale();
        float textScaleAdjustment = (float) (this.scaleFactor / mcScale);

        // Push a new pose onto the stack and apply the scaling.
        graphics.pose().pushPose();
        graphics.pose().scale(textScaleAdjustment, textScaleAdjustment, 1.0f);

        // Inject this specific GuiGraphics instance into the renderer implementation.
        // This allows widgets (like Text or EditBox) to draw text using this transformed stack.
        UIRenderImpl.getInstance().setGuiGraphics(graphics);

        // Render the root widget and all its descendants recursively.
        root.render(UIRenderImpl.getInstance(), (int) logicalMouseX, (int) logicalMouseY, partialTicks);

        // Flush the render buffers (endBatch) before popping the pose.
        // This forces any text or geometry buffered by GuiGraphics to be drawn to the screen
        // using the *current* scaling matrix and scissor state.
        // Without this, the draw calls might execute later after we've popped the matrix, causing artifacts.
        mc.renderBuffers().bufferSource().endBatch();

        // Restore the previous matrix state.
        graphics.pose().popPose();
    }

    /**
     * Gets the root widget of this context.
     * Use this method to add children components to the screen.
     *
     * @return The root panel widget.
     */
    public UIPanel getRoot() {
        return root;
    }

    /**
     * Checks if the root has any children widgets added to it.
     * <p>
     * This is useful for screens to determine if they need to build the UI hierarchy
     * (e.g., inside {@code init()}) or if it has already been built (preserving state).
     * </p>
     *
     * @return {@code true} if the root has one or more children; {@code false} otherwise.
     */
    public boolean isInitialized() {
        return !root.getChildren().isEmpty();
    }

    /**
     * Gets the current logical width of the UI context.
     * This value changes based on the window's aspect ratio.
     *
     * @return The width in logical pixels.
     */
    public float getLogicalWidth() {
        return root.getWidth();
    }

    // =================================================================================
    // Input Handling & Transformation
    // =================================================================================

    /**
     * Transforms the X coordinate from Minecraft's GUI space to this context's logical space.
     *
     * @param mcMouseX The X coordinate from Minecraft's mouse handler.
     * @return The X coordinate in the logical UI system.
     */
    private double transformMouseX(double mcMouseX) {
        double mcScale = Minecraft.getInstance().getWindow().getGuiScale();
        // 1. Convert MC coordinate to physical pixels: mcMouseX * mcScale
        // 2. Convert physical pixels to logical pixels: / this.scaleFactor
        return (mcMouseX * mcScale) / this.scaleFactor;
    }

    /**
     * Transforms the Y coordinate from Minecraft's GUI space to this context's logical space.
     *
     * @param mcMouseY The Y coordinate from Minecraft's mouse handler.
     * @return The Y coordinate in the logical UI system.
     */
    private double transformMouseY(double mcMouseY) {
        double mcScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (mcMouseY * mcScale) / this.scaleFactor;
    }

    /**
     * Delegates a mouse click event to the root widget after transforming coordinates.
     *
     * @param mouseX The raw mouse X from the screen.
     * @param mouseY The raw mouse Y from the screen.
     * @param button The mouse button code.
     * @return {@code true} if the event was handled by a widget.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return root.mouseClicked(transformMouseX(mouseX), transformMouseY(mouseY), button);
    }

    /**
     * Delegates a mouse release event to the root widget after transforming coordinates.
     *
     * @param mouseX The raw mouse X from the screen.
     * @param mouseY The raw mouse Y from the screen.
     * @param button The mouse button code.
     * @return {@code true} if the event was handled by a widget.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return root.mouseReleased(transformMouseX(mouseX), transformMouseY(mouseY), button);
    }

    /**
     * Delegates a mouse drag event to the root widget after transforming coordinates.
     * <p>
     * Note that the drag deltas ({@code dragX}, {@code dragY}) are also scaled
     * so that the drag speed matches the movement of the cursor in logical space.
     * </p>
     *
     * @param mouseX The raw mouse X.
     * @param mouseY The raw mouse Y.
     * @param button The mouse button code.
     * @param dragX  The raw drag delta X.
     * @param dragY  The raw drag delta Y.
     * @return {@code true} if the event was handled by a widget.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double mcScale = Minecraft.getInstance().getWindow().getGuiScale();
        // Calculate the ratio to convert drag distance from MC space to logical space
        double scaleRatio = mcScale / this.scaleFactor;

        return root.mouseDragged(
                transformMouseX(mouseX),
                transformMouseY(mouseY),
                button,
                dragX * scaleRatio,
                dragY * scaleRatio
        );
    }

    /**
     * Delegates a mouse scroll event to the root widget after transforming coordinates.
     *
     * @param mouseX      The raw mouse X.
     * @param mouseY      The raw mouse Y.
     * @param scrollDelta The amount scrolled.
     * @return {@code true} if the event was handled by a widget.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return root.mouseScrolled(transformMouseX(mouseX), transformMouseY(mouseY), scrollDelta);
    }

    /**
     * Delegates a character typed event to the root widget.
     * Coordinate transformation is not required for keyboard events.
     *
     * @param codePoint The character code typed.
     * @param modifiers The modifier keys pressed.
     * @return {@code true} if the event was handled.
     */
    public boolean charTyped(char codePoint, int modifiers) {
        return root.charTyped(codePoint, modifiers);
    }

    /**
     * Delegates a key press event to the root widget.
     * Coordinate transformation is not required for keyboard events.
     *
     * @param keyCode   The GLFW key code.
     * @param scanCode  The physical scan code.
     * @param modifiers The modifier keys pressed.
     * @return {@code true} if the event was handled.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return root.keyPressed(keyCode, scanCode, modifiers);
    }
}
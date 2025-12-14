/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.platform.PlatformRenderInterface;
import net.xmx.xui.core.platform.PlatformRenderProvider;
import net.xmx.xui.core.gl.TransformStack;
import net.xmx.xui.core.text.TextComponent;
import org.lwjgl.opengl.GL11;

/**
 * The central rendering coordinator (God Object) for the XUI system.
 * <p>
 * This singleton manages the entire rendering pipeline. It holds the global state,
 * the transformation stack, and references to all specialized sub-renderers (Geometry, Text).
 * </p>
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *     <li>Managing the lifecycle of a frame (begin/end).</li>
 *     <li>Routing drawing commands to the correct renderer.</li>
 *     <li>Managing the interface with the {@link PlatformRenderInterface}.</li>
 *     <li><b>Explicit Initialization:</b> Requires the public {@link #init()} method to be called
 *     to set up internal rendering resources.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    // --- Sub-Systems (Internal Renderers) ---
    private GlState stateManager;
    private ScissorManager scissorManager;
    private TransformStack transformStack;
    private GeometryRenderer geometryRenderer;
    private TextRenderer textRenderer;

    // --- External Dependencies ---
    /**
     * The bridge to the platform/game engine.
     * This is injected via {@link PlatformRenderProvider} during initialization.
     */
    private PlatformRenderInterface platform;

    /**
     * The current logical scale factor.
     */
    private double currentUiScale = 1.0;

    /**
     * Private constructor to enforce Singleton pattern.
     * <p>
     * <b>Note:</b> This constructor is safe to call during early initialization phases.
     * It avoids instantiating heavy rendering resources immediately.
     * The user must call {@link #init()} later to complete the setup.
     * </p>
     */
    private UIRenderer() {
    }

    /**
     * Retrieves the singleton instance of the UIRenderer.
     *
     * @return The active renderer instance.
     */
    public static UIRenderer getInstance() {
        if (instance == null) {
            instance = new UIRenderer();
        }
        return instance;
    }

    /**
     * Initializes all rendering sub-systems.
     * <p>
     * This method MUST be called exactly once before the first frame is rendered.
     * Subsequent calls are safely ignored.
     * </p>
     */
    public void init() {
        if (geometryRenderer == null) {
            this.geometryRenderer = new GeometryRenderer();
            this.textRenderer = new TextRenderer();
            this.transformStack = new TransformStack();
            this.stateManager = new GlState();
            this.scissorManager = new ScissorManager();
        }
    }

    /**
     * Injects the platform-specific backend implementation.
     * <p>
     * This method is called by the {@code PlatformRenderProvider} during the bootstrapping phase.
     * </p>
     *
     * @param platform The concrete implementation of {@link PlatformRenderInterface}.
     */
    public void setPlatform(PlatformRenderInterface platform) {
        this.platform = platform;
    }

    /**
     * Retrieves the platform backend.
     *
     * @return The backend instance.
     * @throws IllegalStateException If the backend has not been initialized.
     */
    public PlatformRenderInterface getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("PlatformRenderInterface is not initialized! Ensure PlatformRenderProvider.register() is called.");
        }
        return platform;
    }

    // =================================================================================
    // Lifecycle Management
    // =================================================================================

    /**
     * Prepares the renderer for a new frame.
     * <p>
     * This method assumes GL resources have been initialized via {@link #init()}.
     * It resets the transformation stack, initializes the platform backend with the
     * current scale, and optionally clears the depth buffer.
     * </p>
     *
     * @param uiScale          The logical scale factor for this frame.
     * @param clearDepthBuffer {@code true} if the depth buffer should be cleared.
     */
    public void beginFrame(double uiScale, boolean clearDepthBuffer) {
        this.currentUiScale = uiScale;

        // Reset global transforms for the new frame
        this.transformStack.reset();

        if (platform != null) {
            // Pass the calculated logical scale to the backend.
            // The backend uses this to scale the native PoseStack, ensuring vanilla elements
            // render at the correct size relative to the UI scale.
            platform.initiateRenderCycle(this.currentUiScale);

            // If we need a clean depth slate, clear it now.
            if (clearDepthBuffer) {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
        }
    }

    /**
     * Finalizes the current frame.
     * <p>
     * Flushes all internal batches and tells the backend to release its resources.
     * </p>
     */
    public void endFrame() {
        if (platform != null) {
            platform.finishRenderCycle();
        }
    }

    /**
     * Draws a text component.
     * <p>
     * <b>Logic:</b> Checks the font type of the component.
     * <ul>
     *     <li>If {@link Font.Type#VANILLA}: Delegates to {@link PlatformRenderInterface} with current matrix.</li>
     *     <li>If {@link Font.Type#CUSTOM}: Delegates to internal MSDF {@link TextRenderer}.</li>
     * </ul>
     * </p>
     *
     * @param text   The component to render.
     * @param x      Absolute X coordinate.
     * @param y      Absolute Y coordinate.
     * @param color  ARGB Color.
     * @param shadow Whether to render a drop shadow.
     */
    public void drawText(TextComponent text, float x, float y, int color, boolean shadow) {
        if (text == null || text.getFont() == null) return;

        if (text.getFont().getType() == Font.Type.VANILLA) {
            // Logic: Pass the current transform stack so the backend knows where to draw
            getPlatform().renderNativeText(text, x, y, color, shadow, transformStack.getDirectModelMatrix());
        } else {
            // Logic: Custom fonts use our internal renderer logic
            if (textRenderer != null) {
                text.getFont().draw(this, text, x, y, color, shadow);
            }
        }
    }

    /**
     * Draws a text component that wraps automatically within a width.
     *
     * @param text   The component to render.
     * @param x      Absolute X coordinate.
     * @param y      Absolute Y coordinate.
     * @param width  The maximum width before wrapping.
     * @param color  ARGB Color.
     * @param shadow Whether to render a drop shadow.
     */
    public void drawWrappedText(TextComponent text, float x, float y, float width, int color, boolean shadow) {
        if (text == null || text.getFont() == null) return;

        if (text.getFont().getType() == Font.Type.VANILLA) {
            getPlatform().renderNativeWrappedText(text, x, y, width, color, shadow, transformStack.getDirectModelMatrix());
        } else {
            if (textRenderer != null) {
                text.getFont().drawWrapped(this, text, x, y, width, color, shadow);
            }
        }
    }

    // =================================================================================
    // State Management API (Matrix, Scissor)
    // =================================================================================

    public void pushMatrix() {
        transformStack.push();
    }

    public void popMatrix() {
        transformStack.pop();
    }

    public void translate(float x, float y, float z) {
        transformStack.applyTranslation(x, y, z);
    }

    public void rotate(float degrees, float x, float y, float z) {
        transformStack.applyRotation(degrees, x, y, z);
    }

    public void scale(float x, float y, float z) {
        transformStack.applyScaling(x, y, z);
    }

    // =================================================================================
    // Internal Getters
    // =================================================================================

    public TransformStack getTransformStack() {
        return transformStack;
    }

    /**
     * Retrieves the State Manager responsible for OpenGL capability handling.
     *
     * @return The GlState manager.
     */
    public GlState getStateManager() {
        return stateManager;
    }

    /**
     * Retrieves the Scissor Manager responsible for clipping operations.
     *
     * @return The ScissorManager instance.
     */
    public ScissorManager getScissor() {
        return scissorManager;
    }

    /**
     * Retrieves the Geometry Renderer responsible for drawing shapes.
     *
     * @return The GeometryRenderer instance.
     * @throws IllegalStateException If {@link #init()} has not been called.
     */
    public GeometryRenderer getGeometry() {
        if (geometryRenderer == null) {
            throw new IllegalStateException("UIRenderer has not been initialized. Call UIRenderer.getInstance().init() first.");
        }
        return geometryRenderer;
    }

    /**
     * Retrieves the Text Renderer responsible for font drawing.
     *
     * @return The TextRenderer instance.
     * @throws IllegalStateException If {@link #init()} has not been called.
     */
    public TextRenderer getText() {
        if (textRenderer == null) {
            throw new IllegalStateException("UIRenderer has not been initialized. Call UIRenderer.getInstance().init() first.");
        }
        return textRenderer;
    }

    public double getCurrentUiScale() {
        return currentUiScale;
    }
}
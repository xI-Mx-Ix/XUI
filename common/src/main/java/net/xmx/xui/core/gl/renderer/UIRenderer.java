/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.PlatformRenderBackend;
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
 *     <li>Routing drawing commands to the correct renderer (e.g., determining if text is Vanilla or MSDF).</li>
 *     <li>Managing the interface with the {@link PlatformRenderBackend}.</li>
 *     <li><b>Lazy Initialization:</b> Ensures OpenGL resources (Shaders) are only created
 *     after the game engine has established a valid OpenGL context.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    // --- Sub-Systems (Internal Renderers) ---
    private final GlState stateManager;
    private final ScissorManager scissorManager;
    private final TransformStack transformStack;

    // These are initialized lazily to prevent OpenGL crashes during mod loading
    private GeometryRenderer geometryRenderer;
    private TextRenderer textRenderer;
    private boolean initialized = false;

    // --- External Dependencies ---
    /**
     * The bridge to the platform/game engine.
     * This is injected via {@link net.xmx.xui.core.gl.RenderProvider} during initialization.
     */
    private PlatformRenderBackend backend;

    /**
     * The current logical scale factor.
     */
    private double currentUiScale = 1.0;

    /**
     * Private constructor to enforce Singleton pattern.
     * <p>
     * <b>Note on GL Context:</b> This constructor is safe to call during mod initialization
     * because it does NOT instantiate the {@link GeometryRenderer} or {@link TextRenderer} immediately.
     * Those classes require an active OpenGL context (for Shader compilation), which is not
     * available during the {@code onInitialize} phase.
     * </p>
     */
    private UIRenderer() {
        // Safe initializations (Pure Java/Math logic)
        this.transformStack = new TransformStack();
        this.stateManager = new GlState();
        this.scissorManager = new ScissorManager();
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
     * Ensures that OpenGL-dependent sub-renderers are initialized.
     * <p>
     * This method is called automatically at the start of {@link #beginFrame}.
     * By deferring initialization until the first render pass, we guarantee that
     * the game window and OpenGL context exist.
     * </p>
     */
    private void ensureInitialized() {
        if (!initialized) {
            this.geometryRenderer = new GeometryRenderer();
            this.textRenderer = new TextRenderer();
            this.initialized = true;
        }
    }

    /**
     * Injects the platform-specific backend implementation.
     * <p>
     * This method is called by the {@code RenderProvider} during the bootstrapping phase.
     * </p>
     *
     * @param backend The concrete implementation of {@link PlatformRenderBackend}.
     */
    public void setBackend(PlatformRenderBackend backend) {
        this.backend = backend;
    }

    /**
     * Retrieves the platform backend.
     *
     * @return The backend instance.
     * @throws IllegalStateException If the backend has not been initialized.
     */
    public PlatformRenderBackend getBackend() {
        if (backend == null) {
            throw new IllegalStateException("PlatformRenderBackend is not initialized! Ensure RenderProvider.register() is called.");
        }
        return backend;
    }

    // =================================================================================
    // Lifecycle Management
    // =================================================================================

    /**
     * Prepares the renderer for a new frame.
     * <p>
     * This method ensures GL resources are initialized, resets the transformation stack,
     * initializes the platform backend with the current scale, and optionally clears the depth buffer.
     * </p>
     *
     * @param uiScale          The logical scale factor for this frame.
     * @param clearDepthBuffer {@code true} if the depth buffer should be cleared.
     */
    public void beginFrame(double uiScale, boolean clearDepthBuffer) {
        // Critical: Ensure Shaders and Buffers are created now that we have a Context
        ensureInitialized();

        this.currentUiScale = uiScale;

        // Reset global transforms for the new frame
        this.transformStack.reset();

        if (backend != null) {
            // Pass the calculated logical scale to the backend.
            // The backend uses this to scale the native PoseStack, ensuring vanilla elements
            // render at the correct size relative to the UI scale.
            backend.initiateRenderCycle(this.currentUiScale);

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
        if (backend != null) {
            backend.finishRenderCycle();
        }
    }

    /**
     * Draws a text component.
     * <p>
     * <b>Logic:</b> Checks the font type of the component.
     * <ul>
     *     <li>If {@link Font.Type#VANILLA}: Delegates to {@link PlatformRenderBackend} with current matrix.</li>
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
            getBackend().renderNativeText(text, x, y, color, shadow, transformStack.getDirectModelMatrix());
        } else {
            // Logic: Custom fonts use our internal renderer logic
            // Requires initialization check
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
            getBackend().renderNativeWrappedText(text, x, y, width, color, shadow, transformStack.getDirectModelMatrix());
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
     */
    public GeometryRenderer getGeometry() {
        ensureInitialized(); // Safety access
        return geometryRenderer;
    }

    /**
     * Retrieves the Text Renderer responsible for font drawing.
     *
     * @return The TextRenderer instance.
     */
    public TextRenderer getText() {
        ensureInitialized(); // Safety access
        return textRenderer;
    }

    public double getCurrentUiScale() {
        return currentUiScale;
    }
}
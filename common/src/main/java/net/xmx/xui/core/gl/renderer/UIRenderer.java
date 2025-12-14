/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.gl.TransformStack;

/**
 * The central hub for the UI rendering system.
 * <p>
 * This class acts as a container for the rendering state and the specialized sub-renderers.
 * It is responsible for maintaining the Global Transformation Stack and the current UI Scale,
 * but it does <b>not</b> contain direct drawing logic.
 * </p>
 * <p>
 * Access to drawing capabilities is provided through the getters:
 * <ul>
 *     <li>{@link #getGeometry()} - For shapes and outlines.</li>
 *     <li>{@link #getText()} - For font rendering.</li>
 *     <li>{@link #getScissor()} - For clipping regions.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    // Sub-systems
    private final GlState stateManager;
    private final ScissorManager scissorManager;
    private final GeometryRenderer geometryRenderer;
    private final TextRenderer textRenderer;

    // Global State
    private final TransformStack transformStack;
    private double currentUiScale = 1.0;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes all sub-renderers and the transform stack.
     */
    private UIRenderer() {
        this.stateManager = new GlState();
        this.scissorManager = new ScissorManager();
        this.geometryRenderer = new GeometryRenderer();
        this.textRenderer = new TextRenderer();
        this.transformStack = new TransformStack();
    }

    /**
     * Retrieves the singleton instance of the UIRenderer.
     *
     * @return The active UIRenderer instance.
     */
    public static UIRenderer getInstance() {
        if (instance == null) {
            instance = new UIRenderer();
        }
        return instance;
    }

    // --- State Accessors ---

    /**
     * Updates the global UI scale factor for the current frame.
     * This is typically called by the platform implementation at the start of a frame.
     *
     * @param scale The new scale factor.
     */
    public void setCurrentUiScale(double scale) {
        this.currentUiScale = scale;
    }

    /**
     * Retrieves the current logical UI scale factor.
     *
     * @return The scale factor.
     */
    public double getCurrentUiScale() {
        return currentUiScale;
    }

    /**
     * Retrieves the Global Transformation Stack.
     *
     * @return The transform stack instance.
     */
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
        return geometryRenderer;
    }

    /**
     * Retrieves the Text Renderer responsible for font drawing.
     *
     * @return The TextRenderer instance.
     */
    public TextRenderer getText() {
        return textRenderer;
    }

    // --- Matrix Passthrough ---

    /**
     * Pushes a new matrix onto the stack.
     */
    public void pushMatrix() {
        transformStack.push();
    }

    /**
     * Pops the current matrix from the stack.
     */
    public void popMatrix() {
        transformStack.pop();
    }

    /**
     * Applies a translation to the current matrix.
     */
    public void translate(float x, float y, float z) {
        transformStack.applyTranslation(x, y, z);
    }

    /**
     * Applies a rotation to the current matrix.
     */
    public void rotate(float degrees, float x, float y, float z) {
        transformStack.applyRotation(degrees, x, y, z);
    }

    /**
     * Applies a scaling to the current matrix.
     */
    public void scale(float x, float y, float z) {
        transformStack.applyScaling(x, y, z);
    }
}
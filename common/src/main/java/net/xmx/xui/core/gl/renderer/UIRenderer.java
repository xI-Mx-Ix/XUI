/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import org.joml.Matrix4f;

/**
 * The central coordinator for the UI rendering system.
 * Acts as a facade, delegating specific rendering tasks to modular sub-components.
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    private final GlState stateManager;
    private final ScissorManager scissorManager;
    private final GeometryRenderer geometryRenderer;
    private final TextRenderer textRenderer;

    private UIRenderer() {
        this.stateManager = new GlState();
        this.scissorManager = new ScissorManager();
        this.geometryRenderer = new GeometryRenderer();
        this.textRenderer = new TextRenderer();
    }

    public static UIRenderer getInstance() {
        if (instance == null) {
            instance = new UIRenderer();
        }
        return instance;
    }

    // --- Accessors ---

    /**
     * Retrieves the state manager responsible for capturing and restoring OpenGL state.
     * This allows external renderers (like Fonts) to isolate their draw calls from the game engine.
     *
     * @return The GlState instance.
     */
    public GlState getStateManager() {
        return stateManager;
    }

    public ScissorManager getScissor() {
        return scissorManager;
    }

    public TextRenderer getText() {
        return textRenderer;
    }

    public GeometryRenderer getGeometry() {
        return geometryRenderer;
    }

    // --- High-Level Geometry API ---

    /**
     * Renders a rectangle with configurable rounded corners.
     * Automatically handles GL state saving/restoring and shader setup,
     * applying the provided ModelView matrix for transformations.
     *
     * @param x              Logical X.
     * @param y              Logical Y.
     * @param width          Logical Width.
     * @param height         Logical Height.
     * @param color          ARGB Color.
     * @param rTL            Top-Left radius.
     * @param rTR            Top-Right radius.
     * @param rBR            Bottom-Right radius.
     * @param rBL            Bottom-Left radius.
     * @param guiScale       Current GUI scale.
     * @param modelViewMatrix The current transformation matrix.
     */
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL, double guiScale, Matrix4f modelViewMatrix) {
        stateManager.capture();
        stateManager.setupForUI();

        geometryRenderer.begin(guiScale, modelViewMatrix);
        geometryRenderer.drawRect(x, y, width, height, color, rTL, rTR, rBR, rBL);
        geometryRenderer.end();

        stateManager.restore();
    }

    /**
     * Renders an outline with configurable rounded corners.
     * Applies the provided ModelView matrix for transformations.
     */
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL, double guiScale, Matrix4f modelViewMatrix) {
        stateManager.capture();
        stateManager.setupForUI();

        geometryRenderer.begin(guiScale, modelViewMatrix);
        geometryRenderer.drawOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
        geometryRenderer.end();

        stateManager.restore();
    }
}
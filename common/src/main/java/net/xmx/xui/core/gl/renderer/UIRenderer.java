/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

/**
 * The central coordinator for the UI rendering system.
 * Acts as a facade, delegating specific rendering tasks to modular sub-components.
 *
 * @author xI-Mx-Ix
 */
public class UIRenderer {

    private static UIRenderer instance;

    private final GLState stateManager;
    private final UIScissorManager scissorManager;
    private final UIGeometryRenderer geometryRenderer;
    private final UITextRenderer textRenderer;

    private UIRenderer() {
        this.stateManager = new GLState();
        this.scissorManager = new UIScissorManager();
        this.geometryRenderer = new UIGeometryRenderer();
        this.textRenderer = new UITextRenderer();
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
     * @return The GLState instance.
     */
    public GLState getStateManager() {
        return stateManager;
    }

    public UIScissorManager getScissor() {
        return scissorManager;
    }

    public UITextRenderer getText() {
        return textRenderer;
    }

    // --- High-Level Geometry API ---

    /**
     * Renders a rectangle with configurable rounded corners.
     * Automatically handles GL state saving/restoring and shader setup.
     */
    public void drawRect(float x, float y, float width, float height, int color, float rTL, float rTR, float rBR, float rBL, double guiScale) {
        stateManager.capture();
        stateManager.setupForUI();

        geometryRenderer.begin(guiScale);
        geometryRenderer.drawRect(x, y, width, height, color, rTL, rTR, rBR, rBL);
        geometryRenderer.end();

        stateManager.restore();
    }

    /**
     * Renders an outline with configurable rounded corners.
     */
    public void drawOutline(float x, float y, float width, float height, int color, float thickness, float rTL, float rTR, float rBR, float rBL, double guiScale) {
        stateManager.capture();
        stateManager.setupForUI();

        geometryRenderer.begin(guiScale);
        geometryRenderer.drawOutline(x, y, width, height, color, thickness, rTL, rTR, rBR, rBL);
        geometryRenderer.end();

        stateManager.restore();
    }
}
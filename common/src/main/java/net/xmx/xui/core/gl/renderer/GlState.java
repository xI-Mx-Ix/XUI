/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/**
 * Manages the backup and restoration of critical OpenGL state.
 * Ensures that XUI rendering does not interfere with the game engine's internal state.
 *
 * @author xI-Mx-Ix
 */
public class GlState {

    private int previousVaoId = -1;
    private int previousVboId = -1;
    private int previousEboId = -1;
    private boolean previousBlend = false;
    private boolean previousDepth = false;
    private boolean previousCull = false;

    /**
     * Captures the current OpenGL bindings and capability states.
     */
    public void capture() {
        previousVaoId = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        previousVboId = GL15.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        previousEboId = GL15.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        previousBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        previousDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        previousCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    }

    /**
     * Restores the OpenGL state captured by {@link #capture()}.
     */
    public void restore() {
        if (previousVaoId != -1) GL30.glBindVertexArray(previousVaoId);
        if (previousVboId != -1) GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousVboId);
        if (previousEboId != -1) GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousEboId);

        if (previousBlend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        if (previousDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (previousCull) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);

        // Reset tracking vars
        previousVaoId = -1;
        previousVboId = -1;
        previousEboId = -1;
    }

    /**
     * Sets the OpenGL capabilities required for UI rendering.
     */
    public void setupForUI() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }
}
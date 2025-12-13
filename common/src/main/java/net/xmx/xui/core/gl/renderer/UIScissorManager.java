/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl.renderer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Handles scissor testing and clipping regions.
 * Manages the stack of clipping rectangles to support nested UI elements (e.g., scroll panes).
 *
 * @author xI-Mx-Ix
 */
public class UIScissorManager {

    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Pushes a new scissor rectangle onto the stack and applies it.
     *
     * @param x        Logical X.
     * @param y        Logical Y.
     * @param width    Logical Width.
     * @param height   Logical Height.
     * @param guiScale Current GUI scale factor.
     */
    public void pushScissor(int x, int y, int width, int height, double guiScale) {
        scissorStack.push(new int[]{x, y, width, height});
        applyScissor(x, y, width, height, guiScale);
    }

    /**
     * Pops the current scissor rectangle and restores the previous one.
     *
     * @param guiScale Current GUI scale factor.
     */
    public void popScissor(double guiScale) {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] prev = scissorStack.peek();
            if (prev != null) {
                applyScissor(prev[0], prev[1], prev[2], prev[3], guiScale);
            }
        }
    }

    /**
     * Gets the currently active logical scissor rectangle.
     * @return int array [x, y, w, h] or null.
     */
    public int[] getCurrentScissor() {
        return scissorStack.peek();
    }

    /**
     * Calculates the physical pixel coordinates and makes the raw OpenGL call.
     */
    private void applyScissor(int x, int y, int width, int height, double scale) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        int[] wW = new int[1];
        int[] wH = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        // Convert to physical pixels
        int sx = (int) (x * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);
        
        // Invert Y axis for OpenGL
        int sy = wH[0] - (int) ((y * scale) + sh);

        if (sx < 0) sx = 0;
        if (sy < 0) sy = 0;
        if (sw < 0) sw = 0;
        if (sh < 0) sh = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }
}
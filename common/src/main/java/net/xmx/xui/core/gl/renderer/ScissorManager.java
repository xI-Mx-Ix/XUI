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
 * Manages scissor testing and clipping regions for the UI.
 * <p>
 * This class handles the stack of clipping rectangles, supporting nested UI elements
 * (e.g., a scroll pane inside another scroll pane). It also handles the conversion
 * from logical UI pixels to physical screen pixels required by OpenGL.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class ScissorManager {

    /**
     * Stack to store previous scissor states.
     * Each entry is an int array [physicalX, physicalY, physicalWidth, physicalHeight].
     */
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Enables a new scissor region.
     * <p>
     * Converts the provided logical coordinates to physical coordinates using the
     * current UI scale from {@link UIRenderer}, pushes them onto the stack,
     * and applies the OpenGL scissor test.
     * </p>
     *
     * @param x      Logical X coordinate.
     * @param y      Logical Y coordinate.
     * @param width  Logical Width.
     * @param height Logical Height.
     */
    public void enableScissor(float x, float y, float width, float height) {
        double scale = UIRenderer.getInstance().getCurrentUiScale();

        // Calculate physical coordinates
        int sx = (int) (x * scale);
        int sy = (int) (y * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        // Store physical rect on stack for restoration
        scissorStack.push(new int[]{sx, sy, sw, sh});

        applyScissor(sx, sy, sw, sh);
    }

    /**
     * Disables the current scissor region.
     * <p>
     * Pops the top element from the stack. If the stack becomes empty, the scissor test
     * is disabled entirely. Otherwise, the previous scissor region is restored.
     * </p>
     */
    public void disableScissor() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] prev = scissorStack.peek();
            if (prev != null) {
                applyScissor(prev[0], prev[1], prev[2], prev[3]);
            }
        }
    }

    /**
     * Gets the currently active scissor rectangle in logical pixels.
     * <p>
     * This method retrieves the physical scissor rect from the top of the stack
     * and converts it back to logical pixels using the current UI scale.
     * </p>
     *
     * @return A float array {@code [x, y, width, height]} in logical pixels,
     *         or {@code null} if no scissor test is active.
     */
    public float[] getCurrentLogicalScissor() {
        int[] phys = scissorStack.peek();
        double scale = UIRenderer.getInstance().getCurrentUiScale();

        if (phys == null || scale == 0) {
            return null;
        }

        return new float[]{
                (float) (phys[0] / scale),
                (float) (phys[1] / scale), // Note: Y is relative to screen top here
                (float) (phys[2] / scale),
                (float) (phys[3] / scale)
        };
    }

    /**
     * Applies the scissor test in OpenGL.
     * Handles the Y-axis inversion required by OpenGL (Bottom-Left origin) vs UI (Top-Left origin).
     *
     * @param x Physical X.
     * @param y Physical Y (Top-Left based).
     * @param width Physical Width.
     * @param height Physical Height.
     */
    private void applyScissor(int x, int y, int width, int height) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        int[] wW = new int[1];
        int[] wH = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        // Invert Y axis for OpenGL: (WindowHeight - Y - Height)
        int glY = wH[0] - (y + height);

        // Safety clamping
        if (x < 0) x = 0;
        if (glY < 0) glY = 0;
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, glY, width, height);
    }
}
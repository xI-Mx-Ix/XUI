/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.renderer;

import net.xmx.xui.core.components.UIScrollPanel;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages scissor testing and clipping regions for the UI.
 * <p>
 * This class maintains a stack of clipping rectangles to support nested UI elements
 * (e.g., a scroll panel inside another clipped panel).
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *     <li><b>Scroll Awareness:</b> Automatically accounts for the current ModelView
 *     translation matrix. This ensures that when a {@link UIScrollPanel}
 *     shifts content visually, the clipping rectangle moves with it.</li>
 *     <li><b>Intersection Logic:</b> Ensures that a child's scissor region never
 *     extends beyond its parent's scissor region. The active scissor is always
 *     the intersection of the requested area and the current top of the stack.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class ScissorManager {

    /**
     * Stack to store active physical scissor states.
     * Each entry is an int array {@code [physicalX, physicalY, physicalWidth, physicalHeight]}.
     */
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Reusable vector to extract translation from the matrix without allocating new objects.
     */
    private final Vector3f scratchPos = new Vector3f();

    /**
     * Enables a new scissor region.
     * <p>
     * The process involves four steps:
     * <ol>
     *     <li><b>Translation:</b> Retrieve the current translation (scroll offset) from the global transform stack.</li>
     *     <li><b>scaling:</b> Convert the logical coordinates to physical window pixels using the UI scale.</li>
     *     <li><b>Intersection:</b> Clip the requested area against the currently active scissor (parent bounds).</li>
     *     <li><b>Application:</b> Push the result to the stack and execute the OpenGL command.</li>
     * </ol>
     * </p>
     *
     * @param x      Logical X coordinate of the clipping area.
     * @param y      Logical Y coordinate of the clipping area.
     * @param width  Logical Width of the clipping area.
     * @param height Logical Height of the clipping area.
     */
    public void enableScissor(float x, float y, float width, float height) {
        double scale = UIRenderer.getInstance().getCurrentUiScale();

        // --- 1. Apply Scroll Offset (Matrix Translation) ---
        // Retrieve the current translation (e.g., set by UIScrollPanel via renderer.translate)
        UIRenderer.getInstance().getTransformStack().getDirectModelMatrix().getTranslation(scratchPos);

        // Add the translation to the logical coordinates.
        // Note: scratchPos.y is usually negative when scrolling down. Adding it correctly moves
        // the virtual window "up" relative to the content, or the content "down".
        float visualX = x + scratchPos.x;
        float visualY = y + scratchPos.y;

        // --- 2. Convert to Physical Coordinates ---
        int reqX = (int) (visualX * scale);
        int reqY = (int) (visualY * scale);
        int reqW = (int) (width * scale);
        int reqH = (int) (height * scale);

        // --- 3. Intersection Logic (Clipping against Parent) ---
        int finalX = reqX;
        int finalY = reqY;
        int finalW = reqW;
        int finalH = reqH;

        // If a parent scissor is already active, the new scissor must be contained within it.
        if (!scissorStack.isEmpty()) {
            int[] parent = scissorStack.peek();
            int pX = parent[0];
            int pY = parent[1];
            int pW = parent[2];
            int pH = parent[3];

            // Calculate the geometric intersection of two rectangles
            int newLeft   = Math.max(reqX, pX);
            int newTop    = Math.max(reqY, pY);
            int newRight  = Math.min(reqX + reqW, pX + pW);
            int newBottom = Math.min(reqY + reqH, pY + pH);

            // Calculate new dimensions from intersection points
            finalX = newLeft;
            finalY = newTop;
            // Ensure non-negative dimensions (if rectangles don't overlap, width/height becomes 0)
            finalW = Math.max(0, newRight - newLeft);
            finalH = Math.max(0, newBottom - newTop);
        }

        // --- 4. Apply & Push ---
        // Store the INTERSECTED result, so subsequent children are clipped against this smaller area.
        scissorStack.push(new int[]{finalX, finalY, finalW, finalH});
        applyScissor(finalX, finalY, finalW, finalH);
    }

    /**
     * Disables the current scissor region by popping the stack.
     * <p>
     * If the stack is not empty after popping, the previous (parent) scissor state is immediately restored.
     * If the stack becomes empty, the scissor test is disabled in OpenGL.
     * </p>
     */
    public void disableScissor() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            // Restore previous (Parent) scissor state
            int[] prev = scissorStack.peek();
            if (prev != null) {
                applyScissor(prev[0], prev[1], prev[2], prev[3]);
            }
        }
    }

    /**
     * Gets the currently active scissor rectangle converted back to logical pixels.
     * <p>
     * This returns the physical scissor rect currently on the GPU, divided by the UI scale.
     * Note that this includes any scroll offsets applied during {@link #enableScissor}.
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
                (float) (phys[1] / scale),
                (float) (phys[2] / scale),
                (float) (phys[3] / scale)
        };
    }

    /**
     * Applies the scissor test to the OpenGL context.
     * <p>
     * Handles the coordinate system inversion required by OpenGL, where (0,0) is at the
     * bottom-left corner of the window, whereas UI systems typically use top-left.
     * </p>
     *
     * @param x      Physical X coordinate (Left).
     * @param y      Physical Y coordinate (Top-Left based).
     * @param width  Physical Width.
     * @param height Physical Height.
     */
    private void applyScissor(int x, int y, int width, int height) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        int[] wW = new int[1];
        int[] wH = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        // Invert Y axis calculation: GL_Y = WindowHeight - UI_Y - UI_Height
        int glY = wH[0] - (y + height);

        // Safety clamping to prevent invalid GL operations
        if (x < 0) x = 0;
        if (glY < 0) glY = 0;
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, glY, width, height);
    }
}
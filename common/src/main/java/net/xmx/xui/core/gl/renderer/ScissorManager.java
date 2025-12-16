/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.renderer;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages the OpenGL scissor test capabilities to define clipping regions for the UI.
 * <p>
 * This manager maintains a stack of clipping rectangles to support nested UI elements.
 * For example, a scroll view inside a modal dialog will clip content correctly against
 * both the scroll view's bounds and the dialog's bounds.
 * </p>
 * <p>
 * <b>Features:</b>
 * <ul>
 *     <li><b>Transform Awareness:</b> The manager accounts for the current ModelView translation.
 *     This ensures that clipping regions move dynamically with the content (e.g., during scrolling).</li>
 *     <li><b>Recursive Intersection:</b> A new scissor region is always intersected with the
 *     currently active region on the stack. This prevents a child widget from rendering outside
 *     its parent's visible area.</li>
 *     <li><b>Coordinate Scaling:</b> Automatically handles the conversion between logical UI pixels
 *     and physical display pixels (Retina/High-DPI support).</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class ScissorManager {

    /**
     * A stack containing the active physical scissor rectangles.
     * Format: {@code [physicalX, physicalY, physicalWidth, physicalHeight]}.
     */
    private final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * Temporary vector used to retrieve matrix translation without memory allocation.
     */
    private final Vector3f scratchPos = new Vector3f();

    /**
     * Activates a new clipping region.
     * <p>
     * The logical coordinates provided are transformed by the current ModelView matrix
     * (accounting for scroll offsets), scaled to physical pixels, and intersected with
     * the current parent scissor rect (if any).
     * </p>
     *
     * @param x      The logical X coordinate of the clipping area (relative to current transform).
     * @param y      The logical Y coordinate of the clipping area.
     * @param width  The logical width of the clipping area.
     * @param height The logical height of the clipping area.
     */
    public void enableScissor(float x, float y, float width, float height) {
        double scale = UIRenderer.getInstance().getCurrentUiScale();

        // 1. Retrieve the current translation from the renderer's transform stack.
        // This accounts for any active scroll offsets or container translations.
        UIRenderer.getInstance().getTransformStack().getDirectModelMatrix().getTranslation(scratchPos);

        // 2. Apply translation to determine the visual position on the virtual screen.
        float visualX = x + scratchPos.x;
        float visualY = y + scratchPos.y;

        // 3. Convert logical visual coordinates to physical window pixels.
        int reqX = (int) (visualX * scale);
        int reqY = (int) (visualY * scale);
        int reqW = (int) (width * scale);
        int reqH = (int) (height * scale);

        // 4. Perform intersection with the current parent scissor (if exists).
        int finalX = reqX;
        int finalY = reqY;
        int finalW = reqW;
        int finalH = reqH;

        if (!scissorStack.isEmpty()) {
            int[] parent = scissorStack.peek();
            int pX = parent[0];
            int pY = parent[1];
            int pW = parent[2];
            int pH = parent[3];

            // Calculate intersection rectangle (AABB)
            int newLeft = Math.max(reqX, pX);
            int newTop = Math.max(reqY, pY);
            int newRight = Math.min(reqX + reqW, pX + pW);
            int newBottom = Math.min(reqY + reqH, pY + pH);

            finalX = newLeft;
            finalY = newTop;
            finalW = Math.max(0, newRight - newLeft);
            finalH = Math.max(0, newBottom - newTop);
        }

        // 5. Push state and apply to GPU.
        scissorStack.push(new int[]{finalX, finalY, finalW, finalH});
        applyScissor(finalX, finalY, finalW, finalH);
    }

    /**
     * Deactivates the current clipping region.
     * <p>
     * Removes the top entry from the scissor stack. If the stack is empty, scissor testing
     * is disabled entirely. Otherwise, the previous (parent) scissor state is restored.
     * </p>
     */
    public void disableScissor() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }

        if (scissorStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            // Restore the parent's scissor state
            int[] prev = scissorStack.peek();
            if (prev != null) {
                applyScissor(prev[0], prev[1], prev[2], prev[3]);
            }
        }
    }

    /**
     * Retrieves the currently active scissor rectangle in logical pixels.
     * <p>
     * This reverses the scaling and coordinate transformation to provide the
     * "virtual" bounds of the current clip region.
     * </p>
     *
     * @return A float array {@code [x, y, width, height]} or {@code null} if inactive.
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
     * Executes the OpenGL commands to set the scissor rectangle.
     * <p>
     * Converts the top-left based coordinate system (UI) to the bottom-left based
     * coordinate system (OpenGL).
     * </p>
     *
     * @param x      Physical X coordinate.
     * @param y      Physical Y coordinate (Top-Left).
     * @param width  Physical Width.
     * @param height Physical Height.
     */
    private void applyScissor(int x, int y, int width, int height) {
        long windowHandle = GLFW.glfwGetCurrentContext();
        int[] wW = new int[1];
        int[] wH = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, wW, wH);

        // Convert Y to OpenGL coordinate space (Bottom-Left origin)
        int glY = wH[0] - (y + height);

        // Clamp values to prevent GL errors
        if (x < 0) x = 0;
        if (glY < 0) glY = 0;
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, glY, width, height);
    }
}
/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages a stack of matrix transformations for the UI rendering pipeline.
 * <p>
 * This class provides an independent transformation hierarchy, allowing the UI system
 * to apply translation, rotation, and scaling operations for custom shaders
 * without relying on the game engine's internal matrix stack.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class TransformStack {

    /**
     * Internal storage for the transformation history.
     */
    private final Deque<State> stack = new ArrayDeque<>();

    /**
     * Initializes the stack with a default identity state.
     */
    public TransformStack() {
        Matrix4f model = new Matrix4f();
        Matrix3f normal = new Matrix3f();
        stack.add(new State(model, normal));
    }

    /**
     * Moves the current coordinate system by the specified offsets.
     *
     * @param x The offset along the X-axis.
     * @param y The offset along the Y-axis.
     * @param z The offset along the Z-axis.
     */
    public void applyTranslation(float x, float y, float z) {
        State current = stack.getLast();
        current.modelMatrix.translate(x, y, z);
    }

    /**
     * Resizes the current coordinate system.
     * <p>
     * Also updates the normal matrix to ensure lighting calculations remain correct
     * (handling non-uniform scaling by inverting the scale for normals).
     * </p>
     *
     * @param x The scale factor for the X-axis.
     * @param y The scale factor for the Y-axis.
     * @param z The scale factor for the Z-axis.
     */
    public void applyScaling(float x, float y, float z) {
        State current = stack.getLast();
        current.modelMatrix.scale(x, y, z);

        // Adjust normal matrix: Uniform scaling is simple, non-uniform requires inversion
        if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
            // Uniform scaling: just verify direction signs
            if (x < 0.0f || y < 0.0f || z < 0.0f) {
                current.normalMatrix.scale(Math.signum(x), Math.signum(y), Math.signum(z));
            }
        } else {
            // Non-uniform scaling: invert the scale factors for normals
            current.normalMatrix.scale(1.0f / x, 1.0f / y, 1.0f / z);
        }
    }

    /**
     * Rotates the current coordinate system using Euler angles (degrees).
     *
     * @param angleDeg The angle in degrees.
     * @param axisX    The X component of the rotation axis.
     * @param axisY    The Y component of the rotation axis.
     * @param axisZ    The Z component of the rotation axis.
     */
    public void applyRotation(float angleDeg, float axisX, float axisY, float axisZ) {
        State current = stack.getLast();
        current.modelMatrix.rotate((float) Math.toRadians(angleDeg), axisX, axisY, axisZ);
        current.normalMatrix.rotate((float) Math.toRadians(angleDeg), axisX, axisY, axisZ);
    }

    /**
     * Rotates the current coordinate system using a Quaternion.
     *
     * @param quaternion The rotation quaternion.
     */
    public void applyRotation(Quaternionf quaternion) {
        State current = stack.getLast();
        current.modelMatrix.rotate(quaternion);
        current.normalMatrix.rotate(quaternion);
    }

    /**
     * Saves the current transformation state by pushing a copy onto the stack.
     */
    public void push() {
        State current = stack.getLast();
        stack.addLast(new State(current));
    }

    /**
     * Restores the previous transformation state by removing the top element.
     * Throws an exception if attempting to pop the root identity state.
     */
    public void pop() {
        if (stack.size() <= 1) {
            throw new IllegalStateException("Stack underflow: Cannot pop the root transform state.");
        }
        stack.removeLast();
    }

    /**
     * Resets the entire stack to a single Identity matrix.
     */
    public void reset() {
        stack.clear();
        Matrix4f model = new Matrix4f();
        Matrix3f normal = new Matrix3f();
        stack.add(new State(model, normal));
    }

    /**
     * Retrieves the current Model-View Matrix (top of the stack).
     *
     * @return The 4x4 transformation matrix.
     */
    public Matrix4f getDirectModelMatrix() {
        return stack.getLast().modelMatrix;
    }

    /**
     * Retrieves the current Normal Matrix (top of the stack).
     *
     * @return The 3x3 normal matrix.
     */
    public Matrix3f getDirectNormalMatrix() {
        return stack.getLast().normalMatrix;
    }

    /**
     * Represents a snapshot of the transformation state.
     * Holds the ModelView matrix and the associated Normal matrix.
     */
    private static final class State {
        final Matrix4f modelMatrix;
        final Matrix3f normalMatrix;

        /**
         * Creates a new state from raw matrices.
         */
        State(Matrix4f model, Matrix3f normal) {
            this.modelMatrix = model;
            this.normalMatrix = normal;
        }

        /**
         * Copy constructor for cloning states during push operations.
         */
        State(State other) {
            this.modelMatrix = new Matrix4f(other.modelMatrix);
            this.normalMatrix = new Matrix3f(other.normalMatrix);
        }
    }
}
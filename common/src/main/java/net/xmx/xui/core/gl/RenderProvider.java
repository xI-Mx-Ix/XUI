/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import net.xmx.xui.core.gl.renderer.UIRenderer;

/**
 * Bootstrapping utility to register the platform-specific rendering backend.
 * <p>
 * This class uses a simplified Service Locator pattern. It allows the Core module
 * to remain agnostic of the implementation (RenderImpl) while ensuring that
 * the {@link UIRenderer} receives a valid backend instance at runtime.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public final class RenderProvider {

    private RenderProvider() {
        // Prevent instantiation
    }

    /**
     * Registers the platform implementation of the render backend.
     * <p>
     * This method must be called exactly once during the client initialization phase
     * of the mod (e.g., ClientModInitializer).
     * </p>
     *
     * @param backend The concrete implementation of {@link PlatformRenderBackend}.
     */
    public static void register(PlatformRenderBackend backend) {
        UIRenderer.getInstance().setBackend(backend);
    }
}
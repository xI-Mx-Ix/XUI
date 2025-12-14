/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.core.gl;

import net.xmx.xui.core.UIContext;
import net.xmx.xui.core.UIWidget;

/**
 * Service provider for the global rendering implementation.
 * <p>
 * This class acts as a bridge (Service Locator pattern) between the abstract Core module
 * and the platform-specific Implementation module. It ensures that the Core module
 * remains completely agnostic of the underlying game engine (Minecraft).
 * </p>
 * <p>
 * The implementation module must register its concrete {@link RenderInterface} instance
 * here exactly once during the client initialization phase (e.g., inside the Client Proxy
 * or ModInitializer).
 * </p>
 *
 * @author xI-Mx-Ix
 */
public final class RenderProvider {

    /**
     * The singleton instance of the renderer implementation.
     */
    private static RenderInterface instance;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RenderProvider() {
        // Prevent instantiation
    }

    /**
     * Registers the concrete implementation of the renderer.
     * <p>
     * This method must be called during the mod's client initialization phase
     * before any UI rendering is attempted.
     * </p>
     *
     * @param implementation The concrete implementation of the renderer (e.g., {@code RenderImpl}).
     * @throws IllegalStateException If a renderer has already been registered.
     */
    public static void register(RenderInterface implementation) {
        if (instance != null) {
            throw new IllegalStateException("RenderInterface is already registered! Multiple registrations are not allowed.");
        }
        instance = implementation;
    }

    /**
     * Retrieves the active renderer implementation.
     * <p>
     * This is the primary access point for {@link UIContext} and
     * individual {@link UIWidget}s to perform drawing operations.
     * </p>
     *
     * @return The registered {@link RenderInterface} instance.
     * @throws IllegalStateException If the renderer has not been registered yet (initialization order error).
     */
    public static RenderInterface get() {
        if (instance == null) {
            throw new IllegalStateException("RenderInterface has not been registered! Ensure XuiMainClass.onClientInit() was called and registered the renderer.");
        }
        return instance;
    }
}
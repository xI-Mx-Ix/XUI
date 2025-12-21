/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.heroicons.component;

import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.gl.vertex.MeshBuffer;
import net.xmx.xui.core.heroicons.HeroIcon;
import net.xmx.xui.core.heroicons.IconType;
import net.xmx.xui.core.heroicons.atlas.HeroIconAtlas;
import net.xmx.xui.core.heroicons.atlas.HeroIconProvider;
import net.xmx.xui.core.heroicons.data.HeroIconData;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.init.XuiMainClass;

import java.util.HashSet;
import java.util.Set;

/**
 * A widget that renders a scalable Heroicon using MTSDF (Multi-channel True Signed Distance Field).
 * <p>
 * This component supports:
 * <ul>
 *     <li>Switching between {@link IconType#SOLID} and {@link IconType#OUTLINE} variants dynamically.</li>
 *     <li>Type-safe icon selection using {@link HeroIcon}.</li>
 *     <li>Sharp rendering at any scale due to distance field technology.</li>
 *     <li>Automatic error handling for missing icons (renders a placeholder).</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIHeroIcon extends UIWidget {

    /**
     * Style key for the color of the icon.
     * Defaults to the theme's text color.
     */
    public static final StyleKey<Integer> ICON_COLOR = ThemeProperties.TEXT_COLOR;

    /**
     * The name of the current icon (e.g., "home", "cog").
     * Stored as a string to allow for custom icons not in the Enum if necessary.
     */
    private String iconName = "question-mark-circle"; 

    /**
     * The visual variant of the icon (Solid vs Outline).
     */
    private IconType iconType = IconType.SOLID;

    /**
     * Internal cache to track which missing icons have already been logged to the console.
     * Prevents log spam when an icon is missing from the atlas during the render loop.
     */
    private static final Set<String> MISSING_LOGGED = new HashSet<>();

    /**
     * Constructs a default HeroIcon.
     * <p>
     * Initializes with a transparent background and white icon color by default.
     * The default icon is a "question-mark-circle".
     * </p>
     */
    public UIHeroIcon() {
        // Icons are typically rendered on a transparent background
        this.style().set(ThemeProperties.BACKGROUND_COLOR, 0x00000000);
        this.style().set(ICON_COLOR, 0xFFFFFFFF);
    }

    /**
     * Sets the icon type and name using the type-safe {@link HeroIcon} enum.
     *
     * @param type The style of the icon (SOLID or OUTLINE).
     * @param icon The standard icon to display.
     * @return This instance for chaining.
     */
    public UIHeroIcon setIcon(IconType type, HeroIcon icon) {
        return setIcon(type, icon.getName());
    }

    /**
     * Sets the icon using the type-safe {@link HeroIcon} enum,
     * preserving the current {@link IconType}.
     *
     * @param icon The standard icon to display.
     * @return This instance for chaining.
     */
    public UIHeroIcon setIcon(HeroIcon icon) {
        return setIcon(icon.getName());
    }

    /**
     * Sets the icon type and name using a raw string.
     * <p>
     * Use this method if you have added custom icons to the atlas that are not
     * present in the {@link HeroIcon} enum.
     * </p>
     *
     * @param type The style of the icon (SOLID or OUTLINE).
     * @param name The filename of the icon (without extension).
     * @return This instance for chaining.
     */
    public UIHeroIcon setIcon(IconType type, String name) {
        this.iconType = type;
        this.iconName = name;
        return this;
    }

    /**
     * Changes the visual style of the current icon (e.g., from Solid to Outline).
     *
     * @param type The new icon type.
     * @return This instance for chaining.
     */
    public UIHeroIcon setIconType(IconType type) {
        this.iconType = type;
        return this;
    }

    /**
     * Sets the icon name using a raw string, preserving the current type.
     *
     * @param name The filename of the icon (without extension).
     * @return This instance for chaining.
     */
    public UIHeroIcon setIcon(String name) {
        this.iconName = name;
        return this;
    }

    /**
     * Gets the current icon name.
     *
     * @return The string identifier of the icon.
     */
    public String getIcon() {
        return iconName;
    }

    /**
     * Gets the current icon type.
     *
     * @return The active {@link IconType}.
     */
    public IconType getIconType() {
        return iconType;
    }

    /**
     * Renders the icon using the UIRenderer's SDF pipeline.
     *
     * @param renderer     The renderer instance.
     * @param mouseX       Current mouse X position.
     * @param mouseY       Current mouse Y position.
     * @param partialTick  Partial tick for interpolation.
     * @param deltaTime    Time elapsed since last frame.
     * @param state        Current interaction state (Hover, Active, etc.).
     */
    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTick, float deltaTime, InteractionState state) {
        // 1. Resolve the correct Atlas based on the requested type (Solid/Outline)
        HeroIconAtlas atlas = HeroIconProvider.getAtlas(iconType);

        // 2. Retrieve the UV coordinates and bounds for the specific icon
        HeroIconData.IconBounds bounds = atlas.getIcon(iconName);

        // If the icon doesn't exist in the JSON, handle the error gracefully
        if (bounds == null) {
            handleMissingIcon(renderer);
            return;
        }

        // 3. Resolve the animated color from styles
        int color = getColor(ICON_COLOR, state, deltaTime);

        // 4. Prepare the Renderer state
        // We need to enable blending and disable culling for correct UI rendering.
        renderer.getStateManager().capture();
        renderer.getStateManager().setupForUI();

        // 5. Begin the SDF Batch
        // Automatically binds the MTSDF shader because the atlas type is MTSDF.
        renderer.getSdf().begin(
                renderer.getCurrentUiScale(),
                atlas,
                renderer.getTransformStack().getDirectModelMatrix()
        );

        // 6. Calculate UV Coordinates
        float atlasW = atlas.getMetadata().atlas.width;
        float atlasH = atlas.getMetadata().atlas.height;

        MeshBuffer mesh = renderer.getSdf().getMesh();

        // Extract color components (0.0 - 1.0)
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Calculate normalized UVs (0.0 to 1.0)
        // u0, v0 = Top-Left of the icon in the atlas
        // u1, v1 = Bottom-Right of the icon in the atlas
        float u0 = bounds.x / atlasW;
        float u1 = (bounds.x + bounds.width) / atlasW;

        // Note: The atlas JSON coordinates usually originate Top-Left.
        // The Renderer's projection matrix is also Ortho Top-Left (0,0 is top-left corner of screen).
        // Therefore, we map Y directly to V without inversion.
        float v0 = bounds.y / atlasH;
        float v1 = (bounds.y + bounds.height) / atlasH;

        // 7. Push Vertices to the Mesh
        // We draw a Quad (2 Triangles).
        
        // Triangle 1
        // Bottom-Left (x, y+h) -> UV (u0, v1)
        mesh.pos(x, y + height, 0).color(r, g, b, a).uv(u0, v1).endVertex();
        // Bottom-Right (x+w, y+h) -> UV (u1, v1)
        mesh.pos(x + width, y + height, 0).color(r, g, b, a).uv(u1, v1).endVertex();
        // Top-Right (x+w, y) -> UV (u1, v0)
        mesh.pos(x + width, y, 0).color(r, g, b, a).uv(u1, v0).endVertex();

        // Triangle 2
        // Top-Right (x+w, y) -> UV (u1, v0)
        mesh.pos(x + width, y, 0).color(r, g, b, a).uv(u1, v0).endVertex();
        // Top-Left (x, y) -> UV (u0, v0)
        mesh.pos(x, y, 0).color(r, g, b, a).uv(u0, v0).endVertex();
        // Bottom-Left (x, y+h) -> UV (u0, v1)
        mesh.pos(x, y + height, 0).color(r, g, b, a).uv(u0, v1).endVertex();

        // 8. Flush and Restore State
        renderer.getSdf().drawBatch(atlas.getTextureId());
        renderer.getSdf().end();
        renderer.getStateManager().restore();
    }

    /**
     * Handles cases where the requested icon name is not found in the atlas.
     * <p>
     * Renders a red placeholder box and logs an error to the console.
     * The error is logged only once per icon/type combination to prevent console spam.
     * </p>
     *
     * @param renderer The renderer instance to draw the placeholder.
     */
    private void handleMissingIcon(UIRenderer renderer) {
        String key = iconType.name() + ":" + iconName;

        if (!MISSING_LOGGED.contains(key)) {
            XuiMainClass.LOGGER.error("UIHeroIcon: Missing icon '{}' in atlas '{}'", iconName, iconType);
            MISSING_LOGGED.add(key);
        }

        // Render a visual indicator (Red Box) so the developer knows the layout is correct but the asset is missing.
        renderer.getGeometry().renderRect(x, y, width, height, 0xFFFF0000, 0);
    }
}
#version 330 core

// Input interpolated data from Vertex Shader
in vec4 fragColor;
in vec2 fragUV;

// The MSDF font texture atlas
uniform sampler2D fontTexture;

// The pixel range used when generating the MSDF texture (e.g., 2.0 or 4.0)
uniform float pxRange;

// The output color for the framebuffer
out vec4 outColor;

/**
 * Calculates the median of three values.
 * In MSDF, the true signed distance is the median of the R, G, and B channels.
 *
 * @param r The red channel value.
 * @param g The green channel value.
 * @param b The blue channel value.
 * @return The median value.
 */
float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    // 1. Retrieve the MSDF data from the texture.
    // The RGB channels contain the signed distances for different edge segments.
    vec3 msd = texture(fontTexture, fragUV).rgb;

    // 2. Compute the signed distance to the shape's edge.
    float sd = median(msd.r, msd.g, msd.b);

    // 3. Convert the normalized distance (0.0 to 1.0) to screen-space pixel distance.
    // The texture stores 0.5 as the edge. Subtracting 0.5 centers the distance at 0.
    float screenPxDistance = pxRange * (sd - 0.5);

    // 4. Calculate the opacity (alpha) using screen-space derivatives.
    // fwidth() calculates the rate of change of the distance field relative to screen pixels.
    // This creates a smooth anti-aliased edge regardless of the scale/zoom level.
    float opacity = clamp(screenPxDistance / fwidth(screenPxDistance) + 0.5, 0.0, 1.0);

    // 5. Compose the final color.
    // Use the vertex color (fragColor) and apply the calculated text opacity to its alpha.
    outColor = vec4(fragColor.rgb, fragColor.a * opacity);

    // Optimization: Discard fully transparent pixels to reduce overdraw processing.
    if (outColor.a < 0.01) discard;
}
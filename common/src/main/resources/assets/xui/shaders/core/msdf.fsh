#version 330 core

// Input interpolated data from Vertex Shader
in vec4 fragColor;
in vec2 fragUV;

// The MSDF texture atlas
uniform sampler2D msdfTexture;

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

/**
 * Computes the screen-space size of the signed distance field's range.
 *
 * This function determines how many physical screen pixels correspond to the
 * 'pxRange' defined in the atlas.
 *
 * By clamping the result to a minimum of 1.0, we ensure that the anti-aliasing
 * edge never becomes thinner than a single pixel. This allows the shape to behave
 * like mip-mapping at very small scales, remaining visible (though blurred)
 * instead of vanishing due to high-frequency noise.
 *
 * @return The width of the distance field range in screen pixels.
 */
float screenPxRange() {
    // Retrieve the dimensions of the texture atlas directly from the sampler
    vec2 texSize = vec2(textureSize(msdfTexture, 0));

    // Convert the pxRange (in atlas pixels) to UV space (0.0 to 1.0)
    vec2 unitRange = vec2(pxRange) / texSize;

    // fwidth(fragUV) returns the change in UV coordinates per screen pixel.
    // The inverse gives us the number of screen pixels per 1.0 UV unit.
    vec2 screenTexSize = vec2(1.0) / fwidth(fragUV);

    // Project the unitRange into screen space dimensions.
    // We enforce a minimum of 1.0 to prevent aliasing artifacts at small scales.
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    // 1. Retrieve the MSDF data from the generic texture
    vec3 msd = texture(msdfTexture, fragUV).rgb;

    // 2. Compute the signed distance to the shape's edge
    float sd = median(msd.r, msd.g, msd.b);

    // 3. Convert the normalized distance to screen-space pixel distance.
    // We subtract 0.5 because the edge is defined at 0.5 in the texture data.
    float screenPxDistance = screenPxRange() * (sd - 0.5);

    // 4. Calculate the opacity (alpha).
    // Adding 0.5 shifts the distance so that 0 (the edge) becomes 0.5 opacity.
    // Clamping limits the result to valid alpha values [0, 1].
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

    // 5. Compose the final color.
    // Combine the vertex color with the calculated opacity.
    outColor = vec4(fragColor.rgb, fragColor.a * opacity);

    // Discard fully transparent pixels to optimize blending operations.
    if (outColor.a < 0.01) discard;
}
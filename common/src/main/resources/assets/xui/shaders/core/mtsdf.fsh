#version 330 core

// Input interpolated data from Vertex Shader
in vec4 fragColor;
in vec2 fragUV;

// The MTSDF texture atlas (RGBA)
uniform sampler2D sdfTexture;

// The pixel range used when generating the texture
uniform float pxRange;

// Outline configuration (0.0 = no outline)
uniform float outlineWidth;
uniform vec4 outlineColor;

// The output color
out vec4 outColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 texSize = vec2(textureSize(sdfTexture, 0));
    vec2 unitRange = vec2(pxRange) / texSize;
    vec2 screenTexSize = vec2(1.0) / fwidth(fragUV);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    // 1. Fetch MTSDF data
    vec4 tex = texture(sdfTexture, fragUV);

    // 2. Calculate Distance Fields
    // MSDF Distance (Sharp corners) from RGB
    float dMsdf = median(tex.r, tex.g, tex.b);
    // True Distance (Smooth shapes/Outlines) from Alpha
    float dSdf = tex.a;

    // 3. Screen Space conversion factors
    float screenRange = screenPxRange();

    // --- Body Rendering ---
    // Use MSDF (RGB) for the main shape to get sharp corners
    float screenDistBody = screenRange * (dMsdf - 0.5);
    float opacityBody = clamp(screenDistBody + 0.5, 0.0, 1.0);

    vec4 bodyColor = vec4(fragColor.rgb, fragColor.a * opacityBody);

    // --- Outline Rendering ---
    // If outline width > 0, we compute the outline
    if (outlineWidth > 0.0) {
        // Use True SDF (Alpha) for outlines as it behaves better for expanded shapes
        // The outline is defined between (0.5 - width) and 0.5
        float screenDistOutline = screenRange * (dSdf - 0.5 + outlineWidth);
        float opacityOutline = clamp(screenDistOutline + 0.5, 0.0, 1.0);

        // The outline is the part that is opaque in 'opacityOutline' but not in 'opacityBody'
        // We composite the body on top of the outline
        // Mix factor: if we are inside the body, use body color, else use outline color

        // Simple composite: Lerp based on body opacity
        vec4 outlineFinal = vec4(outlineColor.rgb, outlineColor.a * opacityOutline);

        // This mix ensures the body draws "over" the outline
        outColor = mix(outlineFinal, bodyColor, opacityBody);
    } else {
        outColor = bodyColor;
    }

    if (outColor.a < 0.01) discard;
}
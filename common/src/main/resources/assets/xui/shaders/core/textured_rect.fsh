#version 120

uniform sampler2D tex;
uniform vec2 size;
uniform float radius;

varying vec4 v_color;
varying vec2 v_texCoord;

// SDF Function for a rounded box
float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {
    // 1. Sample the texture
    vec4 texColor = texture2D(tex, v_texCoord);

    // 2. Calculate pixel coordinates (0 to width/height) based on UV
    vec2 pixelPos = v_texCoord * size;
    vec2 center = size / 2.0;

    // 3. Calculate distance from the rounded box edge
    // We pass half-size to the SDF because it calculates from center
    float dist = roundedBoxSDF(pixelPos - center, center, radius);

    // 4. Smooth Anti-Aliased Edge
    // If dist > 0, we are outside. We smoothstep between 0.0 (inside) and 1.0 (outside) pixel range.
    float alpha = 1.0 - smoothstep(0.0, 1.0, dist);

    // 5. Combine texture, vertex color, and calculated shape alpha
    gl_FragColor = v_color * texColor;
    gl_FragColor.a *= alpha;
}
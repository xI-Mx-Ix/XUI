#version 150

in vec4 vertexColor;
in vec2 vertexUV;

uniform sampler2D fontTexture;

out vec4 fragColor;

void main() {
    // Sample the alpha value from the texture (stb_truetype generates an alpha mask)
    float alpha = texture(fontTexture, vertexUV).a;

    // Discard fully transparent pixels to save fill-rate
    if (alpha < 0.05) discard;

    // Output the vertex color (text color) multiplied by the texture alpha
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
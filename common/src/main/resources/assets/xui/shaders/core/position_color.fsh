#version 150 core

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a < 0.01) {
        discard;
    }

    fragColor = vertexColor;
}
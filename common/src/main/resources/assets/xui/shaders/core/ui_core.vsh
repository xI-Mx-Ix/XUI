#version 150 core

in vec3 position;
in vec4 color;

uniform mat4 projMat;
uniform mat4 modelViewMat;

out vec4 vertexColor;

void main() {
    // Apply both Projection and ModelView matrices
    gl_Position = projMat * modelViewMat * vec4(position, 1.0);
    vertexColor = color;
}
#version 150 core

in vec3 position;
in vec4 color;

uniform mat4 projMat;

out vec4 vertexColor;

void main() {
    gl_Position = projMat * vec4(position, 1.0);
    vertexColor = color;
}
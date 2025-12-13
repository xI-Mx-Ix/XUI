#version 150

in vec3 position;
in vec4 color;
in vec2 uv;

uniform mat4 projMat;

out vec4 vertexColor;
out vec2 vertexUV;

void main() {
    gl_Position = projMat * vec4(position, 1.0);
    vertexColor = color;
    vertexUV = uv;
}
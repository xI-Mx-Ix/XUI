#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;

uniform mat4 projMat;

out vec4 fragColor;
out vec2 fragUV;

void main() {
    gl_Position = projMat * vec4(position, 1.0);
    fragColor = color;
    fragUV = uv;
}
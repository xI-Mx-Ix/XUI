#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;

uniform mat4 projMat;
uniform mat4 modelViewMat;

out vec4 fragColor;
out vec2 fragUV;

void main() {
    // Apply both Projection and ModelView matrices
    gl_Position = projMat * modelViewMat * vec4(position, 1.0);
    fragColor = color;
    fragUV = uv;
}
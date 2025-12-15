#version 120

attribute vec3 position;
attribute vec4 color;
attribute vec2 uv;

uniform mat4 projMat;
uniform mat4 modelViewMat;

varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    gl_Position = projMat * modelViewMat * vec4(position, 1.0);
    v_color = color;
    v_texCoord = uv;
}
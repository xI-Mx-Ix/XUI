/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import java.nio.FloatBuffer;

/**
 * Manages the OpenGL shader program for UI rendering.
 * Compiles Vertex and Fragment shaders and handles uniform data uploads (like the projection matrix).
 *
 * <p>The shader expects:</p>
 * <ul>
 *     <li>Attribute 0: vec3 position</li>
 *     <li>Attribute 1: vec4 color</li>
 *     <li>Uniform "projMat": mat4 projection matrix</li>
 * </ul>
 *
 * @author xI-Mx-Ix
 */
public class UIShader {

    private final int programId;
    private final int uProjMat;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Standard GLSL 150 Vertex Shader source.
     * Transforms the vertex position by the projection matrix and passes the color to the fragment stage.
     */
    private static final String VERTEX_SRC =
            "#version 150 core\n" +
            "in vec3 position;\n" +
            "in vec4 color;\n" +
            "uniform mat4 projMat;\n" +
            "out vec4 vertexColor;\n" +
            "void main() {\n" +
            "    gl_Position = projMat * vec4(position, 1.0);\n" +
            "    vertexColor = color;\n" +
            "}\n";

    /**
     * Standard GLSL 150 Fragment Shader source.
     * Outputs the interpolated color from the vertex stage.
     */
    private static final String FRAGMENT_SRC =
            "#version 150 core\n" +
            "in vec4 vertexColor;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = vertexColor;\n" +
            "}\n";

    /**
     * Compiles and links the shader program.
     *
     * @throws RuntimeException if compilation or linking fails.
     */
    public UIShader() {
        int vShader = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SRC);
        int fShader = compileShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SRC);

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vShader);
        GL20.glAttachShader(programId, fShader);

        // Bind attributes before linking to ensure fixed locations
        GL20.glBindAttribLocation(programId, 0, "position");
        GL20.glBindAttribLocation(programId, 1, "color");

        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader: " + GL20.glGetProgramInfoLog(programId));
        }

        // Retrieve uniform locations
        uProjMat = GL20.glGetUniformLocation(programId, "projMat");

        // Detach and delete individual shaders as they are now part of the program
        GL20.glDetachShader(programId, vShader);
        GL20.glDetachShader(programId, fShader);
        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);
    }

    /**
     * Compiles a single shader source.
     *
     * @param type The OpenGL shader type (e.g., GL_VERTEX_SHADER).
     * @param src  The GLSL source code.
     * @return The shader ID.
     */
    private int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader: " + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    /**
     * Binds this shader program for rendering.
     */
    public void bind() {
        GL20.glUseProgram(programId);
    }

    /**
     * Unbinds the current shader program.
     */
    public void unbind() {
        GL20.glUseProgram(0);
    }

    /**
     * Uploads a 4x4 matrix to the 'projMat' uniform.
     *
     * @param matrix The projection matrix to upload.
     */
    public void uploadProjection(Matrix4f matrix) {
        matrixBuffer.clear();
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(uProjMat, false, matrixBuffer);
    }
}
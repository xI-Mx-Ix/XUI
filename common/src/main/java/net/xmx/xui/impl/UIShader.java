/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.impl;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Manages the OpenGL shader program for UI rendering.
 * Handles loading GLSL source code from the classpath, compilation, linking,
 * and uniform data uploads.
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
     * Creates a new shader instance by loading the vertex and fragment shaders
     * from the classpath using the standard asset structure.
     * <p>
     * The method looks for files at: {@code /assets/{namespace}/shaders/{path}.[vsh/fsh]}
     * </p>
     *
     * @param namespace The resource namespace (e.g., "xui").
     * @param path      The path relative to the shaders folder (e.g., "core/ui_core").
     * @return A compiled and linked {@link UIShader}.
     * @throws RuntimeException if the resources cannot be found or compilation fails.
     */
    public static UIShader ofResource(String namespace, String path) {
        String basePath = "/assets/" + namespace + "/shaders/" + path;
        String vertSource = loadResource(basePath + ".vsh");
        String fragSource = loadResource(basePath + ".fsh");
        return new UIShader(vertSource, fragSource);
    }

    /**
     * Private constructor used by the factory method.
     * Compiles and links the provided source strings.
     *
     * @param vertSrc The vertex shader source code.
     * @param fragSrc The fragment shader source code.
     */
    private UIShader(String vertSrc, String fragSrc) {
        int vShader = compileShader(GL20.GL_VERTEX_SHADER, vertSrc);
        int fShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc);

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
     * Reads a file from the classpath into a String.
     *
     * @param fullPath The absolute path to the resource in the classpath.
     * @return The file content.
     * @throws RuntimeException if the file cannot be found or read.
     */
    private static String loadResource(String fullPath) {
        try (InputStream is = UIShader.class.getResourceAsStream(fullPath)) {
            if (is == null) {
                throw new RuntimeException("Shader file not found in classpath: " + fullPath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + fullPath, e);
        }
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
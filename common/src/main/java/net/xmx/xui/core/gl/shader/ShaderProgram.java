/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.gl.shader;

import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Abstract base class for all UI shader programs.
 * <p>
 * This class handles the loading, compilation, and linking of GLSL source code.
 * Subclasses are responsible for defining specific vertex attributes and
 * retrieving their unique uniform locations.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public abstract class ShaderProgram {

    private final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;

    /**
     * Constructs the shader program by loading sources from the classpath.
     * <p>
     * The file path convention is: {@code /assets/{namespace}/shaders/{path}.[vsh/fsh]}
     * </p>
     *
     * @param namespace The resource namespace (e.g., "xui").
     * @param path      The relative path to the shader name (e.g., "core/ui_core").
     */
    public ShaderProgram(String namespace, String path) {
        String basePath = "/assets/" + namespace + "/shaders/" + path;
        String vertSource = loadResource(basePath + ".vsh");
        String fragSource = loadResource(basePath + ".fsh");

        // 1. Compile individual shaders
        vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertSource);
        fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragSource);

        // 2. Create Program and Attach
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShaderId);
        GL20.glAttachShader(programId, fragmentShaderId);

        // 3. Bind Attributes (Must be done before linking)
        registerAttributes();

        // 4. Link Program
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader [" + path + "]: " + GL20.glGetProgramInfoLog(programId));
        }

        // 5. Validate Program
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader [" + path + "]: " + GL20.glGetProgramInfoLog(programId));
        }

        // 6. Retrieve Uniform Locations (Must be done after linking)
        registerUniforms();
    }

    /**
     * Subclasses must implement this to bind vertex attributes to specific indices.
     * <p>
     * This method is called <b>before</b> the program is linked.
     * Use {@link #bindAttribute(int, String)} within this method.
     * </p>
     */
    protected abstract void registerAttributes();

    /**
     * Subclasses must implement this to query and store uniform locations.
     * <p>
     * This method is called <b>after</b> the program is linked.
     * Use {@link #getUniformLocation(String)} within this method.
     * </p>
     */
    protected abstract void registerUniforms();

    /**
     * Binds a vertex attribute variable to a specific index.
     *
     * @param attribute The index to bind to (e.g., 0 for position).
     * @param variableName The name of the variable in the GLSL vertex shader.
     */
    protected void bindAttribute(int attribute, String variableName) {
        GL20.glBindAttribLocation(programId, attribute, variableName);
    }

    /**
     * Retrieves the location ID of a uniform variable.
     *
     * @param uniformName The name of the uniform in the GLSL code.
     * @return The integer location ID.
     */
    protected int getUniformLocation(String uniformName) {
        return GL20.glGetUniformLocation(programId, uniformName);
    }

    /**
     * Installs this program as part of the current rendering state.
     */
    public void bind() {
        GL20.glUseProgram(programId);
    }

    /**
     * Uninstalls the shader program.
     */
    public void unbind() {
        GL20.glUseProgram(0);
    }

    /**
     * Deletes the program and detached shaders to free GPU memory.
     */
    public void cleanup() {
        unbind();
        GL20.glDetachShader(programId, vertexShaderId);
        GL20.glDetachShader(programId, fragmentShaderId);
        GL20.glDeleteShader(vertexShaderId);
        GL20.glDeleteShader(fragmentShaderId);
        GL20.glDeleteProgram(programId);
    }

    /**
     * Compiles a shader source string.
     */
    private int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader type " + type + ": " + GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    /**
     * Reads a file resource from the classpath.
     */
    private static String loadResource(String fullPath) {
        try (InputStream is = ShaderProgram.class.getResourceAsStream(fullPath)) {
            if (is == null) {
                throw new RuntimeException("Shader file not found: " + fullPath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + fullPath, e);
        }
    }
}
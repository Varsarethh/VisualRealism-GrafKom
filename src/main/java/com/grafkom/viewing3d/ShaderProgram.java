package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertPath, String fragPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertPath));
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragPath));

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link shader gagal: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("File shader tidak ditemukan: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int compile(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Compile shader gagal: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() {
        glUseProgram(programId);
    }

    private int loc(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void setMat4(String name, Matrix4f m) {
        float[] buf = new float[16];
        m.get(buf);
        glUniformMatrix4fv(loc(name), false, buf);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(loc(name), v.x, v.y, v.z);
    }

    public void setInt(String name, int value) {
        glUniform1i(loc(name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(loc(name), value);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(loc(name), value ? 1 : 0);
    }
}
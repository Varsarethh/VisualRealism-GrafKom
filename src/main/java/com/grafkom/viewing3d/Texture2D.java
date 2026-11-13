package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D {
    private final int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap: tekstur diulang jika UV melebihi 1.0
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        ByteBuffer imageBuffer = readResource(resourcePath);
        stbi_set_flip_vertically_on_load(true);   // v=0 di bawah, sesuai konvensi OpenGL

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(imageBuffer, width, height, channels, 4);
            if (data == null) {
                throw new RuntimeException("Gagal memuat tekstur: " + resourcePath
                        + " (" + stbi_failure_reason() + ")");
            }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                         GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(data);
        } finally {
            MemoryUtil.memFree(imageBuffer);
        }

        setFiltering(true);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("File tekstur tidak ditemukan: " + path);
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // true  = Bilinear + Mipmap (GL_LINEAR_MIPMAP_LINEAR)
    // false = Nearest Neighbor (GL_NEAREST)
    public void setFiltering(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }
}
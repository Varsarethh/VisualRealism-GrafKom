package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VisualRealism {

    private long window;
    private int fbWidth = 900, fbHeight = 600;

    private Camera3D camera;
    private PerspectiveProjection perspective;
    private ShaderProgram shader, depthShader;
    private ShadowMapFBO shadowFbo;

    private float radius = 7f, azimuth = 30f, elevation = 25f;
    private boolean dragging = false;
    private double lastX, lastY;

    private Mesh sphereSmooth, sphereFlat, floor;
    private final Vector3f lightPos = new Vector3f(3f, 4f, 3f);
    private final Matrix4f floorModel = new Matrix4f().translate(0f, -1f, 0f);
    private final Matrix4f sphereModel = new Matrix4f().translate(0f, 0.5f, 0f);

    // status toggle
    private boolean useBlinn = true;     // B
    private boolean useSmooth = true;    // F
    private boolean depthOn = true;      // Z
    private boolean cullOn = true;       // C
    private boolean useTex = true;       // T
    private boolean mipmapOn = true;     // N
    private boolean useShadow = true;    // H
    private boolean vsync = true;        // V
    private float biasMax = 0.005f;      // [ ]  atau  , .

    private Texture2D texture;

    // penghitung FPS
    private int frames = 0, fps = 0;
    private double lastFpsTime = 0;

    public static void main(String[] args) {
        new VisualRealism().run();
    }

    public void run() {
        init();
        while (!glfwWindowShouldClose(window)) {
            render();
            glfwSwapBuffers(window);
            glfwPollEvents();
            countFps();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        window = glfwCreateWindow(fbWidth, fbHeight, "Visual Realism", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window (cek dukungan OpenGL 3.3)");
        }

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            fbWidth = w;
            fbHeight = h;
            glViewport(0, 0, w, h);
        });

        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (action == GLFW_RELEASE) return;
            boolean press = (action == GLFW_PRESS);
            switch (key) {
                case GLFW_KEY_B:
                    if (press) useBlinn = !useBlinn;
                    break;
                case GLFW_KEY_F:
                    if (press) useSmooth = !useSmooth;
                    break;
                case GLFW_KEY_Z:
                    if (press) {
                        depthOn = !depthOn;
                        HSRManager.setupDepthTest(depthOn);
                    }
                    break;
                case GLFW_KEY_C:
                    if (press) {
                        cullOn = !cullOn;
                        HSRManager.setupBackFaceCulling(cullOn);
                    }
                    break;
                case GLFW_KEY_T:
                    if (press) useTex = !useTex;
                    break;
                case GLFW_KEY_N:
                    if (press) {
                        mipmapOn = !mipmapOn;
                        texture.setFiltering(mipmapOn);
                    }
                    break;
                case GLFW_KEY_H:
                    if (press) useShadow = !useShadow;
                    break;
                case GLFW_KEY_V:
                    if (press) {
                        vsync = !vsync;
                        glfwSwapInterval(vsync ? 1 : 0);
                    }
                    break;
                case GLFW_KEY_LEFT_BRACKET:
                case GLFW_KEY_COMMA:
                    biasMax = Math.max(0f, Math.round((biasMax - 0.001f) * 10000f) / 10000f);
                    break;
                case GLFW_KEY_RIGHT_BRACKET:
                case GLFW_KEY_PERIOD:
                    biasMax = Math.min(0.1f, Math.round((biasMax + 0.001f) * 10000f) / 10000f);
                    break;
                case GLFW_KEY_ESCAPE:
                    glfwSetWindowShouldClose(window, true);
                    return;
                default:
                    return;
            }
            updateTitle();
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1], y = new double[1];
                    glfwGetCursorPos(window, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (!dragging) return;
            azimuth -= (float) (x - lastX) * 0.4f;
            elevation += (float) (y - lastY) * 0.4f;
            elevation = Math.max(-89f, Math.min(89f, elevation));
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, dx, dy) -> {
            radius -= (float) dy * 0.4f;
            radius = Math.max(2.5f, Math.min(20f, radius));
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        fbWidth = w[0];
        fbHeight = h[0];
        glViewport(0, 0, fbWidth, fbHeight);

        HSRManager.setupDepthTest(depthOn);
        HSRManager.setupBackFaceCulling(cullOn);
        System.out.println("OpenGL: " + glGetString(GL_VERSION));

        camera = new Camera3D(new Vector3f(0, 0, radius), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        perspective = new PerspectiveProjection(45f, (float) fbWidth / fbHeight, 0.1f, 100f);
        shader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/shadow_depth.vert", "/shaders/shadow_depth.frag");
        shadowFbo = new ShadowMapFBO();

        texture = new Texture2D("/textures/brick.jpg");   // sesuaikan dengan nama file tekstur kamu

        sphereSmooth = MeshFactory.createSphere(1f, 16, 24, true);
        sphereFlat = MeshFactory.createSphere(1f, 16, 24, false);
        floor = MeshFactory.createPlane(5f, 6f);

        lastFpsTime = glfwGetTime();
        updateTitle();
    }

    private void countFps() {
        frames++;
        double now = glfwGetTime();
        if (now - lastFpsTime >= 0.5) {
            fps = (int) Math.round(frames / (now - lastFpsTime));
            frames = 0;
            lastFpsTime = now;
            updateTitle();
        }
    }

    private void updateTitle() {
        glfwSetWindowTitle(window, String.format(
            "VR | %s | %s | Z:%s C:%s | Tex:%s %s | Shadow:%s Bias:%.3f | VSync:%s | FPS:%d",
            useBlinn ? "Blinn-Phong" : "Phong",
            useSmooth ? "Smooth" : "Flat",
            depthOn ? "ON" : "OFF",
            cullOn ? "ON" : "OFF",
            useTex ? "ON" : "OFF",
            mipmapOn ? "Mipmap" : "Nearest",
            useShadow ? "ON" : "OFF",
            biasMax,
            vsync ? "ON" : "OFF",
            fps));
    }

    private Vector3f computeEye() {
        float az = (float) Math.toRadians(azimuth);
        float el = (float) Math.toRadians(elevation);
        return new Vector3f(
            radius * (float) (Math.cos(el) * Math.sin(az)),
            radius * (float) Math.sin(el),
            radius * (float) (Math.cos(el) * Math.cos(az)));
    }

    // PASS 1: render kedalaman scene dari sudut pandang cahaya
    private void renderShadowPass(Matrix4f lightSpace) {
        shadowFbo.bindForWriting();
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(false);

        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);

        depthShader.setMat4("model", floorModel);
        floor.draw();
        depthShader.setMat4("model", sphereModel);
        (useSmooth ? sphereSmooth : sphereFlat).draw();

        shadowFbo.unbind(fbWidth, fbHeight);
        HSRManager.setupDepthTest(depthOn);
        HSRManager.setupBackFaceCulling(cullOn);
    }

    private void render() {
        if (fbHeight == 0) return;

        perspective.setAspectRatio((float) fbWidth / fbHeight);
        camera.updateOrbitalPosition(radius, azimuth, elevation);

        // Matriks ruang cahaya: ortografik (directional light) x lookAt dari posisi cahaya
        Matrix4f lightSpace = new Matrix4f()
            .ortho(-8f, 8f, -8f, 8f, 1f, 20f)
            .lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));

        if (useShadow) {
            renderShadowPass(lightSpace);
        }

        // PASS 2: render normal dari sudut pandang kamera
        glClearColor(0.1f, 0.1f, 0.12f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.use();
        shader.setMat4("view", camera.calculateLookAtMatrix());
        shader.setMat4("projection", perspective.calculateProjectionMatrix());
        shader.setMat4("lightSpaceMatrix", lightSpace);
        shader.setVec3("lightPos", lightPos);
        shader.setVec3("viewPos", computeEye());
        shader.setVec3("lightColor", new Vector3f(1f, 1f, 1f));
        shader.setBool("useBlinnPhong", useBlinn);
        shader.setBool("useTexture", useTex);
        shader.setBool("useShadow", useShadow);
        shader.setFloat("biasMax", biasMax);
        shader.setInt("textureSampler", 0);
        shader.setInt("shadowMap", 1);

        texture.bind(0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());
        glActiveTexture(GL_TEXTURE0);

        // lantai
        shader.setMat4("model", floorModel);
        shader.setVec3("objectColor", new Vector3f(0.6f, 0.6f, 0.65f));
        floor.draw();

        // bola
        shader.setMat4("model", sphereModel);
        shader.setVec3("objectColor", new Vector3f(0.85f, 0.25f, 0.25f));
        (useSmooth ? sphereSmooth : sphereFlat).draw();
    }
}
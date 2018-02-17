package cz.cvut.fit.blazeva.app.view;

import cz.cvut.fit.blazeva.app.control.Control;
import cz.cvut.fit.blazeva.app.model.Model;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;

import java.io.IOException;
import java.nio.IntBuffer;

import static cz.cvut.fit.blazeva.app.model.Model.*;
import static cz.cvut.fit.blazeva.app.model.Model.window;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

public class Window {

    private Boolean fullscreen = false;
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cpCallback;
    private GLFWMouseButtonCallback mbCallback;
    private GLFWFramebufferSizeCallback fbCallback;
    private GLFWWindowSizeCallback wsCallback;
    private Callback debugProc;
    private Control control;


    private void init() throws IOException {
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        setDisplayMode(DisplayMode.WINDOWED_FULLSCREEN);

        control.initialize();
    }

    public void registerCallbacks() {

        glfwSetFramebufferSizeCallback(window, fbCallback = GLFWFramebufferSizeCallback.create((window, newWidth, newHeight) -> {
                    if (newWidth > 0 && newHeight > 0 && (fbWidth != newWidth || fbHeight != newHeight)) {
                        fbWidth = newWidth;
                        fbHeight = newHeight;
                    }
                }
        ));
        glfwSetWindowSizeCallback(window, wsCallback = GLFWWindowSizeCallback.create((window, newWidth, newHeight) -> {
            if (newWidth > 0 && newHeight > 0 && (width != newWidth || height != newHeight)) {
                width = newWidth;
                height = newHeight;
            }
        }));

        glfwSetKeyCallback(window, keyCallback = GLFWKeyCallback.create((window, keyCode, scancode, action, mods) -> {
            if (keyCode == GLFW_KEY_UNKNOWN) return;
            if (keyCode == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            if (keyCode == GLFW_KEY_F3 && action == GLFW_RELEASE) {
                control.stopTheWorld(() -> {
                    setDisplayMode(DisplayMode.WINDOWED_FULLSCREEN);
                    return Model.window;
                });
//                fullscreen = !fullscreen;
//                setDisplayMode(fullscreen ? DisplayMode.WINDOWED_FULLSCREEN : DisplayMode.WINDOWED);
            }
            if (action == GLFW_PRESS) {
                control.key.tap(keyCode);
            }
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                control.key.down(keyCode);
            } else {
                control.key.up(keyCode);
            }
        }));
        glfwSetCursorPosCallback(window, cpCallback = GLFWCursorPosCallback.create((window, xpos, ypos) -> {
                    float normX = (float) ((xpos - width / 2.0) / width * 2.0);
                    float normY = (float) ((ypos - height / 2.0) / height * 2.0);
                    Model.mouseX = Math.max(-width / 2.0f, Math.min(width / 2.0f, normX));
                    Model.mouseY = Math.max(-height / 2.0f, Math.min(height / 2.0f, normY));
                }
        ));
        glfwSetMouseButtonCallback(window, mbCallback = GLFWMouseButtonCallback.create((window, button, action, mods) -> {
            if (action == GLFW_PRESS) {
                control.mouse.down(button);
                control.mouse.click(button);
            } else {
                control.mouse.up(button);
            }
        }));
    }

    public void setDisplayMode(DisplayMode mode) {
        if (window != null) {
            destroyAll();
            window = null;
            keyCallback = null;
            cpCallback = null;
            mbCallback = null;
            fbCallback = null;
            wsCallback = null;
            debugProc = null;
        }
        glfwDefaultWindowHints();
//        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
//        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = glfwGetVideoMode(monitor);
        width = videoMode.width();
        height = videoMode.height();
        fbWidth = width;
        fbHeight = height;
        long monitorSetting;
        switch (mode) {
            case FULLSCREEN:
                monitorSetting = monitor;
                break;
            case WINDOWED_FULLSCREEN:
//                glfwWindowHint(GLFW_RED_BITS, videoMode.redBits());
//                glfwWindowHint(GLFW_GREEN_BITS, videoMode.greenBits());
//                glfwWindowHint(GLFW_BLUE_BITS, videoMode.blueBits());
//                glfwWindowHint(GLFW_REFRESH_RATE, videoMode.refreshRate());
                monitorSetting = 0L;
                break;
            case WINDOWED:
            default:
                monitorSetting = 0L;
        }
        window = glfwCreateWindow(width, height, "Small Portal", monitorSetting, NULL);
        if (window == NULL) throw new AssertionError("Failed to create the GLFW window");

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        fbWidth = framebufferSize.get(0);
        fbHeight = framebufferSize.get(1);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);

        registerCallbacks();

//        glfwSetCursor(window, glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR));
//        debugProc = GLUtil.setupDebugMessageCallback();

        Model.caps = GL.createCapabilities();
        if (!Model.caps.OpenGL20) throw new AssertionError("This demo requires OpenGL 2.0.");

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    }

    private void destroyAll() {
        if (debugProc != null) {
            debugProc.free();
        }
        keyCallback.free();
        cpCallback.free();
        mbCallback.free();
        fbCallback.free();
        wsCallback.free();
        glfwDestroyWindow(window);
    }

    public void run() {
        control = new Control();
        try {
            init();
            control.loop(window);
            destroyAll();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            glfwTerminate();
        }
    }

    public enum DisplayMode {
        WINDOWED,
        FULLSCREEN,
        WINDOWED_FULLSCREEN
    }

    @FunctionalInterface
    public interface WindowCallback {
        Long call();
    }

}

package cz.cvut.fit.blazeva.app.control;

import cz.cvut.fit.blazeva.app.model.Model;
import cz.cvut.fit.blazeva.app.view.Drawer;
import cz.cvut.fit.blazeva.app.view.Program;
import cz.cvut.fit.blazeva.app.view.Window;

import java.io.IOException;

import static cz.cvut.fit.blazeva.app.model.Model.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class Control {


    private long lastTime = System.nanoTime();
    public Keyboard key = new Keyboard();
    public Mouse mouse = new Mouse();

    private Program program = new Program();
    private Drawer drawer = new Drawer();
    private Boolean stopTheWorld = false;
    private Window.WindowCallback stopCallback = null;
    private final Object stopSynch = new Object();
    private Long newWindow = null;
    private Long window;

    private boolean paused = false;

    private void update() {
        updateControls();

        long thisTime = System.nanoTime();
        float dt = (thisTime - lastTime) / 1E9f;
        lastTime = thisTime;

        drawer.update(dt, program);
        if (!paused) {
            Model.scenario.update();
        }
    }

    private void updateControls() {
        if (key.wasTapped(GLFW_KEY_SPACE)) {
            paused = !paused;
        }
        if (mouse.isDown(GLFW_MOUSE_BUTTON_LEFT)) {
            int bx = (int) ((Model.mouseX + 0.685) * 20 / 0.685);
            int by = (int) ((Model.mouseY + 0.92) * 20 / 0.92);
            System.out.println(bx + " " + by);
            Model.scenario.toggle(bx, by);
        }
    }

    public void initialize() throws IOException {
        drawer.initialize();
    }

    private void render() {
        drawer.draw();
    }

    public void loop(Long window) {
        this.window = window;
        try {
            while (!glfwWindowShouldClose(this.window)) {
                System.out.println("window: " + this.window);
                glfwPollEvents();
                glViewport(0, 0, fbWidth, fbHeight);
                Thread.sleep(160);
                update();
                render();
                glfwSwapBuffers(this.window);
                if (stopTheWorld) {
                    synchronized (stopSynch) {
                        this.window = stopCallback.call();
                        stopCallback = null;
                        stopTheWorld = false;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopTheWorld(Window.WindowCallback callback) {
        synchronized (stopSynch) {
            stopCallback = callback;
            stopTheWorld = true;
        }
    }

}

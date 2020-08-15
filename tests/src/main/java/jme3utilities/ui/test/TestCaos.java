/*
 Copyright (c) 2018-2020, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.ui.test;

import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.Validate;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.HelpUtils;
import jme3utilities.ui.InputMode;

/**
 * An ActionApplication to test/demonstrate the CameraOrbitAppState and the
 * help-node toggle.
 */
public class TestCaos extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestCaos.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestCaos.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * Node for displaying hotkey help in the GUI scene
     */
    private Node helpNode;
    /**
     * Node for displaying "toggle help: H" in the GUI scene
     */
    private Node minHelpNode;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestCaos application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        /*
         * Instantiate the application.
         */
        TestCaos application = new TestCaos();
        /*
         * Customize the window's title bar.
         */
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setTitle(applicationName);

        settings.setAudioRenderer(null);
        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);
        /*
         * Invoke the JME startup code,
         * which in turn invokes actionInitializeApplication().
         */
        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        flyCam.setDragToRotate(true); // TODO only works for dragToRotate=true?
        flyCam.setMoveSpeed(5f);
        cam.setLocation(new Vector3f(-4f, 4f, 9f));
        cam.setRotation(new Quaternion(0.038f, 0.96148f, -0.1897f, 0.1951f));

        ColorRGBA skyColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        viewPort.setBackgroundColor(skyColor);

        addLighting();
        addBox();

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Attach a help node to the GUI node.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("signal " + CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bind("signal " + CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bind("signal orbitLeft", KeyInput.KEY_LEFT);
        dim.bind("signal orbitRight", KeyInput.KEY_RIGHT);
        dim.bind("toggle help", KeyInput.KEY_H);

        float x = 10f;
        float y = cam.getHeight() - 10f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle bounds = new Rectangle(x, y, width, height);

        attachHelpNode(bounds);
    }

    /**
     * Process an action that wasn't handled by the active InputMode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "toggle help":
                    toggleHelp();
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a mysterious green box at the origin.
     */
    private void addBox() {
        float halfExtent = 1f; // mesh units
        Mesh boxMesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry box = new Geometry("box", boxMesh);
        rootNode.attachChild(box);

        ColorRGBA color = new ColorRGBA(0f, 0.3f, 0f, 1f);
        Material material = MyAsset.createShadedMaterial(assetManager, color);
        box.setMaterial(material);
    }

    /**
     * Add lighting to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -3f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);
    }

    /**
     * Generate full and minimal versions of the hotkey help. Attach the minimal
     * one to the GUI scene.
     *
     * @param bounds the desired screen coordinates (not null, unaffected)
     */
    private void attachHelpNode(Rectangle bounds) {
        Validate.nonNull(bounds, "bounds");

        InputMode inputMode = getDefaultInputMode();
        float extraSpace = 20f;
        helpNode = HelpUtils.buildNode(inputMode, bounds, guiFont, extraSpace);
        helpNode.move(0f, 0f, 1f); // move (slightly) to the front

        InputMode dummyMode = new InputMode("dummy") {
            @Override
            protected void defaultBindings() {
            }

            @Override
            public void onAction(String s, boolean b, float f) {
            }
        };
        dummyMode.bind("toggle help", KeyInput.KEY_H);

        float width = 100f; // in pixels
        float height = bounds.height;
        float x = bounds.x + bounds.width - width;
        float y = bounds.y;
        Rectangle dummyBounds = new Rectangle(x, y, width, height);

        minHelpNode = HelpUtils.buildNode(dummyMode, dummyBounds, guiFont, 0f);
        guiNode.attachChild(minHelpNode);
    }

    /**
     * Toggle between the full help node and the minimal one.
     */
    private void toggleHelp() {
        if (helpNode.getParent() == null) {
            minHelpNode.removeFromParent();
            guiNode.attachChild(helpNode);
        } else {
            helpNode.removeFromParent();
            guiNode.attachChild(minHelpNode);
        }
    }
}

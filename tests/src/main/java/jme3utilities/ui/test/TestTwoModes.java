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

import com.jme3.app.StatsAppState;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
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
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.HelpUtils;
import jme3utilities.ui.InputMode;

/**
 * An ActionApplication to test/demonstrate multiple input modes.
 */
public class TestTwoModes extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestTwoModes.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestTwoModes.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private BitmapText statusText;
    /**
     * help for the default input mode
     */
    private Node defaultHelp;
    // *************************************************************************
    // new methods exposed

    /**
     * Build the help node for the specified mode.
     *
     * @param mode the input mode (not null)
     * @return a new orphan Node, suitable for attachment to the GUI node
     */
    Node buildHelpNode(InputMode mode) {
        float x = 10f;
        float y = cam.getHeight() - 30f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle bounds = new Rectangle(x, y, width, height);

        float space = 20f;
        Node result = HelpUtils.buildNode(mode, bounds, guiFont, space);

        return result;
    }

    /**
     * Main entry point for the TestTwoModes application.
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
        TestTwoModes application = new TestTwoModes();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
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

    /**
     * Switch from edit mode to default mode.
     */
    void switchToDefault() {
        InputMode dim = InputMode.findMode("default");
        InputMode editMode = InputMode.findMode("edit");

        editMode.setEnabled(false);
        dim.setEnabled(true);

        guiNode.attachChild(defaultHelp);
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
        /*
         * Attach a status text to the GUI.
         */
        statusText = new BitmapText(guiFont, false);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);
        /*
         * Attach a (disabled) edit mode to the state manager.
         */
        InputMode editMode = new EditMode();
        stateManager.attach(editMode);
        /*
         * Hide the render-statistics overlay.
         */
        stateManager.getState(StatsAppState.class).toggleStats();
    }

    /**
     * Callback invoked immediately after initializing the hotkey bindings of
     * the default input mode.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();
        dim.bind("edit text", KeyInput.KEY_RETURN, KeyInput.KEY_TAB);
        /*
         * Build and attach the help node for default mode.
         */
        defaultHelp = buildHelpNode(dim);
        guiNode.attachChild(defaultHelp);
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing && actionString.equals("edit text")) {
            /*
             * Switch from default mode to edit mode.
             */
            InputMode dim = InputMode.findMode("default");
            InputMode editMode = InputMode.findMode("edit");

            dim.setEnabled(false);
            editMode.setEnabled(true);

            defaultHelp.removeFromParent();
            return;
        }

        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        /**
         * Update the displayed status.
         */
        EditMode editMode = (EditMode) InputMode.findMode("edit");
        String text = editMode.getText();

        if (editMode.isEnabled()) { // fake a blinking text cursor
            long now = System.nanoTime() % 1_000_000_000L;
            if (now > 500_000_000L) {
                text = text + "_";
            }
        }
        statusText.setText("Text: " + text);
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
}

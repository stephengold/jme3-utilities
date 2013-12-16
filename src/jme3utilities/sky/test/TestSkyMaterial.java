/*
 Copyright (c) 2013, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.sky.test;

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.sky.DomeMesh;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;
import jme3utilities.ui.GuiApplication;

/**
 * A GUI application for testing the SkyMaterial class using a heads-up display
 * (HUD). The application's main entry point is here.
 *
 * Use the 'H' key to toggle HUD visibility, the 'S' key to save the scene, the
 * 'L' key to load a saved scene.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyMaterial
        extends GuiApplication
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * aspect ratio of the viewport (width/height)
     */
    final private static float viewportAspectRatio = 1f;
    /**
     * number of samples in each longitudinal quadrant of the dome, including
     * the top and the rim (>=2)
     */
    final private static int quadrantSamples = 16;
    /**
     * number of samples around the rim of the dome (>=3)
     */
    final private static int rimSamples = 60;
    /**
     * width and height of the viewport (in pixels)
     */
    final private static int viewportSize = 800;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestSkyMaterial.class.getName());
    /**
     * name for the dome geometry
     */
    final private static String geometryName = "sky dome";
    /**
     * asset path for loading and saving
     */
    final private static String savePath = "Models/TestSkyMaterial.j3o";
    /**
     * application name for its window's title bar
     */
    final private static String windowTitle = "TestSkyMaterial";
    // *************************************************************************
    // fields
    /**
     * heads-up display (HUD)
     */
    private TestSkyMaterialHud hud = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the test harness.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Misc.setLoggingLevels(Level.WARNING);
        TestSkyMaterial application = new TestSkyMaterial();
        /*
         * Don't pause on lost focus.  This simplifies debugging and
         * permits the application to keep running while minimized.
         */
        application.setPauseOnLostFocus(false);
        /*
         * Initialize viewport settings.
         */
        application.setShowSettings(false);
        /*
         * This test wants a square viewport.
         */
        AppSettings settings = new AppSettings(true);
        settings.setResolution(viewportSize, viewportSize);
        /*
         * Customize the window's title bar.
         */
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to TestSkyMaterial.guiInitApp()!
         */
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));
        switch (actionString) {
            case "load":
                load();
                break;

            case "save":
                save();
                break;

            default:
                logger.log(Level.WARNING, "Action {0} was not handled.",
                        MyString.quote(actionString));
                break;
        }
    }
    // *************************************************************************
    // Application methods

    /**
     * Handle the Esc hotkey: if there's an active popup menu, close it.
     * Otherwise, stop the application.
     */
    @Override
    public void stop() {
        if (hud.hasActivePopup()) {
            hud.closeActivePopup();
            return;
        }

        logger.log(Level.INFO, "Stopping the application.");
        super.stop(false);
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        configureCamera();
        /*
         * Create and attach a dome mesh geometry for the sky.
         */
        DomeMesh mesh = new DomeMesh(rimSamples, quadrantSamples);
        Geometry geometry = new Geometry(geometryName, mesh);
        rootNode.attachChild(geometry);
        geometry.setQueueBucket(Bucket.Sky);
        /*
         * Create and initialize a material for the sky.
         */
        SkyMaterial material = new SkyMaterial(assetManager);
        geometry.setMaterial(material);
        material.initialize();
        material.addClouds(0);
        material.addClouds(1);
        material.addHaze();
        material.addObject(TestSkyMaterialHud.moonIndex,
                LunarPhase.FULL.imagePath());
        material.addObject(TestSkyMaterialHud.sunIndex, SkyMaterial.sunMapPath);
        material.addStars();

        initializeUserInterface(material);
    }
    // *************************************************************************
    // private methods

    /**
     * Configure the camera, including flyCam.
     */
    private void configureCamera() {
        /*
         * Fish-eye view, from the origin, looking up at the zenith.
         */
        float fovDegrees = 90f;
        setFrustum(fovDegrees);

        cam.setLocation(Vector3f.ZERO.clone());

        Quaternion zenith = new Quaternion();
        zenith.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_X);
        cam.setRotation(zenith);
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
    }

    /**
     * Initialize the user interface.
     */
    private void initializeUserInterface(SkyMaterial material) {
        /*
         * Capture a screenshot when the KEY_SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Create the heads-up display (HUD).
         */
        hud = new TestSkyMaterialHud(niftyDisplay, false);
        hud.setMaterial(material);
        success = stateManager.attach(hud);
        assert success;
        /*
         * Map the 'H' key to toggle HUD visibility.
         */
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(hud, "toggle");
        /*
         * Map the 'L' key to load material from a file.
         */
        inputManager.addMapping("load", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addListener(this, "load");
        /*
         * Map the 'S' key to save material to a file.
         */
        inputManager.addMapping("save", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "save");
    }

    /**
     * Load a saved sky dome from a file.
     */
    private void load() {
        Geometry loadedDome = (Geometry) assetManager.loadModel(savePath);
        SkyMaterial material = (SkyMaterial) loadedDome.getMaterial();
        assert material != null;

        logger.log(Level.INFO, "Loaded {0} from asset {1}",
                new Object[]{
            MyString.quote(loadedDome.getName()),
            MyString.quote(savePath)
        });

        Spatial oldDome = rootNode.getChild(geometryName);
        rootNode.detachChild(oldDome);

        rootNode.attachChild(loadedDome);
        hud.setMaterial(material);
        hud.setEnabled(false);
    }

    /**
     * Save the sky dome to a file.
     */
    private void save() {
        String filePath = "assets/" + savePath;
        File file = new File(filePath);

        BinaryExporter exporter = BinaryExporter.getInstance();
        Spatial dome = rootNode.getChild(geometryName);
        try {
            exporter.save(dome, file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving dome to file {0}",
                    MyString.quote(filePath));
        }
        logger.log(Level.INFO, "Saved {0} to file {1}",
                new Object[]{
            MyString.quote(dome.getName()),
            MyString.quote(filePath)
        });
    }

    /**
     * (Re-)initialize the render camera's frustum.
     *
     * @param fovDegrees the desired field-of-view angle in degrees
     */
    private void setFrustum(float fovDegrees) {
        Camera camera = getCamera();
        float nearPlaneDistance = 0.1f;
        float farPlaneDistance = 10000f;
        camera.setFrustumPerspective(fovDegrees, viewportAspectRatio,
                nearPlaneDistance, farPlaneDistance);
    }
}
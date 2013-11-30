// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities.sky.test;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.sky.DomeMesh;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;
import org.lwjgl.Sys;

/**
 * A simple application for testing the SkyMaterial class using a Nifty heads-up
 * display (HUD).
 *
 * Use the 'H' key to toggle HUD visibility.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyMaterial
        extends SimpleApplication {
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
     * Nifty screen id of the HUD
     */
    final private static String screenId = "test-sky-material";
    /**
     * application name for its window's title bar
     */
    final private static String windowTitle = "TestSkyMaterial";
    // *************************************************************************
    // fields
    /**
     * instance under test
     */
    private SkyMaterial material = null;
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
        /*
         * Set logging levels.
         */
        Misc.setLoggingLevels(Level.WARNING);
        logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
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
         * ... and onward to TestSkyMaterial.simpleInitApp()!
         */
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
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        logger.log(Level.INFO, "LWJGL version is {0}",
                MyString.quote(Sys.getVersion()));

        configureCamera();
        /*
         * Create a dome mesh geometry for the sky.
         */
        DomeMesh mesh = new DomeMesh(rimSamples, quadrantSamples);
        Geometry geometry = new Geometry("sky dome", mesh);
        rootNode.attachChild(geometry);
        geometry.setQueueBucket(Bucket.Translucent);
        /*
         * Create a material for the sky.
         */
        material = new SkyMaterial(assetManager);
        geometry.setMaterial(material);
        material.initialize();
        material.addClouds(0);
        material.addClouds(1);
        material.addHaze();
        material.addObject(TestSkyMaterialHud.moonIndex,
                LunarPhase.FULL.imagePath());
        material.addObject(TestSkyMaterialHud.sunIndex, SkyMaterial.sunMapPath);
        material.addStars();

        initializeUserInterface();
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
    private void initializeUserInterface() {
        /*
         * Capture a screenshot when the KEY_SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Initialize Nifty and log the Nifty version string.
         */
        NiftyJmeDisplay display = new NiftyJmeDisplay(assetManager,
                inputManager, audioRenderer, guiViewPort);
        Nifty nifty = display.getNifty();
        //nifty.setDebugOptionPanelColors(true);
        String niftyVersion = nifty.getVersion();
        logger.log(Level.INFO, "Nifty version is {0}",
                MyString.quote(niftyVersion));
        /*
         * Load the Nifty XML for generic popup menus.
         */
        nifty.addXml("Interface/Nifty/popup-menu.xml");
        /*
         * Create the heads-up display (HUD).
         */
        hud = new TestSkyMaterialHud(display, screenId, material, false);
        success = stateManager.attach(hud);
        assert success;
        /*
         * Map the 'H' key to toggle HUD visibility.
         */
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(hud, "toggle");
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
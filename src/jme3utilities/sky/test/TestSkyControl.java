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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.LandscapeControl;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyControl;
import org.lwjgl.Sys;

/**
 * A simple application for testing/demonstrating the SkyControl class using a
 * Nifty heads-up display (HUD).
 *
 * Use the 'H' key to toggle HUD visibility.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyControl
        extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * flattening of the cloud dome: 0=none, 1=maximum
     */
    final private static float cloudFlattening = 0.9f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestSkyControl.class.getName());
    /**
     * Nifty screen id of the HUD
     */
    final private static String screenId = "test-sky-control";
    /**
     * application name for its window's title bar
     */
    final private static String windowTitle = "TestSkyControl";
    // *************************************************************************
    // fields
    /**
     * the ambient light in the scene
     */
    private AmbientLight ambientLight = null;
    /**
     * the main light in the scene, which represents the sun or moon
     */
    private DirectionalLight mainLight = null;
    /**
     * shadow renderer for the main light
     */
    private DirectionalLightShadowRenderer dlsr = null;
    /**
     * landscape control
     */
    private LandscapeControl landscapeControl = null;
    /**
     * the control under test
     */
    private SkyControl control = null;
    /**
     * the heads-up display (HUD)
     */
    private TestSkyControlHud hud = null;
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
        TestSkyControl application = new TestSkyControl();
        /*
         * Don't pause on lost focus.  This simplifies debugging and
         * permits the application to keep running while minimized.
         */
        application.setPauseOnLostFocus(false);
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to TestSkyControl.simpleInitApp()!
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
         * light sources
         */
        mainLight = new DirectionalLight();
        mainLight.setName("main");
        rootNode.addLight(mainLight);

        ambientLight = new AmbientLight();
        ambientLight.setName("ambient");
        rootNode.addLight(ambientLight);
        /*
         * shadow renderer
         */
        dlsr = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsr.setLight(mainLight);
        viewPort.addProcessor(dlsr);
        /*
         * Create, add, and enable the landscape.
         */
        landscapeControl = new LandscapeControl(assetManager);
        rootNode.addControl(landscapeControl);
        landscapeControl.setEnabled(true);
        /*
         * Create a SkyControl to animate the sky.
         */
        boolean starMotion = true; // allow stars to move
        boolean bottomDome = true; // helpful in case scene has a low horizon
        control = new SkyControl(assetManager, cam, cloudFlattening, starMotion,
                bottomDome);
        /*
         * Put SkyControl in charge of the lights, the
         * shadow renderer, and the viewport background. (optional)
         */
        control.addViewPort(viewPort);
        control.setAmbientLight(ambientLight);
        control.setMainLight(mainLight);
        control.setShadowRenderer(dlsr);
        /*
         * Add SkyControl to the scene and enable it.
         */
        rootNode.addControl(control);
        control.setEnabled(true);

        initializeUserInterface();
    }

    /**
     * Update the scene.
     *
     * @param tpf real seconds elapsed since the previous update (>=0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        LunarPhase lunarPhase = hud.getLunarPhase();
        control.setPhase(lunarPhase);
        /*
         * Adjust SkyControl parameters based on sliders in the HUD.
         */
        float cloudiness = hud.getCloudiness();
        control.setCloudiness(cloudiness);

        boolean cloudModulation = hud.getCloudModulation();
        control.setCloudModulation(cloudModulation);

        float cloudRate = hud.getCloudRate();
        control.setCloudRate(cloudRate);

        float hour = hud.getHour();
        control.getSunAndStars().setHour(hour);

        float observerLatitude = hud.getLatitude();
        control.getSunAndStars().setObserverLatitude(observerLatitude);

        float lunarDiameter = hud.getLunarDiameter();
        control.setLunarDiameter(lunarDiameter);

        float solarLongitude = hud.getSolarLongitude();
        control.getSunAndStars().setSolarLongitude(solarLongitude);
        /*
         * Adjust the scale of the terrain.
         */
        float radius = 0.5f * cam.getFrustumFar();
        float baseY = 0f;
        float topY = hud.getRelief();
        landscapeControl.setTerrainScale(radius, baseY, topY);
    }
    // *************************************************************************
    // private methods

    /**
     * Configure the camera, including flyCam.
     */
    private void configureCamera() {
        /*
         * The camera is initially pointed due west.
         */
        cam.setLocation(new Vector3f(6.5f, 13f, 50f));
        Quaternion orientation = new Quaternion();
        orientation.lookAt(new Vector3f(0.17f, 0f, -0.985f), Vector3f.UNIT_Y);
        cam.setRotation(orientation);

        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(2f);
        flyCam.setMoveSpeed(20f);
        flyCam.setZoomSpeed(20f);
        flyCam.setUpVector(Vector3f.UNIT_Y);
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
         * Disable rendering of JME statistics.
         * Statistics can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
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
         * Create and attach the heads-up display (HUD).
         */
        hud = new TestSkyControlHud(display, screenId, true);
        success = stateManager.attach(hud);
        assert success;
        /*
         * Map the 'H' hotkey to toggle HUD visibility.
         */
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(hud, "toggle");
    }
}
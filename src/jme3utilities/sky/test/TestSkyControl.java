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

import com.beust.jcommander.JCommander;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.LandscapeControl;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyControl;
import jme3utilities.ui.GuiApplication;
import org.lwjgl.Sys;

/**
 * A GUI application for testing/demonstrating the SkyControl class using a
 * heads-up display (HUD). The application's main entry point is here.
 *
 * Use the 'H' key to toggle HUD visibility.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyControl
        extends GuiApplication {
    // *************************************************************************
    // constants

    /**
     * width and height of rendered shadow maps (pixels per side, >0)
     */
    final private static int shadowMapSize = 4_096;
    /**
     * number of shadow map splits (>0)
     */
    final private static int shadowMapSplits = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestSkyControl.class.getName());
    /**
     * application name for its window's title bar and its usage message
     */
    final private static String applicationName = "TestSkyControl";
    /**
     * Nifty screen id of the HUD
     */
    final private static String screenId = "test-sky-control";
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
     * shadow filter for the main light (null means none)
     */
    private DirectionalLightShadowFilter dlsf = null;
    /**
     * shadow renderer for the main light (null means none)
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
    /**
     * command-line parameters
     */
    final private static TestSkyControlParameters parameters =
            new TestSkyControlParameters();
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
         * Parse the command-line arguments into parameters.
         */
        JCommander jCommander = new JCommander(parameters, arguments);
        jCommander.setProgramName(applicationName);
        if (parameters.usageOnly()) {
            jCommander.usage();
            return;
        }
        /*
         * Don't pause on lost focus.  This simplifies debugging and
         * permits the application to keep running while minimized.
         */
        application.setPauseOnLostFocus(false);
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);
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
         * Add light sources.
         */
        mainLight = new DirectionalLight();
        mainLight.setName("main");
        rootNode.addLight(mainLight);

        ambientLight = new AmbientLight();
        ambientLight.setName("ambient");
        rootNode.addLight(ambientLight);
        /*
         * Add shadows, using a filter or renderer.
         */
        if (parameters.shadowFilter()) {
            dlsf = new DirectionalLightShadowFilter(
                    assetManager, shadowMapSize, shadowMapSplits);
            dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsf.setLight(mainLight);
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            fpp.addFilter(dlsf);
            viewPort.addProcessor(fpp);

        } else {
            dlsr = new DirectionalLightShadowRenderer(
                    assetManager, shadowMapSize, shadowMapSplits);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsr.setLight(mainLight);
            viewPort.addProcessor(dlsr);
        }
        /*
         * Create, add, and enable the landscape.
         */
        landscapeControl = new LandscapeControl(assetManager);
        rootNode.addControl(landscapeControl);
        landscapeControl.setEnabled(true);
        /*
         * Create a SkyControl to animate the sky.
         */
        float cloudFlattening;
        boolean starMotion;
        boolean bottomDome;
        if (parameters.singleDome()) {
            cloudFlattening = 0f; // single dome implies clouds on hemisphere
            starMotion = false; // single dome implies non-moving stars
            bottomDome = false; // single dome implies exposed background
        } else {
            cloudFlattening = 0.9f; // overhead clouds 10x closer than horizon
            starMotion = true; // allow stars to move
            bottomDome = true; // helpful in case scene has a low horizon
        }
        control = new SkyControl(assetManager, cam, cloudFlattening, starMotion,
                bottomDome);
        /*
         * Put SkyControl in charge of the lights, shadows, and background.
         * (all optional)
         */
        control.addViewPort(viewPort);
        control.setAmbientLight(ambientLight);
        control.setMainLight(mainLight);
        if (dlsf != null) {
            control.setShadowFilter(dlsf);
        }
        if (dlsr != null) {
            control.setShadowRenderer(dlsr);
        }
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
     * @param unused
     */
    @Override
    public void simpleUpdate(float unused) {
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
         * Point the camera 10 degrees north of west.
         */
        cam.setLocation(new Vector3f(6.5f, 13f, 50f));
        Quaternion orientation = new Quaternion();
        orientation.lookAt(new Vector3f(0.17f, 0f, -0.985f), Vector3f.UNIT_Y);
        cam.setRotation(orientation);

        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(2f);
        flyCam.setMoveSpeed(20f);
        flyCam.setUpVector(Vector3f.UNIT_Y);
        flyCam.setZoomSpeed(20f);
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
         * Initialize Nifty for the graphical user interface (GUI).
         */
        startGui();
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
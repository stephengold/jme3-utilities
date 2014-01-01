/*
 Copyright (c) 2013-2014, Stephen Gold
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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.LandscapeControl;
import jme3utilities.Misc;
import jme3utilities.MyVector3f;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyControl;
import jme3utilities.ViewPortListener;
import jme3utilities.WaterProcessor;
import jme3utilities.sky.Updater;
import jme3utilities.ui.GuiApplication;

/**
 * A GUI application for testing/demonstrating the SkyControl class using a
 * heads-up display (HUD). The application's main entry point is here.
 *
 * Use the 'H' key to toggle HUD visibility.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyControl
        extends GuiApplication
        implements ViewPortListener {
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
     * action string to toggle HUD visibility
     */
    final private static String actionStringToggle = "toggle hud";
    /**
     * application name for its window's title bar and its usage message
     */
    final private static String applicationName = "TestSkyControl";
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
        Misc.setLoggingLevels(Level.WARNING);
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
         * ... and onward to TestSkyControl.guiInitializeApplication()!
         */
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        configureCamera();
        /**
         * A node to parent geometries which can appear reflected in the water.
         */
        Node sceneNode = new Node("scene node");
        rootNode.attachChild(sceneNode);
        /*
         * Add light sources and shadows.
         */
        mainLight = new DirectionalLight();
        mainLight.setName("main");
        sceneNode.addLight(mainLight);

        ambientLight = new AmbientLight();
        ambientLight.setName("ambient");
        sceneNode.addLight(ambientLight);
        /*
         * Create, add, and enable the landscape.
         */
        landscapeControl = new LandscapeControl(assetManager);
        sceneNode.addControl(landscapeControl);
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
         * Put SkyControl in charge of updating the lights and background.
         * (all optional)
         */
        Updater updater = control.getUpdater();
        updater.addViewPort(viewPort);
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        /*
         * Add SkyControl to the scene and enable it.
         */
        sceneNode.addControl(control);
        control.setEnabled(true);
        /*
         * Add shadows to the main view port, with shadow intensities
         * updated by SkyControl.
         */
        addShadows(viewPort);

        if (parameters.water()) {
            /*
             * Create a horizontal square of water and add it to the scene.
             *
             * During initialization of the water processor (on the first
             * update), the processor will discover the SkyControl and put the
             * SkyControl in charge of the processor's background colors.
             */
            WaterProcessor wp = new WaterProcessor(assetManager);
            viewPort.addProcessor(wp);
            wp.addListener(updater);
            wp.addListener(this);
            //wp.setDebug(true);
            wp.setDistortionMix(1f);
            wp.setDistortionScale(0.1f);
            wp.setReflectionClippingOffset(0f);
            wp.setReflectionScene(sceneNode);
            wp.setRefractionClippingOffset(0f);
            wp.setWaterTransparency(0f);
            wp.setWaveSpeed(0.02f);

            float diameter = 400f; // world units
            Geometry water = wp.createWaterGeometry(diameter, diameter);
            rootNode.attachChild(water);

            float depth = 0.3f; // world units
            Plane waterPlane = new Plane(Vector3f.UNIT_Y, depth);
            wp.setPlane(waterPlane);
            wp.setWaterDepth(depth);

            float xzOffset = diameter / 2f;
            water.setLocalTranslation(-xzOffset, depth, xzOffset);
            Vector2f textureScale = new Vector2f(10f, 10f);
            water.getMesh().scaleTextureCoordinates(textureScale);
        }

        //new jme3utilities.Printer().printSubtree(rootNode);
        initializeUserInterface();
    }

    /**
     * Process an action from the GUI or keyboard which was not handled by the
     * default input mode.
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

        if (actionString.equals(actionStringToggle)) {
            boolean newState = !hud.isEnabled();
            hud.setEnabled(newState);
            return;
        }
        super.onAction(actionString, ongoing, ignored);
    }
    // *************************************************************************
    // SimpleApplication methods

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
         * Adjust vertical scale of the terrain based on a slider in the HUD.
         */
        float radius = 0.5f * cam.getFrustumFar();
        float baseY = 0f;
        float topY = hud.getRelief();
        landscapeControl.setTerrainScale(radius, baseY, topY);
    }
    // *************************************************************************
    // ViewPortListener methods

    /**
     * Callback when a view port is added, to apply shadows to the viewport.
     *
     * @param viewPort (not null)
     */
    @Override
    public void addViewPort(ViewPort viewPort) {
        assert viewPort != null;
        addShadows(viewPort);
    }

    /**
     * Callback when a view port is removed. Does nothing.
     */
    @Override
    public void removeViewPort(ViewPort unused) {
        /* no action required */
    }
    // *************************************************************************
    // private methods

    /**
     * Add shadows to a view port, using either a filter or a renderer.
     *
     * @param viewPort (not null)
     */
    private void addShadows(ViewPort viewPort) {
        assert viewPort != null;

        Updater updater = control.getUpdater();
        if (parameters.shadowFilter()) {
            dlsf = new DirectionalLightShadowFilter(
                    assetManager, shadowMapSize, shadowMapSplits);
            dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsf.setLight(mainLight);
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            fpp.addFilter(dlsf);
            updater.addShadowFilter(dlsf);
            viewPort.addProcessor(fpp);

        } else {
            dlsr = new DirectionalLightShadowRenderer(
                    assetManager, shadowMapSize, shadowMapSplits);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsr.setLight(mainLight);
            updater.addShadowRenderer(dlsr);
            viewPort.addProcessor(dlsr);
        }
    }

    /**
     * Configure the camera, including flyCam.
     */
    private void configureCamera() {
        /*
         * Point the camera 10 degrees north of west, tilted down 1 degree.
         * The downward tilt is to work around a bug in SimpleWater which was
         * fixed at r10899.
         */
        cam.setLocation(new Vector3f(6.5f, 13f, 50f));
        float altitudeAngle = -1f * FastMath.DEG_TO_RAD;
        float azimuthAngle = 280f * FastMath.DEG_TO_RAD;
        Vector3f direction = MyVector3f.fromAltAz(altitudeAngle, azimuthAngle);
        Vector3f up = Vector3f.UNIT_Y.clone();
        cam.lookAtDirection(direction, up);

        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(2f);
        flyCam.setMoveSpeed(20f);
        flyCam.setUpVector(up);
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
         * Disable display of JME statistics.
         * Statistics display can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Create and attach the heads-up display (HUD).
         */
        boolean reenableFlyCam = true;
        hud = new TestSkyControlHud(reenableFlyCam);
        success = stateManager.attach(hud);
        assert success;
        /*
         * Bind the 'H' hotkey to toggle HUD visibility in default input mode.
         */
        getDefaultInputMode().bind(actionStringToggle, KeyInput.KEY_H);
    }
}
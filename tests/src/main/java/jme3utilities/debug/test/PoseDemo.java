/*
 Copyright (c) 2017-2020, Stephen Gold
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
package jme3utilities.debug.test;

import com.jme3.app.StatsAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.Dumper;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.sky.Constants;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.StarsOption;
import jme3utilities.sky.Updater;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.UiVersion;

/**
 * GUI application to demonstrate AxesControl and SkeletonDebugControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PoseDemo extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * diameter of the platform (in world units, &gt;0)
     */
    final private static float platformDiameter = 4f;
    /**
     * thickness of the platform (in world units, &gt;0)
     */
    final private static float platformThickness = 0.1f;
    /**
     * width and height of rendered shadow maps (pixels per side, &gt;0)
     */
    final private static int shadowMapSize = 4_096;
    /**
     * number of shadow map splits (&gt;0)
     */
    final private static int shadowMapSplits = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            PoseDemo.class.getName());
    /**
     * path to hotkey bindings configuration asset
     */
    final private static String hotkeyBindingsAssetPath = "Interface/bindings/PoseDemo.properties";
    /**
     * name of the platform geometry
     */
    final static String platformName = "platform";
    /**
     * path to texture asset for the platform
     */
    final private static String platformTextureAssetPath = "Textures/Terrain/splat/dirt.jpg";
    // *************************************************************************
    // fields

    /**
     * Nifty screen for editing hotkey bindings
     */
    static BindScreen bindScreen = new BindScreen();
    /**
     * shadow filter for the scene
     */
    static DirectionalLightShadowFilter dlsf = null;
    /**
     * printer for scene dump
     */
    final private static Dumper dumper = new Dumper();
    /**
     * AppState to manage the loaded model
     */
    static ModelState modelState = new ModelState();
    /**
     * AppState to manage the Camera
     */
    static PoseCameraState cameraState = new PoseCameraState();
    /**
     * heads-up display (HUD)
     */
    static PoseDemoHud hudState = new PoseDemoHud();
    // *************************************************************************
    // new methods exposed

    /**
     * Access the scene-dump printer.
     */
    static Dumper getDumper() {
        return dumper;
    }

    /**
     * Main entry point for the PoseDemo application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        PoseDemo application = new PoseDemo();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle("PoseDemo");
        application.setSettings(settings);
        /*
         * Invoke the application startup code, which in turn
         * invokes guiInitializeApplication().
         */
        application.start();
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Callback to initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        /*
         * Log library versions.
         */
        logger.log(Level.INFO, "jme3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        logger.log(Level.INFO, "Heart version is {0}",
                MyString.quote(Heart.versionShort()));
        logger.log(Level.INFO, "SkyControl version is {0}",
                MyString.quote(Constants.versionShort()));
        logger.log(Level.INFO, "jme3-utilities-ui version is {0}",
                MyString.quote(UiVersion.versionShort()));
        logger.log(Level.INFO, "jme3-utilities-nifty version is {0}",
                MyString.quote(LibraryVersion.versionShort()));
        /*
         * Attach screen controllers for the HUD, the scene, and BindScreen.
         */
        boolean success = stateManager.attach(modelState);
        assert success;
        success = stateManager.attach(hudState);
        assert success;
        success = stateManager.attach(bindScreen);
        assert success;
        /*
         * Disable the StatsAppState.  F5 will re-enable it.
         */
        StatsAppState sas = stateManager.getState(StatsAppState.class);
        sas.setDisplayFps(false);
        sas.setDisplayStatView(false);
        /*
         * Disable flyCam and attach a custom camera app state.
         */
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(-2.4f, 1f, 1.6f));
        cam.setRotation(new Quaternion(0.006f, 0.86884f, -0.01049f, 0.49493f));
        success = stateManager.attach(cameraState);
        assert success;
        /*
         * Create lights, shadows, and a daytime sky.
         */
        createLightsAndSky();
        /*
         * Create a square platform.
         */
        createPlatform();
        /*
         * Add visible indicators for 3 global axes.
         */
        AxesVisualizer visualizer = new AxesVisualizer(assetManager, 1f, 1f);
        rootNode.addControl(visualizer);
        /*
         * Default input mode directly influences the camera state and
         * (indirectly) the HUD.
         */
        InputMode dim = getDefaultInputMode();
        dim.influence(cameraState);

        dim.setConfigPath(hotkeyBindingsAssetPath);
    }

    /**
     * Process an action (from the GUI or keyboard) which wasn't handled by the
     * default input mode or the HUD.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (ongoing) {
            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    return;
                case "print scene":
                    dumper.dump(renderManager);
                    return;
                case "toggle hud":
                    cameraState.toggleHud();
                    return;
                case "view horizontal":
                    cameraState.viewHorizontal();
                    return;
                case "warp cursor":
                    cameraState.warp();
                    return;
            }
        }
        /*
         * Forward unhandled action to the superclass.
         */
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Create lights, shadows, and a daytime sky.
     */
    private void createLightsAndSky() {
        /*
         * Light the scene.
         */
        AmbientLight ambientLight = new AmbientLight();
        rootNode.addLight(ambientLight);
        DirectionalLight mainLight = new DirectionalLight();
        rootNode.addLight(mainLight);
        /*
         * Add a shadow filter.
         */
        dlsf = new DirectionalLightShadowFilter(assetManager, shadowMapSize,
                shadowMapSplits);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsf.setLight(mainLight);
        int numSamples = settings.getSamples();
        Heart.getFpp(viewPort, assetManager, numSamples).addFilter(dlsf);
        /*
         * Create a daytime sky.
         */
        SkyControl sky = new SkyControl(assetManager, cam, 0.9f,
                StarsOption.TopDome, true);
        rootNode.addControl(sky);
        sky.setCloudiness(0.5f);
        sky.getSunAndStars().setHour(11f);
        sky.setEnabled(true);
        Updater updater = sky.getUpdater();
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        updater.addShadowFilter(dlsf);
        updater.setMainMultiplier(2f);
    }

    /**
     * Create a square platform for the model to stand on.
     */
    private void createPlatform() {
        float radius = platformDiameter / 2f;
        Mesh platformMesh = new Box(radius, platformThickness, radius);
        Spatial platform = new Geometry(platformName, platformMesh);

        boolean mipmaps = true;
        Texture dirt = MyAsset.loadTexture(assetManager,
                platformTextureAssetPath, mipmaps);
        Material mat = MyAsset.createShadedMaterial(assetManager, dirt);
        platform.setMaterial(mat);

        platform.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(platform);
        float yOffset = -1.001f * platformThickness;
        MySpatial.setWorldLocation(platform, new Vector3f(0f, yOffset, 0f));
    }
}

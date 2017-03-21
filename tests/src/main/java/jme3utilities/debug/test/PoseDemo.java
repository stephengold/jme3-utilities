/*
 Copyright (c) 2017, Stephen Gold
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
package jme3utilities.debug.test;

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
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.debug.AxesControl;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import jme3utilities.ui.InputMode;

/**
 * GUI application to demonstrate AxesControl and SkeletonDebugControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PoseDemo extends GuiApplication {
    // *************************************************************************
    // constants and loggers

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
    final private static String hotkeyBindingsAssetPath =
            "Interface/bindings/PoseDemo.properties";
    /**
     * path to texture asset for the floor
     */
    final private static String floorTextureAssetPath =
            "Textures/Terrain/splat/dirt.jpg";
    // *************************************************************************
    // fields

    static AxesControl axes = null;
    /**
     * Nifty screen for editing hotkey bindings
     */
    static BindScreen bindScreen = new BindScreen();

    static DirectionalLightShadowFilter dlsf = null;
    /**
     * app state to control the camera
     */
    static PoseCameraState cameraState = new PoseCameraState();
    /**
     * heads-up display (HUD)
     */
    static PoseDemoHud hud = new PoseDemoHud();
    // *************************************************************************
    // new methods exposed

    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
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
         * Attach screen controllers for the HUD and BindScreen.
         */
        boolean success = stateManager.attach(hud);
        assert success;
        success = stateManager.attach(bindScreen);
        assert success;
        /*
         * Disable flyCam and attach a custom camera app state.
         */
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(-2, 0.85f, 1.35f));
        cam.setRotation(new Quaternion(0.006f, 0.86884f, -0.01049f, 0.49493f));
        success = stateManager.attach(cameraState);
        assert success;
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
        Misc.getFpp(viewPort, assetManager).addFilter(dlsf);
        /*
         * Create a daytime sky.
         */
        SkyControl sky = new SkyControl(assetManager, cam, 0.9f, false, true);
        rootNode.addControl(sky);
        sky.setCloudiness(0.5f);
        sky.getSunAndStars().setHour(11f);
        sky.setEnabled(true);
        Updater updater = sky.getUpdater();
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        updater.addShadowFilter(dlsf);
        updater.setMainMultiplier(4f);
        /*
         * Create a square floor.
         */
        Mesh box = new Box(2f, 0.1f, 2f);
        Spatial floor = new Geometry("floor", box);
        Texture dirt = MyAsset.loadTexture(assetManager, floorTextureAssetPath);
        Material mat = MyAsset.createShadedMaterial(assetManager, dirt);
        floor.setMaterial(mat);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(floor);
        MySpatial.setWorldLocation(floor, new Vector3f(0f, -0.1001f, 0f));
        /*
         * Indicate the 3 principal axes.
         */
        axes = new AxesControl(assetManager, 0.3f, 2f);
        rootNode.addControl(axes);
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
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    return;
                case "toggle hud":
                    cameraState.toggleHud();
                    return;
            }
        }
        /*
         * The action has not yet been handled: forward to superclass.
         */
        super.onAction(actionString, ongoing, tpf);
    }
}

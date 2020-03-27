/*
 Copyright (c) 2013-2020, Stephen Gold
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
package jme3utilities.sky.test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.mesh.DomeMesh;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.sky.Constants;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;

/**
 * GUI application for testing the SkyMaterial class using a heads-up display
 * (HUD). The application's main entry point is here.
 * <p>
 * Use the 'H' key to toggle HUD visibility, the 'S' key to save the current sky
 * geometry, and the 'L' key to load a saved geometry.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSkyMaterial extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * aspect ratio of the viewport (width/height)
     */
    final private static float viewportAspectRatio = 1f;
    /**
     * number of samples in each longitudinal quadrant of the dome, including
     * the top and the rim (&ge;2)
     */
    final private static int quadrantSamples = 16;
    /**
     * number of samples around the rim of the dome (&ge;3)
     */
    final private static int rimSamples = 60;
    /**
     * width and height of the viewport (in pixels)
     */
    final private static int viewportSize = 800;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestSkyMaterial.class.getName());
    /**
     * action string to load sky geometry from an asset
     */
    final private static String actionStringLoad = "load sky";
    /**
     * action string to save the current sky geometry to a file
     */
    final private static String actionStringSave = "save sky";
    /**
     * action string to toggle HUD visibility
     */
    final private static String actionStringToggle = "toggle hud";
    /**
     * application name for the usage message and the window's title bar
     */
    final private static String applicationName = "TestSkyMaterial";
    /**
     * name for the dome geometry
     */
    final private static String geometryName = "sky dome";
    /**
     * asset path for loading and saving
     */
    final private static String saveAssetPath = "Models/TestSkyMaterial.j3o";
    // *************************************************************************
    // fields

    /**
     * true means just display the usage message; false means run the
     * application
     */
    @Parameter(names = {"-h", "-u", "--help", "--usage"}, help = true,
            description = "display this usage message")
    private static boolean usageOnly = false;
    /**
     * name of material to test, or null to auto-select
     */
    @Parameter(names = {"-m", "--material"}, description = "specify material")
    private static String materialName = null;
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
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        TestSkyMaterial application = new TestSkyMaterial();
        /*
         * Parse the command-line arguments.
         */
        JCommander jCommander = new JCommander(application);
        jCommander.parse(arguments);
        jCommander.setProgramName(applicationName);
        if (usageOnly) {
            jCommander.usage();
            return;
        }
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
        String title = applicationName + " " + MyString.join(arguments);
        settings.setTitle(title);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to TestSkyMaterial.guiInitializeApplication()!
         */
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        /*
         * Log library versions.
         */
        logger.log(Level.INFO, "jme3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        logger.log(Level.INFO, "SkyControl version is {0}",
                MyString.quote(Constants.versionShort()));

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
        SkyMaterial material;
        if (materialName == null) {
            /*
             * Auto-select the material asset.
             */
            int objects = 2;
            int cloudLayers = 2;
            material = new SkyMaterial(assetManager, objects, cloudLayers);
        } else {
            /*
             * Select material asset by pathname.
             */
            String assetPath = String.format("MatDefs/skies/%s/%s.j3md",
                    materialName, materialName);
            material = new SkyMaterial(assetManager, assetPath);
        }
        geometry.setMaterial(material);
        material.initialize();
        int maxCloudLayers = material.getMaxCloudLayers();
        if (maxCloudLayers > 0) {
            material.addClouds(0);
        }
        if (maxCloudLayers > 1) {
            material.addClouds(1);
        }
        material.addHaze();
        int maxObjects = material.getMaxObjects();
        if (maxObjects > TestSkyMaterialHud.moonIndex) {
            String imageAssetPath = LunarPhase.FULL.imagePath("");
            try {
                material.addObject(TestSkyMaterialHud.moonIndex,
                        imageAssetPath);
            } catch (AssetNotFoundException exception) {
                imageAssetPath = LunarPhase.FULL.imagePath("-nonviral");
                material.addObject(TestSkyMaterialHud.moonIndex,
                        imageAssetPath);
            }
        }
        if (maxObjects > TestSkyMaterialHud.sunIndex) {
            material.addObject(TestSkyMaterialHud.sunIndex,
                    SkyMaterial.sunMapPath);
        }
        material.addStars();
        /*
         * Create and apply a bloom filter to the viewport.
         */
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        int numSamples = settings.getSamples();
        FilterPostProcessor fpp
                = Heart.getFpp(viewPort, assetManager, numSamples);
        fpp.addFilter(bloom);

        initializeUserInterface(material, bloom);
    }

    /**
     * Callback invoked immediately after initializing the hotkey bindings of
     * the default input mode.
     */
    @Override
    public void moreDefaultBindings() {
        /*
         * Add/override hotkey bindings of the default input mode:
         *  'H' to toggle HUD visibility
         *  'L' to load sky geometry from an asset
         *  'S' to save the current sky geometry to a file
         */
        InputMode defaultInputMode = getDefaultInputMode();
        defaultInputMode.bind(actionStringToggle, KeyInput.KEY_H);
        defaultInputMode.bind(actionStringLoad, KeyInput.KEY_L);
        defaultInputMode.bind(actionStringSave, KeyInput.KEY_S);
    }

    /**
     * Process an action (from the GUI or keyboard) that was not handled by the
     * default input mode or the HUD.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case actionStringLoad:
                    load();
                    return;

                case actionStringSave:
                    save();
                    return;

                case actionStringToggle:
                    boolean newState = !hud.isEnabled();
                    hud.setEnabled(newState);
                    return;
            }
        }
        /*
         * The action is not handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, tpf);
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

        cam.setLocation(new Vector3f(0f, 0f, 0f));
        MyCamera.look(cam, Vector3f.UNIT_Y);
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
    }

    /**
     * Initialize the user interface.
     *
     * @param material sky material under test (not null)
     * @param bloom bloom filter applied to the viewport (not null)
     */
    private void initializeUserInterface(SkyMaterial material,
            BloomFilter bloom) {
        assert material != null;
        assert bloom != null;
        /*
         * Create and attach the heads-up display (HUD).
         */
        hud = new TestSkyMaterialHud(bloom);
        hud.setMaterial(material);
        boolean success = stateManager.attach(hud);
        assert success;
    }

    /**
     * Load sky geometry from an asset.
     */
    private void load() {
        /*
         * Remove any copy from the asset manager's cache.
         */
        ModelKey key = new ModelKey(saveAssetPath);
        assetManager.deleteFromCache(key);

        Geometry loadedDome;
        try {
            loadedDome = (Geometry) assetManager.loadAsset(key);
        } catch (AssetNotFoundException e) {
            logger.log(Level.SEVERE, "Didn''t find asset {0}",
                    MyString.quote(saveAssetPath));
            return;
        }
        SkyMaterial material = (SkyMaterial) loadedDome.getMaterial();
        assert material != null;

        logger.log(Level.INFO, "Loaded {0} from asset {1}", new Object[]{
            MyString.quote(loadedDome.getName()),
            MyString.quote(saveAssetPath)
        });

        Spatial oldDome = MySpatial.findChild(rootNode, geometryName);
        rootNode.detachChild(oldDome);

        rootNode.attachChild(loadedDome);
        hud.setMaterial(material);
    }

    /**
     * Save the current sky geometry to a J3O file.
     */
    private void save() {
        String filePath = ActionApplication.filePath(saveAssetPath);
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();
        Spatial dome = MySpatial.findChild(rootNode, geometryName);
        try {
            exporter.save(dome, file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving {0} to file {1}",
                    new Object[]{
                        MyString.quote(dome.getName()),
                        MyString.quote(filePath)
                    });
            return;
        }
        logger.log(Level.INFO, "Saved {0} to file {1}", new Object[]{
            MyString.quote(dome.getName()),
            MyString.quote(filePath)
        });
    }

    /**
     * (Re-)initialize the render camera's frustum.
     *
     * @param fovDegrees desired field-of-view angle in degrees (&gt;0)
     */
    private void setFrustum(float fovDegrees) {
        assert fovDegrees > 0f : fovDegrees;

        Camera camera = getCamera();
        float nearPlaneDistance = 0.1f;
        float farPlaneDistance = 10000f;
        camera.setFrustumPerspective(fovDegrees, viewportAspectRatio,
                nearPlaneDistance, farPlaneDistance);
    }
}

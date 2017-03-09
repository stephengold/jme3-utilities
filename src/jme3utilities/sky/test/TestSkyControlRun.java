/*
 Copyright (c) 2013-2017, Stephen Gold
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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ViewPortListener;
import jme3utilities.WaterProcessor;
import jme3utilities.debug.LandscapeControl;
import jme3utilities.debug.Printer;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.sky.CloudLayer;
import jme3utilities.sky.FloorControl;
import jme3utilities.sky.GlobeRenderer;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.SunAndStars;
import jme3utilities.sky.Updater;
import jme3utilities.ui.ActionAppState;

/**
 * Action app state to manage the scene for the TestSkyControl application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSkyControlRun
        extends ActionAppState
        implements ViewPortListener {
    // *************************************************************************
    // constants

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
            TestSkyControlRun.class.getName());
    /**
     * name for the cube geometry
     */
    final private static String cubeName = "cube";
    /**
     * asset path for loading and saving
     */
    final private static String savePath = "Models/TestSkyControl.j3o";
    // *************************************************************************
    // fields
    
    /**
     * ambient light-source in the scene
     */
    private AmbientLight ambientLight = null;
    /**
     * true &rarr; show the HUD when this state is enabled
     */
    private boolean showHud = true;
    /**
     * main light-source in the scene, which represents the sun or moon
     */
    private DirectionalLight mainLight = null;
    /**
     * floor control
     */
    private FloorControl floorControl = null;
    /**
     * landscape control
     */
    private LandscapeControl landscapeControl = null;
    /**
     * node to parent geometries that can appear reflected in water
     */
    private Node sceneNode = new Node("scene node");
    /**
     * printer for scene dump
     */
    final private Printer printer = new Printer();
    /**
     * control under test
     */
    private SkyControl skyControl = null;
    /**
     * star map background (external to SkyControl)
     */
    private Spatial cubeMap = null;
    /**
     * heads-up display (HUD) (set by initialize)
     */
    private TestSkyControlHud hud;
    /**
     * command-line parameters (set by initialize)
     */
    private TestSkyControlParameters parameters;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an uninitialized, disabled state.
     */
    TestSkyControlRun() {
        super(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Load the scene from an asset.
     */
    void load() {
        if (assetManager instanceof DesktopAssetManager) {
            /*
             * Clear cache to force loadModel() to read the J3O file.
             */
            DesktopAssetManager dam = (DesktopAssetManager) assetManager;
            dam.clearCache();
        }

        Spatial loadedScene = assetManager.loadModel(savePath);
        logger.log(Level.INFO, "Loaded {0} from asset {1}", new Object[]{
            MyString.quote(loadedScene.getName()),
            MyString.quote(savePath)
        });
        Node loadedNode = (Node) loadedScene;
        /*
         * Update cached references to refer to the newly loaded scene.
         */
        cubeMap = MySpatial.findChild(loadedNode, cubeName);
        assert cubeMap != null;
        floorControl = loadedNode.getControl(FloorControl.class);
        assert floorControl != null;
        landscapeControl = loadedNode.getControl(LandscapeControl.class);
        assert landscapeControl != null;

        Updater oldUpdater = skyControl.getUpdater();
        skyControl = loadedNode.getControl(SkyControl.class);
        Updater updater = skyControl.getUpdater();

        ambientLight = updater.getAmbientLight();
        assert ambientLight != null;
        Light sceneLight = MySpatial.findLight(loadedNode, AmbientLight.class);
        if (sceneLight != null) {
            loadedNode.removeLight(sceneLight);
        }

        mainLight = updater.getMainLight();
        assert mainLight != null;
        sceneLight = MySpatial.findLight(loadedNode, DirectionalLight.class);
        if (sceneLight != null) {
            loadedNode.removeLight(sceneLight);
        }
        /*
         * Set the cameras, filters, renderers, and viewports for the loaded 
         * controls.  (These were not serialized.)
         */
        floorControl.setCamera(cam);
        skyControl.setCamera(cam);
        updater.setFRV(oldUpdater);
        GlobeRenderer moonRenderer = stateManager.getState(GlobeRenderer.class);
        skyControl.setMoonRenderer(moonRenderer);
        /*
         * Replace the old scene node.
         */
        Node parent = sceneNode.getParent();
        parent.detachChild(sceneNode);
        parent.attachChild(loadedNode);
        sceneNode = loadedNode;
        /*
         * Set GUI controls in the HUD to match the scene.
         */
        hud.matchScene(skyControl, landscapeControl);
    }

    /**
     * Callback from the HUD to aim the camera at the moon, even if it's below
     * the horizon.
     */
    void lookAtTheMoon() {
        Vector3f moonDirection = skyControl.moonDirection();
        MyCamera.look(cam, moonDirection);
    }

    /**
     * Callback from the HUD to aim the camera at the sun, even if it's below
     * the horizon.
     */
    void lookAtTheSun() {
        SunAndStars sunAndStars = skyControl.getSunAndStars();
        Vector3f sunDirection = sunAndStars.sunDirection();
        MyCamera.look(cam, sunDirection);
    }

    /**
     * Dump the scene (as text) to the "standard" output stream.
     */
    void print() {
        printer.printSubtree(sceneNode);
    }

    /**
     * Save the current scene to a J3O file.
     */
    void save() {
        String filePath = "assets/" + savePath;
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        try {
            exporter.save(sceneNode, file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving {0} to file {1}",
                    new Object[]{
                MyString.quote(sceneNode.getName()),
                MyString.quote(filePath)
            });
            return;
        }
        logger.log(Level.INFO, "Saved {0} to file {1}", new Object[]{
            MyString.quote(sceneNode.getName()),
            MyString.quote(filePath)
        });
    }

    /**
     * Toggle the visibility of the HUD.
     */
    void toggleHud() {
        showHud = !showHud;
        if (isEnabled()) {
            hud.setEnabled(showHud);
        }
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Initialize this app state on the 1st update after it gets attached.
     *
     * @param sm the application's state manager (not null)
     * @param app the application that owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);

        hud = TestSkyControl.hud;
        assert hud != null;
        showHud = hud.isEnabled();
        parameters = TestSkyControl.parameters;
        assert parameters != null;

        initializeCamera();
        initializeLandscape();
        initializeLights();
        initializeSky();
        initializeFloor();
        /*
         * Add shadows to the main view port, with shadow intensities
         * updated by SkyControl.
         */
        addShadows(viewPort);

        if (parameters.water()) {
            addWater();
        }
        /*
         * Add bloom filter to the main view port.
         */
        addBloom(viewPort);
    }

    /**
     * Enable or disable the functionality of this state.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
        boolean oldSetting = isEnabled();
        super.setEnabled(newSetting);
        /*
         * Show HUD only when this state is enabled.
         */
        if (!oldSetting && newSetting) {
            TestSkyControl.hud.setEnabled(showHud);
        } else if (oldSetting && !newSetting) {
            TestSkyControl.hud.setEnabled(false);
        }
    }

    /**
     * Callback invoked once per render pass.
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        /*
         * Adjust SkyControl parameters based on GUI controls in the HUD.
         */
        LunarPhase lunarPhase = hud.getLunarPhase();
        skyControl.setPhase(lunarPhase);
        if (lunarPhase == LunarPhase.CUSTOM) {
            float longitudeDifference = hud.getLongitudeDifference();
            float lunarLatitude = hud.getLunarLatitude();
            skyControl.setPhase(longitudeDifference, lunarLatitude);
        }
        String sunStyle = hud.getSunStyle();
        skyControl.setSunStyle(sunStyle);

        float cloudiness = hud.getCloudiness();
        skyControl.setCloudiness(cloudiness);

        boolean cloudModulation = hud.getCloudModulation();
        skyControl.setCloudModulation(cloudModulation);

        float cloudRate = hud.getCloudRate();
        skyControl.setCloudRate(cloudRate);

        float ambientMultiplier = hud.getAmbientMultiplier();
        skyControl.getUpdater().setAmbientMultiplier(ambientMultiplier);
        float mainMultiplier = hud.getMainMultiplier();
        skyControl.getUpdater().setMainMultiplier(mainMultiplier);

        if (!parameters.singleDome()) {
            float cloudYOffset = TestSkyControl.hud.getCloudYOffset();
            skyControl.setCloudYOffset(cloudYOffset);
        }

        float hour = hud.getHour();
        skyControl.getSunAndStars().setHour(hour);

        float observerLatitude = hud.getLatitude();
        skyControl.getSunAndStars().setObserverLatitude(observerLatitude);

        float lunarDiameter = hud.getLunarDiameter();
        skyControl.setLunarDiameter(lunarDiameter);

        float solarDiameter = hud.getSolarDiameter();
        skyControl.setSolarDiameter(solarDiameter);

        float solarLongitude = hud.getSolarLongitude();
        skyControl.getSunAndStars().setSolarLongitude(solarLongitude);

        float topVerticalAngle = hud.getTopVerticalAngle();
        skyControl.setTopVerticalAngle(topVerticalAngle);
        /*
         * Adjust vertical scale of the terrain based on a slider in the HUD.
         */
        float radius = 0.5f * cam.getFrustumFar();
        float baseY = 0f;
        float topY = hud.getRelief();
        landscapeControl.setTerrainScale(radius, baseY, topY);
        /*
         * Enable or disable lights based on check boxes in the HUD.
         */
        boolean added = MySpatial.hasLight(sceneNode, ambientLight);
        boolean ambientLightOn = hud.getAmbientFlag();
        if (ambientLightOn && !added) {
            sceneNode.addLight(ambientLight);
        } else if (!ambientLightOn && added) {
            sceneNode.removeLight(ambientLight);
        }
        added = MySpatial.hasLight(sceneNode, mainLight);
        boolean mainLightOn = hud.getMainLightFlag();
        if (mainLightOn && !added) {
            sceneNode.addLight(mainLight);
        } else if (!mainLightOn && added) {
            sceneNode.removeLight(mainLight);
        }
        /*
         * Enable or disable controls based on check boxes in the HUD.
         */
        boolean floorFlag = hud.getFloorFlag();
        floorControl.setEnabled(floorFlag);
        boolean landscapeFlag = hud.getLandscapeFlag();
        landscapeControl.setEnabled(landscapeFlag);
        boolean skyFlag = hud.getSkyFlag();
        skyControl.setEnabled(skyFlag);
        /*
         * Enable or disable filters based on check boxes in the HUD.
         * Note that the shadow renderer (if any) cannot be disabled.
         */
        boolean bloomFlag = hud.getBloomFlag();
        skyControl.getUpdater().setBloomEnabled(bloomFlag);
        boolean shadowFiltersFlag = hud.getShadowFiltersFlag();
        skyControl.getUpdater().setShadowFiltersEnabled(shadowFiltersFlag);

        String starMapName = hud.getStarMapName();
        switch (starMapName) {
            case "4m":
                if (parameters.singleDome()) {
                    starMapName = "Textures/skies/star-maps/wiltshire.png";
                } else {
                    starMapName = "equator";
                }
                skyControl.setStarMaps(starMapName);
                break;
            case "16m":
                if (parameters.singleDome()) {
                    starMapName = "Textures/skies/star-maps/16m/wiltshire.png";
                } else {
                    starMapName = "equator16m";
                }
                skyControl.setStarMaps(starMapName);
                break;
            case "nebula":
                /*
                 * Turn off SkyControl's star maps to reveal the external sky.
                 */
                skyControl.clearStarMaps();
                break;
            default:
                throw new IllegalStateException("unknown star map name");
        }
        /*
         * Translate the external star map to center it on the camera.
         */
        Vector3f cameraLocation = cam.getLocation();
        MySpatial.setWorldLocation(cubeMap, cameraLocation);
        /*
         * Scale the external star map so that its geometries lie
         * between the near and far planes of the view frustum.
         */
        float far = cam.getFrustumFar();
        float near = cam.getFrustumNear();
        float cubeRadius = (near + far) / 2f;
        assert cubeRadius > near : cubeRadius;
        MySpatial.setWorldScale(cubeMap, cubeRadius);
        /*
         * Re-orient the external star map.
         */
        skyControl.getSunAndStars().orientEquatorialSky(cubeMap, false);
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
        Validate.nonNull(viewPort, "viewport");
        addShadows(viewPort);
    }

    /**
     * Callback when a view port is removed. Does nothing.
     *
     * @param unused which viewport (not null)
     */
    @Override
    public void removeViewPort(ViewPort unused) {
        /* no action taken */
    }
    // *************************************************************************
    // private methods

    /**
     * Add a bloom filter to a specified view port.
     *
     * @param viewPort (not null)
     */
    private void addBloom(ViewPort viewPort) {
        assert viewPort != null;

        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(1.7f);
        bloom.setBlurScale(2.5f);
        bloom.setExposurePower(1f);
        Misc.getFpp(viewPort, assetManager).addFilter(bloom);
        skyControl.getUpdater().addBloomFilter(bloom);
    }

    /**
     * Add shadows to a view port, using either a filter or a renderer.
     *
     * @param viewPort (not null)
     */
    private void addShadows(ViewPort viewPort) {
        assert viewPort != null;

        Updater updater = skyControl.getUpdater();
        if (parameters.shadowFilter()) {
            DirectionalLightShadowFilter dlsf;
            dlsf = new DirectionalLightShadowFilter(assetManager,
                    shadowMapSize, shadowMapSplits);
            dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsf.setLight(mainLight);
            Misc.getFpp(viewPort, assetManager).addFilter(dlsf);
            updater.addShadowFilter(dlsf);

        } else {
            DirectionalLightShadowRenderer dlsr;
            dlsr = new DirectionalLightShadowRenderer(assetManager,
                    shadowMapSize, shadowMapSplits);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
            dlsr.setLight(mainLight);
            updater.addShadowRenderer(dlsr);
            viewPort.addProcessor(dlsr);
        }
    }

    /**
     * Create a horizontal square of water and add it to the scene.
     * <p>
     * During initialization of the water processor (on the 1st update), the
     * processor will discover the SkyControl and put the SkyControl in charge
     * of the processor's background colors.
     */
    private void addWater() {
        WaterProcessor wp = new WaterProcessor(assetManager);
        viewPort.addProcessor(wp);
        wp.addListener(skyControl.getUpdater());
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

    /**
     * Add SkyControl to the scene and enable it.
     */
    private void initializeSky() {
        /*
         * Load and attach the external star map.
         */
        cubeMap = MyAsset.createStarMapQuads(assetManager,
                "purple-nebula-complex");
        cubeMap.setName(cubeName);
        sceneNode.attachChild(cubeMap);
        /*
         * Create a SkyControl to animate the sky.
         */
        float cloudFlattening;
        boolean bottomDome, starMotion;
        if (parameters.singleDome()) {
            cloudFlattening = 0f; // single dome implies clouds on top dome
            starMotion = false; // single dome implies non-moving stars
            bottomDome = false; // single dome implies exposed background
        } else {
            cloudFlattening = 0.9f; // overhead clouds 10x closer than horizon
            starMotion = true; // allow stars to move
            bottomDome = true; // helpful in case the scene has a low horizon
        }
        skyControl = new SkyControl(assetManager, cam, cloudFlattening,
                starMotion, bottomDome);
        sceneNode.addControl(skyControl);

        if (parameters.cyclone()) {
            CloudLayer mainLayer = skyControl.getCloudLayer(0);
            mainLayer.setMotion(0.37f, 0f, 0.2f, 0.001f);
            mainLayer.setTexture("Textures/skies/clouds/cyclone.png", 0.3f);
            skyControl.getCloudLayer(1).clearTexture();
        }
        /*
         * Set up a globe renderer for the Moon.
         */
        Texture moonTexture = MyAsset.loadTexture(assetManager,
                "Textures/skies/moon/clementine.png");
        Material moonMaterial = MyAsset.createShadedMaterial(assetManager,
                moonTexture);
        int equatorSamples = 12;
        int meridianSamples = 24;
        int resolution = 512;
        GlobeRenderer moonRenderer = new GlobeRenderer(moonMaterial,
                Image.Format.Luminance8Alpha8, equatorSamples, meridianSamples,
                resolution);
        stateManager.attach(moonRenderer);
        skyControl.setMoonRenderer(moonRenderer);
        /*
         * Put SkyControl in charge of updating both lights plus the
         * viewport background. (all optional)
         */
        Updater updater = skyControl.getUpdater();
        updater.addViewPort(viewPort);
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
    }

    /**
     * Configure the camera, including flyCam.
     */
    private void initializeCamera() {
        /*
         * Point the camera 10 degrees north of west, tilted down 1 degree.
         * The downward tilt is to work around a bug in SimpleWaterProcessor
         * which was fixed at r10899.
         */
        cam.setLocation(new Vector3f(6.5f, 13f, 50f));
        float altitudeAngle = MyMath.toRadians(-1f);
        float azimuthAngle = MyMath.toRadians(280f);
        Vector3f direction = MyVector3f.fromAltAz(altitudeAngle, azimuthAngle);
        MyCamera.look(cam, direction);

        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(2f);
        flyCam.setMoveSpeed(20f);
        flyCam.setUpVector(Vector3f.UNIT_Y);
        flyCam.setZoomSpeed(20f);
    }

    /**
     * Create a floor for the scene. Do this after the landscape is created and
     * after the sky in attached.
     */
    private void initializeFloor() {
        Material grass = landscapeControl.getGrass();
        float textureScale = 10f;
        floorControl = new FloorControl(cam, grass, textureScale);
        sceneNode.addControl(floorControl);
    }

    /**
     * Create, configure, and add the landscape/terrain.
     */
    private void initializeLandscape() {
        rootNode.attachChild(sceneNode);
        landscapeControl = new LandscapeControl(assetManager);
        sceneNode.addControl(landscapeControl);
    }

    /**
     * Create light sources for the scene.
     */
    private void initializeLights() {
        mainLight = new DirectionalLight();
        mainLight.setName("main");

        ambientLight = new AmbientLight();
        ambientLight.setName("ambient");
    }
}

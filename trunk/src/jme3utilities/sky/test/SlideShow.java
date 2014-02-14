/*
 Copyright (c) 2014, Stephen Gold
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

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.LandscapeControl;
import jme3utilities.debug.LoopMesh;
import jme3utilities.math.MyVector3f;
import jme3utilities.sky.DomeMesh;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;
import jme3utilities.sky.SunAndStars;

/**
 * A simple application to generate illustrations for a slide show about
 * SkyControl.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SlideShow
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * color of clear skyBlue sky
     */
    final public static ColorRGBA skyBlue = new ColorRGBA(0.4f, 0.6f, 1f, 1f);
    /**
     * 0900 hours, local solar time
     */
    final private static float baseHour = 9f;
    /**
     * 57 degrees north
     */
    final private static float baseLatitude = 1f;
    /*
     * scaling factor for inner domes
     */
    final private static float innerScale = 0.9f;
    /**
     * number of cloud layers in sky material
     */
    final public static int maxCloudLayers = 1;
    /**
     * number of objects in sky material
     */
    final public static int maxObjects = 2;
    /**
     * object index for the moon
     */
    final public static int moonIndex = 1;
    /**
     * object index for the sun
     */
    final public static int sunIndex = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SlideShow.class.getName());
    /**
     * application name for its window's title bar
     */
    final private static String applicationName = "SlideShow";
    /**
     * rotation vector for the moon in sky material
     */
    final private static Vector2f rotateMoon = new Vector2f(-1f, 0f);
    /**
     * UV coordinates for the moon in the sky material
     */
    final private static Vector2f uvMoon = new Vector2f(0.5f, 0.6f);
    /**
     * UV coordinates for the sun in the sky material
     */
    final private static Vector2f uvSun = new Vector2f(0.5f, 0.8f);
    // *************************************************************************
    // fields
    /**
     * mesh for a hemisphere with a slice removed
     */
    private DomeMesh cutawayMesh = null;
    /**
     * mesh for a complete hemisphere
     */
    private DomeMesh fullMesh = null;
    /**
     * dome to block stars below the horizon
     */
    private Geometry bottomDome = null;
    /**
     * circle to represent the cloud horizon
     */
    private Geometry cloudHorizon = null;
    /**
     * clouds-only dome
     */
    private Geometry cloudsDome = null;
    /**
     * circle to represent the celestial equator
     */
    private Geometry equator = null;
    /**
     * circle to represent the horizon
     */
    private Geometry horizon = null;
    /**
     * dome to represent the northern sky
     */
    private Geometry northDome = null;
    /**
     * dome to represent the southern sky
     */
    private Geometry southDome = null;
    /**
     * dome for sun, moon, clear sky color, and horizon haze
     */
    private Geometry topDome = null;
    /**
     * which slide is currently displayed
     */
    private int slideIndex = 0;
    /**
     * landscape control
     */
    private LandscapeControl landscapeControl = null;
    /**
     * mesh for a complete circle
     */
    private LoopMesh circle = null;
    /**
     * material to for the bottom dome
     */
    private Material bottomMaterial = null;
    /**
     * material to render wireframes
     */
    private Material wireframe = null;
    /**
     * node for attaching sky geometries and axes
     */
    final private Node skyNode = new Node("sky");
    /**
     * sky material for the clouds-only dome
     */
    private SkyMaterial cloudsMaterial = null;
    /**
     * sky material for the top dome
     */
    private SkyMaterial topMaterial = null;
    /**
     * orientation of the star domes relative to the observer
     */
    final private SunAndStars sunAndStars = new SunAndStars();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the illustrator.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);

        SlideShow application = new SlideShow();
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
         * ... and onward to SlideShow.simpleInitApp()!
         */
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a keystroke or mouse click.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        if (ongoing && "next".equals(actionString)) {
            slideIndex++;
            doStep();
        }
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize the SlideShow application.
     */
    @Override
    public void simpleInitApp() {
        /*
         * Log the jME3-utilities version string.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));

        configureCamera();
        generateMeshes();
        createSpatials();

        sunAndStars.setHour(baseHour);
        sunAndStars.setObserverLatitude(baseLatitude);
        /*
         * Initialize the material for wireframes.
         */
        wireframe = MyAsset.createUnshadedMaterial(assetManager);
        wireframe.setColor("Color", ColorRGBA.Magenta);
        wireframe.getAdditionalRenderState()
                .setFaceCullMode(FaceCullMode.Front);
        wireframe.getAdditionalRenderState().setWireframe(true);

        //new jme3utilities.Printer().printSubtree(rootNode);

        initializeUserInterface();
    }

    @Override
    public void simpleUpdate(float fps) {
        String message = String.format("Slide #%d", slideIndex);
        fpsText.setText(message);

        sunAndStars.orientStarDomes(northDome, southDome);
        Quaternion rot = northDome.getLocalRotation();
        equator.setLocalRotation(rot);
    }
    // *************************************************************************
    // private methods

    /**
     * Configure the camera, including flyCam.
     */
    private void configureCamera() {
        cam.setLocation(new Vector3f(159f, 177f, -250f));
        Vector3f lookDirection = cam.getLocation().negate().normalize();
        Vector3f upDirection = Vector3f.UNIT_Y;
        cam.lookAtDirection(lookDirection, upDirection);

        float frustumFar = 1000f;
        float frustumNear = 0.1f;
        MyCamera.setNearFar(cam, frustumNear, frustumFar);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(20f);
        flyCam.setRotationSpeed(2f);
        flyCam.setUpVector(upDirection);
        flyCam.setZoomSpeed(20f);
        /*
         * Apply a grey background to the viewport
         * to make starry black skies more visible.
         */
        ColorRGBA backgroundColor = new ColorRGBA(0.4f, 0.4f, 0.4f, 1f);
        viewPort.setBackgroundColor(backgroundColor);
    }

    /**
     * Create and initialize the sky node and all its geometries.
     */
    private void createSpatials() {
        /*
         * Initialize spatials.
         */
        float skyRadius = 100f;
        skyNode.setLocalScale(skyRadius);
        rootNode.attachChild(skyNode);

        float length = innerScale;
        float width = 2f;
        AxesControl axesControl = new AxesControl(assetManager, length, width);
        skyNode.addControl(axesControl);
        axesControl.setEnabled(true);

        landscapeControl = new LandscapeControl(assetManager);
        float baseY = 0f;
        float peakY = baseY + 0.1f * skyRadius;
        float radius = 0.4f * skyRadius;
        landscapeControl.setTerrainScale(radius, baseY, peakY);
        rootNode.addControl(landscapeControl);
        landscapeControl.setEnabled(true);

        bottomDome = new Geometry("bottom", fullMesh);
        Quaternion upsideDown = new Quaternion();
        upsideDown.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y.negate());
        bottomDome.setLocalRotation(upsideDown);
        bottomDome.setLocalScale(innerScale);

        cloudHorizon = new Geometry("cloud horizon", circle);
        float cloudScale = innerScale * innerScale;
        cloudHorizon.setLocalScale(cloudScale);

        cloudsDome = new Geometry("clouds", fullMesh);
        cloudsDome.setLocalScale(cloudScale);


        horizon = new Geometry("horizon", circle);
        horizon.setLocalScale(innerScale);

        equator = new Geometry("equator", circle);
        northDome = new Geometry("north", fullMesh);
        southDome = new Geometry("south", fullMesh);
        topDome = new Geometry("top", fullMesh);

        float altitude = -cutawayMesh.elevationAngle(uvSun.x, uvSun.y);
        float sunAzimuth = FastMath.HALF_PI;
        Vector3f sunlightDirection = MyVector3f.fromAltAz(altitude, sunAzimuth);
        DirectionalLight sunlight = new DirectionalLight();
        sunlight.setDirection(sunlightDirection);
        rootNode.addLight(sunlight);
    }

    /**
     *
     */
    private void doStep() {
        switch (slideIndex) {
            case 1:
                topMaterial = new SkyMaterial(assetManager, maxObjects,
                        maxCloudLayers);
                topMaterial.initialize();

                topMaterial.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Front);
                topMaterial.getAdditionalRenderState().setWireframe(true);
                topDome.setMaterial(topMaterial);
                /*
                 * Attach the top dome.
                 */
                skyNode.attachChild(topDome);
                break;
            case 2:
                /*
                 * Add stars to topMaterial.
                 */
                topMaterial.addStars();
                topMaterial.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                topMaterial.getAdditionalRenderState().setWireframe(false);
                topMaterial.setClearColor(ColorRGBA.BlackNoAlpha);
                break;
            case 3:
                /*
                 * Add sun.
                 */
                topMaterial.addObject(sunIndex, SkyMaterial.sunMapPath);

                topMaterial.setObjectTransform(sunIndex, uvSun, 0.2f, null);
                break;
            case 4:
                /*
                 * Add crescent moon.
                 */
                topMaterial.addObject(moonIndex,
                        LunarPhase.WANING_CRESCENT.imagePath());
                topMaterial.setObjectTransform(moonIndex, uvMoon, 0.05f,
                        rotateMoon);
                break;
            case 5:
                /*
                 * Add clear skyBlue sky.
                 */
                topMaterial.setClearColor(skyBlue);
                break;
            case 6:
                /*
                 * Add horizon haze.
                 */
                topMaterial.addHaze();
                break;
            case 7:
                /*
                 * Add clouds.
                 */
                topMaterial.addClouds(0);
                break;
            case 8:
                /*
                 * Remove a slice.
                 */
                topDome.setMesh(cutawayMesh);
                break;
            case 9:
                /*
                 * Translate the sky and axes slightly.
                 */
                skyNode.setLocalTranslation(0f, 3f, 0f);
                break;
            case 10:
                /*
                 * Translate the sky and axes slightly.
                 */
                skyNode.setLocalTranslation(5f, 5f, 0f);
                break;
            case 11:
                /*
                 * Translate the sky and axes slightly.
                 */
                skyNode.setLocalTranslation(0f, 2f, 5f);
                break;
            case 12:
                /*
                 * Revert translations.
                 */
                skyNode.setLocalTranslation(0f, 0f, 0f);
                break;
            case 13:
                /*
                 * Detach the top dome.
                 */
                skyNode.detachChild(topDome);
                break;
            case 14:
                /*
                 * Attach the north dome as a wireframe.
                 */
                skyNode.attachChild(northDome);
                northDome.setMaterial(wireframe);
                break;
            case 15:
                /*
                 * Apply starry material to the north dome.
                 */
                Material northern = MyAsset.createUnshadedMaterial(assetManager,
                        "Textures/skies/star-maps/northern.png");
                northern.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                northDome.setMaterial(northern);
                /*
                 * Attach the celestial equator.
                 */
                equator.setMaterial(wireframe);
                skyNode.attachChild(equator);
                break;
            case 16:
                /*
                 * Attach the south dome as a wireframe.
                 */
                skyNode.attachChild(southDome);
                southDome.setMaterial(wireframe);
                break;
            case 17:
                Material southern = MyAsset.createUnshadedMaterial(assetManager,
                        "Textures/skies/star-maps/southern.png");
                southern.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                /*
                 * Attach the south dome.
                 */
                southDome.setMaterial(southern);
                skyNode.attachChild(southDome);
                break;
            case 18:
                skyNode.detachChild(northDome);

                topMaterial = new SkyMaterial(assetManager, maxObjects,
                        maxCloudLayers);
                topMaterial.initialize();
                topMaterial.addClouds(0);
                topMaterial.addHaze();
                topMaterial.addObject(sunIndex, SkyMaterial.sunMapPath);
                topMaterial.setObjectTransform(sunIndex, uvSun, 0.2f, null);
                topMaterial.addObject(moonIndex,
                        LunarPhase.WANING_CRESCENT.imagePath());
                topMaterial.setObjectTransform(moonIndex, uvMoon, 0.05f,
                        rotateMoon);

                topMaterial.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                topMaterial.getAdditionalRenderState().setWireframe(false);

                topDome.setLocalScale(innerScale);
                topDome.setMaterial(topMaterial);
                skyNode.attachChild(topDome);
                break;
            case 19:
                /*
                 * Remove the sun and clear blue sky, and cancel the
                 * cutaway from the top dome.
                 */
                topMaterial.hideObject(sunIndex);
                topMaterial.setClearColor(ColorRGBA.BlackNoAlpha);
                topDome.setMesh(fullMesh);
                topDome.setQueueBucket(RenderQueue.Bucket.Translucent);
                break;
            case 20:
                /*
                 * Re-attach the north dome as a cutaway.
                 */
                northDome.setMesh(cutawayMesh);
                skyNode.attachChild(northDome);
                break;
            case 21:
                /*
                 * Adjust hour.
                 */
                sunAndStars.setHour(baseHour + 2f);
                break;
            case 22:
                /*
                 * Revert hour.
                 */
                sunAndStars.setHour(baseHour);
                break;
            case 23:
                /*
                 * Adjust latitude.
                 */
                sunAndStars.setObserverLatitude(FastMath.HALF_PI);
                break;
            case 24:
                /*
                 * Revert latitude.
                 */
                sunAndStars.setObserverLatitude(baseLatitude);
                break;
            case 25:
                /*
                 * Detach the north dome, cut away the top dome.
                 */
                skyNode.detachChild(northDome);
                topDome.setMesh(cutawayMesh);
                topMaterial.setClearColor(skyBlue);
                topMaterial.setObjectTransform(sunIndex, uvSun, 0.2f, null);
                topDome.setQueueBucket(RenderQueue.Bucket.Opaque);
                break;
            case 26:
                /*
                 * Attach the bottom dome as a wireframe.
                 */
                wireframe.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                bottomDome.setMaterial(wireframe);
                skyNode.attachChild(bottomDome);
                break;
            case 27:
                /*
                 * Attach the horizon.
                 */
                horizon.setMaterial(wireframe);
                skyNode.attachChild(horizon);
                /*
                 * Make bottom dome solid white.
                 */
                bottomMaterial = MyAsset.createUnshadedMaterial(assetManager);
                bottomMaterial.setColor("Color", ColorRGBA.White);
                bottomDome.setMaterial(bottomMaterial);
                break;
            case 28:
                /*
                 * Remove clouds from the top dome.
                 */
                topMaterial.setCloudsColor(0, ColorRGBA.BlackNoAlpha);
                /*
                 * Attach the clouds-only dome as a wireframe.
                 */
                cloudsDome.setMaterial(wireframe);
                skyNode.attachChild(cloudsDome);
                break;
            case 29:
                /*
                 * Attach the cloud horizon.
                 */
                cloudHorizon.setMaterial(wireframe);
                skyNode.attachChild(cloudHorizon);
                /*
                 * Apply translucent material to the clouds-only dome.
                 */
                cloudsMaterial = new SkyMaterial(assetManager, 0,
                        maxCloudLayers);
                cloudsMaterial.initialize();
                cloudsMaterial.getAdditionalRenderState()
                        .setFaceCullMode(FaceCullMode.Off);
                cloudsMaterial.addClouds(0);
                cloudsMaterial.setClearColor(ColorRGBA.BlackNoAlpha);
                cloudsDome.setMaterial(cloudsMaterial);
                cloudsDome.setQueueBucket(RenderQueue.Bucket.Translucent);
                break;
            case 30:
                /*
                 * flatten the clouds-only dome
                 */
                Vector3f scale = cloudsDome.getLocalScale();
                scale.y *= 0.5f; // flatten 50%
                cloudsDome.setLocalScale(scale);
                break;
            default: // 31
                /*
                 * flatten the clouds-only dome
                 */
                Vector3f trans = new Vector3f(0f, -0.2f, 0f);
                cloudsDome.setLocalTranslation(trans);
                cloudHorizon.setLocalTranslation(trans);
                slideIndex = 31;
                break;
        }
    }

    /**
     * Generate the meshes.
     */
    private void generateMeshes() {
        int quadrantSamples = 16;
        int rimSamples = 32;
        circle = new LoopMesh(rimSamples);
        circle.setLineWidth(2f);
        cutawayMesh = new DomeMesh(rimSamples, quadrantSamples);
        cutawayMesh.setSegmentAngle(5f);
        fullMesh = new DomeMesh(rimSamples, quadrantSamples);
    }

    /**
     * Initialize the user interface.
     */
    private void initializeUserInterface() {
        /*
         * Capture a screenshot each time the KEY_SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Disable jME3 stat view.
         */
        setDisplayStatView(false);
        /*
         * Press spacebar or left-click to advance the slides.
         */
        inputManager.addMapping("next",
                new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("next",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "next");
    }
}
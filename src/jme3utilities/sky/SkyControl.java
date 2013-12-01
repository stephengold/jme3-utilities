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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.AbstractShadowRenderer;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import jme3utilities.MyMath;
import jme3utilities.MySpatial;
import jme3utilities.SimpleControl;

/**
 * A simple control to simulate a dynamic sky dome using assets and techniques
 * derived from
 * http://code.google.com/p/jmonkeyplatform-contributions/source/browse/trunk/SkyDome
 *
 * While not astronomically accurate, the simulation approximates the motion of
 * the sun and moon. The sun crosses the meridian at noon (12:00 hours).
 *
 * The control is disabled by default. When enabled, it attaches a "sky" node to
 * the controlled spatial, which must also be a node. For best results, place
 * the scene's main light, ambient light, and directional light shadow renderer
 * under simulation control by invoking setMainLight(), setAmbientLight(), and
 * setShadowRenderer().
 *
 * The "top" dome is oriented so that its rim coincides with the horizon. The
 * top dome always includes the clear sky color, sun, moon, and horizon haze.
 * Object index 0 is used for the sun, and the remaining object indices are used
 * for phases of the moon.
 *
 * This control simulates two layers of clouds. The cloud density may be
 * adjusted by invoking setCloudiness(). The rate of cloud motion may be
 * adjusted by invoking setCloudsSpeed(). Flatten the clouds for best results;
 * this puts them on their own translucent "clouds only" dome.
 *
 * To simulate star motion, several more domes are added: one for northern
 * stars, one for southern stars, and an optional bottom dome which extends the
 * horizon haze for scenes with a low horizon.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * maximum value for any opacity
     */
    final static float alphaMax = 1f;
    /**
     * base color of the daytime sky: pale blue
     */
    final private static ColorRGBA colorDay =
            new ColorRGBA(0.4f, 0.6f, 1f, alphaMax);
    /**
     * light color and intensity for full moonlight: bluish gray
     */
    final private static ColorRGBA moonLight =
            new ColorRGBA(0.4f, 0.4f, 0.6f, alphaMax);
    /**
     * light color and intensity for moonless night: nearly black
     */
    final private static ColorRGBA starLight =
            new ColorRGBA(0.03f, 0.03f, 0.03f, alphaMax);
    /**
     * light color and intensity for full sunlight: yellowish white
     */
    final private static ColorRGBA sunLight =
            new ColorRGBA(0.8f, 0.8f, 0.75f, alphaMax);
    /**
     * color blended in around sunrise and sunset: ruddy orange
     */
    final private static ColorRGBA twilight =
            new ColorRGBA(0.8f, 0.4f, 0.2f, alphaMax);
    /**
     * minimum value for opacity
     */
    final static float alphaMin = 0f;
    /**
     * U-component of the initial offset of each cloud layer
     */
    final private static float[] layerU0 = {0.4f, DomeMesh.uvMin};
    /**
     * U-component of the standard motion of each cloud layer (cycles per
     * second)
     */
    final private static float[] layerURate = {-0.0005f, 0.0003f};
    /**
     * V-component of the initial offset of each cloud layer
     */
    final private static float[] layerV0 = {0.3f, DomeMesh.uvMin};
    /**
     * V-component of the standard motion of each cloud layer (cycles per
     * second)
     */
    final private static float[] layerVRate = {0.003f, 0.001f};
    /**
     * extent of the twilight periods before sunrise and after sunset, expressed
     * as the sine of the sun's angle below the horizon (<=1, >=0)
     */
    final private static float limitOfTwilight = 0.1f;
    /**
     * the duration of a full day (in hours)
     */
    final public static int hoursPerDay = 24;
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final static float radiansPerHour = FastMath.TWO_PI / hoursPerDay;
    /**
     * texture scale for Sun_L.png
     */
    final private static float sunScale = 0.08f;
    /**
     * object index for the first lunar phase
     */
    final private static int moonBaseIndex = 1;
    /**
     * number of cloud layers
     */
    final private static int numCloudLayers = 2;
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
     * object index for the sun
     */
    final public static int sunIndex = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SkyControl.class.getName());
    /**
     * asset path of the northern sky texture map
     */
    final private static String northAssetPath =
            "Textures/skies/star-maps/northern.png";
    /**
     * asset path of the southern sky texture map
     */
    final private static String southAssetPath =
            "Textures/skies/star-maps/southern.png";
    /**
     * asset path of the Unshaded material definition
     */
    final private static String unshadedMaterialAssetPath =
            "Common/MatDefs/Misc/Unshaded.j3md";
    // *************************************************************************
    // fields
    /**
     * which shadow renderer to update (or null for none)
     */
    private AbstractShadowRenderer shadowRenderer = null;
    /**
     * which ambient light to update (or null for none)
     */
    private AmbientLight ambientLight = null;
    /**
     * which asset manager to use for loading textures and material definitions:
     * set by constructor
     */
    final private AssetManager assetManager;
    /**
     * true to create a material and geometry for the hemisphere below the
     * horizon, false to leave this hemisphere to background color (if
     * starMotionFlag=false) or stars (if starMotionFlag=true)
     */
    final private boolean bottomDomeFlag;
    /**
     * true if clouds modulate the main light, false for steady light
     */
    private boolean cloudModulationFlag = false;
    /**
     * true to simulate moving stars, false for fixed stars
     */
    final private boolean starMotionFlag;
    /**
     * the application's camera: set by constructor
     */
    final private Camera camera;
    /**
     * viewports whose background colors are updated by this control
     */
    final private Collection<ViewPort> viewPorts = new ArrayList<>();
    /**
     * which directional light to update (or null for none)
     */
    private DirectionalLight mainLight = null;
    /**
     * mesh used to generate dome geometries
     */
    private DomeMesh mesh = null;
    /**
     * cloud opacity: 0=cloudless, 1=maximum opacity
     */
    private float cloudiness = 0f;
    /**
     * simulation time for cloud layer animations
     */
    private float cloudsAnimationTime = 0f;
    /**
     * rate for cloud layer animations (1=standard)
     */
    private float cloudsRelativeSpeed = 1f;
    /**
     * texture scale for moon images; larger value results in a larger moon
     *
     * The default value of 0.02 exaggerates the moon's size by a factor of 8.
     */
    private float moonScale = 0.02f;
    /**
     * flattened dome for clouds only: set by initialize()
     */
    private Geometry cloudsOnlyDome = null;
    /**
     * dome representing the northern stars: set by initialize()
     */
    private Geometry northDome = null;
    /**
     * dome representing the southern stars: set by initialize()
     */
    private Geometry southDome = null;
    /**
     * phase of the moon
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * material for bottom dome: set by constructor
     */
    final private Material bottomMaterial;
    /**
     * parent node for attaching the geometries: set by initialize()
     */
    private Node skyNode = null;
    /**
     * material for clouds-only dome: set by constructor
     */
    final private SkyMaterial cloudsMaterial;
    /**
     * material for top dome: set by constructor
     */
    final private SkyMaterial topMaterial;
    /**
     * orientations of the sun and stars relative to the observer
     */
    final private SunAndStars sunAndStars = new SunAndStars();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control for dense clouds moving at the standard
     * speed, with no lights and no viewports.
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param camera the application's camera (not null)
     * @param cloudFlattening the oblateness (ellipticity) of the dome with the
     * clouds: 0=no flattening (hemisphere), 1=maximum flattening
     * @param starMotion true to simulate moving stars, false for fixed stars
     * @param bottomDome true to create a material and geometry for the
     * hemisphere below the horizon, false to leave this hemisphere to
     * background color (if starMotionFlag=false) or stars (if
     * starMotionFlag=true)
     */
    public SkyControl(AssetManager assetManager, Camera camera,
            float cloudFlattening, boolean starMotion, boolean bottomDome) {
        super.setEnabled(false);
        if (assetManager == null) {
            throw new NullPointerException("asset manager cannot be null");
        }
        if (camera == null) {
            throw new NullPointerException("camera cannot be null");
        }
        if (cloudFlattening < 0f || cloudFlattening >= 1f) {
            throw new IllegalArgumentException(
                    "flattening must be between 0 and 1");
        }

        this.assetManager = assetManager;
        this.camera = camera;
        this.starMotionFlag = starMotion;
        this.bottomDomeFlag = bottomDome;
        /*
         * Create and initialize the sky material for sun, moon, and haze.
         */
        topMaterial = new SkyMaterial(assetManager,
                "MatDefs/skies/dome62/dome62.j3md", 6, numCloudLayers);
        topMaterial.initialize();
        topMaterial.addHaze();
        topMaterial.addObject(sunIndex, SkyMaterial.sunMapPath);
        for (LunarPhase potm : LunarPhase.values()) {
            int objectIndex = potm.ordinal() + moonBaseIndex;
            String assetPath = potm.imagePath();
            topMaterial.addObject(objectIndex, assetPath);
        }
        if (!starMotionFlag) {
            topMaterial.addStars();
        }

        if (cloudFlattening != 0f) {
            /*
             * Create and initialize a separate sky material for clouds only.
             */
            cloudsMaterial = new SkyMaterial(assetManager,
                    "MatDefs/skies/dome02/dome02.j3md", 0, numCloudLayers);
            cloudsMaterial.initialize();
            cloudsMaterial.getAdditionalRenderState().setDepthWrite(false);
            cloudsMaterial.setClearColor(ColorRGBA.BlackNoAlpha);
        } else {
            cloudsMaterial = topMaterial;
        }
        int numLayers = cloudsMaterial.getMaxCloudLayers();
        for (int layer = 0; layer < numLayers; layer++) {
            cloudsMaterial.addClouds(layer);
            cloudsMaterial.setCloudsScale(layer, 1.5f);
        }

        if (bottomDomeFlag) {
            /*
             * Create and initialize a material for the bottom dome.
             */
            bottomMaterial = new Material(assetManager,
                    unshadedMaterialAssetPath);
        } else {
            bottomMaterial = null;
        }

        createSpatials(cloudFlattening);

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a viewport to the collection of viewports whose background colors are
     * updated by this control.
     *
     * @param viewPort (not null)
     */
    public void addViewPort(ViewPort viewPort) {
        if (viewPort == null) {
            throw new NullPointerException("view port cannot be null");
        }

        viewPorts.add(viewPort);
    }

    /**
     * Access the orientations of the sun and stars.
     *
     * @return the pre-existing object
     */
    public SunAndStars getSunAndStars() {
        return sunAndStars;
    }

    /**
     * Save a reference to the scene's main ambient light. As long as the
     * reference has a non-null value, this control will continuously update the
     * light's color and intensity.
     *
     * @param ambientLight the scene's main ambient light (or null for none)
     */
    public void setAmbientLight(AmbientLight ambientLight) {
        this.ambientLight = ambientLight;
    }

    /**
     * Alter the opacity of the clouds.
     *
     * @param newAlpha desired opacity of the cloud layers (<=1, >=0)
     */
    public void setCloudiness(float newAlpha) {
        if (newAlpha < alphaMin || newAlpha > alphaMax) {
            throw new IllegalArgumentException(
                    "alpha must be between 0 and 1, inclusive");
        }

        cloudiness = newAlpha;
    }

    /**
     * Alter the cloud modulation flag.
     *
     * @param newValue true for clouds to modulate the main light, false for a
     * steady main light
     */
    public void setCloudModulation(boolean newValue) {
        cloudModulationFlag = newValue;
    }

    /**
     * Alter the speed or direction of cloud motion.
     *
     * @param newRate rate relative to the standard (may be negative)
     */
    public void setCloudRate(float newRate) {
        cloudsRelativeSpeed = newRate;
    }

    /**
     * Alter the angular diameter of the moon.
     *
     * @param newDiameter (in radians, <Pi, >0)
     */
    public void setLunarDiameter(float newDiameter) {
        if (newDiameter <= 0f || newDiameter >= FastMath.PI) {
            throw new IllegalArgumentException(
                    "diameter must be between 0 and Pi");
        }

        moonScale = newDiameter * mesh.uvScale / FastMath.HALF_PI;
    }

    /**
     * Alter the phase of the moon.
     *
     * @param phase (or null to hide the moon)
     */
    public void setPhase(LunarPhase phase) {
        this.phase = phase;
    }

    /**
     * Save a reference to the scene's main directional light. As long as the
     * reference has a non-null value, this control will continuously update the
     * light's color and direction.
     *
     * @param mainLight the scene's main directional light (or null for none)
     */
    public void setMainLight(DirectionalLight mainLight) {
        this.mainLight = mainLight;
    }

    /**
     * Save a reference to the scene's shadow renderer. As long as the reference
     * has a non-null value, this control will continuously update the
     * renderer's shadow intensity.
     *
     * @param renderer the scene's shadow renderer (or null for none)
     */
    public void setShadowRenderer(AbstractShadowRenderer renderer) {
        this.shadowRenderer = renderer;
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Alter the visibility of this control's sky. This control must be added to
     * a node before its sky can be revealed.
     *
     * @param newState if true, reveal the sky; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        Node node = (Node) spatial;

        if (enabled && !newState) {
            if (node != null) {
                /*
                 * Detach the sky node from the controlled node.
                 */
                int position = node.detachChild(skyNode);
                assert position != -1;
            }

        } else if (!enabled && newState) {
            if (node == null) {
                throw new IllegalStateException(
                        "cannot enable control before adding it to a node");
            }
            /*
             * Attach the sky node to the controlled node.
             */
            node.attachChild(skyNode);
            /*
             * Scale skyNode that the geometries lie near the far end of
             * the view frustrum.
             */
            float radius = camera.getFrustumFar();
            MySpatial.setWorldScale(skyNode, radius);
        }
        super.setEnabled(newState);
    }

    /**
     * Alter the controlled node.
     *
     * @param newNode which node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newNode) {
        super.setSpatial(newNode);
        if (enabled && newNode != null) {
            ((Node) spatial).attachChild(skyNode);
        }
    }

    /**
     * Callback to update this control. (Invoked once per frame.)
     *
     * @param tpf seconds since the previous update (>=0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (spatial == null || !enabled) {
            return;
        }
        assert tpf >= 0f : tpf;
        updateClouds(tpf);
        /*
         * Translate the sky node to center it on the camera.
         */
        Vector3f cameraLocation = camera.getLocation();
        MySpatial.setWorldLocation(skyNode, cameraLocation);

        updateFull();
    }
    // *************************************************************************
    // private methods

    /**
     * Create and initialize the sky node and all the dome geometries.
     */
    private void createSpatials(float cloudFlattening) {
        /*
         * Sky node serves as the parent for all sky geometries.
         */
        skyNode = new Node("sky node");
        skyNode.setShadowMode(ShadowMode.Off);

        mesh = new DomeMesh(rimSamples, quadrantSamples);

        float innerDomeScale;
        if (starMotionFlag) {
            /*
             * For star motion, make the bottom, cloud-only, and top domes
             * slightly smaller than the star domes, so stars will be visible
             * only at night.
             */
            innerDomeScale = 0.9f;
        } else {
            innerDomeScale = 1f;
        }

        if (bottomDomeFlag) {
            Geometry bottomDome = new Geometry("bottom", mesh);
            skyNode.attachChild(bottomDome);
            Quaternion upsideDown = new Quaternion();
            upsideDown.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y.negate());
            bottomDome.setLocalRotation(upsideDown);
            bottomDome.setLocalScale(innerDomeScale);
            bottomDome.setMaterial(bottomMaterial);
        }

        if (starMotionFlag) {
            northDome = new Geometry("north", mesh);
            skyNode.attachChild(northDome);
            Material north = createUnshadedMaterial(northAssetPath);
            northDome.setMaterial(north);
            northDome.setQueueBucket(Bucket.Sky);

            southDome = new Geometry("south", mesh);
            skyNode.attachChild(southDome);
            Material south = createUnshadedMaterial(southAssetPath);
            southDome.setMaterial(south);
            southDome.setQueueBucket(Bucket.Sky);
        }

        Geometry topDome = new Geometry("top", mesh);
        skyNode.attachChild(topDome);
        topDome.setLocalScale(innerDomeScale);
        topDome.setMaterial(topMaterial);
        topDome.setQueueBucket(Bucket.Translucent);

        if (cloudsMaterial != topMaterial) {
            assert cloudFlattening > 0f : cloudFlattening;
            assert cloudFlattening < 1f : cloudFlattening;
            /*
             * Flatten the clouds-only dome in order to foreshorten clouds
             * near the horizon.
             */
            cloudsOnlyDome = new Geometry("clouds", mesh);
            skyNode.attachChild(cloudsOnlyDome);
            float yScale = innerDomeScale * (1f - cloudFlattening);
            cloudsOnlyDome.setLocalScale(innerDomeScale, yScale, innerDomeScale);
            cloudsOnlyDome.setMaterial(cloudsMaterial);
            cloudsOnlyDome.setQueueBucket(Bucket.Translucent);
        }
    }

    /**
     * Create an unshaded material from a texture asset path.
     *
     * @param assetPath to load the texture from (not null)
     * @return a new instance
     */
    private Material createUnshadedMaterial(String assetPath) {
        assert assetPath != null;

        Material result = new Material(assetManager, unshadedMaterialAssetPath);
        Texture texture = loadTexture(assetPath);
        result.setTexture("ColorMap", texture);
        return result;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetPath pathname to the texture asset (not null)
     * @return the texture which was loaded
     */
    private Texture loadTexture(String assetPath) {
        assert assetPath != null;

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }

    /**
     * Update astronomical objects, sky color, lighting, and stars.
     */
    private void updateFull() {
        Vector3f sunDirection = updateSun();
        /*
         * Daytime sky texture is phased in during the twilight periods
         * before sunrise and after sunset. Update the sky material's
         * clear color accordingly.
         */
        ColorRGBA clearColor = colorDay.clone();
        clearColor.a = MyMath.clampFraction(1f + sunDirection.y / limitOfTwilight);
        topMaterial.setClearColor(clearColor);

        Vector3f moonDirection = updateMoon();
        updateLighting(sunDirection, moonDirection);
        if (starMotionFlag) {
            updateStars();
        }
    }

    /**
     * Update the cloud layers. (Invoked once per frame.)
     *
     * @param tpf seconds since the previous update (>=0)
     */
    private void updateClouds(float tpf) {
        assert tpf >= 0f : tpf;

        cloudsAnimationTime += tpf * cloudsRelativeSpeed;
        for (int layer = 0; layer < numCloudLayers; layer++) {
            float u = layerU0[layer] + cloudsAnimationTime * layerURate[layer];
            float v = layerV0[layer] + cloudsAnimationTime * layerVRate[layer];
            cloudsMaterial.setCloudsOffset(layer, u, v);
        }
    }

    /**
     * Update background colors, cloud colors, haze color, lights, and shadows.
     *
     * @param sunDirection the world direction to the sun (unit vector)
     * @param moonDirection the world direction to the moon (unit vector or
     * null)
     */
    private void updateLighting(Vector3f sunDirection, Vector3f moonDirection) {
        assert sunDirection != null;
        assert sunDirection.isUnitVector() : sunDirection;
        if (moonDirection != null) {
            assert moonDirection.isUnitVector() : moonDirection;
        }
        /*
         * Determine the direction to the main light source.
         */
        float sineSolarAltitude = sunDirection.y;
        boolean sunUp = sineSolarAltitude >= 0f;
        boolean moonUp = moonDirection != null && moonDirection.y >= 0f;
        Vector3f mainDirection;
        if (sunUp) {
            mainDirection = sunDirection.clone();
        } else if (moonUp) {
            mainDirection = moonDirection.clone();
        } else {
            mainDirection = Vector3f.UNIT_Y.clone();
        }
        assert mainDirection.isUnitVector() : mainDirection;
        assert mainDirection.y >= 0f : mainDirection;
        /*
         * Determine the base color (applied to horizon haze, bottom dome, and
         * viewport backgrounds) based on the altitudes of the sun and moon:
         *  + sunlight when ssa >= 0.25,
         *  + twilight when ssa = 0,
         *  + moonlight when ssa <= -0.04 and sla >=0, and
         *  + starlight when ssa <= -0.04 and sla < 0,
         * with interpolated transitions around sunrise and sunset.
         */
        ColorRGBA baseColor = new ColorRGBA();
        if (sunUp) {
            float weight = MyMath.clampFraction(sineSolarAltitude / 0.25f);
            baseColor.interpolate(twilight, sunLight, weight);
        } else {
            float weight = MyMath.clampFraction(-sineSolarAltitude / 0.04f);
            if (moonUp) {
                baseColor.interpolate(twilight, moonLight, weight);
            } else {
                baseColor.interpolate(twilight, starLight, weight);
            }
        }

        topMaterial.setHazeColor(baseColor);
        if (bottomMaterial != null) {
            bottomMaterial.setColor("Color", baseColor);
        }
        for (ViewPort viewPort : viewPorts) {
            viewPort.setBackgroundColor(baseColor);
        }
        /*
         * Each cloud layer gets a saturated version of the base color,
         * with its opacity equal to the sky's cloudiness.
         */
        float factor = 1f / MyMath.max(baseColor.r, baseColor.g, baseColor.b);
        ColorRGBA cloudsColor = baseColor.mult(factor);
        if (!sunUp && !moonUp) {
            /*
             * On moonless nights, darken all clouds by 75%.
             */
            cloudsColor.multLocal(0.25f);
        }
        cloudsColor.a = cloudiness;
        int numLayers = cloudsMaterial.getMaxCloudLayers();
        for (int layer = 0; layer < numLayers; layer++) {
            cloudsMaterial.setCloudsColor(layer, cloudsColor);
        }
        /*
         * The main light is based on the base color during the day,
         * on moonlight at night when the moon is up, and on starlight
         * on moonless nights.
         */
        ColorRGBA main;
        if (sunUp) {
            main = baseColor.clone();
        } else if (moonUp) {
            main = moonLight.clone();
        } else {
            main = starLight.clone();
        }
        /* The main light's intensity is modulated by the cube root of the
         * y-component of the light's direction and also (if cloud modulation
         * is enabled) by any clouds passing in front of it.
         */
        float mainFactor = MyMath.cubeRoot(mainDirection.y);
        if (cloudModulationFlag) {
            /*
             * Calculate where mainDirection intersects the cloud dome,
             * in the dome's local coordinates.
             */
            float minorAxis = cloudsOnlyDome.getLocalScale().y
                    / cloudsOnlyDome.getLocalScale().x;
            float sinAltitude = mainDirection.y;
            float cosAltitude = MyMath.circle(sinAltitude);
            float distance = minorAxis / MyMath.hypotenuse(sinAltitude,
                    cosAltitude * minorAxis);
            float x = distance * mainDirection.x;
            float z = distance * mainDirection.z;
            float y = MyMath.sphere(x, z);
            Vector3f intersection = new Vector3f(x, y, z);
            /*
             * Calculate the texture coordinates of the cloud dome
             * at the point of intersection.
             */
            Vector2f texCoord = mesh.directionUV(intersection);

            float cloudsTransmission = cloudsMaterial.getTransmission(texCoord);
            mainFactor *= cloudsTransmission;
        }
        main.multLocal(mainFactor);

        if (mainLight != null) {
            mainLight.setColor(main);
            /*
             * The direction of the main light is the direction in which it
             * propagates, which is the opposite of the direction to the
             * light source.
             */
            Vector3f lightDirection = mainDirection.negate();
            mainLight.setDirection(lightDirection);
        }
        /*
         * Set ambient light based on the cloud color; its intensity is
         * modulated according to the "slack" left by strongest component
         * of the main light.
         */
        float slack = 1f - MyMath.max(main.r, main.g, main.b);
        assert slack >= 0f : slack;
        ColorRGBA ambient = cloudsColor.mult(slack);
        if (ambientLight != null) {
            ambientLight.setColor(ambient);
        }

        if (shadowRenderer != null) {
            float mainAmount = main.r + main.g + main.b;
            float ambientAmount = ambient.r + ambient.g + ambient.b;
            float totalAmount = mainAmount + ambientAmount;
            float shadowIntensity;
            if (totalAmount > 0f) {
                /*
                 * Set shadow intensity equal to the fraction of
                 * the total light which is directional.
                 */
                shadowIntensity = mainAmount / totalAmount;
            } else {
                shadowIntensity = alphaMin;
            }
            shadowRenderer.setShadowIntensity(shadowIntensity);
        }
    }

    /**
     * Update the moon's position and size.
     *
     * @return world direction to the moon (new unit vector) or null if the moon
     * is hidden
     */
    private Vector3f updateMoon() {
        if (phase == null) {
            for (LunarPhase potm : LunarPhase.values()) {
                int objectIndex = potm.ordinal() + moonBaseIndex;
                topMaterial.hideObject(objectIndex);
            }
            return null;
        }

        float solarLongitude = sunAndStars.getSolarLongitude();
        float celestialLongitude = solarLongitude + phase.longitudeDifference();
        celestialLongitude = MyMath.modulo(celestialLongitude, FastMath.TWO_PI);
        Vector3f worldDirection =
                sunAndStars.convertToWorld(0f, celestialLongitude);
        /*
         * Convert world coordinates to mesh coordinates.
         */
        Vector2f uv = mesh.directionUV(worldDirection);
        /*
         * Update the active phases's position and size and hide
         * phases that are inactive.
         */
        int activeObjectIndex = phase.ordinal() + moonBaseIndex;
        for (LunarPhase potm : LunarPhase.values()) {
            int objectIndex = potm.ordinal() + moonBaseIndex;
            if (objectIndex == activeObjectIndex && uv != null) {
                /*
                 * Reveal the object and update its texture transform.
                 */
                Vector2f rotation = new Vector2f(1f, 0f); // TODO
                topMaterial.setObjectTransform(objectIndex, uv, moonScale,
                        rotation);
            } else {
                topMaterial.hideObject(objectIndex);
            }
        }
        return worldDirection;
    }

    /**
     * Update the orientation of the star domes.
     */
    private void updateStars() {
        float siderealAngle = sunAndStars.getSiderealAngle();
        Quaternion yRotation = new Quaternion();
        Quaternion zRotation = new Quaternion();
        /*
         * Rotate the star domes.
         */
        yRotation.fromAngleNormalAxis(-siderealAngle, Vector3f.UNIT_Y);
        float coLatitude = FastMath.HALF_PI - sunAndStars.getObserverLatitude();
        zRotation.fromAngleNormalAxis(-coLatitude, Vector3f.UNIT_Z);
        Quaternion orientation = zRotation.mult(yRotation);
        MySpatial.setWorldOrientation(northDome, orientation);

        yRotation.fromAngleNormalAxis(siderealAngle, Vector3f.UNIT_Y);
        float angle = FastMath.HALF_PI + sunAndStars.getObserverLatitude();
        zRotation.fromAngleNormalAxis(angle, Vector3f.UNIT_Z);
        orientation = zRotation.mult(yRotation);
        MySpatial.setWorldOrientation(southDome, orientation);
    }

    /**
     * Update the sun's position and size.
     *
     * @return world direction to the sun (new unit vector)
     */
    private Vector3f updateSun() {
        float solarLongitude = sunAndStars.getSolarLongitude();
        Vector3f worldDirection =
                sunAndStars.convertToWorld(0f, solarLongitude);
        /*
         * Convert world direction to mesh coordinates.
         */
        Vector2f uv = mesh.directionUV(worldDirection);

        if (uv == null) {
            /*
             * The sun is below the horizon, so hide it.
             */
            topMaterial.hideObject(sunIndex);
        } else {
            topMaterial.setObjectTransform(sunIndex, uv, sunScale, null);
        }

        return worldDirection;
    }
}
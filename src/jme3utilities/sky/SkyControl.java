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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySpatial;
import jme3utilities.SimpleControl;
import jme3utilities.math.MyMath;

/**
 * A simple control to simulate a dynamic sky using assets and techniques
 * derived from
 * http://code.google.com/p/jmonkeyplatform-contributions/source/browse/trunk/SkyDome
 * <p>
 * While not astronomically accurate, the simulation approximates the motion of
 * the sun and moon as seen from Earth. The coordinate system is: +X=north
 * horizon, +Y=zenith (up), and +Z=east horizon. The sun crosses the meridian at
 * noon (12:00 hours).
 * <p>
 * The control is disabled at creation. When enabled, it attaches a "sky" node
 * to the controlled spatial, which must also be a node. For best results, place
 * the scene's main light, ambient light, and shadow filters/renderers under
 * simulation control by adding them to the Updater.
 * <p>
 * The "top" dome is oriented so that its rim coincides with the horizon. The
 * top dome implements the sun, moon, clear sky color, and horizon haze. Object
 * index 0 is used for the sun, and the remaining object indices are used for
 * phases of the moon.
 * <p>
 * This control simulates two layers of clouds. The cloud density may be
 * adjusted by invoking setCloudiness(). The rate of cloud motion may be
 * adjusted by invoking setCloudsSpeed(). Flatten the clouds for best results;
 * this puts them on a translucent "clouds only" dome.
 * <p>
 * To simulate star motion, several more domes are added: one for northern
 * stars, one for southern stars, and an optional "bottom" dome which extends
 * the horizon haze for scenes with a low horizon.
 * <p>
 * This control is not serializable.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * base color of the daytime sky: pale blue
     */
    final private static ColorRGBA colorDay =
            new ColorRGBA(0.4f, 0.6f, 1f, Constants.alphaMax);
    /**
     * light color and intensity for full moonlight: bluish gray
     */
    final private static ColorRGBA moonLight =
            new ColorRGBA(0.4f, 0.4f, 0.6f, Constants.alphaMax);
    /**
     * light color and intensity for moonless night: nearly black
     */
    final private static ColorRGBA starLight =
            new ColorRGBA(0.03f, 0.03f, 0.03f, Constants.alphaMax);
    /**
     * light color and intensity for full sunlight: yellowish white
     */
    final private static ColorRGBA sunLight =
            new ColorRGBA(0.8f, 0.8f, 0.75f, Constants.alphaMax);
    /**
     * color blended in around sunrise and sunset: ruddy orange
     */
    final private static ColorRGBA twilight =
            new ColorRGBA(0.6f, 0.3f, 0.15f, Constants.alphaMax);
    /**
     * extent of the twilight periods before sunrise and after sunset, expressed
     * as the sine of the sun's angle below the horizon (<=1, >=0)
     */
    final private static float limitOfTwilight = 0.1f;
    /**
     * object index for the moon
     */
    final public static int moonIndex = 1;
    /**
     * maximum number of cloud layers
     */
    final private static int numCloudLayers = 6;
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
     * name for the bottom geometry
     */
    final private static String bottomName = "bottom";
    /**
     * name for the clouds-only geometry
     */
    final private static String cloudsName = "clouds";
    /**
     * name for the northern sky geometry
     */
    final private static String northName = "north";
    /**
     * name for the southern sky geometry
     */
    final private static String southName = "south";
    // *************************************************************************
    // fields
    /**
     * which asset manager to use for loading textures and material definitions:
     * set by constructor
     */
    final private AssetManager assetManager;
    /**
     * true to create a material and geometry for the hemisphere below the
     * horizon, false to leave this hemisphere to background color (if
     * starMotionFlag==false) or stars (if starMotionFlag==true)
     */
    final private boolean bottomDomeFlag;
    /**
     * true if clouds modulate the main light, false for steady light (the
     * default)
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
     * information about individual cloud layers
     */
    final private CloudLayer[] cloudLayers;
    /**
     * mesh used to generate dome geometries
     */
    private DomeMesh mesh = null;
    /**
     * simulation time for cloud layer animations
     */
    private float cloudsAnimationTime = 0f;
    /**
     * rate of motion for cloud layer animations (1->standard)
     */
    private float cloudsRelativeSpeed = 1f;
    /**
     * texture scale for moon images; larger value gives a larger moon
     *
     * The default value (0.02) exaggerates the moon's size by a factor of 8.
     */
    private float moonScale = 0.02f;
    /**
     * phase angle of the moon: default corresponds to a 100% full moon
     */
    private float phaseAngle = FastMath.PI;
    /**
     * texture scale for sun images; larger value would give a larger sun
     *
     * The default value (0.08) exaggerates the sun's size by a factor of 8.
     */
    private float sunScale = 0.08f;
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
     * off-screen renderer for the moon
     */
    private GlobeRenderer moonRenderer = null;
    /**
     * phase of the moon: default is FULL
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
    /**
     * which lights, shadows, and viewports to update
     */
    final private Updater updater = new Updater();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control for no clouds, full moon, no cloud
     * modulation, no lights, no shadows, and no viewports. For a visible sky,
     * the control must be (1) added to a node of the scene graph and (2)
     * enabled.
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
            throw new NullPointerException("asset manager should not be null");
        }
        if (camera == null) {
            throw new NullPointerException("camera should not be null");
        }
        if (!(cloudFlattening >= 0f && cloudFlattening < 1f)) {
            logger.log(Level.SEVERE, "cloudFlattening={0}", cloudFlattening);
            throw new IllegalArgumentException(
                    "flattening should be between 0 and 1");
        }

        this.assetManager = assetManager;
        this.camera = camera;
        this.starMotionFlag = starMotion;
        this.bottomDomeFlag = bottomDome;
        /*
         * Create and initialize the sky material for sun, moon, and haze.
         */
        int topObjects = 2; // a sun and a moon
        boolean cloudDomeFlag = cloudFlattening != 0f;
        int topCloudLayers = cloudDomeFlag ? 0 : numCloudLayers;
        topMaterial = new SkyMaterial(assetManager, topObjects, topCloudLayers);
        topMaterial.initialize();
        topMaterial.addHaze();
        topMaterial.addObject(sunIndex, "Textures/skies/suns/hazy-disc.png");
        if (!starMotionFlag) {
            topMaterial.addStars();
        }

        if (cloudDomeFlag) {
            /*
             * Create and initialize a separate sky material for clouds only.
             */
            int numObjects = 0;
            cloudsMaterial = new SkyMaterial(assetManager, numObjects,
                    numCloudLayers);
            cloudsMaterial.initialize();
            cloudsMaterial.getAdditionalRenderState().setDepthWrite(false);
            cloudsMaterial.setClearColor(ColorRGBA.BlackNoAlpha);
        } else {
            cloudsMaterial = topMaterial;
        }
        /*
         * Initialize the cloud layers.
         */
        cloudLayers = new CloudLayer[numCloudLayers];
        for (int layerIndex = 0; layerIndex < numCloudLayers; layerIndex++) {
            cloudLayers[layerIndex] =
                    new CloudLayer(cloudsMaterial, layerIndex);
        }

        if (bottomDomeFlag) {
            bottomMaterial = Misc.createUnshadedMaterial(assetManager);
        } else {
            bottomMaterial = null;
        }

        createSpatials(cloudFlattening);
        if (starMotionFlag) {
            setStarMaps("Textures/skies/star-maps");
        }

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Clear the star maps.
     */
    public void clearStarMaps() {
        if (!starMotionFlag) {
            topMaterial.removeStars();
            return;
        }
        /*
         * Don't remove the north/south domes because, then how would you add
         * them back into the render queue ahead of the top dome?
         * Instead, make the north/south domes fully transparent.
         */
        Material clear = Misc.createUnshadedMaterial(assetManager);
        clear.setColor("Color", ColorRGBA.BlackNoAlpha);
        RenderState additional = clear.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);
        northDome.setMaterial(clear);
        southDome.setMaterial(clear);
    }

    /**
     * Access a particular cloud layer.
     *
     * @param layerIndex (<numCloudLayers, >=0)
     * @return the pre-existing object
     */
    public CloudLayer getCloudLayer(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= numCloudLayers) {
            logger.log(Level.SEVERE, "index={0}", layerIndex);
            throw new IllegalArgumentException("index out of range");
        }

        CloudLayer layer = cloudLayers[layerIndex];

        assert layer != null;
        return layer;
    }

    /**
     * Access the orientations of the sun and stars.
     *
     * @return the pre-existing object
     */
    public SunAndStars getSunAndStars() {
        assert sunAndStars != null;
        return sunAndStars;
    }

    /**
     * Alter an object's color map texture.
     *
     * @param objectIndex which object (>=0)
     * @param newColorMap texture to apply (not null)
     */
    public void setObjectTexture(int objectIndex, Texture newColorMap) {
        if (objectIndex < 0) {
            logger.log(Level.SEVERE, "objectIndex={0}", objectIndex);
            throw new IllegalArgumentException(
                    "objectIndex should not be negative");
        }
        if (newColorMap == null) {
            throw new NullPointerException("texture should not be null");
        }

        topMaterial.addObject(objectIndex, newColorMap);
    }

    /**
     * Access the updater.
     *
     * @return the pre-existing object
     */
    public Updater getUpdater() {
        assert updater != null;
        return updater;
    }

    /**
     * Alter the opacity of all cloud layers.
     *
     * @param newAlpha desired opacity of the cloud layers (<=1, >=0)
     */
    public void setCloudiness(float newAlpha) {
        if (!(newAlpha >= Constants.alphaMin
                && newAlpha <= Constants.alphaMax)) {
            logger.log(Level.SEVERE, "alpha={0}", newAlpha);
            throw new IllegalArgumentException(
                    "alpha should be between 0 and 1, inclusive");
        }

        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].setOpacity(newAlpha);
        }
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
     * Alter the vertical position of the clouds-only dome. When the scene's
     * horizon lies below the astronomical horizon, it may be helpful to depress
     * the clouds-only dome.
     *
     * @param newYOffset desired vertical offset as a fraction of the dome
     * height (<1, >=0 when flattening>0; 0 when flattening=0)
     */
    public void setCloudYOffset(float newYOffset) {
        if (cloudsOnlyDome == null) {
            if (newYOffset != 0f) {
                logger.log(Level.SEVERE, "offset={0}", newYOffset);
                throw new IllegalArgumentException("offset should be 0");
            }
            return;
        }
        if (!(newYOffset >= 0f && newYOffset < 1f)) {
            logger.log(Level.SEVERE, "offset={0}", newYOffset);
            throw new IllegalArgumentException(
                    "offset should be between 0 and 1");
        }

        float deltaY = -newYOffset * cloudsOnlyDome.getLocalScale().y;
        cloudsOnlyDome.setLocalTranslation(0f, deltaY, 0f);
    }

    /**
     * Alter the angular diameter of the moon.
     *
     * @param newDiameter (in radians, <Pi, >0)
     */
    public void setLunarDiameter(float newDiameter) {
        if (!(newDiameter > 0f && newDiameter < FastMath.PI)) {
            logger.log(Level.SEVERE, "diameter={0}", newDiameter);
            throw new IllegalArgumentException(
                    "diameter should be between 0 and Pi");
        }

        moonScale = newDiameter * mesh.uvScale / FastMath.HALF_PI;
    }

    /**
     * Alter the phase of the moon to a pre-set value.
     *
     * @param newPreset (or null to hide the moon)
     */
    public void setPhase(LunarPhase newPreset) {
        if (newPreset == LunarPhase.CUSTOM) {
            setPhaseAngle(phaseAngle);
            return;
        }

        if (moonRenderer != null) {
            moonRenderer.setEnabled(false);
        }
        phase = newPreset;
        if (newPreset != null) {
            phaseAngle = newPreset.longitudeDifference();
            String assetPath = newPreset.imagePath();
            topMaterial.addObject(moonIndex, assetPath);
        }
    }

    /**
     * Customize the phase angle of the moon for off-screen rendering.
     *
     * @param newAngle (in radians, <=2*Pi, >=0)
     */
    public void setPhaseAngle(float newAngle) {
        if (!(newAngle >= 0f && newAngle <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "angle={0}", newAngle);
            throw new IllegalArgumentException(
                    "angle should be between 0 and 2*Pi");
        }
        if (moonRenderer == null) {
            throw new IllegalStateException("moon renderer not yet added");
        }

        moonRenderer.setEnabled(true);
        phase = LunarPhase.CUSTOM;
        phaseAngle = newAngle;

        Texture dynamicTexture = moonRenderer.getTexture();
        topMaterial.addObject(moonIndex, dynamicTexture);
    }

    /**
     * Alter the angular diameter of the sun.
     *
     * @param newDiameter (in radians, <Pi, >0)
     */
    public void setSolarDiameter(float newDiameter) {
        if (!(newDiameter > 0f && newDiameter < FastMath.PI)) {
            logger.log(Level.SEVERE, "diameter={0}", newDiameter);
            throw new IllegalArgumentException(
                    "diameter should be between 0 and Pi");
        }

        sunScale = newDiameter * mesh.uvScale
                / (Constants.discDiameter * FastMath.HALF_PI);
    }

    /**
     * Alter the star maps.
     *
     * @param assetPath if starMotion is true: path to an asset folder
     * containing "northern.png" and "southern.png" textures (not null)<br>
     * if starMotion is false: path to a star dome texture asset (not null)
     */
    final public void setStarMaps(String assetPath) {
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        if (!starMotionFlag) {
            topMaterial.addStars(assetPath);
            return;
        }

        String northPath = String.format("%s/%sern.png", assetPath, northName);
        Material north = Misc.createUnshadedMaterial(assetManager, northPath);
        northDome.setMaterial(north);

        String southPath = String.format("%s/%sern.png", assetPath, southName);
        Material south = Misc.createUnshadedMaterial(assetManager, southPath);
        southDome.setMaterial(south);
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
                        "cannot enable control before it's added to a node");
            }
            /*
             * Attach the sky node to the controlled node.
             */
            node.attachChild(skyNode);
            /*
             * Scale the sky node so that its furthest geometries are midway
             * between the near and far planes of the view frustum.
             */
            float far = camera.getFrustumFar();
            float near = camera.getFrustumNear();
            float radius = (near + far) / 2f;
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

        updateAll();
    }
    // *************************************************************************
    // private methods

    /**
     * Create and initialize the sky node and all its dome geometries.
     *
     * @param cloudFlattening the oblateness (ellipticity) of the dome with the
     * clouds: 0=no flattening (hemisphere), 1=maximum flattening
     */
    private void createSpatials(float cloudFlattening) {
        /*
         * A mesh which serves as a prototype for the dome geometries.
         */
        mesh = new DomeMesh(rimSamples, quadrantSamples);
        /*
         * A node which serves as the parent for the dome geometries.
         */
        skyNode = new Node("sky node");
        skyNode.setQueueBucket(Bucket.Sky);
        skyNode.setShadowMode(ShadowMode.Off);
        /*
         * Attach geometries to the sky node from the outside in
         * because they'll be rendered in that order.
         */
        if (starMotionFlag) {
            northDome = new Geometry(northName, mesh);
            skyNode.attachChild(northDome);

            southDome = new Geometry(southName, mesh);
            skyNode.attachChild(southDome);
        }

        Geometry topDome = new Geometry("top", mesh);
        skyNode.attachChild(topDome);
        topDome.setMaterial(topMaterial);

        if (bottomDomeFlag) {
            DomeMesh bottomMesh = new DomeMesh(3, 2);
            Geometry bottomDome = new Geometry(bottomName, bottomMesh);
            skyNode.attachChild(bottomDome);

            Quaternion upsideDown = new Quaternion();
            upsideDown.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y.negate());
            bottomDome.setLocalRotation(upsideDown);
            bottomDome.setMaterial(bottomMaterial);
        }

        if (cloudsMaterial != topMaterial) {
            assert cloudFlattening > 0f : cloudFlattening;
            assert cloudFlattening < 1f : cloudFlattening;

            cloudsOnlyDome = new Geometry(cloudsName, mesh);
            skyNode.attachChild(cloudsOnlyDome);
            /*
             * Flatten the clouds-only dome in order to foreshorten clouds
             * near the horizon -- even if cloudYOffset=0.
             */
            float yScale = 1f - cloudFlattening;
            cloudsOnlyDome.setLocalScale(1f, yScale, 1f);
            cloudsOnlyDome.setMaterial(cloudsMaterial);
        }
    }

    /**
     * Compute where mainDirection intersects the cloud dome in the dome's local
     * coordinates, accounting for the dome's flattening and vertical offset.
     *
     * @param mainDirection (unit vector with non-negative y-component)
     * @return a new unit vector
     */
    private Vector3f intersectCloudDome(Vector3f mainDirection) {
        assert mainDirection != null;
        assert mainDirection.isUnitVector() : mainDirection;
        assert mainDirection.y >= 0f : mainDirection;

        double cosSquared = MyMath.sumOfSquares(mainDirection.x,
                mainDirection.z);
        if (cosSquared == 0.0) {
            /*
             * Special case when the main light is directly overhead.
             */
            return Vector3f.UNIT_Y.clone();
        }

        float deltaY;
        float semiMinorAxis;
        if (cloudsOnlyDome == null) {
            deltaY = 0f;
            semiMinorAxis = 1f;
        } else {
            Vector3f offset = cloudsOnlyDome.getLocalTranslation();
            assert offset.x == 0f : offset;
            assert offset.y <= 0f : offset;
            assert offset.z == 0f : offset;
            deltaY = offset.y;

            Vector3f scale = cloudsOnlyDome.getLocalScale();
            assert scale.x == 1f : scale;
            assert scale.y > 0f : scale;
            assert scale.z == 1f : scale;
            semiMinorAxis = scale.y;
        }
        /*
         * Solve for the most positive root of a quadratic equation
         * in w = sqrt(x^2 + z^2).  Use double precision arithmetic.
         */
        double cosAltitude = Math.sqrt(cosSquared);
        double tanAltitude = mainDirection.y / cosAltitude;
        double smaSquared = semiMinorAxis * semiMinorAxis;
        double a = tanAltitude * tanAltitude + smaSquared;
        assert a > 0.0 : a;
        double b = -2.0 * deltaY * tanAltitude;
        double c = deltaY * deltaY - smaSquared;
        double discriminant = MyMath.discriminant(a, b, c);
        assert discriminant >= 0.0 : discriminant;
        double w = (-b + Math.sqrt(discriminant)) / (2.0 * a);

        double distance = w / cosAltitude;
        if (distance > 1.0) {
            /*
             * Squash rounding errors.
             */
            distance = 1.0;
        }
        float x = (float) (mainDirection.x * distance);
        float y = (float) MyMath.circle(w);
        float z = (float) (mainDirection.z * distance);
        Vector3f result = new Vector3f(x, y, z);

        assert result.isUnitVector() : result;
        return result;
    }

    /**
     * Compute the clockwise (left-handed) rotation of the moon's texture
     * relative to the sky's texture.
     *
     * @param longitude the moon's celestial longitude (in radians)
     * @param uvCenter texture coordinates of the moon's center (not null)
     * @return a new unit vector with its x-component equal to the cosine of the
     * rotation angle and its y-component equal to the sine of the rotation
     * angle
     */
    private Vector2f lunarRotation(float longitude, Vector2f uvCenter) {
        assert uvCenter != null;
        /*
         * Compute UV coordinates for 0.01 radians north of the center
         * of the moon.
         */
        Vector3f north = sunAndStars.convertToWorld(1f, longitude);
        Vector2f uvNorth = mesh.directionUV(north);
        if (uvNorth != null) {
            Vector2f offset = uvNorth.subtract(uvCenter);
            assert offset.length() > 0f : offset;
            Vector2f result = offset.normalize();
            return result;
        }
        /*
         * Compute UV coordinates for 0.01 radians south of the center
         * of the moon.
         */
        Vector3f south = sunAndStars.convertToWorld(-1f, longitude);
        Vector2f uvSouth = mesh.directionUV(south);
        if (uvSouth != null) {
            Vector2f offset = uvCenter.subtract(uvSouth);
            assert offset.length() > 0f : offset;
            Vector2f result = offset.normalize();
            return result;
        }
        assert false : south;
        return null;
    }

    /**
     * Specify a globe renderer for the moon.
     *
     * @param newRenderer (not null)
     */
    public void setMoonRenderer(GlobeRenderer newRenderer) {
        if (newRenderer == null) {
            throw new NullPointerException("renderer should not be null");
        }

        if (moonRenderer != null) {
            boolean enabledFlag = moonRenderer.isEnabled();
            newRenderer.setEnabled(enabledFlag);
        }
        moonRenderer = newRenderer;

        if (moonRenderer.isEnabled()) {
            Texture dynamicTexture = moonRenderer.getTexture();
            topMaterial.addObject(moonIndex, dynamicTexture);
        }
    }

    /**
     * Update astronomical objects, sky color, lighting, and stars.
     */
    private void updateAll() {
        Vector3f sunDirection = updateSun();
        /*
         * Daytime sky texture is phased in during the twilight periods
         * before sunrise and after sunset. Update the sky material's
         * clear color accordingly.
         */
        ColorRGBA clearColor = colorDay.clone();
        clearColor.a =
                MyMath.clampFraction(1f + sunDirection.y / limitOfTwilight);
        topMaterial.setClearColor(clearColor);

        Vector3f moonDirection = updateMoon();
        updateLighting(sunDirection, moonDirection);
        if (starMotionFlag) {
            sunAndStars.orientStarDomes(northDome, southDome);
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
            cloudLayers[layer].updateOffset(cloudsAnimationTime);
        }
    }

    /**
     * Update background colors, cloud colors, haze color, sun color, lights,
     * and shadows.
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
         * Modulate the sun's color based on its altitude.
         */
        float sineSolarAltitude = sunDirection.y;
        float green = MyMath.clampFraction(3f * sineSolarAltitude);
        float blue = MyMath.clampFraction(sineSolarAltitude - 0.1f);
        ColorRGBA sunColor = new ColorRGBA(1f, green, blue, Constants.alphaMax);
        topMaterial.setObjectColor(sunIndex, sunColor);
        topMaterial.setObjectGlow(sunIndex, sunColor);
        /*
         * Determine the world direction to the main light source.
         */
        boolean moonUp = moonDirection != null && moonDirection.y >= 0f;
        boolean sunUp = sineSolarAltitude >= 0f;
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
         *  + moonlight when ssa <= -0.04 and sla >= 0, and
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
        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].setColor(cloudsColor);
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
            Vector3f intersection = intersectCloudDome(mainDirection);
            /*
             * Compute the texture coordinates of the cloud dome
             * at the point of intersection.
             */
            Vector2f texCoord = mesh.directionUV(intersection);

            float cloudsTransmission = cloudsMaterial.getTransmission(texCoord);
            mainFactor *= cloudsTransmission;
        }
        main.multLocal(mainFactor);
        /*
         * The ambient light color is based on the cloud color; its intensity is
         * modulated according to the "slack" left by strongest component
         * of the main light.
         */
        float slack = 1f - MyMath.max(main.r, main.g, main.b);
        assert slack >= 0f : slack;
        ColorRGBA ambient = cloudsColor.mult(slack);
        /*
         * Compute the recommended shadow intensity as the fraction of
         * the total light which is directional.
         */
        float mainAmount = main.r + main.g + main.b;
        float ambientAmount = ambient.r + ambient.g + ambient.b;
        float totalAmount = mainAmount + ambientAmount;
        assert totalAmount > 0f : totalAmount;
        float shadowIntensity = MyMath.clampFraction(mainAmount / totalAmount);
        /*
         * Compute the recommended bloom intensity.
         */
        float bloomIntensity = 6f * sineSolarAltitude;
        bloomIntensity = FastMath.clamp(bloomIntensity, 0f, 1.7f);

        updater.update(ambient, baseColor, main, bloomIntensity,
                shadowIntensity, mainDirection);
    }

    /**
     * Update the moon's position and size.
     *
     * @return world direction to the moon (new unit vector) or null if the moon
     * is hidden
     */
    private Vector3f updateMoon() {
        if (phase == null) {
            topMaterial.hideObject(moonIndex);
            return null;
        }
        if (phase == LunarPhase.CUSTOM) {
            assert moonRenderer != null;
            float intensity = 2f + FastMath.abs(phaseAngle - FastMath.PI);
            moonRenderer.setLightIntensity(intensity);
            moonRenderer.setPhase(phaseAngle);
        }
        /*
         * Compute the UV coordinates of the center of the moon.
         */
        float solarLongitude = sunAndStars.getSolarLongitude();
        float celestialLongitude = solarLongitude + phaseAngle;
        celestialLongitude = MyMath.modulo(celestialLongitude, FastMath.TWO_PI);
        Vector3f worldDirection =
                sunAndStars.convertToWorld(0f, celestialLongitude);
        Vector2f uvCenter = mesh.directionUV(worldDirection);

        if (uvCenter != null) {
            Vector2f rotation = lunarRotation(celestialLongitude, uvCenter);
            /*
             * Reveal the object and update its texture transform.
             */
            topMaterial.setObjectTransform(moonIndex, uvCenter, moonScale,
                    rotation);
        } else {
            topMaterial.hideObject(moonIndex);
        }

        return worldDirection;
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
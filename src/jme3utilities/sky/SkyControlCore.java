/*
 Copyright (c) 2014-2017, Stephen Gold
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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;
import jme3utilities.math.MyColor;

/**
 * Core fields and methods of a subtree control to simulate a dynamic sky.
 * <p>
 * The control is disabled at creation. When enabled, it attaches a "sky" node
 * to the controlled spatial, which must be a scene-graph node.
 * <p>
 * The "top" dome is oriented so that its rim is parallel to the horizon. The
 * top dome implements the sun, moon, clear sky color, and horizon haze.
 * <p>
 * This control simulates up to six layers of clouds. The cloud density may be
 * adjusted by invoking setCloudiness(). The rate of cloud motion may be
 * adjusted by invoking setCloudsRate(). Flatten the clouds for best results;
 * this puts them on a translucent "clouds only" dome.
 * <p>
 * To simulate star motion, additional geometries are added: a star cube and an
 * optional "bottom" dome, which extends the horizon haze for scenes with a low
 * horizon.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkyControlCore extends SubtreeControl {
    // *************************************************************************
    // constants

    /**
     * maximum number of cloud layers
     */
    final protected static int numCloudLayers = 6;
    /**
     * number of samples in each longitudinal quadrant of a major dome,
     * including both its top and rim (&ge;2)
     */
    final private static int quadrantSamples = 16;
    /**
     * number of samples around the rim of a major dome (&ge;3)
     */
    final private static int rimSamples = 60;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkyControlCore.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion nullRotation = new Quaternion();
    /**
     * name for the bottom geometry
     */
    final private static String bottomName = "bottom";
    /**
     * name for the clouds-only geometry
     */
    final private static String cloudsName = "clouds";
    /**
     * name for the star cube spatial
     */
    final private static String starCubeName = "star cube";
    /**
     * name for the top geometry
     */
    final private static String topName = "top";
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * negative Y-axis
     */
    final private static Vector3f negativeYAxis = new Vector3f(0f, -1f, 0f);
    // *************************************************************************
    // fields

    /**
     * asset manager for loading textures and material definitions: set by
     * constructor
     */
    private AssetManager assetManager;
    /**
     * true to create a material and geometry for the hemisphere below the
     * horizon, false to leave this hemisphere to background color (if
     * starMotionFlag==false) or stars (if starMotionFlag==true): set by
     * constructor
     */
    private boolean bottomDomeFlag;
    /**
     * true to counteract rotation of the controlled node, false to allow
     * rotation
     */
    private boolean stabilizeFlag = false;
    /**
     * true to simulate moving stars, false for fixed stars: set by constructor
     */
    protected boolean starMotionFlag;
    /**
     * which camera to track: set by constructor or
     * {@link #setCamera(com.jme3.renderer.Camera)}
     */
    private Camera camera;
    /**
     * information about individual cloud layers
     */
    protected CloudLayer[] cloudLayers;
    /**
     * simulation time for cloud layer animations (initially 0, may be negative)
     */
    private float cloudsAnimationTime = 0f;
    /**
     * rate of motion for cloud layer animations (default is 1, may be negative)
     */
    private float cloudsRelativeSpeed = 1f;
    /**
     * phase angle of the moon (0 &rarr; new, Pi &rarr; full, full is default)
     */
    protected float phaseAngle = FastMath.PI;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public SkyControlCore() {
        assetManager = null;
        bottomDomeFlag = false;
        starMotionFlag = false;
        camera = null;
        cloudLayers = null;
    }

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
     * clouds (&ge; 0, &lt;1, 0 &rarr; no flattening (hemisphere), 1 &rarr;
     * maximum flattening
     * @param starMotionFlag true to simulate moving stars, false for fixed
     * stars
     * @param bottomDomeFlag true to create a bottom dome, false to leave this
     * region to background color (if starMotionFlag=false) or stars (if
     * starMotionFlag=true)
     */
    public SkyControlCore(AssetManager assetManager, Camera camera,
            float cloudFlattening, boolean starMotionFlag,
            boolean bottomDomeFlag) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(camera, "camera");
        if (!(cloudFlattening >= 0f && cloudFlattening < 1f)) {
            logger.log(Level.SEVERE, "cloudFlattening={0}", cloudFlattening);
            throw new IllegalArgumentException(
                    "flattening should be between 0 and 1");
        }

        this.assetManager = assetManager;
        this.camera = camera;
        this.starMotionFlag = starMotionFlag;
        this.bottomDomeFlag = bottomDomeFlag;
        /*
         * Create and initialize the sky material for sun, moon, and haze.
         */
        int topObjects = 2; // a sun and a moon
        boolean cloudDomeFlag = cloudFlattening != 0f;
        int topCloudLayers = cloudDomeFlag ? 0 : numCloudLayers;
        SkyMaterial topMaterial = new SkyMaterial(assetManager, topObjects,
                topCloudLayers);
        topMaterial.initialize();
        topMaterial.addHaze();
        if (!starMotionFlag) {
            topMaterial.addStars();
        }

        SkyMaterial cloudsMaterial;
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
            cloudLayers[layerIndex] = new CloudLayer(
                    cloudsMaterial, layerIndex);
        }

        Material bottomMaterial;
        if (bottomDomeFlag) {
            bottomMaterial = MyAsset.createUnshadedMaterial(assetManager);
        } else {
            bottomMaterial = null;
        }

        createSpatials(cloudFlattening, topMaterial, bottomMaterial,
                cloudsMaterial);

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Clear the star maps.
     */
    public void clearStarMaps() {
        if (starMotionFlag) {
            Spatial starCube = MySpatial.findChild(subtree, starCubeName);
            if (starCube != null) {
                subtree.detachChild(starCube);
                starCube = null;
            }
        } else {
            SkyMaterial topMaterial = getTopMaterial();
            topMaterial.removeStars();
        }
    }

    /**
     * Access the indexed cloud layer.
     *
     * @param layerIndex (&lt;numCloudLayers, &ge;0)
     * @return pre-existing instance
     */
    public CloudLayer getCloudLayer(int layerIndex) {
        Validate.inRange(layerIndex, "cloud layer index",
                0, numCloudLayers - 1);
        CloudLayer layer = cloudLayers[layerIndex];

        assert layer != null;
        return layer;
    }

    /**
     * Compute the contribution of the moon to the nighttime illumination mix
     * using its phase, assuming it is above the horizon.
     *
     * @return fraction (&le;1, &ge;0) 1 &rarr; full moon, 0 &rarr; no
     * contribution
     */
    public float getMoonIllumination() {
        float fullAngle = FastMath.abs(phaseAngle - FastMath.PI);
        float weight = 1f - FastMath.saturate(fullAngle * 0.6f);

        assert weight >= 0f : weight;
        assert weight <= 1f : weight;
        return weight;
    }

    /**
     * Alter which camera to track.
     *
     * @param camera which camera to track (not null)
     */
    public void setCamera(Camera camera) {
        Validate.nonNull(camera, "camera");
        this.camera = camera;
    }

    /**
     * Alter the opacity of all cloud layers.
     *
     * @param newAlpha desired opacity of the cloud layers (&le;1, &ge;0)
     */
    public void setCloudiness(float newAlpha) {
        Validate.fraction(newAlpha, "alpha");

        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].setOpacity(newAlpha);
        }
    }

    /**
     * Alter the speed and/or direction of cloud motion.
     *
     * @param newRate multiple of the default rate (may be negative)
     */
    public void setCloudRate(float newRate) {
        cloudsRelativeSpeed = newRate;
    }

    /**
     * Alter the vertical offset of the clouds-only dome. When the scene's
     * horizon lies below the astronomical horizon, it may help to depress the
     * clouds-only dome.
     *
     * @param newYOffset desired vertical offset as a fraction of the dome
     * height (&lt;1, &ge;0 when flattening&gt;0; 0 when flattening=0)
     */
    public void setCloudYOffset(float newYOffset) {
        Spatial cloudsOnlyDome = getCloudsOnlyDome();
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
     * Alter an object's color map texture.
     *
     * @param objectIndex which object (&ge;0)
     * @param newColorMap texture to apply (not null)
     */
    public void setObjectTexture(int objectIndex, Texture newColorMap) {
        Validate.nonNegative(objectIndex, "index");
        Validate.nonNull(newColorMap, "texture");

        SkyMaterial topMaterial = getTopMaterial();
        topMaterial.addObject(objectIndex, newColorMap);
    }

    /**
     * Alter the stabilize flag.
     *
     * @param newState true to counteract rotation of the controlled node, false
     * to allow rotation
     */
    public void setStabilizeFlag(boolean newState) {
        stabilizeFlag = newState;
    }

    /**
     * Alter the star map.
     *
     * @param assetName if starMotion is true: name of a cube map folder in
     * Textures/skies/star-maps (not null, not empty)<br>
     * if starMotion is false: path to texture asset (not null, not empty)
     */
    final public void setStarMaps(String assetName) {
        Validate.nonEmpty(assetName, "asset name");

        if (starMotionFlag) {
            Node starCube = getStarCube();
            if (starCube != null) {
                int index = subtree.detachChild(starCube);
                assert index == 0 : index;
            }
            starCube = MyAsset.createStarMapQuads(assetManager, assetName);
            starCube.setName(starCubeName);
            subtree.attachChildAt(starCube, 0);
        } else {
            SkyMaterial topMaterial = getTopMaterial();
            topMaterial.addStars(assetName);
        }
    }

    /**
     * Alter the vertical angle of the top dome, which is Pi/2 by default. If
     * the terrain's horizon lies below the horizontal, increase this angle (to
     * values greater than Pi/2) to avoid clipping the sun and moon when they
     * are near the horizontal.
     *
     * @param newAngle desired angle from the zenith to the rim of the top dome
     * (in radians, &lt;1.785, &gt;0)
     */
    public void setTopVerticalAngle(float newAngle) {
        if (!(newAngle > 0f && newAngle < 1.785f)) {
            logger.log(Level.SEVERE, "angle={0}", newAngle);
            throw new IllegalArgumentException(
                    "angle should be between 0 and 1.785");
        }

        DomeMesh topMesh = getTopMesh();
        topMesh.setVerticalAngle(newAngle);
        if (bottomDomeFlag) {
            DomeMesh bottomMesh = getBottomMesh();
            bottomMesh.setVerticalAngle(FastMath.PI - newAngle);
        }
    }
    // *************************************************************************
    // protected methods

    /**
     * Access the bottom dome geometry.
     *
     * @return the pre-existing geometry (or null if none)
     */
    protected Geometry getBottomDome() {
        Geometry bottomDome = (Geometry) MySpatial.findChild(subtree,
                bottomName);
        return bottomDome;
    }

    /**
     * Access the bottom dome material.
     *
     * @return the pre-existing instance (or null if none)
     */
    protected Material getBottomMaterial() {
        Geometry bottomDome = getBottomDome();
        if (bottomDome == null) {
            return null;
        }
        Material bottomMaterial = bottomDome.getMaterial();

        return bottomMaterial;
    }

    /**
     * Access the bottom dome mesh.
     *
     * @return the pre-existing instance (or null if none)
     */
    protected DomeMesh getBottomMesh() {
        Geometry bottomDome = getBottomDome();
        if (bottomDome == null) {
            return null;
        }
        DomeMesh bottomMesh = (DomeMesh) bottomDome.getMesh();

        return bottomMesh;
    }

    /**
     * Access the clouds-only dome geometry.
     *
     * @return the pre-existing geometry (or null if none)
     */
    protected Geometry getCloudsOnlyDome() {
        Geometry cloudsOnlyDome = (Geometry) MySpatial.findChild(
                subtree, cloudsName);
        return cloudsOnlyDome;
    }

    /**
     * Access the clouds material.
     *
     * @return the pre-existing instance (not null)
     */
    protected SkyMaterial getCloudsMaterial() {
        Geometry cloudsOnlyDome = getCloudsOnlyDome();
        SkyMaterial cloudsMaterial;
        if (cloudsOnlyDome == null) {
            cloudsMaterial = getTopMaterial();
        } else {
            cloudsMaterial = (SkyMaterial) cloudsOnlyDome.getMaterial();
        }

        assert cloudsMaterial != null;
        return cloudsMaterial;
    }

    /**
     * Access the clouds mesh.
     *
     * @return the pre-existing instance (not null)
     */
    protected DomeMesh getCloudsMesh() {
        Geometry cloudsOnlyDome = getCloudsOnlyDome();
        DomeMesh cloudsMesh;
        if (cloudsOnlyDome == null) {
            cloudsMesh = getTopMesh();
        } else {
            cloudsMesh = (DomeMesh) cloudsOnlyDome.getMesh();
        }

        assert cloudsMesh != null;
        return cloudsMesh;
    }

    /**
     * Access the star cube node.
     *
     * @return the pre-existing node (or null if none)
     */
    protected Node getStarCube() {
        Node starCube = (Node) MySpatial.findChild(subtree, starCubeName);
        return starCube;
    }

    /**
     * Access the top dome geometry.
     *
     * @return the pre-existing geometry (not null)
     */
    protected Geometry getTopDome() {
        Geometry topDome = (Geometry) MySpatial.findChild(subtree, topName);
        assert topDome != null;
        return topDome;
    }

    /**
     * Access the top dome material.
     *
     * @return the pre-existing instance (not null)
     */
    protected SkyMaterial getTopMaterial() {
        Geometry topDome = getTopDome();
        SkyMaterial topMaterial = (SkyMaterial) topDome.getMaterial();

        assert topMaterial != null;
        return topMaterial;
    }

    /**
     * Access the top dome mesh.
     *
     * @return the pre-existing instance (not null)
     */
    protected DomeMesh getTopMesh() {
        Geometry topDome = getTopDome();
        DomeMesh topMesh = (DomeMesh) topDome.getMesh();

        assert topMesh != null;
        return topMesh;
    }

    /**
     * Apply a modified version of the base color to each cloud layer.
     * <p>
     * The return value is used in calculating ambient light intensity.
     *
     * @param baseColor (not null, unaffected, alpha is ignored)
     * @param sunUp true if sun is above the horizon, otherwise false
     * @param moonUp true if moon is above the horizon, otherwise false
     * @return new instance (alpha is undefined)
     */
    protected ColorRGBA updateCloudsColor(ColorRGBA baseColor, boolean sunUp,
            boolean moonUp) {
        assert baseColor != null;

        ColorRGBA cloudsColor = MyColor.saturate(baseColor);
        if (!sunUp) {
            /*
             * At night, darken the clouds by 15%-75%.
             */
            float cloudBrightness = 0.25f;
            if (moonUp) {
                cloudBrightness += 0.6f * getMoonIllumination();
            }
            cloudsColor.multLocal(cloudBrightness);
        }
        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].setColor(cloudsColor);
        }

        return cloudsColor;
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Callback invoked when the sky node's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param elapsedTime time interval between updates (in seconds, &ge;0)
     */
    @Override
    public void controlUpdate(float elapsedTime) {
        super.controlUpdate(elapsedTime);

        updateClouds(elapsedTime);
        /*
         * Translate the sky node to center the sky on the camera.
         */
        Vector3f cameraLocation = camera.getLocation();
        MySpatial.setWorldLocation(subtree, cameraLocation);
        /*
         * Scale the sky node so that its furthest geometries are midway
         * between the near and far planes of the view frustum.
         */
        float far = camera.getFrustumFar();
        float near = camera.getFrustumNear();
        float radius = (near + far) / 2f;
        MySpatial.setWorldScale(subtree, radius);

        if (stabilizeFlag) {
            /*
             * Counteract rotation of the controlled node.
             */
            MySpatial.setWorldOrientation(subtree, nullRotation);
        }
    }

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule ic = importer.getCapsule(this);

        assetManager = importer.getAssetManager();
        stabilizeFlag = ic.readBoolean("stabilizeFlag", false);
        starMotionFlag = ic.readBoolean("starMotionFlag", false);
        /* camera not serialized */
        Savable[] sav = ic.readSavableArray("cloudLayers", null);
        cloudLayers = new CloudLayer[sav.length];
        System.arraycopy(sav, 0, cloudLayers, 0, sav.length);

        cloudsAnimationTime = ic.readFloat("cloudsAnimationTime", 0f);
        cloudsRelativeSpeed = ic.readFloat("cloudsRelativeSpeed", 1f);
        phaseAngle = ic.readFloat("phaseAngle", FastMath.PI);
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule oc = exporter.getCapsule(this);

        oc.write(stabilizeFlag, "stabilizeFlag", false);
        oc.write(starMotionFlag, "starMotionFlag", false);
        /* camera not serialized */
        oc.write(cloudLayers, "cloudLayers", null);
        oc.write(cloudsAnimationTime, "cloudsAnimationTime", 0f);
        oc.write(cloudsRelativeSpeed, "cloudsRelativeSpeed", 1f);
        oc.write(phaseAngle, "phaseAngle", FastMath.PI);
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SkyControlCore clone() throws CloneNotSupportedException {
        SkyControlCore clone = (SkyControlCore) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Create and initialize the sky node and all its dome geometries.
     *
     * @param cloudFlattening the oblateness (ellipticity) of the dome with the
     * clouds (&ge; 0, &lt;1, 0 &rarr; no flattening (hemisphere), 1 &rarr;
     * maximum flattening
     * @param topMaterial (not null)
     * @param bottomMaterial (may be null)
     * @param cloudsMaterial (not null)
     */
    private void createSpatials(float cloudFlattening, Material topMaterial,
            Material bottomMaterial, Material cloudsMaterial) {
        /*
         * Create a node to parent the dome geometries.
         */
        subtree = new Node("sky node");
        subtree.setQueueBucket(Bucket.Sky);
        subtree.setShadowMode(ShadowMode.Off);
        /*
         * Attach geometries to the sky node from the outside in
         * because they'll be rendered in that order.
         */
        if (starMotionFlag) {
            setStarMaps("equator");
        }

        DomeMesh topMesh = new DomeMesh(rimSamples, quadrantSamples);
        Geometry topDome = new Geometry(topName, topMesh);
        subtree.attachChild(topDome);
        topDome.setMaterial(topMaterial);

        if (bottomDomeFlag) {
            DomeMesh bottomMesh = new DomeMesh(rimSamples, 2);
            Geometry bottomDome = new Geometry(bottomName, bottomMesh);
            subtree.attachChild(bottomDome);

            Quaternion upsideDown = new Quaternion();
            upsideDown.lookAt(xAxis, negativeYAxis);
            bottomDome.setLocalRotation(upsideDown);
            bottomDome.setMaterial(bottomMaterial);
        }

        if (cloudsMaterial != topMaterial) {
            assert cloudFlattening > 0f : cloudFlattening;
            assert cloudFlattening < 1f : cloudFlattening;

            DomeMesh cloudsMesh = new DomeMesh(rimSamples, quadrantSamples);
            Geometry cloudsOnlyDome = new Geometry(cloudsName, cloudsMesh);
            subtree.attachChild(cloudsOnlyDome);
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
     * Update the cloud layers. (Invoked once per frame.)
     *
     * @param elapsedTime since the previous update (in seconds, &ge;0)
     */
    private void updateClouds(float elapsedTime) {
        assert elapsedTime >= 0f : elapsedTime;

        cloudsAnimationTime += elapsedTime * cloudsRelativeSpeed;
        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].updateOffset(cloudsAnimationTime);
        }
    }
}

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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySpatial;
import jme3utilities.SimpleControl;
import jme3utilities.math.MyMath;

/**
 * The core fields and methods of a simple control to simulate a dynamic sky.
 * <p>
 * The control is disabled at creation. When enabled, it attaches a "sky" node
 * to the controlled spatial, which must also be a node.
 * <p>
 * The "top" dome is oriented so that its rim coincides with the horizon. The
 * top dome implements the sun, moon, clear sky color, and horizon haze.
 * <p>
 * This control simulates up to six layers of clouds. The cloud density may be
 * adjusted by invoking setCloudiness(). The rate of cloud motion may be
 * adjusted by invoking setCloudsRate(). Flatten the clouds for best results;
 * this puts them on a translucent "clouds only" dome.
 * <p>
 * To simulate star motion, several more domes are added: one for northern
 * stars, one for southern stars, and an optional "bottom" dome which extends
 * the horizon haze for scenes with a low horizon.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyControlCore
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * maximum number of cloud layers
     */
    final private static int numCloudLayers = 6;
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
    final private static Logger logger =
            Logger.getLogger(SkyControlCore.class.getName());
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
     * starMotionFlag==false) or stars (if starMotionFlag==true): set by
     * constructor
     */
    final private boolean bottomDomeFlag;
    /**
     * true to simulate moving stars, false for fixed stars: set by constructor
     */
    final protected boolean starMotionFlag;
    /**
     * the application's camera: set by constructor
     */
    final private Camera camera;
    /**
     * information about individual cloud layers
     */
    final private CloudLayer[] cloudLayers;
    /**
     * mesh of the bottom dome, or null if there's no bottom dome
     */
    protected DomeMesh bottomMesh = null;
    /**
     * mesh of the dome with clouds
     */
    protected DomeMesh cloudsMesh = null;
    /**
     * mesh of the dome with sun, moon, and horizon haze
     */
    protected DomeMesh topMesh = null;
    /**
     * simulation time for cloud layer animations
     */
    private float cloudsAnimationTime = 0f;
    /**
     * rate of motion for cloud layer animations (1 &rarr; standard)
     */
    private float cloudsRelativeSpeed = 1f;
    /**
     * flattened dome for clouds only: set by initialize()
     */
    protected Geometry cloudsOnlyDome = null;
    /**
     * bottom dome: set by initialize()
     */
    protected Geometry bottomDome = null;
    /**
     * dome representing the northern stars: set by initialize()
     */
    protected Geometry northDome = null;
    /**
     * dome representing the southern stars: set by initialize()
     */
    protected Geometry southDome = null;
    /**
     * dome representing the sun, moon, and horizon haze: set by initialize()
     */
    protected Geometry topDome = null;
    /**
     * material for bottom dome: set by constructor
     */
    final protected Material bottomMaterial;
    /**
     * parent node for attaching the geometries: set by initialize()
     */
    private Node skyNode = null;
    /**
     * material of the dome with clouds: set by constructor
     */
    final protected SkyMaterial cloudsMaterial;
    /**
     * material of the top dome: set by constructor
     */
    final protected SkyMaterial topMaterial;
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
     * @param starMotionFlag true to simulate moving stars, false for fixed
     * stars
     * @param bottomDomeFlag true to create a bottom dome, false to leave this
     * region to background color (if starMotionFlag=false) or stars (if
     * starMotionFlag=true)
     */
    public SkyControlCore(AssetManager assetManager, Camera camera,
            float cloudFlattening, boolean starMotionFlag, boolean bottomDomeFlag) {
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
        this.starMotionFlag = starMotionFlag;
        this.bottomDomeFlag = bottomDomeFlag;
        /*
         * Create and initialize the sky material for sun, moon, and haze.
         */
        int topObjects = 2; // a sun and a moon
        boolean cloudDomeFlag = cloudFlattening != 0f;
        int topCloudLayers = cloudDomeFlag ? 0 : numCloudLayers;
        topMaterial = new SkyMaterial(assetManager, topObjects, topCloudLayers);
        topMaterial.initialize();
        topMaterial.addHaze();
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
     * @param layerIndex (&lt;numCloudLayers, &ge;0)
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
     * Alter the opacity of all cloud layers.
     *
     * @param newAlpha desired opacity of the cloud layers (&le;1, &ge;0)
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
     * Alter the speed or direction of cloud motion.
     *
     * @param newRate rate relative to the standard (may be negative)
     */
    public void setCloudRate(float newRate) {
        cloudsRelativeSpeed = newRate;
    }

    /**
     * Alter the vertical position of the clouds-only dome. When the scene's
     * horizon lies below the astronomical horizon, it may help to depress the
     * clouds-only dome.
     *
     * @param newYOffset desired vertical offset as a fraction of the dome
     * height (&lt;1, &ge;0 when flattening&gt;0; 0 when flattening=0)
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
     * Alter an object's color map texture.
     *
     * @param objectIndex which object (&ge;0)
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

    /**
     * Alter the vertical angle of the top dome. When the scene's horizon lies
     * below the astronomical horizon, it may help to increase this angle.
     *
     * @param newAngle desired angle from the zenith to the rim of the top dome
     * (in radians, &lt;Pi, &gt;0)
     */
    public void setTopVerticalAngle(float newAngle) {
        if (!(newAngle > 0f && newAngle < FastMath.PI)) {
            logger.log(Level.SEVERE, "angle={0}", newAngle);
            throw new IllegalArgumentException(
                    "angle should be between 0 and Pi");
        }

        topMesh.setVerticalAngle(newAngle);
        topDome.setMesh(topMesh);
        if (bottomDomeFlag) {
            bottomMesh.setVerticalAngle(FastMath.PI - newAngle);
            bottomDome.setMesh(bottomMesh);
        }
    }
    // *************************************************************************
    // protected methods

    /**
     * Apply a saturated version of the base color to each cloud layer.
     *
     * @param baseColor (not null, unaffected, alpha is ignored)
     * @param sunUp true if sun is above the horizon, otherwise false
     * @param moonUp true if moon is above the horizon, otherwise false
     * @return a new instance (alpha is undefined)
     */
    protected ColorRGBA updateCloudsColor(ColorRGBA baseColor, boolean sunUp,
            boolean moonUp) {
        assert baseColor != null;

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

        return cloudsColor;
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
     * @param tpf seconds since the previous update (&ge;0)
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
            DomeMesh hemisphere = new DomeMesh(rimSamples, quadrantSamples);
            northDome = new Geometry(northName, hemisphere);
            skyNode.attachChild(northDome);

            southDome = new Geometry(southName, hemisphere);
            skyNode.attachChild(southDome);
        }

        topMesh = new DomeMesh(rimSamples, quadrantSamples);
        topDome = new Geometry("top", topMesh);
        skyNode.attachChild(topDome);
        topDome.setMaterial(topMaterial);

        if (bottomDomeFlag) {
            bottomMesh = new DomeMesh(rimSamples, 2);
            bottomDome = new Geometry(bottomName, bottomMesh);
            skyNode.attachChild(bottomDome);

            Quaternion upsideDown = new Quaternion();
            upsideDown.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y.negate());
            bottomDome.setLocalRotation(upsideDown);
            bottomDome.setMaterial(bottomMaterial);
        }

        if (cloudsMaterial != topMaterial) {
            assert cloudFlattening > 0f : cloudFlattening;
            assert cloudFlattening < 1f : cloudFlattening;

            cloudsMesh = new DomeMesh(rimSamples, quadrantSamples);
            cloudsOnlyDome = new Geometry(cloudsName, cloudsMesh);
            skyNode.attachChild(cloudsOnlyDome);
            /*
             * Flatten the clouds-only dome in order to foreshorten clouds
             * near the horizon -- even if cloudYOffset=0.
             */
            float yScale = 1f - cloudFlattening;
            cloudsOnlyDome.setLocalScale(1f, yScale, 1f);
            cloudsOnlyDome.setMaterial(cloudsMaterial);
        } else {
            cloudsMesh = topMesh;
        }
    }

    /**
     * Update the cloud layers. (Invoked once per frame.)
     *
     * @param tpf seconds since the previous update (&ge;0)
     */
    private void updateClouds(float tpf) {
        assert tpf >= 0f : tpf;

        cloudsAnimationTime += tpf * cloudsRelativeSpeed;
        for (int layer = 0; layer < numCloudLayers; layer++) {
            cloudLayers[layer].updateOffset(cloudsAnimationTime);
        }
    }
}
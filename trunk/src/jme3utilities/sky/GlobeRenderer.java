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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.GammaCorrectionFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;

/**
 * An app state which generates a dynamic texture for an object by rendering an
 * off-screen globe.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class GlobeRenderer
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * aspect ratio of the output texture
     */
    final private static float aspectRatio = 1f;
    /**
     * distance from camera to globe (in meters)
     */
    final private static float distance = 4e8f;
    /**
     * radius of the globe (in meters)
     */
    final private static float radius = 1.738e6f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(GlobeRenderer.class.getName());
    // *************************************************************************
    // fields
    /**
     * camera for off-screen render: set by constructor
     */
    final private Camera camera;
    /**
     * light source for the scene: set by constructor
     */
    final private DirectionalLight light;
    /**
     * gamma value to use in initialize()
     */
    private float initialGamma = 2f;
    /**
     * frame buffer for off-screen render: set by constructor
     */
    final private FrameBuffer frameBuffer;
    /**
     * filter to adjust the contrast: set by initialize()
     */
    private GammaCorrectionFilter filter = null;
    /**
     * root of the the preview scene: set by constructor
     */
    final private Node rootNode;
    /**
     * dynamic output texture: set by constructor
     */
    final private Texture2D outputTexture;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled renderer with the specified resolution and globe
     * material.
     *
     * @param resolution (in pixels, >0)
     * @param globeMaterial (not null)
     */
    public GlobeRenderer(int resolution, Material globeMaterial) {
        if (resolution <= 0) {
            logger.log(Level.SEVERE, "resolution={0}", resolution);
            throw new IllegalArgumentException(
                    "resolution should be positive");
        }
        if (globeMaterial == null) {
            throw new NullPointerException("material should not be null");
        }

        rootNode = new Node("off-screen root node");
        /*
         * Add a directional light to the scene.
         */
        light = new DirectionalLight();
        rootNode.addLight(light);
        setLightIntensity(2f);
        setPhase(FastMath.PI);
        /*
         * Add a globe at the origin.
         * North is in the +X direction.
         */
        int zSamples = 24;
        int radialSamples = 10;
        Sphere mesh = new Sphere(zSamples, radialSamples, radius);
        mesh.setTextureMode(TextureMode.Projected);
        Geometry globe = new Geometry("off-screen globe", mesh);
        rootNode.attachChild(globe);
        Quaternion orientation = new Quaternion();
        orientation.fromAngles(0f, FastMath.HALF_PI, 0f);
        globe.setLocalRotation(orientation);
        Vector3f planetCenter = Vector3f.ZERO;
        globe.setLocalTranslation(planetCenter);
        globe.setMaterial(globeMaterial);
        /*
         * Add a camera on the +Z axis.
         */
        camera = new Camera(resolution, resolution);
        camera.setLocation(new Vector3f(0f, 0f, distance));
        camera.lookAt(planetCenter, Vector3f.UNIT_X);
        float fovY = 2f * FastMath.asin(radius / distance);
        float fovYDegrees = fovY * FastMath.RAD_TO_DEG;
        float near = 0.5f * (distance - radius);
        float far = 2f * (distance + radius);
        camera.setFrustumPerspective(fovYDegrees, aspectRatio, near, far);
        /*
         * Create a texture, frame buffer, and viewport for output.
         */
        outputTexture = new Texture2D(resolution, resolution,
                Image.Format.Luminance8Alpha8);
        outputTexture.setMagFilter(Texture.MagFilter.Bilinear);
        outputTexture.setMinFilter(Texture.MinFilter.Trilinear);

        int numSamples = 1;
        frameBuffer = new FrameBuffer(resolution, resolution, numSamples);
        frameBuffer.setColorTexture(outputTexture);

        assert isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the output texture.
     *
     * @return pre-existing instance
     */
    public Texture2D getTexture() {
        assert outputTexture != null;
        return outputTexture;
    }

    /**
     * Alter the contrast of the render.
     *
     * @param newGamma parameter for the filter (>0, 1->linear)
     */
    final public void setGamma(float newGamma) {
        if (!(newGamma > 0f)) {
            logger.log(Level.SEVERE, "gamma={0}", newGamma);
            throw new IllegalArgumentException("gamma should be positive");
        }

        if (isInitialized()) {
            filter.setGamma(newGamma);
        } else {
            assert filter == null : filter;
            initialGamma = newGamma;
        }
    }

    /**
     * Alter the intensity of the (white) light.
     *
     * @param intensity (>=0, 1->standard)
     */
    final public void setLightIntensity(float intensity) {
        if (!(intensity >= 0f)) {
            logger.log(Level.SEVERE, "intensity={0}", intensity);
            throw new IllegalArgumentException(
                    "intensity shouldn't be negative");
        }

        ColorRGBA lightColor = ColorRGBA.White.mult(intensity);
        light.setColor(lightColor);
    }

    /**
     * Alter the lighting phase of the globe.
     *
     * @param phaseAngle (in radians, <=2*Pi, >=0, 0->dark, Pi->fully lit)
     */
    final public void setPhase(float phaseAngle) {
        if (!(phaseAngle >= 0f && phaseAngle <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "phase={0}", phaseAngle);
            throw new IllegalArgumentException(
                    "phase should be between 0 and 2*Pi");
        }

        Quaternion turn = new Quaternion().fromAngles(-phaseAngle, 0f, 0f);
        Vector3f lightDirection = turn.mult(Vector3f.UNIT_Z);
        light.setDirection(lightDirection);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this controller.
     *
     * @param stateManager (not null)
     * @param application which application owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
        if (stateManager == null) {
            throw new NullPointerException("manager shouldn't be null");
        }
        if (application == null) {
            throw new NullPointerException("application shouldn't be null");
        }

        super.initialize(stateManager, application);

        RenderManager renderManager = application.getRenderManager();
        ViewPort viewPort =
                renderManager.createPreView("off-screen render", camera);
        viewPort.attachScene(rootNode);
        viewPort.setClearFlags(true, true, true);
        viewPort.setOutputFrameBuffer(frameBuffer);
        /*
         * Apply a contrast correction filter to the render.
         */
        AssetManager assetManager = application.getAssetManager();
        FilterPostProcessor fpp = Misc.getFpp(viewPort, assetManager);
        filter = new GammaCorrectionFilter(initialGamma);
        fpp.addFilter(filter);
    }

    /**
     * Update the off-screen scene.
     *
     * @param elapsedTime since previous update (>=0, in seconds)
     */
    @Override
    public void update(float elapsedTime) {
        if (!(elapsedTime >= 0f)) {
            logger.log(Level.SEVERE, "elapsedTime={0}", elapsedTime);
            throw new IllegalArgumentException(
                    "elapsed time shouldn't be negative");
        }

        rootNode.updateLogicalState(elapsedTime);
        rootNode.updateGeometricState();
    }
}
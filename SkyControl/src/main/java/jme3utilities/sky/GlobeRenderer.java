/*
 Copyright (c) 2014-2018, Stephen Gold
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
package jme3utilities.sky;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
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
import jme3utilities.ContrastAdjustmentFilter;
import jme3utilities.Misc;
import jme3utilities.MySpatial;
import jme3utilities.SimpleAppState;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * A simple app state to generate a dynamic texture for an object by rendering
 * an off-screen globe. Each instance has its own camera and root node.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GlobeRenderer extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * aspect ratio of the output texture
     */
    final private static float aspectRatio = 1f;
    /**
     * initial distance from camera to center of globe (in world units)
     */
    final private static float initialCameraDistance = 4e8f;
    /**
     * initial radius of the globe (in world units)
     */
    final private static float initialGlobeRadius = 1.738e6f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GlobeRenderer.class.getName());
    /**
     * location of the globe's center (in world coordinates)
     */
    final private static Vector3f globeCenter = new Vector3f(0f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f unitX = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f unitY = new Vector3f(0f, 1f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Z}
     */
    final private static Vector3f unitZ = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // fields

    /**
     * camera for off-screen render (set by constructor)
     */
    private Camera camera;
    /**
     * filter to adjust the contrast: set by initialize()
     */
    private ContrastAdjustmentFilter filter = null;
    /**
     * light source for the scene (set by constructor)
     */
    private DirectionalLight light;
    /**
     * exponent to set in initialize(): afterwards it's ignored
     */
    private float initialExponent = 0.5f;
    /**
     * spin rate (in radians per second, default is 0)
     */
    private float spinRate = 0f;
    /**
     * frame buffer for off-screen render (set by constructor)
     */
    final private FrameBuffer frameBuffer;
    /**
     * geometry for the globe (set by constructor)
     */
    private Geometry globe;
    /**
     * image format for off-screen render (set by constructor)
     */
    final private Image.Format outputFormat;
    /**
     * root of the off-screen scene graph
     */
    final private Node offscreenRootNode = new Node("off-screen root node");
    /**
     * name for the off-screen render of the globe
     */
    private String preViewName = "off-screen render";
    /**
     * dynamic output texture: set by constructor
     */
    final private Texture2D outputTexture;
    /**
     * spin axis (length=1)
     */
    final private Vector3f spinAxis = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new enabled, uninitialized renderer with the specified
     * resolution and globe material.
     *
     * @param globeMaterial suitable for equirectangular projection (not null)
     * @param outputFormat (not null, ABGR8 &rarr; color, Luminance8Alpha8
     * &rarr; grayscale)
     * @param equatorSamples number of samples around the globe's middle (&ge;3)
     * @param meridianSamples number of samples from pole to pole (&ge;3)
     * @param resolution number of pixels per side of the output texture (&gt;0)
     */
    public GlobeRenderer(Material globeMaterial, Image.Format outputFormat,
            int equatorSamples, int meridianSamples, int resolution) {
        super(true);
        Validate.nonNull(globeMaterial, "material");
        Validate.nonNull(outputFormat, "format");
        Validate.inRange(equatorSamples, "equator samples",
                3, Integer.MAX_VALUE);
        Validate.inRange(meridianSamples, "meridian samples",
                3, Integer.MAX_VALUE);
        Validate.positive(resolution, "resolution");

        this.outputFormat = outputFormat;
        initializeCamera(resolution);
        initializeGlobe(globeMaterial, equatorSamples, meridianSamples);
        initializeLights();
        /*
         * Create a texture, frame buffer, and viewport for output.
         */
        outputTexture = new Texture2D(resolution, resolution, outputFormat);
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
     * Compute the distance from the camera to the center of the globe.
     *
     * @return distance in world units (&gt;0)
     */
    public float getCameraDistance() {
        Vector3f cameraLocation = camera.getLocation();
        float result = cameraLocation.distance(globeCenter);

        assert result > 0f : result;
        return result;
    }

    /**
     * Read the radius of the globe.
     *
     * @return radius in world units (&gt;0)
     */
    public float getGlobeRadius() {
        float result = MySpatial.uniformScale(globe);

        assert result > 0f : result;
        return result;
    }

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
     * Move the camera to a new location and orientation.
     *
     * @param newLocation (in world coordinates, not null, unaffected)
     * @param newUpDirection (length&gt;0, unaffected)
     */
    final public void moveCamera(Vector3f newLocation,
            Vector3f newUpDirection) {
        Validate.nonNull(newLocation, "location");
        Validate.nonZero(newUpDirection, "up direction");

        camera.setLocation(newLocation.clone());
        camera.lookAt(globeCenter, newUpDirection);
    }

    /**
     * Alter the contrast of the render.
     *
     * @param newGamma parameter for the filter (&gt;0, 1 &rarr; linear)
     */
    final public void setGamma(float newGamma) {
        Validate.positive(newGamma, "gamma");

        if (isInitialized()) {
            filter.setExponent(newGamma);
        } else {
            assert filter == null : filter;
            initialExponent = newGamma;
        }
    }

    /**
     * Change the size of the globe.
     *
     * @param newRadius (in world units, &gt;0)
     */
    final public void setGlobeRadius(float newRadius) {
        Validate.positive(newRadius, "radius");

        MySpatial.setWorldScale(globe, newRadius);
    }

    /**
     * Alter the intensity of the (directional white) light.
     *
     * @param intensity (&ge;0, 1 &rarr; standard)
     */
    final public void setLightIntensity(float intensity) {
        Validate.nonNegative(intensity, "intensity");

        ColorRGBA lightColor = ColorRGBA.White.mult(intensity);
        light.setColor(lightColor);
    }

    /**
     * Alter the light direction.
     *
     * @param theta 1st polar coordinate (in radians, &le;2*Pi, &ge;0)
     * @param phi 2nd polar coordinate (in radians, &le;Pi/2, &ge;-Pi/2)
     */
    final public void setPhase(float theta, float phi) {
        Validate.inRange(theta, "theta", 0f, FastMath.TWO_PI);
        Validate.inRange(phi, "phi", -FastMath.HALF_PI, FastMath.HALF_PI);

        Quaternion xRot = new Quaternion();
        xRot.fromAngleNormalAxis(-theta, unitX);
        Quaternion yRot = new Quaternion();
        yRot.fromAngleNormalAxis(-phi, unitY);
        Quaternion turn = yRot.mult(xRot);

        Vector3f lightDirection = turn.mult(unitZ);
        light.setDirection(lightDirection);
    }

    /**
     * Alter the spin axis of the globe.
     *
     * @param newAxis direction in the globe's local coordinate system
     * (length&gt;0, unaffected)
     */
    public void setSpinAxis(Vector3f newAxis) {
        Validate.nonZero(newAxis, "axis");
        Vector3f norm = newAxis.normalize();
        spinAxis.set(norm);
    }

    /**
     * Alter the spin rate of the globe.
     *
     * @param newRate (in radians per second)
     */
    public void setSpinRate(float newRate) {
        spinRate = newRate;
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Clean up this renderer after it has been detached. Should be invoked only
     * by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        ViewPort preView = renderManager.getPreView(preViewName);
        boolean success = renderManager.removePreView(preView);
        assert success;

        super.cleanup();
    }

    /**
     * Initialize this controller prior to its 1st update. Should be invoked
     * only by a subclass or by the AppStateManager.
     *
     * @param stateManager (not null)
     * @param application which application owns this renderer (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        ViewPort offscreenViewPort = renderManager.createPreView(
                preViewName, camera);
        offscreenViewPort.attachScene(offscreenRootNode);
        offscreenViewPort.setClearFlags(true, true, true);
        offscreenViewPort.setOutputFrameBuffer(frameBuffer);
        if (!isEnabled()) {
            offscreenRootNode.updateLogicalState(0f);
            offscreenRootNode.updateGeometricState();
        }
        /*
         * Apply a contrast correction filter to the render.
         */
        FilterPostProcessor fpp
                = Misc.getFpp(offscreenViewPort, assetManager, 1);
        fpp.setFrameBufferFormat(outputFormat);
        filter = new ContrastAdjustmentFilter(initialExponent);
        fpp.addFilter(filter);
    }

    /**
     * Update the off-screen scene.
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        /*
         * spin the globe on its axis
         */
        float angle = spinRate * tpf;
        Quaternion spin = new Quaternion().fromAngleNormalAxis(angle, spinAxis);
        globe.rotate(spin);

        updateFrustum();

        offscreenRootNode.updateLogicalState(tpf);
        offscreenRootNode.updateGeometricState();
    }
    // *************************************************************************
    // private methods

    /**
     * Add a camera on the +Z axis.
     */
    private void initializeCamera(int resolution) {
        assert resolution > 0 : resolution;

        camera = new Camera(resolution, resolution);
        Vector3f location = new Vector3f(0f, 0f, initialCameraDistance);
        Vector3f upDirection = unitX;
        moveCamera(location, upDirection);
    }

    /**
     * Add a globe and orient it so that its north pole is in the global +X
     * direction.
     */
    private void initializeGlobe(Material globeMaterial, int equatorSamples,
            int meridianSamples) {
        assert globeMaterial != null;
        assert equatorSamples >= 3 : equatorSamples;
        assert meridianSamples >= 3 : meridianSamples;

        Sphere mesh = new Sphere(meridianSamples, equatorSamples, 1f);
        mesh.setTextureMode(TextureMode.Projected);
        globe = new Geometry("off-screen globe", mesh);
        offscreenRootNode.attachChild(globe);
        Quaternion orientation = new Quaternion();
        orientation.fromAngles(0f, FastMath.HALF_PI, 0f);
        globe.setLocalRotation(orientation);
        globe.setLocalTranslation(globeCenter);
        globe.setMaterial(globeMaterial);
        setGlobeRadius(initialGlobeRadius);
    }

    /**
     * Add a directional light to the scene.
     */
    private void initializeLights() {
        light = new DirectionalLight();
        offscreenRootNode.addLight(light);
        setLightIntensity(2f);
        setPhase(FastMath.PI, 0f); // full phase
    }

    /**
     * Update the camera's frustum so that the rendered globe will fill the
     * frame buffer.
     */
    private void updateFrustum() {
        float cameraDistance = getCameraDistance();
        float globeRadius = getGlobeRadius();

        if (!(cameraDistance > globeRadius)) {
            logger.log(Level.SEVERE, "cameraDistance={0} globeRadius={1}",
                    new Object[]{
                        cameraDistance, globeRadius
                    });
            throw new IllegalArgumentException(
                    "camera should be outside the globe");
        }

        float fovY = 2f * FastMath.asin(globeRadius / cameraDistance);
        float fovYDegrees = MyMath.toDegrees(fovY);
        float near = 0.5f * (cameraDistance - globeRadius);
        float far = 2f * (cameraDistance + globeRadius);
        camera.setFrustumPerspective(fovYDegrees, aspectRatio, near, far);
    }
}

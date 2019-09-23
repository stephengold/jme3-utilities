/*
 Copyright (c) 2014-2019, Stephen Gold
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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to provide a visual "floor" for a scene. The floor would
 * typically be textured to look like an extension of the terrain.
 * <p>
 * The control is disabled at creation. When enabled, it attaches a floor node
 * to the controlled spatial, which must be a scene-graph node.
 * <p>
 * For best results, the control should be enabled <em>after</em> all sky
 * geometries have been attached.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class FloorControl extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(FloorControl.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotationIdentity = new Quaternion();
    /**
     * name for the geometry
     */
    final private static String geometryName = "floor";
    /**
     * name for the node
     */
    final private static String nodeName = "floor node";
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f unitX = new Vector3f(1f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * true to counteract rotation of the controlled node, false to allow
     * rotation
     */
    private boolean stabilizeFlag = false;
    /**
     * which camera to track: set by constructor or
     * {@link #setCamera(com.jme3.renderer.Camera)}
     */
    private Camera camera;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected FloorControl() {
        super();
        camera = null;
    }

    /**
     * Instantiate a disabled control.
     *
     * @param camera the application's camera (not null)
     * @param material to apply to the floor (not null)
     * @param textureScale scale to apply to texture coordinates (&gt;0)
     */
    public FloorControl(Camera camera, Material material, float textureScale) {
        Validate.nonNull(camera, "camera");
        Validate.nonNull(material, "material");
        Validate.positive(textureScale, "scale");

        this.camera = camera;
        createSpatials(material, textureScale);

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

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
     * Alter the stabilize flag.
     *
     * @param newState true to counteract rotation of the controlled node, false
     * to allow rotation
     */
    public void setStabilizeFlag(boolean newState) {
        stabilizeFlag = newState;
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
    public FloorControl clone() throws CloneNotSupportedException {
        FloorControl clone = (FloorControl) super.clone();
        return clone;
    }

    /**
     * Convert this shallow-cloned control into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        camera = cloner.clone(camera);
    }

    /**
     * Callback invoked when the controlled spatial's geometric state is about
     * to be updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    public void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);

        if (camera == null) {
            return;
        }
        /*
         * Translate the floor to center it below the camera.
         */
        Vector3f cameraLocation = camera.getLocation();
        Node subtreeNode = (Node) getSubtree();
        MySpatial.setWorldLocation(subtreeNode, cameraLocation);
        MySpatial.setWorldScale(subtreeNode, 1f);

        Spatial floor = subtreeNode.getChild(0);
        float radius = camera.getFrustumFar();
        floor.setLocalScale(2f * radius);
        float drop = camera.getFrustumNear();
        floor.setLocalTranslation(-radius, -drop, radius);

        if (stabilizeFlag) {
            /*
             * Counteract rotation of the controlled node.
             */
            MySpatial.setWorldOrientation(subtreeNode, rotationIdentity);
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
        stabilizeFlag = ic.readBoolean("stabilizeFlag", false);
        /* camera not serialized */
    }

    /**
     * Callback invoked when the controlled spatial is about to be rendered to a
     * viewport.
     *
     * @param renderManager (not null)
     * @param viewPort viewport where the spatial will be rendered (not null)
     */
    @Override
    public void render(final RenderManager renderManager,
            final ViewPort viewPort) {
        super.render(renderManager, viewPort);
        camera = viewPort.getCamera();
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
        /* camera not serialized */
    }
    // *************************************************************************
    // private methods

    /**
     * Create and initialize the floor node and geometry.
     *
     * @param material material to apply to the floor (not null)
     * @param textureScale scale to apply to texture coordinates (&gt;0)
     */
    private void createSpatials(Material material, float textureScale) {
        assert material != null;
        assert textureScale > 0f : textureScale;
        /*
         * Create a Node to parent the floor geometry.
         */
        Node subtreeNode = new Node(nodeName);
        subtreeNode.setQueueBucket(RenderQueue.Bucket.Sky);
        subtreeNode.setShadowMode(RenderQueue.ShadowMode.Off);
        setSubtree(subtreeNode);
        /*
         * Create and attach the floor geometry.
         */
        Quad mesh = new Quad(1f, 1f);
        mesh.scaleTextureCoordinates(new Vector2f(textureScale, textureScale));
        Geometry floor = new Geometry(geometryName, mesh);
        subtreeNode.attachChild(floor);
        floor.setMaterial(material);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(-FastMath.HALF_PI, unitX);
        floor.setLocalRotation(rotation); // rotate from X-Y plane to X-Z plane

        float radius = camera.getFrustumFar();
        floor.setLocalScale(2f * radius);
        float drop = camera.getFrustumNear();
        floor.setLocalTranslation(-radius, -drop, radius);
    }
}

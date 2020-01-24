/*
 Copyright (c) 2017-2020, Stephen Gold
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
package jme3utilities.debug;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A SubtreeControl to visualize the world bounds of a subject spatial.
 * <p>
 * The controlled spatial must be a Node, but the subject may be a Geometry.
 * <p>
 * The Control is disabled by default. When enabled, it attaches a Geometry to
 * the subtree.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoundsVisualizer extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default depth-test setting (disabled)
     */
    final private static boolean defaultDepthTest = false;
    /**
     * default color for lines (blue)
     */
    final private static ColorRGBA defaultLineColor
            = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * default width for lines (in pixels)
     */
    final private static float defaultLineWidth = 2f;
    /**
     * child position of the lines geometry in the subtree node
     */
    final private static int linesChildPosition = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoundsVisualizer.class.getName());
    /**
     * name for the lines geometry
     */
    final private static String linesName = "bound lines";
    /**
     * name for the subtree node
     */
    final private static String subtreeName = "bound node";
    /**
     * field names for serialization
     */
    final private static String tagBillboardAxis = "billboardAxis";
    final private static String tagCamera = "camera";
    final private static String tagLineMaterial = "lineMaterial";
    final private static String tagLineWidth = "lineWidth";
    final private static String tagSphereType = "sphereType";
    final private static String tagSubject = "subject";
    // *************************************************************************
    // fields

    /**
     * camera for billboarding, or null if not billboarding
     */
    private Camera camera = null;
    /**
     * effective line width (in pixels, &ge;0, values &lt;1 hide the lines)
     */
    private float effectiveLineWidth = defaultLineWidth;
    /**
     * local axis for billboarding
     */
    private int billboardAxis = MyVector3f.xAxis;
    /**
     * wireframe material for lines (color and depth-test are stored here)
     */
    private Material lineMaterial;
    /**
     * spatial whose world bounds are being visualized, or null for none
     */
    private Spatial subject = null;
    /**
     * type of visualization for spheres
     */
    private SphereMeshes sphereType = SphereMeshes.WireSphere;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public BoundsVisualizer() {
    }

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading material definitions (not null)
     */
    public BoundsVisualizer(AssetManager assetManager) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        lineMaterial = MyAsset.createWireframeMaterial(assetManager,
                defaultLineColor);
        RenderState rs = lineMaterial.getAdditionalRenderState();
        rs.setDepthTest(defaultDepthTest);
        lineMaterial.setName("bound mat");

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Determine which local axis is being used for billboarding.
     *
     * @return the axis index (&ge;0, &le;2) or -1 if not billboarding
     */
    public int billboardAxis() {
        int result = (camera == null) ? -1 : billboardAxis;
        return result;
    }

    /**
     * Copy the color of the lines.
     *
     * @return a new instance
     * @deprecated use {@link #copyLineColor(com.jme3.math.ColorRGBA)}
     */
    @Deprecated
    public ColorRGBA copyLineColor() {
        MatParam parameter = lineMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Copy the color of the lines.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a new instance
     */
    public ColorRGBA copyLineColor(ColorRGBA storeResult) {
        ColorRGBA result
                = (storeResult == null) ? new ColorRGBA() : storeResult;
        MatParam parameter = lineMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();
        result.set(color);

        return result;
    }

    /**
     * Disable billboarding.
     */
    public void disableBillboarding() {
        camera = null;
    }

    /**
     * Enable billboarding. Note: billboarding is implemented only for spheres,
     * not axis-aligned boxes.
     *
     * @param camera the camera to use for billboarding (not null, alias
     * created)
     * @param axisIndex which local axis should point toward the camera:
     * 0&rarr;+X, 1&rarr;+Y, 2&rarr;+Z
     */
    public void enableBillboarding(Camera camera, int axisIndex) {
        Validate.nonNull(camera, "camera");
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        this.camera = camera;
        billboardAxis = axisIndex;
    }

    /**
     * Access the Camera used for billboarding.
     *
     * @return the pre-existing instance (may be null)
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Access the Spatial whose bounds are being visualized.
     *
     * @return the pre-existing instance (may be null)
     */
    public Spatial getSubject() {
        return subject;
    }

    /**
     * Determine the depth-test setting.
     * <p>
     * The test provides depth cues, but might hide portions of the
     * visualization.
     *
     * @return true if the test is enabled, otherwise false
     */
    public boolean isDepthTest() {
        RenderState rs = lineMaterial.getAdditionalRenderState();
        boolean result = rs.isDepthTest();

        return result;
    }

    /**
     * Determine the effective line width of the visualization.
     *
     * @return width (in pixels, &ge;0)
     */
    public float lineWidth() {
        assert effectiveLineWidth >= 0f : effectiveLineWidth;
        return effectiveLineWidth;
    }

    /**
     * Alter the color of all lines.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        ColorRGBA colorClone = newColor.clone();
        lineMaterial.setColor("Color", colorClone);
    }

    /**
     * Alter the depth test setting. The test provides depth cues, but might
     * hide portions of the visualization.
     *
     * @param newSetting true to enable test, false to disable it
     */
    public void setDepthTest(boolean newSetting) {
        RenderState rs = lineMaterial.getAdditionalRenderState();
        rs.setDepthTest(newSetting);
    }

    /**
     * Alter the effective line width of the visualization.
     *
     * @param newWidth (in pixels, &ge;0, values &lt;1 hide the lines)
     */
    public void setLineWidth(float newWidth) {
        Validate.nonNegative(newWidth, "new width");
        effectiveLineWidth = newWidth;
    }

    /**
     * Alter the type of Mesh used to visualize spheres.
     *
     * @param type (not null)
     */
    public void setSphereType(SphereMeshes type) {
        Validate.nonNull(type, "type");
        sphereType = type;
    }

    /**
     * Alter which spatial's bounds are being visualized.
     *
     * @param newSubject which spatial (may be null, alias created)
     */
    public void setSubject(Spatial newSubject) {
        subject = newSubject;
    }

    /**
     * Read the type of Mesh used to visualize spheres.
     *
     * @return an enum value (not null)
     */
    public SphereMeshes sphereType() {
        assert sphereType != null;
        return sphereType;
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * Create a shallow copy of this Control.
     *
     * @return a new control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public BoundsVisualizer clone() throws CloneNotSupportedException {
        BoundsVisualizer clone = (BoundsVisualizer) super.clone();
        return clone;
    }

    /**
     * Convert this shallow-cloned Control into a deep-cloned one, using the
     * specified Cloner and original to resolve copied fields.
     *
     * @param cloner the Cloner currently cloning this Control
     * @param original the instance from which this Control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        camera = cloner.clone(camera);
        lineMaterial = cloner.clone(lineMaterial);
        subject = cloner.clone(subject);
    }

    /**
     * Callback invoked when the controlled spatial's geometric state is about
     * to be updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);

        Node subtreeNode = (Node) getSubtree();
        if (subject == null || effectiveLineWidth < 1f) {
            subtreeNode.detachAllChildren();

        } else if (subtreeNode.getQuantity() == 0) {
            addLines();

        } else {
            Geometry lines = (Geometry) subtreeNode.getChild(linesChildPosition);
            BoundingVolume bound = subject.getWorldBound();
            Mesh mesh = lines.getMesh();
            if (bound instanceof BoundingBox && mesh instanceof WireBox) {
                updateBox();
            } else if (bound instanceof BoundingSphere
                    && sphereType.isInstance(mesh)) {
                updateSphere();
            } else { // wrong type of mesh - create a new Geometry
                subtreeNode.detachAllChildren();
                addLines();
            }
        }
    }

    /**
     * De-serialize this Control from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        billboardAxis = capsule.readInt(tagBillboardAxis, MyVector3f.xAxis);
        camera = (Camera) capsule.readSavable(tagCamera, null);
        lineMaterial = (Material) capsule.readSavable(tagLineMaterial, null);
        effectiveLineWidth = capsule.readFloat(tagLineWidth, 0f);
        sphereType = capsule.readEnum(tagSphereType, SphereMeshes.class, null);
        subject = (Spatial) capsule.readSavable(tagSubject, null);
    }

    /**
     * Alter the visibility of the visualization.
     *
     * @param newState if true, reveal the visualization; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        if (newState && getSubtree() == null) {
            /*
             * Before enabling this Control for the first time,
             * create the subtree.
             */
            Node subtreeNode = new Node(subtreeName);
            subtreeNode.setQueueBucket(RenderQueue.Bucket.Transparent);
            subtreeNode.setShadowMode(RenderQueue.ShadowMode.Off);
            setSubtree(subtreeNode);
        }

        super.setEnabled(newState);
    }

    /**
     * Serialize this Control to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(billboardAxis, tagBillboardAxis, MyVector3f.xAxis);
        capsule.write(camera, tagCamera, null);
        capsule.write(lineMaterial, tagLineMaterial, null);
        capsule.write(effectiveLineWidth, tagLineWidth, 0f);
        capsule.write(sphereType, tagSphereType, null);
        capsule.write(subject, tagSubject, null);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a lines geometry and attach it to the empty subtree.
     */
    private void addLines() {
        assert ((Node) getSubtree()).getQuantity() == 0;

        Mesh mesh;
        BoundingVolume bound = subject.getWorldBound();
        if (bound instanceof BoundingBox) {
            mesh = new WireBox();
        } else if (bound instanceof BoundingSphere) {
            float radius = 1f;
            boolean wantNormals = false;
            boolean wantUVs = false;
            mesh = sphereType.makeSphere(radius, wantNormals, wantUVs);
        } else {
            throw new IllegalStateException();
        }

        Geometry lines = new Geometry(linesName, mesh);
        lines.setMaterial(lineMaterial);
        ((Node) getSubtree()).attachChildAt(lines, linesChildPosition);

        if (bound instanceof BoundingBox) {
            updateBox();
        } else if (bound instanceof BoundingSphere) {
            updateSphere();
        }
    }

    /**
     * Update the existing box geometry for an axis-aligned bounding box.
     */
    private void updateBox() {
        BoundingVolume bound = subject.getWorldBound();
        BoundingBox boundingBox = (BoundingBox) bound;
        Node subtreeNode = (Node) getSubtree();
        Geometry lines = (Geometry) subtreeNode.getChild(linesChildPosition);
        WireBox boxMesh = (WireBox) lines.getMesh();
        /*
         * Update the mesh extents.
         */
        float xExtent = boundingBox.getXExtent();
        float yExtent = boundingBox.getYExtent();
        float zExtent = boundingBox.getZExtent();
        assert xExtent >= 0f : xExtent;
        assert yExtent >= 0f : yExtent;
        assert zExtent >= 0f : zExtent;
        boxMesh.updatePositions(xExtent, yExtent, zExtent);
        /*
         * Update the transform.
         */
        Transform transform = new Transform();
        Vector3f center = boundingBox.getCenter();
        transform.setTranslation(center);
        MySpatial.setWorldTransform(lines, transform);

        updateLineWidth();
    }

    /**
     * Update the line width in lineMaterial.
     */
    private void updateLineWidth() {
        Node subtreeNode = (Node) getSubtree();
        Geometry lines = (Geometry) subtreeNode.getChild(linesChildPosition);
        assert effectiveLineWidth >= 1f : effectiveLineWidth;
        assert lineMaterial == lines.getMaterial();
        RenderState rs = lineMaterial.getAdditionalRenderState();
        rs.setLineWidth(effectiveLineWidth);
    }

    /**
     * Update the existing sphere geometry for a bounding sphere.
     */
    private void updateSphere() {
        BoundingVolume bound = subject.getWorldBound();
        BoundingSphere boundingSphere = (BoundingSphere) bound;
        Node subtreeNode = (Node) getSubtree();
        Geometry lines = (Geometry) subtreeNode.getChild(linesChildPosition);
        /*
         * Update the transform.
         */
        Transform transform = new Transform();
        Vector3f center = boundingSphere.getCenter();
        transform.setTranslation(center);
        if (camera != null) {
            Vector3f offset = camera.getLocation().subtract(center);
            Vector3f axis1 = new Vector3f();
            Vector3f axis2 = new Vector3f();
            MyVector3f.generateBasis(offset, axis1, axis2);

            Quaternion orientation = transform.getRotation();
            if (billboardAxis == MyVector3f.xAxis) {
                orientation.fromAxes(offset, axis1, axis2);
            } else if (billboardAxis == MyVector3f.yAxis) {
                orientation.fromAxes(axis2, offset, axis1);
            } else if (billboardAxis == MyVector3f.zAxis) {
                orientation.fromAxes(axis1, axis2, offset);
            } else {
                String message = "billboardAxis = " + billboardAxis;
                throw new IllegalStateException(message);
            }
        }
        float radius = boundingSphere.getRadius();
        transform.setScale(radius);
        MySpatial.setWorldTransform(lines, transform);

        updateLineWidth();
    }
}

/*
 Copyright (c) 2017-2019, Stephen Gold
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
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.debug.WireSphere;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * A SubtreeControl to visualize the world bound of a subject spatial.
 * <p>
 * The controlled spatial must be a Node, but the subject (visualized spatial)
 * may be a Geometry.
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
    final private static String tagLineMaterial = "lineMaterial";
    final private static String tagLineWidth = "lineWidth";
    final private static String tagSubject = "subject";
    // *************************************************************************
    // fields

    /**
     * effective line width (in pixels, &ge;0, values &lt;1 hide the lines)
     */
    private float effectiveLineWidth = defaultLineWidth;
    /**
     * wireframe material for lines (color and depth-test are stored here)
     */
    private Material lineMaterial;
    /**
     * the spatial whose world bound is being visualized, or null for none
     */
    private Spatial subject = null;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected BoundsVisualizer() {
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

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the lines. TODO storeResult argument
     *
     * @return a new instance
     */
    public ColorRGBA copyLineColor() {
        MatParam parameter = lineMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Read the depth-test setting.
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
     * Read the effective line width of the visualization.
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
     * Alter which Spatial is visualized.
     *
     * @param newSubject which spatial to visualize (may be null, alias created)
     */
    public void setSubject(Spatial newSubject) {
        subject = newSubject;
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
                    && mesh instanceof WireSphere) {
                updateSphere();
            } else { // wrong type of mesh
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

        lineMaterial = (Material) capsule.readSavable(tagLineMaterial, null);
        effectiveLineWidth = capsule.readFloat(tagLineWidth, 0f);
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

        capsule.write(lineMaterial, tagLineMaterial, null);
        capsule.write(effectiveLineWidth, tagLineWidth, 0f);
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
            mesh = new WireSphere();
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
     * Update existing WireBox lines based on the subject's world bound.
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
     * Update the line width.
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
     * Update existing WireSphere lines based on the subject's world bound.
     */
    private void updateSphere() {
        BoundingVolume bound = subject.getWorldBound();
        BoundingSphere boundingSphere = (BoundingSphere) bound;
        Node subtreeNode = (Node) getSubtree();
        Geometry lines = (Geometry) subtreeNode.getChild(linesChildPosition);
        WireSphere sphereMesh = (WireSphere) lines.getMesh();
        /*
         * Update the mesh radius.
         */
        float radius = boundingSphere.getRadius();
        assert radius >= 0f : radius;
        sphereMesh.updatePositions(radius);
        /*
         * Update the transform.
         */
        Transform transform = new Transform();
        Vector3f center = boundingSphere.getCenter();
        transform.setTranslation(center);
        MySpatial.setWorldTransform(lines, transform);

        updateLineWidth();
    }
}

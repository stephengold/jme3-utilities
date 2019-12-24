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
package jme3utilities.debug;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A SubtreeControl to visualize the coordinate axes of a Node.
 * <p>
 * The controlled spatial must be a Node. TODO option to specify a subject
 * spatial
 * <p>
 * A new Control is disabled by default. When enabled, it attaches up to 3 arrow
 * geometries to the subtree, each of which represents an axis.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesVisualizer extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default depth-test setting (disabled)
     */
    final private static boolean defaultDepthTest = false;
    /**
     * color of the X-axis arrow (red)
     */
    final private static ColorRGBA xColor = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * color of the Y-axis arrow (green)
     */
    final private static ColorRGBA yColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * color of the Z-axis arrow (blue)
     */
    final private static ColorRGBA zColor = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AxesVisualizer.class.getName());
    /**
     * asset path to the solid arrow model
     */
    final private static String modelAssetPath
            = "Models/indicators/arrow/arrow.j3o";
    /**
     * name for the subtree node
     */
    final private static String subtreeName = "axes node";
    /**
     * field names for serialization
     */
    final private static String tagAxisLength = "axisLength";
    final private static String tagDepthTest = "depthTest";
    final private static String tagLineWidth = "lineWidth";
    final private static String tagNumAxes = "numAxes";
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
     * asset manager to use (not null)
     */
    private AssetManager assetManager;
    /**
     * true &rarr; enabled, false &rarr; disabled.
     *
     * The test provides depth cues, but often hides the axes.
     */
    private boolean depthTest = defaultDepthTest;
    /**
     * length of each axis arrow (in world units, &gt;0)
     */
    private float axisLength;
    /**
     * line width for wireframe arrows (in pixels, &ge;1) or 0 for solid arrows
     */
    private float lineWidth;
    /**
     * number of axis arrows (&ge;1, &le;3, defaults to 3)
     */
    private int numAxes = MyVector3f.numAxes;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected AxesVisualizer() {
        assetManager = null;
    }

    /**
     * Instantiate a set of hidden solid coordinate axes.
     *
     * @param manager for loading assets (not null)
     * @param length length of each axis arrow (in world units, &gt;0)
     */
    public AxesVisualizer(AssetManager manager, float length) {
        super();
        Validate.nonNull(manager, "asset manager");
        Validate.positive(length, "axis length");

        assetManager = manager;
        axisLength = length;
        lineWidth = 0f;
    }

    /**
     * Instantiate a set of hidden wireframe coordinate axes.
     *
     * @param manager for loading material definitions (not null)
     * @param length length of each axis arrow (in world units, &gt;0)
     * @param width thickness of each axis arrow (in pixels, &ge;1)
     */
    public AxesVisualizer(AssetManager manager, float length, float width) {
        super();
        Validate.nonNull(manager, "asset manager");
        Validate.positive(length, "axis length");
        Validate.inRange(width, "line width", 1f, Float.MAX_VALUE);

        assetManager = manager;
        axisLength = length;
        lineWidth = width;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the length of the axis arrows.
     *
     * @return length (in world units, &gt;0)
     */
    public float axisLength() {
        assert axisLength > 0f : axisLength;
        return axisLength;
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
        return depthTest;
    }

    /**
     * Read the line width of the axis arrows.
     *
     * @return width (in pixels, &ge;1) or 0 for solid arrows
     */
    public float lineWidth() {
        assert lineWidth >= 0f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the number of axis arrows.
     *
     * @return count (&ge;1, &le;3)
     */
    public int numAxes() {
        assert numAxes >= 1 : numAxes;
        assert numAxes <= MyVector3f.numAxes : numAxes;

        return numAxes;
    }

    /**
     * Alter the length of the axis arrows.
     *
     * @param length (in world units, &gt;0)
     */
    public void setAxisLength(float length) {
        Validate.positive(length, "length");
        axisLength = length;
    }

    /**
     * Alter the depth-test setting. The test provides depth cues, but often
     * hides the axes.
     *
     * @param newSetting true to enable test, false to disable it
     */
    public void setDepthTest(boolean newSetting) {
        depthTest = newSetting;
    }

    /**
     * Alter the number of axis arrows.
     *
     * @param newNumber (&ge;1, &le;3)
     */
    public void setNumAxes(int newNumber) {
        Validate.inRange(newNumber, "new number", 1, MyVector3f.numAxes);
        numAxes = newNumber;
    }

    /**
     * Alter the line width.
     *
     * @param width (in pixels, &ge;1) or 0 for solid arrows
     */
    public void setLineWidth(float width) {
        Validate.inRange(width, "width", 0f, Float.MAX_VALUE);
        lineWidth = width;
    }

    /**
     * Calculate the tip location of the indexed axis arrow.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return a new vector (in world coordinates) or null if not displayed
     */
    public Vector3f tipLocation(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        Vector3f result = null;
        if (isEnabled() && axisIndex < numAxes) {
            Node subtreeNode = (Node) getSubtree();
            MySpatial.setWorldScale(subtreeNode, axisLength);
            Geometry arrow = (Geometry) subtreeNode.getChild(axisIndex);
            result = arrow.localToWorld(unitX, null);
        }

        return result;
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * Create a shallow copy of this Control.
     *
     * @return a new Control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public AxesVisualizer clone() throws CloneNotSupportedException {
        AxesVisualizer clone = (AxesVisualizer) super.clone();
        return clone;
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
        int numChildren = subtreeNode.getQuantity();
        if (numChildren != numAxes) {
            subtreeNode.detachAllChildren();
            addArrows();

        } else {
            Geometry xArrow = (Geometry) subtreeNode.getChild(0);
            Mesh xMesh = xArrow.getMesh();
            boolean arrowMesh = xMesh instanceof Arrow;

            if (lineWidth >= 1f && arrowMesh) {
                updateArrows();
            } else if (lineWidth < 1f && !arrowMesh) {
                updateArrows();
            } else {
                subtreeNode.detachAllChildren();
                addArrows();
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
        assetManager = importer.getAssetManager();
        InputCapsule capsule = importer.getCapsule(this);

        axisLength = capsule.readFloat(tagAxisLength, 1f);
        depthTest = capsule.readBoolean(tagDepthTest, defaultDepthTest);
        lineWidth = capsule.readFloat(tagLineWidth, 0f);
        numAxes = capsule.readInt(tagNumAxes, MyVector3f.numAxes);
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

        capsule.write(axisLength, tagAxisLength, 1f);
        capsule.write(depthTest, tagDepthTest, defaultDepthTest);
        capsule.write(lineWidth, tagLineWidth, 0f);
        capsule.write(numAxes, tagNumAxes, MyVector3f.numAxes);
    }
    // *************************************************************************
    // private methods

    /**
     * Create up to 3 arrow geometries and add them to the subtree.
     */
    private void addArrows() {
        assert ((Node) getSubtree()).getQuantity() == 0;
        if (lineWidth >= 1f) {
            addWireArrow(xColor, "xAxis", unitX);
            if (numAxes > MyVector3f.yAxis) {
                addWireArrow(yColor, "yAxis", unitY);
                if (numAxes > MyVector3f.zAxis) {
                    addWireArrow(zColor, "zAxis", unitZ);
                }
            }

        } else {
            addSolidArrow(xColor, "xAxis", unitX);
            if (numAxes > MyVector3f.yAxis) {
                addSolidArrow(yColor, "yAxis", unitY);
                if (numAxes > MyVector3f.zAxis) {
                    addSolidArrow(zColor, "zAxis", unitZ);
                }
            }
        }

        updateArrows();
    }

    /**
     * Create and attach a solid arrow geometry to represent an axis.
     *
     * @param color for the arrow (not null, unaffected)
     * @param name for the geometry (not null)
     * @param direction for the arrow to point (in local coordinates, length=1,
     * unaffected)
     */
    private void addSolidArrow(ColorRGBA color, String name,
            Vector3f direction) {
        assert assetManager != null;
        assert color != null;
        assert name != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Node node = (Node) assetManager.loadModel(modelAssetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        Geometry geometry = (Geometry) node3.getChild(0);
        ((Node) getSubtree()).attachChild(geometry);

        Vector3f xDir = direction.clone();
        Vector3f yDir = new Vector3f();
        Vector3f zDir = new Vector3f();
        MyVector3f.generateBasis(xDir, yDir, zDir);
        Quaternion orientation = new Quaternion();
        orientation.fromAxes(xDir, yDir, zDir);
        geometry.setLocalRotation(orientation);
        geometry.setName(name);

        Material material = MyAsset.createUnshadedMaterial(assetManager, color);
        geometry.setMaterial(material);
        material.getAdditionalRenderState().setDepthTest(depthTest);
        material.setName(name + "Material");
    }

    /**
     * Create and attach a wireframe arrow geometry to represent an axis.
     *
     * @param color for the wireframe (not null, unaffected)
     * @param name for the geometry (not null)
     * @param direction for the arrow to point (in local coordinates, length=1,
     * unaffected)
     */
    private void addWireArrow(ColorRGBA color, String name,
            Vector3f direction) {
        assert assetManager != null;
        assert color != null;
        assert name != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Arrow mesh = new Arrow(direction);
        Geometry geometry = new Geometry(name, mesh);
        ((Node) getSubtree()).attachChild(geometry);

        Material material
                = MyAsset.createWireframeMaterial(assetManager, color);
        geometry.setMaterial(material);
        material.getAdditionalRenderState().setDepthTest(depthTest);
        material.setName(name + "Material");
    }

    /**
     * Update the existing axis arrows.
     */
    private void updateArrows() {
        Node subtreeNode = (Node) getSubtree();
        MySpatial.setWorldScale(subtreeNode, axisLength);

        for (Spatial axis : subtreeNode.getChildren()) {
            Geometry geometry = (Geometry) axis;
            Material material = geometry.getMaterial();
            RenderState state = material.getAdditionalRenderState();

            state.setDepthTest(depthTest);
            if (lineWidth >= 1f) {
                state.setLineWidth(lineWidth);
            }
        }
    }
}

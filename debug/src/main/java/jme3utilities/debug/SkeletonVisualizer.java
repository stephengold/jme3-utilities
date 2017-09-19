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

import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to visualize the skeleton of a skeletonized node.
 * <p>
 * The controlled spatial must be a node.
 * <p>
 * The control is disabled by default. When enabled, it attaches a node and 2
 * geometries to the scene graph.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonVisualizer extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default color for lines (blue)
     */
    final private static ColorRGBA defaultLineColor = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * default color for points (white)
     */
    final private static ColorRGBA defaultPointColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * default width for lines (in pixels)
     */
    final private static float defaultLineWidth = 2f;
    /**
     * default size for points (in pixels)
     */
    final private static float defaultPointSize = 4f;
    /**
     * child position of the heads geometry in the subtree node
     */
    final private static int headsChildPosition = 0;
    /**
     * child position of the links geometry in the subtree node
     */
    final private static int linksChildPosition = 1;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonVisualizer.class.getName());
    /**
     * asset path to default shape for points
     */
    final private static String defaultPointShapeAssetPath = "Textures/shapes/solid circle.png";
    // *************************************************************************
    // fields

    /**
     * standard color for bone heads
     */
    private ColorRGBA standardPointColor = defaultPointColor.clone();
    /**
     * line width (in pixels)
     */
    private float lineWidth;
    /**
     * custom color for each bone's head
     */
    private Map<Integer, ColorRGBA> pointColors = new TreeMap<>();
    /**
     * material for lines/links
     */
    private Material lineMaterial;
    /**
     * material for points/heads
     */
    private Material pointMaterial;
    /**
     * skeleton being visualized, or null for none
     */
    private Skeleton skeleton = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading material definitions (not null)
     */
    public SkeletonVisualizer(AssetManager assetManager) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        lineMaterial = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        lineMaterial.setBoolean("UseVertexColor", true);
        lineMaterial.setFloat("AlphaDiscardThreshold", 0.9999f);
        RenderState rs2 = lineMaterial.getAdditionalRenderState();
        rs2.setBlendMode(BlendMode.Alpha);
        rs2.setDepthTest(false);
        rs2.setWireframe(true);
        setLineColor(defaultLineColor);
        setLineWidth(defaultLineWidth);

        pointMaterial = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        pointMaterial.setBoolean("UseVertexColor", true);
        RenderState rs = pointMaterial.getAdditionalRenderState();
        rs.setBlendMode(BlendMode.Alpha);
        rs.setDepthTest(false);
        rs.setWireframe(true);

        if (supportsPointShape()) {
            Texture pointShape = MyAsset.loadTexture(assetManager,
                    defaultPointShapeAssetPath);
            setPointShape(pointShape);
        }

        if (supportsPointSize()) {
            setPointSize(defaultPointSize);
        }

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the lines.
     *
     * @return a new instance
     */
    public ColorRGBA copyLineColor() {
        MatParam parameter = lineMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Copy the point color for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return a new instance
     */
    public ColorRGBA copyPointColor(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        ColorRGBA color = pointColors.get(boneIndex);
        if (color == null) {
            color = standardPointColor;
        }

        return color.clone();
    }

    /**
     * Read the line width of the visualization.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        float result = lineMaterial.getAdditionalRenderState().getLineWidth();
        assert result >= 1f : result;
        return result;
    }

    /**
     * Read the point size of the visualization.
     *
     * @return size (in pixels, &ge;1)
     */
    public float getPointSize() {
        float result;
        if (supportsPointSize()) {
            MatParam parameter = pointMaterial.getParam("PointSize");
            result = (float) parameter.getValue();
        } else {
            result = 1f;
        }

        return result;
    }

    /**
     * Test whether the specified spatial has skeleton visualization enabled.
     *
     * @param model spatial (not null)
     * @return true if enabled, otherwise false
     */
    public static boolean isDebugEnabled(Spatial model) {
        SkeletonVisualizer control = model.getControl(SkeletonVisualizer.class);
        if (control == null) {
            return false;
        }
        boolean result = control.isEnabled();

        return result;
    }

    /**
     * Alter the colors of all lines and points.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        setLineColor(newColor);
        setPointColor(newColor);
    }

    /**
     * Alter a spatial's visualization status. Has no effect if the spatial
     * lacks a SkeletonVisualizer.
     *
     * @param model skeletonized spatial (not null)
     * @param newState true to enable, false to disable
     */
    public static void setDebugEnabled(Spatial model, boolean newState) {
        SkeletonVisualizer control = model.getControl(SkeletonVisualizer.class);
        if (control != null) {
            control.setEnabled(newState);
        }
    }

    /**
     * Alter the colors of all lines.
     *
     * @param newColor (not null, unaffected)
     */
    final public void setLineColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        lineMaterial.setColor("Color", newColor.clone());
    }

    /**
     * Alter the line width of the visualization.
     *
     * @param width (in pixels, values &lt;1 hide the lines)
     */
    final public void setLineWidth(float width) {
        lineWidth = width;

        if (subtree != null) {
            Geometry links = (Geometry) subtree.getChild(linksChildPosition);
            if (lineWidth < 1f) {
                links.setCullHint(Spatial.CullHint.Always);
            } else {
                links.setCullHint(Spatial.CullHint.Inherit);
                lineMaterial.getAdditionalRenderState().setLineWidth(lineWidth);
            }
        }
    }

    /**
     * Alter the colors of all points.
     *
     * @param newColor (not null, unaffected)
     */
    public void setPointColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        standardPointColor.set(newColor);
        pointColors.clear();
    }

    /**
     * Alter the point color for the indexed bone only.
     *
     * @param boneIndex which bone (&ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setPointColor(int boneIndex, ColorRGBA newColor) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(newColor, "new color");

        pointColors.put(boneIndex, newColor.clone());
    }

    /**
     * Alter the point shape in the visualization.
     *
     * @param shape shape texture (not null)
     */
    final public void setPointShape(Texture shape) {
        Validate.nonNull(shape, "shape");

        if (supportsPointShape()) {
            pointMaterial.setTexture("PointShape", shape);
        } else {
            logger.log(Level.WARNING, "Cannot set point shape.");
        }
    }

    /**
     * Alter the point size in the visualization.
     *
     * @param size (in pixels, &ge;0, 0 &rarr; hide the points)
     */
    final public void setPointSize(float size) {
        Validate.inRange(size, "size", 0f, Float.MAX_VALUE);

        if (supportsPointSize()) {
            pointMaterial.setFloat("PointSize", size);
        } else {
            logger.log(Level.WARNING, "Cannot set point size.");
        }
    }

    /**
     * Alter the skeleton being visualized.
     *
     * @param newSkeleton which skeleton to visualize (may be null, alias
     * created)
     */
    public void setSkeleton(Skeleton newSkeleton) {
        skeleton = newSkeleton;

        if (subtree != null) {
            subtree.detachAllChildren();

            String namePrefix = "";
            if (spatial != null) {
                namePrefix = spatial.getName() + " ";
            }

            String headsName = namePrefix + "heads";
            SkeletonMesh headsMesh = new SkeletonMesh(skeleton, Mode.Points);
            Geometry headsGeometry = new Geometry(headsName, headsMesh);
            headsGeometry.setMaterial(pointMaterial);
            subtree.attachChildAt(headsGeometry, headsChildPosition);

            String linksName = namePrefix + "links";
            SkeletonMesh linksMesh = new SkeletonMesh(skeleton, Mode.Lines);
            Geometry linksGeometry = new Geometry(linksName, linksMesh);
            linksGeometry.setMaterial(lineMaterial);
            subtree.attachChildAt(linksGeometry, linksChildPosition);
        }
    }

    /**
     * Test whether the points material supports PointShape.
     *
     * @return true if supported, false otherwise
     */
    final public boolean supportsPointShape() {
        MaterialDef def = pointMaterial.getMaterialDef();
        if (def.getMaterialParam("PointShape") == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the points material supports PointSize. (The PointSize
     * parameter was missing from Unshaded.j3md in JME 3.1.0.)
     *
     * @return true if supported, false otherwise
     */
    final public boolean supportsPointSize() {
        MaterialDef def = pointMaterial.getMaterialDef();
        if (def.getMaterialParam("PointSize") == null) {
            return false;
        } else {
            return true;
        }
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Callback invoked when the spatial's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);
        /*
         * Copy the world transform from an animated geometry to the visualizer
         * (and hope any other animated geometries share the same transform!)
         */
        Geometry ag = MySpatial.findAnimatedGeometry(spatial);
        if (ag != null) {
            Spatial loopSpatial = ag;
            Transform combined = new Transform();
            /*
             * Climb the scene graph applying local transforms until the
             * controlled spatial is reached.
             */
            while (loopSpatial != spatial && loopSpatial != null) {
                Transform localTransform = loopSpatial.getLocalTransform();
                combined.combineWithParent(localTransform);
                loopSpatial = loopSpatial.getParent();
            }
            subtree.setLocalTransform(combined);
        }

        int numBones;
        if (skeleton == null) {
            numBones = 0;
        } else {
            numBones = skeleton.getBoneCount();
        }
        ColorRGBA[] colors = new ColorRGBA[numBones];
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            colors[boneIndex] = copyPointColor(boneIndex);
        }

        Geometry headsGeom = (Geometry) subtree.getChild(headsChildPosition);
        SkeletonMesh headsMesh = (SkeletonMesh) headsGeom.getMesh();
        headsMesh.updateColors(colors);
        headsMesh.updatePositions(skeleton);

        Geometry linksGeom = (Geometry) subtree.getChild(linksChildPosition);
        SkeletonMesh linksMesh = (SkeletonMesh) linksGeom.getMesh();
        linksMesh.updateColors(colors);
        linksMesh.updatePositions(skeleton);
    }

    /**
     * Alter the visibility of the visualization.
     *
     * @param newState if true, reveal the visualization; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        if (newState && subtree == null) {
            /*
             * Before enabling this control for the first time,
             * create the subtree.
             */
            String nodeName = spatial.getName() + " skeleton";
            subtree = new Node(nodeName);
            subtree.setQueueBucket(RenderQueue.Bucket.Transparent);
            subtree.setShadowMode(RenderQueue.ShadowMode.Off);

            setSkeleton(skeleton);
            setLineWidth(lineWidth);
        }

        super.setEnabled(newState);
    }

    /**
     * Alter which node is controlled.
     *
     * @param newNode the node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newNode) {
        super.setSpatial(newNode);

        Skeleton foundSkeleton = null;
        if (newNode != null) {
            foundSkeleton = MySkeleton.findSkeleton(newNode);
        }
        setSkeleton(foundSkeleton);
    }
    // *************************************************************************
    // JmeCloneable methods

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

        lineMaterial = cloner.clone(lineMaterial);
        pointMaterial = cloner.clone(pointMaterial);
        standardPointColor = cloner.clone(standardPointColor);
        skeleton = cloner.clone(skeleton);

        Map<Integer, ColorRGBA> copyColors = new TreeMap<>();
        for (Map.Entry<Integer, ColorRGBA> entry : pointColors.entrySet()) {
            int key = entry.getKey();
            ColorRGBA value = entry.getValue().clone();
            copyColors.put(key, value);
        }
        pointColors = copyColors;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SkeletonVisualizer clone() throws CloneNotSupportedException {
        SkeletonVisualizer clone = (SkeletonVisualizer) super.clone();
        return clone;
    }
}

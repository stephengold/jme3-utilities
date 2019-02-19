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

import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * A SubtreeControl to visualize a Skeleton.
 * <p>
 * The controlled Spatial must be a Node.
 * <p>
 * A new Control is disabled by default. When enabled, it attaches 2 geometries
 * to its subtree Node.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonVisualizer extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default color for bone heads (white)
     */
    final private static ColorRGBA defaultHeadColor
            = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * default color for link lines (blue)
     */
    final private static ColorRGBA defaultLineColor
            = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * default size for bone heads (in pixels)
     */
    final private static float defaultHeadSize = 4f;
    /**
     * default width for link lines (in pixels)
     */
    final private static float defaultLineWidth = 2f;
    /**
     * child position of the heads geometry in the subtree Node
     */
    final private static int headsChildPosition = 0;
    /**
     * child position of the links geometry in the subtree Node
     */
    final private static int linksChildPosition = 1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonVisualizer.class.getName());
    /**
     * asset path to the default shape for bone heads
     */
    final private static String defaultShapeAssetPath
            = "Textures/shapes/solid circle.png";
    /**
     * name for the heads Geometry
     */
    final private static String headsName = "skeleton heads";
    /**
     * name for the links Geometry
     */
    final private static String linksName = "skeleton links";
    /**
     * asset path to the skeleton material definition
     */
    final private static String matDefsAssetPath
            = "MatDefs/wireframe/multicolor2.j3md";
    /**
     * name for the subtree node
     */
    final private static String subtreeName = "skeleton node";
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // fields

    /**
     * general color for bone heads
     */
    private ColorRGBA headColor = defaultHeadColor.clone();
    /**
     * effective line width (in pixels, &ge;0, values &lt;1 hide the lines)
     */
    private float effectiveLineWidth = defaultLineWidth;
    /**
     * custom colors for the heads of specific bones
     */
    private Map<Integer, ColorRGBA> customColors = new TreeMap<>();
    /**
     * material for bone heads (shape, size, and standard color are stored here)
     */
    private Material headMaterial;
    /**
     * material for link lines (standard color is stored here)
     */
    private Material lineMaterial;
    /**
     * Skeleton being visualized, or null for none
     */
    private Skeleton skeleton = null;
    /**
     * Spatial providing the world transform, or null for none
     */
    private Spatial transformSpatial = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading material definitions (not null)
     * @param subject the skeleton control to visualize (may be null)
     */
    public SkeletonVisualizer(AssetManager assetManager,
            SkeletonControl subject) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        lineMaterial = new Material(assetManager, matDefsAssetPath);
        lineMaterial.setBoolean("UseVertexColor", true);
        lineMaterial.setColor("Color", defaultLineColor.clone());
        lineMaterial.setFloat("AlphaDiscardThreshold", 0.9999f);
        RenderState lineState = lineMaterial.getAdditionalRenderState();
        lineState.setBlendMode(BlendMode.Alpha);
        lineState.setDepthTest(false);

        Texture headShape
                = MyAsset.loadTexture(assetManager, defaultShapeAssetPath);

        headMaterial = new Material(assetManager, matDefsAssetPath);
        headMaterial.setBoolean("UseVertexColor", true);
        headMaterial.setFloat("AlphaDiscardThreshold", 0.0001f);
        headMaterial.setFloat("PointSize", defaultHeadSize);
        headMaterial.setTexture("PointShape", headShape);
        RenderState headState = headMaterial.getAdditionalRenderState();
        headState.setBlendMode(BlendMode.Alpha);
        headState.setDepthTest(false);

        setSubject(subject);

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color for the head of the indexed bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult (modified if not null)
     * @return the color (either storeResult or a new instance)
     */
    public ColorRGBA copyHeadColor(int boneIndex, ColorRGBA storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        ColorRGBA result
                = (storeResult == null) ? new ColorRGBA() : storeResult;

        ColorRGBA color = customColors.get(boneIndex);
        if (color == null) {
            color = headColor;
        }
        result.set(color);

        return result;
    }

    /**
     * Copy the color for link lines.
     *
     * @param storeResult (modified if not null)
     * @return the color (either storeResult or a new instance)
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
     * Read the size for bone heads (in pixels).
     *
     * @return size (in pixels, &ge;1)
     */
    public float headSize() {
        MatParam parameter = headMaterial.getParam("PointSize");
        float result = (float) parameter.getValue();

        assert result >= 1f : result;
        return result;
    }

    /**
     * Read the effective line width for links.
     *
     * @return width (in pixels, &ge;0)
     */
    public float lineWidth() {
        assert effectiveLineWidth >= 0f : effectiveLineWidth;
        return effectiveLineWidth;
    }

    /**
     * Alter the colors of all link lines and bone heads.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        setLineColor(newColor);
        SkeletonVisualizer.this.setHeadColor(newColor);
    }

    /**
     * Alter the colors of all bone heads.
     *
     * @param newColor (not null, unaffected)
     */
    public void setHeadColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        headColor.set(newColor);
        customColors.clear();
    }

    /**
     * Alter the color of the indexed bone's head.
     *
     * @param boneIndex which bone (&ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setHeadColor(int boneIndex, ColorRGBA newColor) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(newColor, "new color");

        customColors.put(boneIndex, newColor.clone());
    }

    /**
     * Alter the shape used to visualize bone heads.
     *
     * @param shape shape texture (not null, alias created)
     */
    public void setHeadShape(Texture shape) {
        Validate.nonNull(shape, "shape");
        headMaterial.setTexture("PointShape", shape);
    }

    /**
     * Alter the size of bone heads.
     *
     * @param size (in pixels, &ge;0, 0 &rarr; hide the heads)
     */
    public void setHeadSize(float size) {
        Validate.inRange(size, "size", 0f, Float.MAX_VALUE);
        headMaterial.setFloat("PointSize", size);
    }

    /**
     * Alter the colors of all link lines.
     *
     * @param newColor (not null, unaffected)
     */
    public void setLineColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        lineMaterial.setColor("Color", newColor.clone());
    }

    /**
     * Alter the effective line width for links.
     *
     * @param width (in pixels, &ge;0, values &lt;1 hide the lines)
     */
    public void setLineWidth(float width) {
        Validate.nonNegative(width, "width");
        effectiveLineWidth = width;
    }

    /**
     * Alter which Skeleton is visualized.
     *
     * @param newSkeleton the Skeleton to visualize (may be null, alias created)
     */
    public void setSkeleton(Skeleton newSkeleton) {
        if (skeleton != newSkeleton) {
            if (subtree != null) {
                subtree.detachAllChildren();
            }
            skeleton = newSkeleton;
        }
    }

    /**
     * Configure the skeleton and transform spatial based on the specified SkeletonControl.
     *
     * @param subject the SkeletonControl to analyze (may be null)
     */
    final public void setSubject(SkeletonControl subject) {
        if (subject == null) {
            skeleton = null;
            transformSpatial = null;
        } else {
            setSkeleton(subject.getSkeleton());
            Spatial controlledSpatial = subject.getSpatial();
            Spatial animatedGeometry
                    = MySpatial.findAnimatedGeometry(controlledSpatial);
            if (animatedGeometry == null) {
                transformSpatial = controlledSpatial;
            } else {
                transformSpatial = animatedGeometry;
            }
        }
    }

    /**
     * Alter which Spatial provides the world transform
     *
     * @param spatial which spatial to use (may be null, alias created)
     */
    public void setTransformSpatial(Spatial spatial) {
        transformSpatial = spatial;
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new Control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SkeletonVisualizer clone() throws CloneNotSupportedException {
        SkeletonVisualizer clone = (SkeletonVisualizer) super.clone();
        return clone;
    }

    /**
     * Convert this shallow-cloned Control into a deep-cloned one, using the
     * specified Cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null,
     * modified)
     * @param original the control from which this control was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        SkeletonVisualizer originalVisualizer = (SkeletonVisualizer) original;
        customColors = new TreeMap<>();
        for (Map.Entry<Integer, ColorRGBA> entry
                : originalVisualizer.customColors.entrySet()) {
            int boneIndex = entry.getKey();
            ColorRGBA color = entry.getValue();
            ColorRGBA copyColor = cloner.clone(color);
            customColors.put(boneIndex, copyColor);
        }

        headColor = cloner.clone(headColor);
        headMaterial = cloner.clone(headMaterial);
        lineMaterial = cloner.clone(lineMaterial);
        skeleton = cloner.clone(skeleton);
        transformSpatial = cloner.clone(transformSpatial);
    }

    /**
     * Callback invoked when the spatial's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);

        if (skeleton == null || skeleton.getBoneCount() == 0) {
            subtree.detachAllChildren();
        } else if (subtree.getQuantity() == 0) {
            addGeometries();
        } else {
            updateGeometries();
        }
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
             * Before enabling this control for the 1st time,
             * create the subtree.
             */
            subtree = new Node(subtreeName);
            subtree.setQueueBucket(RenderQueue.Bucket.Transparent);
            subtree.setShadowMode(RenderQueue.ShadowMode.Off);
        }

        super.setEnabled(newState);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a heads Geometry and a links Geometry and attach them to the empty
     * subtree Node.
     */
    private void addGeometries() {
        assert subtree.getQuantity() == 0;

        SkeletonMesh headsMesh = new SkeletonMesh(skeleton, Mesh.Mode.Points);
        Geometry headsGeometry = new Geometry(headsName, headsMesh);
        headsGeometry.setMaterial(headMaterial);
        subtree.attachChildAt(headsGeometry, headsChildPosition);

        SkeletonMesh linksMesh = new SkeletonMesh(skeleton, Mesh.Mode.Lines);
        Geometry linksGeometry = new Geometry(linksName, linksMesh);
        linksGeometry.setMaterial(lineMaterial);
        subtree.attachChildAt(linksGeometry, linksChildPosition);

        updateGeometries();
    }

    /**
     * Update existing geometries based on the skeleton and the transform
     * spatial.
     */
    private void updateGeometries() {
        Transform worldTransform;
        if (transformSpatial == null
                || MySpatial.isIgnoringTransforms(transformSpatial)) {
            worldTransform = transformIdentity;
        } else {
            worldTransform = transformSpatial.getWorldTransform();
        }
        MySpatial.setWorldTransform(subtree, worldTransform);

        int numBones = skeleton.getBoneCount();
        ColorRGBA[] colors = new ColorRGBA[numBones]; // TODO re-use the array
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            colors[boneIndex] = copyHeadColor(boneIndex, null);
        }

        Geometry headsGeometry
                = (Geometry) subtree.getChild(headsChildPosition);
        SkeletonMesh headsMesh = (SkeletonMesh) headsGeometry.getMesh();
        headsMesh.updateColors(colors);
        headsMesh.updatePositions(skeleton);

        Geometry linksGeometry
                = (Geometry) subtree.getChild(linksChildPosition);
        SkeletonMesh linksMesh = (SkeletonMesh) linksGeometry.getMesh();
        linksMesh.updateColors(colors);
        linksMesh.updatePositions(skeleton);

        if (effectiveLineWidth >= 1f) {
            assert lineMaterial == linksGeometry.getMaterial();
            RenderState rs = lineMaterial.getAdditionalRenderState();
            rs.setLineWidth(effectiveLineWidth);
            linksGeometry.setCullHint(Spatial.CullHint.Inherit);
        } else {
            linksGeometry.setCullHint(Spatial.CullHint.Always);
        }
    }
}

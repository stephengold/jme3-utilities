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
package jme3utilities.debug;

import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
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
 * The control is disabled by default. When enabled, it attaches a node and two
 * geometries to the scene graph.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonDebugControl extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default color for the lines (blue)
     */
    final private static ColorRGBA defaultLineColor = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * default color for the points (white)
     */
    final private static ColorRGBA defaultPointColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * default width for the lines (in pixels)
     */
    final private static float defaultLineWidth = 2f;
    /**
     * default size for the points (in pixels)
     */
    final private static float defaultPointSize = 4f;
    /**
     * child position of the heads geometry in the subtree node
     */
    final private static int headsChildPosition = 0;
    /**
     * child position of the links in geometry in the subtree node
     */
    final private static int linksChildPosition = 1;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonDebugControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * current line width (in pixels)
     */
    private float lineWidth;
    /**
     * material for lines/links
     */
    private Material lineMaterial;
    /**
     * material for points/heads
     */
    private Material pointMaterial;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading material definitions (not null)
     */
    public SkeletonDebugControl(AssetManager assetManager) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        lineMaterial = MyAsset.createWireframeMaterial(
                assetManager, defaultLineColor);
        lineMaterial.getAdditionalRenderState().setDepthTest(false);
        setLineWidth(defaultLineWidth);

        pointMaterial = MyAsset.createWireframeMaterial(
                assetManager, defaultPointColor);
        pointMaterial.getAdditionalRenderState().setDepthTest(false);
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
     * Copy the color of the points.
     *
     * @return a new instance
     */
    public ColorRGBA copyPointColor() {
        MatParam parameter = pointMaterial.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

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
        if (!supportsPointSize()) {
            return 1f;
        }
        MatParam parameter = pointMaterial.getParam("PointSize");
        float result = (float) parameter.getValue();

        return result;
    }

    /**
     * Test whether a skeletonized spatial has debugging enabled.
     *
     * @param model skeletonized spatial (not null)
     * @return true if enabled, otherwise false
     */
    public static boolean isDebugEnabled(Spatial model) {
        SkeletonDebugControl control = model.getControl(
                SkeletonDebugControl.class);
        if (control == null) {
            return false;
        }
        boolean result = control.isEnabled();

        return result;
    }

    /**
     * Alter the color of both the lines and the points.
     *
     * @param newColor (not null)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        lineMaterial.setColor("Color", newColor);
        pointMaterial.setColor("Color", newColor);
    }

    /**
     * Alter a skeletonized spatial's debug status. Has no effect if the spatial
     * lacks a SkeletonDebugControl.
     *
     * @param model skeletonized spatial (not null)
     * @param newState true to enable, false to disable
     */
    public static void setDebugEnabled(Spatial model, boolean newState) {
        SkeletonDebugControl control = model.getControl(
                SkeletonDebugControl.class);
        if (control != null) {
            control.setEnabled(newState);
        }
    }

    /**
     * Alter the color of the lines.
     *
     * @param newColor (not null)
     */
    public void setLineColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        lineMaterial.setColor("Color", newColor);
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
     * Alter the color of the points.
     *
     * @param newColor (not null)
     */
    public void setPointColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        pointMaterial.setColor("Color", newColor);
    }

    /**
     * Alter the point size of the visualization.
     *
     * @param size (in pixels, &ge;0, 0 &rarr; hide the points)
     */
    final public void setPointSize(float size) {
        Validate.inRange(size, "size", 0f, Float.MAX_VALUE);

        if (supportsPointSize()) {
            pointMaterial.setFloat("PointSize", size);
        } else {
            logger.log(Level.WARNING, "PointSize not set.");
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

        Skeleton skeleton = MySkeleton.getSkeleton(spatial);

        Geometry heads = (Geometry) subtree.getChild(headsChildPosition);
        BoneHeads headsMesh = (BoneHeads) heads.getMesh();
        headsMesh.update(skeleton);

        Geometry links = (Geometry) subtree.getChild(linksChildPosition);
        SkeletonLinks linksMesh = (SkeletonLinks) links.getMesh();
        linksMesh.update(skeleton);
    }

    /**
     * Alter the visibility of the visualization.
     *
     * @param newState if true, reveal the visualization; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        if (newState && subtree == null) {
            String nodeName = spatial.getName() + " skeleton";
            subtree = new Node(nodeName);
            /*
             * Copy local transform from 1st geometry to the debugger (and hope
             * any other geometries share the same transform!)
             */
            Node controlledNode = (Node) spatial;
            Geometry firstGeometry = MySpatial.findChild(controlledNode,
                    Geometry.class);
            if (firstGeometry != null) {
                Transform transform = firstGeometry.getLocalTransform();
                subtree.setLocalTransform(transform);
            }

            Skeleton skeleton = MySkeleton.getSkeleton(spatial);
            int numBones = skeleton.getBoneCount();

            BoneHeads headsMesh = new BoneHeads(numBones);
            String headsName = spatial.getName() + " heads";
            Geometry heads = new Geometry(headsName, headsMesh);
            heads.setMaterial(pointMaterial);
            subtree.attachChildAt(heads, headsChildPosition);

            SkeletonLinks linksMesh = new SkeletonLinks(skeleton);
            String linksName = spatial.getName() + " links";
            Geometry links = new Geometry(linksName, linksMesh);
            links.setMaterial(lineMaterial);
            subtree.attachChildAt(links, linksChildPosition);

            subtree.setQueueBucket(RenderQueue.Bucket.Transparent);
            subtree.setShadowMode(RenderQueue.ShadowMode.Off);
            setLineWidth(lineWidth);
        }

        super.setEnabled(newState);
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
    public SkeletonDebugControl clone() throws CloneNotSupportedException {
        SkeletonDebugControl clone = (SkeletonDebugControl) super.clone();
        return clone;
    }
}

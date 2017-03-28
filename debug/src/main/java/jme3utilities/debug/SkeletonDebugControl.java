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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.SkeletonDebugger;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySkeleton;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to visualize the skeleton of a skeletonized node.
 * <p>
 * The controlled spatial must be a Node.
 * <p>
 * The control is disabled by default. When enabled, it attaches a
 * SkeletonDebugger to the scene graph.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonDebugControl extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * default line width for the wireframe material
     */
    final private static float defaultLineWidth = 2f;
    /**
     * default point size for the wireframe material
     */
    final private static float defaultPointSize = 4f;
    /**
     * default color for the wireframe material
     */
    final private static ColorRGBA defaultColor = ColorRGBA.Blue;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonDebugControl.class.getName());
    // *************************************************************************
    // fields

    /*
     * wireframe material (set by constructor)
     */
    final private Material material;
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

        material = MyAsset.createWireframeMaterial(assetManager, defaultColor);
        material.getAdditionalRenderState().setDepthTest(false);
        setLineWidth(defaultLineWidth);
        if (supportsPointSize()) {
            setPointSize(defaultPointSize);
        }

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the visualization.
     *
     * @return a new instance
     */
    public ColorRGBA getColor() {
        MatParam parameter = material.getParam("Color");
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Read the line width of the visualization.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        float result = material.getAdditionalRenderState().getLineWidth();
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
        MatParam parameter = material.getParam("PointSize");
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
     * Alter the color of the visualization.
     *
     * @param newColor (not null)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");
        material.setColor("Color", newColor);
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
     * Alter the line width of the visualization.
     *
     * @param width (in pixels, &ge;1)
     */
    final public void setLineWidth(float width) {
        Validate.inRange(width, "width", 1f, Float.MAX_VALUE);
        material.getAdditionalRenderState().setLineWidth(width);
    }

    /**
     * Alter the point size of the visualization.
     *
     * @param size (in pixels, &ge;1)
     */
    final public void setPointSize(float size) {
        Validate.inRange(size, "size", 1f, Float.MAX_VALUE);

        if (supportsPointSize()) {
            material.setFloat("PointSize", size);
        } else {
            logger.log(Level.WARNING, "PointSize not set.");
        }
    }

    /**
     * Test whether the wireframe material supports PointSize. (The PointSize
     * parameter was missing from Unshaded.j3md in 3.1.0.)
     *
     * @return true if supported, false otherwise
     */
    final public boolean supportsPointSize() {
        MaterialDef def = material.getMaterialDef();
        if (def.getMaterialParam("PointSize") == null) {
            return false;
        } else {
            return true;
        }
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * Alter the visibility of the visualization.
     *
     * @param newState if true, reveal the visualization; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        if (newState && subtree == null) {
            Skeleton skeleton = MySkeleton.getSkeleton(spatial);
            String nodeName = spatial.getName() + " skeleton debugger";
            subtree = new SkeletonDebugger(nodeName, skeleton);
            subtree.setMaterial(material);
            subtree.setShadowMode(RenderQueue.ShadowMode.Off);
        }
        super.setEnabled(newState);
    }
}

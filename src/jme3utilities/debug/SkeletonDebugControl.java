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
package jme3utilities.debug;

import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.debug.SkeletonDebugger;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySkeleton;
import jme3utilities.SubtreeControl;

/**
 * A subtree control to visualize the skeleton of a skeletonized node.
 * <p>
 * The controlled spatial must be a Node.
 * <p>
 * The control is disabled by default. When enabled, it attaches a
 * SkeletonDebugger to the scene graph.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
final public class SkeletonDebugControl
        extends SubtreeControl {
    // *************************************************************************
    // constants

    /**
     * color for debug material
     */
    final private static ColorRGBA color = ColorRGBA.Blue;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SkeletonDebugControl.class.getName());
    // *************************************************************************
    // fields
    /*
     * material for the wire frame: set by constructor
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

        material = MyAsset.createWireframeMaterial(assetManager, color);
        material.getAdditionalRenderState().setDepthTest(false);
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
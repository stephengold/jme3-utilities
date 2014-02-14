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
package jme3utilities;

import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;

/**
 * A simple control which manages a single subtree of the scene graph. Such
 * controls can only be added to nodes, not geometries.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class SubtreeControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SubtreeControl.class.getName());
    // *************************************************************************
    // fields
    /**
     * the subtree managed by this control: set by subclass
     */
    protected Node subtree = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     */
    public SubtreeControl() {
        super.setEnabled(false);
    }
    // *************************************************************************
    // new public methods

    /**
     * Traverse this control's subtree in depth-first order.
     *
     * @param visitor method invoked on each spatial (not null)
     */
    public void traverse(SceneGraphVisitor visitor) {
        assert visitor != null;

        spatial.depthFirstTraversal(visitor);
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Alter the visibility of this control's subtree by adding or removing it
     * from the scene graph. This control must be added to a node before its
     * subtree can be revealed.
     *
     * @param newState if true, reveal the subtree; if false, hide them
     */
    @Override
    public void setEnabled(boolean newState) {
        if (subtree == null) {
            throw new IllegalStateException("subtree should be initialized");
        }
        Node node = (Node) spatial;

        if (enabled && !newState) {
            if (node != null) {
                /*
                 * Detach the subtree from the controlled node.
                 */
                int position = node.detachChild(subtree);
                assert position != -1;
            }

        } else if (!enabled && newState) {
            if (node == null) {
                throw new IllegalStateException(
                        "cannot enable the control before it's added to a node");
            }
            /*
             * Attach the subtree to the controlled node.
             */
            node.attachChild(subtree);
        }

        super.setEnabled(newState);
    }

    /**
     * Alter the controlled node.
     *
     * @param newNode which node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newNode) {
        super.setSpatial(newNode);

        if (enabled && newNode != null) {
            Node node = (Node) spatial;
            node.attachChild(subtree);
        }
    }
}
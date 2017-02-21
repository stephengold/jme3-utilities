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
package jme3utilities;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Simple control to manage a single subtree of the scene graph. Such controls
 * can only be added to nodes, not geometries.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class SubtreeControl extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SubtreeControl.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * subtree managed by this control: set by subclass
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
     * Traverse this control's subtree in depth-1st order.
     *
     * @param visitor method invoked on each spatial (not null)
     */
    public void traverse(SceneGraphVisitor visitor) {
        Validate.nonNull(visitor, "visitor");

        subtree.depthFirstTraversal(visitor);
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Clone this control for a different node.
     *
     * @param cloneSpatial node for clone to control (or null)
     * @return a new instance
     */
    @Override
    public Control cloneForSpatial(Spatial cloneSpatial) {
        if (cloneSpatial != null && !(cloneSpatial instanceof Node)) {
            throw new IllegalArgumentException(
                    "Clone's spatial must be a Node or null.");
        }

        SubtreeControl clone;
        try {
            clone = (SubtreeControl) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Can't clone control.", e);
        }

        Node node = (Node) spatial;
        int childIndex = node.getChildIndex(subtree);
        if (cloneSpatial == null || childIndex == -1) {
            clone.subtree = subtree.clone(true);
        } else {
            /*
             * The subtree was cloned when the controlled node was cloned.
             * Assume the cloned subtree has the same child index.
             */
            Node cloneNode = (Node) cloneSpatial;
            Spatial cloneChild = cloneNode.getChild(childIndex);
            assert cloneChild != null;
            clone.subtree = (Node) cloneChild;
        }

        clone.setSpatial(cloneSpatial);
        return clone;
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
        subtree = (Node) ic.readSavable("subtree", null);
    }

    /**
     * Alter the visibility of this control's subtree by adding or removing it
     * from the scene graph. This control must be added to a node before its
     * subtree can be revealed.
     *
     * @param newState if true, reveal the subtree; if false, hide it
     */
    @Override
    public void setEnabled(boolean newState) {
        Node node = (Node) spatial;

        if (enabled && !newState) {
            if (node != null && subtree != null) {
                /*
                 * Detach the subtree from the controlled node.
                 */
                int position = node.detachChild(subtree);
                assert position != -1 : position;
            }

        } else if (!enabled && newState) {
            if (node == null) {
                throw new IllegalStateException(
                        "control should be added to a node");
            }
            if (subtree == null) {
                throw new IllegalStateException(
                        "subtree should be initialized");
            }
            /*
             * Attach the subtree to the controlled node.
             */
            node.attachChild(subtree);
        }

        super.setEnabled(newState);
    }

    /**
     * Alter which node is controlled.
     *
     * @param newSpatial node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newSpatial) {
        if (newSpatial != null && !(newSpatial instanceof Node)) {
            throw new IllegalArgumentException(
                    "New spatial must be a Node or null.");
        }

        if (subtree != null && subtree.getParent() != newSpatial) {
            subtree.removeFromParent();
            if (enabled && newSpatial != null) {
                Node newNode = (Node) newSpatial;
                newNode.attachChild(subtree);
            }
        }
        spatial = newSpatial;
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
        oc.write(subtree, "subtree", null);
    }
}

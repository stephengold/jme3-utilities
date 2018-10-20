/*
 Copyright (c) 2014-2018, Stephen Gold
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
package jme3utilities;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
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
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SubtreeControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * subtree managed by this control: set by subclass
     */
    protected Node subtree = null;  // TODO could be a Geometry
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
     * Access this control's subtree. // TODO change return type
     *
     * @return the pre-existing instance, or null if none
     */
    public Node getSubtree() {
        return subtree;
    }

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
    // SimpleControl methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new control, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SubtreeControl clone() throws CloneNotSupportedException {
        SubtreeControl clone = (SubtreeControl) super.clone();
        return clone;
    }

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
        subtree = cloner.clone(subtree);
    }

    /**
     * Clone this control for a different node. No longer used as of JME 3.1.
     *
     * @param cloneNode the node for the clone to control (or null)
     * @return never
     * @throws UnsupportedOperationException always
     */
    @Override
    public Control cloneForSpatial(Spatial cloneNode) {
        throw new UnsupportedOperationException();
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
        assert subtree.getParent() == (enabled ? spatial : null);
    }

    /**
     * Enable or disable this control.
     * <p>
     * Disabling the control immediately removes the subtree (if any) from the
     * controlled node (if any). Enabling the control immediately adds the
     * subtree to the node.
     *
     * @param newState true&rarr;enable the control, false&rarr;disable it
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
     * Alter which node is controlled. Invoked when the control is added to or
     * removed from a node. Should be invoked only by a subclass or from
     * Spatial. Do not invoke directly from user code.
     *
     * @param newSpatial the node to control, or null to remove
     */
    @Override
    public void setSpatial(Spatial newSpatial) {
        if (newSpatial != null && !(newSpatial instanceof Node)) {
            throw new IllegalArgumentException(
                    "Controlled spatial must be a Node or null.");
        }

        if (subtree != null && subtree.getParent() != newSpatial) {
            subtree.removeFromParent();
            if (enabled && newSpatial != null) {
                Node newNode = (Node) newSpatial;
                newNode.attachChild(subtree);
            }
        }

        super.setSpatial(newSpatial);
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

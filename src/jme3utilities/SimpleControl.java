/*
 Copyright (c) 2013, Stephen Gold
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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.io.IOException;

/**
 * A simplified abstract control.
 *
 * Although this is an abstract class, it implements all its required methods in
 * order to simplify the development of subclasses -- unlike AbstractControl.
 *
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class SimpleControl
        extends AbstractControl {
    // *************************************************************************
    // new public methods

    /**
     * Traverse the node's subtree in depth-first order.
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
     * Callback to perform any rendering this control requires.
     *
     * Does nothing. Meant to be overridden.
     *
     * @param renderManager
     * @param viewPort
     */
    @Override
    protected void controlRender(RenderManager renderManager,
            ViewPort viewPort) {
        /* no rendering required */
    }

    /**
     * Callback to update this control. (Invoked once per frame.)
     *
     * Does nothing. Meant to be overridden.
     *
     * @param simInterval seconds since the previous update (>=0)
     */
    @Override
    protected void controlUpdate(float simInterval) {
        /* no updating required */
    }

    /**
     * De-serialize this control when loading from a .jm3o file.
     *
     * @param importer (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Serialize this control when saving to a .jm3o file.
     *
     * @param exporter (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    // *************************************************************************
    // Control methods

    /**
     * Clone this control for a different spatial.
     *
     * @param spatial which spatial to clone for (not null)
     * @return a new control
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
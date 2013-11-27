// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
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
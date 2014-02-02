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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.SimpleControl;

/**
 * A simple control to provide visible coordinate axes for a Node.
 * <p>
 * The controlled spatial must be a Node.
 * <p>
 * The control is disabled by default. When enabled, it attaches three
 * geometries (one for each arrow) to the controlled spatial.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class AxesControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * color of the X-axis when visible.
     */
    final private static ColorRGBA xColor = ColorRGBA.Red;
    /**
     * color of the Y-axis when visible.
     */
    final private static ColorRGBA yColor = ColorRGBA.Green;
    /**
     * color of the Z-axis when visible
     */
    final private static ColorRGBA zColor = ColorRGBA.Blue;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(AxesControl.class.getName());
    // *************************************************************************
    // fields
    /**
     * the application's asset manager: set by constructor
     */
    final private AssetManager assetManager;
    /**
     * length of each axis (in local units, >0): set by constructor
     */
    final private float length;
    /**
     * width of each axis indicator (in pixels, >0): set by constructor
     */
    final private float thickness;
    /**
     * geometry which represents the X-axis
     */
    final private Geometry xAxis;
    /**
     * geometry which represents the Y-axis
     */
    final private Geometry yAxis;
    /**
     * geometry which represents the Z-axis
     */
    final private Geometry zAxis;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a set of hidden coordinate axes.
     *
     * @param assetManager for loading material definitions (not null)
     * @param length length of each axis (in local units, >0)
     * @param width width of each axis indicator (in pixels, >0)
     */
    public AxesControl(AssetManager assetManager, float length, float width) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        this.assetManager = assetManager;

        if (!(length > 0f)) {
            logger.log(Level.SEVERE, "length={0}", length);
            throw new IllegalArgumentException("length should be positive");
        }
        this.length = length;

        if (!(width > 0f)) {
            logger.log(Level.SEVERE, "width={0}", length);
            throw new IllegalArgumentException("width should be positive");
        }
        this.thickness = width;

        xAxis = createAxis(xColor, "xAxis", Vector3f.UNIT_X);
        yAxis = createAxis(yColor, "yAxis", Vector3f.UNIT_Y);
        zAxis = createAxis(zColor, "zAxis", Vector3f.UNIT_Z);

        super.setEnabled(false);
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Alter the visibility of these coordinate axes. Assumes that the control
     * has been added to a node.
     *
     * @param newState if true, reveal the axes; if false, hide them
     */
    @Override
    public void setEnabled(boolean newState) {
        if (spatial == null) {
            throw new IllegalStateException(
                    "control should be added to a node");
        }

        Node node = (Node) spatial;
        if (!enabled && newState) {
            node.attachChild(xAxis);
            node.attachChild(yAxis);
            node.attachChild(zAxis);
        } else if (enabled && !newState) {
            node.detachChild(xAxis);
            node.detachChild(yAxis);
            node.detachChild(zAxis);
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
            node.attachChild(xAxis);
            node.attachChild(yAxis);
            node.attachChild(zAxis);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create an arrow geometry for a specific axis.
     *
     * @param color for the wireframe (not null)
     * @param name for the geometry (not null)
     * @param direction for the arrow to point (unit vector, not altered)
     */
    private Geometry createAxis(ColorRGBA color, String name,
            Vector3f direction) {
        assert color != null;
        assert name != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Material wireMaterial = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        wireMaterial.getAdditionalRenderState().setWireframe(true);
        wireMaterial.setColor("Color", color);

        Vector3f extent = direction.mult(length);
        Arrow mesh = new Arrow(extent);
        mesh.setLineWidth(thickness);
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(wireMaterial);

        return geometry;
    }
}
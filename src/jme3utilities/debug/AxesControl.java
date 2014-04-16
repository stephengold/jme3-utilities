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
import com.jme3.scene.debug.Arrow;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to visualize the coordinate axes of a Node.
 * <p>
 * The controlled spatial must be a Node.
 * <p>
 * The control is disabled by default. When enabled, it attaches three
 * geometries (one for each arrow) to the scene graph.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class AxesControl
        extends SubtreeControl {
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
     * length of each axis (in local units, &gt;0): set by constructor
     */
    final private float length;
    /**
     * width of each axis indicator (in pixels, &gt;0): set by constructor
     */
    final private float thickness;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a set of hidden coordinate axes.
     *
     * @param assetManager for loading material definitions (not null)
     * @param length length of each axis (in local units, &gt;0)
     * @param thickness thickness of each axis indicator (in pixels, &gt;0)
     */
    public AxesControl(AssetManager assetManager, float length,
            float thickness) {
        super();
        Validate.nonNull(assetManager, "asset manager");
        Validate.positive(length, "length");
        Validate.positive(thickness, "thickness");

        this.length = length;
        this.thickness = thickness;

        subtree = new Node("axes node");
        createAxis(assetManager, xColor, "xAxis", Vector3f.UNIT_X);
        createAxis(assetManager, yColor, "yAxis", Vector3f.UNIT_Y);
        createAxis(assetManager, zColor, "zAxis", Vector3f.UNIT_Z);
    }
    // *************************************************************************
    // private methods

    /**
     * Create an arrow geometry to represent a particular axis.
     *
     * @param assetManager for loading material definitions (not null)
     * @param color for the wireframe (not null)
     * @param name for the geometry (not null)
     * @param direction for the arrow to point (unit vector, unaffected)
     */
    private Geometry createAxis(AssetManager assetManager, ColorRGBA color,
            String name, Vector3f direction) {
        assert assetManager != null;
        assert color != null;
        assert name != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Vector3f extent = direction.mult(length);
        Arrow mesh = new Arrow(extent);
        mesh.setLineWidth(thickness);
        Geometry geometry = new Geometry(name, mesh);
        subtree.attachChild(geometry);

        Material wireMaterial =
                MyAsset.createWireframeMaterial(assetManager, color);
        geometry.setMaterial(wireMaterial);

        return geometry;
    }
}
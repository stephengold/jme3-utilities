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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
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
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesControl extends SubtreeControl {
    // *************************************************************************
    // constants and loggers

    /**
     * color of the X-axis
     */
    final private static ColorRGBA xColor = ColorRGBA.Red;
    /**
     * color of the Y-axis
     */
    final private static ColorRGBA yColor = ColorRGBA.Green;
    /**
     * color of the Z-axis
     */
    final private static ColorRGBA zColor = ColorRGBA.Blue;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AxesControl.class.getName());
    /**
     * local copy of Vector3f#UNIT_X
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of Vector3f#UNIT_Y
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    /**
     * local copy of Vector3f#UNIT_Z
     */
    final private static Vector3f zAxis = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a set of hidden coordinate axes.
     *
     * @param assetManager for loading material definitions (not null)
     * @param axisLength length of each axis (in local units, &gt;0)
     * @param lineWidth thickness of each axis indicator (in pixels, &ge;1)
     */
    public AxesControl(AssetManager assetManager, float axisLength,
            float lineWidth) {
        super();
        Validate.nonNull(assetManager, "asset manager");
        Validate.positive(axisLength, "axis length");
        Validate.inRange(lineWidth, "line width", 1f, Float.MAX_VALUE);

        subtree = new Node("axes node");
        createAxis(assetManager, xColor, "xAxis", xAxis);
        createAxis(assetManager, yColor, "yAxis", yAxis);
        createAxis(assetManager, zColor, "zAxis", zAxis);

        setAxisLength(axisLength);
        setLineWidth(lineWidth);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the length of the axes.
     *
     * @return length (in local units, &gt;0)
     */
    public float getAxisLength() {
        Vector3f localScale = spatial.getLocalScale();
        float result = localScale.x;

        assert result > 0f : result;
        return result;
    }

    /**
     * Read the line width of the visualization.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        Geometry axis = (Geometry) subtree.getChild(0);
        Material material = axis.getMaterial();
        RenderState state = material.getAdditionalRenderState();
        float result = state.getLineWidth();

        assert result >= 1f : result;
        return result;
    }

    /**
     * Alter the lengths of the axes.
     *
     * @param length (in local units, &gt;0)
     */
    final public void setAxisLength(float length) {
        Validate.positive(length, "length");
        subtree.setLocalScale(length);
    }

    /**
     * Alter the line width of the visualization.
     *
     * @param width (in pixels, &ge;1)
     */
    final public void setLineWidth(float width) {
        Validate.inRange(width, "width", 1f, Float.MAX_VALUE);

        for (Spatial child : subtree.getChildren()) {
            Geometry axis = (Geometry) child;
            Material material = axis.getMaterial();
            RenderState state = material.getAdditionalRenderState();
            state.setLineWidth(width);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create an arrow geometry to represent a particular axis.
     *
     * @param assetManager for loading material definitions (not null)
     * @param color for the wireframe (not null)
     * @param name for the geometry (not null)
     * @param direction for the arrow to point (length=1, unaffected)
     */
    private Geometry createAxis(AssetManager assetManager, ColorRGBA color,
            String name, Vector3f direction) {
        assert assetManager != null;
        assert color != null;
        assert name != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Arrow mesh = new Arrow(direction);
        Geometry geometry = new Geometry(name, mesh);
        subtree.attachChild(geometry);

        Material wireMaterial = MyAsset.createWireframeMaterial(
                assetManager, color);
        geometry.setMaterial(wireMaterial);

        return geometry;
    }
}

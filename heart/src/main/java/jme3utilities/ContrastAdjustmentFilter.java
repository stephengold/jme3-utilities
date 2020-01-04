/*
 Copyright (c) 2017-2020, Stephen Gold
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

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A filter to adjust the contrast of a rendered scene using a power-law
 * function.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ContrastAdjustmentFilter extends Filter {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ContrastAdjustmentFilter.class.getName());
    /**
     * asset path of the material definition
     */
    final public static String assetPath
            = "MatDefs/filter/ContrastAdjustment.j3md";
    // *************************************************************************
    // fields

    /**
     * exponent of the power law (&gt;0) 1&rarr;filter has no effect,
     * &gt;1&rarr;increase contrast in bright areas, &lt;1&rarr;increase
     * contrast in dark areas
     */
    private float exponent;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a filter with the specified exponent.
     *
     * @param exponent exponent for the power law (&gt;0)
     */
    public ContrastAdjustmentFilter(float exponent) {
        super("ContrastAdjustmentFilter");
        Validate.positive(exponent, "exponent");
        setExponent(exponent);
    }
    // *************************************************************************
    // new public methods

    /**
     * Read the filter's power-law exponent.
     *
     * @return exponent (&gt;0)
     */
    public float getExponent() {
        assert exponent > 0f : exponent;
        return exponent;
    }

    /**
     * Alter the filter's power-law exponent.
     *
     * @param newExponent new value (&gt;0)
     */
    public final void setExponent(float newExponent) {
        Validate.positive(newExponent, "exponent");

        if (material != null) {
            material.setFloat("Exponent", newExponent);
        }
        exponent = newExponent;
    }
    // *************************************************************************
    // Filter methods

    /**
     * Access the material for the filter. Invoked on every frame.
     *
     * @return the pre-existing instance (not null)
     */
    @Override
    protected Material getMaterial() {
        assert material != null;
        return material;
    }

    /**
     * Initialize the filter. Invoked when the filter is added to the
     * FilterPostProcessor.
     *
     * @param assetManager asset manager (not null)
     * @param renderManager render manager (not null)
     * @param viewPort view port where the filter will be rendered (not null)
     * @param width the width of the filter (in pixels, &gt;0)
     * @param height the height of the filter (in pixels, &gt;0)
     */
    @Override
    protected void initFilter(AssetManager assetManager,
            RenderManager renderManager, ViewPort viewPort, int width,
            int height) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(renderManager, "render manager");
        Validate.nonNull(viewPort, "view port");
        Validate.positive(width, "width");
        Validate.positive(height, "height");

        material = new Material(assetManager, assetPath);
        material.setFloat("Exponent", exponent);
    }

    /**
     * De-serialize this filter, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        exponent = capsule.readFloat("exponent", 1f);
    }

    /**
     * Serialize this filter, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule oc = exporter.getCapsule(this);
        oc.write(exponent, "exponent", 1f);
    }
}

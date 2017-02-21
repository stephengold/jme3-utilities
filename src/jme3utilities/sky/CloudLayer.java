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
package jme3utilities.sky;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.ColorRGBA;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Component of SkyControl which manages a particular cloud layer.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CloudLayer
        implements Savable {
    // *************************************************************************
    // constants

    /**
     * default UV scaling factor for cloud layers
     */
    final private static float defaultScale = 1.5f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CloudLayer.class.getName());
    // *************************************************************************
    // fields

    /**
     * opacity of the layer: 0 &rarr; transparent/cloudless, 1 &rarr; maximum
     * opacity
     */
    private float opacity = 0f;
    /**
     * U-component of the initial offset: set by constructor
     */
    private float u0;
    /**
     * U-component of the standard motion (cycles per second): set by
     * constructor
     */
    private float uRate;
    /**
     * V-component of the initial offset: set by constructor
     */
    private float v0;
    /**
     * V-component of the standard motion (cycles per second): set by
     * constructor
     */
    private float vRate;
    /**
     * this layer's index within the material: set by constructor
     */
    private int layerIndex;
    /**
     * cloud material: set by constructor
     */
    private SkyMaterial material;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public CloudLayer() {
        layerIndex = -1;
        material = null;
    }

    /**
     * Instantiate a layer with a specified index for a specified material.
     *
     * @param material (not null)
     * @param layerIndex (&ge;0)
     */
    CloudLayer(SkyMaterial material, int layerIndex) {
        assert material != null;
        assert layerIndex >= 0 : layerIndex;
        assert layerIndex < material.getMaxCloudLayers() : layerIndex;

        this.layerIndex = layerIndex;
        this.material = material;
        /*
         * Default initial offset and standard movement rates.
         * Since the Earth turns westward on its axis, clouds tend to
         * move in a easterly direction, so the V-component of the
         * texture offset should increase with time.
         */
        if (MyMath.isOdd(layerIndex)) {
            u0 = 0f;
            uRate = 0.0003f;
            v0 = 0f;
            vRate = 0.001f;
        } else {
            u0 = 0.4f;
            uRate = -0.0005f;
            v0 = 0.3f;
            vRate = 0.003f;
        }

        if (layerIndex < 2) {
            material.addClouds(layerIndex);
            material.setCloudsScale(layerIndex, defaultScale);
        } else {
            clearTexture();
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Make this layer invisible.
     */
    final public void clearTexture() {
        material.addClouds(layerIndex, "Textures/skies/clouds/clear.png");
    }

    /**
     * Alter the color of this layer.
     *
     * @param newColor (not null, unaffected, alpha is ignored)
     */
    public void setColor(ColorRGBA newColor) {
        ColorRGBA layerColor = newColor.clone();
        layerColor.a = opacity;

        material.setCloudsColor(layerIndex, layerColor);
    }

    /**
     * Alter the motion of this layer.
     *
     * @param u0 U-component of the initial offset
     * @param uRate U-component of the standard motion (cycles per second)
     * @param v0 V-component of the initial offset
     * @param vRate V-component of the standard motion (cycles per second)
     */
    public void setMotion(float u0, float uRate, float v0, float vRate) {
        this.u0 = u0;
        this.uRate = uRate;
        this.v0 = v0;
        this.vRate = vRate;
    }

    /**
     * Alter the opacity of this layer.
     *
     * @param newAlpha desired opacity of the layer (&le;1, &ge;0)
     */
    public void setOpacity(float newAlpha) {
        Validate.fraction(newAlpha, "alpha");

        opacity = newAlpha;
    }

    /**
     * Change the texture and scale of this layer.
     *
     * @param assetPath asset path to the new alpha map texture (not null)
     * @param scale texture scaling factor (&gt;0, typically &le;2)
     */
    public void setTexture(String assetPath, float scale) {
        Validate.nonNull(assetPath, "path");
        Validate.positive(scale, "scale");

        material.addClouds(layerIndex, assetPath);
        material.setCloudsScale(layerIndex, scale);
    }

    /**
     * Update this layer's texture offset in the material.
     *
     * @param time (in seconds)
     */
    void updateOffset(float time) {
        float u = u0 + time * uRate;
        float v = v0 + time * vRate;
        material.setCloudsOffset(layerIndex, u, v);
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        InputCapsule capsule = importer.getCapsule(this);

        layerIndex = capsule.readInt("layerIndex", 0);
        material = (SkyMaterial) capsule.readSavable("material", null);
        opacity = capsule.readFloat("opacity", 0f);
        u0 = capsule.readFloat("u0", 0f);
        uRate = capsule.readFloat("uRate", 0f);
        v0 = capsule.readFloat("v0", 0f);
        vRate = capsule.readFloat("vRate", 0f);
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(layerIndex, "layerIndex", 0);
        capsule.write(material, "material", null);
        capsule.write(opacity, "opacity", 0f);
        capsule.write(u0, "u0", 0f);
        capsule.write(uRate, "uRate", 0f);
        capsule.write(v0, "v0", 0f);
        capsule.write(vRate, "vRate", 0f);
    }
}

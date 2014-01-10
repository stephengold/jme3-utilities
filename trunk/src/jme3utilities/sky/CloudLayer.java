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
package jme3utilities.sky;

import com.jme3.math.ColorRGBA;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMath;

/**
 * The component of SkyControl which manages a particular cloud layer.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class CloudLayer {

    /**
     * default UV scaling factor for cloud layers
     */
    final private static float defaultScale = 1.5f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(CloudLayer.class.getName());
    // *************************************************************************
    // fields
    /**
     * opacity of the layer: 0->transparent/cloudless, 1->maximum opacity
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
     * this layer's index in the material
     */
    final private int layerIndex;
    /**
     * the material
     */
    final private SkyMaterial material;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a layer with a particular index for a particular material.
     *
     * @param material (not null)
     * @param layerIndex (>=0)
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
     * @param newColor (unaffected, not null)
     */
    void setColor(ColorRGBA newColor) {
        assert newColor != null;

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
     * @param newAlpha desired opacity of the layer (<=1, >=0)
     */
    public void setOpacity(float newAlpha) {
        if (newAlpha < Constants.alphaMin || newAlpha > Constants.alphaMax) {
            logger.log(Level.SEVERE, "alpha={0}", newAlpha);
            throw new IllegalArgumentException(
                    "alpha should be between 0 and 1, inclusive");
        }

        opacity = newAlpha;
    }

    /**
     * Change the texture and scale of this layer.
     *
     * @param assetPath asset path to the new alpha map texture (not null)
     * @param scale texture scaling factor (>0, typically <=2)
     */
    public void setTexture(String assetPath, float scale) {
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }
        if (scale <= 0f) {
            logger.log(Level.SEVERE, "scale={0}", scale);
            throw new IllegalArgumentException("scale should be positive");
        }

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
}
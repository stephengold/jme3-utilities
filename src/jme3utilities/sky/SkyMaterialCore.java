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

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ImageRaster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Core fields and methods of a material for a dynamic sky dome.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyMaterialCore
        extends Material {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SkyMaterialCore.class.getName());
    /**
     * texture coordinate for hidden objects
     */
    final private static Vector2f hidden = Vector2f.ZERO;
    // *************************************************************************
    // fields
    /**
     * asset manager used to load textures and material definitions: set by
     * constructor
     */
    protected AssetManager assetManager;
    /**
     * maximum opacity of each cloud layer (&le;1, &ge;0)
     */
    private float[] cloudAlphas;
    /**
     * scale factor of each cloud layer
     */
    private float[] cloudScales;
    /**
     * image of each cloud layer
     * <p>
     * Since ImageRaster does not implement Savable, these are retained for use
     * by write().
     */
    private Image[] cloudImages;
    /**
     * cached rasterization of each cloud layer
     */
    private ImageRaster[] cloudsRaster;
    /**
     * maximum number of cloud layers (&ge;0)
     */
    protected int maxCloudLayers;
    /**
     * maximum number of astronomical objects (&ge;0)
     */
    protected int maxObjects;
    /**
     * UV offset of each cloud layer
     */
    private Vector2f[] cloudOffsets;
    /**
     * sky texture coordinates of the center of each astronomical object
     */
    private Vector2f[] objectCenters;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not use!
     */
    public SkyMaterialCore() {
        assetManager = null;
        cloudAlphas = null;
        cloudImages = null;
        cloudScales = null;
        cloudsRaster = null;
        cloudOffsets = null;
        maxCloudLayers = 0;
        maxObjects = 0;
        objectCenters = null;
    }

    /**
     * Instantiate sky material from a specified asset path. The 1st method
     * invoked should be initialize().
     *
     * @param assetManager asset manager for loading textures and material
     * definitions (not null)
     * @param assetPath pathname to the material definitions asset (not null)
     * @param maxObjects number of astronomical objects allowed (&ge;0)
     * @param maxCloudLayers number of cloud layers allowed (&ge;0)
     */
    public SkyMaterialCore(AssetManager assetManager, String assetPath,
            int maxObjects, int maxCloudLayers) {
        super(assetManager, assetPath);
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNegative(maxObjects, "limit");
        Validate.nonNegative(maxCloudLayers, "limit");

        this.assetManager = assetManager;
        this.maxObjects = maxObjects;
        this.maxCloudLayers = maxCloudLayers;

        cloudAlphas = new float[maxCloudLayers];
        cloudImages = new Image[maxCloudLayers];
        cloudOffsets = new Vector2f[maxCloudLayers];
        cloudsRaster = new ImageRaster[maxCloudLayers];
        cloudScales = new float[maxCloudLayers];
        objectCenters = new Vector2f[maxObjects];
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a cloud layer to this material using the specified alpha map asset
     * path.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param assetPath asset path to the alpha map (not null)
     */
    public void addClouds(int layerIndex, String assetPath) {
        validateLayerIndex(layerIndex);
        Validate.nonNull(assetPath, "path");

        boolean firstTime = (cloudsRaster[layerIndex] == null);

        Texture alphaMap = MyAsset.loadTexture(assetManager, assetPath);
        alphaMap.setWrap(Texture.WrapMode.Repeat);
        String parameterName = String.format("Clouds%dAlphaMap", layerIndex);
        setTexture(parameterName, alphaMap);

        Image image = alphaMap.getImage();
        cloudImages[layerIndex] = image;
        cloudsRaster[layerIndex] = ImageRaster.create(image);

        if (firstTime) {
            cloudOffsets[layerIndex] = new Vector2f();
            setCloudsColor(layerIndex, ColorRGBA.White);
            setCloudsOffset(layerIndex, 0f, 0f);
            setCloudsScale(layerIndex, 1f);
        }
    }

    /**
     * Add an astronomical object to this material using the specified color
     * map.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param colorMap color map to use (not null)
     */
    public void addObject(int objectIndex, Texture colorMap) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(colorMap, "texture");

        String parameterName = String.format("Object%dColorMap", objectIndex);
        setTexture(parameterName, colorMap);

        if (objectCenters[objectIndex] == null) {
            objectCenters[objectIndex] = new Vector2f();
            setObjectColor(objectIndex, ColorRGBA.White);
            setObjectGlow(objectIndex, ColorRGBA.Black);
            setObjectTransform(objectIndex, Constants.topUV, 1f, null);
        }
    }

    /**
     * Estimate how much of an object's light is transmitted through the clouds.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return fraction of light transmitted (&lt;1, &ge;0)
     */
    public float getTransmission(int objectIndex) {
        validateObjectIndex(objectIndex);

        Vector2f center = objectCenters[objectIndex];
        if (center == null) {
            throw new IllegalStateException("object not yet added");
        }
        float result = getTransmission(center);

        return result;
    }

    /**
     * Estimate how much light is transmitted through the clouds at the
     * specified texture coordinates.
     *
     * @param skyCoordinates (unaffected, not null)
     * @return fraction of light transmitted (&le;1, &ge;0)
     */
    public float getTransmission(Vector2f skyCoordinates) {
        Validate.nonNull(skyCoordinates, "coordinates");

        float result = 1f;
        for (int layerIndex = 0; layerIndex < maxCloudLayers; layerIndex++) {
            if (cloudsRaster[layerIndex] != null) {
                float transparency = transparency(layerIndex, skyCoordinates);
                result *= transparency;
            }
        }

        assert result >= Constants.alphaMin : result;
        assert result <= Constants.alphaMax : result;
        return result;
    }

    /**
     * Hide an astronomical object temporarily.
     * <p>
     * Use setObjectTransform() to reveal an object which has been hidden.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     */
    public void hideObject(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String objectParameterName =
                String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, hidden);
        objectCenters[objectIndex].set(hidden);

        /*
         * Scale down the object to occupies only a few pixels in texture space.
         */
        float scale = 1000f;
        String transformUParameterName =
                String.format("Object%dTransformU", objectIndex);
        setVector2(transformUParameterName, new Vector2f(scale, scale));
        String transformVParameterName =
                String.format("Object%dTransformV", objectIndex);
        setVector2(transformVParameterName, new Vector2f(scale, scale));
    }

    /**
     * Alter the color of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newColor (not null)
     */
    public void setCloudsColor(int layerIndex, ColorRGBA newColor) {
        validateLayerIndex(layerIndex);
        Validate.nonNull(newColor, "color");
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dColor", layerIndex);
        setColor(parameterName, newColor);
        cloudAlphas[layerIndex] = newColor.a;
    }

    /**
     * Alter the glow color of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newColor (not null)
     */
    public void setCloudsGlow(int layerIndex, ColorRGBA newColor) {
        validateLayerIndex(layerIndex);
        Validate.nonNull(newColor, "color");
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dGlow", layerIndex);
        setColor(parameterName, newColor);
    }

    /**
     * Alter the texture offset of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newU 1st component of the new offset
     * @param newV 2nd component of the new offset
     */
    public void setCloudsOffset(int layerIndex, float newU, float newV) {
        validateLayerIndex(layerIndex);
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        float uOffset = MyMath.modulo(newU, 1f);
        float vOffset = MyMath.modulo(newV, 1f);
        Vector2f offset = new Vector2f(uOffset, vOffset);

        String parameterName = String.format("Clouds%dOffset", layerIndex);
        setVector2(parameterName, offset);
        cloudOffsets[layerIndex].set(offset);
    }

    /**
     * Alter the texture scale of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newScale (&gt;0)
     */
    public void setCloudsScale(int layerIndex, float newScale) {
        validateLayerIndex(layerIndex);
        Validate.positive(newScale, "scale");
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dScale", layerIndex);
        setFloat(parameterName, newScale);
        cloudScales[layerIndex] = newScale;
    }

    /**
     * Alter the color of an astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param newColor (not null)
     */
    public void setObjectColor(int objectIndex, ColorRGBA newColor) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(newColor, "color");
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dColor", objectIndex);
        setColor(parameterName, newColor);
    }

    /**
     * Alter the glow color of an astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param newColor (not null)
     */
    public void setObjectGlow(int objectIndex, ColorRGBA newColor) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(newColor, "color");
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dGlow", objectIndex);
        setColor(parameterName, newColor);
    }

    /**
     * Alter the location and scaling of an astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param centerUV sky texture coordinates for the center of the object (not
     * null, each component &le;1 and &ge;0, unaffected)
     * @param newScale ratio of the sky's texture scale to that of the object
     * (&ge;0, usually &lt;1)
     * @param newRotate (cos, sin) of clockwise rotation angle (or null if
     * rotation doesn't matter)
     */
    public void setObjectTransform(int objectIndex, Vector2f centerUV,
            float newScale, Vector2f newRotate) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(centerUV, "coordinates");
        Validate.positive(newScale, "scale");
        if (newRotate != null) {
            if (!MyMath.isUnitVector(newRotate)) {
                logger.log(Level.SEVERE, "newRotate={0}", newRotate);
                throw new IllegalArgumentException(
                        "rotation should have length=1");
            }
        }
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String objectParameterName =
                String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, centerUV);
        objectCenters[objectIndex].set(centerUV);

        Vector2f offset = centerUV.subtract(Constants.topUV);
        float topDist = offset.length();
        /*
         * The texture coordinate transforms are broken into pairs of
         * vectors because there is no Matrix2f class.
         */
        Vector2f transformU = new Vector2f();
        Vector2f transformV = new Vector2f();
        Vector2f tU = new Vector2f();
        Vector2f tV = new Vector2f();

        if (topDist > 0f) {
            /*
             * Stretch the image horizontally to compensate for UV distortion
             * near the horizon.
             */
            float a = offset.x / topDist;
            float b = offset.y / topDist;
            tU.set(b, -a);
            tV.set(a, b);

            float stretchFactor = 1f
                    + Constants.stretchCoefficient * topDist * topDist;
            tU.divideLocal(stretchFactor);

            if (newRotate != null) {
                transformU.set(tU.x * b + tV.x * a, tU.y * b + tV.y * a);
                transformV.set(tV.x * b - tU.x * a, tV.y * b - tU.y * a);
            } else {
                transformU.set(tU);
                transformV.set(tV);
            }

        } else {
            /*
             * No UV distortion at the top of the dome.
             */
            transformU.set(1f, 0f);
            transformV.set(0f, 1f);
        }

        if (newRotate != null) {
            /*
             * Rotate so top is toward the north horizon.
             */
            tU.set(transformV);
            tV.set(-transformU.x, -transformU.y);
            /*
             * Rotate by newRotate.
             */
            transformU.set(tU.x * newRotate.x + tV.x * newRotate.y,
                    tU.y * newRotate.x + tV.y * newRotate.y);
            transformV.set(tV.x * newRotate.x - tU.x * newRotate.y,
                    tV.y * newRotate.x - tU.y * newRotate.y);
        }
        /*
         * Scale by newScale.
         */
        transformU.divideLocal(newScale);
        transformV.divideLocal(newScale);

        String transformUParameterName =
                String.format("Object%dTransformU", objectIndex);
        setVector2(transformUParameterName, transformU);

        String transformVParameterName =
                String.format("Object%dTransformV", objectIndex);
        setVector2(transformVParameterName, transformV);
    }
    // *************************************************************************
    // protected methods

    /**
     * Validate a layer index.
     *
     * @param layerIndex the index of a cloud layer
     * @throws IllegalArgumentException if the index is out of range
     */
    protected void validateLayerIndex(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            logger.log(Level.SEVERE, "layerIndex={0}, maxCloudLayers={1}",
                    new Object[]{layerIndex, maxCloudLayers});
            throw new IllegalArgumentException(
                    "cloud layer index out of range");
        }
    }

    /**
     * Validate an object index.
     *
     * @param objectIndex the index of an astronomical object
     * @throws IllegalArgumentException if the index is out of range
     */
    protected void validateObjectIndex(int objectIndex) {
        if (objectIndex < 0 || objectIndex >= maxObjects) {
            logger.log(Level.SEVERE, "objectIndex={0}, maxObjects={1}",
                    new Object[]{objectIndex, maxObjects});
            throw new IllegalArgumentException("object index out of range");
        }
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     * @throws IOException
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);

        cloudAlphas = capsule.readFloatArray("cloudAlphas", null);

        Savable[] sav = capsule.readSavableArray("cloudImages", null);
        cloudImages = new Image[sav.length];
        System.arraycopy(sav, 0, cloudImages, 0, sav.length);

        sav = capsule.readSavableArray("cloudOffsets", null);
        cloudOffsets = new Vector2f[sav.length];
        System.arraycopy(sav, 0, cloudOffsets, 0, sav.length);

        cloudScales = capsule.readFloatArray("cloudScales", null);

        sav = capsule.readSavableArray("objectCenters", null);
        objectCenters = new Vector2f[sav.length];
        System.arraycopy(sav, 0, objectCenters, 0, sav.length);
        /*
         * cached values
         */
        assetManager = importer.getAssetManager();
        maxCloudLayers = cloudImages.length;
        maxObjects = objectCenters.length;

        cloudsRaster = new ImageRaster[maxCloudLayers];
        for (int layerIndex = 0; layerIndex < maxCloudLayers; layerIndex++) {
            Image image = cloudImages[layerIndex];
            if (image == null) {
                cloudsRaster[layerIndex] = null;
            } else {
                cloudsRaster[layerIndex] = ImageRaster.create(image);
            }
        }
    }

    /**
     * Serialize this instance when saving.
     *
     * @param exporter (not null)
     * @throws IOException
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(cloudAlphas, "cloudAlphas", null);
        capsule.write(cloudImages, "cloudImages", null);
        capsule.write(cloudOffsets, "cloudOffsets", null);
        capsule.write(cloudScales, "cloudScales", null);
        capsule.write(objectCenters, "objectCenters", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Estimate how much light is transmitted through an indexed cloud layer at
     * the specified texture coordinates.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param skyCoordinates (unaffected, not null)
     * @return fraction of light transmitted (&le;1, &ge;0)
     */
    private float transparency(int layerIndex, Vector2f skyCoordinates) {
        assert layerIndex >= 0 : layerIndex;
        assert layerIndex < maxCloudLayers : layerIndex;
        assert skyCoordinates != null;
        assert cloudsRaster[layerIndex] != null : layerIndex;

        Vector2f coord = skyCoordinates.mult(cloudScales[layerIndex]);
        coord.addLocal(cloudOffsets[layerIndex]);
        coord.x = MyMath.modulo(coord.x, Constants.uvMax);
        coord.y = MyMath.modulo(coord.y, Constants.uvMax);
        float opacity = sampleRed(cloudsRaster[layerIndex], coord);
        opacity *= cloudAlphas[layerIndex];
        float result = Constants.alphaMax - opacity;

        assert result >= Constants.alphaMin : result;
        assert result <= Constants.alphaMax : result;
        return result;
    }

    /**
     * Sample the red component of a rasterized texture at the specified
     * coordinates.
     *
     * @param colorImage the texture to sample (not null, unaffected)
     * @param uv texture coordinates to sample (not null, each component &lt;1
     * and &ge;0, unaffected)
     * @return red intensity (&le;1, &ge;0)
     */
    private float sampleRed(ImageRaster colorImage, Vector2f uv) {
        assert colorImage != null;
        assert uv != null;
        float u = uv.x;
        float v = uv.y;
        assert u >= Constants.uvMin : uv;
        assert u < Constants.uvMax : uv;
        assert v >= Constants.uvMin : uv;
        assert v < Constants.uvMax : uv;

        int width = colorImage.getWidth();
        float x = u * width;
        int x0 = (int) FastMath.floor(x);
        float xFraction1 = x - x0;
        float xFraction0 = 1 - xFraction1;
        int x1 = (x0 + 1) % width;

        int height = colorImage.getHeight();
        float y = v * width;
        int y0 = (int) FastMath.floor(y);
        float yFraction1 = y - y0;
        float yFraction0 = 1 - yFraction1;
        int y1 = (y0 + 1) % height;
        /*
         * Access the red values of the four nearest pixels.
         */
        float r00 = colorImage.getPixel(x0, y0).r;
        float r01 = colorImage.getPixel(x1, y0).r;
        float r10 = colorImage.getPixel(x0, y1).r;
        float r11 = colorImage.getPixel(x1, y1).r;
        /*
         * Sample using bidirectional linear interpolation.
         */
        float result = r00 * xFraction0 * yFraction0
                + r01 * xFraction0 * yFraction1
                + r10 * xFraction1 * yFraction0
                + r11 * xFraction1 * yFraction1;

        assert result >= Constants.alphaMin : result;
        assert result <= Constants.alphaMax : result;
        return result;
    }
}
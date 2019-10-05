/*
 Copyright (c) 2014-2019, Stephen Gold
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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ImageRaster;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Core fields and methods of a material for a dynamic sky dome.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkyMaterialCore extends Material {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkyMaterialCore.class.getName());
    /**
     * special texture coordinates for hidden objects
     */
    final private static Vector2f hidden = new Vector2f(0f, 0f);
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
     * scale factors of cloud layers (each &gt;0)
     */
    private float[] cloudScales;
    /**
     * scale factors of astronomical objects (each &gt;0)
     */
    private float[] objectScales;
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
    /**
     * rotation vectors of astronomical objects (each may be null)
     */
    private Vector2f[] objectRotations;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected SkyMaterialCore() {
        assetManager = null;
        cloudAlphas = null;
        cloudImages = null;
        cloudScales = null;
        cloudsRaster = null;
        cloudOffsets = null;
        maxCloudLayers = 0;
        maxObjects = 0;

        objectCenters = null;
        objectRotations = null;
        objectScales = null;
    }

    /**
     * Instantiate sky material from a specified asset path. The first method
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
        objectRotations = new Vector2f[maxObjects];
        objectScales = new float[maxObjects];
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a cloud layer to this material using the specified alpha map asset
     * path.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param assetPath the asset path to the alpha map (not null, not empty)
     */
    public void addClouds(int layerIndex, String assetPath) {
        validateLayerIndex(layerIndex);
        Validate.nonEmpty(assetPath, "asset path");

        boolean firstTime = (cloudsRaster[layerIndex] == null);

        boolean mipmaps = false;
        Texture alphaMap
                = MyAsset.loadTexture(assetManager, assetPath, mipmaps);
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
     * map. If the object already exists, its color map is updated.
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
            objectRotations[objectIndex] = new Vector2f();
            setObjectColor(objectIndex, ColorRGBA.White);
            setObjectGlow(objectIndex, ColorRGBA.Black);
            setObjectTransform(objectIndex, Constants.topUV, 1f, null);
        }
    }

    /**
     * Copy the color of the specified cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @return a new instance
     * @see #setCloudsColor(int, com.jme3.math.ColorRGBA)
     */
    public ColorRGBA copyCloudsColor(int layerIndex) {
        validateLayerIndex(layerIndex);
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dColor", layerIndex);
        ColorRGBA color = copyColor(parameterName);
        color.a = cloudAlphas[layerIndex];

        return color;
    }

    /**
     * Copy the glow color of the specified cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @return a new instance
     * @see #setCloudsGlow(int, com.jme3.math.ColorRGBA)
     */
    public ColorRGBA copyCloudsGlow(int layerIndex) {
        validateLayerIndex(layerIndex);
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dGlow", layerIndex);
        ColorRGBA color = copyColor(parameterName);

        return color;
    }

    /**
     * Copy the texture offset of the specified cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @return a new instance
     * @see #setCloudsOffset(int, float, float)
     */
    public Vector2f copyCloudsOffset(int layerIndex) {
        validateLayerIndex(layerIndex);
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        Vector2f offset = cloudOffsets[layerIndex];
        return offset.clone();
    }

    /**
     * Copy the value of the specified color parameter.
     *
     * @param name name of the color parameter
     * @return a new instance
     * @see com.jme3.material.Material#setColor(java.lang.String,
     * com.jme3.math.ColorRGBA)
     */
    public ColorRGBA copyColor(String name) {
        MatParam parameter = getParam(name);
        ColorRGBA color = (ColorRGBA) parameter.getValue();

        return color.clone();
    }

    /**
     * Copy the color of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return a new instance
     * @see #setObjectColor(int, com.jme3.math.ColorRGBA)
     */
    public ColorRGBA copyObjectColor(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dColor", objectIndex);
        ColorRGBA color = copyColor(parameterName);

        return color;
    }

    /**
     * Copy the glow color of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return a new instance
     * @see #setObjectGlow(int, com.jme3.math.ColorRGBA)
     */
    public ColorRGBA copyObjectGlow(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dGlow", objectIndex);
        ColorRGBA color = copyColor(parameterName);

        return color;
    }

    /**
     * Copy the texture offset of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return a new instance
     * @see #setObjectTransform(int, com.jme3.math.Vector2f, float,
     * com.jme3.math.Vector2f)
     */
    public Vector2f copyObjectOffset(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        Vector2f offset = objectCenters[objectIndex];
        return offset.clone();
    }

    /**
     * Copy the texture rotation vector of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return a new instance
     * @see #setObjectTransform(int, com.jme3.math.Vector2f, float,
     * com.jme3.math.Vector2f)
     */
    public Vector2f copyObjectRotation(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        Vector2f vector = objectRotations[objectIndex];
        if (vector == null) {
            return null;
        } else {
            return vector.clone();
        }
    }

    /**
     * Copy the value of the specified vector2 parameter.
     *
     * @param name name of the parameter
     * @return a new instance
     * @see com.jme3.material.Material#setVector2(java.lang.String,
     * com.jme3.math.Vector2f)
     */
    public Vector2f copyVector2(String name) {
        MatParam parameter = getParam(name);
        Vector2f vector = (Vector2f) parameter.getValue();

        return vector.clone();
    }

    /**
     * Read the scale of the specified cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @return scale factor (&gt;0)
     */
    public float getCloudsScale(int layerIndex) {
        validateLayerIndex(layerIndex);
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dScale", layerIndex);
        float result = getFloat(parameterName);

        assert result > 0f : result;
        return result;
    }

    /**
     * Read the scale of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @return scale factor (&gt;0)
     * @see #setObjectTransform(int, com.jme3.math.Vector2f, float,
     * com.jme3.math.Vector2f)
     */
    public float getObjectScale(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        float result = objectScales[objectIndex];

        assert result > 0f : result;
        return result;
    }

    /**
     * Read the value of the specified float parameter.
     *
     * @param name name of the parameter
     * @return value
     * @see com.jme3.material.Material#setFloat(java.lang.String, float)
     */
    public float getFloat(String name) {
        MatParam parameter = getParam(name);
        float result = (float) parameter.getValue();

        return result;
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
     * Use
     * {@link #setObjectTransform(int, com.jme3.math.Vector2f, float, com.jme3.math.Vector2f)}
     * to reveal an object that has been hidden.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     */
    public void hideObject(int objectIndex) {
        validateObjectIndex(objectIndex);
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String objectParameterName
                = String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, hidden);
        objectCenters[objectIndex].set(hidden);

        /*
         * Scale down the object to occupy only a few pixels in texture space.
         */
        float scale = 1000f;
        String transformUParameterName
                = String.format("Object%dTransformU", objectIndex);
        setVector2(transformUParameterName, new Vector2f(scale, scale));
        String transformVParameterName
                = String.format("Object%dTransformV", objectIndex);
        setVector2(transformVParameterName, new Vector2f(scale, scale));
    }

    /**
     * Alter the color of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setCloudsColor(int layerIndex, ColorRGBA newColor) {
        validateLayerIndex(layerIndex);
        Validate.nonNull(newColor, "color");
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dColor", layerIndex);
        setColor(parameterName, newColor.clone());
        cloudAlphas[layerIndex] = newColor.a;
    }

    /**
     * Alter the glow color of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setCloudsGlow(int layerIndex, ColorRGBA newColor) {
        validateLayerIndex(layerIndex);
        Validate.nonNull(newColor, "color");
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dGlow", layerIndex);
        setColor(parameterName, newColor.clone());
    }

    /**
     * Alter the texture offset of a cloud layer.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     * @param newU first component of the new offset
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
     * @param newScale scale factor (&gt;0)
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
     * Alter the color of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setObjectColor(int objectIndex, ColorRGBA newColor) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(newColor, "color");
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dColor", objectIndex);
        setColor(parameterName, newColor.clone());
    }

    /**
     * Alter the glow color of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param newColor (not null, unaffected)
     */
    public void setObjectGlow(int objectIndex, ColorRGBA newColor) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(newColor, "color");
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dGlow", objectIndex);
        setColor(parameterName, newColor.clone());
    }

    /**
     * Alter the position and scaling of the specified astronomical object.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param centerUV sky texture coordinates for the center of the object (not
     * null, each component &le;1 and &ge;0, unaffected)
     * @param newScale ratio of the sky's texture scale to that of the object
     * (&ge;0, usually &lt;1)
     * @param newRotate (cos, sin) of clockwise rotation angle (length&gt;0,
     * unaffected) or null if rotation doesn't matter
     */
    public void setObjectTransform(int objectIndex, Vector2f centerUV,
            float newScale, Vector2f newRotate) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(centerUV, "coordinates");
        Validate.positive(newScale, "scale");
        if (newRotate != null) {
            Validate.nonZero(newRotate, "rotation vector");
        }
        if (objectCenters[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }
        /*
         * Record transform parameters for save().
         */
        objectCenters[objectIndex] = centerUV.clone();
        if (newRotate == null) {
            objectRotations[objectIndex] = null;
        } else {
            objectRotations[objectIndex] = newRotate.clone();
        }
        objectScales[objectIndex] = newScale;

        String objectParameterName
                = String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, centerUV);

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
            Vector2f norm = newRotate.normalize();
            transformU.set(tU.x * norm.x + tV.x * norm.y,
                    tU.y * norm.x + tV.y * norm.y);
            transformV.set(tV.x * norm.x - tU.x * norm.y,
                    tV.y * norm.x - tU.y * norm.y);
        }
        /*
         * Scale by newScale.
         */
        transformU.divideLocal(newScale);
        transformV.divideLocal(newScale);

        String transformUParameterName
                = String.format("Object%dTransformU", objectIndex);
        setVector2(transformUParameterName, transformU);

        String transformVParameterName
                = String.format("Object%dTransformV", objectIndex);
        setVector2(transformVParameterName, transformV);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Validate a cloud layer index used as a method argument.
     *
     * @param layerIndex the index of a cloud layer
     * @throws IllegalArgumentException if the index is out of range
     */
    protected void validateLayerIndex(int layerIndex) {
        Validate.inRange(layerIndex, "cloud layer index",
                0, maxCloudLayers - 1);
    }

    /**
     * Validate an object index used as a method argument.
     *
     * @param objectIndex the index of an astronomical object
     * @throws IllegalArgumentException if the index is out of range
     */
    protected void validateObjectIndex(int objectIndex) {
        Validate.inRange(objectIndex, "object index",
                0, maxObjects - 1);
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        /*
         * cloud layers
         */
        cloudAlphas = capsule.readFloatArray("cloudAlphas", null);

        Savable[] sav = capsule.readSavableArray("cloudImages", null);
        cloudImages = new Image[sav.length];
        System.arraycopy(sav, 0, cloudImages, 0, sav.length);

        sav = capsule.readSavableArray("cloudOffsets", null);
        cloudOffsets = new Vector2f[sav.length];
        System.arraycopy(sav, 0, cloudOffsets, 0, sav.length);

        cloudScales = capsule.readFloatArray("cloudScales", null);
        /*
         * astronomical objects
         */
        sav = capsule.readSavableArray("objectCenters", null);
        objectCenters = new Vector2f[sav.length];
        System.arraycopy(sav, 0, objectCenters, 0, sav.length);

        sav = capsule.readSavableArray("objectRotations", null);
        objectRotations = new Vector2f[sav.length];
        System.arraycopy(sav, 0, objectRotations, 0, sav.length);

        objectScales = capsule.readFloatArray("objectScales", null);
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
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(cloudAlphas, "cloudAlphas", null);
        capsule.write(cloudImages, "cloudImages", null);
        capsule.write(cloudOffsets, "cloudOffsets", null);
        capsule.write(cloudScales, "cloudScales", null);

        capsule.write(objectCenters, "objectCenters", null);
        capsule.write(objectRotations, "objectRotations", null);
        capsule.write(objectScales, "objectScales", null);
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

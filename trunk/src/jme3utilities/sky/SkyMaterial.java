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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.image.ImageRaster;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MyMath;

/**
 * A material for a dynamic sky dome using assets and techniques derived from
 * http://code.google.com/p/jmonkeyplatform-contributions/source/browse/trunk/SkyDome
 *
 * The color of clear sky may be adjusted by invoking setClearColor().
 *
 * Stars can be added to the material by invoking addStars().
 *
 * Up to two astronomical objects can be added to the material by invoking
 * addObject(); once added, their positions, sizes, and colors may be adjusted
 * by invoking setObjectTransform() and setObjectColor().
 *
 * Up to two layers of clouds can be added to the material by invoking
 * addClouds(); once added, their positions, sizes, and colors may be adjusted
 * by invoking setCloudsOffset(), setCloudsScale(), and setCloudsColor().
 *
 * Horizon haze can be added to the material by invoking addHaze(); once added,
 * its color may be adjusted by invoking setHazeColor().
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyMaterial
        extends Material {
    // *************************************************************************
    // constants

    /**
     * maximum value for opacity
     */
    final private static float alphaMax = 1f;
    /**
     * default color of clear sky: pale blue
     */
    final private static ColorRGBA defaultClearColor =
            new ColorRGBA(0.4f, 0.6f, 1f, alphaMax);
    /**
     * minimum value for opacity
     */
    final private static float alphaMin = 0f;
    /**
     * 1st (U) texture coordinate of the zenith
     */
    final static float topU = 0.5f;
    /**
     * 2nd (V) texture coordinate of the zenith
     */
    final static float topV = 0.5f;
    /**
     * maximum value for texture coordinates which do not wrap
     */
    final private static float uvMax = 1f;
    /**
     * minimum value for texture coordinates which do not wrap
     */
    final private static float uvMin = 0f;
    /**
     * UV distance from zenith to horizon (<0.5, >0)
     */
    final static float uvScale = 0.44f;
    /**
     * coefficient used to compute stretchFactor
     */
    final private static float stretchCoefficient =
            (FastMath.HALF_PI - uvMax) / (uvScale * uvScale);
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SkyMaterial.class.getName());
    /**
     * asset path to the default cloud alpha map
     */
    final private static String cloudsMapPath =
            "Textures/skies/t0neg0d/Clouds_L.png";
    /**
     * asset path to the default alpha map for horizon haze
     */
    final private static String hazeMapPath = "Textures/skies/ramps/haze.png";
    /**
     * asset path to the default starry night sky color map
     */
    final private static String starsMapPath =
            "Textures/skies/star-maps/wiltshire.png";
    /**
     * asset path to a color map for the sun
     */
    final public static String sunMapPath = "Textures/skies/t0neg0d/Sun_L.png";
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
    private AssetManager assetManager;
    /**
     * maximum opacity of each cloud layer (<=1, >=0)
     */
    private float[] cloudsAlpha;
    /**
     * scale factor of each cloud layer
     */
    private float[] cloudsScale;
    /**
     * image of each cloud layer
     *
     * Since ImageRaster does not implement Savable, these are retained for use
     * by write().
     */
    private Image[] cloudsImage;
    /**
     * cached rasterization of each cloud layer
     */
    private ImageRaster[] cloudsRaster;
    /**
     * maximum number of cloud layers (>=0)
     */
    private int maxCloudLayers;
    /**
     * maximum number of astronomical objects (>=0)
     */
    private int maxObjects;
    /**
     * UV offset of each cloud layer
     */
    private Vector2f[] cloudsOffset;
    /**
     * sky texture coordinates of the center of each astronomical object
     */
    private Vector2f[] objectCenter;
    // *************************************************************************
    // constructors

    /**
     * Null constructor for serialization purposes only. Do not use!
     */
    public SkyMaterial() {
        assetManager = null;
        cloudsAlpha = null;
        cloudsImage = null;
        cloudsScale = null;
        cloudsRaster = null;
        cloudsOffset = null;
        maxCloudLayers = 0;
        maxObjects = 0;
        objectCenter = null;
    }

    /**
     * Instantiate a sky material from the default definitions. The first method
     * invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     */
    public SkyMaterial(AssetManager assetManager) {
        this(assetManager, "MatDefs/skies/dome22/dome22.j3md", 2, 2);
    }

    /**
     * Instantiate a sky material from a particular asset path. The first method
     * invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param assetPath pathname to the material definitions asset (not null)
     * @param maxObjects number of astronomical objects allowed (>=0)
     * @param maxCloudLayers number of cloud layers allowed (>=0)
     */
    public SkyMaterial(AssetManager assetManager, String assetPath,
            int maxObjects, int maxCloudLayers) {
        super(assetManager, assetPath);

        this.assetManager = assetManager;
        this.maxCloudLayers = maxCloudLayers;
        this.maxObjects = maxObjects;

        cloudsAlpha = new float[maxCloudLayers];
        cloudsImage = new Image[maxCloudLayers];
        cloudsOffset = new Vector2f[maxCloudLayers];
        cloudsRaster = new ImageRaster[maxCloudLayers];
        cloudsScale = new float[maxCloudLayers];
        objectCenter = new Vector2f[maxObjects];
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a cloud layer to this material using the default alpha map.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     */
    public void addClouds(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            throw new IllegalArgumentException("layer index out of range");
        }
        addClouds(layerIndex, cloudsMapPath);
    }

    /**
     * Add a cloud layer to this material using the specified alpha map asset.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     * @param assetPath asset path to the alpha map (not null)
     */
    public void addClouds(int layerIndex, String assetPath) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            throw new IllegalArgumentException("layer index out of range");
        }
        if (assetPath == null) {
            throw new NullPointerException("path cannot be null");
        }

        boolean firstTime = (cloudsRaster[layerIndex] == null);

        Texture alphaMap = loadTextureRepeat(assetPath);
        String parameterName = String.format("Clouds%dAlphaMap", layerIndex);
        setTexture(parameterName, alphaMap);

        Image image = alphaMap.getImage();
        cloudsImage[layerIndex] = image;
        cloudsRaster[layerIndex] = ImageRaster.create(image);

        if (firstTime) {
            cloudsOffset[layerIndex] = new Vector2f();
            setCloudsColor(layerIndex, ColorRGBA.White);
            setCloudsOffset(layerIndex, 0f, 0f);
            setCloudsScale(layerIndex, 1f);
        }
    }

    /**
     * Add horizon haze to this material using the default alpha map.
     */
    public void addHaze() {
        addHaze(hazeMapPath);
    }

    /**
     * Add horizon haze to this material using the specified alpha map asset.
     *
     * @param assetPath asset path to the alpha map (not null)
     */
    public void addHaze(String assetPath) {
        if (assetPath == null) {
            throw new NullPointerException("path cannot be null");
        }

        Texture alphaMap = loadTextureClamp(assetPath);
        setTexture("HazeAlphaMap", alphaMap);
        setHazeColor(ColorRGBA.White);
    }

    /**
     * Add an astronomical object to this material using the specified color map
     * asset.
     *
     * @param objectIndex (<maxObjects, >=0)
     * @param assetPath asset path to the color map (not null)
     */
    public void addObject(int objectIndex, String assetPath) {
        if (objectIndex < 0 || objectIndex >= maxObjects) {
            throw new IllegalArgumentException("object index out of range");
        }
        if (assetPath == null) {
            throw new NullPointerException("path cannot be null");
        }

        Texture objectMap = loadTextureClamp(assetPath);
        String parameterName = String.format("Object%dColorMap", objectIndex);
        setTexture(parameterName, objectMap);

        if (objectCenter[objectIndex] == null) {
            objectCenter[objectIndex] = new Vector2f();
            setObjectColor(objectIndex, ColorRGBA.White);
            setObjectTransform(objectIndex, new Vector2f(topU, topV), 1f, null);
        }
    }

    /**
     * Add stars to this material using the default color map.
     */
    public void addStars() {
        addStars(starsMapPath);
    }

    /**
     * Add stars to this material using the specified color map asset.
     *
     * @param assetPath (not null)
     */
    public void addStars(String assetPath) {
        if (assetPath == null) {
            throw new NullPointerException("path cannot be null");
        }

        Texture colorMap = loadTextureClamp(assetPath);
        setTexture("StarsColorMap", colorMap);
    }

    /**
     * Read the upper limit on the number of cloud layers.
     */
    public int getMaxCloudLayers() {
        return maxCloudLayers;
    }

    /**
     * Read the upper limit on the number of astronomical objects.
     */
    public int getMaxObjects() {
        return maxObjects;
    }

    /**
     * Estimate how much of an object's light is transmitted through the clouds.
     *
     * @param objectId (<maxObjects, >=0)
     * @return fraction of light transmitted (<=1, >=0)
     */
    public float getTransmission(int objectId) {
        if (objectId < 0 || objectId >= maxObjects) {
            throw new IllegalArgumentException("object id out of range");
        }

        Vector2f center = objectCenter[objectId];
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
     * @return fraction of light transmitted (<=1, >=0)
     */
    public float getTransmission(Vector2f skyCoordinates) {
        if (skyCoordinates == null) {
            throw new NullPointerException("coordinates cannot be null");
        }

        float result = 1f;
        for (int layerIndex = 0; layerIndex < maxCloudLayers; layerIndex++) {
            if (cloudsRaster[layerIndex] != null) {
                float transparency =
                        getTransparency(layerIndex, skyCoordinates);
                result *= transparency;
            }
        }

        assert result >= alphaMin : result;
        assert result <= alphaMax : result;
        return result;
    }

    /**
     * Hide an astronomical object temporarily.
     *
     * Use setObjectTransform() to reveal an object which has been hidden.
     *
     * @param objectIndex (<maxObjects, >=0)
     */
    public void hideObject(int objectIndex) {
        if (objectIndex < 0 || objectIndex >= maxObjects) {
            throw new IllegalArgumentException("object index out of range");
        }
        if (objectCenter[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String objectParameterName =
                String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, hidden);
        objectCenter[objectIndex].set(hidden);

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
     * Initialize this material.
     */
    public void initialize() {
        setClearColor(defaultClearColor);
        setVector2("TopCoord", new Vector2f(topU, topV));
        /*
         * The default blend mode is "off".  Since this material may have
         * translucent regions, specify "alpha" blending.
         */
        RenderState additional = getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
    }

    /**
     * Alter the color of clear sky.
     *
     * @param newColor (not null)
     */
    public void setClearColor(ColorRGBA newColor) {
        if (newColor == null) {
            throw new NullPointerException("color cannot be null");
        }

        setColor("ClearColor", newColor);
    }

    /**
     * Alter the color of a cloud layer.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     * @param newColor (not null)
     */
    public void setCloudsColor(int layerIndex, ColorRGBA newColor) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            throw new IllegalArgumentException("layer index out of range");
        }
        if (newColor == null) {
            throw new NullPointerException("color cannot be null");
        }
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dColor", layerIndex);
        setColor(parameterName, newColor);
        cloudsAlpha[layerIndex] = newColor.a;
    }

    /**
     * Alter the texture offset of a cloud layer.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     * @param newU 1st component of the new offset
     * @param newV 2nd component of the new offset
     */
    public void setCloudsOffset(int layerIndex, float newU, float newV) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            throw new IllegalArgumentException("layer index out of range");
        }
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        float uOffset = MyMath.modulo(newU, 1f);
        float vOffset = MyMath.modulo(newV, 1f);
        Vector2f offset = new Vector2f(uOffset, vOffset);

        String parameterName = String.format("Clouds%dOffset", layerIndex);
        setVector2(parameterName, offset);
        cloudsOffset[layerIndex].set(offset);
    }

    /**
     * Alter the texture scale of a cloud layer.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     * @param newScale (>0)
     */
    public void setCloudsScale(int layerIndex, float newScale) {
        if (layerIndex < 0 || layerIndex >= maxCloudLayers) {
            throw new IllegalArgumentException("layer index out of range");
        }
        if (newScale <= 0f) {
            throw new IllegalArgumentException("scale must be positive");
        }
        if (cloudsRaster[layerIndex] == null) {
            throw new IllegalStateException("layer not yet added");
        }

        String parameterName = String.format("Clouds%dScale", layerIndex);
        setFloat(parameterName, newScale);
        cloudsScale[layerIndex] = newScale;
    }

    /**
     * Alter the color of the horizon haze.
     *
     * @param newColor (not null)
     */
    public void setHazeColor(ColorRGBA newColor) {
        if (newColor == null) {
            throw new NullPointerException("color cannot be null");
        }

        setColor("HazeColor", newColor);
    }

    /**
     * Alter the color of an astronomical object.
     *
     * @param objectIndex (<maxObjects, >=0)
     * @param newColor (not null)
     */
    public void setObjectColor(int objectIndex, ColorRGBA newColor) {
        if (objectIndex < 0 || objectIndex >= maxObjects) {
            throw new IllegalArgumentException("object index out of range");
        }
        if (newColor == null) {
            throw new NullPointerException("color cannot be null");
        }
        if (objectCenter[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        String parameterName = String.format("Object%dColor", objectIndex);
        setColor(parameterName, newColor);
    }

    /**
     * Alter the location and scaling of an astronomical object.
     *
     * @param objectIndex (<maxObjects, >=0)
     * @param centerUV sky texture coordinates for the center of the object (not
     * null, each component <=1 and >=0, unaffected)
     * @param newScale ratio of the sky's texture scale to that of the object
     * (>0, usually <1)
     * @param newRotate (cos, sin) of clockwise rotation angle (or null if
     * rotation doesn't matter)
     */
    public void setObjectTransform(int objectIndex, Vector2f centerUV,
            float newScale, Vector2f newRotate) {
        if (objectIndex < 0 || objectIndex >= maxObjects) {
            throw new IllegalArgumentException("object index out of range");
        }
        if (centerUV == null) {
            throw new NullPointerException("coordinates cannot be null");
        }
        if (newScale <= 0f) {
            throw new IllegalArgumentException("scale must be positive");
        }
        if (newRotate != null) {
            if (FastMath.abs(newRotate.lengthSquared() - 1f) > 1e-4) {
                throw new IllegalArgumentException(
                        "rotation must be a unit vector");
            }
        }
        if (objectCenter[objectIndex] == null) {
            throw new IllegalStateException("object not yet added");
        }

        float u = centerUV.x;
        float v = centerUV.y;
        if (u > uvMax || u < uvMin || v > uvMax || v < uvMin) {
            throw new IllegalArgumentException(
                    "texture coordinates must be between 0 and 1, inclusive");
        }

        String objectParameterName =
                String.format("Object%dCenter", objectIndex);
        setVector2(objectParameterName, centerUV);
        objectCenter[objectIndex].set(centerUV);

        Vector2f offset = new Vector2f(u - topU, v - topV);
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

            float stretchFactor = 1f + stretchCoefficient * topDist * topDist;
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
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);

        cloudsAlpha = capsule.readFloatArray("cloudAlphas", null);

        Savable[] sav = capsule.readSavableArray("cloudImages", null);
        cloudsImage = new Image[sav.length];
        System.arraycopy(sav, 0, cloudsImage, 0, sav.length);

        sav = capsule.readSavableArray("cloudOffsets", null);
        cloudsOffset = new Vector2f[sav.length];
        System.arraycopy(sav, 0, cloudsOffset, 0, sav.length);

        cloudsScale = capsule.readFloatArray("cloudScales", null);

        sav = capsule.readSavableArray("objectCenters", null);
        objectCenter = new Vector2f[sav.length];
        System.arraycopy(sav, 0, objectCenter, 0, sav.length);
        /*
         * cached values
         */
        assetManager = importer.getAssetManager();
        maxCloudLayers = cloudsImage.length;
        maxObjects = objectCenter.length;

        cloudsRaster = new ImageRaster[maxCloudLayers];
        for (int layerIndex = 0; layerIndex < maxCloudLayers; layerIndex++) {
            Image image = cloudsImage[layerIndex];
            cloudsRaster[layerIndex] = ImageRaster.create(image);
        }
    }

    /**
     * Serialize this instance when saving.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(cloudsAlpha, "cloudAlphas", null);
        capsule.write(cloudsImage, "cloudImages", null);
        capsule.write(cloudsOffset, "cloudOffsets", null);
        capsule.write(cloudsScale, "cloudScales", null);
        capsule.write(objectCenter, "objectCenters", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Estimate how much light is transmitted through a particular cloud layer
     * at the specified texture coordinates.
     *
     * @param layerIndex (<maxCloudLayers, >=0)
     * @param skyCoordinates (unaffected, not null)
     * @return fraction of light transmitted (<=1, >=0)
     */
    private float getTransparency(int layerIndex, Vector2f skyCoordinates) {
        assert layerIndex >= 0 : layerIndex;
        assert layerIndex < maxCloudLayers : layerIndex;
        assert skyCoordinates != null;
        assert cloudsRaster[layerIndex] != null : layerIndex;

        Vector2f coord = skyCoordinates.mult(cloudsScale[layerIndex]);
        coord.addLocal(cloudsOffset[layerIndex]);
        coord.x = MyMath.modulo(coord.x, uvMax);
        coord.y = MyMath.modulo(coord.y, uvMax);
        float opacity = sampleRed(cloudsRaster[layerIndex], coord);
        opacity *= cloudsAlpha[layerIndex];
        float result = alphaMax - opacity;

        assert result >= alphaMin : result;
        assert result <= alphaMax : result;
        return result;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetPath pathname to the texture asset (not null)
     * @return the texture which was loaded
     */
    private Texture loadTexture(String assetPath) {
        assert assetPath != null;

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }

    /**
     * Load a non-flipped texture asset in clamp mode.
     *
     * @param assetPath pathname to the texture asset (not null)
     * @return the texture which was loaded
     */
    private Texture loadTextureClamp(String assetPath) {
        assert assetPath != null;

        Texture texture = loadTexture(assetPath);
        texture.setWrap(WrapMode.Clamp);

        return texture;
    }

    /**
     * Load a non-flipped texture asset in repeat mode.
     *
     * @param assetPath pathname to the texture asset (not null)
     * @return the texture which was loaded
     */
    private Texture loadTextureRepeat(String assetPath) {
        assert assetPath != null;

        Texture texture = loadTexture(assetPath);
        texture.setWrap(WrapMode.Repeat);

        return texture;
    }

    /**
     * Sample the red component of a rasterized texture at the specified
     * coordinates.
     *
     * @param colorImage the texture to sample (not null, unaffected)
     * @param uv texture coordinates to sample (not null, each component <1 and
     * >=0, unaffected)
     * @return red intensity (<=1, >=0)
     */
    private float sampleRed(ImageRaster colorImage, Vector2f uv) {
        assert colorImage != null;
        assert uv != null;
        float u = uv.x;
        float v = uv.y;
        assert u >= uvMin : uv;
        assert u < uvMax : uv;
        assert v >= uvMin : uv;
        assert v < uvMax : uv;

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
         * Get the red values of the four nearest pixels.
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

        assert result >= alphaMin : result;
        assert result <= alphaMax : result;
        return result;
    }
}
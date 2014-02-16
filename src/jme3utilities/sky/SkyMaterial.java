/*
 Copyright (c) 2013-2014, Stephen Gold
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
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyString;
import jme3utilities.debug.Validate;

/**
 * A material for a dynamic sky dome using assets and techniques derived from
 * http://code.google.com/p/jmonkeyplatform-contributions/source/browse/trunk/SkyDome
 * <p>
 * The color of clear sky may be adjusted by invoking setClearColor().
 * <p>
 * Stars can be added to the material by invoking addStars().
 * <p>
 * Astronomical objects can be added to the material by invoking addObject();
 * once added, their positions, sizes, and colors may be adjusted by invoking
 * setObjectTransform(), setObjectColor(), and setObjectGlow(). Each object is
 * identified by an index: 0 indicates the object furthest from the observer.
 * <p>
 * Cloud layers can be added to the material by invoking addClouds(); once
 * added, their positions, sizes, and colors may be adjusted by invoking
 * setCloudsOffset(), setCloudsScale(), setCloudsColor(), and setCloudsGlow().
 * <p>
 * Horizon haze can be added to the material by invoking addHaze(); once added,
 * its color may be adjusted by invoking setHazeColor().
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SkyMaterial
        extends SkyMaterialCore {
    // *************************************************************************
    // constants

    /**
     * default color of clear sky: pale blue
     */
    final private static ColorRGBA defaultClearColor =
            new ColorRGBA(0.4f, 0.6f, 1f, Constants.alphaMax);
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SkyMaterial.class.getName());
    /**
     * asset path to the default cloud layer alpha map
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
    // *************************************************************************
    // constructors

    /**
     * Instantiate a sky material from the default definitions. The first method
     * invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     */
    public SkyMaterial(AssetManager assetManager) {
        this(assetManager, 2, 2);
    }

    /**
     * Instantiate a sky material from a particular asset path. The first method
     * invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param assetPath pathname to the material definitions asset (not null)
     */
    public SkyMaterial(AssetManager assetManager, String assetPath) {
        super(assetManager, assetPath, numObjects(assetPath),
                numCloudLayers(assetPath));
    }

    /**
     * Instantiate a sky material with the specified number of objects and cloud
     * layers. Material definitions will be automatically selected. The first
     * method invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param maxObjects number of astronomical objects required (&ge;0)
     * @param maxCloudLayers number of cloud layers required (&ge;0)
     */
    public SkyMaterial(AssetManager assetManager, int maxObjects,
            int maxCloudLayers) {
        super(assetManager, pickMatDefs(maxObjects, maxCloudLayers), maxObjects,
                maxCloudLayers);
    }

    /**
     * Instantiate a sky material from a particular asset path. The first method
     * invoked should be initialize().
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param assetPath pathname to the material definitions asset (not null)
     * @param maxObjects number of astronomical objects allowed (&ge;0)
     * @param maxCloudLayers number of cloud layers allowed (&ge;0)
     */
    public SkyMaterial(AssetManager assetManager, String assetPath,
            int maxObjects, int maxCloudLayers) {
        super(assetManager, assetPath, maxObjects, maxCloudLayers);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a cloud layer to this material using the default alpha map.
     *
     * @param layerIndex (&lt;maxCloudLayers, &ge;0)
     */
    public void addClouds(int layerIndex) {
        validateLayerIndex(layerIndex);
        addClouds(layerIndex, cloudsMapPath);
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
        Validate.nonNull(assetPath, "path");

        Texture alphaMap = MyAsset.loadTexture(assetManager, assetPath);
        setTexture("HazeAlphaMap", alphaMap);
        setHazeColor(ColorRGBA.White);
    }

    /**
     * Add an astronomical object to this material using the specified color map
     * asset.
     *
     * @param objectIndex (&lt;maxObjects, &ge;0)
     * @param assetPath asset path to the color map (not null)
     */
    public void addObject(int objectIndex, String assetPath) {
        validateObjectIndex(objectIndex);
        Validate.nonNull(assetPath, "path");

        Texture colorMap = MyAsset.loadTexture(assetManager, assetPath);
        addObject(objectIndex, colorMap);
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
        Validate.nonNull(assetPath, "path");

        Texture colorMap = MyAsset.loadTexture(assetManager, assetPath);
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
     * Initialize this material.
     */
    public void initialize() {
        setClearColor(defaultClearColor);
        setVector2("TopCoord", Constants.topUV);
        /*
         * The default blend mode is "off".  Since this material may have
         * translucent regions, specify "alpha" blending.
         */
        RenderState additional = getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
    }

    /**
     * Remove any stars from this material.
     */
    public void removeStars() {
        clearParam("StarsColorMap");
    }

    /**
     * Alter the color of clear sky.
     *
     * @param newColor (not null)
     */
    public void setClearColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");

        setColor("ClearColor", newColor);
    }

    /**
     * Alter the glow color of clear sky.
     *
     * @param newColor (not null)
     */
    public void setClearGlow(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");

        setColor("ClearGlow", newColor);
    }

    /**
     * Alter the color of the horizon haze.
     *
     * @param newColor (not null)
     */
    public void setHazeColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");

        setColor("HazeColor", newColor);
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the number of cloud layers supported by a specified asset.
     *
     * @param assetPath path to material definitions (not null)
     */
    private static int numCloudLayers(String assetPath) {
        assert assetPath != null;

        int result;
        switch (assetPath) {
            case "MatDefs/skies/dome20/dome20.j3md":
            case "MatDefs/skies/dome60/dome60.j3md":
                result = 0;
                break;
            case "MatDefs/skies/dome02/dome02.j3md":
            case "MatDefs/skies/dome22/dome22.j3md":
                result = 2;
                break;
            case "MatDefs/skies/dome06/dome06.j3md":
            case "MatDefs/skies/dome66/dome66.j3md":
                result = 6;
                break;
            default:
                logger.log(Level.SEVERE, "assetPath={0}", assetPath);
                throw new IllegalArgumentException("unknown asset");
        }
        return result;
    }

    /**
     * Determine the number of objects supported by a specified asset.
     *
     * @param assetPath path to material definitions (not null)
     */
    private static int numObjects(String assetPath) {
        assert assetPath != null;

        int result;
        switch (assetPath) {
            case "MatDefs/skies/dome02/dome02.j3md":
            case "MatDefs/skies/dome06/dome06.j3md":
                result = 0;
                break;
            case "MatDefs/skies/dome20/dome20.j3md":
            case "MatDefs/skies/dome22/dome22.j3md":
                result = 2;
                break;
            case "MatDefs/skies/dome60/dome60.j3md":
            case "MatDefs/skies/dome66/dome66.j3md":
                result = 6;
                break;
            default:
                logger.log(Level.SEVERE, "assetPath={0}", assetPath);
                throw new IllegalArgumentException("unknown asset");
        }
        return result;
    }

    /**
     * Select a material definitions asset with at least the specified numbers
     * of objects and cloud layers.
     *
     * @param numObjects (&le;6, &ge;0)
     * @param numCloudLayers (&le;6, &ge;0)
     * @return asset path
     */
    private static String pickMatDefs(int numObjects, int numCloudLayers) {
        assert numObjects >= 0 : numObjects;
        assert numCloudLayers >= 0 : numCloudLayers;

        String assetPath;
        if (numObjects == 0 && numCloudLayers <= 2) {
            assetPath = "MatDefs/skies/dome02/dome02.j3md";
        } else if (numObjects <= 2 && numCloudLayers <= 0) {
            assetPath = "MatDefs/skies/dome20/dome20.j3md";
        } else if (numObjects <= 2 && numCloudLayers <= 2) {
            assetPath = "MatDefs/skies/dome22/dome22.j3md";
        } else if (numObjects == 0 && numCloudLayers <= 6) {
            assetPath = "MatDefs/skies/dome06/dome06.j3md";
        } else if (numObjects <= 6 && numCloudLayers <= 0) {
            assetPath = "MatDefs/skies/dome60/dome60.j3md";
        } else if (numObjects <= 6 && numCloudLayers <= 6) {
            assetPath = "MatDefs/skies/dome66/dome66.j3md";
        } else if (numObjects > 6) {
            logger.log(Level.SEVERE, "numObjects={0}", numObjects);
            throw new IllegalArgumentException("too many objects");
        } else {
            logger.log(Level.SEVERE, "numCloudLayers={0}", numCloudLayers);
            throw new IllegalArgumentException("too many cloud layers");
        }

        logger.log(Level.INFO, "asset path={0}", MyString.quote(assetPath));
        return assetPath;
    }
}
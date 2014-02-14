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
package jme3utilities;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import java.util.logging.Logger;

/**
 * Miscellaneous utility methods for loading assets. Aside from test cases, all
 * methods here should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MyAsset {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyAsset.class.getName());
    /**
     * asset path of the shaded material definition
     */
    final public static String shadedMaterialAssetPath =
            "Common/MatDefs/Light/Lighting.j3md";
    /**
     * asset path to the Unshaded material definition
     */
    final public static String unshadedMaterialAssetPath =
            "Common/MatDefs/Misc/Unshaded.j3md";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyAsset() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create an invisible material.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createInvisibleMaterial(
            AssetManager assetManager) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }

        Material material = createUnshadedMaterial(assetManager);
        material.setColor("Color", ColorRGBA.BlackNoAlpha);
        RenderState additional = material.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);

        return material;
    }

    /**
     * Create a shaded material for a specific diffuse texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return a new instance
     */
    public static Material createShadedMaterial(AssetManager assetManager,
            Texture texture) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        if (texture == null) {
            throw new NullPointerException("texture should not be null");
        }

        Material material = new Material(assetManager, shadedMaterialAssetPath);
        material.setTexture("DiffuseMap", texture);
        return material;
    }

    /**
     * Create an unshaded material.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }

        Material material = new Material(assetManager,
                unshadedMaterialAssetPath);
        return material;
    }

    /**
     * Create an unshaded material from a texture asset path.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        Texture texture = loadTexture(assetManager, assetPath);
        Material material = createUnshadedMaterial(assetManager, texture);

        return material;
    }

    /**
     * Create an unshaded material for a specific colormap texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            Texture texture) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }
        if (texture == null) {
            throw new NullPointerException("texture should not be null");
        }

        Material material = createUnshadedMaterial(assetManager);
        material.setTexture("ColorMap", texture);

        return material;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return the texture which was loaded (not null)
     */
    public static Texture loadTexture(AssetManager assetManager,
            String assetPath) {
        if (assetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }
}
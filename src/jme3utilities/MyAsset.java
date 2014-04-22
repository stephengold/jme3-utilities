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
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.logging.Logger;

/**
 * Utility methods for loading assets. Aside from test cases, all methods here
 * should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
final public class MyAsset {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyAsset.class.getName());
    /**
     * asset path of the Particle material definition
     */
    final private static String particleMaterialAssetPath =
            "Common/MatDefs/Misc/Particle.j3md";
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
     * @return new instance
     */
    public static Material createInvisibleMaterial(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");

        Material material = createUnshadedMaterial(assetManager);
        material.setColor("Color", ColorRGBA.BlackNoAlpha);
        RenderState additional = material.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);

        return material;
    }

    /**
     * Create a particle material from the specified texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return new instance
     */
    public static Material createParticleMaterial(AssetManager assetManager,
            Texture texture) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(texture, "texture");

        Material material =
                new Material(assetManager, particleMaterialAssetPath);
        material.setTexture("Texture", texture);

        return material;
    }

    /**
     * Create a shaded material for a specified diffuse texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return new instance
     */
    public static Material createShadedMaterial(AssetManager assetManager,
            Texture texture) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(texture, "texture");

        Material material = new Material(assetManager, shadedMaterialAssetPath);
        material.setTexture("DiffuseMap", texture);
        return material;
    }

    /**
     * Create a shiny lit material with a specified uniform color.
     *
     * @param assetManager (not null)
     * @param color (not null)
     * @return new instance
     */
    public static Material createShinyMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = new Material(assetManager, shadedMaterialAssetPath);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", color);
        material.setColor("Diffuse", color);
        material.setColor("Specular", ColorRGBA.White);
        material.setFloat("Shininess", 1f);

        return material;
    }

    /**
     * Create a cubic star map.
     *
     * @param assetManager (not null)
     * @param name name of the star map (not null)
     * @return new instance
     */
    public static Spatial createStarMap(AssetManager assetManager,
            String name) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(name, "star map name");
        /*
         * Load the cube map textures.
         */
        Texture right = loadFace(assetManager, name, "right1");
        Texture left = loadFace(assetManager, name, "left2");
        Texture top = loadFace(assetManager, name, "top3");
        Texture bottom = loadFace(assetManager, name, "bottom4");
        Texture front = loadFace(assetManager, name, "front5");
        Texture back = loadFace(assetManager, name, "back6");
        /*
         * Create the map.
         */
        Vector3f normalScale = Vector3f.UNIT_XYZ;
        int sphereRadius = 10;
        Spatial starMap = SkyFactory.createSky(assetManager, right, left, back,
                front, top, bottom, normalScale, sphereRadius);
        return starMap;
    }

    /**
     * Create an unshaded material.
     *
     * @param assetManager (not null)
     * @return new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");

        Material material =
                new Material(assetManager, unshadedMaterialAssetPath);
        return material;
    }

    /**
     * Create an unshaded material with the specified color.
     *
     * @param assetManager (not null)
     * @param color (not null, unaffected)
     * @return new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = MyAsset.createUnshadedMaterial(assetManager);
        material.setColor("Color", color);

        return material;
    }

    /**
     * Create an unshaded material from a texture asset path.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            String assetPath) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(assetPath, "path");

        Texture texture = loadTexture(assetManager, assetPath);
        Material material = createUnshadedMaterial(assetManager, texture);

        return material;
    }

    /**
     * Create an unshaded material for a specified colormap texture.
     *
     * @param assetManager (not null)
     * @param texture (not null)
     * @return new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            Texture texture) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(texture, "texture");

        Material material = createUnshadedMaterial(assetManager);
        material.setTexture("ColorMap", texture);

        return material;
    }

    /**
     * Create a wireframe material.
     *
     * @param assetManager (not null)
     * @param color (not null)
     * @return new instance
     */
    public static Material createWireframeMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = createUnshadedMaterial(assetManager, color);
        material.getAdditionalRenderState().setWireframe(true);

        return material;
    }

    /**
     * Load the texture asset for a named face of a cubical star map.
     *
     * @param assetManager (not null)
     * @param mapName name of the star map folder (not null)
     * @param faceName name of the face (not null, e.g. "top3")
     * @return texture which was loaded (not null)
     */
    public static Texture loadFace(AssetManager assetManager, String mapName,
            String faceName) {
        Validate.nonNull(mapName, "folder name");
        Validate.nonNull(faceName, "face name");

        String path = String.format("Textures/skies/star-maps/%s/%s_%s.png",
                mapName, mapName, faceName);
        Texture texture = assetManager.loadTexture(path);

        assert texture != null;
        return texture;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null)
     * @return texture which was loaded (not null)
     */
    public static Texture loadTexture(AssetManager assetManager,
            String assetPath) {
        Validate.nonNull(assetPath, "path");

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }
}
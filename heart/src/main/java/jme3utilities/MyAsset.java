/*
 Copyright (c) 2014-2020, Stephen Gold
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
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.logging.Logger;
import jme3utilities.mesh.RectangleMesh;

/**
 * Utility methods for loading assets. Aside from test cases, all methods here
 * should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyAsset {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyAsset.class.getName());
    /**
     * reusable square mesh, each side 2 mesh units in length
     */
    final private static Mesh squareMesh
            = new RectangleMesh(-1f, 1f, -1f, 1f, 1f);
    /**
     * asset path of the ShowNormals material definition
     */
    final public static String debugMaterialAssetPath
            = "Common/MatDefs/Misc/ShowNormals.j3md";
    /**
     * asset path to the multicolor wireframe material definition with support
     * for alpha discard and non-default point shapes
     */
    final public static String multicolor2MaterialAssetPath
            = "MatDefs/wireframe/multicolor2.j3md";
    /**
     * asset path of the Particle material definition
     */
    final public static String particleMaterialAssetPath
            = "Common/MatDefs/Misc/Particle.j3md";
    /**
     * asset path of the shaded material definition
     */
    final public static String shadedMaterialAssetPath
            = "Common/MatDefs/Light/Lighting.j3md";
    /**
     * asset path to the Unshaded material definition
     */
    final public static String unshadedMaterialAssetPath
            = "Common/MatDefs/Misc/Unshaded.j3md";
    /**
     * asset path to the unicolor, default-shape wireframe material definition
     */
    final public static String wireframeMaterialAssetPath
            = "MatDefs/wireframe/wireframe.j3md";
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    /**
     * Direction vector for the center of each cube face.
     */
    final private static Vector3f[] faceDirection = {
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(1f, 0f, 0f),
        new Vector3f(0f, 1f, 0f),
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, 0f, -1f),
        new Vector3f(0f, 0f, 1f)
    };
    /**
     * Direction vector for first (+U) texture coordinate of each cube face.
     */
    final private static Vector3f[] uDirection = {
        new Vector3f(0f, 0f, 1f),
        new Vector3f(0f, 0f, -1f),
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(1f, 0f, 0f)
    };
    /**
     * Direction vector for 2nd (+V) texture coordinate of each cube face.
     */
    final private static Vector3f[] vDirection = {
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, 0f, -1f),
        new Vector3f(0f, 0f, 1f),
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, -1f, 0f)
    };
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
     * Copy the direction to the center of the specified face of a sky cube.
     *
     * @param faceIndex (&ge;0, &lt;6)
     * @return a new unit vector
     */
    public static Vector3f copyFaceDirection(int faceIndex) {
        Vector3f result = faceDirection[faceIndex].normalize();
        return result;
    }

    /**
     * Copy the direction of the first (+U) texture axis of the specified face
     * of a sky cube.
     *
     * @param faceIndex (&ge;0, &lt;6)
     * @return a new unit vector
     */
    public static Vector3f copyUDirection(int faceIndex) {
        Vector3f result = uDirection[faceIndex].normalize();
        return result;
    }

    /**
     * Copy the direction of the 2nd (+V) texture axis of the specified face of
     * a sky cube.
     *
     * @param faceIndex (&ge;0, &lt;6)
     * @return a new unit vector
     */
    public static Vector3f copyVDirection(int faceIndex) {
        Vector3f result = vDirection[faceIndex].normalize();
        return result;
    }

    /**
     * Create a material for debugging mesh normals.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createDebugMaterial(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");

        Material material = new Material(assetManager, debugMaterialAssetPath);
        material.setName("debug");

        return material;
    }

    /**
     * Create an invisible material.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createInvisibleMaterial(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");

        Material material = createUnshadedMaterial(assetManager);
        material.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0f));
        material.setName("invisible");

        RenderState additional = material.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);
        additional.setFaceCullMode(RenderState.FaceCullMode.FrontAndBack);

        return material;
    }

    /**
     * Create a multicolor wireframe material with the specified point shape and
     * point size. (Points are visible only with a point-mode mesh.)
     *
     * @param assetManager (not null)
     * @param pointShape shape texture (alias created) or null for default shape
     * @return a new instance
     * @param pointSize in pixels (&ge;0, whole numbers recommended)
     */
    public static Material createMulticolor2Material(AssetManager assetManager,
            Texture pointShape, float pointSize) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNegative(pointSize, "point size");

        Material material
                = new Material(assetManager, multicolor2MaterialAssetPath);
        material.setTexture("PointShape", pointShape);
        material.setFloat("PointSize", pointSize);

        return material;
    }

    /**
     * Create a particle material with the specified Texture.
     *
     * @param assetManager (not null)
     * @param texture (not null, alias created)
     * @return a new instance
     */
    public static Material createParticleMaterial(AssetManager assetManager,
            Texture texture) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(texture, "texture");

        Material material
                = new Material(assetManager, particleMaterialAssetPath);
        material.setTexture("Texture", texture);

        return material;
    }

    /**
     * Create a non-shiny shaded material with the specified color.
     *
     * @param assetManager (not null)
     * @param color ambient/diffuse color (not null, unaffected)
     * @return a new instance (not null)
     */
    public static Material createShadedMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = new Material(assetManager, shadedMaterialAssetPath);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", color.clone());
        material.setColor("Diffuse", color.clone());

        return material;
    }

    /**
     * Create a non-shiny shaded material with the specified diffuse texture.
     *
     * @param assetManager (not null)
     * @param texture (not null, alias created)
     * @return a new instance
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
     * Create a shiny shaded material with the specified color.
     *
     * @param assetManager (not null)
     * @param color ambient/diffuse color (not null, unaffected)
     * @return a new instance
     */
    public static Material createShinyMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = createShadedMaterial(assetManager, color);
        material.setColor("Specular", new ColorRGBA(1f, 1f, 1f, 1f));
        material.setFloat("Shininess", 1f);

        return material;
    }

    /**
     * Load a cube-mapped star map onto a cube formed by squares.
     * <p>
     * This method uses Unshaded.j3md materials, which can be translated,
     * rotated, and scaled in the usual fashion.
     * <p>
     * For the sky to be visible, its surface must lie between the near and far
     * planes of the camera's frustum. This can usually be achieved by scaling
     * the Node. The sky's surface ranges from 1.0 to 1.732 local units from the
     * center.
     * <p>
     * To avoid distortion, the camera must remain at the center of the sky.
     * This can usually be achieved by translating the Node to the camera's
     * location.
     *
     * @param assetManager (not null)
     * @param name name of the star map in the Textures/skies/star-maps asset
     * folder (not null, not empty)
     * @return a new instance
     */
    public static Node createStarMapQuads(AssetManager assetManager,
            String name) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonEmpty(name, "star map name");
        /*
         * Load the cube map textures.
         */
        Texture[] faceTexture = new Texture[6];
        faceTexture[0] = loadFace(assetManager, name, "right1", false);
        faceTexture[1] = loadFace(assetManager, name, "left2", false);
        faceTexture[2] = loadFace(assetManager, name, "top3", false);
        faceTexture[3] = loadFace(assetManager, name, "bottom4", false);
        faceTexture[4] = loadFace(assetManager, name, "front5", false);
        faceTexture[5] = loadFace(assetManager, name, "back6", false);
        /*
         * Create square faces.
         */
        Node result = new Node("star map");
        result.setQueueBucket(Bucket.Sky);
        for (int faceIndex = 0; faceIndex < 6; ++faceIndex) {
            String faceName = String.format("%s_face%d", name, faceIndex + 1);
            Geometry geometry = new Geometry(faceName, squareMesh);
            result.attachChild(geometry);
            /*
             * Create a material for the face and apply it.
             */
            Texture texture = faceTexture[faceIndex];
            Material material = createUnshadedMaterial(assetManager, texture);
            geometry.setMaterial(material);
            /*
             * Set the location of the face.
             */
            Vector3f offset = faceDirection[faceIndex].clone();
            geometry.setLocalTranslation(offset);
            /*
             * Orient the face.
             */
            Vector3f u = uDirection[faceIndex];
            Vector3f v = vDirection[faceIndex];
            Vector3f w = faceDirection[faceIndex].negate();
            Quaternion orientation = new Quaternion();
            orientation.fromAxes(u, v, w);
            geometry.setLocalRotation(orientation);
        }

        return result;
    }

    /**
     * Load a cube-mapped star map onto a sky sphere with the specified radius.
     * <p>
     * For the sky to be visible, its radius must fall between the near and far
     * planes of the camera's frustum.
     * <p>
     * Sky spheres use Sky.j3md materials. Translation and scaling of the
     * geometry is ignored and rotations are applied in the shader. The effect
     * of rotating a sky sphere is the inverse of rotating an ordinary geometry.
     *
     * @param assetManager (not null)
     * @param name name of the star map in the Textures/skies/star-maps asset
     * folder (not null, not empty)
     * @param radius size of the sphere (&gt;0)
     * @return a new instance
     */
    public static Geometry createStarMapSphere(AssetManager assetManager,
            String name, float radius) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonEmpty(name, "star map name");
        Validate.positive(radius, "radius");
        /*
         * Load the cube map textures for a viewer facing the south horizon.
         */
        Texture right = loadFace(assetManager, name, "right1", true); // west
        Texture left = loadFace(assetManager, name, "left2", true); // east
        Texture top = loadFace(assetManager, name, "top3", true); // up
        Texture bottom = loadFace(assetManager, name, "bottom4", true); // down
        Texture front = loadFace(assetManager, name, "front5", true); // south
        Texture back = loadFace(assetManager, name, "back6", true); // north
        /*
         * Create the sky sphere.
         */
        Spatial starMap = SkyFactory.createSky(assetManager, right, left, back,
                front, top, bottom, scaleIdentity, radius);

        return (Geometry) starMap;
    }

    /**
     * Create a default unshaded material.
     *
     * @param assetManager (not null)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");
        Material material
                = new Material(assetManager, unshadedMaterialAssetPath);
        return material;
    }

    /**
     * Create an unshaded material with the specified color.
     *
     * @param assetManager (not null)
     * @param color (not null, unaffected)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        Material material = createUnshadedMaterial(assetManager);
        material.setColor("Color", color.clone());

        return material;
    }

    /**
     * Create an unshaded material with the colormap texture from the specified
     * asset path.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null, not empty)
     * @return a new instance
     */
    public static Material createUnshadedMaterial(AssetManager assetManager,
            String assetPath) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonEmpty(assetPath, "path");

        Texture texture = loadTexture(assetManager, assetPath, false);
        Material material = createUnshadedMaterial(assetManager, texture);

        return material;
    }

    /**
     * Create an unshaded material with the specified colormap texture.
     *
     * @param assetManager (not null)
     * @param texture (not null, alias created)
     * @return a new instance
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
     * Create a unicolor wireframe material with default point shape and a point
     * size of 1. (Points are visible only with a point-mode mesh.)
     *
     * @param assetManager (not null)
     * @param color (not null, unaffected)
     * @return a new instance
     */
    public static Material createWireframeMaterial(AssetManager assetManager,
            ColorRGBA color) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");

        float pointSize = 1f;
        Material material
                = createWireframeMaterial(assetManager, color, pointSize);

        return material;
    }

    /**
     * Create a unicolor wireframe material with the default point shape and
     * specified point size. (Points are visible only with a point-mode mesh.)
     *
     * @param assetManager (not null)
     * @param color (not null, unaffected)
     * @param pointSize in pixels (&ge;0, whole numbers recommended)
     * @return a new instance
     */
    public static Material createWireframeMaterial(AssetManager assetManager,
            ColorRGBA color, float pointSize) {
        Validate.nonNull(assetManager, "asset manager");
        Validate.nonNull(color, "color");
        Validate.nonNegative(pointSize, "point size");

        Material material
                = new Material(assetManager, wireframeMaterialAssetPath);
        material.setColor("Color", color.clone());
        material.setFloat("PointSize", pointSize);

        return material;
    }

    /**
     * Load the texture asset for a named face of a cubical star map.
     *
     * @param assetManager (not null)
     * @param mapName name of the star map folder (not null, not empty)
     * @param faceName name of the face (not null, not empty, e.g. "top3")
     * @param flipY true &rarr; flipped, false &rarr; not flipped
     * @return texture which was loaded (not null)
     */
    public static Texture loadFace(AssetManager assetManager, String mapName,
            String faceName, boolean flipY) {
        Validate.nonEmpty(mapName, "folder name");
        Validate.nonEmpty(faceName, "face name");

        String path = String.format("Textures/skies/star-maps/%s/%s_%s.png",
                mapName, mapName, faceName);
        TextureKey key = new TextureKey(path, flipY);
        key.setGenerateMips(true);
        Texture texture = assetManager.loadTexture(key);

        assert texture != null;
        return texture;
    }

    /**
     * Load a non-flipped texture asset in edge-clamp mode.
     *
     * @param assetManager (not null)
     * @param assetPath to the texture asset (not null, not empty)
     * @param generateMips true&rarr;generate mipmaps, false&rarr;don't generate
     * them
     * @return the texture that was loaded (not null)
     */
    public static Texture loadTexture(AssetManager assetManager,
            String assetPath, boolean generateMips) {
        Validate.nonEmpty(assetPath, "path");

        boolean flipY = false;
        TextureKey key = new TextureKey(assetPath, flipY);
        key.setGenerateMips(generateMips);
        Texture texture = assetManager.loadTexture(key);
        // edge-clamp mode is the default

        assert texture != null;
        return texture;
    }
}

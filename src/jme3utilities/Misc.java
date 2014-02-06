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
package jme3utilities;

import jme3utilities.math.MyVector3f;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.Bone;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.IntMap;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Miscellaneous utility methods. Aside from test cases, all methods here should
 * be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Misc {
    // *************************************************************************
    // constants

    /**
     * blend time for animations (in real seconds, >=0)
     */
    final public static float blendTime = 0.3f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Misc.class.getName());
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
    private Misc() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Augment an existing compound collision collision shape with a scaled
     * shape.
     *
     * @param parent compound shape to augment (not null, altered)
     * @param child unscaled child shape (not null, altered)
     * @param offset child's location relative to the parent's center, in
     * unscaled world coordinates (not null, not altered)
     * @param orientation child's orientation in world coordinates (null means
     * don't care, not altered)
     * @param worldScale to be applied to child and offset (not null, not
     * altered, all components non-negative)
     */
    public static void addChildShape(CompoundCollisionShape parent,
            CollisionShape child, Vector3f offset, Matrix3f orientation,
            Vector3f worldScale) {
        if (worldScale == null) {
            throw new NullPointerException("scale should not be null");
        }
        if (!MyVector3f.isAllNonNegative(worldScale)) {
            logger.log(Level.SEVERE, "scale={0}", worldScale);
            throw new IllegalArgumentException(
                    "scale factors should all be non-negative");
        }

        child.setScale(worldScale);
        Vector3f scaledOffset = offset.mult(worldScale);

        if (orientation == null) {
            parent.addChildShape(child, scaledOffset);
        } else {
            parent.addChildShape(child, scaledOffset, orientation);
        }
    }

    /**
     * Smoothly transition an animation channel to a new animation.
     *
     * @param channel which animation channel (not null)
     * @param newAnimation name of animation (or null to reset the channel)
     */
    public static void blendTo(AnimChannel channel, String newAnimation) {
        if (newAnimation == null) {
            channel.reset(true);
            return;
        }
        String oldAnimation = channel.getAnimationName();
        if (newAnimation.equals(oldAnimation)) {
            return;
        }
        channel.setAnim(newAnimation, blendTime);
        channel.setLoopMode(LoopMode.Loop);
    }

    /**
     * Compute the minimum and maximum elevations of a mesh geometry.
     *
     * @param geometry which geometry to measure (not null)
     * @return array consisting of array[0]: the lowest world Y-coordinate (in
     * world units) and array[1]: the highest world Y-coordinate (in world
     * units)
     */
    public static float[] findMinMaxHeights(Geometry geometry) {
        Vector3f vertexLocal[] = new Vector3f[3];
        for (int j = 0; j < 3; j++) {
            vertexLocal[j] = new Vector3f();
        }
        Vector3f worldLocation = new Vector3f();

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        Mesh mesh = geometry.getMesh();
        assert mesh.getMode() == Mesh.Mode.Triangles : mesh.getMode();
        int count = mesh.getTriangleCount();
        for (int triangleIndex = 0; triangleIndex < count; triangleIndex++) {
            /*
             * Get the vertex locations for a triangle in the mesh.
             */
            mesh.getTriangle(triangleIndex, vertexLocal[0], vertexLocal[1],
                    vertexLocal[2]);
            /*
             * Compare with lowest and highest world elevations so far.
             */
            for (int j = 0; j < 3; j++) {
                geometry.localToWorld(vertexLocal[j], worldLocation);
                float y = worldLocation.y;
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        /*
         * Create the result array.
         */
        float[] minMax = {minY, maxY};
        return minMax;
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
     * Get the existing filter post-processor for a viewport, or if it has none
     * add a new one to it.
     *
     * @param viewPort (not null)
     * @param assetManager (not null)
     */
    public static FilterPostProcessor getFpp(ViewPort viewPort,
            AssetManager assetManager) {
        if (assetManager == null) {
            throw new NullPointerException("asset manager should not be null");
        }

        for (SceneProcessor processor : viewPort.getProcessors()) {
            if (processor instanceof FilterPostProcessor) {
                return (FilterPostProcessor) processor;
            }
        }
        /*
         * Add a new filter post-processor.
         */
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        return fpp;
    }

    /**
     * Generate the filesystem path to a file in the user's home directory.
     *
     * @param fileName (not null)
     * @return the generated path
     */
    public static String getUserPath(String fileName) {
        if (fileName == null) {
            throw new NullPointerException("file name should not be null");
        }

        String homePath = System.getProperty("user.home");
        String result = String.format("%s/%s", homePath, fileName);
        return result;
    }

    /**
     * Read the verbose version string for this package.
     *
     * @return the package name, branch, and revision of this file
     */
    public static String getVersion() {
        return "jme3-utilities trunk $Rev$";
    }

    /**
     * Read the terse version string for this package.
     *
     * @return the branch and revision of this file
     */
    public static String getVersionShort() {
        String verbose = getVersion();
        String[] words = verbose.split("\\s+");
        String result = String.format("%s %s", words[1], words[3]);

        return result;
    }

    /**
     * Compute the world elevation of a horizontal surface.
     *
     * @param geometry which surface to measure (not null)
     * @return world elevation of the surface (in world units)
     */
    public static float getYLevel(Geometry geometry) {
        if (geometry == null) {
            throw new NullPointerException("geometry should not be null");
        }

        float minMax[] = findMinMaxHeights(geometry);
        assert minMax[0] == minMax[1] : minMax[0];
        return minMax[0];
    }

    /**
     * Test whether a mesh has texture (U-V) coordinates.
     *
     * @param mesh which mesh to test (not null)
     * @return true if the mesh has texture coordinates, otherwise false
     */
    public static boolean hasUV(Mesh mesh) {
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        int key = Type.TexCoord.ordinal();
        boolean result = buffers.containsKey(key);
        return result;
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

    /**
     * Alter a single bone angle in the bind pose.
     *
     * @param bone which bone to adjust (not null)
     * @param axis which local rotation axis to adjust (0 -> x, 1 -> y, 2 -> z)
     * @param newAngle new rotation angle (in radians)
     */
    public static void setAngle(Bone bone, int axis, float newAngle) {
        if (axis < 0 || axis > 2) {
            logger.log(Level.SEVERE, "axis={0}", axis);
            throw new IllegalArgumentException(
                    "axis should be between 0 and 2, inclusive");
        }

        Vector3f location = bone.getLocalPosition();
        Vector3f scale = bone.getLocalScale();
        Quaternion orientation = bone.getLocalRotation().clone();
        float[] angles = orientation.toAngles(null);
        angles[axis] = newAngle;
        orientation.fromAngles(angles);
        bone.setBindTransforms(location, orientation, scale);
    }

    /**
     * Alter the logging levels of all known loggers.
     *
     * @param newLevel (not null)
     */
    public static void setLoggingLevels(Level newLevel) {
        if (newLevel == null) {
            throw new NullPointerException("level should not be null");
        }

        Logger.getLogger("").setLevel(newLevel);
    }

    /**
     * Convert a collection of strings into an array. This is more convenient
     * than Collection.toArray() because the elements of the resulting array
     * will all be strings.
     *
     * @param collection to convert (not null)
     * @return new array containing the same strings in the same order
     */
    public static String[] toArray(Collection<String> collection) {
        int itemCount = collection.size();
        String[] result = new String[itemCount];

        int nextIndex = 0;
        for (String string : collection) {
            result[nextIndex] = string;
            nextIndex++;
        }
        return result;
    }

    /**
     * Write an image to a PNG file, attempting to overwrite any pre-existing
     * file.
     *
     * @param filePath path to the output file (not null)
     * @param image to be written (not null)
     */
    public static void writeMap(String filePath, RenderedImage image)
            throws IOException {
        if (filePath == null) {
            throw new NullPointerException("path should not be null");
        }
        if (image == null) {
            throw new NullPointerException("image should not be null");
        }

        File textureFile = new File(filePath);
        try {
            ImageIO.write(image, "png", textureFile);
            logger.log(Level.INFO, "wrote texture to {0}",
                    MyString.quote(filePath));

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "write to {0} failed",
                    MyString.quote(filePath));
            boolean success = textureFile.delete();
            if (success) {
                logger.log(Level.INFO, "deleted file {0}",
                        MyString.quote(filePath));
            } else {
                logger.log(Level.SEVERE, "delete of {0} failed",
                        MyString.quote(filePath));
            }
            throw exception;
        }
    }
}
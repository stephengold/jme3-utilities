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
package jme3utilities;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.Bone;
import com.jme3.animation.LoopMode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
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
     * Calculate the minimum and maximum elevations of a mesh geometry.
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
     * Generate the filesystem path to a file in the user's home directory.
     *
     * @param fileName (not null)
     * @return the generated path
     */
    public static String getUserPath(String fileName) {
        assert fileName != null;

        String homePath = System.getProperty("user.home");
        String result = String.format("%s/%s", homePath, fileName);
        return result;
    }

    /**
     * Calculate the world elevation of a horizontal surface.
     *
     * @param geometry which surface to measure (not null)
     * @return world elevation of the surface (in world units)
     */
    public static float getYLevel(Geometry geometry) {
        assert geometry != null;

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
     * Alter a single bone angle in the bind pose.
     *
     * @param bone which bone to adjust (not null)
     * @param axis which local rotation axis to adjust (0 = x, 1 = y, 2 = z)
     * @param newAngle new rotation angle (in radians)
     */
    public static void setAngle(Bone bone, int axis, float newAngle) {
        assert bone != null;
        assert axis >= 0 : axis;
        assert axis <= 2 : axis;

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
        assert newLevel != null;

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
     * Write an image to a file, attempting to overwrite any pre-existing file.
     *
     * @param filePath path to the output file (not null)
     * @param image to be written (not null)
     */
    public static void writeMap(String filePath, RenderedImage image)
            throws IOException {
        if (filePath == null) {
            throw new NullPointerException("path cannot be null");
        }
        if (image == null) {
            throw new NullPointerException("image cannot be null");
        }

        File textureFile = new File(filePath);
        try {
            ImageIO.write(image, "png", textureFile);
            logger.log(Level.INFO, "wrote texture to {0}", filePath);

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "write to {0} failed", filePath);
            boolean success = textureFile.delete();
            if (success) {
                logger.log(Level.INFO, "deleted file {0}", filePath);
            } else {
                logger.log(Level.SEVERE, "delete of {0} failed", filePath);
            }
            throw exception;
        }
    }
}
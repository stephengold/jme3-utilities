/*
 Copyright (c) 2013-2017, Stephen Gold
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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for manipulating skeletonized spatials, skeletons, and bones.
 * Aside from test cases, all methods should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MySkeleton
        extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MySkeleton.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MySkeleton() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access a named bone in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial which contains the bone (not null)
     * @param boneName name of the bone to access (not null)
     * @return pre-existing instance (or null if not found)
     */
    public static Bone getBone(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Skeleton skeleton = getSkeleton(spatial);
        Bone result = skeleton.getBone(boneName);

        return result;
    }

    // TODO methods out of order
    /**
     * Find the index of a named bone in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial which contains the bone (not null,
     * unaffected)
     * @param boneName name of the bone to find (not null)
     * @return bone index (&ge; 0) or -1 if not found
     */
    public static int findBoneIndex(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Skeleton skeleton = getSkeleton(spatial);
        int index = skeleton.getBoneIndex(boneName);

        return index;
    }

    /**
     * Access the skeleton of a skeletonized spatial.
     *
     * @param spatial skeletonized spatial (not null)
     * @return a pre-existing instance
     */
    public static Skeleton getSkeleton(Spatial spatial) {
        SkeletonControl control = spatial.getControl(SkeletonControl.class);
        if (control == null) {
            throw new IllegalArgumentException(
                    "expected the spatial to have an SkeletonControl");
        }

        Skeleton skeleton = control.getSkeleton();
        if (skeleton == null) {
            throw new IllegalArgumentException(
                    "expected the spatial to have a skeleton");
        }

        return skeleton;
    }

    /**
     * List all bones in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial (not null)
     * @return a new collection in lexicographic order (may be empty)
     */
    public static Collection<String> listBones(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Skeleton skeleton = getSkeleton(spatial);
        int boneCount = skeleton.getBoneCount();

        Collection<String> names = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            String name = bone.getName();
            names.add(name);
        }

        return names;
    }

    /**
     * Convert a location in a bone's local space to a location in model space.
     *
     * @param bone (not null)
     * @param x displacement along the bone's X-axis
     * @param y displacement along the bone's Y-axis
     * @param z displacement along the bone's Z-axis
     * @return a new vector
     *
     */
    public static Vector3f modelLocation(Bone bone, float x, float y, float z) {
        Vector3f tail = bone.getModelSpacePosition();
        Vector3f scale = bone.getModelSpaceScale();
        Vector3f result = new Vector3f(x, y, z).multLocal(scale);
        result.addLocal(tail);

        return result;
    }

    /**
     * Alter the position of the specified bone using world coordinates, without
     * updating its descendents.
     *
     * @param spatial skeletonized spatial which contains the bone (not null)
     * @param bone (not null)
     * @param tailLocation desired world location of the bone's tail (not null)
     * @param orientation desired world orientation of the bone (not null)
     */
    public static void positionInWorld(Spatial spatial, Bone bone,
            Vector3f tailLocation, Quaternion orientation) {
        Validate.nonNull(tailLocation, "location");
        Validate.nonNull(orientation, "orientation");
        /*
         * Convert location and orientation from world to model space.
         */
        Vector3f offset = spatial.worldToLocal(tailLocation, null);
        Quaternion convert = spatial.getWorldRotation().inverse();
        Quaternion rotation = convert.mult(orientation);

        bone.setUserTransformsWorld(offset, rotation);
    }

    /**
     * Alter the userControl flag for an entire skeletonized spatial.
     *
     * @param spatial skeletonized spatial (not null)
     * @param newValue true to enable, false to disable
     */
    public static void setUserControl(Spatial spatial, boolean newValue) {
        Validate.nonNull(spatial, "spatial");

        Skeleton skeleton = getSkeleton(spatial);
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserControl(newValue);
        }
    }

    /**
     * Compute the world location of (the tail of) a named bone.
     *
     * @param spatial skeletonized spatial which contains the bone (not null)
     * @param boneName (not null)
     * @return new vector in world coordinates
     */
    public static Vector3f worldLocation(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Bone bone = getBone(spatial, boneName);
        Vector3f localCoordinates = bone.getModelSpacePosition();
        Vector3f result = spatial.localToWorld(localCoordinates, null);

        return result;
    }

    /**
     * Compute the world orientation of a bone.
     *
     * @param spatial skeletonized spatial which contains the bone (not null)
     * @param bone (not null)
     * @return new instance in world coordinates
     */
    public static Quaternion worldOrientation(Spatial spatial, Bone bone) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(bone, "bone");

        Quaternion boneInModel = bone.getModelSpaceRotation();
        Quaternion modelInWorld = spatial.getWorldRotation();
        Quaternion result = modelInWorld.mult(boneInModel);

        return result;
    }

    /**
     * Compute the world orientation of a named bone.
     *
     * @param spatial skeletonized spatial which contains the bone (not null)
     * @param boneName (not null)
     * @return new instance
     */
    public static Quaternion worldOrientation(Spatial spatial,
            String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone");

        Bone bone = getBone(spatial, boneName);
        Quaternion result = worldOrientation(spatial, bone);

        return result;
    }
    // *************************************************************************
    // test cases

    /**
     * Simple application to test the MySkeleton class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.SEVERE);
        MySkeleton application = new MySkeleton();
        application.setShowSettings(false);
        application.start();
    }

    @Override
    public void simpleInitApp() {
        logger.setLevel(Level.INFO);
        System.out.print("Test results for class MySkeleton:\n\n");

        String modelPath = "Models/Oto/Oto.mesh.xml";
        Node node = (Node) assetManager.loadModel(modelPath);
        rootNode.attachChild(node);

        //String bone = "uparm.right";

        Quaternion orientation = new Quaternion();
        float[] angles = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            angles[axis] = 0.4f;
        }
        for (int axis = 0; axis < 3; axis++) {
            float angle = 0.2f + 0.1f * axis;
            angles[axis] = angle;
            orientation.fromAngles(angles);
            //
        }

        stop();
    }
}
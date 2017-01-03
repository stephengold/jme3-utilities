/*
 Copyright (c) 2013-2015, Stephen Gold
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
     * Compute one rotation angle of a named bone in a skeletonized spatial.
     *
     * @param model animated spatial which contains the bone (not null)
     * @param boneName name of the bone to measure (not null, not empty)
     * @param axis local rotation axis to measure (0 &rarr; X, 1 &rarr; Y, 2
     * &rarr; Z)
     * @return rotation angle (in radians) or zero for unknown bone
     */
    public static float boneAngle(Spatial model, String boneName, int axis) {
        Validate.nonNull(model, "model");
        Validate.nonEmpty(boneName, "bone");
        if (axis < 0 || axis > 2) {
            logger.log(Level.SEVERE, "axis={0}", axis);
            throw new IllegalArgumentException(
                    "axis should be between 0 and 2, inclusive");
        }

        Bone bone = getBone(model, boneName);
        if (bone == null) {
            return 0f;
        }
        Quaternion orientation = bone.getLocalRotation();
        float[] angles = orientation.toAngles(null);
        float angle = angles[axis];
        return angle;
    }

    /**
     * Access a named bone in a skeletonized spatial.
     *
     * @param model animated spatial which contains the bone (not null)
     * @param boneName name of the bone to access (not null, not empty)
     * @return pre-existing instance (or null if not found)
     */
    public static Bone getBone(Spatial model, String boneName) {
        Validate.nonNull(model, "model");
        Validate.nonEmpty(boneName, "name");

        Skeleton skeleton = getSkeleton(model);
        if (skeleton == null) {
            return null;
        }
        Bone bone = skeleton.getBone(boneName);
        return bone;
    }

    /**
     * Access the skeleton of a skeletonized spatial.
     *
     * @param model skeletonized spatial (not null)
     * @return pre-existing instance (or null if not found)
     */
    public static Skeleton getSkeleton(Spatial model) {
        SkeletonControl control = model.getControl(SkeletonControl.class);
        if (control == null) {
            return null;
        }
        Skeleton skeleton = control.getSkeleton();
        return skeleton;
    }

    /**
     * List all bones in a skeletonized spatial.
     *
     * @param model skeletonized spatial (or null)
     * @return new collection in lexicographic order (may be empty)
     */
    public static Collection<String> listBones(Spatial model) {
        Collection<String> names = new TreeSet<>();
        if (model == null) {
            return names;
        }
        Skeleton skeleton = getSkeleton(model);
        if (skeleton != null) {
            int boneCount = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                String name = bone.getName();
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Convert a location in a bone's local space to a model location vector.
     *
     * @param bone (not null)
     * @param x displacement along the bone's X-axis
     * @param y displacement along the bone's Y-axis
     * @param z displacement along the bone's Z-axis
     * @return new vector
     *
     */
    public static Vector3f modelLocation(Bone bone, float x, float y, float z) {
        Vector3f tail = bone.getModelSpacePosition();
        Vector3f scale = bone.getModelSpaceScale();
        Vector3f local = new Vector3f(x, y, z).multLocal(scale);
        local.addLocal(tail);
        return local;
    }

    /**
     * Alter the position of the specified bone using world coordinates, without
     * updating its descendents.
     *
     * @param model skeletonized spatial which contains the bone (not null)
     * @param bone (not null)
     * @param tailLocation desired world location of the bone's tail (not null)
     * @param orientation desired world orientation of the bone (not null)
     */
    public static void positionInWorld(Spatial model, Bone bone,
            Vector3f tailLocation, Quaternion orientation) {
        Validate.nonNull(tailLocation, "location");
        Validate.nonNull(orientation, "orientation");
        /*
         * Convert location and orientation from world to model space.
         */
        Vector3f offset = model.worldToLocal(tailLocation, null);
        Quaternion convert = model.getWorldRotation().inverse();
        Quaternion rotation = convert.mult(orientation);

        bone.setUserTransformsWorld(offset, rotation);
    }

    /**
     * Adjust one rotation angle in the bind pose of a skeletonized spatial.
     *
     * @param model skeletonized spatial which contains the bone (not null)
     * @param boneName name of the bone to adjust (not null, not empty)
     * @param axis local rotation axis to adjust (0 &rarr; X, 1 &rarr; Y, 2
     * &rarr; Z)
     * @param newAngle new rotation angle (in radians)
     */
    public static void setBoneAngle(Spatial model, String boneName, int axis,
            float newAngle) {
        Validate.nonNull(model, "model");
        Validate.nonEmpty(boneName, "bone");
        if (axis < 0 || axis > 2) {
            logger.log(Level.SEVERE, "axis={0}", axis);
            throw new IllegalArgumentException(
                    "axis should be between 0 and 2, inclusive");
        }

        Bone bone = getBone(model, boneName);
        if (bone == null) {
            return;
        }
        setAngle(bone, axis, newAngle);
        getSkeleton(model).updateWorldVectors();
    }

    /**
     * Alter a single bone angle in the bind pose.
     *
     * @param bone bone to adjust (not null)
     * @param axis local rotation axis to adjust (0 &rarr; X, 1 &rarr; Y, 2
     * &rarr; Z)
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
     * Alter the user control flag for an entire skeletonized spatial.
     *
     * @param model skeletonized spatial (or null)
     * @param newValue true to enable, false to disable
     */
    public static void setUserControl(Spatial model, boolean newValue) {
        assert model != null;

        Skeleton skeleton = getSkeleton(model);
        if (skeleton != null) {
            int boneCount = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                bone.setUserControl(newValue);
            }
        }
    }

    /**
     * Compute the world location of (the tail of) a named bone.
     *
     * @param model skeletonized spatial which contains the bone (not null)
     * @param boneName (not null)
     * @return new vector
     */
    public static Vector3f worldLocation(Spatial model, String boneName) {
        Validate.nonNull(model, "model");
        Validate.nonNull(boneName, "name");

        Bone bone = getBone(model, boneName);
        Vector3f local = bone.getModelSpacePosition();
        Vector3f world = model.localToWorld(local, null);
        return world;
    }

    /**
     * Compute the world orientation of a bone.
     *
     * @param model skeletonized spatial which contains the bone (not null)
     * @param bone (not null)
     * @return new instance
     */
    public static Quaternion worldOrientation(Spatial model, Bone bone) {
        Validate.nonNull(model, "model");
        Validate.nonNull(bone, "bone");

        Quaternion boneInModel = bone.getModelSpaceRotation();
        Quaternion modelInWorld = model.getWorldRotation();
        Quaternion result = modelInWorld.mult(boneInModel);

        return result;
    }

    /**
     * Compute the world orientation of a named bone.
     *
     * @param model skeletonized spatial which contains the bone (not null)
     * @param boneName (not null, not empty)
     * @return new instance
     */
    public static Quaternion worldOrientation(Spatial model, String boneName) {
        Validate.nonNull(model, "model");
        Validate.nonEmpty(boneName, "bone");

        Bone bone = getBone(model, boneName);
        Quaternion result = worldOrientation(model, bone);

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

        String bone = "uparm.right";
        for (int axis = 0; axis < 3; axis++) {
            float angle = 0.2f + 0.1f * axis;
            System.out.printf("angle = %s%n", angle);
            setBoneAngle(node, bone, axis, angle);
            float angle2 = boneAngle(node, bone, axis);
            System.out.printf("angle2 = %s%n", angle2);
        }

        stop();
    }
}
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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility methods for manipulating skeletonized spatials, skeletons, and bones.
 * All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MySkeleton {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MySkeleton.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
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
     * Copy the bind transform of the specified bone.
     *
     * @param bone which bone to use (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return transform in local coordinates (either storeResult or a new
     * instance)
     */
    public static Transform copyBindTransform(Bone bone,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Vector3f translation = bone.getBindPosition();
        storeResult.setTranslation(translation);

        Quaternion rotation = bone.getBindRotation();
        storeResult.setRotation(rotation);

        Vector3f scale = bone.getBindScale();
        if (scale == null) {
            scale = scaleIdentity;
        }
        storeResult.setScale(scale);

        return storeResult;
    }

    /**
     * Test whether the indexed bone descends from the indexed ancestor in the
     * specified skeleton.
     *
     * @param boneIndex index of bone to test (&ge;0)
     * @param ancestorIndex index of ancestor bone (&ge;0)
     * @param skeleton (not null, unaffected)
     * @return true if descended from the parent, otherwise false
     */
    public static boolean descendsFrom(int boneIndex, int ancestorIndex,
            Skeleton skeleton) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNegative(ancestorIndex, "ancestor index");

        Bone bone = skeleton.getBone(boneIndex);
        Bone ancestor = skeleton.getBone(ancestorIndex);
        while (bone != null) {
            bone = bone.getParent();
            if (bone == ancestor) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find a named bone in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial to search (not null, alias created)
     * @param boneName name of the bone to access (not null)
     * @return a pre-existing instance (or null if not found)
     */
    public static Bone findBone(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Bone result = null;
        int numControls = spatial.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            Skeleton skeleton = MyControl.findSkeleton(control);
            if (skeleton != null) {
                result = skeleton.getBone(boneName);
                break;
            }
        }

        return result;
    }

    /**
     * Find a skeleton of the specified spatial.
     *
     * @param spatial which spatial to search (not null, alias created)
     * @return a pre-existing instance, or null if none found
     */
    public static Skeleton findSkeleton(Spatial spatial) {
        Skeleton skeleton = null;
        AnimControl animControl = spatial.getControl(AnimControl.class);
        if (animControl != null) {
            skeleton = animControl.getSkeleton();
        }

        if (skeleton == null) {
            SkeletonControl skeletonControl;
            skeletonControl = spatial.getControl(SkeletonControl.class);
            if (skeletonControl != null) {
                skeleton = skeletonControl.getSkeleton();
            }
        }

        return skeleton;
    }

    /**
     * Enumerate all named bones in the specified skeleton.
     *
     * @param skeleton which skeleton (not null, unaffected)
     * @param addResult (added to if not null)
     * @return a list of names in arbitrary order, without any duplicates
     * (either addResult or a new list)
     */
    public static List<String> listBones(Skeleton skeleton,
            List<String> addResult) {
        int boneCount = skeleton.getBoneCount();
        if (addResult == null) {
            addResult = new ArrayList<>(boneCount);
        }

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (bone != null) {
                String name = bone.getName();
                if (name != null && !addResult.contains(name)) {
                    addResult.add(name);
                }
            }
        }

        return addResult;
    }

    /**
     * Enumerate the names of all bones in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial (not null, unaffected)
     * @return a new list of names in lexicographic order, without any
     * duplicates (may be empty)
     */
    public static List<String> listBones(Spatial spatial) {
        List<String> result = new ArrayList<>(80);

        int numControls = spatial.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            Skeleton skeleton = MyControl.findSkeleton(control);
            if (skeleton != null) {
                listBones(skeleton, result);
            }
        }

        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all skeleton instances in the specified subtree of a scene
     * graph. Note: recursive!
     *
     * @param subtree (not null, aliases created)
     * @param addResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Skeleton> listSkeletons(Spatial subtree,
            List<Skeleton> addResult) {
        Validate.nonNull(subtree, "subtree");
        if (addResult == null) {
            addResult = new ArrayList<>(4);
        }

        int numControls = subtree.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            Control control = subtree.getControl(controlIndex);
            Skeleton skeleton = MyControl.findSkeleton(control);
            if (skeleton != null && !addResult.contains(skeleton)) {
                addResult.add(skeleton);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listSkeletons(child, addResult);
            }
        }

        return addResult;
    }

    /**
     * Convert a location in a bone's local space to a location in model space.
     * TODO storeResult
     *
     * @param bone (not null, unaffected)
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
        // TODO rotation??
        result.addLocal(tail);

        return result;
    }

    /**
     * Count the number of leaf bones in the specified skeleton.
     *
     * @param skeleton (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int numLeafBones(Skeleton skeleton) {
        int boneCount = skeleton.getBoneCount();
        int result = 0;
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            List<Bone> children = bone.getChildren();
            if (children.isEmpty()) {
                ++result;
            }
        }

        return result;
    }

    /**
     * Count the number of root bones in the specified skeleton.
     *
     * @param skeleton (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int numRootBones(Skeleton skeleton) {
        Bone[] roots = skeleton.getRoots();
        int result = roots.length;

        return result;
    }

    /**
     * Alter the name of the specified bone. The caller is responsible for
     * avoiding duplicate names.
     *
     * @param bone bone to change (not null, modified)
     * @param newName name to apply
     * @return true if successful, otherwise false
     */
    public static boolean setName(Bone bone, String newName) {
        Class<?> boneClass = bone.getClass();
        Field nameField;
        try {
            nameField = boneClass.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return false;
        }
        nameField.setAccessible(true);
        try {
            /*
             * Rename the bone.
             */
            nameField.set(bone, newName);
        } catch (IllegalAccessException e) {
            return false;
        }
        /*
         * Find the attach node, if any.
         */
        Field attachNodeField;
        try {
            attachNodeField = boneClass.getDeclaredField("attachNode");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException();
        }
        attachNodeField.setAccessible(true);
        Node attachNode;
        try {
            attachNode = (Node) attachNodeField.get(bone);
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
        if (attachNode != null) {
            /*
             * Also rename the attach node.
             */
            String newNodeName = newName + "_attachnode";
            attachNode.setName(newNodeName);
        }

        return true;
    }

    /**
     * Alter all the user-control flags in the specified skeleton.
     *
     * @param skeleton skeleton to alter (not null, modified)
     * @param newSetting true to enable user control, false to disable
     */
    public static void setUserControl(Skeleton skeleton, boolean newSetting) {
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserControl(newSetting);
        }
    }

    /**
     * Alter all the user-control flags in the specified subtree.
     *
     * @param subtree subtree to alter (not null)
     * @param newSetting true to enable user control, false to disable
     */
    public static void setUserControl(Spatial subtree, boolean newSetting) {
        Validate.nonNull(subtree, "spatial");

        List<Skeleton> skeletons = listSkeletons(subtree, null);
        for (Skeleton skeleton : skeletons) {
            setUserControl(skeleton, newSetting);
        }
    }

    /**
     * Calculate the world location of (the tail of) a named bone. TODO
     * storeResult
     *
     * @param spatial skeletonized spatial that contains the bone (not null,
     * unaffected)
     * @param boneName (not null)
     * @return new vector in world coordinates
     */
    public static Vector3f worldLocation(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Bone bone = findBone(spatial, boneName);
        Vector3f localCoordinates = bone.getModelSpacePosition();

        Vector3f result;
        if (MySpatial.isIgnoringTransforms(spatial)) { // TODO JME 3.2
            result = localCoordinates.clone();
        } else {
            result = spatial.localToWorld(localCoordinates, null);
        }

        return result;
    }

    /**
     * Calculate the world orientation of a bone. TODO storeResult
     *
     * @param spatial skeletonized spatial that contains the bone (not null,
     * unaffected)
     * @param bone (not null, unaffected)
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
     * Calculate the world orientation of a named bone. TODO storeResult
     *
     * @param spatial skeletonized spatial that contains the bone (not null)
     * @param boneName (not null)
     * @return new instance
     */
    public static Quaternion worldOrientation(Spatial spatial,
            String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone");

        Bone bone = findBone(spatial, boneName);
        Quaternion result = worldOrientation(spatial, bone);

        return result;
    }
}

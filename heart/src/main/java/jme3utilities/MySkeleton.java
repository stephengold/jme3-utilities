/*
 Copyright (c) 2013-2019, Stephen Gold
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility methods for manipulating skeletonized spatials, skeletons, and bones.
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
     * Cancel the attachments node (if any) of the specified Bone. The invoker
     * is responsible for removing the Node from the scene graph.
     *
     * @param bone the Bone to modify (not null, modified)
     */
    public static void cancelAttachments(Bone bone) {
        Field attachNodeField;
        try {
            attachNodeField = Bone.class.getDeclaredField("attachNode");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException();
        }
        attachNodeField.setAccessible(true);

        try {
            attachNodeField.set(bone, null);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException();
        }
    }

    /**
     * Copy the bind transform of the specified Bone.
     *
     * @param bone which Bone to read (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the bone's bind transform (in its parent's coordinates, either
     * storeResult or a new instance)
     */
    public static Transform copyBindTransform(Bone bone,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Vector3f translation = bone.getBindPosition();
        result.setTranslation(translation);

        Quaternion rotation = bone.getBindRotation(); // alias
        result.setRotation(rotation);

        Vector3f scale = bone.getBindScale();
        if (scale == null) {
            scale = scaleIdentity;
        }
        result.setScale(scale);

        return result;
    }

    /**
     * Copy the transform of the specified Bone relative to its parent.
     *
     * @param bone which Bone to read (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the bone's Transform (in its parent's coordinates, either
     * storeResult or a new instance)
     */
    public static Transform copyLocalTransform(Bone bone,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Vector3f translation = bone.getLocalPosition();
        result.setTranslation(translation);

        Quaternion rotation = bone.getLocalRotation();
        result.setRotation(rotation);

        Vector3f scale = bone.getLocalScale();
        result.setScale(scale);

        return result;
    }

    /**
     * Copy the transform of the specified Bone relative to its mesh(es).
     *
     * @param bone which Bone to read (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the bone's Transform (in mesh coordinates, either storeResult or
     * a new instance)
     */
    public static Transform copyMeshTransform(Bone bone,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Vector3f translation = bone.getModelSpacePosition();
        result.setTranslation(translation);

        Quaternion rotation = bone.getModelSpaceRotation();
        result.setRotation(rotation);

        Vector3f scale = bone.getModelSpaceScale();
        result.setScale(scale);

        return result;
    }

    /**
     * Count the leaf bones in the specified Skeleton.
     *
     * @param skeleton the Skeleton to read (not null, unaffected)
     * @return the count (&ge;0)
     */
    public static int countLeafBones(Skeleton skeleton) {
        int boneCount = skeleton.getBoneCount();
        int result = 0;
        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex);
            List<Bone> children = bone.getChildren();
            if (children.isEmpty()) {
                ++result;
            }
        }

        return result;
    }

    /**
     * Count the root bones in the specified Skeleton.
     *
     * @param skeleton the Skeleton to read (not null, unaffected)
     * @return the count (&ge;0)
     */
    public static int countRootBones(Skeleton skeleton) {
        Bone[] roots = skeleton.getRoots();
        int result = roots.length;

        return result;
    }

    /**
     * Test whether the indexed bone descends from the indexed ancestor in the
     * specified Skeleton.
     *
     * @param boneIndex the index of Bone to test (&ge;0)
     * @param ancestorIndex the index of the ancestor Bone (&ge;0)
     * @param skeleton the Skeleton to read (not null, unaffected)
     * @return true if descends from the ancestor, otherwise false
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
     * Find a named Bone in a skeletonized Spatial.
     *
     * @param spatial the skeletonized Spatial to search (not null, alias
     * created)
     * @param boneName the name of the Bone to access (not null)
     * @return a pre-existing instance, or null if not found
     */
    public static Bone findBone(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Bone result = null;
        int numControls = spatial.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
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
     * Find a Skeleton of the specified Spatial.
     *
     * @param spatial the Spatial to search (not null, alias created)
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
     * Access the attachments node of the specified Bone.
     * <p>
     * Unlike
     * {@link com.jme3.animation.SkeletonControl#getAttachmentsNode(java.lang.String)}
     * this won't add a node to the scene graph.
     *
     * @param bone the Bone to read (not null, unaffected)
     * @return the pre-existing instance, or null if none
     */
    public static Node getAttachments(Bone bone) {
        Field attachNodeField;
        try {
            attachNodeField = Bone.class.getDeclaredField("attachNode");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
        attachNodeField.setAccessible(true);

        Node result;
        try {
            result = (Node) attachNodeField.get(bone);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }

    /**
     * Enumerate all named bones in the specified skeleton.
     *
     * @param skeleton the skeleton to search (not null, unaffected)
     * @param addResult storage for results (added to if not null)
     * @return a list of names in arbitrary order, without any duplicates
     * (either addResult or a new list)
     */
    public static List<String> listBones(Skeleton skeleton,
            List<String> addResult) {
        int boneCount = skeleton.getBoneCount();
        if (addResult == null) {
            addResult = new ArrayList<>(boneCount);
        }

        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
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
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
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
     * @param addResult storage for results (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Skeleton> listSkeletons(Spatial subtree,
            List<Skeleton> addResult) {
        Validate.nonNull(subtree, "subtree");
        if (addResult == null) {
            addResult = new ArrayList<>(4);
        }

        int numControls = subtree.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; ++controlIndex) {
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
     * Map all attachments in the specified skeleton.
     *
     * @param skeleton the skeleton to search (not null, unaffected)
     * @param storeResult storage for results (added to if not null)
     * @return an expanded map (either storeResult or a new instance)
     */
    public static Map<Bone, Spatial> mapAttachments(Skeleton skeleton,
            Map<Bone, Spatial> storeResult) {
        Validate.nonNull(skeleton, "skeleton");
        if (storeResult == null) {
            storeResult = new HashMap<>(4);
        }

        int numBones = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex);
            Node attachmentsNode = getAttachments(bone);
            if (attachmentsNode != null) {
                if (storeResult.containsKey(bone)) {
                    if (storeResult.get(bone) != attachmentsNode) {
                        throw new IllegalStateException();
                    }
                } else {
                    storeResult.put(bone, attachmentsNode);
                }
            }

        }

        return storeResult;
    }

    /**
     * Map all attachments nodes in the specified subtree of a scene graph.
     *
     * @param subtree (not null, unaffected)
     * @param storeResult storage for results (added to if not null)
     * @return an expanded map (either storeResult or a new instance)
     */
    public static Map<Bone, Spatial> mapAttachments(Spatial subtree,
            Map<Bone, Spatial> storeResult) {
        Validate.nonNull(subtree, "subtree");
        if (storeResult == null) {
            storeResult = new HashMap<>(4);
        }

        List<SkeletonControl> list
                = MySpatial.listControls(subtree, SkeletonControl.class, null);
        for (SkeletonControl control : list) {
            Skeleton skeleton = control.getSkeleton();
            mapAttachments(skeleton, storeResult);
        }

        return storeResult;
    }

    /**
     * Enumerate all bones in a pre-order, depth-first traversal of the
     * specified Skeleton, such that child bones never precede their ancestors.
     *
     * @param skeleton the Skeleton to traverse (not null, unaffected)
     * @return a new list of pre-existing bones
     */
    public static List<Bone> preOrderBones(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        List<Bone> result = new ArrayList<>(numBones);
        Bone[] rootBones = skeleton.getRoots();
        for (Bone rootBone : rootBones) {
            addPreOrderBones(rootBone, result);
        }

        assert result.size() == numBones : result.size();
        return result;
    }

    /**
     * Rename of the specified bone. The caller is responsible for avoiding
     * duplicate names.
     *
     * @param bone the Bone to rename (not null, modified)
     * @param newName the desired name
     * @return true if successful, otherwise false
     */
    public static boolean setName(Bone bone, String newName) {
        Field nameField;
        try {
            nameField = Bone.class.getDeclaredField("name");
        } catch (NoSuchFieldException exception) {
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
         * Find the attachments node, if any.
         */
        Node attachmentsNode = getAttachments(bone);
        if (attachmentsNode != null) {
            /*
             * Also rename the attach node.
             */
            String newNodeName = newName + "_attachnode";
            attachmentsNode.setName(newNodeName);
        }

        return true;
    }

    /**
     * Alter the transform of the specified bone relative to its parent. User
     * control is temporarily overridden. The model transform is not updated.
     *
     * @param bone which bone to use (not null, unaffected)
     * @param transform the desired bone transform (in its parent's coordinates,
     * not null, unaffected)
     */
    public static void setLocalTransform(Bone bone, Transform transform) {
        boolean hadControl = bone.hasUserControl();
        if (!hadControl) {
            bone.setUserControl(true);
        }

        Vector3f translation = transform.getTranslation();
        bone.setLocalTranslation(translation);

        Quaternion rotation = transform.getRotation();
        bone.setLocalRotation(rotation);

        Vector3f scale = transform.getScale();
        bone.setLocalScale(scale);

        if (!hadControl) {
            bone.setUserControl(false);
        }
    }

    /**
     * Alter all the user-control flags in the specified Skeleton.
     *
     * @param skeleton the Skeleton to alter (not null, modified)
     * @param newSetting true to select user control, false to select animation
     * control
     */
    public static void setUserControl(Skeleton skeleton, boolean newSetting) {
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserControl(newSetting);
        }
    }

    /**
     * Alter all the user-control flags in the specified subtree of the scene
     * graph.
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
    // *************************************************************************
    // private methods

    /**
     * Helper method: append the specified Bone and all its descendants to the
     * specified List, using a pre-order, depth-first traversal of the Skeleton,
     * such that child bones never precede their ancestors. Note: recursive!
     *
     * @param bone the next Bone to append (not null, aliases created)
     * @param addResult the List to append to (modified)
     */
    private static void addPreOrderBones(Bone bone, List<Bone> addResult) {
        assert bone != null;
        addResult.add(bone);
        List<Bone> children = bone.getChildren();
        for (Bone child : children) {
            addPreOrderBones(child, addResult);
        }
    }
}

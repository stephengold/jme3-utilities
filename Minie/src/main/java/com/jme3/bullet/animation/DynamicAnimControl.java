/*
 * Copyright (c) 2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.animation;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Before adding this control to a spatial, configure it by invoking
 * {@link #link(java.lang.String, float, com.jme3.bullet.animation.JointPreset)}
 * for each bone that should have its own rigid body. Leave some unlinked bones
 * near the root of the skeleton to form the torso of the ragdoll.
 * <p>
 * When you add the control to a spatial and set its physics space, it generates
 * a rigid body with a hull collision shape for the torso and for each linked
 * bone. It also creates a SixDofJoint connecting each linked bone to its parent
 * in the link hierarchy. The mass of each rigid body and the range-of-motion of
 * each joint can be reconfigured on the fly.
 * <p>
 * Each link is either dynamic (driven by gravity and collisions) or kinematic
 * (unaffected by gravity and collisions).
 *
 * TODO handle applyLocal, catch ignoreTransforms, ghost mode
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class DynamicAnimControl
        extends ConfigDynamicAnimControl
        implements PhysicsCollisionListener, PhysicsTickListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger3
            = Logger.getLogger(DynamicAnimControl.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix3f#IDENTITY}
     */
    final private static Matrix3f matrixIdentity = new Matrix3f();
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * viscous damping ratio for new rigid bodies (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    private float damping = 0.6f;
    /**
     * minimum applied impulse for a collision event to be dispatched to
     * listeners (default=0)
     */
    private float eventDispatchImpulseThreshold = 0f;
    /**
     * bone links in a pre-order, depth-first traversal of the link hierarchy
     * TODO include attachment links?
     */
    private List<BoneLink> boneLinkList = null;
    /**
     * list of registered collision listeners
     */
    private List<RagdollCollisionListener> listeners
            = new SafeArrayList<>(RagdollCollisionListener.class);
    /**
     * map bone names to attachment links
     */
    private Map<String, AttachmentLink> attachmentLinks = new HashMap<>(8);
    /**
     * map bone names to bone links
     */
    private Map<String, BoneLink> boneLinks = new HashMap<>(32);
    /**
     * skeleton being controlled
     */
    private Skeleton skeleton = null;
    /**
     * spatial that provides the mesh-coordinate transform
     */
    private Spatial transformer = null;
    /**
     * torso link for this control
     */
    private TorsoLink torsoLink = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control without any linked bones (torso only).
     */
    public DynamicAnimControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a collision listener to this control.
     *
     * @param listener (not null, alias created)
     */
    public void addCollisionListener(RagdollCollisionListener listener) {
        Validate.nonNull(listener, "listener");
        listeners.add(listener);
    }

    /**
     * Begin blending the named linked bone and all its descendants into an
     * amputated state. This has the effect of hiding the descendants.
     *
     * @param boneName the name of the linked bone to amputate (not null, not
     * empty)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    public void amputateHierarchy(String boneName, float blendInterval) {
        if (!isBoneLinkName(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNegative(blendInterval, "blend interval");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        blendDescendants(boneName, KinematicSubmode.Amputated, blendInterval);

        BoneLink boneLink = getBoneLink(boneName);
        boneLink.blendToKinematicMode(KinematicSubmode.Amputated,
                blendInterval);
    }

    /**
     * Begin blending the named link and all its descendants into bind pose.
     *
     * @param linkName the name of the link (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    public void bindHierarchy(String linkName, float blendInterval) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNegative(blendInterval, "blend interval");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        blendDescendants(linkName, KinematicSubmode.Bound, blendInterval);

        if (linkName.equals(torsoName)) {
            torsoLink.blendToKinematicMode(KinematicSubmode.Bound,
                    blendInterval, null);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.blendToKinematicMode(KinematicSubmode.Bound,
                    blendInterval);
        }
    }

    /**
     * Begin blending all links to fully kinematic mode, driven by animation.
     * TODO callback at end of transition
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the controlled
     * spatial when the transition completes or null for no change to local
     * transform (unaffected)
     */
    public void blendToKinematicMode(float blendInterval,
            Transform endModelTransform) {
        Validate.nonNegative(blendInterval, "blend interval");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        torsoLink.blendToKinematicMode(KinematicSubmode.Animated, blendInterval,
                endModelTransform);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.blendToKinematicMode(KinematicSubmode.Animated,
                    blendInterval);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.blendToKinematicMode(blendInterval, null);
        }
    }

    /**
     * Enumerate all immediate children (in the link hierarchy) of the named
     * link.
     *
     * @param linkName the name of the link (not null)
     * @return a new list of bone names
     */
    public List<String> childNames(String linkName) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }

        List<String> result = new ArrayList<>(8);
        for (String childName : linkedBoneNames()) {
            BoneLink boneLink = getBoneLink(childName);
            if (boneLink.parentName().equals(linkName)) {
                result.add(childName);
            }
        }

        return result;
    }

    /**
     * Count the immediate children (in the link hierarchy) of the named link.
     *
     * @param linkName the name of the parent link (not null)
     * @return count (&ge;0)
     */
    public int countChildren(String linkName) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }

        int result = 0;
        for (String childName : linkedBoneNames()) {
            BoneLink boneLink = getBoneLink(childName);
            if (boneLink.parentName().equals(linkName)) {
                ++result;
            }
        }

        return result;
    }

    /**
     * Read the damping ratio for new rigid bodies.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float damping() {
        assert damping >= 0f : damping;
        return damping;
    }

    /**
     * Immediately freeze the named link and all its descendants. Note:
     * recursive!
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param linkName the name of the link (not null)
     */
    public void freezeHierarchy(String linkName) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        if (linkName.equals(torsoName)) {
            torsoLink.freeze();
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.freeze();
        }

        List<String> children = childNames(linkName);
        for (String childName : children) {
            freezeHierarchy(childName);
        }

        for (AttachmentLink link : attachmentLinks.values()) {
            if (link.managerName().equals(linkName)) {
                link.freeze();
            }
        }
    }

    /**
     * Access the named bone.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param boneName the name of the skeleton bone to access
     * @return the pre-existing instance, or null if not found
     */
    public Bone getBone(String boneName) {
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot access bones unless added to a spatial.");
        }

        Bone result = skeleton.getBone(boneName);
        return result;
    }

    /**
     * Access the physics link for the named bone. Returns null if bone is not
     * linked, or if the control is not added to a spatial.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @return the pre-existing spatial, or null
     */
    public BoneLink getBoneLink(String boneName) {
        Validate.nonEmpty(boneName, "bone name");
        BoneLink boneLink = boneLinks.get(boneName);
        return boneLink;
    }

    /**
     * Read the event-dispatch impulse threshold of this control.
     *
     * @return the threshold value (&ge;0)
     */
    public float getEventDispatchImpulseThreshold() {
        assert eventDispatchImpulseThreshold >= 0f;
        return eventDispatchImpulseThreshold;
    }

    /**
     * Access the rigid body of the named bone/torso link. Returns null if the
     * control is not added to a spatial.
     *
     * @param linkName the name of the link (not null)
     * @return the pre-existing instance, or null
     */
    public PhysicsRigidBody getRigidBody(String linkName) {
        PhysicsRigidBody result;

        if (getSpatial() == null) {
            result = null;

        } else if (torsoName.equals(linkName)) {
            result = torsoLink.getRigidBody();

        } else if (isBoneLinkName(linkName)) {
            BoneLink boneLink = boneLinks.get(linkName);
            result = boneLink.getRigidBody();

        } else {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }

        return result;
    }

    /**
     * Access the skeleton. Returns null if the control is not added to a
     * spatial.
     *
     * @return the pre-existing skeleton, or null
     */
    Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * Access the spatial with the mesh-coordinate transform. Returns null if
     * the control is not added to a spatial.
     *
     * @return the pre-existing spatial, or null
     */
    Spatial getTransformer() {
        return transformer;
    }

    /**
     * Access the physics link for the torso. Returns null if the control is not
     * added to a spatial.
     *
     * @return the pre-existing spatial, or null
     */
    public TorsoLink getTorsoLink() {
        return torsoLink;
    }

    /**
     * Enumerate all managed bones of the named link, in a pre-order,
     * depth-first traversal of the skeleton, such that child bones never
     * precede their ancestors.
     *
     * @param managerName the name of the managing link (not null)
     * @return a new array of managed bones, including the manager if it is not
     * the torso
     */
    Bone[] listManagedBones(String managerName) {
        List<Bone> list = new ArrayList<>(8);

        if (torsoName.equals(managerName)) {
            Bone[] roots = skeleton.getRoots();
            for (Bone rootBone : roots) {
                list.add(rootBone);
                addUnlinkedDescendants(rootBone, list);
            }

        } else {
            BoneLink manager = getBoneLink(managerName);
            if (manager == null) {
                String msg = "No link named " + MyString.quote(managerName);
                throw new IllegalArgumentException(msg);
            }
            Bone managerBone = manager.getBone();
            list.add(managerBone);
            addUnlinkedDescendants(managerBone, list);
        }
        /*
         * Convert the list to an array.
         */
        int numManagedBones = list.size();
        Bone[] array = new Bone[numManagedBones];
        list.toArray(array);

        return array;
    }

    /**
     * Copy the model's mesh-to-world transform.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the model's mesh transform (in world coordinates, either
     * storeResult or a new transform, not null)
     */
    Transform meshTransform(Transform storeResult) {
        Transform result = MySpatial.worldTransform(transformer, storeResult);
        return result;
    }

    /**
     * Find the parent (in the link hierarchy) of the named BoneLink.
     *
     * @param childName the name of the linked bone (not null, not empty)
     * @return the bone name or torsoFakeBoneName (not null)
     */
    public String parentName(String childName) {
        if (!isBoneLinkName(childName)) {
            String msg = "No BoneLink named " + MyString.quote(childName);
            throw new IllegalArgumentException(msg);
        }

        String result = torsoName;

        Bone child = getBone(childName);
        Bone parent = child.getParent();
        while (parent != null) {
            String name = parent.getName();
            if (isBoneLinkName(name)) {
                result = name;
                break;
            }
            parent = parent.getParent();
        }

        assert result != null;
        return result;
    }

    /**
     * Calculate the physics transform to match the specified skeleton bone.
     *
     * @param bone the skeleton bone to match (not null, unaffected)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated physics transform (either storeResult or a new
     * transform, not null)
     */
    Transform physicsTransform(Bone bone, Vector3f localOffset,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        /*
         * Start with the body's transform in the bone's local coordinates.
         */
        result.setTranslation(localOffset);
        result.setRotation(rotateIdentity);
        result.setScale(1f);
        /*
         * Convert to mesh coordinates.
         */
        Transform localToMesh = MySkeleton.copyMeshTransform(bone, null);
        result.combineWithParent(localToMesh);
        /*
         * Convert to world/physics coordinates.
         */
        Transform meshToWorld = meshTransform(null);
        result.combineWithParent(meshToWorld);

        return result;
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale to the model
     * after it was initialized.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void rebuild() {
        Spatial controlledSpatial = getSpatial();
        if (controlledSpatial == null) {
            throw new IllegalStateException(
                    "Cannot rebuild unless added to a spatial.");
        }

        Map<String, BoneLink> saveBones = new HashMap<>(boneLinks);
        TorsoLink saveTorso = torsoLink;

        removeSpatialData(controlledSpatial);
        createSpatialData(controlledSpatial);

        for (Map.Entry<String, BoneLink> entry : boneLinks.entrySet()) {
            String name = entry.getKey();
            BoneLink newLink = entry.getValue();
            BoneLink oldLink = saveBones.get(name);
            newLink.postRebuild(oldLink);
        }
        torsoLink.postRebuild(saveTorso);
    }

    /**
     * Remove all inverse-kinematics targets.
     */
    public void removeAllIKTargets() {
        // TODO
    }

    /**
     * Remove any inverse-kinematics target for the specified bone.
     *
     * @param bone which bone has the target (not null)
     */
    public void removeIKTarget(Bone bone) {
        // TODO
    }

    /**
     * Alter the CCD motion threshold of all rigid bodies in this control.
     *
     * @see PhysicsRigidBody#setCcdMotionThreshold(float)
     * @param speed the desired threshold speed (in physics-space units per
     * second, &gt;0) or zero to disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float speed) {
        Validate.nonNegative(speed, "speed");

        torsoLink.getRigidBody().setCcdMotionThreshold(speed);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.getRigidBody().setCcdMotionThreshold(speed);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.getRigidBody().setCcdMotionThreshold(speed);
        }
    }

    /**
     * Alter the CCD swept-sphere radius of all rigid bodies in the ragdoll.
     *
     * @see PhysicsRigidBody#setCcdSweptSphereRadius(float)
     * @param radius the desired radius of the sphere used for continuous
     * collision detection (&ge;0)
     */
    public void setCcdSweptSphereRadius(float radius) {
        Validate.nonNegative(radius, "radius");

        torsoLink.getRigidBody().setCcdSweptSphereRadius(radius);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.getRigidBody().setCcdSweptSphereRadius(radius);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.getRigidBody().setCcdSweptSphereRadius(radius);
        }
    }

    /**
     * Alter the viscous damping ratio for new rigid bodies.
     *
     * @param dampingRatio the desired damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    public void setDamping(float dampingRatio) {
        Validate.nonNegative(dampingRatio, "damping ratio");

        damping = dampingRatio;

        if (getSpatial() != null) {
            torsoLink.getRigidBody().setDamping(damping, damping);
            for (BoneLink boneLink : boneLinkList) {
                boneLink.getRigidBody().setDamping(damping, damping);
            }
            for (AttachmentLink link : attachmentLinks.values()) {
                link.getRigidBody().setDamping(damping, damping);
            }
        }
    }

    /**
     * Immediately put the named link into dynamic mode.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param linkName the name of the link (not null, not empty)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     * @param lockX true to lock the joint's X-axis (if not the torso)
     * @param lockY true to lock the joint's Y-axis (if not the torso)
     * @param lockZ true to lock the joint's Z-axis (if not the torso)
     */
    public void setDynamic(String linkName, Vector3f uniformAcceleration,
            boolean lockX, boolean lockY, boolean lockZ) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNull(uniformAcceleration, "uniform acceleration");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        if (linkName.equals(torsoName)) {
            torsoLink.setDynamic(uniformAcceleration);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.setDynamic(uniformAcceleration, lockX, lockY, lockZ);
        }
    }

    /**
     * Immediately put the named link and all its descendants into dynamic mode.
     * Note: recursive!
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param linkName the name of the link at the root of the subtree (not
     * null)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     * @param lockAll true to lock all axes of all links (except the torso)
     */
    public void setDynamicHierarchy(String linkName,
            Vector3f uniformAcceleration, boolean lockAll) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNull(uniformAcceleration, "uniform acceleration");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        if (linkName.equals(torsoName)) {
            torsoLink.setDynamic(uniformAcceleration);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.setDynamic(uniformAcceleration, lockAll, lockAll, lockAll);
        }

        List<String> childNames = childNames(linkName);
        for (String childName : childNames) {
            setDynamicHierarchy(childName, uniformAcceleration, lockAll);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            if (link.managerName().equals(linkName)) {
                link.setDynamic(uniformAcceleration);
            }
        }
    }

    /**
     * Alter the the event-dispatch impulse threshold of this control.
     *
     * @param threshold the desired threshold (&ge;0)
     */
    public void setEventDispatchImpulseThreshold(float threshold) {
        Validate.nonNegative(threshold, "threshold");
        eventDispatchImpulseThreshold = threshold;
    }

    /**
     * Add a target for inverse kinematics.
     *
     * @param bone which bone the target applies to (not null)
     * @param worldGoal the world coordinates of the goal (not null)
     */
    public void setIKTarget(Bone bone, Vector3f worldGoal) {
        // TODO
    }

    /**
     * Immediately put all links into fully kinematic mode.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void setKinematicMode() {
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        Transform localTransform = getSpatial().getLocalTransform();
        blendToKinematicMode(0f, localTransform);
    }

    /**
     * Immediately put all links into fully dynamic mode with gravity.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void setRagdollMode() {
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        Vector3f ragdollGravity = gravity(null);

        torsoLink.setDynamic(ragdollGravity);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.setDynamic(ragdollGravity, false, false, false);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.setDynamic(ragdollGravity);
        }
    }
    // *************************************************************************
    // ConfigDynamicAnimControl methods

    /**
     * Add all managed physics objects to the physics space.
     */
    @Override
    protected void addPhysics() {
        PhysicsSpace space = getPhysicsSpace();
        Vector3f gravity = gravity(null);

        PhysicsRigidBody rigidBody = torsoLink.getRigidBody();
        space.add(rigidBody);
        rigidBody.setGravity(gravity);

        for (BoneLink boneLink : boneLinkList) {
            rigidBody = boneLink.getRigidBody();
            space.add(rigidBody);
            rigidBody.setGravity(gravity);

            PhysicsJoint joint = boneLink.getJoint();
            space.add(joint);
        }

        for (AttachmentLink link : attachmentLinks.values()) {
            rigidBody = link.getRigidBody();
            space.add(rigidBody);
            rigidBody.setGravity(gravity);

            PhysicsJoint joint = link.getJoint();
            space.add(joint);
        }

        space.addCollisionListener(this);
        space.addTickListener(this);
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        boneLinkList = cloner.clone(boneLinkList);
        listeners = cloner.clone(listeners);
        attachmentLinks = cloner.clone(attachmentLinks);
        boneLinks = cloner.clone(boneLinks);
        skeleton = cloner.clone(skeleton);
        transformer = cloner.clone(transformer);
        torsoLink = cloner.clone(torsoLink);
    }

    /**
     * Create spatial-dependent data. Invoked each time the control is added to
     * a spatial.
     *
     * @param spatial the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial spatial) {
        Validate.nonNull(spatial, "controlled spatial");

        SkeletonControl skeletonControl
                = spatial.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "The controlled spatial must have a SkeletonControl. "
                    + "Make sure the control is there and not on a subnode.");
        }
        sortControls(skeletonControl);
        /*
         * Analyze the model's skeleton.
         */
        skeleton = skeletonControl.getSkeleton();
        RagUtils.validate(skeleton); // TODO warn if any root bone is linked
        String[] tempManagerMap = managerMap(skeleton);
        int numBones = skeleton.getBoneCount();
        /*
         * Temporarily set all local translations and rotations to bind.
         */
        MySkeleton.setUserControl(skeleton, true);
        Transform[] savedTransforms = new Transform[numBones];
        Vector3f userScale = new Vector3f();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            savedTransforms[boneIndex]
                    = MySkeleton.copyLocalTransform(bone, null);

            userScale.set(bone.getLocalScale());
            userScale.divideLocal(bone.getBindScale());
            bone.setUserTransforms(translateIdentity, rotateIdentity,
                    userScale);
        }
        MySkeleton.setUserControl(skeleton, false);
        skeleton.updateWorldVectors();
        /*
         * Find the target meshes and the main bone.  Don't invoke
         * skeletonControl.getTargets() here, since the SkeletonControl
         * might not be initialized yet.
         */
        List<Mesh> targetList = MySpatial.listAnimatedMeshes(spatial, null);
        Mesh[] targets = new Mesh[targetList.size()];
        targetList.toArray(targets);
        /*
         * Analyze the model's coordinate systems.
         */
        transformer = MySpatial.findAnimatedGeometry(spatial);
        if (transformer == null) {
            transformer = spatial;
        }
        /*
         * Analyze the model's meshes.
         */
        Map<String, List<Vector3f>> coordsMap
                = RagUtils.coordsMap(targets, tempManagerMap);
        /*
         * Create the torso link.
         */
        List<Vector3f> vertexLocations = coordsMap.get(torsoName);
        createTorsoLink(vertexLocations, targets);
        /*
         * Create bone links.
         */
        Collection<String> linkedBoneNames = linkedBoneNames();
        int numLinkedBones = countLinkedBones();
        assert linkedBoneNames.size() == numLinkedBones;
        for (String boneName : linkedBoneNames) {
            vertexLocations = coordsMap.get(boneName);
            createBoneLink(boneName, vertexLocations);
        }
        assert boneLinks.size() == numLinkedBones;
        /*
         * Add joints to connect each link with its parent in the hierarchy.
         * Also initialize the boneLinkList.
         */
        boneLinkList = new ArrayList<>(numLinkedBones);
        addJoints(torsoName);
        assert boneLinkList.size() == numLinkedBones;
        /*
         * Create attachment links with joints.
         */
        Collection<String> attachBoneNames = attachmentBoneNames();
        for (String boneName : attachBoneNames) {
            createAttachmentLink(boneName, skeletonControl, tempManagerMap);
        }
        /*
         * Restore the skeleton's pose.
         */
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            MySkeleton.setLocalTransform(bone, savedTransforms[boneIndex]);
        }
        skeleton.updateWorldVectors();

        if (added) {
            addPhysics();
        }

        logger3.log(Level.FINE, "Created ragdoll for skeleton.");
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public DynamicAnimControl jmeClone() {
        try {
            DynamicAnimControl clone = (DynamicAnimControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        damping = ic.readFloat("damping", 0.6f);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);

        // TODO attachmentLinks, boneLinkList, listeners
        BoneLink[] loadedBoneLinks
                = (BoneLink[]) ic.readSavableArray("boneList", new BoneLink[0]);
        for (BoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.getBone().getName(), physicsBoneLink);
        }

        skeleton = (Skeleton) ic.readSavable("skeleton", null);
        transformer = (Spatial) ic.readSavable("transformer", null);
        torsoLink = (TorsoLink) ic.readSavable("torsoLink", null);
    }

    /**
     * Remove all managed physics objects from the physics space.
     */
    @Override
    protected void removePhysics() {
        assert added;
        PhysicsSpace space = getPhysicsSpace();

        PhysicsRigidBody rigidBody = torsoLink.getRigidBody();
        space.remove(rigidBody);

        for (BoneLink boneLink : boneLinks.values()) {
            rigidBody = boneLink.getRigidBody();
            space.remove(rigidBody);

            PhysicsJoint joint = boneLink.getJoint();
            space.remove(joint);
        }

        for (AttachmentLink link : attachmentLinks.values()) {
            rigidBody = link.getRigidBody();
            space.remove(rigidBody);

            PhysicsJoint joint = link.getJoint();
            space.remove(joint);
        }

        space.removeCollisionListener(this);
        space.removeTickListener(this);
    }

    /**
     * Remove spatial-dependent data. Invoked each time this control is removed
     * from a spatial.
     *
     * @param spat the previously controlled spatial (unused)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics();
        }
        MySkeleton.setUserControl(skeleton, false);
        boneLinkList = null;
        attachmentLinks.clear();
        boneLinks.clear();
        skeleton = null;
        transformer = null;
        torsoLink = null;
    }

    /**
     * Alter this control's gravitational acceleration for Ragdoll mode.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected, default=0,-9.8,0)
     */
    @Override
    public void setGravity(Vector3f gravity) {
        Validate.nonNull(gravity, "gravity");

        super.setGravity(gravity);

        if (getSpatial() != null) { // TODO make sure it's in ragdoll mode
            torsoLink.getRigidBody().setGravity(gravity);
            for (BoneLink boneLink : boneLinkList) {
                boneLink.getRigidBody().setGravity(gravity);
            }
            for (AttachmentLink link : attachmentLinks.values()) {
                link.getRigidBody().setGravity(gravity);
            }
        }
    }

    /**
     * Alter the range of motion of the joint connecting the named BoneLink to
     * its parent in the link hierarchy.
     *
     * @param boneName the name of the BoneLink (not null, not empty)
     * @param preset the desired range of motion (not null)
     */
    @Override
    public void setJointLimits(String boneName, JointPreset preset) {
        if (!isBoneLinkName(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNull(preset, "preset");

        super.setJointLimits(boneName, preset);

        if (getSpatial() != null) {
            BoneLink boneLink = getBoneLink(boneName);
            SixDofJoint joint = boneLink.getJoint();
            preset.setupJoint(joint, false, false, false);
        }
    }

    /**
     * Alter the mass of the named bone/torso link.
     *
     * @param linkName the name of the link (not null)
     * @param mass the desired mass (&gt;0)
     */
    @Override
    public void setMass(String linkName, float mass) {
        Validate.positive(mass, "mass");

        super.setMass(linkName, mass);

        PhysicsRigidBody rigidBody = getRigidBody(linkName);
        if (rigidBody != null) {
            rigidBody.setMass(mass);
        }
    }

    /**
     * Translate the torso to the specified location.
     *
     * @param vec desired location (not null, unaffected)
     */
    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        torsoLink.getRigidBody().setPhysicsLocation(vec);
    }

    /**
     * Rotate the torso to the specified orientation.
     *
     * @param quat desired orientation (not null, unaffected)
     */
    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        torsoLink.getRigidBody().setPhysicsRotation(quat);
    }

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is added to a scene. Do not invoke directly
     * from user code.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        assert getSpatial() != null;
        if (!isEnabled()) {
            return;
        }

        torsoLink.update(tpf);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.update(tpf);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.update(tpf);
        }
    }

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        oc.write(damping, "limbDampening", 0.6f);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);
        // TODO boneLinkList, listeners
        oc.write(boneLinks.values().toArray(new BoneLink[boneLinks.size()]),
                "boneLinks", new BoneLink[0]);
        oc.write(skeleton, "skeleton", null);
        oc.write(transformer, "transformer", null);
        oc.write(torsoLink, "torsoLink", null);
    }
    // *************************************************************************
    // PhysicsCollisionListener methods

    /**
     * For internal use only: callback for collision events.
     *
     * @param event (not null)
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {
        if (event.getNodeA() == null && event.getNodeB() == null) {
            return;
        }
        /*
         * Determine which bone was involved (if any) and also the
         * other collision object involved.
         */
        boolean thisControlInvolved = false;
        Bone bone = null;
        PhysicsCollisionObject otherPco = null;
        PhysicsCollisionObject pcoA = event.getObjectA();
        PhysicsCollisionObject pcoB = event.getObjectB();

        Object userA = pcoA.getUserObject();
        Object userB = pcoB.getUserObject();
        if (userA instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) userA;
            thisControlInvolved = true;
            bone = boneLink.getBone();
            otherPco = pcoB;
        } else if (userA == this) { // TODO check for torsoLink
            thisControlInvolved = true;
            bone = null;
            otherPco = pcoB;
        } else if (userB instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) userB;
            thisControlInvolved = true;
            bone = boneLink.getBone();
            otherPco = pcoA;
        } else if (userB == this) {
            thisControlInvolved = true;
            bone = null;
            otherPco = pcoA;
        }
        /*
         * Discard low-impulse collisions.
         */
        if (event.getAppliedImpulse() < eventDispatchImpulseThreshold) {
            return;
        }
        /*
         * Dispatch an event if this control was involved in the collision.
         * TODO flag self collisions
         */
        if (thisControlInvolved) {
            for (RagdollCollisionListener listener : listeners) {
                listener.collide(bone, otherPco, event);
            }
        }
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just after the physics has been stepped.
     * Used to re-activate deactivated rigid bodies.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        PhysicsRigidBody prb = torsoLink.getRigidBody();
        prb.activate();

        for (BoneLink boneLink : boneLinkList) {
            prb = boneLink.getRigidBody();
            prb.activate();
        }
    }

    /**
     * Callback from Bullet, invoked just before the physics is stepped. A good
     * time to clear/apply forces.
     *
     * @param space the space that is about to be stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        torsoLink.prePhysicsTick();
        for (BoneLink boneLink : boneLinkList) {
            boneLink.prePhysicsTick();
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            link.prePhysicsTick();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add joints to connect the named bone/torso link with each of its
     * children. Also fill in the boneLinkList. Note: recursive!
     *
     * @param parentName the name of the parent link (not null)
     */
    private void addJoints(String parentName) {
        PhysicsRigidBody parentBody = getRigidBody(parentName);

        Transform parentToWorld;
        if (torsoName.equals(parentName)) {
            parentToWorld = torsoLink.physicsTransform(null);
        } else {
            BoneLink parentLink = getBoneLink(parentName);
            parentToWorld = parentLink.physicsTransform(null);
        }
        parentToWorld.setScale(1f);
        Transform worldToParent = parentToWorld.invert();

        List<String> childNames = childNames(parentName);
        for (String childName : childNames) {
            Bone childBone = getBone(childName);
            BoneLink childLink = getBoneLink(childName);
            PhysicsRigidBody childBody = childLink.getRigidBody();

            // TODO move some of this to BoneLink.setJoint()
            Transform childToWorld = childLink.physicsTransform(null);
            childToWorld.setScale(1f);

            Transform childToParent = childToWorld.clone();
            childToParent.combineWithParent(worldToParent);

            Vector3f pivotMesh = childBone.getModelSpacePosition();
            Vector3f pivotWorld = transformer.localToWorld(pivotMesh, null);
            Vector3f pivotChild
                    = childToWorld.transformInverseVector(pivotWorld, null);
            Vector3f pivotParent
                    = parentToWorld.transformInverseVector(pivotWorld, null);

            Matrix3f rotChild = matrixIdentity;
            Matrix3f rotParent = childToParent.getRotation().toRotationMatrix();

            // TODO try Point2PointJoint or HingeJoint
            SixDofJoint joint = new SixDofJoint(parentBody, childBody,
                    pivotParent, pivotChild, rotParent, rotChild, true);
            assert childLink.getJoint() == null;
            childLink.setJoint(joint);
            /*
             * Add the BoneLink to the pre-order list. TODO attachment links?
             */
            boneLinkList.add(childLink);

            addJoints(childName);
        }
    }

    /**
     * Begin blending all descendants of the named link to the specified
     * kinematic submode. Note: recursive!
     *
     * @param linkName the name of the link (not null)
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    private void blendDescendants(String linkName, KinematicSubmode submode,
            float blendInterval) {
        assert linkName != null;
        assert submode != null;
        assert blendInterval >= 0f : blendInterval;

        List<String> childNames = childNames(linkName);
        for (String childName : childNames) {
            BoneLink boneLink = getBoneLink(childName);
            boneLink.blendToKinematicMode(submode, blendInterval);
            blendDescendants(childName, submode, blendInterval);
        }
        for (AttachmentLink link : attachmentLinks.values()) {
            if (link.managerName().equals(linkName)) {
                link.blendToKinematicMode(blendInterval, null);
            }
        }
    }

    /**
     * Create a jointed AttachmentLink for the named bone, and add it to the
     * attachLinks map.
     *
     * @param boneName the name of the attachment bone to be linked (not null)
     * @param skeletonControl (not null)
     * @param managerMap a map from bone indices to managing link names (not
     * null, unaffected)
     * @return a attachment link with a joint, added to the boneLinks map
     */
    private void createAttachmentLink(String boneName,
            SkeletonControl skeletonControl, String[] managerMap) {
        assert boneName != null;
        assert skeletonControl != null;
        assert managerMap != null;

        Spatial attachModel = getAttachmentModel(boneName);
        attachModel = (Spatial) Misc.deepCopy(attachModel);

        Node node = skeletonControl.getAttachmentsNode(boneName);
        node.attachChild(attachModel);

        Bone bone = skeleton.getBone(boneName);
        int boneIndex = skeleton.getBoneIndex(bone);
        String managerName = managerMap[boneIndex];

        CollisionShape shape
                = CollisionShapeFactory.createDynamicMeshShape(attachModel);
        float mass = attachmentMass(boneName);
        PhysicsRigidBody rigidBody = createRigidBody(shape, mass);

        AttachmentLink link = new AttachmentLink(this, boneName, managerName,
                attachModel, rigidBody);
        attachmentLinks.put(boneName, link);
    }

    /**
     * Create a jointless BoneLink for the named bone, and add it to the
     * boneLinks map.
     *
     * @param boneName the name of the bone to be linked (not null)
     * @param vertexLocations the collection of vertices (not null, not empty)
     */
    private void createBoneLink(String boneName,
            List<Vector3f> vertexLocations) {
        if (vertexLocations == null || vertexLocations.isEmpty()) {
            String msg = String.format("No mesh vertices for linked bone %s.",
                    MyString.quote(boneName));
            throw new IllegalArgumentException(msg);
        }
        /*
         * Create the collision shape.
         */
        Bone bone = getBone(boneName);
        Transform boneToMesh = MySkeleton.copyMeshTransform(bone, null);
        Transform meshToBone = boneToMesh.invert();
        meshToBone.setScale(1f);
        Vector3f center = RagUtils.center(vertexLocations, null);
        center.subtractLocal(bone.getModelSpacePosition());
        CollisionShape shape
                = RagUtils.createShape(meshToBone, center, vertexLocations);

        float mass = mass(boneName);
        PhysicsRigidBody rigidBody = createRigidBody(shape, mass);

        meshToBone.getTranslation().zero();
        Vector3f offset = meshToBone.transformVector(center, null);

        BoneLink link = new BoneLink(this, bone, rigidBody, offset);
        boneLinks.put(boneName, link);
    }

    /**
     * Create and configure a rigid body for a link.
     *
     * @param shape the desired shape (not null, alias created)
     * @param mass the desired mass (&gt;0)
     * @return a new instance, not in any physics space
     */
    private PhysicsRigidBody createRigidBody(CollisionShape shape, float mass) {
        assert mass > 0f : mass;

        PhysicsRigidBody rigidBody = new PhysicsRigidBody(shape, mass);

        float viscousDamping = damping();
        rigidBody.setDamping(viscousDamping, viscousDamping);

        return rigidBody;
    }

    /**
     * Create the TorsoLink.
     *
     * @param vertexLocations the collection of vertices (not null, not empty)
     * @param meshes array of animated meshes to use (not null, unaffected)
     */
    private void createTorsoLink(Collection<Vector3f> vertexLocations,
            Mesh[] meshes) {
        if (vertexLocations == null) {
            throw new IllegalArgumentException(
                    "No mesh vertices for the torso."
                    + " Make sure the root bone is not linked.");
        }
        /*
         * Create the collision shape.
         */
        Bone bone = findTorsoMainBone(meshes);
        assert bone.getParent() == null;
        Transform boneToMesh = MySkeleton.copyMeshTransform(bone, null);
        Transform meshToBone = boneToMesh.invert();
        Vector3f center = RagUtils.center(vertexLocations, null);
        center.subtractLocal(bone.getModelSpacePosition());
        CollisionShape shape
                = RagUtils.createShape(meshToBone, center, vertexLocations);

        meshToBone.getTranslation().zero();
        Vector3f offset = meshToBone.transformVector(center, null);

        float mass = mass(torsoName);
        PhysicsRigidBody rigidBody = createRigidBody(shape, mass);

        // TODO utility method spatialToAncestor()
        Spatial loopSpatial = transformer;
        Transform modelToMesh = new Transform();
        while (loopSpatial != getSpatial()) {
            Transform localTransform = loopSpatial.getLocalTransform();
            modelToMesh.combineWithParent(localTransform);
            loopSpatial = loopSpatial.getParent();
        }
        Transform meshToModel = modelToMesh.invert();

        torsoLink = new TorsoLink(this, bone, rigidBody, meshToModel, offset);
        torsoLink.setSkeleton(skeleton);
    }

    /**
     * Find the torso's main root bone.
     *
     * @param targets an array of animated meshes to provide bone weights (not
     * null)
     * @return a root bone, or null if none found
     */
    private Bone findTorsoMainBone(Mesh[] targets) {
        assert targets != null;

        Bone[] rootBones = skeleton.getRoots();

        Bone result;
        if (rootBones.length == 1) {
            result = rootBones[0];
        } else {
            result = null;
            float[] totalWeights = RagUtils.totalWeights(targets, skeleton);
            float greatestTotalWeight = Float.NEGATIVE_INFINITY;
            for (Bone rootBone : rootBones) {
                int boneIndex = skeleton.getBoneIndex(rootBone);
                float weight = totalWeights[boneIndex];
                if (weight > greatestTotalWeight) {
                    result = rootBone;
                    greatestTotalWeight = weight;
                }
            }
        }

        return result;
    }

    /**
     * Sort the controls of the controlled spatial, such that this control will
     * come BEFORE the specified SkeletonControl.
     *
     * @param skeletonControl (not null)
     */
    private void sortControls(SkeletonControl skeletonControl) {
        assert skeletonControl != null;

        Spatial spatial = getSpatial();
        int dacIndex = RagUtils.findIndex(spatial, this);
        assert dacIndex != -1;
        int scIndex = RagUtils.findIndex(spatial, skeletonControl);
        assert scIndex != -1;
        assert dacIndex != scIndex;

        if (dacIndex > scIndex) {
            /*
             * Remove the SkeletonControl and re-add it to make sure it will get
             * updated *after* this control. TODO also arrange with AnimControl
             */
            spatial.removeControl(skeletonControl);
            spatial.addControl(skeletonControl);

            dacIndex = RagUtils.findIndex(spatial, this);
            assert dacIndex != -1;
            scIndex = RagUtils.findIndex(spatial, skeletonControl);
            assert scIndex != -1;
            assert dacIndex < scIndex;
        }
    }
}

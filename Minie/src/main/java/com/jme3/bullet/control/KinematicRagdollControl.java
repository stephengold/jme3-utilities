/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.bullet.control;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.ragdoll.ConfigRagdollControl;
import com.jme3.bullet.control.ragdoll.JointPreset;
import com.jme3.bullet.control.ragdoll.KinematicSubmode;
import com.jme3.bullet.control.ragdoll.RagUtils;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
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
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Before adding this control to a spatial, configure it by invoking
 * {@link #link(java.lang.String, float, com.jme3.bullet.control.ragdoll.JointPreset)}
 * for each bone that should have its own rigid body. Leave some unlinked bones
 * near the root of the skeleton to form the torso.
 * <p>
 * When you add the control to a spatial, it generates a rigid body with a hull
 * collision shape for the torso and for each linked bone. It also creates a
 * SixDofJoint connecting each linked bone to its parent in the linked-bone
 * hierarchy. The mass of each rigid body and the range-of-motion of each joint
 * can be reconfigured on the fly.
 * <p>
 * This control has 3 modes: <ul> <li><strong>The kinematic modes :</strong><br>
 * this is the default behavior, this means that the collision shapes of the
 * body are able to interact with physics enabled objects. in this mode physics
 * shapes follow the motion of the animated skeleton (for example animated by a
 * key framed animation) this mode is enabled by calling setKinematicMode();
 * </li> <li><strong>The ragdoll modes:</strong><br> To enable this behavior,
 * you need to invoke the setRagdollMode() method. In this mode the character is
 * entirely controlled by physics, so it will fall under the gravity and move if
 * any force is applied to it.</li>
 * </ul>
 *
 * TODO handle applyLocal, handle attachments, catch ignoreTransforms, rename
 * PhysicsAnimControl or RagdollControl or DynamicControl, ghost mode
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class KinematicRagdollControl
        extends ConfigRagdollControl
        implements PhysicsCollisionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger3
            = Logger.getLogger(KinematicRagdollControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * bone links in a pre-order, depth-first traversal of the linked-bone
     * hierarchy
     */
    private List<BoneLink> boneLinkList = null;
    /**
     * list of registered collision listeners
     */
    private List<RagdollCollisionListener> listeners
            = new SafeArrayList<>(RagdollCollisionListener.class);
    /**
     * map bone names to links
     */
    private Map<String, BoneLink> boneLinks = new HashMap<>(32);
    /**
     * skeleton being controlled
     */
    private Skeleton skeleton = null;
    /**
     * mesh scale at the time this control was added to a spatial (in world
     * coordinates)
     */
    private Vector3f initScale = null;
    /**
     * minimum applied impulse for a collision event to be dispatched to
     * listeners (default=0)
     */
    private float eventDispatchImpulseThreshold = 0f;
    /**
     * viscous damping ratio for new rigid bodies (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    private float damping = 0.6f;
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
    public KinematicRagdollControl() {
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

        if (linkName.equals(torsoFakeBoneName)) {
            torsoLink.blendToKinematicMode(KinematicSubmode.Bound,
                    blendInterval, null);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.blendToKinematicMode(KinematicSubmode.Bound,
                    blendInterval);
        }
    }

    /**
     * Begin blending all links to fully kinematic mode, driven by animation. In
     * that mode, collision objects follow the movements of the skeleton while
     * interacting with the physics environment. TODO callback at end of
     * transition
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
    }

    /**
     * Enumerate all immediate children (in the linked-bone hierarchy) of the
     * named link.
     *
     * @param linkName the name of the link (not null)
     * @return a new list of linked bone names
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
     * Count the immediate children (in the linked-bone hierarchy) of the named
     * link.
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
     * Immediately freeze the named link and all its descendants.
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

        blendDescendants(linkName, KinematicSubmode.Frozen, 0f);

        if (linkName.equals(torsoFakeBoneName)) {
            torsoLink.blendToKinematicMode(KinematicSubmode.Frozen, 0f, null);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.blendToKinematicMode(KinematicSubmode.Frozen, 0f);
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
     * Access the physics link for the named bone. This returns null if bone is
     * not linked, or if the control is not added to a spatial.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @return the pre-existing spatial or null
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
     * Access the rigid body of the named link. This returns null if the control
     * is not added to a spatial.
     *
     * @param linkName the name of the link (not null)
     * @return the pre-existing instance, or null
     */
    public PhysicsRigidBody getRigidBody(String linkName) {
        PhysicsRigidBody result;

        if (getSpatial() == null) {
            result = null;

        } else if (torsoFakeBoneName.equals(linkName)) {
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
     * Access the physics link for the torso. This returns null if the control
     * is not added to a spatial.
     *
     * @return the pre-existing spatial, or null
     */
    public TorsoLink getTorsoLink() {
        return torsoLink;
    }

    /**
     *
     * Access the spatial that provides the mesh-coordinate transform. This
     * returns null if the control is not added to a spatial.
     *
     * @return the pre-existing spatial, or null
     */
    Spatial getTransformer() {
        return transformer;
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

        if (torsoFakeBoneName.equals(managerName)) {
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
     * Calculate the local bone transform to match the physics transform of the
     * specified rigid body.
     *
     * @param rigidBody the rigid body to match (not null, unaffected)
     * @param bone
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated bone transform (in local coordinates, either
     * storeResult or a new transform, not null)
     */
    Transform localBoneTransform(PhysicsRigidBody rigidBody, Bone bone,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();

        rigidBody.getPhysicsTransform(result);
        /*
         * Transform to mesh coordinate system.
         */
        Transform worldToMesh = transformer.getWorldTransform().invert();
        result.combineWithParent(worldToMesh);
        /*
         * Transform to local coordinates by factoring out the
         * parent bone's transform. TODO utility
         */
        Bone parentBone = bone.getParent();
        Vector3f pmTranslate = parentBone.getModelSpacePosition();
        Quaternion pmRotInv = parentBone.getModelSpaceRotation().inverse();
        Vector3f pmScale = parentBone.getModelSpaceScale();
        location.subtractLocal(pmTranslate);
        location.divideLocal(pmScale);
        pmRotInv.mult(location, location);
        scale.divideLocal(pmScale);
        pmRotInv.mult(orientation, orientation);

        return result;
    }

    /**
     * Find the parent (in the linked-bone hierarchy) of the named linked bone.
     *
     * @param childName the name of the linked bone (not null, not empty)
     * @return the bone name or torsoFakeBoneName (not null)
     */
    public String parentName(String childName) {
        if (!isBoneLinkName(childName)) {
            String msg = "No linked bone named " + MyString.quote(childName);
            throw new IllegalArgumentException(msg);
        }

        String result = torsoFakeBoneName;

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
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated physics transform (either storeResult or a new
     * transform, not null)
     */
    Transform physicsTransform(Bone bone, Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        MySkeleton.copyMeshTransform(bone, result);
        /*
         * Transform to world coordinates.
         */
        Transform meshToWorld = transformer.getWorldTransform();
        result.combineWithParent(meshToWorld);

        return result;
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale to the model
     * after it was initialized. Same as re-attaching. TODO rename?
     */
    public void reBuild() {
        Spatial controlledSpatial = getSpatial();
        if (controlledSpatial != null) {
            removeSpatialData(controlledSpatial);
            createSpatialData(controlledSpatial);
        }
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
     * @param speed the desired threshold speed (&gt;0) or zero to disable CCD
     * (default=0)
     */
    public void setCcdMotionThreshold(float speed) {
        Validate.nonNegative(speed, "speed");

        torsoLink.getRigidBody().setCcdMotionThreshold(speed);
        for (BoneLink boneLink : boneLinkList) {
            boneLink.getRigidBody().setCcdMotionThreshold(speed);
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
        }
    }

    /**
     * Immediately put the named link into dynamic mode.
     *
     * @param linkName the name of the link (not null, not empty)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamic(String linkName, Vector3f uniformAcceleration) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNull(uniformAcceleration, "uniform acceleration");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        if (linkName.equals(torsoFakeBoneName)) {
            torsoLink.setDynamic(uniformAcceleration);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.setDynamic(uniformAcceleration);
        }
    }

    /**
     * Immediately put the named link and all its descendants into dynamic mode.
     * Note: recursive!
     *
     * @param linkName the name of the link at the root of the subtree (not
     * null)
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamicHierarchy(String linkName,
            Vector3f uniformAcceleration) {
        if (!isLinkName(linkName)) {
            String msg = "No link named " + MyString.quote(linkName);
            throw new IllegalArgumentException(msg);
        }
        Validate.nonNull(uniformAcceleration, "uniform acceleration");
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        if (linkName.equals(torsoFakeBoneName)) {
            torsoLink.setDynamic(uniformAcceleration);
        } else {
            BoneLink boneLink = getBoneLink(linkName);
            boneLink.setDynamic(uniformAcceleration);
        }

        List<String> childNames = childNames(linkName);
        for (String childName : childNames) {
            setDynamicHierarchy(childName, uniformAcceleration);
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
     * Immediately put all links into fully kinematic mode. In this mode,
     * collision objects follow the movements of the skeleton while interacting
     * with the physics environment.
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
     * Immediately put all links into fully dynamic ragdoll mode. The skeleton
     * is entirely controlled by physics, including gravity.
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
            boneLink.setDynamic(ragdollGravity);
        }
    }
    // *************************************************************************
    // ConfigRagdollControl methods

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

        space.addCollisionListener(this);
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
        boneLinks = cloner.clone(boneLinks);
        skeleton = cloner.clone(skeleton);
        initScale = cloner.clone(initScale);
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
                    "The controlled spatial must have a SkeletonControl. Make sure the control is there and not on a subnode.");
        }
        /*
         * Remove the SkeletonControl and re-add it to make sure it will get
         * updated *after* this control. TODO also arrange with AnimControl
         */
        spatial.removeControl(skeletonControl);
        spatial.addControl(skeletonControl);

        skeleton = skeletonControl.getSkeleton();
        validate(skeleton);
        /*
         * Find the target meshes and the main bone.  Don't invoke
         * skeletonControl.getTargets() here since the SkeletonControl
         * might not be initialized yet.
         */
        List<Mesh> targetList = MySpatial.listAnimatedMeshes(spatial, null);
        Mesh[] targets = new Mesh[targetList.size()];
        targetList.toArray(targets);
        Bone torsoMainBone = findTorsoMainBone(targets);
        assert torsoMainBone.getParent() == null;
        /*
         * Analyze the model's coordinate systems.
         */
        transformer = MySpatial.findAnimatedGeometry(spatial);
        if (transformer == null) {
            transformer = spatial;
        }
        Spatial loopSpatial = transformer;
        Transform modelToMesh = new Transform();
        while (loopSpatial != spatial) {
            Transform localTransform = loopSpatial.getLocalTransform();
            modelToMesh.combineWithParent(localTransform);
            loopSpatial = loopSpatial.getParent();
        }
        Transform meshToModel = modelToMesh.invert();
        initScale = transformer.getWorldScale().clone();
        /*
         * Analyze the model's meshes.
         */
        String[] tempManagerMap = managerMap(skeleton);
        Map<String, List<Vector3f>> coordsMap
                = coordsMap(targets, tempManagerMap);
        /*
         * Create a shape for the torso.
         */
        List<Vector3f> vertexLocations = coordsMap.get(torsoFakeBoneName);
        if (vertexLocations == null) {
            throw new IllegalArgumentException(
                    "No mesh vertices for the torso. Make sure the root bone is not linked.");
        }
        skeleton.resetAndUpdate();
        Transform boneToMesh
                = MySkeleton.copyMeshTransform(torsoMainBone, null);
        Transform invTransform = boneToMesh.invert();
        CollisionShape torsoShape = createShape(invTransform, vertexLocations);
        /*
         * Create a rigid body for the torso.
         */
        float torsoMass = mass(torsoFakeBoneName);
        PhysicsRigidBody torsoRigidBody
                = new PhysicsRigidBody(torsoShape, torsoMass);
        float viscousDamping = damping();
        torsoRigidBody.setDamping(viscousDamping, viscousDamping);
        /*
         * Create torso link.
         */
        torsoLink = new TorsoLink(this, torsoMainBone, meshToModel,
                torsoRigidBody);
        torsoLink.setSkeleton(skeleton);
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
         * Add joints to connect each linked bone with its parent.
         * Also initialize the boneLinkList.
         */
        boneLinkList = new ArrayList<>(numLinkedBones);
        addJoints(torsoFakeBoneName);
        assert boneLinkList.size() == numLinkedBones;

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
    public KinematicRagdollControl jmeClone() {
        try {
            KinematicRagdollControl clone
                    = (KinematicRagdollControl) super.clone();
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

        // TODO boneLinkList, etc.
        BoneLink[] loadedBoneLinks
                = (BoneLink[]) ic.readSavableArray("boneList",
                        new BoneLink[0]);
        for (BoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.getBone().getName(), physicsBoneLink);
        }

        skeleton = (Skeleton) ic.readSavable("skeleton", null);
        transformer
                = (Spatial) ic.readSavable("transformer", null);
        initScale = (Vector3f) ic.readSavable("initScale", null);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);
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

        space.removeCollisionListener(this);
    }

    /**
     * Destroy all spatial-dependent data. Invoked each time this control is
     * removed from a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics();
        }
        boneLinkList = null;
        boneLinks.clear();
        torsoLink.setSkeleton(null);
        torsoLink = null;
        skeleton = null;
        initScale = null;
        transformer = null;
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
        }
    }

    /**
     * Alter the limits of the joint connecting the named linked bone to its
     * parent in the linked-bone hierarchy.
     *
     * @param boneName the name of the bone (not null, not empty)
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
            preset.setupJoint(joint);
        }
    }

    /**
     * Alter the mass of the named link.
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
        /*
         * Update bone links in pre-order, depth-first.
         */
        for (BoneLink boneLink : boneLinkList) {
            boneLink.update(tpf);
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

        oc.write(skeleton.getRoots(), "torsoManagedBones", null);
        // TODO boneLinkList, etc.
        oc.write(boneLinks.values().toArray(
                new BoneLink[boneLinks.size()]),
                "boneLinks", new BoneLink[0]);
        oc.write(skeleton, "skeleton", null);
        oc.write(transformer, "transformer", null);
        oc.write(initScale, "initScale", null);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);
        oc.write(damping, "limbDampening", 0.6f);
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
        boolean krcInvolved = false;
        Bone bone = null;
        PhysicsCollisionObject otherPco = null;
        PhysicsCollisionObject pcoA = event.getObjectA();
        PhysicsCollisionObject pcoB = event.getObjectB();

        Object userA = pcoA.getUserObject();
        Object userB = pcoB.getUserObject();
        if (userA instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) userA;
            krcInvolved = true;
            bone = boneLink.getBone();
            otherPco = pcoB;
        } else if (userA == this) {
            krcInvolved = true;
            bone = null;
            otherPco = pcoB;
        } else if (userB instanceof BoneLink) {
            BoneLink boneLink = (BoneLink) userB;
            krcInvolved = true;
            bone = boneLink.getBone();
            otherPco = pcoA;
        } else if (userB == this) {
            krcInvolved = true;
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
        if (krcInvolved) {
            for (RagdollCollisionListener listener : listeners) {
                listener.collide(bone, otherPco, event);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add joints to connect the named link with each of its children. Also fill
     * in the boneLinkList. Note: recursive!
     *
     * @param parentName the name of the parent link (not null)
     */
    private void addJoints(String parentName) {
        PhysicsRigidBody parentBody = getRigidBody(parentName);

        Bone parentBone;
        if (torsoFakeBoneName.equals(parentName)) {
            parentBone = torsoLink.getBone();
        } else {
            parentBone = getBone(parentName);
        }
        Transform parentToMesh = MySkeleton.copyMeshTransform(parentBone, null);
        Transform meshToParent = parentToMesh.invert();

        List<String> childNames = childNames(parentName);
        for (String childName : childNames) {
            Bone childBone = getBone(childName);
            BoneLink boneLink = getBoneLink(childName);
            PhysicsRigidBody childBody = boneLink.getRigidBody();

            Transform childToMesh
                    = MySkeleton.copyMeshTransform(childBone, null);
            Transform childToParent = childToMesh.clone();
            childToParent.combineWithParent(meshToParent);

            Vector3f pivotChild = new Vector3f(0f, 0f, 0f);
            Vector3f pivotParent = childToParent.getTranslation();
            pivotParent = pivotParent.mult(initScale);

            Matrix3f rotChild = new Matrix3f(); // identity
            Matrix3f rotParent = childToParent.getRotation().toRotationMatrix();

            SixDofJoint joint = new SixDofJoint(parentBody, childBody,
                    pivotParent, pivotChild, rotParent, rotChild, true);
            assert boneLink.getJoint() == null;
            boneLink.setJoint(joint);
            boneLinkList.add(boneLink);

            JointPreset preset = getJointLimits(childName);
            preset.setupJoint(joint);
            joint.setCollisionBetweenLinkedBodies(false);

            addJoints(childName);
        }
    }

    /**
     * Begin blending all descendants of the named link to a kinematic submode.
     * Note: recursive!
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
    }

    /**
     * Create a BoneLink for the named bone.
     *
     * @param name the name of the bone to be linked (not null)
     * @param lbNames map from bone indices to linked-bone names (not null,
     * unaffected)
     * @param coordsMap map from bone names to vertex positions (not null,
     * unaffected)
     * @return a new bone link without a joint, added to the boneLinks map
     */
    private BoneLink createBoneLink(String name,
            List<Vector3f> vertexLocations) {
        Bone bone = getBone(name);
        /*
         * Create the collision shape.
         */
        Transform boneToMesh
                = MySkeleton.copyMeshTransform(bone, null);
        Transform invTransform = boneToMesh.invert();
        CollisionShape boneShape = createShape(invTransform, vertexLocations);
        /*
         * Create the rigid body.
         */
        float boneMass = mass(name);
        assert boneMass > 0f : boneMass;
        PhysicsRigidBody prb = new PhysicsRigidBody(boneShape, boneMass);

        float viscousDamping = damping();
        prb.setDamping(viscousDamping, viscousDamping);

        BoneLink boneLink = new BoneLink(this, bone, prb);
        boneLinks.put(name, boneLink);

        return boneLink;
    }

    /**
     * Find the torso's main bone.
     *
     * @param targets array of animated meshes to provide bone weights (not
     * null, unaffected)
     * @return a root bone, or null if none found
     */
    private Bone findTorsoMainBone(Mesh[] targets) {
        float[] totalWeights = RagUtils.totalWeights(targets, skeleton);
        Bone[] rootBones = skeleton.getRoots();

        Bone result = null;
        float greatestTotalWeight = Float.NEGATIVE_INFINITY;
        for (Bone rootBone : rootBones) {
            int boneIndex = skeleton.getBoneIndex(rootBone);
            float weight = totalWeights[boneIndex];
            if (weight > greatestTotalWeight) {
                result = rootBone;
                greatestTotalWeight = weight;
            }
        }

        return result;
    }
}

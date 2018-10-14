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
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.control.ragdoll.JointPreset;
import com.jme3.bullet.control.ragdoll.KinematicSubmode;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
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
 * TODO handle applyLocal, handle attachments, catch ignoreTransforms
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class KinematicRagdollControl
        extends AbstractPhysicsControl
        implements PhysicsCollisionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(KinematicRagdollControl.class.getName());
    /**
     * magic bone name to refer to the model's torso
     */
    final public static String torsoFakeBoneName = "";
    // *************************************************************************
    // fields

    /**
     * duration of the torso's most recent transition to kinematic mode (in
     * seconds, &ge;0)
     */
    private float torsoBlendInterval = 1f;
    /**
     * weighting of kinematic movement for the torso (&ge;0, &le;1, 0=purely
     * dynamic, 1=purely kinematic, default=1)
     */
    private float torsoKinematicWeight = 1f;
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
     * map linked bone names to masses for createSpatialData()
     */
    private Map<String, Float> massMap = new HashMap<>(32);
    /**
     * map linked bone names to joint presets for createSpatialData()
     */
    private Map<String, JointPreset> jointMap = new HashMap<>(32);
    /**
     * map bone names to simulation objects
     */
    private Map<String, BoneLink> boneLinks = new HashMap<>(32);
    /**
     * rigid body for the torso
     */
    private PhysicsRigidBody torsoRigidBody = null;
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
     * mass of the torso (default=15)
     */
    private float torsoMass = 15f;
    /**
     * viscous damping ratio for rigid bodies (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    private float damping = 0.6f;
    /**
     * orientation of each root bone in bind pose (in mesh coordinates)
     */
    private Quaternion rootBindOrientation[] = null;
    /**
     * spatial that provides the mesh-coordinate transform
     */
    private Spatial transformer = null;
    /**
     * local transform for the controlled spatial at the end of the torso's most
     * recent transition to kinematic mode
     */
    private Transform endModelTransform = new Transform();
    /**
     * transform from mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;
    /**
     * local transform of the controlled spatial at the start of the torso's
     * most recent transition to kinematic mode
     */
    private Transform startModelTransform = new Transform();
    /**
     * local transform for each root bone at the start of the torso's most
     * recent transition to kinematic mode
     */
    private Transform startRootTransform[] = null;
    /**
     * gravitational acceleration for ragdoll (default is 9.8 in the -Y
     * direction, corresponding to Earth-normal in MKS units)
     */
    private Vector3f gravityVector = new Vector3f(0f, -9.8f, 0f);
    /**
     * scale of each root bone in bind pose (in mesh coordinates)
     */
    private Vector3f rootBindScale[] = null;
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
     * Begin transitioning the torso and all linked bones to fully kinematic
     * mode, driven by animation. In that mode, collision objects follow the
     * movements of the skeleton while interacting with the physics environment.
     * TODO callback at end of transition
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param blendInterval the duration of the blend interval for linked bones
     * (in seconds, &ge;0)
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

        torsoBlendInterval = blendInterval;
        torsoKinematicWeight = Float.MIN_VALUE; // non-zero to start blending
        torsoRigidBody.setKinematic(true);

        startModelTransform = getSpatial().getLocalTransform().clone();
        if (endModelTransform == null) {
            this.endModelTransform.set(startModelTransform);
        } else {
            this.endModelTransform.set(endModelTransform);
        }

        Bone[] rootBones = skeleton.getRoots();
        int numRootBones = rootBones.length;
        for (int rootIndex = 0; rootIndex < numRootBones; rootIndex++) {
            Quaternion bindOrientation = rootBindOrientation[rootIndex];
            Vector3f bindScale = rootBindScale[rootIndex];
            startRootTransform[rootIndex] = localBoneTransform(torsoRigidBody,
                    bindOrientation, bindScale, startRootTransform[rootIndex]);
        }

        MySkeleton.setUserControl(skeleton, false);

        for (BoneLink link : boneLinks.values()) {
            link.blendToKinematicMode(KinematicSubmode.Animated, blendInterval);
        }
    }

    /**
     * Enumerate all immediate children (in the linked-bone hierarchy) of the
     * named linked bone or the torso.
     *
     * @param parentName the name of the linked bone or the torso (not null)
     * @return a new list of names
     */
    public List<String> childNames(String parentName) {
        if (!torsoFakeBoneName.equals(parentName) && !isLinked(parentName)) {
            String msg = "No linked bone named " + MyString.quote(parentName);
            throw new IllegalArgumentException(msg);
        }

        List<String> result = new ArrayList<>(8);
        for (String childName : massMap.keySet()) {
            BoneLink link = getBoneLink(childName);
            if (link.parentName().equals(parentName)) {
                result.add(childName);
            }
        }

        return result;
    }

    /**
     * Count the immediate children (in the linked-bone hierarchy) of the named
     * linked bone or the torso.
     *
     * @param parentName the name of the linked bone or the torso (not null)
     * @return count (&ge;0)
     */
    public int countChildren(String parentName) {
        if (!torsoFakeBoneName.equals(parentName) && !isLinked(parentName)) {
            String msg = "No linked bone named " + MyString.quote(parentName);
            throw new IllegalArgumentException(msg);
        }

        int result = 0;
        for (String childName : massMap.keySet()) {
            BoneLink link = getBoneLink(childName);
            if (link.parentName().equals(parentName)) {
                ++result;
            }
        }

        return result;
    }

    /**
     * Read the damping ratio.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float damping() {
        assert damping >= 0f : damping;
        return damping;
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
        BoneLink link = boneLinks.get(boneName);
        return link;
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
     * Access the limits of the joint connecting the named linked bone to its
     * parent in the hierarchy.
     *
     * @param boneName the name of the linked bone (not null, not empty)
     * @return the pre-existing instance (not null)
     */
    public JointPreset getJointLimits(String boneName) {
        if (!isLinked(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        JointPreset result = jointMap.get(boneName);

        assert result != null;
        return result;
    }

    /**
     * Access the rigid body of the named linked bone or the torso.
     *
     * @param boneName the name of the linked bone or the torso (not null)
     * @return the pre-existing instance, or null if not added to a spatial
     */
    public PhysicsRigidBody getRigidBody(String boneName) {
        PhysicsRigidBody result;

        if (torsoFakeBoneName.equals(boneName)) {
            result = torsoRigidBody;
        } else if (isLinked(boneName)) {
            BoneLink link = boneLinks.get(boneName);
            result = link.getRigidBody();
        } else {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        return result;
    }

    /**
     *
     * Access the spatial that provides the mesh-coordinate transform. This
     * returns null if the control is not added to a spatial.
     *
     * @return the pre-existing spatial or null
     */
    Spatial getTransformer() {
        return transformer;
    }

    /**
     * Copy this control's gravitational acceleration.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an acceleration vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f gravity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(gravityVector);
        return result;
    }

    /**
     * Test whether the named bone is linked by this control.
     *
     * @param boneName (not null, not empty)
     * @return true if linked, otherwise false
     */
    public boolean isLinked(String boneName) {
        Validate.nonEmpty(boneName, "bone name");
        boolean result = massMap.containsKey(boneName);
        return result;
    }

    /**
     * Link the named bone using a joint preset.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to link (not null, not empty)
     * @param mass the desired mass of the bone (&gt;0)
     * @param jointPreset the desired range of motion (not null)
     * @see #setJointLimits(java.lang.String,
     * com.jme3.bullet.control.ragdoll.JointPreset)
     */
    public void link(String boneName, float mass, JointPreset jointPreset) {
        Validate.nonEmpty(boneName, "bone name");
        Validate.positive(mass, "mass");
        Validate.nonNull(jointPreset, "joint preset");
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot link a bone while added to a spatial.");
        }

        jointMap.put(boneName, jointPreset);
        massMap.put(boneName, mass);
    }

    /**
     * Enumerate all linked bones in this control.
     *
     * @return an unmodifiable collection of names
     */
    public Collection<String> linkedBoneNames() {
        Collection<String> names = massMap.keySet();
        Collection<String> result = Collections.unmodifiableCollection(names);

        return result;
    }

    /**
     * Enumerate all managed bones of the named linked bone or the torso, in a
     * pre-order, depth-first traversal of the skeleton, such that child bones
     * never precede their ancestors.
     *
     * @param managerName the name of the manager (not null)
     * @return a new array of managed bones, including the manager if it is not
     * the torso
     */
    Bone[] listManagedBones(String managerName) {
        List<Bone> list = new ArrayList<>(8);

        if (torsoFakeBoneName.equals(managerName)) {
            Bone[] roots = skeleton.getRoots();
            for (Bone rootBone : roots) {
                list.add(rootBone);
                addUnlinkedDescendents(rootBone, list);
            }

        } else {
            BoneLink manager = getBoneLink(managerName);
            if (manager == null) {
                String msg
                        = "No linked bone named " + MyString.quote(managerName);
                throw new IllegalArgumentException(msg);
            }
            Bone managerBone = manager.getBone();
            list.add(managerBone);
            addUnlinkedDescendents(managerBone, list);
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
     * @param bindOrientation the bone's bind orientation (in model coordinates,
     * not null, unaffected)
     * @param bindScale the bone's bind scale (in model coordinates, not null,
     * unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated local bone transform (either storeResult or a new
     * transform, not null)
     */
    Transform localBoneTransform(PhysicsRigidBody rigidBody,
            Quaternion bindOrientation, Vector3f bindScale,
            Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();

        Transform world = rigidBody.getPhysicsTransform(null);
        Spatial transformSpatial = getTransformer();

        Vector3f worldLoc = world.getTranslation();
        transformSpatial.worldToLocal(worldLoc, location);

        Quaternion worldOri = world.getRotation();
        orientation.set(worldOri);
        orientation.multLocal(bindOrientation);
        Quaternion spatInvRot
                = MySpatial.inverseOrientation(transformSpatial);
        spatInvRot.mult(orientation, orientation);

        Vector3f worldScale = world.getScale();
        scale.set(worldScale);
        Vector3f meshToWorldScale = transformSpatial.getWorldScale();
        scale.divideLocal(meshToWorldScale);
        scale.divideLocal(bindScale);

        return result;
    }

    /**
     * Read the mass of the named linked bone or the torso.
     *
     * @param boneName the name of the linked bone or the torso (not null)
     * @return the mass (&gt;0)
     */
    public float mass(String boneName) {
        float mass;

        if (torsoFakeBoneName.equals(boneName)) {
            mass = torsoMass;
        } else if (isLinked(boneName)) {
            mass = massMap.get(boneName);
        } else {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        assert mass > 0f : mass;
        return mass;
    }

    /**
     * Find the parent (in the linked-bone hierarchy) of the named linked bone.
     *
     * @param childName (not null, not empty)
     * @return the bone name or torsoFakeBoneName
     */
    public String parentName(String childName) {
        if (!isLinked(childName)) {
            String msg = "No linked bone named " + MyString.quote(childName);
            throw new IllegalArgumentException(msg);
        }

        String result = torsoFakeBoneName;

        Bone child = getBone(childName);
        Bone parent = child.getParent();
        while (parent != null) {
            String name = parent.getName();
            if (isLinked(name)) {
                result = name;
                break;
            }
            parent = parent.getParent();
        }

        return result;
    }

    /**
     * Calculate the physics transform to match the local transform of the
     * specified skeleton bone.
     *
     * @param bone the skeleton bone to match (not null, unaffected)
     * @param bindOrientation the bone's bind orientation (in model coordinates,
     * not null, unaffected)
     * @param bindScale the bone's bind scale (in model coordinates, not null,
     * unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated physics transform (either storeResult or a new
     * transform, not null)
     */
    Transform physicsTransform(Bone bone, Quaternion bindOrientation,
            Vector3f bindScale, Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();

        Transform meshToWorld = transformer.getWorldTransform();
        Vector3f msp = bone.getModelSpacePosition();
        Quaternion msr = bone.getModelSpaceRotation();
        Vector3f mss = bone.getModelSpaceScale();

        // Compute the bone's location in world coordinates.
        meshToWorld.transformVector(msp, location);

        // Compute the bone's orientation in world coordinates.
        orientation.set(msr);
        orientation.multLocal(bone.getModelBindInverseRotation());
        meshToWorld.getRotation().mult(orientation, orientation);
        orientation.normalizeLocal();

        // Compute the bone's scale in world coordinates.
        scale.set(mss);
        // TODO mbis
        scale.multLocal(meshToWorld.getScale());

        return result;
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale to the model
     * after it was initialized. Same as re-attaching.
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
     * Alter the transform of a skeleton bone. Unlinked child bones are also
     * altered. Note: recursive!
     *
     * @param bone the skeleton bone to transform (not null, modified)
     * @param localTransform the desired bone transform (in local coordinates,
     * not null, unaffected)
     */
    void setBoneTransform(Bone bone, Transform localTransform) {
        boolean userControl = bone.hasUserControl();
        if (!userControl) {
            // Take control of the bone.
            bone.setUserControl(true);
        }

        Vector3f location = localTransform.getTranslation();
        Quaternion orientation = localTransform.getRotation();
        Vector3f scale = localTransform.getScale();
        /*
         * Set the user transform of the bone.
         */
        bone.setUserTransformsInModelSpace(location, orientation);
        // TODO scale?

        for (Bone childBone : bone.getChildren()) {
            String childName = childBone.getName();
            if (!isLinked(childName)) {
                Transform childLocalTransform
                        = childBone.getCombinedTransform(location, orientation);
                childLocalTransform.setScale(scale);
                setBoneTransform(childBone, childLocalTransform);
            }
        }

        if (!userControl) {
            // Give control back to the animation control.
            bone.setUserControl(false);
        }
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

        torsoRigidBody.setCcdMotionThreshold(speed);
        for (BoneLink link : boneLinks.values()) {
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

        torsoRigidBody.setCcdSweptSphereRadius(radius);
        for (BoneLink link : boneLinks.values()) {
            link.getRigidBody().setCcdSweptSphereRadius(radius);
        }
    }

    /**
     * Alter the viscous damping ratio.
     *
     * @param dampingRatio the desired damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    public void setDamping(float dampingRatio) {
        Validate.nonNegative(dampingRatio, "damping ratio");

        damping = dampingRatio;

        if (getSpatial() != null) {
            torsoRigidBody.setDamping(damping, damping);
            for (BoneLink link : boneLinks.values()) {
                link.getRigidBody().setDamping(damping, damping);
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
     * Alter this control's gravitational acceleration for Ragdoll mode.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected, default=0,-9.8,0)
     */
    public void setGravity(Vector3f gravity) {
        Validate.nonNull(gravity, "gravity");

        this.gravityVector.set(gravity);

        if (getSpatial() != null) {
            torsoRigidBody.setGravity(gravity);
            for (BoneLink link : boneLinks.values()) {
                link.getRigidBody().setGravity(gravity);
            }
        }
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
     * Alter the limits of the joint connecting the named linked bone to its
     * parent in the linked-bone hierarchy.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @param preset the desired range of motion (not null)
     */
    public void setJointLimits(String boneName, JointPreset preset) {
        Validate.nonNull(preset, "preset");
        if (!isLinked(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        jointMap.put(boneName, preset);

        if (getSpatial() != null) {
            BoneLink link = getBoneLink(boneName);
            SixDofJoint joint = link.getJoint();
            preset.setupJoint(joint);
        }
    }

    /**
     * Immediately put the torso and all linked bones into fully kinematic mode.
     * In this mode, collision objects follow the movements of the skeleton
     * while interacting with the physics environment.
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
     * Alter the mass of the named linked bone or the torso.
     *
     * @param boneName the name of the linked bone or the torso (not null)
     * @param mass the desired mass (&gt;0)
     */
    public void setMass(String boneName, float mass) {
        Validate.positive(mass, "mass");

        if (torsoFakeBoneName.equals(boneName)) {
            torsoMass = mass;
        } else if (isLinked(boneName)) {
            massMap.put(boneName, mass);
        } else {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }

        PhysicsRigidBody rigidBody = getRigidBody(boneName);
        if (rigidBody != null) {
            rigidBody.setMass(mass);
        }
    }

    /**
     * Put all bones into fully dynamic ragdoll mode. The skeleton is entirely
     * controlled by physics, including gravity.
     * <p>
     * Allowed only when the control IS added to a spatial.
     */
    public void setRagdollMode() {
        if (getSpatial() == null) {
            throw new IllegalStateException(
                    "Cannot change modes unless added to a spatial.");
        }

        torsoKinematicWeight = 0f;
        torsoRigidBody.setGravity(gravityVector);
        torsoRigidBody.setKinematic(false);

        for (BoneLink link : boneLinks.values()) {
            link.setDynamic(gravityVector);
        }
    }

    /**
     * Alter a user-mode flag of a skeleton bone. Unlinked child bones are also
     * altered. Note: recursive!
     *
     * @param bone the skeleton bone to alter (not null)
     * @param setting the desired flag setting (true&rarr;bone link control,
     * false&rarr;animation control)
     */
    void setUserMode(Bone bone, boolean setting) {
        bone.setUserControl(setting);

        for (Bone childBone : bone.getChildren()) {
            String childName = childBone.getName();
            if (!isLinked(childName)) {
                setUserMode(childBone, setting);
            }
        }
    }

    /**
     * Calculate the ragdoll's total mass.
     *
     * @return the total amount (&gt;0)
     */
    public float totalMass() {
        float totalMass = torsoMass;
        for (float mass : massMap.values()) {
            totalMass += mass;
        }

        assert totalMass > 0f : totalMass;
        return totalMass;
    }

    /**
     * Unlink the named bone.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the linked bone to unlink (not null, not
     * empty)
     */
    public void unlink(String boneName) {
        if (!isLinked(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        if (getSpatial() != null) {
            throw new IllegalStateException(
                    "Cannot unlink a bone while added to a spatial.");
        }

        jointMap.remove(boneName);
        massMap.remove(boneName);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Update this control in Kinematic mode, based on the transformer's
     * transform and the skeleton's pose.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void kinematicUpdate(float tpf) {
        assert torsoRigidBody.isInWorld();

        removePhysics();

        torsoKinematicUpdate();
        for (BoneLink link : boneLinks.values()) {
            link.update(tpf);
        }

        addPhysics();
    }

    /**
     * Update the skeleton in Ragdoll mode, based on Bullet dynamics.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void ragDollUpdate(float tpf) {
        torsoDynamicUpdate();

        for (BoneLink link : boneLinks.values()) {
            link.update(tpf);
        }
    }
    // *************************************************************************
    // AbstractPhysicsControl methods

    /**
     * Add all managed physics objects to the physics space.
     */
    @Override
    protected void addPhysics() {
        PhysicsSpace space = getPhysicsSpace();
        Vector3f gravity = gravity(null);

        space.add(torsoRigidBody);
        torsoRigidBody.setGravity(gravity);

        for (BoneLink physicsBoneLink : boneLinks.values()) {
            PhysicsRigidBody rigidBody = physicsBoneLink.getRigidBody();
            space.add(rigidBody);
            rigidBody.setGravity(gravity);

            PhysicsJoint joint = physicsBoneLink.getJoint();
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
        boneLinks = cloner.clone(boneLinks);
        endModelTransform = cloner.clone(endModelTransform);
        gravityVector = cloner.clone(gravityVector);
        jointMap = cloner.clone(jointMap);
        initScale = cloner.clone(initScale);
        listeners = cloner.clone(listeners);
        massMap = cloner.clone(massMap);
        meshToModel = cloner.clone(meshToModel);
        rootBindOrientation = cloner.clone(rootBindOrientation);
        rootBindScale = cloner.clone(rootBindScale);
        skeleton = cloner.clone(skeleton);
        startModelTransform = cloner.clone(startModelTransform);
        startRootTransform = cloner.clone(startRootTransform);
        torsoRigidBody = cloner.clone(torsoRigidBody);
        transformer = cloner.clone(transformer);
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
        skeleton = skeletonControl.getSkeleton();
        validate(skeleton);
        Bone[] rootBones = skeleton.getRoots();
        int numRootBones = rootBones.length;
        /*
         * Allocate per-root arrays.
         */
        rootBindOrientation = new Quaternion[numRootBones];
        rootBindScale = new Vector3f[numRootBones];
        startRootTransform = new Transform[numRootBones];
        /*
         * Put the skeleton into bind pose.
         */
        skeleton.resetAndUpdate();

        for (int rootIndex = 0; rootIndex < numRootBones; rootIndex++) {
            Bone bone = rootBones[rootIndex];
            Quaternion msr = bone.getModelSpaceRotation();
            rootBindOrientation[rootIndex] = msr.clone();
            Vector3f mss = bone.getModelSpaceScale();
            rootBindScale[rootIndex] = mss.clone();
        }
        /*
         * Remove the SkeletonControl and re-add it to make sure it will get
         * updated *after* this control.
         */
        spatial.removeControl(skeletonControl);
        spatial.addControl(skeletonControl);
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
        meshToModel = modelToMesh.invert();
        initScale = transformer.getWorldScale().clone();
        /*
         * Map bone indices to the names of the bones' managers.
         */
        String[] tempLbNames = linkedBoneNameArray();
        /*
         * Assign each mesh vertex to a linked bone or else to the torso.
         */
        Mesh[] targets = skeletonControl.getTargets();
        Map<String, List<Vector3f>> coordsMap = coordsMap(targets, tempLbNames);
        /*
         * Create a rigid body for the torso.
         */
        List<Vector3f> list = coordsMap.get(torsoFakeBoneName);
        if (list == null) {
            throw new IllegalArgumentException(
                    "No mesh vertices for the torso. Make sure the root bone is not linked.");
        }
        CollisionShape torsoShape
                = createShape(new Transform(), new Vector3f(), list);
        float mass = mass("");
        torsoRigidBody = new PhysicsRigidBody(torsoShape, mass);
        float viscousDamping = damping();
        torsoRigidBody.setDamping(viscousDamping, viscousDamping);
        torsoRigidBody.setKinematic(true);
        torsoRigidBody.setUserObject(this);
        /*
         * Create bone links.
         */
        assert massMap.size() == jointMap.size(); // parallel maps
        for (String boneName : massMap.keySet()) {
            List<Vector3f> vertexLocations = coordsMap.get(boneName);
            createLink(boneName, vertexLocations);
        }
        assert boneLinks.size() == massMap.size();
        /*
         * Add joints to connect each linked bone with its parent.
         */
        boneLinkList = new ArrayList<>(massMap.size());
        addJoints(torsoFakeBoneName);
        assert boneLinkList.size() == boneLinks.size();

        if (added) {
            addPhysics();
        }

        logger.log(Level.FINE, "Created ragdoll for skeleton.");
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
        // TODO jointMap, massMap, boneLinkList, etc.
        BoneLink[] loadedBoneLinks
                = (BoneLink[]) ic.readSavableArray("boneList",
                        new BoneLink[0]);
        for (BoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.getBone().getName(), physicsBoneLink);
        }
        skeleton = (Skeleton) ic.readSavable("skeleton", null);
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        transformer
                = (Spatial) ic.readSavable("transformer", null);
        initScale = (Vector3f) ic.readSavable("initScale", null);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);
        torsoMass = ic.readFloat("rootMass", 15f);
        gravityVector = (Vector3f) ic.readSavable("gravity", null);

        torsoBlendInterval = ic.readFloat("torsoBlendInterval", 1f);
        torsoKinematicWeight = ic.readFloat("torsoKinematicWeight", 1f);
    }

    /**
     * Remove all managed physics objects from the physics space.
     */
    @Override
    protected void removePhysics() {
        PhysicsSpace space = getPhysicsSpace();
        space.remove(torsoRigidBody);

        for (BoneLink physicsBoneLink : boneLinks.values()) {
            space.remove(physicsBoneLink.getJoint());
            space.remove(physicsBoneLink.getRigidBody());
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
        torsoRigidBody = null;
        skeleton = null;
        boneLinks.clear();
        initScale = null;
        rootBindOrientation = null;
        transformer = null;
        meshToModel = null;
        startRootTransform = null;
        rootBindScale = null;
    }

    /**
     * Render this control. Invoked once per view port per frame, provided the
     * control is added to a scene. Should be invoked only by a subclass or by
     * the RenderManager.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * Translate the torso to the specified location.
     *
     * @param vec desired location (not null, unaffected)
     */
    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        torsoRigidBody.setPhysicsLocation(vec);
    }

    /**
     * Rotate the torso to the specified orientation.
     *
     * @param quat desired orientation (not null, unaffected)
     */
    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        torsoRigidBody.setPhysicsRotation(quat);
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

        if (torsoKinematicWeight > 0f) {
            torsoKinematicUpdate(tpf);
        } else {
            torsoDynamicUpdate();
        }
        /*
         * Update bone links in pre-order, depth-first.
         */
        for (BoneLink link : boneLinkList) {
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
        // TODO jointMap, massMap, boneLinkList, etc.
        oc.write(boneLinks.values().toArray(
                new BoneLink[boneLinks.size()]),
                "boneLinks", new BoneLink[0]);
        oc.write(skeleton, "skeleton", null);
        oc.write(transformer, "transformer", null);
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(initScale, "initScale", null);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);
        oc.write(torsoMass, "rootMass", 15f);
        oc.write(damping, "limbDampening", 0.6f);
        oc.write(gravityVector, "gravity", new Vector3f(0f, -9.8f, 0f));

        oc.write(torsoBlendInterval, "torsoBlendInterval", 1f);
        oc.write(torsoKinematicWeight, "torsoKinematicWeight", 1f);
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
            BoneLink link = (BoneLink) userA;
            krcInvolved = true;
            bone = link.getBone();
            otherPco = pcoB;
        } else if (userA == this) {
            krcInvolved = true;
            bone = null;
            otherPco = pcoB;
        } else if (userB instanceof BoneLink) {
            BoneLink link = (BoneLink) userB;
            krcInvolved = true;
            bone = link.getBone();
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
     * Add joints to connect the named parent with each of its children. Also
     * fill in the boneLinkList. Note: recursive!
     *
     * @param parentName the name of the parent, which must either be a linked
     * bone or the torso (not null)
     */
    private void addJoints(String parentName) {
        PhysicsRigidBody parentBody = getRigidBody(parentName);

        Vector3f parentLocation;
        if (torsoFakeBoneName.equals(parentName)) {
            parentLocation = new Vector3f();
        } else {
            Bone parentBone = getBone(parentName);
            parentLocation = parentBone.getModelSpacePosition();
        }

        List<String> childNames = childNames(parentName);
        for (String childName : childNames) {
            Bone childBone = getBone(childName);
            Vector3f posToParent
                    = childBone.getModelSpacePosition().clone();
            posToParent.subtractLocal(parentLocation);
            posToParent.multLocal(initScale);

            BoneLink link = getBoneLink(childName);
            PhysicsRigidBody childBody = link.getRigidBody();
            SixDofJoint joint = new SixDofJoint(parentBody, childBody,
                    posToParent, new Vector3f(), true);
            assert link.getJoint() == null;
            link.setJoint(joint);
            boneLinkList.add(link);

            JointPreset preset = getJointLimits(childName);
            preset.setupJoint(joint);
            joint.setCollisionBetweenLinkedBodies(false);

            addJoints(childName);
        }
    }

    /**
     * Add unlinked descendents of the specified bone to the specified list.
     * Note: recursive.
     *
     * @param bone (not null, alias created)
     * @param addResult (not null, modified)
     */
    private void addUnlinkedDescendents(Bone bone, List<Bone> addResult) {
        for (Bone childBone : bone.getChildren()) {
            String childName = childBone.getName();
            if (!isLinked(childName)) {
                addResult.add(childBone);
                addUnlinkedDescendents(childBone, addResult);
            }
        }
    }

    /**
     * Assign each mesh vertex to a linked bone and add its location (mesh
     * coordinates in bind pose) to that bone's list.
     *
     * @param lbNames a map from bone indices to linked-bone names
     * @return a new map from linked-bone names to coordinates
     */
    private static Map<String, List<Vector3f>> coordsMap(Mesh[] meshes,
            String[] lbNames) {
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Map<String, List<Vector3f>> coordsMap = new HashMap<>(32);
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; vertexI++) {
                MyMesh.vertexBoneIndices(mesh, vertexI, iArray);
                MyMesh.vertexBoneWeights(mesh, vertexI, wArray);

                Map<String, Float> weightMap
                        = weightMap(iArray, wArray, lbNames);

                float bestTotalWeight = Float.NEGATIVE_INFINITY;
                String bestLbName = null;
                for (String lbName : weightMap.keySet()) { // TODO use entrySet
                    float totalWeight = weightMap.get(lbName);
                    if (totalWeight >= bestTotalWeight) {
                        bestTotalWeight = totalWeight;
                        bestLbName = lbName;
                    }
                }
                /*
                 * Add the bind-pose coordinates of the vertex
                 * to the linked bone's list.
                 */
                List<Vector3f> coordList;
                if (coordsMap.containsKey(bestLbName)) {
                    coordList = coordsMap.get(bestLbName);
                } else {
                    coordList = new ArrayList<>(20);
                    coordsMap.put(bestLbName, coordList);
                }
                Vector3f bindPosition = MyMesh.vertexVector3f(mesh,
                        VertexBuffer.Type.BindPosePosition, vertexI, null);
                coordList.add(bindPosition);
            }
        }

        return coordsMap;
    }

    /**
     * Create a PhysicsBoneLink for the named bone. TODO sort methods
     *
     * @param name the name of the bone to be linked (not null)
     * @param lbNames map from bone indices to linked-bone names (not null,
     * unaffected)
     * @param coordsMap map from bone names to vertex positions (not null,
     * unaffected)
     * @return a new bone link without a joint, added to the boneLinks map
     */
    private BoneLink createLink(String name, List<Vector3f> vertexLocations) {
        Bone bone = getBone(name);
        /*
         * Create the collision shape.
         */
        Transform invTransform = bone.getModelBindInverseTransform();
        Vector3f meshLocation = bone.getModelSpacePosition();
        CollisionShape boneShape
                = createShape(invTransform, meshLocation, vertexLocations);
        /*
         * Create the rigid body.
         */
        float boneMass = mass(name);
        assert boneMass > 0f : boneMass;
        PhysicsRigidBody prb = new PhysicsRigidBody(boneShape, boneMass);

        float viscousDamping = damping();
        prb.setDamping(viscousDamping, viscousDamping);

        BoneLink link = new BoneLink(this, bone, prb);
        boneLinks.put(name, link);

        return link;
    }

    /**
     * Create a hull collision shape, using the specified inverse transform and
     * list of vertex locations.
     *
     * @param inverseTransform (not null, unaffected)
     * @param vertexLocations list of vertex locations (not null, not empty,
     * unaffected)
     * @return a new shape
     */
    private static CollisionShape createShape(Transform inverseTransform,
            Vector3f offset, List<Vector3f> vertexLocations) {
        assert inverseTransform != null;
        assert vertexLocations != null;
        assert !vertexLocations.isEmpty();

        for (Vector3f location : vertexLocations) {
            location.subtractLocal(offset);
        }

        CollisionShape boneShape = new HullCollisionShape(vertexLocations);

        return boneShape;
    }

    /**
     * Map bone indices to names of linked bones.
     *
     * @return a new array of bone names
     */
    private String[] linkedBoneNameArray() {
        int numBones = skeleton.getBoneCount();
        String[] nameArray = new String[numBones];
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            while (true) {
                String boneName = bone.getName();
                if (isLinked(boneName)) {
                    nameArray[boneIndex] = boneName;
                    break;
                }
                bone = bone.getParent();
                if (bone == null) {
                    nameArray[boneIndex] = torsoFakeBoneName;
                    break;
                }
            }
        }

        return nameArray;
    }

    /**
     * Update the model transform based on the torso's rigid-body dynamics.
     */
    private void torsoDynamicUpdate() {
        RigidBodyMotionState motionState = torsoRigidBody.getMotionState();
        Transform torsoTransform = motionState.physicsTransform(null);
        Vector3f scale = torsoTransform.getScale();
        torsoRigidBody.getPhysicsScale(scale);

        Transform worldToParent; // inverse world transform of the parent node
        Node parent = getSpatial().getParent();
        if (parent == null) {
            worldToParent = new Transform();
        } else {
            Transform parentTransform = parent.getWorldTransform();
            worldToParent = parentTransform.invert();
        }

        Transform transform = meshToModel.clone();
        transform.combineWithParent(torsoTransform);
        transform.combineWithParent(worldToParent);
        getSpatial().setLocalTransform(transform);
    }

    /**
     * Update the torso's rigid body based on the transformer spatial and 1st
     * root bone.
     */
    private void torsoKinematicUpdate() {
        Bone bone = skeleton.getRoots()[0];
        Transform transform = physicsTransform(bone, rootBindOrientation[0],
                rootBindScale[0], null);
        torsoRigidBody.setPhysicsTransform(transform);
    }

    /**
     * Update the torso in blended Kinematic mode, based on the transforms of
     * the transformSpatial blended with the saved transform.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void torsoKinematicUpdate(float tpf) {
        Validate.nonNegative(tpf, "time per frame");

        if (torsoKinematicWeight < 1f) {
            /*
             * For a smooth transition, blend the saved local transform
             * (from the start of the transition to kinematic mode)
             * with the ending local transform.
             */
            Transform transform = new Transform();
            transform.interpolateTransforms(startModelTransform,
                    endModelTransform, torsoKinematicWeight);
            getSpatial().setLocalTransform(transform);
            /*
             * Also blend the saved root-bone transforms
             * (from the start of the transition to kinematic mode)
             * with any bone transforms applied by the AnimControl.
             */
            Bone[] rootBones = skeleton.getRoots();
            int numRootBones = rootBones.length;
            for (int rootIndex = 0; rootIndex < numRootBones; rootIndex++) {
                Bone bone = rootBones[rootIndex];
                Vector3f msp = bone.getModelSpacePosition();
                Quaternion msr = bone.getModelSpaceRotation();
                Vector3f mss = bone.getModelSpaceScale();
                Transform kinematicTransform = new Transform(msp, msr, mss);
                Transform startTransform = startRootTransform[rootIndex];
                transform.interpolateTransforms(startTransform,
                        kinematicTransform, torsoKinematicWeight);
                setBoneTransform(bone, transform);
            }
        }
        /*
         * Update the rigid body.
         */
        torsoKinematicUpdate();
        /*
         * If blending, increase the kinematic weight.
         */
        if (torsoKinematicWeight < 1f) {
            if (torsoBlendInterval == 0f) {
                torsoKinematicWeight = 1f;
            } else {
                torsoKinematicWeight += tpf / torsoBlendInterval;
                if (torsoKinematicWeight > 1f) {
                    torsoKinematicWeight = 1f; // done blending
                }
            }
        }
    }

    /**
     * Validate a skeleton.
     *
     * @param skeleton the skeleton to validate (not null, unaffected)
     */
    private static void validate(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        if (numBones < 0) {
            throw new IllegalArgumentException("Bone count is negative!");
        }

        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (bone == null) {
                throw new IllegalArgumentException("Bone is null!");
            }
            String boneName = bone.getName();
            if (boneName == null) {
                throw new IllegalArgumentException("Bone name is null!");
            } else if (boneName.equals(torsoFakeBoneName)) {
                throw new IllegalArgumentException("Bone has reserved name.");
            } else if (nameSet.contains(boneName)) {
                String msg = "Duplicate bone name: " + boneName;
                throw new IllegalArgumentException(msg);
            }
            nameSet.add(boneName);
        }
    }

    /**
     * Tabulate the total bone weight associated with each linked bone.
     *
     * @param biArray the array of bone indices (not null, unaffected)
     * @param bwArray the array of bone weights (not null, unaffected)
     * @param lbNames a map from bone indices to linked bone names (not null,
     * unaffected)
     * @return a new map from linked-bone names to total weight
     */
    private static Map<String, Float> weightMap(int[] biArray, float[] bwArray,
            String[] lbNames) {
        assert biArray.length == 4;
        assert bwArray.length == 4;

        Map<String, Float> weightMap = new HashMap<>(4);
        for (int j = 0; j < 4; j++) {
            int boneIndex = biArray[j];
            if (boneIndex != -1) {
                String lbName = lbNames[boneIndex];
                if (weightMap.containsKey(lbName)) {
                    float oldWeight = weightMap.get(lbName);
                    float newWeight = oldWeight + bwArray[j];
                    weightMap.put(lbName, newWeight);
                } else {
                    weightMap.put(lbName, bwArray[j]);
                }
            }
        }

        return weightMap;
    }
}

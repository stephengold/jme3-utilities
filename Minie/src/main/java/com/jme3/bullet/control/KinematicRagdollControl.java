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

import com.jme3.animation.AnimControl;
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
import com.jme3.bullet.control.ragdoll.PhysicsBoneLink;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
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
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * <strong>This control is still a WIP, use it at your own risk</strong><br> To
 * use this control you need a model with an AnimControl and a
 * SkeletonControl.<br> This should be the case if you imported an animated
 * model from Ogre or blender.<br> Note enabling/disabling the control
 * add/removes it from the physics space<br>
 * <p>
 * This control creates collision shapes for each bones of the skeleton when you
 * invoke spatial.addControl(ragdollControl). <ul> <li>The shape is
 * HullCollision shape based on the vertices associated with each bone</li>
 * <li>If you don't want each bone to be a collision shape, you can specify what
 * bones to use by using the addBoneName method<br> By using this method, bone
 * that are not used to create a shape, are "merged" to their parent to create
 * the collision shape. </li>
 * </ul>
 * <p>
 * There are 2 modes for this control: <ul> <li><strong>The kinematic modes
 * :</strong><br> this is the default behavior, this means that the collision
 * shapes of the body are able to interact with physics enabled objects. in this
 * mode physics shapes follow the motion of the animated skeleton (for example
 * animated by a key framed animation) this mode is enabled by calling
 * setKinematicMode(); </li> <li><strong>The ragdoll modes:</strong><br> To
 * enable this behavior, you need to invoke the setRagdollMode() method. In this
 * mode the character is entirely controlled by physics, so it will fall under
 * the gravity and move if any force is applied to it.</li>
 * </ul>
 *
 * TODO handle applyLocal
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
    final public static String torsoFakeBoneName = ">>KRC torso<<";
    // *************************************************************************
    // fields

    /**
     * list of registered collision listeners
     */
    private List<RagdollCollisionListener> listeners
            = new SafeArrayList<>(RagdollCollisionListener.class);
    /**
     * map bone names to masses for createSpatialData()
     */
    private Map<String, Float> massMap = new HashMap<>(32);
    /**
     * map bone names to joint presets for createSpatialData()
     */
    private Map<String, JointPreset> jointMap = new HashMap<>(32);
    /**
     * map bone names to simulation objects
     */
    private Map<String, PhysicsBoneLink> boneLinks = new HashMap<>(32);
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
     * mode of operation (not null, default=Kinematic)
     */
    private Mode mode = Mode.Kinematic;
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
     * map from IK bone names to goal locations
     */
    private Map<String, Vector3f> ikTargets = new HashMap<>();
    /**
     * map from IK bone names to chain depths
     */
    private Map<String, Integer> ikChainDepth = new HashMap<>();
    /**
     * rotational speed for inverse kinematics (radians per second, default=7)
     */
    private float ikRotSpeed = 7f;
    /**
     * viscous damping ratio for rigid bodies (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    private float damping = 0.6f;
    /**
     * distance threshold for inverse kinematics (default=0.1)
     */
    private float IKThreshold = 0.1f;
    /**
     * control that's responsible for skinning
     */
    private SkeletonControl skeletonControl = null;
    /**
     * spatial with the mesh-coordinate transform
     */
    private Spatial transformer = null;
    /**
     * transform mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;
    /**
     * gravitational acceleration for dynamic rigid bodies
     */
    private Vector3f gravityVector = new Vector3f(0f, -9.8f, 0f);

    /**
     * Enumerate joint-control modes for this control.
     */
    public enum Mode {
        /**
         * collision shapes follow the movements of bones in the skeleton
         */
        Kinematic,
        /**
         * skeleton is controlled by Bullet dynamics (gravity and collisions)
         */
        Ragdoll,
        /**
         * skeleton is controlled by inverse-kinematic targets
         */
        IK
    }
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled, Kinematic control with no bones.
     */
    public KinematicRagdollControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Link the named bone using a joint preset.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to link (not null)
     * @param boneMass the desired mass of the bone (&gt;0)
     * @param jointPreset the desired range of motion (not null)
     * @see #setJointLimit(java.lang.String,
     * com.jme3.bullet.control.ragdoll.JointPreset)
     */
    public void addBone(String boneName, float boneMass,
            JointPreset jointPreset) {
        Validate.nonNull(boneName, "name");
        Validate.positive(boneMass, "mass");
        Validate.nonNull(jointPreset, "joint preset");
        if (spatial != null) {
            throw new IllegalStateException(
                    "Cannot add bone while added to a spatial.");
        }

        massMap.put(boneName, boneMass);
        jointMap.put(boneName, jointPreset);
    }

    /**
     * Link the named bone using a joint with no range of motion.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to add (not null)
     * @param boneMass the desired mass of the bone (&gt;0)
     */
    public void addBone(String boneName, float boneMass) {
        Validate.nonNull(boneName, "name");
        Validate.positive(boneMass, "mass");

        addBone(boneName, boneMass, new JointPreset());
    }

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
     * Ensure that user control is enabled for any bones used by inverse
     * kinematics and disabled for any other bones.
     */
    public void applyUserControl() {
        MySkeleton.setUserControl(skeleton, false);

        if (ikTargets.isEmpty()) {
            setKinematicMode();

        } else {
            for (String ikBoneName : ikTargets.keySet()) {
                Bone bone = getBone(ikBoneName);
                while (bone != null) {
                    String name = bone.getName();
                    PhysicsBoneLink link = getBoneLink(name);
                    link.kinematicUpdate(boneLinks.keySet());
                    bone.setUserControl(true);
                    bone = bone.getParent();
                }
            }
        }
    }

    /**
     * Smoothly blend from Ragdoll mode to Kinematic mode. This is useful to
     * blend ragdoll actual position to a keyframe animation, for example.
     *
     * @param blendTime the blending time between ragdoll to anim (in seconds)
     */
    public void blendToKinematicMode(float blendTime) {
        if (mode == Mode.Kinematic) {
            // already in kinematic mode
            return;
        }

        mode = Mode.Kinematic;

        AnimControl animControl = spatial.getControl(AnimControl.class);
        animControl.setEnabled(true);

        for (PhysicsBoneLink link : boneLinks.values()) {
            link.startBlend(blendTime);
        }

        MySkeleton.setUserControl(skeleton, false);
    }

    /**
     * Read the mass of the named bone.
     *
     * @param boneName the name of the bone to access
     * @return the mass (&gt;0)
     */
    public float boneMass(String boneName) {
        if (!massMap.containsKey(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        float mass = massMap.get(boneName);

        assert mass > 0f : mass;
        return mass;
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
     *
     * @param boneName the name of the skeleton bone to access
     * @return the pre-existing instance, or null if not found
     */
    public Bone getBone(String boneName) {
        Bone result = skeleton.getBone(boneName);
        return result;
    }

    /**
     * Access the physics link for the named bone. This returns null if invoked
     * when the control is not added to a spatial.
     *
     * @param boneName the name of the bone to access
     * @return the pre-existing instance, or null if not found
     */
    public PhysicsBoneLink getBoneLink(String boneName) {
        PhysicsBoneLink link = boneLinks.get(boneName);
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
     * Read the rotation speed for inverse kinematics.
     *
     * @return speed (&ge;0)
     */
    public float getIkRotSpeed() {
        return ikRotSpeed;
    }

    /**
     * Read the distance threshold for inverse kinematics.
     *
     * @return the distance threshold (&ge;0)
     */
    public float getIKThreshold() {
        assert IKThreshold >= 0f : IKThreshold;
        return IKThreshold;
    }

    /**
     * Access the preset for the joint connecting the named bone to its parent
     * in the linked-bone hierarchy.
     *
     * @param boneName the name of the bone to access
     * @return the pre-existing instance (not null)
     */
    public JointPreset getJointPreset(String boneName) {
        if (!jointMap.containsKey(boneName)) {
            String msg = "No linked bone named " + MyString.quote(boneName);
            throw new IllegalArgumentException(msg);
        }
        JointPreset result = jointMap.get(boneName);

        assert result != null;
        return result;
    }

    /**
     * Read the mode of this control.
     *
     * @return an enum value (not null)
     */
    public Mode getMode() {
        assert mode != null;
        return mode;
    }

    /**
     * Access the physics object that represents the torso.
     *
     * @return the pre-existing object (not null)
     */
    public PhysicsRigidBody getTorso() {
        assert torsoRigidBody != null;
        return torsoRigidBody;
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
     * Enumerate all linked bones in this control.
     *
     * @return an unmodifiable collection of names
     */
    public Collection<String> linkedBoneNames() {
        Collection<String> names = jointMap.keySet();
        Collection<String> result = Collections.unmodifiableCollection(names);

        return result;
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale to the model
     * after it was initialized. Same as re-attaching.
     */
    public void reBuild() {
        if (spatial != null) {
            removeSpatialData(spatial);
            createSpatialData(spatial);
        }
    }

    /**
     * Remove all inverse-kinematics targets.
     */
    public void removeAllIKTargets() {
        ikTargets.clear();
        ikChainDepth.clear();
        applyUserControl();
    }

    /**
     * Remove the inverse-kinematics target for the specified bone.
     *
     * @param bone which bone has the target (not null, modified)
     */
    public void removeIKTarget(Bone bone) {
        int depth = ikChainDepth.remove(bone.getName());
        int i = 0;
        while (i < depth + 2 && bone.getParent() != null) {
            if (bone.hasUserControl()) {
                bone.setUserControl(false);
            }
            bone = bone.getParent();
            i++;
        }
        // TODO remove from ikTargets and applyUserControl()
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
        for (PhysicsBoneLink link : boneLinks.values()) {
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
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setCcdSweptSphereRadius(radius);
        }
    }

    /**
     * Alter the damping ratio.
     *
     * @param dampingRatio the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    public void setDamping(float dampingRatio) {
        Validate.nonNegative(dampingRatio, "damping ratio");

        damping = dampingRatio;
        torsoRigidBody.setDamping(damping, damping);
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setDamping(damping, damping);
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
     * Alter this control's gravitational acceleration.
     *
     * @param gravity the desired acceleration vector (in physics-space
     * coordinates, not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        Validate.nonNull(gravity, "gravity");

        this.gravityVector.set(gravity);
        torsoRigidBody.setGravity(gravity);
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setGravity(gravity);
        }
    }

    /**
     * Sets the control into Inverse Kinematics mode. The affected bones are
     * affected by IK. physics.
     */
    public void setIKMode() {
        if (mode != Mode.IK) {
            setMode(Mode.IK);
        }
    }

    /**
     * Alter the rotation speed for inverse kinematics.
     *
     * @param ikRotSpeed the desired speed (&ge;0, default=7)
     */
    public void setIkRotSpeed(float ikRotSpeed) {
        this.ikRotSpeed = ikRotSpeed;
    }

    /**
     * Add a target for inverse kinematics.
     *
     * @param bone which bone the IK applies to (not null)
     * @param worldGoal the world coordinates of the goal (not null)
     * @param chainLength number of bones in the chain
     * @return a new instance (not null, already added to ikTargets)
     */
    public Vector3f setIKTarget(Bone bone, Vector3f worldGoal,
            int chainLength) {
        Vector3f offset = transformer.getWorldTranslation();
        Vector3f meshGoal = worldGoal.subtract(offset);
        String boneName = bone.getName();
        ikTargets.put(boneName, meshGoal);
        ikChainDepth.put(boneName, chainLength);
        int i = 0;
        while (i < chainLength + 2 && bone.getParent() != null) {
            if (!bone.hasUserControl()) {
                bone.setUserControl(true);
            }
            bone = bone.getParent();
            i++;
        }

        return meshGoal;
    }

    /**
     * Alter the distance threshold for inverse kinematics.
     *
     * @param IKThreshold the desired distance threshold (default=0.1)
     */
    public void setIKThreshold(float IKThreshold) {
        this.IKThreshold = IKThreshold;
    }

    /**
     * Alter the limits of the joint connecting the named linked bone to its
     * parent.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param boneName the name of the bone (not null)
     * @param preset the desired range of motion (not null)
     */
    public void setJointLimit(String boneName, JointPreset preset) {
        if (spatial == null) {
            throw new IllegalStateException(
                    "Cannot set limits unless added to a spatial.");
        }

        PhysicsBoneLink link = getBoneLink(boneName);
        if (link == null) {
            throw new IllegalStateException(
                    "No linked bone named " + MyString.quote(boneName));
        }

        SixDofJoint joint = link.getJoint();
        preset.setupJoint(joint);
    }

    /**
     * Put the control into Kinematic mode. In this mode, the collision shapes
     * follow the movements of the skeleton while interacting with the physics
     * environment.
     */
    public void setKinematicMode() {
        if (mode != Mode.Kinematic) {
            setMode(Mode.Kinematic);
        }
    }

    /**
     * Sets the control mode to Ragdoll. The skeleton is entirely controlled by
     * physics.
     */
    public void setRagdollMode() {
        if (mode != Mode.Ragdoll) {
            setMode(Mode.Ragdoll);
        }
    }

    /**
     * Alter the mass of the torso.
     *
     * @param mass the desired mass (&gt;0)
     */
    public void setTorsoMass(float mass) {
        Validate.positive(mass, "mass");

        torsoMass = mass;
        if (torsoRigidBody != null) {
            torsoRigidBody.setMass(mass);
        }
    }

    /**
     * Read the mass of the torso.
     *
     * @return the mass (&gt;0)
     */
    public float torsoMass() {
        assert torsoMass > 0f : torsoMass;
        return torsoMass;
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
        Collection<String> boneSet = boneLinks.keySet();
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.kinematicUpdate(tpf, boneSet);
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

        Collection<String> boneSet = boneLinks.keySet();
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.dynamicUpdate(boneSet);
        }
    }

    /**
     * Alter the control mode.
     *
     * @param mode the desired mode (not null)
     */
    protected void setMode(Mode mode) {
        Validate.nonNull(mode, "mode");

        this.mode = mode;
        AnimControl animControl = spatial.getControl(AnimControl.class);
        animControl.setEnabled(mode == Mode.Kinematic);

        torsoRigidBody.setKinematic(mode == Mode.Kinematic);
        if (mode != Mode.IK) {
            for (PhysicsBoneLink link : boneLinks.values()) {
                link.getRigidBody().setKinematic(mode == Mode.Kinematic);
                if (mode == Mode.Ragdoll) {
                    // Ensure that the ragdoll is at the correct place.
                    link.kinematicUpdate(boneLinks.keySet());
                }
            }

            MySkeleton.setUserControl(skeleton, mode == Mode.Ragdoll);
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

        for (PhysicsBoneLink physicsBoneLink : boneLinks.values()) {
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

        boneLinks = cloner.clone(boneLinks);
        gravityVector = cloner.clone(gravityVector);
        jointMap = cloner.clone(jointMap);
        ikChainDepth = cloner.clone(ikChainDepth);
        ikTargets = cloner.clone(ikTargets);
        initScale = cloner.clone(initScale);
        listeners = cloner.clone(listeners);
        massMap = cloner.clone(massMap);
        meshToModel = cloner.clone(meshToModel);
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        torsoRigidBody = cloner.clone(torsoRigidBody);
        transformer = cloner.clone(transformer);
    }

    /**
     * Create spatial-dependent data. Invoked when the control is added to a
     * spatial.
     *
     * @param controlledSpatial the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial controlledSpatial) {
        Validate.nonNull(controlledSpatial, "controlled spatial");

        spatial = controlledSpatial;
        skeletonControl = spatial.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "The controlled spatial must have a SkeletonControl. Make sure the control is there and not on a subnode.");
        }
        skeleton = skeletonControl.getSkeleton();
        skeleton.resetAndUpdate();
        /*
         * Remove the SkeletonControl and re-add it so that it will get
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
         * Map bone indices to names of linked bones.
         */
        String[] tempLbNames = linkedBoneNameArray();
        /*
         * Assign each mesh vertex to a linked bone or else to the torso.
         */
        Map<String, List<Vector3f>> coordsMap = coordsMap(tempLbNames);
        /*
         * Create a rigid body for the torso.
         */
        List<Vector3f> list = coordsMap.get(torsoFakeBoneName);
        if (list == null) {
            throw new IllegalArgumentException(
                    "No mesh vertices found for the torso. Make sure the model's root bone wasn't added to the control.");
        }
        CollisionShape torsoShape
                = createShape(new Transform(), new Vector3f(), list);
        float mass = torsoMass();
        torsoRigidBody = new PhysicsRigidBody(torsoShape, mass);
        float damping = damping();
        torsoRigidBody.setDamping(damping, damping);
        torsoRigidBody.setKinematic(mode == Mode.Kinematic);
        torsoRigidBody.setUserObject(this);
        /*
         * Create bone links.
         */
        for (String boneName : jointMap.keySet()) {
            List<Vector3f> vertexLocations = coordsMap.get(boneName);
            createLink(boneName, tempLbNames, vertexLocations);
        }
        /*
         * Add joints to connect each linked bone with its parent.
         */
        addJoints(torsoFakeBoneName);

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
        // TODO jointMap, massMap
        PhysicsBoneLink[] loadedBoneLinks
                = (PhysicsBoneLink[]) ic.readSavableArray("boneList",
                        new PhysicsBoneLink[0]);
        for (PhysicsBoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.getBone().getName(), physicsBoneLink);
        }
        skeleton = (Skeleton) ic.readSavable("skeleton", null);
        skeletonControl
                = (SkeletonControl) ic.readSavable("skeletonControl", null);
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        transformer
                = (Spatial) ic.readSavable("transformer", null);
        initScale = (Vector3f) ic.readSavable("initScale", null);
        mode = ic.readEnum("mode", Mode.class, Mode.Kinematic);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);
        torsoMass = ic.readFloat("rootMass", 15f);
        gravityVector = (Vector3f) ic.readSavable("gravity", null);
    }

    /**
     * Remove all managed physics objects from the physics space.
     */
    @Override
    protected void removePhysics() {
        PhysicsSpace space = getPhysicsSpace();

        space.remove(torsoRigidBody);

        for (PhysicsBoneLink physicsBoneLink : boneLinks.values()) {
            space.remove(physicsBoneLink.getJoint());
            space.remove(physicsBoneLink.getRigidBody());
        }

        space.removeCollisionListener(this);
    }

    /**
     * Destroy spatial-dependent data. Invoked when this control is removed from
     * a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics();
        }
        boneLinks.clear();
        skeletonControl = null;
        skeleton = null;
        transformer = null;
        meshToModel = null;
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
        if (!isEnabled()) {
            return;
        }

        if (mode == Mode.IK) {
            ikUpdate(tpf);
        } else if (mode == Mode.Ragdoll) {
            // Update each bone's position and rotation based on dynamics.
            ragDollUpdate(tpf);
        } else {
            kinematicUpdate(tpf);
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
        // TODO jointMap, massMap
        oc.write(boneLinks.values().toArray(
                new PhysicsBoneLink[boneLinks.size()]),
                "boneLinks", new PhysicsBoneLink[0]);
        oc.write(skeleton, "skeleton", null);
        oc.write(skeletonControl, "skeletonControl", null);
        oc.write(transformer, "transformer", null);
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(initScale, "initScale", null);
        oc.write(mode, "mode", null);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);
        oc.write(torsoMass, "rootMass", 15f);
        oc.write(ikRotSpeed, "rotSpeed", 7f);
        oc.write(damping, "limbDampening", 0.6f);
        oc.write(gravityVector, "gravity", new Vector3f(0f, -9.8f, 0f));
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
        if (userA instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) userA;
            if (link != null) {
                krcInvolved = true;
                bone = link.getBone();
                otherPco = pcoB;
            }
        } else if (userA == this) {
            krcInvolved = true;
            bone = null;
            otherPco = pcoB;
        } else if (userB instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) userB;
            if (link != null) {
                krcInvolved = true;
                bone = link.getBone();
                otherPco = pcoA;
            }
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
     * Add joints to connect the named parent with each of its children. Note:
     * recursive!
     *
     * @param parentName the name of the parent, which must either be a linked
     * bone or the torso (not null)
     */
    private void addJoints(String parentName) {
        PhysicsBoneLink parentLink = getBoneLink(parentName);
        if (parentLink == null) {
            assert torsoFakeBoneName.equals(parentName);
        }

        PhysicsRigidBody parentBody;
        Vector3f parentLocation;
        if (parentLink == null) {
            parentBody = torsoRigidBody;
            parentLocation = new Vector3f();
        } else {
            parentBody = parentLink.getRigidBody();
            Bone parentBone = getBone(parentName);
            parentLocation = parentBone.getModelSpacePosition();
        }

        for (String name : boneLinks.keySet()) {
            PhysicsBoneLink link = getBoneLink(name);
            if (link.parentName().equals(parentName)) {
                Bone childBone = getBone(name);
                PhysicsRigidBody childBody = link.getRigidBody();
                Vector3f posToParent
                        = childBone.getModelSpacePosition().clone();
                posToParent.subtractLocal(parentLocation);
                posToParent.multLocal(initScale);

                SixDofJoint joint = new SixDofJoint(parentBody, childBody,
                        posToParent, new Vector3f(0f, 0f, 0f), true);
                assert link.getJoint() == null;
                link.setJoint(joint);

                JointPreset preset = getJointPreset(name);
                preset.setupJoint(joint);
                joint.setCollisionBetweenLinkedBodies(false);

                addJoints(name);
            }
        }
    }

    /**
     * Create a PhysicsBoneLink for the named bone.
     *
     * @param name the name of the bone to be linked (not null)
     * @param lbNames map from bone indices to linked-bone names (not null,
     * unaffected)
     * @param coordsMap map from bone names to vertex positions (not null,
     * unaffected)
     * @return a new bone link without a joint, added to the boneLinks map
     */
    private PhysicsBoneLink createLink(String name, String[] lbNames,
            List<Vector3f> vertexLocations) {
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
        float boneMass = boneMass(name);
        assert boneMass > 0f : boneMass;
        PhysicsRigidBody prb = new PhysicsRigidBody(boneShape, boneMass);
        float damping = damping();
        prb.setDamping(damping, damping);
        prb.setKinematic(mode == Mode.Kinematic);
        /*
         * Find the bone's parent in the linked-bone hierarchy.
         */
        Bone parent = bone.getParent();
        String parentName;
        if (parent == null) {
            parentName = torsoFakeBoneName;
        } else {
            int parentIndex = skeleton.getBoneIndex(parent);
            parentName = lbNames[parentIndex];
        }

        PhysicsBoneLink link
                = new PhysicsBoneLink(transformer, bone, prb, parentName);
        prb.setUserObject(link);
        boneLinks.put(name, link);

        return link;
    }

    /**
     * Assign each mesh vertex to a linked bone and add its location (mesh
     * coordinates in bind pose) to that bone's list.
     *
     * @param lbNames a map from bone indices to linked-bone names
     * @return a new map from linked-bone names to coordinates
     */
    private Map<String, List<Vector3f>> coordsMap(String[] lbNames) {
        Mesh[] meshes = skeletonControl.getTargets();
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Map<String, List<Vector3f>> coordsMap = new HashMap<>();
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; vertexI++) {
                MyMesh.vertexBoneIndices(mesh, vertexI, iArray);
                MyMesh.vertexBoneWeights(mesh, vertexI, wArray);

                Map<String, Float> weightMap
                        = weightMap(iArray, wArray, lbNames);

                float bestTotalWeight = Float.NEGATIVE_INFINITY;
                String bestLbName = null;
                for (String lbName : weightMap.keySet()) {
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
     * Create a hull collision shape, using the specified inverse transform and
     * list of vertex locations.
     *
     * @param inverseTransform (not null, unaffected)
     * @param vertexLocations list of vertex locations (not null, not empty,
     * unaffected)
     * @return a new shape
     */
    private CollisionShape createShape(Transform inverseTransform,
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
     * Update this control in IK mode, based on IK targets.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void ikUpdate(float tpf) {
        TempVars vars = TempVars.get();

        Quaternion tmpRot1 = vars.quat1;
        Quaternion[] tmpRot2 = new Quaternion[]{vars.quat2, new Quaternion()};

        Iterator<String> it = ikTargets.keySet().iterator();
        float distance;
        Bone bone;
        String boneName;
        while (it.hasNext()) {

            boneName = it.next();
            bone = getBoneLink(boneName).getBone();
            if (!bone.hasUserControl()) {
                logger.log(Level.FINE, "{0} doesn't have user control",
                        boneName);
                continue;
            }
            distance = bone.getModelSpacePosition().distance(
                    ikTargets.get(boneName));
            if (distance < IKThreshold) {
                logger.log(Level.FINE, "Distance is close enough");
                continue;
            }
            int depth = 0;
            int maxDepth = ikChainDepth.get(bone.getName());
            float changeAmount = tpf * (float) FastMath.sqrt(distance);
            updateBone(getBoneLink(boneName), changeAmount, tmpRot1,
                    tmpRot2, bone, ikTargets.get(boneName), depth, maxDepth);

            for (PhysicsBoneLink link : boneLinks.values()) {
                link.kinematicUpdate(boneLinks.keySet());
            }
        }
        vars.release();
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
                if (jointMap.containsKey(boneName)) {
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

    /*
     * Update the controlled spatial's transform based on torso dynamics.
     */
    private void torsoDynamicUpdate() {
        RigidBodyMotionState motionState = torsoRigidBody.getMotionState();
        Transform torsoTransform = motionState.physicsTransform(null);
        Vector3f scale = torsoTransform.getScale();
        torsoRigidBody.getPhysicsScale(scale);

        Transform worldToParent; // inverse world transform of the parent
        Node parent = spatial.getParent();
        if (parent == null) {
            worldToParent = new Transform();
        } else {
            Transform parentTransform = parent.getWorldTransform();
            worldToParent = parentTransform.invert();
        }

        Transform transform = meshToModel.clone();
        transform.combineWithParent(torsoTransform);
        transform.combineWithParent(worldToParent);
        spatial.setLocalTransform(transform);
    }

    /*
     * Update the torso to based on the transformer spatial.
     */
    private void torsoKinematicUpdate() {
        Vector3f translation = transformer.getWorldTranslation();
        torsoRigidBody.setPhysicsLocation(translation);

        Quaternion orientation = transformer.getWorldRotation();
        torsoRigidBody.setPhysicsRotation(orientation);

        Vector3f scale = transformer.getWorldScale();
        torsoRigidBody.setPhysicsScale(scale);
    }

    /**
     * Update a bone and its ancestors in IK mode. Note: recursive!
     *
     * @param link the bone link for the affected bone (may be null)
     * @param changeAmount amount of change desired (&ge;0)
     * @param tmpRot1 temporary storage used in calculations (not null)
     * @param tmpRot2 temporary storage used in calculations (not null)
     * @param tipBone (not null)
     * @param target the location target in model space (not null, unaffected)
     * @param depth depth of the recursion (&ge;0)
     * @param maxDepth recursion limit (&ge;0)
     */
    private void updateBone(PhysicsBoneLink link, float changeAmount,
            Quaternion tmpRot1, Quaternion[] tmpRot2, Bone tipBone,
            Vector3f target, int depth, int maxDepth) {
        Validate.nonNegative(changeAmount, "change amount");

        if (link == null || link.getBone().getParent() == null) {
            return;
        }
        Quaternion preQuat = link.getBone().getLocalRotation();
        Vector3f vectorAxis;

        float[] measureDist = new float[]{Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY};
        for (int dirIndex = 0; dirIndex < 3; dirIndex++) {
            if (dirIndex == PhysicsSpace.AXIS_X) {
                vectorAxis = Vector3f.UNIT_Z;
            } else if (dirIndex == PhysicsSpace.AXIS_Y) {
                vectorAxis = Vector3f.UNIT_X;
            } else {
                vectorAxis = Vector3f.UNIT_Y;
            }

            for (int posOrNeg = 0; posOrNeg < 2; posOrNeg++) {
                float rot = ikRotSpeed * changeAmount / (link.getRigidBody().getMass() * 2);

                rot = FastMath.clamp(rot,
                        link.getJoint().getRotationalLimitMotor(dirIndex).getLoLimit(),
                        link.getJoint().getRotationalLimitMotor(dirIndex).getHiLimit());
                tmpRot1.fromAngleAxis(rot, vectorAxis);
//                tmpRot1.fromAngleAxis(rotSpeed * tpf / (link.rigidBody.getMass() * 2), vectorAxis);

                tmpRot2[posOrNeg] = link.getBone().getLocalRotation().mult(tmpRot1);
                tmpRot2[posOrNeg].normalizeLocal();

                ikRotSpeed = -ikRotSpeed;

                link.getBone().setLocalRotation(tmpRot2[posOrNeg]);
                link.getBone().update();
                measureDist[posOrNeg]
                        = tipBone.getModelSpacePosition().distance(target);
                link.getBone().setLocalRotation(preQuat);
            }

            if (measureDist[0] < measureDist[1]) {
                link.getBone().setLocalRotation(tmpRot2[0]);
            } else if (measureDist[0] > measureDist[1]) {
                link.getBone().setLocalRotation(tmpRot2[1]);
            }

        }
        link.getBone().getLocalRotation().normalizeLocal();

        link.getBone().update();
        if (link.getBone().getParent() != null && depth < maxDepth) {
            updateBone(getBoneLink(link.getBone().getParent().getName()),
                    0.5f * changeAmount, tmpRot1, tmpRot2, tipBone,
                    target, depth + 1, maxDepth);
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
    private Map<String, Float> weightMap(int[] biArray, float[] bwArray,
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

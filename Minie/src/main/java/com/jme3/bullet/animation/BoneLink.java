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
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Link an animated bone in a skeleton to a jointed rigid body in a ragdoll.
 * TODO superclass shared by all links
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class BoneLink
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(BoneLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * linked bone in the skeleton (not null)
     */
    private Bone bone;
    /**
     * bones managed by this link, in a pre-order, depth-first traversal of the
     * skeleton, starting with the linked bone
     */
    private Bone[] managedBones = null;
    /**
     * back pointer to the control that manages this link
     */
    private DynamicAnimControl control;
    /**
     * duration of the most recent blend interval (in seconds, &ge;0)
     */
    private float blendInterval = 1f;
    /**
     * weighting of kinematic movement (&ge;0, &le;1, 0=purely dynamic, 1=purely
     * kinematic, default=1, progresses from 0 to 1 during the blend interval)
     */
    private float kinematicWeight = 1f;
    /**
     * submode when kinematic
     */
    private KinematicSubmode submode = KinematicSubmode.Animated;
    /**
     * linked rigid body in the ragdoll (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * joint between the rigid body and the parent's rigid body, or null if not
     * yet created
     */
    private SixDofJoint joint = null;
    /**
     * name of parent in the link hierarchy (could be torsoBoneName)
     */
    private String parentName;
    /**
     * local transform of each managed bone from the previous update
     */
    private Transform[] prevBoneTransforms = null;
    /**
     * local transform of each managed bone at the start of the most recent
     * blend interval
     */
    private Transform[] startBoneTransforms = null;
    /**
     * location of the rigid body's center (in the bone's local coordinates)
     */
    private Vector3f localOffset;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public BoneLink() {
    }

    /**
     * Instantiate a purely kinematic link between the specified skeleton bone
     * and the specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param bone the skeleton bone to link (not null, alias created)
     * @param rigidBody the rigid body to link (not null, alias created)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     */
    BoneLink(DynamicAnimControl control, Bone bone,
            PhysicsRigidBody rigidBody, Vector3f localOffset) {
        assert control != null;
        assert bone != null;
        assert rigidBody != null;
        assert localOffset != null;

        this.control = control;
        this.bone = bone;
        this.rigidBody = rigidBody;
        this.localOffset = localOffset.clone();

        kinematicWeight = 1f;
        rigidBody.setKinematic(true);
        rigidBody.setUserObject(this);

        String name = bone.getName();
        parentName = control.parentName(name);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin blending this link to a fully kinematic mode. TODO publicize
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    void blendToKinematicMode(KinematicSubmode submode, float blendInterval) {
        assert submode != null;
        assert blendInterval >= 0f : blendInterval;

        this.submode = submode;
        this.blendInterval = blendInterval;
        kinematicWeight = Float.MIN_VALUE; // non-zero to trigger blending
        rigidBody.setKinematic(true);
        /*
         * Save initial bone transforms for blending.
         */
        int numManagedBones = managedBones.length;
        for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
            Transform transform;
            if (prevBoneTransforms == null) { // this link not updated yet
                Bone managedBone = managedBones[mbIndex];
                transform = MySkeleton.copyLocalTransform(managedBone, null);
            } else {
                transform = prevBoneTransforms[mbIndex];
            }
            startBoneTransforms[mbIndex].set(transform);
        }
        /*
         * Take or release control of the managed bones.
         */
        boolean wantUserControl;
        if (submode == KinematicSubmode.Animated) {
            wantUserControl = false;
        } else {
            wantUserControl = true;
        }
        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(wantUserControl);
        }
    }

    /**
     * Immediately freeze this link.
     */
    void freeze() {
        if (kinematicWeight > 0f) {
            blendToKinematicMode(KinematicSubmode.Frozen, 0f);
        } else {
            setDynamic(new Vector3f(0f, 0f, 0f), true, true, true);
        }
    }

    /**
     * Access the linked bone.
     *
     * @return the pre-existing instance (not null)
     */
    public Bone getBone() {
        assert bone != null;
        return bone;
    }

    /**
     * Access the joint between this link's rigid body and that of its parent.
     *
     * @return the pre-existing instance or null
     */
    public SixDofJoint getJoint() {
        return joint;
    }

    /**
     * Access the linked rigid body.
     *
     * @return the pre-existing instance (not null)
     */
    public PhysicsRigidBody getRigidBody() {
        assert rigidBody != null;
        return rigidBody;
    }

    /**
     * Copy the local offset of this link.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the offset (in bone local coordinates, either storeResult or a
     * new vector, not null)
     */
    Vector3f localOffset(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(localOffset);
        return result;
    }

    /**
     * Read the name of this link's parent in the link hierarchy.
     *
     * @return the link name (not null)
     */
    public String parentName() {
        assert parentName != null;
        return parentName;
    }

    /**
     * Calculate a physics transform for the rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated transform (in physics coordinates, either
     * storeResult or a new transform, not null)
     */
    public Transform physicsTransform(Transform storeResult) {
        Transform result
                = control.physicsTransform(bone, localOffset, storeResult);
        return result;
    }

    /**
     * Internal callback, invoked just before the physics is stepped.
     */
    void prePhysicsTick() {
        if (kinematicWeight > 0f) {
            /*
             * Update the rigid body's transform, including
             * the scale of its shape.
             */
            Transform transform = physicsTransform(null);
            rigidBody.setPhysicsTransform(transform);
        }
    }

    /**
     * Immediately put this link into dynamic mode.
     *
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     * @param lockX true to lock the joint's X-axis
     * @param lockY true to lock the joint's Y-axis
     * @param lockZ true to lock the joint's Z-axis
     */
    public void setDynamic(Vector3f uniformAcceleration, boolean lockX,
            boolean lockY, boolean lockZ) {
        Validate.nonNull(uniformAcceleration, "uniform acceleration");

        kinematicWeight = 0f;
        rigidBody.setGravity(uniformAcceleration);
        rigidBody.setKinematic(false);

        String name = bone.getName();
        JointPreset preset = control.getJointLimits(name);
        preset.setupJoint(joint, lockX, lockY, lockZ);

        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(true);
        }
    }

    /**
     * Assign a physics joint to this link and configure its range of motion.
     * Also initialize its array of managed bones.
     *
     * @param joint (not null, alias created)
     */
    void setJoint(SixDofJoint joint) {
        assert joint != null;

        assert this.joint == null;
        this.joint = joint;

        String name = bone.getName();
        JointPreset rangeOfMotion = control.getJointLimits(name);
        rangeOfMotion.setupJoint(joint, false, false, false);

        joint.setCollisionBetweenLinkedBodies(false);

        assert managedBones == null;
        managedBones = control.listManagedBones(bone.getName());

        int numManagedBones = managedBones.length;
        startBoneTransforms = new Transform[numManagedBones];
        for (int i = 0; i < numManagedBones; i++) {
            startBoneTransforms[i] = new Transform();
        }
    }

    /**
     * Internal callback, invoked once per frame during the logical-state
     * update, provided the control is added to a scene.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    void update(float tpf) {
        assert tpf >= 0f : tpf;

        if (prevBoneTransforms == null) {
            /*
             * On the first update, allocate and initialize
             * the array of previous bone transforms, if it wasn't
             * allocated in blendToKinematicMode().
             */
            int numManagedBones = managedBones.length;
            prevBoneTransforms = new Transform[numManagedBones];
            for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
                Bone managedBone = managedBones[mbIndex];
                Transform boneTransform
                        = MySkeleton.copyLocalTransform(managedBone, null);
                prevBoneTransforms[mbIndex] = boneTransform;
            }
        }

        if (kinematicWeight > 0f) {
            kinematicUpdate(tpf);
        } else {
            dynamicUpdate();
        }
        /*
         * Save copies of the latest bone transforms.
         */
        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            Transform lastTransform = prevBoneTransforms[mbIndex];
            Bone managedBone = managedBones[mbIndex];
            MySkeleton.copyLocalTransform(managedBone, lastTransform);
        }
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned link into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this link (not null)
     * @param original the instance from which this link was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        bone = cloner.clone(bone);
        managedBones = cloner.clone(managedBones);
        control = cloner.clone(control);
        rigidBody = cloner.clone(rigidBody);
        joint = cloner.clone(joint);
        prevBoneTransforms = cloner.clone(prevBoneTransforms);
        startBoneTransforms = cloner.clone(startBoneTransforms);
        localOffset = cloner.clone(localOffset);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public BoneLink jmeClone() {
        try {
            BoneLink clone = (BoneLink) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this link, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        bone = (Bone) ic.readSavable("bone", null);

        Savable[] tmp = ic.readSavableArray("managedBones", null);
        if (tmp == null) {
            managedBones = null;
        } else {
            managedBones = new Bone[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                managedBones[i] = (Bone) tmp[i];
            }
        }

        control = (DynamicAnimControl) ic.readSavable("control", null);
        blendInterval = ic.readFloat("blendInterval", 1f);
        kinematicWeight = ic.readFloat("kinematicWeight", 1f);
        submode = ic.readEnum("submode", KinematicSubmode.class,
                KinematicSubmode.Animated);
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        joint = (SixDofJoint) ic.readSavable("joint", null);
        parentName = ic.readString("parentName", null);
        prevBoneTransforms = RagUtils.readTransformArray(ic,
                "prevBoneTransforms");
        startBoneTransforms = RagUtils.readTransformArray(ic,
                "startBoneTransforms");
        localOffset = (Vector3f) ic.readSavable("offset", new Vector3f());
    }

    /**
     * Serialize this link, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);

        oc.write(bone, "bone", null);
        oc.write(managedBones, "managedBones", null);
        oc.write(control, "control", null);
        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
        oc.write(submode, "submode", KinematicSubmode.Animated);
        oc.write(rigidBody, "rigidBody", null);
        oc.write(joint, "joint", null);
        oc.write(parentName, "parentName", null);
        oc.write(prevBoneTransforms, "prevBoneTransforms", new Transform[0]);
        oc.write(startBoneTransforms, "startBoneTransforms", new Transform[0]);
        oc.write(localOffset, "offset", new Vector3f());
    }
    // *************************************************************************
    // private methods

    /**
     * Update this link in Dynamic mode, setting the linked bone's transform
     * based on the transform of the rigid body.
     */
    private void dynamicUpdate() {
        assert !rigidBody.isKinematic();

        Transform transform = localBoneTransform(null);
        MySkeleton.setLocalTransform(bone, transform);

        for (Bone managedBone : managedBones) {
            managedBone.updateModelTransforms();
        }
    }

    /**
     * Update this link in blended Kinematic mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void kinematicUpdate(float tpf) {
        assert tpf >= 0f : tpf;
        assert rigidBody.isKinematic();

        Transform transform = new Transform();

        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            Bone managedBone = managedBones[mbIndex];
            switch (submode) {
                case Amputated:
                    MySkeleton.copyBindTransform(managedBone, transform);
                    transform.getScale().set(0.001f, 0.001f, 0.001f);
                    break;
                case Animated:
                    MySkeleton.copyLocalTransform(managedBone, transform);
                    break;
                case Bound:
                    MySkeleton.copyBindTransform(managedBone, transform);
                    break;
                case Frozen:
                    transform.set(prevBoneTransforms[mbIndex]);
                    break;
                default:
                    throw new IllegalStateException(submode.toString());
            }

            if (kinematicWeight < 1f) {
                /*
                 * For a smooth transition, blend the saved bone transform
                 * (from the start of the blend interval)
                 * into the goal transform.
                 */
                Transform start = startBoneTransforms[mbIndex];
                Quaternion startQuat = start.getRotation();
                Quaternion endQuat = transform.getRotation();
                if (startQuat.dot(endQuat) < 0f) {
                    endQuat.multLocal(-1f);
                }
                MyMath.slerp(kinematicWeight, startBoneTransforms[mbIndex],
                        transform, transform);
                // TODO smarter sign flipping for bones
            }
            /*
             * Update the managed bone.
             */
            MySkeleton.setLocalTransform(managedBone, transform);
            managedBone.updateModelTransforms();
            // The rigid-body transform gets updated by prePhysicsTick().
        }
        /*
         * If blending, increase the kinematic weight.
         */
        if (blendInterval == 0f) {
            kinematicWeight = 1f; // done blending
        } else {
            kinematicWeight += tpf / blendInterval;
            if (kinematicWeight > 1f) {
                kinematicWeight = 1f; // done blending
            }
        }
    }

    /**
     * Calculate the local bone transform to match the physics transform of the
     * rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated bone transform (in local coordinates, either
     * storeResult or a new transform, not null)
     */
    private Transform localBoneTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();
        /*
         * Start with the body's transform in physics/world coordinates.
         */
        rigidBody.getPhysicsTransform(result);
        /*
         * Convert to mesh coordinates.
         */
        Transform worldToMesh = control.meshTransform(null).invert();
        result.combineWithParent(worldToMesh);
        /*
         * Convert to the bone's local coordinate system by factoring out the
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
        /*
         * Subtract the body's local offset, rotated and scaled.
         */
        Vector3f parentOffset = localOffset.clone();
        parentOffset.multLocal(scale);
        orientation.mult(parentOffset, parentOffset);
        location.subtractLocal(parentOffset);

        return result;
    }
}

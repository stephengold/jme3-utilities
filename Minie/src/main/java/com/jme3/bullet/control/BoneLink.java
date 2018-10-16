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
import com.jme3.bullet.control.ragdoll.KinematicSubmode;
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
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Link an animated bone in a skeleton to a jointed rigid body in a ragdoll.
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
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
     * managed bones in a pre-order, depth-first traversal of the skeleton,
     * starting with the linked bone
     */
    private Bone[] managedBones = null;
    /**
     * duration of the current transition to kinematic mode (in seconds, &ge;0)
     */
    private float blendInterval = 1f;
    /**
     * weighting of kinematic movement (&ge;0, &le;1, 0=purely dynamic, 1=purely
     * kinematic, default=1, progresses from 0 to 1 during the blend interval)
     */
    private float kinematicWeight;
    /**
     * back pointer to the control that manages this linked bone
     */
    private KinematicRagdollControl krc;
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
     * name of parent in the hierarchy of linked bones, or torsoBoneName if
     * parented by the torso
     */
    private String parentName;
    /**
     * local transform of each managed bone at the start of the most recent
     * transition to kinematic mode
     */
    private Transform[] startTransforms = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a purely kinematic link between the specified skeleton bone
     * and rigid body.
     *
     * @param krc the control that will manage this link (not null)
     * @param bone the skeleton bone to link (not null)
     * @param rigidBody the rigid body to link (not null)
     */
    BoneLink(KinematicRagdollControl krc, Bone bone,
            PhysicsRigidBody rigidBody) {
        Validate.nonNull(krc, "control");
        Validate.nonNull(bone, "bone");
        Validate.nonNull(rigidBody, "rigid body");

        this.krc = krc;
        this.bone = bone;
        this.rigidBody = rigidBody;

        kinematicWeight = 1f;
        rigidBody.setKinematic(true);
        rigidBody.setUserObject(this);

        String name = bone.getName();
        parentName = krc.parentName(name);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin transitioning this link to fully kinematic mode.
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    void blendToKinematicMode(KinematicSubmode submode, float blendInterval) {
        Validate.nonNegative(blendInterval, "blend interval");
        assert submode == KinematicSubmode.Animated;

        this.blendInterval = blendInterval;
        kinematicWeight = Float.MIN_VALUE; // non-zero to trigger blending
        rigidBody.setKinematic(true);
        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(false);
        }
    }

    /**
     * Access the linked bone.
     *
     * @return the pre-existing instance (not null)
     */
    public Bone getBone() {
        return bone;
    }

    /**
     * Access the joint between the bone's body and its parent's body, if any.
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
        return rigidBody;
    }

    /**
     * Read the name of the bone's parent in the linked-bone hierarchy.
     *
     * @return name (not null)
     */
    public String parentName() {
        assert parentName != null;
        return parentName;
    }

    /**
     * Put this link into dynamic mode.
     *
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamic(Vector3f uniformAcceleration) {
        kinematicWeight = 0f;
        rigidBody.setGravity(uniformAcceleration);
        rigidBody.setKinematic(false);
    }

    /**
     * Assign the physics joint for this bone link. Also enumerate the managed
     * bones and allocate startTransforms.
     *
     * @param joint (not null, alias created)
     */
    void setJoint(SixDofJoint joint) {
        Validate.nonNull(joint, "joint");
        assert this.joint == null;

        this.joint = joint;
        managedBones = krc.listManagedBones(bone.getName());

        int numManagedBones = managedBones.length;
        startTransforms = new Transform[numManagedBones];
        for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
            startTransforms[mbIndex] = new Transform();
        }
    }

    /**
     * Update this linked bone according to its mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    void update(float tpf) {
        Validate.nonNegative(tpf, "time per frame");

        if (kinematicWeight > 0f) {
            kinematicUpdate(tpf);
        } else {
            dynamicUpdate();
        }

        if (kinematicWeight == 0f || kinematicWeight == 1f) {
            /*
             * Not already blending, so copy the latest bone transforms in
             * case blending starts on the next update.
             */
            for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
                Transform startTransform = startTransforms[mbIndex];
                Bone managedBone = managedBones[mbIndex];
                MySkeleton.copyLocalTransform(managedBone, startTransform);
            }
        }
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned object into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this shape (not null)
     * @param original the instance from which this instance was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        bone = cloner.clone(bone);
        krc = cloner.clone(krc);
        managedBones = cloner.clone(managedBones);
        rigidBody = cloner.clone(rigidBody);
        joint = cloner.clone(joint);
        startTransforms = cloner.clone(startTransforms);
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
     * De-serialize this bone link, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        krc = (KinematicRagdollControl) ic.readSavable("krc", null);
        managedBones = (Bone[]) ic.readSavableArray("managedBones", null);
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        bone = (Bone) ic.readSavable("bone", null);
        joint = (SixDofJoint) ic.readSavable("joint", null);
        parentName = ic.readString("parentName", null);
        startTransforms = (Transform[]) ic.readSavableArray("startTransforms",
                new Transform[0]);
        blendInterval = ic.readFloat("blendInterval", 1f);
        kinematicWeight = ic.readFloat("kinematicWeight", 1f);
    }

    /**
     * Serialize this bone link, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(krc, "krc", null);
        oc.write(managedBones, "managedBones", null);
        oc.write(rigidBody, "rigidBody", null);
        oc.write(bone, "bone", null);
        oc.write(joint, "joint", null);
        oc.write(parentName, "parentName", null);
        oc.write(startTransforms, "startTransforms", new Transform[0]);
        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the skeleton in Dynamic mode, based on the transform of the rigid
     * body.
     */
    private void dynamicUpdate() {
        Transform transform
                = krc.localBoneTransform(rigidBody, bone, null);
        MySkeleton.setLocalTransform(bone, transform);

        for (Bone managedBone : managedBones) {
            managedBone.updateModelTransforms();
        }
    }

    /**
     * Update this linked bone in blended Kinematic mode, based on the
     * transforms of the transformSpatial and the skeleton, blended with the
     * saved transforms.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void kinematicUpdate(float tpf) {
        Validate.nonNegative(tpf, "time per frame");

        Transform transform = new Transform();
        Vector3f location = transform.getTranslation();
        Quaternion orientation = transform.getRotation();
        Vector3f scale = transform.getScale();

        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            /*
             * Read the animation's local transform for this bone, assuming
             * the animation control is enabled.
             */
            Bone managedBone = managedBones[mbIndex];
            MySkeleton.copyLocalTransform(managedBone, transform);

            if (kinematicWeight < 1f) {
                /*
                 * For a smooth transition, blend the saved bone transform
                 * (from the start of the transition to kinematic mode)
                 * into the bone transform from the AnimControl.
                 */
                Transform startTransform = startTransforms[mbIndex];
                MyVector3f.lerp(kinematicWeight,
                        startTransform.getTranslation(), location, location);
                if (startTransform.getRotation().dot(orientation) < 0f) {
                    orientation.multLocal(-1f);
                }
                MyQuaternion.slerp(kinematicWeight,
                        startTransform.getRotation(), orientation, orientation);
                MyVector3f.lerp(kinematicWeight,
                        startTransform.getScale(), scale, scale);
            }
            /*
             * Update the managed bone.
             */
            MySkeleton.setLocalTransform(managedBone, transform);
            managedBone.updateModelTransforms();

            if (managedBone == bone) {
                krc.physicsTransform(bone, transform);
                rigidBody.setPhysicsTransform(transform);
            }
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
}

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
import jme3utilities.Validate;

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
     * animated bone in the skeleton (not null)
     */
    private Bone bone;
    /**
     * duration of the current transition to kinematic mode (in seconds, &ge;0)
     */
    private float blendInterval = 1f;
    /**
     * weighting of kinematic movement (&ge;0, &le;1, 0=purely dynamic, 1=purely
     * kinematic, default=1)
     */
    private float kinematicWeight = 1f;
    /**
     * back pointer to the control that manages this link
     */
    private KinematicRagdollControl krc;
    /**
     * rigid body in the ragdoll (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * orientation of the bone (in mesh coordinates) when this link was created
     */
    private Quaternion bindOrientation = new Quaternion();
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
     * bone transform (in mesh coordinates) at the start of the most recent
     * transition to kinematic mode
     */
    private Transform startTransform = null;
    /**
     * scale of the bone (in mesh coordinates) when this link was created
     */
    private Vector3f bindScale = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a purely kinematic link between the specified skeleton bone
     * and rigid body.
     *
     * @param krc the control that will manage this link (not null)
     * @param bone the skeleton bone to link (not null)
     * @param rigidBody the rigid body to link (not null)
     * @param parentName the name of the bone's parent in the linked-bone
     * hierarchy (not null)
     */
    public BoneLink(KinematicRagdollControl krc, Bone bone,
            PhysicsRigidBody rigidBody, String parentName) {
        Validate.nonNull(krc, "control");
        Validate.nonNull(bone, "bone");
        Validate.nonNull(rigidBody, "rigid body");
        Validate.nonNull(parentName, "parent name");

        this.krc = krc;
        this.bone = bone;
        this.rigidBody = rigidBody;
        this.parentName = parentName;

        Quaternion msr = bone.getModelSpaceRotation();
        bindOrientation.set(msr);

        Vector3f mss = bone.getModelSpaceScale();
        bindScale.set(mss);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin transitioning this link to fully kinematic mode.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    void blendToKinematicMode(float blendInterval) {
        Validate.nonNegative(blendInterval, "blend interval");

        startTransform = krc.localBoneTransform(rigidBody, bindOrientation,
                bindScale, startTransform);
        this.blendInterval = blendInterval;
        kinematicWeight = Float.MIN_VALUE; // not zero!
        rigidBody.setKinematic(true);
        krc.setUserMode(bone, false);
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
     * Assign a physics joint to this bone link.
     *
     * @param joint (not null)
     */
    public void setJoint(SixDofJoint joint) {
        Validate.nonNull(joint, "joint");
        this.joint = joint;
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
        rigidBody = cloner.clone(rigidBody);
        bindOrientation = cloner.clone(bindOrientation);
        joint = cloner.clone(joint);
        startTransform = cloner.clone(startTransform);
        bindScale = cloner.clone(bindScale);
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
        // TODO krc
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        bone = (Bone) ic.readSavable("bone", null);
        joint = (SixDofJoint) ic.readSavable("joint", null);
        parentName = ic.readString("parentName", null);

        Quaternion readQuat = (Quaternion) ic.readSavable("initalWorldRotation",
                new Quaternion());
        bindOrientation.set(readQuat);
        Vector3f readVec = (Vector3f) ic.readSavable("originalScale",
                new Vector3f());
        bindScale.set(readVec);

        Transform read
                = (Transform) ic.readSavable("startTransform", new Transform());
        startTransform.set(read);

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
        // TODO krc
        oc.write(rigidBody, "rigidBody", null);
        oc.write(bone, "bone", null);
        oc.write(joint, "joint", null);
        oc.write(parentName, "parentName", null);

        oc.write(bindOrientation, "initalWorldRotation", null);
        oc.write(bindScale, "originalScale", null);

        oc.write(startTransform, "startTransform", new Transform());

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
        Transform transform = krc.localBoneTransform(rigidBody, bindOrientation,
                bindScale, null);

        // Update the transforms in the skeleton.
        krc.setBoneTransform(bone, transform);
    }

    /**
     * Update the rigid body in pure Kinematic mode, based on the transforms of
     * the transformSpatial and the skeleton, without blending.
     */
    private void kinematicUpdate() {
        Transform transform
                = krc.physicsTransform(bone, bindOrientation, bindScale, null);

        Vector3f location = transform.getTranslation();
        Quaternion orientation = transform.getRotation();
        Vector3f scale = transform.getScale();

        // Update the transform of the rigid body. TODO use MotionState?
        rigidBody.setPhysicsLocation(location);
        rigidBody.setPhysicsRotation(orientation);
        rigidBody.setPhysicsScale(scale);
    }

    /**
     * Update this linked bone in blended Kinematic mode, based on the
     * transforms of the transformSpatial and the skeleton, blended with the
     * saved transform.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void kinematicUpdate(float tpf) {
        Validate.nonNegative(tpf, "time per frame");

        if (kinematicWeight < 1f) {
            /*
             * For a smooth transition, blend the saved bone transform
             * (from the start of the transition to kinematic mode)
             * into the bone transform applied by the AnimControl.
             */
            Vector3f msp = bone.getModelSpacePosition();
            Quaternion msr = bone.getModelSpaceRotation();
            Vector3f mss = bone.getModelSpaceScale();
            Transform kinematicTransform = new Transform(msp, msr, mss);

            Transform transform = new Transform();
            transform.interpolateTransforms(startTransform, kinematicTransform,
                    kinematicWeight);
            krc.setBoneTransform(bone, transform);
        }
        /*
         * Update the rigid body.
         */
        kinematicUpdate();
        /*
         * If blending, increase the kinematic weight.
         */
        if (blendInterval == 0f) {
            kinematicWeight = 1f;
        } else {
            kinematicWeight += tpf / blendInterval;
            if (kinematicWeight > 1f) {
                kinematicWeight = 1f; // done blending
            }
        }
    }
}

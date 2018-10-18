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
package com.jme3.bullet.control;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.bullet.control.ragdoll.KinematicSubmode;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Link the torso of an animated model to a rigid body in a ragdoll.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TorsoLink
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TorsoLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * main bone: the root bone with the most animation weight (not null)
     */
    private Bone bone;
    /**
     * bones managed by this link, in a pre-order, depth-first traversal of the
     * skeleton
     */
    private Bone[] managedBones = null;
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
     * back pointer to the control that manages this link
     */
    private KinematicRagdollControl krc;
    /**
     * submode when kinematic
     */
    private KinematicSubmode submode = KinematicSubmode.Animated;
    /**
     * linked rigid body in the ragdoll (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * skeleton being controlled
     */
    private Skeleton skeleton = null;
    /**
     * local transform for the controlled spatial at the end of the torso's most
     * recent blend interval, or null for no spatial blending
     */
    private Transform endModelTransform = null;
    /**
     * transform from mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;
    /**
     * local transform of the controlled spatial at the start of the torso's
     * most recent blend interval, or null for no spatial blending
     */
    private Transform startModelTransform = null;
    /**
     * /**
     * local transform of each managed bone from the previous update
     */
    private Transform[] prevBoneTransforms = null;
    /**
     * local transform of each managed bone at the start of the most recent
     * blend interval
     */
    private Transform[] startBoneTransforms = null;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public TorsoLink() {
    }

    /**
     * Instantiate a purely kinematic link between the torso of the specified
     * control and the specified rigid body.
     *
     * @param krc the control that will manage this link (not null)
     * @param mainBone the root bone with the most animation weight (not null)
     * @param meshToModel the transform from mesh coordinates to model
     * coordinates (not null, unaffected)
     * @param rigidBody the rigid body to link (not null)
     */
    TorsoLink(KinematicRagdollControl krc, Bone mainBone,
            Transform meshToModel, PhysicsRigidBody rigidBody) {
        assert krc != null;
        assert mainBone != null;
        assert meshToModel != null;
        assert rigidBody != null;

        this.krc = krc;
        this.bone = mainBone;
        this.meshToModel = meshToModel.clone();
        this.rigidBody = rigidBody;

        kinematicWeight = 1f;
        rigidBody.setKinematic(true);
        rigidBody.setUserObject(this);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin blending this link to a fully kinematic mode. TODO publicize
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the controlled
     * spatial when the blend completes or null for no change to local transform
     * (unaffected)
     */
    void blendToKinematicMode(KinematicSubmode submode,
            float blendInterval, Transform endModelTransform) {
        assert submode != null;
        assert blendInterval >= 0f : blendInterval;

        this.submode = submode;
        this.blendInterval = blendInterval;
        this.endModelTransform = endModelTransform;
        kinematicWeight = Float.MIN_VALUE; // non-zero to trigger blending
        rigidBody.setKinematic(true);
        /*
         * Save the starting transforms.
         */
        if (endModelTransform == null) {
            startModelTransform = null;
        } else {
            startModelTransform = krc.getSpatial().getLocalTransform().clone();
        }
        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            Transform lastTransform = prevBoneTransforms[mbIndex];
            startBoneTransforms[mbIndex].set(lastTransform);
        }

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
     * Access the main bone.
     *
     * @return the pre-existing instance (not null)
     */
    public Bone getBone() {
        assert bone != null;
        return bone;
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
     * Put this link into dynamic mode.
     *
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    public void setDynamic(Vector3f uniformAcceleration) {
        Validate.nonNull(uniformAcceleration, "uniform acceleration");

        kinematicWeight = 0f;
        rigidBody.setGravity(uniformAcceleration);
        rigidBody.setKinematic(false);
        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(true);
        }
    }

    /**
     * Analyze the specified skeleton. If a null skeleton is specified, delete
     * the existing analysis results.
     *
     * @param skeleton (may be null, alias created)
     */
    void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;

        if (skeleton == null) {
            assert managedBones != null;
            managedBones = null;
            assert prevBoneTransforms != null;
            prevBoneTransforms = null;
            assert startBoneTransforms != null;
            startBoneTransforms = null;

        } else {
            assert managedBones == null;
            managedBones = krc.listManagedBones(KinematicRagdollControl.torsoName);

            int numManagedBones = managedBones.length;
            assert prevBoneTransforms == null;
            prevBoneTransforms = new Transform[numManagedBones];
            assert startBoneTransforms == null;
            startBoneTransforms = new Transform[numManagedBones];
            for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
                prevBoneTransforms[mbIndex] = new Transform();
                startBoneTransforms[mbIndex] = new Transform();
            }
        }
    }

    /**
     * Update this link according to its mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    void update(float tpf) {
        assert tpf >= 0f : tpf;

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
        krc = cloner.clone(krc);
        rigidBody = cloner.clone(rigidBody);
        endModelTransform = cloner.clone(endModelTransform);
        meshToModel = cloner.clone(meshToModel);
        prevBoneTransforms = cloner.clone(prevBoneTransforms);
        startBoneTransforms = cloner.clone(startBoneTransforms);
        startModelTransform = cloner.clone(startModelTransform);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public TorsoLink jmeClone() {
        try {
            TorsoLink clone = (TorsoLink) super.clone();
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

        blendInterval = ic.readFloat("blendInterval", 1f);
        kinematicWeight = ic.readFloat("kinematicWeight", 1f);
        krc = (KinematicRagdollControl) ic.readSavable("krc", null);
        submode = ic.readEnum("submode", KinematicSubmode.class,
                KinematicSubmode.Animated);
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        endModelTransform = (Transform) ic.readSavable("endModelTransform",
                new Transform());
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        startModelTransform = (Transform) ic.readSavable("startModelTransform",
                new Transform());

        tmp = ic.readSavableArray("prevBoneTransforms", null);
        if (tmp == null) {
            prevBoneTransforms = null;
        } else {
            prevBoneTransforms = new Transform[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                prevBoneTransforms[i] = (Transform) tmp[i];
            }
        }

        tmp = ic.readSavableArray("startBoneTransforms", null);
        if (tmp == null) {
            startBoneTransforms = null;
        } else {
            startBoneTransforms = new Transform[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                startBoneTransforms[i] = (Transform) tmp[i];
            }
        }
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
        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
        oc.write(krc, "krc", null);
        oc.write(submode, "submode", KinematicSubmode.Animated);
        oc.write(rigidBody, "rigidBody", null);
        oc.write(skeleton, "skeleton", null);
        oc.write(endModelTransform, "endModelTransforms", new Transform());
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(startModelTransform, "startModelTransforms", new Transform());
        oc.write(prevBoneTransforms, "prevBoneTransforms", new Transform[0]);
        oc.write(startBoneTransforms, "startBoneTransforms", new Transform[0]);
    }
    // *************************************************************************
    // private methods

    /**
     * Update this torso in Dynamic mode, based on the transform of the rigid
     * body.
     */
    private void dynamicUpdate() {
        RigidBodyMotionState motionState = rigidBody.getMotionState();
        Transform torsoTransform = motionState.physicsTransform(null);
        Vector3f scale = torsoTransform.getScale();
        rigidBody.getPhysicsScale(scale);

        Transform worldToParent; // inverse world transform of the parent node
        Node parent = krc.getSpatial().getParent();
        if (parent == null) {
            worldToParent = new Transform();
        } else {
            Transform parentTransform = parent.getWorldTransform();
            worldToParent = parentTransform.invert();
        }

        Transform transform = meshToModel.clone();
        transform.combineWithParent(torsoTransform);
        transform.combineWithParent(worldToParent);
        krc.getSpatial().setLocalTransform(transform);

        for (Bone managedBone : managedBones) {
            managedBone.updateModelTransforms();
        }
    }

    /**
     * Update this linked bone in blended Kinematic mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void kinematicUpdate(float tpf) {
        assert tpf >= 0f : tpf;

        Transform transform = new Transform();

        if (endModelTransform != null && kinematicWeight < 1f) {
            Quaternion startQuat = startModelTransform.getRotation();
            Quaternion endQuat = endModelTransform.getRotation();
            if (startQuat.dot(endQuat) < 0f) {
                endQuat.multLocal(-1f);
            }
            MyMath.slerp(kinematicWeight, startModelTransform,
                    endModelTransform, transform);
            krc.getSpatial().setLocalTransform(transform);
        }

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

            if (managedBone == bone) {
                /*
                 * Update the rigid body.
                 */
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

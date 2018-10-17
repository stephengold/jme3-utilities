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
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
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
     * bones managed by the torso, in a pre-order, depth-first traversal of the
     * skeleton
     */
    private Bone[] managedBones = null;
    /**
     * duration of the most recent transition to kinematic mode (in seconds,
     * &ge;0)
     */
    private float blendInterval = 1f;
    /**
     * weighting of kinematic movement (&ge;0, &le;1, 0=purely dynamic, 1=purely
     * kinematic, default=1, progresses from 0 to 1 during the blend interval)
     */
    private float kinematicWeight = 1f;
    /**
     * back pointer to the control that manages this torso
     */
    private KinematicRagdollControl krc;
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
     * recent transition to kinematic mode, or null for no spatial blending
     */
    private Transform endModelTransform = null;
    /**
     * transform from mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;
    /**
     * local transform of the controlled spatial at the start of the torso's
     * most recent transition to kinematic mode, or null for no spatial blending
     */
    private Transform startModelTransform = null;
    /**
     * local transform of each managed bone at the start of the most recent
     * transition to kinematic mode
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
    public TorsoLink(KinematicRagdollControl krc, Bone mainBone,
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
        rigidBody.setUserObject(krc);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin transitioning this link to fully kinematic mode.
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the controlled
     * spatial when the transition completes or null for no change to local
     * transform (unaffected)
     */
    public void blendToKinematicMode(KinematicSubmode submode,
            float blendInterval, Transform endModelTransform) {
        assert blendInterval >= 0f : blendInterval;
        assert submode == KinematicSubmode.Animated; // TODO

        this.blendInterval = blendInterval;
        kinematicWeight = Float.MIN_VALUE; // non-zero to trigger blending
        rigidBody.setKinematic(true);
        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(false);
        }

        this.endModelTransform = endModelTransform;
        if (endModelTransform == null) {
            startModelTransform = null;
        } else {
            startModelTransform = krc.getSpatial().getLocalTransform().clone();
        }
    }

    /**
     * Access the main bone of this torso.
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
        kinematicWeight = 0f;
        rigidBody.setGravity(uniformAcceleration);
        rigidBody.setKinematic(false);
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
            assert startBoneTransforms != null;
            startBoneTransforms = null;

        } else {
            assert managedBones == null;
            managedBones = krc.listManagedBones(
                    KinematicRagdollControl.torsoFakeBoneName);

            assert startBoneTransforms == null;
            int numManagedBones = managedBones.length;
            startBoneTransforms = new Transform[numManagedBones];
            for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
                startBoneTransforms[mbIndex] = new Transform();
            }
        }
    }

    /**
     * Update this torso according to its mode.
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

        if (kinematicWeight == 0f || kinematicWeight == 1f) {
            /*
             * Not already blending, so copy the latest bone transforms in
             * case blending starts before the next update.
             */
            for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
                Transform startTransform = startBoneTransforms[mbIndex];
                Bone managedBone = managedBones[mbIndex];
                MySkeleton.copyLocalTransform(managedBone, startTransform);
            }
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
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        endModelTransform = (Transform) ic.readSavable("endModelTransform",
                new Transform());
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        startModelTransform = (Transform) ic.readSavable("startModelTransform",
                new Transform());

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
        oc.write(rigidBody, "rigidBody", null);
        oc.write(skeleton, "skeleton", null);
        oc.write(endModelTransform, "endModelTransforms", new Transform());
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(startModelTransform, "startModelTransforms", new Transform());
        oc.write(startBoneTransforms, "startBoneTransforms", new Transform[0]);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the torso in Dynamic mode, based on the transform of the rigid
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
     * Update this linked bone in blended Kinematic mode, based on the
     * transforms of the transformSpatial and the skeleton, blended with the
     * saved transforms.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void kinematicUpdate(float tpf) {
        assert tpf >= 0f : tpf;

        Transform transform = new Transform();

        if (endModelTransform != null && kinematicWeight < 1f) {
            MyMath.slerp(kinematicWeight, startModelTransform,
                    endModelTransform, transform);
            krc.getSpatial().setLocalTransform(transform);
        }

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
                MyMath.slerp(kinematicWeight, startBoneTransforms[mbIndex],
                        transform, transform);
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

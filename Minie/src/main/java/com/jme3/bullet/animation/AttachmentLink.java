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
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Link an attachments node to a jointed rigid body in a ragdoll. TODO
 * superclass shared by all links
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class AttachmentLink
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(AttachmentLink.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix3f#IDENTITY}
     */
    final private static Matrix3f matrixIdentity = new Matrix3f();
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    // *************************************************************************
    // fields

    /**
     * bone associated with the attachments node (not null)
     */
    private Bone bone;
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
     * linked rigid body in the ragdoll (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * joint between the rigid body and the parent's rigid body, or null if not
     * yet created
     */
    private SixDofJoint joint = null;
    /**
     * attached model (not null)
     */
    private Spatial attachedModel;
    /**
     * name of the managing bone/torso link (not null)
     */
    private String managerName;
    /**
     * local transform for the attached model at the end of this link's most
     * recent blend interval, or null for no spatial blending
     */
    private Transform endModelTransform = null;
    /**
     * local transform of the attached model at the start of this link's most
     * recent blend interval
     */
    private Transform startModelTransform = new Transform();
    /**
     * location of the rigid body's center (in the attached model's local
     * coordinates)
     */
    private Vector3f localOffset;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public AttachmentLink() {
    }

    /**
     * Instantiate a purely kinematic link between the specified model and the
     * specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param boneName the name of the bone associated with the attachment node
     * (not null, not empty)
     * @param managerName the name of the bone/torso link that manages the
     * associated bone (not null)
     * @param attachModel the attached model to link (not null, alias created)
     * @param rigidBody the rigid body to link (not null, alias created)
     * @param localOffset the location of the body's center (in the attached
     * model's local coordinates, not null, unaffected)
     */
    AttachmentLink(DynamicAnimControl control, String boneName,
            String managerName, Spatial attachModel,
            PhysicsRigidBody rigidBody, Vector3f localOffset) {
        assert control != null;
        assert boneName != null;
        assert !boneName.isEmpty();
        assert managerName != null;
        assert attachModel != null;
        assert rigidBody != null;
        assert localOffset != null;

        this.control = control;
        bone = control.getSkeleton().getBone(boneName);
        this.managerName = managerName;
        this.attachedModel = attachModel;
        this.rigidBody = rigidBody;
        this.localOffset = localOffset.clone();

        kinematicWeight = 1f;
        rigidBody.setKinematic(true);
        rigidBody.setUserObject(this);

        BoneLink manager = control.getBoneLink(managerName);
        PhysicsRigidBody managerBody = manager.getRigidBody();
        Transform managerToWorld = manager.physicsTransform(null);
        Transform worldToManager = managerToWorld.invert();

        Transform attachToWorld = physicsTransform(null);
        Transform attachToManager = attachToWorld.clone();
        attachToManager.combineWithParent(worldToManager);

        Vector3f pivotMesh = bone.getModelSpacePosition();
        Spatial transformer = control.getTransformer();
        Vector3f pivotWorld = transformer.localToWorld(pivotMesh, null);
        managerToWorld.setScale(1f);
        Vector3f pivotManager
                = managerToWorld.transformInverseVector(pivotWorld, null);
        attachToWorld.setScale(1f);
        Vector3f pivot = attachToWorld.transformInverseVector(pivotWorld, null);

        Matrix3f rotManager = attachToManager.getRotation().toRotationMatrix();
        Matrix3f rot = matrixIdentity;

        joint = new SixDofJoint(managerBody, rigidBody, pivotManager, pivot,
                rotManager, rot, true);

        JointPreset rangeOfMotion = new JointPreset();
        rangeOfMotion.setupJoint(joint, false, false, false);

        joint.setCollisionBetweenLinkedBodies(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin blending this link to a fully kinematic mode.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endLocalTransform the desired local transform for the attached
     * model the blend completes or null for no change to local transform
     * (unaffected)
     */
    void blendToKinematicMode(float blendInterval,
            Transform endLocalTransform) {
        assert blendInterval >= 0f : blendInterval;

        this.blendInterval = blendInterval;
        this.endModelTransform = endLocalTransform;
        kinematicWeight = Float.MIN_VALUE; // non-zero to trigger blending
        rigidBody.setKinematic(true);
        /*
         * Save initial transform for blending.
         */
        if (endLocalTransform != null) {
            Transform current = attachedModel.getLocalTransform();
            startModelTransform.set(current);
        }
    }

    /**
     * Immediately freeze this link.
     */
    void freeze() {
        if (kinematicWeight > 0f) {
            blendToKinematicMode(0f, null);
        } else {
            setDynamic(new Vector3f(0f, 0f, 0f));
        }
    }

    /**
     * Access the attached model (not the attachment node).
     *
     * @return the pre-existing instance (not null)
     */
    public Spatial getAttachedModel() {
        assert attachedModel != null;
        return attachedModel;
    }

    /**
     * Access the bone associated with the attachments node.
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
     * @return the offset (in the attached model's local coordinates, either
     * storeResult or a new vector, not null)
     */
    Vector3f localOffset(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(localOffset);
        return result;
    }

    /**
     * Read the name of the managing bone/torso link.
     *
     * @return the link name (not null)
     */
    public String managerName() {
        assert managerName != null;
        return managerName;
    }

    /**
     * Calculate a physics transform for the rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated transform (in physics coordinates, either
     * storeResult or a new transform, not null)
     */
    final public Transform physicsTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        /*
         * Start with the rigid body's transform in the attached model's
         * local coordinates.
         */
        result.setTranslation(localOffset);
        result.setRotation(rotateIdentity);
        result.setScale(1f);
        /*
         * Convert to bone local coordinates.
         */
        Transform tmp = attachedModel.getLocalTransform();
        result.combineWithParent(tmp);
        /*
         * Convert to mesh coordinates.
         */
        tmp = MySkeleton.copyMeshTransform(bone, null);
        result.combineWithParent(tmp);
        /*
         * Convert to physics/world coordinates.
         */
        control.meshTransform(tmp);
        result.combineWithParent(tmp);

        return result;
    }

    /**
     * Copy animation data from the specified link, which must have the same
     * name.
     *
     * @param oldLink the link to copy from (not null, unaffected)
     */
    void postRebuild(AttachmentLink oldLink) {
        assert oldLink.bone.getName().equals(bone.getName());

        blendInterval = oldLink.blendInterval;
        kinematicWeight = oldLink.kinematicWeight;
        endModelTransform
                = (Transform) Misc.deepCopy(oldLink.endModelTransform);
        startModelTransform.set(oldLink.startModelTransform);

        rigidBody.setKinematic(oldLink.rigidBody.isKinematic());
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
     */
    public void setDynamic(Vector3f uniformAcceleration) {
        Validate.nonNull(uniformAcceleration, "uniform acceleration");

        kinematicWeight = 0f;
        rigidBody.setGravity(uniformAcceleration);
        rigidBody.setKinematic(false);
    }

    /**
     * Internal callback, invoked once per frame during the logical-state
     * update, provided the control is added to a scene.
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
        control = cloner.clone(control);
        rigidBody = cloner.clone(rigidBody);
        joint = cloner.clone(joint);
        localOffset = cloner.clone(localOffset);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public AttachmentLink jmeClone() {
        try {
            AttachmentLink clone = (AttachmentLink) super.clone();
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
        control = (DynamicAnimControl) ic.readSavable("control", null);
        blendInterval = ic.readFloat("blendInterval", 1f);
        kinematicWeight = ic.readFloat("kinematicWeight", 1f);
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        joint = (SixDofJoint) ic.readSavable("joint", null);
        attachedModel = (Spatial) ic.readSavable("attachedModel", null);
        managerName = ic.readString("managerName", null);
        endModelTransform = (Transform) ic.readSavable("endModelTransform",
                new Transform());
        startModelTransform = (Transform) ic.readSavable("startModelTransform",
                new Transform());
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
        oc.write(control, "control", null);
        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
        oc.write(rigidBody, "rigidBody", null);
        oc.write(joint, "joint", null);
        oc.write(attachedModel, "attachedModel", null);
        oc.write(managerName, "managerName", "");
        oc.write(endModelTransform, "endModelTransform", null);
        oc.write(startModelTransform, "startModelTransform", null);
        oc.write(localOffset, "offset", new Vector3f());
    }
    // *************************************************************************
    // private methods

    /**
     * Update this link in Dynamic mode, setting the local transform of the
     * attached model based on the transform of the linked rigid body.
     */
    private void dynamicUpdate() {
        assert !rigidBody.isKinematic();

        Transform transform = localModelTransform(null);
        attachedModel.setLocalTransform(transform);
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

        if (endModelTransform != null && kinematicWeight < 1f) {
            /*
             * For a smooth transition, blend the saved model transform
             * (from the start of the blend interval) into the goal transform.
             */
            Quaternion startQuat = startModelTransform.getRotation();
            Quaternion endQuat = endModelTransform.getRotation();
            if (startQuat.dot(endQuat) < 0f) {
                endQuat.multLocal(-1f);
            }
            MyMath.slerp(kinematicWeight, startModelTransform,
                    endModelTransform, transform);
            attachedModel.setLocalTransform(transform);
        }
        // The rigid-body transform gets updated by prePhysicsTick().
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
     * Calculate the local transform for the attached model to match the physics
     * transform of the rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated model transform (in local coordinates, either
     * storeResult or a new transform, not null)
     */
    private Transform localModelTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();
        /*
         * Start with the rigid body's transform in physics/world coordinates.
         */
        rigidBody.getPhysicsTransform(result);
        /*
         * Convert to mesh coordinates.
         */
        Transform worldToMesh = control.meshTransform(null).invert();
        result.combineWithParent(worldToMesh);
        /**
         * Convert to bone local coordinates.
         */
        Transform meshToBone
                = MySkeleton.copyMeshTransform(bone, null).invert();
        result.combineWithParent(meshToBone);
        /*
         * Subtract the body's local offset, rotated and scaled.
         */
        Vector3f modelOffset = localOffset.clone();
        modelOffset.multLocal(scale);
        orientation.mult(modelOffset, modelOffset);
        location.subtractLocal(modelOffset);

        return result;
    }
}

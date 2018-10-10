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
package com.jme3.bullet.control.ragdoll;

import com.jme3.animation.Bone;
import com.jme3.bullet.joints.SixDofJoint;
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
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Link an animated bone to a jointed rigid body.
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class PhysicsBoneLink
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsBoneLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * linked bone (not null)
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
     * linked rigid body (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * orientation of the bone (in mesh coordinates) at the start of the most
     * recent transition to kinematic mode
     */
    private Quaternion blendOrientation = new Quaternion();
    /**
     * orientation of the bone (in mesh coordinates) when this link was created
     */
    private Quaternion originalOrientation = new Quaternion();
    /**
     * joint between the bone's body and its parent's body, or null if not yet
     * created
     */
    private SixDofJoint joint = null;
    /**
     * spatial for converting between mesh/world coordinates (not null)
     */
    private Spatial transformSpatial;
    /**
     * name of parent in the hierarchy of linked bones, or torsoBoneName if
     * parented by the torso
     */
    private String parentName;
    /**
     * location of the bone (in mesh coordinates) at the start of the most
     * recent transition to kinematic mode
     */
    private Vector3f blendLocation = new Vector3f();
    /**
     * scale of the bone (in mesh coordinates) at the start of the most recent
     * transition to kinematic mode
     */
    private Vector3f blendScale = new Vector3f();
    /**
     * scale of the bone (in mesh coordinates) when this link was created
     */
    private Vector3f originalScale = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a link between the specified bone and rigid body.
     *
     * @param transformer the spatial to translate between mesh coordinates and
     * world coordinates (not null)
     * @param bone the bone to link (not null)
     * @param rigidBody the rigid body to link (not null)
     * @param parentName the name of the bone's parent in the linked-bone
     * hierarchy (not null)
     */
    public PhysicsBoneLink(Spatial transformer, Bone bone,
            PhysicsRigidBody rigidBody, String parentName) {
        Validate.nonNull(transformer, "transformer");
        Validate.nonNull(bone, "bone");
        Validate.nonNull(rigidBody, "rigid body");
        Validate.nonNull(parentName, "parent name");

        transformSpatial = transformer;
        this.bone = bone;
        this.rigidBody = rigidBody;
        this.parentName = parentName;

        Quaternion msr = bone.getModelSpaceRotation();
        originalOrientation.set(msr);

        Vector3f mss = bone.getModelSpaceScale();
        originalScale.set(mss);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the bone's location and orientation (in mesh coordinates) from the
     * start of the most recent transition to kinematic mode.
     *
     * @param storeLocation storage for the orientation (modified if not null)
     * @param storeOrientation storage for the orientation (modified if not
     * null)
     */
    public void copyBlendStart(Vector3f storeLocation,
            Quaternion storeOrientation) {
        if (storeLocation != null) {
            storeLocation.set(blendLocation);
        }
        if (storeOrientation != null) {
            storeOrientation.set(blendOrientation);
        }
        // TODO blendScale
    }

    /**
     * Update this bone in Dynamic mode, based on the transforms of the rigid
     * body.
     *
     * @param boneSet the names of all linked bones (not null, unaffected)
     */
    public void dynamicUpdate(Collection<String> boneSet) {
        Validate.nonNull(boneSet, "bone set");

        Transform transform = new Transform();
        Vector3f location = transform.getTranslation();
        Quaternion orientation = transform.getRotation();
        Vector3f scale = transform.getScale();

        Transform meshToWorld = transformSpatial.getWorldTransform();
        Transform worldToMesh = meshToWorld.invert();
        RigidBodyMotionState state = rigidBody.getMotionState();

        // Compute the bone's location in mesh coordinates.
        Vector3f worldLocation = state.getWorldLocation();
        worldToMesh.transformVector(worldLocation, location);

        // Compute the bone's orientation in local coordinates.
        Quaternion worldOrientation = state.getWorldRotationQuat();
        orientation.set(worldOrientation);
        orientation.multLocal(originalOrientation);
        worldToMesh.getRotation().mult(orientation, orientation);
        orientation.normalizeLocal();

        // Compute the bone's scale in local coordinates.
        Vector3f worldScale = rigidBody.getPhysicsScale(null);
        scale.set(worldScale);
        scale.multLocal(originalScale);
        scale.multLocal(worldToMesh.getScale());

        // Update the transforms in the skeleton.
        setTransform(bone, transform, boneSet);
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
     * Update this bone in Kinematic mode, based on the transforms of the
     * transformSpatial and the skeleton, without blending.
     *
     * @param boneSet the names of all linked bones (not null, unaffected)
     */
    public void kinematicUpdate(Collection<String> boneSet) {
        Validate.nonNull(boneSet, "bone set");

        Transform transform = new Transform();
        Vector3f location = transform.getTranslation();
        Quaternion orientation = transform.getRotation();
        Vector3f scale = transform.getScale();

        Transform meshToWorld = transformSpatial.getWorldTransform();
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
        scale.multLocal(meshToWorld.getScale());

        // Update the transform of the physics body.
        rigidBody.setPhysicsLocation(location);
        rigidBody.setPhysicsRotation(orientation);
        rigidBody.setPhysicsScale(scale);
    }

    /**
     * Update this bone in blended Kinematic mode, based on the transforms of
     * the transformSpatial and the skeleton, blended with the saved transform.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     * @param boneSet the names of all linked bones (not null, unaffected)
     */
    public void kinematicUpdate(float tpf, Collection<String> boneSet) {
        Validate.nonNegative(tpf, "time per frame");
        Validate.nonNull(boneSet, "bone set");

        if (kinematicWeight < 1f) {
            /*
             * For a smooth transition, blend the saved bone transform
             * (from the start of the transition to kinematic mode)
             * into the bone transform applied by the AnimControl.
             */
            Transform transform = new Transform();
            Vector3f location = transform.getTranslation();
            Quaternion orientation = transform.getRotation();
            Vector3f scale = transform.getScale();

            Vector3f msp = bone.getModelSpacePosition();
            Quaternion msr = bone.getModelSpaceRotation();
            Vector3f mss = bone.getModelSpaceScale();

            MyVector3f.lerp(kinematicWeight, blendLocation, msp,
                    location);
            MyQuaternion.slerp(kinematicWeight, blendOrientation, msr,
                    orientation);
            MyVector3f.lerp(kinematicWeight, blendScale, mss,
                    scale);

            setTransform(bone, transform, boneSet);
        }
        /*
         * Update the rigid body.
         */
        kinematicUpdate(boneSet);
        /*
         * If blending, increase the kinematic weight.
         */
        if (kinematicWeight < 1f) {
            kinematicWeight += tpf / blendInterval;
            if (kinematicWeight > 1f) {
                kinematicWeight = 1f; // done blending
            }
        }
    }

    /**
     * Copy the orientation of the bone (in mesh coordinates) that was captured
     * when this link was created. TODO is this bind orientation?
     *
     * @param storeResult (modified if not null)
     * @return the orientation (either storeResult or a new instance)
     */
    public Quaternion originalOrientation(Quaternion storeResult) {
        if (storeResult == null) {
            return originalOrientation.clone();
        } else {
            return storeResult.set(originalOrientation);
        }
    }

    /**
     * Copy the scale of the bone (in mesh coordinates) that was captured when
     * this link was created. TODO is this bind scale?
     *
     * @param storeResult (modified if not null)
     * @return the orientation (either storeResult or a new instance)
     */
    public Vector3f originalScale(Vector3f storeResult) {
        if (storeResult == null) {
            return originalScale.clone();
        } else {
            return storeResult.set(originalScale);
        }
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
     * Assign a physics joint to this bone link.
     *
     * @param joint (not null)
     */
    public void setJoint(SixDofJoint joint) {
        Validate.nonNull(joint, "joint");
        this.joint = joint;
    }

    /**
     * Begin smoothly transitioning this bone to fully kinematic mode.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    public void startBlend(float blendInterval) {
        Validate.nonNegative(blendInterval, "blend time");

        RigidBodyMotionState state = rigidBody.getMotionState();
        Vector3f worldLoc = state.getWorldLocation();
        Quaternion worldOri = state.getWorldRotationQuat();
        Vector3f scale = rigidBody.getPhysicsScale(null);

        Vector3f loc = worldLoc.clone();
        Quaternion ori = worldOri.mult(originalOrientation);
        scale.multLocal(originalScale);

        if (!MySpatial.isIgnoringTransforms(transformSpatial)) {
            transformSpatial.worldToLocal(worldLoc, loc);
            Quaternion spatInvRot
                    = MySpatial.inverseOrientation(transformSpatial);
            spatInvRot.mult(ori, ori);
            Vector3f factors = transformSpatial.getWorldScale();
            scale.divideLocal(factors);
        }

        blendLocation.set(loc);
        blendOrientation.set(ori);
        blendScale.set(scale);

        this.blendInterval = blendInterval;
        kinematicWeight = Float.MIN_VALUE;
        rigidBody.setKinematic(true);
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
        rigidBody = cloner.clone(rigidBody);
        blendOrientation = cloner.clone(blendOrientation);
        originalOrientation = cloner.clone(originalOrientation);
        joint = cloner.clone(joint);
        transformSpatial = cloner.clone(transformSpatial);
        blendLocation = cloner.clone(blendLocation);
        blendScale = cloner.clone(blendScale);
        originalScale = cloner.clone(originalScale);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public PhysicsBoneLink jmeClone() {
        try {
            PhysicsBoneLink clone = (PhysicsBoneLink) super.clone();
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
        rigidBody = (PhysicsRigidBody) ic.readSavable("rigidBody", null);
        bone = (Bone) ic.readSavable("bone", null);
        transformSpatial = (Spatial) ic.readSavable("transformSpatial", null);
        joint = (SixDofJoint) ic.readSavable("joint", null);
        parentName = ic.readString("parentName", null);

        Quaternion readQuat = (Quaternion) ic.readSavable("initalWorldRotation",
                new Quaternion());
        originalOrientation.set(readQuat);
        Vector3f readVec = (Vector3f) ic.readSavable("originalScale",
                new Vector3f());
        originalScale.set(readVec);

        readQuat = (Quaternion) ic.readSavable(
                "startBlendingRot", new Quaternion());
        blendOrientation.set(readQuat);
        readVec = (Vector3f) ic.readSavable("startBlendingPos", new Vector3f());
        blendLocation.set(readVec);
        readVec = (Vector3f) ic.readSavable("startBlendingScale", new Vector3f());
        blendScale.set(readVec);

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
        oc.write(rigidBody, "rigidBody", null);
        oc.write(bone, "bone", null);
        oc.write(transformSpatial, "transformSpatial", null);
        oc.write(joint, "joint", null);
        oc.write(parentName, "parentName", null);

        oc.write(originalOrientation, "initalWorldRotation", null);
        oc.write(originalScale, "originalScale", null);

        oc.write(blendOrientation, "startBlendingRot", new Quaternion());
        oc.write(blendLocation, "startBlendingPos", new Vector3f());
        oc.write(blendScale, "startBlendingScale", new Vector3f());

        oc.write(blendInterval, "blendInterval", 1f);
        oc.write(kinematicWeight, "kinematicWeight", 1f);
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a the transform of a skeleton bone. Child bones not in the bone set
     * are also updated. Note: recursive!
     *
     * @param bone the skeleton bone to alter (not null)
     * @param localTransform the desired bone transform (in local coordinates,
     * not null, unaffected)
     * @param boneSet the names of all linked bones (not null, unaffected)
     */
    private static void setTransform(Bone bone, Transform localTransform,
            Collection<String> boneSet) {
        Validate.nonNull(boneSet, "bone set");

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
            if (!boneSet.contains(childBone.getName())) {
                Transform childLocalTransform
                        = childBone.getCombinedTransform(location, orientation);
                childLocalTransform.setScale(scale);
                setTransform(childBone, childLocalTransform, boneSet);
            }
        }

        if (!userControl) {
            // Give control back to the animation control.
            bone.setUserControl(false);
        }
    }
}

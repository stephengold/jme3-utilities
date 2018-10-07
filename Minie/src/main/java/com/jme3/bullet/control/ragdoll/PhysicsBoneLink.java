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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;

/**
 * Link an animated bone to a jointed rigid body.
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class PhysicsBoneLink implements Savable {
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
     * linked rigid body (not null)
     */
    private PhysicsRigidBody rigidBody;
    /**
     * orientation of the bone (in mesh space) at the start of the most recent
     * transition to kinematic mode
     */
    final private Quaternion blendOrientation = new Quaternion();
    /**
     * orientation of the bone (in mesh space) when this link was created
     */
    final private Quaternion originalOrientation = new Quaternion();
    /**
     * joint between the bone's body and its parent's body, or null if none
     */
    private SixDofJoint joint;
    /**
     * spatial for converting between mesh/world coordinates (not null)
     */
    private Spatial transformSpatial;
    /**
     * name of parent in the hierarchy of linked bones, or torsoBoneName if none
     */
    private String parentName;
    /**
     * location of the bone (in mesh space) at the start of the most recent
     * transition to kinematic mode
     */
    final private Vector3f blendLocation = new Vector3f();
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
        this.joint = null;
        this.parentName = parentName;

        Quaternion msr = bone.getModelSpaceRotation();
        originalOrientation.set(msr);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the bone's location and orientation (in mesh space) from the start
     * of the most recent transition to kinematic mode.
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
     * Copy the orientation of the bone (in mesh space) that was captured when
     * this link was created.
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
     * Begin a transition to kinematic mode.
     */
    public void startBlend() {
        Vector3f worldLoc = rigidBody.getMotionState().getWorldLocation();
        Quaternion worldOri = rigidBody.getMotionState().getWorldRotationQuat();
        Quaternion q2 = worldOri.clone().multLocal(originalOrientation);

        if (MySpatial.isIgnoringTransforms(transformSpatial)) {
            blendLocation.set(worldLoc);
            blendOrientation.set(q2);
        } else {
            transformSpatial.worldToLocal(worldLoc, blendLocation);
            Quaternion spatInvRot
                    = MySpatial.inverseOrientation(transformSpatial);
            spatInvRot.mult(q2, blendOrientation);
        }

        rigidBody.setKinematic(true);
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

        Quaternion readQuat = (Quaternion) ic.readSavable(
                "initalWorldRotation", new Quaternion());
        originalOrientation.set(readQuat);

        readQuat = (Quaternion) ic.readSavable(
                "startBlendingRot", new Quaternion());
        blendOrientation.set(readQuat);

        Vector3f readVec
                = (Vector3f) ic.readSavable("startBlendingPos", new Vector3f());
        blendLocation.set(readVec);
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
        oc.write(blendOrientation, "startBlendingRot", new Quaternion());
        oc.write(blendLocation, "startBlendingPos", new Vector3f());
    }
}

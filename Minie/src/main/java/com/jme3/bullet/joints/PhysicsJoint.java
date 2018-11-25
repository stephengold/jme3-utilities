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
package com.jme3.bullet.joints;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract base class for physics joints based on Bullet's
 * btTypedConstraint. A physics joint can be single-ended or double-ended:
 * <ul>
 * <li>A single-ended joint constrains the motion of a dynamic rigid body in
 * physics space.</li>
 * <li>A double-ended joint connects 2 rigid bodies together in the same physics
 * space. One of the bodies must be dynamic.</li>
 * </ul>
 * Subclasses include: ConeJoint, HingeJoint, Point2PointJoint, and SixDofJoint.
 *
 * @author normenhansen
 */
abstract public class PhysicsJoint
        implements JmeCloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsJoint.class.getName());
    // *************************************************************************
    // fields

    /**
     * Unique identifier of the btTypedConstraint. Constructors are responsible
     * for setting this to a non-zero value. After that, the id never changes.
     */
    protected long objectId = 0L;
    /**
     * 1st body specified in the constructor
     */
    protected PhysicsRigidBody nodeA;
    /**
     * 2nd body specified in the constructor, or null for a single-ended joint
     */
    protected PhysicsRigidBody nodeB;
    /**
     * copy of offset of this joint's connection point in body A (in scaled
     * local coordinates)
     */
    protected Vector3f pivotA;
    /**
     * copy of offset of this joint's connection point in body B (in scaled
     * local coordinates) or null for a single-ended joint
     */
    protected Vector3f pivotB;
    /**
     * true IFF bodies A and B are allowed to collide
     */
    private boolean collisionBetweenLinkedBodies = true;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsJoint() {
    }

    /**
     * Instantiate a single-ended PhysicsJoint. To be effective, the joint must
     * be added to the physics space of the body. Also, the body must be
     * dynamic.
     *
     * @param nodeA the dynamic rigid body to constrain (not null, alias
     * created)
     * @param pivotA the offset of the constraint point in node A (in scaled
     * local coordinates, not null, unaffected)
     */
    protected PhysicsJoint(PhysicsRigidBody nodeA, Vector3f pivotA) {
        this.nodeA = nodeA;
        this.nodeB = null;
        this.pivotA = pivotA.clone();
        this.pivotB = null;
        nodeA.addJoint(this);
    }

    /**
     * Instantiate a double-ended PhysicsJoint. To be effective, the joint must
     * be added to the physics space of the 2 bodies. Also, the bodies must be
     * dynamic and distinct.
     *
     * @param nodeA the 1st body to connect (not null, alias created)
     * @param nodeB the 2nd body to connect (not null, alias created)
     * @param pivotA the offset of the connection point in node A (in scaled
     * local coordinates, not null, unaffected)
     * @param pivotB the offset of the connection point in node B (in scaled
     * local coordinates, not null, unaffected)
     */
    protected PhysicsJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB) {
        if (nodeA == nodeB) {
            throw new IllegalArgumentException("The bodies must be distinct.");
        }

        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.pivotA = pivotA.clone();
        this.pivotB = pivotB.clone();
        nodeA.addJoint(this);
        nodeB.addJoint(this);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count how many ends this joint has.
     *
     * @return 1 if single-ended, 2 if double-ended
     */
    public int countEnds() {
        if (nodeB == null) {
            return 1;
        } else {
            return 2;
        }
    }

    /**
     * Remove this joint from the joint lists of both connected bodies.
     */
    public void destroy() {
        nodeA.removeJoint(this);
        if (nodeB != null) {
            nodeB.removeJoint(this);
        }
    }

    /**
     * Read the magnitude of the applied impulse.
     *
     * @return impulse magnitude (&ge;0)
     */
    public float getAppliedImpulse() {
        float result = getAppliedImpulse(objectId);
        assert result >= 0f : result;
        return result;
    }

    /**
     * Access the specified body.
     *
     * @param end which end of the joint to access (not null)
     * @return the pre-existing body (may be null)
     */
    public PhysicsRigidBody getBody(JointEnd end) {
        switch (end) {
            case A:
                return nodeA;
            case B:
                return nodeB;
            default:
                throw new IllegalArgumentException("end = " + end.toString());
        }
    }

    /**
     * Access the 1st body specified in the constructor.
     *
     * @return the pre-existing body (not null)
     */
    public PhysicsRigidBody getBodyA() {
        assert nodeA != null;
        return nodeA;
    }

    /**
     * Access the 2nd body specified in the constructor.
     *
     * @return the pre-existing body, or null if this joint is single-ended
     */
    public PhysicsRigidBody getBodyB() {
        return nodeB;
    }

    /**
     * Read the breaking impulse threshold.
     *
     * @return the value
     */
    public float getBreakingImpulseThreshold() {
        float result = getBreakingImpulseThreshold(objectId);
        return result;
    }

    /**
     * Read the id of the Bullet constraint.
     *
     * @return the unique identifier (not zero)
     */
    public long getObjectId() {
        assert objectId != 0L;
        return objectId;
    }

    /**
     * Copy the location of the specified connection point in the specified
     * body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @param end which end of the joint to access (not null)
     * @return a location vector (in scaled local coordinates, either
     * storeResult or a new instance)
     */
    public Vector3f getPivot(JointEnd end, Vector3f storeResult) {
        switch (end) {
            case A:
                return getPivotA(storeResult);
            case B:
                return getPivotB(storeResult);
            default:
                throw new IllegalArgumentException("end = " + end.toString());
        }
    }

    /**
     * Copy the location of the connection point in body A.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a location vector (in scaled local coordinates, either
     * storeResult or a new instance)
     */
    public Vector3f getPivotA(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.set(pivotA);
        return result;
    }

    /**
     * Copy the location of the connection point in body B.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a location vector (in scaled local coordinates, either
     * storeResult or a new instance)
     */
    public Vector3f getPivotB(Vector3f storeResult) {
        if (pivotB == null) {
            throw new IllegalStateException("Joint is single-ended.");
        }
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.set(pivotB);
        return result;
    }

    /**
     * Test whether collisions are allowed between the linked bodies.
     *
     * @return true if collision are allowed, otherwise false
     */
    public boolean isCollisionBetweenLinkedBodies() {
        return collisionBetweenLinkedBodies;
    }

    /**
     * Test whether this joint is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled() {
        boolean result = isEnabled(objectId);
        return result;
    }

    /**
     * Read the breaking impulse threshold.
     *
     * @param desiredThreshold the desired value
     */
    public void setBreakingImpulseThreshold(float desiredThreshold) {
        setBreakingImpulseThreshold(objectId, desiredThreshold);
    }

    /**
     * Enable or disable collisions between the linked bodies. Changes take
     * effect when the joint is added to a PhysicsSpace.
     *
     * @param enable true to allow collisions, false to prevent them
     */
    public void setCollisionBetweenLinkedBodies(boolean enable) {
        collisionBetweenLinkedBodies = enable;
    }

    /**
     * Enable or disable this joint.
     *
     * @param enable true to enable, false to disable
     */
    public void setEnabled(boolean enable) {
        setEnabled(objectId, enable);
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
        nodeA = cloner.clone(nodeA);
        nodeB = cloner.clone(nodeB);
        pivotA = cloner.clone(pivotA);
        pivotB = cloner.clone(pivotB);
        objectId = 0L; // subclass must create the btCollisionObject
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public PhysicsJoint jmeClone() {
        try {
            PhysicsJoint clone = (PhysicsJoint) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this joint, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);

        nodeA = (PhysicsRigidBody) capsule.readSavable("nodeA", null);
        nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB", null);
        pivotA = (Vector3f) capsule.readSavable("pivotA", new Vector3f());
        pivotB = (Vector3f) capsule.readSavable("pivotB", null);
        /*
         * Each subclass must create the btCollisionObject and
         * initialize the breaking impulse threshold and the enabled flag.
         */
    }

    /**
     * Serialize this joint, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(nodeA, "nodeA", null);
        capsule.write(nodeB, "nodeB", null);
        capsule.write(pivotA, "pivotA", null);
        capsule.write(pivotB, "pivotB", null);

        capsule.write(getBreakingImpulseThreshold(), "breakingImpulseThreshold",
                Float.MAX_VALUE);
        capsule.write(isEnabled(), "isEnabled", true);
    }
    // *************************************************************************
    // Object methods

    /**
     * Finalize this physics joint just before it is destroyed. Should be
     * invoked only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.log(Level.FINE, "Finalizing Joint {0}",
                Long.toHexString(objectId));
        finalizeNative(objectId);
    }
    // *************************************************************************
    // private methods

    native private void finalizeNative(long objectId);

    native private float getAppliedImpulse(long objectId);

    native private float getBreakingImpulseThreshold(long objectId);

    native private boolean isEnabled(long objectId);

    native private void setBreakingImpulseThreshold(long objectId,
            float desiredThreshold);

    native private void setEnabled(long objectId, boolean enable);
}

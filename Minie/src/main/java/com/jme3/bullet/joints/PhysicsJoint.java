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
 * btTypedConstraint, used to connect 2 dynamic rigid bodies in the same physics
 * space. TODO make this work for static bodies
 * <p>
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
     * one of the connected dynamic rigid bodies
     */
    protected PhysicsRigidBody nodeA;
    /**
     * the other connected dynamic rigid body
     */
    protected PhysicsRigidBody nodeB;
    /**
     * copy of local offset of this joint's connection point in body A
     */
    protected Vector3f pivotA;
    /**
     * copy of local offset of this joint's connection point in body B
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
     * Instantiate a PhysicsJoint. To be effective, the joint must be added to
     * the physics space of the two bodies. Also, the bodies must be dynamic and
     * distinct.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA local offset of the joint connection point in node A (not
     * null, unaffected)
     * @param pivotB local offset of the joint connection point in node B (not
     * null, unaffected)
     */
    protected PhysicsJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB) {
        assert nodeA != nodeB;

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
     * Remove this joint from the joint lists of both connected bodies.
     */
    public void destroy() {
        getBodyA().removeJoint(this);
        getBodyB().removeJoint(this);
    }

    /**
     * Read the magnitude of the applied impulse.
     *
     * @return impulse
     */
    public float getAppliedImpulse() {
        return getAppliedImpulse(objectId);
    }

    /**
     * Access the 1st body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyA() {
        return nodeA;
    }

    /**
     * Access the 2nd body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyB() {
        return nodeB;
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
     * Access the local offset of the joint connection point in node A.
     *
     * @return the pre-existing vector (not null) TODO
     */
    public Vector3f getPivotA() {
        return pivotA;
    }

    /**
     * Access the local offset of the joint connection point in node B.
     *
     * @return the pre-existing vector (not null) TODO
     */
    public Vector3f getPivotB() {
        return pivotB;
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
     * Enable or disable collisions between the linked bodies. Changes take
     * effect when the joint is added to a PhysicsSpace.
     *
     * @param enable true &rarr; allow collisions, false &rarr; prevent them
     */
    public void setCollisionBetweenLinkedBodies(boolean enable) {
        collisionBetweenLinkedBodies = enable;
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
        nodeA = (PhysicsRigidBody) capsule.readSavable("nodeA",
                new PhysicsRigidBody());
        nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB",
                new PhysicsRigidBody());
        pivotA = (Vector3f) capsule.readSavable("pivotA", new Vector3f());
        pivotB = (Vector3f) capsule.readSavable("pivotB", new Vector3f());
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
}

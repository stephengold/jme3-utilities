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
package com.jme3.bullet.objects;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A collision object for a rigid body, based on Bullet's btRigidBody.
 *
 * @author normenhansen
 */
public class PhysicsRigidBody extends PhysicsCollisionObject {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsRigidBody.class.getName());
    /**
     * magic mass value used to specify a static body
     */
    final public static float massForStatic = 0f;

    /**
     * motion state
     */
    final protected RigidBodyMotionState motionState
            = new RigidBodyMotionState();
    /**
     * copy of mass (&gt;0) of a dynamic body, or 0 for a static body
     * (default=1)
     */
    protected float mass = 1f;
    /**
     * copy of kinematic flag: true&rarr;set kinematic mode (spatial controls
     * body), false&rarr;dynamic/static mode (body controls spatial)
     * (default=false)
     */
    private boolean kinematic = false;
    /**
     * joint list
     */
    protected ArrayList<PhysicsJoint> joints = new ArrayList<>(2);

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsRigidBody() {
    }

    /**
     * Instantiate a dynamic body with mass=1 and the specified collision shape.
     *
     * @param shape the desired shape (not null, alias created)
     */
    public PhysicsRigidBody(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        collisionShape = shape;
        rebuildRigidBody();
    }

    /**
     * Instantiate a body with the specified collision shape and mass.
     *
     * @param shape the desired shape (not null, alias created)
     * @param mass if 0, a static body is created; otherwise a dynamic body is
     * created (&ge;0)
     */
    public PhysicsRigidBody(CollisionShape shape, float mass) {
        Validate.nonNull(shape, "shape");
        Validate.nonNegative(mass, "mass");

        collisionShape = shape;
        this.mass = mass;
        rebuildRigidBody();
    }

    /**
     * Build/rebuild this body after parameters have changed.
     */
    protected void rebuildRigidBody() {
        boolean removed = false;
        if (mass != massForStatic) {
            validateDynamicShape(collisionShape);
        }

        if (objectId != 0L) {
            if (isInWorld(objectId)) {
                PhysicsSpace.getPhysicsSpace().remove(this);
                removed = true;
            }
            logger.log(Level.FINE, "Clearing RigidBody {0}",
                    Long.toHexString(objectId));
            finalizeNative(objectId);
        }
        preRebuild();
        objectId = createRigidBody(mass, motionState.getObjectId(),
                collisionShape.getObjectId());
        logger.log(Level.FINE, "Created RigidBody {0}",
                Long.toHexString(objectId));
        postRebuild();
        if (removed) {
            PhysicsSpace.getPhysicsSpace().add(this);
        }
    }

    /**
     * For use by subclasses.
     */
    protected void preRebuild() {
    }

    private native long createRigidBody(float mass, long motionStateId,
            long collisionShapeId);

    /**
     * For use by subclasses.
     */
    protected void postRebuild() {
        if (mass == massForStatic) {
            setStatic(objectId, true);
        } else {
            setStatic(objectId, false);
        }
        initUserPointer();
    }

    /**
     * Access this body's motion state.
     *
     * @return the pre-existing instance
     */
    public RigidBodyMotionState getMotionState() {
        return motionState;
    }

    /**
     * Test whether this body is in a physics space.
     *
     * @return true&rarr;in a space, false&rarr;not in a space
     */
    public boolean isInWorld() {
        return isInWorld(objectId);
    }

    private native boolean isInWorld(long objectId);

    /**
     * Directly alter the location of this body's center of mass. TODO check for
     * HeightfieldCollisionShape
     *
     * @param location the desired location (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f location) {
        setPhysicsLocation(objectId, location);
    }

    private native void setPhysicsLocation(long objectId, Vector3f location);

    /**
     * Directly alter this body's orientation. TODO check for
     * HeightfieldCollisionShape
     *
     * @param rotation the desired orientation (rotation matrix, not null,
     * unaffected)
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Matrix3f rotation);

    /**
     * Directly alter this body's orientation. TODO check for
     * HeightfieldCollisionShape
     *
     * @param rotation the desired orientation (quaternion, in physics-space
     * coordinates, not null, unaffected)
     */
    public void setPhysicsRotation(Quaternion rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Quaternion rotation);

    /**
     * Copy the location of this body's center of mass.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the location (in physics-space coordinates, either storeResult or
     * a new vector, not null)
     */
    public Vector3f getPhysicsLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getPhysicsLocation(objectId, storeResult);

        return storeResult;
    }

    private native void getPhysicsLocation(long objectId, Vector3f storeResult);

    /**
     * Copy this body's orientation to a quaternion.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the orientation (in physics-space coordinates, either storeResult
     * or a new quaternion, not null)
     */
    public Quaternion getPhysicsRotation(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }
        getPhysicsRotation(objectId, storeResult);

        return storeResult;
    }

    /**
     * Alter the principal components of the local inertia tensor. TODO provide
     * access to the whole tensor
     *
     * @param inverseInertia (not null, unaffected)
     */
    public void setInverseInertiaLocal(Vector3f inverseInertia) {
        Validate.nonNull(inverseInertia, "inverse inertia");
        setInverseInertiaLocal(objectId, inverseInertia);
    }

    private native void setInverseInertiaLocal(long objectId,
            Vector3f inverseInertialLocal);

    /**
     * Copy the principal components of the local inverse inertia tensor. TODO
     * provide access to the whole tensor
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a vector (either storeResult or a new vector, not null)
     */
    public Vector3f getInverseInertiaLocal(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getInverseInertiaLocal(objectId, storeResult);

        return storeResult;
    }

    private native void getInverseInertiaLocal(long objectId,
            Vector3f storeResult);

    private native void getPhysicsRotation(long objectId,
            Quaternion storeResult);

    /**
     * Copy this body's orientation to a matrix.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the orientation (in physics-space coordinates, either storeResult
     * or a new matrix, not null)
     */
    public Matrix3f getPhysicsRotationMatrix(Matrix3f storeResult) {
        if (storeResult == null) {
            storeResult = new Matrix3f();
        }
        getPhysicsRotationMatrix(objectId, storeResult);

        return storeResult;
    }

    private native void getPhysicsRotationMatrix(long objectId,
            Matrix3f storeResult);

    /**
     * Put this body into kinematic mode or take it out of kinematic mode.
     * <p>
     * In kinematic mode, the body is not influenced by physics but can affect
     * other physics objects. Its kinetic force is calculated based on its
     * movement and weight.
     *
     * @param kinematic true&rarr;set kinematic mode, false&rarr;set
     * dynamic/static mode (default=false)
     */
    public void setKinematic(boolean kinematic) {
        this.kinematic = kinematic;
        setKinematic(objectId, kinematic);
    }

    private native void setKinematic(long objectId, boolean kinematic);

    /**
     * Test whether this body is in kinematic mode.
     * <p>
     * In kinematic mode, the body is not influenced by physics but can affect
     * other physics objects. Its kinetic force is calculated based on its
     * movement and weight.
     *
     * @return true if in kinematic mode, otherwise false (dynamic/static mode)
     */
    public boolean isKinematic() {
        return kinematic;
    }

    /**
     * Alter the radius of the swept sphere used for continuous collision
     * detection (CCD).
     *
     * @param radius the desired radius (&ge;0, default=0)
     */
    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

    /**
     * Alter the amount of motion required to activate continuous collision
     * detection (CCD).
     * <p>
     * This addresses the issue of fast objects passing through other objects
     * with no collision detected.
     *
     * @param threshold the desired threshold velocity (&gt;0) or zero to
     * disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float threshold) {
        setCcdMotionThreshold(objectId, threshold);
    }

    private native void setCcdMotionThreshold(long objectId, float threshold);

    /**
     * Read the radius of the swept sphere used for continuous collision
     * detection (CCD).
     *
     * @return radius (&ge;0)
     */
    public float getCcdSweptSphereRadius() {
        return getCcdSweptSphereRadius(objectId);
    }

    private native float getCcdSweptSphereRadius(long objectId);

    /**
     * Calculate this body's continuous collision detection (CCD) motion
     * threshold.
     *
     * @return the threshold velocity (&ge;0)
     */
    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    /**
     * Calculate the square of this body's continuous collision detection (CCD)
     * motion threshold.
     *
     * @return the threshold velocity squared (&ge;0)
     */
    public float getCcdSquareMotionThreshold() {
        return getCcdSquareMotionThreshold(objectId);
    }

    private native float getCcdSquareMotionThreshold(long objectId);

    /**
     * Read this body's mass.
     *
     * @return the mass (&gt;0) or zero for a static body
     */
    public float getMass() {
        return mass;
    }

    /**
     * Alter this body's mass. Bodies with mass=0 are static. For dynamic
     * bodies, it is best to keep the mass around 1.
     *
     * @param mass the desired mass (&gt;0) or 0 for a static body (default=1)
     */
    public void setMass(float mass) {
        Validate.nonNegative(mass, "mass");
        if (mass != massForStatic) {
            validateDynamicShape(collisionShape);
        }

        this.mass = mass;
        if (objectId != 0L) { // TODO necessary?
            if (collisionShape != null) {
                updateMassProps(objectId, collisionShape.getObjectId(), mass);
            }
            if (mass == massForStatic) {
                setStatic(objectId, true);
            } else {
                setStatic(objectId, false);
            }
        }
    }

    private native void setStatic(long objectId, boolean state);

    private native long updateMassProps(long objectId, long collisionShapeId,
            float mass);

    /**
     * Copy this body's gravitational acceleration.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an acceleration vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f getGravity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getGravity(objectId, storeResult);

        return storeResult;
    }

    private native void getGravity(long objectId, Vector3f storeResult);

    /**
     * Alter this body's gravitational acceleration.
     * <p>
     * Invoke this after adding the body to a PhysicsSpace. Adding a body to a
     * PhysicsSpace alters its gravity.
     *
     * @param gravity the desired acceleration vector (not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        setGravity(objectId, gravity);
    }

    private native void setGravity(long objectId, Vector3f gravity);

    /**
     * Read this body's friction.
     *
     * @return friction value
     */
    public float getFriction() {
        return getFriction(objectId);
    }

    private native float getFriction(long objectId);

    /**
     * Alter this body's friction.
     *
     * @param friction the desired friction value (default=0.5)
     */
    public void setFriction(float friction) {
        setFriction(objectId, friction);
    }

    private native void setFriction(long objectId, float friction);

    /**
     * Alter this body's damping.
     *
     * @param linearDamping the desired linear damping value (default=0)
     * @param angularDamping the desired angular damping value (default=0)
     */
    public void setDamping(float linearDamping, float angularDamping) {
        setDamping(objectId, linearDamping, angularDamping);
    }

    private native void setDamping(long objectId, float linearDamping,
            float angularDamping);

    /**
     * Alter this body's linear damping.
     *
     * @param linearDamping the desired linear damping value (default=0)
     */
    public void setLinearDamping(float linearDamping) {
        setDamping(objectId, linearDamping, getAngularDamping());
    }

    /**
     * Alter this body's angular damping.
     *
     * @param angularDamping the desired angular damping value (default=0)
     */
    public void setAngularDamping(float angularDamping) {
        setAngularDamping(objectId, angularDamping);
    }

    private native void setAngularDamping(long objectId, float factor);

    /**
     * Read this body's linear damping.
     *
     * @return damping value
     */
    public float getLinearDamping() {
        return getLinearDamping(objectId);
    }

    private native float getLinearDamping(long objectId);

    /**
     * Read this body's angular damping.
     *
     * @return damping value
     */
    public float getAngularDamping() {
        return getAngularDamping(objectId);
    }

    private native float getAngularDamping(long objectId);

    /**
     * Read this body's restitution (bounciness).
     *
     * @return restitution value
     */
    public float getRestitution() {
        return getRestitution(objectId);
    }

    private native float getRestitution(long objectId);

    /**
     * Alter this body's restitution (bounciness). For best performance, set
     * restitution=0.
     *
     * @param restitution the desired value (default=0)
     */
    public void setRestitution(float restitution) {
        setRestitution(objectId, restitution);
    }

    private native void setRestitution(long objectId, float factor);

    private native void getAngularVelocity(long objectId, Vector3f storeResult);

    /**
     * Copy this body's angular velocity.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a velocity vector (in physics-space coordinates, either
     * storeResult or a new vector, not null))
     */
    public Vector3f getAngularVelocity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getAngularVelocity(objectId, storeResult);

        return storeResult;
    }

    /**
     * Alter this body's angular velocity.
     *
     * @param vec the desired angular velocity vector (not null, unaffected)
     */
    public void setAngularVelocity(Vector3f vec) {
        setAngularVelocity(objectId, vec);
        activate();
    }

    private native void setAngularVelocity(long objectId, Vector3f vec);

    private native void getLinearVelocity(long objectId, Vector3f storeResult);

    /**
     * Copy the linear velocity of this body's center of mass.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a velocity vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f getLinearVelocity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getLinearVelocity(objectId, storeResult);

        return storeResult;
    }

    /**
     * Alter the linear velocity of this body's center of mass.
     *
     * @param vec the desired velocity vector (not null)
     */
    public void setLinearVelocity(Vector3f vec) {
        setLinearVelocity(objectId, vec);
        activate();
    }

    private native void setLinearVelocity(long objectId, Vector3f vec);

    /**
     * Apply a force to the PhysicsRigidBody. Effective only if the next physics
     * update steps the physics space.
     * <p>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply
     * continuous force.
     *
     * @param force the force (not null, unaffected)
     * @param location the location of the force
     */
    public void applyForce(Vector3f force, Vector3f location) {
        applyForce(objectId, force, location);
        activate();
    }

    private native void applyForce(long objectId, Vector3f force,
            Vector3f location);

    /**
     * Apply a force to the PhysicsRigidBody. Effective only if the next physics
     * update steps the physics space.
     * <p>
     * To apply an impulse, use
     * {@link #applyImpulse(com.jme3.math.Vector3f, com.jme3.math.Vector3f)}.
     *
     * @param force the force (not null, unaffected)
     */
    public void applyCentralForce(Vector3f force) {
        applyCentralForce(objectId, force);
        activate();
    }

    private native void applyCentralForce(long objectId, Vector3f force);

    /**
     * Apply a force to the PhysicsRigidBody. Effective only if the next physics
     * update steps the physics space.
     * <p>
     * To apply an impulse, use
     * {@link #applyImpulse(com.jme3.math.Vector3f, com.jme3.math.Vector3f)}.
     *
     * @param torque the torque (not null, unaffected)
     */
    public void applyTorque(Vector3f torque) {
        applyTorque(objectId, torque);
        activate();
    }

    private native void applyTorque(long objectId, Vector3f vec);

    /**
     * Apply an impulse to the body the next physics update.
     *
     * @param impulse applied impulse (not null, unaffected)
     * @param rel_pos location relative to object (not null, unaffected)
     */
    public void applyImpulse(Vector3f impulse, Vector3f rel_pos) {
        applyImpulse(objectId, impulse, rel_pos);
        activate();
    }

    private native void applyImpulse(long objectId, Vector3f impulse,
            Vector3f rel_pos);

    /**
     * Apply a torque impulse to the body in the next physics update.
     *
     * @param vec the torque to apply (not null, unaffected)
     */
    public void applyTorqueImpulse(Vector3f vec) {
        applyTorqueImpulse(objectId, vec);
        activate();
    }

    private native void applyTorqueImpulse(long objectId, Vector3f vec);

    /**
     * Clear all forces acting on this body.
     */
    public void clearForces() {
        clearForces(objectId);
    }

    private native void clearForces(long objectId);

    /**
     * Apply the specified CollisionShape to this body.
     * <p>
     * Note that the body should not be in any physics space while changing
     * shape; the body gets rebuilt on the physics side.
     *
     * @param collisionShape the shape to apply (not null, alias created)
     */
    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        Validate.nonNull(collisionShape, "collision shape");
        if (mass != massForStatic) {
            validateDynamicShape(collisionShape);
        }

        super.setCollisionShape(collisionShape);

        if (objectId == 0L) {
            rebuildRigidBody();
        } else {
            setCollisionShape(objectId, collisionShape.getObjectId());
            updateMassProps(objectId, collisionShape.getObjectId(), mass);
        }
    }

    private native void setCollisionShape(long objectId, long collisionShapeId);

    /**
     * Reactivates this body if it has been deactivated due to lack of motion.
     */
    public void activate() {
        activate(objectId);
    }

    private native void activate(long objectId);

    /**
     * Test whether this body has been deactivated due to lack of motion.
     *
     * @return true if still active, false if deactivated
     */
    public boolean isActive() {
        return isActive(objectId);
    }

    private native boolean isActive(long objectId);

    /**
     * Alter this body's sleeping thresholds.
     * <p>
     * These thresholds determine when the body can be deactivated to save
     * resources. Low values keep the body active when it barely moves.
     *
     * @param linear the desired linear sleeping threshold (&ge;0, default=0.8)
     * @param angular the desired angular sleeping threshold (&ge;0, default=1)
     */
    public void setSleepingThresholds(float linear, float angular) {
        setSleepingThresholds(objectId, linear, angular);
    }

    private native void setSleepingThresholds(long objectId, float linear,
            float angular);

    /**
     * Alter this body's linear sleeping threshold.
     *
     * @param linearSleepingThreshold the desired threshold (&ge;0, default=0.8)
     */
    public void setLinearSleepingThreshold(float linearSleepingThreshold) {
        setLinearSleepingThreshold(objectId, linearSleepingThreshold);
    }

    private native void setLinearSleepingThreshold(long objectId,
            float linearSleepingThreshold);

    /**
     * Alter this body's angular sleeping threshold.
     *
     * @param angularSleepingThreshold the desired threshold (&ge;0, default=1)
     */
    public void setAngularSleepingThreshold(float angularSleepingThreshold) {
        setAngularSleepingThreshold(objectId, angularSleepingThreshold);
    }

    private native void setAngularSleepingThreshold(long objectId,
            float angularSleepingThreshold);

    /**
     * Read this body's linear sleeping threshold.
     *
     * @return the linear sleeping threshold (&ge;0)
     */
    public float getLinearSleepingThreshold() {
        return getLinearSleepingThreshold(objectId);
    }

    private native float getLinearSleepingThreshold(long objectId);

    /**
     * Read this body's angular sleeping threshold.
     *
     * @return the angular sleeping threshold (&ge;0)
     */
    public float getAngularSleepingThreshold() {
        return getAngularSleepingThreshold(objectId);
    }

    private native float getAngularSleepingThreshold(long objectId);

    /**
     * Read this body's angular factor for the X axis.
     *
     * @return the angular factor
     */
    public float getAngularFactor() {
        return getAngularFactor(null).getX();
    }

    /**
     * Copy this body's angular factors.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the angular factor for each axis (either storeResult or a new
     * vector, not null)
     */
    public Vector3f getAngularFactor(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getAngularFactor(objectId, storeResult);

        return storeResult;
    }

    private native void getAngularFactor(long objectId, Vector3f storeResult);

    /**
     * Alter this body's angular factor.
     *
     * @param factor the desired angular factor for all axes (not null,
     * unaffected, default=1)
     */
    public void setAngularFactor(float factor) {
        setAngularFactor(objectId, new Vector3f(factor, factor, factor));
    }

    /**
     * Alter this body's angular factors.
     *
     * @param factor the desired angular factor for each axis (not null,
     * unaffected, default=(1,1,1))
     */
    public void setAngularFactor(Vector3f factor) {
        setAngularFactor(objectId, factor);
    }

    private native void setAngularFactor(long objectId, Vector3f factor);

    /**
     * Copy this body's linear factors.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the linear factor for each axis (either storeResult or a new
     * vector, not null)
     */
    public Vector3f getLinearFactor(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getLinearFactor(objectId, storeResult);

        return storeResult;
    }

    private native void getLinearFactor(long objectId, Vector3f storeResult);

    /**
     * Alter this body's linear factors.
     *
     * @param factor the desired linear factor for each axis (not null,
     * unaffected, default=(1,1,1))
     */
    public void setLinearFactor(Vector3f factor) {
        setLinearFactor(objectId, factor);
    }

    private native void setLinearFactor(long objectId, Vector3f factor);

    /**
     * Do not invoke directly! Joints are added automatically when created.
     *
     * @param joint the joint to add (not null)
     */
    public void addJoint(PhysicsJoint joint) {
        if (!joints.contains(joint)) {
            joints.add(joint);
        }
    }

    /**
     * Do not invoke directly! Joints are removed automatically when destroyed.
     *
     * @param joint the joint to remove (not null)
     */
    public void removeJoint(PhysicsJoint joint) {
        joints.remove(joint);
    }

    /**
     * Access the list of joints connected with this body.
     * <p>
     * This list is only filled when the PhysicsRigidBody is added to a physics
     * space.
     *
     * @return the pre-existing list (not null)
     */
    public List<PhysicsJoint> getJoints() {
        return joints;
    }

    /**
     * Validate a shape suitable for a dynamic body.
     *
     * @param shape (not null, unaffected)
     */
    private void validateDynamicShape(CollisionShape shape) {
        assert shape != null;

        if (shape instanceof HeightfieldCollisionShape) {
            throw new IllegalStateException(
                    "Dynamic rigid body can't have heightfield shape!");
        } else if (shape instanceof MeshCollisionShape) {
            throw new IllegalStateException(
                    "Dynamic rigid body can't have mesh shape!");
        } else if (shape instanceof PlaneCollisionShape) {
            throw new IllegalStateException(
                    "Dynamic rigid body can't have plane shape!");
        }
    }

    /**
     * Serialize this body, for example when saving to a J3O file.
     *
     * @param e exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);

        capsule.write(getMass(), "mass", 1f);

        capsule.write(getGravity(null), "gravity", Vector3f.ZERO);
        capsule.write(getFriction(), "friction", 0.5f);
        capsule.write(getRestitution(), "restitution", 0f);
        Vector3f angularFactor = getAngularFactor(null);
        if (angularFactor.x == angularFactor.y
                && angularFactor.y == angularFactor.z) {
            capsule.write(getAngularFactor(), "angularFactor", 1f);
        } else {
            capsule.write(getAngularFactor(null), "angularFactor",
                    Vector3f.UNIT_XYZ);
            capsule.write(getLinearFactor(null), "linearFactor",
                    Vector3f.UNIT_XYZ);
        }
        capsule.write(kinematic, "kinematic", false);

        capsule.write(getLinearDamping(), "linearDamping", 0f);
        capsule.write(getAngularDamping(), "angularDamping", 0f);
        capsule.write(getLinearSleepingThreshold(), "linearSleepingThreshold",
                0.8f);
        capsule.write(getAngularSleepingThreshold(), "angularSleepingThreshold",
                1f);

        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0f);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0f);

        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation",
                new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()),
                "physicsRotation", new Matrix3f());

        capsule.writeSavableArrayList(joints, "joints", null);
    }

    /**
     * De-serialize this body, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);

        InputCapsule capsule = im.getCapsule(this);
        float mass = capsule.readFloat("mass", 1f);
        this.mass = mass;
        rebuildRigidBody();
        setGravity((Vector3f) capsule.readSavable("gravity",
                Vector3f.ZERO.clone()));
        setFriction(capsule.readFloat("friction", 0.5f));
        setKinematic(capsule.readBoolean("kinematic", false));

        setRestitution(capsule.readFloat("restitution", 0f));
        Vector3f angularFactor = (Vector3f) capsule.readSavable("angularFactor",
                null);
        if (angularFactor == null) {
            setAngularFactor(capsule.readFloat("angularFactor", 1f));
        } else {
            setAngularFactor(angularFactor);
            setLinearFactor((Vector3f) capsule.readSavable("linearFactor",
                    Vector3f.UNIT_XYZ.clone()));
        }
        setDamping(capsule.readFloat("linearDamping", 0f),
                capsule.readFloat("angularDamping", 0f));
        setSleepingThresholds(
                capsule.readFloat("linearSleepingThreshold", 0.8f),
                capsule.readFloat("angularSleepingThreshold", 1f));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0f));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0f));

        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation",
                new Vector3f()));
        setPhysicsRotation((Matrix3f) capsule.readSavable("physicsRotation",
                new Matrix3f()));

        joints = capsule.readSavableArrayList("joints", null);
    }
}

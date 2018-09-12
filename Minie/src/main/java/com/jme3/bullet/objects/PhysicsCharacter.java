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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collision object for simplified character simulation, based on Bullet's
 * btKinematicCharacterController.
 *
 * @author normenhansen
 */
public class PhysicsCharacter extends PhysicsCollisionObject {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsCharacter.class.getName());
    /**
     * Unique identifier of btKinematicCharacterController (as opposed to its
     * collision object, which is a ghost). Constructors are responsible for
     * setting this to a non-zero value. The id might change if the character
     * gets rebuilt.
     */
    private long characterId = 0L;

    private float stepHeight;
    final private Vector3f walkOffset = new Vector3f();
    private float fallSpeed = 55f;
    private float jumpSpeed = 10f;
    private int upAxis = PhysicsSpace.AXIS_Y; // TODO remove

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsCharacter() {
    }

    /**
     * Instantiate a character with the specified collision shape and step
     * height.
     *
     * @param shape the desired shape (not null, alias created)
     * @param stepHeight the quantization size for vertical movement
     */
    public PhysicsCharacter(CollisionShape shape, float stepHeight) {
        this.collisionShape = shape;
        this.stepHeight = stepHeight;
        buildObject();
    }

    /**
     * Create the configured character in Bullet.
     */
    private void buildObject() {
        if (objectId == 0L) {
            objectId = createGhostObject();
            logger.log(Level.FINE, "Creating GhostObject {0}",
                    Long.toHexString(objectId));
            initUserPointer();
        }
        setCharacterFlags(objectId);
        attachCollisionShape(objectId, collisionShape.getObjectId());
        if (characterId != 0L) {
            logger.log(Level.FINE, "Clearing Character {0}",
                    Long.toHexString(objectId));
            finalizeNativeCharacter(characterId);
        }
        characterId = createCharacterObject(objectId,
                collisionShape.getObjectId(), stepHeight);
        logger.log(Level.FINE, "Creating Character {0}",
                Long.toHexString(characterId));
    }

    private native long createGhostObject();

    private native void setCharacterFlags(long objectId);

    private native long createCharacterObject(long objectId, long shapeId,
            float stepHeight);

    /**
     * Directly alter the location of this character's center of mass.
     *
     * @param location the desired physics location (not null, unaffected)
     */
    public void warp(Vector3f location) {
        warp(characterId, location);
    }

    private native void warp(long characterId, Vector3f location);

    /**
     * Alter the walk offset. The offset will continue to be applied until
     * altered again.
     *
     * @param offset the desired position increment for each physics tick (not
     * null, unaffected)
     */
    public void setWalkDirection(Vector3f offset) {
        walkOffset.set(offset);
        setWalkDirection(characterId, offset);
    }

    private native void setWalkDirection(long characterId, Vector3f direction);

    /**
     * Access the walk offset.
     *
     * @return the pre-existing instance TODO
     */
    public Vector3f getWalkDirection() {
        return walkOffset;
    }

    /**
     * @deprecated Deprecated in bullet 2.86.1 use setUp(Vector3f) instead
     * @param axis which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    @Deprecated
    public void setUpAxis(int axis) {
        if (axis < 0) {
            axis = PhysicsSpace.AXIS_X;
        } else if (axis > 2) {
            axis = PhysicsSpace.AXIS_Z;
        }
        upAxis = axis;
        switch (axis) {
            case PhysicsSpace.AXIS_X:
                setUp(Vector3f.UNIT_X);
                break;
            case PhysicsSpace.AXIS_Y:
                setUp(Vector3f.UNIT_Y);
                break;
            case PhysicsSpace.AXIS_Z:
                setUp(Vector3f.UNIT_Z);
        }
    }

    /**
     * Alter this character's "up" direction.
     *
     * @param direction the desired direction (not null, not zero, unaffected)
     */
    public void setUp(Vector3f direction) {
        setUp(characterId, direction);
    }

    private native void setUp(long characterId, Vector3f direction);

    /**
     * Alter this character's angular velocity.
     *
     * @param v the desired angular velocity vector (not null, unaffected)
     */
    public void setAngularVelocity(Vector3f v) {
        setAngularVelocity(characterId, v);
    }

    private native void setAngularVelocity(long characterId, Vector3f v);

    /**
     * Copy this character's angular velocity.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the velocity vector (either the provided storage or a new vector,
     * not null)
     */
    public Vector3f getAngularVelocity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getAngularVelocity(characterId, storeResult);

        return storeResult;
    }

    private native void getAngularVelocity(long characterId,
            Vector3f storeResult);

    /**
     * Alter the linear velocity of this character's center of mass.
     *
     * @param v the desired velocity vector (not null)
     */
    public void setLinearVelocity(Vector3f v) {
        setLinearVelocity(characterId, v);
    }

    private native void setLinearVelocity(long characterId, Vector3f v);

    /**
     * Copy the linear velocity of this character's center of mass.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a vector (either the provided storage or a new vector, not null)
     */
    public Vector3f getLinearVelocity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getLinearVelocity(characterId, storeResult);

        return storeResult;
    }

    private native void getLinearVelocity(long characterId, Vector3f out);

    /**
     * Read the index of the "up" axis. TODO deprecate
     *
     * @return which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public int getUpAxis() {
        return upAxis;
    }

    /**
     * Alter this character's fall speed.
     *
     * @param fallSpeed the desired speed (default=55)
     */
    public void setFallSpeed(float fallSpeed) {
        this.fallSpeed = fallSpeed;
        setFallSpeed(characterId, fallSpeed);
    }

    private native void setFallSpeed(long characterId, float fallSpeed);

    /**
     * Read this character's fall speed.
     *
     * @return speed
     */
    public float getFallSpeed() {
        return fallSpeed;
    }

    /**
     * Alter this character's jump speed.
     *
     * @param jumpSpeed the desired speed (default=10)
     */
    public void setJumpSpeed(float jumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        setJumpSpeed(characterId, jumpSpeed);
    }

    private native void setJumpSpeed(long characterId, float jumpSpeed);

    /**
     * Read this character's jump speed.
     *
     * @return speed
     */
    public float getJumpSpeed() {
        return jumpSpeed;
    }

    /**
     * @deprecated Deprecated in bullet 2.86.1. Use setGravity(Vector3f)
     * instead.
     * @param value the desired upward component of the acceleration (typically
     * negative)
     */
    @Deprecated
    public void setGravity(float value) {
        setGravity(new Vector3f(0f, value, 0f)); // wrong sign?
    }

    /**
     * Alter this character's gravitational acceleration.
     *
     * @param gravity the desired acceleration vector (not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        setGravity(characterId, gravity);
    }

    private native void setGravity(long characterId, Vector3f gravity);

    /**
     * @deprecated Deprecated in bullet 2.86.1. Use getGravity(Vector3f)
     * instead.
     * @return the upward component of the acceleration (typically negative)
     */
    @Deprecated
    public float getGravity() {
        return getGravity(null).y; // wrong sign?
    }

    /**
     * Copy this character's gravitational acceleration.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the acceleration vector (either the provided storage or a new
     * vector, not null)
     */
    public Vector3f getGravity(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        getGravity(characterId, storeResult);

        return storeResult;
    }

    private native void getGravity(long characterId, Vector3f storeResult);

    /**
     * Read this character's linear damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getLinearDamping() {
        return getLinearDamping(characterId);
    }

    private native float getLinearDamping(long characterId);

    /**
     * Alter this character's linear damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped)
     */
    public void setLinearDamping(float damping) {
        setLinearDamping(characterId, damping);
    }

    private native void setLinearDamping(long characterId, float damping);

    /**
     * Read this character's angular damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getAngularDamping() {
        return getAngularDamping(characterId);
    }

    private native float getAngularDamping(long characterId);

    /**
     * Alter this character's angular damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0)
     */
    public void setAngularDamping(float damping) {
        setAngularDamping(characterId, damping);
    }

    private native void setAngularDamping(long characterId, float damping);

    /**
     * Read this character's step height.
     *
     * @return the height (in physics-space units)
     */
    public float getStepHeight() {
        return getStepHeight(characterId);
    }

    private native float getStepHeight(long characterId);

    /**
     * Alter this character's step height.
     *
     * @param height the desired height (in physics-space units)
     */
    public void setStepHeight(float height) {
        setStepHeight(characterId, height);
    }

    private native void setStepHeight(long characterId, float height);

    /**
     * Read this character's maximum penetration depth.
     *
     * @return the depth (in physics-space units)
     */
    public float getMaxPenetrationDepth() {
        return getMaxPenetrationDepth(characterId);
    }

    private native float getMaxPenetrationDepth(long characterId);

    /**
     * Alter this character's maximum penetration depth.
     *
     * @param depth the desired depth (in physics-space units)
     */
    public void setMaxPenetrationDepth(float depth) {
        setMaxPenetrationDepth(characterId, depth);
    }

    private native void setMaxPenetrationDepth(long characterId, float depth);

    /**
     * Alter this character's maximum slope angle.
     *
     * @param slopeRadians the desired angle (in radians)
     */
    public void setMaxSlope(float slopeRadians) {
        setMaxSlope(characterId, slopeRadians);
    }

    private native void setMaxSlope(long characterId, float slopeRadians);

    /**
     * Read this character's maximum slope angle.
     *
     * @return the angle (in radians)
     */
    public float getMaxSlope() {
        return getMaxSlope(characterId);
    }

    private native float getMaxSlope(long characterId);

    /**
     * Test whether this character is on the ground.
     *
     * @return true if on the ground, otherwise false
     */
    public boolean onGround() {
        return onGround(characterId);
    }

    private native boolean onGround(long characterId);

    /**
     * @deprecated Deprecated in bullet 2.86.1. Use jump(Vector3f) instead.
     */
    @Deprecated
    public void jump() {
        jump(Vector3f.UNIT_Y);
    }

    /**
     * Jump in the specified direction.
     *
     * @param dir desired jump direction (not null, unaffected)
     */
    public void jump(Vector3f dir) {
        jump(characterId, dir);
    }

    private native void jump(long characterId, Vector3f v);

    /**
     * Apply the specified CollisionShape to this character. Note that the
     * character should not be in any physics space while changing shape; the
     * character gets rebuilt on the physics side.
     *
     * @param collisionShape the shape to apply (not null, alias created)
     */
    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (objectId == 0L) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Directly alter this character's location. (Same as
     * {@link #warp(com.jme3.math.Vector3f)}).)
     *
     * @param location the desired location (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f location) {
        warp(location);
    }

    /**
     * Copy the location of this character's center of mass.
     *
     * @param trans storage for the result (modified if not null)
     * @return the location vector (either the provided storage or a new vector,
     * not null)
     */
    public Vector3f getPhysicsLocation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        getPhysicsLocation(objectId, trans);
        return trans;
    }

    private native void getPhysicsLocation(long objectId, Vector3f vec);

    /**
     * Copy the location of this character's center of mass.
     *
     * @return a new location vector (not null)
     */
    public Vector3f getPhysicsLocation() {
        return getPhysicsLocation(null);
    }

    /**
     * Alter this character's continuous collision detection (CCD) swept sphere
     * radius.
     *
     * @param radius (&ge;0, default=0)
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
     * Read the radius of the sphere used for continuous collision detection
     * (CCD).
     *
     * @return radius (&ge;0)
     */
    public float getCcdSweptSphereRadius() {
        return getCcdSweptSphereRadius(objectId);
    }

    private native float getCcdSweptSphereRadius(long objectId);

    /**
     * Calculate this character's continuous collision detection (CCD) motion
     * threshold.
     *
     * @return the threshold velocity (&ge;0)
     */
    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    /**
     * Calculate the square of this character's continuous collision detection
     * (CCD) motion threshold.
     *
     * @return the threshold velocity squared (&ge;0)
     */
    public float getCcdSquareMotionThreshold() {
        return getCcdSquareMotionThreshold(objectId);
    }

    private native float getCcdSquareMotionThreshold(long objectId);

    /**
     * used internally
     *
     * @return the Bullet id
     */
    public long getControllerId() {
        return characterId;
    }

    /**
     * Destroy this character and remove it from memory. (Has no effect.) TODO
     * remove
     */
    public void destroy() {
    }

    /**
     * Serialize this character, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(stepHeight, "stepHeight", 1f);
        capsule.write(getGravity(), "gravity", 9.8f * 3); // TODO vector
        capsule.write(getMaxSlope(), "maxSlope", 1f);
        capsule.write(fallSpeed, "fallSpeed", 55f);
        capsule.write(jumpSpeed, "jumpSpeed", 10f);
        capsule.write(upAxis, "upAxis", PhysicsSpace.AXIS_Y);  // TODO vector
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0f);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0f);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation",
                new Vector3f());
    }

    /**
     * De-serialize this character from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        stepHeight = capsule.readFloat("stepHeight", 1f);
        buildObject();
        setGravity(capsule.readFloat("gravity", 9.8f * 3)); // TODO vector
        setMaxSlope(capsule.readFloat("maxSlope", 1f));
        setFallSpeed(capsule.readFloat("fallSpeed", 55f));
        setJumpSpeed(capsule.readFloat("jumpSpeed", 10f));
        setUpAxis(capsule.readInt("upAxis", PhysicsSpace.AXIS_Y)); // TODO vector
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0f));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0f));
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation",
                new Vector3f()));
    }

    /**
     * Finalize this physics character just before it is destroyed. Should be
     * invoked only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finalizeNativeCharacter(characterId);
    }

    private native void finalizeNativeCharacter(long characterId);
}

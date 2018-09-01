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
 * A collision object for simplified character simulation based on Bullet's
 * btKinematicCharacterController.
 *
 * @author normenhansen
 */
public class PhysicsCharacter extends PhysicsCollisionObject {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsCharacter.class.getName());
    /**
     * Bullet identifier for this character (as opposed to its collision object,
     * which is a ghost). Constructors are responsible for setting this to a
     * non-zero value. The id might change if the character gets rebuilt.
     */
    private long characterId = 0;

    private float stepHeight;
    final private Vector3f walkDirection = new Vector3f();
    private float fallSpeed = 55.0f;
    private float jumpSpeed = 10.0f;
    final private int upAxis = 1;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsCharacter() {
    }

    /**
     * Create a character with the specified collision shape and step height.
     *
     * @param shape the desired shape (not null, alias created)
     * @param stepHeight the quantization size for vertical movement
     */
    public PhysicsCharacter(CollisionShape shape, float stepHeight) {
        this.collisionShape = shape;
        this.stepHeight = stepHeight;
        buildObject();
    }

    private void buildObject() {
        if (objectId == 0) {
            objectId = createGhostObject();
            logger.log(Level.FINE, "Creating GhostObject {0}", Long.toHexString(objectId));
            initUserPointer();
        }
        setCharacterFlags(objectId);
        attachCollisionShape(objectId, collisionShape.getObjectId());
        if (characterId != 0) {
            logger.log(Level.FINE, "Clearing Character {0}", Long.toHexString(objectId));
            finalizeNativeCharacter(characterId);
        }
        characterId = createCharacterObject(objectId, collisionShape.getObjectId(), stepHeight);
        logger.log(Level.FINE, "Creating Character {0}", Long.toHexString(characterId));
    }

    private native long createGhostObject();

    private native void setCharacterFlags(long objectId);

    private native long createCharacterObject(long objectId, long shapeId, float stepHeight);

    /**
     * Directly alter the location of this character.
     *
     * @param location the desired physics location (not null)
     */
    public void warp(Vector3f location) {
        warp(characterId, location);
    }

    private native void warp(long characterId, Vector3f location);

    /**
     * Set the walk direction, works continuously. This should probably be named
     * setPositionIncrementPerSimulatorStep. This is neither a direction nor a
     * velocity, but the amount to increment the position each physics tick. So
     * vector length = accuracy*speed in m/s
     *
     * @param vec the walk direction to set
     */
    public void setWalkDirection(Vector3f vec) {
        walkDirection.set(vec);
        setWalkDirection(characterId, vec);
    }

    private native void setWalkDirection(long characterId, Vector3f vec);

    /**
     * @return the currently set walkDirection
     */
    public Vector3f getWalkDirection() {
        return walkDirection;
    }

    /**
     * @deprecated Deprecated in bullet 2.86.1 use setUp(Vector3f) instead
     * @param axis which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    @Deprecated
    public void setUpAxis(int axis) {
        if (axis < 0) {
            axis = 0;
        } else if (axis > 2) {
            axis = 2;
        }
        switch (axis) {
            case 0:
                setUp(Vector3f.UNIT_X);
                break;
            case 1:
                setUp(Vector3f.UNIT_Y);
                break;
            case 2:
                setUp(Vector3f.UNIT_Z);
        }
    }

    public void setUp(Vector3f axis) {
        setUp(characterId, axis);
    }

    private native void setUp(long characterId, Vector3f axis);

    public void setAngularVelocity(Vector3f v) {
        setAngularVelocity(characterId, v);
    }

    private native void setAngularVelocity(long characterId, Vector3f v);

    public Vector3f getAngularVelocity(Vector3f out) {
        if (out == null) {
            out = new Vector3f();
        }
        getAngularVelocity(characterId, out);
        return out;
    }

    private native void getAngularVelocity(long characterId, Vector3f out);

    public void setLinearVelocity(Vector3f v) {
        setLinearVelocity(characterId, v);
    }

    private native void setLinearVelocity(long characterId, Vector3f v);

    public Vector3f getLinearVelocity(Vector3f out) {
        if (out == null) {
            out = new Vector3f();
        }
        getLinearVelocity(characterId, out);
        return out;
    }

    private native void getLinearVelocity(long characterId, Vector3f out);

    public int getUpAxis() {
        return upAxis;
    }

    public void setFallSpeed(float fallSpeed) {
        this.fallSpeed = fallSpeed;
        setFallSpeed(characterId, fallSpeed);
    }

    private native void setFallSpeed(long characterId, float fallSpeed);

    public float getFallSpeed() {
        return fallSpeed;
    }

    public void setJumpSpeed(float jumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        setJumpSpeed(characterId, jumpSpeed);
    }

    private native void setJumpSpeed(long characterId, float jumpSpeed);

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    /**
     * @deprecated Deprecated in bullet 2.86.1. Use setGravity(Vector3f)
     * instead.
     * @param value acceleration
     */
    @Deprecated
    public void setGravity(float value) {
        setGravity(new Vector3f(0, value, 0));
    }

    public void setGravity(Vector3f value) {
        setGravity(characterId, value);
    }

    private native void setGravity(long characterId, Vector3f gravity);

    /**
     * @deprecated Deprecated in bullet 2.86.1. Use getGravity(Vector3f)
     * instead.
     * @return acceleration
     */
    @Deprecated
    public float getGravity() {
        return getGravity(null).y;
    }

    public Vector3f getGravity(Vector3f out) {
        if (out == null) {
            out = new Vector3f();
        }
        getGravity(characterId, out);
        return out;
    }

    private native void getGravity(long characterId, Vector3f out);

    public float getLinearDamping() {
        return getLinearDamping(characterId);
    }

    private native float getLinearDamping(long characterId);

    public void setLinearDamping(float v) {
        setLinearDamping(characterId, v);
    }

    private native void setLinearDamping(long characterId, float v);

    public float getAngularDamping() {
        return getAngularDamping(characterId);
    }

    private native float getAngularDamping(long characterId);

    public void setAngularDamping(float v) {
        setAngularDamping(characterId, v);
    }

    private native void setAngularDamping(long characterId, float v);

    public float getStepHeight() {
        return getStepHeight(characterId);
    }

    private native float getStepHeight(long characterId);

    public void setStepHeight(float v) {
        setStepHeight(characterId, v);
    }

    private native void setStepHeight(long characterId, float v);

    public float getMaxPenetrationDepth() {
        return getMaxPenetrationDepth(characterId);
    }

    private native float getMaxPenetrationDepth(long characterId);

    public void setMaxPenetrationDepth(float v) {
        setMaxPenetrationDepth(characterId, v);
    }

    private native void setMaxPenetrationDepth(long characterId, float v);

    public void setMaxSlope(float slopeRadians) {
        setMaxSlope(characterId, slopeRadians);
    }

    private native void setMaxSlope(long characterId, float slopeRadians);

    public float getMaxSlope() {
        return getMaxSlope(characterId);
    }

    private native float getMaxSlope(long characterId);

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

    public void jump(Vector3f dir) {
        jump(characterId, dir);
    }

    private native void jump(long characterId, Vector3f v);

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (objectId == 0) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Directly alter the location of this character. (Same as
     * {@link #warp(com.jme3.math.Vector3f)}).)
     *
     * @param location the desired location (not null)
     */
    public void setPhysicsLocation(Vector3f location) {
        warp(location);
    }

    /**
     * Copy the location of this character's center of mass.
     *
     * @param trans a vector to store the result in (modified if not null)
     * @return the location (either trans or a new vector)
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

    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

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
     * Read the continuous collision detection (CCD) motion threshold for this
     * character.
     *
     * @return threshold value (&ge;0)
     */
    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    /**
     * Read the CCD square motion threshold for this character.
     *
     * @return threshold value (&ge;0)
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

    public void destroy() {
    }

    /**
     * Serialize this object, for example when saving to a J3O file.
     *
     * @param e exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(stepHeight, "stepHeight", 1.0f);
        capsule.write(getGravity(), "gravity", 9.8f * 3);
        capsule.write(getMaxSlope(), "maxSlope", 1.0f);
        capsule.write(fallSpeed, "fallSpeed", 55.0f);
        capsule.write(jumpSpeed, "jumpSpeed", 10.0f);
        capsule.write(upAxis, "upAxis", 1);
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
    }

    /**
     * De-serialize this character from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param e importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        stepHeight = capsule.readFloat("stepHeight", 1.0f);
        buildObject();
        setGravity(capsule.readFloat("gravity", 9.8f * 3));
        setMaxSlope(capsule.readFloat("maxSlope", 1.0f));
        setFallSpeed(capsule.readFloat("fallSpeed", 55.0f));
        setJumpSpeed(capsule.readFloat("jumpSpeed", 10.0f));
        setUpAxis(capsule.readInt("upAxis", 1));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
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

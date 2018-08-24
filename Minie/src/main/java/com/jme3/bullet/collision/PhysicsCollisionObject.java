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
package com.jme3.bullet.collision;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Base class for collision objects based on btCollisionObject, including
 * PhysicsCharacter, PhysicsRigidBody, and PhysicsGhostObject.
 *
 * @author normenhansen
 */
public abstract class PhysicsCollisionObject implements Savable {

    /**
     * Bullet id of the object. Constructors are expected to set this to a
     * non-zero value. The id might change if the object gets rebuilt.
     */
    protected long objectId = 0;
    protected CollisionShape collisionShape;

    public static final int COLLISION_GROUP_NONE = 0x00000000;
    public static final int COLLISION_GROUP_01 = 0x00000001;
    public static final int COLLISION_GROUP_02 = 0x00000002;
    public static final int COLLISION_GROUP_03 = 0x00000004;
    public static final int COLLISION_GROUP_04 = 0x00000008;
    public static final int COLLISION_GROUP_05 = 0x00000010;
    public static final int COLLISION_GROUP_06 = 0x00000020;
    public static final int COLLISION_GROUP_07 = 0x00000040;
    public static final int COLLISION_GROUP_08 = 0x00000080;
    public static final int COLLISION_GROUP_09 = 0x00000100;
    public static final int COLLISION_GROUP_10 = 0x00000200;
    public static final int COLLISION_GROUP_11 = 0x00000400;
    public static final int COLLISION_GROUP_12 = 0x00000800;
    public static final int COLLISION_GROUP_13 = 0x00001000;
    public static final int COLLISION_GROUP_14 = 0x00002000;
    public static final int COLLISION_GROUP_15 = 0x00004000;
    public static final int COLLISION_GROUP_16 = 0x00008000;

    private int collisionGroup = COLLISION_GROUP_01;
    private int collideWithGroups = COLLISION_GROUP_01;
    private Object userObject;

    /**
     * Apply a CollisionShape to this physics object. Note that the object
     * should not be in the physics space when adding a new collision shape as
     * it is rebuilt on the physics side.
     *
     * @param collisionShape the CollisionShape to set (not null, alias created)
     */
    public void setCollisionShape(CollisionShape collisionShape) {
        Validate.nonNull(collisionShape, "collision shape");
        this.collisionShape = collisionShape;
    }

    /**
     * Access the shape of this physics object.
     *
     * @return the pre-existing instance, which can then be applied to other
     * physics objects (increases performance)
     */
    public CollisionShape getCollisionShape() {
        return collisionShape;
    }

    /**
     * Read the collision group for this physics object.
     *
     * @return the collision group (bit mask with exactly one bit set)
     */
    public int getCollisionGroup() {
        return collisionGroup;
    }

    /**
     * Alter the collision group for this physics object.
     *
     * Groups are represented by integer bit masks with exactly 1 bit set.
     * Pre-made variables are available in PhysicsCollisionObject. By default,
     * physics objects are in COLLISION_GROUP_01.
     *
     * Two objects can collide only if one of them has the collisionGroup of the
     * other in its collideWithGroups set.
     *
     * @param collisionGroup the collisionGroup to apply (bit mask with exactly
     * 1 bit set)
     */
    public void setCollisionGroup(int collisionGroup) {
        assert Integer.bitCount(collisionGroup) == 1 : collisionGroup;

        this.collisionGroup = collisionGroup;
        if (objectId != 0) {
            setCollisionGroup(objectId, collisionGroup);
        }
    }

    /**
     * Add collision groups to the set with which this object can collide.
     *
     * Two objects can collide only if one of them has the collisionGroup of the
     * other in its collideWithGroups set.
     *
     * @param collisionGroup groups to add (bit mask)
     */
    public void addCollideWithGroup(int collisionGroup) {
        this.collideWithGroups = this.collideWithGroups | collisionGroup;
        if (objectId != 0) {
            setCollideWithGroups(objectId, this.collideWithGroups);
        }
    }

    /**
     * Remove collision groups from the set with which this object can collide.
     *
     * @param collisionGroup groups to remove, ORed together (bit mask)
     */
    public void removeCollideWithGroup(int collisionGroup) {
        this.collideWithGroups = this.collideWithGroups & ~collisionGroup;
        if (objectId != 0) {
            setCollideWithGroups(this.collideWithGroups);
        }
    }

    /**
     * Directly alter the collision groups with which this object can collide.
     *
     * @param collisionGroups desired groups, ORed together (bit mask)
     */
    public void setCollideWithGroups(int collisionGroups) {
        this.collideWithGroups = collisionGroups;
        if (objectId != 0) {
            setCollideWithGroups(objectId, this.collideWithGroups);
        }
    }

    /**
     * Read the set of collision groups with which this object can collide.
     *
     * @return bit mask
     */
    public int getCollideWithGroups() {
        return collideWithGroups;
    }

    protected void initUserPointer() {
        Logger.getLogger(this.getClass().getName()).log(Level.FINE,
                "initUserPointer() objectId = {0}", Long.toHexString(objectId));
        initUserPointer(objectId, collisionGroup, collideWithGroups);
    }

    native void initUserPointer(long objectId, int group, int groups);

    /**
     * Access the user object associated with this collision object.
     *
     * @return the pre-existing instance, or null if none
     */
    public Object getUserObject() {
        return userObject;
    }

    /**
     * Associate a user object (such as a Spatial) with this collision object.
     *
     * @param userObject the object to associate with this collision object
     * (alias created, may be null)
     */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    /**
     * Read the id of the corresponding btCollisionObject.
     *
     * @return the id, or 0 if none
     */
    public long getObjectId() {
        return objectId;
    }

    protected native void attachCollisionShape(long objectId, long collisionShapeId);

    native void setCollisionGroup(long objectId, int collisionGroup);

    native void setCollideWithGroups(long objectId, int collisionGroups);

    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(collisionGroup, "collisionGroup", COLLISION_GROUP_01);
        capsule.write(collideWithGroups, "collisionGroupsMask",
                COLLISION_GROUP_01);
        capsule.write(collisionShape, "collisionShape", null);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        collisionGroup = capsule.readInt("collisionGroup", COLLISION_GROUP_01);
        collideWithGroups = capsule.readInt("collisionGroupsMask",
                COLLISION_GROUP_01);
        CollisionShape shape = (CollisionShape) capsule.readSavable("collisionShape", null);
        collisionShape = shape;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE,
                "Finalizing CollisionObject {0}", Long.toHexString(objectId));
        finalizeNative(objectId);
    }

    protected native void finalizeNative(long objectId);
}

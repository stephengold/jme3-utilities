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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class replaces the deprecated CharacterControl class.
 * <p>
 * A rigid body with cylinder collision shape is used and its velocity is set
 * continuously. A ray test is used to test whether the character is on the
 * ground.
 * <p>
 * The character keeps their own local coordinate system which adapts based on
 * the gravity working on the character so they will always stand upright.
 * <p>
 * Forces in the local X-Z plane are dampened while those in the local Y
 * direction are applied fully (e.g. jumping, falling).
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class BetterCharacterControl extends AbstractPhysicsControl
        implements PhysicsTickListener, JmeCloneable {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BetterCharacterControl.class.getName());

    private PhysicsRigidBody rigidBody;
    private float radius;
    private float height;
    private float mass;
    private float duckedFactor = 0.6f;
    /**
     * Local up direction, derived from gravity.
     */
    private final Vector3f localUp = new Vector3f(0f, 1f, 0f);
    /**
     * Local absolute z-forward direction, derived from gravity and UNIT_Z,
     * updated continuously when gravity changes.
     */
    private final Vector3f localForward = new Vector3f(0f, 0f, 1f);
    /**
     * Local left direction, derived from up and forward.
     */
    private final Vector3f localLeft = new Vector3f(1f, 0f, 0f);
    /**
     * Local z-forward quaternion for the "local absolute" z-forward direction.
     */
    private final Quaternion localForwardRotation
            = new Quaternion(Quaternion.DIRECTION_Z);
    /**
     * Is a z-forward vector based on the view direction and the current local
     * x/z plane.
     */
    private final Vector3f viewDirection = new Vector3f(0f, 0f, 1f);
    /**
     * Stores final spatial location, corresponds to RigidBody location.
     */
    private final Vector3f location = new Vector3f();
    /**
     * Stores final spatial rotation, is a z-forward rotation based on the view
     * direction and the current local x/z plane. See also rotatedViewDirection.
     */
    private final Quaternion rotation = new Quaternion(Quaternion.DIRECTION_Z);
    private final Vector3f rotatedViewDirection = new Vector3f(0f, 0f, 1f);
    private final Vector3f walkDirection = new Vector3f();
    private final Vector3f jumpForce;
    private float physicsDamping = 0.9f;
    private final Vector3f scale = new Vector3f(1f, 1f, 1f);
    private final Vector3f velocity = new Vector3f();
    private boolean jump = false;
    private boolean onGround = false;
    private boolean ducked = false;
    private boolean wantToUnDuck = false;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public BetterCharacterControl() {
        jumpForce = new Vector3f();
    }

    /**
     * Create a new character with the specified properties. Note that to avoid
     * issues the final height when ducking should be larger than 2x radius. The
     * jumpForce will be set to an upwards force of 5x mass.
     *
     * @param radius the radius of the character's collision shape
     * @param height the height of the character's collision shape
     * @param mass the mass of the character
     */
    public BetterCharacterControl(float radius, float height, float mass) {
        this.radius = radius;
        this.height = height;
        this.mass = mass;
        rigidBody = new PhysicsRigidBody(getShape(), mass);
        jumpForce = new Vector3f(0, mass * 5, 0);
        rigidBody.setAngularFactor(0);
    }

    /**
     * Update this control. (Invoked once per frame.)
     *
     * @param tpf the time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        rigidBody.getPhysicsLocation(location);
        //rotation has been set through viewDirection
        applyPhysicsTransform(location, rotation);
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * Used internally, don't call manually
     *
     * @param space unused
     * @param tpf unused
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        checkOnGround();
        if (wantToUnDuck && checkCanUnDuck()) {
            setHeightPercent(1);
            wantToUnDuck = false;
            ducked = false;
        }
        TempVars vars = TempVars.get();

        Vector3f currentVelocity = vars.vect2.set(velocity);

        // dampen existing x/z forces
        float existingLeftVelocity = velocity.dot(localLeft);
        float existingForwardVelocity = velocity.dot(localForward);
        Vector3f counter = vars.vect1;
        existingLeftVelocity = existingLeftVelocity * physicsDamping;
        existingForwardVelocity = existingForwardVelocity * physicsDamping;
        counter.set(-existingLeftVelocity, 0, -existingForwardVelocity);
        localForwardRotation.multLocal(counter);
        velocity.addLocal(counter);

        float designatedVelocity = walkDirection.length();
        if (designatedVelocity > 0) {
            Vector3f localWalkDirection = vars.vect1;
            //normalize walkdirection
            localWalkDirection.set(walkDirection).normalizeLocal();
            //check for the existing velocity in the desired direction
            float existingVelocity = velocity.dot(localWalkDirection);
            //calculate the final velocity in the desired direction
            float finalVelocity = designatedVelocity - existingVelocity;
            localWalkDirection.multLocal(finalVelocity);
            //add resulting vector to existing velocity
            velocity.addLocal(localWalkDirection);
        }
        if (currentVelocity.distance(velocity) > FastMath.ZERO_TOLERANCE) {
            rigidBody.setLinearVelocity(velocity);
        }
        if (jump) {
            //TODO: precalculate jump force
            Vector3f rotatedJumpForce = vars.vect1;
            rotatedJumpForce.set(jumpForce);
            rigidBody.applyImpulse(
                    localForwardRotation.multLocal(rotatedJumpForce),
                    Vector3f.ZERO);
            jump = false;
        }
        vars.release();
    }

    /**
     * Used internally, don't call manually
     *
     * @param space unused
     * @param tpf unused
     */
    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        rigidBody.getLinearVelocity(velocity);
    }

    /**
     * Move the character somewhere. Note the character also takes the location
     * of any spatial its being attached to in the moment it is attached.
     *
     * @param vec the desired character location (not null)
     */
    public void warp(Vector3f vec) {
        setPhysicsLocation(vec);
    }

    /**
     * Makes the character jump with the set jump force.
     */
    public void jump() {
        //TODO: debounce over some frames
        if (!onGround) {
            return;
        }
        jump = true;
    }

    /**
     * Set the jump force as a Vector3f. The jump force is local to the
     * characters coordinate system, which normally is always z-forward (in
     * world coordinates, parent coordinates when set to applyLocalPhysics)
     *
     * @param jumpForce the desired jump force
     */
    public void setJumpForce(Vector3f jumpForce) {
        this.jumpForce.set(jumpForce);
    }

    /**
     * Gets the current jump force. The default is 5 * character mass in y
     * direction.
     *
     * @return the pre-existing vector
     */
    public Vector3f getJumpForce() {
        return jumpForce;
    }

    /**
     * Check if the character is on the ground. This is determined by a ray test
     * in the center of the character and might return false even if the
     * character is not falling yet.
     *
     * @return true if on the ground, otherwise false
     */
    public boolean isOnGround() {
        return onGround;
    }

    /**
     * Toggle character ducking. When ducked the characters capsule collision
     * shape height will be multiplied by duckedFactor to make the capsule
     * smaller. When unducking, the character will check with a ray test if it
     * can in fact unduck and only do so when its possible. You can check the
     * state of the unducking by checking isDucked().
     *
     * @param enabled true&rarr;duck, false&rarr;unduck
     */
    public void setDucked(boolean enabled) {
        if (enabled) {
            setHeightPercent(duckedFactor);
            ducked = true;
            wantToUnDuck = false;
        } else {
            if (checkCanUnDuck()) {
                setHeightPercent(1);
                ducked = false;
            } else {
                wantToUnDuck = true;
            }
        }
    }

    /**
     * Check if the character is ducking, either due to user input or due to
     * unducking being impossible at the moment (obstacle above).
     *
     * @return true if ducking, otherwise false
     */
    public boolean isDucked() {
        return ducked;
    }

    /**
     * Sets the height multiplication factor for ducking.
     *
     * @param factor The factor by which the height should be multiplied when
     * ducking
     */
    public void setDuckedFactor(float factor) {
        duckedFactor = factor;
    }

    /**
     * Gets the height multiplication factor for ducking.
     *
     * @return the factor
     */
    public float getDuckedFactor() {
        return duckedFactor;
    }

    /**
     * Sets the walk direction of the character. This parameter is framerate
     * independent and the character will move continuously in the direction
     * given by the vector with the speed given by the vector length in m/s.
     *
     * @param vec The movement direction and speed in m/s
     */
    public void setWalkDirection(Vector3f vec) {
        walkDirection.set(vec);
    }

    /**
     * Gets the current walk direction and speed of the character. The length of
     * the vector defines the speed.
     *
     * @return the pre-existing vector
     */
    public Vector3f getWalkDirection() {
        return walkDirection;
    }

    /**
     * Sets the view direction for the character. Note this only defines the
     * rotation of the spatial in the local x/z plane of the character.
     *
     * @param vec a direction vector (not null, unaffected)
     */
    public void setViewDirection(Vector3f vec) {
        viewDirection.set(vec);
        updateLocalViewDirection();
    }

    /**
     * Gets the current view direction, note this doesn't need to correspond
     * with the spatials forward direction.
     *
     * @return the pre-existing vector
     */
    public Vector3f getViewDirection() {
        return viewDirection;
    }

    /**
     * Realign the local forward vector to given direction vector, if null is
     * supplied Vector3f.UNIT_Z is used. Input vector has to be perpendicular to
     * current gravity vector. This normally only needs to be called when the
     * gravity direction changed continuously and the local forward vector is
     * off due to drift. E.g. after walking around on a sphere "planet" for a
     * while and then going back to a y-up coordinate system the local z-forward
     * might not be 100% alinged with Z axis.
     *
     * @param vec The new forward vector, has to be perpendicular to the current
     * gravity vector!
     */
    public void resetForward(Vector3f vec) {
        if (vec == null) {
            vec = Vector3f.UNIT_Z;
        }
        localForward.set(vec);
        updateLocalCoordinateSystem();
    }

    /**
     * Get the current linear velocity along the three axes of the character.
     * This is prepresented in world coordinates, parent coordinates when the
     * control is set to applyLocalPhysics.
     *
     * @return The current linear velocity of the character
     */
    public Vector3f getVelocity() {
        return velocity;
    }

    /**
     * Set the gravity for this character. Note that this also realigns the
     * local coordinate system of the character so that continuous changes in
     * gravity direction are possible while maintaining a sensible control over
     * the character.
     *
     * @param gravity an acceleration vector (not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        rigidBody.setGravity(gravity);
        localUp.set(gravity).normalizeLocal().negateLocal();
        updateLocalCoordinateSystem();
    }

    /**
     * Get the current gravity of the character.
     *
     * @return a new acceleration vector
     */
    public Vector3f getGravity() {
        return rigidBody.getGravity();
    }

    /**
     * Get the current gravity of the character.
     *
     * @param store The vector to store the result in
     * @return either store or a new instance
     */
    public Vector3f getGravity(Vector3f store) {
        return rigidBody.getGravity(store);
    }

    /**
     * Sets how much the physics forces in the local x/z plane should be
     * dampened.
     *
     * @param physicsDamping The dampening value, 0 = no dampening, 1 = no
     * external force, default = 0.9
     */
    public void setPhysicsDamping(float physicsDamping) {
        this.physicsDamping = physicsDamping;
    }

    /**
     * Gets how much the physics forces in the local x/z plane should be
     * dampened.
     *
     * @return the damping factor
     */
    public float getPhysicsDamping() {
        return physicsDamping;
    }

    /**
     * This actually sets a new collision shape to the character to change the
     * height of the capsule.
     *
     * @param percent factor to apply
     */
    protected void setHeightPercent(float percent) {
        scale.setY(percent);
        rigidBody.setCollisionShape(getShape());
    }

    /**
     * This checks if the character is on the ground by doing a ray test.
     */
    protected void checkOnGround() {
        TempVars vars = TempVars.get();
        Vector3f location = vars.vect1;
        Vector3f rayVector = vars.vect2;
        float height = getFinalHeight();
        location.set(localUp).multLocal(height).addLocal(this.location);
        rayVector.set(localUp).multLocal(-height - 0.1f).addLocal(location);
        List<PhysicsRayTestResult> results
                = getPhysicsSpace().rayTest(location, rayVector);
        vars.release();
        for (PhysicsRayTestResult physicsRayTestResult : results) {
            if (!physicsRayTestResult.getCollisionObject().equals(rigidBody)) {
                onGround = true;
                return;
            }
        }
        onGround = false;
    }

    /**
     * This checks if the character can go from ducked to unducked state by
     * doing a ray test.
     *
     * @return true if able to unduck, otherwise false
     */
    protected boolean checkCanUnDuck() {
        TempVars vars = TempVars.get();
        Vector3f location = vars.vect1;
        Vector3f rayVector = vars.vect2;
        location.set(localUp).multLocal(FastMath.ZERO_TOLERANCE).addLocal(this.location);
        rayVector.set(localUp).multLocal(height + FastMath.ZERO_TOLERANCE).addLocal(location);
        List<PhysicsRayTestResult> results
                = getPhysicsSpace().rayTest(location, rayVector);
        vars.release();
        for (PhysicsRayTestResult physicsRayTestResult : results) {
            if (!physicsRayTestResult.getCollisionObject().equals(rigidBody)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a new collision shape based on the current scale parameter. The
     * created collisionshape is a capsule collision shape that is attached to a
     * compound collision shape with an offset to set the object center at the
     * bottom of the capsule.
     *
     * @return a new compound shape
     */
    protected CollisionShape getShape() {
        //TODO: cleanup size mess..
        CapsuleCollisionShape capsuleCollisionShape = new CapsuleCollisionShape(getFinalRadius(), (getFinalHeight() - (2 * getFinalRadius())));
        CompoundCollisionShape compoundCollisionShape = new CompoundCollisionShape();
        Vector3f addLocation = new Vector3f(0, (getFinalHeight() / 2.0f), 0);
        compoundCollisionShape.addChildShape(capsuleCollisionShape, addLocation);
        return compoundCollisionShape;
    }

    /**
     * Gets the scaled height.
     *
     * @return the height
     */
    protected float getFinalHeight() {
        return height * scale.getY();
    }

    /**
     * Gets the scaled radius.
     *
     * @return the radius
     */
    protected float getFinalRadius() {
        return radius * scale.getZ();
    }

    /**
     * Updates the local coordinate system from the localForward and localUp
     * vectors, adapts localForward, sets localForwardRotation quaternion to
     * local z-forward rotation.
     */
    protected void updateLocalCoordinateSystem() {
        //gravity vector has possibly changed, calculate new world forward (UNIT_Z)
        calculateNewForward(localForwardRotation, localForward, localUp);
        localLeft.set(localUp).crossLocal(localForward);
        rigidBody.setPhysicsRotation(localForwardRotation);
        updateLocalViewDirection();
    }

    /**
     * Updates the local x/z-flattened view direction and the corresponding
     * rotation quaternion for the spatial.
     */
    protected void updateLocalViewDirection() {
        //update local rotation quaternion to use for view rotation
        localForwardRotation.multLocal(rotatedViewDirection.set(viewDirection));
        calculateNewForward(rotation, rotatedViewDirection, localUp);
    }

    /**
     * This method works similar to Camera.lookAt but where lookAt sets the
     * priority on the direction, this method sets the priority on the up vector
     * so that the result direction vector and rotation is guaranteed to be
     * perpendicular to the up vector.
     *
     * @param rotation The rotation to set the result on or null to create a new
     * Quaternion, this will be set to the new "z-forward" rotation if not null
     * @param direction The direction to base the new look direction on, will be
     * set to the new direction
     * @param worldUpVector The up vector to use, the result direction will be
     * perpendicular to this
     */
    protected final void calculateNewForward(Quaternion rotation, Vector3f direction, Vector3f worldUpVector) {
        if (direction == null) {
            return;
        }
        TempVars vars = TempVars.get();
        Vector3f newLeft = vars.vect1;
        Vector3f newLeftNegate = vars.vect2;

        newLeft.set(worldUpVector).crossLocal(direction).normalizeLocal();
        if (newLeft.equals(Vector3f.ZERO)) {
            if (direction.x != 0) {
                newLeft.set(direction.y, -direction.x, 0f).normalizeLocal();
            } else {
                newLeft.set(0f, direction.z, -direction.y).normalizeLocal();
            }
            logger.log(Level.INFO, "Zero left for direction {0}, up {1}", new Object[]{direction, worldUpVector});
        }
        newLeftNegate.set(newLeft).negateLocal();
        direction.set(worldUpVector).crossLocal(newLeftNegate).normalizeLocal();
        if (direction.equals(Vector3f.ZERO)) {
            direction.set(Vector3f.UNIT_Z);
            logger.log(Level.INFO, "Zero left for left {0}, up {1}", new Object[]{newLeft, worldUpVector});
        }
        if (rotation != null) {
            rotation.fromAxes(newLeft, worldUpVector, direction);
        }
        vars.release();
    }

    /**
     * This is implemented from AbstractPhysicsControl and called when the
     * spatial is attached for example.
     *
     * @param vec the desired location (not null)
     */
    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        rigidBody.setPhysicsLocation(vec);
        location.set(vec);
    }

    /**
     * This is implemented from AbstractPhysicsControl and called when the
     * spatial is attached for example. We don't set the actual physics rotation
     * but the view rotation here. It might actually be altered by the
     * calculateNewForward method.
     *
     * @param quat the desired orientation (not null, unaffected)
     */
    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        rotation.set(quat);
        rotation.multLocal(rotatedViewDirection.set(viewDirection));
        updateLocalViewDirection();
    }

    /**
     * This is implemented from AbstractPhysicsControl and called when the
     * control is supposed to add all objects to the physics space.
     *
     * @param space the physics space to add to (not null)
     */
    @Override
    protected void addPhysics(PhysicsSpace space) {
        space.getGravity(localUp).normalizeLocal().negateLocal();
        updateLocalCoordinateSystem();

        space.addCollisionObject(rigidBody);
        space.addTickListener(this);
    }

    /**
     * This is implemented from AbstractPhysicsControl and called when the
     * control is supposed to remove all objects from the physics space.
     *
     * @param space the physics space to remove from (not null)
     */
    @Override
    protected void removePhysics(PhysicsSpace space) {
        space.removeCollisionObject(rigidBody);
        space.removeTickListener(this);
    }

    /**
     * Create spatial-dependent data. Invoked when this control is added to a
     * spatial.
     *
     * @param spat the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial spat) {
        rigidBody.setUserObject(spatial);
    }

    /**
     * Destroy spatial-dependent data. Invoked when this control is removed from
     * a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        rigidBody.setUserObject(null);
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        BetterCharacterControl control
                = new BetterCharacterControl(radius, height, mass);
        control.setJumpForce(jumpForce);
        return control;
    }

    @Override
    public Object jmeClone() {
        BetterCharacterControl control
                = new BetterCharacterControl(radius, height, mass);
        control.setJumpForce(jumpForce);
        control.spatial = this.spatial;
        return control;
    }

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(radius, "radius", 1);
        oc.write(height, "height", 1);
        oc.write(mass, "mass", 1);
        oc.write(jumpForce, "jumpForce", new Vector3f(0, mass * 5, 0));
        oc.write(physicsDamping, "physicsDamping", 0.9f);
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        this.radius = in.readFloat("radius", 1);
        this.height = in.readFloat("height", 2);
        this.mass = in.readFloat("mass", 80);
        this.physicsDamping = in.readFloat("physicsDamping", 0.9f);
        this.jumpForce.set((Vector3f) in.readSavable("jumpForce", new Vector3f(0, mass * 5, 0)));
        rigidBody = new PhysicsRigidBody(getShape(), mass);
        jumpForce.set(new Vector3f(0, mass * 5, 0));
        rigidBody.setAngularFactor(0);
    }
}

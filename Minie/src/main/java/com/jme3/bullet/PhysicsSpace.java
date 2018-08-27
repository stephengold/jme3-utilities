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
package com.jme3.bullet;

import com.jme3.app.AppTask;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * <p>
 * PhysicsSpace - The central jbullet-jme physics space</p>
 *
 * @author normenhansen
 */
public class PhysicsSpace {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsSpace.class.getName());
    /**
     * index of the X axis
     */
    public static final int AXIS_X = 0;
    /**
     * index of the Y axis
     */
    public static final int AXIS_Y = 1;
    /**
     * index of the Z axis
     */
    public static final int AXIS_Z = 2;

    private long physicsSpaceId = 0;
    private static ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>> pQueueTL
            = new ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>>() {
        @Override
        protected ConcurrentLinkedQueue<AppTask<?>> initialValue() {
            return new ConcurrentLinkedQueue<>();
        }
    };
    final private ConcurrentLinkedQueue<AppTask<?>> pQueue
            = new ConcurrentLinkedQueue<>();
    private static ThreadLocal<PhysicsSpace> physicsSpaceTL
            = new ThreadLocal<PhysicsSpace>();
    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
    final private Map<Long, PhysicsGhostObject> physicsGhostObjects
            = new ConcurrentHashMap<>();
    final private Map<Long, PhysicsCharacter> physicsCharacters
            = new ConcurrentHashMap<>();
    final private Map<Long, PhysicsRigidBody> physicsBodies
            = new ConcurrentHashMap<>();
    final private Map<Long, PhysicsJoint> physicsJoints
            = new ConcurrentHashMap<>();
    final private Map<Long, PhysicsVehicle> physicsVehicles
            = new ConcurrentHashMap<>();
    final private ArrayList<PhysicsCollisionListener> collisionListeners
            = new ArrayList<>();
    final private ArrayDeque<PhysicsCollisionEvent> collisionEvents
            = new ArrayDeque<>();
    final private Map<Integer, PhysicsCollisionGroupListener> collisionGroupListeners
            = new ConcurrentHashMap<>();
    final private ConcurrentLinkedQueue<PhysicsTickListener> tickListeners
            = new ConcurrentLinkedQueue<>();
    final private PhysicsCollisionEventFactory eventFactory
            = new PhysicsCollisionEventFactory();
    /**
     * minimum coordinate values when using AXIS_SWEEP broadphase algorithms
     */
    final private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    /**
     * maximum coordinate values when using AXIS_SWEEP broadphase algorithms
     */
    final private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    private float accuracy = 1f / 60f;
    private int maxSubSteps = 4, rayTestFlags = 1 << 2;
    /**
     * number of iterations used by the contact-and-constraint solver
     * (default=10)
     */
    private int solverNumIterations = 10;

    /**
     * Access the PhysicsSpace <b>running on this thread</b>. For parallel
     * physics, this can be called from the OpenGL thread.
     *
     * @return the PhysicsSpace running on this thread
     */
    public static PhysicsSpace getPhysicsSpace() {
        return physicsSpaceTL.get();
    }

    /**
     * Used internally
     *
     * @param space which physics space to simulate
     */
    public static void setLocalThreadPhysicsSpace(PhysicsSpace space) {
        physicsSpaceTL.set(space);
    }

    /**
     * Create a PhysicsSpace. Must be called from the designated physics thread.
     *
     * @param worldMin the desired minimum coordinates values (not null,
     * unaffected)
     * @param worldMax the desired minimum coordinates values (not null,
     * unaffected)
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public PhysicsSpace(Vector3f worldMin, Vector3f worldMax,
            BroadphaseType broadphaseType) {
        Validate.nonNull(worldMin, "world min");
        Validate.nonNull(worldMax, "world max");
        Validate.nonNull(broadphaseType, "broadphase type");

        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
        create();
    }

    /**
     * Must be invoked from the designated physics thread.
     */
    private void create() {
        physicsSpaceId = createPhysicsSpace(worldMin.x, worldMin.y, worldMin.z,
                worldMax.x, worldMax.y, worldMax.z, broadphaseType.ordinal(),
                false);
        pQueueTL.set(pQueue);
        physicsSpaceTL.set(this);
    }

    private native long createPhysicsSpace(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ, int broadphaseType,
            boolean threading);

    /**
     * This method is invoked from native code.
     *
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    private void preTick_native(float timeStep) {
        AppTask task;
        while ((task = pQueue.poll()) != null) {
            if (task.isCancelled()) {
                continue;
            }
            try {
                task.invoke();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        for (PhysicsTickListener listener : tickListeners) {
            listener.prePhysicsTick(this, timeStep);
        }
    }

    /**
     * This method is invoked from native code.
     *
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    private void postTick_native(float timeStep) {
        for (PhysicsTickListener listener : tickListeners) {
            listener.physicsTick(this, timeStep);
        }
    }

    private void addCollision_native() {
    }

    private boolean needCollision_native(PhysicsCollisionObject objectA,
            PhysicsCollisionObject objectB) {
        return false;
    }

    private void addCollisionEvent_native(PhysicsCollisionObject node, PhysicsCollisionObject node1, long manifoldPointObjectId) {
//        System.out.println("addCollisionEvent:"+node.getObjectId()+" "+ node1.getObjectId());
        collisionEvents.add(eventFactory.getEvent(PhysicsCollisionEvent.TYPE_PROCESSED, node, node1, manifoldPointObjectId));
    }

    private boolean notifyCollisionGroupListeners_native(
            PhysicsCollisionObject node, PhysicsCollisionObject node1) {
        PhysicsCollisionGroupListener listener
                = collisionGroupListeners.get(node.getCollisionGroup());
        PhysicsCollisionGroupListener listener1
                = collisionGroupListeners.get(node1.getCollisionGroup());
        boolean result = true;

        if (listener != null) {
            result = listener.collide(node, node1);
        }
        if (listener1 != null && node.getCollisionGroup() != node1.getCollisionGroup()) {
            result = listener1.collide(node, node1) && result;
        }

        return result;
    }

    /**
     * Update this space. Invoked (by the app state) once per render pass.
     *
     * @param time time-per-frame multiplied by speed (in seconds, &ge;0)
     */
    void update(float time) {
        assert time >= 0f : time;
        update(time, maxSubSteps);
    }

    /**
     * Update this space, simulating at most the specified number of steps.
     *
     * @param time time-per-frame multiplied by speed (in seconds, &ge;0)
     * @param maxSteps maximum number of steps to simulate (&ge;0)
     */
    private void update(float time, int maxSteps) {
        assert time >= 0f : time;
        assert maxSteps >= 0 : maxSteps;

        stepSimulation(physicsSpaceId, time, maxSteps, accuracy);
    }

    private native void stepSimulation(long space, float time, int maxSteps,
            float accuracy);

    public void distributeEvents() {
        //add collision callbacks
        int clistsize = collisionListeners.size();
        while (collisionEvents.isEmpty() == false) {
            PhysicsCollisionEvent physicsCollisionEvent = collisionEvents.pop();
            for (int i = 0; i < clistsize; i++) {
                collisionListeners.get(i).collision(physicsCollisionEvent);
            }
            //recycle events
            eventFactory.recycle(physicsCollisionEvent);
        }
    }

    public static <V> Future<V> enqueueOnThisThread(Callable<V> callable) {
        AppTask<V> task = new AppTask<>(callable);
        System.out.println("created apptask");
        pQueueTL.get().add(task);
        return task;
    }

    /**
     * calls the callable on the next physics tick (ensuring e.g. force
     * applying)
     *
     * @param <V> the type of result returned by the callable
     * @param callable which callable to invoke
     * @return Future object
     */
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<>(callable);
        pQueue.add(task);
        return task;
    }

    /**
     * Add the specified object to this space.
     *
     * @param obj the PhysicsControl, Spatial-with-PhysicsControl,
     * PhysicsCollisionObject, or PhysicsJoint to add (not null, modified)
     */
    public void add(Object obj) {
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(this);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    add(((PhysicsControl) node.getControl(i)));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            addCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            addJoint((PhysicsJoint) obj);
        } else {
            throw (new IllegalArgumentException(
                    "Cannot add this kind of object to the physics space."));
        }
    }

    /**
     * Add the specified collision object to this space.
     *
     * @param obj the PhysicsCollisionObject to add (not null, modified)
     */
    public void addCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            addGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            addRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsVehicle) {
            addRigidBody((PhysicsVehicle) obj);
        } else if (obj instanceof PhysicsCharacter) {
            addCharacter((PhysicsCharacter) obj);
        } else {
            throw (new IllegalArgumentException("Unknown type of collision object."));
        }
    }

    /**
     * Remove the specified object from this space.
     *
     * @param obj the PhysicsCollisionObject to add, or null (modified)
     */
    public void remove(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(null);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    remove(((PhysicsControl) node.getControl(i)));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            removeCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            removeJoint((PhysicsJoint) obj);
        } else {
            throw (new IllegalArgumentException(
                    "Cannot remove this kind of object from the physics space."));
        }
    }

    /**
     * Remove the specified collision object from this space.
     *
     * @param obj the PhysicsControl or Spatial with PhysicsControl to remove
     */
    public void removeCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            removeGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            removeRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsCharacter) {
            removeCharacter((PhysicsCharacter) obj);
        } else {
            throw (new IllegalArgumentException(
                    "Unknown type of collision object."));
        }
    }

    /**
     * Add all physics controls and joints in the specified subtree of the scene
     * graph to this space (e.g. after loading from disk). Note: recursive!
     *
     * @param spatial the root of the subtree (not null)
     */
    public void addAll(Spatial spatial) {
        add(spatial);

        if (spatial.getControl(RigidBodyControl.class) != null) {
            RigidBodyControl control
                    = spatial.getControl(RigidBodyControl.class);
            // Add only the joints with the RigidBodyControl as BodyA.
            List<PhysicsJoint> joints = control.getJoints();
            for (PhysicsJoint physicsJoint : joints) {
                if (control == physicsJoint.getBodyA()) {
                    add(physicsJoint);
                }
            }
            // TODO multiple controls per spatial?
        }
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Spatial spat : children) {
                addAll(spat);
            }
        }
    }

    /**
     * Remove all physics controls and joints in the specified subtree of the
     * scene graph from the physics space (e.g. before saving to disk) Note:
     * recursive!
     *
     * @param spatial the root of the subtree (not null)
     */
    public void removeAll(Spatial spatial) {
        RigidBodyControl control
                = spatial.getControl(RigidBodyControl.class);
        if (control != null) {
            // Remove only the joints with the RigidBodyControl as BodyA.
            List<PhysicsJoint> joints = control.getJoints();
            for (Iterator<PhysicsJoint> it1 = joints.iterator(); it1.hasNext();) {
                PhysicsJoint physicsJoint = it1.next();
                if (control.equals(physicsJoint.getBodyA())) {
                    removeJoint(physicsJoint);
                }
            }
            // TODO multiple controls per spatial?
        }

        remove(spatial);
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Spatial spat : children) {
                removeAll(spat);
            }
        }
    }

    private native void addCollisionObject(long space, long id);

    private native void removeCollisionObject(long space, long id);

    private native void addRigidBody(long space, long id);

    private native void removeRigidBody(long space, long id);

    private native void addCharacterObject(long space, long id);

    private native void removeCharacterObject(long space, long id);

    private native void addAction(long space, long id);

    private native void removeAction(long space, long id);

    private native void addVehicle(long space, long id);

    private native void removeVehicle(long space, long id);

    private native void addConstraint(long space, long id);

    private native void addConstraintC(long space, long id, boolean collision);

    private native void removeConstraint(long space, long id);

    private void addGhostObject(PhysicsGhostObject node) {
        if (physicsGhostObjects.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING,
                    "GhostObject {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsGhostObjects.put(node.getObjectId(), node);
        logger.log(Level.FINE, "Adding ghost object {0} to physics space.",
                Long.toHexString(node.getObjectId()));
        addCollisionObject(physicsSpaceId, node.getObjectId());
    }

    private void removeGhostObject(PhysicsGhostObject node) {
        if (!physicsGhostObjects.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING,
                    "GhostObject {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsGhostObjects.remove(node.getObjectId());
        logger.log(Level.FINE,
                "Removing ghost object {0} from physics space.", Long.toHexString(node.getObjectId()));
        removeCollisionObject(physicsSpaceId, node.getObjectId());
    }

    private void addCharacter(PhysicsCharacter node) {
        if (physicsCharacters.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING,
                    "Character {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsCharacters.put(node.getObjectId(), node);
        logger.log(Level.FINE, "Adding character {0} to physics space.",
                Long.toHexString(node.getObjectId()));
        addCharacterObject(physicsSpaceId, node.getObjectId());
        addAction(physicsSpaceId, node.getControllerId());
    }

    private void removeCharacter(PhysicsCharacter node) {
        if (!physicsCharacters.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING,
                    "Character {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsCharacters.remove(node.getObjectId());
        logger.log(Level.FINE, "Removing character {0} from physics space.", Long.toHexString(node.getObjectId()));
        removeAction(physicsSpaceId, node.getControllerId());
        removeCharacterObject(physicsSpaceId, node.getObjectId());
    }

    /**
     * NOTE: When a rigid body is added, its gravity gets set to that of the
     * physics space.
     *
     * @param node the body to add (not null, not already in the space)
     */
    private void addRigidBody(PhysicsRigidBody node) {
        if (physicsBodies.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "RigidBody {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsBodies.put(node.getObjectId(), node);

        //Workaround
        //It seems that adding a Kinematic RigidBody to the dynamicWorld prevent it from being non kinematic again afterward.
        //so we add it non kinematic, then set it kinematic again.
        boolean kinematic = false;
        if (node.isKinematic()) {
            kinematic = true;
            node.setKinematic(false);
        }
        addRigidBody(physicsSpaceId, node.getObjectId());
        if (kinematic) {
            node.setKinematic(true);
        }

        logger.log(Level.FINE, "Adding RigidBody {0} to physics space.", node.getObjectId());
        if (node instanceof PhysicsVehicle) {
            logger.log(Level.FINE, "Adding vehicle constraint {0} to physics space.", Long.toHexString(((PhysicsVehicle) node).getVehicleId()));
            physicsVehicles.put(((PhysicsVehicle) node).getVehicleId(), (PhysicsVehicle) node);
            addVehicle(physicsSpaceId, ((PhysicsVehicle) node).getVehicleId());
        }
    }

    private void removeRigidBody(PhysicsRigidBody node) {
        if (!physicsBodies.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "RigidBody {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        if (node instanceof PhysicsVehicle) {
            logger.log(Level.FINE, "Removing vehicle constraint {0} from physics space.", Long.toHexString(((PhysicsVehicle) node).getVehicleId()));
            physicsVehicles.remove(((PhysicsVehicle) node).getVehicleId());
            removeVehicle(physicsSpaceId, ((PhysicsVehicle) node).getVehicleId());
        }
        logger.log(Level.FINE, "Removing RigidBody {0} from physics space.", Long.toHexString(node.getObjectId()));
        physicsBodies.remove(node.getObjectId());
        removeRigidBody(physicsSpaceId, node.getObjectId());
    }

    private void addJoint(PhysicsJoint joint) {
        if (physicsJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} already exists in PhysicsSpace, cannot add.", joint);
            return;
        }
        logger.log(Level.FINE, "Adding Joint {0} to physics space.", Long.toHexString(joint.getObjectId()));
        physicsJoints.put(joint.getObjectId(), joint);
        addConstraintC(physicsSpaceId, joint.getObjectId(), !joint.isCollisionBetweenLinkedBodys());
    }

    private void removeJoint(PhysicsJoint joint) {
        if (!physicsJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} does not exist in PhysicsSpace, cannot remove.", joint);
            return;
        }
        logger.log(Level.FINE, "Removing Joint {0} from physics space.", Long.toHexString(joint.getObjectId()));
        physicsJoints.remove(joint.getObjectId());
        removeConstraint(physicsSpaceId, joint.getObjectId());
    }

    public Collection<PhysicsRigidBody> getRigidBodyList() {
        return new LinkedList<>(physicsBodies.values());
    }

    public Collection<PhysicsGhostObject> getGhostObjectList() {
        return new LinkedList<>(physicsGhostObjects.values());
    }

    public Collection<PhysicsCharacter> getCharacterList() {
        return new LinkedList<>(physicsCharacters.values());
    }

    public Collection<PhysicsJoint> getJointList() {
        return new LinkedList<>(physicsJoints.values());
    }

    public Collection<PhysicsVehicle> getVehicleList() {
        return new LinkedList<>(physicsVehicles.values());
    }

    /**
     * Alter the gravitational acceleration acting on newly-added bodies.
     * <p>
     * Whenever a rigid body is added to a space, the body's gravity gets set to
     * that of the space. Thus it makes sense to set space's vector before
     * adding any bodies to the space.
     *
     * @param gravity the desired acceleration vector (not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        this.gravity.set(gravity);
        setGravity(physicsSpaceId, gravity);
    }

    private native void setGravity(long spaceId, Vector3f gravity);

    /**
     * gravity-acceleration vector (default is 9.81 in the -Y direction,
     * corresponding to Earth-normal in MKS units)
     */
    private final Vector3f gravity = new Vector3f(0, -9.81f, 0);

    /**
     * Copy the gravitational acceleration acting on newly-added bodies.
     *
     * @param gravity storage for the result (not null, modified)
     * @return acceleration (in the vector provided)
     */
    public Vector3f getGravity(Vector3f gravity) {
        return gravity.set(this.gravity);
    }

    /**
     * Register the specified tick listener with this space.
     * <p>
     * Tick listeners are notified before and after each physics step. A physics
     * step is not necessarily the same as a frame; it is more influenced by the
     * accuracy of the physics space.
     *
     * @see #setAccuracy(float)
     *
     * @param listener the listener to register (not null)
     */
    public void addTickListener(PhysicsTickListener listener) {
        Validate.nonNull(listener, "listener");
        assert !tickListeners.contains(listener);

        tickListeners.add(listener);
    }

    /**
     * De-register the specified tick listener.
     *
     * @see #addTickListener(com.jme3.bullet.PhysicsTickListener)
     * @param listener the listener to de-register (not null)
     */
    public void removeTickListener(PhysicsTickListener listener) {
        Validate.nonNull(listener, "listener");
        assert tickListeners.contains(listener);

        tickListeners.remove(listener);
    }

    /**
     * Register the specified collision listener with this space.
     * <p>
     * Collision listeners are notified when collisions occur in the space.
     *
     * @param listener the listener to register (not null, alias created)
     */
    public void addCollisionListener(PhysicsCollisionListener listener) {
        Validate.nonNull(listener, "listener");
        assert !collisionListeners.contains(listener);

        collisionListeners.add(listener);
    }

    /**
     * De-register the specified collision listener.
     *
     * @see
     * #addCollisionListener(com.jme3.bullet.collision.PhysicsCollisionListener)
     * @param listener the listener to de-register (not null)
     */
    public void removeCollisionListener(PhysicsCollisionListener listener) {
        Validate.nonNull(listener, "listener");
        assert collisionListeners.contains(listener);

        collisionListeners.remove(listener);
    }

    /**
     * Register the specified collision-group listener with the specified
     * collision group of this space.
     * <p>
     * Such a listener can disable collisions when they occur. There can be only
     * one listener per collision group per space.
     *
     * @param listener the listener to register (not null)
     * @param collisionGroup which group it should listen for (bit mask with
     * exactly one bit set)
     */
    public void addCollisionGroupListener(
            PhysicsCollisionGroupListener listener, int collisionGroup) {
        Validate.nonNull(listener, "listener");
        assert collisionGroupListeners.get(collisionGroup) == null;
        assert Integer.bitCount(collisionGroup) == 1 : collisionGroup;

        collisionGroupListeners.put(collisionGroup, listener);
    }

    /**
     * De-register the specified collision-group listener.
     *
     * @see
     * #addCollisionGroupListener(com.jme3.bullet.collision.PhysicsCollisionGroupListener,
     * int)
     * @param collisionGroup the group of the listener to de-register (bit mask
     * with exactly one bit set)
     */
    public void removeCollisionGroupListener(int collisionGroup) {
        assert collisionGroupListeners.get(collisionGroup) != null;
        assert Integer.bitCount(collisionGroup) == 1 : collisionGroup;

        collisionGroupListeners.remove(collisionGroup);
    }

    /**
     * Performs a ray collision test and returns the results as a list of
     * PhysicsRayTestResults ordered by it hitFraction (lower to higher)
     *
     * @param from the starting location
     * @param to the ending location
     * @return a new list of results
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to) {
        List<PhysicsRayTestResult> results = new ArrayList<>();
        rayTest(from, to, results);

        return results;
    }

    /**
     * Performs a ray collision test and returns the results as a list of
     * PhysicsRayTestResults without performing any sort operation
     *
     * @param from the starting location
     * @param to the ending location
     * @return a new list of results
     */
    public List rayTestRaw(Vector3f from, Vector3f to) {
        List<PhysicsRayTestResult> results = new ArrayList<>();
        rayTestRaw(from, to, results);

        return results;
    }

    /**
     * Sets m_flags for raytest, see
     * https://code.google.com/p/bullet/source/browse/trunk/src/BulletCollision/NarrowPhaseCollision/btRaycastCallback.h
     * for possible options. Defaults to using the faster, approximate raytest.
     *
     * @param flags which flags to set
     */
    public void setRayTestFlags(int flags) {
        rayTestFlags = flags;
    }

    /**
     * Gets m_flags for raytest, see
     * https://code.google.com/p/bullet/source/browse/trunk/src/BulletCollision/NarrowPhaseCollision/btRaycastCallback.h
     * for possible options.
     *
     * @return rayTestFlags
     */
    public int getRayTestFlags() {
        return rayTestFlags;
    }

    private static Comparator<PhysicsRayTestResult> hitFractionComparator = new Comparator<PhysicsRayTestResult>() {
        @Override
        public int compare(PhysicsRayTestResult r1, PhysicsRayTestResult r2) {
            float comp = r1.getHitFraction() - r2.getHitFraction();
            return comp > 0 ? 1 : -1;
        }
    };

    /**
     * Performs a ray collision test and returns the results as a list of
     * PhysicsRayTestResults ordered by it hitFraction (lower to higher)
     *
     * @param from the starting location
     * @param to the ending location
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to,
            List<PhysicsRayTestResult> results) {
        results.clear();
        rayTest_native(from, to, physicsSpaceId, results, rayTestFlags);

        Collections.sort(results, hitFractionComparator);
        return results;
    }

    /**
     * Performs a ray collision test and returns the results as a list of
     * PhysicsRayTestResults without performing any sort operation
     *
     * @param from the starting location
     * @param to the ending location
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsRayTestResult> rayTestRaw(Vector3f from, Vector3f to,
            List<PhysicsRayTestResult> results) {
        results.clear();
        rayTest_native(from, to, physicsSpaceId, results, rayTestFlags);
        return results;
    }

    public native void rayTest_native(Vector3f from, Vector3f to,
            long physicsSpaceId, List<PhysicsRayTestResult> results, int flags);

    /**
     * Performs a sweep collision test and returns the results as a list of
     * PhysicsSweepTestResults
     *
     * You have to use different Transforms for start and end (at least distance
     * &gt; 0.4f). SweepTest will not see a collision if it starts INSIDE an
     * object and is moving AWAY from its center.
     *
     * @param shape the shape to use
     * @param start the starting transform
     * @param end the ending transform
     * @return a new list of results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape,
            Transform start, Transform end) {
        List<PhysicsSweepTestResult> results = new LinkedList<>();
        sweepTest(shape, start, end, results);
        return results;
    }

    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape,
            Transform start, Transform end,
            List<PhysicsSweepTestResult> results) {
        return sweepTest(shape, start, end, results, 0.0f);
    }

    public native void sweepTest_native(long shape, Transform from,
            Transform to, long physicsSpaceId,
            List<PhysicsSweepTestResult> results, float allowedCcdPenetration);

    /**
     * Performs a sweep collision test and returns the results as a list of
     * PhysicsSweepTestResults
     * <p>
     * You have to use different Transforms for start and end (at least distance
     * &gt; allowedCcdPenetration). SweepTest will not see a collision if it
     * starts INSIDE an object and is moving AWAY from its center.
     *
     * @param shape the shape to use
     * @param start the starting transform
     * @param end the ending transform
     * @param results the list to hold results (not null, modified)
     * @param allowedCcdPenetration true&rarr;allow, false&rarr;disallow
     * @return results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape,
            Transform start, Transform end,
            List<PhysicsSweepTestResult> results, float allowedCcdPenetration) {
        results.clear();
        sweepTest_native(shape.getObjectId(), start, end, physicsSpaceId,
                results, allowedCcdPenetration);
        return results;
    }

    /**
     * Destroy the current PhysicsSpace so that a new one can be created.
     */
    public void destroy() {
        physicsBodies.clear();
        physicsJoints.clear();
    }

    /**
     * // * used internally //
     *
     * @return the dynamicsWorld //
     */
    public long getSpaceId() {
        return physicsSpaceId;
    }

    public BroadphaseType getBroadphaseType() {
        return broadphaseType;
    }

    public void setBroadphaseType(BroadphaseType broadphaseType) {
        this.broadphaseType = broadphaseType;
    }

    /**
     * Read the maximum number of extra steps.
     *
     * @return number of steps
     */
    public int maxSubSteps() {
        return maxSubSteps;
    }

    /**
     * Set the maximum number of extra steps that will be used when the render
     * fps is below the physics fps. Doing this maintains determinism in
     * physics. For example a maximum number of 2 can compensate for framerates
     * as low as 30fps when the physics has the default accuracy of 60 fps. Note
     * that setting this value too high can cause the physics to drive down its
     * own fps in case it's overloaded.
     *
     * @param steps The maximum number of extra steps, default is 4.
     */
    public void setMaxSubSteps(int steps) {
        maxSubSteps = steps;
    }

    /**
     * Read the current accuracy of the physics simulation.
     *
     * @return the current value (in seconds)
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Alter the accuracy (time step) of the physics simulation. In general, the
     * smaller the time step, the more accurate (and compute-intensive) the
     * simulation will be.
     *
     * @param accuracy (in seconds, &gt;0, default=1/60)
     */
    public void setAccuracy(float accuracy) {
        Validate.positive(accuracy, "accuracy");
        this.accuracy = accuracy;
    }

    /**
     * Access the minimum coordinate values for this space.
     *
     * @return the pre-existing vector
     */
    public Vector3f getWorldMin() {
        return worldMin;
    }

    /**
     * Alter the minimum coordinate values for this space. (only affects
     * AXIS_SWEEP broadphase algorithms)
     *
     * @param worldMin the desired minimum coordinate values (not null,
     * unaffected)
     */
    public void setWorldMin(Vector3f worldMin) {
        this.worldMin.set(worldMin);
    }

    /**
     * Access the maximum coordinate values for this space.
     *
     * @return the pre-existing vector
     */
    public Vector3f getWorldMax() {
        return worldMax;
    }

    /**
     * Alter the maximum coordinate values for this space. (only affects
     * AXIS_SWEEP broadphase algorithms)
     *
     * @param worldMax the desired maximum coordinate values (not null,
     * unaffected)
     */
    public void setWorldMax(Vector3f worldMax) {
        this.worldMax.set(worldMax);
    }

    /**
     * Set the number of iterations used by the contact solver.
     * <p>
     * The default is 10. Use 4 for low quality, 20 for high quality.
     *
     * @param numIterations the number of iterations (&ge;1)
     */
    public void setSolverNumIterations(int numIterations) {
        Validate.positive(numIterations, "number of iterations");

        this.solverNumIterations = numIterations;
        setSolverNumIterations(physicsSpaceId, numIterations);
    }

    /**
     * Read the number of iterations used by the contact-and-constraint solver.
     *
     * @return the number of iterations used
     */
    public int getSolverNumIterations() {
        return solverNumIterations;
    }

    private native void setSolverNumIterations(long physicsSpaceId,
            int numIterations);

    public static native void initNativePhysics();

    /**
     * Enumerate Broadphase collision-detection algorithms.
     */
    public enum BroadphaseType {
        /**
         * the basic algorithm
         */
        SIMPLE,
        /**
         * better algorithm, needs world bounds, max of 16384 objects
         */
        AXIS_SWEEP_3,
        /**
         * better algorithm, needs world bounds, max of 65536 objects
         */
        AXIS_SWEEP_3_32,
        /**
         * algorithm allowing quicker addition/removal of physics objects
         */
        DBVT;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.log(Level.FINE, "Finalizing PhysicsSpace {0}",
                Long.toHexString(physicsSpaceId));
        finalizeNative(physicsSpaceId);
    }

    private native void finalizeNative(long objectId);
}

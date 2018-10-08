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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.control.ragdoll.JointPreset;
import com.jme3.bullet.control.ragdoll.PhysicsBoneLink;
import com.jme3.bullet.control.ragdoll.RagdollUtils;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;

/**
 * <strong>This control is still a WIP, use it at your own risk</strong><br> To
 * use this control you need a model with an AnimControl and a
 * SkeletonControl.<br> This should be the case if you imported an animated
 * model from Ogre or blender.<br> Note enabling/disabling the control
 * add/removes it from the physics space<br>
 * <p>
 * This control creates collision shapes for each bones of the skeleton when you
 * invoke spatial.addControl(ragdollControl). <ul> <li>The shape is
 * HullCollision shape based on the vertices associated with each bone</li>
 * <li>If you don't want each bone to be a collision shape, you can specify what
 * bones to use by using the addBoneName method<br> By using this method, bone
 * that are not used to create a shape, are "merged" to their parent to create
 * the collision shape. </li>
 * </ul>
 * <p>
 * There are 2 modes for this control: <ul> <li><strong>The kinematic modes
 * :</strong><br> this is the default behavior, this means that the collision
 * shapes of the body are able to interact with physics enabled objects. in this
 * mode physics shapes follow the motion of the animated skeleton (for example
 * animated by a key framed animation) this mode is enabled by calling
 * setKinematicMode(); </li> <li><strong>The ragdoll modes:</strong><br> To
 * enable this behavior, you need to invoke the setRagdollMode() method. In this
 * mode the character is entirely controlled by physics, so it will fall under
 * the gravity and move if any force is applied to it.</li>
 * </ul>
 *
 * TODO handle applyLocal
 *
 * @author Normen Hansen and Rémy Bouquet (Nehon)
 */
public class KinematicRagdollControl
        extends AbstractPhysicsControl
        implements PhysicsCollisionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(KinematicRagdollControl.class.getName());
    /**
     * magic bone name to refer to the model's torso
     */
    final public static String torsoFakeBoneName = ">>KRC torso<<";
    // *************************************************************************
    // fields

    /**
     * list of registered collision listeners
     */
    private List<RagdollCollisionListener> listeners;
    /**
     * map bone names to joint presets for createSpatialData()
     */
    private Map<String, JointPreset> jointMap = new HashMap<>();
    // TODO threshold for each bone
    /**
     * map bone names to simulation objects
     */
    private Map<String, PhysicsBoneLink> boneLinks = new HashMap<>();
    private Vector3f modelLocation = new Vector3f(); // TODO temporary
    private Quaternion modelOrientation = new Quaternion(); // TODO temporary
    /**
     * rigid body for the torso
     */
    private PhysicsRigidBody torsoRigidBody = null;
    /**
     * skeleton being controlled
     */
    private Skeleton skeleton = null;
    private Vector3f initScale = null;
    /**
     * mode of operation (not null, default=Kinematic)
     */
    private Mode mode = Mode.Kinematic;
    /**
     * true IFF recently transitioned to kinematic mode and still blending
     */
    private boolean isBlending = false;
    /**
     * elapsed time since switching to kinematic mode, if isBlending is true (in
     * seconds, &ge;0)
     */
    private float blendProgress = 0f;
    /**
     * blend interval after switching to kinematic mode (in seconds, &ge;0)
     */
    private float blendTime = 1f;
    /**
     * minimum applied impulse for a collision even to be dispatched to
     * listeners (default=0)
     */
    private float eventDispatchImpulseThreshold = 0f;
    /**
     * mass of the torso TODO specify in constructor
     */
    private float torsoMass = 15f;
    /**
     * accumulate total mass of ragdoll when control is added to a scene
     */
    private float totalMass = 0f;
    /**
     * map from IK bone names to goal locations
     */
    private Map<String, Vector3f> ikTargets = new HashMap<>();
    /**
     * map from IK bone names to chain depths
     */
    private Map<String, Integer> ikChainDepth = new HashMap<>();
    /**
     * rotational speed for inverse kinematics (radians per second, default=7)
     */
    private float ikRotSpeed = 7f;
    /**
     * viscous limb-damping ratio (0&rarr;no damping, 1&rarr;critically damped,
     * default=0.6)
     */
    private float limbDamping = 0.6f;
    /**
     * distance threshold for inverse kinematics (default=0.1)
     */
    private float IKThreshold = 0.1f;
    /**
     * control that's responsible for skinning
     */
    private SkeletonControl skeletonControl = null;
    /**
     * spatial with the mesh-coordinate transform
     */
    private Spatial transformer = null;
    /**
     * transform mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;

    /**
     * Enumerate joint-control modes for this control.
     */
    public enum Mode {
        /**
         * collision shapes follow the movements of bones in the skeleton
         */
        Kinematic,
        /**
         * skeleton is controlled by Bullet dynamics (gravity and collisions)
         */
        Ragdoll,
        /**
         * skeleton is controlled by inverse-kinematic targets
         */
        IK
    }
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled, Kinematic control with no bones.
     */
    public KinematicRagdollControl() {
    }
    // *************************************************************************

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is added to a scene. Do not invoke directly
     * from user code.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        if (mode == Mode.IK) {
            ikUpdate(tpf);
        } else if (mode == Mode.Ragdoll) {
            // Update each bone's position and rotation based on dynamics.
            ragDollUpdate(tpf);
        } else {
            kinematicUpdate(tpf);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the skeleton in Ragdoll mode, based on Bullet dynamics.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void ragDollUpdate(float tpf) {
        torsoDynamicUpdate();

        TempVars vars = TempVars.get();
        Vector3f location = vars.vect1;
        Quaternion orientation = vars.quat1;

        Transform meshToWorld = transformer.getWorldTransform();
        Quaternion worldToMesh = meshToWorld.getRotation().inverse();

        for (PhysicsBoneLink link : boneLinks.values()) {
            PhysicsRigidBody body = link.getRigidBody();

            // Start with the bone's location in world coordinates.
            Vector3f worldLocation = body.getMotionState().getWorldLocation();
            // transform into mesh coordinates
            meshToWorld.transformInverseVector(worldLocation, location);

            // Start with the bone's orientation in world coordinates.
            Quaternion worldOrientation
                    = body.getMotionState().getWorldRotationQuat();
            // transform into mesh coordinates
            orientation.set(worldOrientation);
            orientation.multLocal(link.originalOrientation(null));
            worldToMesh.mult(orientation, orientation);
            orientation.normalizeLocal();

            Bone bone = link.getBone();
            RagdollUtils.setTransform(bone, location, orientation, false,
                    jointMap.keySet());
        }
        vars.release();
    }

    /**
     * Update this control in Kinematic mode, based on the model's transform and
     * the skeleton's pose.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    protected void kinematicUpdate(float tpf) {
        torsoKinematicUpdate();

        TempVars vars = TempVars.get();
        Quaternion tmpRot1 = vars.quat1;
        Quaternion tmpRot2 = vars.quat2;
        Vector3f position = vars.vect1;
        for (PhysicsBoneLink link : boneLinks.values()) {
//            if(link.usedbyIK){
//                continue;
//            }
            //if blending, keyframed animation is updating the skeleton,
            //but to allow smooth transition, we blend this transformation with the saved position of the ragdoll
            if (isBlending) {
                Vector3f position2 = vars.vect2;
                //initialize tmp vars with the start position/rotation of the ragdoll
                link.copyBlendStart(position, tmpRot1);

                //interpolate between ragdoll position/rotation and keyframed position/rotation
                tmpRot2.set(tmpRot1);
                tmpRot2.nlerp(link.getBone().getModelSpaceRotation(),
                        blendProgress / blendTime);
                position2.set(position).interpolateLocal(
                        link.getBone().getModelSpacePosition(),
                        blendProgress / blendTime);
                tmpRot1.set(tmpRot2);
                position.set(position2);

                //update bone transforms
                RagdollUtils.setTransform(link.getBone(), position, tmpRot1,
                        true, jointMap.keySet());
            }
            //set bone transforms to the ragdoll
            matchPhysicObjectToBone(link, position, tmpRot1);
            modelLocation.set(spatial.getLocalTranslation());
        }
        vars.release();

        if (isBlending) {
            blendProgress += tpf;
            if (blendProgress > blendTime) {
                isBlending = false;
            }
        }
    }

    /**
     * Update this control in IK mode, based on IK targets.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    private void ikUpdate(float tpf) {
        TempVars vars = TempVars.get();

        Quaternion tmpRot1 = vars.quat1;
        Quaternion[] tmpRot2 = new Quaternion[]{vars.quat2, new Quaternion()};

        Iterator<String> it = ikTargets.keySet().iterator();
        float distance;
        Bone bone;
        String boneName;
        while (it.hasNext()) {

            boneName = it.next();
            bone = (Bone) boneLinks.get(boneName).getBone();
            if (!bone.hasUserControl()) {
                logger.log(Level.FINE, "{0} doesn't have user control",
                        boneName);
                continue;
            }
            distance = bone.getModelSpacePosition().distance(
                    ikTargets.get(boneName));
            if (distance < IKThreshold) {
                logger.log(Level.FINE, "Distance is close enough");
                continue;
            }
            int depth = 0;
            int maxDepth = ikChainDepth.get(bone.getName());
            float changeAmount = tpf * (float) FastMath.sqrt(distance);
            updateBone(boneLinks.get(boneName), changeAmount, tmpRot1,
                    tmpRot2, bone, ikTargets.get(boneName), depth, maxDepth);

            Vector3f position = vars.vect1;

            for (PhysicsBoneLink link : boneLinks.values()) {
                matchPhysicObjectToBone(link, position, tmpRot1);
            }
        }
        vars.release();
    }

    /**
     * Update a bone and its ancestors in IK mode. Note: recursive!
     *
     * @param link the bone link for the affected bone (may be null)
     * @param changeAmount amount of change desired (&ge;0)
     * @param tmpRot1 temporary storage used in calculations (not null)
     * @param tmpRot2 temporary storage used in calculations (not null)
     * @param tipBone (not null)
     * @param target the location target in model space (not null, unaffected)
     * @param depth depth of the recursion (&ge;0)
     * @param maxDepth recursion limit (&ge;0)
     */
    private void updateBone(PhysicsBoneLink link, float changeAmount,
            Quaternion tmpRot1, Quaternion[] tmpRot2, Bone tipBone,
            Vector3f target, int depth, int maxDepth) {
        Validate.nonNegative(changeAmount, "change amount");

        if (link == null || link.getBone().getParent() == null) {
            return;
        }
        Quaternion preQuat = link.getBone().getLocalRotation();
        Vector3f vectorAxis;

        float[] measureDist = new float[]{Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY};
        for (int dirIndex = 0; dirIndex < 3; dirIndex++) {
            if (dirIndex == PhysicsSpace.AXIS_X) {
                vectorAxis = Vector3f.UNIT_Z;
            } else if (dirIndex == PhysicsSpace.AXIS_Y) {
                vectorAxis = Vector3f.UNIT_X;
            } else {
                vectorAxis = Vector3f.UNIT_Y;
            }

            for (int posOrNeg = 0; posOrNeg < 2; posOrNeg++) {
                float rot = ikRotSpeed * changeAmount / (link.getRigidBody().getMass() * 2);

                rot = FastMath.clamp(rot,
                        link.getJoint().getRotationalLimitMotor(dirIndex).getLoLimit(),
                        link.getJoint().getRotationalLimitMotor(dirIndex).getHiLimit());
                tmpRot1.fromAngleAxis(rot, vectorAxis);
//                tmpRot1.fromAngleAxis(rotSpeed * tpf / (link.rigidBody.getMass() * 2), vectorAxis);

                tmpRot2[posOrNeg] = link.getBone().getLocalRotation().mult(tmpRot1);
                tmpRot2[posOrNeg].normalizeLocal();

                ikRotSpeed = -ikRotSpeed;

                link.getBone().setLocalRotation(tmpRot2[posOrNeg]);
                link.getBone().update();
                measureDist[posOrNeg]
                        = tipBone.getModelSpacePosition().distance(target);
                link.getBone().setLocalRotation(preQuat);
            }

            if (measureDist[0] < measureDist[1]) {
                link.getBone().setLocalRotation(tmpRot2[0]);
            } else if (measureDist[0] > measureDist[1]) {
                link.getBone().setLocalRotation(tmpRot2[1]);
            }

        }
        link.getBone().getLocalRotation().normalizeLocal();

        link.getBone().update();
        if (link.getBone().getParent() != null && depth < maxDepth) {
            updateBone(boneLinks.get(link.getBone().getParent().getName()),
                    0.5f * changeAmount, tmpRot1, tmpRot2, tipBone,
                    target, depth + 1, maxDepth);
        }
    }

    /**
     * Alter the transforms of a rigidBody to match the transforms of a bone.
     * This is used to make the ragdoll follow animated motion in Kinematic mode
     * TODO rename and move to PhysicsBoneLink
     *
     * @param link the bone link connecting the bone and the rigidBody
     * @param location temporary vector used in calculations (not null)
     * @param orientation temporary quaternion used in calculations (not null)
     */
    protected void matchPhysicObjectToBone(PhysicsBoneLink link,
            Vector3f location, Quaternion orientation) {
        Transform meshToWorld = transformer.getWorldTransform();

        // Compute the location of the bone in world coordinates.
        Bone bone = link.getBone();
        Vector3f meshLocation = bone.getModelSpacePosition();
        meshToWorld.transformVector(meshLocation, location);

        // Compute the orientation of the bone in world coordinates.
        orientation.set(bone.getModelSpaceRotation());
        orientation.multLocal(bone.getModelBindInverseRotation());
        meshToWorld.getRotation().mult(orientation, orientation);
        orientation.normalizeLocal();

        // Compute the scale of the bone in world coordinates.
        Vector3f scale = bone.getModelSpaceScale().clone();
        scale.multLocal(meshToWorld.getScale());

        // Update the location and rotation of the physics body.
        PhysicsRigidBody body = link.getRigidBody();
        body.setPhysicsLocation(location);
        body.setPhysicsRotation(orientation);

        PhysicsSpace space = getPhysicsSpace();
        body.setPhysicsScale(scale, space);
    }

    /**
     * Rebuild the ragdoll. This is useful if you applied scale on the ragdoll
     * after it was initialized. Same as re-attaching.
     */
    public void reBuild() {
        if (spatial == null) {
            return;
        }
        removeSpatialData(spatial);
        createSpatialData(spatial);
    }

    /**
     * Create spatial-dependent data. Invoked when the control is added to a
     * spatial.
     *
     * @param controlledSpatial the controlled spatial (not null)
     */
    @Override
    protected void createSpatialData(Spatial controlledSpatial) {
        Validate.nonNull(controlledSpatial, "controlled spatial");

        spatial = controlledSpatial;
        skeletonControl = spatial.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            throw new IllegalArgumentException(
                    "The controlled spatial must have a SkeletonControl. Make sure the control is there and not on a subnode.");
        }
        skeleton = skeletonControl.getSkeleton();
        /*
         * Remove the SkeletonControl and readd it to ensure it will be
         * updated after this control.
         */
        spatial.removeControl(skeletonControl);
        spatial.addControl(skeletonControl);
        /*
         * Analyze the model's coordinate systems.
         */
        transformer = MySpatial.findAnimatedGeometry(spatial);
        if (transformer == null) {
            transformer = spatial;
        }
        Spatial loopSpatial = transformer;
        Transform modelToMesh = new Transform();
        while (loopSpatial != spatial) {
            Transform localTransform = loopSpatial.getLocalTransform();
            modelToMesh.combineWithParent(localTransform);
            loopSpatial = loopSpatial.getParent();
        }
        meshToModel = modelToMesh.invert();
        initScale = spatial.getLocalScale().clone();
        /*
         * Map bone indices to names of linked bones.
         */
        String[] tempLbNames = linkedBoneNameArray();

        Map<String, List<Vector3f>> coordsMap = coordsMap(tempLbNames);
        /*
         * Create a rigid body for the torso.
         */
        List<Vector3f> list = coordsMap.get(torsoFakeBoneName);
        CollisionShape torsoShape
                = createShape(new Transform(), new Vector3f(), list);
        totalMass += torsoMass;
        torsoRigidBody = new PhysicsRigidBody(torsoShape, torsoMass);
        torsoRigidBody.setDamping(limbDamping, limbDamping);
        torsoRigidBody.setKinematic(mode == Mode.Kinematic);
        torsoRigidBody.setUserObject(this);

        scanSpatial(coordsMap);

        if (added) {
            addPhysics(getPhysicsSpace());
        }

        logger.log(Level.FINE, "Created ragdoll for skeleton {0}", skeleton);
    }

    /**
     * Destroy spatial-dependent data. Invoked when this control is removed from
     * a spatial.
     *
     * @param spat the previously controlled spatial (not null)
     */
    @Override
    protected void removeSpatialData(Spatial spat) {
        if (added) {
            removePhysics(getPhysicsSpace());
        }
        boneLinks.clear();
        skeletonControl = null;
        skeleton = null;
        transformer = null;
        meshToModel = null;
    }

    /**
     * Add a bone with a joint preset to this control.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to add (not null)
     * @param jointPreset (not null)
     * @see #setJointLimit(java.lang.String, float, float, float, float, float,
     * float)
     */
    public void addBone(String boneName, JointPreset jointPreset) {
        Validate.nonNull(boneName, "name");
        Validate.nonNull(jointPreset, "joint preset");
        if (spatial != null) {
            throw new IllegalStateException(
                    "Cannot add bone while added to a spatial.");
        }

        jointMap.put(boneName, jointPreset);
    }

    /**
     * Add a bone name to this control and preset the bone's joint to "no
     * motion". Repeated invocations of this method can be used to specify which
     * bones to use when generating collision shapes.
     * <p>
     * Allowed only when the control is NOT added to a spatial.
     *
     * @param boneName the name of the bone to add (not null)
     */
    public void addBoneName(String boneName) {
        Validate.nonNull(boneName, "name");
        addBone(boneName, new JointPreset());
    }

    /**
     * Generate physics shapes and bone links for the skeleton.
     *
     * @param coordsMap map from bone names to vertex positions (not null)
     */
    protected void scanSpatial(Map<String, List<Vector3f>> coordsMap) {
        skeleton.resetAndUpdate();
        for (Bone rootBone : skeleton.getRoots()) {
            logger.log(Level.FINE, "Found root bone in skeleton {0}", skeleton);
            boneRecursion(rootBone, torsoRigidBody, 1, coordsMap);
        }
    }

    /**
     * Generate a physics shape and bone links for the specified bone. Note:
     * recursive!
     *
     * @param bone the bone to be linked (not null)
     * @param parent the body linked to the parent bone (not null)
     * @param reccount depth of the recursion (&ge;1)
     * @param coordsMap map from bone names to vertex positions (not null)
     */
    protected void boneRecursion(Bone bone, PhysicsRigidBody parent,
            int reccount, Map<String, List<Vector3f>> coordsMap) {
        PhysicsRigidBody parentShape = parent;
        String boneName = bone.getName();
        if (jointMap.containsKey(boneName)) {
            /*
             * Create the collision shape.
             */
            Transform invTransform = bone.getModelBindInverseTransform();
            List<Vector3f> vertexLocations = coordsMap.get(boneName);
            Vector3f meshLocation = bone.getModelSpacePosition();
            CollisionShape shape
                    = createShape(invTransform, meshLocation, vertexLocations);

            float limbMass = torsoMass / (float) reccount;
            assert limbMass > 0f : limbMass;
            totalMass += limbMass;
            PhysicsRigidBody shapeNode = new PhysicsRigidBody(shape, limbMass);
            shapeNode.setDamping(limbDamping, limbDamping);
            shapeNode.setKinematic(mode == Mode.Kinematic);

            SixDofJoint joint = null;
            if (parent != null) {
                //get joint position for parent
                Vector3f posToParent = new Vector3f();
                if (bone.getParent() != null) {
                    bone.getModelSpacePosition()
                            .subtract(bone.getParent().getModelSpacePosition(), posToParent)
                            .multLocal(initScale);
                }

                joint = new SixDofJoint(parent, shapeNode,
                        posToParent, new Vector3f(0f, 0f, 0f), true);

                JointPreset preset = jointMap.get(boneName);
                preset.setupJoint(joint);
                joint.setCollisionBetweenLinkedBodies(false);
            }

            PhysicsBoneLink link
                    = new PhysicsBoneLink(transformer, bone, shapeNode, "");
            link.setJoint(joint);
            boneLinks.put(bone.getName(), link);
            shapeNode.setUserObject(link);
            parentShape = shapeNode;
        }

        for (Bone childBone : bone.getChildren()) {
            boneRecursion(childBone, parentShape, reccount + 1, coordsMap);
        }
    }

    /**
     * Alter the limits of the joint connecting the specified bone to its
     * parent.
     * <p>
     * Allowed only when the control IS added to a spatial.
     *
     * @param boneName the name of the bone
     * @param maxX the maximum rotation on the X axis (in radians)
     * @param minX the minimum rotation on the X axis (in radians)
     * @param maxY the maximum rotation on the Y axis (in radians)
     * @param minY the minimum rotation on the Y axis (in radians)
     * @param maxZ the maximum rotation on the Z axis (in radians)
     * @param minZ the minimum rotation on the Z axis (in radians)
     * @see #addBone(java.lang.String,
     * com.jme3.bullet.control.ragdoll.JointPreset)
     */
    public void setJointLimit(String boneName, float maxX, float minX,
            float maxY, float minY, float maxZ, float minZ) {
        if (spatial == null) {
            throw new IllegalStateException(
                    "Cannot set limits unless added to a spatial.");
        }

        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            RagdollUtils.setJointLimit(link.getJoint(),
                    maxX, minX, maxY, minY, maxZ, minZ);
        } else {
            logger.log(Level.WARNING,
                    "Not joint was found for bone {0}. make sure you call spatial.addControl(ragdoll) before setting joints limit", boneName);
        }
    }

    /**
     * Return the joint between the specified bone and its parent. This returns
     * null if it's invoked before adding the control to a spatial
     *
     * @param boneName the name of the bone
     * @return the joint between the given bone and its parent
     */
    public SixDofJoint getJoint(String boneName) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            return link.getJoint();
        } else {
            logger.log(Level.WARNING,
                    "No joint was found for bone {0}. Make sure you call spatial.addControl(ragdoll) before accessing joints.", boneName);
            return null;
        }
    }

    @Override
    protected void setPhysicsLocation(Vector3f vec) {
        torsoRigidBody.setPhysicsLocation(vec);
    }

    @Override
    protected void setPhysicsRotation(Quaternion quat) {
        torsoRigidBody.setPhysicsRotation(quat);
    }

    /**
     * Add all managed physics objects to the specified space.
     *
     * @param space which physics space to add to (not null)
     */
    @Override
    protected void addPhysics(PhysicsSpace space) {
        space.add(torsoRigidBody);
        for (PhysicsBoneLink physicsBoneLink : boneLinks.values()) {
            space.add(physicsBoneLink.getRigidBody());
            space.add(physicsBoneLink.getJoint());
        }
        space.addCollisionListener(this);
    }

    /**
     * Remove all managed physics objects from the specified space.
     *
     * @param space which physics space to remove from (not null)
     */
    @Override
    protected void removePhysics(PhysicsSpace space) {
        space.remove(torsoRigidBody);
        for (PhysicsBoneLink physicsBoneLink : boneLinks.values()) {
            space.remove(physicsBoneLink.getJoint());
            space.remove(physicsBoneLink.getRigidBody());
        }
        space.removeCollisionListener(this);
    }

    /**
     * For internal use only: callback for collision events.
     *
     * @param event (not null)
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {
        if (event.getNodeA() == null && event.getNodeB() == null) {
            return;
        }
        /*
         * Determine which bone was involved (if any) and also the
         * other collision object involved.
         */
        boolean krcInvolved = false;
        Bone bone = null;
        PhysicsCollisionObject otherPco = null;
        PhysicsCollisionObject pcoA = event.getObjectA();
        PhysicsCollisionObject pcoB = event.getObjectB();

        Object userA = pcoA.getUserObject();
        Object userB = pcoB.getUserObject();
        if (userA instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) userA;
            if (link != null) {
                krcInvolved = true;
                bone = link.getBone();
                otherPco = pcoB;
            }
        } else if (userA == this) {
            krcInvolved = true;
            bone = null;
            otherPco = pcoB;
        } else if (userB instanceof PhysicsBoneLink) {
            PhysicsBoneLink link = (PhysicsBoneLink) userB;
            if (link != null) {
                krcInvolved = true;
                bone = link.getBone();
                otherPco = pcoA;
            }
        } else if (userB == this) {
            krcInvolved = true;
            bone = null;
            otherPco = pcoA;
        }
        /*
         * Discard low-impulse collisions.
         */
        if (event.getAppliedImpulse() < eventDispatchImpulseThreshold) {
            return;
        }
        /*
         * Dispatch an event if this control was involved in the collision.
         */
        if (krcInvolved && listeners != null) {
            for (RagdollCollisionListener listener : listeners) {
                listener.collide(bone, otherPco, event);
            }
        }
    }

    /**
     * Enable or disable the ragdoll behavior. if ragdollEnabled is true, the
     * character motion will only be powered by physics else, the character will
     * be animated by the keyframe animation, but will be able to physically
     * interact with its physics environment
     *
     * @param mode an enum value (not null)
     */
    protected void setMode(Mode mode) {
        this.mode = mode;
        AnimControl animControl = spatial.getControl(AnimControl.class);
        animControl.setEnabled(mode == Mode.Kinematic);

        torsoRigidBody.setKinematic(mode == Mode.Kinematic);
        if (mode != Mode.IK) {
            TempVars vars = TempVars.get();

            for (PhysicsBoneLink link : boneLinks.values()) {
                link.getRigidBody().setKinematic(mode == Mode.Kinematic);
                if (mode == Mode.Ragdoll) {
                    Quaternion tmpRot1 = vars.quat1;
                    Vector3f position = vars.vect1;
                    //Ensure that the ragdoll is at the correct place.
                    matchPhysicObjectToBone(link, position, tmpRot1);
                }

            }
            vars.release();

            MySkeleton.setUserControl(skeleton, mode == Mode.Ragdoll);
        }
    }

    /**
     * Smoothly blend from Ragdoll mode to Kinematic mode. This is useful to
     * blend ragdoll actual position to a keyframe animation, for example.
     *
     * @param blendTime the blending time between ragdoll to anim (in seconds)
     */
    public void blendToKinematicMode(float blendTime) {
        if (mode == Mode.Kinematic) {
            // already in kinematic mode
            return;
        }

        mode = Mode.Kinematic;
        isBlending = true;
        this.blendTime = blendTime;
        blendProgress = 0f;

        AnimControl animControl = spatial.getControl(AnimControl.class);
        animControl.setEnabled(true);

        for (PhysicsBoneLink link : boneLinks.values()) {
            link.startBlend();
        }

        MySkeleton.setUserControl(skeleton, false);
    }

    /**
     * Put the control into Kinematic mode. In this mode, the collision shapes
     * follow the movements of the skeleton while interacting with the physics
     * environment.
     */
    public void setKinematicMode() {
        if (mode != Mode.Kinematic) {
            setMode(Mode.Kinematic);
        }
    }

    /**
     * Sets the control into Ragdoll mode The skeleton is entirely controlled by
     * physics.
     */
    public void setRagdollMode() {
        if (mode != Mode.Ragdoll) {
            setMode(Mode.Ragdoll);
        }
    }

    /**
     * Sets the control into Inverse Kinematics mode. The affected bones are
     * affected by IK. physics.
     */
    public void setIKMode() {
        if (mode != Mode.IK) {
            setMode(Mode.IK);
        }
    }

    /**
     * Read the mode of this control.
     *
     * @return an enum value (not null)
     */
    public Mode getMode() {
        assert mode != null;
        return mode;
    }

    /**
     * Add a collision listener to this control.
     *
     * @param listener (not null, alias created)
     */
    public void addCollisionListener(RagdollCollisionListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>(); // TODO use SafeArrayList
        }
        listeners.add(listener);
    }

    /**
     * Alter the ragdoll's root mass. Used only for cloning.
     *
     * @param rootMass the desired mass (&ge;0)
     */
    private void setRootMass(float rootMass) {
        torsoMass = rootMass;
    }

    /**
     * Access the physics object that represents the torso.
     *
     * @return the pre-existing object (not null)
     */
    public PhysicsRigidBody getTorso() {
        assert torsoRigidBody != null;
        return torsoRigidBody;
    }

    /**
     * Read the ragdoll's total mass. Not allowed until the control has been
     * added to a spatial.
     *
     * @return mass (&ge;0)
     */
    public float getTotalMass() {
        assert spatial != null;
        return totalMass;
    }

    /**
     * Read the ragdoll's event-dispatch impulse threshold.
     *
     * @return threshold
     */
    public float getEventDispatchImpulseThreshold() {
        return eventDispatchImpulseThreshold;
    }

    /**
     * Alter the ragdoll's event-dispatch impulse threshold.
     *
     * @param eventDispatchImpulseThreshold desired threshold
     */
    public void setEventDispatchImpulseThreshold(
            float eventDispatchImpulseThreshold) {
        this.eventDispatchImpulseThreshold = eventDispatchImpulseThreshold;
    }

    /**
     * Alter the CcdMotionThreshold of all rigid bodies in the ragdoll.
     *
     * @see PhysicsRigidBody#setCcdMotionThreshold(float)
     * @param value the desired threshold value (velocity, &gt;0) or zero to
     * disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float value) {
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setCcdMotionThreshold(value);
        }
    }

    /**
     * Alter the CcdSweptSphereRadius of all rigid bodies in the ragdoll.
     *
     * @see PhysicsRigidBody#setCcdSweptSphereRadius(float)
     * @param value the desired radius of the sphere used for continuous
     * collision detection (&ge;0)
     */
    public void setCcdSweptSphereRadius(float value) {
        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setCcdSweptSphereRadius(value);
        }
    }

    /**
     * Access the rigidBody associated with the named bone.
     *
     * @param boneName the name of the bone
     * @return the associated rigidBody.
     */
    public PhysicsRigidBody getBoneRigidBody(String boneName) {
        PhysicsBoneLink link = boneLinks.get(boneName);
        if (link != null) {
            return link.getRigidBody();
        }
        return null;
    }

    /**
     * Render this control. Invoked once per view port per frame, provided the
     * control is added to a scene. Should be invoked only by a subclass or by
     * the RenderManager.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        boneLinks = cloner.clone(boneLinks);
        jointMap = cloner.clone(jointMap);
        ikChainDepth = cloner.clone(ikChainDepth);
        ikTargets = cloner.clone(ikTargets);
        initScale = cloner.clone(initScale);
        listeners = cloner.clone(listeners);
        meshToModel = cloner.clone(meshToModel);
        modelLocation = cloner.clone(modelLocation);
        modelOrientation = cloner.clone(modelOrientation);
        skeleton = cloner.clone(skeleton);
        skeletonControl = cloner.clone(skeletonControl);
        torsoRigidBody = cloner.clone(torsoRigidBody);
        transformer = cloner.clone(transformer);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public KinematicRagdollControl jmeClone() {
        try {
            KinematicRagdollControl clone
                    = (KinematicRagdollControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Add a target for inverse kinematics.
     *
     * @param bone which bone the IK applies to (not null)
     * @param worldGoal the world coordinates of the goal (not null)
     * @param chainLength number of bones in the chain
     * @return a new instance (not null, already added to ikTargets)
     */
    public Vector3f setIKTarget(Bone bone, Vector3f worldGoal,
            int chainLength) {
        Vector3f offset = transformer.getWorldTranslation();
        Vector3f meshGoal = worldGoal.subtract(offset);
        String boneName = bone.getName();
        ikTargets.put(boneName, meshGoal);
        ikChainDepth.put(boneName, chainLength);
        int i = 0;
        while (i < chainLength + 2 && bone.getParent() != null) {
            if (!bone.hasUserControl()) {
                bone.setUserControl(true);
            }
            bone = bone.getParent();
            i++;
        }

        return meshGoal;
    }

    /**
     * Remove the inverse-kinematics target for the specified bone.
     *
     * @param bone which bone has the target (not null, modified)
     */
    public void removeIKTarget(Bone bone) {
        int depth = ikChainDepth.remove(bone.getName());
        int i = 0;
        while (i < depth + 2 && bone.getParent() != null) {
            if (bone.hasUserControl()) {
                bone.setUserControl(false);
            }
            bone = bone.getParent();
            i++;
        }
        // TODO remove from ikTargets and applyUserControl()
    }

    /**
     * Remove all inverse-kinematics targets.
     */
    public void removeAllIKTargets() {
        ikTargets.clear();
        ikChainDepth.clear();
        applyUserControl();
    }

    /**
     * Ensure that user control is enabled for any bones used by inverse
     * kinematics and disabled for any other bones.
     */
    public void applyUserControl() {
        MySkeleton.setUserControl(skeleton, false);

        if (ikTargets.isEmpty()) {
            setKinematicMode();

        } else {
            TempVars vars = TempVars.get();

            for (String ikBoneName : ikTargets.keySet()) {
                Bone bone = skeleton.getBone(ikBoneName);
                while (bone != null) {
                    String name = bone.getName();
                    PhysicsBoneLink link = boneLinks.get(name);
                    Vector3f tmpVec = vars.vect1;
                    Quaternion tmpQuat = vars.quat1;
                    matchPhysicObjectToBone(link, tmpVec, tmpQuat);

                    bone.setUserControl(true);
                    bone = bone.getParent();
                }
            }

            vars.release();
        }
    }

    /**
     * Read the rotation speed for inverse kinematics.
     *
     * @return speed (&ge;0)
     */
    public float getIkRotSpeed() {
        return ikRotSpeed;
    }

    /**
     * Alter the rotation speed for inverse kinematics.
     *
     * @param ikRotSpeed the desired speed (&ge;0, default=7)
     */
    public void setIkRotSpeed(float ikRotSpeed) {
        this.ikRotSpeed = ikRotSpeed;
    }

    /**
     * Read the distance threshold for inverse kinematics.
     *
     * @return distance threshold
     */
    public float getIKThreshold() {
        return IKThreshold;
    }

    /**
     * Alter the distance threshold for inverse kinematics.
     *
     * @param IKThreshold the desired distance threshold (default=0.1)
     */
    public void setIKThreshold(float IKThreshold) {
        this.IKThreshold = IKThreshold;
    }

    /**
     * Read the limb damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getLimbDamping() {
        return limbDamping;
    }

    /**
     * Alter the limb damping.
     *
     * @param dampingRatio the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0.6)
     */
    public void setLimbDamping(float dampingRatio) {
        limbDamping = dampingRatio;

        for (PhysicsBoneLink link : boneLinks.values()) {
            link.getRigidBody().setDamping(limbDamping, limbDamping);
        }
    }

    /**
     * Access the named bone.
     *
     * @param name which bone to access
     * @return the pre-existing instance, or null if not found
     */
    public Bone getBone(String name) {
        return skeleton.getBone(name);
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
        // TODO jointMap
        oc.write(boneLinks.values().toArray(
                new PhysicsBoneLink[boneLinks.size()]),
                "boneLinks", new PhysicsBoneLink[0]);
        oc.write(modelLocation, "modelPosition", new Vector3f());
        oc.write(modelOrientation, "modelRotation", new Quaternion());
        oc.write(skeleton, "skeleton", null);
        oc.write(skeletonControl, "skeletonControl", null);
        oc.write(transformer, "transformer", null);
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(initScale, "initScale", null);
        oc.write(mode, "mode", null);
        oc.write(isBlending, "blendedControl", false);
        oc.write(blendProgress, "blendStart", 0f);
        oc.write(blendTime, "blendTime", 1f);
        oc.write(eventDispatchImpulseThreshold, "eventDispatchImpulseThreshold",
                0f);
        oc.write(torsoMass, "rootMass", 15f);
        oc.write(totalMass, "totalMass", 0f);
        oc.write(ikRotSpeed, "rotSpeed", 7f);
        oc.write(limbDamping, "limbDampening", 0.6f);
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
        InputCapsule ic = im.getCapsule(this);
        // TODO jointMap
        PhysicsBoneLink[] loadedBoneLinks
                = (PhysicsBoneLink[]) ic.readSavableArray("boneList",
                        new PhysicsBoneLink[0]);
        for (PhysicsBoneLink physicsBoneLink : loadedBoneLinks) {
            boneLinks.put(physicsBoneLink.getBone().getName(), physicsBoneLink);
        }
        modelLocation.set((Vector3f) ic.readSavable("modelPosition",
                new Vector3f()));
        modelOrientation.set((Quaternion) ic.readSavable("modelRotation",
                new Quaternion()));
        skeleton = (Skeleton) ic.readSavable("skeleton", null);
        skeletonControl
                = (SkeletonControl) ic.readSavable("skeletonControl", null);
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        transformer
                = (Spatial) ic.readSavable("transformer", null);
        initScale = (Vector3f) ic.readSavable("initScale", null);
        mode = ic.readEnum("mode", Mode.class, Mode.Kinematic);
        isBlending = ic.readBoolean("blendedControl", false);
        blendProgress = ic.readFloat("blendStart", 0f);
        blendTime = ic.readFloat("blendTime", 1f);
        eventDispatchImpulseThreshold
                = ic.readFloat("eventDispatchImpulseThreshold", 0f);
        torsoMass = ic.readFloat("rootMass", 15f);
        totalMass = ic.readFloat("totalMass", 0f);
    }
    // *************************************************************************
    // private methods

    /**
     * Assign each mesh vertex to a linked bone and add its location (mesh
     * coordinates in bind pose) to that bone's list.
     *
     * @param lbNames a map from bone indices to linked-bone names
     * @return a new map from linked-bone names to coordinates
     */
    private Map<String, List<Vector3f>> coordsMap(String[] lbNames) {
        Mesh[] meshes = skeletonControl.getTargets();
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Map<String, List<Vector3f>> coordsMap = new HashMap<>();
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; vertexI++) {
                MyMesh.vertexBoneIndices(mesh, vertexI, iArray);
                MyMesh.vertexBoneWeights(mesh, vertexI, wArray);

                Map<String, Float> weightMap
                        = weightMap(iArray, wArray, lbNames);

                float bestTotalWeight = Float.NEGATIVE_INFINITY;
                String bestLbName = null;
                for (String lbName : weightMap.keySet()) {
                    float totalWeight = weightMap.get(lbName);
                    if (totalWeight >= bestTotalWeight) {
                        bestTotalWeight = totalWeight;
                        bestLbName = lbName;
                    }
                }
                /*
                 * Add the bind-pose coordinates of the vertex
                 * to the linked bone's list.
                 */
                List<Vector3f> coordList;
                if (coordsMap.containsKey(bestLbName)) {
                    coordList = coordsMap.get(bestLbName);
                } else {
                    coordList = new ArrayList<>(20);
                    coordsMap.put(bestLbName, coordList);
                }
                Vector3f bindPosition = MyMesh.vertexVector3f(mesh,
                        VertexBuffer.Type.BindPosePosition, vertexI, null);
                coordList.add(bindPosition);
            }
        }

        return coordsMap;
    }

    /**
     * Create a hull collision shape, using the specified inverse transform and
     * list of vertex locations.
     *
     * @param inverseTransform (not null, unaffected)
     * @param vertexLocations list of vertex locations (not null, not empty,
     * unaffected)
     * @return a new shape
     */
    private CollisionShape createShape(Transform inverseTransform,
            Vector3f offset, List<Vector3f> vertexLocations) {
        assert inverseTransform != null;
        assert vertexLocations != null;
        assert !vertexLocations.isEmpty();

        for (Vector3f location : vertexLocations) {
            location.subtractLocal(offset).mult(initScale);
        }

        CollisionShape boneShape = new HullCollisionShape(vertexLocations);

        return boneShape;
    }

    /**
     * Map bone indices to names of linked bones.
     *
     * @return a new array of bone names
     */
    private String[] linkedBoneNameArray() {
        int numBones = skeleton.getBoneCount();
        String[] nameArray = new String[numBones];
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            while (true) {
                String boneName = bone.getName();
                if (jointMap.containsKey(boneName)) {
                    nameArray[boneIndex] = boneName;
                    break;
                }
                bone = bone.getParent();
                if (bone == null) {
                    nameArray[boneIndex] = torsoFakeBoneName;
                    break;
                }
            }
        }

        return nameArray;
    }

    /*
     * Update the controlled spatial's transform based on torso dynamics.
     */
    private void torsoDynamicUpdate() {
        RigidBodyMotionState motionState = torsoRigidBody.getMotionState();
        Transform torsoTransform = motionState.physicsTransform(null);
        Vector3f scale = torsoTransform.getScale();
        torsoRigidBody.getPhysicsScale(scale);

        Transform worldToParent; // inverse world transform of the parent
        Node parent = spatial.getParent();
        if (parent == null) {
            worldToParent = new Transform();
        } else {
            Transform parentTransform = parent.getWorldTransform();
            worldToParent = parentTransform.invert();
        }

        Transform transform = meshToModel.clone();
        transform.combineWithParent(torsoTransform);
        transform.combineWithParent(worldToParent);
        spatial.setLocalTransform(transform);
    }

    /*
     * Update the torso to based on the transformer spatial.
     */
    private void torsoKinematicUpdate() {
        Vector3f translation = transformer.getWorldTranslation();
        torsoRigidBody.setPhysicsLocation(translation);
        Quaternion orientation = transformer.getWorldRotation();
        torsoRigidBody.setPhysicsRotation(orientation);

        Vector3f scale = transformer.getWorldScale();
        PhysicsSpace space = getPhysicsSpace();
        torsoRigidBody.setPhysicsScale(scale, space);
    }

    /**
     * Tabulate the total weight associated with each linked bone.
     *
     * @param biArray the array of bone indices (not null, unaffected)
     * @param bwArray the array of bone weights (not null, unaffected)
     * @param lbNames a map from bone indices to linked bone names (not null,
     * unaffected)
     * @return a new map from linked-bone names to total weight
     */
    private Map<String, Float> weightMap(int[] biArray, float[] bwArray, String[] lbNames) {
        assert biArray.length == 4;
        assert bwArray.length == 4;

        Map<String, Float> weightMap = new HashMap<>();
        for (int j = 0; j < 4; j++) {
            int boneIndex = biArray[j];
            if (boneIndex != -1) {
                String lbName = lbNames[boneIndex];
                if (weightMap.containsKey(lbName)) {
                    float oldWeight = weightMap.get(lbName);
                    float newWeight = oldWeight + bwArray[j];
                    weightMap.put(lbName, newWeight);
                } else {
                    weightMap.put(lbName, bwArray[j]);
                }
            }
        }

        return weightMap;
    }
}

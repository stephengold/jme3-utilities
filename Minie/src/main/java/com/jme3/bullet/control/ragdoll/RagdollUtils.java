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
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods used by KinematicRagdollControl.
 *
 * @author Nehon
 */
public class RagdollUtils {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(RagdollUtils.class.getName());

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RagdollUtils() {
    }

    /**
     * Alter the limits of the specified 6-DOF joint.
     *
     * @param joint which joint to alter (not null)
     * @param maxX the maximum rotation on the X axis (in radians)
     * @param minX the minimum rotation on the X axis (in radians)
     * @param maxY the maximum rotation on the Y axis (in radians)
     * @param minY the minimum rotation on the Y axis (in radians)
     * @param maxZ the maximum rotation on the Z axis (in radians)
     * @param minZ the minimum rotation on the Z axis (in radians)
     */
    public static void setJointLimit(SixDofJoint joint, float maxX, float minX,
            float maxY, float minY, float maxZ, float minZ) {
        Validate.inRange(maxX, "max X rotation", minX, FastMath.PI);
        Validate.inRange(minX, "min X rotation", -FastMath.PI, maxX);
        Validate.inRange(maxY, "max Y rotation", minY, FastMath.PI);
        Validate.inRange(minY, "min Y rotation", -FastMath.PI, maxY);
        Validate.inRange(maxZ, "max Z rotation", minZ, FastMath.PI);
        Validate.inRange(minZ, "min Z rotation", -FastMath.PI, maxZ);

        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_X).setHiLimit(maxX);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_X).setLoLimit(minX);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Y).setHiLimit(maxY);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Y).setLoLimit(minY);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Z).setHiLimit(maxZ);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Z).setLoLimit(minZ);
    }

    /**
     * Updates a bone position and rotation. if the child bones are not in the
     * bone list this means, they are not associated with a physics shape. So
     * they have to be updated
     *
     * @param bone the bone
     * @param pos the position
     * @param rot the rotation
     * @param restoreBoneControl true &rarr; user-control flag should be set
     * @param boneList the names of all bones without collision shapes (not
     * null, unaffected)
     */
    public static void setTransform(Bone bone, Vector3f pos, Quaternion rot,
            boolean restoreBoneControl, Set<String> boneList) {
        // Ensure user control
        if (restoreBoneControl) {
            bone.setUserControl(true);
        }
        // Set the user transform of the bone.
        bone.setUserTransformsInModelSpace(pos, rot);
        for (Bone childBone : bone.getChildren()) {
            //each child bone not in the list is updated
            if (!boneList.contains(childBone.getName())) {
                Transform t = childBone.getCombinedTransform(pos, rot);
                setTransform(childBone, t.getTranslation(), t.getRotation(),
                        restoreBoneControl, boneList);
            }
        }
        //we give back the control to the keyframed animation
        if (restoreBoneControl) {
            bone.setUserControl(false);
        }
    }

    /**
     * Test whether the indexed bone has at least one vertex in the specified
     * meshes with a weight greater than the specified threshold.
     *
     * @param boneIndex the index of the bone (&ge;0)
     * @param targets the meshes to search (not null, no null elements)
     * @param weightThreshold the threshold (&ge;0, &le;1)
     * @return true if at least 1 vertex found, otherwise false
     */
    public static boolean hasVertices(int boneIndex, Mesh[] targets,
            float weightThreshold) {
        for (Mesh mesh : targets) {
            ByteBuffer boneIndices
                    = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
            FloatBuffer boneWeight
                    = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

            boneIndices.rewind();
            boneWeight.rewind();

            int vertexComponents = mesh.getVertexCount() * 3;
            for (int i = 0; i < vertexComponents; i += 3) {
                int start = i / 3 * 4;
                for (int k = start; k < start + 4; k++) {
                    if (boneIndices.get(k) == boneIndex
                            && boneWeight.get(k) >= weightThreshold) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

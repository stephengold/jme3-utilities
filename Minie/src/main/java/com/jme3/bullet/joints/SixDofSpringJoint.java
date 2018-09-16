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
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;

/**
 * A 6 degree-of-freedom joint based on Bullet's btGeneric6DofSpringConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * This generic constraint can emulate a variety of standard constraints, by
 * configuring each of the 6 degrees of freedom (DoF). The first 3 DoFs are
 * translations of the rigid bodies, and the last 3 DoFs are rotations.
 * <p>
 * Each DoF can be locked, free, or limited. On construction of a new
 * btGeneric6DofConstraint, all axes are locked. After that, the axes can be
 * reconfigured. However, some combinations that include free and/or limited
 * angular degrees of freedom are undefined.
 *
 * @author normenhansen
 */
public class SixDofSpringJoint extends SixDofJoint {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(SixDofSpringJoint.class.getName());

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public SixDofSpringJoint() {
    }

    /**
     * Instantiate a SixDofSpringJoint. To be effective, the joint must be added
     * to a physics space.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA the local offset of the connection point in node A (not
     * null, alias created) TODO
     * @param pivotB the local offset of the connection point in node B (not
     * null, alias created) TODO
     * @param rotA the local orientation of the connection to node A (not null,
     * alias created) TODO
     * @param rotB the local orientation of the connection to node B (not null,
     * alias created) TODO
     * @param useLinearReferenceFrameA true&rarr;use node A, false&rarr;use node
     * B
     */
    public SixDofSpringJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB,
            boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB, rotA, rotB,
                useLinearReferenceFrameA);
    }

    /**
     * Enable or disable the spring for the indexed degree of freedom.
     *
     * @param index which degree of freedom (&ge;0, &lt;6)
     * @param onOff true &rarr; enable, false &rarr; disable
     */
    public void enableSpring(int index, boolean onOff) {
        enableSpring(objectId, index, onOff);
    }

    native void enableSpring(long objectId, int index, boolean onOff);

    /**
     * Alter the spring stiffness for the indexed degree of freedom.
     *
     * @param index which degree of freedom (&ge;0, &lt;6)
     * @param stiffness the desired stiffness
     */
    public void setStiffness(int index, float stiffness) {
        setStiffness(objectId, index, stiffness);
    }

    native void setStiffness(long objectId, int index, float stiffness);

    /**
     * Alter the damping for the indexed degree of freedom.
     *
     * @param index which degree of freedom (&ge;0, &lt;6)
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=1)
     */
    public void setDamping(int index, float damping) {
        setDamping(objectId, index, damping);
    }

    native void setDamping(long objectId, int index, float damping);

    /**
     * Alter the equilibrium points for all degrees of freedom, based on the
     * current constraint position/orientation.
     */
    public void setEquilibriumPoint() {
        setEquilibriumPoint(objectId);
    }

    native void setEquilibriumPoint(long objectId);

    /**
     * Alter the equilibrium point of the indexed degree of freedom, based on
     * the current constraint position/orientation.
     *
     * @param index which degree of freedom (&ge;0, &lt;6)
     */
    public void setEquilibriumPoint(int index) {
        setEquilibriumPoint(objectId, index);
    }

    native void setEquilibriumPoint(long objectId, int index);

    @Override
    native long createJoint(long objectIdA, long objectIdB, Vector3f pivotA,
            Matrix3f rotA, Vector3f pivotB, Matrix3f rotB,
            boolean useLinearReferenceFrameA);
}

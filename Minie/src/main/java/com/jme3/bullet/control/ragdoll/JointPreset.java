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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.math.FastMath;
import jme3utilities.Validate;

/**
 * Range of motion for a joint. Note: immutable.
 *
 * @author Nehon
 */
public class JointPreset {

    final private float maxX;
    final private float minX;
    final private float maxY;
    final private float minY;
    final private float maxZ;
    final private float minZ;

    /**
     * Instantiate a preset with no motion allowed.
     */
    public JointPreset() {
        maxX = 0f;
        minX = 0f;
        maxY = 0f;
        minY = 0f;
        maxZ = 0f;
        minZ = 0f;
    }

    /**
     * Instantiate a preset with the specified range of motion.
     *
     * @param maxX the maximum rotation on the X axis (in radians)
     * @param minX the minimum rotation on the X axis (in radians)
     * @param maxY the maximum rotation on the Y axis (in radians)
     * @param minY the minimum rotation on the Y axis (in radians)
     * @param maxZ the maximum rotation on the Z axis (in radians)
     * @param minZ the minimum rotation on the Z axis (in radians)
     */
    public JointPreset(float maxX, float minX, float maxY, float minY,
            float maxZ, float minZ) {
        Validate.inRange(maxX, "max X rotation", minX, FastMath.PI);
        Validate.inRange(minX, "min X rotation", -FastMath.PI, maxX);
        Validate.inRange(maxY, "max Y rotation", minY, FastMath.PI);
        Validate.inRange(minY, "min Y rotation", -FastMath.PI, maxY);
        Validate.inRange(maxZ, "max Z rotation", minZ, FastMath.PI);
        Validate.inRange(minZ, "min Z rotation", -FastMath.PI, maxZ);

        this.maxX = maxX;
        this.minX = minX;
        this.maxY = maxY;
        this.minY = minY;
        this.maxZ = maxZ;
        this.minZ = minZ;
    }

    /**
     * Apply this preset to the specified joint.
     *
     * @param joint where to apply this preset (not null, modified)
     */
    public void setupJoint(SixDofJoint joint) {
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_X).setHiLimit(maxX);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_X).setLoLimit(minX);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Y).setHiLimit(maxY);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Y).setLoLimit(minY);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Z).setHiLimit(maxZ);
        joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Z).setLoLimit(minZ);
    }
}

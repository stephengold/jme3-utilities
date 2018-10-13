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
import com.jme3.bullet.joints.motors.RotationalLimitMotor;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import java.io.IOException;
import jme3utilities.Validate;

/**
 * Range of motion for a ragdoll joint. Note: immutable.
 *
 * @author Nehon
 */
public class JointPreset implements Savable {
    // *************************************************************************
    // fields

    /**
     * maximum rotation angle around the X axis (in radians)
     */
    private float maxX;
    /**
     * minimum rotation angle around the X axis (in radians)
     */
    private float minX;
    /**
     * maximum rotation angle around the Y axis (in radians)
     */
    private float maxY;
    /**
     * minimum rotation angle around the Y axis (in radians)
     */
    private float minY;
    /**
     * maximum rotation angle around the Z axis (in radians)
     */
    private float maxZ;
    /**
     * minimum rotation angle around the Z axis (in radians)
     */
    private float minZ;
    // *************************************************************************
    // constructors

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
     * @param maxX the maximum rotation around the X axis (in radians)
     * @param minX the minimum rotation around the X axis (in radians)
     * @param maxY the maximum rotation around the Y axis (in radians)
     * @param minY the minimum rotation around the Y axis (in radians)
     * @param maxZ the maximum rotation around the Z axis (in radians)
     * @param minZ the minimum rotation around the Z axis (in radians)
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
    // *************************************************************************
    // new methods exposed

    /**
     * Read the maximum rotation around the indexed axis.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     *
     * @return the rotation angle (in radians)
     */
    public float getMaxRotation(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", 0, 2);

        float result;
        switch (axisIndex) {
            case PhysicsSpace.AXIS_X:
                result = maxX;
                break;
            case PhysicsSpace.AXIS_Y:
                result = maxY;
                break;
            case PhysicsSpace.AXIS_Z:
                result = maxZ;
                break;
            default:
                throw new RuntimeException();
        }

        return result;
    }

    /**
     * Read the minimum rotation around the indexed axis.
     *
     * @param axisIndex which axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     *
     * @return the rotation angle (in radians)
     */
    public float getMinRotation(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", 0, 2);

        float result;
        switch (axisIndex) {
            case PhysicsSpace.AXIS_X:
                result = minX;
                break;
            case PhysicsSpace.AXIS_Y:
                result = minY;
                break;
            case PhysicsSpace.AXIS_Z:
                result = minZ;
                break;
            default:
                throw new RuntimeException();
        }

        return result;
    }

    /**
     * Apply this preset to the specified joint.
     *
     * @param joint where to apply this preset (not null, modified)
     */
    public void setupJoint(SixDofJoint joint) {
        Validate.nonNull(joint, "joint");

        RotationalLimitMotor rotX
                = joint.getRotationalLimitMotor(PhysicsSpace.AXIS_X);
        rotX.setUpperLimit(maxX);
        rotX.setLowerLimit(minX);
        rotX.setMaxLimitForce(1e6f);

        RotationalLimitMotor rotY
                = joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Y);
        rotY.setUpperLimit(maxY);
        rotY.setLowerLimit(minY);
        rotY.setMaxLimitForce(1e6f);

        RotationalLimitMotor rotZ
                = joint.getRotationalLimitMotor(PhysicsSpace.AXIS_Z);
        rotZ.setUpperLimit(maxZ);
        rotZ.setLowerLimit(minZ);
        rotZ.setMaxLimitForce(1e6f);
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this preset, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        maxX = capsule.readFloat("maxX", 0f);
        minX = capsule.readFloat("minX", 0f);
        maxY = capsule.readFloat("maxY", 0f);
        minY = capsule.readFloat("minY", 0f);
        maxZ = capsule.readFloat("maxZ", 0f);
        minZ = capsule.readFloat("minZ", 0f);
    }

    /**
     * Serialize this preset, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(maxX, "maxX", 0f);
        capsule.write(minX, "minX", 0f);
        capsule.write(maxY, "maxY", 0f);
        capsule.write(minY, "minY", 0f);
        capsule.write(maxZ, "maxZ", 0f);
        capsule.write(minZ, "minZ", 0f);
    }
}

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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A slider joint based on Bullet's btSliderConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * The slider constraint allows the body to rotate around one axis and translate
 * along this axis.
 *
 * @author normenhansen
 */
public class SliderJoint extends PhysicsJoint {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(SliderJoint.class.getName());
    // *************************************************************************
    // fields

    private Matrix3f rotA;
    private Matrix3f rotB;
    private boolean useLinearReferenceFrameA;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public SliderJoint() {
    }

    /**
     * Instantiate a SliderJoint. To be effective, the joint must be added to a
     * physics space.
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
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB,
            boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = rotA;
        this.rotB = rotB;
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * Instantiate a SliderJoint. To be effective, the joint must be added to a
     * physics space.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA the local offset of the connection point in node A (not
     * null, alias created) TODO
     * @param pivotB the local offset of the connection point in node B (not
     * null, alias created) TODO
     * @param useLinearReferenceFrameA true&rarr;use node A, false&rarr;use node
     * B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB,
            boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA = new Matrix3f();
        this.rotB = new Matrix3f();
        this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        createJoint();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the joint's lower limit for on-axis translation.
     *
     * @return the lower limit
     */
    public float getLowerLinLimit() {
        return getLowerLinLimit(objectId);
    }

    /**
     * Alter the joint's lower limit for on-axis translation.
     *
     * @param lowerLinLimit the desired lower limit (default=-1)
     */
    public void setLowerLinLimit(float lowerLinLimit) {
        setLowerLinLimit(objectId, lowerLinLimit);
    }

    /**
     * Read the joint's upper limit for on-axis translation.
     *
     * @return the upper limit
     */
    public float getUpperLinLimit() {
        return getUpperLinLimit(objectId);
    }

    /**
     * Alter the joint's upper limit for on-axis translation.
     *
     * @param upperLinLimit the desired upper limit (default=1)
     */
    public void setUpperLinLimit(float upperLinLimit) {
        setUpperLinLimit(objectId, upperLinLimit);
    }

    /**
     * Read the joint's lower limit for on-axis rotation.
     *
     * @return the lower limit angle (in radians)
     */
    public float getLowerAngLimit() {
        return getLowerAngLimit(objectId);
    }

    /**
     * Alter the joint's lower limit for on-axis rotation.
     *
     * @param lowerAngLimit the desired lower limit angle (in radians,
     * default=0)
     */
    public void setLowerAngLimit(float lowerAngLimit) {
        setLowerAngLimit(objectId, lowerAngLimit);
    }

    /**
     * Read the joint's upper limit for on-axis rotation.
     *
     * @return the upper limit angle (in radians)
     */
    public float getUpperAngLimit() {
        return getUpperAngLimit(objectId);
    }

    /**
     * Alter the joint's upper limit for on-axis rotation.
     *
     * @param upperAngLimit the desired upper limit angle (in radians,
     * default=0)
     */
    public void setUpperAngLimit(float upperAngLimit) {
        setUpperAngLimit(objectId, upperAngLimit);
    }

    /**
     * Read the joint's softness for on-axis translation between the limits.
     *
     * @return the softness
     */
    public float getSoftnessDirLin() {
        return getSoftnessDirLin(objectId);
    }

    /**
     * Alter the joint's softness for on-axis translation between the limits.
     *
     * @param softnessDirLin the desired softness (default=1)
     */
    public void setSoftnessDirLin(float softnessDirLin) {
        setSoftnessDirLin(objectId, softnessDirLin);
    }

    /**
     * Read the joint's restitution for on-axis translation between the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionDirLin() {
        return getRestitutionDirLin(objectId);
    }

    /**
     * Alter the joint's restitution for on-axis translation between the limits.
     *
     * @param restitutionDirLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionDirLin(float restitutionDirLin) {
        setRestitutionDirLin(objectId, restitutionDirLin);
    }

    /**
     * Read the joint's damping for on-axis translation between the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingDirLin() {
        return getDampingDirLin(objectId);
    }

    /**
     * Alter the joint's damping for on-axis translation between the limits.
     *
     * @param dampingDirLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=0)
     */
    public void setDampingDirLin(float dampingDirLin) {
        setDampingDirLin(objectId, dampingDirLin);
    }

    /**
     * Read the joint's softness for on-axis rotation between the limits.
     *
     * @return the softness
     */
    public float getSoftnessDirAng() {
        return getSoftnessDirAng(objectId);
    }

    /**
     * Alter the joint's softness for on-axis rotation between the limits.
     *
     * @param softnessDirAng the desired softness (default=1)
     */
    public void setSoftnessDirAng(float softnessDirAng) {
        setSoftnessDirAng(objectId, softnessDirAng);
    }

    /**
     * Read the joint's restitution for on-axis rotation between the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionDirAng() {
        return getRestitutionDirAng(objectId);
    }

    /**
     * Alter the joint's restitution for on-axis rotation between the limits.
     *
     * @param restitutionDirAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionDirAng(float restitutionDirAng) {
        setRestitutionDirAng(objectId, restitutionDirAng);
    }

    /**
     * Read the joint's damping for on-axis rotation between the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingDirAng() {
        return getDampingDirAng(objectId);
    }

    /**
     * Alter the joint's damping for on-axis rotation between the limits.
     *
     * @param dampingDirAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=0)
     */
    public void setDampingDirAng(float dampingDirAng) {
        setDampingDirAng(objectId, dampingDirAng);
    }

    /**
     * Read the joint's softness for on-axis translation hitting the limits.
     *
     * @return the softness
     */
    public float getSoftnessLimLin() {
        return getSoftnessLimLin(objectId);
    }

    /**
     * Alter the joint's softness for on-axis translation hitting the limits.
     *
     * @param softnessLimLin the desired softness (default=1)
     */
    public void setSoftnessLimLin(float softnessLimLin) {
        setSoftnessLimLin(objectId, softnessLimLin);
    }

    /**
     * Read the joint's restitution for on-axis translation hitting the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionLimLin() {
        return getRestitutionLimLin(objectId);
    }

    /**
     * Alter the joint's restitution for on-axis translation hitting the limits.
     *
     * @param restitutionLimLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionLimLin(float restitutionLimLin) {
        setRestitutionLimLin(objectId, restitutionLimLin);
    }

    /**
     * Read the joint's damping for on-axis translation hitting the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingLimLin() {
        return getDampingLimLin(objectId);
    }

    /**
     * Alter the joint's damping for on-axis translation hitting the limits.
     *
     * @param dampingLimLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingLimLin(float dampingLimLin) {
        setDampingLimLin(objectId, dampingLimLin);
    }

    /**
     * Read the joint's softness for on-axis rotation hitting the limits.
     *
     * @return the softness
     */
    public float getSoftnessLimAng() {
        return getSoftnessLimAng(objectId);
    }

    /**
     * Alter the joint's softness for on-axis rotation hitting the limits.
     *
     * @param softnessLimAng the desired softness (default=1)
     */
    public void setSoftnessLimAng(float softnessLimAng) {
        setSoftnessLimAng(objectId, softnessLimAng);
    }

    /**
     * Read the joint's restitution for on-axis rotation hitting the limits.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionLimAng() {
        return getRestitutionLimAng(objectId);
    }

    /**
     * Alter the joint's restitution for on-axis rotation hitting the limits.
     *
     * @param restitutionLimAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionLimAng(float restitutionLimAng) {
        setRestitutionLimAng(objectId, restitutionLimAng);
    }

    /**
     * Read the joint's damping for on-axis rotation hitting the limits.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingLimAng() {
        return getDampingLimAng(objectId);
    }

    /**
     * Alter the joint's damping for on-axis rotation hitting the limits.
     *
     * @param dampingLimAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingLimAng(float dampingLimAng) {
        setDampingLimAng(objectId, dampingLimAng);
    }

    /**
     * Read the joint's softness for off-axis translation.
     *
     * @return the softness
     */
    public float getSoftnessOrthoLin() {
        return getSoftnessOrthoLin(objectId);
    }

    /**
     * Alter the joint's softness for off-axis translation.
     *
     * @param softnessOrthoLin the desired softness (default=1)
     */
    public void setSoftnessOrthoLin(float softnessOrthoLin) {
        setSoftnessOrthoLin(objectId, softnessOrthoLin);
    }

    /**
     * Read the joint's restitution for off-axis translation.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionOrthoLin() {
        return getRestitutionOrthoLin(objectId);
    }

    /**
     * Alter the joint's restitution for off-axis translation.
     *
     * @param restitutionOrthoLin the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionOrthoLin(float restitutionOrthoLin) {
        setRestitutionOrthoLin(objectId, restitutionOrthoLin);
    }

    /**
     * Read the joint's damping for off-axis translation.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingOrthoLin() {
        return getDampingOrthoLin(objectId);
    }

    /**
     * Alter the joint's damping for off-axis translation.
     *
     * @param dampingOrthoLin the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingOrthoLin(float dampingOrthoLin) {
        setDampingOrthoLin(objectId, dampingOrthoLin);
    }

    /**
     * Read the joint's softness for off-axis rotation.
     *
     * @return the softness
     */
    public float getSoftnessOrthoAng() {
        return getSoftnessOrthoAng(objectId);
    }

    /**
     * Alter the joint's softness for off-axis rotation.
     *
     * @param softnessOrthoAng the desired softness (default=1)
     */
    public void setSoftnessOrthoAng(float softnessOrthoAng) {
        setSoftnessOrthoAng(objectId, softnessOrthoAng);
    }

    /**
     * Read the joint's restitution for off-axis rotation.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitutionOrthoAng() {
        return getRestitutionOrthoAng(objectId);
    }

    /**
     * Alter the joint's restitution for off-axis rotation.
     *
     * @param restitutionOrthoAng the desired restitution (bounce) factor
     * (default=0.7)
     */
    public void setRestitutionOrthoAng(float restitutionOrthoAng) {
        setRestitutionOrthoAng(objectId, restitutionOrthoAng);
    }

    /**
     * Read the joint's damping for off-axis rotation.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDampingOrthoAng() {
        return getDampingOrthoAng(objectId);
    }

    /**
     * Alter the joint's damping for off-axis rotation.
     *
     * @param dampingOrthoAng the desired viscous damping ratio (0&rarr;no
     * damping, 1&rarr;critically damped, default=1)
     */
    public void setDampingOrthoAng(float dampingOrthoAng) {
        setDampingOrthoAng(objectId, dampingOrthoAng);
    }

    /**
     * Test whether the translation motor is powered.
     *
     * @return true if powered, otherwise false
     */
    public boolean isPoweredLinMotor() {
        return isPoweredLinMotor(objectId);
    }

    /**
     * Alter whether the translation motor is powered.
     *
     * @param poweredLinMotor true to power the motor, false to de-power it
     * (default=false)
     */
    public void setPoweredLinMotor(boolean poweredLinMotor) {
        setPoweredLinMotor(objectId, poweredLinMotor);
    }

    /**
     * Read the velocity target of the translation motor.
     *
     * @return the velocity target
     */
    public float getTargetLinMotorVelocity() {
        return getTargetLinMotorVelocity(objectId);
    }

    /**
     * Alter the velocity target of the translation motor.
     *
     * @param targetLinMotorVelocity the desired velocity target (default=0)
     */
    public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
        setTargetLinMotorVelocity(objectId, targetLinMotorVelocity);
    }

    /**
     * Read the maximum force of the translation motor.
     *
     * @return the maximum force
     */
    public float getMaxLinMotorForce() {
        return getMaxLinMotorForce(objectId);
    }

    /**
     * Alter the maximum force of the translation motor.
     *
     * @param maxLinMotorForce the desired maximum force (default=0)
     */
    public void setMaxLinMotorForce(float maxLinMotorForce) {
        setMaxLinMotorForce(objectId, maxLinMotorForce);
    }

    /**
     * Test whether the rotation motor is powered.
     *
     * @return true if powered, otherwise false
     */
    public boolean isPoweredAngMotor() {
        return isPoweredAngMotor(objectId);
    }

    /**
     * Alter whether the rotation motor is powered.
     *
     * @param poweredAngMotor true to power the motor, false to de-power it
     * (default=false)
     */
    public void setPoweredAngMotor(boolean poweredAngMotor) {
        setPoweredAngMotor(objectId, poweredAngMotor);
    }

    /**
     * Read the velocity target of the rotation motor.
     *
     * @return the velocity target (in radians per second)
     */
    public float getTargetAngMotorVelocity() {
        return getTargetAngMotorVelocity(objectId);
    }

    /**
     * Alter the velocity target of the rotation motor.
     *
     * @param targetAngMotorVelocity the desired velocity target (in radians per
     * second, default=0)
     */
    public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
        setTargetAngMotorVelocity(objectId, targetAngMotorVelocity);
    }

    /**
     * Read the maximum force of the rotation motor.
     *
     * @return the maximum force
     */
    public float getMaxAngMotorForce() {
        return getMaxAngMotorForce(objectId);
    }

    /**
     * Alter the maximum force of the rotation motor.
     *
     * @param maxAngMotorForce the desired maximum force (default=0)
     */
    public void setMaxAngMotorForce(float maxAngMotorForce) {
        setMaxAngMotorForce(objectId, maxAngMotorForce);
    }
    // *************************************************************************
    // PhysicsJoint methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned object into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this shape (not null)
     * @param original the instance from which this instance was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        rotA = cloner.clone(rotA);
        rotB = cloner.clone(rotB);
        createJoint();

        SliderJoint old = (SliderJoint) original;
        setDampingDirAng(old.getDampingDirAng());
        setDampingDirLin(old.getDampingDirLin());
        setDampingLimAng(old.getDampingLimAng());
        setDampingLimLin(old.getDampingLimLin());
        setDampingOrthoAng(old.getDampingOrthoAng());
        setDampingOrthoLin(old.getDampingOrthoLin());

        setLowerAngLimit(old.getLowerAngLimit());
        setLowerLinLimit(old.getLowerLinLimit());

        setMaxAngMotorForce(old.getMaxAngMotorForce());
        setMaxLinMotorForce(old.getMaxLinMotorForce());

        setPoweredAngMotor(old.isPoweredAngMotor());
        setPoweredLinMotor(old.isPoweredLinMotor());

        setRestitutionDirAng(old.getRestitutionDirAng());
        setRestitutionDirLin(old.getRestitutionDirLin());
        setRestitutionLimAng(old.getRestitutionLimAng());
        setRestitutionLimLin(old.getRestitutionLimLin());
        setRestitutionOrthoAng(old.getRestitutionOrthoAng());
        setRestitutionOrthoLin(old.getRestitutionOrthoLin());

        setSoftnessDirAng(old.getSoftnessDirAng());
        setSoftnessDirLin(old.getSoftnessDirLin());
        setSoftnessLimAng(old.getSoftnessLimAng());
        setSoftnessLimLin(old.getSoftnessLimLin());
        setSoftnessOrthoAng(old.getSoftnessOrthoAng());
        setSoftnessOrthoLin(old.getSoftnessOrthoLin());

        setTargetAngMotorVelocity(old.getTargetAngMotorVelocity());
        setTargetLinMotorVelocity(old.getTargetLinMotorVelocity());

        setUpperAngLimit(old.getUpperAngLimit());
        setUpperLinLimit(old.getUpperLinLimit());
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SliderJoint jmeClone() {
        try {
            SliderJoint clone = (SliderJoint) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Serialize this joint, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        //TODO: standard values..
        capsule.write(getDampingDirAng(), "dampingDirAng", 0f);
        capsule.write(getDampingDirLin(), "dampingDirLin", 0f);
        capsule.write(getDampingLimAng(), "dampingLimAng", 0f);
        capsule.write(getDampingLimLin(), "dampingLimLin", 0f);
        capsule.write(getDampingOrthoAng(), "dampingOrthoAng", 0f);
        capsule.write(getDampingOrthoLin(), "dampingOrthoLin", 0f);
        capsule.write(getLowerAngLimit(), "lowerAngLimit", 0f);
        capsule.write(getLowerLinLimit(), "lowerLinLimit", 0f);
        capsule.write(getMaxAngMotorForce(), "maxAngMotorForce", 0f);
        capsule.write(getMaxLinMotorForce(), "maxLinMotorForce", 0f);
        capsule.write(isPoweredAngMotor(), "poweredAngMotor", false);
        capsule.write(isPoweredLinMotor(), "poweredLinMotor", false);
        capsule.write(getRestitutionDirAng(), "restitutionDirAng", 0f);
        capsule.write(getRestitutionDirLin(), "restitutionDirLin", 0f);
        capsule.write(getRestitutionLimAng(), "restitutionLimAng", 0f);
        capsule.write(getRestitutionLimLin(), "restitutionLimLin", 0f);
        capsule.write(getRestitutionOrthoAng(), "restitutionOrthoAng", 0f);
        capsule.write(getRestitutionOrthoLin(), "restitutionOrthoLin", 0f);

        capsule.write(getSoftnessDirAng(), "softnessDirAng", 0f);
        capsule.write(getSoftnessDirLin(), "softnessDirLin", 0f);
        capsule.write(getSoftnessLimAng(), "softnessLimAng", 0f);
        capsule.write(getSoftnessLimLin(), "softnessLimLin", 0f);
        capsule.write(getSoftnessOrthoAng(), "softnessOrthoAng", 0f);
        capsule.write(getSoftnessOrthoLin(), "softnessOrthoLin", 0f);

        capsule.write(getTargetAngMotorVelocity(),
                "targetAngMotorVelicoty", 0f); //oops
        capsule.write(getTargetLinMotorVelocity(),
                "targetLinMotorVelicoty", 0f); //oops

        capsule.write(getUpperAngLimit(), "upperAngLimit", 0f);
        capsule.write(getUpperLinLimit(), "upperLinLimit", 0f);

        capsule.write(useLinearReferenceFrameA, "useLinearReferenceFrameA",
                false);
    }

    /**
     * De-serialize this joint, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        float dampingDirAng = capsule.readFloat("dampingDirAng", 0f);
        float dampingDirLin = capsule.readFloat("dampingDirLin", 0f);
        float dampingLimAng = capsule.readFloat("dampingLimAng", 0f);
        float dampingLimLin = capsule.readFloat("dampingLimLin", 0f);
        float dampingOrthoAng = capsule.readFloat("dampingOrthoAng", 0f);
        float dampingOrthoLin = capsule.readFloat("dampingOrthoLin", 0f);
        float lowerAngLimit = capsule.readFloat("lowerAngLimit", 0f);
        float lowerLinLimit = capsule.readFloat("lowerLinLimit", 0f);
        float maxAngMotorForce = capsule.readFloat("maxAngMotorForce", 0f);
        float maxLinMotorForce = capsule.readFloat("maxLinMotorForce", 0f);
        boolean poweredAngMotor = capsule.readBoolean("poweredAngMotor", false);
        boolean poweredLinMotor = capsule.readBoolean("poweredLinMotor", false);
        float restitutionDirAng = capsule.readFloat("restitutionDirAng", 0f);
        float restitutionDirLin = capsule.readFloat("restitutionDirLin", 0f);
        float restitutionLimAng = capsule.readFloat("restitutionLimAng", 0f);
        float restitutionLimLin = capsule.readFloat("restitutionLimLin", 0f);
        float restitutionOrthoAng
                = capsule.readFloat("restitutionOrthoAng", 0f);
        float restitutionOrthoLin
                = capsule.readFloat("restitutionOrthoLin", 0f);

        float softnessDirAng = capsule.readFloat("softnessDirAng", 0f);
        float softnessDirLin = capsule.readFloat("softnessDirLin", 0f);
        float softnessLimAng = capsule.readFloat("softnessLimAng", 0f);
        float softnessLimLin = capsule.readFloat("softnessLimLin", 0f);
        float softnessOrthoAng = capsule.readFloat("softnessOrthoAng", 0f);
        float softnessOrthoLin = capsule.readFloat("softnessOrthoLin", 0f);

        float targetAngMotorVelocity
                = capsule.readFloat("targetAngMotorVelicoty", 0f); //oops
        float targetLinMotorVelocity
                = capsule.readFloat("targetLinMotorVelicoty", 0f); //oops

        float upperAngLimit = capsule.readFloat("upperAngLimit", 0f);
        float upperLinLimit = capsule.readFloat("upperLinLimit", 0f);

        useLinearReferenceFrameA = capsule.readBoolean(
                "useLinearReferenceFrameA", false);

        createJoint();

        setDampingDirAng(dampingDirAng);
        setDampingDirLin(dampingDirLin);
        setDampingLimAng(dampingLimAng);
        setDampingLimLin(dampingLimLin);
        setDampingOrthoAng(dampingOrthoAng);
        setDampingOrthoLin(dampingOrthoLin);
        setLowerAngLimit(lowerAngLimit);
        setLowerLinLimit(lowerLinLimit);
        setMaxAngMotorForce(maxAngMotorForce);
        setMaxLinMotorForce(maxLinMotorForce);
        setPoweredAngMotor(poweredAngMotor);
        setPoweredLinMotor(poweredLinMotor);
        setRestitutionDirAng(restitutionDirAng);
        setRestitutionDirLin(restitutionDirLin);
        setRestitutionLimAng(restitutionLimAng);
        setRestitutionLimLin(restitutionLimLin);
        setRestitutionOrthoAng(restitutionOrthoAng);
        setRestitutionOrthoLin(restitutionOrthoLin);

        setSoftnessDirAng(softnessDirAng);
        setSoftnessDirLin(softnessDirLin);
        setSoftnessLimAng(softnessLimAng);
        setSoftnessLimLin(softnessLimLin);
        setSoftnessOrthoAng(softnessOrthoAng);
        setSoftnessOrthoLin(softnessOrthoLin);

        setTargetAngMotorVelocity(targetAngMotorVelocity);
        setTargetLinMotorVelocity(targetLinMotorVelocity);

        setUpperAngLimit(upperAngLimit);
        setUpperLinLimit(upperLinLimit);
    }
    // *************************************************************************
    // private methods

    /**
     * Instantiate the configured constraint in Bullet.
     */
    private void createJoint() {
        assert objectId == 0L;

        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(), pivotA,
                rotA, pivotB, rotB, useLinearReferenceFrameA);
        assert objectId != 0L;
        logger.log(Level.FINE, "Created Joint {0}", Long.toHexString(objectId));
    }

    native private long createJoint(long objectIdA, long objectIdB,
            Vector3f pivotA, Matrix3f rotA, Vector3f pivotB, Matrix3f rotB,
            boolean useLinearReferenceFrameA);

    native private float getDampingDirAng(long objectId);

    native private float getDampingDirLin(long objectId);

    native private float getDampingLimAng(long objectId);

    native private float getDampingLimLin(long objectId);

    native private float getDampingOrthoAng(long objectId);

    native private float getDampingOrthoLin(long objectId);

    native private float getLowerAngLimit(long objectId);

    native private float getLowerLinLimit(long objectId);

    native private float getMaxAngMotorForce(long objectId);

    native private float getMaxLinMotorForce(long objectId);

    native private float getRestitutionDirAng(long objectId);

    native private float getRestitutionDirLin(long objectId);

    native private float getRestitutionLimAng(long objectId);

    native private float getRestitutionLimLin(long objectId);

    native private float getRestitutionOrthoAng(long objectId);

    native private float getRestitutionOrthoLin(long objectId);

    native private float getSoftnessDirAng(long objectId);

    native private float getSoftnessDirLin(long objectId);

    native private float getSoftnessLimAng(long objectId);

    native private float getSoftnessLimLin(long objectId);

    native private float getSoftnessOrthoAng(long objectId);

    native private float getSoftnessOrthoLin(long objectId);

    native private float getTargetAngMotorVelocity(long objectId);

    native private float getTargetLinMotorVelocity(long objectId);

    native private float getUpperAngLimit(long objectId);

    native private float getUpperLinLimit(long objectId);

    native private boolean isPoweredAngMotor(long objectId);

    native private boolean isPoweredLinMotor(long objectId);

    native private void setDampingDirAng(long objectId, float value);

    native private void setDampingDirLin(long objectId, float value);

    native private void setDampingLimAng(long objectId, float value);

    native private void setDampingLimLin(long objectId, float value);

    native private void setDampingOrthoAng(long objectId, float value);

    native private void setDampingOrthoLin(long objectId, float value);

    native private void setLowerAngLimit(long objectId, float value);

    native private void setLowerLinLimit(long objectId, float value);

    native private void setMaxAngMotorForce(long objectId, float value);

    native private void setMaxLinMotorForce(long objectId, float value);

    native private void setPoweredAngMotor(long objectId, boolean value);

    native private void setPoweredLinMotor(long objectId, boolean value);

    native private void setRestitutionDirAng(long objectId, float value);

    native private void setRestitutionDirLin(long objectId, float value);

    native private void setRestitutionLimAng(long objectId, float value);

    native private void setRestitutionLimLin(long objectId, float value);

    native private void setRestitutionOrthoAng(long objectId, float value);

    native private void setRestitutionOrthoLin(long objectId, float value);

    native private void setSoftnessDirAng(long objectId, float value);

    native private void setSoftnessDirLin(long objectId, float value);

    native private void setSoftnessLimAng(long objectId, float value);

    native private void setSoftnessLimLin(long objectId, float value);

    native private void setSoftnessOrthoAng(long objectId, float value);

    native private void setSoftnessOrthoLin(long objectId, float value);

    native private void setTargetAngMotorVelocity(long objectId, float value);

    native private void setTargetLinMotorVelocity(long objectId, float value);

    native private void setUpperAngLimit(long objectId, float value);

    native private void setUpperLinLimit(long objectId, float value);
}

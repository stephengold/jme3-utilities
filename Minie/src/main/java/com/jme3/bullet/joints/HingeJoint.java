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
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A hinge joint based on Bullet's btHingeConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * Hinge constraint, or revolute joint restricts two additional angular degrees
 * of freedom, so the body can only rotate around one axis, the hinge axis. This
 * can be useful to represent doors or wheels rotating around one axis. The user
 * can specify limits and motor for the hinge.
 *
 * @author normenhansen
 */
public class HingeJoint extends PhysicsJoint {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(HingeJoint.class.getName());
    // *************************************************************************
    // fields

    private Vector3f axisA;
    private Vector3f axisB;

    /**
     * copy of the angular-only flag (default=false)
     */
    private boolean angularOnly = false;
    /**
     * copy of the limit's bias factor: how strictly position errors (drift) is
     * corrected (default=0.3)
     */
    private float biasFactor = 0.3f;
    /**
     * copy of the limit's relaxation factor: the rate at which velocity errors
     * are corrected (default=1)
     */
    private float relaxationFactor = 1f;
    /**
     * copy of the limit's softness: the range fraction at which velocity-error
     * correction starts operating (default=0.9)
     */
    private float limitSoftness = 0.9f;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public HingeJoint() {
    }

    /**
     * Instantiate a HingeJoint. To be effective, the joint must be added to a
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
     * @param axisA the local axis of the connection to node A (not null, alias
     * created) TODO
     * @param axisB the local axis of the connection to node B (not null, alias
     * created) TODO
     */
    public HingeJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB, Vector3f axisA, Vector3f axisB) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.axisA = axisA;
        this.axisB = axisB;
        createJoint();
        /*
         * Synchronize the btHingeConstraint with the local copies.
         */
        setAngularOnly(objectId, angularOnly);

        float low = getLowerLimit();
        float high = getUpperLimit();
        setLimit(objectId, low, high, limitSoftness, biasFactor,
                relaxationFactor);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable or disable this joint's motor.
     *
     * @param enable true to enable, false to disable
     * @param targetVelocity the desired target velocity
     * @param maxMotorImpulse the desired maximum rotational force
     */
    public void enableMotor(boolean enable, float targetVelocity,
            float maxMotorImpulse) {
        enableMotor(objectId, enable, targetVelocity, maxMotorImpulse);
    }

    /**
     * Read this joint's bias factor.
     *
     * @return the magnitude of the position correction: how strictly position
     * errors (drift) are corrected
     */
    public float getBiasFactor() {
        return biasFactor;
    }

    /**
     * Test whether this joint's motor is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean getEnableMotor() {
        return getEnableAngularMotor(objectId);
    }

    /**
     * Read this joint's limit softness.
     *
     * @return the range fraction at which velocity-error correction starts
     * operating
     */
    public float getLimitSoftness() {
        return limitSoftness;
    }

    /**
     * Read the motor's target velocity.
     *
     * @return velocity
     */
    public float getMotorTargetVelocity() {
        return getMotorTargetVelocity(objectId);
    }

    /**
     * Read the motor's maximum impulse.
     *
     * @return impulse
     */
    public float getMaxMotorImpulse() {
        return getMaxMotorImpulse(objectId);
    }

    /**
     * Read this joint's relaxation factor.
     *
     * @return the rate at which velocity errors are corrected
     */
    public float getRelaxationFactor() {
        return relaxationFactor;
    }

    /**
     * Test whether this joint is angular only.
     *
     * @return true if angular only, otherwise false
     */
    public boolean isAngularOnly() {
        return angularOnly;
    }

    /**
     * Alter this joint's limits. If you're above the softness, velocities that
     * would shoot through the actual limit are slowed down. The bias should be
     * in the range of 0.2 - 0.5.
     *
     * @param low the desired lower limit of the hinge angle (in radians)
     * @param high the desired upper limit of the joint angle (in radians)
     * @param _softness the desired range fraction at which velocity-error
     * correction starts operating. A softness of 0.9 means that the correction
     * starts at 90% of the limit range. (default=0.9)
     * @param _biasFactor the desired magnitude of the position correction: how
     * strictly position errors (drift) are corrected (default=0.3)
     * @param _relaxationFactor the desired rate at which velocity errors are
     * corrected. This can be seen as the strength of the limits. A low value
     * will make the limits more spongy. (default=1)
     */
    public void setLimit(float low, float high, float _softness,
            float _biasFactor, float _relaxationFactor) {
        biasFactor = _biasFactor;
        relaxationFactor = _relaxationFactor;
        limitSoftness = _softness;
        setLimit(objectId, low, high, _softness, _biasFactor, _relaxationFactor);
    }

    /**
     * Read the upper limit of the hinge angle.
     *
     * @return angle (in radians)
     */
    final public float getUpperLimit() {
        return getUpperLimit(objectId);
    }

    /**
     * Read the lower limit of the hinge angle.
     *
     * @return the angle (in radians)
     */
    final public float getLowerLimit() {
        return getLowerLimit(objectId);
    }

    /**
     * Alter the hinge translation flag.
     *
     * @param angularOnly true&rarr;rotate only, false&rarr;rotate and translate
     * (default=false)
     */
    public void setAngularOnly(boolean angularOnly) {
        this.angularOnly = angularOnly;
        setAngularOnly(objectId, angularOnly);
    }

    /**
     * Read the hinge angle.
     *
     * @return the angle (in radians)
     */
    public float getHingeAngle() {
        return getHingeAngle(objectId);
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
        axisA = cloner.clone(axisA);
        axisB = cloner.clone(axisB);
        createJoint();

        setAngularOnly(angularOnly);

        HingeJoint old = (HingeJoint) original;
        float low = old.getLowerLimit();
        float high = old.getUpperLimit();
        setLimit(low, high, limitSoftness, biasFactor, relaxationFactor);

        boolean enable = old.getEnableMotor();
        float targetVelocity = old.getMotorTargetVelocity();
        float maxImpulse = old.getMaxMotorImpulse();
        enableMotor(enable, targetVelocity, maxImpulse);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public HingeJoint jmeClone() {
        try {
            HingeJoint clone = (HingeJoint) super.clone();
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
        capsule.write(axisA, "axisA", new Vector3f());
        capsule.write(axisB, "axisB", new Vector3f());

        capsule.write(angularOnly, "angularOnly", false);

        capsule.write(getLowerLimit(), "lowerLimit", 1e30f);
        capsule.write(getUpperLimit(), "upperLimit", -1e30f);

        capsule.write(biasFactor, "biasFactor", 0.3f);
        capsule.write(relaxationFactor, "relaxationFactor", 1f);
        capsule.write(limitSoftness, "limitSoftness", 0.9f);

        capsule.write(getEnableMotor(), "enableAngularMotor", false);
        capsule.write(getMotorTargetVelocity(), "targetVelocity", 0f);
        capsule.write(getMaxMotorImpulse(), "maxMotorImpulse", 0f);
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
        axisA = (Vector3f) capsule.readSavable("axisA", new Vector3f());
        axisB = (Vector3f) capsule.readSavable("axisB", new Vector3f());

        angularOnly = capsule.readBoolean("angularOnly", false);

        float lowerLimit = capsule.readFloat("lowerLimit", 1e30f);
        float upperLimit = capsule.readFloat("upperLimit", -1e30f);
        biasFactor = capsule.readFloat("biasFactor", 0.3f);
        relaxationFactor = capsule.readFloat("relaxationFactor", 1f);
        limitSoftness = capsule.readFloat("limitSoftness", 0.9f);

        boolean enableAngularMotor
                = capsule.readBoolean("enableAngularMotor", false);
        float targetVelocity = capsule.readFloat("targetVelocity", 0f);
        float maxMotorImpulse = capsule.readFloat("maxMotorImpulse", 0f);

        createJoint();
        enableMotor(enableAngularMotor, targetVelocity, maxMotorImpulse);
        setAngularOnly(angularOnly);
        setLimit(lowerLimit, upperLimit, limitSoftness, biasFactor,
                relaxationFactor);
    }
    // *************************************************************************
    // private methods

    /**
     * Create the configured joint in Bullet.
     */
    private void createJoint() {
        assert objectId == 0L;

        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(),
                pivotA, axisA, pivotB, axisB);
        assert objectId != 0L;
        logger2.log(Level.FINE, "Created Joint {0}", Long.toHexString(objectId));
    }

    native private long createJoint(long objectIdA, long objectIdB,
            Vector3f pivotA, Vector3f axisA, Vector3f pivotB, Vector3f axisB);

    native private void enableMotor(long objectId, boolean enable,
            float targetVelocity, float maxMotorImpulse);

    native private boolean getEnableAngularMotor(long objectId);

    native private float getHingeAngle(long objectId);

    native private float getLowerLimit(long objectId);

    native private float getMaxMotorImpulse(long objectId);

    native private float getMotorTargetVelocity(long objectId);

    native private float getUpperLimit(long objectId);

    native private void setAngularOnly(long objectId, boolean angularOnly);

    native private void setLimit(long objectId, float low, float high,
            float softness, float biasFactor, float relaxationFactor);
}

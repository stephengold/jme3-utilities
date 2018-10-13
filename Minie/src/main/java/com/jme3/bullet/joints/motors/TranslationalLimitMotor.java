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
package com.jme3.bullet.joints.motors;

import com.jme3.math.Vector3f;
import java.util.logging.Logger;

/**
 * A motor based on Bullet's btTranslationalLimitMotor. Motors are used to drive
 * joints.
 *
 * @author normenhansen
 */
public class TranslationalLimitMotor {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TranslationalLimitMotor.class.getName());
    // *************************************************************************
    // fields

    /**
     * Unique identifier of the btTranslationalLimitMotor. The constructor sets
     * this to a non-zero value. After that, the id never changes.
     */
    private long motorId = 0L;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a motor for the identified btTranslationalLimitMotor.
     *
     * @param motor the unique identifier (not zero)
     */
    public TranslationalLimitMotor(long motor) {
        assert motor != 0L;
        motorId = motor;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the id of the btTranslationalLimitMotor.
     *
     * @return the unique identifier (not zero)
     */
    public long getMotor() {
        assert motorId != 0L;
        return motorId;
    }

    /**
     * Copy this motor's constraint lower limits.
     *
     * @return a new vector (not null)
     */
    public Vector3f getLowerLimit() {
        Vector3f vec = new Vector3f();
        getLowerLimit(motorId, vec);

        return vec;
    }

    /**
     * Alter this motor's constraint lower limits.
     *
     * @param lowerLimit (unaffected, not null, default=0,0,0)
     */
    public void setLowerLimit(Vector3f lowerLimit) {
        setLowerLimit(motorId, lowerLimit);
    }

    /**
     * Copy this motor's constraint upper limits.
     *
     * @return a new vector (not null, default=0,0,0)
     */
    public Vector3f getUpperLimit() {
        Vector3f vec = new Vector3f();
        getUpperLimit(motorId, vec);

        return vec;
    }

    /**
     * Alter the constraint upper limits.
     *
     * @param upperLimit (unaffected, not null)
     */
    public void setUpperLimit(Vector3f upperLimit) {
        setUpperLimit(motorId, upperLimit);
    }

    /**
     * Copy the accumulated impulse.
     *
     * @return a new vector (not null)
     */
    public Vector3f getAccumulatedImpulse() {
        Vector3f vec = new Vector3f();
        getAccumulatedImpulse(motorId, vec);

        return vec;
    }

    /**
     * Alter the accumulated impulse.
     *
     * @param accumulatedImpulse the desired vector (not null, unaffected)
     */
    public void setAccumulatedImpulse(Vector3f accumulatedImpulse) {
        setAccumulatedImpulse(motorId, accumulatedImpulse);
    }

    /**
     * Read this motor's limit softness.
     *
     * @return the softness
     */
    public float getLimitSoftness() {
        return getLimitSoftness(motorId);
    }

    /**
     * Alter the limit softness.
     *
     * @param limitSoftness the desired limit softness (default=0.7)
     */
    public void setLimitSoftness(float limitSoftness) {
        setLimitSoftness(motorId, limitSoftness);
    }

    /**
     * Read this motor's damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDamping() {
        return getDamping(motorId);
    }

    /**
     * Alter this motor's damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=1)
     */
    public void setDamping(float damping) {
        setDamping(motorId, damping);
    }

    /**
     * Read this motor's restitution.
     *
     * @return the restitution (bounce) factor
     */
    public float getRestitution() {
        return getRestitution(motorId);
    }

    /**
     * Alter this motor's restitution.
     *
     * @param restitution the desired restitution (bounce) factor (default=0.5)
     */
    public void setRestitution(float restitution) {
        setRestitution(motorId, restitution);
    }
    // *************************************************************************
    // private methods

    native private void getAccumulatedImpulse(long motorId, Vector3f vector);

    native private float getDamping(long motorId);

    native private void getERP(long motorId, Vector3f vector);

    native private float getLimitSoftness(long motorId);

    native private void getLowerLimit(long motorId, Vector3f vector);

    native private void getMaxMotorForce(long motorId, Vector3f vector);

    native private void getNormalCFM(long motorId, Vector3f vector);

    native private float getRestitution(long motorId);

    native private void getStopCFM(long motorId, Vector3f vector);

    native private void getUpperLimit(long motorId, Vector3f vector);

    native private void setAccumulatedImpulse(long motorId, Vector3f vector);

    native private void setDamping(long motorId, float damping);

    native private void setERP(long motorId, Vector3f vector);

    native private void setLimitSoftness(long motorId, float limitSoftness);

    native private void setLowerLimit(long motorId, Vector3f vector);

    native private void setMaxMotorForce(long motorId, Vector3f vector);

    native private void setNormalCFM(long motorId, Vector3f vector);

    native private void setRestitution(long motorId, float restitution);

    native private void setStopCFM(long motorId, Vector3f vector);

    native private void setUpperLimit(long motorId, Vector3f vector);
}

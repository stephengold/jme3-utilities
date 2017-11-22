/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.math;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Mathematical utility methods.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyQuaternion {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyQuaternion.class.getName());
    /**
     * array of cardinal axes
     */
    final private static Vector3f cardinalAxes[] = {
        new Vector3f(1f, 0f, 0f),
        new Vector3f(0f, 1f, 0f),
        new Vector3f(0f, 0f, 1f),
        new Vector3f(-1f, 0f, 0f),
        new Vector3f(0f, -1f, 0f),
        new Vector3f(0f, 0f, -1f)
    };
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyQuaternion() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Accumulate a linear combination of quaternions.
     *
     * @param total sum of the scaled inputs so far (not null, updated)
     * @param input the quaternion to scale and add (not null, unaffected)
     * @param scale scale factor to apply to the input
     */
    public static void accumulateScaled(Quaternion total, Quaternion input,
            float scale) {
        Validate.nonNull(total, "total");
        Validate.nonNull(input, "input");

        float x = total.getX() + input.getX() * scale;
        float y = total.getY() + input.getY() * scale;
        float z = total.getZ() + input.getZ() * scale;
        float w = total.getW() + input.getW() * scale;
        total.set(x, y, z, w);
    }

    /**
     * Find the cardinal rotation most similar to the specified input. A
     * cardinal rotation is one for which the rotation angles on all 3 axes are
     * integer multiples of Pi/2 radians.
     *
     * @param input (not null, modified)
     */
    public static void cardinalizeLocal(Quaternion input) {
        Validate.nonNull(input, "input");

        input.normalizeLocal();
        /*
         * Generate each of the 24 cardinal rotations.
         */
        Quaternion cardinalRotation = new Quaternion();
        Quaternion bestCardinalRotation = new Quaternion();
        Vector3f z = new Vector3f();
        float bestAbsDot = -1f;
        for (Vector3f x : cardinalAxes) {
            for (Vector3f y : cardinalAxes) {
                x.cross(y, z);
                if (z.isUnitVector()) {
                    cardinalRotation.fromAxes(x, y, z);
                    /*
                     * Measure the similarity of the 2 rotations
                     * using the absolute value of their dot product.
                     */
                    float dot = cardinalRotation.dot(input);
                    float absDot = FastMath.abs(dot);
                    if (absDot > bestAbsDot) {
                        bestAbsDot = absDot;
                        bestCardinalRotation.set(cardinalRotation);
                    }
                }
            }
        }

        input.set(bestCardinalRotation);
    }

    /**
     * Calculate the conjugate of a quaternion. For unit quaternions, the
     * conjugate is a faster way to calculate the inverse.
     *
     * @param q input value (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a conjugate quaternion (either storeResult or a new instance)
     */
    public static Quaternion conjugate(Quaternion q, Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float qx = q.getX();
        float qy = q.getY();
        float qz = q.getZ();
        float qw = q.getW();
        storeResult.set(-qx, -qy, -qz, qw);

        return storeResult;
    }

    /**
     * Calculate the exponential of a pure quaternion.
     *
     * @param q input value (not null, unaffected, w=0)
     * @param storeResult (modified if not null)
     * @return a unit quaternion (either storeResult or a new instance)
     */
    public static Quaternion exp(Quaternion q, Quaternion storeResult) {
        assert q.getW() == 0f : q;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        double qx = q.getX();
        double qy = q.getY();
        double qz = q.getZ();
        double theta = MyMath.hypotenuse(qx, qy, qz);
        if (theta == 0.0) {
            storeResult.loadIdentity();
        } else {
            float w = (float) Math.cos(theta);
            double scale = Math.sin(theta) / theta;
            float x = (float) (scale * qx);
            float y = (float) (scale * qy);
            float z = (float) (scale * qz);
            storeResult.set(x, y, z, w);
        }

        return storeResult;
    }

    /**
     * Test whether the specified quaternion represents an identity rotation.
     * This test is weaker than that implemented by
     * {@link com.jme3.math.Quaternion#isIdentity()} because it accepts any
     * non-zero value for w.
     *
     * @param q input value (not null, unaffected)
     * @return true for a rotation identity, otherwise false
     */
    public static boolean isRotationIdentity(Quaternion q) {
        float qx = q.getX();
        float qy = q.getY();
        float qz = q.getZ();
        float qw = q.getW();

        if (qx == 0f && qy == 0f && qz == 0f && qw != 0f) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculate the natural logarithm of a unit quaternion. Generally the
     * logarithm isn't itself a unit.
     *
     * @param q input value (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return a pure quaternion (either storeResult or a new instance)
     */
    public static Quaternion log(Quaternion q, Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float qw = q.getW();
        if (qw >= 1f || qw <= -1f) {
            storeResult.set(0f, 0f, 0f, 0f);
        } else {
            double qx = q.getX();
            double qy = q.getY();
            double qz = q.getZ();
            double sineTheta = MyMath.hypotenuse(qx, qy, qz);
            sineTheta = MyMath.clamp(sineTheta, 0.0, 1.0);
            if (sineTheta == 0.0) {
                storeResult.set(0f, 0f, 0f, 0f);
            } else {
                double theta = Math.asin(sineTheta);
                double scale = theta / sineTheta;
                float x = (float) (scale * qx);
                float y = (float) (scale * qy);
                float z = (float) (scale * qz);
                storeResult.set(x, y, z, 0f);
            }
        }

        return storeResult;
    }

    /**
     * Test whether two quaternions are distinct, without distinguishing 0 from
     * -0.
     *
     * @param a 1st input quaternion (not null, unaffected)
     * @param b 2nd input quaternion (not null, unaffected)
     * @return true if distinct, otherwise false
     */
    public static boolean ne(Quaternion a, Quaternion b) {
        Validate.nonNull(a, "1st input quaternion");
        Validate.nonNull(b, "2nd input quaternion");

        boolean result = a.getW() != b.getW()
                || a.getX() != b.getX()
                || a.getY() != b.getY()
                || a.getZ() != b.getZ();
        return result;
    }

    /**
     * Raise a unit quaternion to the specified real power.
     *
     * @param base input value (not null, unaffected, norm=1)
     * @param exponent the exponent
     * @param storeResult (modified if not null)
     * @return a unit quaternion (either storeResult or a new instance)
     */
    public static Quaternion pow(Quaternion base, float exponent,
            Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float baseW = base.getW();
        if (baseW >= 1f || baseW <= -1f || exponent == 0f) {
            storeResult.loadIdentity();
        } else {
            double baseX = base.getX();
            double baseY = base.getY();
            double baseZ = base.getZ();
            double sineTheta = MyMath.hypotenuse(baseX, baseY, baseZ);
            sineTheta = MyMath.clamp(sineTheta, 0.0, 1.0);
            if (sineTheta == 0.0) {
                storeResult.loadIdentity();
            } else {
                double theta = Math.asin(sineTheta);
                float w = (float) Math.cos(exponent * theta);
                double scale = Math.sin(exponent * theta) / sineTheta;
                float x = (float) (scale * baseX);
                float y = (float) (scale * baseY);
                float z = (float) (scale * baseZ);
                storeResult.set(x, y, z, w);
            }
        }

        return storeResult;
    }

    /**
     * Interpolate between 2 unit quaternions using spherical linear (Slerp)
     * interpolation. This method is slower (but more accurate) than
     * {@link com.jme3.math.Quaternion#slerp(com.jme3.math.Quaternion, float)},
     * always produces a unit, and doesn't trash q1. The caller is responsible
     * for flipping the sign of q0 or q1 when it's appropriate to do so.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value at t=0 (not null, unaffected, norm=1)
     * @param q1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion slerp(float t, Quaternion q0, Quaternion q1,
            Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(q0, "q0", 0.0001f);
        MyQuaternion.validateUnit(q1, "q1", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q0inverse = MyQuaternion.conjugate(q0, null);
        Quaternion ratio = q0inverse.multLocal(q1);
        Quaternion power = MyQuaternion.pow(ratio, t, ratio);
        storeResult.set(q0);
        storeResult.multLocal(power);

        return storeResult;
    }

    /**
     * Round the rotation angle of the indexed axis to the nearest Pi/2 radians.
     *
     * @param input (not null, modified)
     * @param axisIndex which axis (&ge;0, &lt;3)
     */
    public static void snapLocal(Quaternion input, int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        float[] angles = new float[MyVector3f.numAxes];
        input.toAngles(angles);
        double angle = angles[axisIndex];
        angle = MyMath.halfPi * Math.round(angle / MyMath.halfPi);
        angles[axisIndex] = (float) angle;
        input.fromAngles(angles);
    }

    /**
     * Interpolate between 4 unit quaternions using the Squad function. The
     * caller is responsible for flipping signs when it's appropriate to do so.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param p function value at t=0 (not null, unaffected, norm=1)
     * @param a 1st control point (not null, unaffected, norm=1)
     * @param b 2nd control point (not null, unaffected, norm=1)
     * @param q function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion squad(float t, Quaternion p, Quaternion a,
            Quaternion b, Quaternion q, Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(p, "p", 0.0001f);
        MyQuaternion.validateUnit(a, "a", 0.0001f);
        MyQuaternion.validateUnit(b, "b", 0.0001f);
        MyQuaternion.validateUnit(q, "q", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion qSlerp = slerp(t, p, q, null);
        Quaternion aSlerp = slerp(t, a, b, null);
        slerp(2f * t * (1f - t), qSlerp, aSlerp, storeResult);

        return storeResult;
    }

    /**
     * Calculate Squad parameter "a" for a continuous 1st derivative at the
     * middle point of 3 specified control points.
     *
     * @param q0 previous control point (not null, unaffected, norm=1)
     * @param q1 current control point (not null, unaffected, norm=1)
     * @param q2 following control point (not null, unaffected, norm=1)
     * @param storeResult (modified if not null)
     * @return a unit quaternion for use as a Squad parameter (either
     * storeResult or a new instance)
     */
    public static Quaternion squadA(Quaternion q0, Quaternion q1,
            Quaternion q2, Quaternion storeResult) {
        MyQuaternion.validateUnit(q0, "q0", 0.0001f);
        MyQuaternion.validateUnit(q1, "q1", 0.0001f);
        MyQuaternion.validateUnit(q2, "q2", 0.0001f);
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Quaternion q1c = MyQuaternion.conjugate(q1, null);
        Quaternion turn0 = q1c.mult(q0);
        Quaternion logTurn0 = MyQuaternion.log(turn0, turn0);
        Quaternion turn2 = q1c.mult(q2);
        Quaternion logTurn2 = MyQuaternion.log(turn2, turn2);
        Quaternion sum = logTurn2.addLocal(logTurn0);
        sum.multLocal(-0.25f);
        Quaternion exp = MyQuaternion.exp(sum, sum);
        storeResult.set(q1);
        storeResult.multLocal(exp);

        return storeResult;
    }

    /**
     * Standardize a quaternion in preparation for hashing.
     *
     * @param input (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return an equivalent quaternion without negative zeroes (either
     * storeResult or a new instance)
     */
    public static Quaternion standardize(Quaternion input,
            Quaternion storeResult) {
        Validate.nonNull(input, "input quaternion");
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        float w = input.getW();
        float x = input.getX();
        float y = input.getY();
        float z = input.getZ();
        w = MyMath.standardize(w);
        x = MyMath.standardize(x);
        y = MyMath.standardize(y);
        z = MyMath.standardize(z);
        storeResult.set(x, y, z, w);

        return storeResult;
    }

    /**
     * Validate a unit quaternion as a method argument.
     *
     * @param q quaternion to validate (not null, unaffected)
     * @param description description of the quaternion
     * @param tolerance for the norm (&ge;0)
     * @throws IllegalArgumentException if the norm is out of tolerance
     * @throws NullPointerException if the quaternion is null
     */
    public static void validateUnit(Quaternion q, String description,
            float tolerance) {
        Validate.nonNull(q, description);

        float norm = q.norm();
        float delta = FastMath.abs(1f - norm);
        if (!(delta <= tolerance)) {
            String what;
            if (description == null) {
                what = "quaternion argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "norm({0})={1}", new Object[]{what, norm});
            String message = String.format(
                    "norm(%s) must be within %f of 1.", what, tolerance);
            throw new IllegalArgumentException(message);
        }
    }
}

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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.math;

import com.jme3.math.Quaternion;
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
    final private static Logger logger = Logger.getLogger(
            MyQuaternion.class.getName());
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
     * Round the rotation angle of the indexed axis to the nearest Pi/2 radians.
     *
     * @param input (not null, modified)
     * @param axisIndex which axis (&ge;0, &lt;3)
     */
    public static void snapLocal(Quaternion input, int axisIndex) {
        float[] angles = new float[3];
        input.toAngles(angles);
        double angle = angles[axisIndex];
        angle = MyMath.halfPi * Math.round(angle / MyMath.halfPi);
        angles[axisIndex] = (float) angle;
        input.fromAngles(angles);
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
}

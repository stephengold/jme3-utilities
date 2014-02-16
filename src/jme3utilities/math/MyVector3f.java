/*
 Copyright (c) 2013-2014, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.debug.Validate;

/**
 * Utility methods for 3-D vectors.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MyVector3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyVector3f.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyVector3f() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Compute the altitude angle of a non-zero offset.
     *
     * @param offset difference of world coordinates (not null, not zero length,
     * not altered)
     * @return angle above the X-Z plane (in radians, &le;Pi/2, &ge;-Pi/2)
     */
    public static float altitude(Vector3f offset) {
        Validate.nonNull(offset, "offset");
        if (isZeroLength(offset)) {
            logger.log(Level.SEVERE, "offset={0}", offset);
            throw new IllegalArgumentException(
                    "offset should have positive length");
        }

        float xzRange = MyMath.hypotenuse(offset.x, offset.z);
        float result = (float) Math.atan2(offset.y, xzRange);

        assert result <= FastMath.HALF_PI : result;
        assert result >= -FastMath.HALF_PI : result;
        return result;
    }

    /**
     * Compute the azimuth angle of an offset.
     *
     * @param offset difference of world coordinates (not null, not altered)
     * @return horizontal angle in radians (measured CW from the X axis) or 0 if
     * the vector is zero or parallel to the Y axis.
     */
    public static float azimuth(Vector3f offset) {
        if (offset.x == 0f && offset.z == 0f) {
            return 0f;
        }
        float result = (float) Math.atan2(offset.z, offset.x);
        return result;
    }

    /**
     * Compute a horizontal direction of an offset.
     *
     * @param offset difference of world coordinates (not null, not altered)
     * @return a new unit vector
     */
    public static VectorXZ direction(Vector3f offset) {
        Validate.nonNull(offset, "offset");

        VectorXZ result = new VectorXZ(offset);
        result.normalizeLocal();
        return result;
    }

    /**
     * Compute the distance from one location to another.
     *
     * @param from world coordinates of starting location (not null, not
     * altered)
     * @param to world coordinates of ending location (not null, not altered)
     * @return distance (in world units, &ge;0)
     */
    public static float distanceFrom(Vector3f from, Vector3f to) {
        Validate.nonNull(from, "vector");

        Vector3f offset = to.subtract(from);
        float distance = offset.length();
        return distance;
    }

    /**
     * Generate a direction from altitude and azimuth angles.
     *
     * @param altitude angle above the X-Z plane (radians toward +Y)
     * @param azimuth angle in the X-Z plane (radians CCW from +X)
     * @return a new unit vector
     */
    public static Vector3f fromAltAz(float altitude, float azimuth) {
        Quaternion elevate = new Quaternion();
        elevate.fromAngleNormalAxis(altitude, Vector3f.UNIT_Z);
        Vector3f elevation = elevate.mult(Vector3f.UNIT_X);
        Vector3f direction = MyVector3f.yRotate(elevation, azimuth);

        assert direction.isUnitVector() : direction;
        return direction;
    }

    /**
     * Test whether all components of a vector are all non-negative.
     *
     * @param vector (not null, not altered)
     */
    public static boolean isAllNonNegative(Vector3f vector) {
        boolean result = (vector.x >= 0f && vector.y >= 0f && vector.z >= 0f);
        return result;
    }

    /**
     * Test a vector for zero length.
     *
     * @param vector (not null, not altered)
     * @return true if the vector has zero length, false otherwise
     */
    public static boolean isZeroLength(Vector3f vector) {
        boolean result = (vector.x == 0f && vector.y == 0f && vector.z == 0f);
        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: Used for applying
     * azimuths, which is why its rotation angle convention is non-standard.
     *
     * @param input (not null, not altered)
     * @param angle clockwise (LH) angle of rotation in radians
     * @return a new vector
     */
    public static Vector3f yRotate(Vector3f input, float angle) {
        float cosine = FastMath.cos(angle);
        float sine = FastMath.sin(angle);
        float x = cosine * input.x - sine * input.z;
        float y = input.y;
        float z = cosine * input.z + sine * input.x;
        Vector3f result = new Vector3f(x, y, z);

        return result;
    }
    // *************************************************************************
    // test cases

    /**
     * Console application to test the MyVector3f class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyVector3f:\n\n");

        // vector test cases
        Vector3f[] vectorCases = new Vector3f[]{
            Vector3f.ZERO,
            Vector3f.UNIT_X,
            Vector3f.UNIT_Z
        };

        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            System.out.printf(" yRotate(v, 0) = %s%n",
                    yRotate(v, 0).toString());
            System.out.printf(" yRotate(v, PI/2) = %s%n",
                    yRotate(v, FastMath.HALF_PI).toString());
            System.out.printf(" yRotate(v, PI) = %s%n",
                    yRotate(v, FastMath.PI).toString());
            System.out.printf(" yRotate(v, 2*PI) = %s%n",
                    yRotate(v, FastMath.TWO_PI).toString());
            System.out.println();
        }
        System.out.println();
    }
}
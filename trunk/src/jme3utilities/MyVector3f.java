// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;

/**
 * 3-D vector utility methods.
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
     * Calculate the altitude angle of a non-zero offset.
     *
     * @param offset difference of world coordinates (not null, not zero length,
     * not altered)
     * @return angle above the X-Z plane (in radians, <=Pi/2, >=-Pi/2)
     */
    public static float altitude(Vector3f offset) {
        if (offset == null) {
            throw new NullPointerException("direction cannot be null");
        }
        if (isZeroLength(offset)) {
            throw new IllegalArgumentException(
                    "direction must have positive length");
        }

        float xzRange = MyMath.hypotenuse(offset.x, offset.z);
        float result = (float) Math.atan2(offset.y, xzRange);

        assert result <= FastMath.HALF_PI : result;
        assert result >= -FastMath.HALF_PI : result;
        return result;
    }

    /**
     * Calculate the azimuth angle of an offset.
     *
     * @param offset difference of world coordinates (not null, not altered)
     * @return horizontal angle in radians (measured CW from the X axis) or 0 if
     * the vector is zero or parallel to the Y axis.
     */
    public static float azimuth(Vector3f offset) {
        if (offset == null) {
            throw new NullPointerException("direction cannot be null");
        }

        if (offset.x == 0f && offset.z == 0f) {
            return 0f;
        }
        float result = (float) Math.atan2(offset.z, offset.x);
        return result;
    }

    /**
     * Calculate a horizontal direction of an offset.
     *
     * @param offset difference of world coordinates (not null, not altered)
     * @return a new unit vector
     */
    public static VectorXZ direction(Vector3f offset) {
        if (offset == null) {
            throw new NullPointerException("offset cannot be null");
        }

        VectorXZ result = new VectorXZ(offset);
        result.normalizeLocal();
        return result;
    }

    /**
     * Calculate the distance from one location to another.
     *
     * @param from world coordinates of starting location (not null, not
     * altered)
     * @param to world coordinates of ending location (not null, not altered)
     * @return distance (in world units, >=0)
     */
    public static float distanceFrom(Vector3f from, Vector3f to) {
        assert from != null;
        assert to != null;

        Vector3f offset = to.subtract(from);
        float distance = offset.length();
        return distance;
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
     * @param vector input (not null, not altered)
     * @param radians clockwise (LH) angle of rotation in radians
     * @return a new vector
     */
    public static Vector3f yRotate(Vector3f vector, float radians) {
        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        float x = cosine * vector.x - sine * vector.z;
        float y = vector.y;
        float z = cosine * vector.z + sine * vector.x;
        Vector3f result = new Vector3f(x, y, z);
        return result;
    }
    // *************************************************************************
    // test cases

    /**
     * Test cases for this class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyVector3f:\n");

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
/*
 Copyright (c) 2013-2017, Stephen Gold
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
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods for 3-D vectors.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyVector3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyVector3f.class.getName());
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
     * @param offset difference of world coordinates (length&gt;0, unaffected)
     * @return angle above the X-Z plane (in radians, &le;Pi/2, &ge;-Pi/2)
     */
    public static float altitude(Vector3f offset) {
        Validate.nonZero(offset, "offset");

        float xzRange = MyMath.hypotenuse(offset.x, offset.z);
        float result = (float) Math.atan2(offset.y, xzRange);

        assert result <= FastMath.HALF_PI : result;
        assert result >= -FastMath.HALF_PI : result;
        return result;
    }

    /**
     * Test whether three points are collinear.
     *
     * @param point1 location of the 1st point (not null, unaffected)
     * @param point2 location of the 2nd point (not null, unaffected)
     * @param point3 location of the 3rd point (not null, unaffected)
     * @param tolerance2 for coincidence (in squared units, &ge;0)
     * @return true if collinear, otherwise false
     */
    public static boolean areCollinear(Vector3f point1, Vector3f point2,
            Vector3f point3, float tolerance2) {
        Validate.nonNull(point1, "first point");
        Validate.nonNull(point2, "second point");
        Validate.nonNegative(tolerance2, "tolerance");
        /*
         * Shortcut:
         * If point1 and point3 coincide, then the three points are collinear. 
         */
        Vector3f offset3 = point3.subtract(point1);
        double normSquared3 = lengthSquared(offset3);
        if (normSquared3 <= tolerance2) {
            return true;
        }
        /*
         * The long way:
         * Calculate the projection of offset2 onto offset3.
         * 
         * Don't use Vector3f.project() because (as of jME 3.0.10) it contains 
         * a logic bug.
         */
        Vector3f offset2 = point2.subtract(point1);
        double scaleFactor = MyVector3f.dot(offset2, offset3) / normSquared3;
        Vector3f projection = offset3.mult((float) scaleFactor);
        /*
         * If the projection coincides with offset2, 
         * then the three points are collinear.
         */
        boolean result = doCoincide(projection, offset2, tolerance2);

        return result;
    }

    /**
     * Compute the azimuth angle of an offset.
     *
     * @param offset difference of world coordinates (not null, unaffected)
     * @return horizontal angle in radians (measured CW from the X axis) or 0 if
     * the vector is zero or parallel to the Y axis.
     */
    public static float azimuth(Vector3f offset) {
        float result = (float) Math.atan2(offset.z, offset.x);
        return result;
    }

    /**
     * Compare two vectors lexicographically, with the x-component having
     * priority.
     *
     * @param v1 1st input vector (not null, unaffected)
     * @param v2 2nd input vector (not null, unaffected)
     * @return 0 if v1 is equal to v2; negative if v1 comes before v2; positive
     * if v1 comes after v2
     */
    public static int compare(Vector3f v1, Vector3f v2) {
        int result;

        if (v1.x != v2.x) {
            result = Float.compare(v1.x, v2.x);
        } else if (v1.y != v2.y) {
            result = Float.compare(v1.y, v2.y);
        } else {
            result = Float.compare(v1.z, v2.z);
        }

        return result;
    }

    /**
     * Compute the squared distance between two vectors. Unlike
     * {@link com.jme3.math.Vector3f#distanceSquared(Vector3f)}, this method
     * returns a double-precision value for precise comparison of distances.
     *
     * @param vector1 1st input vector (not null, unaffected)
     * @param vector2 2nd input vector (not null, unaffected)
     * @return the squared distance (&ge;0)
     */
    public static double distanceSquared(Vector3f vector1, Vector3f vector2) {
        double dx = vector1.x - vector2.x;
        double dy = vector1.y - vector2.y;
        double dz = vector1.z - vector2.z;
        double result = dx * dx + dy * dy + dz * dz;

        return result;
    }

    /**
     * Test whether two points coincide.
     *
     * @param point1 coordinates of the 1st point (not null, unaffected)
     * @param point2 coordinates of the 2nd point (not null, unaffected)
     * @param tolerance2 for coincidence (in squared units, &ge;0)
     * @return true if they coincide, otherwise false
     */
    public static boolean doCoincide(Vector3f point1, Vector3f point2,
            float tolerance2) {
        Validate.nonNull(point1, "first point");
        Validate.nonNull(point2, "second point");
        Validate.nonNegative(tolerance2, "tolerance");

        double d2 = MyVector3f.distanceSquared(point1, point2);
        if (d2 > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Compute the dot (scalar) product of two vectors. Unlike
     * {@link com.jme3.math.Vector3f#dot(Vector3f)}, this method returns a
     * double-precision value for precise calculation of angles.
     *
     * @param vector1 1st input vector (not null, unaffected)
     * @param vector2 2nd input vector (not null, unaffected)
     * @return the dot product
     */
    public static double dot(Vector3f vector1, Vector3f vector2) {
        double x1 = vector1.x;
        double x2 = vector2.x;
        double y1 = vector1.y;
        double y2 = vector2.y;
        double z1 = vector1.z;
        double z2 = vector2.z;
        double product = x1 * x2 + y1 * y2 + z1 * z2;

        return product;
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
     * Compute the horizontal direction of an offset in world space.
     *
     * @param offset difference of world coordinates (not null, unaffected)
     * @return a unit vector or a zero vector
     */
    public static VectorXZ horizontalDirection(Vector3f offset) {
        Validate.nonNull(offset, "offset");

        VectorXZ horizontalOffset = new VectorXZ(offset);
        VectorXZ result = horizontalOffset.normalize();

        return result;
    }

    /**
     * Test whether all components of a vector are all non-negative.
     *
     * @param vector input (not null, unaffected)
     * @return true if all non-negative, false otherwise
     */
    public static boolean isAllNonNegative(Vector3f vector) {
        if (vector.x < 0f || vector.y < 0f || vector.z < 0f) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test a vector for zero length.
     *
     * @param vector input (not null, unaffected)
     * @return true if the vector has zero length, false otherwise
     */
    public static boolean isZeroLength(Vector3f vector) {
        if (vector.x == 0f && vector.y == 0f && vector.z == 0f) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compute the squared length of a vector. Unlike
     * {@link com.jme3.math.Vector3f#lengthSquared()}, this method returns a
     * double-precision value for precise comparison of lengths.
     *
     * @param vector input (not null, unaffected)
     * @return the squared length (&ge;0)
     */
    public static double lengthSquared(Vector3f vector) {
        double xx = vector.x;
        double yy = vector.y;
        double zz = vector.z;
        double result = xx * xx + yy * yy + zz * zz;

        return result;
    }

    /**
     * Project vector1 onto vector2. Don't use
     * {@link com.jme3.math.Vector3f#project(Vector3f)} for this because (as of
     * jME 3.0.10) it contains a logic bug which gives the wrong magnitude when
     * vector2 has length != 1.
     *
     * @param vector1 1st input vector (not null, unaffected)
     * @param vector2 2nd input vector (length&gt;0, unaffected)
     * @return a new vector with the same direction as vector2
     */
    public static Vector3f projection(Vector3f vector1, Vector3f vector2) {
        Validate.nonZero(vector2, "vector2");

        double lengthSquared = lengthSquared(vector2);
        double dot = MyVector3f.dot(vector1, vector2);
        double scaleFactor = dot / lengthSquared;
        Vector3f projection = vector2.mult((float) scaleFactor);

        return projection;
    }

    /**
     * Project vector1 onto vector2.
     *
     * @param vector1 1st input vector (not null, unaffected)
     * @param vector2 2nd input vector (length&gt;0, unaffected)
     * @return the scalar projection of vector1 onto vector2
     */
    public static float scalarProjection(Vector3f vector1, Vector3f vector2) {
        Validate.nonZero(vector2, "vector2");

        float dot = vector1.dot(vector2);
        float norm = vector2.length();
        float projection = dot / norm;

        return projection;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: Used for applying
     * azimuths, which is why its rotation angle convention is non-standard.
     *
     * @param input (not null, unaffected)
     * @param angle clockwise (LH) angle of rotation in radians
     * @return new vector
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
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyVector3f:\n\n");

        // vector test cases
        Vector3f[] vectorCases = new Vector3f[]{
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.5f, 4.5f, 11.5f),
            Vector3f.ZERO,
            Vector3f.UNIT_X,
            Vector3f.UNIT_Z
        };

        System.out.println("Testing yRotate():");
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

        System.out.println("Testing projection():");
        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            for (Vector3f w : vectorCases) {
                System.out.printf(" w = %s%n", w.toString());
                if (w.length() != 0f) {
                    System.out.printf("  v proj w = %s%n",
                            projection(v, w).toString());
                }
                //System.out.printf("             %s%n",
                //        v.project(w).toString());
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Testing doCoincide() with tolerance2 = 0.9:");
        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            for (Vector3f w : vectorCases) {
                System.out.printf(" w = %s%n", w.toString());
                if (doCoincide(v, w, 0.9f)) {
                    System.out.printf("  v coincides with w%n");
                } else {
                    System.out.printf("  v does not coincide with w%n");
                }
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Testing areCollinear():");
        Vector3f p1 = new Vector3f(1f, 2f, 3f);
        Vector3f p2 = new Vector3f(2f, 0f, 5f);
        Vector3f p3 = new Vector3f(4f, -4f, 9f);
        Vector3f p2bad = new Vector3f(2f, 1f, 5f);

        assert areCollinear(p1, p1, p1, 0.01f);
        assert areCollinear(p1, p2, p3, 0.01f);
        assert areCollinear(p1, p2, p2, 0.01f);
        assert !areCollinear(p1, p2bad, p3, 0.01f);

        System.out.println("Success!");
    }
}

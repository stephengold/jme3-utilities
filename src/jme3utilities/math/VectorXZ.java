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
 * Immutable single-precision vector with no 'y' coordinate, used to represent
 * horizontal locations, offsets, directions, and rotations. For viewport
 * coordinates, use Vector2f instead.
 * <p>
 * By convention, +X is north/forward and +Z is east/right.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorXZ implements Comparable<VectorXZ> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            VectorXZ.class.getName());
    /**
     * local copy of #Vector3f.UNIT_Y
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    /**
     * backward direction
     */
    final public static VectorXZ backward = new VectorXZ(-1f, 0f);
    /**
     * east direction on map
     *
     * @see com.jme3.math.Vector3f#UNIT_Z
     */
    final public static VectorXZ east = new VectorXZ(0f, 1f);
    /**
     * forward direction
     *
     * @see com.jme3.math.Vector3f#UNIT_Z
     */
    final public static VectorXZ forward = new VectorXZ(1f, 0f);
    /**
     * left turn/rotation
     */
    final public static VectorXZ left = new VectorXZ(0f, -1f);
    /**
     * north direction on map
     *
     * @see com.jme3.math.Vector3f#UNIT_X
     */
    final public static VectorXZ north = forward;
    /**
     * right turn/rotation
     *
     * @see com.jme3.math.Vector3f#UNIT_Z
     */
    final public static VectorXZ right = east;
    /**
     * south direction on map
     */
    final public static VectorXZ south = backward;
    /**
     * west direction on map
     */
    final public static VectorXZ west = left;
    /**
     * a zero vector
     *
     * @see com.jme3.math.Vector3f#ZERO
     */
    final public static VectorXZ zero = new VectorXZ(0f, 0f);
    // *************************************************************************
    // fields
    /**
     * northing component or X coordinate or cosine
     */
    final private float x;
    /**
     * easting component or Z coordinate or sine
     */
    final private float z;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a zero vector.
     *
     * @see #zero
     */
    public VectorXZ() {
        x = 0f;
        z = 0f;
    }

    /**
     * Instantiate a unit vector from an azimuth value.
     *
     * @param azimuth radians east of north
     */
    public VectorXZ(float azimuth) {
        x = FastMath.cos(azimuth);
        z = FastMath.sin(azimuth);
    }

    /**
     * Instantiate a vector by projecting a 3-D vector onto the X-Z plane.
     *
     * @param vector3D the 3-D vector (not null, unaffected)
     */
    public VectorXZ(Vector3f vector3D) {
        x = vector3D.x;
        z = vector3D.z;
    }

    /**
     * Instantiate a vector from a pair of coordinate values.
     *
     * @param x northing component or X coordinate or cosine
     * @param z easting component or Z coordinate or sine
     */
    public VectorXZ(float x, float z) {
        this.x = x;
        this.z = z;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add to (translate) this vector.
     *
     * @param increment vector to be added to this vector (not null)
     * @return the vector sum
     * @see com.jme3.math.Vector3f#add(Vector3f)
     */
    public VectorXZ add(VectorXZ increment) {
        float sumX = x + increment.getX();
        float sumZ = z + increment.getZ();

        if (sumX == x && sumZ == z) {
            return this;
        }
        VectorXZ sum = new VectorXZ(sumX, sumZ);

        return sum;
    }

    /**
     * Compute the azimuth of this vector. Note: the directional convention is
     * left-handed. If this vector is zero, return zero.
     *
     * @return angle in radians (&gt;-Pi, &le;Pi), measured CW from north (the
     * +X direction)
     */
    public float azimuth() {
        float result = (float) Math.atan2(z, x);
        return result;
    }

    /**
     * Convert this vector to one of the four cardinal directions. If this
     * vector is zero, return a zero vector.
     *
     * @return a unit vector (four possible values) or a zero vector
     */
    public VectorXZ cardinalize() {
        if (isZero()) {
            return zero;
        }
        final float absX = FastMath.abs(x);
        final float absZ = FastMath.abs(z);
        float newX, newZ;
        if (absX > absZ) {
            newX = FastMath.sign(x);
            newZ = 0f;
        } else {
            newX = 0f;
            newZ = FastMath.sign(z);
        }

        if (newX == x && newZ == z) {
            return this;
        }
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Clamp this vector to be within a specified angle of north (the +X axis).
     *
     * @param maxAbsAngle tolerance angle in radians (&ge;0, &le;Pi)
     * @return clamped vector with same length
     */
    public VectorXZ clampDirection(float maxAbsAngle) {
        Validate.inRange(maxAbsAngle, "angle", 0f, FastMath.PI);

        if (x >= 0) {
            if (maxAbsAngle >= FastMath.HALF_PI) {
                return this;
            } else if (z == 0f) {
                return this;
            }
        }

        float length = length();
        float minX = length * FastMath.cos(maxAbsAngle);
        if (x >= minX) {
            return this;
        }
        float newZ = length * FastMath.sin(maxAbsAngle);
        if (z < 0f) {
            newZ = -newZ;
        }
        VectorXZ result = new VectorXZ(minX, newZ);

        return result;
    }

    /**
     * Clamp this vector to be within an origin-centered, axis-aligned ellipse.
     *
     * @param maxX radius of the ellipse in the X-direction (&ge;0)
     * @param maxZ radius of the ellipse in the Z-direction (&ge;0)
     * @return clamped vector with the same direction
     */
    public VectorXZ clampElliptical(float maxX, float maxZ) {
        Validate.nonNegative(maxX, "maximum X");
        Validate.nonNegative(maxZ, "maximum Z");

        if (isZero()) {
            return zero;
        }
        /*
         * An origin-centered, axis-aligned ellipse in polar coordinates:
         * r^2 = maxX^2 * maxZ^2 / [ (maxZ * cos(th))^2 + (maxX * sin(th))^2 ]  
         */
        float length = length();
        float sineTheta = z / length;
        float cosineTheta = x / length;
        float asin = maxX * sineTheta;
        float bcos = maxZ * cosineTheta;
        double denominator = MyMath.sumOfSquares(asin, bcos);
        double product = maxX * maxZ;
        final double rSquared = product * product / denominator;
        double lengthSquared = lengthSquared();
        if (lengthSquared <= rSquared) {
            return this;
        }
        /*
         * Scale so that length <= r.
         */
        float scale = (float) Math.sqrt(rSquared / lengthSquared);
        float clampedX = x * scale;
        float clampedZ = z * scale;
        VectorXZ result = new VectorXZ(clampedX, clampedZ);

        return result;
    }

    /**
     * Clamp this vector to be within an origin-centered circle.
     *
     * @param radius radius of the circle (&ge;0)
     * @return clamped vector with the same direction
     * @see MyMath#clamp(float, float)
     */
    public VectorXZ clampLength(float radius) {
        Validate.nonNegative(radius, "radius");

        if (isZero()) {
            return zero;
        }
        double lengthSquared = lengthSquared();
        double dRadius = radius;
        double rSquared = dRadius * dRadius;
        if (lengthSquared <= rSquared) {
            return this;
        }
        /*
         * Scale so that length <= radius.
         */
        float scale = (float) Math.sqrt(rSquared / lengthSquared);
        float clampedX = x * scale;
        float clampedZ = z * scale;
        VectorXZ result = new VectorXZ(clampedX, clampedZ);

        return result;
    }

    /**
     * Compute the left-handed cross product of this vector with another. For
     * example, north.cross(east) = +1 and east.cross(north) = -1.
     *
     * @param otherVector the other vector (not null)
     * @return the left-handed cross product
     * @see com.jme3.math.Vector3f#cross(Vector3f)
     */
    public float cross(VectorXZ otherVector) {
        float product = x * otherVector.getZ() - z * otherVector.getX();
        return product;
    }

    /**
     * Compute a signed directional error of this vector with respect to a goal.
     * The result is positive if the goal is to the right and negative if the
     * goal is to the left.
     *
     * @param directionGoal goal direction (length&gt;0)
     * @return the sine of the angle from the goal, or +/-1 if that angle's
     * magnitude exceeds 90 degrees
     */
    public float directionError(VectorXZ directionGoal) {
        validateNonZero(this, "this direction");
        validateNonZero(directionGoal, "goal direction");

        float cross = cross(directionGoal);
        double dot = dot(directionGoal);
        if (dot >= 0.0) {
            double lpSquared = lengthSquared() * directionGoal.lengthSquared();
            float lengthProduct = (float) Math.sqrt(lpSquared);
            float sine = cross / lengthProduct;
            return sine;
        }
        /*
         * The goal and actual direction are more than 90 degrees apart.
         */
        if (cross > 0f) {
            return 1f; // turn hard right
        } else {
            return -1f; // turn hard left
        }
    }

    /**
     * Divide this vector by a scalar.
     *
     * @param scalar scaling factor (not zero)
     * @return a vector 'scalar' times shorter than this one
     * @see com.jme3.math.Vector3f#divide(float)
     */
    public VectorXZ divide(float scalar) {
        Validate.nonZero(scalar, "scalar");

        if (scalar == 1f) {
            return this;
        }
        float scaledX = x / scalar;
        float scaledZ = z / scalar;
        VectorXZ result = new VectorXZ(scaledX, scaledZ);

        return result;
    }

    /**
     * Compute the dot (scalar) product of this vector with another.
     *
     * @param otherVector other vector (not null)
     * @return the dot product
     * @see MyVector3f#dot(Vector3f, Vector3f)
     */
    public double dot(VectorXZ otherVector) {
        double x1 = x;
        double x2 = otherVector.getX();
        double z1 = z;
        double z2 = otherVector.getZ();
        double product = x1 * x2 + z1 * z2;

        return product;
    }

    /**
     * Read the X-component of this vector.
     *
     * @return X-component
     * @see com.jme3.math.Vector3f#getX()
     */
    public float getX() {
        return x;
    }

    /**
     * Read the Z-component of this vector.
     *
     * @return Z-component
     * @see com.jme3.math.Vector3f#getZ()
     */
    public float getZ() {
        return z;
    }

    /**
     * Interpolate (blend) this vector with another.
     *
     * @param otherVector other vector (not null)
     * @param otherFraction how much weight to give to the other vector (&ge;0,
     * &le;1, 0 &rarr; purely this, 1 &rarr; purely the other)
     * @return a blended vector
     */
    public VectorXZ interpolate(VectorXZ otherVector, float otherFraction) {
        float thisFraction = 1f - otherFraction;
        float xBlend = x * thisFraction + otherVector.getX() * otherFraction;
        float zBlend = z * thisFraction + otherVector.getZ() * otherFraction;

        if (x == xBlend && z == zBlend) {
            return this;
        }
        VectorXZ blend = new VectorXZ(xBlend, zBlend);

        return blend;
    }

    /**
     * Test this vector to see if it's the zero vector.
     *
     * @return true if both components are zero, false otherwise
     */
    public boolean isZero() {
        boolean result = (x == 0f && z == 0f);
        return result;
    }

    /**
     * Compute the length (or magnitude or norm) of this vector.
     *
     * @return the length (&ge;0)
     * @see com.jme3.math.Vector3f#length()
     */
    public float length() {
        float result = MyMath.hypotenuse(x, z);
        return result;
    }

    /**
     * Compute the squared length of this vector. Returns a double-precision
     * value for precise comparisons.
     *
     * @return the squared length (&ge;0)
     * @see MyVector3f#lengthSquared(Vector3f)
     */
    public double lengthSquared() {
        double result = MyMath.sumOfSquares(x, z);
        return result;
    }

    /**
     * Mirror this vector across the X-axis (complex conjugate or inverse
     * rotation).
     *
     * @return a mirrored vector with the same length
     */
    public VectorXZ mirrorZ() {
        if (z == 0f) {
            return this;
        }
        VectorXZ result = new VectorXZ(x, -z);

        return result;
    }

    /**
     * Scale this vector by a scalar.
     *
     * @param scalar scaling factor
     * @return a vector 'scalar' times longer than this one
     */
    public VectorXZ mult(float scalar) {
        if (scalar == 1f) {
            return this;
        }
        float scaledX = x * scalar;
        float scaledZ = z * scalar;
        VectorXZ result = new VectorXZ(scaledX, scaledZ);

        return result;
    }

    /**
     * Negate this vector.
     *
     * @return a vector with same magnitude and opposite direction
     * @see com.jme3.math.Vector3f#negate()
     */
    public VectorXZ negate() {
        if (isZero()) {
            return zero;
        }

        VectorXZ result = new VectorXZ(-x, -z);

        return result;
    }

    /**
     * Normalize this vector to a unit vector. If this vector is zero, return a
     * zero vector.
     *
     * @return a unit vector (with the same direction) or a zero vector
     * @see com.jme3.math.Vector3f#normalize()
     */
    public VectorXZ normalize() {
        if (isZero()) {
            logger.info("Normalizing a zero vector.");
            return zero;
        }

        double lengthSquared = lengthSquared();
        if ((float) lengthSquared == 1f) {
            return this;
        }

        float length = (float) Math.sqrt(lengthSquared);
        float newX = x / length;
        float newZ = z / length;

        if (newX == x && newZ == z) {
            return this;
        }
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: This method is used to
     * apply azimuths, which is why its angle convention is left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return a vector with the same length
     */
    public VectorXZ rotate(float radians) {
        if (radians == 0f) {
            return this;
        }

        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;

        if (newX == x && newZ == z) {
            return this;
        }
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Rotate and scale this vector by another vector (complex product).
     *
     * @param direction rotated/scaled vector for the current north (not null)
     * @return the complex product
     */
    public VectorXZ rotate(VectorXZ direction) {
        float cosine = direction.getX();
        float sine = direction.getZ();
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;

        if (newX == x && newZ == z) {
            return this;
        }
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Subtract from (inverse translate) this vector.
     *
     * @param decrement vector to be subtracted from this vector (not null)
     * @return a vector equal to the difference of the two vectors
     * @see com.jme3.math.Vector3f#subtract(Vector3f)
     */
    public VectorXZ subtract(VectorXZ decrement) {
        float newX = x - decrement.getX();
        float newZ = z - decrement.getZ();

        if (newX == x && newZ == z) {
            return this;
        }
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Treating this vector as a rotation (from north), generate an equivalent
     * quaternion.
     *
     * @return new quaternion
     */
    public Quaternion toQuaternion() {
        Quaternion result = new Quaternion();
        /*
         * Vector3f.lookAt() orients the Z-axis, whereas VectorXZ.rotate() 
         * orients the X-axis, so a 90-degree tranformation of coordinates is
         * required.
         */
        Vector3f direction = new Vector3f(-z, 0f, x);
        result.lookAt(direction, yAxis);

        return result;
    }

    /**
     * Create a 3D equivalent of this vector.
     *
     * @return new 3D vector with y=0
     */
    public Vector3f toVector3f() {
        Vector3f result = new Vector3f(x, 0f, z);
        return result;
    }

    /**
     * Create an equivalent 3D vector with a specified y value.
     *
     * @param y y-coordinate
     * @return a new 3D vector
     */
    public Vector3f toVector3f(float y) {
        Vector3f result = new Vector3f(x, y, z);
        return result;
    }

    /**
     * Validate a non-zero VectorXZ as a method argument.
     *
     * @param vector vector to validate (not null, non-zero)
     * @param description textual description of the vector
     * @throws IllegalArgumentException if the vector is zero
     * @see jme3utilities.Validate#nonZero(Vector3f, String)
     */
    public static void validateNonZero(VectorXZ vector, String description) {
        Validate.nonNull(vector, description);

        if (vector.isZero()) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message;
            message = String.format("%s should be non-zero.", what);
            throw new IllegalArgumentException(message);
        }
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare lexicographically with another vector, with the X-component
     * having priority.
     *
     * @param otherVector (not null)
     * @return 0 if this vector equals otherVector; negative if this comes
     * before otherVector; positive if this comes after otherVector
     */
    @Override
    public int compareTo(VectorXZ otherVector) {
        int result;
        if (x != otherVector.getX()) {
            result = Float.compare(x, otherVector.getX());
        } else {
            result = Float.compare(z, otherVector.getZ());
        }
        /*
         * Verify consistency with equals().
         */
        if (result == 0) {
            assert this.equals(otherVector);
        }
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for exact equality. TODO: also provide comparison with tolerance
     *
     * @param otherObject (may be null)
     * @return true if the vectors are equal, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject instanceof VectorXZ) {
            VectorXZ otherVector = (VectorXZ) otherObject;
            return otherVector.getX() == x && otherVector.getZ() == z;
        }
        return false;
    }

    /**
     * Generate the hash code for this vector.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(this.x);
        hash = 71 * hash + Float.floatToIntBits(this.z);

        return hash;
    }

    /**
     * Represent this vector as a text string. The format is: [X=XX.XXX,
     * Z=ZZ.ZZZ]
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String result = String.format("[%.3f, %.3f]", x, z);
        return result;
    }
    // *************************************************************************
    // test cases

    /**
     * Console application to test the VectorXZ class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.printf("Test results for class VectorXZ:%n%n");

        // vector test cases
        VectorXZ[] cases = new VectorXZ[4];
        cases[0] = east;
        cases[1] = new VectorXZ(1f, 1f);
        cases[2] = west;
        cases[3] = zero;

        for (VectorXZ vin : cases) {
            float a = vin.azimuth();
            VectorXZ vout = new VectorXZ(a);

            System.out.printf(
                    "vin = %s  azimuth(x)=%f (%f degrees)  vout = %s%n",
                    vin.toString(), a, MyMath.toDegrees(a),
                    vout.toString());
            System.out.println();

            Vector3f v3 = new Vector3f(1f, 2f, 3f);
            VectorXZ vxz = new VectorXZ(v3);
            VectorXZ r1 = vin.normalize().rotate(vxz);

            Quaternion q1 = vin.toQuaternion();
            VectorXZ r2 = new VectorXZ(q1.mult(v3));

            Quaternion q2 = new Quaternion();
            q2.fromAngleAxis(-a, yAxis);
            VectorXZ r3 = new VectorXZ(q2.mult(v3));

            System.out.printf("vin=%s  r1=%s, r2=%s, r3=%s%n",
                    vin.toString(), r1.toString(), r2.toString(),
                    r3.toString());
            System.out.println();
        }
        System.out.println();
    }
}

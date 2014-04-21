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
import jme3utilities.Validate;

/**
 * Immutable single-precision vector with no 'y' coordinate, used to represent
 * horizontal locations, offsets, directions, and rotations. For viewport
 * coordinates use Vector2f instead.
 * <p>
 * By convention, +X is north and +Z is east.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class VectorXZ
        implements Cloneable, Comparable<VectorXZ> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(VectorXZ.class.getName());
    /**
     * east direction
     */
    final public static VectorXZ east = new VectorXZ(0f, 1f);
    /**
     * north direction
     */
    final public static VectorXZ north = new VectorXZ(1f, 0f);
    /**
     * south direction
     */
    final public static VectorXZ south = new VectorXZ(-1f, 0f);
    /**
     * west direction
     */
    final public static VectorXZ west = new VectorXZ(0f, -1f);
    // *************************************************************************
    // fields
    /**
     * northing component (X coordinate)
     */
    private float x;
    /**
     * easting component (Z coordinate)
     */
    private float z;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a vector with zero length.
     */
    public VectorXZ() {
        x = 0f;
        z = 0f;
    }

    /**
     * Instantiate a vector from an azimuth value.
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
     * @param vector3D the 3-D vector (unaffected, not null)
     */
    public VectorXZ(Vector3f vector3D) {
        x = vector3D.x;
        z = vector3D.z;
    }

    /**
     * Instantiate a vector from a pair of coordinate values.
     *
     * @param x northing component (X coordinate)
     * @param z easting component (Z coordinate)
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
     * @return new vector equal to the sum
     */
    public VectorXZ add(VectorXZ increment) {
        float sumX = x + increment.getX();
        float sumZ = z + increment.getZ();
        VectorXZ sum = new VectorXZ(sumX, sumZ);

        return sum;
    }

    /**
     * Compute the azimuth of this vector. Note: the directional convention is
     * left-handed.
     *
     * @return angle in radians (between -Pi and Pi), measured CW from the north
     * (+X direction)
     */
    public float azimuth() {
        float result = (float) Math.atan2(z, x);
        return result;
    }

    /**
     * Clamp this vector to be within an axis-aligned ellipse.
     *
     * @param maxX radius of the ellipse in the X-direction
     * @param maxZ radius of the ellipse in the Z-direction
     * @return new vector with the same direction
     */
    public VectorXZ clampElliptical(float maxX, float maxZ) {
        if (isZeroLength()) {
            /*
             * Clamping has no effect on a zero-length vector.
             */
            return clone();
        }
        /*
         * Represent the ellipse in polar coordinates.
         */
        float theta = azimuth();
        float asin = maxX * FastMath.sin(theta);
        float bcos = maxZ * FastMath.cos(theta);
        float r = maxX * maxZ / MyMath.hypotenuse(asin, bcos);
        /*
         * Scale so that length <= r.
         */
        float length = length();
        float newX = x;
        float newZ = z;
        if (length > r) {
            float scale = r / length;
            newX *= scale;
            newZ *= scale;
        }

        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Compute the cross product of this vector with another.
     *
     * @param other the other vector (not null)
     * @return cross product
     */
    public float cross(VectorXZ other) {
        float product = x * other.getZ() - z * other.getX();
        return product;
    }

    /**
     * Compute the directional error of this direction with respect to a goal.
     *
     * @param goal unit vector
     * @return sine of the angle from the goal to this direction, or +1/-1 if
     * the angle's magnitude exceeds 90 degrees
     */
    public float directionError(VectorXZ goal) {
        if (!goal.isUnitVector()) {
            logger.log(Level.SEVERE, "goal={0}", goal);
            throw new IllegalArgumentException("goal should have length=1");
        }
        if (!isUnitVector()) {
            logger.log(Level.SEVERE, "this={0}", this);
            throw new IllegalStateException("vector should have length=1");
        }

        float cosine = dot(goal);
        float sine = cross(goal);
        if (cosine >= 0f) {
            return sine;
        }
        /*
         * The goal and actual directions are more than 90 degrees apart.
         */
        if (sine > 0f) {
            return 1f; // turn hard right
        } else {
            return -1f; // turn hard left
        }
    }

    /**
     * Divide this vector by a scalar.
     *
     * @param scalar scaling factor (not zero)
     * @return new vector 'scalar' times shorter than this one
     */
    public VectorXZ divide(float scalar) {
        Validate.nonZero(scalar, "scalar");

        VectorXZ result = new VectorXZ(x / scalar, z / scalar);
        return result;
    }

    /**
     * Compute the dot product of this vector with another.
     *
     * @param other other vector (not null)
     * @return dot product
     */
    public float dot(VectorXZ other) {
        float product = x * other.getX() + z * other.getZ();
        return product;
    }

    /**
     * Read the X-component of this vector.
     *
     * @return X-component
     */
    public float getX() {
        return x;
    }

    /**
     * Read the Z-component of this vector.
     *
     * @return Z-component
     */
    public float getZ() {
        return z;
    }

    /**
     * Test this vector for unit length.
     *
     * @return true if this vector's length is roughly one, false otherwise
     */
    public boolean isUnitVector() {
        float delta = length() - 1f;
        boolean result = (FastMath.abs(delta) < 1e-4);
        return result;
    }

    /**
     * Test this vector for zero length.
     *
     * @return true if this vector has zero length, false otherwise
     */
    public boolean isZeroLength() {
        boolean result = (x == 0f && z == 0f);
        return result;
    }

    /**
     * Compute the length of this vector.
     *
     * @return length (&ge;0) of this vector
     */
    public float length() {
        float result = MyMath.hypotenuse(x, z);
        return result;
    }

    /**
     * Mirror this vector across the X-axis (complex conjugate).
     *
     * @return new vector
     */
    public VectorXZ mirrorZ() {
        VectorXZ result = new VectorXZ(x, -z);
        return result;
    }

    /**
     * Scale this vector by a scalar.
     *
     * @param scalar scaling factor
     * @return new vector 'scalar' times longer than this one
     */
    public VectorXZ mult(float scalar) {
        VectorXZ result = new VectorXZ(x * scalar, z * scalar);
        return result;
    }

    /**
     * Negate this vector.
     *
     * @return new vector with same magnitude and opposite direction
     */
    public VectorXZ negate() {
        VectorXZ result = new VectorXZ(-x, -z);
        return result;
    }

    /**
     * Normalize this vector to a unit vector. If this vector has zero length,
     * generate a random direction.
     *
     * @return new unit vector
     */
    public VectorXZ normalize() {
        float length = length();
        float newX = x;
        float newZ = z;
        while (length == 0f) {
            /*
             * Pick random X and Z, each between -0.5 and +0.5
             */
            newX = Noise.nextFloat() - 0.5f;
            newZ = Noise.nextFloat() - 0.5f;
            length = MyMath.hypotenuse(newX, newZ);
        }

        newX /= length;
        newZ /= length;
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: This method is used to
     * apply azimuths, which is why its angle convention is left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return new vector with the same length
     */
    public VectorXZ rotate(float radians) {
        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Rotate a vector by a direction vector (complex product).
     *
     * @param direction final direction for the original X-axis (not null)
     * @return new vector
     */
    public VectorXZ rotate(VectorXZ direction) {
        float cosine = direction.getX();
        float sine = direction.getZ();
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Subtract from this vector.
     *
     * @param decrement vector to be subtracted from this vector (not null)
     * @return new vector equal to the difference of the two vectors
     */
    public VectorXZ subtract(VectorXZ decrement) {
        float newX = x + decrement.getX();
        float newZ = z + decrement.getZ();
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Treat this vector as a rotation and generate an equivalent quaternion.
     *
     * @return new instance
     */
    public Quaternion toQuaternion() {
        Quaternion result = new Quaternion();
        Vector3f direction = toVector3f();
        result.lookAt(direction, Vector3f.UNIT_Y);

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
     * @return new 3D vector
     */
    public Vector3f toVector3f(float y) {
        Vector3f result = new Vector3f(x, y, z);
        return result;
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
     * Clone this instance.
     *
     * @return new vector with the same components as this vector
     */
    @Override
    public VectorXZ clone() {
        try {
            VectorXZ clone = (VectorXZ) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError();
        }
    }

    /**
     * Compare for equality.
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
     */
    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(this.x);
        hash = 71 * hash + Float.floatToIntBits(this.z);

        return hash;
    }

    /**
     * Represent this vector as a string. The format is: [X=XX.XXX, Z=ZZ.ZZZ]
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
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class VectorXZ:\n\n");

        // vector test cases
        VectorXZ[] cases = new VectorXZ[4];
        cases[0] = new VectorXZ(0f, 1f);
        cases[1] = new VectorXZ(1f, 1f);
        cases[2] = new VectorXZ(0f, 0f);
        cases[3] = new VectorXZ(0f, -1f);

        for (VectorXZ vin : cases) {
            float a = vin.azimuth();
            VectorXZ vout = new VectorXZ(a);

            System.out.printf(
                    "vin = %s  azimuth(x)=%f (%f degrees)  vout = %s%n",
                    vin.toString(), a, a * FastMath.RAD_TO_DEG,
                    vout.toString());
            System.out.println();
        }
        System.out.println();
    }
}
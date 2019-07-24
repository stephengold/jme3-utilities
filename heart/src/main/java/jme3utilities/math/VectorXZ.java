/*
 Copyright (c) 2013-2019, Stephen Gold
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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Single-precision vector with no 'y' coordinate, used to represent horizontal
 * locations, offsets, orientations, directions, rotations, and extents.
 * Immutable except for {@link #read(com.jme3.export.JmeImporter)}. For viewport
 * coordinates, use {@link com.jme3.math.Vector2f} instead.
 * <p>
 * By convention, +X is north/forward and +Z is east/right.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorXZ
        implements Comparable<ReadXZ>, ReadXZ, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VectorXZ.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f unitY = new Vector3f(0f, 1f, 0f);
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
     * @see com.jme3.math.Vector3f#UNIT_X
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
    private float x;
    /**
     * easting component or Z coordinate or sine
     */
    private float z;
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
     * Validate a non-zero VectorXZ as a method argument.
     *
     * @param vector vector to validate (not null, non-zero)
     * @param description textual description of the vector
     * @throws IllegalArgumentException if the vector is zero
     * @see jme3utilities.Validate#nonZero(Vector3f, String)
     */
    public static void validateNonZero(ReadXZ vector, String description) {
        Validate.nonNull(vector, description);

        if (vector.isZero()) {
            String what;
            if (description == null) {
                what = "VectorXZ argument";
            } else {
                what = description;
            }
            String message;
            message = String.format("%s must not be zero.", what);
            throw new IllegalArgumentException(message);
        }
    }
    // *************************************************************************
    // ReadXZ methods

    /**
     * Test for approximate equality with another vector using a Chebyshev
     * metric.
     *
     * @param otherVector (not null)
     * @param absoluteTolerance (&ge;0)
     * @return true if each component differs by tolerance or less, otherwise
     * false
     */
    @Override
    public boolean aboutEquals(ReadXZ otherVector, float absoluteTolerance) {
        Validate.nonNull(otherVector, "other vector");
        Validate.nonNegative(absoluteTolerance, "absolute tolerance");

        boolean result = equals(otherVector);
        if (!result) {
            float otherX = otherVector.getX();
            float dx = Math.abs(x - otherX);
            if (dx <= absoluteTolerance) {
                float otherZ = otherVector.getZ();
                float dz = Math.abs(z - otherZ);
                if (dz <= absoluteTolerance) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Add to (translate) this vector.
     *
     * @param increment vector to be added to this vector (not null)
     * @return a sum vector
     * @see com.jme3.math.Vector3f#add(com.jme3.math.Vector3f)
     */
    @Override
    public ReadXZ add(ReadXZ increment) {
        float sumX = x + increment.getX();
        float sumZ = z + increment.getZ();

        VectorXZ sum;
        if (equals(sumX, sumZ)) {
            sum = this;
        } else {
            sum = new VectorXZ(sumX, sumZ);
        }

        return sum;
    }

    /**
     * Calculate the azimuth of this vector. Note: the directional convention is
     * left-handed. If this vector is zero, return zero.
     *
     * @return angle in radians (&gt;-Pi, &le;Pi), measured CW from north (the
     * +X direction)
     */
    @Override
    public float azimuth() {
        float result = (float) Math.atan2(z, x);
        return result;
    }

    /**
     * Convert this vector to one of the 4 cardinal directions. If this vector
     * is zero, return a zero vector.
     *
     * @return a unit vector (4 possible values) or a zero vector
     */
    @Override
    public ReadXZ cardinalize() {
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

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Clamp this vector to be within a specified angle of north (the +X axis).
     *
     * @param maxAbsAngle tolerance angle in radians (&ge;0, &le;Pi)
     * @return clamped vector with same length
     */
    @Override
    public ReadXZ clampDirection(float maxAbsAngle) {
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
    @Override
    public ReadXZ clampElliptical(float maxX, float maxZ) {
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
    @Override
    public ReadXZ clampLength(float radius) {
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
     * Compare lexicographically with a hypothetical vector having the specified
     * components, distinguishing 0 and -0 and giving priority to the X
     * components.
     *
     * @param hX X component of the hypothetical vector
     * @param hZ Z component of the hypothetical vector
     * @return 0 if this vector equals the hypothetical; negative if this comes
     * before the hypothetical; positive if this comes after hypothetical
     */
    @Override
    public int compareTo(float hX, float hZ) {
        int result = Float.compare(x, hX);
        if (result == 0) {
            result = Float.compare(z, hZ);
        }

        return result;
    }

    /**
     * Calculate the cosine of the angle between this vector and another. This
     * is used to compare the similarity of direction vectors. Returns a
     * double-precision value for precise comparisons.
     *
     * @param otherVector the other vector (not null)
     * @return the cosine of the angle (&ge;-1, &le;1) or 1 if either vector is
     * zero
     * @see com.jme3.math.Vector3f#angleBetween(com.jme3.math.Vector3f)
     */
    @Override
    public double cosineAngleBetween(ReadXZ otherVector) {
        double x2 = otherVector.getX();
        double z2 = otherVector.getZ();
        double otherLengthSquared = x2 * x2 + z2 * z2;

        double x1 = x;
        double z1 = z;
        double lengthSquared = x1 * x1 + z1 * z1;

        double lsProduct = lengthSquared * otherLengthSquared;
        if (lsProduct == 0.0) {
            return 1.0;
        }

        double dotProduct = x1 * x2 + z1 * z2;
        double cosine = dotProduct / Math.sqrt(lsProduct);
        cosine = MyMath.clamp(cosine, 1.0);

        return cosine;
    }

    /**
     * Calculate the (left-handed) cross product of this vector with another.
     * For example, north.cross(east) = +1 and east.cross(north) = -1.
     *
     * @param otherVector the other vector (not null)
     * @return the left-handed cross product
     * @see com.jme3.math.Vector3f#cross(com.jme3.math.Vector3f)
     */
    @Override
    public float cross(ReadXZ otherVector) {
        float product = x * otherVector.getZ() - z * otherVector.getX();
        return product;
    }

    /**
     * Calculate a signed directional error of this vector with respect to a
     * goal. The result is positive if the goal is to the right and negative if
     * the goal is to the left.
     *
     * @param directionGoal goal direction (not null, not zero)
     * @return the sine of the angle from the goal, or +/-1 if that angle's
     * magnitude exceeds 90 degrees
     */
    @Override
    public float directionError(ReadXZ directionGoal) {
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
     * @return a vector 'scalar' times shorter than this one, with same
     * direction if scalar&gt;0, opposite direction if scalar&lt;0
     * @see com.jme3.math.Vector3f#divide(float)
     */
    @Override
    public ReadXZ divide(float scalar) {
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
     * Calculate the dot (scalar) product of this vector with another.
     *
     * @param otherVector other vector (not null)
     * @return the dot product
     * @see MyVector3f#dot(com.jme3.math.Vector3f, com.jme3.math.Vector3f)
     */
    @Override
    public double dot(ReadXZ otherVector) {
        double x1 = x;
        double x2 = otherVector.getX();
        double z1 = z;
        double z2 = otherVector.getZ();
        double product = x1 * x2 + z1 * z2;

        return product;
    }

    /**
     * Test for equality with a hypothetical vector having the specified
     * components, distinguishing 0 and -0.
     *
     * @param hX X component of the hypothetical vector
     * @param hZ Z component of the hypothetical vector
     * @return true if equivalent, otherwise false
     */
    @Override
    public boolean equals(float hX, float hZ) {
        int compare = compareTo(hX, hZ);
        return compare == 0;
    }

    /**
     * Mirror (or reflect) this vector to the first quadrant.
     *
     * @return a mirrored vector with the same length, both components &ge;0
     */
    @Override
    public ReadXZ firstQuadrant() {
        if (isFirstQuadrant()) {
            return this;
        }
        float newX = FastMath.abs(x);
        float newZ = FastMath.abs(z);
        VectorXZ result = new VectorXZ(newX, newZ);

        return result;
    }

    /**
     * Read the X-component of this vector.
     *
     * @return the X-component
     * @see com.jme3.math.Vector3f#getX()
     */
    @Override
    public float getX() {
        return x;
    }

    /**
     * Read the Z-component of this vector.
     *
     * @return the Z-component
     * @see com.jme3.math.Vector3f#getZ()
     */
    @Override
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
    @Override
    public ReadXZ interpolate(ReadXZ otherVector, float otherFraction) {
        float thisFraction = 1f - otherFraction;
        float xBlend = x * thisFraction + otherVector.getX() * otherFraction;
        float zBlend = z * thisFraction + otherVector.getZ() * otherFraction;

        VectorXZ blend;
        if (equals(xBlend, zBlend)) {
            blend = this;
        } else {
            blend = new VectorXZ(xBlend, zBlend);
        }

        return blend;
    }

    /**
     * Test whether this vector is in the first quadrant.
     *
     * @return true if both components are &ge;0, false otherwise
     * @see MyVector3f#isAllNonNegative(com.jme3.math.Vector3f)
     */
    @Override
    public boolean isFirstQuadrant() {
        boolean result = (x >= 0f && z >= 0f);
        return result;
    }

    /**
     * Test whether this vector is a zero vector.
     *
     * @return true if both components are zero, false otherwise
     * @see MyVector3f#isZero(com.jme3.math.Vector3f)
     */
    @Override
    public boolean isZero() {
        boolean result = (x == 0f && z == 0f);
        return result;
    }

    /**
     * Calculate the length (or magnitude or norm) of this vector.
     *
     * @return the length (&ge;0)
     * @see com.jme3.math.Vector3f#length()
     */
    @Override
    public float length() {
        float result = MyMath.hypotenuse(x, z);
        return result;
    }

    /**
     * Calculate the squared length of this vector. This is used to compare the
     * lengths of vectors. Returns a double-precision value for precise
     * comparisons.
     *
     * @return the squared length (&ge;0)
     * @see MyVector3f#lengthSquared(Vector3f)
     */
    @Override
    public double lengthSquared() {
        double result = MyMath.sumOfSquares(x, z);
        return result;
    }

    /**
     * Mirror (or reflect) this vector across the X-axis (complex conjugate or
     * inverse rotation).
     *
     * @return a mirrored vector with the same length
     */
    @Override
    public ReadXZ mirrorZ() {
        if (z == 0f) {
            return this;
        }
        VectorXZ result = new VectorXZ(x, -z);

        return result;
    }

    /**
     * Scale this vector by a scalar.
     *
     * @param multiplier scaling factor
     * @return a vector 'scalar' times longer than this one, with same direction
     * if multiplier&gt;0, opposite direction if multiplier&lt;0
     * @see com.jme3.math.Vector3f#mult(float)
     */
    @Override
    public ReadXZ mult(float multiplier) {
        if (multiplier == 1f) {
            return this;
        }
        float scaledX = x * multiplier;
        float scaledZ = z * multiplier;
        VectorXZ result = new VectorXZ(scaledX, scaledZ);

        return result;
    }

    /**
     * Multiply this vector by another (complex product or rotate-and-scale).
     * This is NOT analogous to {@link com.jme3.math.Vector3f#mult(Vector3f)},
     * which performs non-uniform scaling.
     *
     * @param multiplier rotated/scaled result for the current north (not null)
     * @return the complex product
     *
     * @see #cross(jme3utilities.math.ReadXZ)
     * @see #dot(jme3utilities.math.ReadXZ)
     * @see #scale(jme3utilities.math.ReadXZ)
     */
    @Override
    public ReadXZ mult(ReadXZ multiplier) {
        float cosine = multiplier.getX();
        float sine = multiplier.getZ();
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Negate this vector (or reverse its direction or reflect it in the
     * origin). This is equivalent to #mult(-1f)
     *
     * @return a vector with same magnitude and opposite direction
     * @see com.jme3.math.Vector3f#negate()
     */
    @Override
    public ReadXZ negate() {
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
    @Override
    public ReadXZ normalize() {
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

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: This method is used to
     * apply azimuths, which is why its angle convention is left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return a vector with the same length
     * @see com.jme3.math.Vector2f#rotateAroundOrigin(float, boolean)
     */
    @Override
    public ReadXZ rotate(float radians) {
        if (radians == 0f) {
            return this;
        }

        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Scale this vector by another (non-uniform scaling).
     *
     * @param multiplier scaled result for the current north (not null)
     * @return a scaled vector
     * @see com.jme3.math.Vector3f#mult(com.jme3.math.Vector3f)
     */
    @Override
    public ReadXZ scale(ReadXZ multiplier) {
        float newX = x * multiplier.getX();
        float newZ = z * multiplier.getZ();

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Subtract from (inverse translate) this vector.
     *
     * @param decrement vector to be subtracted from this vector (not null)
     * @return a vector equal to the difference of the 2 vectors
     * @see com.jme3.math.Vector3f#subtract(com.jme3.math.Vector3f)
     */
    @Override
    public ReadXZ subtract(ReadXZ decrement) {
        float newX = x - decrement.getX();
        float newZ = z - decrement.getZ();

        VectorXZ result;
        if (equals(newX, newZ)) {
            result = this;
        } else {
            result = new VectorXZ(newX, newZ);
        }

        return result;
    }

    /**
     * Treating this vector as a rotation (from north), generate an equivalent
     * Quaternion.
     *
     * @return a new Quaternion
     */
    @Override
    public Quaternion toQuaternion() {
        Quaternion result = toQuaternion(null);
        return result;
    }

    /**
     * Treating this vector as a rotation (from north), generate an equivalent
     * Quaternion.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a Quaternion (either storeResult or a new instance, not null)
     */
    @Override
    public Quaternion toQuaternion(Quaternion storeResult) {
        Quaternion result;
        if (storeResult == null) {
            result = new Quaternion();
        } else {
            result = storeResult;
        }
        /*
         * Vector3f.lookAt() orients the Z-axis, whereas VectorXZ.rotate()
         * orients the X-axis, so a 90-degree tranformation of coordinates is
         * required.
         */
        Vector3f direction = new Vector3f(-z, 0f, x);
        result.lookAt(direction, unitY);

        return result;
    }

    /**
     * Create an equivalent 3-D vector.
     *
     * @return a new 3-D vector with y=0
     */
    @Override
    public Vector3f toVector3f() {
        Vector3f result = toVector3f(0f, null);
        return result;
    }

    /**
     * Create an equivalent 3-D vector with the specified y value.
     *
     * @param y the y-coordinate
     * @return a new 3-D vector
     */
    @Override
    public Vector3f toVector3f(float y) {
        Vector3f result = toVector3f(y, null);
        return result;
    }

    /**
     * Create an equivalent 3-D vector with the specified y value.
     *
     * @param y the y-coordinate
     * @param storeResult storage for the result (modified if not null)
     * @return a 3-D vector (either storeResult or a new instance, not null)
     */
    @Override
    public Vector3f toVector3f(float y, Vector3f storeResult) {
        Vector3f result;
        if (storeResult == null) {
            result = new Vector3f(x, y, z);
        } else {
            result = storeResult.set(x, y, z);
        }

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
    public int compareTo(ReadXZ otherVector) {
        float otherX = otherVector.getX();
        int result = Float.compare(x, otherX);
        if (result == 0) {
            float otherZ = otherVector.getZ();
            result = Float.compare(z, otherZ);
        }

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for exact equality, distinguishing 0 and -0.
     *
     * @param otherObject (may be null)
     * @return true if the vectors are equal, otherwise false
     * @see #aboutEquals(jme3utilities.math.ReadXZ, float)
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result;
        if (this == otherObject) {
            result = true;
        } else if (otherObject instanceof ReadXZ) {
            ReadXZ otherVector = (ReadXZ) otherObject;
            float otherX = otherVector.getX();
            float otherZ = otherVector.getZ();
            result = equals(otherX, otherZ);
        } else {
            result = false;
        }

        return result;
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
     * Represent this vector as a text string. The format is: (x=XX.XXX,
     * z=ZZ.ZZZ)
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String result = String.format("(x=%.3f, z=%.3f)", x, z);
        return result;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        InputCapsule capsule = importer.getCapsule(this);

        x = capsule.readFloat("x", 0f);
        z = capsule.readFloat("z", 0f);
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(x, "x", 0f);
        capsule.write(z, "z", 0f);
    }
}

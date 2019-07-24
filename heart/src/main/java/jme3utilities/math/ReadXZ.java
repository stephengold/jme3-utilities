/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * Single-precision vector with no 'y' coordinate, used to represent horizontal
 * locations, offsets, orientations, directions, rotations, and extents. For
 * viewport coordinates, use {@link com.jme3.math.Vector2f} instead.
 * <p>
 * By convention, +X is north/forward and +Z is east/right.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface ReadXZ {
    /**
     * Test for approximate equality with another vector using a Chebyshev
     * metric.
     *
     * @param otherVector (not null)
     * @param absoluteTolerance (&ge;0)
     * @return true if each component differs by tolerance or less, otherwise
     * false
     */
    boolean aboutEquals(ReadXZ otherVector, float absoluteTolerance);

    /**
     * Add to (translate) this vector.
     *
     * @param increment vector to be added to this vector (not null)
     * @return a sum vector
     * @see com.jme3.math.Vector3f#add(com.jme3.math.Vector3f)
     */
    ReadXZ add(ReadXZ increment);

    /**
     * Calculate the azimuth of this vector. Note: the directional convention is
     * left-handed. If this vector is zero, return zero.
     *
     * @return angle in radians (&gt;-Pi, &le;Pi), measured CW from north (the
     * +X direction)
     */
    float azimuth();

    /**
     * Convert this vector to one of the 4 cardinal directions. If this vector
     * is zero, return a zero vector.
     *
     * @return a unit vector (4 possible values) or a zero vector
     */
    ReadXZ cardinalize();

    /**
     * Clamp this vector to be within a specified angle of north (the +X axis).
     *
     * @param maxAbsAngle tolerance angle in radians (&ge;0, &le;Pi)
     * @return clamped vector with same length
     */
    ReadXZ clampDirection(float maxAbsAngle);

    /**
     * Clamp this vector to be within an origin-centered, axis-aligned ellipse.
     *
     * @param maxX radius of the ellipse in the X-direction (&ge;0)
     * @param maxZ radius of the ellipse in the Z-direction (&ge;0)
     * @return clamped vector with the same direction
     */
    ReadXZ clampElliptical(float maxX, float maxZ);

    /**
     * Clamp this vector to be within an origin-centered circle.
     *
     * @param radius radius of the circle (&ge;0)
     * @return clamped vector with the same direction
     * @see MyMath#clamp(float, float)
     */
    ReadXZ clampLength(float radius);

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
    int compareTo(float hX, float hZ);

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
    double cosineAngleBetween(ReadXZ otherVector);

    /**
     * Calculate the (left-handed) cross product of this vector with another.
     * For example, north.cross(east) = +1 and east.cross(north) = -1.
     *
     * @param otherVector the other vector (not null)
     * @return the left-handed cross product
     * @see com.jme3.math.Vector3f#cross(com.jme3.math.Vector3f)
     */
    float cross(ReadXZ otherVector);

    /**
     * Calculate a signed directional error of this vector with respect to a
     * goal. The result is positive if the goal is to the right and negative if
     * the goal is to the left.
     *
     * @param directionGoal goal direction (not null, not zero)
     * @return the sine of the angle from the goal, or +/-1 if that angle's
     * magnitude exceeds 90 degrees
     */
    float directionError(ReadXZ directionGoal);

    /**
     * Divide this vector by a scalar.
     *
     * @param scalar scaling factor (not zero)
     * @return a vector 'scalar' times shorter than this one, with same
     * direction if scalar&gt;0, opposite direction if scalar&lt;0
     * @see com.jme3.math.Vector3f#divide(float)
     */
    ReadXZ divide(float scalar);

    /**
     * Calculate the dot (scalar) product of this vector with another.
     *
     * @param otherVector other vector (not null)
     * @return the dot product
     * @see MyVector3f#dot(com.jme3.math.Vector3f, com.jme3.math.Vector3f)
     */
    double dot(ReadXZ otherVector);

    /**
     * Test for equality with a hypothetical vector having the specified
     * components, distinguishing 0 and -0.
     *
     * @param hX X component of the hypothetical vector
     * @param hZ Z component of the hypothetical vector
     * @return true if equivalent, otherwise false
     */
    boolean equals(float hX, float hZ);

    /**
     * Mirror (or reflect) this vector to the first quadrant.
     *
     * @return a mirrored vector with the same length, both components &ge;0
     */
    ReadXZ firstQuadrant();

    /**
     * Read the X-component of this vector.
     *
     * @return the X-component
     * @see com.jme3.math.Vector3f#getX()
     */
    float getX();

    /**
     * Read the Z-component of this vector.
     *
     * @return the Z-component
     * @see com.jme3.math.Vector3f#getZ()
     */
    float getZ();

    /**
     * Interpolate (blend) this vector with another.
     *
     * @param otherVector other vector (not null)
     * @param otherFraction how much weight to give to the other vector (&ge;0,
     * &le;1, 0 &rarr; purely this, 1 &rarr; purely the other)
     * @return a blended vector
     */
    ReadXZ interpolate(ReadXZ otherVector, float otherFraction);

    /**
     * Test whether this vector is in the first quadrant.
     *
     * @return true if both components are &ge;0, false otherwise
     * @see MyVector3f#isAllNonNegative(com.jme3.math.Vector3f)
     */
    boolean isFirstQuadrant();

    /**
     * Test whether this vector is a zero vector.
     *
     * @return true if both components are zero, false otherwise
     * @see MyVector3f#isZero(com.jme3.math.Vector3f)
     */
    boolean isZero();

    /**
     * Calculate the length (or magnitude or norm) of this vector.
     *
     * @return the length (&ge;0)
     * @see com.jme3.math.Vector3f#length()
     */
    float length();

    /**
     * Calculate the squared length of this vector. This is used to compare the
     * lengths of vectors. Returns a double-precision value for precise
     * comparisons.
     *
     * @return the squared length (&ge;0)
     * @see MyVector3f#lengthSquared(Vector3f)
     */
    double lengthSquared();

    /**
     * Mirror (or reflect) this vector across the X-axis (complex conjugate or
     * inverse rotation).
     *
     * @return a mirrored vector with the same length
     */
    ReadXZ mirrorZ();

    /**
     * Scale this vector by a scalar.
     *
     * @param multiplier scaling factor
     * @return a vector 'scalar' times longer than this one, with same direction
     * if multiplier&gt;0, opposite direction if multiplier&lt;0
     * @see com.jme3.math.Vector3f#mult(float)
     */
    ReadXZ mult(float multiplier);

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
    ReadXZ mult(ReadXZ multiplier);

    /**
     * Negate this vector (or reverse its direction or reflect it in the
     * origin). This is equivalent to #mult(-1f)
     *
     * @return a vector with same magnitude and opposite direction
     * @see com.jme3.math.Vector3f#negate()
     */
    ReadXZ negate();

    /**
     * Normalize this vector to a unit vector. If this vector is zero, return a
     * zero vector.
     *
     * @return a unit vector (with the same direction) or a zero vector
     * @see com.jme3.math.Vector3f#normalize()
     */
    ReadXZ normalize();

    /**
     * Rotate a vector CLOCKWISE about the +Y axis. Note: This method is used to
     * apply azimuths, which is why its angle convention is left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return a vector with the same length
     * @see com.jme3.math.Vector2f#rotateAroundOrigin(float, boolean)
     */
    ReadXZ rotate(float radians);

    /**
     * Scale this vector by another (non-uniform scaling).
     *
     * @param multiplier scaled result for the current north (not null)
     * @return a scaled vector
     * @see com.jme3.math.Vector3f#mult(com.jme3.math.Vector3f)
     */
    ReadXZ scale(ReadXZ multiplier);

    /**
     * Subtract from (inverse translate) this vector.
     *
     * @param decrement vector to be subtracted from this vector (not null)
     * @return a vector equal to the difference of the 2 vectors
     * @see com.jme3.math.Vector3f#subtract(com.jme3.math.Vector3f)
     */
    ReadXZ subtract(ReadXZ decrement);

    /**
     * Treating this vector as a rotation (from north), generate an equivalent
     * Quaternion.
     *
     * @return a new Quaternion
     */
    Quaternion toQuaternion();

    /**
     * Treating this vector as a rotation (from north), generate an equivalent
     * Quaternion.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a Quaternion (either storeResult or a new instance, not null)
     */
    Quaternion toQuaternion(Quaternion storeResult);

    /**
     * Create an equivalent 3-D vector.
     *
     * @return a new 3-D vector with y=0
     */
    Vector3f toVector3f();

    /**
     * Create an equivalent 3-D vector with the specified y value.
     *
     * @param y the y-coordinate
     * @return a new 3-D vector
     */
    Vector3f toVector3f(float y);

    /**
     * Create an equivalent 3-D vector with the specified y value.
     *
     * @param y the y-coordinate
     * @param storeResult storage for the result (modified if not null)
     * @return a 3-D vector (either storeResult or a new instance, not null)
     */
    Vector3f toVector3f(float y, Vector3f storeResult);
}

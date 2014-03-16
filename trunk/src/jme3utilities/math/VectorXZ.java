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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A single-precision vector with no 'y' coordinate. These vectors are used to
 * represent map coordinates, directions, and map offsets. For viewport
 * coordinates use Vector2f instead.
 * <p>
 * By convention, +X is north and +Z is east
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class VectorXZ
        implements Cloneable, Savable {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(VectorXZ.class.getName());
    // *************************************************************************
    // fields
    /**
     * the northing component (X coordinate)
     */
    public float x;
    /**
     * the easting component (Z coordinate)
     */
    public float z;
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
     * Instantiate a vector from a quaternion.
     *
     * @param rotation
     */
    public VectorXZ(Quaternion rotation) {
        Vector3f direction = rotation.mult(Vector3f.UNIT_X);
        x = direction.x;
        z = direction.z;
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
     * Add to this vector (with no side effect).
     *
     * @param increment vector to be added to this vector (unaffected, not null)
     * @return a new vector equal to the sum
     */
    public VectorXZ add(VectorXZ increment) {
        VectorXZ result = new VectorXZ(x + increment.x, z + increment.z);
        return result;
    }

    /**
     * Accumulate to this vector (add in place).
     *
     * @param increment vector to be added to this vector (unaffected, not null)
     * @return this vector (with its components modified)
     */
    public VectorXZ addLocal(VectorXZ increment) {
        x += increment.x;
        z += increment.z;
        return this;
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
     * Convert this vector to one of the four cardinal directions (in place).
     *
     * @return this vector (with its components modified)
     */
    public VectorXZ cardinalizeLocal() {
        float length = length();
        while (length == 0f) {
            /*
             * pick random X and Z, each between -0.5 and +0.5
             */
            x = Noise.nextFloat() - 0.5f;
            z = Noise.nextFloat() - 0.5f;
            length = length();
        }

        float absX = FastMath.abs(x);
        float absZ = FastMath.abs(z);
        if (absX > absZ) {
            x = FastMath.sign(x);
            z = 0f;
        } else {
            x = 0f;
            z = FastMath.sign(z);
        }

        return this;
    }

    /**
     * Clamp this direction (in place) to be within a specified angle of the
     * X-axis.
     *
     * @param maxAbsAngle angle in radians (&ge;0)
     * @return this vector (with its components modified)
     */
    public VectorXZ clampDirectionLocal(float maxAbsAngle) {
        Validate.nonNegative(maxAbsAngle, "angle");

        if (maxAbsAngle >= FastMath.HALF_PI) {
            maxAbsAngle = FastMath.HALF_PI;
        }
        if (isZeroLength()) {
            /*
             * special case
             */
            return this;
        }

        float signZ = FastMath.sign(z);
        if (x < 0f) {
            /*
             * clamp to the +X half-plane
             */
            x = 0f;
            z = signZ;
        } else {
            normalizeLocal();
        }
        float maxAbsSine = FastMath.sin(maxAbsAngle);
        if (FastMath.abs(z) > maxAbsSine) {
            z = maxAbsSine * signZ;
            x = MyMath.circle(z);
        }
        return this;
    }

    /**
     * Clamp this vector to be within an axis-aligned ellipse (with no
     * side-effect).
     *
     * @param maxX radius of the ellipse in the X-direction
     * @param maxZ radius of the ellipse in the Z-direction
     * @return a new vector with the same direction
     */
    public VectorXZ clampElliptical(float maxX, float maxZ) {
        VectorXZ result = clone();
        result.clampEllipticalLocal(maxX, maxZ);
        return result;
    }

    /**
     * Clamp this vector to be within an axis-aligned ellipse (in place).
     *
     * @param maxX radius of the ellipse in the X-direction
     * @param maxZ radius of the ellipse in the Z-direction
     * @return this vector (with its components modified)
     */
    public VectorXZ clampEllipticalLocal(float maxX, float maxZ) {
        if (isZeroLength()) {
            /*
             * special case
             */
            return this;
        }

        float theta = azimuth();

        // ellipse in polar coordinates
        float asin = maxX * FastMath.sin(theta);
        float bcos = maxZ * FastMath.cos(theta);
        float r = maxX * maxZ / MyMath.hypotenuse(asin, bcos);
        // scale so that length <= r
        float length = length();
        if (length > r) {
            multLocal(r / length);
        }
        return this;
    }

    /**
     * Copy another vector's components to this vector.
     *
     * @param vector the vector to copy (unaffected, not null)
     */
    public void copy(VectorXZ vector) {
        x = vector.x;
        z = vector.z;
    }

    /**
     * Compute the cross product of this vector with another.
     *
     * @param other the other vector (unaffected, not null)
     * @return the cross product
     */
    public float cross(VectorXZ other) {
        float product = x * other.z - z * other.x;
        return product;
    }

    /**
     * Compute the directional error of this direction with respect to a goal.
     *
     * @param goal a unit vector (unaffected)
     * @return the sine of the angle from the goal to this direction, or +1/-1
     * if the angle's magnitude exceeds 90 degrees
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
     * Divide this vector by a scalar (with no side effect).
     *
     * @param scalar scaling factor
     * @return new vector 'scalar' times shorter than this one
     */
    public VectorXZ divide(float scalar) {
        VectorXZ result = new VectorXZ(x / scalar, z / scalar);
        return result;
    }

    /**
     * Divide this vector by a scalar (in place).
     *
     * @param scalar scaling factor
     * @return this vector (with its components divided)
     */
    public VectorXZ divideLocal(float scalar) {
        x /= scalar;
        z /= scalar;
        return this;
    }

    /**
     * Compute the dot product of this vector with another.
     *
     * @param other the other vector (unaffected, not null)
     * @return the dot product
     */
    public float dot(VectorXZ other) {
        float product = x * other.x + z * other.z;
        return product;
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
     * Mirror this vector across the X-axis (complex conjugate with no side
     * effect).
     *
     * @return a new vector
     */
    public VectorXZ mirrorZ() {
        VectorXZ result = new VectorXZ(x, -z);
        return result;
    }

    /**
     * Scale this vector by a scalar (with no side effect).
     *
     * @param scalar scaling factor
     * @return new vector 'scalar' times longer than this one
     */
    public VectorXZ mult(float scalar) {
        VectorXZ result = new VectorXZ(x * scalar, z * scalar);
        return result;
    }

    /**
     * Scale this vector by a scalar (in place).
     *
     * @param scalar scaling factor
     * @return this vector (with its components scaled)
     */
    public VectorXZ multLocal(float scalar) {
        x *= scalar;
        z *= scalar;
        return this;
    }

    /**
     * Negate this vector (with no side effect).
     *
     * @return a new vector with same magnitude and opposite direction
     */
    public VectorXZ negate() {
        VectorXZ result = new VectorXZ(-x, -z);
        return result;
    }

    /**
     * Negate this vector (in place).
     *
     * @return this vector (with its components negated)
     */
    public VectorXZ negateLocal() {
        x = -x;
        z = -z;
        return this;
    }

    /**
     * Normalize this vector to a unit vector (with no side effect). If this
     * vector has zero length, generate a random direction.
     *
     * @return a new unit vector
     */
    public VectorXZ normalize() {
        VectorXZ result = clone();
        result.normalizeLocal();
        return result;
    }

    /**
     * Normalize this vector to a unit vector (in place). If this vector has
     * zero length, generate a random direction.
     *
     * @return this vector (with its components modified)
     */
    public VectorXZ normalizeLocal() {
        float length = length();
        while (length == 0f) {
            // pick random X and Z, each between -0.5 and +0.5
            x = Noise.nextFloat() - 0.5f;
            z = Noise.nextFloat() - 0.5f;
            length = length();
        }
        divideLocal(length);
        return this;
    }

    /**
     * Subtract from this vector (with no side effect).
     *
     * @param change vector to be subtracted from this vector (unaffected, not
     * null)
     * @return a new vector equal to the difference of the two vectors
     */
    public VectorXZ subtract(VectorXZ change) {
        VectorXZ result = new VectorXZ(x - change.x, z - change.z);
        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis (with no side effect). Note:
     * This method is used to apply azimuths, which is why its angle convention
     * is left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return a new vector with the same length
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
     * Rotate a vector by a direction vector (complex product with no side
     * effect).
     *
     * @param direction new direction for the X-axis (unaffected, not null)
     * @return a new vector
     */
    public VectorXZ rotate(VectorXZ direction) {
        float cosine = direction.x;
        float sine = direction.z;
        float newX = cosine * x - sine * z;
        float newZ = cosine * z + sine * x;
        VectorXZ result = new VectorXZ(newX, newZ);
        return result;
    }

    /**
     * Rotate a vector CLOCKWISE about the +Y axis (in place). Note: This method
     * is used to apply azimuths, which is why its angle convention is
     * left-handed.
     *
     * @param radians clockwise (LH) angle of rotation in radians
     * @return this vector (with its components modified)
     */
    public VectorXZ rotateLocal(float radians) {
        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        float newX = cosine * x - sine * z;
        z = cosine * z + sine * x;
        x = newX;
        return this;
    }

    /**
     * Rotate a vector by a direction vector (complex product in place).
     *
     * @param direction new direction for the X-axis (unaffected, not null)
     * @return this vector (with its components modified)
     */
    public VectorXZ rotateLocal(VectorXZ direction) {
        float cosine = direction.x;
        float sine = direction.z;
        float newX = cosine * x - sine * z;
        z = cosine * z + sine * x;
        x = newX;
        return this;
    }

    /**
     * Treat this vector as a rotation and generate an equivalent quaternion.
     *
     * @return a new instance
     */
    public Quaternion toQuaternion() {
        Quaternion rotation = new Quaternion();
        float angle = -azimuth();
        rotation.fromAngleNormalAxis(angle, Vector3f.UNIT_Y);
        return rotation;
    }

    /**
     * Create a 3D equivalent of this vector.
     *
     * @return a new 3D vector with y=0
     */
    public Vector3f toVector3f() {
        Vector3f result = new Vector3f(x, 0f, z);
        return result;
    }

    /**
     * Create an equivalent 3D vector with a specific y value.
     *
     * @return a new 3D vector with y set
     */
    public Vector3f toVector3f(float y) {
        Vector3f result = new Vector3f(x, y, z);
        return result;
    }

    /**
     * Reset this vector to zero length.
     *
     * @return this vector (with its components modified)
     */
    public VectorXZ zeroLocal() {
        x = 0f;
        z = 0f;
        return this;
    }
    // *************************************************************************
    // Object methods

    /**
     * Clone this instance.
     *
     * @return a new vector with the same components as this vector
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
     * Represent this vector as a string. The format is:
     *
     * [X=XX.XXX, Z=ZZ.ZZZ]
     */
    @Override
    public String toString() {
        String result = String.format("[%.3f, %.3f]", x, z);
        return result;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize the vector, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        x = capsule.readFloat("x", 0f);
        z = capsule.readFloat("z", 0f);
    }

    /**
     * Serialize the vector, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(x, "x", 0f);
        capsule.write(z, "z", 0f);
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
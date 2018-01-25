/*
 Copyright (c) 2014-2018, Stephen Gold
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
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods for computing volumes of shapes. Aside from test cases, all
 * methods should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyVolume {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyVolume.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyVolume() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Compute the volume of an axis-aligned box with specified half-extents.
     *
     * @param halfExtents (not null, all components &ge;0, unaffected)
     * @return volume (ge;0)
     */
    public static float boxVolume(Vector3f halfExtents) {
        Validate.nonNull(halfExtents, "half-extents");
        if (!MyVector3f.isAllNonNegative(halfExtents)) {
            logger.log(Level.SEVERE, "halfExtents={0}", halfExtents);
            throw new IllegalArgumentException(
                    "half-extents should all be non-negative");
        }

        float volume = 8f * halfExtents.x * halfExtents.y * halfExtents.z;
        return volume;
    }

    /**
     * Compute the volume of a capsule with the specified radius and height.
     *
     * @param radius (&ge;0)
     * @param height (&ge;0)
     * @return volume (&ge;0)
     */
    public static float capsuleVolume(float radius, float height) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");

        float halfHeight = 0.5f * height;
        Vector3f cylinderHalfExtents = new Vector3f(halfHeight, radius, radius);
        float cylinderVolume = cylinderVolume(cylinderHalfExtents);
        float volume = cylinderVolume + sphereVolume(radius);

        return volume;
    }

    /**
     * Compute the volume of a cone with the specified radius and height.
     *
     * @param radius (&ge;0)
     * @param height (&ge;0)
     * @return volume (&ge;0)
     */
    public static float coneVolume(float radius, float height) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");

        float volume
                = FastMath.PI * FastMath.ONE_THIRD * radius * radius * height;
        return volume;
    }

    /**
     * Compute the volume of an axis-aligned cylinder with specified
     * half-extents.
     *
     * @param halfExtents (not null, all components &ge;0, unaffected)
     * @return volume (&ge;0)
     */
    public static float cylinderVolume(Vector3f halfExtents) {
        Validate.nonNull(halfExtents, "half-extents");
        if (!MyVector3f.isAllNonNegative(halfExtents)) {
            logger.log(Level.SEVERE, "halfExtents={0}", halfExtents);
            throw new IllegalArgumentException(
                    "half-extents should all be non-negative");
        }

        float volume = FastMath.TWO_PI
                * halfExtents.x * halfExtents.y * halfExtents.z;
        return volume;
    }

    /**
     * Compute the volume of a sphere with the specified radius.
     *
     * @param radius (&ge;0)
     * @return volume (&ge;0)
     */
    public static float sphereVolume(float radius) {
        Validate.nonNegative(radius, "radius");

        float volume
                = 4f * FastMath.ONE_THIRD * FastMath.PI * MyMath.cube(radius);
        return volume;
    }
}

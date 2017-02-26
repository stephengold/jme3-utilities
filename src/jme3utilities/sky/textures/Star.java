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
package jme3utilities.sky.textures;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * An immutable star in a star catalog, used by MakeStarMaps.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Star implements Comparable<Star> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Star.class.getName());
    // *************************************************************************
    // fields

    /**
     * apparent brightness (inverted logarithmic scale, set by constructor)
     */
    final private float apparentMagnitude;
    /**
     * declination (radians north of the celestial equator, &le;Pi/2, &ge;-Pi/2,
     * set by constructor)
     */
    final private float declination;
    /**
     * right ascension (radians east of the vernal equinox, &lt;2*Pi, &ge;0, set
     * by constructor)
     */
    final private float rightAscension;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a star.
     *
     * @param rightAscension radians east of the vernal equinox (&le;2*Pi,
     * &ge;0)
     * @param declination radians north of the celestial equator (&le;Pi/2,
     * &ge;-Pi/2)
     *
     * @param apparentMagnitude apparent brightness (inverted logarithmic scale)
     */
    Star(float rightAscension, float declination, float apparentMagnitude) {
        assert declination <= FastMath.HALF_PI : declination;
        assert declination >= -FastMath.HALF_PI : declination;
        assert rightAscension >= 0f : rightAscension;
        assert rightAscension < FastMath.TWO_PI : rightAscension;

        this.rightAscension = rightAscension;
        this.declination = declination;
        this.apparentMagnitude = apparentMagnitude;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the star's apparent brightness.
     *
     * @return magnitude (inverted logarithmic scale)
     */
    float getApparentMagnitude() {
        return apparentMagnitude;
    }

    /**
     * Compute a star's position in a right-handed Cartesian equatorial
     * coordinate system where:<ul>
     * <li>+X points to the juncture of the meridian with the celestial equator
     * <li>+Y points to the east horizon (also on the celestial equator)
     * <li>+Z points to the celestial north pole
     * </ul>
     *
     * @param siderealTime radians since sidereal midnight (&ge;0, &lt;2*Pi)
     * @return new unit vector
     */
    Vector3f getEquatorialLocation(float siderealTime) {
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;
        /*
         * Compute the hour angle.
         */
        float hourAngle = siderealTime - rightAscension;
        /*
         * Convert hour angle and declination to Cartesian coordinates.
         */
        float cosDec = FastMath.cos(declination);
        float cosHA = FastMath.cos(hourAngle);
        float sinDec = FastMath.sin(declination);
        float sinHA = FastMath.sin(hourAngle);
        float x = cosDec * cosHA;
        float y = -cosDec * sinHA;
        float z = sinDec;
        Vector3f result = new Vector3f(x, y, z);

        assert result.isUnitVector() : result;
        return result;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare this star to another star.
     *
     * @param other the other star
     * @return 0 if the stars are identical or not comparable
     */
    @Override
    public int compareTo(Star other) {
        Validate.nonNull(other, "object");

        if (apparentMagnitude < other.getApparentMagnitude()) {
            return 1;
        } else if (apparentMagnitude > other.getApparentMagnitude()) {
            return -1;
        }
        if (rightAscension < other.getRightAscension()) {
            return 1;
        } else if (rightAscension > other.getRightAscension()) {
            return -1;
        }
        if (declination < other.getDeclination()) {
            return 1;
        } else if (declination > other.getDeclination()) {
            return -1;
        }

        return 0;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test whether this star is equivalent to another.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Star) {
            return compareTo((Star) other) == 0;
        }
        return false;
    }

    /**
     * Generate the hash code for this star.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        float sum = apparentMagnitude + rightAscension + declination;
        int code = Float.valueOf(sum).hashCode();
        return code;
    }
    // *************************************************************************
    // private methods

    /**
     * Read the declination of the star.
     *
     * @return
     */
    private float getDeclination() {
        return declination;
    }

    /**
     * Read the right ascension of the star.
     *
     * @return
     */
    private float getRightAscension() {
        return rightAscension;
    }
}

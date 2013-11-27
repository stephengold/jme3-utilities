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
package jme3utilities.sky;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;

/**
 * A star from a star catalog, used by MakeStarMaps.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class Star
        implements Comparable {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Star.class.getName());
    // *************************************************************************
    // fields
    /**
     * id number in the catalog (>=1)
     */
    int entryId;
    /**
     * apparent brightness (inverted logarithmic scale)
     */
    float apparentMagnitude;
    /**
     * declination (radians north of the celestial equator, <=Pi/2, >=-Pi/2)
     */
    float declination;
    /**
     * right ascension (radians east of the vernal equinox, <2*Pi, >=0)
     */
    float rightAscension;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a star.
     *
     * @param entryId id number in the catalog (>=1)
     * @param rightAscension radians east of the vernal equinox (<2*Pi, >=0)
     * @param declination radians north of the celestial equator (>=-Pi/2,
     * <=Pi/2)
     *
     * @param apparentMagnitude apparent brightness (inverted logarithmic scale)
     */
    Star(int entryId, float rightAscension, float declination,
            float apparentMagnitude) {
        assert entryId >= 1 : entryId;
        assert declination <= FastMath.HALF_PI : declination;
        assert declination >= -FastMath.HALF_PI : declination;
        assert rightAscension >= 0f : rightAscension;
        assert rightAscension < FastMath.TWO_PI : rightAscension;

        this.entryId = entryId;
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
     * Calculate a star's position in a right-handed Cartesian equatorial
     * coordinate system where:
     *
     * +X points to the juncture of the meridian with the celestial equator
     *
     * +Y points to the east horizon (also on the celestial equator)
     *
     * +Z points to the celestial north pole
     *
     * @param siderealTime radians since sidereal midnight (>=0, <2*Pi)
     * @return a new unit vector
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
     * @param object the other star
     * @return 0 if the stars are identical or not comparable
     */
    @Override
    public int compareTo(Object object) {
        if (object == null) {
            throw new NullPointerException("object cannot be null");
        }
        if (object instanceof Star) {
            Star other = (Star) object;

            if (apparentMagnitude < other.apparentMagnitude) {
                return 1;
            } else if (apparentMagnitude > other.apparentMagnitude) {
                return -1;
            }
            if (rightAscension < other.rightAscension) {
                return 1;
            } else if (rightAscension > other.rightAscension) {
                return -1;
            }
            if (declination < other.declination) {
                return 1;
            } else if (declination > other.declination) {
                return -1;
            }
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
            return compareTo(other) == 0;
        }
        return false;
    }

    /**
     * Calculate a hash code for the object.
     */
    @Override
    public int hashCode() {
        float sum = apparentMagnitude + rightAscension + declination;
        int code = Float.valueOf(sum).hashCode();
        return code;
    }
}
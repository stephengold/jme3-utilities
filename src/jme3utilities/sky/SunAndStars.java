/*
 Copyright (c) 2013, Stephen Gold
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
package jme3utilities.sky;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import jme3utilities.MyMath;

/**
 * The orientations of the sun and stars relative to an observer on Earth.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SunAndStars
        implements Savable {
    // *************************************************************************
    // constants

    /**
     * default observer's latitude (radians north of the equator)
     */
    final public static float defaultLatitude = 51.1788f * FastMath.DEG_TO_RAD;
    /**
     * obliquity of the ecliptic, in degrees
     */
    final private static float obliquity = 23.44f * FastMath.DEG_TO_RAD;
    /**
     * the duration of a full day (in hours)
     */
    final private static int hoursPerDay = 24;
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final private static float radiansPerHour = FastMath.TWO_PI / hoursPerDay;
    // *************************************************************************
    // fields
    /**
     * local solar time (hours since midnight, <24, >=0)
     */
    private float hour = 0f;
    /**
     * the observer's latitude (radians north of the equator)
     */
    private float observerLatitude = defaultLatitude;
    /**
     * celestial longitude of the sun (radians east of the vernal equinox,
     * <2*Pi, >=0)
     */
    private float solarLongitude = 0f;
    /**
     * right ascension of the sun (hours east of the vernal equinox, <24, >=0)
     */
    private float solarRaHours = 0f;
    // *************************************************************************
    // new methods exposed

    /**
     * Convert ecliptical angles into an equatorial direction vector.
     *
     * @param lat celestial latitude (radians north of the ecliptic, <=Pi/2,
     * >=-Pi/2)
     * @param lon celestial longitude (radians east of the vernal equinox,
     * <=2*Pi, >=0)
     * @return a new unit vector in a system where: +X points to the vernal
     * equinox, +Y points to the celestial equator 90 degrees east of the vernal
     * equinox, and +Z points to the north celestial pole
     */
    public static Vector3f convertToEquatorial(float lat, float lon) {
        if (lat < -FastMath.HALF_PI || lat > FastMath.HALF_PI) {
            throw new IllegalArgumentException(
                    "latitude must be between -Pi/2 and Pi/2, inclusive");
        }
        if (lon < 0f || lon > FastMath.TWO_PI) {
            throw new IllegalArgumentException(
                    "longitude must be between 0 and 2*Pi, inclusive");
        }
        /*
         * Convert angles to Cartesian ecliptical coordinates.
         */
        float cosLat = FastMath.cos(lat);
        float sinLat = FastMath.sin(lat);
        float cosLon = FastMath.cos(lon);
        float sinLon = FastMath.sin(lon);
        Vector3f ecliptical =
                new Vector3f(cosLat * cosLon, cosLat * sinLon, sinLat);
        assert ecliptical.isUnitVector();
        /*
         * Convert to equatorial coordinates.
         */
        Vector3f equatorial = convertToEquatorial(ecliptical);

        assert equatorial.isUnitVector();
        return equatorial;
    }

    /**
     * Convert ecliptical coordinates into an equatorial coordinates.
     *
     * @param ecliptical coordinates in a system where: +X points to the vernal
     * equinox, +Y points to the ecliptic 90 degrees east of the vernal equinox,
     * and +Z points to the ecliptic north pole (not null)
     *
     * @return a new vector in a system where: +X points to the vernal equinox,
     * +Y points to the celestial equator 90 degrees east of the vernal equinox,
     * and +Z points to the north celestial pole
     */
    public static Vector3f convertToEquatorial(Vector3f ecliptical) {
        if (ecliptical == null) {
            throw new NullPointerException("coordinates cannot be null");
        }
        /*
         * The conversion consists of an (obliquity) rotation about the X
         * (vernal equinox) axis.
         */
        Quaternion rotate = new Quaternion();
        rotate.fromAngleNormalAxis(obliquity, Vector3f.UNIT_X);
        Vector3f equatorial = rotate.mult(ecliptical);

        return equatorial;
    }

    /**
     * Convert ecliptical angles into a world direction vector.
     *
     * @param lat celestial latitude (radians north of the ecliptic, <=Pi/2,
     * >=-Pi/2)
     * @param lon celestial longitude (radians east of the vernal equinox,
     * <=2*Pi, >=0)
     * @return a new unit vector in a system where: +X points to the north
     * horizon, +Y points to the zenith, and +Z points to the east horizon
     */
    public Vector3f convertToWorld(float lat, float lon) {
        if (lat < -FastMath.HALF_PI || lat > FastMath.HALF_PI) {
            throw new IllegalArgumentException(
                    "latitude must be between -Pi/2 and Pi/2, inclusive");
        }
        if (lon < 0f || lon > FastMath.TWO_PI) {
            throw new IllegalArgumentException(
                    "longitude must be between 0 and 2*Pi, inclusive");
        }

        Vector3f equatorial = convertToEquatorial(lat, lon);
        Vector3f world = convertToWorld(equatorial);

        assert world.isUnitVector();
        return world;
    }

    /**
     * Convert equatorial coordinates to world coordinates.
     *
     * @param equatorial coordinates in in a system where: +X points to the
     * vernal equinox, +Y points to the celestial equator 90 degrees east of the
     * vernal equinox, and +Z points to the north celestial pole (not null)
     *
     * @return a new vector in a system where: +X points to the north horizon,
     * +Y points to the zenith, and +Z points to the east horizon
     */
    public Vector3f convertToWorld(Vector3f equatorial) {
        if (equatorial == null) {
            throw new NullPointerException("coordinates cannot be null");
        }

        float siderealAngle = getSiderealAngle();
        /*
         * The conversion consists of a (-siderealAngle) rotation about the Z
         * (north celestial pole) axis followed by a (latitude - Pi/2) rotation
         * about the Y (east) axis followed by a permutation of the axes.
         */
        Quaternion zRotation = new Quaternion();
        zRotation.fromAngleNormalAxis(-siderealAngle, Vector3f.UNIT_Z);
        Vector3f rotated = zRotation.mult(equatorial);

        float coLatitude = FastMath.HALF_PI - observerLatitude;
        Quaternion yRotation = new Quaternion();
        yRotation.fromAngleNormalAxis(-coLatitude, Vector3f.UNIT_Y);
        rotated = yRotation.mult(rotated);

        Vector3f world = new Vector3f(-rotated.x, rotated.z, rotated.y);

        return world;
    }

    /**
     * Read the time of day.
     *
     * @return hours since midnight, solar time (<=24, >=0)
     */
    public float getHour() {
        assert hour <= hoursPerDay : hour;
        assert hour >= 0f : hour;

        return hour;
    }

    /**
     * Read the observer's latitude.
     *
     * @return radians north of the equator (<=Pi/2, >=-Pi/2)
     */
    public float getObserverLatitude() {
        assert observerLatitude <= FastMath.HALF_PI : observerLatitude;
        assert observerLatitude >= -FastMath.HALF_PI : observerLatitude;

        return observerLatitude;
    }

    /**
     * Compute the angle between the meridian and the vernal equinox.
     *
     * @return angle (in radians, <2*Pi, >=0)
     */
    public float getSiderealAngle() {
        float siderealHour = getSiderealHour();
        float siderealAngle = siderealHour * radiansPerHour;

        assert siderealAngle >= 0f : siderealAngle;
        assert siderealAngle < FastMath.TWO_PI : siderealAngle;
        return siderealAngle;
    }

    /**
     * Compute the sidereal time.
     *
     * @return time (in hours, <24, >=0)
     */
    public float getSiderealHour() {
        float noon = 12f;
        float siderealHour = hour - noon - solarRaHours;
        siderealHour = MyMath.modulo(siderealHour, hoursPerDay);

        return siderealHour;
    }

    /**
     * Read the solar longitude.
     *
     * @return radians east of the vernal equinox (<=2*Pi, >=0)
     */
    public float getSolarLongitude() {
        assert solarLongitude <= FastMath.TWO_PI : solarLongitude;
        assert solarLongitude >= 0f : solarLongitude;

        return solarLongitude;
    }

    /**
     * Alter the time of day.
     *
     * @param hour hours since midnight, solar time (<=24, >=0)
     */
    public void setHour(float hour) {
        if (hour < 0f || hour > hoursPerDay) {
            throw new IllegalArgumentException(
                    "hour must be between 0 and 24, inclusive");
        }

        this.hour = hour;
    }

    /**
     * Alter the observer's latitude.
     *
     * @param latitude radians north of the equator (<=Pi/2, >=-Pi/2)
     */
    public void setObserverLatitude(float latitude) {
        if (latitude < -FastMath.HALF_PI || latitude > FastMath.HALF_PI) {
            throw new IllegalArgumentException(
                    "latitude must be between -Pi/2 and Pi/2, inclusive");
        }

        this.observerLatitude = latitude;
    }

    /**
     * Alter the sun's celestial longitude.
     *
     * @param longitude radians east of the vernal equinox (<=2*Pi, >=0)
     */
    public void setSolarLongitude(float longitude) {
        if (longitude < 0f || longitude > FastMath.TWO_PI) {
            throw new IllegalArgumentException(
                    "longitude must be between 0 and 2*Pi");
        }

        solarLongitude = longitude;
        /*
         * Update the cached solar right ascension.
         */
        Vector3f equatorial = convertToEquatorial(0f, longitude);
        float ra = -FastMath.atan2(equatorial.y, equatorial.x);
        solarRaHours = MyMath.modulo(ra / radiansPerHour, hoursPerDay);
        assert solarRaHours >= 0f : solarRaHours;
        assert solarRaHours < hoursPerDay : solarRaHours;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading from a .jm3o file.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);

        float value = capsule.readFloat("hour", 0f);
        setHour(value);

        value = capsule.readFloat("observerLatitude", defaultLatitude);
        setObserverLatitude(value);

        value = capsule.readFloat("solarLongitude", 0f);
        setSolarLongitude(value);
    }

    /**
     * Serialize this instance when saving to a .jm3o file.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(hour, "hour", 0f);
        capsule.write(observerLatitude, "observerLatitude", defaultLatitude);
        capsule.write(solarLongitude, "observerLatitude", 0f);
    }
}
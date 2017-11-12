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
package jme3utilities.sky;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Component of SkyControl to model the orientations of the sun, moon, and stars
 * relative to an observer on Earth.
 * <p>
 * Three right-handed Cartesian coordinate systems are used: ecliptical,
 * equatorial, and world.
 * <p>
 * In ecliptical coordinates:<ul>
 * <li>+X points to the March equinox (in Pisces)
 * <li>+Y points to the ecliptic 90 degrees east of the March equinox (in
 * Gemini)
 * <li>+Z points to the north ecliptic pole (in Draco)
 * </ul>
 * In equatorial coordinates:<ul>
 * <li>+X points to the March equinox (in Pisces)
 * <li>+Y points to the celestial equator 90 degrees east of the March equinox
 * (in Orion)
 * <li>+Z points to the north celestial pole (in Ursa Minor)
 * </ul>
 * In world coordinates:<ul>
 * <li>+X points to the north horizon,
 * <li>+Y points to the zenith
 * <li>+Z points to the east horizon
 * </ul>
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SunAndStars
        implements Cloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * obliquity of the ecliptic, in radians
     */
    final private static float obliquity = MyMath.toRadians(23.44f);
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final private static float radiansPerHour
            = FastMath.TWO_PI / Constants.hoursPerDay;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SunAndStars.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Z}
     */
    final private static Vector3f zAxis = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // fields

    /**
     * local solar time (hours since midnight, &lt;24, &ge;0)
     */
    private float hour = 0f;
    /**
     * observer's latitude (radians north of the equator)
     */
    private float observerLatitude = Constants.defaultLatitude;
    /**
     * celestial longitude of the sun (radians east of the March equinox,
     * &lt;2*Pi, &ge;0)
     */
    private float solarLongitude = 0f;
    /**
     * right ascension of the sun (hours east of the March equinox, &lt;24,
     * &ge;0)
     */
    private float solarRaHours = 0f;
    // *************************************************************************
    // new methods exposed

    /**
     * Convert ecliptical angles into an equatorial direction vector.
     *
     * @param latitude celestial latitude (radians north of the ecliptic,
     * &le;Pi/2, &ge;-Pi/2)
     * @param longitude celestial longitude (radians east of the March equinox,
     * &le;2*Pi, &ge;0)
     * @return a new unit vector in equatorial coordinates
     */
    public static Vector3f convertToEquatorial(float latitude,
            float longitude) {
        Validate.inRange(latitude, "latitude",
                -FastMath.HALF_PI, FastMath.HALF_PI);
        Validate.inRange(longitude, "longitude", 0f, FastMath.TWO_PI);
        /*
         * Convert angles to Cartesian ecliptical coordinates.
         */
        float cosLat = FastMath.cos(latitude);
        float sinLat = FastMath.sin(latitude);
        float cosLon = FastMath.cos(longitude);
        float sinLon = FastMath.sin(longitude);
        Vector3f ecliptical = new Vector3f(
                cosLat * cosLon, cosLat * sinLon, sinLat);
        assert ecliptical.isUnitVector() : ecliptical;
        /*
         * Convert to equatorial coordinates.
         */
        Vector3f equatorial = convertToEquatorial(ecliptical);

        assert equatorial.isUnitVector() : equatorial;
        return equatorial;
    }

    /**
     * Convert ecliptical coordinates to equatorial coordinates.
     *
     * @param ecliptical coordinates (not null, unaffected)
     * @return a new vector in equatorial coordinates
     */
    public static Vector3f convertToEquatorial(Vector3f ecliptical) {
        Validate.nonNull(ecliptical, "coordinates");
        /*
         * The conversion consists of a rotation about the +X
         * (March equinox) axis.
         */
        Quaternion rotate = new Quaternion();
        rotate.fromAngleNormalAxis(obliquity, xAxis);
        Vector3f equatorial = rotate.mult(ecliptical);

        return equatorial;
    }

    /**
     * Convert ecliptical angles into a world direction vector.
     *
     * @param latitude celestial latitude (radians north of the ecliptic,
     * &le;Pi/2, &ge;-Pi/2)
     * @param longitude celestial longitude (radians east of the March equinox,
     * &le;2*Pi, &ge;0)
     * @return a new unit vector in world (horizontal) coordinates
     */
    public Vector3f convertToWorld(float latitude, float longitude) {
        Validate.inRange(latitude, "latitude",
                -FastMath.HALF_PI, FastMath.HALF_PI);
        Validate.inRange(longitude, "longitude", 0f, FastMath.TWO_PI);

        Vector3f equatorial = convertToEquatorial(latitude, longitude);
        Vector3f world = convertToWorld(equatorial);

        assert world.isUnitVector();
        return world;
    }

    /**
     * Convert equatorial coordinates to world (horizontal) coordinates.
     *
     * @param equatorial coordinates (not null, unaffected)
     * @return a new vector in a world coordinates
     */
    public Vector3f convertToWorld(Vector3f equatorial) {
        Validate.nonNull(equatorial, "coordinates");

        float siderealAngle = siderealAngle();
        /*
         * The conversion consists of a (-siderealAngle) rotation about the Z
         * (north celestial pole) axis followed by a (latitude - Pi/2) rotation
         * about the Y (east) axis followed by a permutation of the axes.
         */
        Quaternion zRotation = new Quaternion();
        zRotation.fromAngleNormalAxis(-siderealAngle, zAxis);
        Vector3f rotated = zRotation.mult(equatorial);

        float coLatitude = FastMath.HALF_PI - observerLatitude;
        Quaternion yRotation = new Quaternion();
        yRotation.fromAngleNormalAxis(-coLatitude, yAxis);
        rotated = yRotation.mult(rotated);

        Vector3f world = new Vector3f(-rotated.x, rotated.z, rotated.y);

        return world;
    }

    /**
     * Read the time of day.
     *
     * @return hours since midnight, solar time (&le;24, &ge;0)
     */
    public float getHour() {
        assert hour <= Constants.hoursPerDay : hour;
        assert hour >= 0f : hour;

        return hour;
    }

    /**
     * Read the observer's latitude.
     *
     * @return radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     */
    public float getObserverLatitude() {
        assert observerLatitude <= FastMath.HALF_PI : observerLatitude;
        assert observerLatitude >= -FastMath.HALF_PI : observerLatitude;

        return observerLatitude;
    }

    /**
     * Read the solar longitude.
     *
     * @return radians east of the March equinox (&le;2*Pi, &ge;0)
     */
    public float getSolarLongitude() {
        assert solarLongitude <= FastMath.TWO_PI : solarLongitude;
        assert solarLongitude >= 0f : solarLongitude;

        return solarLongitude;
    }

    /**
     * Update the orientation of a sky whose local axes are equatorial, as
     * defined above.
     *
     * @param spatial the geometries to orient (not null)
     * @param invertRotation true for rotation-inverting materials such as
     * Sky.j3md, false for ordinary materials such as Unshaded.j3md
     */
    public void orientEquatorialSky(Spatial spatial, boolean invertRotation) {
        Validate.nonNull(spatial, "spatial");

        float siderealAngle = siderealAngle();
        Quaternion xRotation = new Quaternion();
        xRotation.fromAngleNormalAxis(-siderealAngle, xAxis);
        Quaternion zRotation = new Quaternion();
        zRotation.fromAngleNormalAxis(observerLatitude, zAxis);
        Quaternion orientation = zRotation.mult(xRotation);
        if (invertRotation) {
            orientation.inverseLocal();
        }
        MySpatial.setWorldOrientation(spatial, orientation);
    }

    /**
     * Update the orientations of north and south star domes.
     *
     * @param northDome (ignored if null)
     * @param southDome (ignored if null)
     */
    public void orientStarDomes(Spatial northDome, Spatial southDome) {
        float siderealAngle = siderealAngle();
        Quaternion yRotation = new Quaternion();
        Quaternion zRotation = new Quaternion();
        if (northDome != null) {
            /*
             * Orient the north dome.
             */
            yRotation.fromAngleNormalAxis(-siderealAngle, yAxis);
            float coLatitude = FastMath.HALF_PI - observerLatitude;
            zRotation.fromAngleNormalAxis(-coLatitude, zAxis);
            Quaternion orientation = zRotation.mult(yRotation);
            MySpatial.setWorldOrientation(northDome, orientation);
        }
        if (southDome != null) {
            /*
             * Orient the south dome.
             */
            yRotation.fromAngleNormalAxis(siderealAngle, yAxis);
            float angle = FastMath.HALF_PI + observerLatitude;
            zRotation.fromAngleNormalAxis(angle, zAxis);
            Quaternion orientation = zRotation.mult(yRotation);
            MySpatial.setWorldOrientation(southDome, orientation);
        }
    }

    /**
     * Alter the time of day.
     *
     * @param newHour hours since midnight, solar time (&le;24, &ge;0)
     */
    public void setHour(float newHour) {
        Validate.inRange(newHour, "new hour", 0f, Constants.hoursPerDay);
        hour = newHour;
    }

    /**
     * Alter the observer's latitude.
     *
     * @param latitude radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     */
    public void setObserverLatitude(float latitude) {
        Validate.inRange(latitude, "latitude",
                -FastMath.HALF_PI, FastMath.HALF_PI);
        observerLatitude = latitude;
    }

    /**
     * Alter the sun's celestial longitude directly.
     *
     * @param longitude radians east of the March equinox (&le;2*Pi, &ge;0)
     */
    public void setSolarLongitude(float longitude) {
        Validate.inRange(longitude, "longitude", 0f, FastMath.TWO_PI);

        solarLongitude = longitude;
        /*
         * Update the cached solar right ascension.
         */
        Vector3f equatorial = convertToEquatorial(0f, longitude);
        float ra = -FastMath.atan2(equatorial.y, equatorial.x);
        solarRaHours
                = MyMath.modulo(ra / radiansPerHour, Constants.hoursPerDay);
        assert solarRaHours >= 0f : solarRaHours;
        assert solarRaHours < Constants.hoursPerDay : solarRaHours;
    }

    /**
     * Set the sun's celestial longitude to approximate a specified day of the
     * year.
     * <p>
     * This convenience method uses a crude approximation, which is accurate
     * within a couple degrees of arc. A more accurate formula may be obtained
     * from Steyaert, C. (1991) "Calculating the solar longitude 2000.0", WGN
     * (Journal of the International Meteor Organization) 19-2, pages 31-34,
     * available from http://adsabs.harvard.edu/full/1991JIMO...19...31S
     *
     * @param month zero-based month of the Gregorian year (&lt;12, &ge;0, 0
     * &rarr; January)
     * @param day day of the Gregorian month (&le;31, &ge;1)
     */
    public void setSolarLongitude(int month, int day) {
        Validate.inRange(month, "month", 0, 11);
        Validate.inRange(day, "day", 1, 31);
        /*
         * Convert month and day to day-of-the-year.
         */
        int year = 2_000; // a recent leap year
        Calendar calendar = new GregorianCalendar();
        calendar.set(year, month, day, 12, 0, 0); // noon, standard time
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        /*
         * Compute the approximate solar longitude (in radians).
         */
        float daysSinceEquinox = dayOfYear - 80;
        float longitude = FastMath.TWO_PI * daysSinceEquinox / 366f;

        longitude = MyMath.modulo(longitude, FastMath.TWO_PI);
        setSolarLongitude(longitude);
    }

    /**
     * Compute the angle between the meridian and the March equinox.
     *
     * @return angle (in radians, &lt;2*Pi, &ge;0)
     */
    public float siderealAngle() {
        float siderealHour = siderealHour();
        float siderealAngle = siderealHour * radiansPerHour;

        assert siderealAngle >= 0f : siderealAngle;
        assert siderealAngle < FastMath.TWO_PI : siderealAngle;
        return siderealAngle;
    }

    /**
     * Compute the sidereal time.
     *
     * @return time (in hours, &lt;24, &ge;0)
     */
    public float siderealHour() {
        float noon = 12f;
        float siderealHour = hour - noon - solarRaHours;
        siderealHour = MyMath.modulo(siderealHour, Constants.hoursPerDay);

        return siderealHour;
    }

    /**
     * Calculate the direction to the center of the sun.
     *
     * @return a new unit vector in world (horizontal) coordinates
     */
    public Vector3f sunDirection() {
        Vector3f result = convertToWorld(0f, solarLongitude);

        assert result.isUnitVector();
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Clone this instance.
     *
     * @return new instance equivalent to this one
     * @throws CloneNotSupportedException from Object.clone()
     */
    @Override
    public SunAndStars clone() throws CloneNotSupportedException {
        SunAndStars clone = (SunAndStars) super.clone();
        return clone;
    }

    /**
     * Represent this instance as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        float latitudeDegrees = MyMath.toDegrees(observerLatitude);
        float longitudeDegrees = MyMath.toDegrees(solarLongitude);
        String result = String.format(
                "[hour=%f, lat=%f deg, long=%f deg, ra=%f]",
                hour, latitudeDegrees, longitudeDegrees, solarRaHours);

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
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);

        float value = capsule.readFloat("hour", 0f);
        setHour(value);

        value = capsule.readFloat("observerLatitude",
                Constants.defaultLatitude);
        setObserverLatitude(value);

        value = capsule.readFloat("solarLongitude", 0f);
        setSolarLongitude(value);
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(hour, "hour", 0f);
        capsule.write(observerLatitude, "observerLatitude",
                Constants.defaultLatitude);
        capsule.write(solarLongitude, "solarLongitude", 0f);
    }
}

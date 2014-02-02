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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.MySpatial;

/**
 * The component of SkyControl which models the orientations of the sun and
 * stars relative to an observer on Earth.
 * <p>
 * Three right-handed Cartesian coordinate systems are used: ecliptical,
 * equatorial, and world.
 * <p>
 * In ecliptical coordinates:<ul>
 * <li>+X points to the vernal equinox
 * <li>+Y points to the celestial equator 90 degrees east of the vernal equinox
 * <li>+Z points to the north celestial pole
 * </ul>
 * In equatorial coordinates:<ul>
 * <li>+X points to the vernal equinox
 * <li>+Y points to the celestial equator 90 degrees east of the vernal equinox
 * <li>+Z points to the north celestial pole
 * </ul>
 * In world coordinates:<ul>
 * <li>+X points to the north horizon,
 * <li>+Y points to the zenith
 * <li>+Z points to the east horizon
 * </ul>
 *
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SunAndStars
        implements Cloneable, Savable {
    // *************************************************************************
    // constants

    /**
     * obliquity of the ecliptic, in radians
     */
    final private static float obliquity = 23.44f * FastMath.DEG_TO_RAD;
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final private static float radiansPerHour =
            FastMath.TWO_PI / Constants.hoursPerDay;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SunAndStars.class.getName());
    // *************************************************************************
    // fields
    /**
     * local solar time (hours since midnight, <24, >=0)
     */
    private float hour = 0f;
    /**
     * the observer's latitude (radians north of the equator)
     */
    private float observerLatitude = Constants.defaultLatitude;
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
     * @param latitude celestial latitude (radians north of the ecliptic,
     * <=Pi/2, >=-Pi/2)
     * @param longitude celestial longitude (radians east of the vernal equinox,
     * <=2*Pi, >=0)
     * @return a new unit vector in equatorial coordinates
     */
    public static Vector3f convertToEquatorial(float latitude,
            float longitude) {
        if (!(latitude >= -FastMath.HALF_PI && latitude <= FastMath.HALF_PI)) {
            logger.log(Level.SEVERE, "latitude={0}", latitude);
            throw new IllegalArgumentException(
                    "latitude should be between -Pi/2 and Pi/2, inclusive");
        }
        if (!(longitude >= 0f && longitude <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "longitude={0}", longitude);
            throw new IllegalArgumentException(
                    "longitude should be between 0 and 2*Pi, inclusive");
        }
        /*
         * Convert angles to Cartesian ecliptical coordinates.
         */
        float cosLat = FastMath.cos(latitude);
        float sinLat = FastMath.sin(latitude);
        float cosLon = FastMath.cos(longitude);
        float sinLon = FastMath.sin(longitude);
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
     * Convert ecliptical coordinates to equatorial coordinates.
     *
     * @param ecliptical coordinates (not null, not altered)
     * @return a new vector in equatorial coordinates
     */
    public static Vector3f convertToEquatorial(Vector3f ecliptical) {
        if (ecliptical == null) {
            throw new NullPointerException("coordinates should not be null");
        }
        /*
         * The conversion consists of a rotation about the +X
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
     * @param latitude celestial latitude (radians north of the ecliptic,
     * <=Pi/2, >=-Pi/2)
     * @param longitude celestial longitude (radians east of the vernal equinox,
     * <=2*Pi, >=0)
     * @return a new unit vector in world (horizontal) coordinates
     */
    public Vector3f convertToWorld(float latitude, float longitude) {
        if (!(latitude >= -FastMath.HALF_PI && latitude <= FastMath.HALF_PI)) {
            logger.log(Level.SEVERE, "latitude={0}", latitude);
            throw new IllegalArgumentException(
                    "latitude should be between -Pi/2 and Pi/2, inclusive");
        }
        if (!(longitude >= 0f && longitude <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "latitude={0}", latitude);
            throw new IllegalArgumentException(
                    "longitude should be between 0 and 2*Pi, inclusive");
        }

        Vector3f equatorial = convertToEquatorial(latitude, longitude);
        Vector3f world = convertToWorld(equatorial);

        assert world.isUnitVector();
        return world;
    }

    /**
     * Convert equatorial coordinates to world (horizontal) coordinates.
     *
     * @param equatorial coordinates (not null, not altered)
     * @return a new vector in a world coordinates
     */
    public Vector3f convertToWorld(Vector3f equatorial) {
        if (equatorial == null) {
            throw new NullPointerException("coordinates should not be null");
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
        assert hour <= Constants.hoursPerDay : hour;
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
        siderealHour = MyMath.modulo(siderealHour, Constants.hoursPerDay);

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
     * Update the orientations of north and south star domes.
     *
     * @param northDome (ignored if null)
     * @param southDome (ignored if null)
     */
    public void orientStarDomes(Spatial northDome, Spatial southDome) {
        float siderealAngle = getSiderealAngle();
        Quaternion yRotation = new Quaternion();
        Quaternion zRotation = new Quaternion();
        if (northDome != null) {
            /*
             * Orient the north dome.
             */
            yRotation.fromAngleNormalAxis(-siderealAngle, Vector3f.UNIT_Y);
            float coLatitude = FastMath.HALF_PI - observerLatitude;
            zRotation.fromAngleNormalAxis(-coLatitude, Vector3f.UNIT_Z);
            Quaternion orientation = zRotation.mult(yRotation);
            MySpatial.setWorldOrientation(northDome, orientation);
        }
        if (southDome != null) {
            /*
             * Orient the north dome.
             */
            yRotation.fromAngleNormalAxis(siderealAngle, Vector3f.UNIT_Y);
            float angle = FastMath.HALF_PI + observerLatitude;
            zRotation.fromAngleNormalAxis(angle, Vector3f.UNIT_Z);
            Quaternion orientation = zRotation.mult(yRotation);
            MySpatial.setWorldOrientation(southDome, orientation);
        }
    }

    /**
     * Alter the time of day.
     *
     * @param newHour hours since midnight, solar time (<=24, >=0)
     */
    public void setHour(float newHour) {
        if (!(newHour >= 0f && newHour <= Constants.hoursPerDay)) {
            logger.log(Level.SEVERE, "hour={0}", newHour);
            throw new IllegalArgumentException(
                    "hour should be between 0 and 24, inclusive");
        }

        this.hour = newHour;
    }

    /**
     * Alter the observer's latitude.
     *
     * @param latitude radians north of the equator (<=Pi/2, >=-Pi/2)
     */
    public void setObserverLatitude(float latitude) {
        if (!(latitude >= -FastMath.HALF_PI && latitude <= FastMath.HALF_PI)) {
            logger.log(Level.SEVERE, "latitude={0}", latitude);
            throw new IllegalArgumentException(
                    "latitude should be between -Pi/2 and Pi/2, inclusive");
        }

        this.observerLatitude = latitude;
    }

    /**
     * Alter the sun's celestial longitude directly.
     *
     * @param longitude radians east of the vernal equinox (<=2*Pi, >=0)
     */
    public void setSolarLongitude(float longitude) {
        if (!(longitude >= 0f && longitude <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "longitude={0}", longitude);
            throw new IllegalArgumentException(
                    "longitude should be between 0 and 2*Pi");
        }

        solarLongitude = longitude;
        /*
         * Update the cached solar right ascension.
         */
        Vector3f equatorial = convertToEquatorial(0f, longitude);
        float ra = -FastMath.atan2(equatorial.y, equatorial.x);
        solarRaHours =
                MyMath.modulo(ra / radiansPerHour, Constants.hoursPerDay);
        assert solarRaHours >= 0f : solarRaHours;
        assert solarRaHours < Constants.hoursPerDay : solarRaHours;
    }

    /**
     * Set the sun's celestial longitude to approximate a specific day of the
     * year.
     * <p>
     * This convenience method uses a crude approximation which is accurate
     * within a couple degrees of arc. A more accurate formula may be obtained
     * from Steyaert, C. (1991) "Calculating the solar longitude 2000.0", WGN
     * (Journal of the International Meteor Organization) 19-2, pages 31-34,
     * available from http://adsabs.harvard.edu/full/1991JIMO...19...31S
     *
     * @param month zero-based month of the Gregorian year (<12, >=0)
     * @param day of the Gregorian month (<=31, >=1)
     */
    public void setSolarLongitude(int month, int day) {
        if (month < 0 || month >= 12) {
            logger.log(Level.SEVERE, "month={0}", month);
            throw new IllegalArgumentException(
                    "month should be between 0 and 11, inclusive");
        }
        if (day < 1 || day > 31) {
            logger.log(Level.SEVERE, "day={0}", day);
            throw new IllegalArgumentException(
                    "day should be between 1 and 31, inclusive");
        }
        /*
         * Convert month and day to day-of-the-year.
         */
        int year = 2000; // a recent leap year
        Calendar calendar = new GregorianCalendar();
        calendar.set(year, month, day, 12, 0, 0); // noon, standard time
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        /*
         * Compute the approximate solar longitude (in radians).
         */
        float daysSinceEquinox = (float) (dayOfYear - 80);
        float longitude = FastMath.TWO_PI * daysSinceEquinox / 366f;

        longitude = MyMath.modulo(longitude, FastMath.TWO_PI);
        setSolarLongitude(longitude);
    }
    // *************************************************************************
    // Object methods

    /**
     * Clone this instance.
     *
     * @return a new instance equivalent to this one
     */
    @Override
    public SunAndStars clone() {
        try {
            SunAndStars clone = (SunAndStars) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError();
        }
    }

    /**
     * Represent this instance as a string.
     */
    @Override
    public String toString() {
        float latitudeDegrees = observerLatitude * FastMath.RAD_TO_DEG;
        float longitudeDegrees = solarLongitude * FastMath.RAD_TO_DEG;
        String result = String.format(
                "[hour=%f, lat=%f deg, long=%f deg, ra=%f]",
                hour, latitudeDegrees, longitudeDegrees, solarRaHours);

        return result;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading from a J3O file.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
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
     * Serialize this instance when saving to a J3O file.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(hour, "hour", 0f);
        capsule.write(observerLatitude, "observerLatitude",
                Constants.defaultLatitude);
        capsule.write(solarLongitude, "observerLatitude", 0f);
    }
    // *************************************************************************
    // test cases

    /**
     * A console application to test this class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        logger.setLevel(Level.INFO);
        System.out.print("Test results for class SunAndStars:\n\n");

        SunAndStars test = new SunAndStars();
        System.out.printf("Default value:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.DECEMBER, 31);
        System.out.printf(" on December 31st:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.JANUARY, 1);
        System.out.printf(" on January 1st:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.FEBRUARY, 29);
        System.out.printf(" on February 29th:  %s%n", test.toString());

        test.setSolarLongitude(Calendar.MARCH, 1);
        System.out.printf(" on March 1st:  %s%n", test.toString());

        SunAndStars copy = test.clone();
        System.out.printf("Clone of last value:  %s%n", copy.toString());

        test.setSolarLongitude(Calendar.MARCH, 20);
        System.out.printf(" on March 20th:  %s%n", test.toString());

        test.setObserverLatitude(FastMath.HALF_PI);
        System.out.printf(" at the North Pole:  %s%n", test.toString());

        test.setObserverLatitude(-FastMath.HALF_PI);
        System.out.printf(" at the South Pole:  %s%n", test.toString());

        test.setObserverLatitude(0f);
        System.out.printf(" at the Equator:  %s%n", test.toString());

        test.setHour(23f + (59f + 59f / 60f) / 60f);
        System.out.printf(" at 23:59:59 LST:  %s%n", test.toString());
    }
}
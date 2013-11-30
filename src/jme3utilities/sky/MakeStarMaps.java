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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyMath;
import jme3utilities.MyString;

/**
 * A utility application to generate starry sky texture maps for use with
 * SkyMaterial and DomeMesh, based on data from a star catalog. In the resulting
 * textures, east is at the top and north is to the right.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MakeStarMaps {
    // *************************************************************************
    // throwables

    /**
     * exception to indicate unexpected invalid data in a catalog entry
     */
    static class InvalidEntryException
            extends Exception {

        InvalidEntryException(String message) {
            super(message);
        }
    }

    /**
     * exception to indicate an invalid apparent magnitude in a catalog entry:
     * such entries can be ignored
     */
    static class InvalidMagnitudeException
            extends Exception {
    }
    // *************************************************************************
    // constants
    /**
     * luminosity of a magnitude-0 star
     */
    final private static float luminosity0 = 37f;
    /**
     * luminosity of the faintest stars to include
     */
    final private static float luminosityCutoff = 0.1f;
    /**
     * maximum (dimmest) apparent magnitude of all stars in the catalog
     */
    final private static float maxMagnitude = 7.96f;
    /**
     * minimum (brightest) apparent magnitude of all stars in the catalog
     */
    final private static float minMagnitude = 0.01f;
    /**
     * luminosity ratio between successive stellar magnitudes (5th root of 100)
     */
    final private static float pogsonsRatio = FastMath.pow(100f, 0.2f);
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final private static float radiansPerHour =
            FastMath.TWO_PI / SkyControl.hoursPerDay;
    /**
     * expected id of the last entry in the catalog
     */
    final private static int lastEntryExpected = 9110;
    /**
     * number of degrees from equator to pole
     */
    final private static int maxDeclination = 90;
    /**
     * number of minutes in an hour or degree
     */
    final private static int maxMinutes = 60;
    /**
     * number of seconds in a minute
     */
    final private static int maxSeconds = 60;
    /**
     * size of the texture map (pixels per side)
     */
    final private static int textureSize = 2048;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MakeStarMaps.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeStarMaps";
    /**
     * file path to the input file, a textual version of the Yale Catalogue of
     * Bright Stars, which may be downloaded from
     * http://www-kpno.kpno.noao.edu/Info/Caches/Catalogs/BSC5/catalog5.html
     */
    final private static String catalogFilePath =
            "assets/Textures/skies/Bright Star Catalog.htm";
    // *************************************************************************
    // fields
    /**
     * true means just display the usage message; false means run the
     * application
     */
    @Parameter(names = {"-h", "-u", "--help", "--usage"}, help = true,
            description = "display this usage message")
    private static boolean usageOnly = false;
    /**
     * stars read from the catalog
     */
    private Collection<Star> stars = new TreeSet();
    /**
     * sample dome mesh for calculating texture coordinates
     */
    private DomeMesh mesh = new DomeMesh();
    /**
     * name of preset
     */
    @Parameter(names = {"-p", "--preset"}, description = "specify preset")
    private static String presetName = "all";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeStarMaps application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        /*
         * Set the logging level for this class and also for writeMap().
         */
        logger.setLevel(Level.WARNING);
        Logger.getLogger(jme3utilities.Misc.class.getName())
                .setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        MakeStarMaps application = new MakeStarMaps();
        /*
         * Parse the command-line arguments.
         */
        JCommander jCommander = new JCommander(application, arguments);
        jCommander.setProgramName(applicationName);
        if (usageOnly) {
            jCommander.usage();
            return;
        }
        if (!"all".equals(presetName)) {
            StarMapPreset preset = StarMapPreset.fromDescription(presetName);
            if (preset == null) {
                /*
                 * invalid preset name
                 */
                jCommander.usage();
                return;
            }
        }
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));
        /*
         * Read the star catalog.
         */
        application.readCatalog();
        if (application.stars.isEmpty()) {
            return;
        }
        /*
         * Generate texture maps.
         */
        if ("all".equals(presetName)) {
            for (StarMapPreset preset : StarMapPreset.values()) {
                application.generateMap(preset);
            }

        } else {
            StarMapPreset preset = StarMapPreset.fromDescription(presetName);
            application.generateMap(preset);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a starry sky texture map.
     *
     * @param preset which map to generate (not null)
     */
    private void generateMap(StarMapPreset preset) {
        assert preset != null;

        float latitude = preset.latitude();
        logger.log(Level.INFO, "latitude is {0} degrees",
                latitude * FastMath.RAD_TO_DEG);

        float siderealHour = preset.hour();
        logger.log(Level.INFO, "sidereal time is {0} hours", siderealHour);

        RenderedImage image = generateMap(latitude, siderealHour);
        String filePath = String.format("assets/Textures/skies/star-maps/%s.png",
                preset.textureFileName());
        try {
            Misc.writeMap(filePath, image);
        } catch (IOException exception) {
            // ignored
        }
    }

    /**
     * Generate a starry sky texture map.
     *
     * @param latitude radians north of the equator (>=-Pi/2, <=Pi/2)
     *
     * @param siderealTime hours since sidereal midnight (>=0, <24)
     * @return a new instance
     */
    private RenderedImage generateMap(float latitude, float siderealHour) {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealHour >= 0f : siderealHour;
        assert siderealHour < SkyControl.hoursPerDay : siderealHour;
        /*
         * Create a blank, grayscale buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_BYTE_GRAY);
        /*
         * Convert the sidereal time from hours to radians.
         */
        float siderealTime = siderealHour * radiansPerHour;
        /*
         * Plot individual stars on the image, starting with the faintest.
         */
        int plotCount = 0;
        for (Star star : stars) {
            boolean success = plotStar(map, star, latitude, siderealTime);
            if (success) {
                plotCount++;
            }
        }
        logger.log(Level.INFO, "plotted {0} stars", plotCount);

        return map;
    }

    /**
     * Extract a star's declination from a catalog entry.
     *
     * @param line of text read from the catalog (not null)
     * @return angle north of the celestial equator (in degrees, >=-90, <=90)
     */
    private float getDeclinationDegrees(String line)
            throws InvalidEntryException {
        assert line != null;
        /*
         * Extract declination components from the text.
         */
        String dd = line.substring(18, 21);
        String mm = line.substring(22, 24);
        String ss = line.substring(25, 30);
        logger.log(Level.FINE, "{0}d {1}m {2}s", new Object[]{dd, mm, ss});
        /*
         * sanity checks
         */
        int degrees = Integer.valueOf(dd);
        if (degrees < -maxDeclination || degrees > maxDeclination) {
            throw new InvalidEntryException(
                    "dec degrees must be between -90 and 90, inclusive");
        }
        int minutes = Integer.valueOf(mm);
        if (minutes < 0 || minutes >= maxMinutes) {
            throw new InvalidEntryException(
                    "dec minutes must be between 0 and 59, inclusive");
        }
        float seconds = Float.valueOf(ss);
        if (seconds < 0f || seconds >= maxSeconds) {
            throw new InvalidEntryException(
                    "dec seconds must be between 0 and 59, inclusive");
        }
        /*
         * Convert to an angle.
         */
        float result; // in degrees
        if (degrees > 0) {
            result = degrees + minutes / 60f + seconds / 3600f;
        } else {
            result = degrees - minutes / 60f - seconds / 3600f;
        }

        assert result >= -maxDeclination : result;
        assert result <= maxDeclination : result;
        logger.log(Level.FINE, "result = {0}", result);
        return result;
    }

    /**
     * Extract a star's right ascension from a catalog entry.
     *
     * @param line of text read from the catalog (not null)
     * @return angle east of the vernal equinox (in hours, >=0, <24)
     */
    private float getRightAscensionHours(String line)
            throws InvalidEntryException {
        assert line != null;
        /*
         * Extract right ascension components from the line of text.
         */
        String hh = line.substring(6, 8);
        String mm = line.substring(9, 11);
        String ss = line.substring(12, 17);
        logger.log(Level.FINE, "{0}:{1}:{2}", new Object[]{hh, mm, ss});
        /*
         * sanity checks
         */
        int hours = Integer.valueOf(hh);
        if (hours < 0 || hours >= SkyControl.hoursPerDay) {
            throw new InvalidEntryException(
                    "RA hours must be between 0 and 23, inclusive");
        }
        int minutes = Integer.valueOf(mm);
        if (minutes < 0 || minutes >= maxMinutes) {
            throw new InvalidEntryException(
                    "RA minutes must be between 0 and 59, inclusive");
        }
        float seconds = Float.valueOf(ss);
        if (seconds < 0f || seconds >= maxSeconds) {
            throw new InvalidEntryException(
                    "RA seconds must be between 0 and 59, inclusive");
        }
        /*
         * Convert to an angle.
         */
        float result = hours + minutes / 60f + seconds / 3600f; // in hours

        assert result >= 0f : result;
        assert result < SkyControl.hoursPerDay : result;
        logger.log(Level.FINE, "result = {0}", result);
        return result;
    }

    /**
     * Plot a star's position at a particular time onto a texture map.
     *
     * @param map which texture map (not null)
     * @param star which star to plot (not null)
     * @param latitude radians north of the equator (>=-Pi/2, <=Pi/2)
     *
     * @param siderealTime radians since sidereal midnight (>=0, <2*Pi)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStar(BufferedImage map, Star star, float latitude,
            float siderealTime) {
        assert map != null;
        assert star != null;
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;

        Vector3f equatorial = star.getEquatorialLocation(siderealTime);
        /*
         * Convert equatorial coordinates to world coordinates, where:
         *   +X points to the north horizon
         *   +Y points to the zenith
         *   +Z points to the east horizon
         *
         * The conversion consists of a (latitude - Pi/2) rotation about the Y
         * (east) axis followed by permutation of the axes.
         */
        float coLatitude = FastMath.HALF_PI - latitude;
        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(-coLatitude, Vector3f.UNIT_Y);
        Vector3f rotated = rotation.mult(equatorial);
        assert rotated.isUnitVector() : rotated;
        if (rotated.z < 0f) {
            /*
             * The star lies below the horizon, so skip it.
             */
            return false;
        }
        Vector3f world = new Vector3f(-rotated.x, rotated.z, rotated.y);

        float apparentMagnitude = star.getApparentMagnitude();
        boolean success = plotStar(map, apparentMagnitude, world);

        return success;
    }

    /**
     * Plot a star on a texture map.
     *
     * @param map which texture map (not null)
     * @param apparentMagnitude the star's brightness
     * @param worldDirection the star's world coordinates (unit vector)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStar(BufferedImage map, float apparentMagnitude,
            Vector3f worldDirection) {
        assert map != null;
        assert worldDirection != null;
        assert worldDirection.isUnitVector() : worldDirection;
        /*
         * Convert apparent magnitude to relative luminosity.
         */
        float luminosity = luminosity0
                * FastMath.pow(pogsonsRatio, -apparentMagnitude);
        if (luminosity < luminosityCutoff) {
            return false;
        }
        /*
         * Convert world direction to texture coordinates.
         */
        Vector2f uv = mesh.directionUV(worldDirection);

        boolean success = plotStar(map, luminosity, uv);
        return success;
    }

    /**
     * Plot a star on a texture map.
     *
     * @param map which texture map (not null)
     * @param luminosity the star's relative luminosity (>0, <=37)
     * @param uv the star's texture coordinates (not null)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStar(BufferedImage map, float luminosity, Vector2f uv) {
        assert luminosity > 0f : luminosity;
        assert luminosity <= 37f : luminosity;
        assert uv != null;
        /*
         * Convert the star's luminosity into a shape and pixel color.
         *
         * The shape must be big enough that the pixels will not be
         * oversaturated. For instance, a star with luminosity=4.1
         * must fill at least 5 pixels.
         */
        int minPixels = (int) FastMath.ceil(luminosity);
        assert minPixels >= 1 : minPixels;
        /*
         * Star shapes consist of a square portion (up to 5x5 pixels)
         * plus optional rays.  Rays are used only with odd-sized squares;
         * they add either 1 or 3 pixels to each side of the square.
         * In other words, they add either 4 or 12 pixels.
         */
        int raySize, squareSize;
        if (minPixels == 1) {
            raySize = 0;
            squareSize = 1;
        } else if (minPixels <= 4) {
            raySize = 0;
            squareSize = 2;
        } else if (minPixels <= 5) {
            raySize = 1;
            squareSize = 1;
        } else if (minPixels <= 9) {
            raySize = 0;
            squareSize = 3;
        } else if (minPixels <= 13) {
            raySize = 1;
            squareSize = 3;
        } else if (minPixels <= 16) {
            raySize = 0;
            squareSize = 4;
        } else if (minPixels <= 21) {
            raySize = 3;
            squareSize = 3;
        } else if (minPixels <= 29) {
            raySize = 1;
            squareSize = 5;
        } else if (minPixels <= 37) {
            raySize = 3;
            squareSize = 5;
        } else {
            logger.log(Level.SEVERE, "no shape contains {0} pixels", minPixels);
            return false;
        }
        int numPixels = squareSize * squareSize + 4 * raySize;
        assert numPixels >= minPixels : minPixels;
        int brightness = Math.round(255f * luminosity / numPixels);
        assert brightness >= 0 : brightness;
        assert brightness <= 255 : brightness;
        // TODO apply tint based on spectral type
        Color color = new Color(brightness, brightness, brightness);
        /*
         * Convert the texture coordinates into (x, y) image coordinates of
         * the square's upper-left pixel.
         */
        float u = uv.x;
        float v = uv.y;
        assert u >= DomeMesh.uvMin : u;
        assert u <= DomeMesh.uvMax : u;
        assert v >= DomeMesh.uvMin : v;
        assert v <= DomeMesh.uvMax : v;
        float cornerOffset = 0.5f * (squareSize - 1);
        int x = Math.round(u * textureSize - cornerOffset);
        int y = Math.round(v * textureSize - cornerOffset);
        /*
         * Plot the star onto the texture map.
         */
        Graphics2D graphics = map.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(x, y, squareSize, squareSize);
        if (raySize == 0) {
            return true;
        }

        assert MyMath.isOdd(squareSize) : squareSize;
        int halfSize = (squareSize - 1) / 2;
        switch (raySize) {
            case 1:
                graphics.fillRect(x - 1, y + halfSize, 1, 1);
                graphics.fillRect(x + halfSize, y - 1, 1, 1);
                graphics.fillRect(x + halfSize, y + squareSize, 1, 1);
                graphics.fillRect(x + squareSize, y + halfSize, 1, 1);
                break;

            case 3:
                graphics.fillRect(x - 1, y + halfSize - 1, 1, 3);
                graphics.fillRect(x + halfSize - 1, y - 1, 3, 1);
                graphics.fillRect(x + halfSize - 1, y + squareSize, 3, 1);
                graphics.fillRect(x + squareSize, y + halfSize - 1, 1, 3);
                break;

            default:
                assert false : raySize;
        }
        return true;
    }

    /**
     * Read the star catalog and add valid stars to the collection.
     */
    private void readCatalog() {
        /*
         * Open the catalog file.
         */
        File catalogFile = new File(catalogFilePath);
        BufferedReader bufferedReader;
        try {
            FileReader fileReader = new FileReader(catalogFile);
            bufferedReader = new BufferedReader(fileReader);
        } catch (FileNotFoundException exception) {
            logger.log(Level.SEVERE, "open of {0} failed",
                    MyString.quote(catalogFilePath));
            return;
        }
        /*
         * Read the catalog line by line and use the data therein
         * to build up the collection of stars.
         */
        int duplicateEntries = 0;
        int nextEntry = 1;
        int missedEntries = 0;
        int readEntries = 0;
        int skippedEntries = 0;
        for (;;) {
            String textLine = null;
            try {
                textLine = bufferedReader.readLine();
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "read of {0} failed", catalogFilePath);
            }
            if (textLine == null) {
                break;
            }
            /*
             * If the line does not resemble a catalog entry,
             * then silently ignore it.
             */
            if (textLine.length() < 5) {
                continue;
            }
            String actualPrefix = textLine.substring(0, 5);
            if (!actualPrefix.matches("[ ]*[0-9]+")) {
                continue;
            }
            readEntries++;
            /*
             * Cope with missing/duplicate entry ids.
             */
            int actualEntry = Integer.valueOf(actualPrefix.trim());
            if (actualEntry > nextEntry) {
                logger.log(Level.FINE, "missed entries #{0} through #{1}",
                        new Object[]{nextEntry, actualEntry - 1});
                nextEntry = actualEntry;
                missedEntries += actualEntry - nextEntry;

            } else if (actualEntry < nextEntry) {
                logger.log(Level.WARNING,
                        "skipped entry due to duplicate id #{0}",
                        actualEntry);
                skippedEntries++;
                continue;
            }

            assert actualEntry == nextEntry : nextEntry;
            Star star = null;
            try {
                star = readStar(textLine, nextEntry);

            } catch (InvalidEntryException exception) {
                return;

            } catch (InvalidMagnitudeException exception) {
                logger.log(Level.FINE,
                        "skipped entry #{0} due to invalid magnitude",
                        nextEntry);
                skippedEntries++;
            }
            if (star != null) {
                if (stars.contains(star)) {
                    logger.log(Level.FINE, "entry #{0} is a duplicate",
                            nextEntry);
                    duplicateEntries++;
                } else {
                    boolean success = stars.add(star);
                    assert success : nextEntry;
                }
            }
            nextEntry++;
        }
        try {
            bufferedReader.close();
        } catch (IOException exception) {
            logger.log(Level.WARNING, "close of {0} failed", catalogFilePath);
        }
        /*
         * Verify that the entire catalog was read.
         */
        int lastEntryRead = nextEntry - 1;
        if (lastEntryRead != lastEntryExpected) {
            logger.log(Level.WARNING,
                    "expected last entry to be #{0} but it was actually #{1}",
                    new Object[]{lastEntryExpected, lastEntryRead});
        }
        /*
         * Log statistics.
         */
        if (missedEntries > 0) {
            logger.log(Level.WARNING, "missed {0} entries", missedEntries);
        }
        logger.log(Level.INFO, "read {0} catalog entries from {1}",
                new Object[]{readEntries, catalogFilePath});
        if (duplicateEntries > 0) {
            logger.log(Level.WARNING, "{0} duplicate entries", duplicateEntries);
        }
        if (skippedEntries > 0) {
            logger.log(Level.WARNING, "{0} entries skipped", skippedEntries);
        }
        logger.log(Level.INFO, "collected {0} stars", stars.size());
    }

    /**
     * Construct a new star based on a catalog entry.
     *
     * @param textLine which was read from the catalog (not null)
     * @param entryId (>=1)
     * @return a new instance
     */
    private Star readStar(String textLine, int entryId)
            throws InvalidEntryException, InvalidMagnitudeException {
        assert textLine != null;
        assert entryId >= 1 : entryId;
        /*
         * Extract the apparent magnitude from the text.
         */
        String magnitudeText = textLine.substring(56, 60);
        logger.log(Level.FINE, "mag={0}", magnitudeText);
        /*
         * sanity checks on the magnitude
         */
        if (magnitudeText.equals("????")) {
            throw new InvalidMagnitudeException();
        }
        float apparentMagnitude;
        try {
            apparentMagnitude = Float.valueOf(magnitudeText);
        } catch (NumberFormatException exception) {
            logger.log(Level.WARNING,
                    "entry #{0} has invalid magnitude {1}",
                    new Object[]{entryId, MyString.quote(magnitudeText)});
            throw new InvalidMagnitudeException();
        }
        if (apparentMagnitude < minMagnitude
                || apparentMagnitude > maxMagnitude) {
            throw new InvalidEntryException("magnitude is out of range");
        }
        /*
         * Get the star's equatorial coordinates and convert them to radians.
         */
        float declination =
                getDeclinationDegrees(textLine) * FastMath.DEG_TO_RAD;
        float rightAscension =
                getRightAscensionHours(textLine) * radiansPerHour;
        /*
         * Instantiate the star.
         */
        Star result = new Star(entryId, rightAscension, declination,
                apparentMagnitude);
        return result;
    }
}
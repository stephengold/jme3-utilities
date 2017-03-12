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
import jme3utilities.MyAsset;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.sky.Constants;
import jme3utilities.sky.DomeMesh;

/**
 * Console application to generate starry sky texture maps for use with
 * SkyMaterial and DomeMesh, based on data from a star catalog. In the resulting
 * textures, east is at the top and north is to the right.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MakeStarMaps {
    // *************************************************************************
    // throwables

    /**
     * exception to indicate unexpected invalid data in a catalog entry
     */
    static private class InvalidEntryException extends Exception {
        static final long serialVersionUID = 1L;

        InvalidEntryException(String message) {
            super(message);
        }
    }

    /**
     * exception to indicate an invalid apparent magnitude in a catalog entry:
     * such entries can be ignored
     */
    static private class InvalidMagnitudeException extends Exception {
        static final long serialVersionUID = 1L;
    }
    // *************************************************************************
    // constants and loggers

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
    final private static float minMagnitude = -1.47f;
    /**
     * luminosity ratio between successive stellar magnitudes (5th root of 100)
     */
    final private static float pogsonsRatio = FastMath.pow(100f, 0.2f);
    /**
     * Earth's rate of rotation (radians per sidereal hour)
     */
    final private static float radiansPerHour = 
            FastMath.TWO_PI / Constants.hoursPerDay;
    /**
     * expected id of the last entry in the catalog
     */
    final private static int lastEntryExpected = 9_110;
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
     * number of points per ellipse
     */
    final private static int ellipseNumPoints = 32;
    /**
     * x-coordinates used to draw an ellipse
     */
    final private static int[] ellipseXs = new int[ellipseNumPoints];
    /**
     * y-coordinates used to draw an ellipse
     */
    final private static int[] ellipseYs = new int[ellipseNumPoints];
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MakeStarMaps.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeStarMaps";
    /**
     * filesystem path to the input file, an ASCII version of version 5 of the
     * Yale Bright Star Catalog, which may be downloaded from
     * http://tdc-www.harvard.edu/catalogs/bsc5.html
     */
    final private static String catalogFilePath =
            "assets/Textures/skies/bsc5.dat";
    /**
     * English names for the faces of a cube, in the order expected by
     * {@link jme3utilities.MyAsset#createStarMap(com.jme3.asset.AssetManager, java.lang.String)}
     */
    final private static String[] faceName = {
        "right", "left", "top", "bottom", "front", "back"
    };
    /**
     * filesystem path to the output directory/folder
     */
    final private static String outputDirPath = 
            "assets/Textures/skies/star-maps";
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
     * true &rarr; generate textures for a cube; false &rarr; for a dome
     */
    @Parameter(names = {"-c", "--cube"}, description = "generate for a cube")
    private static boolean forCube = false;
    /**
     * stars read from the catalog
     */
    final private Collection<Star> stars = new TreeSet<>();
    /**
     * sample dome mesh for calculating texture coordinates
     */
    final private DomeMesh domeMesh = new DomeMesh(3, 2);
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
        logger.setLevel(Level.INFO);
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
        /*
         * Log the jME3-utilities version string and working directory.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));
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
     * Calculate the texture coordinates of a point that lies in the specified
     * direction from the center of the cube.
     *
     * @param direction (length&gt;0, unaffected)
     * @param faceIndex (&ge;0, &lt;6) which face of the cube
     * @return a new vector, or null if direction is too far outside the face
     */
    private Vector2f cubeUV(Vector3f direction, int faceIndex) {
        assert direction != null;
        assert !MyVector3f.isZero(direction);
        assert faceIndex >= 0 : faceIndex;
        assert faceIndex < 6 : faceIndex;

        Vector3f faceDir = MyAsset.copyFaceDirection(faceIndex);
        Vector3f norm = direction.normalize();
        float dot = faceDir.dot(norm);
        if (dot < 0.5f) {
            /*
             * way outside the face
             */
            return null;
        }
        /*
         * project outward to the plane of the face
         */
        norm.divideLocal(dot);
        /*
         * convert to texture coordinates
         */
        Vector3f uDir = MyAsset.copyUDirection(faceIndex);
        Vector3f vDir = MyAsset.copyVDirection(faceIndex);
        float u = 0.5f * (1f + uDir.dot(norm));
        float v = 0.5f * (1f + vDir.dot(norm));
        Vector2f uv = new Vector2f(u, v);

        return uv;
    }

    /**
     * Extract a star's declination from a catalog entry.
     *
     * @param line of text read from the catalog (not null)
     * @return angle north of the celestial equator (in degrees, &le;90,
     * &ge;-90)
     */
    private float declination(String line)
            throws InvalidEntryException {
        assert line != null;
        /*
         * Extract declination components from the line of text.
         */
        String dd = line.substring(83, 86);
        String mm = line.substring(86, 88);
        String ss = line.substring(88, 90);
        logger.log(Level.FINE, "{0}d {1}m {2}s", new Object[]{dd, mm, ss});
        /*
         * sanity checks
         */
        int degrees = Integer.parseInt(dd);
        if (degrees < -maxDeclination || degrees > maxDeclination) {
            throw new InvalidEntryException(
                    "dec degrees should be between -90 and 90, inclusive");
        }
        int minutes = Integer.parseInt(mm);
        if (minutes < 0 || minutes >= maxMinutes) {
            throw new InvalidEntryException(
                    "dec minutes should be between 0 and 59, inclusive");
        }
        float seconds = Float.parseFloat(ss);
        if (seconds < 0f || seconds >= maxSeconds) {
            throw new InvalidEntryException(
                    "dec seconds should be between 0 and 59, inclusive");
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
     * Generate 6 starry sky texture maps for a cube.
     *
     * @param latitude radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     * @param siderealTime radians since sidereal midnight (&lt;2*Pi, &ge;0)
     * @param textureSize size of each texture map (pixels per side, &gt;2)
     * @return new instance
     */
    private RenderedImage[] generateCubeMap(float latitude, float siderealTime,
            int textureSize) {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;
        assert textureSize > 2 : textureSize;
        /*
         * Create a blank, grayscale buffered image for each texture map.
         */
        BufferedImage[] maps = new BufferedImage[6];
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            maps[faceIndex] = new BufferedImage(textureSize, textureSize,
                    BufferedImage.TYPE_BYTE_GRAY);
        }
        /*
         * Plot individual stars on the images, starting with the faintest.
         */
        int plotCount = 0;
        for (Star star : stars) {
            boolean success = plotStarOnCube(maps, star, latitude,
                    siderealTime, textureSize);
            if (success) {
                plotCount++;
            }
        }
        logger.log(Level.FINE, "plotted {0} stars", plotCount);

        return maps;
    }

    /**
     * Generate a starry sky texture map for a dome.
     *
     * @param latitude radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     * @param siderealTime radians since sidereal midnight (&lt;2*Pi, &ge;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @return new instance
     */
    private RenderedImage generateDomeMap(float latitude, float siderealTime,
            int textureSize) {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;
        assert textureSize > 2 : textureSize;
        /*
         * Create a blank, grayscale buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_BYTE_GRAY);
        /*
         * Plot individual stars on the image, starting with the faintest.
         */
        int plotCount = 0;
        for (Star star : stars) {
            boolean success = plotStarOnDome(map, star, latitude, siderealTime,
                    textureSize);
            if (success) {
                plotCount++;
            }
        }
        logger.log(Level.FINE, "plotted {0} stars", plotCount);

        return map;
    }

    /**
     * Generate starry sky texture map(s) for the specified preset.
     *
     * @param preset map preset to generate (not null)
     */
    private void generateMap(StarMapPreset preset) {
        assert preset != null;

        float latitude = preset.latitude();
        logger.log(Level.FINE, "latitude is {0} degrees",
                MyMath.toDegrees(latitude));

        float siderealHour = preset.hour();
        logger.log(Level.FINE, "sidereal time is {0} hours", siderealHour);

        int textureSize = preset.textureSize();
        logger.log(Level.FINE, "resolution is {0} pixels", textureSize);
        /*
         * Convert the sidereal time from hours to radians.
         */
        float siderealTime = siderealHour * radiansPerHour;

        if (forCube) {
            /*
            * Generate 6 texture maps for a cube.
             */
            RenderedImage images[] = generateCubeMap(latitude, siderealTime,
                    textureSize);
            assert images.length == 6 : images.length;
            for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
                String filePath = String.format("%s/%s/%s_%s%d.png",
                        outputDirPath, preset.textureFileName(),
                        preset.textureFileName(), faceName[faceIndex],
                        faceIndex + 1);
                try {
                    Misc.writeMap(filePath, images[faceIndex]);
                } catch (IOException exception) {
                    // ignored
                }
            }

        } else {
            /*
             * Generate a texture map for a dome.
             */
            RenderedImage image = generateDomeMap(latitude, siderealTime,
                    textureSize);
            String filePath = String.format("%s/%s.png", outputDirPath,
                    preset.textureFileName());
            try {
                Misc.writeMap(filePath, image);
            } catch (IOException exception) {
                // ignored
            }
        }
    }

    /**
     * Plot a four-pointed star shape on a texture map.
     *
     * @param map texture map (not null)
     * @param luminosity star's relative luminosity (in terms of pure white
     * pixels, &le;37, &gt;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @param uv star's texture coordinates (not null)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plot4PointStar(BufferedImage map, float luminosity,
            int textureSize, Vector2f uv) {
        assert luminosity > 0f : luminosity;
        assert luminosity <= 37f : luminosity;
        assert textureSize > 2 : textureSize;
        assert uv != null;
        /*
         * Convert the star's luminosity into a shape and pixel color.
         *
         * The shape must be big enough to ensure that the pixels will not be
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
        // TODO tint based on spectral type
        Color color = new Color(brightness, brightness, brightness);
        /*
         * Convert the texture coordinates into (x, y) image coordinates of
         * the square's upper-left pixel.
         */
        float u = uv.x;
        float v = uv.y;
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
     * Draw an ellipse -- a circle stretched to compensate for UV distortion
     * near the rim of the dome.
     *
     * @param map texture map (not null)
     * @param luminosity star's relative luminosity (in terms of pure white
     * pixels, &gt;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @param uv star's texture coordinates (not null)
     * @return true if the star was successfully plotted, otherwise false
     */
    private void plotEllipseForDome(BufferedImage map, float luminosity,
            int textureSize, Vector2f uv) {
        assert map != null;
        assert luminosity > 0f : luminosity;
        assert textureSize > 2 : textureSize;
        assert uv != null;
        float u = uv.x;
        float v = uv.y;
        assert u >= Constants.uvMin : u;
        assert u <= Constants.uvMax : u;
        assert v >= Constants.uvMin : v;
        assert v <= Constants.uvMax : v;

        Vector2f offset = uv.subtract(Constants.topUV);
        float topDist = offset.length();
        float xDir, yDir;
        if (topDist > 0f) {
            xDir = offset.x / topDist;
            yDir = offset.y / topDist;
        } else {
            xDir = 1f;
            yDir = 0f;
        }
        float stretchFactor = 1f
                + Constants.stretchCoefficient * topDist * topDist;
        float a = FastMath.sqrt(luminosity * stretchFactor / FastMath.PI);
        float b = a / stretchFactor;

        for (int i = 0; i < ellipseNumPoints; i++) {
            float theta = FastMath.TWO_PI * i / ellipseNumPoints;
            float da = a * FastMath.cos(theta);
            float db = b * FastMath.sin(theta);
            float dx = db * xDir + da * yDir;
            float dy = db * yDir - da * xDir;
            int x = Math.round(u * textureSize + dx);
            int y = Math.round(v * textureSize + dy);
            ellipseXs[i] = x;
            ellipseYs[i] = y;
        }
        Graphics2D graphics = map.createGraphics();
        graphics.setColor(Color.WHITE); // TODO tint based on spectral type
        graphics.fillPolygon(ellipseXs, ellipseYs, ellipseNumPoints);
    }

    /**
     * Draw an ellipse -- a circle stretched to compensate for UV distortion
     * near the edges of the quad.
     *
     * @param map texture map (not null)
     * @param luminosity star's relative luminosity (&gt;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @param worldDirection the star's world coordinates (length=1)
     * @param faceIndex which face of the cube (&ge;0, &lt;6)
     * @return true if the star was successfully plotted, otherwise false
     */
    private void plotEllipseForQuad(BufferedImage map, float luminosity,
            int textureSize, Vector3f worldDirection, int faceIndex) {
        assert map != null;
        assert luminosity > 0f : luminosity;
        assert textureSize > 2 : textureSize;
        assert worldDirection != null;
        assert faceIndex >= 0 : faceIndex;
        assert faceIndex < 6 : faceIndex;

        Vector3f basis1 = worldDirection.clone();
        Vector3f basis2 = new Vector3f();
        Vector3f basis3 = new Vector3f();
        MyVector3f.generateBasis(basis1, basis2, basis3);
        float numPixels = textureSize * textureSize;
        float area = luminosity / numPixels;
        float r = 1.2f * FastMath.sqrt(area);

        Vector3f p = new Vector3f();
        for (int i = 0; i < ellipseNumPoints; i++) {
            float theta = FastMath.TWO_PI * i / ellipseNumPoints;
            float rCos = r * FastMath.cos(theta);
            float rSin = r * FastMath.sin(theta);
            p.scaleAdd(rCos, basis2, basis1);
            p.scaleAdd(rSin, basis3, p);
            Vector2f uv = cubeUV(p, faceIndex);
            if (uv == null) {
                return;
            }
            int x = Math.round(uv.x * textureSize);
            int y = Math.round(uv.y * textureSize);
            ellipseXs[i] = x;
            ellipseYs[i] = y;
        }
        Graphics2D graphics = map.createGraphics();
        graphics.setColor(Color.WHITE); // TODO tint based on spectral type
        graphics.fillPolygon(ellipseXs, ellipseYs, ellipseNumPoints);
    }

    /**
     * Plot a star's position at the specified time onto a cube.
     *
     * @param maps texture maps for 6 cube faces (not null, modified)
     * @param star star to plot (not null)
     * @param latitude radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     * @param siderealTime radians since sidereal midnight (&lt;2*Pi, &ge;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStarOnCube(BufferedImage[] maps, Star star,
            float latitude, float siderealTime, int textureSize) {
        assert maps != null;
        assert maps.length == 6 : maps.length;
        assert star != null;
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;
        assert textureSize > 2 : textureSize;

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
        Vector3f world = new Vector3f(-rotated.x, rotated.z, rotated.y);

        float apparentMagnitude = star.getApparentMagnitude();
        boolean success = plotStarOnCube(maps, apparentMagnitude,
                textureSize, world);

        return success;
    }

    /**
     * Plot a star onto a texture map for a cube.
     *
     * @param maps texture maps for 6 cube faces (not null, modified)
     * @param apparentMagnitude the star's brightness (log scale)
     * @param textureSize size of the texture map (pixels per side, &lt;2)
     * @param worldDirection the star's world coordinates (length=1)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStarOnCube(BufferedImage[] maps,
            float apparentMagnitude, int textureSize, Vector3f worldDirection) {
        assert maps != null;
        assert maps.length == 6 : maps.length;
        assert worldDirection != null;
        assert worldDirection.isUnitVector() : worldDirection;
        assert textureSize > 2 : textureSize;
        /*
         * Convert apparent magnitude to relative luminosity.
         */
        float resolution = textureSize / 2_048f;
        float luminosity0 = 100f * resolution * resolution;
        float luminosity = luminosity0 * 1.5f
                * FastMath.pow(pogsonsRatio, -apparentMagnitude);
        if (luminosity < luminosityCutoff) {
            return false;
        }

        boolean success = false;
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            /*
             * Convert world direction to texture coordinates on this
             * face of the cube.
             */
            Vector2f uv = cubeUV(worldDirection, faceIndex);
            if (uv != null) {
                BufferedImage map = maps[faceIndex];

                if (luminosity <= 37f) {
                    boolean succ = plot4PointStar(map, luminosity,
                            textureSize, uv);
                    if (succ) {
                        success = true;
                    }
                } else {
                    plotEllipseForQuad(map, luminosity, textureSize,
                            worldDirection, faceIndex);
                    success = true;
                }
            }
        }
        return success;
    }

    /**
     * Plot a star's position at the specified time onto a texture map for a
     * dome.
     *
     * @param map texture map (not null, modified)
     * @param star star to plot (not null)
     * @param latitude radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     * @param siderealTime radians since sidereal midnight (&lt;2*Pi, &ge;0)
     * @param textureSize size of the texture map (pixels per side, &gt;2)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStarOnDome(BufferedImage map, Star star, float latitude,
            float siderealTime, int textureSize) {
        assert map != null;
        assert star != null;
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        assert siderealTime >= 0f : siderealTime;
        assert siderealTime < FastMath.TWO_PI : siderealTime;
        assert textureSize > 2 : textureSize;

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
        boolean success = plotStarOnDome(map, apparentMagnitude,
                textureSize, world);

        return success;
    }

    /**
     * Plot a star on a texture map for a dome.
     *
     * @param map texture map (not null)
     * @param apparentMagnitude the star's brightness
     * @param textureSize size of the texture map (pixels per side, &lt;2)
     * @param worldDirection the star's world coordinates (length=1)
     * @return true if the star was successfully plotted, otherwise false
     */
    private boolean plotStarOnDome(BufferedImage map, float apparentMagnitude,
            int textureSize, Vector3f worldDirection) {
        assert map != null;
        assert worldDirection != null;
        assert worldDirection.isUnitVector() : worldDirection;
        assert textureSize > 2 : textureSize;
        /*
         * Convert apparent magnitude to relative luminosity.
         */
        float resolution = textureSize / 2_048f;
        float luminosity0 = 37f * resolution * resolution;
        float luminosity = luminosity0
                * FastMath.pow(pogsonsRatio, -apparentMagnitude);
        if (luminosity < luminosityCutoff) {
            return false;
        }
        /*
         * Convert world direction to texture coordinates on a dome.
         */
        Vector2f uv = domeMesh.directionUV(worldDirection);

        if (luminosity <= 37f) {
            boolean success = plot4PointStar(map, luminosity, textureSize, uv);
            return success;
        }
        plotEllipseForDome(map, luminosity, textureSize, uv);
        return true;
    }

    /**
     * Read the star catalog and add each valid star to the collection.
     */
    private void readCatalog() {
        File catalogFile = new File(catalogFilePath);
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(catalogFile);
            bufferedReader = new BufferedReader(fileReader);
            readCatalog(bufferedReader);
        } catch (FileNotFoundException exception) {
            logger.log(Level.SEVERE, "unable to open {0}",
                    MyString.quote(catalogFilePath));
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "unable to read {0}",
                    MyString.quote(catalogFilePath));
        } catch (InvalidEntryException exception) {
            logger.log(Level.SEVERE, "", exception);
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException exception) {
                logger.log(Level.WARNING, "unable to close {0}",
                        MyString.quote(catalogFilePath));
            }
        }
    }

    /**
     * Read the catalog line by line and use the data therein to build up the
     * collection of stars.
     */
    private void readCatalog(BufferedReader bufferedReader)
            throws IOException, InvalidEntryException {
        assert bufferedReader != null;

        int duplicateEntries = 0;
        int nextEntry = 1;
        int missedEntries = 0;
        int readEntries = 0;
        int skippedEntries = 0;
        for (;;) {
            String textLine;
            textLine = bufferedReader.readLine();
            if (textLine == null) {
                /*
                 * Might have reached the end of the catalog file.
                 */
                break;
            }
            logger.log(Level.FINE, "{0}", textLine);
            /*
             * If the line does not resemble a catalog entry,
             * then silently ignore it.
             */
            if (textLine.length() < 5) {
                continue;
            }
            String actualPrefix = textLine.substring(0, 4);
            if (!actualPrefix.matches("[ ]*[0-9]+")) {
                continue;
            }
            readEntries++;
            /*
             * Cope with missing/duplicate entry ids.
             */
            int actualEntry = Integer.parseInt(actualPrefix.trim());
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
            logger.log(Level.WARNING, "{0} duplicate entries",
                    duplicateEntries);
        }
        if (skippedEntries > 0) {
            logger.log(Level.WARNING, "{0} entries skipped", skippedEntries);
        }
        logger.log(Level.INFO, "collected {0} stars", stars.size());
    }

    /**
     * Construct a new star based on a catalog entry.
     *
     * @param textLine line of text read from the catalog (not null)
     * @param entryId (&ge;1)
     * @return new instance
     */
    private Star readStar(String textLine, int entryId)
            throws InvalidEntryException, InvalidMagnitudeException {
        assert textLine != null;
        assert entryId >= 1 : entryId;
        /*
         * Extract the apparent magnitude field from the line of text.
         */
        if (textLine.length() < 107) {
            throw new InvalidEntryException("catalog entry is too short");
        }
        String magnitudeText = textLine.substring(102, 107);
        logger.log(Level.FINE, "mag={0}", magnitudeText);
        /*
         * sanity checks on the magnitude
         */
        if (magnitudeText.equals("     ")) {
            throw new InvalidMagnitudeException();
        }
        float apparentMagnitude;
        try {
            apparentMagnitude = Float.valueOf(magnitudeText);
        } catch (NumberFormatException exception) {
            logger.log(Level.WARNING, "entry #{0} has invalid magnitude {1}",
                    new Object[]{entryId, MyString.quote(magnitudeText)});
            throw new InvalidMagnitudeException();
        }
        if (apparentMagnitude < minMagnitude
                || apparentMagnitude > maxMagnitude) {
            logger.log(Level.WARNING, "entry #{0} has invalid magnitude {1}",
                    new Object[]{entryId, MyString.quote(magnitudeText)});
            throw new InvalidMagnitudeException();
        }
        /*
         * Compute the star's equatorial coordinates 
         * and convert them to radians.
         */
        float declinationDegrees = declination(textLine);
        float declination = MyMath.toRadians(declinationDegrees);
        float rightAscension = rightAscensionHours(textLine) * radiansPerHour;
        /*
         * Instantiate the star.
         */
        Star result = new Star(rightAscension, declination, apparentMagnitude);

        return result;
    }

    /**
     * Extract a star's right ascension from a catalog entry.
     *
     * @param line of text read from the catalog (not null)
     * @return angle east of the March equinox (in hours, &lt;24, &ge;0)
     */
    private float rightAscensionHours(String line)
            throws InvalidEntryException {
        assert line != null;
        /*
         * Extract right ascension components from the line of text.
         */
        String hh = line.substring(75, 77);
        String mm = line.substring(77, 79);
        String ss = line.substring(79, 83);
        logger.log(Level.FINE, "{0}:{1}:{2}", new Object[]{hh, mm, ss});
        /*
         * sanity checks
         */
        int hours = Integer.parseInt(hh);
        if (hours < 0 || hours >= Constants.hoursPerDay) {
            throw new InvalidEntryException(
                    "RA hours should be between 0 and 23, inclusive");
        }
        int minutes = Integer.parseInt(mm);
        if (minutes < 0 || minutes >= maxMinutes) {
            throw new InvalidEntryException(
                    "RA minutes should be between 0 and 59, inclusive");
        }
        float seconds = Float.parseFloat(ss);
        if (seconds < 0f || seconds >= maxSeconds) {
            throw new InvalidEntryException(
                    "RA seconds should be between 0 and 59, inclusive");
        }
        /*
         * Convert to an angle.
         */
        float result = hours + minutes / 60f + seconds / 3600f; // in hours

        assert result >= 0f : result;
        assert result < Constants.hoursPerDay : result;
        logger.log(Level.FINE, "result = {0}", result);
        return result;
    }
}

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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;

/**
 * A utility application to generate ramping alpha maps for use with SkyMaterial
 * and DomeMesh.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MakeRamps {
    // *************************************************************************
    // constants

    /**
     * ratio of atmosphere thickness to Earth's radius
     */
    final private static double delta = 0.02;
    /**
     * size of the texture map (pixels per side)
     */
    final private static int textureSize = 512;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MakeRamps.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeRamps";
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
     * sample dome mesh for calculating texture coordinates
     */
    private DomeMesh mesh = new DomeMesh(3, 2);
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeRamps application.
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
        MakeRamps application = new MakeRamps();
        /*
         * Parse the command-line arguments.
         */
        JCommander jCommander = new JCommander(application, arguments);
        jCommander.setProgramName(applicationName);
        if (usageOnly) {
            jCommander.usage();
            return;
        }
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));

        try {
            application.generateRamp("haze", 0f);
        } catch (IOException exception) {
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a color ramp image.
     *
     * @param fileName (not null)
     * @param flattening the oblateness (ellipticity) of the dome with the haze:
     * 0=no flattening (hemisphere), 1=maximum flattening
     */
    private void generateRamp(String fileName, float flattening)
            throws IOException {
        assert fileName != null;

        RenderedImage image = generateRamp(flattening);
        String filePath = String.format("assets/Textures/skies/ramps/%s.png",
                fileName);
        Misc.writeMap(filePath, image);
    }

    /**
     * Generate a color ramp image.
     *
     * @param flattening the oblateness (ellipticity) of the dome with the haze:
     * 0=no flattening (hemisphere), 1=maximum flattening
     * @return a new instance
     */
    private RenderedImage generateRamp(float flattening) {
        /*
         * Create a blank, grayscale buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = map.createGraphics();
        /*
         * Compute the alpha of each pixel.
         */
        for (int x = 0; x < textureSize; x++) {
            float u = ((float) x) / textureSize;
            for (int y = 0; y < textureSize; y++) {
                float v = ((float) y) / textureSize;
                float elevationAngle = mesh.elevationAngle(u, v);
                if (elevationAngle != FastMath.HALF_PI) {
                    float tan = FastMath.tan(elevationAngle);
                    tan *= 1f - flattening;
                    elevationAngle = FastMath.atan(tan);
                }
                float alpha = hazeAlpha(elevationAngle);
                int brightness = Math.round(255f * alpha);
                setPixel(graphics, x, y, brightness);
            }
        }

        return map;
    }

    /**
     * Calculate the haze opacity for a given elevation angle.
     *
     * @param elevationAngle in radians above the horizon
     * @return opacity (<=1, >0)
     */
    private float hazeAlpha(float elevationAngle) {
        if (elevationAngle < 0f) {
            return 1f;
        }
        double dMax = Math.sqrt(delta * (2.0 + delta));

        double cosElevation = Math.cos(elevationAngle);
        /*
         * Beta is the (acute, positive) angle between the observer and the
         * center of the Earth, measured at the "top" of the atmosphere.
         */
        double sinBeta = cosElevation / (1.0 + delta);
        /*
         * Gamma is the (acute, positive) angle between the observer and the
         * "top" of the atmosphere, measured at the center of the Earth.
         */
        double gamma = Math.PI / 2.0 - elevationAngle - Math.asin(sinBeta);
        /*
         * D is the (positive) path length from the observer to
         * the "top" of the atmosphere, in Earth radii.
         */
        double d = (1.0 + delta) * Math.sin(gamma) / cosElevation;

        float result = (float) (d / dMax);

        assert result > 0f : result;
        assert result <= 1f : result;
        return result;
    }

    /**
     * Set a particular pixel to a particular brightness.
     *
     * @param graphics context (not null)
     * @param x coordinate (<=textureSize, >=0)
     * @param y coordinate (<=textureSize, >=0)
     * @param brightness (<=255, >=0)
     *
     */
    private void setPixel(Graphics2D graphics, int x, int y, int brightness) {
        assert graphics != null;
        assert x >= 0 : x;
        assert y >= 0 : y;
        assert x < textureSize : x;
        assert y < textureSize : y;
        assert brightness >= 0 : brightness;
        assert brightness <= 255 : brightness;

        Color color = new Color(brightness, brightness, brightness);
        graphics.setColor(color);
        graphics.fillRect(x, y, 1, 1);
    }
}
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
 * A console application to generate ramping alpha maps for use with SkyMaterial
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
        /*
         * Log the jME3-utilities version string and working directory.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));

        try {
            application.makeRamp("haze", 0f);
        } catch (IOException exception) {
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Compute the haze opacity for a given elevation angle.
     *
     * @param elevationAngle in radians above the horizon
     * @return opacity (<=1, >0)
     */
    private float hazeAlpha(float elevationAngle) {
        if (elevationAngle < 0f) {
            return 1f;
        }

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
         * Path is the (positive) path length from the observer to
         * the "top" of the atmosphere, in Earth radii.
         */
        double path = (1.0 + delta) * Math.sin(gamma) / cosElevation;
        double pathMax = Math.sqrt(delta * (2.0 + delta));
        /*
         * Apply a Gaussian function to the path difference so that the
         * result changes very slowly near the horizon, ensuring
         * a smooth transition to the background color or the bottom dome.
         */
        double deltaPath = 8f * (pathMax - path);
        float result = (float) Math.exp(-deltaPath * deltaPath);

        assert result > 0f : result;
        assert result <= 1f : result;
        return result;
    }

    /**
     * Generate a grayscale ramp image.
     *
     * @param fileName (not null)
     * @param flattening the oblateness (ellipticity) of the dome with the haze:
     * 0=no flattening (hemisphere), 1=maximum flattening
     */
    private void makeRamp(String fileName, float flattening)
            throws IOException {
        assert fileName != null;

        RenderedImage image = makeRamp(flattening);
        String filePath = String.format("assets/Textures/skies/ramps/%s.png",
                fileName);
        Misc.writeMap(filePath, image);
    }

    /**
     * Generate a grayscale ramp image.
     *
     * @param flattening the oblateness (ellipticity) of the dome with the haze:
     * 0=no flattening (hemisphere), 1=maximum flattening
     * @return a new instance
     */
    private RenderedImage makeRamp(float flattening) {
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
     * Set a particular pixel to a particular brightness.
     *
     * @param graphics context (not null)
     * @param x coordinate (<textureSize, >=0)
     * @param y coordinate (<textureSize, >=0)
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
/*
 Copyright (c) 2014-2017, Stephen Gold
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.math.noise.Noise;
import jme3utilities.math.noise.Perlin2;

/**
 * Console application to generate cloud layer alpha maps for use with
 * SkyMaterial and DomeMesh.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MakeClouds {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MakeClouds.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeClouds";
    /**
     * filesystem path to the output directory/folder
     */
    final private static String outputDirPath =
            "../SkyControl/src/main/resources/Textures/skies/clouds";
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
     * square array of FBM noise samples
     */
    private static float[][] samples = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeClouds application.
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
        MakeClouds application = new MakeClouds();
        /*
         * Parse the command-line arguments.
         */
        JCommander jCommander = new JCommander(application);
        jCommander.parse(arguments);
        jCommander.setProgramName(applicationName);
        if (usageOnly) {
            jCommander.usage();
            return;
        }
        /*
         * Log the working directory.
         */
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));

        initializeSamples(2_048, 10);
        RenderedImage fbm = application.makeFbmClouds(2_048, 0.58f, 0.82f);

        RenderedImage clear = application.makeFillClouds(64, 0f);
        RenderedImage overcast = application.makeFillClouds(64, 1f);

        try {
            writeClouds("clear", clear);
            writeClouds("fbm", fbm);
            writeClouds("overcast", overcast);
        } catch (IOException exception) {
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the 2-D array of normalized FBM noise samples.
     *
     * @param numRows size of array (&ge;1)
     * @param fundamental base frequency for FBM (&ge;1)
     */
    private static void initializeSamples(int numRows, int fundamental) {
        assert numRows >= 1 : numRows;
        assert fundamental >= 1 : fundamental;
        /*
         * noise parameters for fractional Brownian motion (FBM)
         * and Perlin generator
         */
        long seed = -35_930_871;
        Perlin2 generator = new Perlin2(fundamental, fundamental, seed, seed);
        int numOctaves = 12;
        float gain = 0.45f;
        float lacunarity = 2f;
        /*
         * Generate FBM noise and store it in a 2-D array.
         */
        samples = new float[numRows][];
        for (int x = 0; x < numRows; x++) {
            float u = ((float) x) / numRows;
            samples[x] = new float[numRows];
            for (int y = 0; y < numRows; y++) {
                float v = ((float) y) / numRows;
                float n = Noise.fbmNoise(generator, u, v, numOctaves,
                        fundamental, gain, lacunarity);
                samples[x][y] = n;
            }
        }
        /*
         * Normalize the noise to fill the range [0, 1].
         */
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (int x = 0; x < numRows; x++) {
            for (int y = 0; y < numRows; y++) {
                float n = samples[x][y];
                max = Math.max(n, max);
                min = Math.min(n, min);
            }
        }
        assert max > min;
        float range = max - min;
        for (int x = 0; x < numRows; x++) {
            for (int y = 0; y < numRows; y++) {
                samples[x][y] = (samples[x][y] - min) / range;
            }
        }
    }

    /**
     * Generate a grayscale cloud layer from FBM noise.
     *
     * @param textureSize size of the texture map (pixels per side, &ge;1)
     * @param blackCutoff normalized noise value below which pixel is black
     * (&ge;0)
     * @param whiteCutoff normalized noise value above which pixel is white
     * (&le;1, &gt;blackCutoff)
     * @return new instance
     */
    private RenderedImage makeFbmClouds(int textureSize, float blackCutoff,
            float whiteCutoff) {
        assert textureSize >= 1 : textureSize;
        assert blackCutoff >= 0f : blackCutoff;
        assert blackCutoff < whiteCutoff;
        assert whiteCutoff <= 1f : whiteCutoff;
        /*
         * Create a blank, grayscale buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = map.createGraphics();
        /*
         * Set brightness of each pixel based on the noise array.
         */
        for (int x = 0; x < textureSize; x++) {
            for (int y = 0; y < textureSize; y++) {
                float alpha = samples[x][y];
                alpha = (alpha - blackCutoff) / (whiteCutoff - blackCutoff);
                alpha = FastMath.saturate(alpha);
                Misc.setGrayPixel(graphics, x, y, alpha, 1f);
            }
        }

        return map;
    }

    /**
     * Generate a grayscale cloud layer with constant opacity.
     *
     * @param textureSize size of the texture map (pixels per side, &ge;1)
     * @param alpha opacity (&le;1, &ge;0)
     * @return new instance
     */
    private RenderedImage makeFillClouds(int textureSize, float alpha) {
        assert textureSize >= 1 : textureSize;
        assert alpha >= 0f : alpha;
        assert alpha <= 1f : alpha;
        /*
         * Create a blank, grayscale buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = map.createGraphics();
        /*
         * Set brightness of each pixel.
         */
        for (int x = 0; x < textureSize; x++) {
            for (int y = 0; y < textureSize; y++) {
                Misc.setGrayPixel(graphics, x, y, alpha, 1f);
            }
        }

        return map;
    }

    /**
     * Write a rendered image to a PNG file in the "clouds" folder.
     *
     * @param fileName name of file to write the image to (no extension, not
     * null, not empty)
     * @param image image to write (not null)
     */
    private static void writeClouds(String fileName, RenderedImage image)
            throws IOException {
        assert fileName != null;
        assert fileName.length() > 0;
        assert image != null;

        String filePath = String.format("%s/%s.png", outputDirPath, fileName);
        Misc.writeMap(filePath, image);
    }
}

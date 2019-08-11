/*
 Copyright (c) 2017-2019, Stephen Gold
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
package jme3utilities.sky.textures;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;
import jme3utilities.sky.LunarPhase;

/**
 * Console application to generate moon images for use with SkyMaterial.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MakeMoons {
    // *************************************************************************
    // constants and loggers

    /**
     * UV radius of a moon disc
     */
    final private static float discRadius = 0.49f;
    /**
     * 1/gamma for gamma correction
     */
    final private static float inverseGamma = 0.2f;
    /**
     * size of the texture map (pixels per side)
     */
    final private static int textureSize = 128;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MakeMoons.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeMoons";
    /**
     * filesystem path to the asset directory/folder for output
     */
    final private static String assetDirPath
            = "../SkyControl/src/main/resources";
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
     * name of style
     */
    @Parameter(names = {"-p", "--phase"}, description = "specify phase")
    private static String phaseName = "all";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeMoons application.
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
        //logger.setLevel(Level.INFO);
        //Logger.getLogger(Misc.class.getName()).setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        MakeMoons application = new MakeMoons();
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
        /*
         * Generate color image maps.
         */
        if ("all".equals(phaseName)) {
            for (LunarPhase phase : LunarPhase.values()) {
                if (phase != LunarPhase.CUSTOM) {
                    application.makeMoon(phase);
                }
            }
        } else {
            LunarPhase phase = LunarPhase.fromDescription(phaseName);
            application.makeMoon(phase);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Generate an image map for a moon shape.
     *
     * @param phase (not null, not CUSTOM)
     */
    private void makeMoon(LunarPhase phase) {
        assert phase != null;
        assert phase != LunarPhase.CUSTOM;
        /*
         * Create a blank, color buffered image for the texture map.
         */
        BufferedImage image = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = image.createGraphics();
        /*
         * Calculate the direction to the light source.
         */
        float angle = phase.longitudeDifference();
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        Vector3f lightDirection = new Vector3f(sin, 0f, -cos);
        /*
         * Compute the opacity and luminance of each pixel.
         */
        Vector3f normal = new Vector3f();
        for (int x = 0; x < textureSize; x++) {
            float u = ((float) x) / textureSize;
            float du = (u - 0.5f) / discRadius;
            for (int y = 0; y < textureSize; y++) {
                float v = ((float) y) / textureSize;
                float dv = (v - 0.5f) / discRadius;
                /*
                 * Convert Cartesian texture coordinates to polar coordinates.
                 */
                double uvRadiusSquared = MyMath.sumOfSquares(dv, du);

                float opacity, brightness;
                if (uvRadiusSquared > 1.0) {
                    opacity = 0f;
                    brightness = 0f;
                } else {
                    opacity = 1f;
                    float dw = (float) Math.sqrt(1.0 - uvRadiusSquared);
                    normal.set(du, dv, dw);
                    float dot = lightDirection.dot(normal);
                    brightness = FastMath.saturate(dot);
                    brightness = FastMath.pow(brightness, inverseGamma);
                }

                Misc.setGrayPixel(graphics, x, y, brightness, opacity);
            }
        }
        /*
         * Write the image to the asset file.
         */
        String assetPath = phase.imagePath("-nonviral");
        String filePath = String.format("%s/%s", assetDirPath, assetPath);
        try {
            Misc.writeMap(filePath, image);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}

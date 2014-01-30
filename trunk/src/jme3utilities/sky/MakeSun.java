/*
 Copyright (c) 2014, Stephen Gold
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;

/**
 * A utility application to generate sun images for use with SkyMaterial.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MakeSun {
    // *************************************************************************
    // constants

    /**
     * size of the texture map (pixels per side)
     */
    final private static int textureSize = 512;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MakeSun.class.getName());
    /**
     * application name for the usage message
     */
    final private static String applicationName = "MakeSun";
    // *************************************************************************
    // fields
    /**
     * true means just display the usage message; false means run the
     * application
     */
    @Parameter(names = {"-h", "-u", "--help", "--usage"}, help = true,
            description = "display this usage message")
    private static boolean usageOnly = false;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeSun application.
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
        MakeSun application = new MakeSun();
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
            application.makeSun("disc", 0.125f, 60f);
        } catch (IOException exception) {
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a color image map.
     *
     * @param fileName (not null)
     * @param discRadius UV radius of the disc (<=0.5, >0)
     * @param sharpness of the disc's edge (>0)
     */
    private void makeSun(String fileName, float discRadius, float sharpness)
            throws IOException {
        assert fileName != null;
        assert discRadius > 0f : discRadius;
        assert discRadius <= 0.5f : discRadius;
        assert sharpness > 0f : sharpness;

        RenderedImage image = makeSun(discRadius, sharpness);
        String filePath = String.format("assets/Textures/skies/sun/%s.png",
                fileName);
        Misc.writeMap(filePath, image);
    }

    /**
     * Generate a color image map.
     *
     * @param discRadius UV radius of the disc (<=0.5, >0)
     * @param sharpness of the disc's edge (>0)
     * @return a new instance
     */
    private RenderedImage makeSun(float discRadius, float sharpness) {
        assert discRadius > 0f : discRadius;
        assert discRadius <= 0.5f : discRadius;
        assert sharpness > 0f : sharpness;
        /*
         * Create a blank, color buffered image for the texture map.
         */
        BufferedImage map = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = map.createGraphics();
        /*
         * Compute the alpha of each pixel.
         */
        for (int x = 0; x < textureSize; x++) {
            float u = ((float) x) / textureSize;
            for (int y = 0; y < textureSize; y++) {
                float v = ((float) y) / textureSize;
                float r = MyMath.hypotenuse(u - 0.5f, v - 0.5f);
                float alpha = sharpness * (discRadius - r);
                alpha = MyMath.clampFraction(alpha);
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
     * @param x coordinate (<=textureSize, >=0)
     * @param y coordinate (<=textureSize, >=0)
     * @param alpha (<=255, >=0)
     */
    private void setPixel(Graphics2D graphics, int x, int y, int alpha) {
        assert graphics != null;
        assert x >= 0 : x;
        assert y >= 0 : y;
        assert x < textureSize : x;
        assert y < textureSize : y;
        assert alpha >= 0 : alpha;
        assert alpha <= 255 : alpha;

        Color color = new Color(255, 255, 255, alpha);
        graphics.setColor(color);
        graphics.fillRect(x, y, 1, 1);
    }
}
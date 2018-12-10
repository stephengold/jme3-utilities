/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.debug.textures;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;

/**
 * Console application to generate the texture "ring.png".
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MakeRing {
    // *************************************************************************
    // constants and loggers

    /**
     * number of samples per arc
     */
    final private static int samplesPerArc = 32;
    /**
     * size of the texture map (pixels per side)
     */
    final private static int textureSize = 64;
    /**
     * outer radius of the ring (in pixels)
     */
    final private static double rOuter = 0.5 * textureSize;
    /**
     * inner radius of the ring (in pixels)
     */
    final private static double rInner = 0.625 * rOuter;
    /**
     * X-coordinate of the center of the ring
     */
    final private static double xCenter = rOuter;
    /**
     * Y-coordinate of the center of the ring
     */
    final private static double yCenter = rOuter;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MakeRing.class.getName());
    /**
     * filesystem path to the asset directory/folder for output
     */
    final private static String assetDirPath
            = "../debug/src/main/resources";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeRing application.
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
        Logger.getLogger(Misc.class.getName()).setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        MakeRing application = new MakeRing();
        /*
         * Log the working directory.
         */
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));
        /*
         * Generate color image map.
         */
        application.makeRing();
    }
    // *************************************************************************
    // private methods

    /**
     * Generate an image map for a ring.
     */
    private void makeRing() {
        /*
         * Create a blank, color buffered image for the texture map.
         */
        BufferedImage image = new BufferedImage(textureSize, textureSize,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = image.createGraphics();

        float brightness = 1f;
        float opacity = 1f;
        Color white = new Color(brightness, brightness, brightness, opacity);
        graphics.setColor(white);

        fillSection(graphics, 0.0, Math.PI);
        fillSection(graphics, Math.PI, 2.0 * Math.PI);
        /*
         * Write the image to the asset file.
         */
        String assetPath = "Textures/shapes/ring.png";
        String filePath = String.format("%s/%s", assetDirPath, assetPath);
        try {
            Misc.writeMap(filePath, image);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Fill a section of the ring.
     *
     * @param graphics the graphics context on which to draw (not null)
     * @param startTheta starting angle, measured CCW from the +Y axis
     * @param endTheta ending angle, measured CCW from the +Y axis
     */
    private void fillSection(Graphics2D graphics, double startTheta,
            double endTheta) {
        double thetaStep = (endTheta - startTheta) / (samplesPerArc - 1);
        int numPoints = 2 * samplesPerArc;
        int[] xPoints = new int[numPoints];
        int[] yPoints = new int[numPoints];

        for (int i = 0; i < samplesPerArc; i++) {
            double theta = startTheta + i * thetaStep;
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);

            double xx = xCenter + rOuter * sin;
            double yy = yCenter + rOuter * cos;
            xPoints[i] = (int) Math.round(xx);
            yPoints[i] = (int) Math.round(yy);

            int j = numPoints - i - 1;
            xx = xCenter + rInner * sin;
            yy = yCenter + rInner * cos;
            xPoints[j] = (int) Math.round(xx);
            yPoints[j] = (int) Math.round(yy);
        }

        graphics.fillPolygon(xPoints, yPoints, numPoints);
    }
}

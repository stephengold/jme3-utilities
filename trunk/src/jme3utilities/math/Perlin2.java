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
package jme3utilities.math;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A 2-dimensional Perlin noise generator.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Perlin2
        implements Noise2 {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Perlin2.class.getName());
    // *************************************************************************
    // fields
    /**
     * permutation for hashing: set by constructor
     */
    final private Permutation permutation;
    /**
     * array of 2-D gradients (must all be unit vectors): set by constructor
     */
    private Vector2f gradients[] = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a generator with specified parameters.
     *
     * @param period coordinate value at which the function repeats itself
     * (&ge;numGradients)
     * @param numGradients number of distinct gradients (&ge;2)
     * @param gSeed seed for generating gradients
     * @param pSeed seed for generating the permutation
     */
    public Perlin2(int period, int numGradients, long gSeed, long pSeed) {
        if (period < numGradients) {
            logger.log(Level.SEVERE, "period={0}, numGradients={1}",
                    new Object[]{period, numGradients});
            String message =
                    String.format("period shouldn't be less than numGradients");
            throw new IllegalArgumentException(message);
        }
        if (numGradients < 2) {
            logger.log(Level.SEVERE, "numGradients={0}", numGradients);
            String message =
                    String.format("numGradients should be greater than 1");
            throw new IllegalArgumentException(message);
        }

        generateGradients(numGradients, gSeed);
        permutation = new Permutation(period, pSeed);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Sample the noise function at a specified point.
     *
     * @param sampleX first coordinate of the sample point
     * @param sampleY second coordinate of the sample point
     * @return normalized noise value (&lt;1, &gt;-1)
     */
    @Override
    public float sample(float sampleX, float sampleY) {
        /*
         * Determine which square contains the point.
         */
        int squareX = (int) Math.floor(sampleX);
        int squareY = (int) Math.floor(sampleY);
        /*
         * Calculate the noise contribution of each corner.
         */
        float n00 = gradient(squareX, squareY, sampleX, sampleY);
        float n01 = gradient(squareX, squareY + 1, sampleX, sampleY);
        float n10 = gradient(squareX + 1, squareY, sampleX, sampleY);
        float n11 = gradient(squareX + 1, squareY + 1, sampleX, sampleY);
        /*
         * 2-D interpolation between the four corners of the square.
         */
        float fadeX = MyMath.fade(sampleX - squareX);
        float nx0 = FastMath.interpolateLinear(fadeX, n00, n10);
        float nx1 = FastMath.interpolateLinear(fadeX, n01, n11);

        float fadeY = MyMath.fade(sampleY - squareY);
        float noise = FastMath.interpolateLinear(fadeY, nx0, nx1);

        return noise;
    }

    /**
     * Sample the noise function at a specified point and normalize it to the
     * range [-1, 1].
     *
     * @param sampleX first coordinate of the sample point
     * @param sampleY second coordinate of the sample point
     * @return noise value
     */
    @Override
    public float sampleNormalized(float sampleX, float sampleY) {
        float noise = sample(sampleX, sampleY);
        /*
         * Scale to fill the range [-1, 1].
         */
        noise /= MyMath.rootHalf;

        assert noise >= -1f : noise;
        assert noise <= 1f : noise;
        return noise;
    }
    // *************************************************************************
    // private methods

    /**
     * Generate the array of 2-D gradients for a given seed.
     *
     * @param numGradients (&gt;1)
     * @param gSeed seed for generating gradients
     */
    private void generateGradients(int numGradients, long seed) {
        if (numGradients <= 1) {
            logger.log(Level.SEVERE, "length={0}", numGradients);
            throw new IllegalArgumentException(
                    "length should be greater than 1");
        }

        gradients = new Vector2f[numGradients];
        Random thetaGenerator = new Random(seed);

        for (int index = 0; index < numGradients; index++) {
            float theta = thetaGenerator.nextFloat() * FastMath.TWO_PI;
            float x = FastMath.cos(theta);
            float y = FastMath.sin(theta);
            gradients[index] = new Vector2f(x, y);
        }
    }

    /**
     * Compute the contribution of a specific grid point to a sample.
     *
     * @param gridX 1st coordinate of the grid point
     * @param gridY 2nd coordinate of the grid point
     * @param sampleX 1st coordinate of the sample point
     * @param sampleY 2nd coordinate of the sample point
     */
    private float gradient(int gridX, int gridY, double sampleX,
            double sampleY) {
        /*
         * Compute a hashed index into the array of gradients.
         */
        int index = permutation.permute(gridX + permutation.permute(gridY));
        index = MyMath.modulo(index, gradients.length);
        /*
         * Dot the gradient at the grid point with the sample's offset.
         */
        Vector2f gradient = gradients[index];
        float offsetX = (float) (sampleX - gridX);
        float offsetY = (float) (sampleY - gridY);
        float result = gradient.x * offsetX + gradient.y * offsetY;

        return result;
    }
}
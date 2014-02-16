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
 * Random and noise utility methods. Aside from test cases, all methods should
 * be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Noise {
    // *************************************************************************
    // constants

    /**
     * the square root of 1/2
     */
    final private static float rootHalf = FastMath.sqrt(0.5f);
    /**
     * default length for the permutation
     */
    final private static int defaultLength = 256;
    /**
     * default seed for generating the permutation
     */
    final private static long defaultSeed = -35930871;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Noise.class.getName());
    /**
     * shared pseudo-random generator
     */
    final private static Random generator = new Random();
    // *************************************************************************
    // fields
    /**
     * permutation for generating Perlin noise
     */
    private static int[] permutation = null;
    /**
     * array of 2-D gradients for generating Perlin noise (must all be unit
     * vectors)
     */
    private static Vector2f gradients[] = null;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Noise() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Sample fractional Brownian motion (FBM) noise in two dimensions.
     *
     * @param x first coordinate of the point
     * @param y second coordinate of the point
     * @param numOctaves number of passes (&gt;0)
     * @param fundamental frequency for the first pass (&gt;0)
     * @param gain factor to increase amplitude after each pass (&gt;0, &lt;1)
     * @param lacunarity factor to increase frequency after each pass (&gt;1)
     * @return noise value (range depends on parameters)
     */
    public static float fbmNoise(float sampleX, float sampleY, int numOctaves,
            float fundamental, float gain, float lacunarity) {
        if (numOctaves <= 0) {
            logger.log(Level.SEVERE, "numOctaves={0}", numOctaves);
            throw new IllegalArgumentException(
                    "numOctaves should be greater than 0");
        }
        if (!(fundamental > 0f)) {
            logger.log(Level.SEVERE, "fundamental={0}", gain);
            throw new IllegalArgumentException(
                    "fundamental should be positive");
        }
        if (!(gain > 0f && gain < 1f)) {
            logger.log(Level.SEVERE, "gain={0}", gain);
            throw new IllegalArgumentException(
                    "gain should be between 0 and 1");
        }
        if (!(lacunarity > 1f)) {
            logger.log(Level.SEVERE, "lacunarity={0}", lacunarity);
            throw new IllegalArgumentException(
                    "lacunarity should be greater than 1");
        }

        float amplitude = 1f;
        float frequency = fundamental;
        float total = 0f;
        for (int i = 0; i < numOctaves; i++) {
            float p = perlinNoise(sampleX * frequency, sampleY * frequency);
            total += amplitude * p;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return total;
    }

    /**
     * Generate the default gradients for a specific length.
     *
     * @param length (&gt;1)
     */
    public static void generateGradients(int length) {
        if (length <= 1) {
            logger.log(Level.SEVERE, "length={0}", length);
            throw new IllegalArgumentException(
                    "length should be greater than 1");
        }
        gradients = new Vector2f[length];
        Random thetaGenerator = new Random(defaultSeed);
        for (int i = 0; i < length; i++) {
            float theta = thetaGenerator.nextFloat() * FastMath.TWO_PI;
            float x = FastMath.cos(theta);
            float y = FastMath.sin(theta);
            gradients[i] = new Vector2f(x, y);
        }
    }

    /**
     * Generate the default permutation of a specific length.
     *
     * @param length (&gt;1)
     */
    public static void generatePermutation(int length) {
        if (length <= 1) {
            logger.log(Level.SEVERE, "length={0}", length);
            throw new IllegalArgumentException(
                    "length should be greater than 1");
        }
        generatePermutation(length, defaultSeed);
    }

    /**
     * Generate a permutation with a specific length and seed.
     *
     * @param length (&gt;1)
     * @param seed for the pseudo-random generator
     */
    public static void generatePermutation(int length, long seed) {
        if (length <= 1) {
            logger.log(Level.SEVERE, "length={0}", length);
            throw new IllegalArgumentException(
                    "length should be greater than 1");
        }
        /*
         * Initialize the permutation to identity.
         */
        permutation = new int[length];
        for (int i = 0; i < length; i++) {
            permutation[i] = i;
        }
        /*
         * Shuffle the permutation by performing one pass of
         * pseudo-random swaps.
         */
        Random swapGenerator = new Random(seed);
        for (int sequential = 0; sequential < length; sequential++) {
            int nextInt = swapGenerator.nextInt();
            int random = MyMath.modulo(nextInt, length);
            swapTableEntries(sequential, random);
        }
    }

    /**
     * Compute the contribution of a specific grid point to a Perlin noise
     * sample.
     *
     * @param gridX first coordinate of grid point
     * @param gridY second coordinate of grid point
     * @param x first coordinate of sample point
     * @param y second coordinate of sample point
     */
    public static float gradient(int gridX, int gridY, double x, double y) {
        if (gradients == null) {
            generateGradients(23);
        }
        int index = permute(gridX + permute(gridY));
        index = MyMath.modulo(index, gradients.length);
        Vector2f gradient = gradients[index];
        float offsetX = (float) (x - gridX);
        float offsetY = (float) (y - gridY);
        float result = gradient.x * offsetX + gradient.y * offsetY;

        return result;
    }

    /**
     * Get the next pseudo-random single-precision value from the shared
     * generator.
     */
    public static float nextFloat() {
        return generator.nextFloat();
    }

    /**
     * Sample Perlin noise in two dimensions.
     *
     * @param sampleX first coordinate of the sample point
     * @param sampleY second coordinate of the sample point
     * @return noise value (&lt;1, &gt;-1)
     */
    public static float perlinNoise(float sampleX, float sampleY) {
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
        float nx0 = MyMath.mix(n00, n10, fadeX);
        float nx1 = MyMath.mix(n01, n11, fadeX);

        float fadeY = MyMath.fade(sampleY - squareY);
        float noise = MyMath.mix(nx0, nx1, fadeY);
        /*
         * Scale to fill the range [-1, 1].
         */
        noise /= rootHalf;

        assert noise >= -1f : noise;
        assert noise <= 1f : noise;
        return noise;
    }

    /**
     * Permute an index, wrapping to table length.
     *
     * @param index of the desired entry
     * @return (&lt;length, &ge;0)
     */
    public static int permute(int index) {
        if (permutation == null) {
            generatePermutation(defaultLength);
        }
        int i = MyMath.modulo(index, permutation.length);
        int result = permutation[i];

        assert result >= 0 : result;
        assert result < permutation.length : result;
        return result;
    }

    /**
     * Re-seed the shared pseudo-random generator.
     *
     * @param newSeed
     */
    public static void reseedGenerator(long newSeed) {
        generator.setSeed(newSeed);
    }

    /**
     * Swap two entries in the permutation.
     *
     * @param i index of the first entry (&lt;length, &ge;0)
     * @param j index of the second entry (&lt;length, &ge;0)
     */
    public static void swapTableEntries(int i, int j) {
        int saveValue = permutation[i];
        permutation[i] = permutation[j];
        permutation[j] = saveValue;
    }
}
/*
 Copyright (c) 2014-2019, Stephen Gold
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
package jme3utilities.math.noise;

import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Pseudo-random and noise utility methods. Aside from test cases, all methods
 * should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class Noise {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Noise.class.getName());
    /**
     * shared generator, for when you're not feeling fastidious
     */
    final private static Generator generator = new Generator();
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
     * Sample fractional Brownian motion (FBM) noise in 2 dimensions.
     *
     * @param generator base noise generator (not null)
     * @param sampleX the first coordinate of the sample point
     * @param sampleY the 2nd coordinate of the sample point
     * @param numOctaves number of noise components (&gt;0)
     * @param fundamental frequency for the first component (&gt;0)
     * @param gain amplitude ratio between octaves (&gt;0, &lt;1)
     * @param lacunarity frequency ratio between octaves (&gt;1)
     * @return noise value (range depends on parameters)
     */
    public static float fbmNoise(Noise2 generator, float sampleX,
            float sampleY, int numOctaves, float fundamental, float gain,
            float lacunarity) {
        Validate.nonNull(generator, "generator");
        Validate.positive(numOctaves, "octaves");
        Validate.positive(fundamental, "fundamental");
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
        for (int octave = 0; octave < numOctaves; ++octave) {
            float sample = generator.sampleNormalized(sampleX * frequency,
                    sampleY * frequency);
            total += amplitude * sample;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return total;
    }

    /**
     * Access the shared generator.
     *
     * @return float value (&ge;0, &lt;1)
     */
    public static Generator getSharedGenerator() {
        return generator;
    }

    /**
     * Obtain the next uniformly distributed, pseudo-random, single-precision
     * value from the shared generator.
     *
     * @return float value (&ge;0, &lt;1)
     * @see com.jme3.math.FastMath#nextRandomFloat()
     */
    public static float nextFloat() {
        return generator.nextFloat();
    }

    /**
     * Re-seed the shared generator.
     *
     * @param newSeed seed for generating pseudo-random numbers
     */
    public static void reseedGenerator(long newSeed) {
        generator.setSeed(newSeed);
    }
}

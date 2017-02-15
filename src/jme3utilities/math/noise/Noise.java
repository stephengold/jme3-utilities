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
package jme3utilities.math.noise;

import com.jme3.math.Vector3f;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * Random and noise utility methods. Aside from test cases, all methods should
 * be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class Noise {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Noise.class.getName());
    /**
     * shared pseudo-random generator
     */
    final private static Random generator = new Random();
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
     * @param generator base noise generator (not null)
     * @param sampleX 1st coordinate of the sample point
     * @param sampleY 2nd coordinate of the sample point
     * @param numOctaves number of noise components (&gt;0)
     * @param fundamental frequency for the 1st component (&gt;0)
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
        for (int octave = 0; octave < numOctaves; octave++) {
            float sample = generator.sampleNormalized(sampleX * frequency,
                    sampleY * frequency);
            total += amplitude * sample;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return total;
    }

    /**
     * Obtain the next uniformly distributed, pseudo-random, single-precision
     * value from the shared generator.
     *
     * @return float value (&ge;0, &lt;1)
     */
    public static float nextFloat() {
        return generator.nextFloat();
    }

    /**
     * Generate a uniformly distributed, pseudo-random unit vector. TODO rename
     *
     * @param generator (not null)
     * @return a new unit vector
     */
    public static Vector3f nextVector3f(Random generator) {
        Validate.nonNull(generator, "generator");

        Vector3f result = new Vector3f();
        double lengthSquared = 0.0;
        while (lengthSquared < 0.1 || lengthSquared > 1.0) {
            float x = 2f * generator.nextFloat() - 1f;
            float y = 2f * generator.nextFloat() - 1f;
            float z = 2f * generator.nextFloat() - 1f;
            result.set(x, y, z);
            lengthSquared = MyVector3f.lengthSquared(result);
        }
        double scaleFactor = 1.0 / Math.sqrt(lengthSquared);
        result.multLocal((float) scaleFactor);

        assert result.isUnitVector();
        return result;
    }

    /**
     * Generate a pseudo-random unit vector orthogonal to the input vector.
     *
     * @param input input vector (not null, not zero, unaffected)
     * @param generator (not null)
     * @return a new unit vector
     */
    public static Vector3f ortho(Vector3f input, Random generator) {
        Validate.nonZero(input, "input");
        Validate.nonNull(generator, "generator");

        Vector3f ref = input.normalize();
        Vector3f result = new Vector3f();
        double lengthSquared = 0.0;
        while (lengthSquared < 0.1) {
            Vector3f sample = nextVector3f(generator);
            ref.cross(sample, result);
            lengthSquared = MyVector3f.lengthSquared(result);
        }
        double scaleFactor = 1.0 / Math.sqrt(lengthSquared);
        result.multLocal((float) scaleFactor);

        assert result.isUnitVector();
        return result;
    }

    /**
     * Pick a pseudo-random element from the specified array.
     *
     * @param array array to select from (not null)
     * @param generator generator to use (not null)
     * @return element of array or null if it's empty
     */
    @SuppressWarnings("rawtypes")
    public static Object pick(Object[] array, Random generator) {
        Validate.nonNull(generator, "generator");
        Validate.nonNull(array, "array");

        int count = array.length;
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = generator.nextInt(count);
        Object result = array[index];

        return result;
    }

    /**
     * Pick a bit with the specified value from the specified set.
     *
     * @param bitValue true or false
     * @param bitset (not null, positive size, unaffected)
     * @param maxIndex the last usable bit index (&ge;0, &lt;size)
     * @param generator generator to use (not null)
     * @return bit index (&ge;0, &le;maxIndex)
     */
    public static int pick(BitSet bitset, int maxIndex, boolean value,
            Random generator) {
        Validate.nonNull(bitset, "bit set");
        Validate.inRange(maxIndex, "max index", 0, bitset.size() - 1);
        Validate.nonNull(generator, "generator");

        int firstIndex;
        int lastIndex;
        if (value) {
            firstIndex = bitset.nextSetBit(0);
            lastIndex = bitset.previousSetBit(maxIndex);
        } else {
            firstIndex = bitset.nextClearBit(0);
            lastIndex = bitset.previousClearBit(maxIndex);
        }
        if (firstIndex == -1) {
            /*
             * No possibilities.
             */
            assert lastIndex == -1 : lastIndex;
            return -1;
        } else if (firstIndex == lastIndex) {
            /*
             * Single possibility.
             */
            return firstIndex;
        }

        int numPossibilties = lastIndex - firstIndex + 1;
        int bitIndex = firstIndex + generator.nextInt(numPossibilties);
        while (bitset.get(bitIndex) != value) {
            bitIndex = firstIndex + generator.nextInt(numPossibilties);
        }

        return bitIndex;
    }

    /**
     * Pick a pseudo-random element from the specified list.
     *
     * @param list list to select from (not null)
     * @param generator generator to use (not null)
     * @return member of list or null if it's empty
     */
    @SuppressWarnings("rawtypes")
    public static Object pick(List list, Random generator) {
        Validate.nonNull(generator, "generator");
        Validate.nonNull(list, "list");

        int count = list.size();
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = generator.nextInt(count);
        Object result = list.get(index);

        return result;
    }

    /**
     * Re-seed the shared pseudo-random generator.
     *
     * @param newSeed seed for generating pseudo-random numbers
     */
    public static void reseedGenerator(long newSeed) {
        generator.setSeed(newSeed);
    }
}

/*
 Copyright (c) 2017-2020, Stephen Gold
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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Generate pseudo-random numbers, vectors, and selections.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Generator extends Random {
    // *************************************************************************
    // fields

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Generator.class.getName());
    /**
     * version number for serialization
     */
    static final long serialVersionUID = 37_705_297_950_129_619L;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a pseudo-random generator with a seed likely to be unique.
     */
    public Generator() {
        super();
    }

    /**
     * Instantiate a pseudo-random generator with the specified seed.
     *
     * @param seed initial value for the seed
     */
    public Generator(long seed) {
        super(seed);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a single-precision value, uniformly distributed between 2
     * extremes.
     *
     * @param e1 the first extreme
     * @param e2 the 2nd extreme
     * @return a pseudo-random value (&ge;min(e1,e2), &le;max(e1,e2))
     */
    public float nextFloat(float e1, float e2) {
        float result = e1 + nextFloat() * (e2 - e1);

        assert MyMath.isBetween(e1, result, e2);
        return result;
    }

    /**
     * Generate an integer value, uniformly distributed between 2 extremes.
     *
     * @param e1 the first extreme
     * @param e2 the 2nd extreme
     * @return a pseudo-random value (&ge;min(e1,e2), &le;max(e1,e2))
     */
    public int nextInt(int e1, int e2) {
        int max = Math.max(e1, e2);
        int min = Math.min(e1, e2);
        int result = min + nextInt(max - min + 1);

        assert MyMath.isBetween(e1, result, e2);
        return result;
    }

    /**
     * Generate an integer with a Poisson distribution, after Knuth.
     *
     * @param lambda the expected rate of arrivals per unit time (&gt;0)
     * @return the number of arrivals observed in a unit time interval (&ge;0)
     */
    public int nextPoisson(double lambda) {
        double expMinusLambda = Math.exp(-lambda);

        double p = 1.0;
        int result = -1;

        do {
            ++result;
            p *= nextDouble();
        } while (p > expMinusLambda);

        return result;
    }

    /**
     * Generate a uniformly distributed, pseudo-random unit quaternion.
     *
     * @return a new unit quaternion
     */
    public Quaternion nextQuaternion() {
        Quaternion result = new Quaternion();
        double lengthSquared = 2.0;
        while (lengthSquared < 0.1 || lengthSquared > 1.0) {
            float x = nextFloat(-1f, 1f);
            float y = nextFloat(-1f, 1f);
            float z = nextFloat(-1f, 1f);
            float w = nextFloat(-1f, 1f);
            result.set(x, y, z, w);
            lengthSquared = MyQuaternion.lengthSquared(result);
        }
        double scaleFactor = 1.0 / Math.sqrt(lengthSquared);
        result.multLocal((float) scaleFactor);

        return result;
    }

    /**
     * Generate a uniformly distributed, pseudo-random unit vector.
     *
     * @return a new unit vector
     */
    public Vector3f nextUnitVector3f() {
        Vector3f result = nextUnitVector3f(null);
        return result;
    }

    /**
     * Generate a uniformly distributed, pseudo-random unit vector.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a unit vector (either storeResult or a new instance)
     */
    public Vector3f nextUnitVector3f(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        double lengthSquared = 0.0;
        while (lengthSquared < 0.1 || lengthSquared > 1.0) {
            float x = nextFloat(-1f, 1f);
            float y = nextFloat(-1f, 1f);
            float z = nextFloat(-1f, 1f);
            result.set(x, y, z);
            lengthSquared = MyVector3f.lengthSquared(result);
        }
        double scaleFactor = 1.0 / Math.sqrt(lengthSquared);
        result.multLocal((float) scaleFactor);

        assert result.isUnitVector();
        return result;
    }

    /**
     * Generate a pseudo-random vector that is uniformly distributed throughout
     * the unit sphere centered on the origin.
     *
     * @return a new vector with length&le;1
     */
    public Vector3f nextVector3f() {
        Vector3f result = nextVector3f(null);
        return result;
    }

    /**
     * Generate a pseudo-random vector that is uniformly distributed throughout
     * the unit sphere centered on the origin.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a vector with length&le;1 (either storeResult or a new instance)
     */
    public Vector3f nextVector3f(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        double lengthSquared = 2.0;
        while (lengthSquared > 1.0) {
            float x = nextFloat(-1f, 1f);
            float y = nextFloat(-1f, 1f);
            float z = nextFloat(-1f, 1f);
            result.set(x, y, z);
            lengthSquared = MyVector3f.lengthSquared(result);
        }

        return result;
    }

    /**
     * Generate a pseudo-random unit vector orthogonal to the input vector.
     *
     * @param input direction vector (not null, not zero, unaffected)
     * @return a new unit vector
     */
    public Vector3f ortho(Vector3f input) {
        Validate.nonZero(input, "input");

        Vector3f ref = input.normalize();
        Vector3f result = new Vector3f();
        double lengthSquared = 0.0;
        while (lengthSquared < 0.1) {
            Vector3f sample = nextUnitVector3f();
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
     * @param array the array to select from (not null, may be empty)
     * @return a pre-existing element of array, or null if it's empty
     */
    @SuppressWarnings("rawtypes")
    public Object pick(Object[] array) {
        Validate.nonNull(array, "array");

        int count = array.length;
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = nextInt(count);
        Object result = array[index];

        return result;
    }

    /**
     * Pick a pseudo-random bit with the specified value from the specified set.
     *
     * @param bitset (not null, positive size, unaffected)
     * @param maxIndex the last usable bit index (&ge;0, &lt;size)
     * @param bitValue true or false
     * @return bit index (&ge;0, &le;maxIndex)
     */
    public int pick(BitSet bitset, int maxIndex, boolean bitValue) {
        Validate.nonNull(bitset, "bit set");
        Validate.inRange(maxIndex, "max index", 0, bitset.size() - 1);

        int firstIndex;
        int lastIndex;
        if (bitValue) {
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
        int bitIndex = firstIndex + nextInt(numPossibilties);
        while (bitset.get(bitIndex) != bitValue) {
            bitIndex = firstIndex + nextInt(numPossibilties);
        }

        return bitIndex;
    }

    /**
     * Pick a pseudo-random element from the specified list.
     *
     * @param list the list to select from (not null, may be empty)
     * @return a pre-existing element of list, or null if it's empty
     */
    @SuppressWarnings("rawtypes")
    public Object pick(List list) {
        Validate.nonNull(list, "list");

        int count = list.size();
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = nextInt(count);
        Object result = list.get(index);

        return result;
    }
}

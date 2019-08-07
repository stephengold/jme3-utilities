/*
 Copyright (c) 2019 Stephen Gold
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
package jme3utilities.math;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A simplified collection of Vector3f values without duplicates, implemented
 * using a FloatBuffer.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorSetUsingBuffer implements VectorSet {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in a Vector3f
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VectorSetUsingBuffer.class.getName());
    // *************************************************************************
    // fields

    /**
     * buffer to hold the Vector3f values
     */
    private FloatBuffer buffer;
    /**
     * look up last buffer position for hash index
     */
    private int endPosition[];
    /**
     * number of enlargements since last clearStats()
     */
    static int numEnlargements = 0;
    /**
     * number of reads since last clearStats()
     */
    static int numReads = 0;
    /**
     * number of searches since last clearStats()
     */
    static int numSearches = 0;
    /**
     * look up first buffer position plus 1 for hash index
     */
    private int startPositionPlus1[];
    /**
     * system milliseconds as of last clearStats()
     */
    static long resetMillis = 0L;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty set with the specified initial capacity.
     *
     * @param numVectors the number of vectors this set can hold without
     * enlargement (&gt;0)
     */
    public VectorSetUsingBuffer(int numVectors) {
        Validate.positive(numVectors, "number of vectors");
        allocate(numVectors);
        flip();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Reset the hashing statistics.
     */
    public static void clearStats() {
        numEnlargements = 0;
        numReads = 0;
        numSearches = 0;
        resetMillis = System.currentTimeMillis();
    }

    /**
     * Print the hashing statistics.
     *
     * @param tag (not null)
     */
    public static void dumpStats(String tag) {
        long msec = System.currentTimeMillis() - resetMillis;
        String msg = String.format(
                "%s %d enlargement%s, %d search%s, and %d read%s in %d msec",
                tag, numEnlargements, (numEnlargements == 1) ? "" : "s",
                numSearches, (numSearches == 1) ? "" : "es",
                numReads, (numReads == 1) ? "" : "s", msec);
        System.out.println(msg);
    }
    // *************************************************************************
    // VectorSet methods

    /**
     * Add the value of the specified Vector3f to this set, if it's not already
     * present.
     *
     * @param vector the value to add (not null, unaffected)
     */
    @Override
    public void add(Vector3f vector) {
        if (startPositionPlus1 == null) {
            throw new IllegalStateException("toBuffer() has been invoked.");
        }

        int hashCode = vector.hashCode();
        if (!contains(vector, hashCode)) {
            unflip();
            if (buffer.remaining() < numAxes) {
                enlarge();
                assert buffer.remaining() >= numAxes;
            }
            add(vector, hashCode);
            flip();
        }
    }

    /**
     * Test whether this set contains the specified value.
     *
     * @param vector the value to find (not null, unaffected)
     * @return true if found, otherwise false
     */
    @Override
    public boolean contains(Vector3f vector) {
        int hashCode = vector.hashCode();
        boolean result = contains(vector, hashCode);

        return result;
    }

    /**
     * Calculate the sample covariance of the values in this set.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the unbiased sample covariance (either storeResult or a new
     * matrix, not null)
     */
    @Override
    public Matrix3f covariance(Matrix3f storeResult) {
        Matrix3f result
                = MyBuffer.covariance(buffer, 0, buffer.limit(), storeResult);
        return result;
    }

    /**
     * Find the maximum absolute coordinate for each axis among the Vector3f
     * values in this set.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the half extent for each axis (either storeResult or a new
     * instance)
     */
    @Override
    public Vector3f maxAbs(Vector3f storeResult) {
        Vector3f result
                = MyBuffer.maxAbs(buffer, 0, buffer.limit(), storeResult);
        return result;
    }

    /**
     * Find the length of the longest Vector3f value in this set.
     *
     * @return the magnitude (&ge;0)
     */
    @Override
    public float maxLength() {
        float length = MyBuffer.maxLength(buffer, 0, buffer.limit());
        return length;
    }

    /**
     * Find the maximum and minimum coordinates for each axis among the values
     * in this set.
     *
     * @param storeMaxima storage for the maxima (not null, modified)
     * @param storeMinima storage for the minima (not null, modified)
     */
    @Override
    public void maxMin(Vector3f storeMaxima, Vector3f storeMinima) {
        Validate.nonNull(storeMaxima, "store maxima");
        Validate.nonNull(storeMinima, "store minima");

        MyBuffer.maxMin(buffer, 0, buffer.limit(), storeMaxima, storeMinima);
    }

    /**
     * Calculate the sample mean for each axis among the values in this set.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the sample mean for each axis (either storeResult or a new
     * Vector3f)
     */
    @Override
    public Vector3f mean(Vector3f storeResult) {
        Vector3f result = MyBuffer.mean(buffer, 0, buffer.limit(), storeResult);
        return result;
    }

    /**
     * Determine the number of values in this set.
     *
     * @return the count (&ge;0)
     */
    @Override
    public int numVectors() {
        int limit = buffer.limit();
        int numFloats = (limit < buffer.capacity()) ? limit : buffer.position();
        assert numFloats % 3 == 0 : numFloats;
        int numVectors = numFloats / 3;

        assert numVectors >= 0 : numVectors;
        return numVectors;
    }

    /**
     * Access a Buffer containing all values in this set. No further add() is
     * allowed.
     *
     * @return the pre-existing Buffer, flipped but possibly not rewound
     */
    @Override
    public FloatBuffer toBuffer() {
        startPositionPlus1 = null;
        endPosition = null;

        return buffer;
    }
    // *************************************************************************
    // private methods

    /**
     * Add the specified vector's value to this set without checking for
     * capacity or duplication. The buffer cannot be in a flipped state.
     *
     * @param vector the value to add (not null, unaffected)
     * @param hasCode the hash code of vector
     */
    private void add(Vector3f vector, int hashCode) {
        assert buffer.limit() == buffer.capacity();

        int position = buffer.position();
        int hashIndex = MyMath.modulo(hashCode, startPositionPlus1.length);
        int spp1 = startPositionPlus1[hashIndex];
        if (spp1 == 0) {
            startPositionPlus1[hashIndex] = position + 1;
        }
        endPosition[hashIndex] = position;

        buffer.put(vector.x);
        buffer.put(vector.y);
        buffer.put(vector.z);
    }

    /**
     * Initialize an empty, unflipped set with the specified initial capacity.
     *
     * @param numVectors (&gt;0)
     */
    private void allocate(int numVectors) {
        assert numVectors > 0 : numVectors;

        int numFloats = numAxes * numVectors + 1;
        buffer = BufferUtils.createFloatBuffer(numFloats);
        startPositionPlus1 = new int[numFloats]; // initialized to all 0s
        endPosition = new int[numFloats];
    }

    /**
     * Test whether the specified vector's value is in the set.
     *
     * @param vector the value to find (not null, unaffected)
     * @param hashCode the hash code of vector
     * @return true if found, otherwise false
     */
    private boolean contains(Vector3f vector, int hashCode) {
        boolean result = false;
        int hashIndex = MyMath.modulo(hashCode, startPositionPlus1.length);
        int spp1 = startPositionPlus1[hashIndex];
        if (spp1 != 0) {
            int end = endPosition[hashIndex];
            buffer.position(spp1 - 1);
            while (buffer.position() <= end) {
                float x = buffer.get();
                float y = buffer.get();
                float z = buffer.get();
                if (x == vector.x && y == vector.y && z == vector.z) {
                    result = true;
                    break;
                }
            }
            ++numSearches;
            numReads += (end - spp1 + 1) / 3;
        }

        return result;
    }

    /**
     * Quadruple the capacity of the buffer, which must be unflipped.
     */
    private void enlarge() {
        Vector3f tempVector = new Vector3f();
        int numVectors = numVectors();

        FloatBuffer oldBuffer = toBuffer();
        allocate(4 * numVectors);
        oldBuffer.flip();
        while (oldBuffer.hasRemaining()) {
            tempVector.x = oldBuffer.get();
            tempVector.y = oldBuffer.get();
            tempVector.z = oldBuffer.get();
            int hashCode = tempVector.hashCode();
            add(tempVector, hashCode);
        }
        assert numVectors() == numVectors;
        ++numEnlargements;
    }

    /**
     * Switch from writing to reading.
     */
    private void flip() {
        assert buffer.limit() == buffer.capacity();
        buffer.limit(buffer.position());
        assert buffer.limit() != buffer.capacity();
    }

    /**
     * Switch from reading to writing.
     */
    private void unflip() {
        assert buffer.limit() != buffer.capacity();
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
        assert buffer.limit() == buffer.capacity();
    }
}

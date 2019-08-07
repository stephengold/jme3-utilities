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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A simplified collection of Vector3f values without duplicates, implemented
 * using a Collection.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorSetUsingCollection implements VectorSet {
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
            = Logger.getLogger(VectorSetUsingCollection.class.getName());
    // *************************************************************************
    // fields

    /**
     * collection of values
     */
    final private Set<Vector3f> set;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty set with the specified initial capacity and default
     * load factor.
     *
     * @param numVectors the initial capacity of the hash table (&gt;0)
     */
    public VectorSetUsingCollection(int numVectors) {
        Validate.positive(numVectors, "number of vectors");
        set = new HashSet<>(numVectors);
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
        Validate.nonNull(vector, "vector");
        set.add(vector.clone());
    }

    /**
     * Test whether this set contains the specified value.
     *
     * @param vector the value to find (not null, unaffected)
     * @return true if found, otherwise false
     */
    @Override
    public boolean contains(Vector3f vector) {
        boolean result = set.contains(vector);
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
        Matrix3f result = (storeResult == null) ? new Matrix3f() : storeResult;
        int numSamples = numVectors();
        assert numSamples > 1 : numSamples;

        Vector3f sampleMean = mean(null);
        /*
         * Accumulate sums in the upper triangle of the matrix.
         */
        result.zero();
        float[] aboveMean = new float[numAxes];
        for (Vector3f v : set) {
            aboveMean[0] = v.x - sampleMean.x;
            aboveMean[1] = v.y - sampleMean.y;
            aboveMean[2] = v.z - sampleMean.z;
            for (int rowI = 0; rowI < numAxes; ++rowI) {
                for (int columnI = rowI; columnI < numAxes; ++columnI) {
                    float sum = result.get(rowI, columnI);
                    sum += aboveMean[rowI] * aboveMean[columnI];
                    result.set(rowI, columnI, sum);
                }
            }
        }
        /*
         * Multiply sums by 1/(N-1) and fill in the lower triangle.
         */
        float nMinus1 = numSamples - 1;
        for (int rowI = 0; rowI < numAxes; ++rowI) {
            for (int columnI = rowI; columnI < numAxes; ++columnI) {
                float sum = result.get(rowI, columnI);
                float element = sum / nMinus1;
                result.set(rowI, columnI, element);
                result.set(columnI, rowI, element);
            }
        }

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
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.zero();
        for (Vector3f tempVector : set) {
            result.x = Math.max(result.x, Math.abs(tempVector.x));
            result.y = Math.max(result.y, Math.abs(tempVector.y));
            result.z = Math.max(result.z, Math.abs(tempVector.z));
        }

        return result;
    }

    /**
     * Find the length of the longest Vector3f value in this set.
     *
     * @return the magnitude (&ge;0)
     */
    @Override
    public float maxLength() {
        double maxLengthSquared = 0.0;
        for (Vector3f tempVector : set) {
            double lengthSquared = MyVector3f.lengthSquared(tempVector);
            if (lengthSquared > maxLengthSquared) {
                maxLengthSquared = lengthSquared;
            }
        }

        float length = (float) Math.sqrt(maxLengthSquared);
        assert length >= 0f : length;
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
        storeMaxima.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
        storeMinima.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);

        for (Vector3f tempVector : set) {
            MyVector3f.accumulateMinima(storeMinima, tempVector);
            MyVector3f.accumulateMaxima(storeMaxima, tempVector);
        }
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
        int numVectors = numVectors();
        assert numVectors > 0 : numVectors;
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.zero();
        for (Vector3f tempVector : set) {
            result.addLocal(tempVector);
        }
        result.divideLocal(numVectors);

        return result;
    }

    /**
     * Determine the number of values in this set.
     *
     * @return the count (&ge;0)
     */
    @Override
    public int numVectors() {
        int numVectors = set.size();
        assert numVectors >= 0 : numVectors;
        return numVectors;
    }

    /**
     * Access a Buffer containing all values in this set. No further add() is
     * allowed.
     *
     * @return a new Buffer, flipped and rewound
     */
    @Override
    public FloatBuffer toBuffer() {
        int numFloats = 3 * set.size();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);
        for (Vector3f tempVector : set) {
            buffer.put(tempVector.x);
            buffer.put(tempVector.y);
            buffer.put(tempVector.z);
        }
        buffer.flip();

        return buffer;
    }
}

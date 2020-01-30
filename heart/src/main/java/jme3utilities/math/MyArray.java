/*
 Copyright (c) 2013-2020, Stephen Gold
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

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods that operate on arrays.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyArray {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyArray.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyArray() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the smallest axis-aligned bounding box for the vectors in the
     * specified array.
     *
     * @see com.jme3.bounding.BoundingBox#containAABB(java.nio.FloatBuffer)
     *
     * @param array the vectors (not null, not empty, all finite, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the BoundingBox (either storeResult or a new instance, not null)
     */
    public static BoundingBox aabb(Vector3f[] array, BoundingBox storeResult) {
        Validate.nonEmpty(array, "array");

        Vector3f maxima = new Vector3f(Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f minima = new Vector3f(Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        for (Vector3f tempVector : array) {
            if (!Vector3f.isValidVector(tempVector)) {
                logger.log(Level.WARNING, "Invalid vector {0} in array",
                        tempVector);
            }
            MyVector3f.accumulateMinima(minima, tempVector);
            MyVector3f.accumulateMaxima(maxima, tempVector);
        }

        BoundingBox result;
        if (storeResult == null) {
            result = new BoundingBox(minima, maxima);
        } else {
            storeResult.setMinMax(minima, maxima);
            result = storeResult;
        }

        return result;
    }

    /**
     * Calculate the sample covariance of the 3-D vectors in the specified float
     * array.
     *
     * @param input the input array (not null, at least 6 elements, length a
     * multiple of 3, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the unbiased sample covariance (either storeResult or a new
     * matrix, not null)
     */
    public static Matrix3f covarianceVector3f(float[] input, Matrix3f storeResult) {
        Validate.nonEmpty(input, "input");
        int length = input.length;
        assert (length % 3 == 0) : length;
        assert length >= 6 : length;
        Matrix3f result = (storeResult == null) ? new Matrix3f() : storeResult;

        Vector3f sampleMean = meanVector3f(input, null);
        /*
         * Accumulate sums in the upper triangle of the matrix.
         */
        result.zero();
        float[] aboveMean = new float[3];
        int vectorCount = length / 3;
        for (int vectorIndex = 0; vectorIndex < vectorCount; ++vectorIndex) {
            aboveMean[0] = input[3 * vectorIndex] - sampleMean.x;
            aboveMean[1] = input[3 * vectorIndex + 1] - sampleMean.y;
            aboveMean[2] = input[3 * vectorIndex + 2] - sampleMean.z;
            for (int rowIndex = 0; rowIndex < 3; ++rowIndex) {
                for (int columnIndex = rowIndex; columnIndex < 3; ++columnIndex) {
                    float sum = result.get(rowIndex, columnIndex);
                    sum += aboveMean[rowIndex] * aboveMean[columnIndex];
                    result.set(rowIndex, columnIndex, sum);
                }
            }
        }
        /*
         * Multiply sums by 1/(N-1) and fill in the lower triangle.
         */
        float nMinus1 = vectorCount - 1;
        for (int rowIndex = 0; rowIndex < 3; ++rowIndex) {
            for (int columnIndex = rowIndex; columnIndex < 3; ++columnIndex) {
                float sum = result.get(rowIndex, columnIndex);
                float element = sum / nMinus1;
                result.set(rowIndex, columnIndex, element);
                result.set(columnIndex, rowIndex, element);
            }
        }

        return result;
    }

    /**
     * Count the number of distinct vectors in the specified array, without
     * distinguishing 0 from -0.
     *
     * @param array the array to analyze (not null, unaffected)
     * @return count (&ge;0)
     */
    public static int countNe(Vector3f[] array) {
        int length = array.length;
        Set<Vector3f> distinct = new HashSet<>(length);
        for (Vector3f vector : array) {
            Vector3f standard = MyVector3f.standardize(vector, null);
            distinct.add(standard);
        }
        int count = distinct.size();

        return count;
    }

    /**
     * Test whether the first N elements of the specified vector contain &gt;1
     * distinct values, without distinguishing 0 from -0.
     *
     * @param vector the array to analyze (not null, unaffected)
     * @param n number of elements to consider (&ge;0)
     * @return true if multiple values found, otherwise false
     */
    public static boolean distinct(float[] vector, int n) {
        Validate.nonNull(vector, "vector");
        Validate.inRange(n, "length", 0, vector.length);

        boolean result = false;
        if (n > 1) {
            float firstValue = vector[0];
            for (int i = 1; i < n; ++i) {
                float value = vector[i];
                if (value != firstValue) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find the index of the last value &le; the specified one in the specified
     * sorted float array, using binary search.
     *
     * @param value value to search for
     * @param array array to search (not null, strictly monotonic increasing
     * order, unaffected)
     * @return array index (&ge;0) or -1 if array is empty or value&le;array[0]
     *
     * @see java.util.Arrays#binarySearch(float[], float)
     */
    public static int findPreviousIndex(float value, float[] array) {
        Validate.nonNull(array, "array");

        int lowerBound = -1;
        int upperBound = array.length - 1;
        int result;
        while (true) {
            if (upperBound == lowerBound) {
                result = lowerBound;
                break;
            }
            int testIndex = (lowerBound + upperBound + 1) / 2;
            float testValue = array[testIndex];
            if (value > testValue) {
                lowerBound = testIndex;
            } else if (value < testValue) {
                upperBound = testIndex - 1;
            } else if (value == testValue) {
                result = testIndex;
                break;
            }
        }

        assert result >= -1 : result;
        return result;
    }

    /**
     * Find the index of the first true element in the specified boolean array.
     *
     * @param array the array to analyze (not null, unaffected)
     * @return the index (&ge;0, &lt;length) or -1 if no true element found
     */
    public static int first(boolean[] array) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Test whether the specified array is sorted in ascending order with no
     * duplicates.
     *
     * @param array the array to analyze (not null, unaffected)
     * @return true if sorted, otherwise false
     */
    @SuppressWarnings("unchecked")
    public static boolean isSorted(Comparable[] array) {
        for (int i = 0; i < array.length - 1; ++i) {
            if (array[i].compareTo(array[i + 1]) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether the specified array is sorted in ascending order with no
     * duplicates.
     *
     * @param array the array to analyze (not null, unaffected)
     * @return true if sorted, otherwise false
     */
    public static boolean isSorted(float[] array) {
        for (int i = 0; i < array.length - 1; ++i) {
            if (array[i] >= array[i + 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate the arithmetic mean of the 3-D vectors in the specified float
     * array.
     *
     * @param input the array to analyze (not null, not empty, length a multiple
     * of 3, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the mean (either storeResult or a new vector, not null)
     */
    public static Vector3f meanVector3f(float[] input, Vector3f storeResult) {
        Validate.nonEmpty(input, "input");
        int length = input.length;
        assert (length % 3 == 0) : length;
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.zero();
        int vectorCount = length / 3;
        for (int vectorIndex = 0; vectorIndex < vectorCount; ++vectorIndex) {
            result.x += input[3 * vectorIndex];
            result.y += input[3 * vectorIndex + 1];
            result.z += input[3 * vectorIndex + 2];
        }
        result.divideLocal(vectorCount);

        return result;
    }

    /**
     * Calculate the arithmetic mean of a non-empty array of vectors.
     *
     * @param array the vectors to average (not null, not empty, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the mean (either storeResult or a new vector, not null)
     */
    public static Vector3f mean(Vector3f[] array, Vector3f storeResult) {
        Validate.nonEmpty(array, "array");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        result.zero();
        for (Vector3f location : array) {
            result.addLocal(location);
        }
        int count = array.length;
        result.divideLocal(count);

        return result;
    }

    /**
     * Normalize a dataset to [0, 1] using the specified min and max. If
     * min==max, all data will be set to 0.5.
     *
     * @param dataset data to normalize (not null, modified)
     * @param min value to normalize to 0
     * @param max value to normalize to 1
     */
    public static void normalize(float[] dataset, float min, float max) {
        Validate.nonNull(dataset, "dataset");

        for (int i = 0; i < dataset.length; ++i) {
            if (min == max) {
                dataset[i] = 0.5f;
            } else {
                dataset[i] = (dataset[i] - min) / (max - min);
            }
        }
    }
}

/*
 Copyright (c) 2013-2018, Stephen Gold
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

import com.jme3.math.Vector3f;
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
     * Test whether the first N elements of the specified vector contain &gt;1
     * distinct values, without distinguishing 0 from -0.
     *
     * @param vector input (not null, unaffected)
     * @param n number of elements to consider
     * @return true if multiple values found, otherwise false
     */
    public static boolean distinct(float[] vector, int n) {
        Validate.nonNull(vector, "vector");
        Validate.inRange(n, "length", 0, vector.length);

        boolean result = false;
        if (n > 1) {
            float firstValue = vector[0];
            for (int i = 1; i < n; i++) {
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
     * Find the index of the last value &le; the specified one in a sorted
     * array, using binary search.
     *
     * @param value value to search for
     * @param array array to search (not null, strictly monotonic increasing
     * order, unaffected)
     * @return array index (&ge;0) or -1 if array is empty or value&le;array[0]
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
     * Find the index of the first true element in the input array.
     *
     * @param array input (not null, unaffected)
     * @return index (&ge;0, &lt;length) or -1 if no true element found
     */
    public static int first(boolean[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Test whether the specified array contains more than one distinct value,
     * distinguishing 0 from -0.
     *
     * @param array input (not null, unaffected)
     * @return true if multiple values found, otherwise false
     */
    public static boolean hasDistinct(float[] array) {
        Validate.nonNull(array, "array");

        boolean result = false;
        if (array.length > 1) {
            float first = array[0];
            for (float value : array) {
                if (Float.compare(value, first) != 0) {
                    result = true;
                    break;
                }
            }
        }

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
     * Normalize a dataset to [0, 1]. If min=max, all data will be set to 0.5 .
     *
     * @param dataset data to normalize (not null, modified)
     */
    public static void normalize(float[] dataset) {
        Validate.nonNull(dataset, "dataset");

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float value : dataset) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        normalize(dataset, min, max);
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

        for (int i = 0; i < dataset.length; i++) {
            if (min == max) {
                dataset[i] = 0.5f;
            } else {
                dataset[i] = (dataset[i] - min) / (max - min);
            }
        }
    }
}

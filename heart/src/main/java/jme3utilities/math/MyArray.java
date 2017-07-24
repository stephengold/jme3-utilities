/*
 Copyright (c) 2013-2017, Stephen Gold
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
    final private static Logger logger = Logger.getLogger(
            MyArray.class.getName());
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

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
package jme3utilities;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to throw exceptions for invalid method arguments. Aside from
 * test cases, all methods should be public and static.
 * <p>
 * These methods are intended for checking the arguments of public/protected
 * methods. For private/package methods, use assertions instead.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class Validate {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Validate.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Validate() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Validate a non-negative proper fraction as a method argument.
     *
     * @param value fraction to validate (&le;1, &ge;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is outside the range [0, 1]
     */
    public static void fraction(float value, String description) {
        inRange(value, description, 0f, 1f);
    }

    /**
     * Validate a limited integer as a method argument.
     *
     * @param value value to validate (&le;max, &ge;min)
     * @param description description of the value
     * @param min the smallest valid value (&le;max)
     * @param max the largest valid value (&ge;max)
     * @throws IllegalArgumentException if the value is outside the range [min,
     * max]
     */
    public static void inRange(int value, String description,
            int min, int max) {
        if (value < min) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be greater than or equal to %d.", what, min);
            throw new IllegalArgumentException(message);
        }

        if (value > max) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be less than or equal to %d.", what, max);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a limited single-precision value as a method argument.
     *
     * @param value value to validate (&le;max, &ge;min)
     * @param description description of the value
     * @param min the smallest valid value (&le;max)
     * @param max the largest valid value (&ge;max)
     * @throws IllegalArgumentException if the value is outside the range
     */
    public static void inRange(float value, String description,
            float min, float max) {
        if (!(value >= min)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be greater than or equal to %f.", what, min);
            throw new IllegalArgumentException(message);
        }

        if (!(value <= max)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be less than or equal to %f.", what, max);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a limited double-precision value as a method argument.
     *
     * @param value value to validate (&le;max, &ge;min)
     * @param description description of the value
     * @param min the smallest valid value (&le;max)
     * @param max the largest valid value (&ge;max)
     * @throws IllegalArgumentException if the value is outside the range
     */
    public static void inRange(double value, String description,
            double min, double max) {
        if (!(value >= min)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be greater than or equal to %f.", what, min);
            throw new IllegalArgumentException(message);
        }

        if (!(value <= max)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format(
                    "%s should be less than or equal to %f.", what, max);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-null, non-empty string as a method argument.
     *
     * @param string string to validate (not null, not empty)
     * @param description description of the string
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string has zero length
     */
    public static void nonEmpty(String string, String description) {
        nonNull(string, description);

        int length = string.length();
        if (length <= 0) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message = String.format("%s should not be empty.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-negative integer as a method argument.
     *
     * @param value value to validate (&ge;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is negative
     */
    public static void nonNegative(int value, String description) {
        if (value < 0) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format("%s should not be negative.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-negative single-precision value as a method argument.
     *
     * @param value value to validate (&ge;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is negative or NaN
     */
    public static void nonNegative(float value, String description) {
        if (!(value >= 0f)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format("%s should not be negative.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-null reference. In many methods, validation can be omitted
     * because the object in question is about to be dereferenced.
     * <p>
     * While it might seem more logical to throw an IllegalArgumentException in
     * the case of a method argument, the javadoc for NullPointerException says,
     * "Applications should throw instances of this class..."
     *
     * @param object reference to validate (not null, unaffected)
     * @param description description of the object
     * @throws NullPointerException if the reference is null
     */
    public static void nonNull(Object object, String description) {
        if (object == null) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message = String.format("%s should not be null.", what);
            throw new NullPointerException(message);
        }
    }

    /**
     * Validate a non-zero single-precision value as a method argument.
     *
     * @param value value to validate (&ne;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is zero
     */
    public static void nonZero(float value, String description) {
        if (value == 0f) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message = String.format("%s should not be zero.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-zero Vector2f as a method argument.
     *
     * @param vector vector to validate (not null, non-zero)
     * @param description description of the vector
     * @throws IllegalArgumentException if the vector has zero length
     * @throws NullPointerException if the vector is null
     */
    public static void nonZero(Vector2f vector, String description) {
        nonNull(vector, description);

        if (vector.x == 0f && vector.y == 0f) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message = String.format(
                    "%s should have positive length.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-zero Vector3f as a method argument.
     *
     * @param vector vector to validate (not null, non-zero)
     * @param description description of the vector
     * @throws IllegalArgumentException if the vector has zero length
     * @throws NullPointerException if the vector is null
     */
    public static void nonZero(Vector3f vector, String description) {
        nonNull(vector, description);

        if (vector.x == 0f && vector.y == 0f && vector.z == 0f) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            String message = String.format(
                    "%s should have positive length.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a positive integer value as a method argument.
     *
     * @param value value to validate (&gt;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is negative or zero
     */
    public static void positive(int value, String description) {
        if (value <= 0) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format("%s should be positive.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a positive single-precision value as a method argument.
     *
     * @param value value to validate (&gt;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is not positive
     */
    public static void positive(float value, String description) {
        if (!(value > 0f)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format("%s should be positive.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a positive double-precision value as a method argument.
     *
     * @param value value to validate (&gt;0)
     * @param description description of the value
     * @throws IllegalArgumentException if the value is not positive
     */
    public static void positive(double value, String description) {
        if (!(value > 0.0)) {
            String what;
            if (description == null) {
                what = "method argument";
            } else {
                what = description;
            }
            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, value});
            String message = String.format("%s should be positive.", what);
            throw new IllegalArgumentException(message);
        }
    }
}

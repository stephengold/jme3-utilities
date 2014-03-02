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
package jme3utilities.debug;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to throw exceptions for invalid method arguments. Aside from
 * test cases, all methods should be public and static.
 * <p>
 * These methods are intended for checking the arguments of public/protected
 * methods. For private/package methods, use assertions instead.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
final public class Validate {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Validate.class.getName());
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
     * Validate a non-negative proper fraction.
     *
     * @param value to validate (&le;1, &ge;0)
     * @param description of the value (not null)
     * @throws IllegalArgumentException if the value is outside the range [0, 1]
     */
    public static void fraction(float value, String description) {
        if (!(value >= 0f && value <= 1f)) {
            logger.log(Level.SEVERE, "{0}={1}",
                    new Object[]{description, value});
            String message = String.format(
                    "%s should be between 0 and 1, inclusive", description);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-negative integer.
     *
     * @param value to validate (&ge;0)
     * @param description of the value (not null)
     * @throws IllegalArgumentException if the value is negative
     */
    public static void nonNegative(int value, String description) {
        if (value < 0) {
            logger.log(Level.SEVERE, "{0}={1}",
                    new Object[]{description, value});
            String message =
                    String.format("%s should not be negative", description);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-negative single-precision value.
     *
     * @param value to validate (&ge;0)
     * @param description of the value (not null)
     * @throws IllegalArgumentException if the value is negative or NaN
     */
    public static void nonNegative(float value, String description) {
        if (!(value >= 0f)) {
            logger.log(Level.SEVERE, "{0}={1}",
                    new Object[]{description, value});
            String message =
                    String.format("%s should not be negative", description);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a non-null reference. In many methods, validation can be omitted
     * because the object in question is about to be dereferenced.
     * <p>
     * While it might seem more logical to throw an IllegalArgumentException,
     * the javadoc for NullPointerException says, "Applications should throw
     * instances of this class..."
     *
     * @param object reference to validate (not null)
     * @param description of the object (not null)
     * @throws NullPointerException if the reference is null
     */
    public static void nonNull(Object object, String description) {
        if (object == null) {
            String message =
                    String.format("%s should not be null", description);
            throw new NullPointerException(message);
        }
    }

    /**
     * Validate a positive integer.
     *
     * @param value to validate (&gt;0)
     * @param description of the value (not null)
     * @throws IllegalArgumentException if the value is negative or zero
     */
    public static void positive(int value, String description) {
        if (value <= 0) {
            logger.log(Level.SEVERE, "{0}={1}",
                    new Object[]{description, value});
            String message =
                    String.format("%s should be positive", description);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate a positive single-precision value.
     *
     * @param value to validate (&gt;0)
     * @param description of the value (not null)
     * @throws IllegalArgumentException if the value is not positive
     */
    public static void positive(float value, String description) {
        if (!(value > 0f)) {
            logger.log(Level.SEVERE, "{0}={1}",
                    new Object[]{description, value});
            String message =
                    String.format("%s should be positive", description);
            throw new IllegalArgumentException(message);
        }
    }
}
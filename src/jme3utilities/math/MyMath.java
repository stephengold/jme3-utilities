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

import com.jme3.math.FastMath;
import java.io.PrintStream;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Mathematical utility methods. Aside from test cases, all methods should be
 * public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyMath {
    // *************************************************************************
    // constants

    /**
     * the square root of 1/2
     */
    final public static float rootHalf = FastMath.sqrt(0.5f);
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyMath.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyMath() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Compute the circle function sqrt(1 - x^2) for a double-precision value.
     *
     * @param abscissa input (&le;1, &ge;-1)
     * @return positive ordinate of the unit circle at the abscissa (&le;1,
     * &ge;0)
     */
    public static double circle(double abscissa) {
        Validate.inRange(abscissa, "abscissa", -1.0, 1.0);

        double y = Math.sqrt(1.0 - abscissa * abscissa);

        assert y >= 0.0 : y;
        assert y <= 1.0 : y;
        return y;
    }

    /**
     * Compute the circle function sqrt(1 - x^2) for a single-precision value.
     * Double precision arithmetic is used to reduce the risk of overflow.
     *
     * @param abscissa input (&le;1, &ge;-1)
     * @return positive ordinate of the unit circle at the abscissa (&le;1,
     * &ge;0)
     */
    public static float circle(float abscissa) {
        Validate.inRange(abscissa, "abscissa", -1f, 1f);

        double x = (double) abscissa;
        float y = (float) circle(x);

        assert y >= 0f : y;
        assert y <= 1f : y;
        return y;
    }

    /**
     * Clamp the magnitude of a single-precision value.
     *
     * @param fValue input value to be clamped
     * @param maxMagnitude limit of the clamp (&ge;0)
     * @return value between -maxMagnitude and +maxMagnitude inclusive which is
     * closest to fValue
     * @see com.jme3.math.FastMath#clamp(float,float,float)
     */
    public static float clamp(float fValue, float maxMagnitude) {
        Validate.nonNegative(maxMagnitude, "limit");
        float result = FastMath.clamp(fValue, -maxMagnitude, maxMagnitude);

        assert result >= -maxMagnitude : result;
        assert result <= maxMagnitude : result;
        return result;
    }

    /**
     * Clamp the magnitude of a double-precision value.
     *
     * @param dValue input value to be clamped
     * @param maxMagnitude limit of the clamp (&ge;0)
     * @return value between -maxMagnitude and +maxMagnitude inclusive which is
     * closest to fValue
     * @see com.jme3.math.FastMath#clamp(float,float,float)
     */
    public static double clamp(double dValue, double maxMagnitude) {
        Validate.nonNegative(maxMagnitude, "limit");

        if (dValue < -maxMagnitude) {
            return -maxMagnitude;
        } else if (dValue > maxMagnitude) {
            return maxMagnitude;
        } else {
            return dValue;
        }
    }

    /**
     * Cube the specified single-precision value. Logs a warning in case of
     * overflow.
     *
     * @param fValue input value to be cubed
     * @return fValue raised to the third power
     * @see #cubeRoot(float)
     * @see com.jme3.math.FastMath#sqr(float)
     */
    public static float cube(float fValue) {
        float result = fValue * fValue * fValue;

        if (Float.isInfinite(result)) {
            String message = String.format("Overflow from cubing %g.", fValue);
            logger.warning(message);
        }
        return result;
    }

    /**
     * Extract the cube root of a single-precision value. Unlike
     * com.jme3.math.FastMath#pow(), this method works on negative values.
     *
     * @param fValue input cube to be extracted (may be negative)
     * @return cube root of fValue
     * @see #cube(float)
     * @see com.jme3.math.FastMath#pow(float,float)
     * @see java.lang.Math#cbrt(double)
     */
    public static float cubeRoot(float fValue) {
        double dValue = fValue;
        float result = (float) Math.cbrt(dValue);

        return result;
    }

    /**
     * Compute the discriminant (b^2 - 4*a*c) of a quadratic equation in
     * standard form (a*x^2 + b*x + c).
     *
     * @param a coefficient of the square term
     * @param b coefficient of the linear term
     * @param c constant term
     * @return discriminant
     */
    public static double discriminant(double a, double b, double c) {
        double result = b * b - 4.0 * a * c;
        return result;
    }

    /**
     * Fade polynomial for Perlin noise. Double precision arithmetic is used to
     * reduce rounding error.
     *
     * @param t input value (&le;1, &ge;0)
     * @return 6*t^5 - 15*t^4 + 10*t^3 (&le;1, &ge;0)
     */
    public static float fade(float t) {
        Validate.fraction(t, "t");

        double tt = (double) t;
        double ff = tt * tt * tt * (10.0 + tt * (-15.0 + 6.0 * tt));
        float result = (float) ff;

        assert result >= 0f : result;
        assert result <= 1f : result;
        return result;
    }

    /**
     * Find the first true element of the input array.
     *
     * @param array input (not null)
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
     * Compute the hypotenuse of a right triangle using the Pythagorean Theorem.
     * This method accepts negative arguments.
     *
     * @param legA length of the 1st leg (may be negative)
     * @param legB length of the 2nd leg (may be negative)
     * @return length of the hypotenuse (&ge;0)
     * @see #sumOfSquares(float,float)
     * @see java.lang.Math#hypot(double, double)
     */
    public static float hypotenuse(float legA, float legB) {
        double sumSquares = sumOfSquares(legA, legB);
        float result = (float) Math.sqrt(sumSquares);

        assert result >= 0f : result;
        return result;
    }

    /**
     * Test whether an integer value is odd.
     *
     * @param iValue input value to be tested
     * @return true if x is odd, false if it's even
     */
    public static boolean isOdd(int iValue) {
        boolean result = (iValue % 2) != 0;
        return result;
    }

    /**
     * Find the maximum of three single-precision values.
     *
     * @param a 1st input value
     * @param b 2nd input value
     * @param c 3rd input value
     * @return greatest of the three values
     * @see java.lang.Math#max(float, float)
     */
    public static float max(float a, float b, float c) {
        if (a >= b && a >= c) {
            return a;
        } else if (b >= c) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Find the maximum of three double-precision values.
     *
     * @param a 1st input value
     * @param b 2nd input value
     * @param c 3rd input value
     * @return greatest of the three values
     * @see java.lang.Math#max(double, double)
     */
    public static double max(double a, double b, double c) {
        if (a >= b && a >= c) {
            return a;
        } else if (b >= c) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Find the minimum of three single-precision values.
     *
     * @param a 1st input value
     * @param b 2nd input value
     * @param c 3rd input value
     * @return least of the three values
     * @see java.lang.Math#min(float, float)
     */
    public static float min(float a, float b, float c) {
        if (a <= b && a <= c) {
            return a;
        } else if (b <= c) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Find the minimum of three double-precision values.
     *
     * @param a 1st input value
     * @param b 2nd input value
     * @param c 3rd input value
     * @return least of the three values
     * @see java.lang.Math#min(double, double)
     */
    public static double min(double a, double b, double c) {
        if (a <= b && a <= c) {
            return a;
        } else if (b <= c) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Compute the least non-negative value congruent with an integer value with
     * respect to the specified modulus. modulo() differs from remainder for
     * negative values of the first argument. For instance, modulo(-1, 4) == 3,
     * while -1 % 4 == -1.
     *
     * @param iValue input value
     * @param modulus (&gt;0)
     * @return iValue MOD modulus (&lt;modulus, &ge;0)
     */
    public static int modulo(int iValue, int modulus) {
        Validate.positive(modulus, "modulus");

        int result = (iValue % modulus + modulus) % modulus;

        assert result >= 0f : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Compute the least non-negative value congruent with a single-precision
     * value with respect to the specified modulus. modulo() differs from
     * remainder for negative values of the first argument. For instance,
     * modulo(-1f, 4f) == 3f, while -1f % 4f == -1f.
     *
     * @param fValue input value
     * @param modulus (&gt;0)
     * @return fValue MOD modulus (&lt;modulus, &ge;0)
     */
    public static float modulo(float fValue, float modulus) {
        Validate.positive(modulus, "modulus");

        float result = (fValue % modulus + modulus) % modulus;

        assert result >= 0f : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Compute the least non-negative value congruent with a double-precision
     * value with respect to the specified modulus.
     *
     * @param dValue input value
     * @param modulus (&gt;0)
     * @return x MOD modulus (&lt;modulus, &ge;0)
     */
    public static double modulo(double dValue, double modulus) {
        Validate.positive(modulus, "modulus");

        double result = (dValue % modulus + modulus) % modulus;

        assert result >= 0.0 : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Square the specified double-precision value. Logs a warning in case of
     * overflow.
     *
     * @param dValue input value to be squared
     * @return dValue squared (&ge;0)
     * @see com.jme3.math.FastMath#sqr(float)
     * @see Math#sqrt(double)
     */
    public static double sqr(double dValue) {
        double result = dValue * dValue;

        if (Double.isInfinite(result)) {
            String message = String.format(
                    "Overflow from squaring %g.", dValue);
            logger.warning(message);
        }
        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Standardize a rotation angle to the range [-Pi, Pi).
     *
     * @param angle input (in radians)
     * @return standardized angle (in radians, &lt;Pi, &ge;-Pi)
     */
    public static float standardizeAngle(float angle) {
        float result = modulo(angle, FastMath.TWO_PI);
        if (result >= FastMath.PI) {
            result -= FastMath.TWO_PI;
        }

        assert result >= -FastMath.PI : result;
        assert result < FastMath.PI : result;
        return result;
    }

    /**
     * Compute the sum-of-squares of two single-precision values. Double
     * precision arithmetic is used to reduce the risk of overflow.
     *
     * @param firstValue 1st input value
     * @param secondValue 2nd input value
     * @return sum of squares (&ge;0)
     */
    public static double sumOfSquares(float firstValue, float secondValue) {
        double x = (double) firstValue;
        double y = (double) secondValue;
        double result = x * x + y * y;

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Convert an angle from radians to degrees.
     *
     * @param radians input angle
     * @return equivalent in degrees
     * @see java.lang.Math#toDegrees(double)
     */
    public static float toDegrees(float radians) {
        float result = radians * FastMath.RAD_TO_DEG;
        return result;
    }

    /**
     * Convert an angle from degrees to radians.
     *
     * @param degrees input angle
     * @return equivalent in radians
     * @see java.lang.Math#toRadians(double)
     */
    public static float toRadians(float degrees) {
        float result = degrees * FastMath.DEG_TO_RAD;
        return result;
    }
    // *************************************************************************
    // test cases

    /**
     * Console application to test the MyMath class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        PrintStream console = System.out;
        console.print("Test results for class MyMath:\n\n");

        float h = hypotenuse(3f, 4f);
        console.printf("hypot(3,4) = %f%n", h);

        float[] floatCases = new float[]{
            -3f, 0f, 1f, 8f, Float.MAX_VALUE / 2f, Float.MAX_VALUE, -1f
        };
        for (float x : floatCases) {
            console.println();

            h = hypotenuse(x, x);
            console.printf("x = %g    hypot(x,x) = %g%n", x, h);

            float c = cube(x);
            float cr = cubeRoot(x);
            console.printf("  cube(x)=%g    cubeRoot(x)=%g%n", c, cr);

            float x1 = cube(cr);
            console.printf("  cube(cubeRoot(x)) = %g%n", x1);

            float x2 = cubeRoot(c);
            console.printf("  cubeRoot(cube(x)) = %g%n", x2);

            console.printf("  x %% 4 = %f    x mod 4 = %f%n",
                    x % 4f, modulo(x, 4f));
        }
        console.println();
    }
}

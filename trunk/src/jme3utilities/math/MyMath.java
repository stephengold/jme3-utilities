/*
 Copyright (c) 2013-2014, Stephen Gold
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
import com.jme3.math.Vector2f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Mathematical utility methods. Aside from test cases, all methods should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
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
    final private static Logger logger =
            Logger.getLogger(MyMath.class.getName());
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
        if (!(abscissa >= -1.0 && abscissa <= 1.0)) {
            logger.log(Level.SEVERE, "abscissa={0}", abscissa);
            throw new IllegalArgumentException(
                    "abscissa should be between -1 and 1, inclusive");
        }

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
        if (!(abscissa >= -1f && abscissa <= 1f)) {
            logger.log(Level.SEVERE, "abscissa={0}", abscissa);
            throw new IllegalArgumentException(
                    "abscissa should be between -1 and 1, inclusive");
        }

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
     * @see FastMath#clamp(float,float,float)
     */
    public static float clamp(float fValue, float maxMagnitude) {
        Validate.nonNegative(maxMagnitude, "limit");

        return FastMath.clamp(fValue, -maxMagnitude, maxMagnitude);
    }

    /**
     * Clamp a value to be between 0 and 1, inclusive.
     *
     * @param fValue input value to be clamped
     * @return value between 0 and 1 inclusive which is closest to fValue
     * @see FastMath#clamp(float,float,float)
     */
    public static float clampFraction(float fValue) {
        float result = FastMath.clamp(fValue, 0f, 1f);

        assert result >= 0f : result;
        assert result <= 1f : result;
        return result;
    }

    /**
     * Compute the product of two complex numbers or the composition of two
     * scale-and-rotate vectors.
     *
     * @param a 1st complex number (not null, not altered)
     * @param b 2nd complex number (not null, not altered)
     * @return new vector
     */
    public static Vector2f complexProduct(Vector2f a, Vector2f b) {
        Validate.nonNull(a, "vector a");
        Validate.nonNull(b, "vector b");

        float productX = a.x * b.x - a.y * b.y;
        float productY = a.y * b.x + a.x * b.y;
        return new Vector2f(productX, productY);
    }

    /**
     * Cube a single-precision value.
     *
     * @param fValue input value to be cubed
     * @return fValue raised to the third power
     * @see #cubeRoot(float)
     */
    public static float cube(float fValue) {
        return fValue * fValue * fValue;
    }

    /**
     * Extract the cube root of a single-precision value. Unlike FastMath.pow(),
     * this method works on negative values.
     *
     * @param fValue input cube to be extracted (may be negative)
     * @return cube root of fValue
     * @see #cube(float)
     * @see FastMath#pow(float,float)
     * @see Math#cbrt(double)
     */
    public static float cubeRoot(float fValue) {
        float magnitude = FastMath.abs(fValue);
        float exponent = FastMath.ONE_THIRD;
        float rootMagnitude = FastMath.pow(magnitude, exponent);
        float result = FastMath.copysign(rootMagnitude, fValue);
        return result;
    }

    /**
     * Compute the discriminant (b^2 - 4*a*c) of a quadratic equation in
     * standard form: (a*x^2 + b*x + c).
     *
     * @param a coefficient of the square term
     * @param b coefficient of the linear term
     * @param c constant term
     */
    public static double discriminant(double a, double b, double c) {
        double result = b * b - 4.0 * a * c;

        return result;
    }

    /**
     * Compute the discriminant (b^2 - 4*a*c) of a quadratic equation in
     * standard form: (a*x^2 + b*x + c). Double precision arithmetic is used to
     * reduce the risk of overflow.
     *
     * @param a coefficient of the square term
     * @param b coefficient of the linear term
     * @param c constant term
     */
    public static double discriminant(float a, float b, float c) {
        double aa = (double) a;
        double bb = (double) b;
        double cc = (double) c;
        double result = discriminant(aa, bb, cc);

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
     * Compute the hypotenuse of a right triangle using the Pythagorean Theorem.
     * This method accepts negative arguments.
     *
     * @param legA length of the 1st leg (may be negative)
     * @param legB length of the 2nd leg (may be negative)
     * @return length of the hypotenuse (&ge;0)
     * @see #sumOfSquares(float,float)
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
     * Test whether a vector's length is within 1% of unity.
     */
    public static boolean isUnitVector(Vector2f vector) {
        float norm = vector.length();
        return 0.99f < norm && norm < 1.01f;
    }

    /**
     * Find the max of three single-precision values.
     *
     * @param a 1st input value
     * @param b 2nd input value
     * @param c 3rd input value
     * @return greatest of the three values
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
     * Compute the least non-negative value congruent with an integer value with
     * respect to the specified modulus.
     *
     * @param iValue input value
     * @param modulus (&gt;0)
     * @return x MOD modulus (&lt;modulus, &ge;0)
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
     * value with respect to the specified modulus.
     *
     * @param fValue input value
     * @param modulus (&gt;0)
     * @return x MOD modulus (&lt;modulus, &ge;0)
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
        if (!(modulus > 0.0)) {
            logger.log(Level.SEVERE, "modulus={0}", modulus);
            throw new IllegalArgumentException("modulus should be positive");
        }

        double result = (dValue % modulus + modulus) % modulus;

        assert result >= 0.0 : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Compute the sphere function sqrt(1 - x^2 - y^2) for single-precision
     * values. Double precision is used to reduce the risk of overflow.
     *
     * @param x 1st coordinate (&le;1, &ge;-1)
     * @param y 2nd coordinate (&le;1, &ge;-1)
     * @return positive height of the unit sphere at the coordinates (&le;1,
     * &ge;0)
     */
    public static float sphere(float x, float y) {
        if (!(x >= -1f && x <= 1f)) {
            logger.log(Level.SEVERE, "x={0}", x);
            throw new IllegalArgumentException(
                    "x should be between -1 and 1, inclusive");
        }
        if (!(y >= -1f && y <= 1f)) {
            logger.log(Level.SEVERE, "y={0}", y);
            throw new IllegalArgumentException(
                    "y should be between -1 and 1, inclusive");
        }

        double rSquared = sumOfSquares(x, y);
        if (rSquared >= 1.0) {
            return 0f;
        }

        float z = (float) Math.sqrt(1.0 - rSquared);
        assert z >= 0f : z;
        assert z <= 1f : z;
        return z;
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
    // *************************************************************************
    // test cases

    /**
     * Console application to test the MyMath class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyMath:\n\n");

        Vector2f[] v2Cases = new Vector2f[]{new Vector2f(0f, 0f),
            new Vector2f(1f, 0f),
            new Vector2f(0f, 1f),
            new Vector2f(0.6f, -0.8f)
        };

        for (Vector2f x : v2Cases) {
            for (Vector2f y : v2Cases) {
                System.out.printf("%s * %s = %s%n", x, y, complexProduct(x, y));
            }
            System.out.println();
        }

        float h = hypotenuse(3f, 4f);
        System.out.printf("hypot(3,4)=%f%n", h);

        float[] floatCases = new float[]{
            -3f, 0f, 1f, 8f, Float.MAX_VALUE / 2f, Float.MAX_VALUE
        };
        for (float x : floatCases) {
            float c = cube(x);
            float cr = cubeRoot(x);

            System.out.println();
            h = hypotenuse(x, x);
            System.out.printf("x=%e  hypot(x,x)=%e%n", x, h);
            System.out.printf("  cube(x)=%e  cubeRoot(x)=%e%n", c, cr);
            System.out.printf("  cube(cubeRoot(x))=%e  cubeRoot(cube(x))=%e%n",
                    cube(cr), cubeRoot(c));
        }
        System.out.println();
    }
}
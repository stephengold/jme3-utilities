// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import com.jme3.math.FastMath;
import java.util.Random;
import java.util.logging.Logger;

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
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyMath.class.getName());
    // *************************************************************************
    // fields
    /**
     * pseudo-random generator
     */
    final private static Random generator = new Random();
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
     * Calculate the circle function sqrt(1 - x^2) for a single-precision value.
     * Double precision is used to reduce the risk of overflow.
     *
     * @param abscissa (<=1, >=-1)
     * @return the positive ordinate of the unit circle at the abscissa (<=1,
     * >=0)
     */
    public static float circle(float abscissa) {
        if (abscissa < -1f || abscissa > 1f) {
            throw new IllegalArgumentException(
                    "abscissa must lie between -1 and 1, inclusive");
        }
        double x = (double) abscissa;
        float y = (float) Math.sqrt(1.0 - x * x);
        assert y >= 0f : y;
        assert y <= 1f : y;
        return y;
    }

    /**
     * Clamp the magnitude of a single-precision value.
     *
     * @param fValue value to be clamped
     * @param maxMagnitude limit of the clamp (>=0)
     * @return value between -maxMagnitude and +maxMagnitude inclusive which is
     * closest to fValue
     * @see FastMath#clamp(float,float,float)
     */
    public static float clamp(float fValue, float maxMagnitude) {
        if (maxMagnitude < 0f) {
            throw new IllegalArgumentException("limit cannot be negative");
        }
        return FastMath.clamp(fValue, -maxMagnitude, maxMagnitude);
    }

    /**
     * Clamp a value to lie between 0 and 1.
     *
     * @param fValue value to be clamped
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
     * Cube a single-precision value.
     *
     * @param fValue value to be cubed
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
     * @param fValue cube to be extracted (may be negative)
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
     * Calculate the discriminant (b^2 - 4*a*c) of a standard quadratic equation
     * (a*x^2 + b*x + c). Double precision is used to reduce the risk of
     * overflow.
     *
     * @param a coefficient of the square term
     * @param b coefficient of the linear term
     * @param c constant term
     */
    public static double discriminant(float a, float b, float c) {
        double aa = (double) a;
        double bb = (double) b;
        double cc = (double) c;
        double result = bb * bb - 4.0 * aa * cc;

        return result;
    }

    /**
     * Calculate the hypotenuse of a right triangle using the Pythagorean
     * Theorem. This method accepts negative arguments.
     *
     * @param legA length of the first leg (may be negative)
     * @param legB length of the second leg (may be negative)
     * @return length of the hypotenuse (>=0)
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
     * @param iValue value to be tested
     * @return true if x is odd, false if it's even
     */
    public static boolean isOdd(int iValue) {
        boolean result = (iValue % 2) != 0;
        return result;
    }

    /**
     * Find the max of three single-precision values.
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
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
     * Calculate the least non-negative value congruent with a single-precision
     * value with respect to a given modulus.
     *
     * @param fValue which value
     * @param modulus (>0)
     * @return x MOD modulus (<modulus, >=0)
     */
    public static float modulo(float fValue, float modulus) {
        if (modulus <= 0f) {
            throw new IllegalArgumentException("modulus must be positive");
        }

        float result = (fValue % modulus + modulus) % modulus;

        assert result >= 0f : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Calculate the least non-negative value congruent with a double-precision
     * value with respect to a given modulus.
     *
     * @param dValue which value
     * @param modulus (>0)
     * @return x MOD modulus (<modulus, >=0)
     */
    public static double modulo(double dValue, double modulus) {
        if (modulus <= 0.0) {
            throw new IllegalArgumentException("modulus must be positive");
        }

        double result = (dValue % modulus + modulus) % modulus;

        assert result >= 0.0 : result;
        assert result < modulus : result;
        return result;
    }

    /**
     * Get the next pseudo-random single-precision value.
     */
    public static float nextFloat() {
        return generator.nextFloat();
    }

    /**
     * Re-seed the pseudo-random generator.
     *
     * @param newSeed
     */
    public static void reseedGenerator(long newSeed) {
        generator.setSeed(newSeed);
    }

    /**
     * Calculate the sphere function sqrt(1 - x^2 - y^2) for single-precision
     * values. Double precision is used to reduce the risk of overflow.
     *
     * @param x coordinate (<=1, >=-1)
     * @param y coordinate (<=1, >=-1)
     * @return the positive height of the unit sphere at the coordinates (<=1,
     * >=0)
     */
    public static float sphere(float x, float y) {
        double rSquared = sumOfSquares(x, y);
        if (rSquared > 1.0) {
            throw new IllegalArgumentException(
                    "(x,y) must lie on or within the unit circle");
        }

        float z = (float) Math.sqrt(1.0 - rSquared);
        assert z >= 0f : z;
        assert z <= 1f : z;
        return z;
    }

    /**
     * Standardize a rotation angle to the range [-PI, PI).
     *
     * @param angle (in radians)
     * @return a standard angle (in radians)
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
     * Calculate the sum-of-squares of two single-precision values. Double
     * precision is used to reduce the risk of overflow.
     *
     * @param firstValue
     * @param secondValue
     * @return sum of squares (>=0)
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
     * Test cases for this class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyMath:\n\n");

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
            System.out.printf("x=%e  hypot(x,x)=%e%n",
                    x, h);
            System.out.printf("  cube(x)=%e  cubeRoot(x)=%e%n",
                    c, cr);
            System.out.printf("  cube(cubeRoot(x))=%e  cubeRoot(cube(x))=%e%n",
                    cube(cr), cubeRoot(c));
        }
        System.out.println();
    }
}
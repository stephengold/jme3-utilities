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
package jme3utilities.math.test;

import java.io.PrintStream;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * Test cases for the MyMath class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMyMath {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestMyMath.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the MyMath class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        PrintStream console = System.out;
        console.print("Test results for class MyMath:\n\n");

        float h = MyMath.hypotenuse(3f, 4f);
        console.printf("hypot(3,4) = %f%n", h);

        float[] floatCases = new float[]{
            -3f, 0f, 1f, 8f, Float.MAX_VALUE / 2f, Float.MAX_VALUE, -1f
        };
        for (float x : floatCases) {
            console.println();

            h = MyMath.hypotenuse(x, x);
            console.printf("x = %g    hypot(x,x) = %g%n", x, h);

            float c = MyMath.cube(x);
            float cr = MyMath.cubeRoot(x);
            console.printf("  cube(x)=%g    cubeRoot(x)=%g%n", c, cr);

            float x1 = MyMath.cube(cr);
            console.printf("  cube(cubeRoot(x)) = %g%n", x1);

            float x2 = MyMath.cubeRoot(c);
            console.printf("  cubeRoot(cube(x)) = %g%n", x2);

            console.printf("  x %% 4 = %f    x mod 4 = %f%n",
                    x % 4f, MyMath.modulo(x, 4f));
        }
        console.println();
    }
}

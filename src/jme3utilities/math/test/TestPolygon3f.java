/*
 Copyright (c) 2017, Stephen Gold
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
package jme3utilities.math.test;

import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.math.Polygon3f;

/**
 * Simple application to test the Polygon3f class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestPolygon3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestPolygon3f.class.getName());

    /**
     * Console application to test the MyVector3f class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.WARNING);
        System.out.printf("Test results for class Polygon3f:%n%n");

        /*
         * degenerate test cases
         */
        int numTestCases = 13;
        Vector3f[][] degenerateCase = new Vector3f[numTestCases][];

        degenerateCase[0] = new Vector3f[]{}; // null

        degenerateCase[1] = new Vector3f[]{ // 1-gon
            new Vector3f(1f, 2f, 3f)
        };

        degenerateCase[2] = new Vector3f[]{ // duplicative 2-gon
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 0f)
        };

        degenerateCase[3] = new Vector3f[]{ // small 2-gon
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9f, 4.1f, 11.9f)
        };

        degenerateCase[4] = new Vector3f[]{ // duplicative triangle
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 1f),
            new Vector3f(3f, 4f, 0f)
        };

        degenerateCase[5] = new Vector3f[]{ // small collinear triangle
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9f, 4.1f, 11.9f),
            new Vector3f(2.8f, 4.2f, 11.8f)
        };

        degenerateCase[6] = new Vector3f[]{ // lopsided collinear triangle
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9999f, 4.0001f, 11.9999f),
            new Vector3f(0f, 0f, 0f)
        };

        degenerateCase[7] = new Vector3f[]{ // small collinear quad
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9f, 4.1f, 11.9f),
            new Vector3f(2.8f, 4.2f, 11.8f),
            new Vector3f(2.7f, 4.3f, 11.7f)
        };

        degenerateCase[8] = new Vector3f[]{ // duplicative quad
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 1f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(3f, 4f, 0f)
        };

        degenerateCase[9] = new Vector3f[]{ // horizontal quad with 180
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        degenerateCase[10] = new Vector3f[]{ // pent with 0-degree angle
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        degenerateCase[11] = new Vector3f[]{ // non-planar hexagon with 180s
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };

        degenerateCase[12] = new Vector3f[]{ // non-planar hexagon with dup
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };
        /*
         * non-degenerate test cases
         */
        numTestCases = 4;
        Vector3f[][] nonDegenerateCase = new Vector3f[numTestCases][];

        nonDegenerateCase[0] = new Vector3f[]{ // 3-4-5 triangle
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(4f, 12f, 9f)
        };

        nonDegenerateCase[1] = new Vector3f[]{ // square in Y-Z plane
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(0f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        nonDegenerateCase[2] = new Vector3f[]{ // skewed quad
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(1f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        nonDegenerateCase[3] = new Vector3f[]{ // long side close to origin
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 0f, 0f)
        };

        Vector3f[] theCase;
        Polygon3f poly;

        for (int caseIndex = 0; caseIndex < numTestCases; caseIndex++) {
            System.out.printf("degenerate case %d:%n", caseIndex);
            theCase = degenerateCase[caseIndex];
            poly = new Polygon3f(theCase, 0.001f);
            assert poly.isDegenerate();
            tryMethods(poly);
        }

        for (int caseIndex = 0; caseIndex < numTestCases; caseIndex++) {
            System.out.printf("non-degenerate case %d:%n", caseIndex);
            theCase = nonDegenerateCase[caseIndex];
            poly = new Polygon3f(theCase, 0.001f);
            assert !poly.isDegenerate();
            tryMethods(poly);
        }

        System.out.printf("Success.%n");
    }

    static void tryMethods(Polygon3f poly) {
        System.out.printf(" numCorners = %d:%n",
                poly.numCorners());
        Vector3f[] array = poly.copyCornerLocations();
        for (int i = 0; i < array.length; i++) {
            System.out.printf("  corners[%d] = %s%n",
                    i, array[i].toString());
        }
        System.out.printf(" %s planar%n",
                poly.isPlanar() ? "is" : "isn't");
        System.out.printf(" corner closest to origin = %d%n",
                poly.findCorner(Vector3f.ZERO));
        System.out.printf(" perimeter = %f%n",
                poly.perimeter());

        Vector3f tempVector = new Vector3f();
        int tempIndex = poly.findEdge(Vector3f.ZERO, tempVector);
        System.out.printf(" edge closest to origin = %d", tempIndex);
        if (tempIndex >= 0) {
            System.out.printf(" at %s", tempVector.toString());
        }
        System.out.printf("%n%n");
    }
}
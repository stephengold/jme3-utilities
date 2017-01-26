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
import jme3utilities.math.polygon.GenericPolygon3f;
import jme3utilities.math.polygon.Polygon3f;

/**
 * Simple application to test the Polygon3f and GenericPolygon3f classes.
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
        Vector3f[][] degenerateCase = new Vector3f[14][];

        degenerateCase[0] = new Vector3f[]{}; // null

        degenerateCase[1] = new Vector3f[]{ // 1-gon
            new Vector3f(1f, 2f, 3f)
        };

        degenerateCase[2] = new Vector3f[]{ // duplicative 2-gon
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 0f)
        };

        degenerateCase[3] = new Vector3f[]{ // distinct 2-gon
            new Vector3f(1f, 4f, 9f),
            new Vector3f(1f, 9f, 4f)
        };

        degenerateCase[4] = new Vector3f[]{ // duplicative triangle
            new Vector3f(3f, 4f, -1f),
            new Vector3f(3f, 4f, 2f),
            new Vector3f(3f, 4f, -1f)
        };

        degenerateCase[5] = new Vector3f[]{ // near-duplicative triangle
            new Vector3f(3f, 2f, 0.0005f),
            new Vector3f(3f, 2f, 1f),
            new Vector3f(3f, 2f, 0f)
        };

        degenerateCase[6] = new Vector3f[]{ // small collinear triangle
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9f, 4.1f, 11.9f),
            new Vector3f(2.8f, 4.2f, 11.8f)
        };

        degenerateCase[7] = new Vector3f[]{ // lopsided collinear triangle
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9999f, 4.0001f, 11.9999f),
            new Vector3f(0f, 0f, 0f)
        };

        degenerateCase[8] = new Vector3f[]{ // small collinear quad
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.9f, 4.1f, 11.9f),
            new Vector3f(2.8f, 4.2f, 11.8f),
            new Vector3f(2.7f, 4.3f, 11.7f)
        };

        degenerateCase[9] = new Vector3f[]{ // duplicative quad
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 1f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(3f, 4f, 0f)
        };

        degenerateCase[10] = new Vector3f[]{ // horizontal quad with 180
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        degenerateCase[11] = new Vector3f[]{ // pent with 0-degree angle
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        degenerateCase[12] = new Vector3f[]{ // non-planar hexagon with 180s
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };

        degenerateCase[13] = new Vector3f[]{ // non-planar hexagon with dup
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
        Vector3f[][] genericCase = new Vector3f[4][];

        genericCase[0] = new Vector3f[]{ // 3-4-5 triangle
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(4f, 12f, 9f)
        };

        genericCase[1] = new Vector3f[]{ // square in Y-Z plane
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(0f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        genericCase[2] = new Vector3f[]{ // skewed quad
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(1f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        genericCase[3] = new Vector3f[]{ // long side close to origin
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 0f, 0f)
        };

        Vector3f[] theCase;
        Polygon3f poly;

        for (int caseI = 0; caseI < degenerateCase.length; caseI++) {
            System.out.printf("degenerate test case #%d:%n", caseI);
            theCase = degenerateCase[caseI];
            poly = new Polygon3f(theCase, 0.01f);
            assert poly.isDegenerate();
            tryMethods(poly);
        }

        for (int caseI = 0; caseI < genericCase.length; caseI++) {
            System.out.printf("generic test case #%d:%n", caseI);
            theCase = genericCase[caseI];
            poly = new GenericPolygon3f(theCase, 0.01f);
            tryMethods(poly);
        }

        System.out.printf("Success.%n");
    }

    static void tryMethods(Polygon3f poly) {
        System.out.printf(" numCorners = %d:%n",
                poly.numCorners());
        Vector3f[] array = poly.copyCornerLocations();
        for (int i = 0; i < array.length; i++) {
            System.out.printf("  corner%d at %s%n",
                    i, array[i].toString());
        }
        System.out.printf(" %s planar,",
                poly.isPlanar() ? "is" : "isn't");
        System.out.printf(" perimeter = %f%n",
                poly.perimeter());

        System.out.print(" corner closest to origin = ");
        int ccto = poly.findCorner(Vector3f.ZERO);
        if (ccto == -1) {
            System.out.printf("n/a%n");
        } else {
            float distance = array[ccto].length();
            System.out.printf("corner%d, %f wu%n", ccto, distance);
        }

        System.out.print(" side closest to origin = ");
        Vector3f tempVector = new Vector3f();
        int scto = poly.findSide(Vector3f.ZERO, tempVector);
        if (scto == -1) {
            System.out.printf("n/a");
        } else {
            float distance = tempVector.length();
            System.out.printf("side%d at %s, %f wu",
                    scto, tempVector.toString(), distance);
        }
        System.out.printf("%n%n");
    }
}

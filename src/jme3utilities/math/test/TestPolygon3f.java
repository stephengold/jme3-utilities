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
         * degenerate planar test cases
         */
        Vector3f[][] degenerateCase = new Vector3f[12][];

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

        degenerateCase[9] = new Vector3f[]{ // duplicative quad in Y-Z plane
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

        degenerateCase[11] = new Vector3f[]{ // horizontal pent w/0-degree angle
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };
        /*
         * degenerate non-planar test cases
         */
        Vector3f[][] dnpCase = new Vector3f[2][];

        dnpCase[0] = new Vector3f[]{ // non-planar hexagon with 180s
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };

        dnpCase[1] = new Vector3f[]{ // non-planar hexagon with dup
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };
        /*
         * generic planar test cases
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

        genericCase[2] = new Vector3f[]{ // long side close to origin
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 0f, 0f)
        };

        genericCase[3] = new Vector3f[]{ // self-intersecting quad
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 9f, 9f),
            new Vector3f(2f, -9f, -9f)
        };
        /*
         * generic non-planar test cases
         */
        Vector3f[][] gnpCase = new Vector3f[1][];

        gnpCase[0] = new Vector3f[]{ // skewed quad
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(1f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        Vector3f[] theCase;
        Polygon3f poly;

        for (int caseI = 0; caseI < degenerateCase.length; caseI++) {
            System.out.printf("degenerate/planar test case #%d:%n", caseI);
            theCase = degenerateCase[caseI];
            poly = new Polygon3f(theCase, 0.01f);
            assert poly.isDegenerate();
            assert poly.isPlanar();
            tryMethods(poly);
        }

        for (int caseI = 0; caseI < dnpCase.length; caseI++) {
            System.out.printf("degenerate/non-planar test case #%d:%n", caseI);
            theCase = dnpCase[caseI];
            poly = new Polygon3f(theCase, 0.01f);
            assert poly.isDegenerate();
            assert !poly.isPlanar();
            tryMethods(poly);
        }

        for (int caseI = 0; caseI < genericCase.length; caseI++) {
            System.out.printf("generic/planar test case #%d:%n", caseI);
            theCase = genericCase[caseI];
            poly = new GenericPolygon3f(theCase, 0.01f);
            assert poly.isPlanar();
            tryMethods(poly);
        }

        for (int caseI = 0; caseI < gnpCase.length; caseI++) {
            System.out.printf("generic/non-planar test case #%d:%n", caseI);
            theCase = gnpCase[caseI];
            poly = new GenericPolygon3f(theCase, 0.01f);
            assert !poly.isPlanar();
            tryMethods(poly);
        }

        System.out.printf("Success.%n");
    }

    static void tryMethods(Polygon3f poly) {
        GenericPolygon3f generic = null;
        if (poly instanceof GenericPolygon3f) {
            generic = (GenericPolygon3f) poly;
        }

        int numCorners = poly.numCorners();
        System.out.printf(" numCorners = %d:%n", numCorners);

        Vector3f[] array = poly.copyCornerLocations();
        assert array.length == numCorners : array.length;
        for (int i = 0; i < numCorners; i++) {
            System.out.printf("  corner%d at %s%n", i, array[i].toString());
        }
        for (int i = 0; i < numCorners; i++) {
            System.out.printf("  side%d: %f wu%n", i, poly.sideLength(i));
        }

        System.out.printf(" %s degenerate,",
                poly.isDegenerate() ? "IS" : "is NOT");
        if (generic != null) {
            System.out.printf(" %s self-intersecting,",
                    generic.isSelfIntersecting() ? "IS" : "is NOT");
        }
        System.out.printf(" %s planar,", poly.isPlanar() ? "IS" : "is NOT");
        System.out.printf(" perimeter = %f%n", poly.perimeter());

        System.out.print(" shortest side = ");
        int shortI = poly.findShortest();
        if (shortI == -1) {
            System.out.printf("n/a%n");
        } else {
            float length = poly.sideLength(shortI);
            System.out.printf("side%d, %f wu%n", shortI, length);
        }

        System.out.print(" longest side = ");
        int longI = poly.findLongest();
        if (shortI == -1) {
            System.out.printf("n/a%n");
        } else {
            float length = poly.sideLength(longI);
            System.out.printf("side%d, %f wu%n", longI, length);
        }

        System.out.print(" largest triangle = ");
        int tri[] = poly.largestTriangle();
        if (tri == null) {
            System.out.printf("n/a%n");
        } else {
            System.out.printf("%d-%d-%d%n", tri[0], tri[1], tri[2]);
        }

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

        if (poly.numCorners() > 3) {
            Polygon3f sub = poly.fromRange(numCorners - 1, 1);
            System.out.printf("triangle %d-0-1:%n", numCorners - 1);
            assert sub.numCorners() == 3 : sub.numCorners();
            tryMethods(sub);
        }
    }
}

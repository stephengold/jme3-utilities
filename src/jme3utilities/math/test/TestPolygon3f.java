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
import jme3utilities.math.MyMath;
import jme3utilities.math.VectorXZ;
import jme3utilities.math.polygon.GenericPolygon3f;
import jme3utilities.math.polygon.Polygon3f;
import jme3utilities.math.polygon.SimplePolygon3f;

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
    // *************************************************************************
    // fields

    /**
     * test cases for degenerate planar polygons
     */
    private static Vector3f[][] degenerateCase;
    /**
     * test cases for degenerate non-planar polygons
     */
    private static Vector3f[][] dnpCase;
    /**
     * test cases for generic planar polygons
     */
    private static Vector3f[][] genericCase;
    /**
     * test cases for generic non-planar polygons
     */
    private static Vector3f[][] gnpCase;
    /**
     * test cases for simple polygons
     */
    private static Vector3f[][] simpleCase;
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the MyVector3f class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.WARNING);
        System.out.printf("Test results for Polygon3f, GenericPolygon3f, "
                + "and SimplePolygon3f:%n%n");

        initialize();
        runAll();

        System.out.printf("Success.%n");
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize all test cases.
     */
    private static void initialize() {
        /*
         * degenerate planar test cases
         */
        degenerateCase = new Vector3f[11][];

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
            new Vector3f(5f, 2f, 1f),
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

        degenerateCase[10] = new Vector3f[]{ // duplicative pent in X-Z plane
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        /*
         * degenerate non-planar test cases
         */
        dnpCase = new Vector3f[2][];

        dnpCase[0] = new Vector3f[]{ // non-planar hexagon with dup corner
            new Vector3f(1f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };

        dnpCase[1] = new Vector3f[]{ // non-planar pentagon with 180
            new Vector3f(1f, 2f, 1f),
            new Vector3f(1f, 2f, 3f),
            new Vector3f(1f, 2f, 2f),
            new Vector3f(-2f, 2f, 2f),
            new Vector3f(-1f, 3f, 1f)
        };

        /*
         * generic planar test cases
         */
        genericCase = new Vector3f[2][];

        genericCase[0] = new Vector3f[]{ // self-intersecting quad
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 9f, 9f),
            new Vector3f(2f, -9f, -9f)
        };

        genericCase[1] = new Vector3f[]{ // chevron with clipped point
            new Vector3f(1f, 0f, 2f),
            new Vector3f(0f, 0f, 0f),
            new Vector3f(1f, 0f, -2f),
            new Vector3f(0f, 0f, -1f),
            new Vector3f(0f, 0f, 1f)
        };

        /*
         * generic non-planar test cases
         */
        gnpCase = new Vector3f[2][];

        gnpCase[0] = new Vector3f[]{ // skewed quad
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(1f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        gnpCase[1] = new Vector3f[]{ // non-planar hexagon with redundant corner
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(3f, 5f, 1f),
            new Vector3f(2f, 4f, 1f),
            new Vector3f(1f, 4f, 1f)
        };

        /*
         * simple test cases
         */
        simpleCase = new Vector3f[6][];

        simpleCase[0] = new Vector3f[]{ // 3-4-5 triangle in X-Y plane
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(4f, 12f, 9f)
        };

        simpleCase[1] = new Vector3f[]{ // square in Y-Z plane
            new Vector3f(0f, 9f, 9f),
            new Vector3f(0f, 12f, 9f),
            new Vector3f(0f, 12f, 6f),
            new Vector3f(0f, 9f, 6f)
        };

        simpleCase[2] = new Vector3f[]{ // horizontal concave chevron
            new Vector3f(1f, 0f, 2f),
            new Vector3f(0f, 0f, 0f),
            new Vector3f(1f, 0f, -2f),
            new Vector3f(-1f, 0f, 0f)
        };

        simpleCase[3] = new Vector3f[]{ // long base passes origin
            new Vector3f(1f, 9f, 9f),
            new Vector3f(1f, -9f, -9f),
            new Vector3f(2f, 0f, 0f)
        };

        simpleCase[4] = new Vector3f[]{ // horizontal quad with 180
            new Vector3f(1f, 4f, 0f),
            new Vector3f(2f, 4f, 0f),
            new Vector3f(3f, 4f, 0f),
            new Vector3f(2f, 4f, 1f)
        };

        simpleCase[5] = new Vector3f[]{ // four-pointed star
            new Vector3f(1f, 2f, 0f),
            new Vector3f(0f, 6f, 0f),
            new Vector3f(-1f, 2f, 0f),
            new Vector3f(-5f, 1f, 0f),
            new Vector3f(-1f, 0f, 0f),
            new Vector3f(0f, -4f, 0f),
            new Vector3f(1f, 0f, 0f),
            new Vector3f(5f, 1f, 0f)
        };
    }

    /**
     * Run all the tests on all the test cases.
     */
    private static void runAll() {
        Polygon3f poly;

        for (int caseI = 0; caseI < degenerateCase.length; caseI++) {
            System.out.printf("degenerate/planar test case #%d:%n", caseI);
            poly = new Polygon3f(degenerateCase[caseI], 0.01f);
            assert poly.isDegenerate() : caseI;
            assert poly.isPlanar() : caseI;
            testPolygon(poly);
        }

        for (int caseI = 0; caseI < dnpCase.length; caseI++) {
            System.out.printf("degenerate/non-planar test case #%d:%n", caseI);
            poly = new Polygon3f(dnpCase[caseI], 0.01f);
            assert poly.isDegenerate();
            assert !poly.isPlanar();
            testPolygon(poly);
        }

        for (int caseI = 0; caseI < genericCase.length; caseI++) {
            System.out.printf("generic/planar test case #%d:%n", caseI);
            poly = new GenericPolygon3f(genericCase[caseI], 0.01f);
            assert poly.isPlanar();
            testPolygon(poly);
        }

        for (int caseI = 0; caseI < gnpCase.length; caseI++) {
            System.out.printf("generic/non-planar test case #%d:%n", caseI);
            poly = new GenericPolygon3f(gnpCase[caseI], 0.01f);
            assert !poly.isPlanar();
            testPolygon(poly);
        }

        for (int caseI = 0; caseI < simpleCase.length; caseI++) {
            System.out.printf("simple test case #%d:%n", caseI);
            poly = new SimplePolygon3f(simpleCase[caseI], 0.01f);
            testPolygon(poly);
        }
    }

    /**
     * Run all tests on a specific case.
     *
     * @param poly which polygon to test (no null)
     */
    private static void testPolygon(Polygon3f poly) {
        GenericPolygon3f generic = null;
        if (poly instanceof GenericPolygon3f) {
            generic = (GenericPolygon3f) poly;
        }
        SimplePolygon3f simple = null;
        if (poly instanceof SimplePolygon3f) {
            simple = (SimplePolygon3f) poly;
        }
        /*
         * Describe the polygon in terms of corners and sides.
         */
        int numCorners = poly.numCorners();
        System.out.printf(" numCorners = %d:%n", numCorners);

        Vector3f[] array = poly.copyCornerLocations();
        assert array.length == numCorners : array.length;
        float turnSum = 0f;
        for (int cornerI = 0; cornerI < numCorners; cornerI++) {
            assert poly.onCorner(array[cornerI], cornerI);
            System.out.printf("  C%d at %s",
                    cornerI, array[cornerI].toString());
            if (simple == null) {
                double turn = poly.absTurnAngle(cornerI);
                if (!Double.isNaN(turn)) {
                    System.out.printf(": turn +/- %.1f degrees",
                            Math.toDegrees(turn));
                }
            } else {
                assert simple.inPlane(array[cornerI]);
                double interior = simple.interiorAngle(cornerI);
                double turn = simple.turnAngle(cornerI);
                turnSum += turn;
                System.out.printf(
                        ": turn %.1f degrees, %.1f-degree internal angle",
                        Math.toDegrees(turn), Math.toDegrees(interior));
                VectorXZ xz = simple.planarOffset(cornerI);
                System.out.printf(", xz = %s", xz.toString());
            }
            System.out.println();
        }
        if (simple != null) {
            System.out.printf(" sum of turns = %.1f degrees%n",
                    MyMath.toDegrees(turnSum));
        }

        for (int sideI = 0; sideI < numCorners; sideI++) {
            Vector3f midpoint = poly.midpoint(sideI);
            assert poly.onSide(midpoint, sideI);
            System.out.printf("  S%d is %f wu in length, midpoint at %s.%n",
                    sideI, poly.sideLength(sideI), midpoint);
        }
        /*
         * Classify the polygon as degenerate, planar, etc.
         */
        System.out.printf(" This polygon");
        if (simple != null) {
            System.out.printf(" %s convex,",
                    simple.isConvex() ? "IS" : "is NOT");
        }
        System.out.printf(" %s degenerate,",
                poly.isDegenerate() ? "IS" : "is NOT");
        if (generic != null) {
            System.out.printf(" %s self-intersecting,",
                    generic.isSelfIntersecting() ? "IS" : "is NOT");
        }
        System.out.printf(" %s planar.%n", poly.isPlanar() ? "IS" : "is NOT");
        /*
         * Calculate the perimeter, diameter, and shortest/longest side.
         */
        System.out.printf(" The perimeter is %f wu.%n", poly.perimeter());
        System.out.printf(" The diameter is %f wu.%n", poly.diameter());
        System.out.print(" The shortest side is ");
        int shortI = poly.findShortest();
        if (shortI == -1) {
            System.out.printf("n/a.%n");
        } else {
            float length = poly.sideLength(shortI);
            System.out.printf("S%d (%f wu).%n", shortI, length);
        }
        System.out.print(" The longest side is ");
        int longI = poly.findLongest();
        if (shortI == -1) {
            System.out.printf("n/a%n");
        } else {
            float length = poly.sideLength(longI);
            System.out.printf("S%d (%f wu).%n", longI, length);
        }
        /*
         * Find the largest triangle.
         */
        System.out.print(" The largest triangle is: ");
        int tri[] = poly.largestTriangle();
        if (tri == null) {
            System.out.printf("n/a.%n");
        } else {
            System.out.printf("%d-%d-%d.%n", tri[0], tri[1], tri[2]);
        }
        /*
         * Test with the origin and the centroid.
         */
        testPoint(Vector3f.ZERO, "the origin", poly);
        if (simple != null) {
            Vector3f centroid = simple.centroid();
            assert simple.inPlane(centroid);
            testPoint(centroid, "the centroid", poly);
        }
        /*
         * Calculate the area.
         */
        if (simple != null) {
            System.out.printf(" area = %f wu^2%n", simple.area());
        }

        System.out.println();
        if (poly.numCorners() > 3) {
            Polygon3f sub = poly.fromRange(numCorners - 1, 1);
            System.out.printf("triangle %d-0-1:%n", numCorners - 1);
            assert sub.numCorners() == 3 : sub.numCorners();
            testPolygon(sub);
            System.out.println();
        }
    }

    private static void testPoint(Vector3f point, String name, Polygon3f poly) {
        assert point != null;
        assert name != null;
        assert poly != null;

        System.out.printf(" For %s at %s:%n", name, point.toString());
        if (poly instanceof SimplePolygon3f) {
            SimplePolygon3f simple = (SimplePolygon3f) poly;
            System.out.printf("  %s in the plane,",
                    simple.inPlane(point) ? "IS" : "is NOT");
            System.out.printf(" %s inside the polygon.%n",
                    simple.contains(point) ? "IS" : "is NOT");
        }

        System.out.print("  Its closest corner is ");
        int cornerIndex = poly.findCorner(point);
        if (cornerIndex == -1) {
            System.out.print("n/a.");
        } else {
            double sd = poly.squaredDistanceToCorner(point, cornerIndex);
            System.out.printf("C%d, %f wu away.", cornerIndex, Math.sqrt(sd));
        }
        System.out.println();

        System.out.print("  Its closest side is ");
        Vector3f storage = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
        int sideIndex = poly.findSide(point, storage);
        if (sideIndex == -1) {
            System.out.print("n/a.");
        } else {
            System.out.printf("S%d at ", sideIndex);
            int corner = poly.onCorner(storage);
            if (corner == -1) {
                System.out.print(storage.toString());
            } else {
                System.out.printf("C%d", corner);
            }
            float distance = point.distance(storage);
            System.out.printf(", %f wu away.", distance);
        }
        System.out.println();
    }
}

/*
 Copyright (c) 2013-2019, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;

/**
 * Test cases for the MyVector3f class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMyVector3f {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestMyVector3f.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the MyVector3f class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.print("Test results for class MyVector3f:\n\n");

        // vector test cases
        Vector3f[] vectorCases = new Vector3f[]{
            new Vector3f(3f, 4f, 12f),
            new Vector3f(2.5f, 4.5f, 11.5f),
            Vector3f.ZERO,
            Vector3f.UNIT_X,
            Vector3f.UNIT_Z
        };

        System.out.println("Testing yRotate():");
        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            System.out.printf(" yRotate(v, 0) = %s%n",
                    MyVector3f.yRotate(v, 0).toString());
            System.out.printf(" yRotate(v, PI/2) = %s%n",
                    MyVector3f.yRotate(v, FastMath.HALF_PI).toString());
            System.out.printf(" yRotate(v, PI) = %s%n",
                    MyVector3f.yRotate(v, FastMath.PI).toString());
            System.out.printf(" yRotate(v, 2*PI) = %s%n",
                    MyVector3f.yRotate(v, FastMath.TWO_PI).toString());
            System.out.println();
        }
        System.out.println();

        System.out.println("Testing projection():");
        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            for (Vector3f w : vectorCases) {
                System.out.printf(" w = %s%n", w.toString());
                if (w.length() != 0f) {
                    System.out.printf("  v proj w = %s%n",
                            MyVector3f.projection(v, w, null).toString());
                }
                //System.out.printf("             %s%n",
                //        v.project(w).toString());
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Testing doCoincide() with tolerance2 = 0.9:");
        for (Vector3f v : vectorCases) {
            System.out.printf("v = %s%n", v.toString());
            for (Vector3f w : vectorCases) {
                System.out.printf(" w = %s%n", w.toString());
                if (MyVector3f.doCoincide(v, w, 0.9f)) {
                    System.out.printf("  v coincides with w%n");
                } else {
                    System.out.printf("  v does not coincide with w%n");
                }
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Testing areCollinear():");
        Vector3f p1 = new Vector3f(1f, 2f, 3f);
        Vector3f p2 = new Vector3f(2f, 0f, 5f);
        Vector3f p3 = new Vector3f(4f, -4f, 9f);
        Vector3f p2bad = new Vector3f(2f, 1f, 5f);

        assert MyVector3f.areCollinear(p1, p1, p1, 0.01f);
        assert MyVector3f.areCollinear(p1, p2, p3, 0.01f);
        assert MyVector3f.areCollinear(p1, p2, p2, 0.01f);
        assert !MyVector3f.areCollinear(p1, p2bad, p3, 0.01f);

        System.out.println("Success!");
    }
}

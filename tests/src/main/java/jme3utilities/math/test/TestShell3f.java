/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.locus.Metric;
import jme3utilities.math.locus.Shell3f;
import jme3utilities.math.noise.Generator;

/**
 * Test cases for the Shell3f class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class TestShell3f {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestShell3f.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the Shell3f class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.printf("Test results for class Shell3f:%n%n");

        float r1;
        float r2;
        float r3;
        float x;
        float y;
        float z;
        Shell3f hole;
        Shell3f shell;
        Shell3f solid;
        Vector3f center;
        Vector3f location;

        Generator random = new Generator(395_782L);

        float[] scales = {0.1f, 1f, 10f, 10000f, 1e10f, 0.0001f, 1e-10f};

        for (float centerScale : scales) {
            for (float radiusScale : scales) {
                for (float locScale : scales) {
                    for (int i = 0; i < 10_000; ++i) {
                        center = random.nextUnitVector3f();
                        center.multLocal(centerScale);

                        x = random.nextFloat() * locScale;
                        y = random.nextFloat() * locScale;
                        z = random.nextFloat() * locScale;
                        location = new Vector3f(x, y, z);
                        location.addLocal(center);

                        r1 = random.nextFloat() * radiusScale;
                        r2 = random.nextFloat() * radiusScale;
                        r3 = random.nextFloat() * radiusScale;
                        if (r1 == 0f || r2 == 0f || r3 == 0f) {
                            continue;
                        }

                        hole = new Shell3f(center, r1, Float.POSITIVE_INFINITY);
                        hole.findLocation(location);
                        hole.findLocation(center);

                        solid = new Shell3f(Metric.EUCLID, center, r1, r2, r3);
                        solid.findLocation(location);
                        solid.findLocation(center);

                        float inner = Math.min(r1, r2);
                        float outer = Math.max(r1, r2);
                        double cm = Metric.CHEBYSHEV.value(center);
                        if (outer - inner < cm * 1e-6) {
                            continue;
                        }
                        shell = new Shell3f(center, inner, outer);
                        shell.findLocation(location);
                        shell.findLocation(center);
                    }
                }
            }
        }
    }
}

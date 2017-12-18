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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;

/**
 * Test cases for the VectorXZ class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestVectorXZ {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestVectorXZ.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Console application to test the VectorXZ class.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        System.out.printf("Test results for class VectorXZ:%n%n");

        // vector test cases
        VectorXZ[] cases = new VectorXZ[4];
        cases[0] = VectorXZ.east;
        cases[1] = new VectorXZ(1f, 1f);
        cases[2] = VectorXZ.west;
        cases[3] = VectorXZ.zero;

        for (VectorXZ vin : cases) {
            float a = vin.azimuth();
            VectorXZ vout = new VectorXZ(a);

            System.out.printf(
                    "vin = %s  azimuth(x)=%f (%f degrees)  vout = %s%n",
                    vin.toString(), a, MyMath.toDegrees(a),
                    vout.toString());
            System.out.println();

            Vector3f v3 = new Vector3f(1f, 2f, 3f);
            VectorXZ vxz = new VectorXZ(v3);
            ReadXZ r1 = vin.normalize().mult(vxz);

            Quaternion q1 = vin.toQuaternion();
            VectorXZ r2 = new VectorXZ(q1.mult(v3));

            Quaternion q2 = new Quaternion();
            q2.fromAngleNormalAxis(-a, new Vector3f(0f, 1f, 0f));
            VectorXZ r3 = new VectorXZ(q2.mult(v3));

            System.out.printf("vin=%s  r1=%s, r2=%s, r3=%s%n",
                    vin.toString(), r1.toString(), r2.toString(),
                    r3.toString());
            System.out.println();
        }
        System.out.println();
    }
}

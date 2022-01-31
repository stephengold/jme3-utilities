/*
 Copyright (c) 2017-2022, Stephen Gold
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
package jme3utilities.math.locus;

import com.jme3.math.Vector3f;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Enumerate the metrics available in Shell3f.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum Metric {
    // *************************************************************************
    // values

    /**
     * Euclidean metric (sqrt(x^2 + y^2)) for spherical and ellipsoidal shapes
     */
    EUCLID,
    /**
     * Chebyshev metric (max{abs{x},abs{y}}) for cubic and rectangular shapes
     */
    CHEBYSHEV,
    /**
     * Manhattan metric (abs{x}+abs{y}) for octahedral shapes
     */
    MANHATTAN;
    // *************************************************************************
    // new methods exposed

    /**
     * Look up the textual description of this metric.
     *
     * @return a string of text or "?" if not known
     */
    public String describe() {
        switch (this) {
            case EUCLID:
                return "Euclidean";
            case CHEBYSHEV:
                return "Chebyshev";
            case MANHATTAN:
                return "Manhattan";
        }
        return "?";
    }

    /**
     * Select a metric based on its textual description.
     *
     * @param description text returned by #describe()
     * @return enum value, or for an invalid input
     */
    public static Metric fromDescription(String description) {
        for (Metric mode : values()) {
            if (mode.describe().equals(description)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * Calculate the squared value of this metric for a specified 3-D offset.
     *
     * @param offset input vector (not null, unaffected)
     * @return squared metric value (&ge;0)
     */
    public double squaredValue(Vector3f offset) {
        Validate.nonNull(offset, "offset");
        double result;

        switch (this) {
            case EUCLID:
                result = MyVector3f.lengthSquared(offset);
                break;

            case CHEBYSHEV:
                double dx = offset.x;
                double dy = offset.y;
                double dz = offset.z;
                result = MyMath.maxDouble(dx * dx, dy * dy, dz * dz);
                break;

            case MANHATTAN:
                dx = Math.abs(offset.x);
                dy = Math.abs(offset.y);
                dz = Math.abs(offset.z);
                double sum = dx + dy + dz;
                result = sum * sum;
                break;

            default:
                throw new IllegalStateException(this.toString());
        }

        return result;
    }

    /**
     * Evaluate this metric for a specified 3-D offset.
     *
     * @param offset input vector (not null, unaffected)
     * @return metric value (&ge;0)
     */
    public double value(Vector3f offset) {
        Validate.nonNull(offset, "offset");
        double result;

        switch (this) {
            case EUCLID:
                result = MyVector3f.lengthSquared(offset);
                result = Math.sqrt(result);
                break;

            case CHEBYSHEV:
                double dx = Math.abs(offset.x);
                double dy = Math.abs(offset.y);
                double dz = Math.abs(offset.z);
                result = MyMath.maxDouble(dx, dy, dz);
                break;

            case MANHATTAN:
                dx = Math.abs(offset.x);
                dy = Math.abs(offset.y);
                dz = Math.abs(offset.z);
                result = dx + dy + dz;
                break;

            default:
                throw new IllegalStateException(this.toString());
        }

        return result;
    }
}

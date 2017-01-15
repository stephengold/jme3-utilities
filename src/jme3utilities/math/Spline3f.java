/*
 Copyright (c) 2014-2017, Stephen Gold
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
package jme3utilities.math;

import com.jme3.math.Vector3f;

/**
 * Interface for three-dimensional spline interpolation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface Spline3f {

    /**
     * Copy the specified control point of this spline.
     *
     * @param index index of the control point (&ge;0)
     * @return new vector
     */
    Vector3f getControlPoint(int index);

    /**
     * Interpolate the spline at the specified parameter value.
     *
     * @param sampleT input value
     * @return new vector
     */
    Vector3f interpolate(float sampleT);

    /**
     * Read the number of control points.
     *
     * @return count (&gt;0)
     */
    int numControlPoints();

    /**
     * Compute the spline's 1st derivative to the right of the specified
     * parameter value.
     *
     * @param sampleT input value
     * @return new vector
     */
    Vector3f rightDerivative(float sampleT);

    /**
     * Read the total path length of this spline.
     *
     * @return path length from start to end (&ge;0)
     */
    float totalLength();
}
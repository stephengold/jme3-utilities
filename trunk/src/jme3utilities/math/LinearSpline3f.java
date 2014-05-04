/*
 Copyright (c) 2014, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Immutable linear spline in three dimensions.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class LinearSpline3f
        implements Spline3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(LinearSpline3f.class.getName());
    // *************************************************************************
    // fields
    /**
     * location of each control point (size&ge;2)
     */
    final private List<Vector3f> controlPoints = new ArrayList<>(2);
    /**
     * parameter value for each control point (size&ge;2)
     */
    final private List<Float> controlTs = new ArrayList<>(2);
    /**
     * total path length (&ge;0)
     */
    final private float totalLength;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new spline using the specified control points. The 1st
     * control point will be the value at t=0. t increases with Euclidean
     * distance along the path.
     *
     * @param points control points (not null, totalLength&ge;2, unaffected)
     */
    public LinearSpline3f(Vector3f[] points) {
        Validate.nonNull(points, "control points");
        if (points.length < 2) {
            logger.log(Level.SEVERE, "length={0}", points.length);
            throw new IllegalArgumentException(
                    "should provide at least 2 control points");
        }

        float sumDistance = 0f;
        Vector3f previousPoint = null;
        for (Vector3f point : points) {
            if (previousPoint != null) {
                float distance = previousPoint.distance(point);
                if (distance > 0f) {
                    sumDistance += distance;
                    controlPoints.add(point);
                    controlTs.add(sumDistance);
                }
            } else {
                controlPoints.add(point);
                controlTs.add(sumDistance);
            }
            previousPoint = point;
        }
        totalLength = sumDistance;

        assert controlPoints.size() == controlTs.size() : controlTs.size();
    }
    // *************************************************************************
    // Spline3f methods

    /**
     * Copy the specified control point of this spline.
     *
     * @param index index of the control point (&ge;0)
     * @return new vector
     */
    @Override
    public Vector3f getControlPoint(int index) {
        Validate.nonNegative(index, "index");
        Vector3f result = controlPoints.get(index);
        return result.clone();
    }

    /**
     * Interpolate the spline at the specified parameter value.
     *
     * @param sampleT input value
     * @return new vector
     */
    @Override
    public Vector3f interpolate(float sampleT) {
        if (sampleT <= controlTs.get(0)) {
            Vector3f controlPoint = controlPoints.get(0);
            return controlPoint.clone();
        }

        int lastIndex = controlPoints.size() - 1;
        if (sampleT >= totalLength) {
            Vector3f controlPoint = controlPoints.get(lastIndex);
            return controlPoint.clone();
        }

        int leftIndex = leftIndex(sampleT);
        assert leftIndex >= 0 : leftIndex;
        assert leftIndex < lastIndex : leftIndex;

        float t0 = controlTs.get(leftIndex);
        assert sampleT >= t0 : sampleT;
        Vector3f p0 = controlPoints.get(leftIndex);
        if (sampleT == t0) {
            return p0.clone();
        }

        int rightIndex = leftIndex + 1;
        float t1 = controlTs.get(rightIndex);
        assert t1 > t0 : t1;
        assert sampleT < t1 : sampleT;
        float fraction = (sampleT - t0) / (t1 - t0);
        assert fraction >= 0f : fraction;
        assert fraction < 1f : fraction;

        Vector3f p1 = controlPoints.get(rightIndex);
        Vector3f result = FastMath.interpolateLinear(fraction, p0, p1, null);

        return result;
    }

    /**
     * Read the number of control points.
     *
     * @return count (&gt;0)
     */
    @Override
    public int numControlPoints() {
        int result = controlPoints.size();
        return result;
    }

    /**
     * Compute the spline's 1st derivative to the right of the specified
     * parameter value.
     *
     * @param sampleT input value
     * @return new vector
     */
    @Override
    public Vector3f rightDerivative(float sampleT) {
        if (sampleT < controlTs.get(0) || sampleT >= totalLength) {
            return new Vector3f();
        }

        int leftIndex = leftIndex(sampleT);
        assert leftIndex >= 0 : leftIndex;
        int lastIndex = controlPoints.size() - 1;
        assert leftIndex < lastIndex : leftIndex;

        Vector3f p0 = controlPoints.get(leftIndex);
        float t0 = controlTs.get(leftIndex);
        int rightIndex = leftIndex + 1;
        Vector3f p1 = controlPoints.get(rightIndex);
        float t1 = controlTs.get(rightIndex);
        Vector3f offset = p1.subtract(p0);
        float dt = t1 - t0;
        assert dt > 0f : dt;
        Vector3f result = offset.divide(dt);

        return result;
    }

    /**
     * Read the total path length of this spline.
     *
     * @return path length from start to end (&ge;0)
     */
    @Override
    public float totalLength() {
        assert totalLength >= 0f : totalLength;
        return totalLength;
    }
    // *************************************************************************
    // private methods

    /**
     * Find the index of the control point at or to the left of a given
     * coordinate.
     *
     * @param sampleT sample coordinate
     * @return index (&ge;0, &lt;numPoints) or -1 if sampleT&lt;0
     */
    private int leftIndex(float sampleT) {
        if (sampleT < 0f) {
            return -1;
        }
        /*
         * TODO: use binary search
         */
        int numPoints = controlTs.size();
        for (int index = 1; index < numPoints; index++) {
            float controlT = controlTs.get(index);
            if (sampleT < controlT) {
                return index - 1;
            }
        }
        return numPoints - 1;
    }
}
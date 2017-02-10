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
package jme3utilities.math.locus;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.spline.LinearSpline3f;
import jme3utilities.math.spline.Spline3f;

/**
 * An immutable line segment in three-dimensional space, consisting of 2 corners
 * (points) connected by a straight-line segment.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Segment3f implements Locus3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Segment3f.class.getName());
    // *************************************************************************
    // fields

    /**
     * maximum distance for points to coincide (&ge;0, set by constructor)
     */
    final protected float tolerance;
    /**
     * square of the tolerance (&ge;0, set by constructor)
     */
    final protected float tolerance2;
    /**
     * locations of the corners (not null or containing any nulls, initialized
     * by constructor)
     */
    final protected Vector3f[] cornerLocations;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a segment with the specified corners.
     *
     * @param corner0 location of 1st corner (not null, unaffected)
     * @param corner1 location of 2nd corner (not null, unaffected)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     */
    public Segment3f(Vector3f corner0, Vector3f corner1,
            float compareTolerance) {
        Validate.nonNull(corner0, "1st corner");
        Validate.nonNull(corner1, "2nd corner");
        Validate.nonNegative(compareTolerance, "compare tolerance");
        /*
         * Allocate array space for caching values.
         */
        cornerLocations = new Vector3f[2];
        cornerLocations[0] = corner0;
        cornerLocations[1] = corner1;
        /*
         * Set compare tolerancess.
         */
        tolerance = compareTolerance;
        tolerance2 = tolerance * tolerance;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the location of a specified corner.
     *
     * @param cornerIndex which corner (0 or 1)
     * @return a new vector
     */
    public Vector3f copyCornerLocation(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");
        Vector3f result = cornerLocations[cornerIndex].clone();
        return result;
    }

    /**
     * Copy the array of corner locations.
     *
     * @return a new array of new vectors
     */
    public Vector3f[] copyCornerLocations() {
        Vector3f[] result = new Vector3f[2];
        result[0] = cornerLocations[0].clone();
        result[1] = cornerLocations[1].clone();

        return result;
    }

    /**
     * Test whether the corners coincide.
     *
     * @return true if they coincide, otherwise false
     */
    public boolean doCoincide() {
        Vector3f corner0 = cornerLocations[0];
        Vector3f corner1 = cornerLocations[1];
        double squaredDistance = MyVector3f.distanceSquared(corner0, corner1);
        if (squaredDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Find the corner nearest to a specified location.
     *
     * @param location input coordinates (not null, unaffected)
     *
     * @return index of the nearest corner (0 or 1)
     */
    public int findCorner(Vector3f location) {
        Vector3f corner0 = cornerLocations[0];
        double sd0 = MyVector3f.distanceSquared(location, corner0);
        Vector3f corner1 = cornerLocations[1];
        double sd1 = MyVector3f.distanceSquared(location, corner1);

        if (sd0 > sd1) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Read the tolerance.
     *
     * @return tolerance (&ge; 0)
     */
    public float getTolerance() {
        return tolerance;
    }

    /**
     * Calculate the length of this segment: the distance between its corners.
     *
     * @return diameter squared (in squared world units, &ge;0)
     */
    public float length() {
        Vector3f corner0 = cornerLocations[0];
        Vector3f corner1 = cornerLocations[1];
        float result = corner0.distance(corner1);

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Find a corner with which a given point coincides.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @return index of the coincident corner, or -1 if none coincide
     */
    public int onCorner(Vector3f point) {
        Validate.nonNull(point, "point");

        for (int cornerIndex = 0; cornerIndex < 2; cornerIndex++) {
            if (onCorner(point, cornerIndex)) {
                return cornerIndex;
            }
        }
        return -1;
    }

    /**
     * Test whether a point coincides with a specific corner.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param cornerIndex index of the corner (0 or 1)
     * @return true if the point coincides, false if it doesn't
     */
    public boolean onCorner(Vector3f point, int cornerIndex) {
        Validate.nonNull(point, "point");
        validateIndex(cornerIndex, "corner index");

        Vector3f corner = cornerLocations[cornerIndex];
        boolean result = MyVector3f.doCoincide(corner, point, tolerance2);

        return result;
    }

    /**
     * Test whether this segment shares one or more corners with another
     * segment.
     *
     * @param other the other segment (not null)
     * @param storeCornerMap if not null, used to store indices of matching
     * corners
     * @return true if one or more shared corners were found, otherwise false
     */
    public boolean sharesCornerWith(Segment3f other,
            Map<Integer, Integer> storeCornerMap) {
        Validate.nonNull(other, "other segment");

        if (storeCornerMap != null) {
            storeCornerMap.clear();
        }

        float tol = other.getTolerance();
        float tol2 = (tolerance2 + tol * tol) / 2f;

        boolean result = false;
        for (int otherI = 0; otherI < 2; otherI++) {
            Vector3f otherCorner = other.copyCornerLocation(otherI);
            for (int thisI = 0; thisI < 2; thisI++) {
                Vector3f thisCorner = cornerLocations[thisI];
                if (MyVector3f.doCoincide(otherCorner, thisCorner, tol2)) {
                    result = true;
                    if (storeCornerMap != null) {
                        storeCornerMap.put(thisI, otherI);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Calculate the squared distance from a point to a specific corner of this
     * segment.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param cornerIndex index of the corner (0 or 1)
     * @return squared distance from point to corner (&ge;0)
     */
    public double squaredDistanceToCorner(Vector3f point, int cornerIndex) {
        Validate.nonNull(point, "point");
        validateIndex(cornerIndex, "corner index");

        Vector3f corner = cornerLocations[cornerIndex];
        double result = MyVector3f.distanceSquared(corner, point);

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Calculate the squared distance from a point to this segment.
     *
     * @param point coordinates of the input (not null, unaffected)
     * @param storeClosestPoint if not null, used to store the coordinates of
     * the closest point
     * @return squared distance from point to segment (&ge;0)
     */
    public double squaredDistance(Vector3f point, Vector3f storeClosestPoint) {
        Validate.nonNull(point, "point");
        /*
         * Calculate the direction of a straight line containing the segment.
         */
        Vector3f corner0 = cornerLocations[0];
        Vector3f corner1 = cornerLocations[1];
        Vector3f segmentOffset = corner1.subtract(corner0);
        /*
         * If the segment has zero length, return the squared 
         * distance to corner0.
         */
        double segmentDS = MyVector3f.distanceSquared(corner0, corner1);
        if (segmentDS == 0.0) {
            if (storeClosestPoint != null) {
                storeClosestPoint.set(corner0);
            }
            double result = MyVector3f.distanceSquared(corner0, point);
            return result;
        }
        /*
         * Calculate parametric value for the closest point on that line.
         */
        Vector3f pointOffset = point.subtract(corner0);
        double dot = MyVector3f.dot(pointOffset, segmentOffset);
        double t = dot / segmentDS;
        /*
         * Calculate offset of the closest point on the side.
         */
        float scaleFactor = FastMath.clamp((float) t, 0f, 1f);
        Vector3f closestOffset = segmentOffset.mult(scaleFactor);
        if (storeClosestPoint != null) {
            storeClosestPoint.set(corner0);
            storeClosestPoint.addLocal(closestOffset);
        }
        double result = MyVector3f.distanceSquared(closestOffset, pointOffset);

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Validate a corner index as a method argument.
     *
     * @param index index to validate (&gt;0)
     * @param description description of the index (not null)
     * @throws IllegalArgumentException if the value is &lt;0 or &ge;2
     */
    public static void validateIndex(int index, String description) {
        Validate.inRange(index, description, 0, 1);
    }
    // *************************************************************************
    // Locus3f methods

    /**
     * Calculate the centroid of this region. The centroid need not be contained
     * in the region, but it should be relatively near all locations that are.
     *
     * @return a new coordinate vector
     */
    @Override
    public Vector3f centroid() {
        Vector3f corner0 = cornerLocations[0];
        Vector3f corner1 = cornerLocations[1];
        Vector3f midpoint = MyVector3f.midpoint(corner0, corner1);

        return midpoint;
    }

    /**
     * Test whether this region contains a specified location.
     *
     * @param location coordinates of location to test (not null, unaffected)
     * @return true if location is in this region, false otherwise
     */
    @Override
    public boolean contains(Vector3f location) {
        Validate.nonNull(location, "location");

        double squaredDistance = squaredDistance(location, null);
        if (squaredDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Find the location in this region nearest to a specified location.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return a new vector, or null it none found
     */
    @Override
    public Vector3f findLocation(Vector3f location) {
        Validate.nonNull(location, "location");

        Vector3f closestLocation = new Vector3f();
        squaredDistance(location, closestLocation);

        return closestLocation;
    }

    /**
     * Calculate a representative location (or rep) for this region. The rep
     * must be contained in the region.
     *
     * @return a new coordinate vector, or null if none found
     */
    @Override
    public Vector3f rep() {
        Vector3f result = centroid();
        assert contains(result) : result;
        return result;
    }

    /**
     * Score a location based on how well it "fits" with this region.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return score value (more positive &rarr; better)
     */
    @Override
    public double score(Vector3f location) {
        Validate.nonNull(location, "location");

        Vector3f closestLocation = new Vector3f();
        double distanceSquared = squaredDistance(location, closestLocation);

        return -distanceSquared;
    }

    /**
     * Find a path between two locations in this region without leaving the
     * region. Short paths are preferred over long ones.
     *
     * @param startLocation coordinates (contained in region, unaffected)
     * @param goalLocation coordinates (contained in region, unaffected)
     * @return a new path spline, or null if none found
     */
    @Override
    public Spline3f shortestPath(Vector3f startLocation,
            Vector3f goalLocation) {
        assert contains(startLocation) : startLocation;
        assert contains(goalLocation) : goalLocation;

        Vector3f[] joints = {startLocation, goalLocation};
        Spline3f result = new LinearSpline3f(joints);

        return result;
    }

    /**
     * Calculate the distance from the specified starting point to the first
     * point of support (if any) directly below it in this region.
     *
     * @param location coordinates of starting point(not null, unaffected)
     * @param cosineTolerance cosine of maximum slope for support (&gt;0, &lt;1)
     * @return the shortest support distance (&ge;0) or
     * {@link Float#POSITIVE_INFINITY} if no support
     */
    @Override
    public float supportDistance(Vector3f location, float cosineTolerance) {
        Validate.nonNull(location, "location");
        Validate.fraction(cosineTolerance, "cosine tolerance");
        throw new UnsupportedOperationException(); // TODO
    }
}

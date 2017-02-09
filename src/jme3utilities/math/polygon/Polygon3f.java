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
package jme3utilities.math.polygon;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * An immutable polygon in three-dimensional space, consisting of N corners
 * (points) connected by N sides (straight-line segments). It may be degenerate
 * and/or planar, though it need not be. For efficiency, many calculated values
 * are cached.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Polygon3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Polygon3f.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, then there are fewer than 3 corners and/or there are coincident
     * corners and/or there is a 180-degree turn (set by #setIsDegenerate())
     */
    private Boolean isDegenerate = null;
    /**
     * if true, then all corners (and sides) lie in a single plane (set by
     * #setIsPlanar())
     */
    private Boolean isPlanar = null;
    /**
     * cached dot product at each corner (allocated by constructor; initialized
     * by #setCorner())
     */
    final private Double[] dotProducts;
    /**
     * cached squared distance between each pair of corners (each &ge;0,
     * allocated by constructor, each initialized by #setSquaredDistance())
     */
    final private Double[][] squaredDistances;
    /**
     * maximum distance for points to coincide (&ge;0, set by constructor)
     */
    final protected float tolerance;
    /**
     * square of the tolerance (&ge;0, set by constructor)
     */
    final protected float tolerance2;
    /**
     * the number of corners, which is also the number of sides (&ge;0, set by
     * constructor)
     */
    final protected int numCorners;
    /**
     * cached vector of corner indices which define the largest triangle (each
     * &ge;0 and &lt;numCorners, in ascending order, set by
     * #setLargestTriangle())
     */
    private int[] largestTriangle = null;
    /**
     * locations of all corners, in sequence (not null or containing any nulls,
     * initialized by constructor)
     */
    final protected Vector3f[] cornerLocations;
    /**
     * cached cross product at each corner (allocated by constructor;
     * initialized by #setCorner())
     */
    final private Vector3f[] crossProducts;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a polygon from an array of corners.
     *
     * @param cornerArray locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     */
    public Polygon3f(Vector3f[] cornerArray, float compareTolerance) {
        Validate.nonNull(cornerArray, "corner array");
        numCorners = cornerArray.length;
        for (int index = 0; index < numCorners; index++) {
            String description = String.format("cornerList[%d]", index);
            Validate.nonNull(cornerArray[index], description);
        }
        Validate.nonNegative(compareTolerance, "compare tolerance");

        /*
         * Allocate array space for caching values.
         */
        cornerLocations = new Vector3f[numCorners];
        crossProducts = new Vector3f[numCorners];
        dotProducts = new Double[numCorners];
        squaredDistances = new Double[numCorners][numCorners];
        /*
         * Copy corner locations.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            cornerLocations[cornerIndex] = cornerArray[cornerIndex].clone();
        }
        /*
         * Set compare tolerancess.
         */
        tolerance = compareTolerance;
        tolerance2 = tolerance * tolerance;
    }

    /**
     * Instantiate a polygon from a list of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     */
    public Polygon3f(List<Vector3f> cornerList, float compareTolerance) {
        Validate.nonNull(cornerList, "corner list");
        numCorners = cornerList.size();
        for (int index = 0; index < numCorners; index++) {
            String description = String.format("cornerList[%d]", index);
            Validate.nonNull(cornerList.get(index), description);
        }
        Validate.nonNegative(compareTolerance, "compare tolerance");
        /*
         * Allocate array space for caching values.
         */
        cornerLocations = new Vector3f[numCorners];
        crossProducts = new Vector3f[numCorners];
        dotProducts = new Double[numCorners];
        squaredDistances = new Double[numCorners][numCorners];
        /*
         * Copy corner locations.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            cornerLocations[cornerIndex] = cornerList.get(cornerIndex).clone();
        }
        /*
         * Set compare tolerances.
         */
        tolerance = compareTolerance;
        tolerance2 = tolerance * tolerance;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the location of a specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
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
        Vector3f[] result = new Vector3f[numCorners];

        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            result[cornerIndex] = cornerLocations[cornerIndex].clone();
        }

        return result;
    }

    /**
     * Calculate the magnitude of the turn angle at the specified corner. The
     * turn angle is sometimes called the "external angle".
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return magnitude of the angle (&ge;0, &le;Pi) or NaN if an adjacent side
     * has zero length
     */
    public double absTurnAngle(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");

        int nextI = nextIndex(cornerIndex);
        int prevI = prevIndex(cornerIndex);
        double lsPrevious = squaredDistance(prevI, cornerIndex);
        double lsCurrent = squaredDistance(cornerIndex, nextI);
        double lsProduct = lsPrevious * lsCurrent;
        if (lsProduct == 0.0) {
            return Double.NaN;
        }

        double dot = dotProduct(cornerIndex);
        double cosAngle = dot / Math.sqrt(lsProduct);
        if (cosAngle > 1.0) {
            assert false;
        }
        assert cosAngle >= -1.0 : cosAngle;
        assert cosAngle <= 1.0 : cosAngle;

        double result = Math.acos(cosAngle);

        assert result >= 0.0 : result;
        assert result <= Math.PI : result;
        return result;
    }

    /**
     * Calculate (or look up) the cross product of successive sides which meet
     * at the specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return cross product (not null)
     */
    public Vector3f crossProduct(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");

        if (crossProducts[cornerIndex] == null) {
            setCorner(cornerIndex);
        }
        Vector3f result = crossProducts[cornerIndex].clone();

        return result;
    }

    /**
     * Calculate this polygon's diameter: the distance between its two most
     * distant corners.
     *
     * @return distance (in world units, &ge;0)
     */
    public float diameter() {
        double largestSD = 0.0;

        for (int i = 0; i < numCorners; i++) {
            for (int j = i + 1; j < numCorners; j++) {
                double squaredDistance = squaredDistance(i, j);
                if (squaredDistance > largestSD) {
                    largestSD = squaredDistance;
                }
            }
        }
        float result = (float) Math.sqrt(largestSD);

        assert result >= 0.0 : result;
        return result;
    }

    /**
     * Calculate (or look up) the dot product of successive sides which meet at
     * the specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return dot product
     */
    public double dotProduct(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");

        if (dotProducts[cornerIndex] == null) {
            setCorner(cornerIndex);
        }
        double result = dotProducts[cornerIndex];

        return result;
    }

    /**
     * Find the corner nearest to a specified location.
     *
     * @param location input coordinates (not null, unaffected)
     *
     * @return index of the nearest corner (&ge;0, &lt;numCorners) or -1 if this
     * polygon has no corners
     */
    public int findCorner(Vector3f location) {
        int result = -1;
        double bestSD = Double.POSITIVE_INFINITY;
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            Vector3f corner = cornerLocations[cornerIndex];
            double squaredDistance = MyVector3f.distanceSquared(corner, location);
            if (squaredDistance < bestSD) {
                result = cornerIndex;
                bestSD = squaredDistance;
            }
        }

        return result;
    }

    /**
     * Find the polygon's longest side.
     *
     * @return index of the longest side (&ge;0, &lt;numCorners) or -1 if this
     * polygon has no sides
     */
    public int findLongest() {
        int result = -1;
        double biggestSD = Double.NEGATIVE_INFINITY;
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            int nextI = nextIndex(sideIndex);
            double squaredDistance = squaredDistance(sideIndex, nextI);
            if (squaredDistance > biggestSD) {
                result = sideIndex;
                biggestSD = squaredDistance;
            }
        }
        return result;
    }

    /**
     * Find the polygon's shortest side.
     *
     * @return index of the shortest side (&ge;0, &lt;numCorners) or -1 if this
     * polygon has no sides
     */
    public int findShortest() {
        int result = -1;
        double leastSD = Double.POSITIVE_INFINITY;
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            int nextI = nextIndex(sideIndex);
            double squaredDistance = squaredDistance(sideIndex, nextI);
            if (squaredDistance < leastSD) {
                result = sideIndex;
                leastSD = squaredDistance;
            }
        }
        return result;
    }

    /**
     * Find the side closest to a specified point in space.
     *
     * @param point coordinates of the specified point (not null, unaffected)
     * @param storeClosestPoint if not null, used to store the coordinates of
     * the closest point on the perimeter, if determined
     * @return index of closest the side (&ge;0, &lt;numCorners) or -1 if this
     * polygon has no sides
     */
    public int findSide(Vector3f point, Vector3f storeClosestPoint) {
        Validate.nonNull(point, "coordinates of point");

        int result = -1;
        double leastSD = Double.POSITIVE_INFINITY;
        Vector3f closestPointCurrentSide = new Vector3f();
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            double squaredDistance = squaredDistanceToSide(
                    point, sideIndex, closestPointCurrentSide);
            if (squaredDistance < leastSD) {
                result = sideIndex;
                leastSD = squaredDistance;
                if (storeClosestPoint != null) {
                    storeClosestPoint.set(closestPointCurrentSide);
                }
            }
        }

        return result;
    }

    /**
     * Generate a new polygon which shares a range of corners with this one.
     *
     * @param firstIndex index of the first corner in the range (&ge;0,
     * &lt;numCorners)
     * @param lastIndex index of the last corner in the range (&ge;0,
     * &lt;numCorners)
     * @return a new polygon with at least two corners
     */
    public Polygon3f fromRange(int firstIndex, int lastIndex) {
        validateIndex(firstIndex, "first corner index");
        validateIndex(lastIndex, "last corner index");
        if (firstIndex == lastIndex) {
            throw new IllegalArgumentException("Corner indices must differ.");
        }
        /*
         * Count how many corners the new polygon will include.
         */
        int newNumCorners = 1;
        for (int oldI = firstIndex; oldI != lastIndex; oldI = nextIndex(oldI)) {
            newNumCorners++;
        }
        assert newNumCorners >= 2 : newNumCorners;
        assert newNumCorners <= numCorners : newNumCorners;
        /*        
         * Allocate and fill the array of corner locations.
         */
        Vector3f[] newCornerLocations = new Vector3f[newNumCorners];
        int[] oldIndex = new int[newNumCorners];
        int newI = 0;
        for (int oldI = firstIndex; oldI != lastIndex; oldI = nextIndex(oldI)) {
            oldIndex[newI] = oldI;
            newCornerLocations[newI] = cornerLocations[oldI];
            newI++;
        }
        assert newI == newNumCorners - 1;
        oldIndex[newI] = lastIndex;
        newCornerLocations[newI] = cornerLocations[lastIndex];

        Polygon3f result = new Polygon3f(newCornerLocations, tolerance);
        assert result.numCorners() == newNumCorners;

        if (isPlanar != null && isPlanar) {
            result.setIsPlanar(true);
        }
        for (newI = 0; newI < newNumCorners; newI++) {
            int oldI = oldIndex[newI];
            for (int newJ = newI; newJ < newNumCorners; newJ++) {
                int oldJ = oldIndex[newJ];
                Double sd = squaredDistances[oldI][oldJ];
                if (sd != null) {
                    result.setSquaredDistance(newI, newJ, sd);
                }
            }
        }

        return result;
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
     * Test (or look up) whether this polygon is degenerate.
     *
     * @return true if it is degenerate, otherwise false
     */
    final public boolean isDegenerate() {
        if (isDegenerate == null) {
            setIsDegenerate();
        }

        return isDegenerate;
    }

    /**
     * Test (or look up) whether this polygon is planar.
     *
     * @return true if it is planar, otherwise false
     */
    final public boolean isPlanar() {
        if (isPlanar == null) {
            setIsPlanar();
        }

        return isPlanar;
    }

    /**
     * Calculate (or look up) the largest triangle formed by any three corners
     * in this polygon.
     *
     * @return a new vector of three corner indices (sorted in ascending order)
     * or null if the polygon has fewer than three corners
     */
    public int[] largestTriangle() {
        if (largestTriangle == null) {
            setLargestTriangle();
        }
        if (largestTriangle == null) {
            return null;
        }
        int[] result = new int[3];
        result[0] = largestTriangle[0];
        result[1] = largestTriangle[1];
        result[2] = largestTriangle[2];

        assert result[0] < result[1];
        assert result[1] < result[2];
        return result;
    }

    /**
     * Read the number of corners, which is also the number of sides.
     *
     * @return count (&ge; 0)
     */
    public int numCorners() {
        assert numCorners >= 0 : numCorners;
        return numCorners;
    }

    /**
     * Calculate which corner (or side) follows the specified one. For this to
     * work, there must be at least one corner.
     *
     * @param index index of a corner (or side) (&ge;0, &lt;numCorners)
     * @return index of the following corner (or side) (&ge;0, &lt;numCorners)
     */
    public int nextIndex(int index) {
        validateIndex(index, "index");
        if (numCorners < 1) {
            throw new IllegalStateException("no corners");
        }

        int next = (index + 1) % numCorners;
        return next;
    }

    /**
     * Find a corner with which a given point coincides.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @return index of the coincident corner, or -1 if none coincide
     */
    public int onCorner(Vector3f point) {
        Validate.nonNull(point, "point");

        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            if (onCorner(point, cornerIndex)) {
                return cornerIndex;
            }
        }
        return -1;
    }

    /**
     * Test whether a point coincides with a specific corner of this polygon.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param cornerIndex index of the corner (&ge;0, &lt;numCorners-1)
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
     * Find a side on which a given point lies.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @return index of the side, or -1 if point not on perimeter
     */
    public int onSide(Vector3f point) {
        Validate.nonNull(point, "point");

        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            if (onSide(point, sideIndex)) {
                return sideIndex;
            }
        }
        return -1;
    }

    /**
     * Test whether a point lies on a specific side of this polygon.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param sideIndex index of the side (&ge;0, &lt;numCorners-1)
     * @return true if the point lies on the side, false if it doesn't
     */
    public boolean onSide(Vector3f point, int sideIndex) {
        Validate.nonNull(point, "point");
        validateIndex(sideIndex, "side index");

        double squaredDistance = squaredDistanceToSide(point, sideIndex, null);
        if (squaredDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculate the polygon's perimeter.
     *
     * @return perimeter (&ge;0)
     */
    public float perimeter() {
        float sum = 0f;
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            float length = sideLength(sideIndex);
            sum += length;
        }

        assert sum >= 0f : sum;
        return sum;
    }

    /**
     * Calculate which corner (or side) precedes the specified one. For this to
     * work, there must be at least one corner.
     *
     * @param index index of a corner (or side) (&ge;0, &lt;numCorners)
     * @return index of the preceding corner (or side) (&ge;0, &lt;numCorners)
     */
    public int prevIndex(int index) {
        validateIndex(index, "index");
        if (numCorners < 1) {
            throw new IllegalStateException("no corners");
        }

        int next = (index + numCorners - 1) % numCorners;
        return next;
    }

    /**
     * Test whether this polygon shares one or more corners with another
     * polygon.
     *
     * @param other the other polygon (not null)
     * @param storeCornerMap if not null, used to store indices of matching
     * corners
     * @return true if one or more shared corners were found, otherwise false
     */
    public boolean sharesCornerWith(Polygon3f other,
            Map<Integer, Integer> storeCornerMap) {
        Validate.nonNull(other, "other polygon");

        if (storeCornerMap != null) {
            storeCornerMap.clear();
        }

        float tol = other.getTolerance();
        float tol2 = (tolerance2 + tol * tol) / 2f;

        boolean result = false;
        for (int otherI = 0; otherI < other.numCorners(); otherI++) {
            Vector3f otherCorner = other.copyCornerLocation(otherI);
            for (int thisI = 0; thisI < numCorners; thisI++) {
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
     * Test whether this polygon shares one or more sides with another polygon.
     *
     * @param other the other polygon (not null)
     * @param storeMap if not null, used to store indices of matching sides
     * @return true if one or more shared sides were found, otherwise false
     */
    public boolean sharesSideWith(Polygon3f other,
            Map<Integer, Integer> storeMap) {
        Validate.nonNull(other, "other polygon");

        if (storeMap != null) {
            storeMap.clear();
        }

        Map<Integer, Integer> cornerMap = new TreeMap<>();
        boolean result = sharesCornerWith(other, cornerMap);
        if (result == false) {
            return false;
        }

        for (Map.Entry<Integer, Integer> entry : cornerMap.entrySet()) {
            int thisI = entry.getKey();
            int thisN = nextIndex(thisI);
            int otherI = entry.getValue();
            int otherN = cornerMap.get(thisN);

            if (otherN == other.nextIndex(otherI)) {
                result = true;
                if (storeMap != null) {
                    storeMap.put(thisI, otherI);
                }
            }
            if (otherI == other.nextIndex(otherN)) {
                result = true;
                if (storeMap != null) {
                    storeMap.put(thisI, otherN);
                }
            }
        }

        return result;
    }

    /**
     * Calculate the length of a side.
     *
     * @param sideIndex index of the side to measure (&ge;0, &lt;numSides)
     * @return length (&ge; 0)
     */
    public float sideLength(int sideIndex) {
        validateIndex(sideIndex, "side index");

        int nextIndex = nextIndex(sideIndex);
        double squaredDistance = squaredDistance(sideIndex, nextIndex);
        float result = (float) Math.sqrt(squaredDistance);

        return result;
    }

    /**
     * Calculate the squared distance from a point to a specific corner of this
     * polygon.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param cornerIndex index of the corner (&ge;0, &le;numCorners-1)
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
     * Calculate the squared distance from a point to a specific side of this
     * polygon.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @param sideIndex index of the side (&ge;0, &lt;numCorners-1)
     * @param storeClosestPoint if not null, used to store the coordinates of
     * the closest point
     * @return squared distance from point to side (&ge;0)
     */
    public double squaredDistanceToSide(Vector3f point, int sideIndex,
            Vector3f storeClosestPoint) {
        Validate.nonNull(point, "point");
        validateIndex(sideIndex, "side index");
        /*
         * Calculate start and direction of a straight line containing the side.
         */
        Vector3f corner1 = cornerLocations[sideIndex];
        int nextIndex = nextIndex(sideIndex);
        Vector3f corner2 = cornerLocations[nextIndex];
        Vector3f sideOffset = corner2.subtract(corner1);
        /*
         * If the side has zero length, return the squared distance to corner1.
         */
        double sideLengthSquared = squaredDistance(sideIndex, nextIndex);
        if (sideLengthSquared == 0.0) {
            if (storeClosestPoint != null) {
                storeClosestPoint.set(corner1);
            }
            double result = MyVector3f.distanceSquared(corner1, point);
            return result;
        }
        /*
         * Calculate parametric value for the closest point on that line.
         */
        Vector3f pointOffset = point.subtract(corner1);
        double dot = MyVector3f.dot(pointOffset, sideOffset);
        double t = dot / sideLengthSquared;
        /*
         * Calculate offset of the closest point on the side.
         */
        float scaleFactor = FastMath.clamp((float) t, 0f, 1f);
        Vector3f closestOffset = sideOffset.mult(scaleFactor);
        if (storeClosestPoint != null) {
            storeClosestPoint.set(corner1);
            storeClosestPoint.addLocal(closestOffset);
        }
        double result = MyVector3f.distanceSquared(closestOffset, pointOffset);

        assert result >= 0.0 : result;
        return result;
    }
    // *************************************************************************
    // protected methods

    /**
     * Test whether all the corners in a set lie on the line connecting two
     * given corners.
     *
     * @param cornerIndex1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param corners (length&ge;1, length&le;numCorners, unaffected)
     * @return true if the corners are collinear, false otherwise
     */
    protected boolean allCollinear(int cornerIndex1, int cornerIndex2,
            BitSet corners) {
        validateIndex(cornerIndex1, "index of 1st corner");
        validateIndex(cornerIndex2, "index of 2nd corner");
        Validate.nonNull(corners, "set of corners");
        int length = corners.length();
        Validate.inRange(length, "length of set of corners", 1, numCorners);

        if (doCoincide(cornerIndex1, cornerIndex2)) {
            /*
             * The line is ill-defined.
             */
            return true;
        }
        /*
         * Calculate the offset of the last corner from the first.
         */
        Vector3f first = cornerLocations[cornerIndex1];
        Vector3f last = cornerLocations[cornerIndex2];
        Vector3f fl = last.subtract(first);

        for (int middleI = corners.nextSetBit(0);
                middleI >= 0; middleI = corners.nextSetBit(middleI + 1)) {
            assert middleI < numCorners : middleI;
            /*
             * Calculate the offset of the middle corner from the first.
             */
            Vector3f middle = cornerLocations[middleI];
            Vector3f fm = middle.subtract(first);
            /*
             * Project FM onto FL.
             * 
             * Don't use Vector3f.project() because (as of jME 3.0.10) 
             * it contains a logic bug.
             */
            double fm_dot_fl = MyVector3f.dot(fm, fl);
            double normSquaredFL = squaredDistance(cornerIndex1, cornerIndex2);
            double fraction = fm_dot_fl / normSquaredFL;
            Vector3f projection = fl.mult((float) fraction);
            /*
             * If the projection coincides with FM, 
             * then consider the three points to be collinear.
             */
            boolean collin = MyVector3f.doCoincide(projection, fm, tolerance2);
            if (!collin) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether three corners are collinear.
     *
     * @param cornerIndex1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param cornerIndex3 index of the 3rd corner (&ge;0, &lt;numCorners)
     * @param storeMiddleIndex if not null and the result is true, used to store
     * the index of the middle corner, if determined
     * @return true if collinear, otherwise false
     */
    protected boolean areCollinear(int cornerIndex1, int cornerIndex2,
            int cornerIndex3, Integer storeMiddleIndex) {
        validateIndex(cornerIndex1, "index of 1st corner");
        validateIndex(cornerIndex2, "index of 2nd corner");
        validateIndex(cornerIndex3, "index of 3rd corner");
        /*
         * Shortcut:
         * If any of the corners coincide, then the set is collinear,
         * but the middle corner is ill-defined.
         */
        if (doCoincide(cornerIndex1, cornerIndex2)
                || doCoincide(cornerIndex1, cornerIndex3)
                || doCoincide(cornerIndex2, cornerIndex3)) {
            return true;
        }
        /*
         * The hard way: find which two corners are most distant from each other.
         */
        BitSet corners = new BitSet(numCorners);
        corners.set(cornerIndex1);
        corners.set(cornerIndex2);
        corners.set(cornerIndex3);
        int[] longest = mostDistant(corners);
        assert longest != null;
        int firstIndex = longest[0];
        int lastIndex = longest[1];
        /*
         * The remaining corner is the middle one.
         */
        corners.clear(firstIndex);
        corners.clear(lastIndex);
        int middleIndex = corners.nextSetBit(0);

        boolean result = allCollinear(firstIndex, lastIndex, corners);
        if (result && storeMiddleIndex != null) {
            storeMiddleIndex = middleIndex;
        }

        return result;
    }

    /**
     * Test whether two corners coincide.
     *
     * @param cornerIndex1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @return true if they coincide, otherwise false
     */
    protected boolean doCoincide(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of 1st corner");
        validateIndex(cornerIndex2, "index of 2nd corner");

        double squaredDistance = squaredDistance(cornerIndex1, cornerIndex2);
        if (squaredDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Find the most distant pair of corners in a specified set.
     *
     * @param cornerSet indices of the corners to consider (not null,
     * unaffected)
     * @return a new array of two corner indices, or null if no pairs were found
     */
    protected int[] mostDistant(BitSet cornerSet) {
        Validate.nonNull(cornerSet, "corner set");

        double largestSD = -1.0;
        int[] result = null;

        for (int i = cornerSet.nextSetBit(0);
                i >= 0; i = cornerSet.nextSetBit(i + 1)) {
            for (int j = cornerSet.nextSetBit(i + 1);
                    j >= 0; j = cornerSet.nextSetBit(j + 1)) {
                double squaredDistance = squaredDistance(i, j);
                if (squaredDistance > largestSD) {
                    largestSD = squaredDistance;
                    if (result == null) {
                        result = new int[2];
                    }
                    result[0] = i;
                    result[1] = j;
                }
            }
        }

        return result;
    }

    /**
     * Calculate the square of the area of the triangle formed by three corners.
     *
     * @param indexA index of the 1st corner (&ge;0, &lt;numCorners)
     * @param indexB index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param indexC index of the 3rd corner (&ge;0, &lt;numCorners)
     * @return area^2 (&ge;0)
     */
    protected double squaredArea(int indexA, int indexB, int indexC) {
        validateIndex(indexA, "index of 1st corner");
        validateIndex(indexB, "index of 2nd corner");
        validateIndex(indexC, "index of 3rd corner");
        /*
         * Shortcut:
         * If any corners coincide, then the area is effectively zero. 
         */
        if (doCoincide(indexA, indexB)) {
            return 0.0;
        }
        if (doCoincide(indexA, indexC)) {
            return 0.0;
        }
        if (doCoincide(indexB, indexC)) {
            return 0.0;
        }
        /*
         * Calculate the offsets of the B and C relative to A.
         */
        Vector3f a = cornerLocations[indexA];
        Vector3f b = cornerLocations[indexB];
        Vector3f c = cornerLocations[indexC];
        Vector3f ab = b.subtract(a);
        Vector3f ac = c.subtract(a);
        /*
         * Using AC as the base, calculate the squared height.
         */
        double abDotAC = MyVector3f.dot(ab, ac);
        double ac2 = squaredDistance(indexA, indexC);
        double fraction = abDotAC / ac2;
        Vector3f projABonAC = ac.mult((float) fraction);
        double heightSquared = MyVector3f.distanceSquared(ab, projABonAC);
        /*
         * Calculate the squared area.  Area = base * height / 2f.
         */
        double baseSquared = squaredDistance(indexA, indexC);
        double areaSquared = baseSquared * heightSquared / 4f;

        return areaSquared;
    }

    /**
     * Calculate (or look up) the squared distance between two corners.
     *
     * @param cornerIndex1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @return squared distance (&ge;0)
     */
    protected double squaredDistance(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of 1st corner");
        validateIndex(cornerIndex2, "index of 2nd corner");

        if (squaredDistances[cornerIndex1][cornerIndex2] == null) {
            setSquaredDistance(cornerIndex1, cornerIndex2);
        }
        return squaredDistances[cornerIndex1][cornerIndex2];
    }

    /**
     * Validate a corner (or side) index as a method argument.
     *
     * @param index index to validate (&gt;0)
     * @param description description of the index (not null)
     * @throws IllegalArgumentException if the value is &lt;0 or &ge;numCorners
     */
    protected void validateIndex(int index, String description) {
        Validate.inRange(index, description, 0, numCorners - 1);
    }

    /**
     * Verify that this polygon is non-degenerate.
     *
     * @throws IllegalStateException if it is degenerate
     */
    protected void verifyNonDegenerate() {
        if (isDegenerate()) {
            throw new IllegalStateException("degenerate polygon");
        }
    }

    /**
     * Verify that this polygon is planar.
     *
     * @throws IllegalStateException if it is non-planar
     */
    protected void verifyPlanar() {
        if (!isPlanar()) {
            throw new IllegalStateException("non-planar polygon");
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the elements of the #cosTurnAngles and #crossProducts fields
     * for a particular corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     */
    private void setCorner(int cornerIndex) {
        int nextI = nextIndex(cornerIndex);
        int prevI = prevIndex(cornerIndex);
        /*
         * Calculate the offsets of the two adjacent sides.
         */
        Vector3f a = cornerLocations[prevI];
        Vector3f b = cornerLocations[cornerIndex];
        Vector3f c = cornerLocations[nextI];
        Vector3f offsetB = b.subtract(a);
        Vector3f offsetC = c.subtract(b);

        double dot = MyVector3f.dot(offsetB, offsetC);
        Vector3f cross = offsetB.cross(offsetC);
        setCorner(cornerIndex, dot, cross);
    }

    /**
     * Direct setter for the #cosTurnAngles and #crossProduct fields.
     *
     * @param cornerIndex index of the corner (&ge;0, &lt;numCorners)
     * @param newDot new value for dot product
     * @param newCross new value for cross product (not null, unaffected)
     */
    private void setCorner(int cornerIndex, double newDot,
            Vector3f newCross) {
        assert cornerIndex >= 0 : cornerIndex;
        assert cornerIndex < numCorners : cornerIndex;
        assert newCross != null;
        assert crossProducts[cornerIndex] == null;
        assert dotProducts[cornerIndex] == null;

        crossProducts[cornerIndex] = newCross.clone();
        dotProducts[cornerIndex] = newDot;
    }

    /**
     * Initialize the isDegenerate field. The polygon is degenerate if it has
     * (1) fewer than three corners OR (2) coincident corners OR (3) a
     * 180-degree turn. In a non-degenerate polygon, each corner forms an angle
     * less than Pi radians, though the sign of the angle may be ambiguous.
     */
    private void setIsDegenerate() {
        if (numCorners < 3) {
            setIsDegenerate(true);
            return;
        }
        /*
         * (2) Test for coincident corners.
         */
        for (int iCorner = 0; iCorner < numCorners; iCorner++) {
            for (int jCorner = iCorner + 1; jCorner < numCorners; jCorner++) {
                if (doCoincide(iCorner, jCorner)) {
                    setIsDegenerate(true);
                    return;
                }
            }
        }
        /*
         * (3) Test for 180-degree turns.
         */
        for (int cornerI = 0; cornerI < numCorners; cornerI++) {
            double dot = dotProduct(cornerI);
            int nextI = nextIndex(cornerI);
            int prevI = prevIndex(cornerI);
            double lsPrevious = squaredDistance(prevI, cornerI);
            double lsCurrent = squaredDistance(cornerI, nextI);
            double lsProduct = lsPrevious * lsCurrent;
            double lengthProduct = Math.sqrt(lsProduct);

            if (dot < tolerance2 - lengthProduct) {
                setIsDegenerate(true);
                return;
            }
        }

        setIsDegenerate(false);
    }

    /**
     * Direct setter for the #isDegenerate field.
     *
     * @param newValue new value
     */
    private void setIsDegenerate(boolean newValue) {
        assert isDegenerate == null;
        isDegenerate = newValue;
    }

    /**
     * Initialize the #isPlanar field. A polygon is planar if all corners (and
     * sides) lie in a single plane.
     */
    private void setIsPlanar() {
        /*
         * Shortcut #1: all polygons with less than 4 sides are planar.
         */
        if (numCorners < 4) {
            setIsPlanar(true);
            return;
        }
        /*
         * Shortcut #2: if all corners have the same y-coordinate, 
         * then the polygon is planar.
         */
        boolean sameY = true;
        float y = cornerLocations[0].y;
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            float dy = cornerLocations[cornerIndex].y - y;
            if (dy * dy > tolerance2) {
                sameY = false;
                break;
            }
        }
        if (sameY) {
            setIsPlanar(true);
            return;
        }
        /*
         * The hard way: find the three corners which form the largest triangle.
         */
        int[] triangle = largestTriangle();
        if (triangle == null) {
            /*
             * Degenerate case:
             * Treat as planar even though the plane is ill-defined.
             */
            assert isDegenerate();
            setIsPlanar(true);
            return;
        }
        int aIndex = triangle[0];
        int bIndex = triangle[1];
        int cIndex = triangle[2];
        /*
         * Calculate the plane containing that triangle.
         */
        Vector3f a = cornerLocations[aIndex];
        Vector3f b = cornerLocations[bIndex];
        Vector3f c = cornerLocations[cIndex];
        Vector3f offsetB = b.subtract(a);
        Vector3f offsetC = c.subtract(b);
        Vector3f crossProduct = offsetB.cross(offsetC);
        float crossLength = crossProduct.length();
        if (crossLength == 0f) {
            /*
             * Degenerate case:
             * Treat as planar even though the plane is ill-defined.
             */
            assert isDegenerate();
            setIsPlanar(true);
            return;
        }
        Vector3f planeNormal = crossProduct.divide(crossLength);
        float planeConstant = -planeNormal.dot(a);
        /*
         * If all the polygon's corners lie in that plane, 
         * then the polygon is planar, otherwise it isn't.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            Vector3f corner = cornerLocations[cornerIndex];
            float pseudoDistance = planeNormal.dot(corner) + planeConstant;
            float distanceSquared = pseudoDistance * pseudoDistance;
            if (distanceSquared > tolerance2) {
                setIsPlanar(false);
                return;
            }
        }
        setIsPlanar(true);
    }

    /**
     * Direct setter for the #isPlanar field.
     *
     * @param newValue new value
     */
    private void setIsPlanar(boolean newValue) {
        assert isPlanar == null;
        isPlanar = newValue;
    }

    /**
     * Initialize the #largestTriangle field if numCorners&gt;3, otherwise leave
     * it unchanged.
     */
    private void setLargestTriangle() {
        double largestSA = -1f;
        int[] largest = null;
        for (int i = 0; i < numCorners - 2; i++) {
            for (int j = i + 1; j < numCorners - 1; j++) {
                for (int k = j + 1; k < numCorners; k++) {
                    double sa = squaredArea(i, j, k);
                    if (sa > largestSA) {
                        largestSA = sa;
                        if (largest == null) {
                            largest = new int[3];
                        }
                        largest[0] = i;
                        largest[1] = j;
                        largest[2] = k;
                    }
                }
            }
        }
        if (largest != null) {
            setLargestTriangle(largest);
        }
    }

    /**
     * Direct setter for the #largestTriangle field.
     *
     * @param newValue indices of the triangle's corners (not null, each &ge;0
     * and &lt;numCorners, in ascending order, unaffected)
     */
    private void setLargestTriangle(int[] newValue) {
        assert largestTriangle == null;
        assert newValue != null;
        assert newValue[0] >= 0 : newValue[0];
        assert newValue[1] >= 0 : newValue[1];
        assert newValue[2] >= 0 : newValue[2];
        assert newValue[0] < numCorners : newValue[0];
        assert newValue[1] < numCorners : newValue[1];
        assert newValue[2] < numCorners : newValue[2];
        assert newValue[1] > newValue[0];
        assert newValue[2] > newValue[1];

        largestTriangle = new int[3];
        largestTriangle[0] = newValue[0];
        largestTriangle[1] = newValue[1];
        largestTriangle[2] = newValue[2];
    }

    /**
     * Initialize the element(s) of the #squaredDistances field for a particular
     * pair of corners.
     *
     * @param cornerIndex1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     */
    private void setSquaredDistance(int cornerIndex1, int cornerIndex2) {
        assert cornerIndex1 >= 0 : cornerIndex1;
        assert cornerIndex1 < numCorners : cornerIndex1;
        assert cornerIndex2 >= 0 : cornerIndex2;
        assert cornerIndex2 < numCorners : cornerIndex2;

        if (cornerIndex1 == cornerIndex2) {
            setSquaredDistance(cornerIndex1, cornerIndex2, 0.0);
        } else {
            Vector3f corner1 = cornerLocations[cornerIndex1];
            Vector3f corner2 = cornerLocations[cornerIndex2];
            double square = MyVector3f.distanceSquared(corner1, corner2);
            setSquaredDistance(cornerIndex1, cornerIndex2, square);
        }
    }

    /**
     * Direct setter for the #squaredDistances field.
     *
     * @param ci1 index of the 1st corner (&ge;0, &lt;numCorners)
     * @param ci2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param newValue new value for squared distance (&ge;0)
     */
    private void setSquaredDistance(int ci1, int ci2, double newValue) {
        assert ci1 >= 0 : ci1;
        assert ci1 < numCorners : ci1;
        assert ci2 >= 0 : ci2;
        assert ci2 < numCorners : ci2;
        assert newValue >= 0.0 : newValue;
        assert squaredDistances[ci1][ci2] == null;
        assert squaredDistances[ci2][ci1] == null;

        squaredDistances[ci1][ci2] = newValue;
        squaredDistances[ci2][ci1] = newValue;
    }
}

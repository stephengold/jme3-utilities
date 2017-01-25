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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * An immutable polygon in three-dimensional space, consisting of N corners
 * (points) connected by N sides (straight-line segments). For the sake of
 * efficiency, many calculated values are cached.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Polygon3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Polygon3f.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, then two adjacent sides are collinear and/or there are fewer
     * than 3 corners and/or there are duplicate corners (set by
     * setIsDegenerate())
     */
    private Boolean isDegenerate = null;
    /**
     * if true, then all corners (and sides) lie in a single plane (set by
     * setIsPlanar())
     */
    private Boolean isPlanar = null;
    /**
     * maximum distance for points to coincide (&ge;0, set by constructor)
     */
    final protected float tolerance;
    /**
     * square of the tolerance (&ge;0, set by constructor)
     */
    final protected float tolerance2;
    /**
     * cached squared distances between corners (each &ge;0, allocated by
     * constructor, each initialized by setSquaredDistance())
     */
    final private Float[][] squaredDistances;
    /**
     * the number of corners, which is also the number of sides (&ge;0, set by
     * constructor)
     */
    final protected int numCorners;
    /**
     * locations of all corners, in sequence (not null or containing any nulls,
     * initialized by constructor)
     */
    final protected Vector3f[] cornerLocations;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a polygon with the specified sequence of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     */
    public Polygon3f(Vector3f[] cornerList, float compareTolerance) {
        Validate.nonNull(cornerList, "corner list");
        for (int index = 0; index < cornerList.length; index++) {
            String description = String.format("cornerList[%d]", index);
            Validate.nonNull(cornerList[index], description);
        }
        Validate.nonNegative(compareTolerance, "compare tolerance");

        numCorners = cornerList.length;
        /*
         * Allocate array space.
         */
        cornerLocations = new Vector3f[numCorners];
        squaredDistances = new Float[numCorners][];
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            squaredDistances[cornerIndex] = new Float[numCorners];
        }
        /*
         * Copy corner locations.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            cornerLocations[cornerIndex] = cornerList[cornerIndex].clone();
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
     * Find the corner closest to a point in space.
     *
     * @param point coordinates of the point (not null, unaffected)
     *
     * @return index of the corner (&ge;0, &lt;numCorners) or -1 if this polygon
     * has no corners
     */
    public int findCorner(Vector3f point) {
        int result = -1;
        float bestDS = Float.POSITIVE_INFINITY;
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            Vector3f corner = cornerLocations[cornerIndex];
            float distanceSquared = corner.distanceSquared(point);
            if (distanceSquared < bestDS) {
                result = cornerIndex;
                bestDS = distanceSquared;
            }
        }

        return result;
    }

    /**
     * Find the side closest to a specified point in space.
     *
     * @param point coordinates of the specified point (not null, unaffected)
     * @param storeCoordinates if not null, used to store coordinates of the
     * point on the side closest to the specified point
     *
     * @return index of the side (&ge;0, &lt;numCorners) or -1 if this polygon
     * has no sides
     */
    public int findSide(Vector3f point, Vector3f storeCoordinates) {
        int result = -1;
        float bestSD = Float.POSITIVE_INFINITY;
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            /*
             * Try the first corner of the current side.
             */
            Vector3f corner = cornerLocations[sideIndex];
            Vector3f offset = point.subtract(corner);
            float squaredDistance = offset.lengthSquared();
            if (squaredDistance < bestSD) {
                result = sideIndex;
                bestSD = squaredDistance;
                if (storeCoordinates != null) {
                    storeCoordinates.set(corner);
                }
            }
            /*
             * Find closest point on the straight line containing the side.
             */
            int nextIndex = nextIndex(sideIndex);
            Vector3f corner2 = cornerLocations[nextIndex];
            Vector3f offset2 = corner2.subtract(corner);
            float dot = offset.dot(offset2);
            float lengthSquared = squaredDistance(sideIndex, nextIndex);
            if (dot > 0f && dot < lengthSquared) {
                /* 
                 * The closest point lies on the side.
                 */
                Vector3f offsetClosest = offset2.mult(dot / lengthSquared);
                squaredDistance = offsetClosest.distanceSquared(offset);
                if (squaredDistance < bestSD) {
                    result = sideIndex;
                    bestSD = squaredDistance;
                    if (storeCoordinates != null) {
                        storeCoordinates.set(corner);
                        storeCoordinates.addLocal(offsetClosest);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Read the number of corners, which is also the number of sides.
     *
     * @return count (&ge; 0)
     */
    public int numCorners() {
        return numCorners;
    }

    /**
     * Calculate the polygon's perimeter.
     *
     * @return perimeter (&ge;0)
     */
    public float perimeter() {
        float sum = 0f;
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            int next = nextIndex(cornerIndex);
            float length2 = squaredDistance(cornerIndex, next);
            float length = FastMath.sqrt(length2);
            sum += length;
        }

        return sum;
    }

    /**
     * Test whether this polygon shares one or more corners with another
     * polygon.
     *
     * @param other the other polygon (not null)
     * @param cornerMap if not null, used to store indices of matching corners
     * @return true if one or more shared corners were found, otherwise false
     */
    public boolean sharesCornerWith(Polygon3f other,
            Map<Integer, Integer> cornerMap) {
        Validate.nonNull(other, "other polygon");

        if (cornerMap != null) {
            cornerMap.clear();
        }

        float tol2 = other.tolerance();
        tol2 = (tolerance2 + tol2 * tol2) / 2f;

        boolean result = false;
        for (int otherI = 0; otherI < other.numCorners(); otherI++) {
            Vector3f otherCorner = other.copyCornerLocation(otherI);
            for (int thisI = 0; thisI < numCorners; thisI++) {
                Vector3f thisCorner = cornerLocations[thisI];
                if (MyVector3f.doCoincide(otherCorner, thisCorner, tol2)) {
                    result = true;
                    if (cornerMap != null) {
                        cornerMap.put(thisI, otherI);
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
     * @param sideMap if not null, used to storage indices of matching sides
     * @return true if one or more shared sides were found, otherwise false
     */
    public boolean sharesEdgeWith(Polygon3f other,
            Map<Integer, Integer> sideMap) {
        Validate.nonNull(other, "other polygon");

        if (sideMap != null) {
            sideMap.clear();
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
                if (sideMap != null) {
                    sideMap.put(thisI, otherI);
                }
            }
            if (otherI == other.nextIndex(otherN)) {
                result = true;
                if (sideMap != null) {
                    sideMap.put(thisI, otherN);
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
    public float tolerance() {
        return tolerance;
    }
    // *************************************************************************
    // protected methods

    /**
     * Test whether all the corners in a set lie on the line connecting two
     * given corners.
     *
     * @param firstIndex index of the first corner (&ge;0, &lt;numCorners)
     * @param secondIndex index of the second corner (&ge;0, &lt;numCorners)
     * @param corners (length&le;numCorners, cardinality&ge;2, unaffected)
     */
    boolean allCollinear(int firstIndex, int secondIndex, BitSet corners) {
        validateIndex(firstIndex, "index of first corner");
        validateIndex(secondIndex, "index of second corner");
        Validate.nonNull(corners, "set of corners");
        assert corners.length() <= numCorners;

        if (doCoincide(firstIndex, secondIndex)) {
            /*
             * The line is ill-defined.
             */
            return true;
        }
        /*
         * Calculate the offset of the last corner from the first.
         */
        Vector3f first = cornerLocations[firstIndex];
        Vector3f last = cornerLocations[secondIndex];
        Vector3f fl = last.subtract(first);

        for (int middleI = corners.nextSetBit(0);
                middleI >= 0; middleI = corners.nextSetBit(middleI + 1)) {
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
            float fm_dot_fl = fm.dot(fl);
            float normSquaredFL = squaredDistance(firstIndex, secondIndex);
            Vector3f projection = fl.mult(fm_dot_fl / normSquaredFL);
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
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the second corner (&ge;0, &lt;numCorners)
     * @param cornerIndex3 index of the third corner (&ge;0, &lt;numCorners)
     * @param storeMiddleIndex if not null and the result is true, this object
     * will be set to the index of the middle corner, if determined
     * @return true if collinear, otherwise false
     */
    protected boolean areCollinear(int cornerIndex1, int cornerIndex2,
            int cornerIndex3, Integer storeMiddleIndex) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of second corner");
        validateIndex(cornerIndex3, "index of third corner");
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
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the second corner (&ge;0, &lt;numCorners)
     * @return true if they coincide, otherwise false
     */
    protected boolean doCoincide(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of second corner");

        float d2 = squaredDistance(cornerIndex1, cornerIndex2);
        if (d2 > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Find the largest triangle formed by any three corners in the specified
     * set.
     *
     * @param cornerSet indices of the corners to consider (not null,
     * unaffected)
     * @return a new array of three corner indices, or null if no triangles were
     * found
     */
    protected int[] largestTriangle(BitSet cornerSet) {
        Validate.nonNull(cornerSet, "corner set");

        float bestSA = -1f;
        int[] result = null;

        for (int i = cornerSet.nextSetBit(0);
                i >= 0; i = cornerSet.nextSetBit(i + 1)) {
            for (int j = cornerSet.nextSetBit(i + 1);
                    j >= 0; j = cornerSet.nextSetBit(j + 1)) {
                for (int k = cornerSet.nextSetBit(j + 1);
                        k >= 0; k = cornerSet.nextSetBit(k + 1)) {
                    float sa = squaredArea(i, j, k);
                    if (sa > bestSA) {
                        bestSA = sa;
                        if (result == null) {
                            result = new int[3];
                        }
                        result[0] = i;
                        result[1] = j;
                        result[2] = k;
                    }
                }
            }
        }

        return result;
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

        float bestSD = -1f;
        int[] result = null;

        for (int i = cornerSet.nextSetBit(0);
                i >= 0; i = cornerSet.nextSetBit(i + 1)) {
            for (int j = cornerSet.nextSetBit(i + 1);
                    j >= 0; j = cornerSet.nextSetBit(j + 1)) {
                float sd = squaredDistance(i, j);
                if (sd > bestSD) {
                    bestSD = sd;
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
     * Calculate which corner (or side) follows the specified one.
     *
     * @param index which corner (or side) (&ge;0, &lt;numCorners)
     * @return index of the following corner (or side) (&ge;0, &lt;numCorners)
     */
    protected int nextIndex(int index) {
        validateIndex(index, "index");

        int next = (index + 1) % numCorners;
        return next;
    }

    /**
     * Calculate the square of the area of the triangle formed by three corners.
     *
     * @param indexA index of the first corner (&ge;0, &lt;numCorners)
     * @param indexB index of the second corner (&ge;0, &lt;numCorners)
     * @param indexC index of the third corner (&ge;0, &lt;numCorners)
     * @return area^2 (&ge;0)
     */
    protected float squaredArea(int indexA, int indexB, int indexC) {
        validateIndex(indexA, "index of first corner");
        validateIndex(indexB, "index of second corner");
        validateIndex(indexC, "index of third corner");
        /*
         * Shortcut:
         * If any corners coincide, then the area is effectively zero. 
         */
        if (doCoincide(indexA, indexB)) {
            return 0f;
        }
        if (doCoincide(indexA, indexC)) {
            return 0f;
        }
        if (doCoincide(indexB, indexC)) {
            return 0f;
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
        float abDotAC = ab.dot(ac);
        float ac2 = squaredDistance(indexA, indexC);
        Vector3f projABonAC = ac.mult(abDotAC / ac2);
        float height2 = ab.distanceSquared(projABonAC);
        /*
         * Calculate the squared area.  Area = base * height / 2f.
         */
        float base2 = squaredDistance(indexA, indexC);
        float area2 = base2 * height2 / 4f;

        return area2;
    }

    /**
     * Calculate (or look up) the squared distance between two corners.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the second corner (&ge;0, &lt;numCorners)
     * @return squared distance in
     */
    protected float squaredDistance(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of second corner");

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
     * Initialize the isDegenerate field. The polygon is degenerate if (1) it
     * has fewer than three corners OR (2) it has duplicate corners OR (3) two
     * adjacent sides are collinear. In a non-degenerate polygon, each corner
     * forms an angle that is neither 0 nor Pi radians, though the sign of the
     * angle may be ambiguous.
     */
    private void setIsDegenerate() {
        if (numCorners < 3) {
            isDegenerate = true;
            return;
        }
        /*
         * Search for duplicate corners.
         */
        for (int iCorner = 0; iCorner < numCorners; iCorner++) {
            for (int jCorner = iCorner + 1; jCorner < numCorners; jCorner++) {
                if (doCoincide(iCorner, jCorner)) {
                    isDegenerate = true;
                    return;
                }
            }
        }
        /*
         * Search for collinear adjacent sides.
         */
        for (int iCorner = 0; iCorner < numCorners; iCorner++) {
            int next = nextIndex(iCorner);
            int nextNext = nextIndex(next);
            if (areCollinear(iCorner, next, nextNext, null)) {
                isDegenerate = true;
                return;

            }
        }

        isDegenerate = false;
    }

    /**
     * Initialize the isPlanar field. A polygon is planar if all corners (and
     * sides) lie in a single plane.
     */
    private void setIsPlanar() {
        /*
         * Shortcut #1: all polygons with less than 4 sides are planar.
         */
        if (numCorners < 4) {
            isPlanar = true;
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
            isPlanar = true;
            return;
        }
        /*
         * The hard way: find the three corners which form the largest triangle.
         */
        BitSet allCorners = new BitSet(numCorners);
        allCorners.set(0, numCorners - 1);
        int[] triangle = largestTriangle(allCorners);
        if (triangle == null) {
            /*
             * Degenerate case:
             * Treat as planar even though the plane is ill-defined.
             */
            assert isDegenerate();
            isPlanar = true;
            return;
        }
        /*
         * Calculate the plane containing that triangle.
         */
        int bestI = triangle[0];
        int bestJ = triangle[1];
        int bestK = triangle[2];
        Vector3f cornerI = cornerLocations[bestI];
        Vector3f cornerJ = cornerLocations[bestJ];
        Vector3f cornerK = cornerLocations[bestK];
        Vector3f offsetJ = cornerJ.subtract(cornerI);
        Vector3f offsetK = cornerK.subtract(cornerI);
        Vector3f crossProduct = offsetJ.cross(offsetK);
        float normSquared = crossProduct.lengthSquared();
        if (normSquared == 0f) {
            /*
             * Degenerate case:
             * Treat as planar even though the plane is ill-defined.
             */
            assert isDegenerate();
            isPlanar = true;
            return;
        }
        float norm = FastMath.sqrt(normSquared);
        Vector3f planeNormal = crossProduct.divide(norm);
        float planeConstant = planeNormal.dot(cornerI);
        /*
         * If all the polygon's corners lie in that plane, 
         * then the polygon is planar, otherwise it isn't.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            Vector3f corner = cornerLocations[cornerIndex];
            float pseudoDistance = planeNormal.dot(corner) - planeConstant;
            float d2 = pseudoDistance * pseudoDistance;
            if (d2 > tolerance2) {
                isPlanar = false;
                return;
            }
        }
        isPlanar = true;
    }

    /**
     * Initialize element(s) of the squaredDistances field for a particular pair
     * of corners.
     *
     * @param index1 (&ge;0, &lt;numCorners)
     * @param index2 (&ge;0, &lt;numCorners)
     */
    private void setSquaredDistance(int index1, int index2) {
        if (index1 == index2) {
            squaredDistances[index1][index2] = 0f;
        } else {
            Vector3f corner1 = cornerLocations[index1];
            Vector3f corner2 = cornerLocations[index2];
            float square = corner1.distanceSquared(corner2);
            squaredDistances[index1][index2] = square;
            squaredDistances[index2][index1] = square;
        }
    }
}

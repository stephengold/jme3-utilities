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
package jme3utilities.math.polygon;

import com.jme3.math.Vector3f;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * An immutable set of corners (points) in 3-dimensional space. For efficiency,
 * many calculated values are cached.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CornerSet3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CornerSet3f.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, then all corners lie in a single plane (set by #setIsPlanar())
     */
    private Boolean isPlanar = null;
    /**
     * cached squared distance between each pair of corners (each &ge;0,
     * allocated by constructor, each initialized by #setSquaredDistance())
     */
    final private Double[][] squaredDistances;
    /**
     * maximum distance for locations to coincide (&ge;0, set by constructor)
     */
    final protected float tolerance;
    /**
     * square of #tolerance (&ge;0, set by constructor)
     */
    final protected float tolerance2;
    /**
     * the number of corners (&ge;0, set by constructor)
     */
    final protected int numCorners;
    /**
     * cached vector of corner indices which define the largest triangle (each
     * &ge;0 and &lt;numCorners, in ascending order, set by
     * #setLargestTriangle())
     */
    private int[] largestTriangle = null;
    /**
     * coordinates of all corners (not null or containing any nulls, initialized
     * by constructor)
     */
    final protected Vector3f[] cornerLocations;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a corner set from an array of corners.
     *
     * @param cornerArray coordinates of the corners (not null or containing any
     * nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     */
    public CornerSet3f(Vector3f[] cornerArray, float compareTolerance) {
        Validate.nonNull(cornerArray, "corner array");
        this.numCorners = cornerArray.length;
        for (int index = 0; index < numCorners; ++index) {
            String description = String.format("cornerList[%d]", index);
            Validate.nonNull(cornerArray[index], description);
        }
        Validate.nonNegative(compareTolerance, "compare tolerance");

        // Allocate array space for caching calculated values.
        this.cornerLocations = new Vector3f[numCorners];
        this.squaredDistances = new Double[numCorners][numCorners];

        // Copy corner locations.
        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
            this.cornerLocations[cornerIndex]
                    = cornerArray[cornerIndex].clone();
        }

        // Set compare tolerances.
        this.tolerance = compareTolerance;
        this.tolerance2 = tolerance * tolerance;
    }

    /**
     * Instantiate a corner set from a list of corners.
     *
     * @param cornerList coordinates of the corners (not null or containing any
     * nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     */
    public CornerSet3f(List<Vector3f> cornerList, float compareTolerance) {
        Validate.nonNull(cornerList, "corner list");
        this.numCorners = cornerList.size();
        for (int index = 0; index < numCorners; ++index) {
            String description = String.format("cornerList[%d]", index);
            Validate.nonNull(cornerList.get(index), description);
        }
        Validate.nonNegative(compareTolerance, "compare tolerance");

        // Allocate array space for caching values.
        this.cornerLocations = new Vector3f[numCorners];
        this.squaredDistances = new Double[numCorners][numCorners];

        // Copy corner locations.
        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
            this.cornerLocations[cornerIndex]
                    = cornerList.get(cornerIndex).clone();
        }

        // Set compare tolerances.
        this.tolerance = compareTolerance;
        this.tolerance2 = tolerance * tolerance;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the location of a specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return a new coordinate vector
     */
    public Vector3f copyCornerLocation(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");
        Vector3f result = cornerLocations[cornerIndex].clone();
        return result;
    }

    /**
     * Copy the array of corner locations.
     *
     * @return a new array of new coordinate vectors
     */
    public Vector3f[] copyCornerLocations() {
        Vector3f[] result = new Vector3f[numCorners];

        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
            result[cornerIndex] = cornerLocations[cornerIndex].clone();
        }

        return result;
    }

    /**
     * Calculate the diameter of this corner set: the distance between its 2
     * most distant corners.
     *
     * @return distance (in world units, &ge;0)
     */
    public float diameter() {
        double largestSD = 0.0;

        for (int i = 0; i < numCorners; ++i) {
            for (int j = i + 1; j < numCorners; ++j) {
                double squaredDistance = squaredDistance(i, j);
                if (squaredDistance > largestSD) {
                    largestSD = squaredDistance;
                }
            }
        }
        float result = (float) Math.sqrt(largestSD);

        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the corner nearest to a specified location.
     *
     * @param location input coordinates (not null, unaffected)
     * @return index of the nearest corner (&ge;0, &lt;numCorners) or -1 if this
     * corner set has no corners
     */
    public int findCorner(Vector3f location) {
        int result = -1;
        double bestSD = Double.POSITIVE_INFINITY;
        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
            Vector3f corner = cornerLocations[cornerIndex];
            double squaredDistance
                    = MyVector3f.distanceSquared(corner, location);
            if (squaredDistance < bestSD) {
                result = cornerIndex;
                bestSD = squaredDistance;
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
     * Test (or look up) whether this corner set is planar.
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
     * Calculate (or look up) the largest triangle formed by any 3 corners.
     *
     * @return a new vector of 3 corner indices (sorted in ascending order) or
     * null if this corner set has &lt;3 corners
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
     * Read the number of corners.
     *
     * @return count (&ge; 0)
     */
    public int numCorners() {
        assert numCorners >= 0 : numCorners;
        return numCorners;
    }

    /**
     * Find a corner with which a specified location coincides.
     *
     * @param location input coordinates (not null, unaffected)
     * @return index of the coincident corner, or -1 if none coincide
     */
    public int onCorner(Vector3f location) {
        Validate.nonNull(location, "location");

        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
            if (onCorner(location, cornerIndex)) {
                return cornerIndex;
            }
        }
        return -1;
    }

    /**
     * Test whether a specified location coincides with a specified corner.
     *
     * @param location coordinates of the test location (not null, unaffected)
     * @param cornerIndex index of the corner (&ge;0, &lt;numCorners-1)
     * @return true if the location coincides, false if it doesn't
     */
    public boolean onCorner(Vector3f location, int cornerIndex) {
        Validate.nonNull(location, "location");
        validateIndex(cornerIndex, "corner index");

        Vector3f corner = cornerLocations[cornerIndex];
        boolean result = MyVector3f.doCoincide(corner, location, tolerance2);

        return result;
    }

    /**
     * Test whether this corner set shares one or more corners with another
     * corner set. The 2 corner sets must have identical tolerances.
     *
     * @param other the other corner set (not null)
     * @param storeSharedCorners if not null, a matrix of numCorners rows and
     * other.numCorners columns used to record shared corners (updated)
     * @return true if one or more shared corners were found, otherwise false
     */
    public boolean sharesCornerWith(
            CornerSet3f other, boolean[][] storeSharedCorners) {
        Validate.nonNull(other, "other corner set");
        if (other.getTolerance() != tolerance) {
            throw new IllegalArgumentException("tolerances differ");
        }
        if (storeSharedCorners != null) {
            if (storeSharedCorners.length != numCorners) {
                throw new IllegalArgumentException("wrong number of rows");
            }
            for (boolean[] row : storeSharedCorners) {
                if (row == null || row.length != other.numCorners()) {
                    throw new IllegalArgumentException(
                            "wrong number of columns");
                }
            }
        }

        boolean result = false;
        for (int otherI = 0; otherI < other.numCorners(); ++otherI) {
            Vector3f otherCorner = other.copyCornerLocation(otherI);
            for (int thisI = 0; thisI < numCorners; ++thisI) {
                Vector3f thisCorner = cornerLocations[thisI];
                if (MyVector3f.doCoincide(otherCorner, thisCorner, tolerance)) {
                    result = true;
                    if (storeSharedCorners != null) {
                        storeSharedCorners[thisI][otherI] = true;
                    }
                } else if (storeSharedCorners != null) {
                    storeSharedCorners[thisI][otherI] = false;
                }
            }
        }

        return result;
    }

    /**
     * Calculate the squared distance from a specified location to a specified
     * corner.
     *
     * @param location input coordinates (not null, unaffected)
     * @param cornerIndex index of the corner (&ge;0, &le;numCorners-1)
     * @return squared distance from location to corner (&ge;0)
     */
    public double squaredDistanceToCorner(Vector3f location, int cornerIndex) {
        Validate.nonNull(location, "location");
        validateIndex(cornerIndex, "corner index");

        Vector3f corner = cornerLocations[cornerIndex];
        double result = MyVector3f.distanceSquared(corner, location);

        assert result >= 0.0 : result;
        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Test whether all the corners in a specified subset lie on the line
     * connecting 2 specified corners.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param subset (length&ge;1, length&le;numCorners, unaffected)
     * @return true if the corners are collinear, false otherwise
     */
    protected boolean allCollinear(int cornerIndex1, int cornerIndex2,
            BitSet subset) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of 2nd corner");
        Validate.nonNull(subset, "subset");
        int length = subset.length();
        Validate.inRange(length, "length of subset", 1, numCorners);

        if (doCoincide(cornerIndex1, cornerIndex2)) {
            // The line is ill-defined.
            return true;
        }

        // Calculate the offset of the last corner from the first.
        Vector3f first = cornerLocations[cornerIndex1];
        Vector3f last = cornerLocations[cornerIndex2];
        Vector3f fl = last.subtract(first);

        for (int middleI = subset.nextSetBit(0);
                middleI >= 0; middleI = subset.nextSetBit(middleI + 1)) {
            assert middleI < numCorners : middleI;

            // Calculate the offset of the middle corner from the first.
            Vector3f middle = cornerLocations[middleI];
            Vector3f fm = middle.subtract(first);
            /*
             * Project FM onto FL.
             *
             * Don't use Vector3f.project() because (as of jME 3.0.10)
             * it contained a logic bug.
             */
            double fmDotFl = MyVector3f.dot(fm, fl);
            double normSquaredFL = squaredDistance(cornerIndex1, cornerIndex2);
            double fraction = fmDotFl / normSquaredFL;
            Vector3f projection = fl.mult((float) fraction);
            /*
             * If the projection coincides with FM,
             * then consider the 3 corners to be collinear.
             */
            boolean collin = MyVector3f.doCoincide(projection, fm, tolerance2);
            if (!collin) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether 3 corners are collinear.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param cornerIndex3 index of the 3rd corner (&ge;0, &lt;numCorners)
     * @param storeMiddleIndex if the result is true, used to store the index of
     * the middle corner, if determined
     * @return true if collinear, otherwise false
     */
    protected boolean areCollinear(int cornerIndex1, int cornerIndex2,
            int cornerIndex3, int[] storeMiddleIndex) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of 2nd corner");
        validateIndex(cornerIndex3, "index of 3rd corner");
        /*
         * Shortcut:
         * If any of the corners coincide, then the 3 corners are collinear,
         * but the middle corner is ill-defined.
         */
        if (doCoincide(cornerIndex1, cornerIndex2)
                || doCoincide(cornerIndex1, cornerIndex3)
                || doCoincide(cornerIndex2, cornerIndex3)) {
            return true;
        }

        // The hard way: find which 2 corners are most distant from each other.
        BitSet corners = new BitSet(numCorners);
        corners.set(cornerIndex1);
        corners.set(cornerIndex2);
        corners.set(cornerIndex3);
        int[] longest = mostDistant(corners);
        assert longest != null;
        int firstIndex = longest[0];
        int lastIndex = longest[1];

        // The remaining corner is the middle one.
        corners.clear(firstIndex);
        corners.clear(lastIndex);
        int middleIndex = corners.nextSetBit(0);

        boolean result = allCollinear(firstIndex, lastIndex, corners);
        if (result) {
            storeMiddleIndex[0] = middleIndex;
        }

        return result;
    }

    /**
     * Test whether 2 specified corners coincide.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @return true if they coincide, otherwise false
     */
    protected boolean doCoincide(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of 2nd corner");

        double squaredDistance = squaredDistance(cornerIndex1, cornerIndex2);
        if (squaredDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Find the most distant pair of corners in a specified subset.
     *
     * @param subset indices of the corners to consider (not null, unaffected)
     * @return a new array of 2 corner indices, or null if no pairs were found
     */
    protected int[] mostDistant(BitSet subset) {
        Validate.nonNull(subset, "subset");

        double largestSD = -1.0;
        int[] result = null;

        for (int i = subset.nextSetBit(0);
                i >= 0; i = subset.nextSetBit(i + 1)) {
            for (int j = subset.nextSetBit(i + 1);
                    j >= 0; j = subset.nextSetBit(j + 1)) {
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
     * Calculate the square of the area of the triangle formed by 3 specified
     * corners.
     *
     * @param indexA index of the first corner (&ge;0, &lt;numCorners)
     * @param indexB index of the 2nd corner (&ge;0, &lt;numCorners)
     * @param indexC index of the 3rd corner (&ge;0, &lt;numCorners)
     * @return area^2 (&ge;0)
     */
    protected double squaredArea(int indexA, int indexB, int indexC) {
        validateIndex(indexA, "index of first corner");
        validateIndex(indexB, "index of 2nd corner");
        validateIndex(indexC, "index of 3rd corner");

        // Calculate the offsets of B and C relative to A.
        Vector3f a = cornerLocations[indexA];
        Vector3f b = cornerLocations[indexB];
        Vector3f c = cornerLocations[indexC];
        Vector3f ab = b.subtract(a);
        Vector3f ac = c.subtract(a);

        Vector3f cross = ab.cross(ac);
        double areaSquared = MyVector3f.lengthSquared(cross) / 4.0;

        assert areaSquared >= 0.0 : areaSquared;
        return areaSquared;
    }

    /**
     * Calculate (or look up) the squared distance between 2 corners.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
     * @param cornerIndex2 index of the 2nd corner (&ge;0, &lt;numCorners)
     * @return squared distance (&ge;0)
     */
    protected double squaredDistance(int cornerIndex1, int cornerIndex2) {
        validateIndex(cornerIndex1, "index of first corner");
        validateIndex(cornerIndex2, "index of 2nd corner");

        if (squaredDistances[cornerIndex1][cornerIndex2] == null) {
            setSquaredDistance(cornerIndex1, cornerIndex2);
        }
        return squaredDistances[cornerIndex1][cornerIndex2];
    }

    /**
     * Validate a corner index as a method argument.
     *
     * @param index index to validate (&gt;0)
     * @param description description of the index (not null)
     * @throws IllegalArgumentException if the value is &lt;0 or &ge;numCorners
     */
    protected void validateIndex(int index, String description) {
        Validate.inRange(index, description, 0, numCorners - 1);
    }

    /**
     * Verify that this corner set is planar.
     *
     * @throws IllegalStateException if it is non-planar
     */
    protected void verifyPlanar() {
        if (!isPlanar()) {
            throw new IllegalStateException("non-planar corner set");
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the #isPlanar field. A corner set is planar if all corners lie
     * in a single plane.
     */
    private void setIsPlanar() {
        // Shortcut #1: all corner sets with less than 4 corners are planar.
        if (numCorners < 4) {
            setIsPlanar(true);
            return;
        }
        /*
         * Shortcut #2: if all corners have the same y-coordinate,
         * then the corner set is planar.
         */
        boolean sameY = true;
        float y = cornerLocations[0].y;
        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
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

        // The hard way: find the 3 corners which form the largest triangle.
        int[] triangle = largestTriangle();
        if (triangle == null) {
            /*
             * Degenerate case:
             * Treat as planar even though the plane is ill-defined.
             */
            setIsPlanar(true);
            return;
        }
        int aIndex = triangle[0];
        int bIndex = triangle[1];
        int cIndex = triangle[2];

        // Calculate the plane containing that triangle.
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
            setIsPlanar(true);
            return;
        }
        Vector3f planeNormal = crossProduct.divide(crossLength);
        float planeConstant = -planeNormal.dot(a);
        /*
         * If all the corners lie in that plane,
         * then the corner set is planar, otherwise it isn't.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; ++cornerIndex) {
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
        double largestSA = -1.0;
        int[] largest = null;
        for (int i = 0; i < numCorners - 2; ++i) {
            for (int j = i + 1; j < numCorners - 1; ++j) {
                for (int k = j + 1; k < numCorners; ++k) {
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

        this.largestTriangle = new int[3];
        this.largestTriangle[0] = newValue[0];
        this.largestTriangle[1] = newValue[1];
        this.largestTriangle[2] = newValue[2];
    }

    /**
     * Initialize the element(s) of the #squaredDistances field for a particular
     * pair of corners.
     *
     * @param cornerIndex1 index of the first corner (&ge;0, &lt;numCorners)
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
     * @param ci1 index of the first corner (&ge;0, &lt;numCorners)
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

        this.squaredDistances[ci1][ci2] = newValue;
        this.squaredDistances[ci2][ci1] = newValue;
    }
}

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
package jme3utilities.math.polygon;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * An immutable polygon in 3-dimensional space, consisting of N corners (points)
 * connected by N sides (straight-line segments) to form a loop. It may be
 * degenerate and/or planar, though it need not be. For efficiency, many
 * calculated values are cached.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Polygon3f extends CornerSet3f {
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
     * cached dot product at each corner (allocated by constructor; initialized
     * by #setCornerProducts())
     */
    final private Double[] dotProducts;
    /**
     * cached cross product at each corner (allocated by constructor;
     * initialized by #setCornerProducts())
     */
    final private Vector3f[] crossProducts;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a polygon from an array of corners.
     *
     * @param cornerArray locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used when comparing locations
     * for coincidence
     */
    public Polygon3f(Vector3f[] cornerArray, float compareTolerance) {
        super(cornerArray, compareTolerance);
        /*
         * Allocate array space for caching values.
         */
        crossProducts = new Vector3f[numCorners];
        dotProducts = new Double[numCorners];
    }

    /**
     * Instantiate a polygon from a list of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used when comparing locations
     * for coincidence
     */
    public Polygon3f(List<Vector3f> cornerList, float compareTolerance) {
        super(cornerList, compareTolerance);
        /*
         * Allocate array space for caching values.
         */
        crossProducts = new Vector3f[numCorners];
        dotProducts = new Double[numCorners];
    }
    // *************************************************************************
    // new methods exposed

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
     * @return a new vector
     */
    public Vector3f crossProduct(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");

        if (crossProducts[cornerIndex] == null) {
            setCornerProducts(cornerIndex);
        }
        Vector3f result = crossProducts[cornerIndex].clone();

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
            setCornerProducts(cornerIndex);
        }
        double result = dotProducts[cornerIndex];

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
        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
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
        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
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
     * Find the side closest to a specified location.
     *
     * @param location coordinates of input location (not null, unaffected)
     * @param storeClosest if not null, used to store the coordinates of the
     * closest perimeter location, if determined (updated)
     * @return index of closest the side (&ge;0, &lt;numCorners) or -1 if this
     * polygon has no sides
     */
    public int findSide(Vector3f location, Vector3f storeClosest) {
        Validate.nonNull(location, "location");

        int result = -1;
        double leastSD = Double.POSITIVE_INFINITY;
        Vector3f closestCurrentSide = new Vector3f();
        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
            double squaredDistance = squaredDistanceToSide(
                    location, sideIndex, closestCurrentSide);
            if (squaredDistance < leastSD) {
                result = sideIndex;
                leastSD = squaredDistance;
                if (storeClosest != null) {
                    storeClosest.set(closestCurrentSide);
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
     * @return a new polygon with at least 2 corners
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
            ++newNumCorners;
        }
        assert newNumCorners >= 2 : newNumCorners;
        assert newNumCorners <= numCorners : newNumCorners;
        /*
         * Allocate and fill the array of corner locations.
         */
        Vector3f[] newCornerLocations = new Vector3f[newNumCorners];
        int newI = 0;
        for (int oldI = firstIndex; oldI != lastIndex; oldI = nextIndex(oldI)) {
            newCornerLocations[newI] = cornerLocations[oldI];
            ++newI;
        }
        assert newI == newNumCorners - 1;
        newCornerLocations[newI] = cornerLocations[lastIndex];

        Polygon3f result = new Polygon3f(newCornerLocations, tolerance);
        assert result.numCorners() == newNumCorners;

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
     * Calculate the midpoint of a specified side.
     *
     * @param sideIndex which side (&ge;0, &lt;numCorners)
     * @return a new coordinate vector
     */
    public Vector3f midpoint(int sideIndex) {
        validateIndex(sideIndex, "side index");

        Vector3f corner1 = cornerLocations[sideIndex];
        int next = nextIndex(sideIndex);
        Vector3f corner2 = cornerLocations[next];
        Vector3f result = MyVector3f.midpoint(corner1, corner2, null);

        assert onSide(result, sideIndex);
        return result;
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
     * Find a side on which a specified location lies.
     *
     * @param location coordinates of input location (not null, unaffected)
     * @return index of the side, or -1 if location not on perimeter
     */
    public int onSide(Vector3f location) {
        Validate.nonNull(location, "location");

        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
            if (onSide(location, sideIndex)) {
                return sideIndex;
            }
        }
        return -1;
    }

    /**
     * Test whether a specified location lies on a specified side.
     *
     * @param location coordinates of test location (not null, unaffected)
     * @param sideIndex index of the side (&ge;0, &lt;numCorners-1)
     * @return true if the location lies on the side, false if it doesn't
     */
    public boolean onSide(Vector3f location, int sideIndex) {
        Validate.nonNull(location, "location");
        validateIndex(sideIndex, "side index");

        double squaredDistance
                = squaredDistanceToSide(location, sideIndex, null);
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
        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
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
     * Test whether this polygon shares one or more sides with another polygon.
     * The polygons must have the same tolerances.
     *
     * @param other the other polygon (not null)
     * @param storeSharedSides if not null, a matrix of numCorners rows and
     * other.numCorners columns used to record shared sides (updated)
     * @return true if one or more shared sides were found, otherwise false
     */
    public boolean sharesSideWith(Polygon3f other,
            boolean[][] storeSharedSides) {
        Validate.nonNull(other, "other polygon");
        if (other.getTolerance() != tolerance) {
            throw new IllegalArgumentException("tolerances differ");
        }
        if (storeSharedSides != null) {
            if (storeSharedSides.length != numCorners) {
                throw new IllegalArgumentException("wrong number of rows");
            }
            for (boolean[] row : storeSharedSides) {
                if (row == null || row.length != other.numCorners()) {
                    throw new IllegalArgumentException(
                            "wrong number of columns");
                }
            }
        }

        boolean[][] cornerMap = new boolean[numCorners][other.numCorners()];
        boolean result = sharesCornerWith(other, cornerMap);
        if (result == false) {
            return false;
        }

        result = false;
        for (int otherI = 0; otherI < other.numCorners(); ++otherI) {
            int otherN = other.nextIndex(otherI);
            for (int thisI = 0; thisI < numCorners; ++thisI) {
                if (cornerMap[thisI][otherI]) {
                    int thisN = nextIndex(thisI);
                    if (cornerMap[thisN][otherN]) {
                        result = true;
                        if (storeSharedSides != null) {
                            storeSharedSides[thisI][otherI] = true;
                        }
                    }
                    int thisP = prevIndex(thisI);
                    if (cornerMap[thisP][otherN]) {
                        result = true;
                        if (storeSharedSides != null) {
                            storeSharedSides[thisP][otherI] = true;
                        }
                    }
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
     * Calculate the squared distance from a specified location to a specified
     * side.
     *
     * @param location coordinates of the location (not null, unaffected)
     * @param sideIndex index of the side (&ge;0, &lt;numCorners-1)
     * @param storeClosest if not null, used to store the coordinates of the
     * closest location (updated)
     * @return squared distance from location to side (&ge;0)
     */
    public double squaredDistanceToSide(Vector3f location, int sideIndex,
            Vector3f storeClosest) {
        Validate.nonNull(location, "location");
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
            if (storeClosest != null) {
                storeClosest.set(corner1);
            }
            double result = MyVector3f.distanceSquared(corner1, location);
            return result;
        }
        /*
         * Calculate parametric value for the closest point on that line.
         */
        Vector3f pointOffset = location.subtract(corner1);
        double dot = MyVector3f.dot(pointOffset, sideOffset);
        double t = dot / sideLengthSquared;
        /*
         * Calculate offset of the closest point on the side.
         */
        float scaleFactor = FastMath.clamp((float) t, 0f, 1f);
        Vector3f closestOffset = sideOffset.mult(scaleFactor);
        if (storeClosest != null) {
            storeClosest.set(corner1);
            storeClosest.addLocal(closestOffset);
        }
        double result = MyVector3f.distanceSquared(closestOffset, pointOffset);

        assert result >= 0.0 : result;
        return result;
    }
    // *************************************************************************
    // new protected methods

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
    // *************************************************************************
    // private methods

    /**
     * Initialize the elements of the #cosTurnAngles and #crossProducts fields
     * for a specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     */
    private void setCornerProducts(int cornerIndex) {
        int nextI = nextIndex(cornerIndex);
        int prevI = prevIndex(cornerIndex);
        /*
         * Calculate the offsets of the 2 adjacent sides.
         */
        Vector3f a = cornerLocations[prevI];
        Vector3f b = cornerLocations[cornerIndex];
        Vector3f c = cornerLocations[nextI];
        Vector3f offsetAB = b.subtract(a);
        Vector3f offsetBC = c.subtract(b);

        double dot = MyVector3f.dot(offsetAB, offsetBC);
        Vector3f cross = offsetAB.cross(offsetBC);
        setCornerProducts(cornerIndex, dot, cross);
    }

    /**
     * Direct setter for the #cosTurnAngles and #crossProduct fields.
     *
     * @param cornerIndex index of the corner (&ge;0, &lt;numCorners)
     * @param newDot value for dot product
     * @param newCross vector for cross product (not null, unaffected)
     */
    private void setCornerProducts(int cornerIndex, double newDot,
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
     * (1) fewer than 3 corners OR (2) coincident corners OR (3) a 180-degree
     * turn. In a non-degenerate polygon, each corner forms an angle less than
     * Pi radians, though the sign of the angle may be ambiguous.
     */
    private void setIsDegenerate() {
        if (numCorners < 3) {
            setIsDegenerate(true);
            return;
        }
        /*
         * (2) Test for coincident corners.
         */
        for (int iCorner = 0; iCorner < numCorners; ++iCorner) {
            for (int jCorner = iCorner + 1; jCorner < numCorners; ++jCorner) {
                if (doCoincide(iCorner, jCorner)) {
                    setIsDegenerate(true);
                    return;
                }
            }
        }
        /*
         * (3) Test for 180-degree turns.
         */
        for (int cornerI = 0; cornerI < numCorners; ++cornerI) {
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
     * @param newValue new value for field
     */
    private void setIsDegenerate(boolean newValue) {
        assert isDegenerate == null;
        isDegenerate = newValue;
    }
}

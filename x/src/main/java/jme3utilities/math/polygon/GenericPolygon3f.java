/*
 Copyright (c) 2017-2021, Stephen Gold
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
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * An immutable polygon in 3-dimensional space, consisting of N corners (points)
 * connected by N sides (straight-line segments) to form a loop. The polygon
 * must be non-degenerate, which implies it has N&ge;3, all corners distinct,
 * and no 180-degree turns. The polygon may also be planar and/or
 * self-intersecting, though it need not be.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GenericPolygon3f extends Polygon3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            GenericPolygon3f.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, then 2 (or more) sides intersect at some location other their
     * shared corners (set by #setIsSelfIntersecting())
     */
    private Boolean isSelfIntersecting = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a generic polygon from an array of corners.
     *
     * @param cornerArray locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     * @throws IllegalArgumentException if the corners provided don't form a
     * generic polygon
     */
    public GenericPolygon3f(Vector3f[] cornerArray, float compareTolerance) {
        super(cornerArray, compareTolerance);
        /*
         * Verify that the polygon is generic.
         */
        if (isDegenerate()) {
            throw new IllegalArgumentException("degenerate polygon");
        }
    }

    /**
     * Instantiate a generic polygon from a list of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     * @throws IllegalArgumentException if the corners provided don't form a
     * generic polygon
     */
    public GenericPolygon3f(List<Vector3f> cornerList, float compareTolerance) {
        super(cornerList, compareTolerance);
        /*
         * Verify that the polygon is generic.
         */
        if (isDegenerate()) {
            throw new IllegalArgumentException("degenerate polygon");
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test (or look up) whether this polygon is self-intersecting.
     *
     * @return true if it is self-intersecting, otherwise false
     */
    final public boolean isSelfIntersecting() {
        if (isSelfIntersecting == null) {
            setIsSelfIntersecting();
        }

        return isSelfIntersecting;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Test whether 2 line segments (each connecting 2 distinct corners)
     * intersect (apart from shared any corners).
     *
     * @param corner1 index of first corner, first segment (&ge;0,
     * &lt;numCorners)
     * @param partner1 index of 2nd corner, first segment (&ge;0,
     * &lt;numCorners)
     * @param corner2 index of first corner, 2nd segment (&ge;0, &lt;numCorners)
     * @param partner2 index of 2nd corner, 2nd segment (&ge;0, &lt;numCorners)
     * @return true if the segments intersect, otherwise false
     */
    protected boolean doSegmentsIntersect(int corner1, int partner1,
            int corner2, int partner2) {
        validateIndex(corner1, "index of first corner of first segment");
        validateIndex(partner1, "index of 2nd corner of first segment");
        if (corner1 == partner1) {
            throw new IllegalArgumentException("first segment is trivial.");
        }
        validateIndex(corner2, "index of first corner of 2nd segment");
        validateIndex(partner2, "index of 2nd corner of 2nd segment");
        if (corner2 == partner2) {
            throw new IllegalArgumentException("2nd segment is trivial.");
        }
        /*
         * Check for any corners shared between the 2 segments.
         */
        BitSet corners = new BitSet(numCorners);
        corners.set(corner1);
        corners.set(partner1);
        corners.set(corner2);
        corners.set(partner2);
        int numUnique = corners.cardinality();
        assert numUnique >= 2 : numUnique;
        assert numUnique <= 4 : numUnique;

        if (numUnique == 2) {
            assert (corner1 == partner2 && corner2 == partner1)
                    || (corner1 == partner1 && corner2 == partner2);
            /*
             * 2 shared corners, so the segments coincide.
             */
            return true;
        }
        /*
         * Find a vector N perpendicular to the directions of both segments.
         */
        Vector3f p1 = cornerLocations[corner1];
        Vector3f q1 = cornerLocations[partner1];
        Vector3f offset1 = q1.subtract(p1);
        Vector3f p2 = cornerLocations[corner2];
        Vector3f q2 = cornerLocations[partner2];
        Vector3f offset2 = q2.subtract(p2);
        Vector3f n = offset1.cross(offset2);

        if (MyVector3f.lengthSquared(n) < tolerance2) {
            /*
             * The segments are parallel. Are they also collinear?
             * Find the most distant pair of corners.
             */
            int[] longest = mostDistant(corners);
            assert longest != null;
            int firstI = longest[0];
            int lastI = longest[1];
            assert firstI != lastI;
            /*
             * Test the middle 2 corners.
             */
            BitSet middleCorners = (BitSet) corners.clone();
            middleCorners.clear(firstI);
            middleCorners.clear(lastI);
            boolean collin = allCollinear(firstI, lastI, middleCorners);
            if (collin) {
                /*
                 * All corners are collinear.  The segments intersect
                 * if and only if they overlap.
                 */
                boolean result = isOverlap(firstI, corner1, partner1,
                        corner2, partner2);
                return result;
            } else {
                /*
                 * The segments are parallel but not collinear, so they
                 * do not intersect.
                 */
                assert numUnique == 4 : numUnique;
                return false;
            }
        }
        if (numUnique == 3) {
            /*
             * Segments not parallel with one shared corner.
             */
            return false;
        }
        assert numUnique == 4 : numUnique;
        /*
         * Segments not parallel and no shared corners.
         * Extend the segments into straight lines and find the
         * locations of closest approach (c1 and c2) on each line.
         * The segments intersect if and only if c1 and c2 not only
         * lie on their respective segments but also coincide.
         */
        Vector3f n1 = offset1.cross(n);
        Vector3f n2 = offset2.cross(n);
        float t1 = p2.subtract(p1).dot(n2) / offset1.dot(n2);
        float t2 = p1.subtract(p2).dot(n1) / offset2.dot(n1);
        Vector3f c1 = offset1.mult(t1).add(p1);
        Vector3f c2 = offset2.mult(t2).add(p2);
        if (!MyVector3f.doCoincide(c1, c2, tolerance2)) {
            return false;
        }
        double fuzz1 = tolerance2 / squaredDistance(corner1, partner1);
        if (t1 < 0f && t1 * t1 > fuzz1) {
            return false;
        }
        float ct1 = 1f - t1;
        if (ct1 < 0f && ct1 * ct1 > fuzz1) {
            return false;
        }
        double fuzz2 = tolerance2 / squaredDistance(corner2, partner2);
        if (t2 < 0 && t2 * t2 > fuzz2) {
            return false;
        }
        float ct2 = 1f - t2;
        if (ct2 < 0f && ct2 * ct2 > fuzz2) {
            return false;
        }
        return true;
    }

    /**
     * Test whether 2 sides intersect (other than at a shared corner).
     *
     * @param side1 index of the first side (&ge;0, &lt;numCorners)
     * @param side2 index of the 2nd side (&ge;0, &lt;numCorners)
     * @return true if they intersect, otherwise false
     */
    protected boolean doSidesIntersect(int side1, int side2) {
        validateIndex(side1, "index of first side");
        validateIndex(side2, "index of 2nd side");

        int next1 = nextIndex(side1);
        int next2 = nextIndex(side2);

        if (doSegmentsIntersect(side1, next1, side2, next2)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find a location (if any) where the specified segment intersects the
     * perimeter, preferably at a corner.
     *
     * @param startLocation coordinates of start of segment (not null,
     * unaffected)
     * @param endLocation coordinates of end of segment (not null, unaffected)
     * @return a new coordinate vector or null if no intersection found
     */
    protected Vector3f intersectionWithPerimeter(Vector3f startLocation,
            Vector3f endLocation) {
        Validate.nonNull(startLocation, "start location");
        Validate.nonNull(endLocation, "end location");
        /*
         * Check for intersection at a corner.
         */
        for (int cornerIndex = 0; cornerIndex < numCorners; cornerIndex++) {
            Vector3f result = intersectionWithCorner(cornerIndex, startLocation,
                    endLocation);
            if (result != null) {
                return result;
            }
        }
        /*
         * Check for intersection with a side.
         */
        for (int sideIndex = 0; sideIndex < numCorners; sideIndex++) {
            Vector3f result = intersectionWithSide(sideIndex, startLocation,
                    endLocation);
            if (result != null) {
                return result;
            }
        }
        /*
         * No intersection found.
         */
        return null;
    }
    // *************************************************************************
    // private methods

    /**
     * Find a location (if any) where the specified segment intersects the
     * specified corner.
     *
     * @param cornerIndex (&ge;0, &lt;numCorners)
     * @param start coordinates of start of segment (not null, unaffected)
     * @param end coordinates of end of segment (not null, unaffected)
     * @return a new coordinate vector or null if no intersection found
     */
    private Vector3f intersectionWithCorner(int cornerIndex, Vector3f start,
            Vector3f end) {
        validateIndex(cornerIndex, "corner index");
        Validate.nonNull(start, "start location");
        Validate.nonNull(end, "end location");
        /*
         * Calculate the direction of a straight line containing the segment.
         */
        Vector3f segmentOffset = end.subtract(start);
        /*
         * If the segment has zero length, test its start for coincidence
         * with the corner.
         */
        double segmentDS = MyVector3f.lengthSquared(segmentOffset);
        Vector3f corner = cornerLocations[cornerIndex];
        if (segmentDS == 0.0) {
            if (MyVector3f.doCoincide(start, corner, tolerance2)) {
                return corner.clone();
            } else {
                return null;
            }
        }
        /*
         * Calculate parametric value for the closest point on that line.
         */
        Vector3f cornerOffset = corner.subtract(start);
        double dot = MyVector3f.dot(cornerOffset, segmentOffset);
        double t = dot / segmentDS;
        /*
         * Calculate offset of the closest point on the segment.
         */
        float scaleFactor = FastMath.clamp((float) t, 0f, 1f);
        Vector3f closestOffset = segmentOffset.mult(scaleFactor);
        double result = MyVector3f.distanceSquared(closestOffset, cornerOffset);

        if (result > tolerance2) {
            return null;
        } else {
            return corner.clone();
        }
    }

    /**
     * Find a location (if any) where the specified segment intersects the
     * specified side.
     *
     * @param sideIndex (&ge;0, &lt;numCorners)
     * @param start coordinates of start of segment (not null, unaffected)
     * @param end coordinates of end of segment (not null, unaffected)
     * @return a new coordinate vector or null if no intersection found
     */
    private Vector3f intersectionWithSide(int sideIndex,
            Vector3f start, Vector3f end) {
        validateIndex(sideIndex, "side index");
        Validate.nonNull(start, "start location");
        Validate.nonNull(end, "end location");

        Vector3f corner1 = cornerLocations[sideIndex];
        int corner2Index = nextIndex(sideIndex);
        Vector3f corner2 = cornerLocations[corner2Index];
        Vector3f result = MyVector3f.intersectSegments(corner1, corner2,
                start, end, tolerance2);

        return result;
    }

    /**
     * Helper method to determine whether 2 collinear segments overlap.
     *
     * @param ext index of an outer corner (&ge;0, &lt; numCorners)
     * @param corner1 index of first corner, first segment (&ge;0,
     * &lt;numCorners)
     * @param partner1 index of 2nd corner, first segment (&ge;0,
     * &lt;numCorners)
     * @param corner2 index of first corner, 2nd segment (&ge;0, &lt;numCorners)
     * @param partner2 index of 2nd corner, 2nd segment (&ge;0, &lt;numCorners)
     */
    private boolean isOverlap(int ext, int corner1, int partner1,
            int corner2, int partner2) {
        assert corner1 != partner1;
        assert corner2 != partner2;
        assert (corner1 != partner2 || corner2 != partner1)
                && (corner1 != partner1 || corner2 != partner2);
        /*
         * Test the partner of ext against both corners of the other segment.
         */
        int otherCorner, otherPartner;
        if (ext == corner1 || ext == partner1) {
            otherCorner = corner2;
            otherPartner = partner2;
        } else {
            assert ext == corner2 || ext == partner2;
            otherCorner = corner1;
            otherPartner = partner1;
        }
        int extPartner;
        if (ext == corner1) {
            extPartner = partner1;
        } else if (ext == partner1) {
            extPartner = corner1;
        } else if (ext == corner2) {
            extPartner = partner2;
        } else {
            assert ext == partner2;
            extPartner = corner2;
        }

        if (ext == otherCorner || ext == otherPartner) {
            return true;
        }
        double sdPartner = squaredDistance(ext, extPartner);
        double sdOther = squaredDistance(ext, otherCorner);
        if (sdPartner > sdOther) {
            return true;
        }
        double sdOtherPartner = squaredDistance(ext, otherPartner);
        if (sdPartner > sdOtherPartner) {
            return true;
        }
        /*
         * Segments are disjoint.
         */
        return false;
    }

    /**
     * Initialize the #isSelfIntersecting field. The polygon is
     * self-intersecting if 2 (or more) sides intersect at some location other
     * their corners.
     */
    private void setIsSelfIntersecting() {
        assert isSelfIntersecting == null : isSelfIntersecting;
        /*
         * consider each pair of sides
         */
        for (int sideI = 0; sideI < numCorners; sideI++) {
            for (int sideJ = sideI + 1; sideJ < numCorners; sideJ++) {
                if (doSidesIntersect(sideI, sideJ)) {
                    isSelfIntersecting = true;
                    return;
                }
            }
        }

        isSelfIntersecting = false;
    }
}

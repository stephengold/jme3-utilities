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

import com.jme3.math.Vector3f;
import java.util.BitSet;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;

/**
 * An immutable, non-degenerate polygon in three-dimensional space, consisting
 * of N corners (points) connected by N sides (straight-line segments). The
 * polygon must be non-degenerate, which implies it has N&ge;3, all corners
 * distinct, and no consecutive sides collinear. The polygon may also be planar,
 * though it need not be.
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
            ConvexPolygon3f.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, then two (or more) sides intersect at some location other their
     * corners (set by setIsSelfIntersecting())
     */
    private Boolean isSelfIntersecting = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a generic polygon with the specified sequence of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     * @throws IllegalArgumentException if the corners provided don't form a
     * generic polygon
     */
    public GenericPolygon3f(Vector3f[] cornerList, float compareTolerance) {
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
    // protected methods

    /**
     * Test whether two sides intersect (other than at a shared corner).
     *
     * @param sideI1 index of the first side (&ge;0, &lt;numCorners)
     * @param sideI2 index of the second side (&ge;0, &lt;numCorners)
     * @return true if they coincide, otherwise false
     */
    protected boolean doIntersect(int sideI1, int sideI2) {
        validateIndex(sideI1, "index of first side");
        validateIndex(sideI2, "index of second side");
        /*
         * Check for any corners shared between the two sides. 
         */
        BitSet corners = new BitSet(numCorners);
        corners.set(sideI1);
        corners.set(sideI2);
        int next1 = nextIndex(sideI1);
        int next2 = nextIndex(sideI2);
        corners.set(next1);
        corners.set(next2);
        int numUnique = corners.cardinality();
        assert numUnique >= 2 : numUnique;
        assert numUnique <= 4 : numUnique;
        if (numUnique == 4) {
            /*
             * Four unique corners, so none shared between the two sides.
             * Find a vector N perpendicular to the directions of both sides.
             */
            Vector3f p1 = cornerLocations[sideI1];
            Vector3f q1 = cornerLocations[next1];
            Vector3f offset1 = q1.subtract(p1);
            Vector3f p2 = cornerLocations[sideI2];
            Vector3f q2 = cornerLocations[next2];
            Vector3f offset2 = q2.subtract(p2);
            Vector3f n = offset1.cross(offset2);

            if (MyVector3f.lengthSquared(n) < tolerance2) {
                /*
                 * The sides are parallel. Are they also collinear? 
                 * Find the most distant pair of corners.
                 */
                int[] longest = mostDistant(corners);
                assert longest != null;
                int firstI = longest[0];
                int lastI = longest[1];
                assert firstI != lastI;
                /*
                 * Test the remaining two corners.
                 */
                BitSet middleCorners = (BitSet) corners.clone();
                middleCorners.clear(firstI);
                middleCorners.clear(lastI);
                boolean collin = allCollinear(firstI, lastI, middleCorners);
                if (collin) {
                    /*
                     * All four corners are collinear.  The sides intersect
                     * if and only if they overlap.
                     */
                    boolean result = isOverlap(firstI, sideI1, sideI2);
                    return result;
                } else {
                    /*
                     * The sides are parallel and not collinear, so they
                     * do not intersect.
                     */
                    return false;
                }
            }
            /*
             * Extend the sides into straight lines and find the 
             * points of closest approach (c1 and c2) on each line. 
             * The sides intersect if and only if c1 and c2 both lie 
             * on their respective sides, and they also coincide.
             */
            Vector3f n1 = offset1.cross(n);
            Vector3f n2 = offset2.cross(n);
            float t1 = p2.subtract(p1).dot(n2) / offset1.dot(n2);
            if (t1 <= 0f || t1 >= 1f) {
                /*
                 * Nearest point on the first line is outside the side.
                 */
                return false;
            }
            float t2 = p1.subtract(p2).dot(n1) / offset2.dot(n1);
            if (t2 <= 0f || t2 >= 1f) {
                /*
                 * Nearest point on the second line is outside the side.
                 */
                return false;
            }
            Vector3f c1 = offset1.mult(t1).add(p1);
            Vector3f c2 = offset2.mult(t2).add(p2);
            if (MyVector3f.doCoincide(c1, c2, tolerance2)) {
                return true;
            } else {
                return false;
            }

        } else if (sideI1 == sideI2) {
            assert numUnique == 2 : numUnique;
            /*
             * Two shared corners, so the sides definitely intersect.
             */
            return true;

        }
        assert numUnique == 3 : numUnique;
        /*
         * Sharing exactly one corner.
         */
        return false;
    }
    // *************************************************************************
    // private methods

    /**
     * Helper method to determine whether two distinct-but-collinear sides
     * overlap.
     *
     * @param firstI index of an outer corner (&ge;0, &lt; numCorners)
     * @param sideIndex1 index of the first side (&ge;0, &lt; numCorners)
     * @param sideIndex2 index of the second side (&ge;0, &lt; numCorners)
     */
    private boolean isOverlap(int firstI, int sideIndex1, int sideIndex2) {
        int next1 = nextIndex(sideIndex1);
        int next2 = nextIndex(sideIndex2);
        /*
         * Test the partner of firstI against both corners of the other side.
         */
        int otherEdge, partner;
        if (firstI == sideIndex1) {
            otherEdge = sideIndex2;
            partner = next1;
        } else if (firstI == next1) {
            otherEdge = sideIndex2;
            partner = sideIndex1;
        } else if (firstI == sideIndex2) {
            otherEdge = sideIndex1;
            partner = next2;
        } else {
            assert firstI == next2;
            otherEdge = sideIndex1;
            partner = sideIndex2;
        }
        double sdPartner = squaredDistance(firstI, partner);
        double sdOther = squaredDistance(firstI, otherEdge);
        if (sdPartner > sdOther) {
            return true;
        } else {
            int otherNext = nextIndex(otherEdge);
            double sdOtherNext = squaredDistance(firstI, otherNext);
            if (sdPartner > sdOtherNext) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Initialize the isSelfIntersecting field. The polygon is self-intersecting
     * if two (or more) sides intersect at some location other their corners.
     */
    private void setIsSelfIntersecting() {
        /*
         * consider each pair of sides
         */
        for (int sideI = 0; sideI < numCorners; sideI++) {
            for (int sideJ = sideI + 1; sideJ < numCorners; sideJ++) {
                if (doIntersect(sideI, sideJ)) {
                    isSelfIntersecting = true;
                    return;
                }
            }
        }

        isSelfIntersecting = false;
    }
}

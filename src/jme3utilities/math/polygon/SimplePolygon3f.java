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
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.spline.Locus3f;
import jme3utilities.math.VectorXZ;
import jme3utilities.math.spline.LinearSpline3f;
import jme3utilities.math.spline.Spline3f;

/**
 * An immutable polygon in three-dimensional space, consisting of N corners
 * (points) connected by N sides (straight-line segments). The polygon must be
 * simple, in other words: non-degenerate, planar, and non-self-intersecting.
 * This means it has a well-defined interior and exterior. It may also be
 * convex, though it need not be.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SimplePolygon3f
        extends GenericPolygon3f
        implements Locus3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SimplePolygon3f.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * if true, then one (or more) of the internal angles is &gt;180 degrees
     * (set by #setIsConvex())
     */
    private Boolean isConvex = null;
    /**
     * cached constant of the plane containing the polygon (initialized by
     * #setPlane())
     */
    private Float planeConstant = null;
    /**
     * cached signed area of this polygon (&ne;0, initialized by
     * setSignedArea()).
     */
    private Float signedArea = null;
    /**
     * cached planar offset of the centroid of a this polygon (initialized by
     * #setCentroid()).
     */
    private VectorXZ centroid = null;
    /**
     * cached normal vector of the plane containing the polygon (unit vector,
     * initialized by #setPlane())
     */
    private Vector3f planeNormal = null;
    /**
     * cached 1st basis vector for planar offsets (unit vector, initialized by
     * #setPlane())
     */
    private Vector3f planeXBasis = null;
    /**
     * cached 2nd basis vector for planar offsets (unit vector, initialized by
     * #setPlane())
     */
    private Vector3f planeZBasis = null;
    /**
     * cached planar offsets relative to the first corner (allocated by
     * constructor, initialized by #setPlanarOffset())
     */
    final private VectorXZ[] planarOffsets;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a simple polygon with the specified sequence of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls)
     * @param compareTolerance tolerance (&ge;0) used when comparing two points
     * or testing for intersection
     * @throws IllegalArgumentException if the corners provided don't form a
     * simple polygon
     */
    public SimplePolygon3f(Vector3f[] cornerList, float compareTolerance) {
        super(cornerList, compareTolerance);
        /*
         * Verify that the polygon is convex.
         */
        if (!isPlanar()) {
            throw new IllegalArgumentException("non-planar polygon");
        }
        if (isSelfIntersecting()) {
            throw new IllegalArgumentException("self-intersecting polygon");
        }
        /*
         * Allocate array space.
         */
        planarOffsets = new VectorXZ[numCorners];
        planarOffsets[0] = new VectorXZ(0f, 0f);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the unsigned area of the polygon.
     *
     * @return area in square units (&ge;0)
     */
    public float area() {
        if (signedArea == null) {
            setSignedArea();
        }
        float result = FastMath.abs(signedArea);

        return result;
    }

    /**
     * Calculate (or look up) the centroid of the polygon.
     *
     * @return a new vector with coordinates of the centroid
     */
    public Vector3f centroid() {
        if (centroid == null) {
            setCentroid();
        }
        Vector3f result = cornerLocations[0].clone();
        result.addLocal(planeXBasis.mult(centroid.getX()));
        result.addLocal(planeZBasis.mult(centroid.getZ()));

        return result;
    }

    /**
     * Test whether a specific point lies in the plane of this polygon.
     *
     * @param point coordinates of the point (not null, unaffected)
     * @return true if the point lies in the plane, false if it doesn't
     */
    public boolean inPlane(Vector3f point) {
        Validate.nonNull(point, "point");

        if (planeConstant == null) {
            setPlane();
        }

        float pseudoDistance = planeNormal.dot(point) + planeConstant;
        if (pseudoDistance * pseudoDistance > tolerance2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculate the interior angle at the specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return angle (in radians, &gt;0, &lt;2*Pi)
     */
    public double interiorAngle(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");

        double result = Math.PI - turnAngle(cornerIndex);

        assert result > 0f : result;
        assert result < 2 * Math.PI : result;
        return result;
    }

    /**
     * Test (or look up) whether this simple polygon is convex.
     *
     * @return true if convex, false if concave.
     */
    public boolean isConvex() {
        if (isConvex == null) {
            setIsConvex();
        }

        return isConvex;
    }

    /**
     * Calculate (or look up) the planar offset of the specified corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return planar offset (not null)
     */
    public VectorXZ planarOffset(int cornerIndex) {
        validateIndex(cornerIndex, "planar offset");

        if (planarOffsets[cornerIndex] == null) {
            setPlanarOffset(cornerIndex);
        }
        VectorXZ result = planarOffsets[cornerIndex];

        assert result != null;
        return result;
    }

    /**
     * Calculate the signed turn angle at the specified corner. This is
     * sometimes called the "external angle".
     *
     * The sign of the angle is positive for an inward turn, negative for an
     * outward turn.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     * @return angle (in radians, &gt;-Pi, &lt;Pi)
     */
    public double turnAngle(int cornerIndex) {
        validateIndex(cornerIndex, "corner index");
        /*
         * Calculate the magnitude of the turn.
         */
        double turnMagnitude = absTurnAngle(cornerIndex);
        assert !Double.isNaN(turnMagnitude);
        assert turnMagnitude >= 0f : turnMagnitude;
        assert turnMagnitude < Math.PI : turnMagnitude;
        /*
         * Calculate the direction/sign of the turn.
         */
        if (planeNormal == null) {
            setPlane();
        }
        Vector3f crossProduct = crossProduct(cornerIndex);
        float sign = planeNormal.dot(crossProduct);

        double result = Math.copySign(turnMagnitude, sign);

        assert result > -Math.PI : result;
        assert result < Math.PI : result;
        return result;
    }
    // *************************************************************************
    // Locus3f methods

    /**
     * Test whether this polygon contains a specific location.
     *
     * @param location coordinates of location to test (not null, unaffected)
     * @return true if location is in this polygon, false otherwise
     */
    @Override
    public boolean contains(Vector3f point) {
        Validate.nonNull(point, "point");

        if (!inPlane(point)) {
            return false;
        }

        Vector3f closestLocation = new Vector3f();
        int closestSide = findSide(point, closestLocation);
        assert closestSide >= 0 : closestSide;
        if (MyVector3f.doCoincide(point, closestLocation, tolerance2)) {
            return true;
        }

        Vector3f corner1 = cornerLocations[closestSide];
        Vector3f pointOffset = point.subtract(corner1);
        int next = nextIndex(closestSide);
        Vector3f corner2 = cornerLocations[next];
        Vector3f sideOffset = corner2.subtract(corner1);
        Vector3f cross = sideOffset.cross(pointOffset);
        double crossDot = cross.dot(planeNormal);
        if (crossDot >= 0.0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Quickly provide a representative point for the polygon. The point need
     * not be contained in this polygon, but it should be relatively close to
     * all points that are.
     *
     * @return a new vector
     */
    @Override
    public Vector3f representative() {
        Vector3f result = centroid();
        return result;
    }

    /**
     * Score a location based on how well it "fits" into this polygon.
     *
     * @return score value (more positive &rarr; better)
     */
    @Override
    public double score(Vector3f location) {
        Validate.nonNull(location, "location");

        if (planeConstant == null) {
            setPlane();
        }

        float pseudoDistance = planeNormal.dot(location) + planeConstant;
        double squaredPD = pseudoDistance * pseudoDistance;

        if (squaredPD > tolerance2) {
            Vector3f rejection = planeNormal.mult(-pseudoDistance);
            Vector3f projection = location.subtract(rejection);
            assert inPlane(projection) : projection;
            double result = score(projection);
            result += squaredPD;
            return result;
        }

        Vector3f closestLocation = new Vector3f();
        int sideIndex = findSide(location, closestLocation);
        assert sideIndex >= 0 : sideIndex;
        double distanceSquared = MyVector3f.distanceSquared(
                location, closestLocation);

        if (contains(location)) {
            return distanceSquared;
        } else {
            return -distanceSquared;
        }
    }

    /**
     * Find a path between two locations in this polygon without leaving the
     * polygon. Short paths are preferred over long ones.
     *
     * @param startLocation coordinates (contained in polygon, unaffected)
     * @param goalLocation coordinates (contained in polygon, unaffected)
     * @return a new path spline, or null if none found
     */
    @Override
    public Spline3f shortestPath(Vector3f startingPoint, Vector3f goal) {
        assert contains(startingPoint) : startingPoint;
        assert contains(goal) : goal;

        assert isConvex(); // TODO
        Vector3f[] joints = {startingPoint, goal};
        Spline3f result = new LinearSpline3f(joints);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the #centroid field.
     */
    private void setCentroid() {
        float sumX = 0f;
        float sumZ = 0f;
        for (int cornerI = 0; cornerI < numCorners; cornerI++) {
            VectorXZ p1 = planarOffset(cornerI);
            int nextI = nextIndex(cornerI);
            VectorXZ p2 = planarOffset(nextI);
            float cross = p1.cross(p2);
            float termX = (p1.getX() + p2.getX()) * cross;
            sumX += termX;
            float termZ = (p1.getZ() + p2.getZ()) * cross;
            sumZ += termZ;
        }

        if (signedArea == null) {
            setSignedArea();
        }
        float x = sumX / (6f * signedArea);
        float z = sumZ / (6f * signedArea);
        centroid = new VectorXZ(x, z);
    }

    /**
     * Initialize the #isConvex field.
     *
     * The polygon is convex if all the turns are inward.
     */
    private void setIsConvex() {
        assert isConvex == null : isConvex;

        if (planeNormal == null) {
            setPlane();
        }
        for (int cornerI = 0; cornerI < numCorners; cornerI++) {
            Vector3f cross = crossProduct(cornerI);
            float dot = planeNormal.dot(cross);
            if (!(dot >= 0f)) {
                isConvex = false;
                return;
            }
        }
        isConvex = true;
    }

    /**
     * Initialize the element of the #planarOffsets field for a particular
     * corner.
     *
     * @param cornerIndex which corner (&ge;0, &lt;numCorners)
     */
    private void setPlanarOffset(int cornerIndex) {
        assert cornerIndex >= 0 : cornerIndex;
        assert cornerIndex < numCorners : cornerIndex;

        Vector3f base = cornerLocations[0];
        Vector3f location = cornerLocations[cornerIndex];
        Vector3f offset = location.subtract(base);

        if (planeXBasis == null) {
            setPlane();
        }

        float x = offset.dot(planeXBasis);
        float z = offset.dot(planeZBasis);
        planarOffsets[cornerIndex] = new VectorXZ(x, z);
    }

    /**
     * Initialize the #planeNormal, #planeConstant, #planeXBasis, and
     * #planeZBasis fields.
     *
     * The direction of the normal is chosen so that when traversing the sides
     * in order, turns toward the interior are right-handed and turns toward the
     * exterior are left-handed. The constant is simply the pseudo-distance of
     * the origin from the plane.
     *
     * #planeXBasis, #planeNormal, and #planeZBasis are orthogonal unit vectors,
     * constituting a basis for planar coordinates.
     */
    private void setPlane() {
        /*
         * Find the three corners which form the largest triangle.
         */
        int[] triangle = largestTriangle();
        assert triangle != null;
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
        planeNormal = crossProduct.normalize();
        planeConstant = -planeNormal.dot(a);
        /*
         * Select basis vectors for planar offsets.
         */
        planeXBasis = offsetB.normalize();
        planeZBasis = planeXBasis.cross(planeNormal);

        float ls = planeZBasis.lengthSquared();
        assert ls > 0.9999 : ls;
        assert ls < 1.0001 : ls;
        float dot1 = planeNormal.dot(planeXBasis);
        assert FastMath.abs(dot1) < 0.0001f : dot1;
        float dot2 = planeNormal.dot(planeZBasis);
        assert FastMath.abs(dot2) < 0.0001f : dot2;
        float dot3 = planeXBasis.dot(planeZBasis);
        assert FastMath.abs(dot3) < 0.0001f : dot3;
    }

    /**
     * Initialize the #signedArea field by applying the shoelace formula to the
     * planar offsets.
     */
    private void setSignedArea() {
        float total = 0f;
        for (int i = 0; i < numCorners; i++) {
            if (planarOffsets[i] == null) {
                setPlanarOffset(i);
            }
            int next = nextIndex(i);
            if (planarOffsets[next] == null) {
                setPlanarOffset(next);
            }
            float cross = planarOffsets[i].cross(planarOffsets[next]);
            total += cross;
        }
        signedArea = 0.5f * total;
    }
}

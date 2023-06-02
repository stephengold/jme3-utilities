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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.VectorXZ;
import jme3utilities.math.locus.Locus3f;
import jme3utilities.math.spline.LinearSpline3f;
import jme3utilities.math.spline.Spline3f;

/**
 * An immutable polygon in 3-dimensional space, consisting of N corners (points)
 * connected by N sides (straight-line segments) to form a loop. The polygon
 * must be simple, in other words: non-degenerate, planar, and
 * non-self-intersecting. This means it has a well-defined interior and
 * exterior. It may also be convex, though it need not be.
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
     * cached planar offset of the centroid of this polygon (initialized by
     * #setCentroid()).
     */
    private VectorXZ centroid = null;
    /**
     * cached normal vector of the plane containing the polygon (unit vector,
     * initialized by #setPlane())
     */
    private Vector3f planeNormal = null;
    /**
     * cached first basis vector for planar offsets (unit vector, initialized by
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
     * Instantiate a simple polygon from an array of corners.
     *
     * @param cornerArray locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     * @throws IllegalArgumentException if the corners provided don't form a
     * simple polygon
     */
    public SimplePolygon3f(Vector3f[] cornerArray, float compareTolerance) {
        super(cornerArray, compareTolerance);

        // Verify that the polygon is simple.
        if (!isPlanar()) {
            throw new IllegalArgumentException("non-planar polygon");
        }
        if (isSelfIntersecting()) {
            throw new IllegalArgumentException("self-intersecting polygon");
        }

        // Allocate array space.
        this.planarOffsets = new VectorXZ[numCorners];
        this.planarOffsets[0] = new VectorXZ(0f, 0f);
    }

    /**
     * Instantiate a simple polygon from a list of corners.
     *
     * @param cornerList locations of the corners, in sequence (not null or
     * containing any nulls, unaffected)
     * @param compareTolerance tolerance (&ge;0) used to compare locations for
     * coincidence
     * @throws IllegalArgumentException if the corners provided don't form a
     * simple polygon
     */
    public SimplePolygon3f(List<Vector3f> cornerList, float compareTolerance) {
        super(cornerList, compareTolerance);

        // Verify that the polygon is simple.
        if (!isPlanar()) {
            throw new IllegalArgumentException("non-planar polygon");
        }
        if (isSelfIntersecting()) {
            throw new IllegalArgumentException("self-intersecting polygon");
        }

        //Allocate array space.
        this.planarOffsets = new VectorXZ[numCorners];
        this.planarOffsets[0] = new VectorXZ(0f, 0f);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the unsigned area of this polygon.
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
     * Test whether the specified location lies in the plane of this polygon.
     *
     * @param point coordinates of the test location (not null, unaffected)
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

        assert result > 0.0 : result;
        assert result < 2.0 * Math.PI : result;
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
     * Calculate a normal vector of the plane containing this polygon.
     *
     * @return a new unit vector
     */
    public Vector3f planeNormal() {
        if (planeNormal == null) {
            setPlane();
        }
        Vector3f result = planeNormal.clone();

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

        // Calculate the magnitude of the turn.
        double turnMagnitude = absTurnAngle(cornerIndex);
        assert !Double.isNaN(turnMagnitude);
        assert turnMagnitude >= 0.0 : turnMagnitude;
        assert turnMagnitude < Math.PI : turnMagnitude;

        // Calculate the direction/sign of the turn.
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
     * Test whether this region can be merged with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return true if they can merge, otherwise false
     */
    @Override
    public boolean canMerge(Locus3f otherLocus) {
        Validate.nonNull(otherLocus, "other locus");

        if (!(otherLocus instanceof SimplePolygon3f)) {
            return false;
        }
        SimplePolygon3f otherPoly = (SimplePolygon3f) otherLocus;
        if (otherPoly.getTolerance() != tolerance) {
            return false;
        }
        List<Vector3f> locs = mergeCorners(otherPoly);
        if (locs == null) {
            return false;
        }
        Polygon3f tempPoly = new Polygon3f(locs, tolerance);
        if (tempPoly.isDegenerate() || !tempPoly.isPlanar()) {
            return false;
        }
        GenericPolygon3f genPoly = new GenericPolygon3f(locs, tolerance);
        if (genPoly.isSelfIntersecting()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculate the centroid of this region. The centroid need not be contained
     * in the region, but it should be relatively near all locations that are.
     *
     * @return a new coordinate vector
     */
    @Override
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
     * Test whether this region contains the specified location.
     *
     * @param location coordinates of test location (not null, unaffected)
     * @return true if location is in region, false otherwise
     */
    @Override
    public boolean contains(Vector3f location) {
        Validate.nonNull(location, "location");

        if (!inPlane(location)) {
            return false;
        }

        Vector3f closestLocation = new Vector3f();
        int closestSide = findSide(location, closestLocation);
        assert closestSide >= 0 : closestSide;
        if (MyVector3f.doCoincide(location, closestLocation, tolerance2)) {
            return true;
        }

        Vector3f corner1 = cornerLocations[closestSide];
        Vector3f pointOffset = location.subtract(corner1);
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
     * Test whether this region contains the specified segment. Note: recursive!
     *
     * @param startLocation coordinates of start of test segment (not null,
     * unaffected)
     * @param endLocation coordinates of end of test segment (not null,
     * unaffected)
     * @return true if test segment is entirely contained in region, false
     * otherwise
     */
    @Override
    public boolean contains(Vector3f startLocation, Vector3f endLocation) {
        Validate.nonNull(startLocation, "start location");
        Validate.nonNull(endLocation, "end location");

        if (!contains(startLocation)) {
            return false;
        } else if (!contains(endLocation)) {
            return false;
        } else if (isConvex()) {
            return true;
        }

        for (int sideIndex = 0; sideIndex < numCorners; ++sideIndex) {
            boolean startOnSide = onSide(startLocation, sideIndex);
            boolean endOnSide = onSide(endLocation, sideIndex);
            if (startOnSide && endOnSide) {
                // Both endpoints lie on a single side, so entirely contained.
                return true;
            }
        }
        /*
         * Find where the test segment intersects the perimeter, preferably
         * at a corner.
         */
        Vector3f joint = intersectionWithPerimeter(startLocation, endLocation);
        if (joint == null) {
            // No intersection with perimeter, so entirely contained.
            return true;
        }
        /*
         * Split the test segment and test each sub-segment separately.
         *
         * If the intersection is one of the endpoints,
         * split at the midpoint instead.
         */
        if (MyVector3f.doCoincide(startLocation, joint, tolerance2)
                || MyVector3f.doCoincide(endLocation, joint, tolerance2)) {
            joint = MyVector3f.midpoint(startLocation, endLocation, null);
        }
        if (!contains(startLocation, joint)) {
            return false;
        } else if (contains(joint, endLocation)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the location in this region nearest to the specified location.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return a new coordinate vector, or null if none found
     */
    @Override
    public Vector3f findLocation(Vector3f location) {
        Vector3f result = location.clone();
        if (contains(result)) {
            return result;
        }

        if (!inPlane(result)) {
            float pseudoDistance = planeNormal.dot(location) + planeConstant;
            Vector3f rejection = planeNormal.mult(pseudoDistance);
            result.subtractLocal(rejection);
            if (!inPlane(result)) {
                assert false;
            }
            assert inPlane(result) : result;
            if (contains(result)) {
                return result;
            }
        }

        Vector3f closestLocation = new Vector3f();
        int sideIndex = findSide(result, closestLocation);
        assert sideIndex >= 0 : sideIndex;

        assert contains(closestLocation) : closestLocation;
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
        Vector3f center = centroid();

        if (contains(center)) {
            return center;
        }
        assert inPlane(center);

        Vector3f result = new Vector3f();
        int sideIndex = findSide(center, result);
        assert sideIndex >= 0 : sideIndex;

        assert contains(result) : result;
        return result;
    }

    /**
     * Merge this region with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return a new region representing the union of the 2 regions
     */
    @Override
    public Locus3f merge(Locus3f otherLocus) {
        Validate.nonNull(otherLocus, "other locus");

        SimplePolygon3f otherPoly = (SimplePolygon3f) otherLocus;
        List<Vector3f> locs = mergeCorners(otherPoly);
        SimplePolygon3f result = new SimplePolygon3f(locs, tolerance);

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
     * Find a path between 2 locations in this region without leaving the
     * region. Short paths are preferred over long ones.
     *
     * @param startLocation coordinates (contained in region, unaffected)
     * @param goalLocation coordinates (contained in region, unaffected)
     * @param maxPoints maximum number of control points to use (&ge;2)
     * @return a new path spline, or null if none found
     */
    @Override
    public Spline3f shortestPath(Vector3f startLocation, Vector3f goalLocation,
            int maxPoints) {
        Validate.nonNull(startLocation, "start location");
        Validate.nonNull(goalLocation, "goal location");
        Validate.inRange(maxPoints, "max control points", 2, Integer.MAX_VALUE);
        assert contains(startLocation) : startLocation;
        assert contains(goalLocation) : goalLocation;

        List<Vector3f> joints = new ArrayList<>(maxPoints);
        joints.add(startLocation);
        joints.add(goalLocation);
        Spline3f result = new LinearSpline3f(joints);

        if (!result.isContainedIn(this) && maxPoints > joints.size()) {
            float bestLength = Float.MAX_VALUE;
            result = null;
            for (int i = 0; i < numCorners; ++i) {
                Vector3f corner = cornerLocations[i];
                List<Vector3f> newJoints = new ArrayList<>(joints);
                newJoints.add(1, corner);
                Spline3f newResult = new LinearSpline3f(newJoints);
                if (newResult.isContainedIn(this)) {
                    float newLength = newResult.totalLength();
                    if (newLength < bestLength) {
                        bestLength = newLength;
                        result = newResult;
                    }
                }
            }
        }
        // TODO further searches if unsuccessful

        return result;
    }

    /**
     * Calculate the distance from the specified starting point to the first
     * point of support (if any) directly below it in this region.
     *
     * @param location coordinates of starting point (not null, unaffected)
     * @param cosineTolerance cosine of maximum slope for support (&gt;0, &lt;1)
     * @return the minimum distance (&ge;0) or {@link Float#POSITIVE_INFINITY}
     * if no support
     */
    @Override
    public float supportDistance(Vector3f location, float cosineTolerance) {
        Validate.nonNull(location, "location");
        Validate.fraction(cosineTolerance, "cosine tolerance");

        if (planeNormal == null) {
            setPlane();
        }
        float cosineSlope = FastMath.abs(planeNormal.y);
        if (cosineSlope < cosineTolerance) {
            /*
             * Slope of this polygon's plane exceeds the limit, so it
             * won't provide support.
             */
            return Float.POSITIVE_INFINITY;
        }

        float dot = planeNormal.dot(location);
        float distance = (dot + planeConstant) / planeNormal.y;
        if (distance < 0f) {
            // Starting point is below the plane of this polygon.
            return Float.POSITIVE_INFINITY;
        }

        Vector3f projection = location.clone();
        projection.y -= distance;
        assert inPlane(projection) : projection;
        if (!contains(projection)) {
            // Calculated point of support lies outside this polygon.
            return Float.POSITIVE_INFINITY;
        } else {
            return distance;
        }
    }
    // *************************************************************************
    // private methods

    /**
     * List the corners that would result from merging this polygon with
     * another.
     *
     * @param other the other polygon (not null)
     * @return a new list with all elements non-null, or null if the polygons
     * cannot be merged
     */
    private List<Vector3f> mergeCorners(SimplePolygon3f other) {
        Vector3f otherNorm = other.planeNormal();
        if (planeNormal == null) {
            setPlane();
        }
        float dot = otherNorm.dot(planeNormal);
        if (dot == 0f) { // The planes of the 2 polygons are orthogonal.
            return null;
        }
        int otherNumCorners = other.numCorners();
        boolean[][] sideMap = new boolean[numCorners][otherNumCorners];
        if (!sharesSideWith(other, sideMap)) {
            // The 2 polygons have no shared sides.
            return null;
        }
        boolean[][] revMap = new boolean[otherNumCorners][numCorners];
        boolean success = other.sharesSideWith(this, revMap);
        assert success;

        // Create a list of corner locations for the result.
        int maxCorners = numCorners + otherNumCorners - 2;
        List<Vector3f> result = new ArrayList<>(maxCorners);

        // Start with an unshared side of this polygon.
        int startI = 0;
        while (MyArray.first(sideMap[startI]) >= 0) {
            ++startI;
            assert startI < numCorners : startI;
        }

        // Add its first corner to the result.
        Vector3f location = cornerLocations[startI];
        result.add(location);

        for (int cornerI = nextIndex(startI);
                cornerI != startI;
                cornerI = nextIndex(cornerI)) {
            location = cornerLocations[cornerI];
            result.add(location);

            int startJ = MyArray.first(sideMap[cornerI]);
            if (startJ >= 0) {
                int cornerJ;
                if (dot > 0f) {
                    /*
                     * The 2 polygons wind in the same direction. Iterate in
                     * the same (forward) direction through the other polygon's
                     * corners until a shared side is found.
                     */
                    startJ = other.nextIndex(startJ);
                    for (cornerJ = other.nextIndex(startJ);
                            MyArray.first(revMap[cornerJ]) < 0;
                            cornerJ = other.nextIndex(cornerJ)) {
                        location = other.copyCornerLocation(cornerJ);
                        result.add(location);
                    }
                    cornerI = MyArray.first(revMap[cornerJ]);

                } else {
                    /*
                     * The 2 polygons wind in opposite directions.  Iterate in
                     * the opposite (reverse) direction through the other
                     * polygon's corners until a shared side is found.
                     */
                    for (cornerJ = other.prevIndex(startJ);
                            MyArray.first(revMap[other.prevIndex(cornerJ)]) < 0;
                            cornerJ = other.prevIndex(cornerJ)) {
                        location = other.copyCornerLocation(cornerJ);
                        result.add(location);
                    }
                    cornerI = MyArray.first(revMap[other.prevIndex(cornerJ)]);
                }
            }
        }

        assert result.size() <= maxCorners : result.size();
        return result;
    }

    /**
     * Initialize the #centroid field.
     */
    private void setCentroid() {
        float sumX = 0f;
        float sumZ = 0f;
        for (int cornerI = 0; cornerI < numCorners; ++cornerI) {
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
        this.centroid = new VectorXZ(x, z);
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
        for (int cornerI = 0; cornerI < numCorners; ++cornerI) {
            Vector3f cross = crossProduct(cornerI);
            float dot = planeNormal.dot(cross);
            if (!(dot >= 0f)) {
                this.isConvex = false;
                return;
            }
        }
        this.isConvex = true;
    }

    /**
     * Initialize the element of the #planarOffsets field for the specified
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
        this.planarOffsets[cornerIndex] = new VectorXZ(x, z);
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
        // Find the 3 corners which form the largest triangle.
        int[] triangle = largestTriangle();
        assert triangle != null;
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
        this.planeNormal = crossProduct.normalize();
        this.planeConstant = -planeNormal.dot(a);

        // Select basis vectors for planar offsets.
        this.planeXBasis = offsetB.normalize();
        this.planeZBasis = planeXBasis.cross(planeNormal);

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
        for (int i = 0; i < numCorners; ++i) {
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
        this.signedArea = 0.5f * total;
    }
}

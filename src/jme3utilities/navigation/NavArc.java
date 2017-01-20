/*
 Copyright (c) 2014-2017, Stephen Gold
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
package jme3utilities.navigation;

import com.jme3.math.Vector3f;
import java.util.Objects;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.LinearSpline3f;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.Spline3f;
import jme3utilities.math.VectorXZ;

/**
 * Immutable arc of a navigation graph: represents a feasible path from one
 * vertex to another vertex. Arcs are unidirectional and need not be straight.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NavArc
        implements Comparable<NavArc> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(NavArc.class.getName());
    // *************************************************************************
    // fields
    /**
     * vertex from which this arc originates (not null)
     */
    final private NavVertex fromVertex;
    /**
     * vertex at which this arc terminates (not null)
     */
    final private NavVertex toVertex;
    /**
     * path between the two vertices
     */
    final private Spline3f path;
    /**
     * direction at the start of this arc (in world space, length=1)
     */
    final private Vector3f startDirection;
    /**
     * direction at the start of this arc (in world space, length=1)
     */
    final private VectorXZ horizontalDirection;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a straight arc from one vertex to another.
     *
     * @param fromVertex starting point (not null, distinct from toVertex)
     * @param toVertex endpoint (not null)
     */
    NavArc(NavVertex fromVertex, NavVertex toVertex) {
        assert fromVertex != null;
        assert toVertex != null;
        assert fromVertex != toVertex : toVertex;

        this.fromVertex = fromVertex;
        this.toVertex = toVertex;

        Vector3f[] points = new Vector3f[2];
        points[0] = fromVertex.getLocation();
        points[1] = toVertex.getLocation();
        path = new LinearSpline3f(points);

        startDirection = path.rightDerivative(0f);
        assert startDirection.isUnitVector() : startDirection;

        horizontalDirection = MyVector3f.horizontalDirection(startDirection);
    }

    /**
     * Instantiate a piecewise-linear arc from one vertex to another.
     *
     * @param fromVertex starting point (not null, distinct from toVertex)
     * @param toVertex endpoint (not null)
     * @param joints intermediate locations along the path (not null, world
     * coordinates, unaffected)
     */
    NavArc(NavVertex fromVertex, NavVertex toVertex, Vector3f[] joints) {
        assert fromVertex != null;
        assert toVertex != null;
        assert fromVertex != toVertex : toVertex;
        assert joints != null;

        this.fromVertex = fromVertex;
        this.toVertex = toVertex;

        int numJoints = joints.length;
        Vector3f[] points = new Vector3f[numJoints + 2];
        points[0] = fromVertex.getLocation();
        for (int jointIndex = 0; jointIndex < numJoints; jointIndex++) {
            points[jointIndex + 1] = joints[jointIndex].clone();
        }
        points[numJoints + 1] = toVertex.getLocation();
        path = new LinearSpline3f(points);

        startDirection = path.rightDerivative(0f);
        assert startDirection.isUnitVector() : startDirection;

        horizontalDirection = MyVector3f.horizontalDirection(startDirection);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the starting vertex of this arc.
     *
     * @return pre-existing instance
     */
    public NavVertex getFromVertex() {
        return fromVertex;
    }

    /**
     * Read the initial direction of this arc in the X-Z plane.
     *
     * @return pre-existing unit vector
     */
    public VectorXZ getHorizontalDirection() {
        return horizontalDirection;
    }

    /**
     * Read the total path length (cost) of this arc.
     *
     * @return length (&gt;0, world units)
     */
    public float getPathLength() {
        float result = path.totalLength();
        return result;
    }

    /**
     * Read the initial direction of this arc.
     *
     * @return new unit vector
     */
    public Vector3f getStartDirection() {
        return startDirection.clone();
    }

    /**
     * Access the endpoint of this arc.
     *
     * @return pre-existing instance
     */
    public NavVertex getToVertex() {
        return toVertex;
    }

    /**
     * Compute the location at the specified distance along this arc's path.
     *
     * @param distance (&ge;0)
     * @return new vector in world coordinates
     */
    public Vector3f pathLocation(float distance) {
        Validate.nonNegative(distance, "distance");

        Vector3f result = path.interpolate(distance);
        return result;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another arc.
     *
     * @param otherArc (not null, unaffected)
     * @return 0 if the arcs are equivalent
     */
    @Override
    public int compareTo(NavArc otherArc) {
        int result = fromVertex.compareTo(otherArc.getFromVertex());
        if (result != 0) {
            return result;
        }
        result = toVertex.compareTo(otherArc.getToVertex());
        if (result != 0) {
            return result;
        }
        result = MyVector3f.compare(startDirection,
                otherArc.getStartDirection());
        /*
         * Verify consistency with equals().
         */
        if (result == 0) {
            assert this.equals(otherArc);
        }
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Compare for equality.
     *
     * @param otherObject (unaffected)
     * @return true if the arcs are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject instanceof NavArc) {
            NavArc otherArc = (NavArc) otherObject;
            if (!fromVertex.equals(otherArc.getFromVertex())) {
                return false;
            } else if (!toVertex.equals(otherArc.getToVertex())) {
                return false;
            } else {
                boolean result =
                        startDirection.equals(otherArc.getStartDirection());
                return result;
            }
        }
        return false;
    }

    /**
     * Generate the hash code for this arc.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.fromVertex);
        hash = 17 * hash + Objects.hashCode(this.toVertex);
        hash = 17 * hash + Objects.hashCode(this.startDirection);

        return hash;
    }

    /**
     * Represent this arc as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String fromString = fromVertex.toString();
        String toString = toVertex.toString();
        String dirString = startDirection.toString();
        String result = String.format("%s to %s dir=%s", fromString, toString,
                dirString);

        return result;
    }
}

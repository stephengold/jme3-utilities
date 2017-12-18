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
package jme3utilities.navigation;

import com.jme3.math.Vector3f;
import java.util.Objects;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.VectorXZ;

/**
 * An immutable connecting arc in a navigation graph: represents feasible
 * path(s) originating from a particular vertex and terminating at one of its
 * neighbors. Arcs are unidirectional and need not be straight. Properties such
 * as cost and implementation must be stored externally.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NavArc implements Comparable<NavArc> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NavArc.class.getName());
    // *************************************************************************
    // fields

    /**
     * vertex from which this arc originates (not null, initialized by
     * constructor)
     */
    final private NavVertex fromVertex;
    /**
     * vertex at which this arc terminates (not null, initialized by
     * constructor)
     */
    final private NavVertex toVertex;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an arc from one vertex to another.
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
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the reverse of this arc, if it exists.
     *
     * @return a pre-existing instance or null if none found
     */
    public NavArc findReverse() {
        NavArc result = toVertex.findOutgoing(fromVertex);
        return result;
    }

    /**
     * Access the starting vertex of this arc.
     *
     * @return the pre-existing instance
     */
    public NavVertex getFromVertex() {
        return fromVertex;
    }

    /**
     * Access the endpoint of this arc.
     *
     * @return the pre-existing instance
     */
    public NavVertex getToVertex() {
        return toVertex;
    }

    /**
     * Calculate the horizontal offset vector of this arc.
     *
     * @return a new offset vector
     */
    public VectorXZ horizontalOffset() {
        Vector3f offset = offset();
        VectorXZ result = new VectorXZ(offset);

        return result;
    }

    /**
     * Compare for reversal.
     *
     * @param otherArc (not null)
     * @return true if the two arcs are opposites, otherwise false
     */
    public boolean isReverse(NavArc otherArc) {
        if (!fromVertex.equals(otherArc.getToVertex())) {
            return false;
        } else if (!toVertex.equals(otherArc.getFromVertex())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculate the midpoint of this arc.
     *
     * @return a new coordinate vector
     */
    public Vector3f midpoint() {
        Vector3f p1 = fromVertex.copyLocation();
        Vector3f p2 = toVertex.copyLocation();
        Vector3f result = MyVector3f.midpoint(p1, p2);

        return result;
    }

    /**
     * Calculate the 3-D offset vector of this arc.
     *
     * @return a new offset vector
     */
    public Vector3f offset() {
        Vector3f p1 = fromVertex.copyLocation();
        Vector3f p2 = toVertex.copyLocation();
        Vector3f result = p2.subtract(p1);

        return result;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another arc.
     *
     * @param otherArc (not null, unaffected)
     * @return 0 if the arcs are equivalent, negative if the other arc comes
     * before this arc, positive if this arc comes before the other arc
     */
    @Override
    public int compareTo(NavArc otherArc) {
        int result = fromVertex.compareTo(otherArc.getFromVertex());
        if (result != 0) {
            return result;
        }
        result = toVertex.compareTo(otherArc.getToVertex());
        /*
         * Verify consistency with equals().
         */
        assert result != 0 || this.equals(otherArc);
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for equivalence.
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
            } else {
                boolean result = toVertex.equals(otherArc.getToVertex());
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

        return hash;
    }

    /**
     * Represent this arc as a string of text.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String fromString = fromVertex.toString();
        String toString = toVertex.toString();
        String result = String.format("%s->%s", fromString, toString);

        return result;
    }
}

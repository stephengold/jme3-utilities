/*
 Copyright (c) 2014-2023, Stephen Gold
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;
import jme3utilities.math.locus.Locus3f;

/**
 * Navigation vertex: represents a reachable region of 3-D space, along with its
 * connections to other such regions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NavVertex implements Comparable<NavVertex> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            NavVertex.class.getName());
    // *************************************************************************
    // fields

    /**
     * region represented by this vertex or null (in world coordinates,
     * initialized by constructor)
     */
    private Locus3f locus;
    /**
     * set of arcs which terminate at this vertex
     */
    final private Set<NavArc> incoming = new HashSet<>(4);
    /**
     * set of arcs which originate from this vertex
     */
    final private Set<NavArc> outgoing = new HashSet<>(4);
    /**
     * name of this vertex (not null, initialized by constructor)
     */
    final private String name;
    /**
     * location for calculating arc offsets (not null, initialized by
     * constructor)
     */
    final private Vector3f location;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a vertex without any arcs.
     *
     * @param name name for the new vertex (not null)
     * @param locus region represented or null
     * @param location for calculating arc offsets (not null, unaffected)
     */
    NavVertex(String name, Locus3f locus, Vector3f location) {
        assert name != null;
        assert location != null;

        this.name = name;
        this.locus = locus;
        this.location = location;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new incoming arc to this vertex.
     *
     * @param arc (not null, terminating at this vertex, not present)
     */
    void addIncoming(NavArc arc) {
        assert arc != null;
        assert arc.getToVertex() == this : arc;
        assert arc.getFromVertex() != this : arc;

        boolean success = incoming.add(arc);
        assert success : arc;
    }

    /**
     * Add a new outgoing arc for this vertex.
     *
     * @param arc (not null, originating at this vertex, not present)
     */
    void addOutgoing(NavArc arc) {
        assert arc != null;
        assert arc.getFromVertex() == this : arc;
        assert arc.getToVertex() != this : arc;

        boolean success = outgoing.add(arc);
        assert success : arc;
    }

    /**
     * Copy all incoming arcs.
     *
     * @return a new array of pre-existing instances
     */
    public NavArc[] copyIncoming() {
        NavArc[] result = new NavArc[incoming.size()];
        int i = 0;
        for (NavArc arc : incoming) {
            result[i] = arc;
            ++i;
        }

        return result;
    }

    /**
     * Copy the location used to calculate arc offsets, which need not be in the
     * locus of this vertex.
     *
     * @return a new coordinate vector
     */
    public Vector3f copyLocation() {
        Vector3f result = location.clone();
        return result;
    }

    /**
     * Copy all outgoing arcs.
     *
     * @return a new array of pre-existing instances
     */
    public NavArc[] copyOutgoing() {
        NavArc[] result = new NavArc[outgoing.size()];
        int i = 0;
        for (NavArc arc : outgoing) {
            result[i] = arc;
            ++i;
        }

        return result;
    }

    /**
     * Find the arc (if any) from a specified origin.
     *
     * @param origin (not null, not this)
     * @return a pre-existing instance, or null if not found
     */
    public NavArc findIncoming(NavVertex origin) {
        Validate.nonNull(origin, "origin");
        if (origin == this) {
            throw new IllegalArgumentException("origin not distinct");
        }

        for (NavArc arc : incoming) {
            if (arc.getFromVertex() == origin) {
                return arc;
            }
        }
        return null;
    }

    /**
     * Find the outgoing arc (if any) to a specified terminus.
     *
     * @param terminus (not null, not this)
     * @return a pre-existing instance, or null if none found
     */
    public NavArc findOutgoing(NavVertex terminus) {
        Validate.nonNull(terminus, "terminus");
        if (terminus == this) {
            throw new IllegalArgumentException("terminus not distinct");
        }

        for (NavArc arc : outgoing) {
            if (arc.getToVertex() == terminus) {
                return arc;
            }
        }
        return null;
    }

    /**
     * Find the outgoing arc (if any) with direction closest to a specified
     * direction with a specified tolerance.
     *
     * @param direction (not zero, unaffected)
     * @param cosineTolerance maximum cosine of angle between directions (&le;1,
     * &ge;-1)
     * @return a pre-existing instance, or null if none found
     */
    public NavArc findOutgoing(Vector3f direction, double cosineTolerance) {
        Validate.nonZero(direction, "direction");
        Validate.inRange(cosineTolerance, "cosine tolerance", -1.0, 1.0);

        double ls = MyVector3f.lengthSquared(direction);

        NavArc result = null;
        double bestCosine = cosineTolerance;
        for (NavArc arc : outgoing) {
            Vector3f offset = arc.offset();
            double dot = MyVector3f.dot(direction, offset);
            double arcLS = MyVector3f.lengthSquared(offset);
            assert arcLS > 0.0 : arcLS;
            double cosine = dot / Math.sqrt(ls * arcLS);
            if (cosine >= bestCosine) {
                bestCosine = cosine;
                result = arc;
            }
        }

        return result;
    }

    /**
     * Find the outgoing arc (if any) with horizontal direction closest to a
     * specified direction with a specified tolerance.
     *
     * @param horizontalDirection (not zero, unaffected)
     * @param cosineTolerance maximum cosine of angle between directions (&le;1,
     * &ge;-1)
     * @return a pre-existing instance, or null if none found
     */
    public NavArc findOutgoing(ReadXZ horizontalDirection,
            double cosineTolerance) {
        VectorXZ.validateNonZero(horizontalDirection, "horizontal direction");
        Validate.inRange(cosineTolerance, "cosine tolerance", -1.0, 1.0);

        double ls = horizontalDirection.lengthSquared();

        NavArc result = null;
        double bestCosine = cosineTolerance;
        for (NavArc arc : outgoing) {
            Vector3f offset = arc.offset();
            VectorXZ offsetXZ = new VectorXZ(offset);
            double dot = horizontalDirection.dot(offsetXZ);
            double arcLS = offset.lengthSquared();
            assert arcLS > 0.0 : arcLS;
            double cosine = dot / Math.sqrt(ls * arcLS);
            if (cosine >= bestCosine) {
                bestCosine = cosine;
                result = arc;
            }
        }

        return result;
    }

    /**
     * Access the region currently represented by this vertex.
     *
     * @return the pre-existing locus, or null if none
     */
    public Locus3f getLocus() {
        return locus;
    }

    /**
     * Read the name of this vertex.
     *
     * @return textual name (not null)
     */
    public String getName() {
        assert name != null;
        return name;
    }

    /**
     * List the incoming arcs.
     *
     * @return a new list of pre-existing instances
     */
    public List<NavArc> listIncoming() {
        List<NavArc> result = new ArrayList<>(incoming);
        return result;
    }

    /**
     * List the outgoing arcs.
     *
     * @return a new list of pre-existing instances
     */
    public List<NavArc> listOutgoing() {
        List<NavArc> result = new ArrayList<>(outgoing);
        return result;
    }

    /**
     * Count how many arcs terminate at this vertex.
     *
     * @return number of arcs (&ge;0)
     */
    public int numIncoming() {
        int result = incoming.size();
        return result;
    }

    /**
     * Count how many arcs originate from this vertex.
     *
     * @return number of arcs (&ge;0)
     */
    public int numOutgoing() {
        int result = outgoing.size();
        return result;
    }

    /**
     * Remove an incoming arc.
     *
     * @param arc (not null, terminating at this vertex)
     */
    void removeIncoming(NavArc arc) {
        assert arc != null;
        assert arc.getToVertex() == this : arc;

        boolean success = incoming.remove(arc);
        assert success : arc;
    }

    /**
     * Remove an outgoing arc.
     *
     * @param arc (not null, originating at this vertex)
     */
    void removeOutgoing(NavArc arc) {
        assert arc != null;
        assert arc.getFromVertex() == this : arc;

        boolean success = outgoing.remove(arc);
        assert success : arc;
    }

    /**
     * Alter the region represented by this vertex.
     *
     * @param newLocus region or null
     */
    public void setLocus(Locus3f newLocus) {
        this.locus = newLocus;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another vertex based on name.
     *
     * @param otherVertex (not null, unaffected)
     * @return 0 if the vertices have the same name
     */
    @Override
    public int compareTo(NavVertex otherVertex) {
        String otherDescription = otherVertex.getName();
        int result = name.compareTo(otherDescription);

        // Verify consistency with equals().
        if (result == 0) {
            assert this.equals(otherVertex);
        }
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Compare for equality based on name.
     *
     * @param otherObject (unaffected)
     * @return true if the vertices have the same name, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result = false;

        if (this == otherObject) {
            result = true;

        } else if (otherObject instanceof NavVertex) {
            NavVertex otherVertex = (NavVertex) otherObject;
            String otherDescription = otherVertex.getName();
            result = name.equals(otherDescription);
        }

        return result;
    }

    /**
     * Generate the hash code for this vertex.
     *
     * @return value for use in hashing
     */
    @Override
    public int hashCode() {
        int hash = Objects.hashCode(name);
        return hash;
    }

    /**
     * Represent this vertex as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        assert name != null;
        return name;
    }
}

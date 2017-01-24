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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.VectorXZ;

/**
 * Navigation vertex: represents a reachable location in the world.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NavVertex
        implements Comparable<NavVertex>, Savable {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(NavVertex.class.getName());
    // *************************************************************************
    // fields
    /**
     * list of arcs which originate from this vertex
     */
    final private ArrayList<NavArc> arcs = new ArrayList<>(4);
    /**
     * textual description of this vertex (not null)
     */
    final private String description;
    /**
     * a representative location in this vertex (not null, in world coordinates)
     */
    final private Vector3f location;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a vertex without any arcs, at the specified location.
     *
     * @param description textual description (not null)
     * @param location a representative location (in world coordinates, not
     * null, unaffected)
     */
    NavVertex(String description, Vector3f location) {
        assert description != null;

        this.location = location.clone();
        this.description = description;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create a straight arc and add it to this vertex. Arcs should ideally be
     * added in left-to-right order (clockwise as seen from above).
     *
     * @param endpoint destination vertex (not null, not this)
     * @return new instance
     */
    public NavArc addArc(NavVertex endpoint) {
        Validate.nonNull(endpoint, "endpoint");
        if (endpoint == this) {
            throw new IllegalArgumentException("endpoint should be distinct");
        }

        NavArc newArc = new NavArc(this, endpoint);
        arcs.add(newArc);

        return newArc;
    }

    /**
     * Create a piecewise-linear arc and add it to this vertex.
     *
     * @param endpoint destination vertex (not null, not this)
     * @param joints intermediate locations along the path (not null,
     * unaffected)
     * @return new instance
     */
    public NavArc addArc(NavVertex endpoint, Vector3f[] joints) {
        Validate.nonNull(endpoint, "endpoint");
        if (endpoint == this) {
            throw new IllegalArgumentException("endpoint should be distinct");
        }
        Validate.nonNull(joints, "joints");

        NavArc newArc = new NavArc(this, endpoint, joints);
        arcs.add(newArc);

        return newArc;
    }

    /**
     * Find the arc with a specified endpoint.
     *
     * @param endpoint (not null, not this)
     * @return pre-existing instance
     */
    public NavArc findArcTo(NavVertex endpoint) {
        Validate.nonNull(endpoint, "endpoint");
        if (endpoint == this) {
            throw new IllegalArgumentException("endpoint should be distinct");
        }

        for (NavArc arc : arcs) {
            if (arc.getToVertex() == endpoint) {
                return arc;
            }
        }
        return null;
    }

    /**
     * Find the arc whose starting direction is most similar to the specified
     * direction.
     *
     * @param direction not null, positive length, unaffected
     * @return pre-existing instance
     */
    public NavArc findLeastTurn(Vector3f direction) {
        Validate.nonZero(direction, "direction");

        NavArc result = null;
        float maxDot = -2f * direction.length();
        for (NavArc arc : arcs) {
            Vector3f arcDirection = arc.getStartDirection();
            float dot = arcDirection.dot(direction);
            if (dot > maxDot) {
                result = arc;
                maxDot = dot;
            }
        }

        return result;
    }

    /**
     * Find the arc whose starting direction is most similar to the specified
     * direction.
     *
     * @param direction (length&gt;0, unaffected)
     * @return pre-existing instance
     */
    public NavArc findLeastTurn(VectorXZ direction) {
        Validate.nonNull(direction, "direction");
        VectorXZ.validateNonZero(direction, "direction");

        NavArc result = null;
        float maxDot = -2f * direction.length();
        for (NavArc arc : arcs) {
            VectorXZ horizontalDirection = arc.getHorizontalDirection();
            float dot = horizontalDirection.dot(direction);
            if (dot > maxDot) {
                result = arc;
                maxDot = dot;
            }
        }

        return result;
    }

    /**
     * Find the arc at a specified list-offset relative to the specified base
     * arc.
     *
     * @param baseArc base arc (not null, from this vertex)
     * @param listOffset number of arcs past the base arc
     * @return pre-existing instance
     */
    public NavArc findNextArc(NavArc baseArc, int listOffset) {
        Validate.nonNull(baseArc, "base arc");

        int baseIndex = arcs.indexOf(baseArc);
        assert baseIndex != -1 : baseArc;
        int limit = arcs.size();
        int index = MyMath.modulo(baseIndex + listOffset, limit);
        NavArc result = arcs.get(index);

        return result;
    }

    /**
     * Copy the list of arcs which originate from this vertex.
     *
     * @return new array of pre-existing instances
     */
    public NavArc[] getArcs() {
        int size = arcs.size();
        NavArc[] result = new NavArc[size];
        arcs.toArray(result);

        return result;
    }

    /**
     * Read the description of this vertex.
     *
     * @return textual description (not null)
     */
    public String getDescription() {
        assert description != null;
        return description;
    }

    /**
     * Copy the world coordinates of the vertex. TODO: rename
     *
     * @return new vector
     */
    public Vector3f getLocation() {
        return location.clone();
    }

    /**
     * Count how many arcs originate from this vertex. TODO: rename
     *
     * @return number of arcs (&ge;0)
     */
    public int getNumArcs() {
        int result = arcs.size();
        return result;
    }

    /**
     * Test whether this vertex has an arc to the specified endpoint.
     *
     * @param endpoint (not null, not this)
     * @return true if found, false if none found
     */
    public boolean hasArcTo(NavVertex endpoint) {
        Validate.nonNull(endpoint, "endpoint");
        assert endpoint != this : endpoint;

        for (NavArc arc : arcs) {
            if (arc.getToVertex() == endpoint) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove the arc with the specified endpoint, if any.
     *
     * @param endpoint (not null, not this)
     * @return true if successful, false if none found
     */
    boolean removeArcTo(NavVertex endpoint) {
        assert endpoint != null;
        assert endpoint != this : endpoint;

        for (NavArc arc : arcs) {
            if (arc.getToVertex() == endpoint) {
                arcs.remove(arc);
                return true;
            }
        }
        return false;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another vertex based on description.
     *
     * @param otherVertex (not null, unaffected)
     * @return 0 if the vertices have the same description
     */
    @Override
    public int compareTo(NavVertex otherVertex) {
        String otherDescription = otherVertex.getDescription();
        int result = description.compareTo(otherDescription);
        /*
         * Verify consistency with equals().
         */
        if (result == 0) {
            assert this.equals(otherVertex);
        }
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Compare for equality based on description.
     *
     * @param otherObject (unaffected)
     * @return true if the vertices have the same description, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result = false;

        if (this == otherObject) {
            result = true;

        } else if (otherObject instanceof NavVertex) {
            NavVertex otherVertex = (NavVertex) otherObject;
            String otherDescription = otherVertex.getDescription();
            result = description.equals(otherDescription);
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
        int hash = Objects.hashCode(description);
        return hash;
    }

    /**
     * Represent this vertex as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        assert description != null;
        return description;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this vertex, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Serialize this vertex, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

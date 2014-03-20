/*
 Copyright (c) 2014, Stephen Gold
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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Navigation node: represents a reachable location in the world.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class NavNode {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(NavNode.class.getName());
    // *************************************************************************
    // fields
    /**
     * list of arcs which originate from this node
     */
    private ArrayList<NavArc> arcs = new ArrayList<>();
    /**
     * temporary flag to track whether this node has been visited during the
     * current pass
     */
    private boolean visited = false;
    /**
     * temporary floating-point value for this node
     */
    private float floatValue = 0f;
    /**
     * textual description of this node (not null)
     */
    private String description;
    /**
     * world coordinates of this node (not null)
     */
    private Vector3f location;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a node without any arcs, at the specified location.
     *
     * @param description textual description (not null)
     * @param location world coordinates (not null)
     */
    NavNode(String description, Vector3f location) {
        assert description != null;

        this.location = location.clone();
        this.description = description;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create an arc and add it to this node. Arcs should ideally be added in
     * left-to-right order (clockwise as seen from above).
     *
     * @param endpoint destination node (not null, not this)
     * @param pathLength length or cost (arbitrary units, &gt;0)
     * @param startDirection direction at the start (unit vector in world space,
     * unaffected)
     * @return the new instance
     */
    public NavArc addArc(NavNode endpoint, float pathLength,
            Vector3f startDirection) {
        Validate.nonNull(endpoint, "endpoint");
        if (endpoint == this) {
            throw new IllegalArgumentException("endpoint should be distinct");
        }
        Validate.positive(pathLength, "length");
        Validate.nonNull(startDirection, "start direction");
        if (!startDirection.isUnitVector()) {
            logger.log(Level.SEVERE, "direction={0}", startDirection);
            throw new IllegalArgumentException(
                    "start direction should be unit vector");
        }

        NavArc newArc = new NavArc(this, endpoint, pathLength, startDirection);
        arcs.add(newArc);
        return newArc;
    }

    /**
     * Find the arc with the specified endpoint.
     *
     * @param endpoint (not null, not this)
     * @return a pre-existing instance
     */
    public NavArc findArcTo(NavNode endpoint) {
        Validate.nonNull(endpoint, "endpoint");
        if (endpoint == this) {
            throw new IllegalArgumentException("endpoint should be distinct");
        }

        for (NavArc arc : arcs) {
            if (arc.getToNode() == endpoint) {
                return arc;
            }
        }
        return null;
    }

    /**
     * Find the arc whose starting direction is most similar to the specified
     * direction.
     *
     * @param direction unit vector, unaffected
     * @return a pre-existing instance
     */
    public NavArc findLeastTurn(Vector3f direction) {
        Validate.nonNull(direction, "direction");
        if (!direction.isUnitVector()) {
            logger.log(Level.SEVERE, "direction={0}", direction);
            throw new IllegalArgumentException(
                    "direction should be unit vector");
        }

        NavArc result = null;
        float maxDot = -2f;
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
     * Find the arc at a specified list offset relative to the specified base
     * arc.
     *
     * @param baseArc (not null, from this node)
     * @param listOffset
     * @return the pre-existing instance
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
     * Copy the list of arcs which originate from this node.
     *
     * @return new array of existing instances
     */
    NavArc[] getArcs() {
        int size = arcs.size();
        NavArc[] result = new NavArc[size];
        arcs.toArray(result);

        return result;
    }

    /**
     * Read the floating-point value associated with this node.
     */
    float getFloatValue() {
        return floatValue;
    }

    /**
     * Copy the world coordinates of the node.
     *
     * @return a new instance
     */
    public Vector3f getLocation() {
        return location.clone();
    }

    /**
     * Test whether the node has been visited.
     *
     * @return true if it has been visited, false if it hasn't
     */
    boolean isVisited() {
        return visited;
    }

    /**
     * Remove the arc with the specified endpoint.
     *
     * @param endpoint (not null, not this)
     * @return true if successful, false if none found
     */
    boolean removeArcTo(NavNode endpoint) {
        assert endpoint != null;
        assert endpoint != this : endpoint;

        for (NavArc arc : arcs) {
            if (arc.getToNode() == endpoint) {
                arcs.remove(arc);
                return true;
            }
        }
        return false;
    }

    /**
     * Alter the floating-point value associated with this node.
     */
    void setFloatValue(float newValue) {
        floatValue = newValue;
    }

    /**
     * Alter the state of the visited flag.
     */
    void setVisited(boolean newState) {
        visited = newState;
    }
    // *************************************************************************
    // Object methods

    /**
     * Format this arc as a text string.
     *
     * @return description (not null)
     */
    @Override
    public String toString() {
        return description;
    }
}
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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Graph for navigation. Its vertices represent reachable locations in the
 * world. Its arcs represent feasible paths between locations.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class NavGraph {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(NavGraph.class.getName());
    // *************************************************************************
    // fields
    /**
     * list of arcs in this graph
     */
    final protected ArrayList<NavArc> arcs = new ArrayList<>();
    /**
     * list of vertices in this graph
     */
    final protected ArrayList<NavVertex> vertices = new ArrayList<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Create a new arc and add it to this graph.
     *
     * @param startVertex starting point (a member, distinct from endVertex)
     * @param endVertex endpoint (a member)
     * @param pathLength length or cost (arbitrary units, &gt;0)
     * @param startDirection direction at the start (unit vector in world space,
     * unaffected)
     * @return the new instance (a member)
     */
    NavArc addArc(NavVertex startVertex, NavVertex endVertex, float pathLength,
            Vector3f startDirection) {
        assert vertices.contains(startVertex) : endVertex;
        assert vertices.contains(endVertex) : endVertex;
        if (startVertex == endVertex) {
            throw new IllegalArgumentException("endpoints should be distinct");
        }
        Validate.positive(pathLength, "length");
        Validate.nonNull(startDirection, "start direction");
        if (!startDirection.isUnitVector()) {
            throw new IllegalArgumentException(
                    "direction should be a unit vector");
        }

        NavArc newArc = startVertex.addArc(endVertex, pathLength,
                startDirection);
        boolean success = arcs.add(newArc);
        assert success : newArc;

        return newArc;
    }

    /**
     * Find the vertex furthest from the specified starting point.
     *
     * @param startVertex starting point (a member)
     * @return a pre-existing instance (a member)
     */
    public NavVertex findFurthest(NavVertex startVertex) {
        validateMember(startVertex);

        setAllVisited(false);
        calculateDistances(startVertex, 0f);
        /*
         * Search for the maximum distance.
         */
        NavVertex result = startVertex;
        float maxDistance = 0f;
        for (NavVertex vertex : vertices) {
            assert vertex.isVisited();
            float distance = vertex.getFloatValue();
            if (distance > maxDistance) {
                maxDistance = distance;
                result = vertex;
            }
        }

        return result;
    }

    /**
     * Enumerate all arcs.
     *
     * @return new array
     */
    public NavArc[] getArcs() {
        int size = arcs.size();
        NavArc[] result = new NavArc[size];
        int index = 0;
        for (NavArc item : arcs) {
            result[index] = item;
            index++;
        }

        return result;
    }

    /**
     * Enumerate all vertices.
     *
     * @return new array
     */
    public NavVertex[] getVertices() {
        int size = vertices.size();
        NavVertex[] result = new NavVertex[size];
        int index = 0;
        for (NavVertex item : vertices) {
            result[index] = item;
            index++;
        }

        return result;
    }

    /**
     * Select a random arc from this graph.
     *
     * @param generator for uniform random values (not null)
     * @return a pre-existing instance (a member)
     */
    public NavArc randomArc(Random generator) {
        Validate.nonNull(generator, "generator");

        int count = arcs.size();
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = generator.nextInt();
        index = MyMath.modulo(index, count);
        NavArc result = arcs.get(index);

        return result;
    }

    /**
     * Select a random vertex from this graph.
     *
     * @param generator for uniform random values (not null)
     * @return a pre-existing instance (a member)
     */
    public NavVertex randomVertex(Random generator) {
        Validate.nonNull(generator, "generator");

        int count = vertices.size();
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = generator.nextInt();
        index = MyMath.modulo(index, count);
        NavVertex result = vertices.get(index);

        return result;
    }

    /**
     * Remove the specified arc (but not its reverse) from this graph.
     *
     * @param arc arc to be removed (not null)
     */
    public void remove(NavArc arc) {
        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        fromVertex.removeArcTo(toVertex);
        boolean success = arcs.remove(arc);
        assert success;
    }

    /**
     * Verify that an arc belongs to this graph.
     *
     * @param arc arc to be validated
     */
    public void validateMember(NavArc arc) {
        if (!arcs.contains(arc)) {
            logger.log(Level.SEVERE, "arc={0}", arc);
            throw new IllegalArgumentException("graph should contain the arc");
        }
    }

    /**
     * Verify that a vertex belongs to this graph.
     *
     * @param vertex vertex to be validated
     */
    public void validateMember(NavVertex vertex) {
        if (!vertices.contains(vertex)) {
            logger.log(Level.SEVERE, "vertex={0}", vertex);
            throw new IllegalArgumentException(
                    "graph should contain the vertex");
        }
    }
    // *************************************************************************
    // new protected methods

    /**
     * Create a vertex without any arcs and add it to this graph.
     *
     * @param description (not null)
     * @param position (not null, unaffected)
     * @return the new instance
     */
    protected NavVertex addVertex(String description, Vector3f position) {
        Validate.nonNull(description, "description");
        Validate.nonNull(position, "position");

        NavVertex newVertex = new NavVertex(description, position);
        boolean success = vertices.add(newVertex);
        assert success;

        return newVertex;
    }

    /**
     * Test whether this graph would still be connected if the specified arc
     * were removed.
     *
     * @param arc arc to hypothetically remove (a member)
     * @return true if still connected, false if not
     */
    protected boolean isConnectedWithout(NavArc arc) {
        validateMember(arc);

        setAllVisited(false);
        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        boolean result = existsPathWithout(arc, fromVertex, toVertex);

        return result;
    }

    /**
     * Remove the specified arc (and its reverse, if any) from this graph.
     *
     * @param arc arc to remove (a member)
     */
    protected void removePair(NavArc arc) {
        validateMember(arc);

        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        NavArc reverseArc = toVertex.findArcTo(fromVertex);

        remove(arc);
        if (reverseArc != null) {
            remove(reverseArc);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Measure the minimum distance to each vertex in the graph. Note:
     * recursive!
     *
     * @param startVertex starting point for measurement (a member)
     * @param startDistance distance to the starting point (&ge;0)
     */
    private void calculateDistances(NavVertex startVertex,
            float startDistance) {
        assert vertices.contains(startVertex);
        assert startDistance >= 0f : startDistance;

        if (startVertex.isVisited()
                && startDistance > startVertex.getFloatValue()) {
            return;
        }
        /*
         * Update the distance of the starting point.
         */
        startVertex.setFloatValue(startDistance);
        startVertex.setVisited(true);
        /*
         * Follow each arc from the starting point.
         */
        for (NavArc arc : startVertex.getArcs()) {
            NavVertex vertex = arc.getToVertex();
            float vertexDistance = startDistance + arc.getPathLength();
            calculateDistances(vertex, vertexDistance);
        }
    }

    /**
     * Test whether there's a path between two specified vertices which avoids a
     * specified arc. Note: recursive!
     *
     * @param avoidArc which arc to avoid (a member)
     * @param fromVertex starting point (a member)
     * @param toVertex endpoint (a member)
     * @return true if such a path exists, false if no such path exists
     */
    private boolean existsPathWithout(NavArc avoidArc, NavVertex fromVertex,
            NavVertex toVertex) {
        assert arcs.contains(avoidArc) : avoidArc;
        assert vertices.contains(fromVertex) : fromVertex;
        assert vertices.contains(toVertex) : toVertex;

        if (fromVertex == toVertex) {
            return true;
        }
        if (fromVertex.isVisited()) {
            return false;
        }

        fromVertex.setVisited(true);

        for (NavArc arc : fromVertex.getArcs()) {
            if (arc == avoidArc) {
                continue;
            }
            NavVertex vertex = arc.getToVertex();
            boolean pathExists = existsPathWithout(avoidArc, vertex, toVertex);
            if (pathExists) {
                return true;
            }
        }
        return false;
    }

    /**
     * Alter the visited status of all vertices in this graph.
     *
     * @param newState true &rarr; visited, false &rarr; not visited
     */
    private void setAllVisited(boolean newState) {
        for (NavVertex vertex : vertices) {
            vertex.setVisited(newState);
        }
    }
}
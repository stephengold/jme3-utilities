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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.Noise;

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
    final protected ArrayList<NavArc> arcs = new ArrayList<>(30);
    /**
     * list of vertices in this graph
     */
    final protected ArrayList<NavVertex> vertices = new ArrayList<>(30);
    // *************************************************************************
    // new methods exposed

    /**
     * Create a new arc and add it to this graph.
     *
     * @param startVertex starting point (member of this graph, distinct from
     * endVertex)
     * @param endVertex endpoint (member of this graph)
     * @param pathLength length or cost (arbitrary units, &gt;0)
     * @param startDirection direction at the start (unit vector in world space,
     * unaffected)
     * @return new member
     */
    public NavArc addArc(NavVertex startVertex, NavVertex endVertex,
            float pathLength, Vector3f startDirection) {
        validateMember(startVertex);
        validateMember(endVertex);
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
     * Create a vertex without any arcs and add it to this graph.
     *
     * @param description (not null)
     * @param position (not null, unaffected)
     * @return new instance
     */
    public NavVertex addVertex(String description, Vector3f position) {
        Validate.nonNull(description, "description");
        Validate.nonNull(position, "position");

        NavVertex newVertex = new NavVertex(description, position);
        boolean success = vertices.add(newVertex);
        assert success;

        return newVertex;
    }

    /**
     * Enumerate all vertices located the specified number of hops from a
     * specified starting point.
     *
     * @param hopCount (&ge;0)
     * @param startVertex (member of this graph)
     * @return new list of members
     */
    public List<NavVertex> findByHops(int hopCount, NavVertex startVertex) {
        Validate.nonNegative(hopCount, "count");
        validateMember(startVertex);

        List<NavVertex> result = findByHops(hopCount, hopCount, startVertex);
        return result;
    }

    /**
     * Enumerate all vertices located in the specified inclusive range of hops.
     *
     * @param minHopCount (&ge;0)
     * @param maxHopCount (&ge;minHopCount)
     * @param startVertex (member of this graph)
     * @return new list of members
     */
    public List<NavVertex> findByHops(int minHopCount, int maxHopCount,
            NavVertex startVertex) {
        Validate.nonNegative(minHopCount, "count");
        if (minHopCount > maxHopCount) {
            logger.log(Level.SEVERE, "min={0} max={1}",
                    new Object[]{minHopCount, maxHopCount});
            throw new IllegalArgumentException(
                    "min should not be greater than max");
        }
        validateMember(startVertex);

        Map<NavVertex, Integer> hopData = new TreeMap<>();
        calculateHops(startVertex, 0, hopData);

        List<NavVertex> result = new ArrayList<>(30);
        for (NavVertex vertex : vertices) {
            Integer hops = hopData.get(vertex);
            if (hops != null && hops >= minHopCount && hops <= maxHopCount) {
                result.add(vertex);
            }
        }

        return result;
    }

    /**
     * Find the vertex furthest from the specified starting point.
     *
     * @param startVertex starting point (member of this graph)
     * @return pre-existing member
     */
    public NavVertex findFurthest(NavVertex startVertex) {
        validateMember(startVertex);
        NavVertex result = findFurthest(startVertex, vertices);
        return result;
    }

    /**
     * Find the vertex in the specified list that's furthest from the specified
     * starting point.
     *
     * @param startVertex starting point (member of this graph)
     * @param candidates collection of candidate vertices (not null, unaffected)
     * @return pre-existing member
     */
    public NavVertex findFurthest(NavVertex startVertex,
            Collection<NavVertex> candidates) {
        validateMember(startVertex);
        Validate.nonNull(candidates, "list");

        Map<NavVertex, Float> hopData = new TreeMap<>();
        calculateDistances(startVertex, 0f, hopData);
        /*
         * Search for the maximum distance.
         */
        NavVertex result = startVertex;
        float maxDistance = 0f;
        for (NavVertex vertex : candidates) {
            Float distance = hopData.get(vertex);
            if (distance != null && distance > maxDistance) {
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
     * Test whether this graph would still be connected if the specified arc
     * were removed.
     *
     * @param arc arc to hypothetically remove (member of this graph)
     * @return true if still connected, false if not
     */
    public boolean isConnectedWithout(NavArc arc) {
        validateMember(arc);

        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        Set<NavVertex> visitedVertices = new TreeSet<>();
        boolean result = existsPathWithout(arc, fromVertex, toVertex,
                visitedVertices);

        return result;
    }

    /**
     * Select a random arc from this graph.
     *
     * @param generator generator of uniform random values (not null)
     * @return pre-existing member
     */
    public NavArc randomArc(Random generator) {
        Validate.nonNull(generator, "generator");
        NavArc result = (NavArc) Noise.pick(arcs, generator);
        return result;
    }

    /**
     * Select a random vertex from this graph.
     *
     * @param generator generator of uniform random values (not null)
     * @return pre-existing member
     */
    public NavVertex randomVertex(Random generator) {
        Validate.nonNull(generator, "generator");
        NavVertex result = (NavVertex) Noise.pick(vertices, generator);
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
     * Remove the specified arc (and its reverse, if any) from this graph.
     *
     * @param arc arc to member
     */
    public void removePair(NavArc arc) {
        validateMember(arc);

        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        NavArc reverseArc = toVertex.findArcTo(fromVertex);

        remove(arc);
        if (reverseArc != null) {
            remove(reverseArc);
        }
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
    // private methods

    /**
     * Measure the minimum distance to each vertex in the graph. Note:
     * recursive!
     *
     * @param currentVertex vertex being visited (member of this graph)
     * @param distance distance from the start vertex to the current vertex
     * (&ge;0)
     * @param pathData distances of all vertices already visited (not null)
     */
    private void calculateDistances(NavVertex currentVertex,
            float distance, Map<NavVertex, Float> pathData) {
        assert vertices.contains(currentVertex);
        assert distance >= 0f : distance;
        assert pathData != null;

        if (pathData.containsKey(currentVertex)
                && distance > pathData.get(currentVertex)) {
            return;
        }
        /*
         * Update the distance of the current vertex.
         */
        pathData.put(currentVertex, distance);
        /*
         * Follow each arc from the current vertex.
         */
        for (NavArc arc : currentVertex.getArcs()) {
            NavVertex vertex = arc.getToVertex();
            float newDistance = distance + arc.getPathLength();
            calculateDistances(vertex, newDistance, pathData);
        }
    }

    /**
     * Measure the minimum number of hops to each vertex in the graph. Note:
     * recursive!
     *
     * @param currentVertex vertex being visited (member of this graph)
     * @param hopCount hop count from the start vertex to the current vertex
     * (&ge;0)
     * @param pathData hop counts of all vertices already visited (not null)
     */
    private void calculateHops(NavVertex currentVertex,
            int hopCount, Map<NavVertex, Integer> pathData) {
        assert vertices.contains(currentVertex);
        assert hopCount >= 0 : hopCount;
        assert pathData != null;

        if (pathData.containsKey(currentVertex)
                && hopCount > pathData.get(currentVertex)) {
            return;
        }
        /*
         * Update the hop count of the current vertex.
         */
        pathData.put(currentVertex, hopCount);
        /*
         * Follow each arc from the current vertex.
         */
        int nextHopCount = hopCount + 1;
        for (NavArc arc : currentVertex.getArcs()) {
            NavVertex vertex = arc.getToVertex();
            calculateHops(vertex, nextHopCount, pathData);
        }
    }

    /**
     * Test whether there's a path between two specified vertices which avoids a
     * specified arc. Note: recursive!
     *
     * @param avoidArc arc to avoid (member of this graph)
     * @param fromVertex starting point (member of this graph)
     * @param toVertex endpoint (member of this graph)
     * @return true if such a path exists, false if no such path exists
     */
    private boolean existsPathWithout(NavArc avoidArc, NavVertex fromVertex,
            NavVertex toVertex, Set<NavVertex> visitedVertices) {
        assert arcs.contains(avoidArc) : avoidArc;
        assert vertices.contains(fromVertex) : fromVertex;
        assert vertices.contains(toVertex) : toVertex;

        if (fromVertex == toVertex) {
            return true;
        }
        if (visitedVertices.contains(fromVertex)) {
            return false;
        }

        visitedVertices.add(fromVertex);

        for (NavArc arc : fromVertex.getArcs()) {
            if (arc == avoidArc) {
                continue;
            }
            NavVertex vertex = arc.getToVertex();
            boolean pathExists = existsPathWithout(avoidArc, vertex, toVertex,
                    visitedVertices);
            if (pathExists) {
                return true;
            }
        }
        return false;
    }
}
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.locus.Locus3f;
import jme3utilities.math.MyVector3f;

/**
 * Graph for navigation. Its vertices represent reachable locations in the
 * world. Its arcs represent feasible paths between locations.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NavGraph {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            NavGraph.class.getName());
    // *************************************************************************
    // fields

    /**
     * cost (or length) of each arc in this graph
     */
    final private Map<NavArc, Float> arcCosts = new HashMap<>(100);
    /**
     * vertex for each name
     */
    final private Map<String, NavVertex> vertices = new HashMap<>(30);
    // *************************************************************************
    // new methods exposed

    /**
     * Connect two member vertices with a new arc.
     *
     * @param origin originating member vertex (distinct from terminus)
     * @param terminus terminating member vertex (distinct from origin)
     * @param initialCost initial cost (or length) of the arc
     * @return a new member arc
     */
    public NavArc addArc(NavVertex origin, NavVertex terminus,
            float initialCost) {
        validateMember(origin, "origin");
        validateMember(terminus, "terminus");
        if (origin == terminus) {
            throw new IllegalArgumentException("vertices not distinct");
        }

        NavArc newArc = new NavArc(origin, terminus);
        if (arcCosts.containsKey(newArc)) {
            throw new IllegalStateException("arc already exists");
        }
        Float oldcost = arcCosts.put(newArc, initialCost);
        assert oldcost == null;

        origin.addOutgoing(newArc);
        terminus.addIncoming(newArc);

        return newArc;
    }

    /**
     * Connect two member vertices with a pair of new arcs.
     *
     * @param v1 first vertex (member of this graph, distinct from endVertex)
     * @param v2 second vertex (member of this graph)
     * @param initialCost initial cost (or length) applied to both new arcs
     */
    public void addArcPair(NavVertex v1, NavVertex v2, float initialCost) {
        validateMember(v1, "v1");
        validateMember(v2, "v2");
        if (v1 == v2) {
            throw new IllegalArgumentException("vertices not distinct");
        }

        addArc(v1, v2, initialCost);
        addArc(v2, v1, initialCost);
    }

    /**
     * Create a new vertex without any arcs and add it to this graph.
     *
     * @param name name for the new vertex (not null, not in use)
     * @param locus region represented by the new vertex (in world coordinates) or null
     * @param location coordinates for calculating arc offsets (not null,
     * unaffected)
     * @return a new member vertex
     */
    public NavVertex addVertex(String name, Locus3f locus, Vector3f location) {
        Validate.nonNull(name, "name");
        if (vertices.containsKey(name)) {
            throw new IllegalArgumentException("name already in use");
        }
        Validate.nonNull(location, "location");

        NavVertex newVertex = new NavVertex(name, locus, location);
        NavVertex oldVertex = vertices.put(name, newVertex);
        assert oldVertex == null : oldVertex;

        return newVertex;
    }

    /**
     * Test whether a particular arc is a member of this graph.
     *
     * @param arc arc to test (not null)
     * @return true if it's a member, otherwise false
     */
    public boolean contains(NavArc arc) {
        Validate.nonNull(arc, "arc");

        boolean result = arcCosts.containsKey(arc);
        return result;
    }

    /**
     * Test whether a particular vertex is a member of this graph.
     *
     * @param vertex vertex to test (not null)
     * @return true if it's a member, otherwise false
     */
    public boolean contains(NavVertex vertex) {
        Validate.nonNull(vertex, "vertex");

        boolean result = vertices.containsValue(vertex);
        return result;
    }

    /**
     * Enumerate all member arcs as an array.
     *
     * @return a new array of pre-existing member arcs
     */
    public NavArc[] copyArcs() {
        NavArc[] result = new NavArc[numArcs()];
        int i = 0;
        for (NavArc arc : arcCosts.keySet()) {
            result[i] = arc;
            i++;
        }

        return result;
    }

    /**
     * Enumerate all member vertices as an array.
     *
     * @return new array of pre-existing member vertices
     */
    public NavVertex[] copyVertices() {
        NavVertex[] result = new NavVertex[numVertices()];
        int i = 0;
        for (NavVertex vertex : vertices.values()) {
            result[i] = vertex;
            i++;
        }

        return result;
    }

    /**
     * Count the number of vertices reachable from the specified member vertex.
     *
     * @param start input vertex (member)
     * @return true if connected, false otherwise
     */
    public int countReachableFrom(NavVertex start) {
        validateMember(start, "start");

        Set<NavVertex> visited = new HashSet<>();
        visitReachable(start, visited);

        return visited.size();
    }

    /**
     * Find a member vertex with the specified name.
     *
     * @param name (not null)
     * @return pre-existing member vertex, or null if none found
     */
    public NavVertex find(String name) {
        Validate.nonNull(name, "name");
        NavVertex result = vertices.get(name);
        return result;
    }

    /**
     * Find a member vertex which contains the specified point.
     *
     * @param point (not null, unaffected)
     * @return pre-existing member vertex, or null if none found
     */
    public NavVertex findContains(Vector3f point) {
        Validate.nonNull(point, "point");
        /*
         * Test the closest vertex first.
         */
        Collection<NavVertex> allVertices = vertices.values();
        NavVertex result = findNearest(allVertices, point);
        if (result == null || result.getLocus().contains(point)) {
            return result;
        }
        /*
         * Test the next closest, and so on.  TODO sort
         */
        Collection<NavVertex> remainingVertices;
        remainingVertices = new HashSet<>(vertices.size());
        for (NavVertex v : vertices.values()) {
            remainingVertices.add(v);
        }
        boolean success = remainingVertices.remove(result);
        assert success;

        while (!remainingVertices.isEmpty()) {
            result = findNearest(remainingVertices, point);
            assert result != null;
            assert remainingVertices.contains(result);

            if (result.getLocus().contains(point)) {
                return result;
            }

            success = remainingVertices.remove(result);
            assert success;
        }

        return null;
    }

    /**
     * Enumerate all member vertices which are exactly a specified number of
     * hops from a specified vertex.
     *
     * @param hopCount (&ge;0)
     * @param startVertex (member of this graph)
     * @return new list of per-existing member vertices
     */
    public List<NavVertex> findByHops(int hopCount, NavVertex startVertex) {
        Validate.nonNegative(hopCount, "count");
        validateMember(startVertex, "start vertex");

        List<NavVertex> result = findByHops(hopCount, hopCount, startVertex);
        return result;
    }

    /**
     * Enumerate all member vertices which are in a specified inclusive range of
     * hops from a specified vertex.
     *
     * @param minHopCount (&ge;0)
     * @param maxHopCount (&ge;minHopCount)
     * @param startVertex (member of this graph)
     * @return new list of pre-existing member vertices
     */
    public List<NavVertex> findByHops(int minHopCount, int maxHopCount,
            NavVertex startVertex) {
        Validate.nonNegative(minHopCount, "count");
        if (minHopCount > maxHopCount) {
            logger.log(Level.SEVERE, "min={0} max={1}",
                    new Object[]{minHopCount, maxHopCount});
            throw new IllegalArgumentException("min should exceed max");
        }
        validateMember(startVertex, "start vertex");

        Map<NavVertex, Integer> hopData = new HashMap<>(numVertices());
        calculateHops(startVertex, 0, hopData);

        List<NavVertex> result = new ArrayList<>(30);
        for (NavVertex vertex : vertices.values()) {
            Integer hops = hopData.get(vertex);
            if (hops != null && hops >= minHopCount && hops <= maxHopCount) {
                result.add(vertex);
            }
        }

        return result;
    }

    /**
     * Enumerate all member vertices which are the maximal number of hops from a
     * specified vertex.
     *
     * @param startVertex (member of this graph)
     * @return new list of pre-existing member vertices
     */
    public List<NavVertex> findMostHops(NavVertex startVertex) {
        validateMember(startVertex, "start vertex");
        List<NavVertex> result = findMostHops(
                startVertex, vertices.values());
        return result;
    }

    /**
     * Enumerate all member vertices in a specified subset which are the maximal
     * number of hops from a specified vertex.
     *
     * @param startVertex (member of this graph)
     * @param subset (not null, all members of this graph, unaffected)
     * @return new list of pre-existing member vertices
     */
    public List<NavVertex> findMostHops(NavVertex startVertex,
            Collection<NavVertex> subset) {
        validateMember(startVertex, "start vertex");

        Map<NavVertex, Integer> hopData = new HashMap<>(subset.size());
        calculateHops(startVertex, 0, hopData);

        int mostHops = 0;
        List<NavVertex> result = new ArrayList<>(10);
        for (NavVertex vertex : subset) {
            Integer hops = hopData.get(vertex);
            if (hops != null) {
                if (hops > mostHops) {
                    mostHops = hops;
                    result.clear();
                }
                if (hops == mostHops) {
                    result.add(vertex);
                }
            }
        }

        return result;
    }

    /**
     * Find the vertex nearest to a specified point.
     *
     * @param point coordinate vector (not null, unaffected)
     * @return pre-existing member vertex, or null if none were specified
     */
    public NavVertex findNearest(Vector3f point) {
        Validate.nonNull(point, "point");

        NavVertex result = null;
        double nearest = Double.POSITIVE_INFINITY;

        for (NavVertex vertex : vertices.values()) {
            Locus3f locus = vertex.getLocus();
            Vector3f location = locus.findLocation(point);
            double ds = MyVector3f.distanceSquared(point, location);
            if (ds < nearest) {
                nearest = ds;
                result = vertex;
            }
        }

        return result;
    }

    /**
     * Find the vertex (in a specified subset) nearest to a specified
     * point.
     *
     * @param subset (not null, all elements non-null, unaffected)
     * @param point coordinate vector (not null, unaffected)
     * @return pre-existing member vertex, or null if none were specified
     */
    public NavVertex findNearest(Collection<NavVertex> subset, Vector3f point) {
        Validate.nonNull(point, "point");
        Validate.nonNull(subset, "subset");

        NavVertex result = null;
        double nearest = Double.POSITIVE_INFINITY;

        for (NavVertex vertex : subset) {
            Locus3f locus = vertex.getLocus();
            Vector3f location = locus.findLocation(point);
            double ds = MyVector3f.distanceSquared(point, location);
            if (ds < nearest) {
                nearest = ds;
                result = vertex;
            }
        }

        return result;
    }

    /**
     * Read the current cost (or length) of a member arc.
     *
     * @param arc which arc to modify (member of this graph)
     * @return cost (or length) of arc
     */
    public float getCost(NavArc arc) {
        validateMember(arc, "arc");
        Float result = arcCosts.get(arc);
        return result;
    }

    /**
     * Test whether every vertex in is reachable from every other.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        int numVertices = numVertices();
        for (NavVertex vertex : vertices.values()) {
            if (countReachableFrom(vertex) != numVertices) {
                return false;
            }
        }

        return true;
    }

    /**
     * Test whether the endpoints of the specified arc would still be connected
     * if the arc were removed. In other words, whether the arc is part of a
     * loop.
     *
     * @param arc arc to hypothetically remove (member of this graph)
     * @return true if still connected, false if not
     */
    public boolean isConnectedWithout(NavArc arc) {
        validateMember(arc, "arc");

        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        Set<NavVertex> visitedVertices = new HashSet<>();
        boolean result = existsPathWithout(
                arc, fromVertex, toVertex, visitedVertices);

        return result;
    }

    /**
     * Test whether the specified vertex is a member of this graph.
     *
     * @param vertex input (may be null)
     * @return true if a member, otherwise false
     */
    public boolean isMember(NavVertex vertex) {
        if (vertex == null) {
            return false;
        }
        String name = vertex.getName();
        if (find(name) == vertex) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether this graph contains a reverse arc for every member arc.
     *
     * @return true if a reversible, otherwise false
     */
    public boolean isReversible() {
        for (NavArc arc : arcCosts.keySet()) {
            NavArc reverse = arc.findReverse();
            if (!contains(reverse)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Enumerate all member arcs as a list.
     *
     * @return new list of pre-existing member arcs
     */
    public List<NavArc> listArcs() {
        Set<NavArc> arcs = arcCosts.keySet();
        List<NavArc> result = new ArrayList<>(numArcs());
        for (NavArc arc : arcs) {
            result.add(arc);
        }

        return result;
    }

    /**
     * Enumerate all member vertices as a list.
     *
     * @return new list of pre-existing member vertices
     */
    public List<NavVertex> listVertices() {
        List<NavVertex> result = new ArrayList<>(numVertices());
        for (NavVertex vertex : vertices.values()) {
            result.add(vertex);
        }

        return result;
    }

    /**
     * Count how many arcs this graph contains.
     *
     * @return count (&ge;0)
     */
    public int numArcs() {
        int result = arcCosts.size();
        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many vertices this graph contains.
     *
     * @return count (&ge;0)
     */
    public int numVertices() {
        int result = vertices.size();
        assert result >= 0 : result;
        return result;
    }

    /**
     * Remove the specified member arc from this graph.
     *
     * @param arc member arc to remove
     */
    public void remove(NavArc arc) {
        validateMember(arc, "arc");

        Float oldCost = arcCosts.remove(arc);
        assert oldCost != null;

        NavVertex fromVertex = arc.getFromVertex();
        fromVertex.removeOutgoing(arc);

        NavVertex toVertex = arc.getToVertex();
        toVertex.removeIncoming(arc);
    }

    /**
     * Remove the arc from one member vertex to another.
     *
     * @param origin originating member vertex (not null)
     * @param terminus terminating member vertex (not null)
     * @return true for success, false arc not found
     */
    public boolean remove(NavVertex origin, NavVertex terminus) {
        validateMember(origin, "origin");
        validateMember(terminus, "terminus");

        NavArc arc = origin.findOutgoing(terminus);
        if (arc == null) {
            return false;
        }
        remove(arc);
        return true;
    }

    /**
     * Find the shortest (or cheapest) path from one vertex to another.
     *
     * @param startVertex starting point (member of this graph, distinct from
     * endVertex)
     * @param endVertex goal (member of this graph)
     * @return first arc in path (existing member arc) or null if unreachable
     */
    public NavArc seek(NavVertex startVertex, NavVertex endVertex) {
        validateMember(startVertex, "start vertex");
        validateMember(endVertex, "end vertex");
        if (startVertex == endVertex) {
            throw new IllegalArgumentException("vertices not distinct");
        }

        Map<NavVertex, Float> distanceData = new HashMap<>(numVertices());
        calculateTotalCosts(endVertex, 0f, distanceData);

        NavArc result = null;
        float minDistance = Float.MAX_VALUE;
        for (NavArc arc : startVertex.copyOutgoing()) {
            NavVertex neighbor = arc.getToVertex();
            float distance = distanceData.get(neighbor) + arcCosts.get(arc);
            if (distance < minDistance) {
                minDistance = distance;
                result = arc;
            }
        }

        return result;
    }

    /**
     * Alter the cost (or length) of a member arc.
     *
     * @param arc which arc to modify (member of this graph)
     * @param newCost value for cost (or length) of arc
     */
    public void setCost(NavArc arc, float newCost) {
        validateMember(arc, "arc");
        Float oldCost = arcCosts.put(arc, newCost);
        assert oldCost != null;
    }

    /**
     * Calculate the distance from the specified starting point to the first
     * point of support (if any) directly below it in the locus of this vertex.
     *
     * @param location coordinates of starting point(not null, unaffected)
     * @param cosineTolerance cosine of maximum slope for support (&gt;0, &lt;1)
     * @return the shortest support distance (&ge;0) or
     * {@link Float#POSITIVE_INFINITY} if no support
     */
    public float supportDistance(Vector3f location, float cosineTolerance) {
        Validate.nonNull(location, "point");

        float shortest = Float.POSITIVE_INFINITY;
        for (NavVertex vertex : vertices.values()) {
            Locus3f locus = vertex.getLocus();
            float distance = locus.supportDistance(location, cosineTolerance);
            if (distance < shortest) {
                shortest = distance;
            }
        }

        return shortest;
    }

    /**
     * Verify that an arc (used as a method argument) is a member of this graph.
     *
     * @param arc arc argument to be validated
     * @param description description of the argument
     */
    public void validateMember(NavArc arc, String description) {
        if (!arcCosts.keySet().contains(arc)) {
            String what;
            if (description == null) {
                what = "arc argument";
            } else {
                what = description;
            }

            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, arc});
            String message = String.format("%s must be a member.", what);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Verify that a vertex (used as a method argument) is a member of this
     * graph.
     *
     * @param vertex vertex argument to be validated
     * @param description description of the argument
     */
    public void validateMember(NavVertex vertex, String description) {
        Validate.nonNull(vertex, description);

        if (!isMember(vertex)) {
            String what;
            if (description == null) {
                what = "vertex argument";
            } else {
                what = description;
            }

            logger.log(Level.SEVERE, "{0}={1}", new Object[]{what, vertex});
            String message = String.format("%s must be a member.", what);
            throw new IllegalArgumentException(message);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the minimum total cost (or distance) to each vertex in the
     * graph from a fixed starting point. Note: recursive!
     *
     * @param currentVertex vertex being visited (member of this graph)
     * @param costSoFar total cost from the starting point to the current vertex
     * (&ge;0)
     * @param costData total costs of vertices already visited (not null)
     */
    private void calculateTotalCosts(NavVertex currentVertex,
            float costSoFar, Map<NavVertex, Float> costData) {
        assert isMember(currentVertex) : currentVertex;
        assert costSoFar >= 0f : costSoFar;
        assert costData != null;

        if (costData.containsKey(currentVertex)
                && costSoFar > costData.get(currentVertex)) {
            return;
        }
        /*
         * Update the distance of the current vertex.
         */
        costData.put(currentVertex, costSoFar);
        /*
         * Follow each outgoing arc from the current vertex.
         */
        for (NavArc arc : currentVertex.copyOutgoing()) {
            NavVertex vertex = arc.getToVertex();
            float newDistance = costSoFar + arcCosts.get(arc);
            calculateTotalCosts(vertex, newDistance, costData);
        }
    }

    /**
     * Measure the minimum number of hops to each vertex in the graph. Note:
     * recursive!
     *
     * @param currentVertex vertex being visited (member of this graph)
     * @param hopsSoFar hop count from the start vertex to the current vertex
     * (&ge;0)
     * @param hopData hop counts of all vertices already visited (not null)
     */
    private void calculateHops(NavVertex currentVertex,
            int hopsSoFar, Map<NavVertex, Integer> hopData) {
        assert isMember(currentVertex) : currentVertex;
        assert hopsSoFar >= 0 : hopsSoFar;
        assert hopData != null;

        if (hopData.containsKey(currentVertex)
                && hopsSoFar > hopData.get(currentVertex)) {
            return;
        }
        /*
         * Update the hop count of the current vertex.
         */
        hopData.put(currentVertex, hopsSoFar);
        /*
         * Follow each outgoing arc from the current vertex.
         */
        int newHopCount = hopsSoFar + 1;
        for (NavArc arc : currentVertex.copyOutgoing()) {
            NavVertex vertex = arc.getToVertex();
            calculateHops(vertex, newHopCount, hopData);
        }
    }

    /**
     * Test whether there's a path between two specified member vertices which
     * avoids a specified arc. Note: recursive!
     *
     * @param avoidArc arc to avoid (member of this graph)
     * @param fromVertex starting point (member of this graph)
     * @param toVertex endpoint (member of this graph)
     * @param visitedVertices
     * @return true if such a path exists, false if no such path exists
     */
    private boolean existsPathWithout(NavArc avoidArc, NavVertex fromVertex,
            NavVertex toVertex, Set<NavVertex> visitedVertices) {
        assert arcCosts.keySet().contains(avoidArc) : avoidArc;
        assert isMember(fromVertex) : fromVertex;
        assert isMember(toVertex) : toVertex;

        if (fromVertex == toVertex) {
            return true;
        }
        if (visitedVertices.contains(fromVertex)) {
            return false;
        }

        Set<NavVertex> newSet = new HashSet<>(visitedVertices);
        newSet.add(fromVertex);

        for (NavArc arc : fromVertex.copyOutgoing()) {
            if (arc.equals(avoidArc)) {
                continue;
            }
            NavVertex vertex = arc.getToVertex();
            boolean pathExists = existsPathWithout(
                    avoidArc, vertex, toVertex, newSet);
            if (pathExists) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enumerate all vertices reachable from a given member vertex. Note:
     * recursive!
     *
     * @param fromVertex starting point (member of this graph)
     * @param visited vertices visited so far, potentially added to (not null)
     */
    private void visitReachable(NavVertex fromVertex, Set<NavVertex> visited) {
        assert isMember(fromVertex) : fromVertex;
        assert visited != null;

        if (visited.contains(fromVertex)) {
            return;
        }
        boolean success = visited.add(fromVertex);
        assert success;

        for (NavArc arc : fromVertex.copyOutgoing()) {
            NavVertex vertex = arc.getToVertex();
            visitReachable(vertex, visited);
        }
    }
}

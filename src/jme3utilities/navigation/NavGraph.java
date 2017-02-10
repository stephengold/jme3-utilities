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
 * Graph for navigation. Its vertices represent reachable regions of the world.
 * Its arcs represent feasible routes between regions.
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
     * cost (or length) of each arc in this graph (all &ge;0)
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
     * @param initialCost initial cost (or length) of the arc (&ge;0)
     * @return a new member arc
     */
    public NavArc addArc(NavVertex origin, NavVertex terminus,
            float initialCost) {
        validateMember(origin, "origin");
        validateMember(terminus, "terminus");
        if (origin == terminus) {
            throw new IllegalArgumentException("vertices not distinct");
        }
        Validate.nonNegative(initialCost, "initial cost");

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
     * @param v1 first vertex (member, distinct from v2)
     * @param v2 second vertex (member, distinct from v1)
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
     * @param locus region represented by the new vertex (in world coordinates)
     * or null
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
     * Test whether the specified arc is a member of this graph.
     *
     * @param arc input (may be null)
     * @return true if a member, otherwise false
     */
    public boolean contains(NavArc arc) {
        if (arc == null) {
            return false;
        }
        if (arcCosts.containsKey(arc)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the specified vertex is a member of this graph.
     *
     * @param vertex input (may be null)
     * @return true if a member, otherwise false
     */
    public boolean contains(NavVertex vertex) {
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
     * @return a pre-existing member vertex, or null if none found
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
     * @param startVertex (member)
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
     * @param startVertex (member)
     * @return a new list of pre-existing member vertices
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
        forwardHopCounts(startVertex, 0, hopData);

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
     * @param startVertex (member)
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
     * @param startVertex (member)
     * @param subset (not null, all members of this graph, unaffected)
     * @return a new list of pre-existing member vertices
     */
    public List<NavVertex> findMostHops(NavVertex startVertex,
            Collection<NavVertex> subset) {
        validateMember(startVertex, "start vertex");

        Map<NavVertex, Integer> hopData = new HashMap<>(subset.size());
        forwardHopCounts(startVertex, 0, hopData);

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
     * Find the vertex (in a specified subset) nearest to a specified point.
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
     * @param arc which arc to modify (member)
     * @return cost (or length) of arc (&ge;0)
     */
    public float getCost(NavArc arc) {
        validateMember(arc, "arc");
        Float result = arcCosts.get(arc);

        assert result >= 0f : result;
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
     * @param arc arc to hypothetically remove (member)
     * @return true if still connected, false if not
     */
    public boolean isConnectedWithout(NavArc arc) {
        validateMember(arc, "arc");

        NavVertex fromVertex = arc.getFromVertex();
        NavVertex toVertex = arc.getToVertex();
        Set<NavVertex> visitedVertices = new HashSet<>();
        boolean result = existsRouteWithout(
                arc, fromVertex, toVertex, visitedVertices);

        return result;
    }

    /**
     * Test whether this graph contains a reverse arc for every member arc.
     *
     * @return true if every arc is reversible, otherwise false
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
     * Find the shortest (or cheapest) route from one vertex to another.
     *
     * @param startVertex starting point (member, distinct from endVertex)
     * @param endVertex goal (member, distinct from startVertex)
     * @return a new list of arcs, or null if goal is unreachable
     */
    public List<NavArc> seek(NavVertex startVertex, NavVertex endVertex) {
        validateMember(startVertex, "start vertex");
        validateMember(endVertex, "end vertex");
        if (startVertex == endVertex) {
            throw new IllegalArgumentException("vertices not distinct");
        }

        Map<NavVertex, Float> totalCosts = new HashMap<>(numVertices());
        reverseTotalCosts(endVertex, 0f, totalCosts);
        if (totalCosts.get(startVertex) == null) {
            return null;
        }
        List<NavArc> result = new ArrayList<>(10);
        NavVertex routeVertex = startVertex;
        while (routeVertex != endVertex) {
            NavArc nextArc = null;
            float minDistance = Float.MAX_VALUE;
            for (NavArc arc : routeVertex.copyOutgoing()) {
                NavVertex neighbor = arc.getToVertex();
                float distance = totalCosts.get(neighbor) + arcCosts.get(arc);
                if (distance < minDistance) {
                    minDistance = distance;
                    nextArc = arc;
                }
            }
            routeVertex = nextArc.getToVertex();
            assert routeVertex != startVertex;
            result.add(nextArc);
        }

        return result;
    }

    /**
     * Alter the cost (or length) of a member arc.
     *
     * @param arc which arc to modify (member)
     * @param newCost value for cost (or length) of arc (&ge;0)
     */
    public void setCost(NavArc arc, float newCost) {
        validateMember(arc, "arc");
        Validate.nonNegative(newCost, "new cost");

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
        if (!contains(arc)) {
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

        if (!contains(vertex)) {
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
     * Test whether there's a route from a starting vertex to an ending vertex
     * which avoids a specified arc. Note: recursive depth-first traversal.
     *
     * @param avoid arc to avoid (member)
     * @param visit vertex being visited, initially the starting vertex (member,
     * unaffected)
     * @param ending ending vertex (member, unaffected)
     * @param visitedSet set of vertices previously visited, initially empty
     * (not null, unaffected)
     * @return true if such a route exists, false if no such route exists
     */
    private boolean existsRouteWithout(NavArc avoid, NavVertex visit,
            NavVertex ending, Set<NavVertex> visitedSet) {
        assert contains(avoid) : avoid;
        assert contains(visit) : visit;
        assert contains(ending) : ending;

        if (visit == ending) {
            return true;
        }
        if (visitedSet.contains(visit)) {
            return false;
        }

        Set<NavVertex> nextVisited = new HashSet<>(visitedSet);
        nextVisited.add(visit);

        for (NavArc arc : visit.copyOutgoing()) {
            if (arc.equals(avoid)) {
                continue;
            }
            NavVertex nextVisit = arc.getToVertex();
            boolean exists = existsRouteWithout(
                    avoid, nextVisit, ending, nextVisited);
            if (exists) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate the minimum number of hops (arcs traversed) to each vertex from
     * a starting vertex. Note: recursive depth-first traversal.
     *
     * @param visit vertex being visited, initially the starting vertex (member,
     * unaffected)
     * @param hopCount hop count from the start vertex to the current vertex,
     * initially zero (&ge;0)
     * @param minHopCounts minimum hop counts to vertices previously visited,
     * initially empty (not null, updated)
     */
    private void forwardHopCounts(NavVertex visit, int hopCount,
            Map<NavVertex, Integer> minHopCounts) {
        assert contains(visit) : visit;
        assert hopCount >= 0 : hopCount;
        assert minHopCounts != null;

        if (minHopCounts.containsKey(visit)
                && hopCount > minHopCounts.get(visit)) {
            return;
        }
        /*
         * Update the hop count of the current vertex.
         */
        minHopCounts.put(visit, hopCount);
        /*
         * Follow each outgoing arc from the current vertex.
         */
        int nextHopCount = hopCount + 1;
        for (NavArc arc : visit.copyOutgoing()) {
            NavVertex nextVisit = arc.getToVertex();
            forwardHopCounts(nextVisit, nextHopCount, minHopCounts);
        }
    }

    /**
     * Calculate the minimum total cost (or distance) from each vertex to an
     * ending vertex. Note: recursive depth-first traversal.
     *
     * @param visit vertex being visited, initially the ending vertex (member,
     * unaffected)
     * @param totalCost total cost from the current vertex to the ending vertex,
     * initially zero (&ge;0)
     * @param minTotalCosts minimum total costs from vertices previously
     * visited, initially empty (not null, updated)
     */
    private void reverseTotalCosts(NavVertex visit, float totalCost,
            Map<NavVertex, Float> minTotalCosts) {
        assert contains(visit) : visit;
        assert totalCost >= 0f : totalCost;
        assert minTotalCosts != null;

        if (minTotalCosts.containsKey(visit)
                && totalCost > minTotalCosts.get(visit)) {
            return;
        }
        /*
         * Update the total cost of the current vertex.
         */
        minTotalCosts.put(visit, totalCost);
        /*
         * Follow each incoming arc of the current vertex.
         */
        for (NavArc arc : visit.copyIncoming()) {
            NavVertex nextVisit = arc.getFromVertex();
            float arcCost = arcCosts.get(arc);
            float nextTotalCost = totalCost + arcCost;
            reverseTotalCosts(nextVisit, nextTotalCost, minTotalCosts);
        }
    }

    /**
     * Enumerate all vertices reachable from a starting vertex. Note: recursive
     * depth-first traversal.
     *
     * @param visit vertex being visited, initially the starting vertex (member,
     * unaffected)
     * @param visitedVertices set of vertices previously visited, initially
     * empty (not null, updated)
     */
    private void visitReachable(NavVertex visit,
            Set<NavVertex> visitedVertices) {
        assert contains(visit) : visit;
        assert visitedVertices != null;

        if (visitedVertices.contains(visit)) {
            return;
        }
        boolean success = visitedVertices.add(visit);
        assert success;

        for (NavArc arc : visit.copyOutgoing()) {
            NavVertex vertex = arc.getToVertex();
            visitReachable(vertex, visitedVertices);
        }
    }
}

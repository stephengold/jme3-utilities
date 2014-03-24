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

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Graph for navigation. Its nodes represent reachable locations in the world.
 * Its arcs represent feasible paths between locations.
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
     * list of nodes in this graph
     */
    final protected ArrayList<NavNode> nodes = new ArrayList<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Create a new arc and add it to this graph.
     *
     * @param startNode start node (a member, distinct from endNode)
     * @param endNode end node (a member)
     * @param pathLength length or cost (arbitrary units, &gt;0)
     * @param startDirection direction at the start (unit vector in world space,
     * unaffected)
     * @return the new instance (a member)
     */
    NavArc addArc(NavNode startNode, NavNode endNode, float pathLength,
            Vector3f startDirection) {
        assert nodes.contains(startNode) : endNode;
        assert nodes.contains(endNode) : endNode;
        if (startNode == endNode) {
            throw new IllegalArgumentException("endpoints should be distinct");
        }
        Validate.positive(pathLength, "length");
        Validate.nonNull(startDirection, "start direction");
        if (!startDirection.isUnitVector()) {
            throw new IllegalArgumentException(
                    "direction should be a unit vector");
        }

        NavArc newArc = startNode.addArc(endNode, pathLength, startDirection);
        boolean success = arcs.add(newArc);
        assert success : newArc;

        return newArc;
    }

    /**
     * Find the node furthest from the specified starting point.
     *
     * @param startNode starting point (a member)
     * @return a pre-existing instance (a member)
     */
    public NavNode findFurthest(NavNode startNode) {
        validateMember(startNode);

        setAllVisited(false);
        calculateDistances(startNode, 0f);
        /*
         * Search for the maximum distance.
         */
        NavNode result = startNode;
        float maxDistance = 0f;
        for (NavNode node : nodes) {
            assert node.isVisited();
            float distance = node.getFloatValue();
            if (distance > maxDistance) {
                maxDistance = distance;
                result = node;
            }
        }

        return result;
    }

    /**
     * Add ball-and-stick representation of this navigation graph to the scene
     * graph.
     *
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param ballRadius radius of each ball (in world units, &ge;0)
     * @param ballMaterial material for geometries which represent navigation
     * nodes (not null)
     * @param stickRadius radius of each stick (in world units, &ge;0)
     * @param stickMaterial material for geometries which represent navigation
     * arcs (not null)
     */
    public void makeBallsAndSticks(Node parentNode, float ballRadius,
            float stickRadius, Material ballMaterial, Material stickMaterial) {
        Validate.nonNull(parentNode, "parent node");
        Validate.nonNegative(ballRadius, "ball radius");
        Validate.nonNull(ballMaterial, "ball material");
        Validate.nonNegative(stickRadius, "stick radius");
        Validate.nonNull(stickMaterial, "stick material");

        if (ballRadius > 0f) {
            makeBalls(parentNode, ballRadius, ballMaterial);
        }
        if (stickRadius > 0f) {
            makeSticks(parentNode, stickRadius, stickMaterial);
        }
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
     * Select a random node from this graph.
     *
     * @param generator for uniform random values (not null)
     * @return a pre-existing instance (a member)
     */
    public NavNode randomNode(Random generator) {
        Validate.nonNull(generator, "generator");

        int count = nodes.size();
        assert count >= 0 : count;
        if (count == 0) {
            return null;
        }
        int index = generator.nextInt();
        index = MyMath.modulo(index, count);
        NavNode result = nodes.get(index);

        return result;
    }

    /**
     * Remove the specified arc (but not its reverse) from this graph.
     *
     * @param arc to be removed (not null)
     */
    public void remove(NavArc arc) {
        NavNode fromNode = arc.getFromNode();
        NavNode toNode = arc.getToNode();
        fromNode.removeArcTo(toNode);
        boolean success = arcs.remove(arc);
        assert success;
    }

    /**
     * Verify that an arc belongs to this graph.
     *
     * @param arc to be validated
     */
    public void validateMember(NavArc arc) {
        if (!arcs.contains(arc)) {
            logger.log(Level.SEVERE, "arc={0}", arc);
            throw new IllegalArgumentException("graph should contain the arc");
        }
    }

    /**
     * Verify that a node belongs to this graph.
     *
     * @param node to be validated
     */
    public void validateMember(NavNode node) {
        if (!nodes.contains(node)) {
            logger.log(Level.SEVERE, "node={0}", node);
            throw new IllegalArgumentException("graph should contain the node");
        }
    }
    // *************************************************************************
    // new protected methods

    /**
     * Create a node without any arcs and add it to this graph.
     *
     * @param description (not null)
     * @param position (not null, unaffected)
     * @return the new instance
     */
    protected NavNode addNode(String description, Vector3f position) {
        Validate.nonNull(description, "description");
        Validate.nonNull(position, "position");

        NavNode newNode = new NavNode(description, position);
        boolean success = nodes.add(newNode);
        assert success;

        return newNode;
    }

    /**
     * Test whether this graph would still be connected if the specified arc
     * were removed.
     *
     * @param arc which arc to hypothetically remove (a member)
     * @return true if still connected, false if not
     */
    protected boolean isConnectedWithout(NavArc arc) {
        validateMember(arc);

        setAllVisited(false);
        NavNode fromNode = arc.getFromNode();
        NavNode toNode = arc.getToNode();
        boolean result = existsPathWithout(arc, fromNode, toNode);

        return result;
    }

    /**
     * Remove the specified arc (and its reverse, if any) from this graph.
     *
     * @param arc which arc to remove (a member)
     */
    protected void removePair(NavArc arc) {
        validateMember(arc);

        NavNode fromNode = arc.getFromNode();
        NavNode toNode = arc.getToNode();
        NavArc reverseArc = toNode.findArcTo(fromNode);

        remove(arc);
        if (reverseArc != null) {
            remove(reverseArc);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Measure the minimum distance to each node in the graph. Note: recursive!
     *
     * @param startNode starting point for measurement (a member)
     * @param startDistance distance to the starting point (&ge;0)
     */
    private void calculateDistances(NavNode startNode, float startDistance) {
        assert nodes.contains(startNode);
        assert startDistance >= 0f : startDistance;

        if (startNode.isVisited()
                && startDistance > startNode.getFloatValue()) {
            return;
        }
        /*
         * Update the node's distance.
         */
        startNode.setFloatValue(startDistance);
        startNode.setVisited(true);
        /*
         * Follow each arc from the node.
         */
        for (NavArc arc : startNode.getArcs()) {
            NavNode node = arc.getToNode();
            float nodeDistance = startDistance + arc.getPathLength();
            calculateDistances(node, nodeDistance);
        }
    }

    /**
     * Test whether there's a path between two specified nodes which avoids a
     * specified arc. Note: recursive!
     *
     * @param avoidArc which arc to avoid (a member)
     * @param fromNode starting point (a member)
     * @param toNode endpoint (a member)
     * @return true if such a path exists, false if no such path exists
     */
    private boolean existsPathWithout(NavArc avoidArc, NavNode fromNode,
            NavNode toNode) {
        assert arcs.contains(avoidArc) : avoidArc;
        assert nodes.contains(fromNode) : fromNode;
        assert nodes.contains(toNode) : toNode;

        if (fromNode == toNode) {
            return true;
        }
        if (fromNode.isVisited()) {
            return false;
        }

        fromNode.setVisited(true);

        for (NavArc arc : fromNode.getArcs()) {
            if (arc == avoidArc) {
                continue;
            }
            NavNode node = arc.getToNode();
            boolean pathExists = existsPathWithout(avoidArc, node, toNode);
            if (pathExists) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add balls to a ball-and-stick representation.
     *
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param radius radius of each ball (in world units, &gt;0)
     * @param material for geometries which represent navigation nodes (not
     * null)
     */
    private void makeBalls(Node parentNode, float radius, Material material) {
        assert material != null;
        assert radius > 0f : radius;

        int meridianSamples = 10;
        int equatorSamples = 20;
        Mesh ballMesh = new Sphere(meridianSamples, equatorSamples, radius);

        for (NavNode node : nodes) {
            Vector3f location = node.getLocation();
            Geometry ball = new Geometry("navigation node", ballMesh);
            ball.setLocalTranslation(location);
            ball.setMaterial(material);
            parentNode.attachChild(ball);
        }
    }

    /**
     * Add sticks to a ball-and-stick representation.
     *
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param radius radius of each stick (in world units, &gt;0)
     * @param material for geometries which represent navigation arcs (not null)
     */
    private void makeSticks(Node parentNode, float radius, Material material) {
        assert material != null;
        assert radius > 0f : radius;

        int lengthSamples = 2;
        int circumferenceSamples = 20;
        float length = 1f;
        Cylinder stickMesh = new Cylinder(lengthSamples, circumferenceSamples,
                radius, length);

        for (NavArc arc : arcs) {
            NavNode fromNode = arc.getFromNode();
            Vector3f from = fromNode.getLocation();
            NavNode toNode = arc.getToNode();
            Vector3f to = toNode.getLocation();
            Vector3f midpoint = from.add(to);
            midpoint.divideLocal(2f);

            Geometry stick = new Geometry("navigation arc", stickMesh);
            stick.setLocalTranslation(midpoint);
            Vector3f offset = to.subtract(from);
            float zScale = offset.length();
            Vector3f scale = new Vector3f(1f, 1f, zScale);
            Quaternion orientation = new Quaternion();
            orientation.lookAt(offset, Vector3f.UNIT_Y);

            stick.setLocalRotation(orientation);
            stick.setLocalScale(scale);
            stick.setMaterial(material);
            parentNode.attachChild(stick);
        }
    }

    /**
     * Alter the visited status of all nodes in this graph.
     *
     * @param newState true &rarr; visited, false &rarr; not visited
     */
    private void setAllVisited(boolean newState) {
        for (NavNode node : nodes) {
            node.setVisited(newState);
        }
    }
}
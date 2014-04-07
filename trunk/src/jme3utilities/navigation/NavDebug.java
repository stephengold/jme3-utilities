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
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods for visualizing navigation graphs. Aside from test cases, all
 * methods here should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class NavDebug {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(NavDebug.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private NavDebug() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add balls to a ball-and-stick representation.
     *
     * @param graph graph to represent (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param radius radius of each ball (in world units, &gt;0)
     * @param material material for geometries which represent vertices (not
     * null)
     */
    public static void makeBalls(NavGraph graph, Node parentNode, float radius,
            Material material) {
        Validate.positive(radius, "radius");
        Validate.nonNull(material, "material");

        int meridianSamples = 10;
        int equatorSamples = 20;
        Mesh ballMesh = new Sphere(meridianSamples, equatorSamples, radius);

        for (NavVertex vertex : graph.getVertices()) {
            Vector3f location = vertex.getLocation();
            Geometry ball = new Geometry("navigation vertex", ballMesh);
            ball.setLocalTranslation(location);
            ball.setMaterial(material);
            parentNode.attachChild(ball);
        }
    }

    /**
     * Add ball-and-stick representation of a specified navigation graph to a
     * specified node.
     *
     * @param graph graph to represent (not null)
     * @param parentNode where in the scene to attach geometries (not null)
     * @param ballRadius radius of each ball (in world units, &ge;0)
     * @param ballMaterial material for geometries which represent navigation
     * vertices (not null)
     * @param stickRadius radius of each stick (in world units, &ge;0)
     * @param stickMaterial material for geometries which represent navigation
     * arcs (not null)
     */
    public static void makeBallsAndSticks(NavGraph graph, Node parentNode,
            float ballRadius, float stickRadius, Material ballMaterial,
            Material stickMaterial) {
        Validate.nonNull(parentNode, "parent node");
        Validate.nonNegative(ballRadius, "ball radius");
        Validate.nonNull(ballMaterial, "ball material");
        Validate.nonNegative(stickRadius, "stick radius");
        Validate.nonNull(stickMaterial, "stick material");

        if (ballRadius > 0f) {
            makeBalls(graph, parentNode, ballRadius, ballMaterial);
        }
        if (stickRadius > 0f) {
            makeSticks(graph, parentNode, stickRadius, stickMaterial);
        }
    }

    /**
     * Add sticks to a ball-and-stick representation.
     *
     * @param graph graph to represent (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param radius radius of each stick (in world units, &gt;0)
     * @param material for geometries which represent navigation arcs (not null)
     */
    public static void makeSticks(NavGraph graph, Node parentNode, float radius,
            Material material) {
        Validate.positive(radius, "radius");
        Validate.nonNull(material, "material");

        int lengthSamples = 2;
        int circumferenceSamples = 20;
        float length = 1f;
        Cylinder stickMesh = new Cylinder(lengthSamples, circumferenceSamples,
                radius, length);

        for (NavArc arc : graph.getArcs()) {
            NavVertex fromVertex = arc.getFromVertex();
            Vector3f from = fromVertex.getLocation();
            NavVertex toVertex = arc.getToVertex();
            Vector3f to = toVertex.getLocation();
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
}
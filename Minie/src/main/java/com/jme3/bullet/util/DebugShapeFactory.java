/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.util;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TempVars;
import java.util.List;
import java.util.logging.Logger;

/**
 * A utility class to generate debug meshes for Bullet collision shapes.
 *
 * @author CJ Hare, normenhansen
 */
public class DebugShapeFactory {
    // *************************************************************************
    // constants and loggers

    /**
     * specify high-resolution debug meshes for convex shapes (256 vertices)
     */
    public static final int highResolution = 1;
    /**
     * specify low-resolution debug meshes for convex shapes (42 vertices)
     */
    public static final int lowResolution = 0;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(DebugShapeFactory.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DebugShapeFactory() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count distinct vertices in the debug mesh for the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return the number of unique vertices (&ge;0)
     */
    public static int countDebugVertices(CollisionShape shape) {
        DebugMeshCallback callback = new DebugMeshCallback();
        long id = shape.getObjectId();
        getVertices2(id, lowResolution, callback);
        int count = callback.countDistinctVertices();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Create a debug spatial from the specified collision shape.
     * <p>
     * This is mostly used internally. To attach a debug shape to a physics
     * object, call <code>attachDebugShape(AssetManager manager);</code> on it.
     *
     * @param collisionShape the shape to visualize (may be null, unaffected)
     * @return a new tree of geometries, or null
     */
    public static Spatial getDebugShape(CollisionShape collisionShape) {
        if (collisionShape == null) {
            return null;
        }

        Spatial debugShape;
        if (collisionShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape shape = (CompoundCollisionShape) collisionShape;
            List<ChildCollisionShape> children = shape.getChildren();
            Node node = new Node("DebugShapeNode");
            for (ChildCollisionShape childCollisionShape : children) {
                CollisionShape ccollisionShape = childCollisionShape.getShape();
                Geometry geometry = createDebugShape(ccollisionShape);

                // apply translation
                geometry.setLocalTranslation(
                        childCollisionShape.getLocation(null));

                // apply rotation
                TempVars vars = TempVars.get();
                Matrix3f tempRot = vars.tempMat3;

                tempRot.set(geometry.getLocalRotation());
                childCollisionShape.getRotation(null).mult(tempRot, tempRot);
                geometry.setLocalRotation(tempRot);

                vars.release();

                node.attachChild(geometry);
            }
            debugShape = node;
        } else {
            debugShape = createDebugShape(collisionShape);
        }
        if (debugShape != null) {
            debugShape.updateGeometricState();
        }

        return debugShape;
    }

    /**
     * Calculate the scaled volume of the debug mesh for the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return the volume (in physics-space units cubed, &ge;0)
     */
    public static float volumeConvex(CollisionShape shape) {
        DebugMeshCallback callback = new DebugMeshCallback();
        long id = shape.getObjectId();
        getVertices2(id, lowResolution, callback);
        float volume = callback.volumeConvex();

        assert volume >= 0f : volume;
        return volume;
    }
    // *************************************************************************
    // private methods

    /**
     * Create a geometry for visualizing the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return a new geometry (not null)
     */
    private static Geometry createDebugShape(CollisionShape shape) {
        Mesh mesh = getDebugMesh(shape);
        Geometry geom = new Geometry("debug shape", mesh);
        geom.updateModelBound();

        return geom;
    }

    /**
     * Create a mesh for visualizing the specified shape.
     *
     * @param shape (not null, unaffected)
     * @return a new mesh (not null)
     */
    public static Mesh getDebugMesh(CollisionShape shape) {
        DebugMeshCallback callback = new DebugMeshCallback();
        long id = shape.getObjectId();
        getVertices2(id, lowResolution, callback);

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, callback.getVertices());

        if (shape.generatesDebugNormals()) {
            mesh.setBuffer(Type.Normal, 3, callback.getFaceNormals());
        }

        return mesh;
    }

    private static native void getVertices2(long shapeId, int resolution,
            DebugMeshCallback buffer);
}

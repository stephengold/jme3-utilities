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

import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Temporary objects used to return debug meshes from native Bullet.
 *
 * @author normenhansen
 */
class DebugMeshCallback {
    // *************************************************************************
    // constants and loggers

    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(DebugMeshCallback.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of vertex locations
     */
    final private ArrayList<Vector3f> list = new ArrayList<>(250);
    // *************************************************************************
    // new methods exposed

    /**
     * Count the number of vertices.
     *
     * @return the count (&ge;0)
     */
    int countVertices() {
        int count = list.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Calculate face normals and store them in a FloatBuffer.
     *
     * @return a new buffer (not null)
     */
    FloatBuffer getFaceNormals() {
        int numVertices = countVertices();
        int numTriangles = numVertices / 3;
        assert numTriangles * vpt == numVertices : numVertices;

        int numFloats = 3 * numVertices;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);

        Triangle triangle = new Triangle();
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int firstVertex = vpt * triIndex;
            Vector3f pos1 = list.get(firstVertex);
            Vector3f pos2 = list.get(firstVertex + 1);
            Vector3f pos3 = list.get(firstVertex + 2);
            triangle.set(pos1, pos2, pos3);
            triangle.setNormal(null);
            Vector3f normal = triangle.getNormal();
            for (int j = 0; j < vpt; j++) {
                buffer.put(normal.x);
                buffer.put(normal.y);
                buffer.put(normal.z);
            }
        }

        return buffer;
    }

    /**
     * Copy the vertex locations to a FloatBuffer.
     *
     * @return a new buffer (not null)
     */
    FloatBuffer getVertices() {
        int numFloats = 3 * countVertices();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(numFloats);
        for (Vector3f location : list) {
            buffer.put(location.x);
            buffer.put(location.y);
            buffer.put(location.z);
        }

        return buffer;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a vertex to the mesh under construction.
     * <p>
     * This method is invoked from native code.
     *
     * @param x local X coordinate of new vertex
     * @param y local Y coordinate of new vertex
     * @param z local Z coordinate of new vertex
     * @param part ignored
     * @param index ignored
     */
    private void addVector(float x, float y, float z, int part, int index) {
        list.add(new Vector3f(x, y, z));
    }
}

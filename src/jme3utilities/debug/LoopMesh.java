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
package jme3utilities.debug;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom mesh for a circle or regular polygon in the XZ plane, with radius=1,
 * centered at the origin.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class LoopMesh
        extends Mesh {
    // *************************************************************************
    // constants

    /**
     * number of vertices per edge
     */
    final private static int vpe = 2;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(LoopMesh.class.getName());
    // *************************************************************************
    // fields
    /**
     * total number of vertices (&ge;3)
     */
    protected int vertexCount;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not use!
     */
    public LoopMesh() {
    }

    /**
     * Instantiate a loop with a specified number of vertices.
     *
     * @param vertexCount (&ge;3)
     */
    public LoopMesh(int vertexCount) {
        if (vertexCount < 3) {
            logger.log(Level.SEVERE, "vertexCount={0}", vertexCount);
            throw new IllegalArgumentException(
                    "should have at least 3 vertices");
        }
        this.vertexCount = vertexCount;

        setMode(Mode.Lines);
        updateAll();
        setStatic();
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);
        vertexCount = capsule.readInt("vertexCount", 60);
    }

    /**
     * Serialize this instance when saving.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(vertexCount, "vertexCount", 60);
    }
    // *************************************************************************
    // private methods

    /**
     * Rebuild this mesh after a parameter change.
     */
    private void updateAll() {
        /*
         * Update each buffer.
         */
        updateCoordinates();
        updateIndices();
        /*
         * Update the bounds of the mesh.
         */
        updateBound();
    }

    /**
     * Update the buffered locations and texture coordinates of each vertex in
     * this mesh.
     */
    private void updateCoordinates() {
        /*
         * Allocate an array to hold the local (XYZ) coordinates.
         */
        Vector3f[] locationArray = new Vector3f[vertexCount];
        /*
         * Compute the non-polar vertices 1st. Vertices are arranged 1st
         * by latitude (starting from the rim).
         */
        float increment = FastMath.TWO_PI / vertexCount;
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            float longitude = increment * vertexIndex;
            float x = FastMath.cos(longitude);
            float z = FastMath.sin(longitude);

            logger.log(Level.FINE, "coords {0}", vertexIndex);
            Vector3f location = new Vector3f(x, 0f, z);
            locationArray[vertexIndex] = location;
        }
        /*
         * Allocate and assign a buffer.
         */
        FloatBuffer locBuffer = BufferUtils.createFloatBuffer(locationArray);
        setBuffer(VertexBuffer.Type.Position, 3, locBuffer);
    }

    /**
     * Update the buffered indices of each edge in this mesh.
     */
    private void updateIndices() {
        /*
         * Allocate an array to hold the two vertex indices of each edge.
         */
        short[] indexArray = new short[vpe * vertexCount];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int nextVertex = (vertexIndex + 1) % vertexCount;
            logger.log(Level.FINE, "index {0}", vertexIndex);
            indexArray[vpe * vertexIndex] = (short) vertexIndex;
            indexArray[vpe * vertexIndex + 1] = (short) nextVertex;
        }
        /*
         * Allocate and assign a buffer for indices.
         */
        ShortBuffer indexBuffer = BufferUtils.createShortBuffer(indexArray);
        setBuffer(VertexBuffer.Type.Index, vpe, indexBuffer);
    }
}
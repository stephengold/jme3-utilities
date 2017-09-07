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
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.mesh;

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
import jme3utilities.Validate;

/**
 * A 3D, static, lineloop-mode mesh which renders a circle or polygon.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoopMesh extends Mesh {
    // *************************************************************************
    // constants

    /**
     * number of vertices per edge
     */
    final private static int vpe = 2;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoopMesh.class.getName());
    // *************************************************************************
    // fields

    /**
     * total number of vertices (&ge;3)
     */
    private int vertexCount;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public LoopMesh() {
    }

    /**
     * Instantiate a regular polygon (or circle) in the XZ plane, centered at
     * the local origin, with radius=1 and the specified number of vertices.
     *
     * @param vertexCount (&ge;3)
     */
    public LoopMesh(int vertexCount) {
        Validate.inRange(vertexCount, "vertex count", 3, Integer.MAX_VALUE);

        this.vertexCount = vertexCount;
        setMode(Mode.LineLoop);
        updateCoordinates();

        updateIndices();
        updateBound();
        setStatic();
    }

    /**
     * Instantiate a polygon from an array of coordinates.
     *
     * @param cornerArray local coordinates of the corners, in sequence (not
     * null or containing any nulls, length&ge;3, unaffected)
     */
    public LoopMesh(Vector3f[] cornerArray) {
        Validate.nonNull(cornerArray, "corner list");
        vertexCount = cornerArray.length;
        Validate.inRange(vertexCount, "length of corner list",
                3, Integer.MAX_VALUE);
        for (int index = 0; index < vertexCount; index++) {
            String description = String.format("cornerArray[%d]", index);
            Validate.nonNull(cornerArray[index], description);
        }

        setMode(Mode.LineStrip);
        Vector3f[] locationArray = new Vector3f[vertexCount];
        System.arraycopy(cornerArray, 0, locationArray, 0, vertexCount);
        /*
         * Allocate and assign a buffer.
         */
        FloatBuffer locBuffer = BufferUtils.createFloatBuffer(locationArray);
        setBuffer(VertexBuffer.Type.Position, 3, locBuffer);

        updateIndices();
        updateBound();
        setStatic();
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);
        vertexCount = capsule.readInt("vertexCount", 60);
    }

    /**
     * Serialize this instance when saving.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(vertexCount, "vertexCount", 60);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the buffered locations for a regular polygon (or circle) in the XZ
     * plane, centered at the origin, with radius=1 and the specified number of
     * vertices.
     */
    private void updateCoordinates() {
        /*
         * Allocate an array to hold the local (XYZ) coordinates.
         */
        Vector3f[] locationArray = new Vector3f[vertexCount];

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
     * Update the buffered indices for a new vertex count.
     */
    private void updateIndices() {
        /*
         * Allocate an array to hold the vertex indices.
         */
        short[] indexArray = new short[vertexCount + 1];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            indexArray[vertexIndex] = (short) vertexIndex;
        }
        /*
         * Allocate and assign a buffer for indices.
         */
        ShortBuffer indexBuffer = BufferUtils.createShortBuffer(indexArray);
        setBuffer(VertexBuffer.Type.Index, vpe, indexBuffer);
    }
}

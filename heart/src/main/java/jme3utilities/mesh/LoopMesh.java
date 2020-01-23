/*
 Copyright (c) 2014-2020, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A static, LineLoop-mode mesh (without indices) that renders a circle or
 * polygon.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoopMesh extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LoopMesh.class.getName());
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public LoopMesh() {
    }

    /**
     * Instantiate a regular polygon (or circle) with radius=1, in the X-Z
     * plane.
     * <p>
     * The center is at (0,0,0).
     *
     * @param vertexCount the desired number of vertices (&ge;3)
     */
    public LoopMesh(int vertexCount) {
        this(vertexCount, 1f);
    }

    /**
     * Instantiate a regular polygon (or circle) in the X-Z plane.
     * <p>
     * The center is at (0,0,0).
     *
     * @param vertexCount the desired number of vertices (&ge;3)
     * @param radius the desired radius (in mesh units, &ge;0)
     */
    public LoopMesh(int vertexCount, float radius) {
        Validate.inRange(vertexCount, "vertex count", 3, Integer.MAX_VALUE);
        Validate.nonNegative(radius, "radius");

        setMode(Mode.LineLoop);
        updateCoordinates(vertexCount, radius);
        updateBound();
        setStatic();
    }

    /**
     * Instantiate a 3-D polygon from an array of coordinates.
     *
     * @param cornerArray the desired mesh coordinates of the corners, in
     * sequence (not null or containing any nulls, length&ge;3, unaffected)
     */
    public LoopMesh(Vector3f[] cornerArray) {
        Validate.nonNullArray(cornerArray, "corner array");
        int vertexCount = cornerArray.length;
        Validate.inRange(vertexCount, "length of corner array",
                3, Integer.MAX_VALUE);

        setMode(Mode.LineLoop);
        /*
         * Allocate, fill, and flip the position buffer.
         */
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(cornerArray);

        setBuffer(VertexBuffer.Type.Position, numAxes, positionBuffer);
        updateBound();
        setStatic();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the buffered locations for a regular polygon (or circle) in the
     * X-Z plane, centered at (0,0,0).
     *
     * @param vertexCount (&ge;3)
     * @param radius (in mesh units, &ge;0)
     */
    private void updateCoordinates(int vertexCount, float radius) {
        int numFloats = numAxes * vertexCount;
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(numFloats);

        float increment = FastMath.TWO_PI / vertexCount;
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            float longitude = increment * vertexIndex;
            float x = radius * FastMath.cos(longitude);
            float z = radius * FastMath.sin(longitude);
            positionBuffer.put(x).put(0f).put(z);
        }

        positionBuffer.flip();
        setBuffer(VertexBuffer.Type.Position, numAxes, positionBuffer);
    }
}

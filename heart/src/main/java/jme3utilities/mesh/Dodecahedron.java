/*
 Copyright (c) 2020, Stephen Gold
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
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * A 3-D, static mesh (with indices but without normals or texture coordinates)
 * that renders a regular dodecahedron. (A regular dodecahedron has 12
 * pentagonal faces.)
 *
 * @author Stephen Gold sgold@sonic.net
 * @see jme3utilities.mesh.Icosahedron
 */
public class Dodecahedron extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * golden ratio = 1.618...
     */
    final public static float phi = (1f + FastMath.sqrt(5f)) / 2f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Dodecahedron.class.getName());
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected Dodecahedron() {
    }

    /**
     * Instantiate a regular dodecahedron with the specified radius.
     *
     * The center is at (0,0,0). The first and last faces lie parallel to the
     * X-Y plane. If mode=Triangles, all triangles face outward.
     *
     * @param radius the desired distance of the vertices from the center (in
     * mesh units, &gt;0)
     * @param mode the desired mode of the Mesh (Lines or Points or Triangles)
     */
    public Dodecahedron(float radius, Mode mode) {
        Validate.positive(radius, "radius");
        Validate.nonNull(mode, "mode");

        float denom = MyMath.hypotenuse(2f, phi + 1f) / radius;

        float za = (phi + 1f) / denom;
        float xa0 = 2f / denom;
        float xa1 = xa0 * FastMath.cos(0.4f * FastMath.PI);
        float xa2 = xa0 * FastMath.cos(0.8f * FastMath.PI);
        float xa3 = xa0 * FastMath.cos(1.2f * FastMath.PI);
        float xa4 = xa0 * FastMath.cos(1.6f * FastMath.PI);
        float ya1 = xa0 * FastMath.sin(0.4f * FastMath.PI);
        float ya2 = xa0 * FastMath.sin(0.8f * FastMath.PI);
        float ya3 = xa0 * FastMath.sin(1.2f * FastMath.PI);
        float ya4 = xa0 * FastMath.sin(1.6f * FastMath.PI);

        float zb = (phi - 1f) / denom;
        float xb0 = (2f * phi) / denom;
        float xb1 = phi * xa1;
        float xb2 = phi * xa2;
        float xb3 = phi * xa3;
        float xb4 = phi * xa4;
        float yb1 = phi * ya1;
        float yb2 = phi * ya2;
        float yb3 = phi * ya3;
        float yb4 = phi * ya4;

        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(
                xa0, 0f, za,
                xa1, ya1, za,
                xa2, ya2, za,
                xa3, ya3, za,
                xa4, ya4, za,
                xb0, 0f, zb,
                xb1, yb1, zb,
                xb2, yb2, zb,
                xb3, yb3, zb,
                xb4, yb4, zb,
                -xb0, 0f, -zb,
                -xb1, -yb1, -zb,
                -xb2, -yb2, -zb,
                -xb3, -yb3, -zb,
                -xb4, -yb4, -zb,
                -xa0, 0f, -za,
                -xa1, -ya1, -za,
                -xa2, -ya2, -za,
                -xa3, -ya3, -za,
                -xa4, -ya4, -za
        );
        setBuffer(VertexBuffer.Type.Position, MyVector3f.numAxes,
                positionBuffer);
        int numFloats = positionBuffer.capacity();
        assert numFloats == 20 * MyVector3f.numAxes;
        positionBuffer.limit(numFloats);

        IntBuffer indexBuffer;
        if (mode == Mode.Lines) {
            indexBuffer = BufferUtils.createIntBuffer(
                    0, 1, 1, 2, 2, 3, 3, 4, 4, 0,
                    0, 5, 1, 6, 2, 7, 3, 8, 4, 9,
                    5, 13, 6, 14, 7, 10, 8, 11, 9, 12,
                    5, 12, 6, 13, 7, 14, 8, 10, 9, 11,
                    10, 15, 11, 16, 12, 17, 13, 18, 14, 19,
                    15, 16, 16, 17, 17, 18, 18, 19, 19, 15
            );
            int numInts = indexBuffer.capacity();
            indexBuffer.limit(numInts);
            setBuffer(VertexBuffer.Type.Index, MyMesh.vpe, indexBuffer);

        } else if (mode == Mode.Triangles) {
            indexBuffer = BufferUtils.createIntBuffer(
                    0, 1, 2, 0, 2, 3, 0, 3, 4,
                    0, 5, 13, 0, 13, 6, 0, 6, 1,
                    1, 6, 14, 1, 14, 7, 1, 7, 2,
                    2, 7, 10, 2, 10, 8, 2, 8, 3,
                    3, 8, 11, 3, 11, 9, 3, 9, 4,
                    4, 9, 12, 4, 12, 5, 4, 5, 0,
                    5, 12, 17, 5, 17, 18, 5, 18, 13,
                    6, 13, 18, 6, 18, 19, 6, 19, 14,
                    7, 14, 19, 7, 19, 15, 7, 15, 10,
                    8, 10, 15, 8, 15, 16, 8, 16, 11,
                    9, 11, 16, 9, 16, 17, 9, 17, 12,
                    15, 19, 18, 15, 18, 17, 15, 17, 16
            );
            int numInts = indexBuffer.capacity();
            indexBuffer.limit(numInts);
            setBuffer(VertexBuffer.Type.Index, MyMesh.vpt, indexBuffer);

        } else if (mode != Mode.Points) {
            String message = "mode = " + mode;
            throw new IllegalArgumentException(message);
        }

        setMode(mode);
        updateBound();
        setStatic();
    }
}

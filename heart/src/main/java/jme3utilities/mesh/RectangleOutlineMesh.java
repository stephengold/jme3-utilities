/*
 Copyright (c) 2017-2020, Stephen Gold
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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.math.MyVector3f;

/**
 * A 2-D, static, LineLoop-mode mesh that renders the outline of a rectangle in
 * the X-Y plane.
 * <p>
 * In local space, the rectangle extends from (x1,y1,0) to (x2,y2,0).
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RectangleOutlineMesh extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RectangleOutlineMesh.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an axis-aligned unit square with its first vertex at the mesh
     * origin.
     */
    public RectangleOutlineMesh() {
        this(0f, 1f, 0f, 1f);
    }

    /**
     * Instantiate an axis-aligned rectangle.
     *
     * @param x1 local X coordinate of the first and 2nd vertices
     * @param x2 local X coordinate of the 3rd and 4th vertices
     * @param y1 local Y coordinate of the first and 4th vertices
     * @param y2 local Y coordinate of the 2nd and 3rd vertices
     */
    public RectangleOutlineMesh(float x1, float x2, float y1, float y2) {
        setMode(Mode.LineLoop);

        setBuffer(VertexBuffer.Type.Position, MyVector3f.numAxes, new float[]{
            x1, y1, 0f,
            x1, y2, 0f,
            x2, y2, 0f,
            x2, y1, 0f});

        setBuffer(VertexBuffer.Type.Index, MyMesh.vpe, new short[]{0, 1, 2, 3});

        updateBound();
        setStatic();
    }
}

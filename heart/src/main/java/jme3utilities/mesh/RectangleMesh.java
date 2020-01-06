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

import com.jme3.font.Rectangle;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A 2-D, static, TriangleFan-mode mesh that renders an axis-aligned rectangle
 * in the X-Y plane.
 * <p>
 * In local space, the rectangle extends from (x1,y1,0) to (x2,y2,0) with
 * normals set to (0,0,zNorm). In texture space, it extends extends from (s1,t1)
 * to (s2, t2).
 * <p>
 * The key differences between this class and com.jme3.scene.shape.Quad are:<ol>
 * <li> the center and extent can be configured independently,
 * <li> TriangleFan mode is used to reduce the number of indices from 6 to 4,
 * <li> the normal direction is configurable, and
 * <li> the texture coordinates can be configured in greater detail.</ol>
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RectangleMesh extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RectangleMesh.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an axis-aligned unit square with right-handed normals and its
     * first vertex at the mesh origin.
     */
    public RectangleMesh() {
        this(0f, 1f, 0f, 1f, 1f);
    }

    /**
     * Instantiate an axis-aligned rectangle with default texture coordinates.
     *
     * @param rect the location of the first vertex and the dimensions of the
     * rectangle (not null, Z components are ignored, unaffected)
     * @param zNorm the Z component of the normal vector (must be +1 or -1)
     */
    public RectangleMesh(Rectangle rect, float zNorm) {
        this(0f, 1f, 0f, 1f,
                rect.x, rect.y,
                rect.x + rect.width, rect.y + rect.height,
                zNorm);
    }

    /**
     * Instantiate an axis-aligned rectangle with default texture coordinates.
     *
     * @param x1 the local X coordinate of the first and 2nd vertices
     * @param x2 the local X coordinate of the 3rd and 4th vertices
     * @param y1 the local Y coordinate of the first and 4th vertices
     * @param y2 the local Y coordinate of the 2nd and 3rd vertices
     * @param zNorm the Z component of the normal vector (must be +1 or -1)
     */
    public RectangleMesh(float x1, float x2, float y1, float y2, float zNorm) {
        this(0f, 1f, 0f, 1f, x1, x2, y1, y2, zNorm);
    }

    /**
     * Instantiate an axis-aligned rectangle with the specified parameters.
     *
     * @param s1 the first (U) texture coordinate of the first and 2nd vertices
     * @param s2 the first (U) texture coordinate of the 3rd and 4th vertices
     * @param t1 the 2nd (V) texture coordinate of the first and 4th vertices
     * @param t2 the 2nd (V) texture coordinate of the 2nd and 3rd vertices
     * @param x1 the local X coordinate of the first and 2nd vertices
     * @param x2 the local X coordinate of the 3rd and 4th vertices
     * @param y1 the local Y coordinate of the first and 4th vertices
     * @param y2 the local Y coordinate of the 2nd and 3rd vertices
     * @param zNorm the Z component of the normal vector (must be +1 or -1)
     */
    public RectangleMesh(float s1, float s2, float t1, float t2,
            float x1, float x2, float y1, float y2, float zNorm) {
        if (zNorm != -1f && zNorm != 1f) {
            logger.log(Level.SEVERE, "zNorm={0}", zNorm);
            throw new IllegalArgumentException("zNorm must be +1 or -1.");
        }

        setMode(Mode.TriangleFan);

        setBuffer(Type.Position, 3, new float[]{
            x1, y1, 0f,
            x1, y2, 0f,
            x2, y2, 0f,
            x2, y1, 0f});

        setBuffer(Type.TexCoord, 2, new float[]{
            s1, t1,
            s1, t2,
            s2, t2,
            s2, t1});

        setBuffer(Type.Normal, 3, new float[]{
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm,
            0f, 0f, zNorm});

        if ((x2 - x1) * (y2 - y1) * zNorm > 0f) {
            setBuffer(Type.Index, vpt, new short[]{0, 3, 2, 1});
        } else {
            setBuffer(Type.Index, vpt, new short[]{0, 1, 2, 3});
        }

        updateBound();
        setStatic();
    }
}

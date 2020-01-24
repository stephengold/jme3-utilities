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
package jme3utilities.debug;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.debug.WireSphere;
import com.jme3.scene.shape.Sphere;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.mesh.Icosphere;
import jme3utilities.mesh.LoopMesh;

/**
 * Enumerate mesh options for visualizing spheres.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum SphereMeshes {
    // *************************************************************************
    // values

    /**
     * Icosphere with numRefineSteps=1 (42 vertices)
     */
    Icosphere,
    /**
     * LoopMesh with 32 vertices (wants Y-axis billboarding)
     */
    LoopMesh,
    /**
     * com.jme3.scene.shape.Sphere with 172 vertices
     */
    PoleSphere,
    /**
     * com.jme3.scene.debug.WireSphere (360 vertices)
     */
    WireSphere;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the specified Mesh has the corresponding type.
     *
     * @param mesh (unaffected)
     * @return true if the correct type, otherwise false
     */
    public boolean isInstance(Mesh mesh) {
        boolean result;
        switch (this) {
            case Icosphere:
                result = mesh instanceof Icosphere;
                break;

            case LoopMesh:
                result = mesh instanceof LoopMesh;
                break;

            case PoleSphere:
                result = mesh instanceof Sphere;
                break;

            case WireSphere:
                result = mesh instanceof WireSphere;
                break;

            default:
                String message = "enum value = " + this;
                throw new IllegalStateException(message);
        }

        return result;
    }

    /**
     * Instantiate a new Mesh.
     *
     * @param radius the desired radius (in mesh units, &gt;0)
     * @param wantNormals true to include normals, false to exclude them
     * @param wantUVs true to include texture coordinates, false to exclude them
     * @return a new Mesh of the corresponding type
     */
    public Mesh makeSphere(float radius, boolean wantNormals, boolean wantUVs) {
        Validate.positive(radius, "radius");

        Mesh result;
        switch (this) {
            case Icosphere:
                int numRefineSteps = 1;
                result = new Icosphere(numRefineSteps, radius);
                break;

            case LoopMesh:
                int numVertices = 32;
                result = new LoopMesh(numVertices, radius);
                break;

            case PoleSphere:
                int zSamples = 12;
                int radialSamples = 16;
                result = new Sphere(zSamples, radialSamples, radius);
                break;

            case WireSphere:
                result = new WireSphere(radius);
                break;

            default:
                String message = "enum value = " + this;
                throw new IllegalStateException(message);
        }

        boolean hasNormals = MyMesh.hasNormals(result);
        if (hasNormals && !wantNormals) {
            result.clearBuffer(VertexBuffer.Type.Normal);
        } else if (wantNormals && !hasNormals) {
            MyMesh.addSphereNormals(result);
        }

        boolean hasUVs = MyMesh.hasUV(result);
        if (hasUVs && !wantUVs) {
            result.clearBuffer(VertexBuffer.Type.TexCoord);
        } else if (wantUVs && !hasUVs) {
            String message = "Texture coordinates not available for " + this;
            throw new IllegalArgumentException(message);
        }

        assert isInstance(result);
        return result;
    }
}

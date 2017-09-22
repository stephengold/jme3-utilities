/*
 Copyright (c) 2017, Stephen Gold
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
package jme3utilities;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.IntMap;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

/**
 * Utility methods for meshes and mesh vertices. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyMesh {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyMesh.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyMesh() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether a mesh has texture (U-V) coordinates.
     *
     * @param mesh mesh to test (not null)
     * @return true if the mesh has texture coordinates, otherwise false
     */
    public static boolean hasUV(Mesh mesh) {
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        int key = VertexBuffer.Type.TexCoord.ordinal();
        boolean result = buffers.containsKey(key);

        return result;
    }

    /**
     * Find the largest weight in the specified mesh for the specified bone.
     *
     * @param mesh which mesh (not null, possibly modified)
     * @param boneIndex which bone (&ge;0)
     * @return bone weight, or 0f if no influence found
     */
    public static float maxWeight(Mesh mesh, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        float result = 0f;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                int bIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert
                        && bIndex == boneIndex
                        && weight > result) {
                    result = weight;
                }
            }
        }

        return result;
    }

    /**
     * Count how many vertices in the specified mesh are directly influenced by
     * the indexed bone.
     *
     * @param mesh which mesh (not null, possibly modified)
     * @param boneIndex which bone (&ge;0)
     * @return count (&ge;0)
     */
    public static int numInfluenced(Mesh mesh, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        int result = 0;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                int bIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert
                        && bIndex == boneIndex
                        && weight > 0f) {
                    result++;
                }
            }
        }

        return result;
    }

    /**
     * Read an index from a buffer.
     *
     * @param buffer a buffer of bytes or shorts (not null)
     * @return index (&ge;0)
     */
    public static int readIndex(Buffer buffer) {
        int result;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) buffer;
            byte b = byteBuffer.get();
            result = 0xff & b;
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer shortBuffer = (ShortBuffer) buffer;
            short s = shortBuffer.get();
            result = 0xffff & s;
        } else {
            throw new IllegalArgumentException();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Calculate the location of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return mesh coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexLocation(Mesh mesh, int vertexIndex,
            Matrix4f[] skinningMatrices, Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        if (mesh.isAnimated()) {
            assert skinningMatrices.length != 0;
            Vector3f b = vertexVector3f(mesh,
                    VertexBuffer.Type.BindPosePosition, vertexIndex, null);

            VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
            weightBuffer.position(4 * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(4 * vertexIndex);

            storeResult.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s = skinningMatrices[boneIndex];
                    storeResult.x += weight
                            * (s.m00 * b.x + s.m01 * b.y + s.m02 * b.z + s.m03);
                    storeResult.y += weight
                            * (s.m10 * b.x + s.m11 * b.y + s.m12 * b.z + s.m13);
                    storeResult.z += weight
                            * (s.m20 * b.x + s.m21 * b.y + s.m22 * b.z + s.m23);
                }
            }

        } else { // not an animated mesh
            vertexVector3f(mesh, VertexBuffer.Type.Position, vertexIndex,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Read a data vector for the indexed vertex in a mesh.
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (6 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return mesh coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexVector3f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        assert bufferType == VertexBuffer.Type.BindPoseNormal
                || bufferType == VertexBuffer.Type.BindPosePosition
                || bufferType == VertexBuffer.Type.BindPoseTangent
                || bufferType == VertexBuffer.Type.Normal
                || bufferType == VertexBuffer.Type.Position
                || bufferType == VertexBuffer.Type.Tangent : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(3 * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();
        storeResult.z = floatBuffer.get();

        return storeResult;
    }

    /**
     * Calculate the location of the indexed vertex in world space using the
     * skinning matrices provided.
     *
     * @param geometry geometry containing the subject mesh (not null)
     * @param vertexIndex index into the geometry's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new instance)
     */
    public static Vector3f vertexWorldLocation(Geometry geometry,
            int vertexIndex, Matrix4f[] skinningMatrices,
            Vector3f storeResult) {
        Validate.nonNull(geometry, "geometry");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Mesh mesh = geometry.getMesh();
        Vector3f meshLocation
                = vertexLocation(mesh, vertexIndex, skinningMatrices, null);
        if (geometry.isIgnoreTransform()) { // TODO JME 3.2
            storeResult.set(meshLocation);
        } else {
            geometry.localToWorld(meshLocation, storeResult);
        }

        return storeResult;
    }
}

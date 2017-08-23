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
package jme3utilities;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.IntMap;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * Utility methods for analyzing meshes. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyMesh {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyMesh.class.getName());
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
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getData();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getData();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        float result = 0f;
        byte biByte = (byte) boneIndex;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                byte bIndex = boneIndexBuffer.get();
                if (wIndex < maxWeightsPerVert
                        && bIndex == biByte
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
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getData();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getData();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        int result = 0;
        byte biByte = (byte) boneIndex;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                byte bIndex = boneIndexBuffer.get();
                if (wIndex < maxWeightsPerVert
                        && bIndex == biByte
                        && weight > 0f) {
                    result++;
                }
            }
        }

        return result;
    }
}

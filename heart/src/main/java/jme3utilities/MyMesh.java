/*
 Copyright (c) 2017-2018, Stephen Gold
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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods for meshes and mesh vertices.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyMesh {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of bones that can influence any one vertex
     */
    final private static int maxWeights = 4;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyMesh.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix4f#IDENTITY}
     */
    final private static Matrix4f matrixIdentity = new Matrix4f();
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
     * Estimate the number of bones in the specified mesh by reading its
     * bone-index buffers.
     *
     * @param mesh mesh to examine (not null)
     * @return estimated number of bones (&ge;0)
     */
    public static int countBones(Mesh mesh) {
        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= maxWeights : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % maxWeights == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / maxWeights;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * maxWeights : numWeights;

        int result = 0;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < maxWeights; wIndex++) {
                float weight = weightBuffer.get();
                int bIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert && weight > 0f
                        && bIndex >= result) {
                    result = bIndex + 1;
                }
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Test whether a mesh has texture (U-V) coordinates.
     *
     * @param mesh mesh to test (not null)
     * @return true if the mesh has texture coordinates, otherwise false
     */
    public static boolean hasUV(Mesh mesh) {
        VertexBuffer buffer = mesh.getBuffer(VertexBuffer.Type.TexCoord);
        if (buffer == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified mesh is animated. Unlike mesh.isAnimated()
     * this method checks for bone weights and ignores HW buffers.
     *
     * @param mesh which mesh to test (not null, unaffected)
     * @return true if animated, otherwise false
     */
    public static boolean isAnimated(Mesh mesh) {
        VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        boolean hasIndices = indices != null;

        VertexBuffer weights = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        boolean hasWeights = weights != null;

        boolean result = hasIndices && hasWeights;
        return result;
    }

    /**
     * Enumerate all meshes in the specified subtree of a scene graph. Note:
     * recursive!
     *
     * @param subtree (may be null, aliases created)
     * @param storeResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Mesh> listMeshes(Spatial subtree,
            List<Mesh> storeResult) {
        if (storeResult == null) {
            storeResult = new ArrayList<>(10);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (!storeResult.contains(mesh)) {
                storeResult.add(mesh);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listMeshes(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Find the largest weight in the specified mesh for the indexed bone.
     *
     * @param mesh which mesh (not null, possibly modified)
     * @param boneIndex which bone (&ge;0)
     * @return bone weight, or 0f if no influence found
     */
    public static float maxWeight(Mesh mesh, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= maxWeights : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % maxWeights == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / maxWeights;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * maxWeights : numWeights;

        float result = 0f;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < maxWeights; wIndex++) {
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
        assert maxWeightsPerVert <= maxWeights : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % maxWeights == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / maxWeights;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * maxWeights : numWeights;

        int result = 0;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < maxWeights; wIndex++) {
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
     * Copy the bone indices for the indexed vertex.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static int[] vertexBoneIndices(Mesh mesh,
            int vertexIndex, int[] storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new int[maxWeights];
        } else {
            assert storeResult.length >= maxWeights : storeResult.length;
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.position(maxWeights * vertexIndex);
        for (int wIndex = 0; wIndex < maxWeightsPerVert; wIndex++) {
            int boneIndex = readIndex(boneIndexBuffer);
            storeResult[wIndex] = boneIndex;
        }
        /*
         * Fill with -1s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; wIndex++) {
            storeResult[wIndex] = -1;
        }

        return storeResult;
    }

    /**
     * Copy the bone weights for the indexed vertex.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static float[] vertexBoneWeights(Mesh mesh,
            int vertexIndex, float[] storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new float[maxWeights];
        } else {
            assert storeResult.length >= maxWeights : storeResult.length;
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.position(maxWeights * vertexIndex);
        for (int wIndex = 0; wIndex < maxWeightsPerVert; wIndex++) {
            storeResult[wIndex] = weightBuffer.get();
        }
        /*
         * Fill with 0s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; wIndex++) {
            storeResult[wIndex] = 0f;
        }

        return storeResult;
    }

    /**
     * Copy the color of the indexed vertex from the color buffer.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the color (either storeResult or a new instance)
     */
    public static ColorRGBA vertexColor(Mesh mesh, int vertexIndex,
            ColorRGBA storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(VertexBuffer.Type.Color);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(4 * vertexIndex);
        storeResult.r = floatBuffer.get();
        storeResult.g = floatBuffer.get();
        storeResult.b = floatBuffer.get();
        storeResult.a = floatBuffer.get();

        return storeResult;
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

        if (isAnimated(mesh)) {
            Vector3f b = vertexVector3f(mesh,
                    VertexBuffer.Type.BindPosePosition, vertexIndex, null);

            VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            storeResult.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s;
                    if (boneIndex < skinningMatrices.length) {
                        s = skinningMatrices[boneIndex];
                    } else {
                        s = matrixIdentity;
                    }
                    float xOf = s.m00 * b.x + s.m01 * b.y + s.m02 * b.z + s.m03;
                    float yOf = s.m10 * b.x + s.m11 * b.y + s.m12 * b.z + s.m13;
                    float zOf = s.m20 * b.x + s.m21 * b.y + s.m22 * b.z + s.m23;
                    storeResult.x += weight * xOf;
                    storeResult.y += weight * yOf;
                    storeResult.z += weight * zOf;
                }
            }

        } else { // not an animated mesh
            vertexVector3f(mesh, VertexBuffer.Type.Position, vertexIndex,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Calculate the normal of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return a unit vector in mesh space (either storeResult or a new
     * instance)
     */
    public static Vector3f vertexNormal(Mesh mesh, int vertexIndex,
            Matrix4f[] skinningMatrices, Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        if (isAnimated(mesh)) {
            Vector3f b = vertexVector3f(mesh, VertexBuffer.Type.BindPoseNormal,
                    vertexIndex, null);

            VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            result.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s;
                    if (boneIndex < skinningMatrices.length) {
                        s = skinningMatrices[boneIndex];
                    } else {
                        s = matrixIdentity;
                    }
                    float xOf = s.m00 * b.x + s.m01 * b.y + s.m02 * b.z;
                    float yOf = s.m10 * b.x + s.m11 * b.y + s.m12 * b.z;
                    float zOf = s.m20 * b.x + s.m21 * b.y + s.m22 * b.z;
                    result.x += weight * xOf;
                    result.y += weight * yOf;
                    result.z += weight * zOf;
                }
            }
            result.normalizeLocal();

        } else { // not an animated mesh
            vertexVector3f(mesh, VertexBuffer.Type.Normal, vertexIndex, result);
        }

        return result;
    }

    /**
     * Read the size of the indexed vertex from the size buffer.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @return the size (in pixels)
     */
    public static float vertexSize(Mesh mesh, int vertexIndex) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");

        VertexBuffer vertexBuffer = mesh.getBuffer(VertexBuffer.Type.Size);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(vertexIndex);
        float result = floatBuffer.get();

        return result;
    }

    /**
     * Calculate the tangent of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return the tangent vector (either storeResult or a new instance)
     */
    public static Vector4f vertexTangent(Mesh mesh, int vertexIndex,
            Matrix4f[] skinningMatrices, Vector4f storeResult) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Vector4f result = (storeResult == null) ? new Vector4f() : storeResult;

        if (isAnimated(mesh)) {
            Vector4f b = vertexVector4f(mesh, VertexBuffer.Type.BindPoseTangent,
                    vertexIndex, null);

            VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
            FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            result.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s;
                    if (boneIndex < skinningMatrices.length) {
                        s = skinningMatrices[boneIndex];
                    } else {
                        s = matrixIdentity;
                    }
                    float xOf = s.m00 * b.x + s.m01 * b.y + s.m02 * b.z;
                    float yOf = s.m10 * b.x + s.m11 * b.y + s.m12 * b.z;
                    float zOf = s.m20 * b.x + s.m21 * b.y + s.m22 * b.z;
                    result.x += weight * xOf;
                    result.y += weight * yOf;
                    result.z += weight * zOf;
                }
            }
            result.normalizeLocal();
            result.w = b.w; // copy the binormal parity

        } else { // not an animated mesh
            vertexVector4f(mesh, VertexBuffer.Type.Tangent, vertexIndex,
                    result);
        }

        return result;
    }

    /**
     * Copy texture coordinates of the indexed vertex from the specified vertex
     * buffer.
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (8 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the texture coordinates (either storeResult or a new instance)
     */
    public static Vector2f vertexVector2f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector2f storeResult) {
        Validate.nonNull(mesh, "mesh");
        assert bufferType == VertexBuffer.Type.TexCoord
                || bufferType == VertexBuffer.Type.TexCoord2
                || bufferType == VertexBuffer.Type.TexCoord3
                || bufferType == VertexBuffer.Type.TexCoord4
                || bufferType == VertexBuffer.Type.TexCoord5
                || bufferType == VertexBuffer.Type.TexCoord6
                || bufferType == VertexBuffer.Type.TexCoord7
                || bufferType == VertexBuffer.Type.TexCoord8 : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector2f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(2 * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();

        return storeResult;
    }

    /**
     * Copy Vector3f data for the indexed vertex from the specified vertex
     * buffer.
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (5 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static Vector3f vertexVector3f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector3f storeResult) {
        Validate.nonNull(mesh, "mesh");
        assert bufferType == VertexBuffer.Type.BindPoseNormal
                || bufferType == VertexBuffer.Type.BindPosePosition
                || bufferType == VertexBuffer.Type.Binormal
                || bufferType == VertexBuffer.Type.Normal
                || bufferType == VertexBuffer.Type.Position : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(MyVector3f.numAxes * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();
        storeResult.z = floatBuffer.get();

        return storeResult;
    }

    /**
     * Copy Vector4f data for the indexed vertex from the specified vertex
     * buffer.
     *
     * @param mesh subject mesh (not null)
     * @param bufferType which buffer to read (5 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static Vector4f vertexVector4f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector4f storeResult) {
        Validate.nonNull(mesh, "mesh");
        assert bufferType == VertexBuffer.Type.BindPoseTangent
                || bufferType == VertexBuffer.Type.BoneWeight
                || bufferType == VertexBuffer.Type.Color
                || bufferType == VertexBuffer.Type.HWBoneWeight
                || bufferType == VertexBuffer.Type.Tangent : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector4f();
        }

        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);
        FloatBuffer floatBuffer = (FloatBuffer) vertexBuffer.getDataReadOnly();
        floatBuffer.position(4 * vertexIndex);
        storeResult.x = floatBuffer.get();
        storeResult.y = floatBuffer.get();
        storeResult.z = floatBuffer.get();
        storeResult.w = floatBuffer.get();

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
        if (geometry.isIgnoreTransform()) {
            storeResult.set(meshLocation);
        } else {
            geometry.localToWorld(meshLocation, storeResult);
        }

        return storeResult;
    }
}

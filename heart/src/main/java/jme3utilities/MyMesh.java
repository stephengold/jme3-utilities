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
package jme3utilities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.VectorSet;
import jme3utilities.math.VectorSetUsingBuffer;
import jme3utilities.math.IntPair;

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
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * number of vertices per edge
     */
    final public static int vpe = 2;
    /**
     * number of vertices per triangle
     */
    final public static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyMesh.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix4f#IDENTITY}
     */
    final private static Matrix4f matrixIdentity = new Matrix4f();
    /**
     * scale factors to reverse the direction of a vector
     */
    final private static Vector3f scaleReverse = new Vector3f(-1f, -1f, -1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyMesh() {
    }
    // *************************************************************************
    // new methods exposed
    // TODO add isConnected(), makeDoubleSided(), merge()

    /**
     * Compress a Mesh by introducing an index buffer.
     *
     * @param input the input Mesh (not null, without an index buffer)
     * @return a new Mesh (with an index buffer)
     */
    public static Mesh addIndices(Mesh input) {
        Validate.nonNull(input, "input");
        assert !hasIndices(input);
        /*
         * Assign new indices and create mappings between
         * the old and new indices.
         */
        int oldN = input.getVertexCount();
        int[] old2new = new int[oldN];
        int[] new2old = new int[oldN];
        int newN = 0;
        for (int oldI = 0; oldI < oldN; ++oldI) {
            old2new[oldI] = -1;
            new2old[oldI] = -1;

            for (int newI = 0; newI < newN; ++newI) {
                if (areIdentical(input, oldI, new2old[newI])) {
                    old2new[oldI] = newI;
                    break;
                }
            }
            if (old2new[oldI] == -1) { // allocate a vertex index
                old2new[oldI] = newN;
                new2old[newN] = oldI;
                ++newN;
            }
        }
        /*
         * Create a clone of the input mesh with smaller vertex buffers.
         */
        Mesh result = input.clone();
        for (VertexBuffer oldVertexBuffer : input.getBufferList()) {
            VertexBuffer.Type type = oldVertexBuffer.getBufferType();
            result.clearBuffer(type);
            VertexBuffer.Format format = oldVertexBuffer.getFormat();
            int numCperE = oldVertexBuffer.getNumComponents();
            Buffer data = VertexBuffer.createBuffer(format, numCperE, newN);
            result.setBuffer(type, numCperE, format, data);
        }
        /*
         * Copy vertex data from the input mesh to the new mesh.
         */
        for (int newI = 0; newI < newN; ++newI) {
            int oldI = new2old[newI];
            for (VertexBuffer newVB : result.getBufferList()) {
                VertexBuffer.Type type = newVB.getBufferType();
                VertexBuffer oldVB = input.getBuffer(type);
                assert oldVB != newVB;
                Element.copy(oldVB, oldI, newVB, newI);
            }
        }
        /*
         * Create the index buffer and fill it with indices.
         */
        IndexBuffer ib = IndexBuffer.createIndexBuffer(newN, oldN);
        for (int oldI = 0; oldI < oldN; ++oldI) {
            int newI = old2new[oldI];
            ib.put(oldI, newI);
        }
        Buffer ibData = ib.getBuffer();
        VertexBuffer.Format ibFormat;
        if (ibData instanceof ShortBuffer) {
            ibFormat = VertexBuffer.Format.UnsignedShort;
        } else {
            assert ibData instanceof IntBuffer;
            ibFormat = VertexBuffer.Format.UnsignedInt;
        }
        result.setBuffer(VertexBuffer.Type.Index, 1, ibFormat, ibData);
        /*
         * Flip each buffer.
         */
        for (VertexBuffer outVB : result.getBufferList()) {
            Buffer data = outVB.getData();
            int endPosition = data.capacity();
            data.position(endPosition);
            data.flip();
        }

        result.updateCounts();

        return result;
    }

    /**
     * Add normals to a Mesh for an outward-facing sphere.
     *
     * @param mesh the Mesh to modify (not null, without normals)
     */
    public static void addSphereNormals(Mesh mesh) {
        Validate.nonNull(mesh, "mesh");
        assert !hasNormals(mesh);

        FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        FloatBuffer normals = BufferUtils.clone(positions);
        int numFloats = positions.limit();
        MyBuffer.normalize(normals, 0, numFloats);
        normals.flip();
        mesh.setBuffer(VertexBuffer.Type.Normal, numAxes, normals);
    }

    /**
     * Test whether 2 vertices in the specified mesh are identical.
     *
     * @param mesh (not null, unaffected)
     * @param vi1 the index of the first vertex (&ge;0)
     * @param vi2 the index of the 2nd vertex (&ge;0)
     * @return true if identical, otherwise false
     */
    public static boolean areIdentical(Mesh mesh, int vi1, int vi2) {
        Validate.nonNull(mesh, "mesh");
        int numVertices = mesh.getVertexCount();
        Validate.inRange(vi1, "first vertex index", 0, numVertices - 1);
        Validate.inRange(vi2, "2nd vertex index", 0, numVertices - 1);

        if (vi1 == vi2) {
            return true;
        }

        for (VertexBuffer vertexBuffer : mesh.getBufferList()) {
            VertexBuffer.Type type = vertexBuffer.getBufferType();
            if (type != VertexBuffer.Type.Index) {
                if (!Element.equals(vertexBuffer, vi1, vi2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Generate a material to visualize the bone weights in the specified Mesh.
     *
     * @param mesh the subject mesh (not null, animated, modified)
     * @param boneIndexToColor map bone indices to colors (not null, unaffected)
     * @param assetManager (not null)
     * @return a new wireframe material instance
     */
    public static Material boneWeightMaterial(Mesh mesh,
            ColorRGBA[] boneIndexToColor, AssetManager assetManager) {
        if (!isAnimated(mesh)) {
            throw new IllegalArgumentException("Must be an animated mesh.");
        }

        int numVertices = mesh.getVertexCount();
        FloatBuffer colorBuf = BufferUtils.createFloatBuffer(4 * numVertices);

        int[] biArray = new int[maxWeights];
        float[] bwArray = new float[maxWeights];
        ColorRGBA sum = new ColorRGBA();
        ColorRGBA term = new ColorRGBA();
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            vertexBoneIndices(mesh, vertexIndex, biArray);
            vertexBoneWeights(mesh, vertexIndex, bwArray);
            sum.set(0f, 0f, 0f, 1f);
            for (int j = 0; j < maxWeights; ++j) {
                int boneI = biArray[j];
                if (boneI >= 0 && boneI < boneIndexToColor.length) {
                    term.set(boneIndexToColor[boneI]);
                    float weight = bwArray[j];
                    term.multLocal(weight);
                    sum.addLocal(term);
                }
            }
            colorBuf.put(sum.r).put(sum.g).put(sum.b).put(1f);
        }

        mesh.setBuffer(VertexBuffer.Type.Color, 4, VertexBuffer.Format.Float,
                colorBuf);

        Material material = MyAsset.createUnshadedMaterial(assetManager);
        material.setBoolean("VertexColor", true);

        RenderState rs = material.getAdditionalRenderState();
        rs.setWireframe(true);

        return material;
    }

    /**
     * Estimate the number of bones in the specified Mesh by reading its
     * bone-index buffers.
     *
     * @param mesh the Mesh to examine (not null)
     * @return an estimated number of bones (&ge;0)
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

        FloatBuffer weightBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * maxWeights : numWeights;

        int result = 0;
        for (int vIndex = 0; vIndex < numVertices; ++vIndex) {
            for (int wIndex = 0; wIndex < maxWeights; ++wIndex) {
                float weight = weightBuffer.get();
                int bIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert && weight != 0f
                        && bIndex >= result) {
                    result = bIndex + 1;
                }
            }
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Expand a Mesh to ensure that no vertex data are re-used. Any index buffer
     * is eliminated and any loop/strip/fan mode is changed to Triangles or
     * Lines.
     *
     * @param in the input mesh (not null, mode not Hybrid or Patch, unaffected)
     * @return a new Mesh (without an index buffer, mode = Triangles or Lines or
     * Points)
     */
    public static Mesh expand(Mesh in) {
        Mesh.Mode outMode;
        Mesh.Mode inMode = in.getMode();
        switch (inMode) {
            case Points:
            case Lines:
            case Triangles:
                outMode = inMode;
                break;

            case LineLoop:
            case LineStrip:
                outMode = Mesh.Mode.Lines;
                break;

            case TriangleFan:
            case TriangleStrip:
                outMode = Mesh.Mode.Triangles;
                break;

            default:
                String message = "mode = " + inMode;
                throw new IllegalArgumentException(message);
        }

        IndexBuffer indexList = in.getIndicesAsList();
        int outVertexCount = indexList.size();
        /*
         * Create a shallow clone of the input mesh.
         */
        Mesh out = in.clone();
        out.setMode(outMode);

        for (VertexBuffer inVertexBuffer : in.getBufferList()) {
            VertexBuffer.Type type = inVertexBuffer.getBufferType();
            out.clearBuffer(type);

            if (type != VertexBuffer.Type.Index) {
                VertexBuffer.Format format = inVertexBuffer.getFormat();
                int numCperE = inVertexBuffer.getNumComponents();
                Buffer data = VertexBuffer.createBuffer(format, numCperE,
                        outVertexCount);
                out.setBuffer(type, numCperE, format, data);
            }
        }
        /*
         * Copy all vertex data to the new mesh.
         */
        for (int outVI = 0; outVI < outVertexCount; ++outVI) {
            int inVI = indexList.get(outVI);
            for (VertexBuffer outVB : out.getBufferList()) {
                VertexBuffer.Type type = outVB.getBufferType();
                VertexBuffer inVB = in.getBuffer(type);
                assert inVB != outVB;
                Element.copy(inVB, inVI, outVB, outVI);
            }
        }
        /*
         * Flip each buffer.
         */
        for (VertexBuffer outVB : out.getBufferList()) {
            Buffer data = outVB.getData();
            int endPosition = data.capacity();
            data.position(endPosition);
            data.flip();
        }

        out.updateCounts();

        return out;
    }

    /**
     * Generate normals on a triangle-by-triangle basis for a Triangles-mode
     * Mesh without an index buffer. Any pre-existing normal buffer is
     * discarded.
     *
     * @param mesh the Mesh to modify (not null, mode=Triangles, not indexed)
     */
    public static void generateNormals(Mesh mesh) {
        assert mesh.getMode() == Mesh.Mode.Triangles : mesh.getMode();
        assert !hasIndices(mesh);

        FloatBuffer positionBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        int numFloats = positionBuffer.limit();

        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(numFloats);
        mesh.setBuffer(VertexBuffer.Type.Normal, numAxes, normalBuffer);

        Triangle triangle = new Triangle();
        Vector3f pos1 = new Vector3f();
        Vector3f pos2 = new Vector3f();
        Vector3f pos3 = new Vector3f();

        int numTriangles = numFloats / vpt / numAxes;
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int trianglePosition = triIndex * vpt * numAxes;
            MyBuffer.get(positionBuffer, trianglePosition, pos1);
            MyBuffer.get(positionBuffer, trianglePosition + numAxes, pos2);
            MyBuffer.get(positionBuffer, trianglePosition + 2 * numAxes, pos3);
            triangle.set(pos1, pos2, pos3);

            triangle.setNormal(null); // work around JME issue #957
            Vector3f normal = triangle.getNormal();
            for (int j = 0; j < vpt; ++j) {
                normalBuffer.put(normal.x);
                normalBuffer.put(normal.y);
                normalBuffer.put(normal.z);
            }
        }
        normalBuffer.flip();
    }

    /**
     * Test whether the specified Mesh has an index buffer.
     *
     * @param mesh the Mesh to test (not null, unaffected)
     * @return true if the Mesh has indices, otherwise false
     */
    public static boolean hasIndices(Mesh mesh) {
        VertexBuffer buffer = mesh.getBuffer(VertexBuffer.Type.Index);
        if (buffer == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified Mesh has vertex normals.
     *
     * @param mesh the Mesh to test (not null, unaffected)
     * @return true if the Mesh has vertex normals, otherwise false
     */
    public static boolean hasNormals(Mesh mesh) {
        VertexBuffer buffer = mesh.getBuffer(VertexBuffer.Type.Normal);
        if (buffer == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified Mesh is composed of triangles.
     *
     * @param mesh the Mesh to test (not null, unaffected)
     * @return true if the Mesh is composed of triangles, otherwise false
     */
    public static boolean hasTriangles(Mesh mesh) {
        Mesh.Mode mode = mesh.getMode();
        boolean result;
        switch (mode) {
            case Points:
            case Lines:
            case LineStrip:
            case LineLoop:
                result = false;
                break;

            case Triangles:
            case TriangleFan:
            case TriangleStrip:
                result = true;
                break;

            default:
                String message = "mode = " + mode;
                throw new IllegalArgumentException(message);
        }

        return result;
    }

    /**
     * Test whether the specified Mesh has texture (U-V) coordinates.
     *
     * @param mesh the Mesh to test (not null, unaffected)
     * @return true if the Mesh has texture coordinates, otherwise false
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
     * Test whether the specified Mesh is animated. Unlike Mesh.isAnimated()
     * this method checks for bone weights and ignores HW buffers.
     *
     * @param mesh which Mesh to test (not null, unaffected)
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
     * @param storeResult storage for results (added to if not null)
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
     * Enumerate the world locations of all vertices in the specified subtree of
     * a scene graph. Note: recursive!
     *
     * @param subtree (may be null)
     * @param storeResult storage for results (added to if not null)
     * @return the resulting set (either storeResult or a new instance)
     */
    public static VectorSet listVertexLocations(Spatial subtree,
            VectorSet storeResult) {
        VectorSet result;
        if (storeResult == null) {
            result = new VectorSetUsingBuffer(64);
        } else {
            result = storeResult;
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            int numVertices = mesh.getVertexCount();
            Vector3f tempLocation = new Vector3f();
            for (int vertexI = 0; vertexI < numVertices; ++vertexI) {
                MyMesh.vertexVector3f(mesh, VertexBuffer.Type.Position, vertexI,
                        tempLocation);
                if (!geometry.isIgnoreTransform()) {
                    geometry.localToWorld(tempLocation, tempLocation);
                }
                result.add(tempLocation);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listVertexLocations(child, result);
            }
        }

        return result;
    }

    /**
     * Count how many vertices in the specified Mesh are directly influenced by
     * the indexed bone.
     *
     * @param mesh the Mesh to analyze (not null, possibly modified)
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

        FloatBuffer weightBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * maxWeights : numWeights;

        int result = 0;
        for (int vIndex = 0; vIndex < numVertices; ++vIndex) {
            for (int wIndex = 0; wIndex < maxWeights; ++wIndex) {
                float weight = weightBuffer.get();
                int bIndex = readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert
                        && bIndex == boneIndex
                        && weight != 0f) {
                    ++result;
                }
            }
        }

        return result;
    }

    /**
     * Read an index from a Buffer and advance the buffer's position.
     *
     * @param buffer a Buffer of bytes or shorts (not null)
     * @return index (&ge;0)
     */
    public static int readIndex(Buffer buffer) {
        Validate.nonNull(buffer, "buffer");

        int result;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) buffer;
            byte b = byteBuffer.get();
            result = 0xff & b;

        } else if (buffer instanceof IntBuffer) {
            IntBuffer intBuffer = (IntBuffer) buffer;
            result = intBuffer.get();

        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer shortBuffer = (ShortBuffer) buffer;
            short s = shortBuffer.get();
            result = 0xffff & s;

        } else {
            String message = buffer.getClass().getName();
            throw new IllegalArgumentException(message);
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Reverse the normals of a Mesh. Apply this method (for instance) after
     * reversing the winding order of a triangle mesh.
     *
     * @param mesh the Mesh to modify (not null)
     */
    public static void reverseNormals(Mesh mesh) {
        FloatBuffer buffer = mesh.getFloatBuffer(VertexBuffer.Type.Normal);
        if (buffer != null) {
            MyBuffer.scale(buffer, 0, buffer.limit(), scaleReverse);
        }

        buffer = mesh.getFloatBuffer(VertexBuffer.Type.BindPoseNormal);
        if (buffer != null) {
            MyBuffer.scale(buffer, 0, buffer.limit(), scaleReverse);
        }
    }

    /**
     * Reverse the winding order of a Triangles-mode Mesh, but don't reverse its
     * normals.
     *
     * @param mesh the Mesh to modify (not null, mode=Triangles)
     */
    public static void reverseWinding(Mesh mesh) {
        assert mesh.getMode() == Mesh.Mode.Triangles : mesh.getMode();

        mesh.updateCounts();
        int numTriangles = mesh.getTriangleCount();

        IndexBuffer indexBuffer = mesh.getIndexBuffer();
        if (indexBuffer != null) { // a Mesh with indices
            int numIndices = vpt * numTriangles;
            assert indexBuffer.size() == numIndices : indexBuffer.size();
            for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
                int v1Index = vpt * triIndex;
                int v3Index = (vpt * triIndex + vpt - 1);
                int i1 = indexBuffer.get(v1Index);
                int i3 = indexBuffer.get(v3Index);
                indexBuffer.put(v1Index, i3);
                indexBuffer.put(v3Index, i1);
            }

        } else { // a Mesh without indices
            int numVertices = vpt * numTriangles;
            for (VertexBuffer vb : mesh.getBufferList()) {
                assert vb.getNumElements() == numVertices : vb.getNumElements();
                for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
                    int v1Index = vpt * triIndex;
                    int v3Index = vpt * triIndex + vpt - 1;
                    Element.swap(vb, v1Index, v3Index);
                }
            }
        }
    }

    /**
     * Replace the BoneIndexBuffer of a Mesh.
     *
     * @param mesh the Mesh to modify (not null)
     * @param wpv the number of bone weights per vertex (&ge;1, &le;4)
     * @param indexBuffer the desired IndexBuffer (not null, alias created)
     */
    public static void setBoneIndexBuffer(Mesh mesh, int wpv,
            IndexBuffer indexBuffer) {
        Validate.nonNull(mesh, "mesh");
        Validate.inRange(wpv, "weights per vertex", 1, maxWeights);

        Buffer buffer = indexBuffer.getBuffer();
        VertexBuffer.Type type = VertexBuffer.Type.BoneIndex;
        if (buffer instanceof ByteBuffer) {
            mesh.setBuffer(type, wpv, (ByteBuffer) buffer);
        } else if (buffer instanceof IntBuffer) {
            mesh.setBuffer(type, wpv, (IntBuffer) buffer);
        } else if (buffer instanceof ShortBuffer) {
            mesh.setBuffer(type, wpv, (ShortBuffer) buffer);
        } else {
            String message = buffer.getClass().getName();
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Smooth the normals of a Mesh by averaging them across all uses of each
     * distinct vertex position.
     *
     * @param mesh the Mesh to modify (not null, with normals)
     */
    public static void smoothNormals(Mesh mesh) {
        Validate.nonNull(mesh, "mesh");
        assert hasNormals(mesh);

        FloatBuffer positionBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        int numVertices = positionBuffer.limit() / numAxes;

        Map<Vector3f, Integer> mapPosToDpid = new HashMap<>(numVertices);
        int numDistinctPositions = 0;
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            Vector3f position = new Vector3f();
            MyBuffer.get(positionBuffer, start, position);
            MyVector3f.standardize(position, position);
            if (!mapPosToDpid.containsKey(position)) {
                mapPosToDpid.put(position, numDistinctPositions);
                ++numDistinctPositions;
            }
        }
        /*
         * Initialize the normal sum for each distinct position.
         */
        Vector3f[] normalSum = new Vector3f[numDistinctPositions];
        for (int dpid = 0; dpid < numDistinctPositions; ++dpid) {
            normalSum[dpid] = new Vector3f(0f, 0f, 0f);
        }

        IndexBuffer indexList = mesh.getIndicesAsList();
        int numIndices = indexList.size();

        FloatBuffer normalBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.Normal);
        Vector3f tmpPosition = new Vector3f();
        Vector3f tmpNormal = new Vector3f();
        for (int ibPosition = 0; ibPosition < numIndices; ++ibPosition) {
            int vertexIndex = indexList.get(ibPosition);
            int start = vertexIndex * numAxes;
            MyBuffer.get(positionBuffer, start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);

            MyBuffer.get(normalBuffer, start, tmpNormal);
            normalSum[dpid].addLocal(tmpNormal);
        }
        /*
         * Re-normalize the normal sum for each distinct position.
         */
        for (int dpid = 0; dpid < normalSum.length; ++dpid) {
            normalSum[dpid].normalizeLocal();
        }
        /*
         * Write new normals to the buffer.
         */
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            MyBuffer.get(positionBuffer, start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);
            MyBuffer.put(normalBuffer, start, normalSum[dpid]);
        }
    }

    /**
     * Convert mesh triangles to lines.
     *
     * @param mesh the mesh to modify (not null,
     * mode=Triangles/TriangleFan/TriangleStrip)
     */
    public static void trianglesToLines(Mesh mesh) {
        Validate.nonNull(mesh, "mesh");
        assert hasTriangles(mesh);

        IndexBuffer indexList = mesh.getIndicesAsList();
        int numTriangles = indexList.size() / vpt;
        Set<IntPair> edgeSet = new HashSet<>(vpt * numTriangles);
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int intOffset = vpt * triIndex;
            int ti0 = indexList.get(intOffset);
            int ti1 = indexList.get(intOffset + 1);
            int ti2 = indexList.get(intOffset + 2);

            edgeSet.add(new IntPair(ti0, ti1));
            edgeSet.add(new IntPair(ti0, ti2));
            edgeSet.add(new IntPair(ti1, ti2));
        }
        int numEdges = edgeSet.size();
        int numIndices = vpe * numEdges;
        int numVertices = mesh.getVertexCount();

        mesh.clearBuffer(VertexBuffer.Type.Index);

        IndexBuffer ib = IndexBuffer.createIndexBuffer(numVertices, numIndices);
        int bufferPosition = 0;
        for (IntPair edge : edgeSet) {
            ib.put(bufferPosition, edge.smaller());
            ib.put(bufferPosition + 1, edge.larger());
            bufferPosition += vpe;
        }
        Buffer ibData = ib.getBuffer();
        VertexBuffer.Format ibFormat;
        if (ibData instanceof ShortBuffer) {
            ibFormat = VertexBuffer.Format.UnsignedShort;
        } else {
            assert ibData instanceof IntBuffer;
            ibFormat = VertexBuffer.Format.UnsignedInt;
        }
        ibData.limit(bufferPosition);
        mesh.setBuffer(VertexBuffer.Type.Index, vpe, ibFormat, ibData);

        mesh.setMode(Mesh.Mode.Lines);
    }

    /**
     * Copy the bone indices for the indexed vertex.
     *
     * @param mesh the subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static int[] vertexBoneIndices(Mesh mesh, int vertexIndex,
            int[] storeResult) {
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
        for (int wIndex = 0; wIndex < maxWeightsPerVert; ++wIndex) {
            int boneIndex = readIndex(boneIndexBuffer);
            storeResult[wIndex] = boneIndex;
        }
        /*
         * Fill with -1s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; ++wIndex) {
            storeResult[wIndex] = -1;
        }

        return storeResult;
    }

    /**
     * Copy the bone weights for the indexed vertex.
     *
     * @param mesh the subject mesh (not null, unaffected)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static float[] vertexBoneWeights(Mesh mesh, int vertexIndex,
            float[] storeResult) {
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

        FloatBuffer weightBuffer
                = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
        int startIndex = maxWeights * vertexIndex;
        for (int wIndex = 0; wIndex < maxWeightsPerVert; ++wIndex) {
            storeResult[wIndex] = weightBuffer.get(startIndex + wIndex);
        }
        /*
         * Fill with 0s.
         */
        int length = storeResult.length;
        for (int wIndex = maxWeightsPerVert; wIndex < length; ++wIndex) {
            storeResult[wIndex] = 0f;
        }

        return storeResult;
    }

    /**
     * Copy the color of the indexed vertex from the color buffer.
     *
     * @param mesh the subject mesh (not null, unaffected)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
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
        int floatIndex = 4 * vertexIndex;
        storeResult.r = floatBuffer.get(floatIndex);
        storeResult.g = floatBuffer.get(floatIndex + 1);
        storeResult.b = floatBuffer.get(floatIndex + 2);
        storeResult.a = floatBuffer.get(floatIndex + 3);

        return storeResult;
    }

    /**
     * Calculate the location of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh the subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
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

            FloatBuffer weightBuffer
                    = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            storeResult.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; ++wIndex) {
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
     * @param mesh the subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
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

            FloatBuffer weightBuffer
                    = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            result.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; ++wIndex) {
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
     * @param mesh the subject mesh (not null, unaffected)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @return the size (in pixels)
     */
    public static float vertexSize(Mesh mesh, int vertexIndex) {
        Validate.nonNull(mesh, "mesh");
        Validate.nonNegative(vertexIndex, "vertex index");

        FloatBuffer floatBuffer = mesh.getFloatBuffer(VertexBuffer.Type.Size);
        float result = floatBuffer.get(vertexIndex);

        return result;
    }

    /**
     * Calculate the tangent of the indexed vertex in mesh space using the
     * skinning matrices provided.
     *
     * @param mesh the subject mesh (not null)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
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

            FloatBuffer weightBuffer
                    = mesh.getFloatBuffer(VertexBuffer.Type.BoneWeight);
            weightBuffer.position(maxWeights * vertexIndex);

            VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
            Buffer boneIndexBuffer = biBuf.getDataReadOnly();
            boneIndexBuffer.position(maxWeights * vertexIndex);

            result.zero();
            int maxWeightsPerVertex = mesh.getMaxNumWeights();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; ++wIndex) {
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
     * @param mesh the subject mesh (not null, unaffected)
     * @param bufferType which buffer to read (8 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the texture coordinates (either storeResult or a new instance)
     */
    public static Vector2f vertexVector2f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector2f storeResult) {
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

        FloatBuffer floatBuffer = mesh.getFloatBuffer(bufferType);
        int floatIndex = 2 * vertexIndex;
        storeResult.x = floatBuffer.get(floatIndex);
        storeResult.y = floatBuffer.get(floatIndex + 1);

        return storeResult;
    }

    /**
     * Copy Vector3f data for the indexed vertex from the specified
     * VertexBuffer.
     * <p>
     * A software skin update is required BEFORE reading vertex
     * positions/normals/tangents from an animated mesh
     *
     * @param mesh the subject mesh (not null, unaffected)
     * @param bufferType which buffer to read (5 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static Vector3f vertexVector3f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector3f storeResult) {
        assert bufferType == VertexBuffer.Type.BindPoseNormal
                || bufferType == VertexBuffer.Type.BindPosePosition
                || bufferType == VertexBuffer.Type.Binormal
                || bufferType == VertexBuffer.Type.Normal
                || bufferType == VertexBuffer.Type.Position : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        FloatBuffer floatBuffer = mesh.getFloatBuffer(bufferType);
        int floatIndex = MyVector3f.numAxes * vertexIndex;
        MyBuffer.get(floatBuffer, floatIndex, result);

        return result;
    }

    /**
     * Copy Vector4f data for the indexed vertex from the specified vertex
     * buffer.
     *
     * @param mesh the subject mesh (not null, unaffected)
     * @param bufferType which buffer to read (5 legal values)
     * @param vertexIndex index into the mesh's vertices (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the data vector (either storeResult or a new instance)
     */
    public static Vector4f vertexVector4f(Mesh mesh,
            VertexBuffer.Type bufferType, int vertexIndex,
            Vector4f storeResult) {
        assert bufferType == VertexBuffer.Type.BindPoseTangent
                || bufferType == VertexBuffer.Type.BoneWeight
                || bufferType == VertexBuffer.Type.Color
                || bufferType == VertexBuffer.Type.HWBoneWeight
                || bufferType == VertexBuffer.Type.Tangent : bufferType;
        Validate.nonNegative(vertexIndex, "vertex index");
        if (storeResult == null) {
            storeResult = new Vector4f();
        }

        FloatBuffer floatBuffer = mesh.getFloatBuffer(bufferType);
        int floatIndex = 4 * vertexIndex;
        storeResult.x = floatBuffer.get(floatIndex);
        storeResult.y = floatBuffer.get(floatIndex + 1);
        storeResult.z = floatBuffer.get(floatIndex + 2);
        storeResult.w = floatBuffer.get(floatIndex + 3);

        return storeResult;
    }

    /**
     * Calculate the location of the indexed vertex in world space using the
     * skinning matrices provided.
     *
     * @param geometry Geometry containing the subject mesh (not null)
     * @param vertexIndex index into the geometry's vertices (&ge;0)
     * @param skinningMatrices (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the location in world coordinates (either storeResult or a new
     * instance)
     */
    public static Vector3f vertexWorldLocation(Geometry geometry,
            int vertexIndex, Matrix4f[] skinningMatrices,
            Vector3f storeResult) {
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

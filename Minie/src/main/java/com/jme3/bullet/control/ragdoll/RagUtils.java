/*
 * Copyright (c) 2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.control.ragdoll;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;

/**
 * Utility methods used by KinematicRagdollControl and associated classes.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RagUtils {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RagUtils.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RagUtils() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add the vertex weights of each bone in the specified mesh to an array of
     * total weights.
     *
     * @param mesh animated mesh to analyze (not null, unaffected)
     * @param totalWeights (not null, modified)
     */
    static void addWeights(Mesh mesh, float[] totalWeights) {
        assert totalWeights != null;

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }
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

        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = MyMesh.readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert) {
                    totalWeights[boneIndex] += weight;
                }
            }
        }
    }

    /**
     * Calculate the total bone weight animated by each bone in the specified
     * meshes.
     *
     * @param meshes the animated meshes to analyze (not null, unaffected)
     * @param skeleton (not null, unaffected)
     * @return map bone indices to total bone weight
     */
    public static float[] totalWeights(Mesh[] meshes, Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        float[] result = new float[numBones];
        for (Mesh mesh : meshes) {
            RagUtils.addWeights(mesh, result);
        }

        List<Bone> bones = MySkeleton.preOrderBones(skeleton);
        Collections.reverse(bones);
        for (Bone childBone : bones) {
            int childIndex = skeleton.getBoneIndex(childBone);
            Bone parent = childBone.getParent();
            if (parent != null) {
                int parentIndex = skeleton.getBoneIndex(parent);
                result[parentIndex] += result[childIndex];
            }
        }

        return result;
    }
}

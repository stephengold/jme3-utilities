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
package com.jme3.bullet.animation;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Utility methods used by DynamicAnimControl and associated classes.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
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
     * Assign each mesh vertex to a link and add its location (mesh coordinates
     * in bind pose) to that link's list.
     *
     * @param meshes array of animated meshes to use (not null, unaffected)
     * @param managerMap a map from bone indices to managing link names
     * @return a new map from link names to lists of coordinates
     */
    public static Map<String, List<Vector3f>> coordsMap(Mesh[] meshes,
            String[] managerMap) {
        float[] wArray = new float[4];
        int[] iArray = new int[4];
        Map<String, List<Vector3f>> coordsMap = new HashMap<>(32);
        for (Mesh mesh : meshes) {
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; vertexI++) {
                MyMesh.vertexBoneIndices(mesh, vertexI, iArray);
                MyMesh.vertexBoneWeights(mesh, vertexI, wArray);

                Map<String, Float> weightMap
                        = weightMap(iArray, wArray, managerMap);

                float bestTotalWeight = Float.NEGATIVE_INFINITY;
                String bestLbName = null;
                for (Map.Entry<String, Float> entry : weightMap.entrySet()) {
                    float totalWeight = entry.getValue();
                    if (totalWeight >= bestTotalWeight) {
                        bestTotalWeight = totalWeight;
                        bestLbName = entry.getKey();
                    }
                }
                /*
                 * Add the bind-pose coordinates of the vertex
                 * to the linked bone's list.
                 */
                List<Vector3f> coordList;
                if (coordsMap.containsKey(bestLbName)) {
                    coordList = coordsMap.get(bestLbName);
                } else {
                    coordList = new ArrayList<>(20);
                    coordsMap.put(bestLbName, coordList);
                }
                Vector3f bindPosition = MyMesh.vertexVector3f(mesh,
                        VertexBuffer.Type.BindPosePosition, vertexI, null);
                coordList.add(bindPosition);
            }
        }

        return coordsMap;
    }

    /**
     * Create a hull collision shape using the specified inverse transform and
     * list of vertex locations. The skeleton is assumed to be in bind pose.
     *
     * @param transform from vertex coordinates to descaled shape coordinates
     * (not null, unaffected)
     * @param vertexLocations list of vertex locations (not null, not empty,
     * unaffected)
     * @return a new shape
     */
    public static CollisionShape createShape(Transform transform,
            List<Vector3f> vertexLocations) {
        assert transform != null;
        assert vertexLocations != null;
        assert !vertexLocations.isEmpty();

        for (Vector3f location : vertexLocations) {
            /*
             * Transform mesh coordinates to descaled shape coordinates.
             */
            transform.transformVector(location, location);
        }

        CollisionShape boneShape = new HullCollisionShape(vertexLocations);

        return boneShape;
    }

    /**
     * Read an array of transforms from an input capsule.
     *
     * @param capsule the input capsule (not null)
     * @param fieldName the name of the field to read (not null)
     * @return a new array or null
     * @throws IOException from capsule
     */
    public static Transform[] readTransformArray(InputCapsule capsule,
            String fieldName) throws IOException {
        Validate.nonNull(capsule, "capsule");
        Validate.nonNull(fieldName, "field name");

        Savable[] tmp = capsule.readSavableArray(fieldName, null);
        Transform[] result;
        if (tmp == null) {
            result = null;
        } else {
            result = new Transform[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                result[i] = (Transform) tmp[i];
            }
        }

        return result;
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

    /**
     * Validate a skeleton for use in a DynamicAnimControl.
     *
     * @param skeleton the skeleton to validate (not null, unaffected)
     */
    public static void validate(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        if (numBones < 0) {
            throw new IllegalArgumentException("Bone count is negative!");
        }

        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (bone == null) {
                String msg = String.format("Bone %d in skeleton is null!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            }
            String boneName = bone.getName();
            if (boneName == null) {
                String msg = String.format("Bone %d in skeleton has null name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (boneName.equals(ConfigDynamicAnimControl.torsoName)) {
                String msg = String.format(
                        "Bone %d in skeleton has a reserved name!",
                        boneIndex);
                throw new IllegalArgumentException(msg);
            } else if (nameSet.contains(boneName)) {
                String msg = "Duplicate bone name in skeleton: "
                        + MyString.quote(boneName);
                throw new IllegalArgumentException(msg);
            }
            nameSet.add(boneName);
        }
    }

    /**
     * Tabulate the total bone weight associated with each link in a ragdoll.
     *
     * @param biArray the array of bone indices (not null, unaffected)
     * @param bwArray the array of bone weights (not null, unaffected)
     * @param lbNames a map from bone indices to managing link names (not null,
     * unaffected)
     * @return a new map from linked-bone names to total weight
     */
    public static Map<String, Float> weightMap(int[] biArray,
            float[] bwArray, String[] lbNames) {
        assert biArray.length == 4;
        assert bwArray.length == 4;

        Map<String, Float> weightMap = new HashMap<>(4);
        for (int j = 0; j < 4; j++) {
            int boneIndex = biArray[j];
            if (boneIndex != -1) {
                String lbName = lbNames[boneIndex];
                if (weightMap.containsKey(lbName)) {
                    float oldWeight = weightMap.get(lbName);
                    float newWeight = oldWeight + bwArray[j];
                    weightMap.put(lbName, newWeight);
                } else {
                    weightMap.put(lbName, bwArray[j]);
                }
            }
        }

        return weightMap;
    }
}

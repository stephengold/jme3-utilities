/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;

/**
 * A mesh used to visualize a skeleton. Each vertex corresponds to a bone in the
 * skeleton and follows that bone's head. Lines connect child bones with their
 * parents.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SkeletonMesh extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in the coordinate system
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonMesh.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh for the specified skeleton.
     *
     * @param skeleton the skeleton to visualize (may be null, unaffected)
     * @param mode mode for the mesh (Mode.Lines or Mode.Points)
     */
    SkeletonMesh(Skeleton skeleton, Mode mode) {
        int boneCount, numConnections;
        if (skeleton == null) {
            boneCount = 0;
            numConnections = 0;
        } else {
            boneCount = skeleton.getBoneCount();
            int numRoots = MySkeleton.countRootBones(skeleton);
            numConnections = boneCount - numRoots;
        }

        FloatBuffer floats = BufferUtils.createFloatBuffer(numAxes * boneCount);
        VertexBuffer positions = new VertexBuffer(Type.Position);
        positions.setupData(Usage.Stream, numAxes, Format.Float, floats);
        setBuffer(positions);

        FloatBuffer fColors = BufferUtils.createFloatBuffer(4 * boneCount);
        VertexBuffer vColors = new VertexBuffer(Type.Color);
        vColors.setupData(Usage.Stream, 4, Format.Float, fColors);
        setBuffer(vColors);

        if (mode == Mode.Lines) {
            int numIndices = 2 * numConnections;
            ShortBuffer shorts = BufferUtils.createShortBuffer(numIndices);
            VertexBuffer indices = new VertexBuffer(Type.Index);
            indices.setupData(Usage.Static, 2, Format.UnsignedShort, shorts);
            setBuffer(indices);
            /*
             * Populate the index buffer.
             */
            shorts.clear(); // prepare for writing
            for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
                Bone child = skeleton.getBone(boneIndex); // skeleton != null
                Bone parent = child.getParent();
                if (parent != null) {
                    short parentIndex = (short) skeleton.getBoneIndex(parent);
                    shorts.put(parentIndex);
                    short childIndex = (short) boneIndex;
                    shorts.put(childIndex);
                }
            }
            shorts.flip(); // prepare for reading
        }

        setMode(mode);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the color of each vertex in the mesh.
     *
     * @param colors color for each bone (not null, unaffected)
     */
    void updateColors(SkeletonVisualizer sv) {
        FloatBuffer fColors = getFloatBuffer(Type.Color);
        fColors.clear(); // prepare for writing

        int boneCount = sv.countBones();
        ColorRGBA color = new ColorRGBA();
        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            sv.copyHeadColor(boneIndex, color);
            fColors.put(color.r);
            fColors.put(color.g);
            fColors.put(color.b);
            fColors.put(color.a);
        }
        fColors.flip(); // prepare for reading

        VertexBuffer vColors = getBuffer(Type.Color);
        vColors.updateData(fColors);
    }

    /**
     * Update the position of each vertex in the mesh.
     *
     * @param skeleton the skeleton to visualize (may be null, unaffected)
     */
    void updatePositions(Skeleton skeleton) {
        FloatBuffer floats = getFloatBuffer(Type.Position);
        floats.clear(); // prepare for writing
        int boneCount;
        if (skeleton == null) {
            boneCount = 0;
        } else {
            boneCount = skeleton.getBoneCount();
        }
        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex); // skeleton != null
            Vector3f location = bone.getModelSpacePosition();
            floats.put(location.x);
            floats.put(location.y);
            floats.put(location.z);
        }
        floats.flip(); // prepare for reading

        VertexBuffer positions = getBuffer(Type.Position);
        positions.updateData(floats);
        /*
         * Update the bounding volume.
         */
        updateBound();
    }
    // *************************************************************************
    // Mesh methods

    /**
     * Create a shallow copy of this mesh.
     *
     * @return a new mesh, equivalent to this one
     */
    @Override
    public SkeletonMesh clone() {
        SkeletonMesh clone = (SkeletonMesh) super.clone();
        return clone;
    }
}

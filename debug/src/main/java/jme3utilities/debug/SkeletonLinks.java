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
package jme3utilities.debug;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
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
import jme3utilities.Validate;

/**
 * A line-mode mesh used to visualize a skeleton. Each vertex corresponds to a
 * bone in the skeleton and follows that bone's head.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonLinks extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonLinks.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mesh for the specified skeleton.
     *
     * @param skeleton the skeleton to visualize (not null)
     */
    public SkeletonLinks(Skeleton skeleton) {
        Validate.nonNull(skeleton, "skeleton");

        int boneCount = skeleton.getBoneCount();
        FloatBuffer floats = BufferUtils.createFloatBuffer(3 * boneCount);
        VertexBuffer positions = new VertexBuffer(Type.Position);
        positions.setupData(Usage.Stream, 3, Format.Float, floats);
        setBuffer(positions);

        int numRoots = MySkeleton.numRootBones(skeleton);
        int numConnections = boneCount - numRoots;
        ShortBuffer shorts = BufferUtils.createShortBuffer(2 * numConnections);
        VertexBuffer indices = new VertexBuffer(Type.Index);
        indices.setupData(Usage.Static, 2, Format.UnsignedShort, shorts);
        setBuffer(indices);
        /*
         * Populate the index buffer.
         */
        shorts.clear(); // prepare for writing
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone child = skeleton.getBone(boneIndex);
            Bone parent = child.getParent();
            if (parent != null) {
                short parentIndex = (short) skeleton.getBoneIndex(parent);
                shorts.put(parentIndex);
                short childIndex = (short) boneIndex;
                shorts.put(childIndex);
            }
        }
        shorts.flip(); // prepare for reading

        setMode(Mode.Lines);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the position of each vertex in the mesh.
     *
     * @param skeleton the skeleton to visualize (not null)
     */
    public void update(Skeleton skeleton) {
        Validate.nonNull(skeleton, "skeleton");

        FloatBuffer floats = getFloatBuffer(Type.Position);
        floats.clear(); // prepare for writing
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
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
}

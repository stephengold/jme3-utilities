/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A terrain collision shape based on Bullet's btHeightfieldTerrainShape.
 * <p>
 * This is much more efficient than a regular mesh, but it has a couple
 * limitations:
 * <ul>
 * <li>No rotation or translation.</li>
 * <li>The collision bounding box must be centered on (0,0,0) with the height
 * above and below the XZ-plane being equal on either side. If not, the whole
 * collision box is shifted vertically and objects won't collide properly.</li>
 * </ul>
 *
 * @author Brent Owens
 */
public class HeightfieldCollisionShape extends CollisionShape {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(HeightfieldCollisionShape.class.getName());

    private int heightStickWidth;
    private int heightStickLength;
    private float[] heightfieldData;
    private float heightScale;
    private float minHeight;
    private float maxHeight;
    private int upAxis;
    private boolean flipQuadEdges;
    private ByteBuffer bbuf;
//    protected FloatBuffer fbuf;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public HeightfieldCollisionShape() {
    }

    /**
     * Instantiate a new shape, based on a height map.
     *
     * @param heightmap (not null)
     */
    public HeightfieldCollisionShape(float[] heightmap) {
        createCollisionHeightfield(heightmap, Vector3f.UNIT_XYZ);
    }

    /**
     * Instantiate a new shape, based on a height map and scale vector.
     *
     * @param heightmap (not null)
     * @param scale (not null)
     */
    public HeightfieldCollisionShape(float[] heightmap, Vector3f scale) {
        createCollisionHeightfield(heightmap, scale);
    }

    private void createCollisionHeightfield(float[] heightmap,
            Vector3f worldScale) {
        this.scale.set(worldScale);
        this.heightScale = 1f; //keep at 1; we use worldScale instead to scale

        this.heightfieldData = heightmap;

        float min = heightfieldData[0];
        float max = heightfieldData[0];
        // calculate min and max height
        for (int i = 0; i < heightfieldData.length; i++) {
            if (heightfieldData[i] < min) {
                min = heightfieldData[i];
            }
            if (heightfieldData[i] > max) {
                max = heightfieldData[i];
            }
        }
        // we need to center the terrain collision box at 0,0,0 for BulletPhysics. And to do that we need to set the
        // min and max height to be equal on either side of the y axis, otherwise it gets shifted and collision is incorrect.
        if (max < 0) {
            max = -min;
        } else {
            if (Math.abs(max) > Math.abs(min)) {
                min = -max;
            } else {
                max = -min;
            }
        }
        this.minHeight = min;
        this.maxHeight = max;

        this.upAxis = PhysicsSpace.AXIS_Y;
        this.flipQuadEdges = false;

        heightStickWidth = (int) FastMath.sqrt(heightfieldData.length);
        heightStickLength = heightStickWidth;

        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape() {
        bbuf = BufferUtils.createByteBuffer(heightfieldData.length * 4);
//        fbuf = bbuf.asFloatBuffer();//FloatBuffer.wrap(heightfieldData);
//        fbuf.rewind();
//        fbuf.put(heightfieldData);
        for (int i = 0; i < heightfieldData.length; i++) {
            float f = heightfieldData[i];
            bbuf.putFloat(f);
        }
//        fbuf.rewind();
        objectId = createShape(heightStickWidth, heightStickLength, bbuf,
                heightScale, minHeight, maxHeight, upAxis, flipQuadEdges);
        logger.log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(int heightStickWidth, int heightStickLength,
            ByteBuffer heightfieldData, float heightScale, float minHeight,
            float maxHeight, int upAxis, boolean flipQuadEdges);

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(heightStickWidth, "heightStickWidth", 0);
        capsule.write(heightStickLength, "heightStickLength", 0);
        capsule.write(heightScale, "heightScale", 0);
        capsule.write(minHeight, "minHeight", 0);
        capsule.write(maxHeight, "maxHeight", 0);
        capsule.write(upAxis, "upAxis", 1);
        capsule.write(heightfieldData, "heightfieldData", new float[0]);
        capsule.write(flipQuadEdges, "flipQuadEdges", false);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        heightStickWidth = capsule.readInt("heightStickWidth", 0);
        heightStickLength = capsule.readInt("heightStickLength", 0);
        heightScale = capsule.readFloat("heightScale", 0);
        minHeight = capsule.readFloat("minHeight", 0);
        maxHeight = capsule.readFloat("maxHeight", 0);
        upAxis = capsule.readInt("upAxis", 1);
        heightfieldData = capsule.readFloatArray("heightfieldData",
                new float[0]);
        flipQuadEdges = capsule.readBoolean("flipQuadEdges", false);
        createShape();
    }
}

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
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A capsule collision shape based on Bullet's btCapsuleShapeX, btCapsuleShape,
 * or btCapsuleShapeZ.
 *
 * @author normenhansen
 */
public class CapsuleCollisionShape extends CollisionShape {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(CapsuleCollisionShape.class.getName());

    /**
     * height of the cylindrical portion (&ge;0)
     */
    private float height;
    /**
     * radius (&ge;0)
     */
    private float radius;
    /**
     * main (height) axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     */
    private int axis;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public CapsuleCollisionShape() {
    }

    /**
     * Instantiate a Y-axis capsule shape with the specified radius and height.
     *
     * @param radius the radius to use (&ge;0)
     * @param height the height (of the cylindrical portion) to use (&ge;0)
     */
    public CapsuleCollisionShape(float radius, float height) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");

        this.radius = radius;
        this.height = height;
        this.axis = PhysicsSpace.AXIS_Y;
        createShape();
    }

    /**
     * Instantiate a capsule shape around the specified main (height) axis.
     *
     * @param radius the radius to use (&ge;0)
     * @param height the height (of the cylindrical portion) to use (&ge;0)
     * @param axis which local axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public CapsuleCollisionShape(float radius, float height, int axis) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");
        Validate.inRange(axis, "axis", 0, 2);

        this.radius = radius;
        this.height = height;
        this.axis = axis;
        createShape();
    }

    /**
     * Read the radius of the capsule.
     *
     * @return radius (&ge;0)
     */
    public float getRadius() {
        assert radius >= 0f : radius;
        return radius;
    }

    /**
     * Read the height (of the cylindrical portion) of the capsule.
     *
     * @return height (&ge;0)
     */
    public float getHeight() {
        assert height >= 0f : height;
        return height;
    }

    /**
     * Determine the main (height) axis of the capsule.
     *
     * @return 0 for local X, 1 for local Y, or 2 for local Z
     */
    public int getAxis() {
        assert axis == 0 || axis == 1 || axis == 2 : axis;
        return axis;
    }

    /**
     * Alter the scaling factors of this shape. Non-uniform scaling is disabled
     * for capsule shapes.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) changes can have unexpected consequences.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=1,1,1)
     */
    @Override
    public void setScale(Vector3f scale) {
        Validate.nonNegative(scale, "scale");

        if (MyVector3f.isScaleUniform(scale)) {
            super.setScale(scale);
        } else {
            logger.log(Level.SEVERE,
                    "CapsuleCollisionShape cannot be scaled non-uniformly.");
        }
    }

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
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 1f);
        capsule.write(axis, "axis", PhysicsSpace.AXIS_Y);
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
        radius = capsule.readFloat("radius", 0.5f);
        height = capsule.readFloat("height", 1f);
        axis = capsule.readInt("axis", PhysicsSpace.AXIS_Y);
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    private void createShape() {
        assert axis == 0 || axis == 1 || axis == 2 : axis;
        assert radius >= 0f : radius;
        assert height >= 0f : height;

        objectId = createShape(axis, radius, height);
        assert objectId != 0L;
        logger.log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(int axis, float radius, float height);
}

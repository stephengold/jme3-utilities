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

import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract base class for collision shapes (such as BoxCollisionShape and
 * CapsuleCollisionShape). As suggested in the Bullet manual, collision shapes
 * can be shared between collision objects.
 *
 * @author normenhansen
 */
public abstract class CollisionShape implements Savable {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CollisionShape.class.getName());

    /**
     * Bullet id for this shape. Constructors are responsible for setting this
     * to a non-zero value. After that, the id never changes.
     */
    protected long objectId = 0;
    /**
     * scaling factors: one for each local axis (default=1,1,1)
     */
    final protected Vector3f scale = new Vector3f(1f, 1f, 1f);
    /**
     * collision margin (in world units, &ge;0, default=0)
     */
    protected float margin = 0f;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public CollisionShape() {
    }

    /**
     * Read the Bullet id of this shape.
     *
     * @return the id (not zero)
     */
    public long getObjectId() {
        return objectId;
    }

    /**
     * Alter the scaling factors of this shape.
     *
     * @param scale the desired scaling factors (not null, unaffected,
     * default=1,1,1)
     */
    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        setLocalScaling(objectId, scale);
    }

    /**
     * Access the scaling factors.
     *
     * @return the pre-existing instance (not null)
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Read the collision margin for this shape.
     *
     * @return margin distance (in world units, &ge;0)
     */
    public float getMargin() {
        return getMargin(objectId);
    }

    private native float getMargin(long objectId);

    /**
     * Alter the collision margin for this shape. Increasing the margin doesn't
     * make the shape larger, but does off its corners.
     *
     * @param margin new value (in world units, &ge;0, default=0)
     */
    public void setMargin(float margin) {
        setMargin(objectId, margin);
        this.margin = margin;
    }

    private native void setLocalScaling(long obectId, Vector3f scale);

    private native void setMargin(long objectId, float margin);

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(scale, "scale", new Vector3f(1f, 1f, 1f));
        capsule.write(getMargin(), "margin", 0f);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        Object s = capsule.readSavable("scale", new Vector3f(1f, 1f, 1f));
        this.scale.set((Vector3f) s);
        this.margin = capsule.readFloat("margin", 0f);
    }

    /**
     * Finalize this shape just before it is destroyed. Should be invoked only
     * by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.log(Level.FINE, "Finalizing CollisionShape {0}",
                Long.toHexString(objectId));
        finalizeNative(objectId);
    }

    private native void finalizeNative(long objectId);
}

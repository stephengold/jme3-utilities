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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.math.MyMath;

/**
 * A physics control to link a PhysicsGhostObject to a spatial.
 * <p>
 * The ghost object moves with the spatial it is attached to and can be used to
 * detect overlaps with other physics objects (e.g. aggro radius).
 *
 * @author normenhansen
 */
public class GhostControl
        extends PhysicsGhostObject
        implements PhysicsControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(GhostControl.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * spatial to which this control is added, or null if none
     */
    protected Spatial spatial;
    /**
     * true &rarr; enable shape scaling (to the extent the collision shape
     * supports it), false &rarr; disable shape scaling (default=false)
     */
    private boolean applyScale = false;
    /**
     * true&rarr;control is enabled, false&rarr;control is disabled
     */
    protected boolean enabled = true;
    /**
     * true&rarr;body is added to the physics space, false&rarr;not added
     */
    protected boolean added = false;
    /**
     * space to which the ghost object is (or would be) added
     */
    protected PhysicsSpace space = null;
    /**
     * true &rarr; physics-space coordinates match local transform, false &rarr;
     * physics-space coordinates match world transform
     */
    private boolean applyLocal = false;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public GhostControl() {
    }

    /**
     * Instantiate an enabled control with the specified shape.
     *
     * @param shape (not null)
     */
    public GhostControl(CollisionShape shape) {
        super(shape);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @return true if matching local coordinates, false if matching world
     * coordinates
     */
    public boolean isApplyPhysicsLocal() {
        return applyLocal;
    }

    /**
     * Test whether the collision-shape scale should match the spatial's scale.
     *
     * @return true if matching scales, otherwise false
     */
    public boolean isApplyScale() {
        return applyScale;
    }

    /**
     * Alter whether physics-space coordinates should match the spatial's local
     * coordinates.
     *
     * @param applyPhysicsLocal true&rarr;match local coordinates,
     * false&rarr;match world coordinates (default=false)
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        applyLocal = applyPhysicsLocal;
    }

    /**
     * Alter whether the collision-shape scale should match the spatial's scale.
     * CAUTION: Not all shapes can be scaled arbitrarily.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) scaling can have unintended consequences.
     *
     * @param setting true &rarr; enable shape scaling (to the extent the
     * collision shape supports it), false &rarr; disable shape scaling
     * (default=false)
     */
    public void setApplyScale(boolean setting) {
        applyScale = setting;
    }

    /**
     * Access whichever spatial translation corresponds to the physics location.
     *
     * @return the pre-existing vector (not null) TODO
     */
    private Vector3f getSpatialTranslation() {
        if (MySpatial.isIgnoringTransforms(spatial)) {
            return translateIdentity;
        } else if (applyLocal) {
            return spatial.getLocalTranslation();
        } else {
            return spatial.getWorldTranslation();
        }
    }

    /**
     * Access whichever spatial rotation corresponds to the physics rotation.
     *
     * @return the pre-existing quaternion (not null) TODO
     */
    private Quaternion getSpatialRotation() {
        if (MySpatial.isIgnoringTransforms(spatial)) {
            return rotateIdentity;
        } else if (applyLocal) {
            return spatial.getLocalRotation();
        } else {
            return spatial.getWorldRotation();
        }
    }

    /**
     * Clone this control for a different spatial. No longer used as of JME 3.1.
     *
     * @param spatial the spatial for the clone to control (or null)
     * @return a new control (not null)
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new control (not null)
     */
    @Override
    public GhostControl jmeClone() {
        try {
            GhostControl clone = (GhostControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned control into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        spatial = cloner.clone(spatial);
    }
    // *************************************************************************
    // PhysicsControl methods

    /**
     * Alter which spatial is controlled. Invoked when the control is added to
     * or removed from a spatial. Should be invoked only by a subclass or from
     * Spatial. Do not invoke directly from user code.
     *
     * @param controlledSpatial the spatial to control (or null)
     */
    @Override
    public void setSpatial(Spatial controlledSpatial) {
        if (spatial == controlledSpatial) {
            return;
        }

        spatial = controlledSpatial;
        setUserObject(controlledSpatial); // link from collision object

        if (controlledSpatial != null) {
            setPhysicsLocation(getSpatialTranslation());
            setPhysicsRotation(getSpatialRotation());
        }
    }

    /**
     * Enable or disable this control.
     * <p>
     * When the control is disabled, the ghost object is removed from physics
     * space. When the control is enabled again, the object is moved to the
     * current location of the spatial and then added to the physics space.
     *
     * @param enabled true&rarr;enable the control, false&rarr;disable it
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if (spatial != null) {
                    setPhysicsLocation(getSpatialTranslation());
                    setPhysicsRotation(getSpatialRotation());
                }
                space.addCollisionObject(this);
                added = true;
            } else if (!enabled && added) {
                space.removeCollisionObject(this);
                added = false;
            }
        }
    }

    /**
     * Test whether this control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is added to a scene. Do not invoke directly
     * from user code.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        if (!enabled) {
            return;
        }

        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
        if (applyScale) {
            Vector3f newScale = copySpatialScale(null);
            if (!collisionShape.canScale(newScale)) {
                float factor = MyMath.cubeRoot(
                        newScale.x * newScale.y * newScale.z);
                newScale.set(factor, factor, factor);
            }
            Vector3f oldScale = collisionShape.getScale(null);
            if (!oldScale.equals(newScale)
                    && collisionShape.canScale(newScale)) {
                collisionShape.setScale(newScale);
                setCollisionShape(collisionShape);
            }
        }
    }

    /**
     * Render this control. Invoked once per view port per frame, provided the
     * control is added to a scene. Should be invoked only by a subclass or by
     * the RenderManager.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }

    /**
     * If enabled, add this control's physics object to the specified physics
     * space. If not enabled, alter where the object would be added. The object
     * is removed from any other space it's currently in.
     *
     * @param newSpace where to add, or null to simply remove
     */
    @Override
    public void setPhysicsSpace(PhysicsSpace newSpace) {
        if (space == newSpace) {
            return;
        }
        if (added) {
            space.removeCollisionObject(this);
            added = false;
        }
        if (newSpace != null && isEnabled()) {
            newSpace.addCollisionObject(this);
            added = true;
        }
        /*
         * If this control isn't enabled, its physics object will be
         * added to the new space when the control becomes enabled.
         */
        space = newSpace;
    }

    /**
     * Access the physics space to which the ghost object is (or would be)
     * added.
     *
     * @return the pre-existing space, or null for none
     */
    @Override
    public PhysicsSpace getPhysicsSpace() {
        return space;
    }
    // *************************************************************************
    // PhysicsGhostObject methods

    /**
     * Serialize this control, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(applyLocal, "applyLocalPhysics", false);
        oc.write(applyScale, "applyScale", false);
        oc.write(spatial, "spatial", null);
    }

    /**
     * De-serialize this control, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        applyLocal = ic.readBoolean("applyLocalPhysics", false);
        applyScale = ic.readBoolean("applyScale", false);
    }
    // *************************************************************************
    // private methods

    /**
     * Copy whichever spatial scale corresponds to the shape scale.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the scale factor for each local axis (either storeResult or a new
     * vector, not null)
     */
    private Vector3f copySpatialScale(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        if (MySpatial.isIgnoringTransforms(spatial)) {
            result.set(scaleIdentity);
        } else if (isApplyPhysicsLocal()) {
            Vector3f scale = spatial.getLocalScale();
            result.set(scale);
        } else {
            Vector3f scale = spatial.getWorldScale();
            result.set(scale);
        }

        return result;
    }
}

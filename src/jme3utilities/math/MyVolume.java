/*
 Copyright (c) 2014-2017, Stephen Gold
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
package jme3utilities.math;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods for computing volumes of shapes. Aside from test cases, all
 * methods should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyVolume {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyVolume.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyVolume() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Compute the volume of an axis-aligned box with specified half-extents.
     *
     * @param halfExtents (not null, all components &ge;0, unaffected)
     */
    public static float boxVolume(Vector3f halfExtents) {
        Validate.nonNull(halfExtents, "half-extents");
        if (!MyVector3f.isAllNonNegative(halfExtents)) {
            logger.log(Level.SEVERE, "halfExtents={0}", halfExtents);
            throw new IllegalArgumentException(
                    "half-extents should all be non-negative");
        }

        float volume = 8f * halfExtents.x * halfExtents.y * halfExtents.z;
        return volume;
    }

    /**
     * Compute the volume of a capsule with the specified radius and height.
     *
     * @param radius (&ge;0)
     * @param height (&ge;0)
     */
    public static float capsuleVolume(float radius, float height) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");

        float halfHeight = 0.5f * height;
        Vector3f cylinderHalfExtents = new Vector3f(halfHeight, radius, radius);
        float cylinderVolume = cylinderVolume(cylinderHalfExtents);
        float volume = cylinderVolume + sphereVolume(radius);

        return volume;
    }

    /**
     * Compute the volume of a cone with the specified radius and height.
     *
     * @param radius (&ge;0)
     * @param height (&ge;0)
     */
    public static float coneVolume(float radius, float height) {
        Validate.nonNegative(radius, "radius");
        Validate.nonNegative(height, "height");

        float volume =
                FastMath.PI * FastMath.ONE_THIRD * radius * radius * height;
        return volume;
    }

    /**
     * Compute the volume of an axis-aligned cylinder with specified
     * half-extents.
     *
     * @param halfExtents (not null, all components &ge;0, unaffected)
     */
    public static float cylinderVolume(Vector3f halfExtents) {
        Validate.nonNull(halfExtents, "half-extents");
        if (!MyVector3f.isAllNonNegative(halfExtents)) {
            logger.log(Level.SEVERE, "halfExtents={0}", halfExtents);
            throw new IllegalArgumentException(
                    "half-extents should all be non-negative");
        }

        float volume =
                FastMath.TWO_PI * halfExtents.x * halfExtents.y * halfExtents.z;
        return volume;
    }

    /**
     * Compute the volume of a sphere with the specified radius.
     *
     * @param radius (&ge;0)
     */
    public static float sphereVolume(float radius) {
        Validate.nonNegative(radius, "radius");

        float volume =
                4f * FastMath.ONE_THIRD * FastMath.PI * MyMath.cube(radius);
        return volume;
    }

    /**
     * Compute the volume of a closed collision shape.
     *
     * @param shape (not null, unaffected)
     * @return volume in world units (&ge;0)
     */
    public static float volume(CollisionShape shape) {
        Vector3f scale = shape.getScale();
        if (!MyVector3f.isAllNonNegative(scale)) {
            logger.log(Level.SEVERE, "scale={0}", scale);
            throw new IllegalArgumentException(
                    "scale factors should all be non-negative");
        }
        float volume = scale.x * scale.y * scale.z;

        if (shape instanceof BoxCollisionShape) {
            BoxCollisionShape box = (BoxCollisionShape) shape;
            Vector3f halfExtents = box.getHalfExtents();
            volume *= boxVolume(halfExtents);

        } else if (shape instanceof CapsuleCollisionShape) {
            /*
             * CapsuleCollisionShape ignores scaling due to Bullet issue #178.
             */
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            float height = capsule.getHeight();
            float radius = capsule.getRadius();
            volume = capsuleVolume(radius, height);

        } else if (shape instanceof CompoundCollisionShape) {
            /*
             * CompoundCollisionShape ignores scaling.  This calculation
             * also ignores any overlaps between the children.
             */
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            volume = 0f;
            for (ChildCollisionShape child : compound.getChildren()) {
                float childVolume = volume(child.shape);
                volume += childVolume;
            }

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            float radius = cone.getRadius();
            float height = cone.getHeight();
            volume *= coneVolume(radius, height);

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            Vector3f halfExtents = cylinder.getHalfExtents();
            volume *= cylinderVolume(halfExtents);

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            float radius = sphere.getRadius();
            volume *= sphereVolume(radius);

        } else {
            logger.log(Level.SEVERE, "shape={0}", shape.getClass());
            throw new IllegalArgumentException("shape should be closed");
        }

        assert volume >= 0f : volume;
        return volume;
    }
}
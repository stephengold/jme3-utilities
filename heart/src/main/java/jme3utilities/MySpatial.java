/*
 Copyright (c) 2013-2017, Stephen Gold
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
package jme3utilities;

import com.jme3.audio.AudioNode;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.light.Light;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.VectorXZ;

/**
 * Utility methods for manipulating nodes and geometries.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MySpatial {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MySpatial.class.getName());
    /**
     * direction of the negative Y-axis
     */
    final private static Vector3f negativeYAxis = new Vector3f(0f, -1f, 0f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MySpatial() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Re-parent a spatial, keeping its world scale unchanged.
     *
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param newParent (not null)
     * @param child spatial to re-parent (not null, not orphan)
     */
    public static void adopt(Node newParent, Spatial child) {
        Validate.nonNull(newParent, "new parent");
        Node oldParent = child.getParent();
        if (oldParent == null) {
            throw new NullPointerException("child should not be an orphan");
        }

        if (newParent != oldParent) {
            float scaleFactor = oldParent.getWorldScale().x
                    / newParent.getWorldScale().x;
            Vector3f localScale = child.getLocalScale();
            localScale.multLocal(scaleFactor);
            child.setLocalScale(localScale);

            newParent.attachChild(child);
        }

        assert child.getParent() == newParent : newParent;
    }

    /**
     * Generate a single-character description of a spatial.
     *
     * @param spatial spatial to describe (unaffected)
     * @return mnemonic character
     */
    public static char describeType(Spatial spatial) {
        if (spatial instanceof AudioNode) {
            return 'a';
        } else if (spatial instanceof BatchNode) {
            return 'b';
        } else if (spatial instanceof BitmapText) {
            return 't';
        } else if (spatial instanceof ParticleEmitter) {
            return 'e';
        } else if (spatial instanceof SkeletonDebugger) {
            return 's';
        } else if (spatial instanceof TerrainQuad) {
            return 'q';
        } else if (spatial instanceof Geometry) {
            return 'g';
        } else if (spatial instanceof Node) {
            return 'n';
        }
        return '?';
    }

    /**
     * Find an animated geometry in the specified subtree of the scene graph.
     * Note: recursive!
     *
     * @param subtree where to search (not null, unaffected)
     * @return a pre-existing instance, or null if none
     */
    public static Geometry findAnimatedGeometry(Spatial subtree) {
        Geometry result = null;
        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (mesh.isAnimated()) {
                result = geometry;
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = findAnimatedGeometry(child);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Find a node's 1st child that is an assignable from the specified class.
     *
     * @param <T> subtype of {@link com.jme3.scene.Spatial}
     * @param node node to search (not null)
     * @param spatialType type of Spatial to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Spatial> T findChild(Node node,
            Class<T> spatialType) {
        for (Spatial child : node.getChildren()) {
            if (spatialType.isAssignableFrom(child.getClass())) {
                return (T) child;
            }
        }

        return null;
    }

    /**
     * Find the 1st child of the specified node with the specified name. The
     * search is case-sensitive and looks only at the node's immediate children,
     * not all its descendents.
     *
     * @param node node to search (not null)
     * @param childName name to search for (not null)
     * @return a pre-existing instance, or null if none found
     *
     * @see com.jme3.scene.Node#getChild(java.lang.String)
     */
    public static Spatial findChild(Node node, String childName) {
        Validate.nonNull(childName, "child name");

        for (Spatial child : node.getChildren()) {
            String name = child.getName();
            if (childName.equals(name)) {
                return child;
            }
        }

        return null;
    }

    /**
     * Find a spatial's 1st local light that is an assignable from the specified
     * class.
     *
     * @param <T> subtype of {@link com.jme3.light.Light}
     * @param spatial spatial to search (not null)
     * @param lightClass type of Light to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Light> T findLight(Spatial spatial,
            Class<T> lightClass) {
        Validate.nonNull(lightClass, "light class");

        for (Light light : spatial.getLocalLightList()) {
            if (lightClass.isAssignableFrom(light.getClass())) {
                return (T) light;
            }
        }

        return null;
    }

    /**
     * Find the 1st local light with the specified name.
     *
     * @param spatial spatial to search (not null, unaffected)
     * @param lightName name to search for (not null)
     * @return a pre-existing instance, or null if none found
     */
    public static Light findLight(Spatial spatial, String lightName) {
        Validate.nonNull(lightName, "light name");

        for (Light light : spatial.getLocalLightList()) {
            String name = light.getName();
            if (lightName.equals(name)) {
                return light;
            }
        }

        return null;
    }

    /**
     * Compute the map (2-D) location of a spatial.
     *
     * @param spatial spatial to locate (not null, unaffected)
     * @return new vector
     */
    public static VectorXZ getMapLocation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Vector3f worldLocation = getWorldLocation(spatial);
        VectorXZ result = new VectorXZ(worldLocation);

        return result;
    }

    /**
     * Access an object's mass.
     *
     * @param spatial object to measure (not null, unaffected)
     * @return mass in kilograms (&gt;0) or zero for a static object.
     */
    public static float getMass(Spatial spatial) {
        RigidBodyControl rigidBodyControl = spatial.getControl(
                RigidBodyControl.class);
        float mass = rigidBodyControl.getMass();

        assert mass >= 0f : mass;
        return mass;
    }

    /**
     * Find the minimum and maximum coordinates of a mesh geometry.
     *
     * @param geometry mesh geometry to measure (not null)
     * @param useWorld true &rarr; use world coordinates, false &rarr; use model
     * coordinates
     * @return array consisting of array[0]: the lowest coordinate for each axis
     * and array[1]: the highest coordinate for each axis
     */
    public static Vector3f[] findMinMaxCoords(Geometry geometry,
            boolean useWorld) {
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f[] result = new Vector3f[]{min, max};

        Mesh mesh = geometry.getMesh();
        VertexBuffer posBuf = mesh.getBuffer(VertexBuffer.Type.Position);
        if (posBuf == null) {
            return result;
        }

        FloatBuffer posBuffer = (FloatBuffer) posBuf.getData();
        posBuffer.rewind();
        int numFloats = posBuffer.remaining();
        int numVertices = mesh.getVertexCount();
        assert numFloats == 3 * numVertices : numFloats;

        Vector3f modelLocation = new Vector3f();
        Vector3f location = new Vector3f();

        for (int vertexIndex = 0; vertexIndex < numVertices; vertexIndex++) {
            modelLocation.x = posBuffer.get();
            modelLocation.y = posBuffer.get();
            modelLocation.z = posBuffer.get();
            if (useWorld) {
                geometry.localToWorld(modelLocation, location);
            } else {
                location.set(modelLocation);
            }
            MyVector3f.accumulateMinima(min, location);
            MyVector3f.accumulateMaxima(max, location);
        }

        return result;
    }

    /**
     * Find the minimum and maximum coordinates in a subtree of the scene graph.
     * Note: recursive!
     *
     * @param spatial what to measure (not null)
     * @param useWorld true &rarr; use world coordinates, false &rarr; use model
     * coordinates
     * @return array consisting of array[0]: the lowest coordinate for each axis
     * and array[1]: the highest coordinate for each axis
     */
    public static Vector3f[] findMinMaxCoords(Spatial spatial,
            boolean useWorld) {
        Vector3f[] result;
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = findMinMaxCoords(geometry, useWorld);

        } else if (spatial instanceof Node) {
            Vector3f maxima = new Vector3f(Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            Vector3f minima = new Vector3f(Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            result = new Vector3f[]{minima, maxima};
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                Vector3f[] childMm = findMinMaxCoords(child, useWorld);
                MyVector3f.accumulateMinima(minima, childMm[0]);
                MyVector3f.accumulateMaxima(maxima, childMm[1]);
            }

        } else {
            throw new IllegalArgumentException(
                    "spatial should be a geometry or a node");
        }

        return result;
    }

    /**
     * Calculate the world scale factor for a uniformly scaled spatial.
     *
     * @param spatial spatial to measure (not null, unaffected)
     * @return scale factor (&gt;0)
     */
    public static float getUniformScale(Spatial spatial) {
        Vector3f worldScale = spatial.getWorldScale();

        assert worldScale.x > 0f : worldScale.x;
        assert worldScale.y > 0f : worldScale.y;
        assert worldScale.z > 0f : worldScale.z;
        assert worldScale.x == worldScale.y;
        assert worldScale.x == worldScale.z;

        return worldScale.x;
    }

    /**
     * Calculate the world location of a spatial.
     *
     * @param spatial spatial to locate (not null, unaffected)
     * @return new vector
     */
    public static Vector3f getWorldLocation(Spatial spatial) {
        /*
         * Access the rigid body, if any.
         */
        RigidBodyControl rigidBodyControl = spatial.getControl(
                RigidBodyControl.class);
        Vector3f location;
        if (rigidBodyControl != null) {
            location = rigidBodyControl.getPhysicsLocation();
        } else {
            location = spatial.getWorldTranslation();
        }

        return location.clone();
    }

    /**
     * Calculate the world orientation of a spatial.
     *
     * @param spatial spatial to orient (not null, unaffected)
     * @return new vector
     */
    public static Quaternion getWorldOrientation(Spatial spatial) {
        /*
         * Access the rigid body, if any.
         */
        RigidBodyControl rigidBodyControl = spatial.getControl(
                RigidBodyControl.class);
        Quaternion orientation;
        if (rigidBodyControl != null) {
            orientation = rigidBodyControl.getPhysicsRotation();
        } else {
            orientation = spatial.getWorldRotation();
        }
        Quaternion result = orientation.clone();
        result.normalizeLocal();

        return result;
    }

    /**
     * Calculate the world elevation of a horizontal surface.
     *
     * @param geometry surface to measure (not null)
     * @return elevation of the surface (in world coordinates)
     */
    public static float getYLevel(Geometry geometry) {
        Validate.nonNull(geometry, "geometry");

        Vector3f minMax[] = findMinMaxCoords(geometry, true);
        float result = minMax[0].y;

        assert result == minMax[1].y : minMax[0];
        return result;
    }

    /**
     * Test whether a spatial has a specific light in its local list.
     *
     * @param spatial spatial to search (not null, unaffected)
     * @param light light to search for (not null)
     * @return true if found, false if not found
     */
    public static boolean hasLight(Spatial spatial, Light light) {
        Validate.nonNull(light, "light");

        for (Light l : spatial.getLocalLightList()) {
            if (l == light) {
                return true;
            }
        }

        return false;
    }

    /**
     * Construct the inverse of a spatial's world orientation, the quaternion
     * that undoes its rotation.
     *
     * @param spatial spatial to analyze (not null, unaffected)
     * @return new instance
     */
    public static Quaternion inverseOrientation(Spatial spatial) {
        Quaternion forward = spatial.getWorldRotation();
        Quaternion result = forward.inverse();

        return result;
    }

    /**
     * Test whether a spatial is an orphan (has no parent node).
     *
     * @param spatial spatial to test (not null, unaffected)
     * @return true if the spatial is an orphan, otherwise false
     */
    public static boolean isOrphan(Spatial spatial) {
        Node parent = spatial.getParent();
        return parent == null;
    }

    /**
     * Test whether a spatial is physics-controlled.
     *
     * @param spatial spatial to test (not null, unaffected)
     * @return true if the spatial is controlled by physics, otherwise false
     */
    public static boolean isPhysical(Spatial spatial) {
        Object rigidBodyControl = spatial.getControl(RigidBodyControl.class);
        boolean result = rigidBodyControl != null;

        return result;
    }

    /**
     * Move (translate) an object in the world coordinate system.
     *
     * @param spatial object to move (not null)
     * @param offset world translation (in world units, not null)
     */
    public static void moveWorld(Spatial spatial, Vector3f offset) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(offset, "offset");

        Vector3f location = getWorldLocation(spatial);
        location.addLocal(offset);
        setWorldLocation(spatial, location);

        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                moveChildWorld(child, offset);
            }
        }
    }

    /**
     * Move (translate) a object's child in the world coordinate system. NOTE
     * recursive
     *
     * @param spatial object to move (not null)
     * @param offset world translation (in world units, not null)
     */
    public static void moveChildWorld(Spatial spatial, Vector3f offset) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(offset, "offset");

        if (isPhysical(spatial)) {
            Vector3f location = getWorldLocation(spatial);
            location.addLocal(offset);
            setWorldLocation(spatial, location);
        }

        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                moveChildWorld(child, offset);
            }
        }
    }

    /**
     * Remove all controls from the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree (not null)
     */
    public static void removeAllControls(Spatial subtree) {
        while (subtree.getNumControls() > 0) {
            Control control = subtree.getControl(0);
            subtree.removeControl(control);
        }
        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                removeAllControls(child);
            }
        }
    }

    /**
     * Turn (rotate) a child object around an axis.
     *
     * @param spatial object to rotate (not null)
     * @param center world coordinates of the point to rotate around (not null)
     * @param rotation (not null)
     */
    public static void rotateChild(Spatial spatial, Vector3f center,
            Quaternion rotation) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(center, "vector");
        Validate.nonNull(rotation, "rotaion");

        if (isPhysical(spatial)) {
            Vector3f location = getWorldLocation(spatial);
            Vector3f offset = location.subtract(center);
            offset = rotation.mult(offset);
            location = center.add(offset);
            setWorldLocation(spatial, location);

            Quaternion orientation = getWorldOrientation(spatial);
            orientation = rotation.mult(orientation);
            setWorldOrientation(spatial, orientation);
        }
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                rotateChild(child, center, rotation);
            }
        }
    }

    /**
     * Turn (rotate) a physical object around an axis.
     *
     * @param spatial object to rotate (not null)
     * @param center world coordinates of the point to rotate around (not null)
     * @param rotation (not null)
     */
    public static void rotateObject(Spatial spatial, Vector3f center,
            Quaternion rotation) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(center, "center");

        Vector3f location = getWorldLocation(spatial);
        Vector3f offset = location.subtract(center);
        offset = rotation.mult(offset);
        location = center.add(offset);
        setWorldLocation(spatial, location);

        Quaternion orientation = getWorldOrientation(spatial);
        orientation = rotation.mult(orientation);
        setWorldOrientation(spatial, orientation);

        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                rotateChild(child, center, rotation);
            }
        }
    }

    /**
     * Turn (rotate) a physical object around its world Y-axis.
     *
     * @param spatial object to rotate (not null)
     * @param angle clockwise rotation angle (in radians)
     */
    public static void rotateY(Spatial spatial, float angle) {
        Validate.nonNull(spatial, "spatial");

        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(angle, negativeYAxis);
        Vector3f center = getWorldLocation(spatial);
        rotateObject(spatial, center, rotation);
    }

    /**
     * Alter the world location of a spatial.
     *
     * @param spatial spatial to relocate (not null)
     * @param newLocation desired world location (not null, unaffected)
     */
    public static void setWorldLocation(Spatial spatial, Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");

        Spatial parent = spatial.getParent();
        Vector3f centerLocal;
        // TODO check for spatial.isIgnoreTransform()
        if (parent != null) {
            centerLocal = parent.worldToLocal(newLocation, null);
        } else {
            centerLocal = newLocation.clone();
        }
        /*
         * Apply to the spatial.
         */
        spatial.setLocalTranslation(centerLocal);
        /*
         * Apply to the physical object, if any.
         */
        RigidBodyControl rigidBodyControl = spatial.getControl(
                RigidBodyControl.class);
        if (rigidBodyControl != null) {
            rigidBodyControl.setPhysicsLocation(newLocation.clone());
        }
    }

    /**
     * Alter the world orientation of a spatial.
     *
     * @param spatial spatial to reorient (not null)
     * @param newOrientation desired world orientation (not null, unaffected)
     */
    public static void setWorldOrientation(Spatial spatial,
            Quaternion newOrientation) {
        Spatial parent = spatial.getParent();
        Quaternion localRotation;
        if (parent != null) {
            localRotation = inverseOrientation(parent);
            localRotation.multLocal(newOrientation);
            localRotation.normalizeLocal();
        } else {
            localRotation = newOrientation;
        }
        /*
         * Apply to the spatial.
         */
        spatial.setLocalRotation(localRotation);
        /*
         * Apply to the physical object, if any.
         */
        RigidBodyControl rigidBodyControl = spatial.getControl(
                RigidBodyControl.class);
        if (rigidBodyControl != null) {
            rigidBodyControl.setPhysicsRotation(newOrientation);
        }
    }

    /**
     * Alter the world scaling of a spatial.
     *
     * @param spatial spatial to rescale (not null)
     * @param scale desired world scale (&gt;0)
     */
    public static void setWorldScale(Spatial spatial, float scale) {
        Validate.positive(scale, "scale");

        Spatial parent = spatial.getParent();
        if (parent == null) {
            spatial.setLocalScale(scale);
            return;
        }
        float parentScale = MySpatial.getUniformScale(parent);
        assert parentScale != 0f : parentScale;
        float localScale = scale / parentScale;
        spatial.setLocalScale(localScale);
    }
}

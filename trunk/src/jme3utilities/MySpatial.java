/*
 Copyright (c) 2013-2014, Stephen Gold
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

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.light.Light;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.math.VectorXZ;

/**
 * Utility methods for generic operations on nodes and geometries. Aside from
 * test cases, all methods should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MySpatial
        extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MySpatial.class.getName());
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
     * Generate a one-letter description of a spatial.
     *
     * @param spatial spatial to describe
     */
    public static char describeType(Spatial spatial) {
        if (spatial instanceof AudioNode) {
            return 'a';
        } else if (spatial instanceof BatchNode) {
            return 'b';
        } else if (spatial instanceof ParticleEmitter) {
            return 'e';
        } else if (spatial instanceof SkeletonDebugger) {
            return 's';
        } else if (spatial instanceof TerrainQuad) {
            return 't';
        } else if (spatial instanceof Geometry) {
            return 'g';
        } else if (spatial instanceof Node) {
            return 'n';
        }
        return '?';
    }

    /**
     * Find a node's 1st child which is an assignable from a specified class.
     *
     * @param node node to search (not null)
     * @param spatialType subclass of Spatial to search for
     * @return 1st matching child, or null if none found
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
     * Find a local light with a specified name.
     *
     * @param spatial spatial to search (not null)
     * @param lightName (not null)
     * @return 1st matching light, or null if none found
     */
    public static Light findLight(Spatial spatial, String lightName) {
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
     * @param spatial spatial to locate (not null)
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
     * @param spatial object to measure (not null)
     * @return mass in kilograms (&gt;0) or zero for a static object.
     */
    public static float getMass(Spatial spatial) {
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
        float mass = rigidBodyControl.getMass();

        assert mass >= 0f : mass;
        return mass;
    }

    /**
     * Compute the minimum and maximum elevations of a mesh geometry.
     *
     * @param geometry mesh geometry to measure (not null)
     * @return array consisting of array[0]: the lowest world Y-coordinate (in
     * world units) and array[1]: the highest world Y-coordinate (in world
     * units)
     */
    public static float[] findMinMaxHeights(Geometry geometry) {
        Vector3f vertexLocal[] = new Vector3f[3];
        for (int j = 0; j < 3; j++) {
            vertexLocal[j] = new Vector3f();
        }
        Vector3f worldLocation = new Vector3f();

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        Mesh mesh = geometry.getMesh();
        assert mesh.getMode() == Mesh.Mode.Triangles : mesh.getMode();
        int count = mesh.getTriangleCount();
        for (int triangleIndex = 0; triangleIndex < count; triangleIndex++) {
            /*
             * Get the vertex locations for a triangle in the mesh.
             */
            mesh.getTriangle(triangleIndex, vertexLocal[0], vertexLocal[1],
                    vertexLocal[2]);
            /*
             * Compare with lowest and highest world elevations so far.
             */
            for (int j = 0; j < 3; j++) {
                geometry.localToWorld(vertexLocal[j], worldLocation);
                float y = worldLocation.y;
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        /*
         * Create the result array.
         */
        float[] minMax = {minY, maxY};
        return minMax;
    }

    /**
     * Find the maximum elevation in a subtree of the scene graph. Note:
     * recursive!
     *
     * @param spatial root of the subtree (not null)
     * @return world Y-coordinate
     */
    public static float getMaxY(Spatial spatial) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            float[] minMax = findMinMaxHeights(geometry);
            return minMax[1];
        }
        if (!(spatial instanceof Node)) {
            throw new IllegalArgumentException(
                    "spatial should be a geometry or a node");
        }

        Node node = (Node) spatial;
        float result = -Float.MAX_VALUE;
        for (Spatial child : node.getChildren()) {
            float childMax = getMaxY(child);
            if (childMax > result) {
                result = childMax;
            }
        }
        return result;
    }

    /**
     * Find the minimum elevation in a subtree of the scene graph. Note:
     * recursive!
     *
     * @param spatial root of the subtree (not null)
     * @return world Y-coordinate (in world units)
     */
    public static float getMinY(Spatial spatial) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            float[] minMax = findMinMaxHeights(geometry);
            return minMax[0];
        }
        if (!(spatial instanceof Node)) {
            throw new IllegalArgumentException(
                    "spatial should be a geometry or a node");
        }

        Node node = (Node) spatial;
        float result = Float.MAX_VALUE;
        for (Spatial child : node.getChildren()) {
            float childMin = getMinY(child);
            if (childMin < result) {
                result = childMin;
            }
        }
        return result;
    }

    /**
     * Get the world scale factor for a uniformly scaled spatial.
     *
     * @param spatial spatial to measure (not null)
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
     * Copy the world location of a spatial.
     *
     * @param spatial spatial to locate (not null)
     * @return new vector
     */
    public static Vector3f getWorldLocation(Spatial spatial) {
        /*
         * Get the physical object, if any.
         */
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
        Vector3f location;
        if (rigidBodyControl != null) {
            location = rigidBodyControl.getPhysicsLocation();
        } else {
            location = spatial.getWorldTranslation();
        }
        return location.clone();
    }

    /**
     * Copy the world orientation of a spatial.
     *
     * @param spatial spatial to orient (not null)
     * @return new vector
     */
    public static Quaternion getWorldOrientation(Spatial spatial) {
        /*
         * Get the physical object, if any.
         */
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
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
     * Compute the world elevation of a horizontal surface.
     *
     * @param geometry surface to measure (not null)
     * @return world elevation of the surface (in world units)
     */
    public static float getYLevel(Geometry geometry) {
        Validate.nonNull(geometry, "geometry");

        float minMax[] = findMinMaxHeights(geometry);
        assert minMax[0] == minMax[1] : minMax[0];
        return minMax[0];
    }

    /**
     * Construct the inverse of a spatial's world orientation, the quaternion
     * which undoes its rotation.
     *
     * @param spatial spatial to analyze (not null)
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
     * @param spatial spatial to test (not null)
     * @return true if the spatial is an orphan, otherwise false
     */
    public static boolean isOrphan(Spatial spatial) {
        Node parent = spatial.getParent();
        return parent == null;
    }

    /**
     * Test whether a spatial is physics-controlled.
     *
     * @param spatial spatial to test (not null)
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
     * Turn (rotate) a physical object around its Y-axis.
     *
     * @param spatial object to rotate (not null)
     * @param angle clockwise rotation angle (in radians)
     */
    public static void rotateY(Spatial spatial, float angle) {
        Validate.nonNull(spatial, "spatial");

        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(-angle, Vector3f.UNIT_Y);
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
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
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
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
        if (rigidBodyControl != null) {
            rigidBodyControl.setPhysicsRotation(newOrientation);
        }
    }

    /**
     * Alter the world scaling of a spatial.
     *
     * @param spatial spatial to rescale (not null, not orphan)
     * @param scale desired world scale (&gt;0)
     */
    public static void setWorldScale(Spatial spatial, float scale) {
        Validate.positive(scale, "scale");

        Spatial parent = spatial.getParent();
        float parentScale = MySpatial.getUniformScale(parent);
        assert parentScale != 0f : parentScale;
        float localScale = scale / parentScale;
        spatial.setLocalScale(localScale);
    }
    // *************************************************************************
    // test cases

    /**
     * Simple application to test the MySpatial class.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.WARNING);
        MySpatial application = new MySpatial();
        application.setShowSettings(false);
        application.start();
    }

    @Override
    public void simpleInitApp() {
        logger.setLevel(Level.INFO);
        System.out.print("Test results for class MySpatial:\n\n");

        Node parent = new Node("parent");
        rootNode.attachChild(parent);
        parent.setLocalScale(new Vector3f(6f, 5f, 4f));
        parent.setLocalRotation(new Quaternion(3f, 5f, 2f, 4f));
        parent.setLocalTranslation(new Vector3f(-1f, 2f, 3f));
        Node child = new Node("child");
        parent.attachChild(child);

        Vector3f loc = new Vector3f(9f, 7f, 8f);
        System.out.printf("loc = %s%n", loc);
        setWorldLocation(child, loc);
        Vector3f loc2 = getWorldLocation(child);
        System.out.printf("loc2 = %s%n", loc2);

        parent.setLocalScale(new Vector3f(2f, 2f, 2f));

        Quaternion rot = new Quaternion(3f, 1f, 4f, 15f);
        rot.normalizeLocal();
        System.out.printf("rot = %s%n", rot);
        setWorldOrientation(child, rot);
        Quaternion rot2 = getWorldOrientation(child);
        rot2.normalizeLocal();
        System.out.printf("rot2 = %s%n", rot2);

        stop();
    }
}
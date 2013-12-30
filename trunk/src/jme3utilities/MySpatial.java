/*
 Copyright (c) 2013, Stephen Gold
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

import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for generic operations on nodes and spatials. Aside from test
 * cases, all methods should be public and static.
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
     *
     * Re-parent a spatial, keeping its world scale unchanged.
     *
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param newParent (not null)
     * @param child which spatial (not null, not orphan)
     */
    public static void adopt(Node newParent, Spatial child) {
        if (newParent == null) {
            throw new NullPointerException("new parent should not be null");
        }
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
     * Find a node's first child which is an assignable from a given class.
     *
     * @param node which node
     * @param spatialType which superclass of Spatial to search for
     * @return the first matching child, or null if none exists
     */
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
     * Calculate the map (2-D) location of a spatial.
     *
     * @param spatial which spatial to locate (not null)
     * @return a new vector
     */
    public static VectorXZ getMapLocation(Spatial spatial) {
        if (spatial == null) {
            throw new NullPointerException("spatial should not be null");
        }

        Vector3f worldLocation = getWorldLocation(spatial);
        VectorXZ result = new VectorXZ(worldLocation);
        return result;
    }

    /**
     * Access an object's mass.
     *
     * @param spatial which object to measure (not null)
     * @return mass in kilograms (>0) or zero for a static object.
     */
    public static float getMass(Spatial spatial) {
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
        float mass = rigidBodyControl.getMass();
        return mass;
    }

    /**
     * Find the maximum elevation in a subtree of the scene graph. Note:
     * recursive!
     *
     * @param spatial root of the subtree (not null)
     * @return world Y-coordinate
     */
    public static float getMaxY(Spatial spatial) {
        if (spatial == null) {
            throw new NullPointerException("spatial should not be null");
        }

        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            float[] minMax = Misc.findMinMaxHeights(geometry);
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
            float[] minMax = Misc.findMinMaxHeights(geometry);
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
     * @param spatial which spatial to measure (not null)
     * @return scale factor (>0)
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
     * @param spatial which spatial to locate (not null)
     * @return a new vector
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
     * @param spatial which spatial to orient (not null)
     * @return a new vector
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
     * Construct the inverse of a spatial's orientation, the quaternion which
     * undoes its rotation.
     *
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param spatial which spatial (not null)
     * @return a new instance
     */
    public static Quaternion inverseOrientation(Spatial spatial) {
        Vector3f origin = spatial.worldToLocal(Vector3f.ZERO, null);

        Vector3f xAxis = spatial.worldToLocal(Vector3f.UNIT_X, null);
        xAxis.subtractLocal(origin);
        xAxis.normalizeLocal();

        Vector3f yAxis = spatial.worldToLocal(Vector3f.UNIT_Y, null);
        yAxis.subtractLocal(origin);
        yAxis.normalizeLocal();

        Vector3f zAxis = spatial.worldToLocal(Vector3f.UNIT_Z, null);
        zAxis.subtractLocal(origin);
        zAxis.normalizeLocal();

        Quaternion result = new Quaternion();
        result.fromAxes(xAxis, yAxis, zAxis);
        return result;
    }

    /**
     * Test whether a spatial is an orphan (has no parent node).
     *
     * @param spatial which spatial to test (not null)
     * @return true if the spatial is an orphan, otherwise false
     */
    public static boolean isOrphan(Spatial spatial) {
        Node parent = spatial.getParent();
        return parent == null;
    }

    /**
     * Test whether a spatial is physics-controlled.
     *
     * @param spatial which spatial (not null)
     * @return true if the spatial is controlled by physics, otherwise false
     */
    public static boolean isPhysical(Spatial spatial) {
        RigidBodyControl rigidBodyControl =
                spatial.getControl(RigidBodyControl.class);
        boolean result = rigidBodyControl != null;
        return result;
    }

    /**
     * List all animations for a particular unit.
     *
     * @param spatial which unit (or null)
     * @return a new sorted set
     */
    public static Collection<String> listAnimations(Spatial spatial) {
        AnimControl control = spatial.getControl(AnimControl.class);
        Collection<String> animationNames = control.getAnimationNames();
        Collection<String> itemSet = new TreeSet<>();
        itemSet.addAll(animationNames);
        return itemSet;
    }

    /**
     * Move (translate) an object in the world coordinate system.
     *
     * @param spatial which object to move (not null)
     * @param offset world translation (in meters, not null)
     */
    public static void moveWorld(Spatial spatial, Vector3f offset) {
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
     * @param spatial which object to move (not null)
     * @param offset world translation (in meters, not null)
     */
    public static void moveChildWorld(Spatial spatial, Vector3f offset) {
        if (spatial == null) {
            throw new NullPointerException("spatial should not be null");
        }

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
     * @param spatial which object to rotate (not null)
     * @param center world coordinates of the point to rotate around (not null)
     * @param rotation (not null)
     */
    public static void rotateChild(Spatial spatial, Vector3f center,
            Quaternion rotation) {
        if (spatial == null) {
            throw new NullPointerException("spatial should not be null");
        }
        if (center == null) {
            throw new NullPointerException("vector should not be null");
        }
        if (rotation == null) {
            throw new NullPointerException("rotation should not be null");
        }

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
     * @param spatial which object to rotate (not null)
     * @param center world coordinates of the point to rotate around (not null)
     * @param rotation (not null)
     */
    public static void rotateObject(Spatial spatial, Vector3f center,
            Quaternion rotation) {
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
     * @param spatial which object to rotate (not null)
     * @param angle clockwise rotation angle (in radians)
     */
    public static void rotateY(Spatial spatial, float angle) {
        if (spatial == null) {
            throw new NullPointerException("spatial should not be null");
        }

        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(-angle, Vector3f.UNIT_Y);
        Vector3f center = getWorldLocation(spatial);
        rotateObject(spatial, center, rotation);
    }

    /**
     * Alter the world location of a spatial.
     *
     * @param spatial which spatial (not null)
     * @param newLocation desired world location (not null, unaffected)
     */
    public static void setWorldLocation(Spatial spatial, Vector3f newLocation) {
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
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param spatial which spatial (not null)
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
            localRotation = newOrientation.clone();
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
            rigidBodyControl.setPhysicsRotation(newOrientation.clone());
        }
    }

    /**
     * Alter the world scaling of a spatial.
     *
     * NOTE: This method may yield incorrect results in the presence of
     * non-uniform scaling.
     *
     * @param spatial which spatial (not null, not orphan)
     * @param scale desired world scale (>0)
     */
    public static void setWorldScale(Spatial spatial, float scale) {
        if (scale <= 0f) {
            logger.log(Level.SEVERE, "scale={0}", scale);
            throw new IllegalArgumentException("scale should be positive");
        }

        Spatial parent = spatial.getParent();
        float parentScale = parent.getWorldScale().x;
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
        Misc.setLoggingLevels(Level.SEVERE);
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
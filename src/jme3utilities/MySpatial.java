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
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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

        stop();
    }
}
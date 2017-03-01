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
package jme3utilities.nifty.test;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.NameGenerator;
import jme3utilities.SimpleAppState;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.noise.Noise;

/**
 * App state to animate a seemingly-endless field of stars. Used by
 * TestBindScreen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class StarfieldState extends SimpleAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            StarfieldState.class.getName());

    /**
     * color for objects (white)
     */
    final private static ColorRGBA objectColor = new ColorRGBA(1f, 1f, 1f, 1f);
    // *************************************************************************
    // fields

    /**
     * apparent rate of motion in the direction the camera is pointed (in world
     * units/second, may be negative)
     */
    private float forwardVelocity = 0f;
    /**
     * rate of camera rotation (in radians/second, may be negative)
     */
    private float rotationRate = 0f;
    /**
     * distance from the origin to the edge of the cubical object zone (in world
     * units, &gt;0, set by {@link #initializeObjects()})
     */
    private float zoneRadius;
    /**
     * number of moving objects to animate (&gt;0)
     */
    final private int numObjects;
    /**
     * scene node for the backdrop (populated by {@link #initializeBackdrop()})
     */
    final private Node backdrop = new Node("backdrop node");
    /**
     * scene node for moving objects (populated by {@link #initializeObjects()})
     */
    final private Node objects = new Node("objects node");
    /**
     * name of the backdrop star map in the Textures/skies/star-maps asset
     * folder (not null, not empty, set by constructor)
     */
    final private String backdropName;
    /**
     * local axis about which the camera rotates (not zero)
     */
    final private Vector3f rotationAxis = new Vector3f(1f, 0f, 0f);
    // *************************************************************************
    // constructor

    /**
     * Instantiate an uninitialized state with the specified backdrop and number
     * of objects.
     *
     * @param enabled true &rarr; enabled, false &rarr; disabled
     * @param numObjects number of objects to animate (&gt;0)
     * @param backdropName name of the star map in the Textures/skies/star-maps
     * asset folder (not null, not empty)
     */
    StarfieldState(boolean enabled, int numObjects, String backdropName) {
        super(enabled);
        Validate.positive(numObjects, "number of objects");
        Validate.nonEmpty(backdropName, "name of backdrop");

        this.numObjects = numObjects;
        this.backdropName = backdropName;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the distance from the origin to the edge of the object zone. The
     * state must be initialized.
     *
     * @return distance (in world units, &gt;0)
     */
    public float getZoneRadius() {
        if (!isInitialized()) {
            throw new IllegalStateException("not yet initialized");
        }

        return zoneRadius;
    }

    /**
     * Alter the apparent rate of movement in the direction the camera is
     * pointed
     *
     * @param velocity (in world units/second, may be negative)
     */
    public void setForwardVelocity(float velocity) {
        forwardVelocity = velocity;
    }

    /**
     * Alter the rotation of the camera.
     *
     * @param rate new rotation rate (in radians/second, may be negative)
     * @param axis new axis (not zero, unaffected)
     */
    public void setRotation(float rate, Vector3f axis) {
        Validate.nonZero(axis, "axis");

        rotationRate = rate;
        rotationAxis.set(axis);
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Initialize this application.
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);
        /*
         * Relocate camera to origin.
         */
        cam.setLocation(new Vector3f(0f, 0f, 0f));
        /*
         * The objects zone extends halfway to the far 
         * plane of the view frustrum.
         */
        zoneRadius = 0.5f * cam.getFrustumFar();
        /*
         * Initialize the scene graph.
         */
        initializeBackdrop();
        initializeObjects();
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param updateInterval time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float updateInterval) {
        super.update(updateInterval);

        assert MyVector3f.isZero(cam.getLocation());
        /*
         * Scale the backdrop so that its furthest geometries are
         * between the near and far planes of the view frustum.
         */
        float far = cam.getFrustumFar();
        float near = cam.getFrustumNear();
        float cubeScale = (near + far) / 2f;
        backdrop.setLocalScale(cubeScale);

        if (rotationRate != 0f) {
            /*
             * Rotate the camera.
             */
            float angle = rotationRate * updateInterval;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(angle, rotationAxis);

            Quaternion oldOrientation = cam.getRotation();
            Quaternion newOrientation = oldOrientation.mult(rotation);
            cam.setRotation(newOrientation);
        }

        if (forwardVelocity != 0f) {
            /*
             * Create an illusion of forward motion by moving all the
             * objects in the direction opposite where the camera is pointing.
             * 
             * Objects which leave the zone wrap around to the other side,
             * giving the illusion of an endless supply of objects.
             */
            float forwardDistance = forwardVelocity * updateInterval;
            Vector3f offset = cam.getDirection().mult(-forwardDistance);

            for (Spatial object : objects.getChildren()) {
                Vector3f location = MySpatial.getWorldLocation(object);
                location.addLocal(offset);
                location.x = wrapToZone(location.x);
                location.y = wrapToZone(location.y);
                location.z = wrapToZone(location.z);
                MySpatial.setWorldLocation(object, location);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create and attach the backdrop.
     */
    private void initializeBackdrop() {
        Spatial cubeMap = MyAsset.createStarMapQuads(
                assetManager, backdropName);
        backdrop.attachChild(cubeMap);
        rootNode.attachChild(backdrop);
    }

    /**
     * Create and attach moving objects.
     */
    private void initializeObjects() {
        rootNode.attachChild(objects);
        /*
         * The moving objects are small white spheres, distributed 
         * pseudo-randomly  throughout a cubical zone centered on the origin
         */
        NameGenerator names = new NameGenerator();
        Material white = MyAsset.createUnshadedMaterial(
                assetManager, objectColor);
        float objectRadius = 0.01f * zoneRadius
                / MyMath.cubeRoot((float) numObjects);
        Mesh objectMesh = new Sphere(3, 10, objectRadius);

        for (int i = 0; i < numObjects; i++) {
            String name = names.unique("obj");
            Geometry object = new Geometry(name, objectMesh);
            object.setMaterial(white);
            objects.attachChild(object);
            float x = zoneRadius * (2f * Noise.nextFloat() - 1f);
            float y = zoneRadius * (2f * Noise.nextFloat() - 1f);
            float z = zoneRadius * (2f * Noise.nextFloat() - 1f);
            Vector3f location = new Vector3f(x, y, z);
            MySpatial.setWorldLocation(object, location);
        }
    }

    /**
     * Wrap an object coordinate to {-zoneRadius, +zoneRadius} in order to keep
     * it in a cubical zone centered on the origin.
     *
     * @param input object coordinate
     * @return wrapped coordinate
     */
    private float wrapToZone(float coordinate) {
        if (Math.abs(coordinate) <= zoneRadius) {
            return coordinate;
        }
        float result = MyMath.modulo(coordinate + zoneRadius, 2f * zoneRadius)
                - zoneRadius;

        assert Math.abs(result) <= zoneRadius : result;
        return result;
    }
}

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

import com.jme3.app.StatsAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.NameGenerator;
import jme3utilities.math.MyMath;
import jme3utilities.math.noise.Noise;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.MessageDisplay;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.sky.DomeMesh;
import jme3utilities.ui.InputMode;

/**
 * GUI application for testing/demonstrating the hotkey bindings editor. The
 * application's main entry point is here.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestBindScreen extends GuiApplication {
    // *************************************************************************
    // constants

    /**
     * number of objects to animate (&gt;0)
     */
    final private static int numObjects = 20_000;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestBindScreen.class.getName());
    /**
     * action strings
     */
    final private static String asFeels = "express feelings";
    final private static String asFire = "launch torpedo";
    final private static String asForward = "accelerate forward";
    final private static String asHail = "open hailing frequencies";
    final private static String asHelp = "edit bindings";
    final private static String asLowerShields = "shields down";
    final private static String asPitchDown = "pitch down";
    final private static String asPitchUp = "pitch up";
    final private static String asRaiseShields = "shields up";
    final private static String asReverse = "accelerate reverse";
    final private static String asRollLeft = "roll left";
    final private static String asRollRight = "roll right";
    final private static String asStopAll = "stop all";
    final private static String asStopRotation = "stop rotation";
    final private static String asYawLeft = "yaw left";
    final private static String asYawRight = "yaw right";
    // *************************************************************************
    // fields

    /**
     * hotkey bindings editor
     */
    private BindScreen bindScreen;
    /**
     * rate of movement in the direction the camera is pointed
     */
    private int warpFactor;
    /**
     * rate of rotation (in radians/sec)
     */
    private float rotationRate;
    /**
     * distance from the origin to the edge of the object zone
     */
    private float zoneRadius;
    /**
     * heads-up display for messages
     */
    private MessageDisplay messageHud;
    /**
     * scene node for the backdrop
     */
    private Node backdrop;
    /**
     * scene node for objects
     */
    private Node objects;
    /**
     * axis about which the camera rotates
     */
    private Vector3f rotationAxis;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the test harness.
     *
     * @param unused array of command-line arguments (not null)
     */
    public static void main(String[] unused) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TestBindScreen application = new TestBindScreen();
        application.start();
        /*
         * ... and onward to TestBindScreen.guiInitializeApplication()!
         */
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        /*
         * Log the jME3-utilities version string.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));
        /*
         * Disable flyCam and stats display.
         */
        flyCam.setEnabled(false);
        StatsAppState sas = stateManager.getState(StatsAppState.class);
        sas.setDisplayFps(false);
        sas.setDisplayStatView(false);
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
        /*
         * Initialize motion:  stationary.
         */
        warpFactor = 0;
        rotationRate = 0f;
        rotationAxis = new Vector3f();
        /*
         * Initialize the input mode for playing the game.
         */
        initializeBindings();
        /*
         * Attach a HUD for messages.
         */
        messageHud = new MessageDisplay();
        messageHud.setListener(this);
        boolean success = stateManager.attach(messageHud);
        assert success;
        messageHud.addLine("Press the H key to view/edit hotkey bindings.");
        /*
         * Attach a screen controller for the hotkey bindings editor.
         */
        bindScreen = new BindScreen();
        success = stateManager.attach(bindScreen);
        assert success;
    }

    /**
     * Callback invoked once per frame.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float updateInterval) {
        super.simpleUpdate(updateInterval);

        if (rotationRate != 0f) {
            /*
             * Rotate the camera.
             */
            float angle = rotationRate * updateInterval;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleNormalAxis(angle, rotationAxis);
            Quaternion oldOrientation = cam.getRotation();
            Quaternion newOrientation = oldOrientation.mult(rotation);
            cam.setRotation(newOrientation);
        }

        if (warpFactor != 0) {
            /*
             * Create an illusion of forward motion by moving all the
             * objects in the direction opposite where the camera is pointing.
             * 
             * Objects which leave the zone wrap around to the other side,
             * giving the illusion of an inexhaustible set of objects.
             */
            float forwardSpeed = 0.1f * warpFactor * zoneRadius;
            float distance = forwardSpeed * updateInterval;
            Vector3f offset = cam.getDirection().mult(-distance);

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

    /**
     * Process a GUI action.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored time per frame (in seconds)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        if (!ongoing) {
            return;
        }
        switch (actionString) {
            case asFeels:
                messageHud.addLine("I have a bad feeling about this.");
                return;
            case asFire:
                messageHud.addLine("Torpedos away!");
                return;
            case asForward:
                warpFactor++;
                messageWarp();
                return;
            case asHail:
                messageHud.addLine("Hailing frequencies open.");
                return;
            case asHelp:
                messageHud.setEnabled(false);
                InputMode thisMode = InputMode.getEnabledMode();
                bindScreen.activate(thisMode);
                return;
            case asLowerShields:
                messageHud.addLine("Shields are down.");
                return;
            case asPitchDown:
                rotationAxis.set(1f, 0f, 0f);
                rotationRate = 0.2f;
                return;
            case asPitchUp:
                rotationAxis.set(1f, 0f, 0f);
                rotationRate = -0.2f;
                return;
            case asRaiseShields:
                messageHud.addLine("Shields up!");
                return;
            case asReverse:
                warpFactor--;
                messageWarp();
                return;
            case asRollLeft:
                rotationAxis.set(0f, 0f, 1f);
                rotationRate = 0.2f;
                return;
            case asRollRight:
                rotationAxis.set(0f, 0f, 1f);
                rotationRate = -0.2f;
                return;
            case asStopAll:
                rotationRate = 0f;
                warpFactor = 0;
                messageWarp();
                return;
            case asStopRotation:
                rotationRate = 0f;
                return;
            case asYawLeft:
                rotationAxis.set(0f, 1f, 0f);
                rotationRate = 0.2f;
                return;
            case asYawRight:
                rotationAxis.set(0f, 1f, 0f);
                rotationRate = -0.2f;
                return;
        }
        /*
         * Action not yet handled: fall back on ActionApplication's handler.
         */
        super.onAction(actionString, ongoing, ignored);
    }
    // *************************************************************************
    // private methods

    /**
     * Create and attach the backdrop.
     */
    private void initializeBackdrop() {
        backdrop = new Node("backdrop node");
        backdrop.setQueueBucket(RenderQueue.Bucket.Sky);
        rootNode.attachChild(backdrop);
        /*
         * The backdrop consists of two hemispherical dome geometries, one
         * for northern stars and one for southern ones.
         */
        Mesh hemisphere = new DomeMesh(60, 16);
        Geometry northDome = new Geometry("north", hemisphere);
        Material north = MyAsset.createUnshadedMaterial(
                assetManager, "Textures/skies/star-maps/northern.png");
        northDome.setMaterial(north);
        backdrop.attachChild(northDome);

        Geometry southDome = new Geometry("south", hemisphere);
        Material south = MyAsset.createUnshadedMaterial(
                assetManager, "Textures/skies/star-maps/southern.png");
        southDome.setMaterial(south);
        backdrop.attachChild(southDome);

        Quaternion z180 = new Quaternion();
        z180.fromAngleNormalAxis(FastMath.PI, new Vector3f(0f, 0f, 1f));
        MySpatial.setWorldOrientation(southDome, z180);
        /*
         * Scale the backdrop so its furthest geometries are midway
         * between the near and far planes of the view frustum.
         */
        float far = cam.getFrustumFar();
        float near = cam.getFrustumNear();
        float radius = (near + far) / 2f;
        MySpatial.setWorldScale(backdrop, radius);
    }

    /**
     * Add action strings and hotkey bindings to the default input mode.
     */
    private void initializeBindings() {
        InputMode mode = getDefaultInputMode();

        mode.addActionName(asFeels);
        mode.addActionName(asFire);
        mode.addActionName(asForward);
        mode.addActionName(asHail);
        mode.addActionName(asHelp);
        mode.addActionName(asLowerShields);
        mode.addActionName(asPitchDown);
        mode.addActionName(asPitchUp);
        mode.addActionName(asRaiseShields);
        mode.addActionName(asReverse);
        mode.addActionName(asRollLeft);
        mode.addActionName(asRollRight);
        mode.addActionName(asStopAll);
        mode.addActionName(asStopRotation);
        mode.addActionName(asYawLeft);
        mode.addActionName(asYawRight);

        mode.setSaveFileName(
                "assets/Interface/bindings/TestBindScreen.properties");
        mode.loadBindings();
    }

    /**
     * Create and attach moving objects.
     */
    private void initializeObjects() {
        objects = new Node("objects node");
        rootNode.attachChild(objects);
        /*
         * The moving objects are small white spheres, distributed randomly 
         * trhoughout a cubical zone centered on the origin
         */
        NameGenerator names = new NameGenerator();
        Material white = MyAsset.createUnshadedMaterial(
                assetManager, new ColorRGBA(1f, 1f, 1f, 1f));
        float objectRadius = 0.01f * zoneRadius
                / MyMath.cubeRoot((float) numObjects);
        Mesh sphere = new Sphere(3, 10, objectRadius);

        for (int i = 0; i < numObjects; i++) {
            String name = names.unique("obj");
            Geometry object = new Geometry(name, sphere);
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
     * Add a message to the HUD about how fast we're going.
     */
    private void messageWarp() {
        String line;
        if (warpFactor > 0) {
            line = String.format("Ahead, warp factor %d!", warpFactor);
        } else if (warpFactor == 0) {
            line = String.format("Full stop!");
        } else {
            line = String.format("Reverse, warp factor %d!", -warpFactor);
        }
        messageHud.addLine(line);
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

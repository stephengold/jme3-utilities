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
package jme3utilities.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.CameraInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Simple application with an action-oriented user interface.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class ActionApplication
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ActionApplication.class.getName());
    /**
     * names of flyCam actions and the signals used to simulate them
     */
    final public static String[] flycamNames = {
        CameraInput.FLYCAM_BACKWARD,
        CameraInput.FLYCAM_FORWARD,
        CameraInput.FLYCAM_LOWER,
        CameraInput.FLYCAM_RISE,
        CameraInput.FLYCAM_STRAFELEFT,
        CameraInput.FLYCAM_STRAFERIGHT
    };
    /**
     * filesystem path to the folder/directory for written assets
     */
    final private static String writtenAssetDirPath = "Written Assets";
    // *************************************************************************
    // fields

    /**
     * initial input mode: set in {@link #simpleInitApp()}
     */
    private InputMode defaultInputMode = null;
    /**
     * signal tracker set in {@link #simpleInitApp()}
     */
    private Signals signals = null;
    // *************************************************************************
    // new public methods

    /**
     * Callback to the user's application startup code.
     */
    abstract public void actionInitializeApplication();

    /**
     * Callback invoked when an ongoing action isn't handled after running
     * through the {@link #onAction(java.lang.String, boolean, float)} methods
     * of both the input mode and the application. Meant to be overridden.
     *
     * @param actionString textual description of the action (not null)
     */
    public void didntHandle(String actionString) {
        Validate.nonNull(actionString, "action string");
        logger.log(Level.WARNING, "Ongoing action {0} was not handled.",
                MyString.quote(actionString));
    }

    /**
     * Convert an asset path to a filesystem path for a writable asset.
     *
     * @param assetPath (not null)
     * @return filesystem path (not null)
     */
    public static String filePath(String assetPath) {
        Validate.nonNull(assetPath, "asset path");
        String result = String.format("%s/%s", writtenAssetDirPath, assetPath);
        return result;
    }

    /**
     * Access the default input mode.
     *
     * @return pre-existing instance (not null)
     */
    public InputMode getDefaultInputMode() {
        assert defaultInputMode != null;
        return defaultInputMode;
    }

    /**
     * Access the signal tracker.
     *
     * @return pre-existing instance (not null)
     */
    public Signals getSignals() {
        assert signals != null;
        return signals;
    }

    /**
     * Callback invoked immediately after initializing the hotkey bindings of
     * the default input mode. Meant to be overridden. Can be used to add action
     * names and/or override those bindings.
     */
    public void moreDefaultBindings() {
    }

    /**
     * Alter the effective speeds of all animations.
     *
     * @param newSpeed animation speed (&gt;0, standard speed &rarr; 1)
     */
    public void setSpeed(float newSpeed) {
        Validate.positive(newSpeed, "speed");
        speed = newSpeed;
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action which wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        /*
         * Handle actions whose mappings may have been deleted by
         * DefaultInputMode.initialize().
         */
        if (ongoing) {
            switch (actionString) {
                case SimpleApplication.INPUT_MAPPING_EXIT:
                    stop();
                    break;

                case SimpleApplication.INPUT_MAPPING_CAMERA_POS:
                    if (cam != null) {
                        Vector3f loc = cam.getLocation();
                        Quaternion rot = cam.getRotation();
                        System.out.println("Camera Position: ("
                                + loc.x + ", " + loc.y + ", " + loc.z + ")");
                        System.out.println("Camera Rotation: " + rot);
                        System.out.println("Camera Direction: "
                                + cam.getDirection());
                        System.out.println("cam.setLocation(new Vector3f("
                                + loc.x + "f, " + loc.y + "f, " + loc.z
                                + "f));");
                        System.out.println("cam.setRotation(new Quaternion("
                                + rot.getX() + "f, " + rot.getY() + "f, "
                                + rot.getZ() + "f, " + rot.getW() + "f));");
                    }
                    break;

                case SimpleApplication.INPUT_MAPPING_HIDE_STATS:
                    StatsAppState sas = stateManager.getState(
                            StatsAppState.class);
                    if (sas != null) {
                        sas.toggleStats();
                    }
                    break;

                case SimpleApplication.INPUT_MAPPING_MEMORY:
                    BufferUtils.printCurrentDirectMemory(null);
                    break;

                default:
                    didntHandle(actionString);
            }
        }
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Startup code for this application.
     */
    @Override
    public void simpleInitApp() {
        if (defaultInputMode != null) {
            throw new IllegalStateException(
                    "app should only be initialized once");
        }
        if (signals != null) {
            throw new IllegalStateException(
                    "app should only be initialized once");
        }
        /*
         * Make sure a folder exists for writable assets.
         */
        File tmpAssetDir = new File(writtenAssetDirPath);
        if (!tmpAssetDir.exists()) {
            tmpAssetDir.mkdirs();
        }
        /*
         * Register a locator for writable assets and
         * make sure it precedes the classpath locator.
         */
        assetManager.registerLocator(writtenAssetDirPath, FileLocator.class);
        assetManager.unregisterLocator("/", ClasspathLocator.class);
        assetManager.registerLocator("/", ClasspathLocator.class);
        /*
         * Register a loader for Properties assets.
         */
        assetManager.registerLoader(PropertiesLoader.class, "properties");
        /*
         * Initialize hotkeys and a signal tracker for modal hotkeys.
         */
        signals = new Signals();
        Hotkey.intialize();
        /*
         * Attach and enable the default input mode.
         */
        defaultInputMode = new DefaultInputMode();
        stateManager.attach(defaultInputMode);
        defaultInputMode.setEnabled(true);
        /*
         * Invoke the startup code of the subclass.
         */
        actionInitializeApplication();
    }

    /**
     * Callback invoked once per render pass.
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        /*
         * Handle flyCam signals whose mappings may have been deleted by
         * DefaultInputMode.initialize().
         */
        if (flyCam != null && flyCam.isEnabled()) {
            for (String signalName : flycamNames) {
                if (signals.test(signalName)) {
                    flyCam.onAnalog(signalName, tpf, tpf);
                }
            }
        }
    }
}

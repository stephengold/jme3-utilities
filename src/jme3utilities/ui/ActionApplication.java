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
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
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
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ActionApplication.class.getName());
    // *************************************************************************
    // fields

    /**
     * initial input mode: set in #simpleInitApp()
     */
    private InputMode defaultInputMode = null;
    /**
     * signal tracker set in #simpleInitApp()
     */
    private Signals signals = null;
    // *************************************************************************
    // new public methods

    /**
     * Callback to the user's application startup code.
     */
    abstract public void actionInitializeApplication();

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
     * Process an action from which is not handled by the default input mode.
     * This method is a placeholder which may be overridden as desired.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored time per frame (in seconds)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        /*
         * Handle actions whose mappings may have been deleted by 
         * DefaultInputMode.initialize() .
         */
        switch (actionString) {
            case SimpleApplication.INPUT_MAPPING_EXIT:
                stop();
                return;

            case SimpleApplication.INPUT_MAPPING_CAMERA_POS:
                if (cam != null) {
                    Vector3f loc = cam.getLocation();
                    Quaternion rot = cam.getRotation();
                    System.out.println("Camera Position: ("
                            + loc.x + ", " + loc.y + ", " + loc.z + ")");
                    System.out.println("Camera Rotation: " + rot);
                    System.out.println("Camera Direction: " + cam.getDirection());
                    System.out.println("cam.setLocation(new Vector3f("
                            + loc.x + "f, " + loc.y + "f, " + loc.z + "f));");
                    System.out.println("cam.setRotation(new Quaternion(" + 
                            rot.getX() + "f, " + rot.getY() + "f, " + rot.getZ() 
                            + "f, " + rot.getW() + "f));");

                }
                return;

            case SimpleApplication.INPUT_MAPPING_HIDE_STATS:
                StatsAppState sas = stateManager.getState(StatsAppState.class);
                if (sas != null) {
                    sas.toggleStats();
                }
                return;

            case SimpleApplication.INPUT_MAPPING_MEMORY:
                BufferUtils.printCurrentDirectMemory(null);
                return;
        }

        logger.log(Level.WARNING, "Action {0} was not handled.",
                MyString.quote(actionString));
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Startup code for this simple application.
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
}

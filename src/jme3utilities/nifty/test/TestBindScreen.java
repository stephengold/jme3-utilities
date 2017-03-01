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
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.MessageDisplay;
import jme3utilities.nifty.bind.BindScreen;
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
    /**
     * axes of rotation
     */
    final private Vector3f pitchAxis = new Vector3f(1f, 0f, 0f);
    final private Vector3f rollAxis = new Vector3f(0f, 0f, 1f);
    final private Vector3f yawAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields
    
    /**
     * editor for hotkey bindings (set by guiInitializeApplication())
     */
    private BindScreen bindScreen;
    /**
     * rate of movement in the direction the camera is pointed (arbitrary units,
     * may be negative)
     */
    private int warpFactor = 0;
    /**
     * heads-up display for messages (set by guiInitializeApplication())
     */
    private MessageDisplay messageHud;
    /**
     * app state to animate a field of stars (set by guiInitializeApplication())
     */
    private StarfieldState starfield;
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
         * Initialize the (default) input mode for playing the game.
         */
        initializeBindings();
        /*
         * Create and attach the starfield animation.
         */
        starfield = new StarfieldState(false, numObjects, "equator");
        boolean success = stateManager.attach(starfield);
        assert success;
        /*
         * Attach a HUD for messages.
         */
        messageHud = new MessageDisplay();
        messageHud.setListener(this);
        success = stateManager.attach(messageHud);
        assert success;
        messageHud.addLine("Press the H key to view/edit hotkey bindings.");
        /*
         * The (default) input mode should influence the animation and HUD.
         */
        InputMode mode = getDefaultInputMode();
        mode.influence(starfield);
        mode.influence(messageHud);
        /*
         * Attach a screen controller for the hotkey bindings editor.
         */
        bindScreen = new BindScreen();
        success = stateManager.attach(bindScreen);
        assert success;
    }

    /**
     * Process a GUI action.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case asFeels:
                    messageHud.addLine("I have a bad feeling about this.");
                    return;
                case asFire:
                    messageHud.addLine("Torpedos away!");
                    return;
                case asForward:
                    setWarpFactor(warpFactor + 1);
                    return;
                case asHail:
                    messageHud.addLine("Hailing frequencies open.");
                    return;
                case asHelp:
                    InputMode thisMode = InputMode.getEnabledMode();
                    bindScreen.activate(thisMode);
                    return;
                case asLowerShields:
                    messageHud.addLine("Shields are down.");
                    return;
                case asPitchDown:
                    starfield.setRotation(0.2f, pitchAxis);
                    return;
                case asPitchUp:
                    starfield.setRotation(-0.2f, pitchAxis);
                    return;
                case asRaiseShields:
                    messageHud.addLine("Shields up!");
                    return;
                case asReverse:
                    setWarpFactor(warpFactor - 1);
                    return;
                case asRollLeft:
                    starfield.setRotation(0.2f, rollAxis);
                    return;
                case asRollRight:
                    starfield.setRotation(-0.2f, rollAxis);
                    return;
                case asStopAll:
                    starfield.setRotation(0f, yawAxis);
                    setWarpFactor(0);
                    return;
                case asStopRotation:
                    starfield.setRotation(0f, yawAxis);
                    return;
                case asYawLeft:
                    starfield.setRotation(0.2f, yawAxis);
                    return;
                case asYawRight:
                    starfield.setRotation(-0.2f, yawAxis);
                    return;
            }
        }
        /*
         * Action not yet handled: fall back on ActionApplication's handler.
         */
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private  methods

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
     * Display a message in the HUD about how fast we're going.
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
     * Set a new warp factor and direction.
     *
     * @param newWarpFactor input value
     */
    private void setWarpFactor(int newWarpFactor) {
        if (newWarpFactor == warpFactor) {
            return;
        }

        warpFactor = newWarpFactor;
        messageWarp();
        float zoneRadius = starfield.getZoneRadius();
        float forwardVelocity = 0.1f * warpFactor * zoneRadius;
        starfield.setForwardVelocity(forwardVelocity);
    }
}

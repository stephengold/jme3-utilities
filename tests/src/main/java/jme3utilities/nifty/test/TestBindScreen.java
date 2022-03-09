/*
 Copyright (c) 2017-2022, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.nifty.test;

import com.jme3.app.StatsAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.MessageDisplay;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.nifty.displaysettings.DsScreen;
import jme3utilities.ui.DisplaySizeLimits;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.UiVersion;

/**
 * Test/demonstrate the hotkey-bindings editor (BindScreen) and the
 * display-settings editor (DsScreen).
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestBindScreen extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * number of objects to animate (&gt;0)
     */
    final private static int numObjects = 20_000;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestBindScreen.class.getName());
    /**
     * action strings
     */
    final private static String asFeels = "express feelings";
    final private static String asFire = "launch torpedo";
    final private static String asForward = "accelerate forward";
    final private static String asHail = "open hailing frequencies";
    final private static String asHelp = "edit bindings";
    final private static String asLowerShields = "shields down";
    final private static String asMenu = "open popup menu";
    final private static String asPitchDown = "pitch down";
    final private static String asPitchUp = "pitch up";
    final private static String asRaiseShields = "shields up";
    final private static String asReverse = "accelerate reverse";
    final private static String asRollLeft = "roll left";
    final private static String asRollRight = "roll right";
    final private static String asSettings = "edit displaySettings";
    final private static String asStopAll = "stop all";
    final private static String asStopRotation = "stop rotation";
    final private static String asYawLeft = "yaw left";
    final private static String asYawRight = "yaw right";
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestBindScreen.class.getSimpleName();
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
     * editor for display settings (set by guiInitializeApplication())
     */
    private DsScreen dsScreen;
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
    /**
     * display settings (set by main())
     */
    private static TbsDisplaySettings displaySettings;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestBindScreen application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        TestBindScreen application = new TestBindScreen();
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        DisplaySizeLimits dsl = new DisplaySizeLimits(
                600, 480, // min width, height
                2_048, 1_080 // max width, height
        );
        displaySettings
                = new TbsDisplaySettings(application, applicationName, dsl) {
            @Override
            protected void applyOverrides(AppSettings settings) {
                super.applyOverrides(settings);
                settings.setGammaCorrection(true);
                settings.setSamples(8);
                settings.setVSync(true);
            }
        };
        displaySettings.setForceDialog(true);
        AppSettings appSettings = displaySettings.initialize();
        if (appSettings == null) {
            return;
        }
        application.setSettings(appSettings);
        /*
         * If the settings dialog should be shown, it was already shown
         * by DisplaySettings.initialize().
         */
        application.setShowSettings(false);
        application.start();
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        /*
         * Log library versions.
         */
        logger.log(Level.INFO, "jme3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        logger.log(Level.INFO, "Heart version is {0}",
                MyString.quote(Heart.versionShort()));
        logger.log(Level.INFO, "jme3-utilities-ui version is {0}",
                MyString.quote(UiVersion.versionShort()));
        logger.log(Level.INFO, "jme3-utilities-nifty version is {0}",
                MyString.quote(LibraryVersion.versionShort()));

        Logger.getLogger(InputMode.class.getName()).setLevel(Level.INFO);
        /*
         * Disable flyCam and stats display.
         */
        flyCam.setEnabled(false);
        StatsAppState sas = stateManager.getState(StatsAppState.class);
        sas.setDisplayFps(false);
        sas.setDisplayStatView(false);
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
        messageHud.addLine("Press H key to view/edit hotkey bindings, U key to view/edit display settings.");
        /*
         * The (default) input mode should influence the animation and HUD.
         */
        InputMode mode = getDefaultInputMode();
        mode.influence(starfield);
        mode.influence(messageHud);

        mode.setConfigPath("Interface/bindings/TestBindScreen.properties");
        /*
         * Attach a screen controller for the hotkey-bindings editor.
         */
        bindScreen = new BindScreen();
        success = stateManager.attach(bindScreen);
        assert success;
        /*
         * Attach a screen controller for the display-settings editor.
         */
        dsScreen = new DsScreen(displaySettings);
        success = stateManager.attach(dsScreen);
        assert success;
    }

    /**
     * Callback invoked immediately after initializing the hotkey bindings of
     * the default input mode.
     */
    @Override
    public void moreDefaultBindings() {
        /*
         * Add action names to the default input mode.
         */
        InputMode dim = getDefaultInputMode();

        dim.addActionName(asFeels);
        dim.addActionName(asFire);
        dim.addActionName(asForward);
        dim.addActionName(asHail);
        dim.addActionName(asHelp);
        dim.addActionName(asLowerShields);
        dim.addActionName(asMenu);
        dim.addActionName(asPitchDown);
        dim.addActionName(asPitchUp);
        dim.addActionName(asRaiseShields);
        dim.addActionName(asReverse);
        dim.addActionName(asRollLeft);
        dim.addActionName(asRollRight);
        dim.addActionName(asSettings);
        dim.addActionName(asStopAll);
        dim.addActionName(asStopRotation);
        dim.addActionName(asYawLeft);
        dim.addActionName(asYawRight);
    }

    /**
     * Process a GUI action.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            GuiScreenController gsc = (GuiScreenController) getEnabledScreen();
            InputMode thisMode = InputMode.findMode("default");
            switch (actionString) {
                case asFeels:
                    messageHud.addLine("I have a bad feeling about this.");
                    return;
                case asFire:
                    messageHud.addLine("Torpedoes away!");
                    return;
                case asForward:
                    setWarpFactor(warpFactor + 1);
                    return;
                case asHail:
                    messageHud.addLine("Hailing frequencies open.");
                    return;
                case asHelp:
                    gsc.closeAllPopups();
                    bindScreen.activate(thisMode);
                    return;
                case asLowerShields:
                    messageHud.addLine("Shields are down.");
                    return;
                case asMenu:
                    String[] items = {asHelp, asSettings};
                    gsc.showPopupMenu("", items);
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
                case asSettings:
                    gsc.closeAllPopups();
                    dsScreen.activate();
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
         * The action is not handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a message in the HUD about how fast we're going.
     */
    private void messageWarp() {
        String line;
        if (warpFactor > 0) {
            line = String.format("Ahead, warp factor %d!", warpFactor);
        } else if (warpFactor == 0) {
            line = "Full stop!";
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

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
package jme3utilities.sky.test;

import com.beust.jcommander.JCommander;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.input.KeyInput;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;

/**
 * GUI application for testing/demonstrating the SkyControl class using a
 * heads-up display (HUD). The application's main entry point is here.
 * <p>
 * Use the 'H' key to toggle HUD visibility. When the HUD is hidden, the flyCam
 * (LMB and scroll wheel) controls are enabled for scene navigation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSkyControl extends GuiApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestSkyControl.class.getName());
    /**
     * action string to open hotkey bindings editor
     */
    final private static String actionStringEdit = "edit bindings";
    /**
     * action string to toggle HUD visibility
     */
    final private static String actionStringToggle = "toggle hud";
    /**
     * application name for its window's title bar and its usage message
     */
    final private static String applicationName = "TestSkyControl";
    /**
     * path to hotkey bindings configuration file
     */
    final private static String hotkeyBindingsPath = 
            "assets/Interface/bindings/TestSkyControl.properties";
    // *************************************************************************
    // fields

    /**
     * Nifty screen for editing hotkey bindings
     */
    static BindScreen bindScreen = new BindScreen();
    /**
     * heads-up display (HUD)
     */
    static TestSkyControlHud hud = new TestSkyControlHud();
    /**
     * scene management app state
     */
    static TestSkyControlRun run = new TestSkyControlRun();
    /**
     * command-line parameters
     */
    final static TestSkyControlParameters parameters =
            new TestSkyControlParameters();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the test harness.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Lower the logging level for this class.
         */
        logger.setLevel(Level.INFO);

        TestSkyControl application = new TestSkyControl();
        /*
         * Parse the command-line arguments into parameters.
         */
        JCommander jCommander = new JCommander(parameters, arguments);
        jCommander.setProgramName(applicationName);
        if (parameters.usageOnly()) {
            jCommander.usage();
            return;
        }
        /*
         * Don't pause on lost focus.  This simplifies debugging and
         * permits the application to keep running while minimized.
         */
        application.setPauseOnLostFocus(false);
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        String title = applicationName + " " + MyString.join(arguments);
        settings.setTitle(title);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to TestSkyControl.guiInitializeApplication()!
         */
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this GUI application.
     */
    @Override
    public void guiInitializeApplication() {
        /*
         * Log the jME3-utilities version string.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));

        //Misc.detachAll(stateManager, DebugKeysAppState.class);
        /*
         * Capture a screenshot each time the KEY_SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Disable display of jME3 statistics.
         * These displays can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Create and attach the heads-up display (HUD), the scene manger,
         * and the hotkey bindings editor.
         */
        success = stateManager.attach(hud);
        assert success;
        success = stateManager.attach(run);
        assert success;
        success = stateManager.attach(bindScreen);
        assert success;
        /*
         * Default input mode directly influencess the scene manager. 
         */
        InputMode dim = getDefaultInputMode();
        dim.influence(run);
        /*
         * Load hotkey bindings from file.
         */
        dim.setConfigPath(hotkeyBindingsPath);
    }

    /**
     * Process an action from the GUI or keyboard which was not handled by the
     * default input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case actionStringEdit:
                    InputMode im = InputMode.getEnabledMode();
                    bindScreen.activate(im);
                    return;
                case actionStringToggle:
                    run.toggleHud();
                    return;
            }
        }

        super.onAction(actionString, ongoing, tpf);
    }
}

/*
 Copyright (c) 2019-2022, Stephen Gold
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

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeVersion;
import com.jme3.system.Platform;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.ui.InputMode;
import org.lwjgl.system.Configuration;

/**
 * Test/demonstrate multiple instances of the WindowController class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMultipleWindows extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestMultipleWindows.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestMultipleWindows.class.getSimpleName();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestMultipleWindows application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Platform platform = JmeSystem.getPlatform();
        if (platform.getOs() == Platform.Os.MacOS) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }

        TestMultipleWindows application = new TestMultipleWindows();
        Heart.parseAppArgs(application, arguments);
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);

        boolean loadDefaults = true;
        AppSettings appSettings = new AppSettings(loadDefaults);
        appSettings.setAudioRenderer(null);
        appSettings.setResizable(true);
        /*
         * Customize the window's title bar.
         */
        String title = applicationName + " " + MyString.join(arguments);
        appSettings.setTitle(title);
        application.setSettings(appSettings);
        /*
         * The AWT settings dialog interferes with LWJGL v3
         * on macOS and Raspbian, so don't show it!
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

        InputMode inputMode = getDefaultInputMode();
        /*
         * Create and attach a controller for the main (and only) screen.
         */
        GuiScreenController screen = new GuiScreenController("main",
                "Interface/Nifty/huds/test-multiple-windows.xml",
                InitialState.Enabled);
        screen.setListener(inputMode);
        boolean success = stateManager.attach(screen);
        assert success;
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
        if (ongoing) {
            switch (actionString) {
                case "release button 1":
                    System.out.println("released 1");
                    return;
                case "release button 2":
                    System.out.println("released 2");
                    return;
                default:
            }
        }
        /*
         * The action is not handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, ignored);
    }
}

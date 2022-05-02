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

import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.ui.InputMode;

/**
 * Test/demonstrate dynamic Nifty labels.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ClockDemo extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ClockDemo.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = ClockDemo.class.getSimpleName();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the ClockDemo application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        ClockDemo application = new ClockDemo();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings appSettings = new AppSettings(loadDefaults);
        appSettings.setAudioRenderer(null);
        /*
         * Customize the window's title bar.
         */
        String title = applicationName + " " + MyString.join(arguments);
        appSettings.setTitle(title);
        application.setSettings(appSettings);
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
        ClockScreenController clockScreen = new ClockScreenController();
        clockScreen.setListener(inputMode);
        boolean success = stateManager.attach(clockScreen);
        assert success;
    }
}

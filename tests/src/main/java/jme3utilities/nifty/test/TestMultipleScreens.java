/*
 Copyright (c) 2014-2022, Stephen Gold
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

import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.PopScreenController;
import jme3utilities.ui.InputMode;

/**
 * Test/demonstrate multiple instances of the BasicScreenController class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMultipleScreens extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestMultipleScreens.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestMultipleScreens.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * controller for screen s1: set in guiInitializeApplication()
     */
    private PopScreenController s1 = null;
    /**
     * controller for screen s2: set in guiInitializeApplication()
     */
    private BasicScreenController s2 = null;
    /**
     * controller for screen s3: set in guiInitializeApplication()
     */
    private BasicScreenController s3 = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestMultipleScreens application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        TestMultipleScreens application = new TestMultipleScreens();
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);

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
         * Create and attach a controller for each screen.
         */
        s1 = new PopScreenController("TestMultipleScreens/s1",
                "Interface/Nifty/screens/TestMultipleScreens/s1.xml",
                InitialState.Enabled);
        s1.setListener(inputMode);
        boolean success = stateManager.attach(s1);
        assert success;

        s2 = new BasicScreenController("TestMultipleScreens/s2",
                "Interface/Nifty/screens/TestMultipleScreens/s2.xml",
                InitialState.Disabled);
        s2.setListener(inputMode);
        success = stateManager.attach(s2);
        assert success;

        s3 = new BasicScreenController("TestMultipleScreens/s3",
                "Interface/Nifty/screens/TestMultipleScreens/s3.xml",
                InitialState.Disabled);
        s3.setListener(inputMode);
        success = stateManager.attach(s3);
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
            BasicScreenController old = getEnabledScreen();
            switch (actionString) {
                case "go s1":
                    old.setEnabled(false);
                    s1.setEnabled(true);
                    return;
                case "go s2":
                    old.setEnabled(false);
                    s2.setEnabled(true);
                    return;
                case "go s3":
                    old.setEnabled(false);
                    s3.setEnabled(true);
                    return;
                case "open menu":
                    assert s1.isEnabled();
                    String[] items = {"s2", "s3"};
                    PopScreenController psc;
                    psc = (PopScreenController) getEnabledScreen();
                    psc.showPopupMenu("go ", items);
                    return;
            }
        }
        /*
         * The action is not handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, ignored);
    }
}

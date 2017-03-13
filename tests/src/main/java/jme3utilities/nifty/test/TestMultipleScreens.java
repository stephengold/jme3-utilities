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
package jme3utilities.nifty.test;

import com.jme3.audio.openal.ALAudioRenderer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.ui.InputMode;

/**
 * GUI application for testing/demonstrating multiple instances of the
 * BasicScreenController class. The application's main entry point is here.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestMultipleScreens
        extends GuiApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestMultipleScreens.class.getName());
    // *************************************************************************
    // fields

    /**
     * controller for screen s1: set in guiInitializeApplication()
     */
    private BasicScreenController s1 = null;
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
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);

        TestMultipleScreens application = new TestMultipleScreens();
        application.start();
        /*
         * ... and onward to TestMultipleScreens.guiInitializeApplication()!
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

        InputMode inputMode = getDefaultInputMode();
        /*
         * Create and attach a screen controller for each screen.
         */
        s1 = new BasicScreenController("TestMultipleScreens/s1",
                "Interface/Nifty/screens/TestMultipleScreens/s1.xml", true);
        s1.setListener(inputMode);
        boolean success = stateManager.attach(s1);
        assert success;

        s2 = new BasicScreenController("TestMultipleScreens/s2",
                "Interface/Nifty/screens/TestMultipleScreens/s2.xml", false);
        s2.setListener(inputMode);
        success = stateManager.attach(s2);
        assert success;

        s3 = new BasicScreenController("TestMultipleScreens/s3",
                "Interface/Nifty/screens/TestMultipleScreens/s3.xml", false);
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
            }
        }
        /*
         * The action has not been handled.
         */
        super.onAction(actionString, ongoing, ignored);
    }
}

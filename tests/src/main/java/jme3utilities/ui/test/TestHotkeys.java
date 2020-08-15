/*
 Copyright (c) 2020, Stephen Gold
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
package jme3utilities.ui.test;

import com.jme3.system.AppSettings;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.Hotkey;
import jme3utilities.ui.InputMode;

/**
 * An ActionApplication to log all button/keyboard input (for testing).
 */
public class TestHotkeys extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestHotkeys.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestHotkeys.class.getSimpleName();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestHotKeys application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Instantiate the application.
         */
        TestHotkeys application = new TestHotkeys();
        /*
         * Customize the window's title bar.
         */
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setTitle(applicationName);
        settings.setAudioRenderer(null);
        application.setSettings(settings);
        /*
         * Invoke the JME startup code,
         * which in turn invokes actionInitializeApplication().
         */
        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        if (settings.isFullscreen()) {
            logger.log(Level.SEVERE, "Not designed to run full-screen.");
            stop();
        }
        /*
         * Avert warnings about signals not added.
         */
        flyCam.setEnabled(false);
    }

    /**
     * Add application-specific hotkey bindings and override any existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        List<Hotkey> allKeys = Hotkey.listAll();
        InputMode dim = getDefaultInputMode();
        /*
         * Rebind all hotkeys.
         */
        for (Hotkey key : allKeys) {
            String actionName = key.getName();
            dim.bind(actionName, key);
        }
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        // do nothing
        // This avoids warnings about ongoing actions not handled.
    }
}

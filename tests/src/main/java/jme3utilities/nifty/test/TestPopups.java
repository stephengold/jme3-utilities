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

import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.ui.InputMode;

/**
 * GUI application for testing/demonstrating popups, including modal dialogs and
 * multi-level popup menus. The application's main entry point is here.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestPopups extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestPopups.class.getName());
    /**
     * application name for its window's title bar
     */
    final private static String applicationName = "TestPopups";
    /**
     * action prefix for the "open" popup menu
     */
    final private static String openMenuPrefix = "open ";
    /**
     * action prefix for the "search string" dialog
     */
    final private static String searchDialogPrefix = "set search ";
    // *************************************************************************
    // fields

    /**
     * controller for the screen: set in guiInitializeApplication()
     */
    private GuiScreenController screen = null;
    /**
     * most recent setting for search string (not null)
     */
    private String searchString = "";
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

        TestPopups application = new TestPopups();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);
        application.setSettings(settings);
        application.start();
        /*
         * ... and onward to TestPopups.guiInitializeApplication()!
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
         * Log library versions.
         */
        logger.log(Level.INFO, "jme3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        logger.log(Level.INFO, "SkyControl version is {0}",
                MyString.quote(Misc.getVersionShort()));

        InputMode inputMode = getDefaultInputMode();
        /*
         * Create and attach a screen controller.
         */
        screen = new GuiScreenController("TestPopups/mainScreen",
                "Interface/Nifty/screens/TestPopups/mainScreen.xml", true);
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
            if (actionString.equals("about")) {
                String msg = "TestPopups is a GUI application for testing/demon"
                        + "strating popup elements, including modal dialog boxe"
                        + "s and multi-level popup menus.\nMumble line 2\n";
                for (int i = 3; i <= 10; i++) {
                    msg += String.format("line %d\n", i);
                }
                screen.showInfoDialog("About the TestPopups application", msg);
                return;

            } else if (actionString.equals("search")) {
                screen.showTextEntryDialog("Enter new search string:",
                        searchString, "Set", searchDialogPrefix, null);
                return;

            } else if (actionString.startsWith(searchDialogPrefix)) {
                searchString = actionString.substring(
                        searchDialogPrefix.length());
                String line = String.format("Search string is %s.",
                        MyString.quote(searchString));
                updateStatusLine(line);
                return;

            } else if (actionString.startsWith(openMenuPrefix)) {
                String path = actionString.substring(openMenuPrefix.length());
                doOpen(actionString, path);
                return;
            }
        }
        /*
         * The action is not handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, ignored);
    }
    // *************************************************************************
    // private methods

    /**
     * Handle an "open" action from the GUI.
     *
     * @param actionString (starts with {@link #openMenuPrefix})
     * @param path filesystem path so far (starts with "/")
     */
    private void doOpen(String actionString, String path) {
        assert path != null;

        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            String line = String.format("Selected file %s.",
                    MyString.quote(path));
            updateStatusLine(line);

        } else if (files.length == 0) {
            String line = String.format("Selected empty folder %s.",
                    MyString.quote(path));
            updateStatusLine(line);

        } else {
            /*
             * Open a submenu with the directory/folder's contents.
             */
            String[] names = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                names[i] = files[i].getName();
            }
            if (actionString.endsWith("/")) {
                screen.showPopupMenu(actionString, names);
            } else {
                screen.showPopupMenu(actionString + "/", names);
            }
        }
    }

    /**
     * Update the status line in the GUI.
     *
     * @param newLine (not null)
     */
    private void updateStatusLine(String line) {
        assert line != null;
        screen.setStatusText("messageLabel", line);
    }
}

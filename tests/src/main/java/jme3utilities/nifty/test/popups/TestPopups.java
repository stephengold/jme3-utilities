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
package jme3utilities.nifty.test.popups;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeVersion;
import com.jme3.system.Platform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.dialog.FloatSliderDialog;
import jme3utilities.nifty.dialog.MinimalDialog;
import jme3utilities.nifty.dialog.MultiSelectDialog;
import jme3utilities.ui.InputMode;
import org.lwjgl.system.Configuration;

/**
 * Test/demonstrate popups, including modal dialogs and multi-level popup menus.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestPopups extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestPopups.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestPopups.class.getSimpleName();
    /**
     * action prefix for the multi-select dialog
     */
    final private static String checkTwicePrefix = "check list twice ";
    /**
     * action prefix for the "open" popup menu
     */
    final private static String openMenuPrefix = "open ";
    /**
     * action prefix for the "search string" dialog
     */
    final private static String searchDialogPrefix = "set search ";
    /**
     * action prefix for the "password" dialog: arguments are a boolean and a
     * text string
     */
    final private static String setPasswordPrefix = "set password ";
    /**
     * action prefix for the "temperature" dialog
     */
    final private static String setTemperaturePrefix = "set temperature ";
    // *************************************************************************
    // fields

    /**
     * most recent setting for temperature
     */
    private float temperature = 20f;
    /**
     * controller for the screen: set in guiInitializeApplication()
     */
    private GuiScreenController screen = null;
    /**
     * controller for the multi-letter selection dialog
     */
    private MultiSelectDialog multiSelectDialog;
    /**
     * most recent setting for search string (not null)
     */
    private String searchString = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestPopups application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Platform platform = JmeSystem.getPlatform();
        if (platform.getOs() == Platform.Os.MacOS) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }

        TestPopups application = new TestPopups();
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

        List<LetterItem> allLetterItems = new ArrayList<>(26);
        for (char c = 'A'; c <= 'Z'; ++c) {
            LetterItem item = new LetterItem(c);
            allLetterItems.add(item);
        }
        multiSelectDialog = new MultiLetterDialogController(
                "Check twice", allLetterItems);

        InputMode inputMode = getDefaultInputMode();
        /*
         * Create and attach a controller for the main (and only) screen.
         */
        screen = new GuiScreenController("TestPopups/mainScreen",
                "Interface/Nifty/screens/TestPopups/mainScreen.xml",
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
            if (actionString.equals("about")) {
                String msg = "TestPopups is a GUI application for testing/demon"
                        + "strating popup elements, including modal dialog boxe"
                        + "s and multi-level popup menus.\nMumble line 2\n";
                for (int i = 3; i <= 10; i++) {
                    msg += String.format("line %d\n", i);
                }
                screen.showInfoDialog("About the TestPopups application", msg);
                return;

            } else if (actionString.equals("make list")) {
                screen.showMultiSelectDialog("Select 2 or more letters:",
                        checkTwicePrefix, multiSelectDialog);
                return;

            } else if (actionString.equals("search")) {
                screen.showTextEntryDialog("Enter new search string:",
                        searchString, searchDialogPrefix,
                        new SearchDialogController());
                return;

            } else if (actionString.equals("set password")) {
                String defaultPassword = "admin123";
                screen.showTextAndCheckDialog("Enter new password:",
                        defaultPassword, setPasswordPrefix,
                        new TAndCDialogController("Set", "salted"));
                return;

            } else if (actionString.equals("set temperature")) {
                String temperatureString = Float.toString(temperature);
                screen.showTextAndSliderDialog("Enter new temperature:",
                        temperatureString, setTemperaturePrefix,
                        new FloatSliderDialog("Set", -273f, 100f));
                return;

            } else if (actionString.startsWith(checkTwicePrefix)) {
                String commitSuffix = MyString.remainder(actionString,
                        checkTwicePrefix);
                String[] descriptions
                        = multiSelectDialog.parseDescriptionArray(commitSuffix);
                int numSelected = descriptions.length;
                assert numSelected >= 2 : numSelected;
                int last = numSelected - 1;

                StringBuilder line = new StringBuilder(60);
                line.append("The letters ");
                for (int i = 0; i < numSelected; ++i) {
                    if (i == last) {
                        if (numSelected > 2) {
                            line.append(","); // Oxford comma
                        }
                        line.append(" and ");
                    } else if (i > 0) {
                        line.append(", ");
                    }
                    line.append(descriptions[i]);
                }
                line.append(".");
                updateStatusLine(line.toString());
                return;

            } else if (actionString.startsWith(searchDialogPrefix)) {
                searchString
                        = MyString.remainder(actionString, searchDialogPrefix);
                String line = String.format("Search string is %s.",
                        MyString.quote(searchString));
                updateStatusLine(line);
                return;

            } else if (actionString.startsWith(setPasswordPrefix)) {
                String argList = MyString.remainder(actionString,
                        setPasswordPrefix);
                if (argList.contains(" ")) {
                    String[] args = argList.split(" ");
                    boolean salted = Boolean.parseBoolean(args[0]);
                    String pw = MyString.remainder(argList, args[0] + " ");
                    String line = String.format(
                            "The password is %s and salted is %s.",
                            MyString.quote(pw), salted);
                    updateStatusLine(line);
                    return;
                }

            } else if (actionString.startsWith(setTemperaturePrefix)) {
                String arg = MyString.remainder(actionString,
                        setTemperaturePrefix);
                temperature = Float.parseFloat(arg);
                String line = String.format("Temperature is %f.", temperature);
                updateStatusLine(line);
                return;

            } else if (actionString.startsWith(openMenuPrefix)) {
                String path = actionString.substring(openMenuPrefix.length());
                doOpen(actionString, path);
                return;

            } else if (actionString.equals(
                    SimpleApplication.INPUT_MAPPING_EXIT)) {
                screen.showConfirmDialog(
                        "Close the application? This one?? Are you sure???",
                        "Close", "really close", new MinimalDialog());
                return;

            } else if (actionString.equals("really close")) {
                stop();
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
            String line
                    = String.format("Selected file %s.", MyString.quote(path));
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
            String[] icons = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                file = files[i];
                names[i] = file.getName();
                if (file.isDirectory()) {
                    icons[i] = "Textures/icons/directory.png";
                } else if (names[i].endsWith(".j3o")) {
                    icons[i] = "Interface/Logo/Monkey.jpg";
                }
            }
            if (actionString.endsWith("/")) {
                screen.showPopupMenu(actionString, names, icons);
            } else {
                screen.showPopupMenu(actionString + "/", names, icons);
            }
        }
    }

    /**
     * Update the status line in the GUI.
     *
     * @param line (not null)
     */
    private void updateStatusLine(String line) {
        assert line != null;
        screen.setStatusText("messageLabel", line);
    }
}

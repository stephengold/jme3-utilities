/*
 Copyright (c) 2022, Stephen Gold
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

import com.jme3.app.state.AppState;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeVersion;
import com.jme3.system.Platform;
import de.lessvoid.nifty.controls.Button;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.test.popups.TestPopups;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.UiVersion;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.lwjgl.system.Configuration;

/**
 * Choose an application from a list, then execute it.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AppChooser extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * main classes of the apps
     */
    final private static Class<?>[] mainClasses = {
        ClockDemo.class,
        TestBindScreen.class,
        TestMultipleScreens.class,
        TestMultipleWindows.class,
        TestPopups.class
    };
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AppChooser.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = AppChooser.class.getSimpleName();
    /**
     * action string to choose an app from a popup menu
     */
    final private static String asChooseApp = "choose app";
    /**
     * action prefix to choose an app
     */
    final private static String apChooseApp = "choose app ";
    /**
     * action string to delete any persistent settings of the chosen app
     */
    final private static String asDeleteSettings = "delete settings";
    /**
     * action string to execute the chosen app
     */
    final private static String asExecute = "execute";
    // *************************************************************************
    // fields

    /**
     * script to execute
     */
    private File script;
    /**
     * controller for the main screen
     */
    private GuiScreenController mainScreen;
    /**
     * index of the chosen app in the {@code mainClasses} array
     */
    private int chosenAppIndex = 0;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a GuiApplication without the usual initial appstates.
     */
    private AppChooser() {
        super((AppState[]) null);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the AppChooser application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Platform platform = JmeSystem.getPlatform();
        if (platform.getOs() == Platform.Os.MacOS) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }

        String title = applicationName + " " + MyString.join(arguments);
        AppChooser application = new AppChooser();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setAudioRenderer(null);
        settings.setResizable(true);
        settings.setSamples(4); // anti-aliasing
        settings.setTitle(title); // Customize the window's title bar.
        application.setSettings(settings);
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
        logger.log(Level.INFO, "Acorus version is {0}",
                MyString.quote(UiVersion.versionShort()));
        logger.log(Level.INFO, "jme3-utilities-nifty version is {0}",
                MyString.quote(LibraryVersion.versionShort()));
        InputMode inputMode = getDefaultInputMode();
        /*
         * Create and attach a controller for the main (and only) screen.
         */
        mainScreen = new GuiScreenController(
                "AppChooser/mainScreen",
                "Interface/Nifty/screens/AppChooser/mainScreen.xml",
                InitialState.Enabled);
        mainScreen.setListener(inputMode);
        boolean success = stateManager.attach(mainScreen);
        assert success;

        this.script = findScriptToExecute();
    }

    /**
     * Process an action that wasn't handled by the active InputMode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case asChooseApp:
                    String[] appNames = new String[mainClasses.length];
                    for (int i = 0; i < mainClasses.length; i++) {
                        appNames[i] = mainClasses[i].getSimpleName();
                    }
                    mainScreen.showPopupMenu(apChooseApp, appNames);
                    return;

                case asDeleteSettings:
                    Class<?> mainClass = mainClasses[chosenAppIndex];
                    String appName = mainClass.getSimpleName();
                    Heart.deleteStoredSettings(appName);
                    return;

                case asExecute:
                    Thread executor = new Thread("Executor") {
                        @Override
                        public void run() {
                            executeChosenApp();
                        }
                    };
                    executor.start();
                    return;

                default:
            }

            if (actionString.startsWith(apChooseApp)) {
                String arg = MyString.remainder(actionString, apChooseApp);
                for (int i = 0; i < mainClasses.length; i++) {
                    String appName = mainClasses[i].getSimpleName();
                    if (arg.equals(appName)) {
                        chosenAppIndex = i;
                        break;
                    }
                }
                return;
            }
        }
        /*
         * The action has not been handled: forward it to the superclass.
         */
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        /*
         * Update the status overlay.
         */
        Button appNameButton = mainScreen.getButton("appName");
        Class<?> mainClass = mainClasses[chosenAppIndex];
        String appName = mainClass.getSimpleName();
        appNameButton.setText(appName);
    }
    // *************************************************************************
    // private methods

    /**
     * Execute the chosen app.
     */
    private void executeChosenApp() {
        CommandLine commandLine = new CommandLine(script);

        Class<?> mainClass = mainClasses[chosenAppIndex];
        String mainClassName = mainClass.getName();
        commandLine.addArgument(mainClassName);

        DefaultExecutor executor = new DefaultExecutor();
        try {
            executor.execute(commandLine);
            // ignore the return code
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Look for the shell script (or batch file) to execute in
     * "./build/install/tests/bin".
     */
    private File findScriptToExecute() {
        File buildDir = new File("build");
        File installDir = new File(buildDir, "install");
        File examplesDir = new File(installDir, "tests");
        File binDir = new File(examplesDir, "bin");

        Platform platform = JmeSystem.getPlatform();
        String fileName = "tests";
        if (platform.getOs() == Platform.Os.Windows) {
            fileName += ".bat";
        }
        File result = new File(binDir, fileName);

        String path = Heart.fixedPath(result);
        if (!result.exists()) {
            String message = MyString.quote(path) + " not found!";
            throw new RuntimeException(message);
        }

        return result;
    }
}

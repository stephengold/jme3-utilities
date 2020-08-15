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

import com.jme3.app.StatsAppState;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.font.Rectangle;
import com.jme3.input.KeyInput;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.HelpUtils;
import jme3utilities.ui.InputMode;

/**
 * An ActionApplication to test the built-in cursors.
 */
public class TestCursors extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestCursors.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestCursors.class.getSimpleName();
    // *************************************************************************
    // fields

    private JmeCursor defaultCursor, dialogCursor, greenCursor, menuCursor;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestCursors application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        /*
         * Instantiate the application.
         */
        TestCursors application = new TestCursors();
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
        String defaultCursorPath = "Textures/cursors/default.cur";
        defaultCursor = (JmeCursor) assetManager.loadAsset(defaultCursorPath);

        String dialogCursorPath = "Textures/cursors/dialog.cur";
        dialogCursor = (JmeCursor) assetManager.loadAsset(dialogCursorPath);

        String greenCursorPath = "Textures/cursors/green.cur";
        greenCursor = (JmeCursor) assetManager.loadAsset(greenCursorPath);

        String menuCursorPath = "Textures/cursors/menu.cur";
        menuCursor = (JmeCursor) assetManager.loadAsset(menuCursorPath);
        /*
         * Avert warnings about signals not added.
         */
        flyCam.setEnabled(false);
        /*
         * Hide the render-statistics overlay.
         */
        stateManager.getState(StatsAppState.class).toggleStats();
    }

    /**
     * Add application-specific hotkey bindings and override any existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();
        dim.bind("cursor default", KeyInput.KEY_1);
        dim.bind("cursor dialog", KeyInput.KEY_2);
        dim.bind("cursor green", KeyInput.KEY_3);
        dim.bind("cursor menu", KeyInput.KEY_4);

        float x = 10f;
        float y = cam.getHeight() - 10f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle bounds = new Rectangle(x, y, width, height);

        float space = 20f;
        Node helpNode = HelpUtils.buildNode(dim, bounds, guiFont, space);
        guiNode.attachChild(helpNode);
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
        if (ongoing) {
            switch (actionString) {
                case "cursor default":
                    inputManager.setMouseCursor(defaultCursor);
                    return;
                case "cursor dialog":
                    inputManager.setMouseCursor(dialogCursor);
                    return;
                case "cursor green":
                    inputManager.setMouseCursor(greenCursor);
                    return;
                case "cursor menu":
                    inputManager.setMouseCursor(menuCursor);
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }
}

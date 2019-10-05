/*
 Copyright (c) 2017-2019, Stephen Gold
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
package jme3utilities.nifty;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

/**
 * Input mode for modal dialog boxes.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DialogInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DialogInputMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String cursorAssetPath = "Textures/cursors/dialog.cur";
    /**
     * short name for this input mode
     */
    final public static String name = "dialog";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    DialogInputMode() {
        super(name);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        boolean handled = false;

        if (ongoing) {
            logger.log(Level.INFO, "Got action {0}",
                    MyString.quote(actionString));
        }

        GuiApplication guiApplication = (GuiApplication) actionApplication;
        BasicScreenController controller = guiApplication.getEnabledScreen();
        if (isEnabled() && ongoing && controller != null) {
            PopScreenController psc = (PopScreenController) controller;
            assert psc.getActiveDialog() != null;
            /*
             * Attempt to handle the action.
             */
            switch (actionString) {
                case "cancel":
                    /*
                     * Close the dialog without performing any other action.
                     */
                    psc.closeActiveDialog();
                    handled = true;
                    break;

                case "commit":
                    /*
                     * Perform the "commit" action and then close the dialog.
                     */
                    psc.dialogCommit();
                    handled = true;
            }
        }

        if (!handled) {
            /*
             * The action is not handled: forward it to the application class.
             */
            guiApplication.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add the hotkey bindings.
     */
    @Override
    protected void defaultBindings() {
        bind("cancel", KeyInput.KEY_ESCAPE);
        bind("commit", KeyInput.KEY_RETURN);
        bind("SIMPLEAPP_HideStats", KeyInput.KEY_F5);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this (disabled) input mode prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(cursorAssetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }
}

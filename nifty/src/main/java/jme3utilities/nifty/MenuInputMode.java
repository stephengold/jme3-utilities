/*
 Copyright (c) 2013-2017, Stephen Gold
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
 * Input mode for popup menus.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MenuInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MenuInputMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String cursorAssetPath = "Textures/cursors/menu.cur";
    /**
     * short name for this mode
     */
    final public static String name = "menu";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    MenuInputMode() {
        super(name);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored time per frame (in seconds)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        if (!isEnabled()) {
            return;
        }
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));
        GuiScreenController screen =
                (GuiScreenController) GuiApplication.getEnabledScreen();
        if (screen == null) {
            return;
        }
        assert GuiScreenController.hasActivePopupMenu();
        if (actionString.equals("close")) {
            /*
             * Close the current popup menu.
             */
            GuiScreenController.closeActivePopupMenu();

        } else if (actionString.matches("select [1-9]")) {
            /*
             * Select a menu item based on its position, with "1"
             * selecting the first item.
             */
            String positionString = actionString.substring("select ".length());
            int position = Integer.parseInt(positionString);
            int index = position - 1;
            screen.selectMenuItem(index);

        } else {
            logger.log(Level.WARNING, "Action {0} was not handled.",
                    MyString.quote(actionString));
        }
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add the hotkey bindings.
     */
    @Override
    protected void defaultBindings() {
        bind("close", KeyInput.KEY_ESCAPE);
        bind("select 1", KeyInput.KEY_1);
        bind("select 2", KeyInput.KEY_2);
        bind("select 3", KeyInput.KEY_3);
        bind("select 4", KeyInput.KEY_4);
        bind("select 5", KeyInput.KEY_5);
        bind("select 6", KeyInput.KEY_6);
        bind("select 7", KeyInput.KEY_7);
        bind("select 8", KeyInput.KEY_8);
        bind("select 9", KeyInput.KEY_9);
    }

    /**
     * Initialize this (disabled) mode prior to its 1st update.
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

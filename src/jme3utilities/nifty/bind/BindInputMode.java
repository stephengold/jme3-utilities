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
package jme3utilities.nifty.bind;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.ui.Hotkey;
import jme3utilities.ui.InputMode;

/**
 * Input mode for the hotkey bindings editor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BindInputMode extends InputMode {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BindInputMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // fields
    
    /**
     * corresponding screen: set by constructor
     */
    final private BindScreen screen;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     *
     * @param screen corresponding screen (not null)
     */
    BindInputMode(BindScreen screen) {
        super("bind");
        Validate.nonNull(screen, "screen");
        this.screen = screen;
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
        screen.onAction(actionString, ongoing, ignored);
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add the default hotkey bindings.
     */
    @Override
    protected void defaultBindings() {
        for (Hotkey hotkey : Hotkey.collectAll()) {
            if (BindScreen.isExempt(hotkey)) {
                continue;
            }
            int keyCode = hotkey.keyCode();
            String actionString = String.format("select %d", keyCode);
            bind(actionString, keyCode);
        }
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
        Validate.nonNull(stateManager, "state manager");
        Validate.nonNull(application, "application");

        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        /*
         * Set the cursor's hotspot to work around GitHub issue #115.
         */
        cursor.setxHotSpot(0);
        cursor.setyHotSpot(31);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }
}
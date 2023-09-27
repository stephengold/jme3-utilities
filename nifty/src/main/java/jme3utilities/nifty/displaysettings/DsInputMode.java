/*
 Copyright (c) 2019-2022, Stephen Gold
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
package jme3utilities.nifty.displaysettings;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.ui.InputMode;

/**
 * Input mode for the display-settings editor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DsInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DsInputMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // fields

    /**
     * corresponding screen: set by constructor
     */
    final private DsScreen screen;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     *
     * @param screen corresponding screen (not null)
     */
    DsInputMode(DsScreen screen) {
        super("ds");
        Validate.nonNull(screen, "screen");
        this.screen = screen;
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add default hotkey bindings.
     */
    @Override
    protected void defaultBindings() {
        Platform platform = JmeSystem.getPlatform();
        if (platform.getOs() == Platform.Os.Linux) {
            bind("ScreenShot", KeyInput.KEY_SCROLL); // window mgr blocks SYSRQ
        } else {
            bind("ScreenShot", KeyInput.KEY_SYSRQ);
        }

        bind("return", KeyInput.KEY_ESCAPE);
        bind(SimpleApplication.INPUT_MAPPING_HIDE_STATS, KeyInput.KEY_F5);
        bind(SimpleApplication.INPUT_MAPPING_MEMORY, KeyInput.KEY_M);
    }

    /**
     * Initialize this (disabled) mode prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        Validate.nonNull(stateManager, "state manager");

        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }

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
}

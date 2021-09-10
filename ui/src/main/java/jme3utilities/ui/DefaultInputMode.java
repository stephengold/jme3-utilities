/*
 Copyright (c) 2013-2021, Stephen Gold
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
package jme3utilities.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.CameraInput;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;

/**
 * The default InputMode for an ActionApplication.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DefaultInputMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DefaultInputMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    DefaultInputMode() {
        super("default");
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the keyboard or mouse.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0} ongoing={1}", new Object[]{
            MyString.quote(actionString), ongoing
        });
        /*
         * Forward all actions to the ActionApplication subclass for processing.
         */
        actionApplication.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add default hotkey bindings to mimic SimpleApplication. These bindings
     * will be used if no custom bindings are found. They are handled by the
     * ActionApplication class.
     */
    @Override
    protected void defaultBindings() {
        bindSignal(CameraInput.FLYCAM_BACKWARD, KeyInput.KEY_S);
        bindSignal(CameraInput.FLYCAM_FORWARD, KeyInput.KEY_W);
        bindSignal(CameraInput.FLYCAM_LOWER, KeyInput.KEY_Z);
        bindSignal(CameraInput.FLYCAM_RISE, KeyInput.KEY_Q);
        bindSignal(CameraInput.FLYCAM_STRAFELEFT, KeyInput.KEY_A);
        bindSignal(CameraInput.FLYCAM_STRAFERIGHT, KeyInput.KEY_D);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            bind("ScreenShot", KeyInput.KEY_SCROLL); // window mgr blocks SYSRQ
        } else {
            bind("ScreenShot", KeyInput.KEY_SYSRQ);
        }

        bind(SimpleApplication.INPUT_MAPPING_CAMERA_POS, KeyInput.KEY_C);
        bind(SimpleApplication.INPUT_MAPPING_EXIT, KeyInput.KEY_ESCAPE);
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
    public void initialize(AppStateManager stateManager,
            Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        setCursor(cursor);
        /*
         * Delete any mappings added by SimpleApplication, in order
         * to avoid future warnings from the input manager.
         */
        InputManager im = application.getInputManager();
        for (String signalName : ActionApplication.flycamNames) {
            deleteAnyMapping(im, signalName);
        }
        deleteAnyMapping(im, "ScreenShot");
        deleteAnyMapping(im, SimpleApplication.INPUT_MAPPING_CAMERA_POS);
        deleteAnyMapping(im, SimpleApplication.INPUT_MAPPING_EXIT);
        deleteAnyMapping(im, SimpleApplication.INPUT_MAPPING_HIDE_STATS);
        deleteAnyMapping(im, SimpleApplication.INPUT_MAPPING_MEMORY);

        super.initialize(stateManager, application);
    }
    // *************************************************************************
    // private methods

    /**
     * Delete any mapping of the specified action string in the specified input
     * manager.
     *
     * @param inputManager which input manager (not null)
     * @param actionString which action string to unmap (not null)
     */
    private static void deleteAnyMapping(InputManager inputManager,
            String actionString) {
        assert inputManager != null;
        assert actionString != null;

        if (inputManager.hasMapping(actionString)) {
            inputManager.deleteMapping(actionString);
        }
    }
}

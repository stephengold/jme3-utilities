/*
 Copyright (c) 2013-2014, Stephen Gold
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
package jme3utilities.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.input.controls.ActionListener;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;

/**
 * A simple application with a Nifty graphical user interface (GUI). Extending
 * this class (instead of SimpleApplication) provides automatic initialization
 * of Nifty and easy access to the Nifty instance.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class GuiApplication
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(GuiApplication.class.getName());
    /**
     * asset path to the Nifty XML for generic popup menus
     */
    final private static String popupMenuAsssetPath =
            "Interface/Nifty/popup-menu.xml";
    // *************************************************************************
    // fields
    /**
     * the initial input mode: set in #simpleInitApp()
     */
    private InputMode defaultInputMode = null;
    /**
     * Nifty display: set in #simpleInitApp()
     */
    private NiftyJmeDisplay niftyDisplay = null;
    /**
     * signal tracker set in #simpleInitApp()
     */
    private Signals signals = null;
    // *************************************************************************
    // new public methods

    /**
     * Access the default input mode.
     *
     * @return pre-existing instance (not null)
     */
    public InputMode getDefaultInputMode() {
        assert defaultInputMode != null;
        return defaultInputMode;
    }

    /**
     * Access the Nifty instance.
     *
     * @return pre-existing instance (not null)
     */
    public Nifty getNifty() {
        Nifty result = getNiftyDisplay().getNifty();
        assert result != null;
        return result;
    }

    /**
     * Access the Nifty display instance.
     *
     * @return pre-existing instance (not null)
     */
    public NiftyJmeDisplay getNiftyDisplay() {
        assert niftyDisplay != null;
        return niftyDisplay;
    }

    /**
     * Access the signal tracker.
     *
     * @return pre-existing instance (not null)
     */
    public Signals getSignals() {
        assert signals != null;
        return signals;
    }

    /**
     * Callback to the user's application startup code.
     */
    abstract public void guiInitializeApplication();

    /**
     * Alter the effective speeds of all animations.
     *
     * @param newSpeed animation speed (>0, standard speed=1)
     */
    public void setSpeed(float newSpeed) {
        if (newSpeed <= 0f) {
            logger.log(Level.SEVERE, "speed={0}", newSpeed);
            throw new IllegalArgumentException("speed should be positive");
        }
        speed = newSpeed;
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard which is not handled by the
     * default input mode. This method is a placeholder which may be overridden
     * as desired.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }

        logger.log(Level.WARNING, "Action {0} was not handled.",
                MyString.quote(actionString));
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Startup code for the application: initialize Nifty with generic popup
     * menus, then invoke the user's guiInitApp().
     */
    @Override
    public void simpleInitApp() {
        assert defaultInputMode == null : defaultInputMode;
        assert niftyDisplay == null : niftyDisplay;
        assert signals == null : signals;
        /*
         * Initialize hotkeys and signal tracker for modal hotkeys.
         */
        signals = new Signals();
        Hotkey.intialize();
        /*
         * Attach and enable the default input mode.
         */
        defaultInputMode = new DefaultInputMode();
        stateManager.attach(defaultInputMode);
        defaultInputMode.setEnabled(true);
        /*
         * Attach the input mode for popup menus.
         */
        InputMode menuMode = new MenuInputMode();
        stateManager.attach(menuMode);
        /*
         * Start Nifty -- without the batched renderer!
         */
        niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager,
                audioRenderer, guiViewPort);

        Nifty nifty = getNifty();
        //nifty.setDebugOptionPanelColors(true);

        String niftyVersion = nifty.getVersion();
        logger.log(Level.INFO, "Nifty version is {0}",
                MyString.quote(niftyVersion));
        /*
         * Load the Nifty XML for generic popup menus.  For some reason the
         * asset does not validate, so skip validation for now.
         */
        nifty.addXml(popupMenuAsssetPath);
        /*
         * Invoke the user's startup code.
         */
        guiInitializeApplication();
    }
}
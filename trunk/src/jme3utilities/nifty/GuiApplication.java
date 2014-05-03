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
package jme3utilities.nifty;

import jme3utilities.nifty.BasicScreenController;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;

/**
 * Action application with a Nifty graphical user interface (GUI). Extending
 * this class (instead of SimpleApplication) provides automatic initialization
 * of Nifty and easy access to the Nifty instance.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class GuiApplication
        extends ActionApplication {
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
     * currently enabled screen controller (null means there's none)
     */
    private static BasicScreenController enabledScreen = null;
    /**
     * Nifty display: set in #simpleInitApp()
     */
    private NiftyJmeDisplay niftyDisplay = null;
    // *************************************************************************
    // new public methods

    /**
     * Access the screen controller which is currently enabled.
     *
     * @return pre-existing instance (or null if none)
     */
    public BasicScreenController getEnabledScreen() {
        return enabledScreen;
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
     * Callback to the startup code of the subclass.
     */
    abstract public void guiInitializeApplication();

    /**
     * Update which screen controller is enabled.
     *
     * @param newScreen (or null for none)
     */
    public static void setEnabledScreen(BasicScreenController newScreen) {
        assert newScreen == null || enabledScreen == null;
        enabledScreen = newScreen;
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Startup code for this class.
     */
    @Override
    public void actionInitializeApplication() {
        if (niftyDisplay != null) {
            throw new IllegalStateException(
                    "app should only be initialized once");
        }
        /*
         * Attach a (disabled) input mode for popup menus.
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
         * Invoke the startup code of the subclass.
         */
        guiInitializeApplication();
    }
}
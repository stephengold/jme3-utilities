/*
 Copyright (c) 2013-2020, Stephen Gold
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

import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;

/**
 * Action application with a Nifty graphical user interface (GUI). Extending
 * this class (instead of ActionApplication) provides automatic initialization
 * of Nifty and easy access to the Nifty instance.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class GuiApplication extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GuiApplication.class.getName());
    // *************************************************************************
    // fields

    /**
     * currently enabled screen controller (null means there's none)
     */
    private BasicScreenController enabledScreen = null;
    /**
     * true &rarr; sRGB colors, false &rarr; linear colors
     */
    private boolean colorsAsSrgbFlag = false;
    /**
     * Determine whether NiftyGUI is rendered after/over guiNode. No effect
     * after initialization. (true &rarr; NiftyGUI over guiNode, false &rarr;
     * guiNode over NiftyGUI)
     */
    private boolean niftyPostViewFlag = false;
    /**
     * Nifty display: set in {@link #actionInitializeApplication()}
     */
    private NiftyJmeDisplay niftyDisplay = null;
    // *************************************************************************
    // new public methods

    /**
     * Access the screen controller that is currently enabled.
     *
     * @return the pre-existing instance (or null if none)
     */
    public BasicScreenController getEnabledScreen() {
        return enabledScreen;
    }

    /**
     * Access the Nifty instance.
     *
     * @return the pre-existing instance (not null)
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
     * Alter which screen controller is enabled.
     *
     * @param newScreen (or null for none)
     */
    public void setEnabledScreen(BasicScreenController newScreen) {
        logger.log(Level.INFO, "newScreen={0}", newScreen);
        assert newScreen == null || enabledScreen == null;
        enabledScreen = newScreen;
    }

    /**
     * Assume that Nifty colors are specified in sRGB space.
     */
    public void setNiftyColorsAsSrgb() {
        if (niftyDisplay != null) {
            throw new IllegalStateException("too late - already initialized");
        }
        colorsAsSrgbFlag = true;
    }

    /**
     * Specify that the NiftyGUI should be rendered after/over the guiNode.
     * Invoke this prior to initialization.
     */
    public void setNiftyPostView() {
        if (niftyDisplay != null) {
            throw new IllegalStateException("too late - already initialized");
        }
        niftyPostViewFlag = true;
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
         * Attach (disabled) input modes for modal dialogs and popup menus.
         */
        InputMode dialogMode = new DialogInputMode();
        boolean success = stateManager.attach(dialogMode);
        assert success;
        InputMode menuMode = new MenuInputMode();
        success = stateManager.attach(menuMode);
        assert success;
        /*
         * Start NiftyGUI -- without the batched renderer!
         */
        niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager,
                audioRenderer, guiViewPort, colorsAsSrgbFlag);
        if (niftyPostViewFlag) {
            /*
             * Render the NiftyGUI after/over the guiNode.
             */
            int height = cam.getHeight();
            int width = cam.getWidth();
            Camera niftyCam = new Camera(width, height);
            ViewPort niftyView
                    = renderManager.createPostView("NiftyGUI", niftyCam);
            niftyView.addProcessor(niftyDisplay);
        }

        Nifty nifty = getNifty();
        //nifty.setDebugOptionPanelColors(true);

        String niftyVersion = nifty.getVersion();
        logger.log(Level.INFO, "Nifty version is {0}",
                MyString.quote(niftyVersion));
        nifty.loadControlFile("nifty-default-controls.xml");
        nifty.loadStyleFile("/Interface/Nifty/nifty-default-styles.xml");
        /*
         * Load the Nifty XML for generic popups.  For some reason the
         * assets do not validate, so skip validation.
         */
        String assetPath = "Interface/Nifty/popup-menu.xml";
        nifty.fromXmlWithoutStartScreen(assetPath);

        for (String dialogName : new String[]{
            "confirm", "info10", "multiSelect",
            "text-and-check", "text-and-slider", "text-entry"
        }) {
            assetPath = String.format("/Interface/Nifty/dialogs/%s.xml",
                    dialogName);
            nifty.fromXmlWithoutStartScreen(assetPath);
        }
        /*
         * Invoke the startup code of the subclass.
         */
        guiInitializeApplication();
    }
}

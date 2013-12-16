/*
 Copyright (c) 2013, Stephen Gold
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
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;

/**
 * A simple application with a Nifty graphical user interface (GUI).
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class GuiApplication
        extends SimpleApplication {
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
    final private static String popupPath = "Interface/Nifty/popup-menu.xml";
    // *************************************************************************
    // fields
    /**
     * GUI display: set in startGui()
     */
    protected NiftyJmeDisplay display = null;
    // *************************************************************************
    // new protected methods

    /*
     * Initialize the GUI.
     */
    protected void startGui() {
        assert display == null : display;

        display = new NiftyJmeDisplay(assetManager,
                inputManager, audioRenderer, guiViewPort);

        Nifty nifty = display.getNifty();
        //nifty.setDebugOptionPanelColors(true);

        String niftyVersion = nifty.getVersion();
        logger.log(Level.INFO, "Nifty version is {0}",
                MyString.quote(niftyVersion));
        /*
         * Validate and load the Nifty XML for generic popup menus.
         */
        try {
            nifty.validateXml(popupPath);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Nifty validation of "
                    + MyString.quote(popupPath) + " failed with exception:",
                    exception);
        }
        /*
         * Re-read the XML to add support for popup menus.
         */
        nifty.addXml(popupPath);
    }
}
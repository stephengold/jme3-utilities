/*
 Copyright (c) 2014-2017, Stephen Gold
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
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import java.util.logging.Logger;
import jme3utilities.ui.ActionAppState;

/**
 * Action app state with protected fields analogous to the private fields of
 * GuiApplication.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GuiAppState extends ActionAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            GuiAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * GUI application instance: set by initialize()
     */
    protected GuiApplication guiApplication;
    /**
     * Nifty instance: set by initialize()
     */
    protected Nifty nifty;
    /**
     * Nifty display: set by initialize()
     */
    protected NiftyJmeDisplay niftyDisplay;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an uninitialized state.
     *
     * @param enabled true &rarr; enabled, false &rarr; disabled
     */
    public GuiAppState(boolean enabled) {
        super(enabled);
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Initialize this app state on the 1st update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        if (!(app instanceof GuiApplication)) {
            throw new IllegalArgumentException(
                    "application should be a GuiApplication");
        }

        super.initialize(sm, app);

        guiApplication = (GuiApplication) app;
        nifty = guiApplication.getNifty();
        assert nifty != null;
        niftyDisplay = guiApplication.getNiftyDisplay();
        assert niftyDisplay != null;
    }
}

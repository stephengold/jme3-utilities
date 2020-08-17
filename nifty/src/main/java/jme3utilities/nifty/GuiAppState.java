/*
 Copyright (c) 2014-2020, Stephen Gold
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
import de.lessvoid.nifty.Nifty;
import java.util.logging.Logger;
import jme3utilities.ui.ActionAppState;

/**
 * An ActionAppState in a GuiApplication.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GuiAppState extends ActionAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GuiAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * GUI application instance: set by initialize()
     */
    private GuiApplication guiApplication;
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
    // new methods exposed

    /**
     * Access the Nifty instance.
     *
     * @return the pre-existing instance (not null)
     */
    public Nifty getNifty() {
        Nifty result = guiApplication.getNifty();

        assert result != null;
        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Access the application instance.
     *
     * @return the pre-existing instance (not null)
     */
    protected GuiApplication getApplication() {
        assert guiApplication != null;
        return guiApplication;
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Initialize this app state on the first update after it gets attached.
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
    }
}

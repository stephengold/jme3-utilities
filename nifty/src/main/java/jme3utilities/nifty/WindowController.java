/*
 Copyright (c) 2017-2023 Stephen Gold
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
import de.lessvoid.nifty.controls.Window;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import java.util.logging.Logger;
import jme3utilities.InitialState;
import jme3utilities.Validate;

/**
 * GUI app state to control a Nifty window. The window is displayed if and only
 * if its controller is enabled. Multiple overlapping windows can be displayed
 * at the same time.
 * <p>
 * Each instance is enabled at creation, with an option for automatic disabling
 * during initialization.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class WindowController extends GuiAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(WindowController.class.getName());
    // *************************************************************************
    // fields

    /**
     * the controller of the screen containing the window
     */
    final private BasicScreenController screenController;
    /**
     * if false, disable this controller during initialization; if true, leave
     * it enabled
     */
    private boolean startEnabled;
    /**
     * the Nifty id of the window control
     */
    final private String controlId;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen containing the
     * window (not null, alias created)
     * @param controlId the Nifty id of the window control
     * @param startEnabled if false, disable this controller during
     * initialization; if true, leave it enabled
     */
    public WindowController(BasicScreenController screenController,
            String controlId, boolean startEnabled) {
        super(InitialState.Enabled);
        Validate.nonNull(screenController, "screen controller");
        Validate.nonNull(controlId, "control id");

        this.screenController = screenController;
        this.controlId = controlId;
        this.startEnabled = startEnabled;

        assert !isInitialized();
        assert isEnabled();
    }

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen containing the
     * window (not null, alias created)
     * @param controlId the Nifty id of the window control
     * @param initialState if Disabled or null, disable this controller during
     * initialization; if Enabled, leave it enabled
     */
    public WindowController(BasicScreenController screenController,
            String controlId, InitialState initialState) {
        super(InitialState.Enabled);
        Validate.nonNull(screenController, "screen controller");
        Validate.nonNull(controlId, "control id");

        this.screenController = screenController;
        this.controlId = controlId;
        this.startEnabled = (initialState == InitialState.Enabled);

        assert !isInitialized();
        assert isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the window's Nifty element.
     *
     * @return the pre-existing instance (not null)
     */
    public Element getElement() {
        Element element = getWindow().getElement();
        assert element != null;
        return element;
    }

    /**
     * Access the controller for the screen which contains the window.
     *
     * @return the pre-existing instance (not null)
     */
    public BasicScreenController getScreenController() {
        assert screenController != null;
        return screenController;
    }

    /**
     * Maximize the window's prominence: make sure it's displayed and then move
     * it to the front.
     */
    public void select() {
        setEnabled(true);
        getWindow().moveToFront();
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its first update.
     *
     * @param stateManager (not null)
     * @param application application which owns the window (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);

        assert screenController.isInitialized();
        screenController.addWindowController(this);

        if (!startEnabled) {
            hide();
        }

        assert isInitialized() : controlId;
    }

    /**
     * Display or hide the window.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
        if (!isInitialized()) { // Defer until initialization.
            this.startEnabled = newSetting;
            return;
        }

        if (newSetting && !isEnabled()) {
            display();
        } else if (!newSetting && isEnabled()) {
            hide();
        }
    }

    /**
     * Update this controller prior to rendering. (Invoked once per frame.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        boolean isDisplayed = getElement().isVisible();
        if (isDisplayed != isEnabled()) {
            setEnabled(isDisplayed);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Display the window without bringing it to the front.
     */
    private void display() {
        assert isInitialized() : controlId;
        assert !isEnabled() : controlId;

        getElement().setVisible(true);
        super.setEnabled(true);
    }

    /**
     * Access the Nifty control of the window.
     *
     * @return the pre-existing instance (not null)
     */
    private Window getWindow() {
        Screen screen = screenController.getScreen();
        Window window = screen.findNiftyControl(controlId, Window.class);

        assert window != null : controlId;
        return window;
    }

    /**
     * Hide the window.
     */
    private void hide() {
        assert isInitialized() : controlId;
        assert isEnabled() : controlId;

        getElement().setVisible(false);
        super.setEnabled(false);
    }
}

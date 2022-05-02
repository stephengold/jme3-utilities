/*
 Copyright (c) 2014-2022, Stephen Gold
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
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector2f;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventAnnotationProcessor;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.InputMode;

/**
 * A GuiAppState to control a Nifty screen. A screen is displayed if and only if
 * its controller is enabled. No more than one screen can be enabled/displayed
 * at a time.
 * <p>
 * Each instance is disabled at creation, with an option for automatic enabling
 * during initialization.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BasicScreenController
        extends GuiAppState
        implements ScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BasicScreenController.class.getName());
    // *************************************************************************
    // fields

    /**
     * action listener for GUI actions from the controlled screen
     */
    private ActionListener listener = null;
    /**
     * if true, enable this controller during initialization; if false, leave it
     * disabled: set by constructor
     */
    private boolean enableDuringInitialization;
    /**
     * false before the controlled screen starts, then true ever after
     */
    private boolean hasStarted = false;
    /**
     * Nifty id of the controlled screen: set by constructor
     */
    final private String screenId;
    /**
     * path to the Nifty XML layout asset for the controlled screen: set by
     * constructor
     */
    final private String xmlAssetPath;
    /**
     * list of controllers for all initialized windows in the screen
     */
    final private List<WindowController> windowControllers
            = new ArrayList<>(20);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled screen for the specified screen id
     * and layout.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization if true, enable this screen controller
     * during initialization; if false, leave it disabled
     */
    public BasicScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        super(InitialState.Disabled);
        Validate.nonNull(screenId, "screen id");
        Validate.nonNull(xmlAssetPath, "asset path");

        this.screenId = screenId;
        this.xmlAssetPath = xmlAssetPath;
        this.enableDuringInitialization = enableDuringInitialization;

        assert !isInitialized();
        assert !isEnabled();
    }

    /**
     * Instantiate an uninitialized, disabled screen for the specified screen id
     * and layout.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param initialState if Enabled, enable this controller during
     * initialization; if Disabled or null, leave it disabled
     */
    public BasicScreenController(String screenId, String xmlAssetPath,
            InitialState initialState) {
        super(InitialState.Disabled);
        Validate.nonNull(screenId, "screen id");
        Validate.nonNull(xmlAssetPath, "asset path");

        this.screenId = screenId;
        this.xmlAssetPath = xmlAssetPath;
        this.enableDuringInitialization
                = (initialState == InitialState.Enabled);

        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Associate a specified window controller with the screen.
     *
     * @param controller (not null)
     */
    void addWindowController(WindowController controller) {
        windowControllers.add(controller);
    }

    /**
     * Access the listener of this screen.
     *
     * @return the pre-existing instance (not null)
     */
    public ActionListener getListener() {
        assert listener != null;
        return listener;
    }

    /**
     * Access the controlled screen.
     *
     * @return the pre-existing instance (not null)
     */
    public Screen getScreen() {
        Screen screen = getNifty().getScreen(screenId);
        assert screen != null;
        return screen;
    }

    /**
     * Read the Nifty screen id.
     *
     * @return id string (not null)
     */
    public String getScreenId() {
        assert screenId != null;
        return screenId;
    }

    /**
     * Test whether Nifty has started the controlled screen yet. As long as the
     * screen has not started, events from the screen should be ignored.
     *
     * @return true if started, false if not started yet
     */
    public boolean hasStarted() {
        return hasStarted;
    }

    /**
     * Test whether the mouse cursor is inside the identified element of this
     * screen.
     *
     * @param elementId Nifty id of the element (not null)
     * @return true if the mouse is inside, false if it's outside
     */
    public boolean isMouseInsideElement(String elementId) {
        Validate.nonNull(elementId, "element id");

        if (!isEnabled()) {
            return false;
        }
        Element element = getScreen().findElementById(elementId);
        if (element == null) {
            /*
             * non-existent element
             */
            return false;
        }

        Vector2f mouseXY = inputManager.getCursorPosition();
        int mouseX = Math.round(mouseXY.x);
        /*
         * Nifty Y-coordinates increase in the opposite direction
         * from those reported by the input manager, so subtract Y
         * from the display height.
         */
        int displayHeight = cam.getHeight();
        int mouseY = displayHeight - Math.round(mouseXY.y);
        boolean result = element.isMouseInsideElement(mouseX, mouseY);

        return result;
    }

    /**
     * List the controllers of all initialized windows in the screen.
     *
     * @return a new collection
     */
    public Collection<WindowController> listWindowControllers() {
        int numWindows = windowControllers.size();
        Collection<WindowController> result = new ArrayList<>(numWindows);
        result.addAll(windowControllers);

        return result;
    }

    /**
     * Perform the action described by the specified action string using the
     * screen's listener, even if it's inactive. Invoked by means of reflection,
     * so both the class and method must be public.
     *
     * @param actionString (not null)
     */
    public void perform(String actionString) {
        Validate.nonNull(actionString, "action string");
        logger.log(Level.INFO, "actionString={0}",
                MyString.quote(actionString));

        BasicScreenController screen = getApplication().getEnabledScreen();
        ActionListener actionListener = screen.getListener();
        boolean isOngoing = true;
        float tpf = 0f;
        try {
            actionListener.onAction(actionString, isOngoing, tpf);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            getApplication().stop(false);
        }
    }

    /**
     * Perform the action described by the specified action string using the
     * active input mode. Invoked by means of reflection, so both the class and
     * method must be public.
     *
     * @param actionString (not null)
     */
    public void performActive(String actionString) {
        Validate.nonNull(actionString, "action string");
        logger.log(Level.INFO, "actionString={0}",
                MyString.quote(actionString));

        ActionListener actionListener = InputMode.getActiveMode();
        boolean isOngoing = true;
        float tpf = 0f;
        try {
            actionListener.onAction(actionString, isOngoing, tpf);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            getApplication().stop(false);
        }
    }

    /**
     * Alter the listener for GUI actions from this screen.
     *
     * @param newListener (not null)
     */
    final public void setListener(ActionListener newListener) {
        Validate.nonNull(newListener, "listener");
        listener = newListener;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Disable this controller. Assumes it is initialized and enabled.
     */
    protected void disable() {
        assert isInitialized();
        assert isEnabled();

        BasicScreenController enabledScreen
                = getApplication().getEnabledScreen();
        assert enabledScreen == this : enabledScreen;
        logger.log(Level.INFO, "screenId={0}", MyString.quote(screenId));
        /*
         * Detach Nifty from the viewport.
         */
        NiftyJmeDisplay display = getApplication().getNiftyDisplay();
        guiViewPort.removeProcessor(display);

        NiftyEventAnnotationProcessor.unprocess(this);

        getApplication().setEnabledScreen(null);
        super.setEnabled(false);
    }

    /**
     * Enable this controller for a particular listener. Assumes this controller
     * is initialized and disabled.
     */
    protected void enable() {
        assert isInitialized();
        assert !isEnabled();
        logger.log(Level.INFO, "screenId={0}", MyString.quote(screenId));
        /*
         * Attach Nifty to the viewport.
         */
        NiftyJmeDisplay display = getApplication().getNiftyDisplay();
        guiViewPort.addProcessor(display);

        getNifty().gotoScreen(screenId);
        NiftyEventAnnotationProcessor.process(this);

        getApplication().setEnabledScreen(this);
        super.setEnabled(true);
    }
    // *************************************************************************
    // GuiAppState methods

    /**
     * Clean up this state during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        if (isEnabled()) {
            disable();
        }

        super.cleanup();
    }

    /**
     * Initialize this controller prior to its first update.
     *
     * @param stateManager (not null)
     * @param application application which owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        getNifty().registerScreenController(this);
        validateAndLoad();
        if (enableDuringInitialization) {
            enable();
        }

        assert isInitialized();
    }

    /**
     * Enable or disable this screen.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    final public void setEnabled(boolean newSetting) {
        if (!isInitialized()) {
            /*
             * Defer until initialization.
             */
            enableDuringInitialization = newSetting;
            return;
        }
        if (listener == null) {
            throw new IllegalStateException("listener should be set");
        }

        if (newSetting && !isEnabled()) {
            enable();
        } else if (!newSetting && isEnabled()) {
            disable();
        }
    }
    // *************************************************************************
    // ScreenController methods

    /**
     * A callback which Nifty invokes when the screen gets enabled for the first
     * time, used for assertions only.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
        Validate.require(nifty == getNifty(), "same Nifty instance");
        Validate.require(screen == getScreen(), "same screen");
    }

    /**
     * A callback from Nifty, invoked each time the screen shuts down.
     */
    @Override
    public void onEndScreen() {
    }

    /**
     * A callback from Nifty, invoked each time the screen starts up.
     */
    @Override
    public void onStartScreen() {
        hasStarted = true;
    }
    // *************************************************************************
    // private methods

    /**
     * Validate and load a Nifty screen layout from an XML asset.
     */
    private void validateAndLoad() {
        assert xmlAssetPath != null;
        logger.log(Level.INFO, "xmlAssetPath={0}", xmlAssetPath);
        /*
         * Read and validate the screen's interface XML and build the interface.
         *
         * Warnings from Nifty's logger are disabled in case the screen
         * wants to override registered styles.
         */
        Logger niftyLogger = Logger.getLogger(Nifty.class.getName());
        Level save = niftyLogger.getLevel();
        niftyLogger.setLevel(Level.SEVERE);
        try {
            getNifty().addXml(xmlAssetPath);
        } catch (Throwable exception) {
            String message = "while loading ScreenController layout from asset "
                    + MyString.quote(xmlAssetPath);
            throw new RuntimeException(message, exception);
        }
        niftyLogger.setLevel(save);
    }
}

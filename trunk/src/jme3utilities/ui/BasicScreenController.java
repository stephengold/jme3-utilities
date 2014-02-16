/*
 Copyright (c) 2014, Stephen Gold
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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector2f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventAnnotationProcessor;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.debug.Validate;

/**
 * An app state to control a Nifty screen. A screen is displayed if and only if
 * its app state is enabled. At most one screen is displayed at a time.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class BasicScreenController
        extends AbstractAppState
        implements ScreenController {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(BasicScreenController.class.getName());
    // *************************************************************************
    // fields
    /**
     * action listener for GUI actions from the screen
     */
    private ActionListener listener = null;
    /**
     * if true, enable this screen controller during initialization; if false,
     * leave it disabled: set by constructor
     */
    final private boolean enableDuringInitialization;
    /**
     * false before this screen controller starts, then true ever after
     */
    private boolean hasStarted = false;
    /**
     * application instance shared by all screens: set by initialize()
     */
    private static GuiApplication application = null;
    /**
     * Nifty id of this screen: set by constructor
     */
    final private String screenId;
    /**
     * path to the Nifty XML layout asset for this screen: set by constructor
     */
    final private String xmlAssetPath;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen for a particular screen id and layout.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization
     */
    public BasicScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        Validate.nonNull(screenId, "screen id");
        Validate.nonNull(xmlAssetPath, "path");

        this.xmlAssetPath = xmlAssetPath;
        this.screenId = screenId;
        this.enableDuringInitialization = enableDuringInitialization;

        super.setEnabled(false);

        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the mouse cursor is inside a particular element of this
     * screen.
     *
     * @param elementId the Nifty id of the element (not null)
     * @return true if the mouse is
     */
    public boolean isMouseInsideElement(String elementId) {
        Validate.nonNull(elementId, "element id");

        if (!isEnabled()) {
            return false;
        }
        Element element = getScreen().findElementByName(elementId);
        if (element == null) {
            /*
             * non-existent element
             */
            return false;
        }

        Vector2f mouseXY = application.getInputManager().getCursorPosition();
        int mouseX = Math.round(mouseXY.x);
        /*
         * Nifty Y-coordinates increase in the opposite direction
         * from those reported by the input manager, so subtract Y
         * from the display height.
         */
        int displayHeight = application.getCamera().getHeight();
        int mouseY = displayHeight - Math.round(mouseXY.y);
        boolean result = element.isMouseInsideElement(mouseX, mouseY);
        return result;
    }

    /**
     * Perform the action specified by an action string. Invoked by means of
     * reflection, so both the class and method must be public.
     *
     * @param actionString (not null)
     */
    public static void perform(String actionString) {
        Validate.nonNull(actionString, "action string");

        logger.log(Level.INFO, "actionString={0}",
                MyString.quote(actionString));
        boolean isOnGoing = true;
        float simInterval = 0f;
        ActionListener actionListener = application.getEnabledScreen().listener;
        try {
            actionListener.onAction(actionString, isOnGoing, simInterval);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            application.stop(false);
        }
    }

    /**
     * Alter the listener for GUI actions from this screen.
     *
     * @param newListener (not null)
     */
    public void setListener(ActionListener newListener) {
        Validate.nonNull(newListener, "listener");

        listener = newListener;
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this controller.
     *
     * @param stateManager (not null)
     * @param application which application owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }
        Validate.nonNull(stateManager, "state manager");
        Validate.nonNull(application, "application");
        if (!(application instanceof GuiApplication)) {
            throw new IllegalArgumentException(
                    "application should be a GuiApplication");
        }

        super.initialize(stateManager, application);
        setApplication((GuiApplication) application);
        getNifty().registerScreenController(this);
        validateAndLoad();
        if (enableDuringInitialization) {
            setEnabled(true);
        }

        assert isInitialized();
    }

    /**
     * Enable or disable this screen.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean newState) {
        if (listener == null) {
            throw new IllegalStateException("listener should be set");
        }

        if (newState && !isEnabled()) {
            enable();
        } else if (!newState && isEnabled()) {
            disable();
        }
    }
    // *************************************************************************
    // ScreenController methods

    /**
     * A callback which Nifty invokes when the screen becomes enabled, used for
     * assertions only.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
        assert nifty == getNifty() : nifty;
        assert screen == getScreen() : screen;
    }

    /**
     * A callback from Nifty, unused.
     */
    @Override
    public void onEndScreen() {
    }

    /**
     * A callback from Nifty, invoked when the screen starts up.
     */
    @Override
    public void onStartScreen() {
        hasStarted = true;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Access the application which owns this screen.
     *
     * @return the pre-existing instance (not null)
     */
    protected GuiApplication getApplication() {
        assert application != null;
        return application;
    }

    /**
     * Access the Nifty instance.
     *
     * @return the pre-existing instance (not null)
     */
    protected static Nifty getNifty() {
        Nifty result = application.getNifty();
        assert result != null;
        return result;
    }

    /**
     * Access the Nifty Screen instance.
     *
     * @return the pre-existing instance (not null)
     */
    protected Screen getScreen() {
        Screen screen = getNifty().getScreen(screenId);
        assert screen != null;
        return screen;
    }

    /**
     * Read the Nifty screen id.
     */
    protected String getScreenId() {
        assert screenId != null;
        return screenId;
    }

    /**
     * Test whether Nifty has started the screen yet. As long as the screen has
     * not started, Nifty events sent to it should be ignored.
     *
     * @return true if started, false if not started yet
     */
    protected boolean hasStarted() {
        return hasStarted;
    }
    // *************************************************************************
    // private methods

    /**
     * Disable this controller. Assumes it is initialized and enabled.
     */
    private void disable() {
        assert isInitialized();
        assert isEnabled();
        BasicScreenController enabledScreen = application.getEnabledScreen();
        assert enabledScreen == this : enabledScreen;
        logger.log(Level.INFO, "screenId={0}", MyString.quote(screenId));
        /*
         * Detatch Nifty from the viewport.
         */
        ViewPort viewPort = application.getGuiViewPort();
        NiftyJmeDisplay niftyDisplay = application.getNiftyDisplay();
        viewPort.removeProcessor(niftyDisplay);

        NiftyEventAnnotationProcessor.unprocess(this);

        GuiApplication.setEnabledScreen(null);
        super.setEnabled(false);
    }

    /**
     * Enable this controller for a particular listener. Assumes this controller
     * is initialized and disabled.
     */
    private void enable() {
        assert isInitialized();
        assert !isEnabled();
        logger.log(Level.INFO, "screenId={0}", MyString.quote(screenId));
        /*
         * Attach Nifty to the viewport.
         */
        ViewPort viewPort = application.getGuiViewPort();
        NiftyJmeDisplay niftyDisplay = application.getNiftyDisplay();
        viewPort.addProcessor(niftyDisplay);

        getNifty().gotoScreen(screenId);
        NiftyEventAnnotationProcessor.process(this);

        GuiApplication.setEnabledScreen(this);
        super.setEnabled(true);
    }

    /**
     * Save a static reference to the current application.
     *
     * @param app (not null)
     */
    private static synchronized void setApplication(GuiApplication app) {
        assert app != null;

        if (application != app) {
            assert application == null : application;
            application = app;
        }
    }

    /**
     * Validate and load a Nifty screen layout from an XML asset.
     */
    private void validateAndLoad() {
        assert xmlAssetPath != null;

        Nifty nifty = getNifty();
        /*
         * Read and validate the interface XML.
         */
        try {
            nifty.validateXml(xmlAssetPath);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Nifty validation of "
                    + MyString.quote(xmlAssetPath)
                    + " failed with exception:",
                    exception);
        }
        /*
         * Re-read the XML and build the interface.
         */
        nifty.addXml(xmlAssetPath);
    }
}
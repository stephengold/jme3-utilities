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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector2f;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventAnnotationProcessor;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * GUI app state to control a Nifty screen. A screen is displayed if and only if
 * its app state is enabled. At most one screen is displayed at a time.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BasicScreenController
        extends GuiAppState
        implements ScreenController {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BasicScreenController.class.getName());
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
    private boolean enableDuringInitialization;
    /**
     * false before this screen controller starts, then true ever after
     */
    private boolean hasStarted = false;
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
     * Instantiate an uninitialized, disabled screen for the specified screen 
     * id and layout.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization if true, enable this screen controller
     * during initialization; if false, leave it disabled
     */
    public BasicScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        super(false);
        Validate.nonNull(screenId, "screen id");
        Validate.nonNull(xmlAssetPath, "asset path");

        this.screenId = screenId;
        this.xmlAssetPath = xmlAssetPath;
        this.enableDuringInitialization = enableDuringInitialization;

        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

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
        Element element = getScreen().findElementByName(elementId);
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
     * Perform the action described by an action string. Invoked by means of
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
        ActionListener actionListener =
                guiApplication.getEnabledScreen().getListener();
        try {
            actionListener.onAction(actionString, isOnGoing, simInterval);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            guiApplication.stop(false);
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
    // SimpleAppState methods

    /**
     * Enable or disable this screen.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
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
    // GuiAppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        nifty.registerScreenController(this);
        validateAndLoad();
        if (enableDuringInitialization) {
            enable();
        }

        assert isInitialized();
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
        assert nifty == BasicScreenController.nifty : nifty;
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
     * Access the Nifty instance.
     *
     * @return pre-existing instance (not null)
     */
    protected static Nifty getNifty() {
        assert nifty != null;
        return nifty;
    }

    /**
     * Access the Nifty Screen instance.
     *
     * @return pre-existing instance (not null)
     */
    protected Screen getScreen() {
        Screen screen = nifty.getScreen(screenId);
        assert screen != null;
        return screen;
    }

    /**
     * Read the Nifty screen id.
     *
     * @return id string (not null)
     */
    protected String getScreenId() {
        assert screenId != null;
        return screenId;
    }

    /**
     * Test whether Nifty has started the screen yet. As long as the screen has
     * not started, Nifty events should be ignored.
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
        BasicScreenController enabledScreen = guiApplication.getEnabledScreen();
        assert enabledScreen == this : enabledScreen;
        logger.log(Level.INFO, "screenId={0}", MyString.quote(screenId));
        /*
         * Detatch Nifty from the viewport.
         */
        guiViewPort.removeProcessor(niftyDisplay);

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
        guiViewPort.addProcessor(niftyDisplay);

        nifty.gotoScreen(screenId);
        NiftyEventAnnotationProcessor.process(this);

        GuiApplication.setEnabledScreen(this);
        super.setEnabled(true);
    }

    /**
     * Access the listener of this screen.
     */
    private ActionListener getListener() {
        return listener;
    }

    /**
     * Validate and load a Nifty screen layout from an XML asset.
     */
    private void validateAndLoad() {
        assert xmlAssetPath != null;
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

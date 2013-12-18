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

import com.google.common.base.Joiner;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventAnnotationProcessor;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;

/**
 * A simplified controller for a Nifty screen, with support for check boxes,
 * popup menus, radio buttons, sliders, and dynamic labels.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SimpleScreenController
        extends AbstractAppState
        implements ScreenController {
    // *************************************************************************
    // constants

    /**
     * joiner for constructing action prefixes
     */
    final private static Joiner actionJoiner = Joiner.on(" ");
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(SimpleScreenController.class.getName());
    // *************************************************************************
    // fields
    /**
     * which action listener handles GUI actions for the screen that's currently
     * enabled (or null for none)
     */
    private static volatile ActionListener listener = null;
    /**
     * false before the screen starts, true thereafter
     */
    protected boolean screenHasStarted = false;
    /**
     * reference to the application instance: set by initialize()
     */
    private static GuiApplication application = null;
    /**
     * the active popup menu (null means none are active)
     */
    private static PopupMenu activePopupMenu = null;
    /**
     * Nifty id of this screen: set by constructor
     */
    final protected String screenId;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen for a particular screen id. After
     * instantiation, the first method invoked should be initialize().
     *
     * @param screenId Nifty screen id (not null)
     */
    public SimpleScreenController(String screenId) {
        if (screenId == null) {
            throw new NullPointerException("id should not be null");
        }
        this.screenId = screenId;

        super.setEnabled(false);

        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Close the active popup menu. Invoked from NiftyMethodInvoker using
     * reflection, so the class and method must both be public.
     */
    public void closeActivePopup() {
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup");
        }

        activePopupMenu.close();
        activePopupMenu = activePopupMenu.getParent();
        if (activePopupMenu != null) {
            /*
             * Re-enable the parent menu.
             */
            activePopupMenu.setEnabled(true);
        }
    }

    /**
     * If the specified popup menu is active, close it and all its ancestors.
     *
     * @param popupMenu which menu to close (not null)
     */
    void closePopup(PopupMenu popupMenu) {
        if (popupMenu == null) {
            throw new NullPointerException("popup menu should not be null");
        }
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup menu");
        }

        if (popupMenu != activePopupMenu) {
            return;
        }
        popupMenu.close();
        PopupMenu ancestor = popupMenu.getParent();
        while (ancestor != null) {
            ancestor.close();
            ancestor = ancestor.getParent();
        }
        activePopupMenu = null;
    }

    /**
     * Disable this controller. Assumes it is initialized and enabled.
     */
    public void disable() {
        if (!isInitialized()) {
            throw new IllegalStateException("not initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("already disabled");
        }
        assert listener != null;
        /*
         * Detatch Nifty from the viewport.
         */
        ViewPort viewPort = application.getGuiViewPort();
        NiftyJmeDisplay niftyDisplay = application.getNiftyDisplay();
        viewPort.removeProcessor(niftyDisplay);

        NiftyEventAnnotationProcessor.unprocess(this);

        listener = null;
        super.setEnabled(false);
    }

    /**
     * Enable this controller for a particular listener. Assumes this controller
     * is initialized and disabled.
     *
     * @param newListener new listener for GUI actions (not null)
     */
    public void enable(ActionListener newListener) {
        if (newListener == null) {
            throw new NullPointerException("listener should not be null");
        }
        if (!isInitialized()) {
            throw new IllegalStateException("not initialized");
        }
        if (isEnabled()) {
            throw new IllegalStateException("already enabled");
        }
        assert listener == null : listener;
        /*
         * Attach Nifty to the viewport.
         */
        ViewPort viewPort = application.getGuiViewPort();
        NiftyJmeDisplay niftyDisplay = application.getNiftyDisplay();
        viewPort.addProcessor(niftyDisplay);

        getNifty().gotoScreen(screenId);
        NiftyEventAnnotationProcessor.process(this);

        listener = newListener;
        super.setEnabled(true);
    }

    /**
     * Access the Nifty screen.
     *
     * @return the pre-existing instance (not null)
     */
    public Screen getScreen() {
        Screen screen = getNifty().getScreen(screenId);
        assert screen != null;
        return screen;
    }

    /**
     * Identify the screen's active popup element.
     *
     * @return true if there's an active popup element, false if there's none
     */
    public boolean hasActivePopup() {
        return activePopupMenu != null;
    }

    /**
     * Test whether the mouse cursor is inside a particular element of this
     * screen.
     *
     * @param elementId the ID string of the element (not null)
     * @return true if the mouse is
     */
    public boolean isMouseInsideElement(String elementId) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }

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

        Vector2f mouseXY = getInputManager().getCursorPosition();
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
        if (actionString == null) {
            throw new NullPointerException("action string should not be null");
        }

        logger.log(Level.INFO, "actionString={0}", actionString);
        boolean isOnGoing = true;
        float simInterval = 0f;
        try {
            listener.onAction(actionString, isOnGoing, simInterval);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            application.stop(false);
        }
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param items collection of menu items (not null, unaffected)
     */
    public void showPopup(String actionPrefix, Collection<String> items) {
        if (actionPrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }
        if (items == null) {
            throw new NullPointerException("collection should not be null");
        }

        String[] itemArray = Misc.toArray(items);
        showPopup(actionPrefix, itemArray);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param itemArray menu items (not null, unaffected)
     */
    public void showPopup(String actionPrefix, String[] itemArray) {
        if (actionPrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }
        /*
         * Create the popup using "popup-menu" as a base.
         * Nifty assigns the popup a new id.
         */
        Nifty nifty = getNifty();
        Element element = nifty.createPopup("popup-menu");
        /*
         * Add items to the popup's menu.
         */
        Menu<String> menu = element.findNiftyControl("#menu", Menu.class);
        for (String item : itemArray) {
            menu.addMenuItem(item, item);
        }
        String elementId = element.getId();
        assert elementId != null;
        /*
         * Create a controller with a subscription to the menu's
         * item activation events.
         */
        PopupMenu popup = new PopupMenu(this, elementId, actionPrefix,
                activePopupMenu);
        Screen screen = nifty.getCurrentScreen();
        String controlId = menu.getId();
        nifty.subscribe(screen, controlId, MenuItemActivatedEvent.class, popup);
        /*
         * Make the popup menu visible.
         */
        nifty.showPopup(screen, elementId, null);

        if (activePopupMenu != null) {
            /*
             * Disable the parent menu.
             */
            activePopupMenu.setEnabled(false);
        }
        activePopupMenu = popup;
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefixWords common prefix words of the menu's action strings
     * (not null, unaffected)
     * @param itemArray menu items (not null, unaffected)
     */
    public void showPopup(String[] actionPrefixWords, String[] itemArray) {
        if (actionPrefixWords == null) {
            throw new NullPointerException("word array should not be null");
        }
        if (itemArray == null) {
            throw new NullPointerException("item array should not be null");
        }

        /*
         * Generate the action prefix for this menu.
         */
        String actionPrefix = "";
        int wordCount = actionPrefixWords.length;
        if (wordCount > 0) {
            actionPrefix = actionJoiner.join(actionPrefixWords) + " ";
        }
        showPopup(actionPrefix, itemArray);
    }

    /**
     * Validate and load a Nifty interface description.
     *
     * @param interfaceAssetPath (not null)
     */
    public void validateAndLoad(String interfaceAssetPath) {
        if (interfaceAssetPath == null) {
            throw new NullPointerException("path should not be null");
        }

        Nifty nifty = getNifty();
        /*
         * Read and validate the interface XML.
         */
        try {
            nifty.validateXml(interfaceAssetPath);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Nifty validation of "
                    + MyString.quote(interfaceAssetPath)
                    + " failed with exception:",
                    exception);
        }
        /*
         * Re-read the XML and build the interface.
         */
        nifty.addXml(interfaceAssetPath);
    }

    /**
     * Validate and load a HUD interface description.
     */
    public void validateAndLoadHud() {
        String assetPath =
                String.format("Interface/Nifty/huds/%s.xml", screenId);
        validateAndLoad(assetPath);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this controller.
     *
     * @param stateManager (not null)
     * @param app which application owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        if (isEnabled()) {
            throw new IllegalStateException("already enabled");
        }
        if (stateManager == null) {
            throw new NullPointerException("manager should not be null");
        }
        if (app == null) {
            throw new NullPointerException("application should not be null");
        }

        super.initialize(stateManager, app);
        application = (GuiApplication) app;
        getNifty().registerScreenController(this);

        assert isInitialized();
    }

    /**
     * Do not invoke!
     *
     * @see #enable(ActionListener)
     * @see #disable()
     */
    @Override
    public void setEnabled(boolean newState) {
        throw new UnsupportedOperationException("not supported");
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
        screenHasStarted = true;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Access the application which owns this screen.
     *
     * @return the pre-existing instance (not null)
     */
    protected Application getApplication() {
        assert application != null;
        return application;
    }

    /**
     * Access the input manager.
     *
     * @return the pre-existing instance (not null)
     */
    protected InputManager getInputManager() {
        InputManager result = application.getInputManager();
        assert result != null;
        return result;
    }

    /**
     * Access the Nifty instance.
     *
     * @return the pre-existing instance (not null)
     */
    protected Nifty getNifty() {
        Nifty result = application.getNifty();
        assert result != null;
        return result;
    }

    /**
     * Alter the label of a button.
     *
     * @param elementId Nifty element id of the button (not null)
     * @param newText (not null)
     */
    protected void setButtonLabel(String elementId, String newText) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }
        if (newText == null) {
            throw new NullPointerException("text should not be null");
        }

        Button button = getScreen().findNiftyControl(elementId, Button.class);
        try {
            button.setText(newText);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks button {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Alter the checked status of a checkbox.
     *
     * @param elementId Nifty element id of the checkbox (not null)
     * @param newStatus true to tick the box, false to un-tick it
     */
    protected void setCheckBox(String elementId, boolean newStatus) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }

        CheckBox box = getScreen().findNiftyControl(elementId, CheckBox.class);
        try {
            box.setChecked(newStatus);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks checkbox {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Select a radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     */
    protected void setRadioButton(String elementId) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }

        RadioButton button =
                getScreen().findNiftyControl(elementId, RadioButton.class);
        try {
            button.select();
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks radio button {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Enable or disable a radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     * @param newState true to enable the button, false to disable it
     */
    protected void setRadioButtonEnabled(String elementId, boolean newState) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }

        RadioButton button =
                getScreen().findNiftyControl(elementId, RadioButton.class);
        try {
            if (newState) {
                button.enable();
            } else {
                button.disable();
            }
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks radio button {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Alter the linear value of a slider. This assumes a naming convention
     * where the slider's Nifty id ends with "Slider".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @param newValue value for the slider
     */
    protected void setSlider(String namePrefix, float newValue) {
        if (namePrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }

        String sliderName = namePrefix + "Slider";
        Slider slider = getScreen().findNiftyControl(sliderName, Slider.class);
        try {
            slider.setValue(newValue);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks slider {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(sliderName)
            });
        }
    }

    /**
     * Alter the text of a status label.
     *
     * @param elementId Nifty element id of the label (not null)
     * @param newText (not null)
     */
    protected void setStatusText(String elementId, String newText) {
        if (elementId == null) {
            throw new NullPointerException("id should not be null");
        }
        if (newText == null) {
            throw new NullPointerException("text should not be null");
        }

        Element element = getScreen().findElementByName(elementId);
        if (element == null) {
            logger.log(Level.INFO, "screen {0} lacks element {1}",
                    new Object[]{
                MyString.quote(screenId),
                MyString.quote(elementId)
            });
            return;
        }
        TextRenderer textRenderer = element.getRenderer(TextRenderer.class);
        if (textRenderer != null) {
            textRenderer.setText(newText);
            return;
        }
        logger.log(Level.WARNING,
                "Nifty element {0} lacks a text renderer",
                MyString.quote(elementId));
    }

    /**
     * Read the value of a linear slider and update its status label. This
     * assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param namePrefix the unique name prefix of the slider (not null)
     * @return value of the slider
     */
    protected float updateSlider(String namePrefix) {
        if (namePrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }

        float value = readSlider(namePrefix);
        updateSliderStatus(namePrefix, value);

        return value;
    }

    /**
     * Read the value of a logarithmic slider and update its status label. This
     * assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @param logBase logarithm base of the slider (>0)
     * @return scaled value of the slider
     */
    protected float updateLogSlider(String namePrefix, float logBase) {
        if (namePrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }
        if (logBase <= 0f) {
            logger.log(Level.SEVERE, "logBase={0}", logBase);
            throw new IllegalArgumentException("log base should be positive");
        }

        float value = readSlider(namePrefix);
        float scaledValue = FastMath.pow(logBase, value);
        updateSliderStatus(namePrefix, scaledValue);

        return scaledValue;
    }
    // *************************************************************************
    // private methods

    /**
     * Read the linear value of a slider. This assumes a naming convention where
     * the slider's Nifty id ends with "Slider".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @return value of the slider
     */
    private float readSlider(String namePrefix) {
        if (namePrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }

        String sliderName = namePrefix + "Slider";
        Slider slider = getScreen().findNiftyControl(sliderName, Slider.class);
        float value = slider.getValue();
        return value;
    }

    /**
     * Update the status of a slider. This assumes a naming convention where the
     * label's Nifty id ends with "SliderStatus".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @param value value of the slider
     */
    private void updateSliderStatus(String namePrefix, float value) {
        if (namePrefix == null) {
            throw new NullPointerException("prefix should not be null");
        }

        String statusName = namePrefix + "SliderStatus";
        /*
         * Select output precision based on the magnitude of the value.
         */
        String format;
        if (FastMath.abs(value) >= 5f) {
            format = "%s = %.1f";
        } else if (FastMath.abs(value) >= 0.5f) {
            format = "%s = %.2f";
        } else if (FastMath.abs(value) >= 0.05f) {
            format = "%s = %.3f";
        } else {
            format = "%s = %.4f";
        }
        String statusText = String.format(format, namePrefix, value);
        statusText = MyString.trimFloat(statusText);
        setStatusText(statusName, statusText);
    }
}
// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

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
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.xml.xpp3.Attributes;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simplified controller for a Nifty screen, with support for sliders and
 * popup menus.
 *
 * Although this is an abstract class, certain methods are implemented to
 * simplify the implementation of subclasses -- unlike Controller and
 * ScreenController.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SimpleScreenController
        extends AbstractAppState
        implements Controller, ScreenController {
    // *************************************************************************
    // constants

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
     * reference to the application instance: set by initialize()
     */
    protected static Application application = null;
    /**
     * which Nifty display this screen uses: set by constructor
     */
    final private NiftyJmeDisplay niftyDisplay;
    /**
     * Nifty element id of the active popup menu (null means none active)
     */
    private static String activePopupId = null;
    /**
     * Nifty id of this screen: set by constructor
     */
    final protected String screenId;
    // *************************************************************************
    // constructors

    /**
     * Default constructor. Do not invoke!
     */
    public SimpleScreenController() {
        niftyDisplay = null;
        screenId = null;
    }

    /**
     * Instantiate a disabled screen for a particular display and id. After
     * instantiation, the first method invoked should be initialize().
     *
     * @param niftyDisplay (not null)
     * @param screenId Nifty screen id (not null)
     */
    public SimpleScreenController(NiftyJmeDisplay niftyDisplay,
            String screenId) {
        if (niftyDisplay == null) {
            throw new NullPointerException("display cannot be null");
        }
        if (screenId == null) {
            throw new NullPointerException("id cannot be null");
        }

        this.niftyDisplay = niftyDisplay;
        this.screenId = screenId;

        super.setEnabled(false);

        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Close the active popup element. Invoked via NiftyMethodInvoker, so the
     * class and method must be public.
     */
    public void closeActivePopup() {
        if (activePopupId == null) {
            throw new IllegalStateException("no active popup");
        }

        getNifty().closePopup(activePopupId);
        activePopupId = null;
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
            throw new NullPointerException("listener cannot be null");
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
        return activePopupId != null;
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
            throw new NullPointerException("id cannot be null");
        }

        if (!isEnabled()) {
            return false;
        }
        Element element = getScreen().findElementByName(elementId);
        if (element == null) {
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
     * Perform the action specified by an action string. Invoked via
     * NiftyMethodInvoker, so the method must be public.
     *
     * @param actionString (not null)
     */
    public static void perform(String actionString) {
        if (actionString == null) {
            throw new NullPointerException("action string cannot be null");
        }

        logger.log(Level.INFO, "{0}", actionString);
        boolean isOnGoing = true;
        float simInterval = 0f;
        try {
            listener.onAction(actionString, isOnGoing, simInterval);
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Caught unexpected throwable:", throwable);
            application.stop(false);
        }
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this screen controller.
     *
     * @param stateManager (not null)
     * @param app which application owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        assert !isEnabled();
        if (stateManager == null) {
            throw new NullPointerException("manager cannot be null");
        }
        if (app == null) {
            throw new NullPointerException("application cannot be null");
        }

        super.initialize(stateManager, app);
        application = app;
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
    // Controller methods

    /**
     * A callback from Nifty, unused.
     */
    @Override
    public void bind(Nifty nifty, Screen screen, Element element,
            Properties properties, Attributes attributes) {
    }

    /**
     * A callback from Nifty, unused.
     */
    @Override
    public void init(Properties properties, Attributes attributes) {
    }

    /**
     * A callback from Nifty, unused.
     */
    @Override
    public void onFocus(boolean flag) {
    }

    /**
     * A callback from Nifty, unused.
     */
    @Override
    public boolean inputEvent(NiftyInputEvent event) {
        return false;
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
     * A callback from Nifty, unused.
     */
    @Override
    public void onStartScreen() {
    }
    // *************************************************************************
    // new protected methods

    /**
     * Access the application which owns the screen.
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
     * Access the Nifty interface.
     *
     * @return the pre-existing instance (not null)
     */
    protected Nifty getNifty() {
        if (niftyDisplay == null) {
            throw new IllegalStateException("no Nifty display");
        }
        Nifty result = niftyDisplay.getNifty();
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
        assert elementId != null;
        assert newText != null;

        Button button = getScreen().findNiftyControl(elementId, Button.class);
        try {
            button.setText(newText);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen lacks button {0}",
                    MyString.quote(elementId));
        }
    }

    /**
     * Alter the checked status of a checkbox.
     *
     * @param elementId Nifty element id of the checkbox (not null)
     * @param newStatus true to tick the box, false to un-tick it
     */
    protected void setCheckBox(String elementId, boolean newStatus) {
        assert elementId != null;

        CheckBox box = getScreen().findNiftyControl(elementId, CheckBox.class);
        try {
            box.setChecked(newStatus);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen lacks checkbox {0}",
                    MyString.quote(elementId));
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
        assert namePrefix != null;

        String sliderName = namePrefix + "Slider";
        Slider slider = getScreen().findNiftyControl(sliderName, Slider.class);
        try {
            slider.setValue(newValue);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen lacks slider {0}",
                    MyString.quote(sliderName));
        }
    }

    /**
     * Alter the text of a status label.
     *
     * @param elementId Nifty element id of the label (not null)
     * @param newText (not null)
     */
    protected void setStatusText(String elementId, String newText) {
        assert elementId != null;
        assert newText != null;

        Element element = getScreen().findElementByName(elementId);
        if (element == null) {
            logger.log(Level.INFO, "Screen lacks element {0}",
                    MyString.quote(elementId));
            return;
        }
        TextRenderer textRenderer = element.getRenderer(TextRenderer.class);
        if (textRenderer != null) {
            textRenderer.setText(newText);
            return;
        }
        logger.log(Level.WARNING, "Nifty element {0} lacks a text renderer",
                MyString.quote(elementId));
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionStringPrefix common prefix of the menu's action strings (not
     * null)
     * @param menuItems collection of menu items (not null)
     */
    protected void showPopup(String actionStringPrefix,
            Collection<String> menuItems) {
        assert activePopupId == null : activePopupId;
        assert actionStringPrefix != null;
        assert menuItems != null;
        /*
         * Parse the action string prefix into words.
         */
        String[] actionPrefixWords = actionStringPrefix.split("\\s+");

        String[] items = Misc.toArray(menuItems);
        showPopup(actionPrefixWords, items);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefixWords prefix words for the menu's action strings (not
     * null)
     * @param items list of menu items (not null)
     */
    protected void showPopup(String[] actionPrefixWords, String[] items) {
        assert activePopupId == null : activePopupId;
        assert actionPrefixWords != null;
        /*
         * Create a popup modeled after "popup-menu" in the XML.
         */
        Nifty nifty = getNifty();
        Element element = nifty.createPopup("popup-menu");
        /*
         * Add items to the popup's menu.
         */
        Menu<String> menu = element.findNiftyControl("#menu", Menu.class);
        for (String item : items) {
            menu.addMenuItem(item, item);
        }
        /*
         * Create a controller with a subscription to the menu's
         * item activation events.
         */
        String elementId = element.getId();
        assert elementId != null;
        PopupMenu popup = new PopupMenu(this, actionPrefixWords);
        Screen screen = nifty.getCurrentScreen();
        String menuId = menu.getId();

        nifty.subscribe(screen, menuId, MenuItemActivatedEvent.class, popup);
        /*
         * Make the element visible.
         */
        nifty.showPopup(screen, elementId, null);
        activePopupId = elementId;
        assert activePopupId != null;
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
            throw new NullPointerException("prefix cannot be null");
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
            throw new NullPointerException("prefix cannot be null");
        }
        if (logBase <= 0f) {
            throw new IllegalArgumentException("log base must be positive");
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
        assert namePrefix != null;

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
        assert namePrefix != null;

        String statusName = namePrefix + "SliderStatus";
        /*
         * Pick output precision based on the magnitude of the value.
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
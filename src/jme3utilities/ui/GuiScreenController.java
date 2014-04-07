/*
 Copyright (c) 2013-2014, Stephen Gold
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

import com.jme3.math.FastMath;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * A BasicScreenController which supports check boxes, popup menus, radio
 * buttons, sliders, and dynamic labels.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class GuiScreenController
        extends BasicScreenController {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(GuiScreenController.class.getName());
    // *************************************************************************
    // fields
    /**
     * screen's input mode while a popup menu is active
     */
    private static InputMode savedMode;
    /**
     * the active popup menu (null means none are active)
     */
    private static PopupMenu activePopupMenu = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen for a specified screen id and layout asset
     * path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization
     */
    public GuiScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        super(screenId, xmlAssetPath, enableDuringInitialization);
        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Escape from the active popup menu and return control to its parent
     * without making a selection. Invoked from NiftyMethodInvoker using
     * reflection, so the class and method must both be public.
     */
    public static synchronized void closeActivePopup() {
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
        } else {
            /*
             * No parent menu, so disable the menu's input mode and
             * re-enable the screen's input mode.
             */
            InputMode menuMode = InputMode.getEnabledMode();
            menuMode.setEnabled(false);
            if (savedMode != null) {
                assert !savedMode.isEnabled();
                savedMode.setEnabled(true);
                savedMode = null;
            }
        }
    }

    /**
     * If the specified popup menu is active, close it and all its ancestors.
     * This method is invoked after a menu selection is made.
     *
     * @param popupMenu which menu to close (not null)
     */
    static synchronized void closePopup(PopupMenu popupMenu) {
        Validate.nonNull(popupMenu, "popup menu");
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
        /*
         * Disable the menu's input mode and re-enable the screen's.
         */
        InputMode menuMode = InputMode.getEnabledMode();
        menuMode.setEnabled(false);
        if (savedMode != null) {
            assert !savedMode.isEnabled();
            savedMode.setEnabled(true);
            savedMode = null;
        }
    }

    /**
     * Test whether there's an active popup element.
     *
     * @return true if there's an active popup element, false if there's none
     */
    public static boolean hasActivePopup() {
        return activePopupMenu != null;
    }

    /**
     * Select a menu item in the active popup.
     *
     * @param index which item (&ge;0, 0 &rarr; first)
     */
    public void selectMenuItem(int index) {
        Validate.nonNegative(index, "index");
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup menu");
        }

        String actionString = activePopupMenu.getActionString(index);
        if (actionString == null) {
            return;
        }
        /*
         * Perform the action described by the action string.
         */
        perform(actionString);
        /*
         * If the menu is still active, close it and all of its ancestors.
         */
        closePopup(activePopupMenu);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param items collection of menu items (not null, unaffected)
     */
    public void showPopup(String actionPrefix, Collection<String> items) {
        Validate.nonNull(actionPrefix, "prefix");
        Validate.nonNull(items, "collection");

        String[] itemArray = MyString.toArray(items);
        showPopup(actionPrefix, itemArray);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param itemArray menu items (not null, unaffected)
     */
    public static synchronized void showPopup(String actionPrefix,
            String[] itemArray) {
        Validate.nonNull(actionPrefix, "prefix");
        Validate.nonNull(itemArray, "item array");
        /*
         * Create the popup using "popup-menu" as a base.
         * Nifty assigns the popup a new id.
         */
        Nifty nifty = getNifty();
        Element element = nifty.createPopup("popup-menu");
        /*
         * Add items to the popup's menu.
         */
        @SuppressWarnings("unchecked")
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
        PopupMenu popup = new PopupMenu(elementId, actionPrefix,
                itemArray, activePopupMenu);
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
        } else {
            /*
             * Save and disable the screen's input mode (if any) and
             * enable the input mode for menus.
             */
            savedMode = InputMode.getEnabledMode();
            if (savedMode != null) {
                savedMode.setEnabled(false);
            }
            InputMode menuMode = InputMode.findMode("menu");
            menuMode.setEnabled(true);
        }
        setActivePopupMenu(popup);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefixWords common prefix words of the menu's action strings
     * (not null, unaffected)
     * @param itemArray menu items (not null, unaffected)
     */
    public void showPopup(String[] actionPrefixWords, String[] itemArray) {
        Validate.nonNull(actionPrefixWords, "word array");
        Validate.nonNull(itemArray, "item array");
        /*
         * Generate the action prefix for this menu.
         */
        String actionPrefix = "";
        int wordCount = actionPrefixWords.length;
        if (wordCount > 0) {
            actionPrefix = MyString.join(actionPrefixWords) + " ";
        }
        showPopup(actionPrefix, itemArray);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Alter the label of a button.
     *
     * @param elementId Nifty element id of the button (not null)
     * @param newText (not null)
     */
    protected void setButtonLabel(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Button button = getScreen().findNiftyControl(elementId, Button.class);
        try {
            button.setText(newText);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks button {1}",
                    new Object[]{
                MyString.quote(getScreenId()),
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
        Validate.nonNull(elementId, "element id");

        CheckBox box = getScreen().findNiftyControl(elementId, CheckBox.class);
        try {
            box.setChecked(newStatus);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks checkbox {1}",
                    new Object[]{
                MyString.quote(getScreenId()),
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
        Validate.nonNull(elementId, "element id");

        RadioButton button =
                getScreen().findNiftyControl(elementId, RadioButton.class);
        try {
            button.select();
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks radio button {1}",
                    new Object[]{
                MyString.quote(getScreenId()),
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
        Validate.nonNull(elementId, "element id");

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
                MyString.quote(getScreenId()),
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
        Validate.nonNull(namePrefix, "prefix");

        String sliderName = namePrefix + "Slider";
        Slider slider = getScreen().findNiftyControl(sliderName, Slider.class);
        try {
            slider.setValue(newValue);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks slider {1}",
                    new Object[]{
                MyString.quote(getScreenId()),
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
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Element element = getScreen().findElementByName(elementId);
        if (element == null) {
            logger.log(Level.INFO, "screen {0} lacks element {1}",
                    new Object[]{
                MyString.quote(getScreenId()),
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
     * @param statusSuffix to specify a unit of measurement (not null)
     * @return value of the slider
     */
    protected float updateSlider(String namePrefix, String statusSuffix) {
        Validate.nonNull(namePrefix, "prefix");
        Validate.nonNull(statusSuffix, "suffix");

        float value = readSlider(namePrefix);
        updateSliderStatus(namePrefix, value, statusSuffix);

        return value;
    }

    /**
     * Read the value of a logarithmic slider and update its status label. This
     * assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @param logBase logarithm base of the slider (&gt;0)
     * @param statusSuffix to specify a unit of measurement (not null)
     * @return scaled value of the slider
     */
    protected float updateLogSlider(String namePrefix, float logBase,
            String statusSuffix) {
        Validate.nonNull(namePrefix, "prefix");
        Validate.positive(logBase, "base");
        Validate.nonNull(statusSuffix, "suffix");

        float value = readSlider(namePrefix);
        float scaledValue = FastMath.pow(logBase, value);
        updateSliderStatus(namePrefix, scaledValue, statusSuffix);

        return scaledValue;
    }
    // *************************************************************************
    // private methods

    /**
     * Read the linear value of a slider. This assumes a naming convention where
     * the slider's Nifty id ends with "Slider".
     *
     * @param namePrefix unique id prefix of the slider (not null)
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
     * Update the static reference to the active popup menu.
     */
    private static void setActivePopupMenu(PopupMenu popupMenu) {
        activePopupMenu = popupMenu;
    }

    /**
     * Update the status of a slider. This assumes a naming convention where the
     * label's Nifty id ends with "SliderStatus".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @param value value of the slider
     * @param statusSuffix to specify a unit of measurement (not null)
     */
    private void updateSliderStatus(String namePrefix, float value,
            String statusSuffix) {
        assert namePrefix != null;
        assert statusSuffix != null;

        /*
         * Select output precision based on the magnitude of the value.
         */
        String format;
        if (FastMath.abs(value) >= 5f) {
            format = "%.1f";
        } else if (FastMath.abs(value) >= 0.5f) {
            format = "%.2f";
        } else if (FastMath.abs(value) >= 0.05f) {
            format = "%.3f";
        } else {
            format = "%.4f";
        }
        String valueString = String.format(format, value);
        valueString = MyString.trimFloat(valueString);
        String statusText = String.format("%s = %s%s",
                namePrefix, valueString, statusSuffix);

        String statusName = namePrefix + "SliderStatus";
        setStatusText(statusName, statusText);
    }
}
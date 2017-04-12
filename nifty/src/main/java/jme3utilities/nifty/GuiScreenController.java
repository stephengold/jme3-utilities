/*
 Copyright (c) 2013-2017, Stephen Gold
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

import com.jme3.math.FastMath;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.InputMode;

/**
 * Basic screen controller with extra support for Nifty controls such as check
 * boxes, dialogs, popup menus, radio buttons, sliders, and dynamic labels.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GuiScreenController extends BasicScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            GuiScreenController.class.getName());
    // *************************************************************************
    // fields

    /**
     * Nifty element for the active modal dialog (null means none active)
     */
    private static Element dialogElement = null;
    /**
     * this screen's suspended input mode (while a popup is active)
     */
    private static InputMode suspendedMode;
    /**
     * active popup menu (null means none are active)
     */
    private static PopupMenu activePopupMenu = null;
    /**
     * action prefix of the active modal dialog (null means none active)
     */
    private static String dialogActionPrefix = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen for a specified screen id and layout asset
     * path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization if true, enable this screen controller
     * during initialization; if false, leave it disabled
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
     * Escape from the active modal dialog and return control to the screen
     * without performing an action.
     */
    static void closeActiveDialog() {
        if (!hasActiveDialog()) {
            throw new IllegalStateException("no active dialog");
        }

        Nifty nifty = getNifty();
        String popupId = dialogElement.getId();
        nifty.closePopup(popupId);
        dialogActionPrefix = null;
        dialogElement = null;
        /*
         * Disable the dialog's input mode and
         * resume the screen's input mode, if any.
         */
        InputMode dialogMode = InputMode.getActiveMode();
        dialogMode.setEnabled(false);
        if (suspendedMode != null) {
            suspendedMode.resume();
            suspendedMode = null;
        }

        assert !hasActiveDialog();
    }

    /**
     * Escape from the active popup menu and return control to its parent
     * without making a selection. Invoked from NiftyMethodInvoker using
     * reflection, so the class and method must both be public.
     */
    public static synchronized void closeActivePopupMenu() {
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup menu");
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
             * resume the screen's input mode.
             */
            InputMode menuMode = InputMode.getActiveMode();
            menuMode.setEnabled(false);
            if (suspendedMode != null) {
                suspendedMode.resume();
                suspendedMode = null;
            }
        }
    }

    /**
     * Close the active popup and all its ancestors.
     */
    public static synchronized void closeAllPopups() {
        if (hasActiveDialog()) {
            closeActiveDialog();
        }
        if (hasActivePopupMenu()) {
            closePopupMenu(activePopupMenu);
        }
    }

    /**
     * If the specified popup menu is active, close it and all its ancestors.
     * This method is invoked after a menu selection is made.
     *
     * @param popupMenu which menu to close (not null)
     */
    static synchronized void closePopupMenu(PopupMenu popupMenu) {
        Validate.nonNull(popupMenu, "popup menu");

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
         * Disable the menu's input mode and resume the screen's input mode.
         */
        InputMode menuMode = InputMode.getActiveMode();
        menuMode.setEnabled(false);
        if (suspendedMode != null) {
            suspendedMode.resume();
            suspendedMode = null;
        }
    }

    /**
     * Perform the dialog entry action, then close the dialog.
     */
    static void dialogEntry() {
        if (!hasActiveDialog()) {
            throw new IllegalStateException("no active dialog");
        }

        TextField textField = dialogElement.findNiftyControl("#textfield",
                TextField.class);
        String enteredText = textField.getRealText();
        /*
         * Convert the text into an action string and perform the action.
         */
        String entryActionString = dialogActionPrefix + enteredText;
        perform(entryActionString);

        closeActiveDialog();
    }

    /**
     * Test whether there's an active dialog.
     *
     * @return true if there's an active dialog, false if there's none
     */
    static boolean hasActiveDialog() {
        return dialogElement != null;
    }

    /**
     * Test whether there's an active popup menu.
     *
     * @return true if there's an active popup menu, false if there's none
     */
    static boolean hasActivePopupMenu() {
        return activePopupMenu != null;
    }

    /**
     * Select an item from the active popup menu.
     *
     * @param index index of the item (&ge;0, 0 &rarr; 1st)
     */
    void selectMenuItem(int index) {
        Validate.nonNegative(index, "index");
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup menu");
        }

        String actionString = activePopupMenu.actionString(index);
        if (actionString == null) {
            /* index out of range */
            return;
        }
        PopupMenu oldPopupMenu = activePopupMenu;
        /*
         * Perform the action described by the action string.
         */
        perform(actionString);
        /*
         * If the old menu is still active, close it and all its ancestors.
         */
        closePopupMenu(oldPopupMenu);
    }

    /**
     * Alter the text of a Nifty element (such as a label) that has a text
     * renderer.
     *
     * @param elementId id of the element (not null)
     * @param newText (not null)
     */
    public void setStatusText(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Element element = getScreen().findElementById(elementId);
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
        logger.log(Level.WARNING, "Nifty element {0} lacks a text renderer",
                MyString.quote(elementId));
    }

    /**
     * Create and activate a simple modal dialog.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param okLabel label for the "OK" button (not null)
     * @param actionPrefix action prefix (not null, usually the final character
     * will be a blank)
     */
    public static void showDialog(String promptMessage, String defaultValue,
            String okLabel, String actionPrefix) {
        Validate.nonNull(actionPrefix, "action prefix");
        /*
         * Create a popup using "dialogs/text-entry" as a base.
         * Nifty assigns the popup a new id.
         */
        dialogElement = nifty.createPopup("dialogs/text-entry");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element prompt = dialogElement.findElementById("#prompt");
        TextRenderer textRenderer = prompt.getRenderer(TextRenderer.class);
        textRenderer.setText(promptMessage);
        TextField textField = dialogElement.findNiftyControl("#textfield",
                TextField.class);
        textField.setText(defaultValue);
        Button okButton = dialogElement.findNiftyControl("#ok",
                Button.class);
        okButton.setText(okLabel);
        /*
         * Make the popup visible without setting the focus.
         */
        Screen screen = nifty.getCurrentScreen();
        nifty.showPopup(screen, popupId, null);
        /*
         * Save and suspend the screen's input mode (if any) and
         * activate the input mode for modal dialogs.
         */
        assert suspendedMode == null : suspendedMode;
        suspendedMode = InputMode.getActiveMode();
        if (suspendedMode != null) {
            suspendedMode.suspend();
        }
        InputMode dialogMode = InputMode.findMode(DialogInputMode.name);
        dialogMode.setEnabled(true);

        dialogActionPrefix = actionPrefix;
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param items collection of menu items (not null, unaffected)
     */
    public static void showPopup(String actionPrefix,
            Collection<String> items) {
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(items, "collection");

        String[] itemArray = MyString.toArray(items);
        showPopup(actionPrefix, itemArray);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a blank)
     * @param itemArray array of menu items (not null, unaffected)
     */
    public static synchronized void showPopup(String actionPrefix,
            String[] itemArray) {
        Validate.nonNull(actionPrefix, "prefix");
        Validate.nonNull(itemArray, "item array");
        /*
         * Create a popup using "popup-menu" as a base.
         * Nifty assigns the popup a new id.
         */
        Element element = nifty.createPopup("popup-menu");
        /*
         * Add items to the new popup's menu.
         */
        @SuppressWarnings("unchecked")
        Menu<String> menu = element.findNiftyControl("#menu", Menu.class);
        assert menu != null;
        for (int itemIndex = 0; itemIndex < itemArray.length; itemIndex++) {
            String item = itemArray[itemIndex];
            String displayText = item.replace("$", "\\$");
            if (itemIndex < 9) {
                displayText = String.format("%d] %s",
                        itemIndex + 1, displayText);
            }
            // TODO icon asset path for each item
            menu.addMenuItem(displayText, item);
        }
        String elementId = element.getId();
        assert elementId != null;
        /*
         * Create a subscriber to handle the new menu's events.
         */
        PopupMenu popup = new PopupMenu(elementId, actionPrefix,
                itemArray, activePopupMenu);
        Screen screen = nifty.getCurrentScreen();
        String controlId = menu.getId();
        nifty.subscribe(screen, controlId, MenuItemActivatedEvent.class, popup);
        /*
         * Make the popup visible without specifying a focus element.
         */
        nifty.showPopup(screen, elementId, null);

        if (activePopupMenu != null) {
            /*
             * Disable the parent popup menu.
             */
            activePopupMenu.setEnabled(false);
        } else {
            /*
             * Save and suspend the screen's input mode (if any) and
             * activate the input mode for popup menus.
             */
            assert suspendedMode == null : suspendedMode;
            suspendedMode = InputMode.getActiveMode();
            if (suspendedMode != null) {
                suspendedMode.suspend();
            }
            InputMode menuMode = InputMode.findMode(MenuInputMode.name);
            menuMode.setEnabled(true);
        }

        setActivePopupMenu(popup);
    }

    /**
     * Create and activate a popup menu.
     *
     * @param actionPrefixWords common prefix words of the menu's action strings
     * (not null, unaffected)
     * @param itemArray array of menu items (not null, unaffected)
     */
    public static void showPopup(String[] actionPrefixWords,
            String[] itemArray) {
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
     * Access a Nifty check box. This assumes a naming convention where the
     * Nifty id of every check box ends with "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @return the pre-existing instance (not null)
     */
    protected CheckBox getCheckBox(String idPrefix) {
        Validate.nonNull(idPrefix, "check box id prefix");

        Screen screen = getScreen();
        String niftyId = idPrefix + "CheckBox";
        CheckBox box = screen.findNiftyControl(niftyId, CheckBox.class);
        if (box == null) {
            logger.log(Level.SEVERE, "missing check box {0} in {1}",
                    new Object[]{
                        MyString.quote(niftyId), MyString.quote(getScreenId())
                    });
            throw new RuntimeException();
        }

        return box;
    }

    /**
     * Access the input mode of this screen while it is suspended by a popup.
     *
     * @return the pre-existing mode, or null if none or not suspended
     */
    protected InputMode getSuspendedMode() {
        return suspendedMode;
    }

    /**
     * Access a Nifty slider. This assumes a naming convention where the Nifty
     * id of every slider ends with "Slider".
     *
     * @param idPrefix unique id prefix of the slider (not null)
     * @return the pre-existing instance (not null)
     */
    protected Slider getSlider(String idPrefix) {
        Validate.nonNull(idPrefix, "slider id prefix");

        Screen screen = getScreen();
        String niftyId = idPrefix + "Slider";
        Slider slider = screen.findNiftyControl(niftyId, Slider.class);
        if (slider == null) {
            logger.log(Level.SEVERE, "missing slider {0} in {1}", new Object[]{
                MyString.quote(niftyId), MyString.quote(getScreenId())
            });
            throw new RuntimeException();
        }

        return slider;
    }

    /**
     * Test whether the identified check box is ticked. This assumes a naming
     * convention where the Nifty id of every check box ends with "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @return true if ticked, otherwise false
     */
    protected boolean isChecked(String idPrefix) {
        Validate.nonNull(idPrefix, "check box id prefix");

        CheckBox checkBox = getCheckBox(idPrefix);
        boolean result = checkBox.isChecked();

        return result;
    }

    /**
     * Alter the label of a Nifty button.
     *
     * @param elementId Nifty element id of the button (not null)
     * @param newText new text for the label (not null)
     */
    protected void setButtonLabel(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Button button = getScreen().findNiftyControl(elementId, Button.class);
        try {
            button.setText(newText);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks button {1}", new Object[]{
                MyString.quote(getScreenId()),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Alter the ticked status of the identified check box. This assumes a
     * naming convention where the Nifty id of every check box ends with
     * "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @param newStatus true to tick the check box, false to un-tick it
     */
    protected void setChecked(String idPrefix, boolean newStatus) {
        Validate.nonNull(idPrefix, "check box id prefix");

        CheckBox checkBox = getCheckBox(idPrefix);
        checkBox.setChecked(newStatus);
    }

    /**
     * Select a Nifty radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     */
    protected void setRadioButton(String elementId) {
        Validate.nonNull(elementId, "element id");

        RadioButton button = getScreen().findNiftyControl(
                elementId, RadioButton.class);
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
     * Enable or disable a Nifty radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     * @param newState true to enable the button, false to disable it
     */
    protected void setRadioButtonEnabled(String elementId, boolean newState) {
        Validate.nonNull(elementId, "element id");

        RadioButton button = getScreen().findNiftyControl(
                elementId, RadioButton.class);
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
     * Alter the linear value of a Nifty slider. This assumes a naming
     * convention where the slider's Nifty id ends with "Slider".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @param newValue new value for the slider
     */
    protected void setSlider(String namePrefix, float newValue) {
        Validate.nonNull(namePrefix, "slider name prefix");

        Slider slider = getSlider(namePrefix);
        slider.setValue(newValue);
    }

    /**
     * Read the value of a linear Nifty slider and update its status label. This
     * assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param namePrefix unique name prefix of the slider (not null)
     * @param statusSuffix suffix to specify a unit of measurement (not null)
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
     * Read the value of a logarithmic Nifty slider and update its status label.
     * This assumes a naming convention where (a) the slider's Nifty id ends in
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
     * Read the linear value of a Nifty slider. This assumes a naming convention
     * where the slider's Nifty id ends with "Slider".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @return value of the slider
     */
    private float readSlider(String namePrefix) {
        assert namePrefix != null;

        Slider slider = getSlider(namePrefix);
        float value = slider.getValue();

        return value;
    }

    /**
     * Update the static reference to the active Nifty popup menu.
     */
    private static void setActivePopupMenu(PopupMenu popupMenu) {
        activePopupMenu = popupMenu;
    }

    /**
     * Update the status of a Nifty slider. This assumes a naming convention
     * where the label's Nifty id ends with "SliderStatus".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @param value value of the slider
     * @param statusSuffix suffix to specify a unit of measurement (not null)
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

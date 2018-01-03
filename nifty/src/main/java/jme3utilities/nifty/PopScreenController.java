/*
 Copyright (c) 2013-2018, Stephen Gold
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

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.ui.InputMode;

/**
 * A basic screen controller with added support for Nifty popup elements such as
 * dialog boxes and popup menus.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PopScreenController extends BasicScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PopScreenController.class.getName());
    // *************************************************************************
    // fields

    /**
     * Nifty element for the active modal dialog box (null means none active)
     */
    private Element dialogElement = null;
    /**
     * Controller for the active modal dialog box (null means none active)
     */
    private DialogController dialogController = null;
    /**
     * this controller's suspended input mode (while a modal popup is active)
     */
    private InputMode suspendedMode;
    /**
     * active popup menu (null means none are active)
     */
    private PopupMenu activePopupMenu = null;
    /**
     * action prefix of the active modal dialog box (null means none active)
     */
    private String dialogActionPrefix = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled controller for the specified screen id and layout
     * asset path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization if true, enable this screen controller
     * during initialization; if false, leave it disabled
     */
    public PopScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        super(screenId, xmlAssetPath, enableDuringInitialization);
        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Activate a newly-created modal dialog box.
     *
     * @param popupId the Nifty id of the popup element (not null)
     * @param actionPrefix action prefix (may be null)
     * @param focusElementId the Nifty id of the focus element, or null for
     * first focusable element
     * @param controller controller for the dialog box, or null for none
     */
    public void activateDialog(String popupId, String actionPrefix,
            String focusElementId, DialogController controller) {
        Validate.nonNull(popupId, "popup id");
        /*
         * Make the popup visible, setting the keyboard focus.
         */
        Screen screen = nifty.getCurrentScreen();
        if (screen == null) {
            throw new NullPointerException();
        }
        Element focusElement;
        if (focusElementId == null) {
            focusElement = null;
        } else {
            focusElement = dialogElement.findElementById(focusElementId);
        }
        nifty.showPopup(screen, popupId, focusElement);
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
        dialogController = controller;
    }

    /**
     * Activate a newly-created popup menu.
     *
     * @param popupMenu (not null)
     * @param menu (not null)
     */
    public void activatePopupMenu(PopupMenu popupMenu, Menu<String> menu) {
        Validate.nonNull(popupMenu, "popup menu");
        /*
         * Subscribe to menu events.
         */
        Screen screen = nifty.getCurrentScreen();
        String controlId = menu.getId();
        nifty.subscribe(screen, controlId, MenuItemActivatedEvent.class,
                popupMenu);
        /*
         * Make the popup visible without specifying a focus element.
         */
        String elementId = popupMenu.getElementId();
        nifty.showPopup(screen, elementId, null);

        if (activePopupMenu == null) {
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

        } else {
            /*
             * Disable the parent popup menu.
             */
            activePopupMenu.setEnabled(false);
        }

        activePopupMenu = popupMenu;
    }

    /**
     * Escape from the active modal dialog and return control to the screen
     * without performing an action.
     */
    void closeActiveDialog() {
        if (!hasActiveDialog()) {
            throw new IllegalStateException("no active dialog");
        }

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
    public synchronized void closeActivePopupMenu() {
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
     * Close the active popup and its ancestors, if any.
     */
    public synchronized void closeAllPopups() {
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
    synchronized void closePopupMenu(PopupMenu popupMenu) {
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
     * If allowed, perform the "commit" action of the active dialog box, then
     * close the dialog box.
     */
    void dialogCommit() {
        if (!hasActiveDialog()) {
            throw new IllegalStateException("no active dialog");
        }

        if (dialogController != null
                && !dialogController.allowCommit(dialogElement)) {
            return;
        }

        if (dialogActionPrefix == null) {
            closeActiveDialog();

        } else {
            ListBox listBox
                    = dialogElement.findNiftyControl("#box", ListBox.class);
            TextField textField = dialogElement.findNiftyControl("#textfield",
                    TextField.class);

            String commitAction;
            if (textField != null) { // text-entry dialog
                String enteredText = textField.getRealText();
                commitAction = dialogActionPrefix + enteredText;

            } else if (listBox != null) { // multi-select dialog
                List indices = listBox.getSelectedIndices();
                commitAction = dialogActionPrefix + MyString.join(",", indices);

            } else { // confirmation dialog
                commitAction = dialogActionPrefix;
            }
            closeActiveDialog();
            perform(commitAction);
        }
    }

    /**
     * Access the input mode of this screen while it is suspended by a popup.
     *
     * @return the pre-existing mode, or null if none or not suspended
     */
    public InputMode getSuspendedMode() {
        return suspendedMode;
    }

    /**
     * Test whether there's an active dialog.
     *
     * @return true if there's an active dialog, false if there's none
     */
    boolean hasActiveDialog() {
        return dialogElement != null;
    }

    /**
     * Test whether there's an active popup menu.
     *
     * @return true if there's an active popup menu, false if there's none
     */
    boolean hasActivePopupMenu() {
        return activePopupMenu != null;
    }

    /**
     * Select the indexed item from the active popup menu.
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
     * Create, customize, and activate a modal confirmation dialog box.
     *
     * @param promptMessage text to display above the buttons (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionString the commit action (not null)
     * @param controller controller for the dialog box, or null for none
     */
    public void showConfirmDialog(String promptMessage, String commitLabel,
            String actionString, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionString, "action string");
        /*
         * Create a popup using the "dialogs/confirm" layout as a base.
         * Nifty assigns the popup a new id.
         */
        dialogElement = nifty.createPopup("dialogs/confirm");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element prompt = dialogElement.findElementById("#prompt");
        TextRenderer textRenderer = prompt.getRenderer(TextRenderer.class);
        textRenderer.setText(promptMessage);

        Button commitButton = dialogElement.findNiftyControl("#commit",
                Button.class);
        commitButton.setText(commitLabel);

        activateDialog(popupId, actionString, "#commit", controller);
    }

    /**
     * Create, customize, and activate a modal informational dialog box.
     *
     * @param titleText text to display above the scrollable area (not null)
     * @param bodyText text to display in the scrollable area (not null)
     */
    public void showInfoDialog(String titleText, String bodyText) {
        Validate.nonNull(titleText, "title text");

        String[] lines = bodyText.split("\n");
        int numLines = lines.length;
        int numChars = bodyText.length();
        /*
         * Create a popup element for the dialog box. Nifty assigns it a new id.
         */
        if (numLines > 10 || numChars > 200) { // TODO use font information
            dialogElement = nifty.createPopup("dialogs/info43");
        } else {
            dialogElement = nifty.createPopup("dialogs/info10");
        }
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element titleElement = dialogElement.findElementById("#title");
        TextRenderer renderer = titleElement.getRenderer(TextRenderer.class);
        renderer.setText(titleText);

        Element lineElement = dialogElement.findElementById("#1");
        renderer = lineElement.getRenderer(TextRenderer.class);
        renderer.setText(bodyText);

        activateDialog(popupId, null, null, null);
    }

    /**
     * Create, customize, and activate a modal multi-selection dialog box.
     *
     * @param promptMessage text to display above the listbox (not null)
     * @param itemList list of items (not null, unaffected)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box, or null for none
     */
    public void showMultiSelectDialog(String promptMessage,
            List<String> itemList, String commitLabel, String actionPrefix,
            DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(itemList, "item list");
        Validate.nonNull(commitLabel, "commit label");
        Validate.nonNull(actionPrefix, "action prefix");
        /*
         * Create a popup using the "dialogs/multiSelect" layout as a base.
         * Nifty assigns the popup a new id.
         */
        dialogElement = nifty.createPopup("dialogs/multiSelect");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element titleElement = dialogElement.findElementById("#prompt");
        TextRenderer renderer = titleElement.getRenderer(TextRenderer.class);
        renderer.setText(promptMessage);

        @SuppressWarnings("unchecked")
        ListBox<String> listBox
                = dialogElement.findNiftyControl("#box", ListBox.class);
        listBox.addAllItems(itemList);

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        activateDialog(popupId, actionPrefix, "#commit", controller);
    }

    /**
     * Create and activate a popup menu without icons.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     * @param itemList list of menu items (not null, unaffected)
     */
    public void showPopupMenu(String actionPrefix, List<String> itemList) {
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(itemList, "item list");

        String[] itemArray = MyString.toArray(itemList);
        showPopupMenu(actionPrefix, itemArray);
    }

    /**
     * Create and activate a popup menu without icons.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     * @param itemArray array of menu items (not null, unaffected)
     */
    public void showPopupMenu(String actionPrefix, String[] itemArray) {
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(itemArray, "item array");

        int numItems = itemArray.length;
        String[] iconArray = new String[numItems];
        showPopupMenu(actionPrefix, itemArray, iconArray);
    }

    /**
     * Create and activate a popup menu with icons.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     * @param itemArray array of menu items (not null, unaffected)
     * @param iconArray array of icon asset paths (not null, unaffected)
     */
    public synchronized void showPopupMenu(String actionPrefix,
            String[] itemArray, String[] iconArray) {
        Validate.nonNull(actionPrefix, "prefix");
        Validate.nonNull(itemArray, "item array");
        Validate.nonNull(itemArray, "icon array");
        assert itemArray.length == iconArray.length;
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
        int maxChars = 0;
        for (int itemIndex = 0; itemIndex < itemArray.length; itemIndex++) {
            String item = itemArray[itemIndex];
            String displayText;
            if (itemIndex < 9) {
                displayText = String.format("%d] %s", itemIndex + 1, item);
            } else if (itemIndex == 9) {
                displayText = String.format("0] %s", item);
            } else {
                displayText = item;
            }
            int numChars = displayText.length();
            if (numChars > maxChars) {
                maxChars = numChars;
            }
            displayText = displayText.replace("$", "\\$"); //escape any $
            String displayIcon = iconArray[itemIndex];
            menu.addMenuItem(displayText, displayIcon, item);
        }
        /*
         * Size the menu to accommodate the item with the longest display text.
         */
        int pixelWidth = 50 + 7 * maxChars; // TODO use font information
        SizeValue width = new SizeValue(pixelWidth, SizeValueType.Pixel);
        menu.setWidth(width);

        String elementId = element.getId();
        assert elementId != null;
        /*
         * Create a subscriber to handle the new menu's events.
         */
        PopupMenu popupMenu;
        if (activePopupMenu == null) {
            popupMenu = new PopupMenu(elementId, actionPrefix, itemArray, this);
        } else {
            popupMenu = new PopupMenu(elementId, actionPrefix,
                    itemArray, activePopupMenu);
        }

        activatePopupMenu(popupMenu, menu);
    }

    /**
     * Create, customize, and activate a modal text-entry dialog box: simplified
     * interface where the controller labels the commit button.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param actionPrefix prefix for generating the commit action (not null,
     * usually the final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextEntryDialog(String promptMessage, String defaultValue,
            String actionPrefix, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");

        showTextEntryDialog(promptMessage, defaultValue, "", actionPrefix,
                controller);
    }

    /**
     * Create, customize, and activate a modal text-entry dialog box.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box, or null for none
     */
    public void showTextEntryDialog(String promptMessage, String defaultValue,
            String commitLabel, String actionPrefix,
            DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionPrefix, "action prefix");
        /*
         * Create a popup using the "dialogs/text-entry" layout as a base.
         * Nifty assigns the popup a new id.
         */
        dialogElement = nifty.createPopup("dialogs/text-entry");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element prompt = dialogElement.findElementById("#prompt");
        TextRenderer textRenderer = prompt.getRenderer(TextRenderer.class);
        textRenderer.setText(promptMessage);

        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        textField.setText(defaultValue);

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        activateDialog(popupId, actionPrefix, "#textfield", controller);
    }
    // *************************************************************************
    // BasicScreenController methods

    /**
     * Callback to update this controller prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        if (hasActiveDialog() && dialogController != null) {
            dialogController.update(dialogElement, elapsedTime);
        }
    }
}

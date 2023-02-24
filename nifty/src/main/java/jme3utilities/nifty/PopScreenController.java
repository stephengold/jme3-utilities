/*
 Copyright (c) 2013-2023, Stephen Gold
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

import de.lessvoid.nifty.NiftyMouse;
import de.lessvoid.nifty.builder.ElementBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.PopupBuilder;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.Menu;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.controls.scrollpanel.builder.ScrollPanelBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.MinimalDialog;
import jme3utilities.nifty.dialog.MultiSelectDialog;
import jme3utilities.ui.InputMode;

/**
 * A BasicScreenController with added support for Nifty popup elements such as
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
     * policy for a submenu's X-coordinate (0&rarr;based purely on mouse
     * pointer, 1&rarr;based purely on parent)
     */
    private float warpX = 0f;
    /**
     * policy for a submenu's Y-coordinate (0&rarr;based purely on mouse
     * pointer, 1&rarr;based purely on parent)
     */
    private float warpY = 0f;
    /**
     * controller for the active modal dialog box (null means none active)
     */
    private DialogController dialogController = null;
    /**
     * Nifty element for the active modal dialog box (null means none active)
     */
    private Element dialogElement = null;
    /**
     * active popup menu (null means none active)
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

    /**
     * Instantiate a disabled controller for the specified screen id and layout
     * asset path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param initialState if Enabled, enable this controller during
     * initialization; if Disabled or null, leave it disabled
     */
    public PopScreenController(
            String screenId, String xmlAssetPath, InitialState initialState) {
        super(screenId, xmlAssetPath, initialState);
        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Escape from the active modal dialog and return control to the screen
     * without performing an action.
     */
    void closeActiveDialog() {
        if (dialogController == null) {
            throw new IllegalStateException("no active dialog");
        }

        String popupId = dialogElement.getId();
        getNifty().closePopup(popupId);
        this.dialogActionPrefix = null;
        setDialogElement(null);
        InputMode.resumeLifo();
        this.dialogController = null;
    }

    /**
     * Escape from the active popup menu and return control to its parent
     * without making a selection. Invoked from NiftyMethodInvoker using
     * reflection, so the class and method must both be public.
     */
    public void closeActivePopupMenu() {
        if (activePopupMenu == null) {
            throw new IllegalStateException("no active popup menu");
        }

        activePopupMenu.close();
        this.activePopupMenu = activePopupMenu.getParent();
        if (activePopupMenu != null) { // Re-enable the parent menu.
            activePopupMenu.setEnabled(true);
        } else {
            InputMode.resumeLifo();
        }
    }

    /**
     * Close one active popup, targeting menus first.
     */
    public void closeAllPopups() {
        if (hasActivePopupMenu()) {
            closePopupMenu(activePopupMenu);
        } else if (dialogController != null) {
            closeActiveDialog();
        }
    }

    /**
     * If the specified popup menu is active, close it and all its ancestors.
     * This method is invoked after a menu selection is made.
     *
     * @param popupMenu which menu to close (not null)
     */
    void closePopupMenu(PopupMenu popupMenu) {
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
        this.activePopupMenu = null;
        InputMode.resumeLifo();
    }

    /**
     * If allowed, perform the "commit" action of the active dialog box, then
     * close the dialog box.
     */
    void dialogCommit() {
        if (dialogController == null) {
            throw new IllegalStateException("no active dialog");
        }

        if (!dialogController.allowCommit(dialogElement)) {
            return;
        }

        if (dialogActionPrefix == null) {
            closeActiveDialog();
        } else {
            String suffix = dialogController.commitSuffix(dialogElement);
            String commitAction = dialogActionPrefix + suffix;
            closeActiveDialog();

            perform(commitAction);
        }
    }

    /**
     * Access the active modal dialog box.
     *
     * @return the controller of active dialog, or null if there's none
     */
    public DialogController getActiveDialog() {
        return dialogController;
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
     * Invert the selection in the active multi-select dialog.
     */
    void invertDialogSelection() {
        if (dialogController == null) {
            throw new IllegalStateException("no active dialog");
        }

        String popupId = dialogElement.getId();
        assert popupId != null;

        @SuppressWarnings("unchecked")
        ListBox<String> listBox
                = dialogElement.findNiftyControl("#box", ListBox.class);

        List selectedIndices = listBox.getSelectedIndices();
        int itemCount = listBox.itemCount();
        for (int itemIndex = 0; itemIndex < itemCount; ++itemIndex) {
            boolean isSelected = selectedIndices.contains(itemIndex);
            if (isSelected) {
                listBox.deselectItemByIndex(itemIndex);
            } else {
                listBox.selectItemByIndex(itemIndex);
            }
        }
    }

    /**
     * Select all items in the active multi-select dialog.
     */
    void selectAllDialogItems() {
        if (dialogController == null) {
            throw new IllegalStateException("no active dialog");
        }

        String popupId = dialogElement.getId();
        assert popupId != null;

        @SuppressWarnings("unchecked")
        ListBox<String> listBox
                = dialogElement.findNiftyControl("#box", ListBox.class);

        List selectedIndices = listBox.getSelectedIndices();
        int itemCount = listBox.itemCount();
        for (int itemIndex = 0; itemIndex < itemCount; ++itemIndex) {
            boolean isSelected = selectedIndices.contains(itemIndex);
            if (!isSelected) {
                listBox.selectItemByIndex(itemIndex);
            }
        }
    }

    /**
     * Select the indexed item from the active popup menu.
     *
     * @param index index of the item (&ge;0, 0 &rarr; first)
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

        // Perform the action described by the action string.
        perform(actionString);

        // If the old menu is still active, close it and all its ancestors.
        closePopupMenu(oldPopupMenu);
    }

    /**
     * Alter the positioning policies for submenus.
     *
     * @param newWarpX the policy for a submenu's X-coordinate (0&rarr;based
     * 100% on mouse pointer, 1&rarr;based 100% on parent)
     * @param newWarpY the policy for a submenu's Y-coordinate (0&rarr;based
     * 100% on mouse pointer, 1&rarr;based 100% on parent)
     */
    public void setSubmenuWarp(float newWarpX, float newWarpY) {
        this.warpX = newWarpX;
        this.warpY = newWarpY;
    }

    /**
     * Create, customize, and activate a modal confirmation dialog box.
     *
     * @param promptMessage text to display above the buttons (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionString the commit action (not null)
     * @param controller controller for the dialog box (not null)
     */
    public void showConfirmDialog(String promptMessage, String commitLabel,
            String actionString, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionString, "action string");
        Validate.nonNull(controller, "controller");
        /*
         * Create a popup using the "dialogs/confirm" layout as a base.
         * Nifty assigns the popup a new id.
         */
        this.dialogElement = getNifty().createPopup("dialogs/confirm");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element prompt = dialogElement.findElementById("#prompt");
        TextRenderer textRenderer = prompt.getRenderer(TextRenderer.class);
        textRenderer.setText(promptMessage);

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
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

        // Create a popup element for the dialog box. Nifty assigns it a new id.
        if (numLines > 10 || numChars > 200) { // TODO use font information
            String masterId = registerInfoScrollDialog(numLines + 2);
            this.dialogElement = getNifty().createPopup(masterId);
        } else {
            this.dialogElement = getNifty().createPopup("dialogs/info10");
        }
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element titleElement = dialogElement.findElementById("#title");
        TextRenderer renderer = titleElement.getRenderer(TextRenderer.class);
        renderer.setText(titleText);

        Element lineElement = dialogElement.findElementById("#1");
        renderer = lineElement.getRenderer(TextRenderer.class);
        renderer.setText(bodyText);

        DialogController controller = new MinimalDialog();
        activateDialog(popupId, null, null, controller);
    }

    /**
     * Create, customize, and activate a modal multi-selection dialog box.
     *
     * @param promptMessage text to display above the listbox (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showMultiSelectDialog(String promptMessage,
            String actionPrefix, MultiSelectDialog<?> controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");
        /*
         * Create a popup using the "dialogs/multiSelect" layout as a base.
         * Nifty assigns the popup a new id.
         */
        this.dialogElement = getNifty().createPopup("dialogs/multiSelect");
        String popupId = dialogElement.getId();
        assert popupId != null;

        Element titleElement = dialogElement.findElementById("#prompt");
        TextRenderer renderer = titleElement.getRenderer(TextRenderer.class);
        renderer.setText(promptMessage);

        @SuppressWarnings("unchecked")
        ListBox<String> listBox
                = dialogElement.findNiftyControl("#box", ListBox.class);
        List<String> allDescriptions = controller.listItemDescriptions();
        listBox.addAllItems(allDescriptions);

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        String commitButtonText = controller.commitDescription();
        commitButton.setText(commitButtonText);

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
     * Display the specified PopupMenuBuilder unless it's empty.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     * @param menuBuilder (not null)
     */
    public void showPopupMenu(
            String actionPrefix, PopupMenuBuilder menuBuilder) {
        Validate.nonNull(menuBuilder, "menuBuilder");
        Validate.nonNull(actionPrefix, "action prefix");
        logger.log(
                Level.INFO, "actionPrefix = {0}", MyString.quote(actionPrefix));

        if (!menuBuilder.isEmpty()) {
            String[] itemArray = menuBuilder.copyItems();
            String[] iconArray = menuBuilder.copyIconAssetPaths();
            showPopupMenu(actionPrefix, itemArray, iconArray);
        }
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
    public void showPopupMenu(
            String actionPrefix, String[] itemArray, String[] iconArray) {
        Validate.nonNull(actionPrefix, "prefix");
        Validate.nonNull(itemArray, "item array");
        Validate.nonNull(itemArray, "icon array");
        Validate.require(
                itemArray.length == iconArray.length, "equal-length arrays");
        assert isEnabled();
        /*
         * Create a popup using "popup-menu" as a base.
         * Nifty assigns the popup a new id.
         */
        Element element = getNifty().createPopup("popup-menu");

        // Add items to the new popup's menu.
        @SuppressWarnings("unchecked")
        Menu<String> menu = element.findNiftyControl("#menu", Menu.class);
        assert menu != null;
        int maxChars = 0;
        for (int itemIndex = 0; itemIndex < itemArray.length; ++itemIndex) {
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

        // Size the menu to accommodate the item with the longest display text.
        int pixelWidth = 50 + 7 * maxChars; // TODO use font information
        SizeValue width = new SizeValue(pixelWidth, SizeValueType.Pixel);
        menu.setWidth(width);

        String elementId = element.getId();
        assert elementId != null;

        // Create a subscriber to handle the new menu's events.
        PopupMenu popupMenu;
        if (activePopupMenu == null) {
            popupMenu = new PopupMenu(elementId, actionPrefix, itemArray, this);
        } else {
            popupMenu = new PopupMenu(
                    elementId, actionPrefix, itemArray, activePopupMenu);
        }

        activatePopupMenu(popupMenu, menu);
    }

    /**
     * Create, customize, and activate a modal text-and-check dialog box:
     * simplified interface where the controller labels the commit button.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param actionPrefix prefix for generating the commit action (not null,
     * usually the final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextAndCheckDialog(
            String promptMessage, String defaultValue, String actionPrefix,
            DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");

        showTextAndCheckDialog(
                promptMessage, defaultValue, "", actionPrefix, controller);
    }

    /**
     * Create, customize, and activate a modal text-and-check dialog box.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextAndCheckDialog(
            String promptMessage, CharSequence defaultValue, String commitLabel,
            String actionPrefix, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");
        assert isEnabled();
        /*
         * Create a popup using the "dialogs/text-and-check" layout as a base.
         * Nifty assigns the popup a new ID.
         */
        this.dialogElement = getNifty().createPopup("dialogs/text-and-check");
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

    /**
     * Create, customize, and activate a modal text-and-slider dialog box:
     * simplified interface where the controller labels the commit button.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param actionPrefix prefix for generating the commit action (not null,
     * usually the final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextAndSliderDialog(
            String promptMessage, String defaultValue, String actionPrefix,
            DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");

        showTextAndSliderDialog(
                promptMessage, defaultValue, "", actionPrefix, controller);
    }

    /**
     * Create, customize, and activate a modal text-and-slider dialog box.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextAndSliderDialog(
            String promptMessage, CharSequence defaultValue, String commitLabel,
            String actionPrefix, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");
        assert isEnabled();
        /*
         * Create a popup using the "dialogs/text-and-slider" layout as a base.
         * Nifty assigns the popup a new id.
         */
        this.dialogElement = getNifty().createPopup("dialogs/text-and-slider");
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

        showTextEntryDialog(
                promptMessage, defaultValue, "", actionPrefix, controller);
    }

    /**
     * Create, customize, and activate a modal text-entry dialog box.
     *
     * @param promptMessage text to display above the textfield (not null)
     * @param defaultValue default text for the textfield (not null)
     * @param commitLabel text for the commit-button label (not null)
     * @param actionPrefix prefix for the commit action (not null, usually the
     * final character will be a space)
     * @param controller controller for the dialog box (not null)
     */
    public void showTextEntryDialog(
            String promptMessage, CharSequence defaultValue, String commitLabel,
            String actionPrefix, DialogController controller) {
        Validate.nonNull(promptMessage, "prompt message");
        Validate.nonNull(defaultValue, "default value");
        Validate.nonNull(commitLabel, "commit-button label");
        Validate.nonNull(actionPrefix, "action prefix");
        Validate.nonNull(controller, "controller");
        assert isEnabled();
        /*
         * Create a popup using the "dialogs/text-entry" layout as a base.
         * Nifty assigns the popup a new id.
         */
        this.dialogElement = getNifty().createPopup("dialogs/text-entry");
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
    // new protected methods

    /**
     * Activate a newly-created modal dialog box.
     *
     * @param popupId the Nifty id of the popup element (not null)
     * @param actionPrefix action prefix (may be null)
     * @param focusElementId the Nifty id of the focus element, or null for
     * first focusable element
     * @param controller controller for the dialog box (not null)
     */
    protected void activateDialog(String popupId, String actionPrefix,
            String focusElementId, DialogController controller) {
        Validate.nonNull(popupId, "popup id");
        Validate.nonNull(controller, "controller");

        // Make the popup visible, setting the keyboard focus.
        Screen screen = getNifty().getCurrentScreen();
        assert screen != null;
        Element focusElement;
        if (focusElementId == null) {
            focusElement = null;
        } else {
            focusElement = dialogElement.findElementById(focusElementId);
        }
        getNifty().showPopup(screen, popupId, focusElement);

        InputMode dialogMode = InputMode.findMode(DialogInputMode.name);
        InputMode.suspendAndActivate(dialogMode);

        this.dialogActionPrefix = actionPrefix;
        this.dialogController = controller;
    }

    /**
     * Access the active dialog.
     *
     * @return the active Element, or null if none
     */
    protected Element getDialogElement() {
        return dialogElement;
    }

    /**
     * Alter the active dialog.
     *
     * @param element which element to activate, or null to deactivate
     */
    protected void setDialogElement(Element element) {
        this.dialogElement = element;
    }
    // *************************************************************************
    // BasicScreenController methods

    /**
     * Update this controller prior to rendering. (Invoked once per frame.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (dialogController != null) {
            dialogController.update(dialogElement, tpf);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Activate a newly-created popup menu.
     *
     * @param popupMenu the popup's controller (not null)
     * @param menu the menu to display in the popup (not null)
     */
    private void activatePopupMenu(PopupMenu popupMenu, Menu<String> menu) {
        Validate.nonNull(popupMenu, "popup menu");

        // Subscribe to menu events.
        Screen screen = getNifty().getCurrentScreen();
        String controlId = menu.getId();
        getNifty().subscribe(
                screen, controlId, MenuItemActivatedEvent.class, popupMenu);
        /*
         * The menu will appear at the mouse pointer.  For a submenu,
         * warp the pointer based on the location of the parent menu.
         * Warping counteracts the tendency of deeply nested menus
         * to drift downward and the right.
         */
        if (activePopupMenu != null) {
            NiftyMouse mouse = getNifty().getNiftyMouse();
            float floatX = mouse.getX();
            float floatY = mouse.getY();

            String parentMenuId = activePopupMenu.getElementId() + "#menu";
            Element parentMenu = screen.findElementById(parentMenuId);
            floatX = MyMath.lerp(warpX, floatX, parentMenu.getX());
            floatY = MyMath.lerp(warpY, floatY, parentMenu.getY());
            int intX = Math.round(floatX);
            int intY = Math.round(floatY);
            mouse.setMousePosition(intX, intY);
        }

        // Make the popup visible without specifying a focus element.
        String elementId = popupMenu.getElementId();
        getNifty().showPopup(screen, elementId, null);

        if (activePopupMenu == null) {
            InputMode menuMode = InputMode.findMode(MenuInputMode.name);
            InputMode.suspendAndActivate(menuMode);
        } else { // Disable the parent popup menu.
            activePopupMenu.setEnabled(false);
        }

        this.activePopupMenu = popupMenu;
    }

    /**
     * Register a scrolling informational dialog box with the specified
     * capacity.
     *
     * @param capacity number of lines of text (&gt;0)
     */
    private String registerInfoScrollDialog(int capacity) {
        Validate.positive(capacity, "number of lines");

        int pixelHeight = 16 * capacity; // TODO use font information
        final String heightText = pixelHeight + "px";

        final ElementBuilder.VAlign top = ElementBuilder.VAlign.Top;
        String masterId = "dialogs/infoScroll" + capacity;
        new PopupBuilder(masterId) {
            {
                backgroundColor("#000a");
                childLayoutCenter();

                panel(new PanelBuilder() {
                    {
                        childLayoutVertical();
                        backgroundColor("#aaaf");
                        padding("8px");
                        height("300px");
                        width("630px");

                        control(new LabelBuilder("#title"));
                        panel(new PanelBuilder() {
                            {
                                height("8px");
                            }
                        });
                        control(new ScrollPanelBuilder("") {
                            {
                                height("220px");
                                parameter("horizontal", "false");
                                parameter("pageSizeY", "200");
                                parameter("stepSizeY", "50");

                                panel(new PanelBuilder() {
                                    {
                                        childLayoutVertical();
                                        backgroundColor("#acff");
                                        padding("0px,6px");
                                        height(heightText);
                                        width("590px");

                                        control(new LabelBuilder("#1") {
                                            {
                                                color("#000f");
                                                textVAlign(top);
                                                wrap(true);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        panel(new PanelBuilder() {
                            {
                                height("8px");
                            }
                        });
                        panel(new PanelBuilder() {
                            {
                                childLayoutHorizontal();

                                panel(new PanelBuilder());
                                control(new ButtonBuilder("#cancel") {
                                    {
                                        label("Dismiss");
                                        width("50px");
                                        interactOnRelease(
                                                "performActive(cancel)");
                                    }
                                });
                                panel(new PanelBuilder());
                            }
                        });
                    }
                });
            }
        }.registerPopup(getNifty());

        return masterId;
    }
}
